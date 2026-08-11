package dao;

import entity.Member;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Saves and retrieves VIP members that are still waiting in the MaxHeap.
 *
 * @author Low Enn Toong
 */
public class MemberDao {

    private static final String FILE_NAME = "member.dat";

    public void saveToFile(Member[] members) {
        try (ObjectOutputStream output = new ObjectOutputStream(
                new FileOutputStream(FILE_NAME))) {

            output.writeObject(members == null ? new Member[0] : members);

        } catch (IOException exception) {
            System.out.println("Unable to save " + FILE_NAME + ".");
        }
    }

    public Member[] retrieveFromFile() {
        File file = new File(FILE_NAME);

        if (!file.exists()) {
            return new Member[0];
        }

        try (ObjectInputStream input = new ObjectInputStream(
                new FileInputStream(file))) {

            Object data = input.readObject();
            return data instanceof Member[]
                    ? (Member[]) data
                    : new Member[0];

        } catch (FileNotFoundException | EOFException exception) {
            return new Member[0];
        } catch (IOException | ClassNotFoundException exception) {
            System.out.println("Unable to read " + FILE_NAME + ".");
            return new Member[0];
        }
    }
}