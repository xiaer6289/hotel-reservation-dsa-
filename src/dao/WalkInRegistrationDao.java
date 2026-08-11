package dao;

import entity.WalkInRegistration;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Saves and retrieves walk-in registration records.
 *
 * @author Lai Jen Feng
 */
public class WalkInRegistrationDao {

    private static final String FILE_NAME = "walkin_registration.dat";

    public void saveToFile(WalkInRegistration[] registrations) {
        try (ObjectOutputStream output = new ObjectOutputStream(
                new FileOutputStream(FILE_NAME))) {

            output.writeObject(registrations == null
                    ? new WalkInRegistration[0]
                    : registrations);

        } catch (IOException exception) {
            System.out.println("Unable to save " + FILE_NAME + ".");
        }
    }

    public WalkInRegistration[] retrieveFromFile() {
        File file = new File(FILE_NAME);

        if (!file.exists()) {
            return new WalkInRegistration[0];
        }

        try (ObjectInputStream input = new ObjectInputStream(
                new FileInputStream(file))) {

            Object data = input.readObject();
            return data instanceof WalkInRegistration[]
                    ? (WalkInRegistration[]) data
                    : new WalkInRegistration[0];

        } catch (FileNotFoundException | EOFException exception) {
            return new WalkInRegistration[0];
        } catch (IOException | ClassNotFoundException exception) {
            System.out.println("Unable to read " + FILE_NAME + ".");
            return new WalkInRegistration[0];
        }
    }

    public void upsert(WalkInRegistration registration) {
        if (registration == null || registration.getRegistrationId() == null) {
            return;
        }

        WalkInRegistration[] current = retrieveFromFile();
        int existingIndex = findIndexById(
                current,
                registration.getRegistrationId());

        if (existingIndex >= 0) {
            current[existingIndex] = registration;
            saveToFile(current);
            return;
        }

        WalkInRegistration[] updated
                = new WalkInRegistration[current.length + 1];

        System.arraycopy(current, 0, updated, 0, current.length);
        updated[current.length] = registration;
        saveToFile(updated);
    }

    private int findIndexById(
            WalkInRegistration[] registrations,
            String registrationId) {

        for (int i = 0; i < registrations.length; i++) {
            WalkInRegistration registration = registrations[i];

            if (registration != null
                    && registration.getRegistrationId()
                            .equalsIgnoreCase(registrationId)) {

                return i;
            }
        }

        return -1;
    }
}