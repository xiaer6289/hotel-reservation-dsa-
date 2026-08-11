package boundary;

import control.VipPriorityController;
import control.report.VipPriorityQueueRP;
import control.report.VipRoomReadinessRP;
import entity.Booking;
import entity.LoyaltyTier;
import entity.Member;
import entity.Room;
import entity.RoomType;
import entity.WalkInRegistration;
import java.util.Scanner;
import utility.Utility;

/**
 * Boundary for VIP & Loyalty Tier Priority Room Allocation.
 *
 * VIP registrations are added through RegistrationUI. This UI focuses on
 * viewing the MaxHeap, allocating a suitable room to its root member and
 * generating VIP management reports.
 *
 * @author Low Enn Toong
 */
public class VipAllocationUI {

    private final VipPriorityController controller;
    private final Scanner scanner;
    private final VipPriorityQueueRP priorityQueueReport;
    private final VipRoomReadinessRP roomReadinessReport;

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
        this.priorityQueueReport = new VipPriorityQueueRP();
        this.roomReadinessReport = new VipRoomReadinessRP();
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

                case 4:
                    generatePriorityQueueReport();
                    break;

                case 5:
                    generateRoomReadinessReport();
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
        System.out.println("4. VIP Priority Queue Analysis Report");
        System.out.println("5. VIP Room Allocation Readiness Report");
        System.out.println("0. Return to Main Menu");
    }

    private void allocateRoom() {
        Member nextMember = controller.peekNextVip();

        if (nextMember == null) {
            Utility.printError("No VIP registrations are waiting.");
            return;
        }

        Booking booking = controller.allocateNextVipBooking();

        if (booking == null) {
            WalkInRegistration registration
                    = nextMember.getRegistration();

            Utility.printError(
                    "No clean/ready room matches the requested room type "
                    + "and capacity. The VIP remains in the heap.");
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
        Room allocatedRoom = booking.getRoom();

        Utility.printSuccess("VIP room allocated and guest checked in successfully.");
        System.out.println(
                "Registration ID    : "
                + registration.getRegistrationId());
        System.out.println(
                "Confirmation No.   : "
                + booking.getConfirmationNo());
        System.out.println(
                "Member ID          : "
                + nextMember.getMemberId());
        System.out.println(
                "Guest ID           : "
                + nextMember.getGuest().getGuestId());
        System.out.println(
                "Guest Name         : "
                + nextMember.getName());
        System.out.println(
                "Loyalty Tier       : "
                + nextMember.getTier());
        System.out.println(
                "Requested Type     : "
                + registration.getRequestedRoomType());
        System.out.println(
                "Allocated Room     : "
                + allocatedRoom.getRoomNumber());
        System.out.println(
                "Allocated Type     : "
                + allocatedRoom.getRoomType());
        System.out.println(
                "Check-In Time      : "
                + allocatedRoom.getCheckInDateTime());
        System.out.println(
                "Expected Check-Out : "
                + allocatedRoom.getCheckOutDateTime());
        System.out.println(
                "Registration Status: "
                + registration.getStatus());
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

    private void generatePriorityQueueReport() {
        Member[] members = controller.getMembersByPriority();

        if (members.length == 0) {
            Utility.printError(
                    "No VIP registrations are waiting. Report cannot be generated.");
            return;
        }

        System.out.println("\n=== VIP PRIORITY QUEUE REPORT FILTERS ===");
        String keyword = readOptionalString(
                "Search Member ID / Registration ID / Guest ID / Name "
                + "(Enter for ALL): ");
        LoyaltyTier tierFilter = readTierFilter();
        String roomTypeFilter = readRoomTypeFilter();
        int minimumGuests = readNonNegativeInteger(
                "Minimum number of guests (0 for ALL): ");

        priorityQueueReport.generateReport(
                members,
                keyword,
                tierFilter,
                roomTypeFilter,
                minimumGuests);
    }

    private void generateRoomReadinessReport() {
        Member[] members = controller.getMembersByPriority();

        if (members.length == 0) {
            Utility.printError(
                    "No VIP registrations are waiting. Report cannot be generated.");
            return;
        }

        System.out.println("\n=== VIP ROOM READINESS REPORT FILTERS ===");
        String keyword = readOptionalString(
                "Search Member ID / Registration ID / Guest ID / Name "
                + "(Enter for ALL): ");
        LoyaltyTier tierFilter = readTierFilter();
        String roomTypeFilter = readRoomTypeFilter();
        String readinessFilter = readReadinessFilter();
        int minimumGuests = readNonNegativeInteger(
                "Minimum number of guests (0 for ALL): ");

        roomReadinessReport.generateReport(
                members,
                controller.getVacantRooms(),
                keyword,
                tierFilter,
                roomTypeFilter,
                readinessFilter,
                minimumGuests);
    }

    private LoyaltyTier readTierFilter() {
        LoyaltyTier[] tiers = LoyaltyTier.values();

        while (true) {
            System.out.println("\nFilter by Loyalty Tier:");
            System.out.println("0. ALL");

            for (int i = 0; i < tiers.length; i++) {
                System.out.println((i + 1) + ". " + tiers[i]);
            }

            int choice = readInteger("Choice: ");

            if (choice == 0) {
                return null;
            }

            if (choice >= 1 && choice <= tiers.length) {
                return tiers[choice - 1];
            }

            Utility.printError("Invalid loyalty tier filter.");
        }
    }

    private String readRoomTypeFilter() {
        RoomType[] roomTypes = RoomType.values();

        while (true) {
            System.out.println("\nFilter by Requested Room Type:");
            System.out.println("0. ALL");

            for (int i = 0; i < roomTypes.length; i++) {
                System.out.println(
                        (i + 1) + ". "
                        + formatRoomType(roomTypes[i].name()));
            }

            int choice = readInteger("Choice: ");

            if (choice == 0) {
                return null;
            }

            if (choice >= 1 && choice <= roomTypes.length) {
                return roomTypes[choice - 1].name();
            }

            Utility.printError("Invalid room type filter.");
        }
    }

    private String readReadinessFilter() {
        while (true) {
            System.out.println("\nFilter by Allocation Readiness:");
            System.out.println("0. ALL");
            System.out.println("1. MATCHED - suitable room is available");
            System.out.println("2. UNMATCHED - no suitable room is available");

            int choice = readInteger("Choice: ");

            switch (choice) {
                case 0:
                    return "ALL";
                case 1:
                    return "MATCHED";
                case 2:
                    return "UNMATCHED";
                default:
                    Utility.printError("Invalid readiness filter.");
                    break;
            }
        }
    }

    private String readOptionalString(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    private int readNonNegativeInteger(String message) {
        while (true) {
            int value = readInteger(message);

            if (value >= 0) {
                return value;
            }

            Utility.printError("Please enter 0 or a positive whole number.");
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

    private String formatRoomType(String roomType) {
        return roomType.replace('_', ' ');
    }
}