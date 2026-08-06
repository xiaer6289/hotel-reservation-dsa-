package dao;

import entity.TaskLogEntry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * @author Low Wei Shin
 */
public class HousekeepingDao {

    private final String fileName = "tasklog.dat";

    public void saveToFile(TaskLogEntry[] taskLogEntries) {
        File file = new File(fileName);
        try {
            ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(file));
            ooStream.writeObject(taskLogEntries);
            ooStream.close();
        } catch (FileNotFoundException ex) {
            System.out.println("\n" + fileName + " not found");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("\ncannot save to " + fileName);
            ex.printStackTrace();
        }
    }

    public TaskLogEntry[] retrieveFromFile() {
        File file = new File(fileName);
        TaskLogEntry[] taskLogEntries = new TaskLogEntry[0];
        try {
            ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file));
            taskLogEntries = (TaskLogEntry[]) oiStream.readObject();
            oiStream.close();
        } catch (FileNotFoundException ex) {
            System.out.println("\n" + fileName + " not found");
        } catch (IOException ex) {
            System.out.println("\nCannot read from " + fileName);
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            System.out.println("\nclass not found");
            ex.printStackTrace();
        } finally {
            return taskLogEntries;
        }
    }

    public TaskLogEntry[] loadOrSeed() {
        return retrieveFromFile();
    }
}
