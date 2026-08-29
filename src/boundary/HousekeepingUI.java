package boundary;

/**
 *
 * @author Low Wei Shin
 */

import control.HousekeepingController;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import utility.Utility;

public class HousekeepingUI {

        private HousekeepingController controller;
        private Scanner scanner;

        public HousekeepingUI() {
                controller = new HousekeepingController();
                scanner = new Scanner(System.in);
        }

        public void showMenu() {

                int choice;

                do {
                        Utility.clearScreen();
                        Utility.printHeader("TARUMT RESORTS - HOUSEKEEPING SYSTEM");
                        System.out.println();
                        Utility.printSectionTitle("TASK MANAGEMENT");
                        System.out.println("   1.  Mark Room as Dirty (Queue for Cleaning)");
                        System.out.println("   2.  Staff: Mark Self Ready (Early Finish)");
                        System.out.println("   3.  Rollback Task Status");
                        System.out.println("   4.  Manually Advance Task Status");
                        System.out.println();
                        Utility.printSectionTitle("VIEW & SEARCH");
                        System.out.println("   5.  View Cleaning Queue & Staff Status");
                        System.out.println("   6.  Search Task by Room Number");
                        System.out.println("   7.  Display All Tasks");
                        System.out.println("   8.  Display Room Housekeeping Status");
                        System.out.println("   9.  Show Only Dirty Rooms");
                        System.out.println("  10.  Show Only Ready Rooms");
                        System.out.println("  11.  Show Tasks for a Selected Room");
                        System.out.println();
                        Utility.printSectionTitle("REPORTS");
                        System.out.println("  12.  Cleaning Status Analysis Report");
                        System.out.println("  13.  Daily Performance Report");
                        System.out.println("  14.  KPI Report (Daily Staff Target)");
                        System.out.println();
                        Utility.printDivider();
                        System.out.println("   0.  Exit Module");
                        Utility.printDivider();
                        System.out.print("  Enter your choice: ");

                        choice = readChoice();

                        switch (choice) {

                                case 1:
                                        markRoomDirty();
                                        break;

                                case 2:
                                        markStaffReady();
                                        break;

                                case 3:
                                        rollbackTask();
                                        break;

                                case 4:
                                        advanceTaskStatus();
                                        break;

                                case 5:
                                        controller.displayQueueAndStaffStatus();
                                        break;

                                case 6:
                                        searchByRoom();
                                        break;

                                case 7:
                                        Utility.printHeader("ALL TASKS");
                                        controller.displayAllTasks();
                                        break;

                                case 8:
                                        controller.displayRoomStatus();
                                        break;

                                case 9:
                                        controller.displayDirtyRooms();
                                        break;

                                case 10:
                                        controller.displayReadyRooms();
                                        break;

                                case 11:
                                        showTasksForSelectedRoom();
                                        break;

                                case 12:
                                        generateCleaningStatusReport();
                                        break;

                                case 13:
                                        generateDailyPerformanceReport();
                                        break;

                                case 14:
                                        generateKpiReport();
                                        break;

                                case 0:
                                        Utility.printInfo("Exiting Housekeeping Module...");
                                        break;

                                default:
                                        Utility.printError("Invalid choice! Please enter a number from the menu.");
                        }

                        if (choice != 0) {
                                Utility.pauseScreen();
                        }

                } while (choice != 0);
        }

        // =========================================================
        // ROOM SELECTION
        // =========================================================

        private String selectRoom() {

                String[][] rooms = controller.getRoomSelectionDisplayData();

                if (rooms == null || rooms.length == 0) {
                        System.out.println("No rooms available.");
                        return null;
                }

                System.out.println("\n--- Select Room ---");

                for (int i = 0; i < rooms.length; i++) {
                        if (rooms[i] != null && rooms[i][0] != null) {
                                System.out.printf("%d. Room %s (%s)%n", (i + 1), rooms[i][0], rooms[i][1]);
                        }
                }

                System.out.print("Enter room choice: ");

                int choice = readChoice();

                if (choice > 0 && choice <= rooms.length
                        && rooms[choice - 1] != null && rooms[choice - 1][0] != null) {
                        return rooms[choice - 1][0];
                }

                System.out.println("[X] Invalid choice.");
                return null;
        }

