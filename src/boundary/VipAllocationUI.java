package boundary;

import control.VipPriorityController;
import control.report.VipRoomAllocationDemandRP;
import control.report.VipTierWaitingPerformanceRP;
import entity.Booking;
import entity.Guest;
import entity.LoyaltyProfile;
import entity.LoyaltyTier;
import entity.Room;
import entity.RoomType;
import entity.WalkInRegistration;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import utility.Utility;

/**
 * Boundary for VIP & Loyalty Tier Priority Room Allocation.
 *
 * VIP creation is automatic through Walk-In Registration when a guest meets
 * the loyalty requirement. This menu focuses on MaxHeap priority viewing,
 * VIP waiting-request maintenance, room readiness and priority allocation.
 *
 * @author Low Enn Toong
 */
public class VipAllocationUI {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final VipPriorityController controller;
    private final Scanner scanner;
    private final VipTierWaitingPerformanceRP waitingPerformanceReport;
    private final VipRoomAllocationDemandRP roomDemandReport;

    public VipAllocationUI() {
        this(new VipPriorityController(), new Scanner(System.in));
    }

    public VipAllocationUI(VipPriorityController controller) {
        this(controller, new Scanner(System.in));
    }

    public VipAllocationUI(VipPriorityController controller, Scanner scanner) {
        this.controller = controller;
        this.scanner = scanner;
        this.waitingPerformanceReport = new VipTierWaitingPerformanceRP();
        this.roomDemandReport = new VipRoomAllocationDemandRP();
    }

    public void run() {
        int choice;

        do {
            Utility.clearScreen();
            displayMenu();
            choice = readMenuChoice("Enter choice (0-10): ", 0, 10);

            switch (choice) {
                case 1:
                    displayAllVipGuests();
                    break;
                case 2:
                    displayNextHighestPriorityVip();
                    break;
                case 3:
                    displayPriorityQueue();
                    break;
                case 4:
                    searchVipRegistration();
                    break;
                case 5:
                    updateVipRoomRequest();
                    break;
                case 6:
                    cancelVipRegistration();
                    break;
                case 7:
                    displayReadyRooms();
                    break;
                case 8:
                    allocateRoom();
                    break;
                case 9:
                    generateWaitingPerformanceReport();
                    break;
                case 10:
                    generateRoomDemandReport();
                    break;
                case 0:
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    break;
            }

            if (choice != 0) {
                Utility.pauseScreen();
            }

        } while (choice != 0);
    }

    private void displayMenu() {
        System.out.println("==============================================");
        System.out.println(" VIP & LOYALTY TIER PRIORITY ROOM ALLOCATION");
        System.out.println("==============================================");
        System.out.println("VIP INFORMATION");
        System.out.println("1. View All VIP Guests");
        System.out.println("2. View Next VIP in Priority");
        System.out.println("3. View VIP Waiting List");
        System.out.println();
        System.out.println("VIP WAITING REGISTRATION");
        System.out.println("4. Find VIP Waiting Registration");
        System.out.println("5. Update VIP Room Request");
        System.out.println("6. Cancel VIP Waiting Registration");
        System.out.println();
        System.out.println("ROOM ASSIGNMENT");
        System.out.println("7. View Ready Rooms");
        System.out.println("8. Assign Room & Check In VIP");
        System.out.println();
        System.out.println("REPORTS");
        System.out.println("9. VIP Tier & Waiting Performance Analysis");
        System.out.println("10. VIP Room Allocation Demand Analysis");
        System.out.println("0. Return to Main Menu");
    }

    private void displayAllVipGuests() {
        System.out.println("\n===== ALL VIP GUESTS =====");
        System.out.println("VIP guests are guests who currently hold an ELITE, PLATINUM or DIAMOND loyalty tier.");

        LoyaltyProfile[] profiles = controller.getAllVipProfiles();

        if (profiles.length == 0) {
            Utility.printError("No VIP guests were found.");
            return;
        }

        System.out.printf(
                "%-4s %-10s %-20s %-15s %-16s %-10s%n",
                "No.", "Guest ID", "Guest Name", "Phone", "Completed Stays", "Tier");
        System.out.println("----------------------------------------------------------------------------------");

        int displayed = 0;
        for (LoyaltyProfile profile : profiles) {
            Guest guest = controller.findGuestById(profile.getGuestId());

            if (guest == null) {
                continue;
            }

            displayed++;
            System.out.printf(
                    "%-4d %-10s %-20s %-15s %-16d %-10s%n",
                    displayed,
                    guest.getGuestId(),
                    shorten(guest.getName(), 20),
                    guest.getPhoneNo(),
                    profile.getCompletedStays(),
                    profile.getTier());
        }

        if (displayed == 0) {
            Utility.printError("VIP loyalty profiles exist, but no matching guest records were found.");
            return;
        }

        System.out.println("\nTotal VIP Guests: " + displayed);
        System.out.println("Note: This list shows all VIP guests, not only VIPs currently waiting for a room.");
    }

