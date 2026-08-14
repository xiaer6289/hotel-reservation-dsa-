/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control;

import adt.bst.Bst;
import adt.bst.BstInterface;
import adt.linear.DoublyLinkedList;
import adt.linear.LinearADT;
import dao.BookingDao;
import dao.GuestDao;
import dao.PaymentDao;
import dao.RoomDao;
import entity.Booking;
import entity.Guest;
import entity.Payment;
import entity.Room;
import entity.RoomStatus;
import entity.TaskLogEntry;

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
    private final LinearADT<Room> readyRoomInbox;
    
    public FrontDeskControl() {
        guestDao = new GuestDao();
        roomDao = new RoomDao();
        paymentDao = new PaymentDao();
        bookingDao = new BookingDao();
        registrationController = new RegistrationController();
        housekeepingController = new HousekeepingController();
        readyRoomInbox = new DoublyLinkedList<>();

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

        Room room = booking.getRoom();
        room.setRoomStatus(RoomStatus.DIRTY);
        roomDao.saveToFile(rooms);

        TaskLogEntry task = housekeepingController.createCheckoutTask(room.getRoomNumber(), staffId, remarks);
        if (task == null) {
            return null;
        }
        
        if (booking.getGuest() != null) {
            registrationController.markGuestCheckedOut(
                booking.getGuest().getGuestId());
        }

        return task;
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
        Guest[] guests = new Guest[bookings.length];
        Room[] roomsToSave = rooms;
        Payment[] payments = new Payment[bookings.length];
        
        for (int i = 0; i < bookings.length; i++) {
            guests[i] = bookings[i].getGuest();
            payments[i] = bookings[i].getPayment();
        }
        
        guestDao.saveToFile(guests);
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

        readyRoomInbox.addLast(room);
        System.out.println("[Front Desk Notification] Room " + room.getRoomNumber()
                + " is READY and can be assigned to a new guest.");
    }

    public Room[] getNotifiedReadyRooms() {
        Room[] readyRooms = new Room[readyRoomInbox.size()];
        for (int i = 0; i < readyRoomInbox.size(); i++) {
            readyRooms[i] = readyRoomInbox.get(i);
        }
        return readyRooms;
    }

    private Room findRoomByNumber(String roomNumber) {
        for (Room room : rooms) {
            if (room != null && room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return room;
            }
        }
        return null;
    }
}