        // =========================================================
        // STAFF SELECTION  (for "Mark Self Ready" only)
        // =========================================================

        private String selectBusyStaff() {
                String[][] staff = controller.getStaffDisplayData();

                System.out.println("\n--- Select Staff Member ---");
                int displayedCount = 0;
                int[] indexMap = new int[staff.length];

                for (int i = 0; i < staff.length; i++) {
                        if (staff[i] != null && staff[i][0] != null) {
                                displayedCount++;
                                indexMap[displayedCount - 1] = i;
                                System.out.printf("%d. %s (%s) - %s%n",
                                        displayedCount, staff[i][1], staff[i][0], staff[i][2]);
                        }
                }

                if (displayedCount == 0) {
                        System.out.println("No staff found.");
                        return null;
                }

                System.out.print("Enter choice: ");
                int choice = readChoice();

                if (choice > 0 && choice <= displayedCount) {
                        int realIdx = indexMap[choice - 1];
                        return staff[realIdx][0];
                }

                System.out.println("[X] Invalid selection.");
                return null;
        }

        // =========================================================
        // TASK SELECTION  (for rollback / advance)
        // =========================================================

        private String selectTask() {

                String[][] tasks = controller.getTaskSelectionDisplayData();

                if (tasks == null || tasks.length == 0) {
                        System.out.println("No tasks available.");
                        return null;
                }

                System.out.println("\n--- Select Task ---");

                for (int i = 0; i < tasks.length; i++) {
                        String[] task = tasks[i];
                        if (task != null && task[0] != null) {
                                System.out.printf("%d. Task %s (Room %s) - %s%n",
                                        (i + 1), task[0], task[1], task[2]);
                        }
                }

                System.out.print("Enter task choice: ");

                int choice = readChoice();

                if (choice > 0 && choice <= tasks.length) {
                        String[] selected = tasks[choice - 1];
                        if (selected != null && selected[0] != null) {
                                return selected[0];
                        }
                }

                System.out.println("[X] Invalid task selection.");
                return null;
        }

        // =========================================================
        // MARK ROOM DIRTY  (replaces old "Log New Task")
        // =========================================================

        private void markRoomDirty() {

                Utility.printSectionTitle("MARK ROOM AS DIRTY");
                System.out.println("Rooms will be automatically assigned to the next free staff member.");
                System.out.println("If all staff are busy, the room will wait in the FIFO queue.");
                System.out.println();

                controller.displayRoomStatus();

                String room = selectRoom();

                if (room == null) {
                        return;
                }

                System.out.print("Remarks (optional, press Enter to skip): ");
                String remarks = scanner.nextLine().trim();

                controller.markRoomDirty(room, remarks.isBlank() ? null : remarks);
        }

        // =========================================================
        // MARK STAFF READY  (early finish)
        // =========================================================

        private void markStaffReady() {

                Utility.printSectionTitle("STAFF - EARLY FINISH (MARK READY)");
                System.out.println("Select the staff member who has finished cleaning.");
                System.out.println("If the 30-minute countdown has not expired, this counts as an early finish.");
                System.out.println("The next queued room (if any) will be auto-assigned.");
                System.out.println();

                controller.displayQueueAndStaffStatus();

                String staffId = selectBusyStaff();
                if (staffId == null) return;

                controller.markStaffReady(staffId);
        }

        // =========================================================
        // ADVANCE TASK STATUS  (manual, for inspection flows)
        // =========================================================