    private void displayNextHighestPriorityVip() {
        System.out.println("\n===== NEXT VIP IN PRIORITY =====");

        WalkInRegistration registration = controller.peekNextVip();

        if (registration == null) {
            Utility.printError("No VIP registrations are currently waiting.");
            return;
        }

        displayVipRegistrationDetails(registration);

        Room suitableRoom = controller.findReadyRoomForRegistration(registration);
        if (suitableRoom == null) {
            System.out.println("Allocation Readiness: WAITING - no suitable ready room currently available");
        } else {
            System.out.println("Allocation Readiness: READY");
            System.out.println("Suggested Room      : " + suitableRoom.getRoomNumber());
        }
    }

    private void displayPriorityQueue() {
        WalkInRegistration[] registrations = controller.getVipRegistrationsByPriority();

        if (registrations.length == 0) {
            Utility.printError("No VIP guests are currently waiting for a room.");
            return;
        }

        System.out.println("\n=== VIP WAITING LIST: HIGHEST PRIORITY FIRST ===");
        System.out.printf(
                "%-4s %-7s %-10s %-18s %-10s %-17s %-6s%n",
                "No.", "Reg ID", "Guest ID", "Guest Name", "Tier", "Requested Room", "Party");
        System.out
                .println("---------------------------------------------------------------------------------");

        for (int i = 0; i < registrations.length; i++) {
            WalkInRegistration registration = registrations[i];
            System.out.printf(
                    "%-4d %-7s %-10s %-18s %-10s %-17s %-6d%n",
                    i + 1,
                    registration.getRegistrationId(),
                    registration.getGuest().getGuestId(),
                    shorten(registration.getGuest().getName(), 18),
                    controller.getLoyaltyTier(registration),
                    formatRoomType(registration.getRequestedRoomType()),
                    registration.getNumberOfGuests());
        }

        System.out.println(
                "\nNext VIP in Priority: " + registrations[0].getRegistrationId()
                + " / " + registrations[0].getGuest().getGuestId() + " (" + controller.getLoyaltyTier(registrations[0]) + ")");
        System.out.println("Priority order: DIAMOND first, followed by PLATINUM, then ELITE.");
    }

    private void searchVipRegistration() {
        System.out.println("\n===== FIND VIP WAITING REGISTRATION =====");
        System.out.println("Hint: VIP registration IDs use the format R followed by 4 digits, e.g. R0001.");
        System.out.println("Enter 0 to return without searching.");

        String registrationId = readRegistrationIdOrCancel(
                "Enter VIP Registration ID (e.g. R0001) or 0 to return: ");

        if (registrationId == null) {
            System.out.println("Search cancelled. Returning to VIP menu.");
            return;
        }

        WalkInRegistration registration = controller.findWaitingVipRegistrationById(registrationId);

        if (registration == null) {
            Utility.printError("No VIP_WAITING registration was found for " + registrationId.toUpperCase() + ".");
            return;
        }

        Utility.printSuccess("VIP registration found.");
        displayVipRegistrationDetails(registration);
    }

