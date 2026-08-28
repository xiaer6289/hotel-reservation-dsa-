/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control.report;

import entity.Booking;
import entity.Room;
import entity.RoomType;
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
    
    public String[][] getOccupancyByRoomType(Booking[] filteredBookings, Room[] allRooms, LocalDate rangeStart, LocalDate rangeEnd) {
        RoomType[] types = RoomType.values();
        long daysInRange = java.time.temporal.ChronoUnit.DAYS.between(rangeStart, rangeEnd) + 1;
        
        String[][] rows = new String[types.length][5];
        for (int i = 0; i < types.length; i++) {
            int roomCount = 0;
            for (Room r : allRooms) {
                if (r != null && r.getRoomType().equals(types[i].name())) {
                    roomCount++;
                }
            }
            long occupiedDays = 0;
            for (Booking b : filteredBookings) {
                if (String.valueOf(b.getRoom().getRoomType()).equals(types[i].name())) {
                    occupiedDays += overlapDays(b.getRoom().getCheckInDateTime(), 
                            b.getRoom().getCheckOutDateTime(), 
                            rangeStart, 
                            rangeEnd);
                }
            }
                
            long totalRoomDays = roomCount * daysInRange;
            double rate = totalRoomDays == 0 ? 0 : (occupiedDays * 100 / totalRoomDays);
            rows[i][0] = types[i].name();
            rows[i][1] = String.valueOf(roomCount);
            rows[i][2] = String.valueOf(occupiedDays);
            rows[i][3] = String.valueOf(totalRoomDays);
            rows[i][4] = String.format("%.1f", rate);
        }
        return rows;
    }
    
    public String[][] getTopUtilizedRooms(Booking[] filteredBookings, LocalDate rangeStart, LocalDate rangeEnd, int topN) {
        String[] roomNumbers = new String[filteredBookings.length];
        String[] roomTypes = new String[filteredBookings.length];
        long[] occupiedDays = new long[filteredBookings.length];
        int uniqueCount = 0;

        for (Booking b : filteredBookings) {
            String rn = b.getRoom().getRoomNumber();
            long days = overlapDays(b.getRoom().getCheckInDateTime(), b.getRoom().getCheckOutDateTime(), rangeStart, rangeEnd);
            int idx = -1;
            for (int i = 0; i < uniqueCount; i++) {
                if (roomNumbers[i].equals(rn)) { idx = i; break; }
            }
            if (idx == -1) {
                roomNumbers[uniqueCount] = rn;
                roomTypes[uniqueCount] = String.valueOf(b.getRoom().getRoomType());
                occupiedDays[uniqueCount] = days;
                uniqueCount++;
            } else {
                occupiedDays[idx] += days;
            }
        }

        for (int i = 0; i < uniqueCount - 1; i++) {
            int maxIdx = i;
            for (int j = i + 1; j < uniqueCount; j++) {
                if (occupiedDays[j] > occupiedDays[maxIdx]) maxIdx = j;
            }
            long td = occupiedDays[i]; occupiedDays[i] = occupiedDays[maxIdx]; occupiedDays[maxIdx] = td;
            String tr = roomNumbers[i]; roomNumbers[i] = roomNumbers[maxIdx]; roomNumbers[maxIdx] = tr;
            String tt = roomTypes[i]; roomTypes[i] = roomTypes[maxIdx]; roomTypes[maxIdx] = tt;
        }

        int n = Math.min(topN, uniqueCount);
        String[][] rows = new String[n][4];
        for (int i = 0; i < n; i++) {
            rows[i][0] = String.valueOf(i + 1);
            rows[i][1] = roomNumbers[i];
            rows[i][2] = roomTypes[i];
            rows[i][3] = String.valueOf(occupiedDays[i]);
        }
        return rows;
    }

    
    public double calcAverageLengthOfStay(Booking[] filteredBookings) {
        if (filteredBookings.length == 0) return 0;
        long totalNights = 0;
        for (Booking b : filteredBookings) {
            long nights = java.time.temporal.ChronoUnit.DAYS.between(
                    b.getRoom().getCheckInDateTime().toLocalDate(),
                    b.getRoom().getCheckOutDateTime().toLocalDate());
            if (nights <= 0) nights = 1;
            totalNights += nights;
        }
        return (double) totalNights / filteredBookings.length;
    }
    
    private long overlapDays(java.time.LocalDateTime checkIn, java.time.LocalDateTime checkOut, LocalDate rangeStart, LocalDate rangeEnd) {
        if (checkIn == null || checkOut == null) return 0;
        LocalDate bookingStart = checkIn.toLocalDate();
        LocalDate bookingEnd = checkOut.toLocalDate();
        LocalDate overlapStart = bookingStart.isAfter(rangeStart) ? bookingStart : rangeStart;
        LocalDate overlapEnd = bookingEnd.isBefore(rangeEnd) ? bookingEnd : rangeEnd;
        if (overlapStart.isAfter(overlapEnd)) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
    }
}
