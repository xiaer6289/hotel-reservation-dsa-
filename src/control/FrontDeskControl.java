/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control;

import adt.bst.Bst;
import adt.bst.BstInterface;
import dao.BookingDao;
import dao.GuestDao;
import dao.PaymentDao;
import dao.RoomDao;
import entity.Booking;
import entity.Guest;
import entity.Payment;
import entity.RegistrationStatus;
import entity.Room;
import entity.RoomStatus;
import entity.TaskLogEntry;
import entity.WalkInRegistration;

/**
 *
 * @author Lee Cheng Xuan
 */
public class FrontDeskControl implements RoomAvailabilityNotifier.RoomReadyListener {

    private BstInterface<String, Booking> bookingBst;
    private BookingDao bookingDao;
    private RoomDao roomDao;
    private PaymentDao paymentDao;
    private GuestDao guestDao;
    private Room[] rooms;
    private RegistrationController registrationController;
    private final HousekeepingController housekeepingController;
    
    public FrontDeskControl() {
        guestDao = new GuestDao();
        roomDao = new RoomDao();
        paymentDao = new PaymentDao();
        bookingDao = new BookingDao();
        registrationController = new RegistrationController();
        housekeepingController = new HousekeepingController();

        RoomAvailabilityNotifier.registerListener(this);

        Guest[] guests = guestDao.loadOrSeed();
        rooms = roomDao.loadOrSeed();
        Payment[] payments = paymentDao.loadOrSeed();
        Booking[] bookings = bookingDao.loadOrSeed(guests, rooms, payments);

        bookingBst = new Bst<>();
        for (Booking booking : bookings) {
            if (booking != null) {
            bookingBst.insert(booking.getConfirmationNo(), booking);
            }
        }
    }
    
    public Booking searchBookingByConfirmationNo(String confirmationNo) {
        return bookingBst.search(confirmationNo);
    }
    
    public boolean isRoomAvailable(String roomNumber) {
        Room room = findRoomByNumber(roomNumber);
        return room != null && room.isAssignable();
    }

