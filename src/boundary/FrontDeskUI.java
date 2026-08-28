/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boundary;

import control.FrontDeskControl;
import java.time.LocalDate;
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
                    updatePaymentStatus();
                    break;
                case 5:
                    updateGuestPhoneNo();
                    break;
                case 6:
                    processCheckout();
                    break;
                case 7:
                    viewReadyRoomNotifications();
                    break;
                case 8:
                    roomOccupancyRP();
                    break;
                case 9:
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
        System.out.println(" 4. Update Payment Status");
        System.out.println(" 5. Update Guest Phone No");
        System.out.println(" 6. Process Guest Check Out");
        System.out.println(" 7. View Ready Room Notifications\n");
        Utility.printSectionTitle("REPORTS");
        System.out.println(" 8. Room Occupancy Report");
        System.out.println(" 9. Billing Summary Report");
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
        System.out.print(" Enter Confirmation Number (8 digits): ");
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
    
    private void updatePaymentStatus() {
        System.out.print("Enter Confirmation Number (8 digits): ");
        String confirmationNo = scanner.nextLine().trim();

        if (!Utility.isValidConfirmationNo(confirmationNo)) {
            Utility.printError("Invalid Confirmation number format.");
            return;
        }

        String[] booking = control.getBookingDisplayData(confirmationNo);
        if (booking == null) {
            Utility.printError("No booking found for " + confirmationNo);
            return;
        }
        
        System.out.println("Payment Amount: " + booking[5]);
        System.out.println("Current Payment Status: " + booking[6]);
        System.out.println("P = Pending, C = Completed, X = Cancelled, R = Refunded");
        System.out.print("Enter New Payment Status: ");
        String statusInput = scanner.nextLine().trim().toUpperCase();

        if (statusInput.isEmpty()) {
            Utility.printError("No input entered");
            return;
        }
        char newStatus = statusInput.charAt(0);

        if (newStatus != 'P' && newStatus != 'C' && newStatus != 'X' && newStatus != 'R') {
            Utility.printError("Invalid status. Must be P, C, X, or R.");
            return;
        }

        if (control.updatePaymentStatus(confirmationNo, newStatus)) {
            Utility.printSuccess("Payment status updated to " + newStatus + ".");
        } else {
            Utility.printError("Failed to update payment status.");
        }
    }
    
    private void updateGuestPhoneNo() {
        System.out.print("Enter Guest ID or Name: ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            Utility.printError("Input cannot be empty.");
            return;
        }

        String[][] matches = control.searchGuestsByIdOrName(input);
        if (matches.length == 0) {
            Utility.printError("No guest found matching \"" + input + "\".");
            return;
        }

        String confirmationNo;
        if (matches.length == 1) {
            confirmationNo = matches[0][0];
        } else {
            System.out.println("\nMultiple guests matched. Please select one:");
            System.out.printf("%-4s %-12s %-10s %-20s%n", "No.", "Conf. No", "Guest ID", "Guest Name");
            for (int i = 0; i < matches.length; i++) {
                System.out.printf("%-4d %-12s %-10s %-20s%n", i + 1, matches[i][0], matches[i][1], matches[i][2]);
            }
            System.out.print("Enter selection number (0 = Cancel): ");
            int selection;
            try {
                selection = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException ex) {
                Utility.printError("Invalid selection.");
                return;
            }
            if (selection == 0) {
                System.out.println("Update cancelled.");
                return;
            }
            if (selection < 1 || selection > matches.length) {
                Utility.printError("Invalid selection.");
                return;
            }
            confirmationNo = matches[selection - 1][0];
        }

        String[] booking = control.getBookingDisplayData(confirmationNo);
        System.out.println("Current Phone Number: " + booking[2]);
        System.out.print("Enter New Phone Number: ");
        String phoneInput = scanner.nextLine().trim();

        if (!Utility.isValidPhoneNo(phoneInput)) {
            Utility.printError("Invalid phone number format.");
            return;
        }

        long newPhoneNo;
        try {
            newPhoneNo = Long.parseLong(phoneInput);
        } catch (NumberFormatException ex) {
            Utility.printError("Invalid phone number format.");
            return;
        }

        if (control.updateGuestPhoneNoByConfirmationNo(confirmationNo, newPhoneNo)) {
            Utility.printSuccess("Phone number updated successfully.");
        } else {
            Utility.printError("Failed to update: phone number is already used by another guest.");
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

        String taskId = control.processCheckoutAndGetTaskId(
                confirmationNo,
                staff,
                null
        );
        
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
        
        System.out.println("\n Date Range Filter: ");
        System.out.print(" 1. Today   2. Select Month  3. Year");
        System.out.print("\nChoice: ");
        int rangeOption = getMenuChoice();
        if (rangeOption < 1 || rangeOption > 3) {
            Utility.printError("Invalid Option. Please try again...");
            return;
        }
        int selectedMonth = 0;
        String rangeLabel;
        if (rangeOption == 1) {
            rangeLabel = LocalDate.now().toString(); 
        } else if (rangeOption == 2) {
            System.out.println("1. January   2. February  3. March     4. April");
            System.out.println("5. May       6. June      7. July      8. August");
            System.out.println("9. September 10. October  11. November 12. December");
            System.out.print("Select Month: ");
            selectedMonth = getMenuChoice();
            if (selectedMonth < 1 || selectedMonth > 12) {
                Utility.printError("Invalid Option. Please try again...");
                return;
            }
            rangeLabel = control.getMonthNameLabel(selectedMonth);
        } else {
            rangeLabel = String.valueOf(java.time.LocalDate.now().getYear());
        }

        String[] summary = control.getRoomOccupancySummary(roomTypeFilter, rangeOption, selectedMonth);
        String[][] occupancyByType = control.getOccupancyByRoomType(roomTypeFilter, rangeOption, selectedMonth);
        String[][] topRooms = control.getTopUtilizedRooms(roomTypeFilter, rangeOption, selectedMonth, 5);
        String avgStay = control.getAverageLengthOfStay(roomTypeFilter, rangeOption, selectedMonth);

        String generatedOn = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        System.out.println("=".repeat(70));
        System.out.printf("%42s%n", "ROOM OCCUPANCY ANALYSIS REPORT");
        System.out.println("=".repeat(70));
        System.out.println("Generated On       : " + generatedOn);
        System.out.println("Room Type          : " + (roomTypeFilter == null ? "ALL" : roomTypeFilter));
        System.out.println("Date Range         : " + rangeLabel);
        System.out.println("-".repeat(70));

        System.out.println("OCCUPANCY BREAKDOWN BY ROOM TYPE");
        System.out.printf("%-16s %-8s %-16s %-16s %-10s%n", "Room Type", "Rooms", "Occupied-Days", "Total Room-Days", "Rate");
        for (String[] row : occupancyByType) {
            System.out.printf("%-16s %-8s %-16s %-16s %-10s%n", row[0], row[1], row[2], row[3], row[4] + "%");
        }
        System.out.println("-".repeat(70));

        System.out.println("UTILIZATION HEALTH");
        System.out.println("Overall Occupancy Rate      : " + summary[5] + "%");
        System.out.println("Average Length of Stay      : " + avgStay + " night(s)");
        System.out.println("-".repeat(70));

        System.out.println("TOP " + topRooms.length + " MOST UTILIZED ROOMS (this filter)");
        System.out.printf("%-5s %-8s %-16s %-14s%n", "Rank", "Room No", "Room Type", "Occupied-Days");
        for (String[] row : topRooms) {
            System.out.printf("%-5s %-8s %-16s %-14s%n", row[0], row[1], row[2], row[3]);
        }
        System.out.println("=".repeat(70));

        double rate = Double.parseDouble(summary[5]);
        if (rate >= 80) {
            System.out.println("Suggestion: High demand detected : consider dynamic/premium pricing and prioritize fast housekeeping turnaround.");
        } else if (rate >= 50) {
            System.out.println("Suggestion: Occupancy is healthy and stable : no immediate action needed.");
        } else if (rate >= 20) {
            System.out.println("Suggestion: Below-average occupancy : consider promotions or targeted marketing for underused room types.");
        } else {
            System.out.println("Suggestion: Low occupancy : recommend reviewing pricing strategy or investigating underlying causes.");
        }
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
        
        String[][] revenueByType = control.getBillingRevenueByRoomType(status);
        String[][] topBookings = control.getBillingTopValueBookings(status, 5);
        String[] health = control.getBillingSummaryBreakdown(status);

        String generatedOn = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        System.out.println("=".repeat(85));
        System.out.printf("%40s%n", "BILLING SUMMARY ANALYSIS REPORT");
        System.out.println("=".repeat(85));
        System.out.println("Generated On       : " + generatedOn);
        System.out.println("Payment Status     : " + status);
        System.out.println("-".repeat(85));
        System.out.println("REVENUE BREAKDOWN BY ROOM TYPE");
        System.out.printf("%-16s %-10s %-14s %-14s %-10s%n", "Room Type", "Bookings", "Total (RM)", "Avg/Booking", "% Revenue");
        for (String[] row : revenueByType) {
            System.out.printf("%-16s %-10s %-14s %-14s %-10s%n", row[0], row[1], row[2], row[3], row[4] + "%");
        }
        System.out.println("-".repeat(85));

        System.out.println("PAYMENT COLLECTION HEALTH");
        System.out.println("Collection Rate (Completed vs Pending)  : " + health[0] + "% (" + health[4] + " Completed / " + health[5] + " Pending)");
        System.out.println("Outstanding Exposure (Pending)          : RM " + health[1] + " (" + health[5] + " bookings, " + health[10] + "% of total)");
        System.out.println("Cancelled Bookings                      : " + health[6] + " (" + health[11] + "% of total, RM " + health[13] + ")");
        System.out.println("Refunded Bookings                       : " + health[7] + " (" + health[12] + "% of total, RM " + health[8] + ")");
        System.out.println("Average Transaction Value (this filter) : RM " + health[2]);
        System.out.println("-".repeat(85));

        System.out.println("TOP " + topBookings.length + " HIGHEST-VALUE BOOKINGS (this filter)");
        System.out.printf("%-5s %-12s %-15s %-10s%n", "Rank", "Conf. No", "Guest Name", "Amount");
        for (String[] row : topBookings) {
            System.out.printf("%-5s %-12s %-15s %-10s%n", row[0], row[1], row[2], row[3]);
        }
        System.out.println("=".repeat(85));

        double rate = Double.parseDouble(health[0]);
        if (rate < 60 && (status == 'P' || status == 'C')) {
            System.out.println("Suggestion: Collection rate is below 60% — recommend following up on pending payments before checkout.");
        } else {
            System.out.println("Suggestion: Collection health is stable for the current filter.");
        }
    }

    private String selectStaff(Scanner scanner) {
        System.out.println();
        Utility.printSectionTitle("SELECT STAFF");
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