    private void updateVipRoomRequest() {
        System.out.println("\n===== UPDATE VIP ROOM REQUEST =====");
        System.out.println("Only a VIP registration that is still waiting can be updated.");
        System.out.println("Loyalty tier and priority cannot be edited manually.");
        System.out.println("Hint: Registration ID example = R0001.");
        System.out.println("Enter 0 to cancel and return to the VIP menu.");

        String registrationId = readRegistrationIdOrCancel(
                "Enter VIP Registration ID to update (e.g. R0001) or 0 to cancel: ");

        if (registrationId == null) {
            System.out.println("Update cancelled. No changes were made.");
            return;
        }

        WalkInRegistration registration = controller.findWaitingVipRegistrationById(registrationId);

        if (registration == null) {
            Utility.printError("No VIP_WAITING registration was found for " + registrationId.toUpperCase() + ".");
            return;
        }

        System.out.println("\nCurrent VIP Request:");
        displayVipRegistrationDetails(registration);

        System.out.println("\nSelect the field to update:");
        System.out.println("1. Requested Room Type");
        System.out.println("2. Number of Guests");
        System.out.println("3. Expected Check-Out Date & Time");
        System.out.println("4. Update All Request Fields");
        System.out.println("0. Cancel Update");

        int choice = readMenuChoice("Enter update choice (0-4): ", 0, 4);

        if (choice == 0) {
            System.out.println("Update cancelled. No changes were saved.");
            return;
        }

        String newRoomType = registration.getRequestedRoomType();
        int newGuestCount = registration.getNumberOfGuests();
        LocalDateTime newCheckOut = registration.getCheckOutDateTime();

        if (choice == 1 || choice == 4) {
            newRoomType = readRoomType("Select new Requested Room Type");

            int maximumCapacity = controller.getMaximumCapacityForRoomType(newRoomType);
            if (choice == 1 && newGuestCount > maximumCapacity) {
                System.out.println("Current party size (" + newGuestCount + ") exceeds the maximum capacity of "
                        + formatRoomType(newRoomType) + " (" + maximumCapacity + ").");
                newGuestCount = readGuestCountForRoomType(newRoomType);
            }
        }

        if (choice == 2 || choice == 4) {
            newGuestCount = readGuestCountForRoomType(newRoomType);
        }

        if (choice == 3 || choice == 4) {
            newCheckOut = readFutureCheckOutDateTime(registration);
        }

        System.out.println("\nProposed Updated Request:");
        System.out.println("Requested Room Type : " + formatRoomType(newRoomType));
        System.out.println("Number of Guests    : " + newGuestCount);
        System.out.println("Expected Check-Out  : " + formatDateTime(newCheckOut));

        boolean confirm = readYesNo("Save these changes? (Y/N, e.g. Y): ");

        if (!confirm) {
            System.out.println("Update cancelled. No changes were saved.");
            return;
        }

        boolean updated = controller.updateVipRegistrationRequest(registrationId, newRoomType, newGuestCount,
                newCheckOut);

        if (!updated) {
            Utility.printError("Unable to update the VIP request. Check room capacity, status and check-out time.");
            return;
        }

        Utility.printSuccess("VIP room request updated successfully.");
        WalkInRegistration updatedRegistration = controller.findWaitingVipRegistrationById(registrationId);
        if (updatedRegistration != null) {
            displayVipRegistrationDetails(updatedRegistration);
        }
        System.out.println("Priority remains controlled automatically by loyalty tier and registration time.");
    }

    private void cancelVipRegistration() {
        System.out.println("\n===== CANCEL VIP WAITING REGISTRATION =====");
        System.out.println("Hint: Enter a waiting VIP registration ID such as R0001.");
        System.out.println("Enter 0 to return without cancelling any registration.");

        String registrationId = readRegistrationIdOrCancel(
                "Enter VIP Registration ID to cancel (e.g. R0001) or 0 to return: ");

        if (registrationId == null) {
            System.out.println("Cancellation action exited. No registration was changed.");
            return;
        }

        WalkInRegistration registration = controller.findWaitingVipRegistrationById(registrationId);

        if (registration == null) {
            Utility.printError("No VIP_WAITING registration was found for " + registrationId.toUpperCase() + ".");
            return;
        }

        System.out.println("\nVIP Registration to Cancel:");
        displayVipRegistrationDetails(registration);

        boolean confirm = readYesNo("Confirm cancellation? (Y/N, e.g. N): ");

        if (!confirm) {
            System.out.println("Cancellation aborted. The VIP remains in the waiting list.");
            return;
        }

        WalkInRegistration cancelled = controller.cancelVipRegistrationById(registrationId);

        if (cancelled == null) {
            Utility.printError("Unable to cancel the VIP registration.");
            return;
        }

        Utility.printSuccess("VIP waiting registration cancelled successfully.");
        System.out.println("Registration ID : " + cancelled.getRegistrationId());
        System.out.println("Guest Name      : " + cancelled.getGuest().getName());
        System.out.println("Status          : " + cancelled.getStatus());
        System.out.println("VIPs Still Waiting: " + controller.getWaitingCount());
    }

