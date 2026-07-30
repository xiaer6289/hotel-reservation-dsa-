package control;

import adt.heap.MaxHeap;
import adt.heap.PriorityQueueADT;
import entity.Member;
import entity.Room;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Low Enn Toong
 */

public class VipPriorityController {

    private PriorityQueueADT<Member> priorityQueue;
    private List<Room> rooms;

    public VipPriorityController() {
        priorityQueue = new MaxHeap<>();
        rooms = new ArrayList<>();
        loadSampleData();
    }

    private void loadSampleData() {
        // Sample VIP / Loyalty members
        priorityQueue.enqueue(new Member("M001", "Alice Tan", "Platinum", "Suite"));
        priorityQueue.enqueue(new Member("M002", "Bob Lee", "Gold", "Deluxe"));
        priorityQueue.enqueue(new Member("M003", "Carol Wong", "Silver", "Standard"));
        priorityQueue.enqueue(new Member("M004", "David Lim", "Platinum", "Suite"));
        priorityQueue.enqueue(new Member("M005", "Emily Ng", "Gold", "Deluxe"));

        // Sample Rooms (using your existing Room constructor)
        // status: 'A' = Available, 'O' = Occupied, 'D' = Dirty, 'R' = Ready
        rooms.add(new Room("101", "Standard", "1", true, 0, null, null, null, 'A'));
        rooms.add(new Room("205", "Deluxe", "2", true, 0, null, null, null, 'A'));
        rooms.add(new Room("308", "Suite", "3", true, 0, null, null, null, 'A'));
        rooms.add(new Room("412", "Deluxe", "4", true, 0, null, null, null, 'A'));
        rooms.add(new Room("501", "Suite", "5", true, 0, null, null, null, 'A'));
        rooms.add(new Room("102", "Standard", "1", true, 0, null, null, null, 'A'));
    }

    public void addVipMember(String id, String name, String tier, String preference) {
        Member member = new Member(id, name, tier, preference);
        priorityQueue.enqueue(member);
        System.out.println("Member added to priority queue: " + member.getName());
    }

    public void allocateNextVipRoom() {
        if (priorityQueue.isEmpty()) {
            System.out.println("No VIP members waiting in the queue.");
            return;
        }

        Member nextVip = priorityQueue.dequeue();
        Room allocatedRoom = findAvailableRoom(nextVip.getRoomPreference());

        if (allocatedRoom != null) {
            allocatedRoom.setAvailability(false);
            allocatedRoom.setStatus('O'); // Occupied
            allocatedRoom.setCheckInDateTime(LocalDateTime.now());

            System.out.println("\n========== ROOM ALLOCATED SUCCESSFULLY ==========");
            System.out.println("Guest Name     : " + nextVip.getName());
            System.out.println("Membership Tier: " + nextVip.getTier());
            System.out.println("Room Number    : " + allocatedRoom.getRoomNumber());
            System.out.println("Room Type      : " + allocatedRoom.getRoomType());
            System.out.println("Status         : Occupied");
            System.out.println("=================================================");
        } else {
            System.out.println("No available room matching preference: " + nextVip.getRoomPreference());
            // Put the member back into the queue
            priorityQueue.enqueue(nextVip);
        }
    }

    private Room findAvailableRoom(String preferredType) {
        // First try preferred type
        for (Room r : rooms) {
            if (r.isAvailability() && r.getRoomType().equalsIgnoreCase(preferredType)) {
                return r;
            }
        }
        // Fallback: any available room
        for (Room r : rooms) {
            if (r.isAvailability()) {
                return r;
            }
        }
        return null;
    }

    public void displayPriorityQueue() {
        priorityQueue.display();
    }

    public void displayRooms() {
        System.out.println("\n=== CURRENT ROOM STATUS ===");
        System.out.printf("%-8s %-12s %-8s %-12s%n", "Room", "Type", "Floor", "Status");
        System.out.println("----------------------------------------");
        
        for (Room r : rooms) {
            String statusText;
            
            switch (r.getStatus()) {
                case 'A':
                    statusText = "Available";
                    break;
                case 'O':
                    statusText = "Occupied";
                    break;
                case 'D':
                    statusText = "Dirty";
                    break;
                case 'R':
                    statusText = "Ready";
                    break;
                default:
                    statusText = "Unknown";
            }
            
            System.out.printf("%-8s %-12s %-8s %-12s%n",
                    r.getRoomNumber(), r.getRoomType(), r.getFloor(), statusText);
        }
    }

    public void markRoomDirty(String roomNumber) {
        for (Room r : rooms) {
            if (r.getRoomNumber().equals(roomNumber)) {
                r.setStatus('D');
                r.setAvailability(false);
                System.out.println("Room " + roomNumber + " marked as Dirty.");
                return;
            }
        }
        System.out.println("Room not found.");
    }

    public void markRoomReady(String roomNumber) {
        for (Room r : rooms) {
            if (r.getRoomNumber().equals(roomNumber)) {
                r.setStatus('A');
                r.setAvailability(true);
                System.out.println("Room " + roomNumber + " is now Available / Ready.");
                return;
            }
        }
        System.out.println("Room not found.");
    }
}