    public String getRoomAvailabilityMessage(String roomNumber) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null) {
            return "Room not found.";
        }

        switch (room.getRoomStatus()) {
            case AVAILABLE:
            case READY:
                return "Room " + roomNumber + " is ready for assignment.";
            case OCCUPIED:
                return "Room " + roomNumber + " is occupied.";
            case DIRTY:
            case CLEANING_IN_PROGRESS:
            case INSPECTED:
                return "Room " + roomNumber + " is not ready yet and still needs cleaning.";
            default:
                return "Room " + roomNumber + " is not available.";
        }
    }

    public Room[] getAssignableRooms() {
        refreshRooms();
        int count = 0;

        for (Room room : rooms) {
            if (room != null && room.isAssignable()) {
                count++;
            }
        }

        Room[] assignableRooms = new Room[count];
        int index = 0;
        for (Room room : rooms) {
            if (room != null && room.isAssignable()) {
                assignableRooms[index++] = room;
            }
        }

        return assignableRooms;
    }

    public TaskLogEntry processCheckout(String confirmationNo, String staffId) {
        return processCheckout(confirmationNo, staffId, null);
    }

    public TaskLogEntry processCheckout(String confirmationNo, String staffId, String remarks) {
        if (confirmationNo == null || confirmationNo.isBlank()) {
            return null;
        }

        Booking booking = searchBookingByConfirmationNo(confirmationNo.trim());
        if (booking == null) {
            return null;
        }

        if (booking.getRoom() == null || booking.getRoom().getRoomNumber() == null) {
            return null;
        }

        /*
         * A historical Booking must not be allowed to check out again. A
         * guest is considered currently checked in only when the matching
         * WalkInRegistration is still CHECKED_IN and the live room record is
         * still OCCUPIED for the same check-in time.
         */
        WalkInRegistration activeRegistration
                = findCheckedInRegistrationForBooking(booking);

        if (activeRegistration == null) {
            return null;
        }

        /*
         * Booking.room is a serialized snapshot of the room at booking time.
         * Never use that snapshot as the current room-state object. The
         * Housekeeping controller updates the shared RoomDao room instead.
         */
        String roomNumber = booking.getRoom().getRoomNumber();
        Room currentRoom = findRoomByNumber(roomNumber);
        if (currentRoom == null) {
            return null;
        }

        boolean sameLiveStay
                = currentRoom.getRoomStatus() == RoomStatus.OCCUPIED
                && currentRoom.getCheckInDateTime() != null
                && activeRegistration.getCheckInDateTime() != null
                && currentRoom.getCheckInDateTime()
                        .equals(activeRegistration.getCheckInDateTime());

        if (!sameLiveStay) {
            return null;
        }

        TaskLogEntry task = housekeepingController.createCheckoutTask(roomNumber, staffId, remarks);
        if (task == null) {
            return null;
        }

        refreshRooms();
        
        if (booking.getGuest() != null) {
            registrationController.markGuestCheckedOut(
                booking.getGuest().getGuestId());
        }

        return task;
    }

    /**
     * Returns only bookings that represent guests who are currently staying
     * in the hotel. Historical booking room snapshots are deliberately not
     * used as the source of truth for current occupancy.
     */
    public Booking[] getCurrentCheckedInBookings() {
        Booking[] allBookings = sortBooking();
        refreshRooms();

        Booking[] matches = new Booking[allBookings.length];
        int count = 0;

        for (Booking booking : allBookings) {
            if (booking == null
                    || booking.getRoom() == null
                    || booking.getRoom().getRoomNumber() == null) {
                continue;
            }

            WalkInRegistration registration
                    = findCheckedInRegistrationForBooking(booking);

            if (registration == null) {
                continue;
            }

            Room currentRoom = findRoomByNumberWithoutRefresh(
                    booking.getRoom().getRoomNumber());

            if (currentRoom == null
                    || currentRoom.getRoomStatus() != RoomStatus.OCCUPIED
                    || currentRoom.getCheckInDateTime() == null
                    || registration.getCheckInDateTime() == null
                    || !currentRoom.getCheckInDateTime()
                            .equals(registration.getCheckInDateTime())) {
                continue;
            }

            matches[count++] = booking;
        }

        Booking[] currentBookings = new Booking[count];
        System.arraycopy(matches, 0, currentBookings, 0, count);
        return currentBookings;
    }

    /**
     * Finds the CHECKED_IN registration that belongs to one Booking by Guest
     * ID and the actual check-in timestamp. This also lets FrontDeskUI show
     * the Registration ID without storing duplicate data in Booking.
     */
    public WalkInRegistration getCheckedInRegistrationForBooking(Booking booking) {
        return findCheckedInRegistrationForBooking(booking);
    }

    private WalkInRegistration findCheckedInRegistrationForBooking(Booking booking) {
        if (booking == null
                || booking.getGuest() == null
                || booking.getGuest().getGuestId() == null
                || booking.getRoom() == null
                || booking.getRoom().getCheckInDateTime() == null) {
            return null;
        }

        String guestId = booking.getGuest().getGuestId();

        for (int i = registrationController.getTotalRegistrationCount() - 1;
                i >= 0; i--) {

            WalkInRegistration registration
                    = registrationController.getRecordAt(i);

            if (registration == null
                    || registration.getGuest() == null
                    || registration.getGuest().getGuestId() == null
                    || registration.getCheckInDateTime() == null
                    || registration.getStatus() != RegistrationStatus.CHECKED_IN) {
                continue;
            }

            boolean sameGuest = registration.getGuest().getGuestId()
                    .equalsIgnoreCase(guestId);

            boolean sameCheckInTime = registration.getCheckInDateTime()
                    .equals(booking.getRoom().getCheckInDateTime());

            if (sameGuest && sameCheckInTime) {
                return registration;
            }
        }

        return null;
    }
    
    public Booking[] sortBooking() {
        Booking[] result = new Booking[bookingBst.size()];
        final int[] INDEX = {0};
        bookingBst.inorderTraversal(booking -> {
            result[INDEX[0]] = booking;
            INDEX[0]++;
        });
        return result;
    }
    
    public boolean save() {
        Booking[] bookings = sortBooking();
        refreshRooms();
        Room[] roomsToSave = rooms;
        Payment[] payments = new Payment[bookings.length];
        
        for (int i = 0; i < bookings.length; i++) {
            payments[i] = bookings[i].getPayment();
        }
        
        roomDao.saveToFile(roomsToSave);
        paymentDao.saveToFile(payments);
        bookingDao.saveToFile(bookings);
        return true;
    }

    @Override
    public void onRoomReady(Room room) {
        if (room == null) {
            return;
        }

        System.out.println("[Front Desk Notification] Room " + room.getRoomNumber()
                + " is READY and can be assigned to a new guest.");
    }

    public Room[] getNotifiedReadyRooms() {
        refreshRooms();
        Room[] notifications = RoomAvailabilityNotifier.getReadyRoomNotifications();
        Room[] temp = new Room[notifications.length];
        int count = 0;

        for (Room notification : notifications) {
            if (notification == null || notification.getRoomNumber() == null) {
                continue;
            }

            Room currentRoom = findRoomByNumberWithoutRefresh(notification.getRoomNumber());
            if (currentRoom != null && currentRoom.isAssignable()) {
                temp[count++] = currentRoom;
            }
        }

        Room[] readyRooms = new Room[count];
        System.arraycopy(temp, 0, readyRooms, 0, count);
        return readyRooms;
    }

    public Room[] getCurrentRooms() {
        refreshRooms();
        return rooms;
    }

    private Room findRoomByNumber(String roomNumber) {
        refreshRooms();
        return findRoomByNumberWithoutRefresh(roomNumber);
    }

    private Room findRoomByNumberWithoutRefresh(String roomNumber) {
        if (roomNumber == null) {
            return null;
        }

        for (Room room : rooms) {
            if (room != null && room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return room;
            }
        }
        return null;
    }

    private void refreshRooms() {
        rooms = roomDao.loadOrSeed();
    }
}