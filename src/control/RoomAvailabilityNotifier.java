package control;

import entity.Room;

public final class RoomAvailabilityNotifier {

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