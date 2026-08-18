package control;

import adt.linear.DoublyLinkedList;
import adt.linear.LinearADT;
import entity.Room;

/**
 * RoomAvailabilityNotifier uses a Listener/Callback pattern to notify the 
 * Front Desk module whenever a room is marked as 'Ready' by the Housekeeping 
 * module. This decouples the two modules, allowing housekeeping to focus on 
 * cleaning without needing to know the complex details of the reservation system.
 */

/**
 * @author Low Wei Shin
 */
public final class RoomAvailabilityNotifier {

    /**
     * Inner interface that defines the contract for any component wishing to
     * be notified of room changes (specifically, a room becoming ready for guests).
     */
    public interface RoomReadyListener {
        void onRoomReady(Room room);
    }

    private static RoomReadyListener listener;
    private static final LinearADT<Room> readyRoomNotifications = new DoublyLinkedList<>();

    private RoomAvailabilityNotifier() {
    }

    public static void registerListener(RoomReadyListener roomReadyListener) {
        listener = roomReadyListener;
    }

    public static void notifyRoomReady(Room room) {
        if (room == null) {
            return;
        }

        // Keep one latest notification per room. This queue is static so a
        // notification is not lost merely because FrontDeskUI is closed and
        // a new FrontDeskControl is created later from the Main Menu.
        for (int i = 0; i < readyRoomNotifications.size(); i++) {
            Room existing = readyRoomNotifications.get(i);
            if (existing != null
                    && existing.getRoomNumber() != null
                    && existing.getRoomNumber().equalsIgnoreCase(room.getRoomNumber())) {
                readyRoomNotifications.removeAt(i);
                break;
            }
        }
        readyRoomNotifications.addLast(room);

        if (listener != null) {
            listener.onRoomReady(room);
        }
    }

    public static Room[] getReadyRoomNotifications() {
        Room[] rooms = new Room[readyRoomNotifications.size()];
        final int[] index = {0};
        readyRoomNotifications.traverse(room -> rooms[index[0]++] = room);
        return rooms;
    }
}