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
}
