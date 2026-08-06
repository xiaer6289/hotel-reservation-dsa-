package control;

import entity.Room;
/**
 * RoomAvailabilityNotifier uses a Listener/Callback pattern to notify the 
 * Front Desk module whenever a room is marked as 'Ready' by the Housekeeping 
 * module. This decouples the two modules, allowing housekeeping to focus on 
 * cleaning without needing to know the complex details of the reservation system.
 */

/**
 *
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

    private RoomAvailabilityNotifier() {
    }

    public static void registerListener(RoomReadyListener roomReadyListener) {
        listener = roomReadyListener;
    }

    public static void notifyRoomReady(Room room) {
        if (listener != null) {
            listener.onRoomReady(room);
        }
    }
}