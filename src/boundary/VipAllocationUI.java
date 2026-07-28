package boundary;

import control.VipAllocationControl;
import entity.LoyaltyTier;
import entity.RoomAllocation;
import entity.VipGuest;
import java.util.Scanner;

/**
 *
 * @author Low Enn Toong
 */
public class VipAllocationUI {
    private final Scanner scanner;
    private final VipAllocationControl control;

    public VipAllocationUI() {
        scanner = new Scanner(System.in);
        control = new VipAllocationControl();
    }

    public void run() {
        int choice;
        do {
            displayMenu();
            choice = readInteger("Enter choice: ");

            switch (choice) {
                case 1:
                    registerVipGuest();
                    break;
                case 2:
                    displayNextPriorityGuest();
                    break;
                case 3:
                    allocateRoom();
                    break;
                case 4:
                    displayWaitingList();
                    break;
                case 5:
                    searchGuest();
                    break;
                case 6:
                    generateWaitingListReport();
                    break;
                case 7:
                    generateAllocationSummaryReport();
                    break;
                case 8:
                    addSampleData();
                    break;
                case 0:
                    System.out.println("Returning to main menu...");
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        } while (choice != 0);
    }

    private void displayMenu() {
        System.out.println("\n=================================================");
        System.out.println(" VIP & LOYALTY TIER PRIORITY ROOM ALLOCATION");
        System.out.println("=================================================");
        System.out.println("1. Register VIP guest");
        System.out.println("2. View next priority guest");
        System.out.println("3. Allocate room to highest-priority guest");
        System.out.println("4. Display priority waiting list");
        System.out.println("5. Search guest by ID");
        System.out.println("6. Report 1 - Filtered VIP waiting list");
        System.out.println("7. Report 2 - Room allocation summary");
        System.out.println("8. Add sample data");
        System.out.println("0. Exit");
    }

    private void registerVipGuest() {
        System.out.print("Guest ID: ");
        String guestId = scanner.nextLine();

        System.out.print("Guest name: ");
        String guestName = scanner.nextLine();

        LoyaltyTier tier = readTier(false);

        System.out.print("Requested room type: ");
        String roomType = scanner.nextLine();

        boolean success = control.registerVipGuest(guestId, guestName, tier, roomType);
        if (success) {
            System.out.println("VIP guest registered successfully.");
        } else {
            System.out.println("Registration failed. Check empty values or duplicate guest ID.");
        }
    }

    private void displayNextPriorityGuest() {
        VipGuest guest = control.viewNextPriorityGuest();
        if (guest == null) {
            System.out.println("No VIP guest is currently waiting.");
            return;
        }

        printGuestHeader();
        System.out.println(guest);
    }

    private void allocateRoom() {
        System.out.print("Enter available room number: ");
        String roomNumber = scanner.nextLine();

        RoomAllocation allocation = control.allocateNextGuest(roomNumber);
        if (allocation == null) {
            System.out.println("Allocation failed. No waiting guest or invalid room number.");
            return;
        }

        System.out.println("Room allocated successfully:");
        printAllocationHeader();
        System.out.println(allocation);
    }

    private void displayWaitingList() {
        VipGuest[] guests = control.getWaitingGuestsSorted();
        if (guests.length == 0) {
            System.out.println("No VIP guest is waiting.");
            return;
        }

        printGuestHeader();
        for (VipGuest guest : guests) {
            System.out.println(guest);
        }
    }

    private void searchGuest() {
        System.out.print("Enter guest ID: ");
        String guestId = scanner.nextLine();

        VipGuest guest = control.searchWaitingGuestById(guestId);
        String status = "WAITING";

        if (guest == null) {
            guest = control.searchAllocatedGuestById(guestId);
            status = "ALLOCATED";
        }

        if (guest == null) {
            System.out.println("Guest not found.");
            return;
        }

        System.out.println("Status: " + status);
        printGuestHeader();
        System.out.println(guest);
    }

    private void generateWaitingListReport() {
        System.out.println("\n--- REPORT 1: FILTERED VIP WAITING LIST ---");
        LoyaltyTier tier = readTier(true);

        System.out.print("Room type filter (press Enter for all): ");
        String roomType = scanner.nextLine();

        VipGuest[] guests = control.filterWaitingGuests(tier, roomType);
        System.out.println("Filter tier: " + (tier == null ? "ALL" : tier));
        System.out.println("Filter room type: " + (roomType.isBlank() ? "ALL" : roomType.toUpperCase()));
        System.out.println("Matching guests: " + guests.length);

        if (guests.length > 0) {
            printGuestHeader();
            for (VipGuest guest : guests) {
                System.out.println(guest);
            }
        }

        System.out.println("\nWaiting count by tier:");
        for (LoyaltyTier loyaltyTier : LoyaltyTier.values()) {
            System.out.printf("%-10s : %d%n",
                    loyaltyTier,
                    control.countWaitingByTier(loyaltyTier));
        }
    }

    private void generateAllocationSummaryReport() {
        System.out.println("\n--- REPORT 2: ROOM ALLOCATION SUMMARY ---");
        System.out.println("Total waiting guests   : " + control.getWaitingCount());
        System.out.println("Total allocated guests : " + control.getAllocationCount());
        System.out.printf("Average waiting time   : %.2f minutes%n",
                control.getAverageWaitingMinutes());

        System.out.println("\nAllocated count by tier:");
        for (LoyaltyTier tier : LoyaltyTier.values()) {
            int count = control.countAllocatedByTier(tier);
            double percentage = control.getAllocationCount() == 0
                    ? 0
                    : (count * 100.0 / control.getAllocationCount());
            System.out.printf("%-10s : %3d (%6.2f%%)%n", tier, count, percentage);
        }

        RoomAllocation[] allocations = control.getAllocationsSortedByLatest();
        if (allocations.length > 0) {
            System.out.println("\nAllocation records (latest first):");
            printAllocationHeader();
            for (RoomAllocation allocation : allocations) {
                System.out.println(allocation);
            }
        }
    }

    private void addSampleData() {
        control.addSampleData();
        System.out.println("Sample data added. Duplicate sample IDs were skipped.");
    }

    private LoyaltyTier readTier(boolean allowAll) {
        while (true) {
            if (allowAll) {
                System.out.println("0. All tiers");
            }
            System.out.println("1. Elite");
            System.out.println("2. Diamond");
            System.out.println("3. Platinum");

            int choice = readInteger("Select loyalty tier: ");
            if (allowAll && choice == 0) {
                return null;
            }

            switch (choice) {
                case 1:
                    return LoyaltyTier.ELITE;
                case 2:
                    return LoyaltyTier.DIAMOND;
                case 3:
                    return LoyaltyTier.PLATINUM;
                default:
                    System.out.println("Invalid tier selection.");
            }
        }
    }

    private int readInteger(String message) {
        while (true) {
            System.out.print(message);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private void printGuestHeader() {
        System.out.printf("%-8s %-20s %-10s %-15s %-16s%n",
                "ID", "GUEST NAME", "TIER", "ROOM TYPE", "REQUEST TIME");
        System.out.println("----------------------------------------------------------------------------");
    }

    private void printAllocationHeader() {
        System.out.printf("%-8s %-8s %-20s %-10s %-10s %-16s%n",
                "ALLOC ID", "GUEST ID", "GUEST NAME", "TIER", "ROOM", "ALLOCATED TIME");
        System.out.println("------------------------------------------------------------------------------------");
    }
}