    private void displayReadyRooms() {
        Room[] readyRooms = controller.getVacantRooms();

        if (readyRooms.length == 0) {
            Utility.printError("No clean/ready rooms are currently available for assignment.");
            return;
        }

        System.out.println("\n=== READY ROOMS ===");
        System.out.printf(
                "%-8s %-18s %-8s %-10s %-12s%n",
                "Room", "Type", "Floor", "Capacity", "Status");
        System.out.println("--------------------------------------------------------------");

        for (Room room : readyRooms) {
            System.out.printf(
                    "%-8s %-18s %-8s %-10d %-12s%n",
                    room.getRoomNumber(),
                    formatRoomType(room.getRoomType()),
                    room.getFloor(),
                    room.getNoOfGuest(),
                    room.getStatusLabel());
        }
    }

    private void allocateRoom() {
        System.out.println("\n===== ASSIGN ROOM TO NEXT SUITABLE VIP =====");

        if (!controller.hasWaitingVip()) {
            Utility.printError("No VIP registrations are waiting.");
            return;
        }

        WalkInRegistration heapRoot = controller.peekNextVip();
        WalkInRegistration nextRegistration = controller.peekNextAllocatableVip();

        if (nextRegistration == null) {
            Utility.printError(
                    "VIP registrations are waiting, but none currently has a clean/ready room matching the requested room type and capacity.");
            System.out.println("All VIPs remain in the waiting list with their original priority.");
            return;
        }

        if (heapRoot != null && heapRoot != nextRegistration) {
            System.out.println("Highest-priority VIP registration "
                    + heapRoot.getRegistrationId()
                    + " (Guest " + heapRoot.getGuest().getGuestId() + ")"
                    + " cannot currently be matched to a suitable ready room.");
            System.out.println(
                    "The system will serve the next highest-priority VIP who can use a ready room.");
        }

        System.out.println("\nVIP Selected for Room Assignment:");
        displayVipRegistrationDetails(nextRegistration);

        Room suggestedRoom = controller.findReadyRoomForRegistration(nextRegistration);
        if (suggestedRoom != null) {
            System.out.println("Suggested Ready Room: " + suggestedRoom.getRoomNumber());
        }

        boolean confirm = readYesNo("Proceed with this room assignment? (Y/N, e.g. Y): ");

        if (!confirm) {
            System.out.println("Room assignment cancelled. The VIP remains in the waiting list.");
            return;
        }

        Booking booking = controller.allocateNextVipBooking();

        if (booking == null) {
            Utility.printError("Unable to complete room assignment. Waiting VIPs remain in the waiting list.");
            return;
        }

        WalkInRegistration registration = nextRegistration;
        Room allocatedRoom = booking.getRoom();

        Utility.printSuccess("VIP room allocated and guest checked in successfully.");
        System.out.println("Registration ID    : " + registration.getRegistrationId());
        System.out.println("Confirmation No.   : " + booking.getConfirmationNo());
        System.out.println("Guest ID           : " + nextRegistration.getGuest().getGuestId());
        System.out.println("Guest Name         : " + nextRegistration.getGuest().getName());
        System.out.println("Loyalty Tier       : " + controller.getLoyaltyTier(nextRegistration));
        System.out.println("Requested Type     : " + formatRoomType(registration.getRequestedRoomType()));
        System.out.println("Allocated Room     : " + allocatedRoom.getRoomNumber());
        System.out.println("Allocated Type     : " + formatRoomType(allocatedRoom.getRoomType()));
        System.out.println("Check-In Time      : " + formatDateTime(allocatedRoom.getCheckInDateTime()));
        System.out.println("Expected Check-Out : " + formatDateTime(allocatedRoom.getCheckOutDateTime()));
        System.out.println("Registration Status: " + registration.getStatus());
        System.out.println("VIP Members Waiting: " + controller.getWaitingCount());
    }

    private void generateWaitingPerformanceReport() {
        WalkInRegistration[] registrations = controller.getVipRegistrationsByPriority();

        if (registrations.length == 0) {
            Utility.printError("No VIP registrations are waiting. Report cannot be generated.");
            return;
        }

        System.out.println("\n=== VIP TIER & WAITING PERFORMANCE ANALYSIS FILTERS ===");
        System.out.println("This report summarizes waiting performance instead of repeating the normal VIP waiting list.");
        String keyword = readOptionalString("Search keyword (e.g. R0001, G0001 or guest name; Enter for ALL): ");
        LoyaltyTier tierFilter = readTierFilter();
        String roomTypeFilter = readRoomTypeFilter();
        int minimumWaitingMinutes = readNonNegativeInteger("Minimum waiting time in minutes (0 = ALL, e.g. 15): ");
        int minimumGuests = readNonNegativeInteger("Minimum party size (0 = ALL, e.g. 2): ");

        waitingPerformanceReport.generateReport(
                registrations,
                controller.getAllVipProfiles(),
                keyword,
                tierFilter,
                roomTypeFilter,
                minimumWaitingMinutes,
                minimumGuests);
    }

