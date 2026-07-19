/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control;

import adt.bst.BstInterface;
import dao.FrontDeskDao;
import entity.Booking;

/**
 *
 * @author Lee Cheng Xuan
 */
public class FrontDeskControl {
    private BstInterface<String, Booking> bookingBst;
    private FrontDeskDao dao;
    
    public FrontDeskControl() {
        dao = new FrontDeskDao();
        bookingBst = dao.loadOrSeed();
        System.out.println("loaded size: " + bookingBst.size());
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
    
    public void save() {
        dao.saveToFile(bookingBst);
    }
}
