package dao;

import entity.LoyaltyProfile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Persists hotel loyalty membership profiles using text format.
 *
 * @author Low Enn Toong
 */
public class LoyaltyProfileDao {

    private static final String FILE_NAME = "loyalty_profile.dat";

    public void saveToFile(LoyaltyProfile[] profiles) {

        try (PrintWriter writer =
                new PrintWriter(new FileWriter(FILE_NAME))) {

            if (profiles == null) {
                return;
            }

            for (LoyaltyProfile profile : profiles) {

                if (profile == null) {
                    continue;
                }

                writer.println(
                        profile.getGuestId()
                        + "|"
                        + profile.getCompletedStays());
            }

        } catch (IOException exception) {

            System.out.println(
                    "Unable to save " + FILE_NAME + ".");
        }
    }

    public LoyaltyProfile[] retrieveFromFile() {

        File file = new File(FILE_NAME);

        if (!file.exists()) {
            return new LoyaltyProfile[0];
        }

        LoyaltyProfile[] temporary =
                new LoyaltyProfile[100];

        int count = 0;

        try (BufferedReader reader =
                new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {

                String[] parts =
                        line.split("\\|", -1);

                if (parts.length < 2) {
                    continue;
                }

                String guestId =
                        parts[0].trim();

                int completedStays =
                        Integer.parseInt(parts[1].trim());

                temporary[count++] =
                        new LoyaltyProfile(
                                guestId,
                                completedStays);
            }

        } catch (IOException | NumberFormatException exception) {

            System.out.println(
                    "Unable to read "
                    + FILE_NAME
                    + ".");

            return new LoyaltyProfile[0];
        }

        LoyaltyProfile[] profiles =
                new LoyaltyProfile[count];

        System.arraycopy(
                temporary,
                0,
                profiles,
                0,
                count);

        return profiles;
    }

    public LoyaltyProfile[] loadOrSeed() {

        File file = new File(FILE_NAME);

        if (file.exists()) {

            LoyaltyProfile[] loaded =
                    retrieveFromFile();

            if (loaded.length > 0) {
                return loaded;
            }
        }

        LoyaltyProfile[] seeded =
                seedSampleData();

        saveToFile(seeded);

        return seeded;
    }

    public LoyaltyProfile[] seedSampleData() {

        return new LoyaltyProfile[] {

            new LoyaltyProfile("G0011", 12), // DIAMOND
            new LoyaltyProfile("G0012", 10), // DIAMOND

            new LoyaltyProfile("G0013", 8),  // PLATINUM
            new LoyaltyProfile("G0014", 6),  // PLATINUM

            new LoyaltyProfile("G0015", 4)   // ELITE
        };
    }
}