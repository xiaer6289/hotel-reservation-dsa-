/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boundary;

import control.FrontDeskControl;
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
                    processCheckout();
                    break;
                case 5:
                    roomOccupancyRP();
                    break;
                case 6:
                    billingSummaryRP();
                    break;
                case 7:
                    viewReadyRoomNotifications();
                    break;
                case 8: 
                    control.save();
                    Utility.printSuccess("Data saved.");
                    Utility.pauseScreen();
                    Utility.clearScreen();
                    break;
                default: 
                    Utility.printError("Invalid option, please try again...");
            }
            if (choice != 8) Utility.pauseScreen();
        } while (choice != 8);
    }
    
    private void displayMenu() {
        Utility.clearScreen();
        System.out.println("--- FRONT DESK SERVICE ---");
        System.out.println("1. Search Booking");
        System.out.println("2. View All Booking");
        System.out.println("3. Check Room Availability");
        System.out.println("4. Process Guest Check Out");
        System.out.println("5. Room Occupancy Report");
        System.out.println("6. Billing Summary Report");
        System.out.println("7. View Ready Room Notifications");
        System.out.println("8. Save and Exit");
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
            Utility.printError("Invalid Confirmation number format. Please try again...");
            return;
        }
        
        String[] booking = control.getBookingDisplayData(confirmationNo);
        if (booking == null) {
            Utility.printError("No booking found for " + confirmationNo);
        } else {
            displayAllBooking(booking);
        }
    }
    
    private void viewAllBooking() {
        System.out.println("--- Bookings (sorted by Confirmation No) ---");
        String[][] bookings = control.getAllBookingDisplayRows();
        for (String[] booking : bookings) {
            System.out.println(booking[0] + " | "
            + booking[1] + " | Room " 
            + booking[2]);
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
    
    private void processCheckout() {
        String[][] checkedInBookings = control.getCurrentCheckedInDisplayRows();

        System.out.println("\n===== PROCESS GUEST CHECK OUT =====");

        if (checkedInBookings.length == 0) {
            Utility.printError("No guests are currently checked in.");
            return;
        }

        System.out.println("\nCURRENT CHECKED-IN GUESTS");
        System.out.printf("%-6s %-10s %-20s %-10s %-18s %-18s%n",
                "Room", "Guest ID", "Guest Name", "Reg ID",
                "Confirmation No.", "Expected Check-Out");
        System.out.println("--------------------------------------------------------------------------------------");

        for (String[] currentBooking : checkedInBookings) {
            System.out.printf("%-6s %-10s %-20s %-10s %-18s %-18s%n",
                    currentBooking[0],
                    currentBooking[1],
                    currentBooking[2],
                    currentBooking[3],
                    currentBooking[4],
                    currentBooking[5]);
        }

        String[] selectedStay = null;

        while (selectedStay == null) {
            System.out.print("\nEnter Room Number to check out (0 = Cancel): ");
            String roomNumber = scanner.nextLine().trim();

            if (roomNumber.equals("0")) {
                System.out.println("Check-out cancelled.");
                return;
            }

            selectedStay = control.getCurrentCheckedInStayDisplayData(roomNumber);

            if (selectedStay == null) {
                Utility.printError("Please enter a room number from the checked-in list above.");
            }
        }

        System.out.println("\nSELECTED CHECKED-IN STAY");
        System.out.println("Room Number         : " + selectedStay[0]);
        System.out.println("Guest ID            : " + selectedStay[1]);
        System.out.println("Guest Name          : " + selectedStay[2]);
        System.out.println("Phone Number        : " + selectedStay[3]);
        System.out.println("Registration ID     : " + selectedStay[4]);
        System.out.println("Confirmation Number : " + selectedStay[5]);
        System.out.println("Check-In Time       : " + selectedStay[6]);
        System.out.println("Expected Check-Out  : " + selectedStay[7]);

        String confirmationNo;

        while (true) {
            System.out.print("\nEnter Confirmation Number shown above (8 digits, 0 = Cancel): ");
            confirmationNo = scanner.nextLine().trim();

            if (confirmationNo.equals("0")) {
                System.out.println("Check-out cancelled.");
                return;
            }

            if (!Utility.isValidConfirmationNo(confirmationNo)) {
                Utility.printError("Invalid Confirmation number format. Please enter 8 digits.");
                continue;
            }

            if (!selectedStay[5].equals(confirmationNo)) {
                Utility.printError("Confirmation number does not match the selected room/guest.");
                continue;
            }

            break;
        }
        
        String staff = selectStaff(scanner);
        if (staff == null) {
            return;
        }
        System.out.println("Select checkout reason: 1. Standard  2. Late Check-Out  3. Special Request");
        System.out.print("Enter choice: ");
        String reasonChoice = scanner.nextLine().trim();
        String remarks = null;

        switch (reasonChoice) {
            case "2":
                remarks = "Late check-out";
                break;
            case "3":
                System.out.print("Enter special request details: ");
                remarks = "Special request - " + scanner.nextLine().trim();
                break;
            default:
                remarks = null;
                break;
        }

        String taskId = control.processCheckoutAndGetTaskId(confirmationNo, staff, remarks);
        if (taskId == null) {
            Utility.printError("Unable to process check-out.");
            return;
        }

        Utility.printSuccess("Check-out completed. Housekeeping task created: " + taskId);
    }

    private void viewReadyRoomNotifications() {
        String[][] rooms = control.getReadyRoomNotificationDisplayRows();
        if (rooms.length == 0) {
            Utility.printError("No ready-room notifications yet.");
            return;
        }

        System.out.println("--- READY ROOMS FOR FRONT DESK ---");
        for (String[] room : rooms) {
            if (room != null) {
                System.out.println("Room " + room[0] + " | " + room[1]);
            }
        }
    }
    
    private void displayAllBooking(String[] booking) {
        System.out.println("Confirmation No: " + booking[0]);
        System.out.println("Guest Name: " + booking[1]);
        System.out.println("Phone Number: " + booking[2]);
        System.out.println("Room Number: " + booking[3]);
        System.out.println("Room Type: " + booking[4]);
        System.out.println("Payment Amount: " + booking[5]);
        System.out.println("Payment Status: " + booking[6]);
    }
    
    private void roomOccupancyRP() {
        System.out.println("--- ROOM OCCUPANCY REPORT ---");
        System.out.println("1. Deluxe  2. Deluxe Twin  3. Superior  4. Superior Twin  5. All");
        System.out.print("Filter by Room Type: ");
        int type = getMenuChoice();
        String roomTypeFilter;
        switch (type) {
            case 1:
                roomTypeFilter = "DELUXE";
                break;
            case 2:
                roomTypeFilter = "DELUXE_TWIN";
                break;
            case 3:
                roomTypeFilter = "SUPERIOR";
                break;
            case 4:
                roomTypeFilter = "SUPERIOR_TWIN";
                break;
            case 5:
                roomTypeFilter = null;
                break;
            default:
                Utility.printError("Invalid Option. Please try again...");
                return;
        }
        
        System.out.print("Show Available (A) or Occupied (O) rooms?");
        String status = scanner.nextLine().trim().toUpperCase();
        boolean availabilityFilter = status.equals("A");
        
        String[][] report = control.generateRoomOccupancyReportDisplay(
                roomTypeFilter,
                availabilityFilter);
        
        System.out.println("\nFilter: Room Type = " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) 
        + " | Status = " + (availabilityFilter ? "AVAILABLE" : "OCCUPIED"));
        System.out.printf("\n%-8s %-14s %-6s %15s %-17s %-17s%n", 
                "Room No", "Type", "Floor", "Guest Name", "Check-In", "Check-Out");
        for (String[] booking : report) {
            System.out.printf("\n%-8s %-14s %-6s %-15s %-17s %-17s%n", 
                    booking[0],
                    booking[1],
                    booking[2],
                    booking[3],
                    booking[4],
                    booking[5]);
        }
        System.out.println("Total matching rooms: " + report.length);
    }
    
    private void billingSummaryRP() {
        System.out.println("--- BILLING SUMMARY REPORT ---");
        System.out.println("P = Pending, C = Completed, X = Cancelled, R = Refunded");
        System.out.print("Filter by Payment Status: ");
        char status = scanner.nextLine().trim().toUpperCase().charAt(0);
        
        if (status != 'P' && status != 'C' && status != 'X' && status != 'R') {
            Utility.printError("Invalid Option. Please try again...");
            return;
        }

        String[][] report = control.generateBillingSummaryReportDisplay(status);
        double total = control.getBillingSummaryTotal(status);
        
        System.out.println("\nFilter: Status = " + status);
        System.out.printf("\n%-12s %-15s %-14s %-10s %-10s%n", 
                "Conf. No", "Guest Name", "Room Type", "Amount", "Status");
        for (String[] booking : report) {
            System.out.printf("\n%-12s %-15s %-14s %-10.2f %-10s%n", 
                    booking[0],
                    booking[1],
                    booking[2],
                    Double.parseDouble(booking[3]),
                    booking[4]);
        }
        System.out.printf("Total Revenue: RM %.2f | Bookings count: %d%n", total, report.length);
            
    }

    private String selectStaff(Scanner scanner) {
        System.out.println("\n---Select Staff---");
        System.out.println("1. S001 Tan");
        System.out.println("2. S002 Choo");
        System.out.println("3. S003 Michelle");
        System.out.print("Enter staff choice: ");
        int choice = getMenuChoice();
        switch (choice) {
            case 1: return "S001";
            case 2: return "S002";
            case 3: return "S003";
            default:
                Utility.printError("Invalid staff selection. Please try again...");
                return null;
        }
    }
}