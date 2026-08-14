/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control.report;

import entity.Booking;

/**
 *
 * @author Lee Cheng Xuan
 */
public class RoomOccupancyRP {
    public Booking[] generateReport(Booking[] bookings, String roomTypeFilter, boolean availabilityFilter) {
        Booking[] temp = new Booking[bookings.length];
        int count = 0;
        for (Booking booking : bookings) {
            if (booking == null) continue;
            boolean matchType = roomTypeFilter == null || booking.getRoom().getRoomType().equals(roomTypeFilter);
            boolean matchAvailability = booking.getRoom().isAvailability() == availabilityFilter;
            if (matchType && matchAvailability) {
                temp[count] = booking;
                count++;
            }
        }
        Booking[] filtered = new Booking[count];
        System.arraycopy(temp, 0, filtered, 0, count);
        sortByRoomNumber(filtered);
        return filtered;
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
}
