package boundary;

import control.VipPriorityController;
import java.util.Scanner;

public class VipAllocationUI {

    private VipPriorityController controller;
    private Scanner scanner;

    public VipAllocationUI() {
        controller = new VipPriorityController();
        scanner = new Scanner(System.in);
    }

    public void run() {
        int choice;
        do {
            System.out.println("\n=========================================");
            System.out.println("   VIP & LOYALTY TIER PRIORITY ALLOCATION");
            System.out.println("=========================================");
            System.out.println("1. Add VIP / Loyalty Member");
            System.out.println("2. Allocate Room to Next Highest Priority");
            System.out.println("3. View Priority Queue");
            System.out.println("4. View All Rooms Status");
            System.out.println("5. Mark Room as Dirty (Checkout)");
            System.out.println("6. Mark Room as Ready (After Housekeeping)");
            System.out.println("0. Back to Main Menu");
            System.out.print("Enter choice: ");

            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                choice = -1;
            }

            switch (choice) {
                case 1:
                    addMember();
                    break;
                case 2:
                    controller.allocateNextVipRoom();
                    break;
                case 3:
                    controller.displayPriorityQueue();
                    break;
                case 4:
                    controller.displayRooms();
                    break;
                case 5:
                    System.out.print("Enter Room Number: ");
                    controller.markRoomDirty(scanner.nextLine().trim());
                    break;
                case 6:
                    System.out.print("Enter Room Number: ");
                    controller.markRoomReady(scanner.nextLine().trim());
                    break;
                case 0:
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    System.out.println("Invalid choice! Please try again.");
            }
        } while (choice != 0);
    }

    private void addMember() {
        System.out.print("Member ID          : ");
        String id = scanner.nextLine().trim();
        System.out.print("Name               : ");
        String name = scanner.nextLine().trim();
        System.out.print("Tier (Platinum/Gold/Silver/Regular): ");
        String tier = scanner.nextLine().trim();
        System.out.print("Room Preference (Standard/Deluxe/Suite): ");
        String pref = scanner.nextLine().trim();

        controller.addVipMember(id, name, tier, pref);
    }
}