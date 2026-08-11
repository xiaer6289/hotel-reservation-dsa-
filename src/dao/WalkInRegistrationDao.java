package dao;

import entity.WalkInRegistration;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Handles storage and retrieval of walk-in registration records.
 *
 * @author Lai Jen Feng
 */
public class WalkInRegistrationDao {

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
        }
    }

    public WalkInRegistration[] retrieveFromFile() {

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
    }
}