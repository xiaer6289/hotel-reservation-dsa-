
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
        while (true) {
            System.out.println("\n=== Housekeeping Task Log ===");
            System.out.println("1. Log New Task");
            System.out.println("2. Update Task Status");
            System.out.println("3. Rollback Task");
            System.out.println("4. View Pending Tasks");
            System.out.println("5. Generate Daily Summary");
            System.out.println("0. Exit");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    /* prompt for details and call controller */ break;
                case 4:
                    controller.generatePendingTasksReport();
                    break;
                // etc.
            }
        }
    }
}
