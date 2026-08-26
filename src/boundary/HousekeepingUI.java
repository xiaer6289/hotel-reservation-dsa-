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
                        System.out.println("   1.  Log New Housekeeping Task");
                        System.out.println("   2.  Update Task Status");
                        System.out.println("   3.  Rollback Task Status");
                        System.out.println();
                        Utility.printSectionTitle("VIEW & SEARCH");
                        System.out.println("   4.  Search Task by Room Number");
                        System.out.println("   5.  Display All Tasks");
                        System.out.println("   6.  Display Room Housekeeping Status");
                        System.out.println("   7.  Show Only Dirty Rooms");
                        System.out.println("   8.  Show Only Ready Rooms");
                        System.out.println("   9.  Show Tasks for a Selected Room");
                        System.out.println();
                        Utility.printSectionTitle("REPORTS");
                        System.out.println("  10.  Generate Cleaning Status Report");
                        System.out.println("  11.  Generate Daily Performance Report");
                        System.out.println();
                        Utility.printDivider();
                        System.out.println("   0.  Exit Module");
                        Utility.printDivider();
                        System.out.print("  Enter your choice: ");

                        choice = readChoice();

                        switch (choice) {

                                case 1:
                                        logNewTask();
                                        break;

                                case 2:
                                        updateTaskStatus();
                                        break;

                                case 3:
                                        rollbackTask();
                                        break;

                                case 4:
                                        searchByRoom();
                                        break;

                                case 5:
                                        Utility.printHeader("ALL TASKS");
                                        controller.displayAllTasks();
                                        break;

                                case 6:
                                        controller.displayRoomStatus();
                                        break;

                                case 7:
                                        controller.displayDirtyRooms();
                                        break;

                                case 8:
                                        controller.displayReadyRooms();
                                        break;

                                case 9:
                                        showTasksForSelectedRoom();
                                        break;

                                case 10:
                                        generateCleaningStatusReport();
                                        break;

                                case 11:
                                        generateDailyPerformanceReport();
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

                if (rooms == null|| rooms.length == 0) {

                        System.out.println("No rooms available.");

                        return null;
                }

                System.out.println("\n--- Select Room ---");

                for (int i = 0; i < rooms.length; i++) {

                        if (rooms[i] != null && rooms[i][0] != null) {

                                System.out.printf("%d. Room %s (%s)%n",(i + 1), rooms[i][0], rooms[i][1]);
                        }
                }

                System.out.print("Enter room choice: ");

                int choice = readChoice();

                if (choice > 0 && choice <= rooms.length && rooms[choice - 1] != null && rooms[choice - 1][0] != null) {

                        return rooms[choice - 1][0];
                }

                System.out.println("❌ Invalid choice.");

                return null;
        }

        // =========================================================
        // STAFF SELECTION
        // =========================================================

        private String selectStaff() {

                System.out.println("\n--- Select Staff ---");

                System.out.println("1. Staff S001 (Tan)");

                System.out.println("2. Staff S002 (Choo)");

                System.out.println("3. Staff S003 (Michelle)");

                System.out.print("Enter staff choice: ");

                int choice = readChoice();

                switch (choice) {

                        case 1:
                                return "S001";

                        case 2:
                                return "S002";

                        case 3:
                                return "S003";

                        default:
                                System.out.println("❌ Invalid staff selection.");

                                return null;
                }
        }

        // =========================================================
        // TASK SELECTION
        // =========================================================

        private String selectTask() {

                String[][] tasks = controller.getTaskSelectionDisplayData();

                if (tasks == null|| tasks.length == 0) {

                        System.out.println("No tasks available.");

                        return null;
                }

                System.out.println(
                                "\n--- Select Task ---");

                for (int i = 0; i < tasks.length; i++) {

                        String[] task = tasks[i];

                        if (task != null && task[0] != null) {

                                System.out.printf("%d. Task %s (Room %s) - %s%n",(i + 1), task[0], task[1], task[2]);
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

                System.out.println("❌ Invalid task selection.");

                return null;
        }

        // =========================================================
        // LOG NEW TASK
        // =========================================================

        private void logNewTask() {

                controller.displayRoomStatus();

                String room = selectRoom();

                if (room == null) {
                        return;
                }

                String staffId = selectStaff();

                if (staffId == null) {
                        return;
                }

                controller.logNewTask(room, staffId);
        }

        // =========================================================
        // UPDATE TASK STATUS
        // =========================================================

        private void updateTaskStatus() {

                String taskId = selectTask();

                if (taskId == null) {
                        return;
                }

                System.out.println("\nSelect New Status:");

                System.out.println("1. Dirty (D)");

                System.out.println("2. Cleaning In Progress (C)");

                System.out.println("3. Inspected (I)");

                System.out.println("4. Ready (R)");

                System.out.print("Enter choice (1-4 or D/C/I/R): ");

                String choice = scanner.nextLine().trim().toUpperCase();

                String status;

                switch (choice) {

                        case "1":
                        case "D":
                                status = "Dirty";
                                break;

                        case "2":
                        case "C":
                                status = "Cleaning In Progress";
                                break;

                        case "3":
                        case "I":
                                status = "Inspected";
                                break;

                        case "4":
                        case "R":
                                status = "Ready";
                                break;

                        default:
                                System.out.println("❌ Invalid status selection.");

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

                // -----------------------------------------------------
                // STATUS FILTER
                // -----------------------------------------------------

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

                // -----------------------------------------------------
                // STAFF FILTER
                // -----------------------------------------------------

                System.out.println("\nFilter by Staff:");

                System.out.println("0. All Staff");

                System.out.println("1. S001");

                System.out.println("2. S002");

                System.out.println("3. S003");

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

                // -----------------------------------------------------
                // ROOM FILTER / SEARCH
                // -----------------------------------------------------

                System.out.println(
                                "\nRoom Number Search:");

                System.out.print("Enter Room Number [Press Enter to show all rooms]: ");

                String roomSearch = scanner.nextLine().trim();

                // -----------------------------------------------------
                // SORT OPTION
                // -----------------------------------------------------

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

                // -----------------------------------------------------
                // DATE FILTER
                // -----------------------------------------------------

                LocalDate reportDate = readReportDate();

                // -----------------------------------------------------
                // STAFF FILTER
                // -----------------------------------------------------

                System.out.println("\nFilter by Staff:");

                System.out.println("0. All Staff");

                System.out.println("1. S001");

                System.out.println("2. S002");

                System.out.println("3. S003");

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

                // -----------------------------------------------------
                // MINIMUM TASK TIME FILTER
                // -----------------------------------------------------

                long minimumMinutes = readNonNegativeLong("Minimum Task Time "
                                + "(0 = All): ");

                // -----------------------------------------------------
                // SORT OPTION
                // -----------------------------------------------------

                System.out.println("\nSort Report By:");

                System.out.println("1. Created Time " + "(Earliest First)");

                System.out.println("2. Task Time " + "(Longest First)");

                int sortOption = readIntegerInRange("Select Sort Option (1-2): ", 1, 2);

                controller.generateDailyPerformanceReport(reportDate, staffFilter, minimumMinutes, sortOption);
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

                controller.showTasksForRoom(
                                room);
        }

        // =========================================================
        // RESET ALL ROOMS
        // =========================================================

        // private void resetAllRooms() {

        //         System.out.print("Are you sure you want to reset "
        //                         + "all rooms to default ready data? "
        //                         + "(yes/no): ");

        //         String confirmation = scanner.nextLine().trim()
        //                         .toLowerCase();

        //         if (!confirmation.equals("yes")
        //                         && !confirmation.equals("y")) {

        //                 System.out.println(
        //                                 "Reset operation cancelled.");

        //                 return;
        //         }

        //         controller.resetToDefaultData();
        // }

        // =========================================================
        // INPUT VALIDATION
        // =========================================================

        private int readChoice() {

                String input = scanner.nextLine().trim();

                try {

                        return Integer.parseInt(
                                        input);

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

                        System.out.println("❌ Invalid input. "+ "Please enter a number from "
                                        + minimum
                                        + " to "
                                        + maximum
                                        + ".");
                }
        }

        /**
         * Reads the report date.
         * Blank input means today's date.
         */
        private LocalDate readReportDate() {

                while (true) {

                        System.out.print("Report Date " + "(YYYY-MM-DD, " + "Press Enter = Today): ");

                        String input = scanner.nextLine().trim();

                        if (input.isEmpty()) {

                                return LocalDate.now();
                        }

                        try {

                                return LocalDate.parse(input);

                        } catch (DateTimeParseException ex) {

                                System.out.println("❌ Invalid date. "
                                                + "Please use YYYY-MM-DD.");
                        }
                }
        }

        /**
         * Reads 0 or a positive whole number.
         */
        private long readNonNegativeLong(
                        String prompt) {

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

                        System.out.println("❌ Invalid input. " + "Please enter 0 or " + "a positive number.");
                }
        }

        // =========================================================
        // GETTER
        // =========================================================

        public HousekeepingController getController() {

                return controller;
        }
}