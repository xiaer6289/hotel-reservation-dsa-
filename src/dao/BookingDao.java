/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import entity.Booking;
import entity.Guest;
import entity.Payment;
import entity.Room;
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
public class BookingDao {
    private String fileName = "booking.dat";
    private static final String[] CONFIRMATION_NOS =
        {"10000001", "10000002", "10000003", "10000004", "10000005"};
    
    public void saveToFile(Booking[] bookings) {
        File file = new File(fileName);
        System.out.println("saving to: " + file.getAbsolutePath());
        try {
            ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(file));
            ooStream.writeObject(bookings);
            ooStream.close();
        } catch (FileNotFoundException ex) {
            System.out.println("\n" + fileName + " not found");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("\ncannot save to " + fileName);
            ex.printStackTrace();
        }
    }
    
    public Booking[] retrieveFromFile() {
        File file = new File(fileName);
        Booking[] bookings = new Booking[0];
        try {
            ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file));
            bookings = (Booking[]) (oiStream.readObject());
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
            return bookings;
        }
    }
    
    public Booking[] loadOrSeed(Guest[] guests, Room[] rooms, Payment[] payments) {
        File file = new File(fileName);
        if (file.exists()) {
            Booking[] loaded = retrieveFromFile();
            if (loaded.length > 0) return loaded;
        }
        return seedSampleData(guests, rooms, payments);
    }
    
    public Booking[] seedSampleData(Guest[] guests, Room[] rooms, Payment[] payments) {
        int count = Math.min(CONFIRMATION_NOS.length, Math.min(guests.length, Math.min(rooms.length, payments.length)));
        Booking[] bookings = new Booking[count];
        for (int i = 0; i < count; i++) {
            bookings[i] = new Booking(CONFIRMATION_NOS[i], guests[i], rooms[i], payments[i]);
        }
        return bookings;
    }
}