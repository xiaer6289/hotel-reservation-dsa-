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
                    viewReadyRoomNotifications();
                    break;
                case 6:
                    roomOccupancyRP();
                    break;
                case 7:
                    billingSummaryRP();
                    break;
                case 0: 
                    control.save();
                    Utility.printSuccess("Data saved. Returning to Main Menu...");
                    break;
                default: 
                    Utility.printError("Invalid option, please try again");
            }
            if (choice != 0) Utility.pauseScreen();
        } while (choice != 0);
    }
    
    private void displayMenu() {
        Utility.clearScreen();
        Utility.printHeader("FRONT DESK SERVICE\n");
        Utility.printSectionTitle("GUEST MANAGEMENT");
        System.out.println(" 1. Search Booking by Confirmation No");
        System.out.println(" 2. View All Booking");
        System.out.println(" 3. Check Room Availability");
        System.out.println(" 4. Process Guest Check Out");
        System.out.println(" 5. View Ready Room Notifications\n");
        Utility.printSectionTitle("REPORTS");
        System.out.println(" 6. Room Occupancy Report");
        System.out.println(" 7. Billing Summary Report");
        System.out.println(" 0. Save & Return to Main Menu");
        System.out.print(" Enter choice: ");
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
        Utility.printHeader("ROOM OCCUPANCY REPORT");
        System.out.println("\nFilter by Room Type: ");
        System.out.println(" 1. Deluxe   2. Deluxe Twin   3. Superior   4. Superior Twin   5. All");
        System.out.print("Choice: ");
        int type = getMenuChoice();
        String roomTypeFilter;
        switch (type) {
            case 1: roomTypeFilter = "DELUXE"; break;
            case 2: roomTypeFilter = "DELUXE_TWIN"; break;
            case 3: roomTypeFilter = "SUPERIOR"; break;
            case 4: roomTypeFilter = "SUPERIOR_TWIN"; break;
            case 5: roomTypeFilter = null; break;
            default:
                Utility.printError("Invalid Option.");
                return;
        }
        
        System.out.println("\n Availability Filter: ");
        System.out.print(" A = Available   O = Occupied");
        System.out.print("\nChoice: ");
        String statusInput = scanner.nextLine().trim().toUpperCase();
        if (!statusInput.equals("A") && !statusInput.equals("O")) {
            Utility.printError("Invalid Option");
            return;
        }
        boolean availabilityFilter = statusInput.equals("A");
        
        String[][] report = control.generateRoomOccupancyReportDisplay(
                roomTypeFilter,
                availabilityFilter);
        
        System.out.println("\n Filter: Room Type = " + (roomTypeFilter == null ? "ALL" : roomTypeFilter) 
        + "  | Status = " + (availabilityFilter ? "AVAILABLE" : "OCCUPIED\n"));

        String hdr = String.format("\n  %-8s %-15s %-6s %-20s %-17s %-17s",
            "Room No", "Type", "Floor", "Guest Name", "Check-In", "Check-Out");
        System.out.println(hdr);
        
        if (report.length == 0) {
            System.out.println("  No rooms match the selected filter.");
        } else {
            for (String[] row : report) {
                System.out.printf("  %-8s %-15s %-6s %-20s %-17s %-17s", 
                row[0], row[1], row[2], row[3],
                formatDT(row[4]), formatDT(row[5]));
            }
        }
        System.out.println("\n Total matching rooms: " + report.length);
    }
    
    private void billingSummaryRP() {
        Utility.printHeader("BILLING SUMMARY REPORT");
        System.out.println("\n Filter by Payment Status: ");
        System.out.println(" P = Pending  C = Completed  X = Cancelled  R = Refunded");
        System.out.print(" Choice: ");
        String statusInput = scanner.nextLine().trim().toUpperCase();
        
        if (statusInput.isEmpty()) {
            Utility.printError("No input entered.");
            return;
        }

        char status = statusInput.charAt(0);
        if (status != 'P' && status != 'C' && status != 'X' && status != 'R') {
            Utility.printError("Invalid Status. Enter P, C, X or R");
            return;
        }

        String[][] report = control.generateBillingSummaryReportDisplay(status);
        double total = control.getBillingSummaryTotal(status);
        
        System.out.println("\n  Filter: Status = " + status + "\n");

        String hdr = String.format("\n  %-12s %-20s %-15s %-12s %-10s",
            "Conf. No", "Guest Name", "Room Type", "Amount (RM)", "Status");
        System.out.println(hdr);
        System.out.println("  " + "-".repeat(hdr.trim().length() + 2));

        if (report.length == 0) {
            System.out.println("  No bookings match the selected status.");
        } else {
            for (String[] booking : report) {
                System.out.printf("  %-12s %-20s %-15s %-12.2f %-10s%n", 
                        booking[0], booking[1], booking[2],
                        Double.parseDouble(booking[3]), booking[4]);
            }
        }
        
        System.out.printf("\n  Total Revenue: RM %.2f  |  Bookings count: %d%n", total, report.length);
            
    }

    private String selectStaff(Scanner scanner) {
        Utility.printSectionTitle("\nSELECT STAFF");
        System.out.println(" 1. S001 - Tan");
        System.out.println(" 2. S002 - Choo");
        System.out.println(" 3. S003 - Michelle");
        System.out.print(" Enter staff choice: ");
        int choice = getMenuChoice();
        switch (choice) {
            case 1: return "S001";
            case 2: return "S002";
            case 3: return "S003";
            default:
                Utility.printError("Invalid staff selection.");
                return null;
        }
    }

    private String formatDT(String raw) {
        if (raw == null || raw.length() < 16) return raw == null? "-" : raw;
        return raw.substring(0, 10) + " " + raw.substring(11, 16);
    }
}