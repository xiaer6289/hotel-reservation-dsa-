package boundary;

/**
 *
 * @author Low Wei Shin
 */

import control.HousekeepingController;
import control.report.CleaningStatusFlowRP;
import control.report.DailyPerformanceRP;
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
            System.out.println("\n=========================================");
            System.out.println("   TARUMT RESORTS - HOUSEKEEPING SYSTEM");
            System.out.println("=========================================");
            System.out.println("1. Log New Housekeeping Task");
            System.out.println("2. Update Task Status");
            System.out.println("3. Advance Task to Next Status");
            System.out.println("4. Rollback Task Status");
            System.out.println("5. Search Task by Room Number");
            System.out.println("6. Generate Cleaning Status Report");
            System.out.println("7. Generate Daily Performance Report");
            System.out.println("8. Display All Tasks");
            System.out.println("9. Display Room Housekeeping Status");
            System.out.println("10. Show Only Dirty Rooms");
            System.out.println("11. Show Only Ready Rooms");
            System.out.println("12. Show Tasks for a Selected Room");
            System.out.println("13. Reset All Rooms to Default Ready Data");
            System.out.println("0. Exit Module");
            System.out.println("=========================================");
            System.out.print("Enter your choice: ");

            choice = readChoice();

            switch (choice) {
                case 1:
                    logNewTask();
                    break;
                case 2:
                    updateTaskStatus();
                    break;
                case 3:
                    advanceTaskStatus();
                    break;
                case 4:
                    rollbackTask();
                    break;
                case 5:
                    searchByRoom();
                    break;
                case 6:
                    new CleaningStatusFlowRP().generateReport(controller.getTaskLog());
                    break;
                case 7:
                    new DailyPerformanceRP().generateReport(controller.getTaskLog());
                    break;
                case 8:
                    System.out.println("\n=== ALL TASKS ===");
                    controller.getTaskLog().display();
                    break;
                case 9:
                    controller.displayRoomStatus();
                    break;
                case 10:
                    controller.displayDirtyRooms();
                    break;
                case 11:
                    controller.displayReadyRooms();
                    break;
                case 12:
                    showTasksForSelectedRoom();
                    break;
                case 13:
                    resetAllRooms();
                    break;
                case 0:
                    System.out.println("Exiting Housekeeping Module...");
                    break;
                default:
                    System.out.println("❌ Invalid choice! Please try again.");
            }

            if (choice != 0) {
                Utility.pauseScreen();
            }

        } while (choice != 0);
    }

    private String selectRoom() {
        entity.Room[] rooms = controller.getRooms();
        if (rooms == null || rooms.length == 0) {
            System.out.println("No rooms available.");
            return null;
        }

        System.out.println("\n--- Select Room ---");
        for (int i = 0; i < rooms.length; i++) {
            if (rooms[i] != null) {
                System.out.printf("%d. Room %s (%s)%n", (i + 1), rooms[i].getRoomNumber(), rooms[i].getStatusLabel());
            }
        }
        System.out.print("Enter room choice: ");
        int choice = readChoice();
        if (choice > 0 && choice <= rooms.length && rooms[choice - 1] != null) {
            return rooms[choice - 1].getRoomNumber();
        }
        System.out.println("❌ Invalid choice.");
        return null;
    }

    private String selectStaff() {
        System.out.println("\n--- Select Staff ---");
        System.out.println("1. Staff S001 (Tan)");
        System.out.println("2. Staff S002 (Choo)");
        System.out.println("3. Staff S003 (Michelle)");
        System.out.print("Enter staff choice: ");
        int choice = readChoice();
        switch (choice) {
            case 1: return "S001";
            case 2: return "S002";
            case 3: return "S003";
            default:
                System.out.println("❌ Invalid staff selection.");
                return null;
        }
    }

    private String selectTask() {
        adt.linear.LinearADT<entity.TaskLogEntry> tasks = controller.getTaskLog();
        if (tasks == null || tasks.size() == 0) {
            System.out.println("No tasks available.");
            return null;
        }

        System.out.println("\n--- Select Task ---");
        for (int i = 0; i < tasks.size(); i++) {
            entity.TaskLogEntry task = tasks.get(i);
            if (task != null) {
                System.out.printf("%d. Task %s (Room %s) - %s%n", (i + 1), task.getTaskId(), task.getRoomNumber(), task.getStatus());
            }
        }
        System.out.print("Enter task choice: ");
        int choice = readChoice();
        if (choice > 0 && choice <= tasks.size()) {
            entity.TaskLogEntry selected = tasks.get(choice - 1);
            if (selected != null) {
                return selected.getTaskId();
            }
        }
        System.out.println("❌ Invalid task selection.");
        return null;
    }

    private void logNewTask() {
        controller.displayRoomStatus();
        String room = selectRoom();
        if (room == null) return;

        String staffId = selectStaff();
        if (staffId == null) return;

        controller.logNewTask(room, staffId);
    }

    private void updateTaskStatus() {
        String taskId = selectTask();
        if (taskId == null) return;
        
        System.out.println("\nSelect New Status:");
        System.out.println("1. Dirty (D)");
        System.out.println("2. Cleaning In Progress (C)");
        System.out.println("3. Inspected (I)");
        System.out.println("4. Ready (R)");
        System.out.print("Enter choice (1-4 or D/C/I/R): ");
        
        String choice = scanner.nextLine().trim().toUpperCase();
        String status;
        
        switch (choice) {
            case "1": case "D":
                status = "Dirty";
                break;
            case "2": case "C":
                status = "Cleaning In Progress";
                break;
            case "3": case "I":
                status = "Inspected";
                break;
            case "4": case "R":
                status = "Ready";
                break;
            default:
                System.out.println("❌ Invalid status selection.");
                return;
        }
        
        controller.updateTaskStatus(taskId, status);
    }

    private void advanceTaskStatus() {
        String taskId = selectTask();
        if (taskId == null) return;
        
        controller.advanceTaskStatus(taskId);
    }

    private void rollbackTask() {
        String taskId = selectTask();
        if (taskId == null) return;
        
        controller.rollbackTask(taskId);
    }

    private void searchByRoom() {
        String room = selectRoom();
        if (room == null) return;
        
        controller.searchByRoom(room);
    }

    private void showTasksForSelectedRoom() {
        controller.displayRoomStatus();
        String room = selectRoom();
        if (room == null) return;
        
        controller.showTasksForRoom(room);
    }

    private void resetAllRooms() {
        System.out.print("Are you sure you want to reset all rooms to default ready data? (yes/no): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        if (!confirmation.equals("yes") && !confirmation.equals("y")) {
            System.out.println("Reset operation cancelled.");
            return;
        }
        controller.resetToDefaultData();
    }

    private int readChoice() {
        String input = scanner.nextLine().trim();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    // Optional: Getter if needed by main menu
    public HousekeepingController getController() {
        return controller;
    }
}