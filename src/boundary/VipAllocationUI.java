package boundary;

import control.VipPriorityController;
import entity.Booking;
import entity.Member;
import entity.Room;
import entity.WalkInRegistration;
import java.util.Scanner;
import utility.Utility;

/**
 * Boundary for VIP & Loyalty Tier Priority Room Allocation.
 *
 * VIP registrations are added through RegistrationUI. This UI focuses only on
 * viewing the MaxHeap and allocating a suitable room to its root member.
 *
 * @author Low Enn Toong
 */
public class VipAllocationUI {

    private final VipPriorityController controller;
    private final Scanner scanner;

    public VipAllocationUI() {
        this(new VipPriorityController(), new Scanner(System.in));
    }

    public VipAllocationUI(VipPriorityController controller) {
        this(controller, new Scanner(System.in));
    }

    public VipAllocationUI(
            VipPriorityController controller,
            Scanner scanner) {

        this.controller = controller;
        this.scanner = scanner;
    }

    public void run() {
        int choice;

        do {
            Utility.clearScreen();
            displayMenu();
            choice = readInteger("Enter choice: ");

            switch (choice) {
                case 1:
                    allocateRoom();
                    break;

                case 2:
                    displayPriorityQueue();
                    break;

                case 3:
                    displayVacantRooms();
                    break;

                case 0:
                    System.out.println("Returning to Main Menu...");
                    break;

                default:
                    Utility.printError(
                            "Invalid choice. Please try again.");
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
        System.out.println("1. Allocate Room to Highest-Priority VIP");
        System.out.println("2. View VIP Heap in Priority Order");
        System.out.println("3. View Vacant Rooms");
        System.out.println("0. Return to Main Menu");
    }

    private void allocateRoom() {
        Member nextMember = controller.peekNextVip();

        if (nextMember == null) {
            Utility.printError("No VIP registrations are waiting.");
            return;
        }

        Room allocatedRoom = controller.allocateNextVipRoom();

        if (allocatedRoom == null) {
            WalkInRegistration registration
                    = nextMember.getRegistration();

            Utility.printError(
                    "No suitable vacant room matches the requested room type and capacity."
                    + " The VIP remains in the heap.");
            System.out.println(
                    "Requested Room Type : "
                    + registration.getRequestedRoomType());
            System.out.println(
                    "Number of Guests    : "
                    + registration.getNumberOfGuests());
            return;
        }

        WalkInRegistration registration
                = nextMember.getRegistration();
        Booking booking = controller.getLastCreatedBooking();

        Utility.printSuccess("VIP room allocated successfully.");
        System.out.println(
                "Registration ID   : "
                + registration.getRegistrationId());
        System.out.println(
                "Member ID         : "
                + nextMember.getMemberId());
        System.out.println(
                "Guest ID          : "
                + nextMember.getGuest().getGuestId());
        System.out.println(
                "Guest Name        : "
                + nextMember.getName());
        System.out.println(
                "Loyalty Tier      : "
                + nextMember.getTier());
        System.out.println(
                "Requested Type    : "
                + registration.getRequestedRoomType());
        System.out.println(
                "Allocated Room    : "
                + allocatedRoom.getRoomNumber());
        System.out.println(
                "Allocated Type    : "
                + allocatedRoom.getRoomType());
        System.out.println(
                "Registration Status: "
                + registration.getStatus());

        if (booking != null) {
            System.out.println(
                    "Confirmation No. : "
                    + booking.getConfirmationNo());
            System.out.printf(
                    "Pending Payment  : RM %.2f%n",
                    booking.getPayment().getAmount());
        }

        System.out.println(
                "VIP Members Waiting: "
                + controller.getWaitingCount());
    }

    private void displayPriorityQueue() {
        Member[] members = controller.getMembersByPriority();

        if (members.length == 0) {
            Utility.printError("VIP priority heap is empty.");
            return;
        }

        System.out.println(
                "=== VIP HEAP: HIGHEST PRIORITY FIRST ===");

        for (int i = 0; i < members.length; i++) {
            System.out.println((i + 1) + ". " + members[i]);
        }
    }

    private void displayVacantRooms() {
        Room[] vacantRooms = controller.getVacantRooms();

        if (vacantRooms.length == 0) {
            Utility.printError("No vacant rooms are available.");
            return;
        }

        System.out.println("=== VACANT ROOMS ===");
        System.out.printf(
                "%-8s %-18s %-8s %-10s%n",
                "Room",
                "Type",
                "Floor",
                "Capacity");

        for (Room room : vacantRooms) {
            System.out.printf(
                    "%-8s %-18s %-8s %-10d%n",
                    room.getRoomNumber(),
                    room.getRoomType(),
                    room.getFloor(),
                    room.getNoOfGuest());
        }
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