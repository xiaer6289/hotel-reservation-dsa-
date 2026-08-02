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
import entity.Room;

/**
 *
 * @author Lee Cheng Xuan
 */
public class FrontDeskControl {
    private BstInterface<String, Booking> bookingBst;
    private BookingDao bookingDao;
    private RoomDao roomDao;
    private PaymentDao paymentDao;
    private GuestDao guestDao;
    
    public FrontDeskControl() {
        guestDao = new GuestDao();
        roomDao = new RoomDao();
        paymentDao = new PaymentDao();
        bookingDao = new BookingDao();

        Guest[] guests = guestDao.loadOrSeed();
        Room[] rooms = roomDao.loadOrSeed();
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
        final boolean[] AVAILABLE = {true}; // normal boolean throw compile error when room is full
        bookingBst.inorderTraversal(booking -> {
            if (booking.getRoom().getRoomNumber().equals(roomNumber)
                && !booking.getRoom().isAvailability()) {
                AVAILABLE[0] = false;
            }
        });
        return AVAILABLE[0];
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
        Room[] rooms = new Room[bookings.length];
        Payment[] payments = new Payment[bookings.length];
        
        for (int i = 0; i < bookings.length; i++) {
            guests[i] = bookings[i].getGuest();
            rooms[i] = bookings[i].getRoom();
            payments[i] = bookings[i].getPayment();
        }
        
        guestDao.saveToFile(guests);
        roomDao.saveToFile(rooms);
        paymentDao.saveToFile(payments);
        bookingDao.saveToFile(bookings);
        return true;
    }
}