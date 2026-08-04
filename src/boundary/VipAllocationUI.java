package boundary;

import control.VipPriorityController;
import entity.LoyaltyTier;
import entity.Member;
import entity.Room;
import java.util.Scanner;
import utility.Utility;

/**
 *
 * @author Low Enn Toong
 */
public class VipAllocationUI {
    private final VipPriorityController controller;
    private final Scanner scanner;

    public VipAllocationUI() {
        controller = new VipPriorityController();
        scanner = new Scanner(System.in);
    }

    public void run() {
        int choice;
        do {
            Utility.clearScreen();
            displayMenu();
            choice = readInteger("Enter choice: ");
            switch (choice) {
                case 1:
                    addMember();
                    break;
                case 2:
                    allocateRoom();
                    break;
                case 3:
                    displayPriorityQueue();
                    break;
                case 4:
                    displayVacantRooms();
                    break;
                case 0:
                    break;
                default:
                    Utility.printError("Invalid choice. Please try again.");
                    break;
            }
            if (choice != 0) {
                Utility.pauseScreen();
            }
        } while (choice != 0);
    }

    private void displayMenu() {
        System.out.println("=========================================");
        System.out.println(" VIP & LOYALTY TIER PRIORITY ALLOCATION");
        System.out.println("=========================================");
        System.out.println("1. Add Existing Guest to VIP Queue");
        System.out.println("2. Allocate Vacant Room to Highest-Tier Member");
        System.out.println("3. View VIP Queue in Priority Order");
        System.out.println("4. View Vacant Rooms");
        System.out.println("0. Back to Main Menu");
    }

    private void addMember() {
        System.out.print("Member ID: ");
        String memberId = scanner.nextLine().trim();
        System.out.print("Existing Guest ID: ");
        String guestId = scanner.nextLine().trim();
        LoyaltyTier tier = readLoyaltyTier();
        if (tier == null) {
            Utility.printError("Invalid loyalty tier.");
            return;
        }
        int result = controller.addVipMember(memberId, guestId, tier);
        switch (result) {
            case VipPriorityController.ADD_SUCCESS:
                Utility.printSuccess("Member added to the VIP priority queue.");
                break;
            case VipPriorityController.GUEST_NOT_FOUND:
                Utility.printError("Guest ID does not exist.");
                break;
            case VipPriorityController.DUPLICATE_MEMBER_ID:
                Utility.printError("Member ID already exists in the queue.");
                break;
            case VipPriorityController.GUEST_ALREADY_QUEUED:
                Utility.printError("This guest is already in the VIP queue.");
                break;
            default:
                Utility.printError("All fields are required.");
                break;
        }
    }

    private void allocateRoom() {
        Member nextMember = controller.peekNextVip();
        if (nextMember == null) {
            Utility.printError("No VIP members are waiting.");
            return;
        }
        Room allocatedRoom = controller.allocateNextVipRoom();
        if (allocatedRoom == null) {
            Utility.printError("No vacant room is available. " + "The member remains in the queue.");
            return;
        }
        Utility.printSuccess("Room allocated successfully.");
        System.out.println("Member ID    : " + nextMember.getMemberId());
        System.out.println("Guest ID     : " + nextMember.getGuest().getGuestId());
        System.out.println("Guest Name   : " + nextMember.getName());
        System.out.println("Loyalty Tier : " + nextMember.getTier());
        System.out.println("Room Number  : " + allocatedRoom.getRoomNumber());
        System.out.println("Room Type    : " + allocatedRoom.getRoomType());
    }

    private void displayPriorityQueue() {
        Member[] members = controller.getMembersByPriority();
        if (members.length == 0) {
            Utility.printError("VIP priority queue is empty.");
            return;
        }
        System.out.println("=== VIP QUEUE: HIGHEST PRIORITY FIRST ===");
        for (Member member : members) {
            System.out.println(member);
        }
    }

    private void displayVacantRooms() {
        Room[] vacantRooms = controller.getVacantRooms();
        if (vacantRooms.length == 0) {
            Utility.printError("No vacant rooms are available.");
            return;
        }
        System.out.println("=== VACANT ROOMS ===");
        System.out.printf("%-8s %-18s %-8s%n", "Room", "Type", "Floor");
        for (Room room : vacantRooms) {
            System.out.printf("%-8s %-18s %-8s%n", room.getRoomNumber(), room.getRoomType(), room.getFloor());
        }
    }

    private LoyaltyTier readLoyaltyTier() {
        LoyaltyTier[] tiers = LoyaltyTier.values();
        System.out.println("Select Loyalty Tier:");
        for (int i = 0; i < tiers.length; i++) {
            System.out.println((i + 1) + ". " + tiers[i]);
        }
        int choice = readInteger("Choice: ");
        if (choice < 1 || choice > tiers.length) {
            return null;
        }
        return tiers[choice - 1];
    }

    private int readInteger(String message) {
        System.out.print(message);
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
}