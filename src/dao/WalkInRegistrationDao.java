package dao;

import entity.Guest;
import entity.RegistrationStatus;
import entity.WalkInRegistration;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * Saves and retrieves walk-in registration records using text format.
 *
 * @author Lai Jen Feng
 */
public class WalkInRegistrationDao {

    private String fileName = "walkin_registration.dat";

    public void saveToFile(WalkInRegistration[] registrations) {

        File file = new File(fileName);

        

        try (PrintWriter writer
                = new PrintWriter(new FileWriter(file))) {

            if (registrations == null) {
                return;
            }

            for (WalkInRegistration registration : registrations) {

                if (registration == null
                        || registration.getGuest() == null) {
                    continue;
                }

                writer.println(
                        registration.getRegistrationId() + "|"
                        + registration.getGuest().getGuestId() + "|"
                        + registration.getRequestedRoomType() + "|"
                        + registration.getNumberOfGuests() + "|"
                        + registration.getRegistrationTime() + "|"
                        + registration.getCheckInDateTime() + "|"
                        + registration.getCheckOutDateTime() + "|"
                        + registration.getActualCheckOutDateTime() + "|"
                        + registration.getStatus().name());
            }

        } catch (FileNotFoundException ex) {

            System.out.println(
                    "\n" + fileName + " not found");

            ex.printStackTrace();

        } catch (IOException ex) {

            System.out.println(
                    "\ncannot save to " + fileName);

            ex.printStackTrace();
        }
    }

    public WalkInRegistration[] retrieveFromFile() {

        File file = new File(fileName);

        WalkInRegistration[] temp
                = new WalkInRegistration[100];

        int count = 0;

        Guest[] guests
                = new GuestDao().loadOrSeed();

        try (BufferedReader reader
                = new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {

                String[] parts
                        = line.split("\\|", -1);

                if (parts.length < 9) {
                    continue;
                }

                Guest guest
                        = findGuestById(
                                guests,
                                parts[1]);

                if (guest == null) {
                    continue;
                }

                WalkInRegistration registration
                        = new WalkInRegistration(
                                parts[0],
                                guest,
                                parts[2],
                                Integer.parseInt(parts[3]),
                                parseDateTime(parts[5]),
                                parseDateTime(parts[6]));

                registration.setRegistrationTime(
                        parseDateTime(parts[4]));

                registration.setActualCheckOutDateTime(
                        parseDateTime(parts[7]));

                registration.setStatus(
                        RegistrationStatus.valueOf(parts[8]));

                temp[count++]
                        = registration;
            }

        } catch (FileNotFoundException ex) {

            System.out.println(
                    "\n" + fileName + " not found");

        } catch (IOException ex) {

            System.out.println(
                    "\nCannot read from " + fileName);

            ex.printStackTrace();
        }

        WalkInRegistration[] registrations
                = new WalkInRegistration[count];

        System.arraycopy(
                temp,
                0,
                registrations,
                0,
                count);

        return registrations;
    }

    public WalkInRegistration[] loadExisting() {

        File file
                = new File(fileName);

        if (!file.exists()) {
            return new WalkInRegistration[0];
        }

        return retrieveFromFile();
    }

    public void upsert(
            WalkInRegistration registration) {

        if (registration == null
                || registration.getRegistrationId() == null) {
            return;
        }

        WalkInRegistration[] current
                = loadExisting();

        int existingIndex
                = findIndexById(
                        current,
                        registration.getRegistrationId());

        if (existingIndex >= 0) {

            current[existingIndex]
                    = registration;

            saveToFile(current);

            return;
        }

        WalkInRegistration[] updated
                = new WalkInRegistration[
                        current.length + 1];

        System.arraycopy(
                current,
                0,
                updated,
                0,
                current.length);

        updated[current.length]
                = registration;

        saveToFile(updated);
    }

    private Guest findGuestById(
            Guest[] guests,
            String guestId) {

        for (Guest guest : guests) {

            if (guest != null
                    && guest.getGuestId()
                            .equals(guestId)) {

                return guest;
            }
        }

        return null;
    }

    private LocalDateTime parseDateTime(
            String value) {

        if (value == null
                || value.equals("null")) {

            return null;
        }

        return LocalDateTime.parse(value);
    }

    private int findIndexById(
            WalkInRegistration[] registrations,
            String registrationId) {

        for (int i = 0;
                i < registrations.length;
                i++) {

            WalkInRegistration registration
                    = registrations[i];

            if (registration != null
                    && registration.getRegistrationId()
                            .equalsIgnoreCase(registrationId)) {

                return i;
            }
        }

        return -1;
    }
}