        private void advanceTaskStatus() {

                Utility.printSectionTitle("ADVANCE TASK STATUS");
                System.out.println("Note: Staff assignment is automatic. This option is for manual corrections.");

                String taskId = selectTask();

                if (taskId == null) {
                        return;
                }

                System.out.println("\nSelect New Status:");
                System.out.println("1. Cleaning In Progress (C)");
                System.out.println("2. Inspected (I)");
                System.out.println("3. Ready (R)");

                System.out.print("Enter choice (1-3 or C/I/R): ");

                String choice = scanner.nextLine().trim().toUpperCase();

                String status;

                switch (choice) {

                        case "1":
                        case "C":
                                status = "Cleaning In Progress";
                                break;

                        case "2":
                        case "I":
                                status = "Inspected";
                                break;

                        case "3":
                        case "R":
                                status = "Ready";
                                break;

                        default:
                                System.out.println("[X] Invalid status selection.");
                                return;
                }

                controller.updateTaskStatus(taskId, status);
        }

        // =========================================================
        // ROLLBACK TASK
        // =========================================================

        private void rollbackTask() {

                String taskId = selectTask();

                if (taskId == null) {
                        return;
                }

                controller.rollbackTask(taskId);
        }

        // =========================================================
        // SEARCH BY ROOM
        // =========================================================

        private void searchByRoom() {

                String room = selectRoom();

                if (room == null) {
                        return;
                }

                controller.searchByRoom(room);
        }

        // =========================================================
        // REPORT 1:
        // CLEANING STATUS ANALYSIS REPORT
        // =========================================================

        private void generateCleaningStatusReport() {

                System.out.println("\n===== CLEANING STATUS ANALYSIS REPORT OPTIONS =====");

                // STATUS FILTER

                System.out.println("\nFilter by Status:");
                System.out.println("0. All Statuses");
                System.out.println("1. Dirty");
                System.out.println("2. Cleaning In Progress");
                System.out.println("3. Inspected");
                System.out.println("4. Ready");

                int statusChoice = readIntegerInRange("Select Status (0-4): ", 0, 4);

                String statusFilter;

                switch (statusChoice) {

                        case 1:
                                statusFilter = "Dirty";
                                break;

                        case 2:
                                statusFilter = "Cleaning In Progress";
                                break;

                        case 3:
                                statusFilter = "Inspected";
                                break;

                        case 4:
                                statusFilter = "Ready";
                                break;

                        case 0:
                        default:
                                statusFilter = "ALL";
                                break;
                }

                // STAFF FILTER

                System.out.println("\nFilter by Staff:");
                System.out.println("0. All Staff");
                System.out.println("1. S001 (Tan)");
                System.out.println("2. S002 (Choo)");
                System.out.println("3. S003 (Michelle)");
                System.out.println("4. SYSTEM");

                int staffChoice = readIntegerInRange("Select Staff (0-4): ", 0, 4);

                String staffFilter;

                switch (staffChoice) {

                        case 1:
                                staffFilter = "S001";
                                break;

                        case 2:
                                staffFilter = "S002";
                                break;

                        case 3:
                                staffFilter = "S003";
                                break;

                        case 4:
                                staffFilter = "SYSTEM";
                                break;

                        case 0:
                        default:
                                staffFilter = "ALL";
                                break;
                }

                // ROOM FILTER

                System.out.println("\nRoom Number Search:");
                System.out.print("Enter Room Number [Press Enter to show all rooms]: ");
                String roomSearch = scanner.nextLine().trim();

                // SORT OPTION

                System.out.println("\nSort Report By:");
                System.out.println("1. Room Number (Ascending)");
                System.out.println("2. Time Spent (Longest First)");

                int sortOption = readIntegerInRange("Select Sort Option (1-2): ", 1, 2);

                controller.generateCleaningStatusReport(statusFilter, staffFilter, roomSearch, sortOption);
        }

        // =========================================================
        // REPORT 2:
        // DAILY HOUSEKEEPING PERFORMANCE REPORT
        // =========================================================

