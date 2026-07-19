/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boundary;

import control.FrontDeskControl;
import entity.Booking;
import java.util.Scanner;
import utility.Utility;

/**
 *
 * @author Lee Cheng Xuan
 */
public class FrontDeskUI {
    Scanner scanner = new Scanner(System.in);
    private FrontDeskControl control = new FrontDeskControl();
    
    public void run() {
        int choice;
        do {
            displayMenu();
            choice = getMenuChoice();
            switch (choice) {
                case 1: 
                    searchBooking();
                    break;
                case 2: 
                    viewAllBooking();
                    break;
                case 3: 
                    checkRoomAvailability();
                    break;
                case 4: 
                    control.save();
                    Utility.printSuccess("Data saved.");
                    Utility.pauseScreen();
                    Utility.clearScreen();
                default: 
                    Utility.printError("Invalid option, try again");
            }
            if (choice != 4) Utility.pauseScreen();
        } while (choice != 4);
    }
    
    private void displayMenu() {
        Utility.clearScreen();
        System.out.println("--- FRONT DESK SERVICE ---");
        System.out.println("1. Search Booking");
        System.out.println("2. View All Booking");
        System.out.println("3. Check Room Availability");
        System.out.println("4. Save and Exit");
        System.out.print("Enter choice: ");
    }
    
    private int getMenuChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
    
    private void searchBooking() {
        System.out.print("Enter Confirmation Number (8 digits): ");
        String confirmationNo = scanner.nextLine().toLowerCase().trim();
        
        if (!Utility.isValidConfirmationNo(confirmationNo)) {
            Utility.printError("Invalid Confirmation number format");
            return;
        }
        
        Booking booking = control.searchBookingByConfirmationNo(confirmationNo);
        if (booking == null) {
            Utility.printError("No booking found for " + confirmationNo);
        } else {
            displayAllBooking(booking);
        }
    }
    
    private void viewAllBooking() {
        System.out.println("--- Bookings (sorted by Confirmation No) ---");
        Booking[] bookings = control.sortBooking();
        for (Booking booking : bookings) {
            System.out.println(booking.getConfirmationNo() + " | "
            + booking.getGuest() + " | Room " 
            + booking.getRoom().getRoomNumber());
        }
    }
    
    private void checkRoomAvailability() {
        System.out.print("Enter Room Number: ");
        String roomNo = scanner.nextLine().trim();
        if (control.isRoomAvailable(roomNo)) {
            Utility.printSuccess("Room " + roomNo + " is available.");
        } else {
            Utility.printError("Room " + roomNo + " is not available.");
        }
    }
    
    private void displayAllBooking(Booking booking) {
        System.out.println("Confirmation No: " + booking.getConfirmationNo());
        System.out.println("Guest Name: " + booking.getGuest().getName());
        System.out.println("Phone Number: " + booking.getGuest().getPhoneNo());
        System.out.println("Room Number: " + booking.getRoom().getRoomNumber());
        System.out.println("Room Type: " + booking.getRoom().getRoomType());
        System.out.println("Payment Amount: " + booking.getPayment().getAmount());
        System.out.println("Payment Status: " + booking.getPayment().getStatus());
    }
}
