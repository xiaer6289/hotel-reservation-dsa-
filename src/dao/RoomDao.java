/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import entity.Room;
import entity.RoomStatus;
import entity.RoomType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;

/**
 *
 * @author Lee Cheng Xuan
 */
public class RoomDao {
    private String fileName = "room.dat";
    private static Room[] cachedRooms;
    
    public void saveToFile(Room[] room) {
        // Keep every module on the same in-memory room state.  Room status is
        // shared by Registration, VIP Allocation, Front Desk and Housekeeping,
        // so the latest saved array must also become the cache used by any
        // controller created afterwards.
        cachedRooms = room;

        File file = new File(fileName);
        System.out.println("saving to: " + file.getAbsolutePath());
        try {
            ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(file));
            ooStream.writeObject(room);
            ooStream.close();
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
        Room[] rooms = new Room[0];
        try {
            ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file));
            rooms = (Room[]) (oiStream.readObject());
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
            cachedRooms = rooms;
            return rooms;
        }
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
                LocalDateTime.now().plusDays(1), 'I')
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