    private void generateRoomDemandReport() {
        WalkInRegistration[] registrations = controller.getVipRegistrationsByPriority();

        if (registrations.length == 0) {
            Utility.printError("No VIP registrations are waiting. Report cannot be generated.");
            return;
        }

        System.out.println("\n=== VIP ROOM ALLOCATION DEMAND ANALYSIS FILTERS ===");
        System.out.println("This report compares current VIP room demand with suitable ready-room supply.");
        String keyword = readOptionalString("Search keyword (e.g. R0001, G0001 or guest name; Enter for ALL): ");
        LoyaltyTier tierFilter = readTierFilter();
        String roomTypeFilter = readRoomTypeFilter();
        String readinessFilter = readAllocationStatusFilter();
        int minimumGuests = readNonNegativeInteger("Minimum party size (0 = ALL, e.g. 2): ");

        roomDemandReport.generateReport(
                registrations,
                controller.getAllVipProfiles(),
                controller.getVacantRooms(),
                keyword,
                tierFilter,
                roomTypeFilter,
                readinessFilter,
                minimumGuests);
    }

    private void displayVipRegistrationDetails(WalkInRegistration registration) {
        LoyaltyTier tier = controller.getLoyaltyTier(registration);

        System.out.println("Registration ID    : " + registration.getRegistrationId());
        System.out.println("Guest ID           : " + registration.getGuest().getGuestId());
        System.out.println("Guest Name         : " + registration.getGuest().getName());
        System.out.println("Phone Number       : " + registration.getGuest().getPhoneNo());
        System.out.println("Loyalty Tier       : " + tier);
        System.out.println("Priority Score     : " + controller.getPriorityScore(registration));
        System.out.println("Requested Room Type: " + formatRoomType(registration.getRequestedRoomType()));
        System.out.println("Number of Guests   : " + registration.getNumberOfGuests());
        System.out.println("Registered At      : " + formatDateTime(registration.getRegistrationTime()));
        System.out.println("Expected Check-Out : " + formatDateTime(registration.getCheckOutDateTime()));
        System.out.println("Status             : " + registration.getStatus());
    }

    private LoyaltyTier readTierFilter() {
        LoyaltyTier[] tiers = LoyaltyTier.values();

        while (true) {
            System.out.println("\nFilter by Loyalty Tier:");
            System.out.println("0. ALL");

            for (int i = 0; i < tiers.length; i++) {
                System.out.println((i + 1) + ". " + tiers[i]);
            }

            int choice = readInteger("Enter tier filter choice (0-" + tiers.length + "): ");

            if (choice == 0) {
                return null;
            }

            if (choice >= 1 && choice <= tiers.length) {
                return tiers[choice - 1];
            }

            Utility.printError("Invalid loyalty tier filter. Choose a listed number.");
        }
    }

    private String readRoomTypeFilter() {
        RoomType[] roomTypes = RoomType.values();

        while (true) {
            System.out.println("\nFilter by Requested Room Type:");
            System.out.println("0. ALL");

            for (int i = 0; i < roomTypes.length; i++) {
                System.out.println((i + 1) + ". " + formatRoomType(roomTypes[i].name()));
            }

            int choice = readInteger("Enter room type filter choice (0-" + roomTypes.length + "): ");

            if (choice == 0) {
                return null;
            }

            if (choice >= 1 && choice <= roomTypes.length) {
                return roomTypes[choice - 1].name();
            }

            Utility.printError("Invalid room type filter. Choose a listed number.");
        }
    }

    private String readAllocationStatusFilter() {
        while (true) {
            System.out.println("\nFilter by Allocation Status:");
            System.out.println("0. ALL");
            System.out.println("1. READY - at least one suitable ready room is available");
            System.out.println("2. BLOCKED - no suitable ready room is available");

            int choice = readInteger("Enter allocation-status filter choice (0-2): ");

            switch (choice) {
                case 0:
                    return "ALL";
                case 1:
                    return "READY";
                case 2:
                    return "BLOCKED";
                default:
                    Utility.printError("Invalid allocation-status filter. Enter 0, 1 or 2.");
                    break;
            }
        }
    }

