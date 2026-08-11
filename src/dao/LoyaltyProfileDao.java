package dao;

import entity.LoyaltyProfile;
import entity.LoyaltyTier;
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
 * The loyalty profile is long-lived master data. It is intentionally separate
 * from MemberDao, which stores VIP guests that are currently waiting in the
 * priority MaxHeap.
 *
 * @author Low Enn Toong
 */
public class LoyaltyProfileDao {

    private static final String FILE_NAME = "loyalty_profile.dat";

    public void saveToFile(LoyaltyProfile[] profiles) {
        try (ObjectOutputStream output = new ObjectOutputStream(
                new FileOutputStream(FILE_NAME))) {

            output.writeObject(
                    profiles == null ? new LoyaltyProfile[0] : profiles);

        } catch (IOException exception) {
            System.out.println("Unable to save " + FILE_NAME + ".");
        }
    }

    public LoyaltyProfile[] retrieveFromFile() {
        File file = new File(FILE_NAME);

        if (!file.exists()) {
            return new LoyaltyProfile[0];
        }

        try (ObjectInputStream input = new ObjectInputStream(
                new FileInputStream(file))) {

            Object data = input.readObject();
            return data instanceof LoyaltyProfile[]
                    ? (LoyaltyProfile[]) data
                    : new LoyaltyProfile[0];

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
                return loaded;
            }
        }

        LoyaltyProfile[] seeded = seedSampleData();
        saveToFile(seeded);
        return seeded;
    }

    /**
     * Sample existing memberships for the seeded guests in GuestDao.
     * New walk-in guests are not automatically given a loyalty tier.
     */
    public LoyaltyProfile[] seedSampleData() {
        return new LoyaltyProfile[] {
            new LoyaltyProfile("M0001", "1", LoyaltyTier.DIAMOND),
            new LoyaltyProfile("M0002", "2", LoyaltyTier.PLATINUM),
            new LoyaltyProfile("M0003", "3", LoyaltyTier.ELITE)
        };
    }
}