        private void generateDailyPerformanceReport() {

                System.out.println("\n===== DAILY HOUSEKEEPING PERFORMANCE REPORT OPTIONS =====");

                LocalDate reportDate = readReportDate();

                System.out.println("\nFilter by Staff:");
                System.out.println("0. All Staff");
                System.out.println("1. S001 (Tan)");
                System.out.println("2. S002 (Choo)");
                System.out.println("3. S003 (Michelle)");
                System.out.println("4. SYSTEM");

                int staffChoice = readIntegerInRange("Select Staff (0-4): ", 0, 4);

                String staffFilter;

                switch (staffChoice) {

                        case 1:
                                staffFilter = "S001";
                                break;

                        case 2:
                                staffFilter = "S002";
                                break;

                        case 3:
                                staffFilter = "S003";
                                break;

                        case 4:
                                staffFilter = "SYSTEM";
                                break;

                        case 0:
                        default:
                                staffFilter = "ALL";
                                break;
                }

                long minimumMinutes = readNonNegativeLong("Minimum Task Time (0 = All): ");

                System.out.println("\nSort Report By:");
                System.out.println("1. Created Time (Earliest First)");
                System.out.println("2. Task Time (Longest First)");

                int sortOption = readIntegerInRange("Select Sort Option (1-2): ", 1, 2);

                controller.generateDailyPerformanceReport(reportDate, staffFilter, minimumMinutes, sortOption);
        }

        // =========================================================
        // REPORT 3:
        // KPI REPORT
        // =========================================================

        private void generateKpiReport() {

                System.out.println("\n===== KPI REPORT OPTIONS =====");
                System.out.println("KPI Target: each staff must clean >= 5 rooms per day.");
                System.out.println("On-Time Target: each room cleaned within 30 minutes.");
                System.out.println();

                LocalDate reportDate = readReportDate();

                controller.generateKpiReport(reportDate);
        }

        // =========================================================
        // SHOW TASKS FOR SELECTED ROOM
        // =========================================================

        private void showTasksForSelectedRoom() {

                controller.displayRoomStatus();

                String room = selectRoom();

                if (room == null) {
                        return;
                }

                controller.showTasksForRoom(room);
        }

        // =========================================================
        // INPUT VALIDATION
        // =========================================================

        private int readChoice() {

                String input = scanner.nextLine().trim();

                try {

                        return Integer.parseInt(input);

                } catch (NumberFormatException ex) {

                        return -1;
                }
        }

        /**
         * Reads an integer and ensures that it is
         * within the required range.
         */
        private int readIntegerInRange(String prompt, int minimum, int maximum) {

                while (true) {

                        System.out.print(prompt);

                        String input = scanner.nextLine().trim();

                        try {

                                int value = Integer.parseInt(input);

                                if (value >= minimum && value <= maximum) {

                                        return value;
                                }

                        } catch (NumberFormatException ex) {
                                // Validation message below.
                        }

                        System.out.println("[X] Invalid input. Please enter a number from "
                                + minimum + " to " + maximum + ".");
                }
        }

        /**
         * Reads the report date.
         * Blank input means today's date.
         */
        private LocalDate readReportDate() {

                while (true) {

                        System.out.print("Report Date (YYYY-MM-DD, Press Enter = Today): ");

                        String input = scanner.nextLine().trim();

                        if (input.isEmpty()) {

                                return LocalDate.now();
                        }

                        try {

                                return LocalDate.parse(input);

                        } catch (DateTimeParseException ex) {

                                System.out.println("[X] Invalid date. Please use YYYY-MM-DD.");
                        }
                }
        }

        /**
         * Reads 0 or a positive whole number.
         */
        private long readNonNegativeLong(String prompt) {

                while (true) {

                        System.out.print(prompt);

                        String input = scanner.nextLine().trim();

                        try {

                                long value = Long.parseLong(input);

                                if (value >= 0) {

                                        return value;
                                }

                        } catch (NumberFormatException ex) {
                                // Validation message below.
                        }

                        System.out.println("[X] Invalid input. Please enter 0 or a positive number.");
                }
        }

        // =========================================================
        // GETTER
        // =========================================================

        public HousekeepingController getController() {

                return controller;
        }
}