/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control.report;

import entity.Booking;
import entity.RoomType;

/**
 *
 * @author Lee Cheng Xuan
 */
public class BillSummaryRP {
    public Booking[] generateReport(Booking[] bookings, char statusFilter) {
        Booking[] temp = new Booking[bookings.length];
        int count = 0;
        for (Booking booking : bookings) {
            if (booking == null || booking.getPayment() == null) continue;
            if (booking.getPayment().getStatus() == statusFilter) {
                temp[count] = booking;
                count++;
            }
        }
        Booking[] filtered = new Booking[count];
        System.arraycopy(temp, 0, filtered, 0, count);
        sortByAmountDesc(filtered);
        return filtered;
    }
    
    private void sortByAmountDesc(Booking[] bookings) {
        for (int i = 0; i < bookings.length - 1; i++) {
            int maxIndex = i;
            for (int j = i + 1; j < bookings.length; j++) {
                if (bookings[j].getPayment().getAmount() > bookings[maxIndex].getPayment().getAmount()) {
                    maxIndex = j;
                }
            }
            Booking temp = bookings[i];
            bookings[i] = bookings[maxIndex];
            bookings[maxIndex] = temp;
        } 
    }
    
    public double calcTotalRevenue(Booking[] bookings) {
        double total = 0;
        for (Booking booking : bookings) {
            if (booking != null)
                total += booking.getPayment().getAmount();
        }
        return total;
    }
    
    public int countByStatus(Booking[] bookings, char status) {
        int count = 0;
        for (Booking booking : bookings) {
            if (booking != null && booking.getPayment().getStatus() == status) count++;
        }
        return count;
    }
    
    public String[][] getRevenueRoomType(Booking[] filteredBookings) {
        RoomType[] types = RoomType.values();
        double grandTotal = 0;
        for (Booking b : filteredBookings) { 
            grandTotal += b.getPayment().getAmount();
        }
        
        String[][] rows = new String[types.length][5];
        for (int i = 0; i< types.length; i++) {
            int count = 0;
            double total = 0;
            for (Booking b : filteredBookings) {
                if (String.valueOf(b.getRoom().getRoomType()).equals(types[i].name())) {
                    count++;
                    total += b.getPayment().getAmount();
                }
            }
            double avg = count == 0 ? 0 : total / count;
            double pct = grandTotal == 0 ? 0 : (total / grandTotal * 100);
            rows[i][0] = types[i].name();
            rows[i][1] = String.valueOf(count);
            rows[i][2] = String.format("%.2f", total);
            rows[i][3] = String.format("%.2f", avg);
            rows[i][4] = String.format("%.1f", pct);
        }
        return rows;
    }
    
    public Booking[] getTopValueBookings(Booking[] filteredBookings, int topN) {
        int n = Math.min(topN, filteredBookings.length);
        Booking[] top = new Booking[n];
        System.arraycopy(filteredBookings, 0, top, 0, n);
        return top;
    }
    
    public double calcCollectionRate(Booking[] allBookings) {
        int completed = 0, pending = 0;
        for (Booking b : allBookings) {
            if (b == null || b.getPayment() == null) continue;
            char s = b.getPayment().getStatus();
            if (s == 'C') completed++;
            else if (s == 'P') pending++;
        }
        int denom = completed + pending;
        return denom == 0 ? 0 : (completed * 100.0 / denom);
    }

    public double calcOutstandingAmount(Booking[] allBookings) {
        double total = 0;
        for (Booking b : allBookings) {
            if (b != null && b.getPayment() != null && b.getPayment().getStatus() == 'P') {
                total += b.getPayment().getAmount();
            }
        }
        return total;
    }

    public double calcAverageTransactionValue(Booking[] filteredBookings) {
        if (filteredBookings.length == 0) return 0;
        double total = 0;
        for (Booking b : filteredBookings) total += b.getPayment().getAmount();
        return total / filteredBookings.length;
    }
    
    public double calcCancelledAmount(Booking[] allBookings) {
        double total = 0;
        for (Booking b : allBookings) {
            if (b != null && b.getPayment().getStatus() == 'C') {
                total += b.getPayment().getAmount();
            }
        }
        return total;
    }
    
    public double calcRefundedAmount(Booking[] allBookings) {
        double total = 0;
        for (Booking b : allBookings) {
            if (b != null && b.getPayment().getStatus() == 'R') {
                total += b.getPayment().getAmount();
            }
        }
        return total;
    }
}
