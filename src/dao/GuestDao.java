/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import entity.Guest;
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
public class GuestDao {
    private String fileName = "guest.dat";
    
    public void saveToFile(Guest[] guests) {
        File file = new File(fileName);
        System.out.println("saving to: " + file.getAbsolutePath());
        try {
            ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(file));
            ooStream.writeObject(guests);
            ooStream.close();
        } catch (FileNotFoundException ex) {
            System.out.println("\n" + fileName + " not found");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("\ncannot save to " + fileName);
            ex.printStackTrace();
        }
    }
    
    public Guest[] retrieveFromFile() {
        File file = new File(fileName);
        Guest[] guests = new Guest[0];
        try {
            ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file));
            guests = (Guest[]) (oiStream.readObject());
            oiStream.close();
        } catch (FileNotFoundException ex) {
            System.out.println("\n" + fileName + " not found");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("\nCannot read from " + fileName);
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            System.out.println("\nclass not found");
            ex.printStackTrace();
        } finally {
            return guests;
        }
    }
    
    public Guest[] loadOrSeed() {
        File file = new File(fileName);
        if (file.exists()) {
            Guest[] loaded = retrieveFromFile();
            if (loaded.length > 0) {
                return loaded;
            }
        }

        // Persist the seeded guest master immediately so Registration, VIP,
        // Front Desk and any future bookings all read the same guest identity
        // instead of creating separate in-memory sample copies.
        Guest[] seeded = seedSampleData();
        saveToFile(seeded);
        return seeded;
    }
    
    /**
     * Demo guest master data shared by all modules.
     * G0001-G0003 intentionally match LoyaltyProfileDao sample profiles:
     * G0001 Ali   -> DIAMOND (10 completed stays)
     * G0002 Aiman -> PLATINUM (6 completed stays)
     * G0003 Mei   -> ELITE (3 completed stays)
     */
    public Guest[] seedSampleData() {
        return new Guest[] {
            new Guest("G0001", "Ali", 60123456789L),
            new Guest("G0002", "Aiman", 60123456788L),
            new Guest("G0003", "Mei", 60123456787L),
            new Guest("G0004", "Daniel Lim", 60123456786L)
        };
    }
}