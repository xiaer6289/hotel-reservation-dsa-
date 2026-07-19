
/**
 *
 * @author Low Wei Shin
 */

import java.util.Scanner;

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
            System.out.println("\n=========================================");
            System.out.println("   TARUMT RESORTS - HOUSEKEEPING SYSTEM");
            System.out.println("=========================================");
            System.out.println("1. Log New Housekeeping Task");
            System.out.println("2. Update Task Status");
            System.out.println("3. Rollback Task Status");
            System.out.println("4. Search Task by Room Number");
            System.out.println("5. Generate Pending Tasks Report");
            System.out.println("6. Generate Daily Summary Report");
            System.out.println("7. Display All Tasks");
            System.out.println("0. Exit Module");
            System.out.println("=========================================");
            System.out.print("Enter your choice: ");

            choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

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
                    controller.generatePendingTasksReport();
                    break;
                case 6:
                    controller.generateDailySummary();
                    break;
                case 7:
                    System.out.println("\n=== ALL TASKS ===");
                    controller.getTaskLog().display();
                    break;
                case 0:
                    System.out.println("Exiting Housekeeping Module...");
                    break;
                default:
                    System.out.println("❌ Invalid choice! Please try again.");
            }
        } while (choice != 0);
    }

    private void logNewTask() {
        System.out.print("Enter Room Number: ");
        String room = scanner.nextLine();
        System.out.print("Enter Staff ID: ");
        String staffId = scanner.nextLine();
        controller.logNewTask(room, staffId);
    }

    private void updateTaskStatus() {
        System.out.print("Enter Task ID (e.g. T001): ");
        String taskId = scanner.nextLine();
        System.out.println("Available Statuses: Dirty, Cleaning In Progress, Inspected, Ready");
        System.out.print("Enter New Status: ");
        String status = scanner.nextLine();
        controller.updateTaskStatus(taskId, status);
    }

    private void rollbackTask() {
        System.out.print("Enter Task ID to rollback: ");
        String taskId = scanner.nextLine();
        controller.rollbackTask(taskId);
    }

    private void searchByRoom() {
        System.out.print("Enter Room Number: ");
        String room = scanner.nextLine();
        controller.searchByRoom(room);
    }

    // Optional: Getter if needed by main menu
    public HousekeepingController getController() {
        return controller;
    }
}