    private String readRoomType(String heading) {
        RoomType[] roomTypes = RoomType.values();

        while (true) {
            System.out.println("\n" + heading + ":");

            for (int i = 0; i < roomTypes.length; i++) {
                int maximumCapacity = controller.getMaximumCapacityForRoomType(roomTypes[i].name());
                System.out.printf(
                        "%d. %-16s | RM %.2f/day | Max Capacity: %d%n",
                        i + 1, formatRoomType(roomTypes[i].name()), roomTypes[i].getPricePerDay(), maximumCapacity);
            }

            int choice = readInteger("Enter room type number (1-" + roomTypes.length + ", e.g. 1): ");

            if (choice >= 1 && choice <= roomTypes.length) {
                return roomTypes[choice - 1].name();
            }

            Utility.printError("Invalid room type selection. Choose a listed number.");
        }
    }

    private int readGuestCountForRoomType(String roomType) {
        int maximumCapacity = controller.getMaximumCapacityForRoomType(roomType);

        while (true) {
            int guests = readInteger("Enter number of guests for " + formatRoomType(roomType) + " (1-" + maximumCapacity + ", e.g. 2): ");

            if (guests >= 1 && guests <= maximumCapacity) {
                return guests;
            }

            Utility.printError("Guest count must be between 1 and " + maximumCapacity + " for " + formatRoomType(roomType) + ".");
        }
    }

    private LocalDateTime readFutureCheckOutDateTime(WalkInRegistration registration) {
        while (true) {
            LocalDateTime value = readDateTimeParts("New Expected Check-Out");
            LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
            LocalDateTime registrationTime = registration.getRegistrationTime();

            if (!value.isAfter(now)) {
                Utility.printError("Expected check-out must be in the future.");
                continue;
            }

            if (registrationTime != null && !value.isAfter(registrationTime)) {
                Utility.printError("Expected check-out must be after the registration/arrival time.");
                continue;
            }

            return value;
        }
    }

    private LocalDateTime readDateTimeParts(String label) {
        while (true) {
            System.out.println("\nEnter " + label + " Date and Time");
            System.out.println("Hint example: 2026-08-15 12:00");

            int year = readInteger("Enter Year (yyyy, e.g. 2026): ");
            int month = readInteger("Enter Month (1-12, e.g. 8): ");
            int day = readInteger("Enter Day (1-31, e.g. 15): ");
            int hour = readInteger("Enter Hour (0-23, e.g. 12): ");
            int minute = readInteger("Enter Minute (0-59, e.g. 0): ");

            try {
                return LocalDateTime.of(year, month, day, hour, minute);
            } catch (DateTimeException exception) {
                Utility.printError("Invalid date/time combination. Please enter a real calendar date and valid time.");
            }
        }
    }

    private String readRegistrationIdOrCancel(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim().toUpperCase();

            if (input.equals("0")) {
                return null;
            }

            if (input.isEmpty()) {
                Utility.printError("Input cannot be empty. Enter a Registration ID or 0 to return.");
                continue;
            }

            if (Utility.isValidRegistrationId(input)) {
                return input;
            }

            Utility.printError(
                    "Invalid Registration ID. Use R followed by 4 digits (e.g. R0001), or enter 0 to return.");
        }
    }

    private boolean readYesNo(String message) {
        while (true) {
            String input = readNonEmptyString(message).toUpperCase();

            if (!Utility.isValidYesNo(input)) {
                Utility.printError("Please enter Y for Yes or N for No.");
                continue;
            }

            return input.equals("Y");
        }
    }

    private String readNonEmptyString(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();

            if (!input.isEmpty()) {
                return input;
            }

            Utility.printError("Input cannot be empty. Please enter the requested value.");
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

    private int readMenuChoice(String message, int minimum, int maximum) {
        while (true) {
            int choice = readInteger(message);

            if (choice >= minimum && choice <= maximum) {
                return choice;
            }

            Utility.printError(
                    "Invalid choice. Enter a number from " + minimum + " to " + maximum + ".");
        }
    }

    private int readInteger(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                Utility.printError("Input cannot be empty. Enter a whole number.");
                continue;
            }

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException exception) {
                Utility.printError("Invalid number. Enter digits only (e.g. 2).");
            }
        }
    }

    private String formatRoomType(String roomType) {
        return roomType == null ? "-" : roomType.replace('_', ' ');
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE_TIME_FORMAT);
    }

    private String shorten(String value, int maximumLength) {
        if (value == null) {
            return "-";
        }
        if (value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, maximumLength - 3) + "...";
    }
}