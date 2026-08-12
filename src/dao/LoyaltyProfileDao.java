package dao;

import entity.LoyaltyProfile;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Persists hotel loyalty membership profiles.
 *
 * LoyaltyProfile is long-lived master data. MemberDao remains separate and is
 * only for VIP guests currently waiting inside the MaxHeap.
 *
 * @author Low Enn Toong
 */
public class LoyaltyProfileDao {
    private static final String FILE_NAME = "loyalty_profile.dat";

    public void saveToFile(LoyaltyProfile[] profiles) {
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            output.writeObject(profiles == null ? new LoyaltyProfile[0] : profiles);
        } catch (IOException exception) {
            System.out.println("Unable to save " + FILE_NAME + ".");
        }
    }

    public LoyaltyProfile[] retrieveFromFile() {
        File file = new File(FILE_NAME);

        if (!file.exists()) {
            return new LoyaltyProfile[0];
        }

        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {
            Object data = input.readObject();
            return data instanceof LoyaltyProfile[] ? (LoyaltyProfile[]) data : new LoyaltyProfile[0];
        } catch (EOFException exception) {
            return new LoyaltyProfile[0];
        } catch (IOException | ClassNotFoundException exception) {
            System.out.println("Unable to read " + FILE_NAME + ".");
            return new LoyaltyProfile[0];
        }
    }

    public LoyaltyProfile[] loadOrSeed() {
        File file = new File(FILE_NAME);

        if (file.exists()) {
            LoyaltyProfile[] loaded = retrieveFromFile();
            if (loaded.length > 0) {
                boolean changed = false;

                for (LoyaltyProfile profile : loaded) {
                    if (profile != null && profile.normalizeLegacyCompletedStays()) {
                        changed = true;
                    }
                }

                if (changed) {
                    saveToFile(loaded);
                }

                return loaded;
            }
        }

        LoyaltyProfile[] seeded = seedSampleData();
        saveToFile(seeded);
        return seeded;
    }

    /**
     * Sample profiles match the simple completed-stay thresholds. These are
     * seed/demo records only; future guests qualify automatically from their
     * completed stay history.
     */
    public LoyaltyProfile[] seedSampleData() {
        return new LoyaltyProfile[] {
            new LoyaltyProfile("1", 10),
            new LoyaltyProfile("2", 6),
            new LoyaltyProfile("3", 3)
        };
    }
}