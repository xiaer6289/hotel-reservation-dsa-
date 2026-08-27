/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import entity.Booking;
import entity.Guest;
import entity.Payment;
import entity.Room;
import dao.GuestDao;
import dao.RoomDao;
import dao.PaymentDao;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author Lee Cheng Xuan
 */
public class BookingDao {
    private String fileName = "booking.dat";
    private static final String[] CONFIRMATION_NOS =
        {"10000001", "10000002", "10000003", "10000004", "10000005",
     "10000006", "10000007", "10000008", "10000009", "10000010"};
    
    public void saveToFile(Booking[] bookings) {
        File file = new File(fileName);
        System.out.println("saving to: " + file.getAbsolutePath());
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (Booking booking : bookings) {
                if (booking == null) continue;
                writer.println(booking.getConfirmationNo() + "|"
                        + booking.getGuest().getGuestId() + "|"
                        + booking.getRoom().getRoomNumber() + "|"
                        + booking.getPayment().getPaymentId());
            }
        } catch (FileNotFoundException ex) {
            System.out.println("\n" + fileName + " not found");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("\ncannot save to " + fileName);
            ex.printStackTrace();
        }
    }
    
    /**
     * No-arg convenience overload used by controllers that do not hold
     * pre-loaded Guest/Room/Payment arrays. Each dependency is loaded from its
     * own DAO file before delegating to the three-arg method.
     */
    public Booking[] retrieveFromFile() {
        Guest[]   guests   = new GuestDao().loadOrSeed();
        Room[]    rooms    = new RoomDao().loadOrSeed();
        Payment[] payments = new PaymentDao().loadOrSeed();
        return retrieveFromFile(guests, rooms, payments);
    }

    public Booking[] retrieveFromFile(Guest[] guests, Room[] rooms, Payment[] payments) {
        File file = new File(fileName);
        Booking[] temp = new Booking[100];
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 4) continue;
                Guest guest = findGuestById(guests, parts[1]);
                Room room = findRoomByNumber(rooms, parts[2]);
                Payment payment = findPaymentById(payments, parts[3]);
                if (guest == null || room == null || payment == null) continue;
                temp[count++] = new Booking(parts[0], guest, room, payment);
            }
        } catch (FileNotFoundException ex) {
            System.out.println("\n" + fileName + " not found");
        } catch (IOException ex) {
            System.out.println("\nCannot read from " + fileName);
            ex.printStackTrace();
        }
        Booking[] bookings = new Booking[count];
        System.arraycopy(temp, 0, bookings, 0, count);
        return bookings;
    }
    
    private Guest findGuestById(Guest[] guests, String id) {
        for (Guest g : guests) if (g != null && g.getGuestId().equals(id)) return g;
        return null;
    }

    private Room findRoomByNumber(Room[] rooms, String number) {
        for (Room r : rooms) if (r != null && r.getRoomNumber().equals(number)) return r;
        return null;
    }

    private Payment findPaymentById(Payment[] payments, String id) {
        for (Payment p : payments) if (p != null && p.getPaymentId().equals(id)) return p;
        return null;
    }
    
    public Booking[] loadOrSeed(Guest[] guests, Room[] rooms, Payment[] payments) {
        File file = new File(fileName);
        if (file.exists()) {
            Booking[] loaded = retrieveFromFile(guests, rooms, payments);
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