/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import entity.Guest;
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
public class GuestDao {
    private String fileName = "guest.dat";
    
    public void saveToFile(Guest[] guests) {
        File file = new File(fileName);
        System.out.println("saving to: " + file.getAbsolutePath());
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (Guest guest : guests) {
                if (guest == null) continue;
                writer.println(guest.getGuestId() + "|" + guest.getName() + "|" + guest.getPhoneNo());
            }
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
        Guest[] temp = new Guest[100];
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 3) continue;
                temp[count++] = new Guest(parts[0], parts[1], Long.parseLong(parts[2]));
            }
        } catch (FileNotFoundException ex) {
            System.out.println("\n" + fileName + " not found");
        } catch (IOException ex) {
            System.out.println("\nCannot read from " + fileName);
            ex.printStackTrace();
        }
        Guest[] guests = new Guest[count];
        System.arraycopy(temp, 0, guests, 0, count);
        return guests;
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
     * VIP sample guests are G0011-G0015 and G0017-G0021 so the
     * loyalty reports can demonstrate DIAMOND, PLATINUM and ELITE tiers.
     */
    public Guest[] seedSampleData() {
        return new Guest[] {
            new Guest("G0001", "Ali", 60123456789L),
            new Guest("G0002", "Aiman", 60123456788L),
            new Guest("G0003", "Mei", 60123456787L),
            new Guest("G0004", "Daniel Lim", 60123456786L),
            new Guest("G0005", "Chong Wei", 60123456785L),
            new Guest("G0006", "Farah", 60123456784L),
            new Guest("G0007", "Sarah", 60123456783L),
            new Guest("G0008", "Siti", 60123456782L),
            new Guest("G0009", "Marcus", 60123456781L),
            new Guest("G0010", "Ming", 60123456780L),
            new Guest("G0011", "Jason", 60189891130L),
            new Guest("G0012", "Lai", 60126648770L),
            new Guest("G0013", "Sheldon", 60189891131L),
            new Guest("G0014", "Eunice", 60126640666L),
            new Guest("G0015", "Chris", 60121112222L),
            new Guest("G0016", "Farah", 60177777789L),
            new Guest("G0017", "Nadia", 60131112221L),
            new Guest("G0018", "Kelvin", 60131112222L),
            new Guest("G0019", "Priya", 60131112223L),
            new Guest("G0020", "Hafiz", 60131112224L),
            new Guest("G0021", "Amanda", 60131112225L)
        };
    }
}