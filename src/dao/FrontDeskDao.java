/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import adt.bst.Bst;
import adt.bst.BstInterface;
import entity.Booking;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * @author Lee Cheng Xuan
 */
public class FrontDeskDao {
    private String fileName = "frontDesk.dat"; // for security, should not hardcode
    
    public void saveToFile(BstInterface<String, Booking> frontDeskBst) {
        File file = new File(fileName);
        System.out.println("saving to: " + file.getAbsolutePath());
        try {
            ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(file));
            ooStream.writeObject(frontDeskBst);
            ooStream.close();
        } catch (FileNotFoundException ex) {
            System.out.println("\n" + fileName + " not found");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("\ncannot save to " + fileName);
            ex.printStackTrace();
        }
    }
    
    public BstInterface<String, Booking> retrieveFromFile() {
        File file = new File(fileName);
        BstInterface<String, Booking> frontDeskBst = new Bst<>();
        try {
            ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file));
            frontDeskBst = (Bst<String, Booking>) (oiStream.readObject());
            oiStream.close();
        } catch (FileNotFoundException ex) {
            System.out.println("\n" + fileName + " not found");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("\nCannot read from " + fileName);
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            System.out.println("\nClass not found");
            ex.printStackTrace();
        } finally {
            return frontDeskBst;
        }
    }
    
    public BstInterface<String, Booking> loadOrSeed() {
        File file = new File(fileName);
        if (file.exists()) {
            BstInterface<String, Booking> loaded = retrieveFromFile();
            if (loaded != null && !loaded.isEmpty()) {
                return loaded;
            }
            System.out.println("file is empty");
        }
        return seedSampleData();
    }

    private BstInterface<String, Booking> seedSampleData() { // hardcode purpose
        BstInterface<String, Booking> bst = new Bst<>();

        // Guest g1 = new Guest("1", "Ali", 60123456789L);
        // Room r1 = new Room("101", RoomType.DELUXE.name(), "1", false, 2,
        //     LocalDateTime.now().minusDays(1),
        //     LocalDateTime.now().minusDays(1),
        //     LocalDateTime.now().plusDays(2), 'R');
        // Payment p1 = new Payment("PAY001", 450.00, LocalDateTime.now(), 'C');
        // Booking b1 = new Booking("10000001", g1, r1, p1);
        // bst.insert(b1.getConfirmationNo(), b1);

        // Guest g2 = new Guest("2", "Aiman", 60123456788L);
        // Room r2 = new Room("102", RoomType.DELUXE_TWIN.name(), "1", true, 1,
        //     LocalDateTime.now().minusDays(3),
        //     LocalDateTime.now().minusDays(3),
        //     LocalDateTime.now().minusDays(1), 'R');
        // Payment p2 = new Payment("PAY002", 180.00, LocalDateTime.now(), 'C');
        // Booking b2 = new Booking("10000002", g2, r2, p2);
        // bst.insert(b2.getConfirmationNo(), b2);
        
        // Guest g3 = new Guest("3", "Mei", 60123456787L);
        // Room r3 = new Room("103", RoomType.DELUXE_TWIN.name(), "1", true, 2,
        //     LocalDateTime.now().minusDays(5),
        //     LocalDateTime.now().minusDays(5),
        //     LocalDateTime.now().minusDays(2), 'R');
        // Payment p3 = new Payment("PAY003", 180.00, LocalDateTime.now(), 'C');
        // Booking b3 = new Booking("10000003", g3, r3, p3);
        // bst.insert(b3.getConfirmationNo(), b3);
        
        // Guest g4 = new Guest("4", "Ah Kang", 60123456786L);
        // Room r4 = new Room("104", RoomType.SUPERIOR.name(), "1", false, 2,
        //     LocalDateTime.now(), LocalDateTime.now(),
        //     LocalDateTime.now().plusDays(3), 'C');
        // Payment p4 = new Payment("PAY004", 220.00, LocalDateTime.now(), 'C');
        // Booking b4 = new Booking("10000004", g4, r4, p4);
        // bst.insert(b4.getConfirmationNo(), b4);
        
        // Guest g5 = new Guest("5", "Wai Keong", 60123456785L);
        // Room r5 = new Room("105", RoomType.SUPERIOR_TWIN.name(), "1", false, 3,
        //     LocalDateTime.now(), LocalDateTime.now(),
        //     LocalDateTime.now().plusDays(1), 'I');
        // Payment p5 = new Payment("PAY005", 260.00, LocalDateTime.now(), 'C');
        // Booking b5 = new Booking("10000005", g5, r5, p5);
        // bst.insert(b5.getConfirmationNo(), b5);

        return bst;
    }
}
