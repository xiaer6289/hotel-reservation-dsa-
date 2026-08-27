/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import entity.Room;
import entity.RoomStatus;
import entity.RoomType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 *
 * @author Lee Cheng Xuan
 */
public class RoomDao {
    private String fileName = "room.dat";
    private static Room[] cachedRooms;
    
    public void saveToFile(Room[] rooms) {
        // Keep every module on the same in-memory room state.  Room status is
        // shared by Registration, VIP Allocation, Front Desk and Housekeeping,
        // so the latest saved array must also become the cache used by any
        // controller created afterwards.
        cachedRooms = rooms;

        File file = new File(fileName);
        System.out.println("saving to: " + file.getAbsolutePath());
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (Room room : rooms) {
                if (room == null) continue;
                writer.println(room.getRoomNumber() + "|" + room.getRoomType() + "|" + room.getFloor() + "|"
                        + room.isAvailability() + "|" + room.getNoOfGuest() + "|"
                        + room.getBookingDate() + "|" + room.getCheckInDateTime() + "|"
                        + room.getCheckOutDateTime() + "|" + room.getStatus());
            }
        } catch (FileNotFoundException ex) {
            System.out.println("\n" + fileName + " not found");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("\ncannot save to " + fileName);
            ex.printStackTrace();
        }
    }
    
    public Room[] retrieveFromFile() {
        File file = new File(fileName);
        Room[] temp = new Room[100];
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 9) continue;
                temp[count++] = new Room(
                        parts[0], parts[1], parts[2],
                        Boolean.parseBoolean(parts[3]), Integer.parseInt(parts[4]),
                        parts[5].equals("null") ? null : LocalDateTime.parse(parts[5]),
                        parts[6].equals("null") ? null : LocalDateTime.parse(parts[6]),
                        parts[7].equals("null") ? null : LocalDateTime.parse(parts[7]),
                        parts[8].charAt(0));
            }
        } catch (FileNotFoundException ex) {
            System.out.println("\n" + fileName + " not found");
        } catch (IOException ex) {
            System.out.println("\nCannot read from " + fileName);
            ex.printStackTrace();
        }
        Room[] rooms = new Room[count];
        System.arraycopy(temp, 0, rooms, 0, count);
        cachedRooms = rooms;
        return rooms;
    }
    
    public Room[] loadOrSeed() {
        if (cachedRooms != null && cachedRooms.length > 0) {
            return cachedRooms;
        }
        
        File file = new File(fileName);
        if (file.exists()) {
            Room[] loaded = retrieveFromFile();
            if (loaded.length > 0) return loaded;
        }
        return seedSampleData();
    }
    
    public Room[] seedSampleData() {
        return new Room[] {
            new Room("101", RoomType.DELUXE.name(), "1", false, 2,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(2), 'R'),
            new Room("102", RoomType.DELUXE_TWIN.name(), "1", true, 1,
                LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(1), 'R'),
            new Room("103", RoomType.DELUXE_TWIN.name(), "1", true, 2,
                LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(2), 'R'),
            new Room("104", RoomType.SUPERIOR.name(), "1", false, 2,
                LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now().plusDays(3), 'C'),
            new Room("105", RoomType.SUPERIOR_TWIN.name(), "1", false, 3,
                LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now().plusDays(1), 'I'),
            new Room("106", RoomType.DELUXE.name(), "2", true, 2,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1), 'R'),
            new Room("107", RoomType.SUPERIOR.name(), "2", false, 2,
                LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now().plusDays(4), 'D'),
            new Room("108", RoomType.DELUXE_TWIN.name(), "2", true, 1,
                LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(4),
                LocalDateTime.now().minusDays(2), 'R'),
            new Room("109", RoomType.SUPERIOR_TWIN.name(), "3", false, 3,
                LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now().plusDays(2), 'C'),
            new Room("110", RoomType.DELUXE.name(), "3", true, 2,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1), 'R')
        };
    }
    
    public Room[] resetToDefaultReadyRooms() {
        Room[] rooms = seedSampleData();
        for (Room room : rooms) {
            if (room != null) {
                room.setRoomStatus(RoomStatus.READY);
                room.setBookingDate(null);
                room.setCheckInDateTime(null);
                room.setCheckOutDateTime(null);
            }
        }

        cachedRooms = rooms;
        saveToFile(cachedRooms);
        return cachedRooms;
    }
}