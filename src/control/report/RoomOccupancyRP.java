/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control.report;

import entity.Booking;
import java.time.LocalDate;

/**
 *
 * @author Lee Cheng Xuan
 */
public class RoomOccupancyRP {
    public Booking[] generateReport(Booking[] bookings, String roomTypeFilter, LocalDate rangeStart, LocalDate rangeEnd) {
        Booking[] temp = new Booking[bookings.length];
        int count = 0;
        for (Booking booking : bookings) {
            if (booking == null || booking.getRoom() == null) continue;
            boolean matchesType = roomTypeFilter == null || booking.getRoom().getRoomType().equals(roomTypeFilter);
            boolean matchesRange = fallsWithinRange(booking, rangeStart, rangeEnd);
            if (matchesType && matchesRange) {
                temp[count++] = booking;
            }
        }
        Booking[] filtered = new Booking[count];
        System.arraycopy(temp, 0, filtered, 0, count);
        sortByRoomNumber(filtered);
        return filtered;
    }
    
    private boolean fallsWithinRange(Booking booking, LocalDate rangeStart, LocalDate rangeEnd) {
        if (booking.getRoom().getCheckInDateTime() == null) return false;
        LocalDate checkInDate = booking.getRoom().getCheckInDateTime().toLocalDate();
        return !checkInDate.isBefore(rangeStart) && !checkInDate.isAfter(rangeEnd);
    }
    
    private void sortByRoomNumber(Booking[] bookings) {
        for (int i = 0; i < bookings.length - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < bookings.length; j++) {
                if (bookings[j].getRoom().getRoomNumber().compareTo(bookings[minIndex].getRoom().getRoomNumber()) < 0) {
                    minIndex = j;
                }
            }
            Booking temp = bookings[i];
            bookings[i] = bookings[minIndex];
            bookings[minIndex] = temp;
        }
    }
    
    public int countAvailable(Booking[] bookings) {
        int count = 0;
        for (Booking booking : bookings) {
            if (booking != null && booking.getRoom().isAvailability()) count++;
        }
        return count;
    }
    
    public int countOccupied(Booking[] bookings) {
        int count = 0;
        for (Booking booking : bookings) {
            if (booking != null && !booking.getRoom().isAvailability()) count++;
        }
        return count;
    }
}
