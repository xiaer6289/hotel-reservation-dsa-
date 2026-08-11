package dao;

import entity.WalkInRegistration;
<<<<<<< HEAD
=======
import java.io.EOFException;
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
<<<<<<< HEAD
 * Handles storage and retrieval of walk-in registration records.
=======
 * Saves and retrieves walk-in registration records.
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
 *
 * @author Lai Jen Feng
 */
public class WalkInRegistrationDao {

<<<<<<< HEAD
    private final String fileName = "registration.dat";

    public void saveToFile(
            WalkInRegistration[] registrations) {

        File file = new File(fileName);

        System.out.println(
                "saving to: "
                + file.getAbsolutePath());

        try {
            ObjectOutputStream output
                    = new ObjectOutputStream(
                            new FileOutputStream(file));

            output.writeObject(registrations);
            output.close();

        } catch (FileNotFoundException ex) {

            System.out.println(
                    "\n"
                    + fileName
                    + " not found");

            ex.printStackTrace();

        } catch (IOException ex) {

            System.out.println(
                    "\ncannot save to "
                    + fileName);

            ex.printStackTrace();
=======
    private static final String FILE_NAME = "walkin_registration.dat";

    public void saveToFile(WalkInRegistration[] registrations) {
        try (ObjectOutputStream output = new ObjectOutputStream(
                new FileOutputStream(FILE_NAME))) {

            output.writeObject(registrations == null
                    ? new WalkInRegistration[0]
                    : registrations);

        } catch (IOException exception) {
            System.out.println("Unable to save " + FILE_NAME + ".");
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
        }
    }

    public WalkInRegistration[] retrieveFromFile() {
<<<<<<< HEAD

        File file = new File(fileName);

        WalkInRegistration[] registrations
                = new WalkInRegistration[0];

        if (!file.exists()) {
            return registrations;
        }

        try {
            ObjectInputStream input
                    = new ObjectInputStream(
                            new FileInputStream(file));

            registrations
                    = (WalkInRegistration[])
                            input.readObject();

            input.close();

        } catch (FileNotFoundException ex) {

            return new WalkInRegistration[0];

        } catch (IOException ex) {

            System.out.println(
                    "\nCannot read from "
                    + fileName);

            ex.printStackTrace();

        } catch (ClassNotFoundException ex) {

            System.out.println(
                    "\nClass not found");

            ex.printStackTrace();
        }

        return registrations;
    }

    public WalkInRegistration[] loadExisting() {

        File file = new File(fileName);

        if (!file.exists()) {
            return new WalkInRegistration[0];
        }

        WalkInRegistration[] registrations
                = retrieveFromFile();

        if (registrations == null) {
            return new WalkInRegistration[0];
        }

        return registrations;
=======
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
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
    }
}