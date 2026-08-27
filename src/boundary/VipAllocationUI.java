package boundary;

import control.VipPriorityController;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import utility.Utility;

/**
 * Boundary for VIP & Loyalty Tier Priority Room Allocation.
 *
 * VIP creation is automatic through Walk-In Registration when a guest meets
 * the loyalty requirement. This menu focuses on MaxHeap priority viewing,
 * VIP waiting-request maintenance, current VIP occupancy and priority allocation.
 *
 * @author Low Enn Toong
 */
public class VipAllocationUI {
    private static final int MAX_STAY_NIGHTS = 30;
    private static final LocalTime STANDARD_CHECKOUT_TIME = LocalTime.NOON;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final VipPriorityController controller;
    private final Scanner scanner;

    public VipAllocationUI() {
        this(new VipPriorityController(), new Scanner(System.in));
    }

    public VipAllocationUI(VipPriorityController controller) {
        this(controller, new Scanner(System.in));
    }

    public VipAllocationUI(VipPriorityController controller, Scanner scanner) {
        this.controller = controller;
        this.scanner = scanner;
    }

    public void run() {
        int choice;

        do {
            Utility.clearScreen();
            displayMenu();
            choice = readMenuChoice("Enter menu choice (0-10): ", 0, 10);

            if (choice == 0) {
                Utility.printInfo("Returning to Main Menu...");
                break;
            }

            Utility.clearScreen();

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
                    displayCurrentVipRooms();
                    break;
                case 8:
                    allocateRoom();
                    break;
                case 9:
                    generatePriorityAllocationPerformanceReport();
                    break;
                case 10:
                    generateLoyaltyStayPerformanceReport();
                    break;
                default:
                    Utility.printError("Invalid option. Please try again.");
                    break;
            }

            pauseForUser();
        } while (true);
    }

    private void displayMenu() {
        final int menuWidth = Utility.UI_WIDTH;

        System.out.println("=".repeat(menuWidth));
        System.out.println("    VIP & LOYALTY TIER PRIORITY ROOM ALLOCATION");
        System.out.println("=".repeat(menuWidth));
        displayModuleStatus();
        System.out.println("-".repeat(menuWidth));
        Utility.printSectionTitle("VIP INFORMATION");
        System.out.println("    1.  View All VIP Guest Profiles");
        System.out.println("    2.  View Next VIP in Priority");
        System.out.println("    3.  View VIP Waiting List by Priority");
        System.out.println();
        Utility.printSectionTitle("WAITING REQUESTS");
        System.out.println("    4.  Find VIP Waiting Registration");
        System.out.println("    5.  Update VIP Room Request");
        System.out.println("    6.  Cancel VIP Waiting Registration");
        System.out.println();
        Utility.printSectionTitle("ROOM OPERATIONS");
        System.out.println("    7.  View Current VIP In-House Rooms");
        System.out.println("    8.  Assign Ready Room & Check In VIP");
        System.out.println();
        Utility.printSectionTitle("REPORTS");
        System.out.println("    9.  VIP Room Allocation & Waiting Time Report");
        System.out.println("   10.  VIP Loyalty Engagement Report");
        System.out.println("-".repeat(menuWidth));
        System.out.println("    0.  Return to Main Menu");
        System.out.println("=".repeat(menuWidth));
    }

    private void displayAllVipGuests() {
        displayScreenHeader("VIP GUEST PROFILE DIRECTORY", "Shows all guests who currently hold ELITE, PLATINUM or DIAMOND loyalty status.");

        int totalProfiles = controller.getVipProfileCount();
        String[][] profiles = controller.getAllVipProfileDisplayData();

        if (totalProfiles == 0) {
            printEmptyState("No VIP guest profiles are available.", "A guest becomes VIP automatically after reaching the required completed-stay threshold.");
            return;
        }

        System.out.printf(
                "%-4s %-9s %-22s %-15s %-10s %-8s %-16s%n",
                "No.", "Guest ID", "Guest Name", "Phone", "Tier", "Stays", "Current Activity");
        System.out.println("-".repeat(94));

        int displayed = 0;
        int diamond = 0;
        int platinum = 0;
        int elite = 0;

        for (String[] profile : profiles) {
            displayed++;
            switch (profile[3]) {
                case "DIAMOND":
                    diamond++;
                    break;
                case "PLATINUM":
                    platinum++;
                    break;
                case "ELITE":
                    elite++;
                    break;
                default:
                    break;
            }

            System.out.printf(
                    "%-4d %-9s %-22s %-15s %-10s %-8d %-16s%n",
                    displayed,
                    profile[0],
                    shorten(profile[1], 22),
                    profile[2],
                    profile[3],
                    Integer.parseInt(profile[4]),
                    profile[5]);
        }

        if (displayed == 0) {
            printEmptyState("VIP loyalty profiles exist, but no matching guest master records were found.", "Check guest.dat and loyalty_profile.dat synchronization.");
            return;
        }

        System.out.println("-".repeat(94));
        System.out.println("VIP PROFILE SUMMARY");
        System.out.println("  Total VIP Guests : " + displayed);
        System.out.println("  DIAMOND          : " + diamond);
        System.out.println("  PLATINUM         : " + platinum);
        System.out.println("  ELITE            : " + elite);
        System.out.println("\nActivity meaning: WAITING = waiting for room allocation | IN HOUSE = currently checked in | PROFILE ONLY = not waiting/in-house.");
    }

    private void displayNextHighestPriorityVip() {
        displayScreenHeader("NEXT VIP IN PRIORITY", "Shows the VIP guest who should be served first for room allocation.");

        String registrationId = controller.getNextVipRegistrationId();

        if (registrationId == null) {
            printEmptyState("No VIP guests are currently waiting for room allocation.", null);
            return;
        }

        System.out.println("PRIORITY RULE");
        System.out.println("  DIAMOND > PLATINUM > ELITE; for the same tier, the earlier registration is served first.");
        System.out.println("-".repeat(104));

        displayVipRegistrationDetails(registrationId, false);

        int matchingRooms = controller.getMatchingReadyRoomCount(registrationId);
        String[] suggestedRoom = controller.getSuggestedReadyRoomDisplayData(registrationId);

        System.out.println("\nALLOCATION READINESS");
        System.out.println("  Suitable Rooms Now   : " + formatRoomAvailability(matchingRooms));
        System.out.println("  Allocation Status    : " + (matchingRooms > 0 ? "ROOM AVAILABLE - CAN PROCEED" : "NO SUITABLE ROOM READY YET"));
        System.out.println("  Suggested Room       : " + (suggestedRoom == null ? "-" : suggestedRoom[0] + " (" + formatRoomType(suggestedRoom[1]) + ", Capacity " + suggestedRoom[2] + ")"));

        if (matchingRooms == 0) {
            System.out.println("\nNext action: Keep this VIP in the waiting list until a suitable room becomes READY or the request is cancelled.");
        } else {
            System.out.println("\nNext action: Use Menu 8 - Assign Ready Room & Check In VIP.");
        }
    }

    private void displayPriorityQueue() {
        displayScreenHeader("VIP WAITING LIST - PRIORITY ORDER", "Shows VIP guests still waiting for a room, starting with the guest who should be served first."
        );

        String[][] registrations = controller.getVipPriorityQueueDisplayData();

        if (registrations.length == 0) {
            printEmptyState("No VIP guests are currently waiting for a room.", "Qualified VIP registrations will appear here automatically when they enter VIP_WAITING status.");
            return;
        }

        System.out.println("Suitable room = READY + requested room type + enough capacity.");
        System.out.println("-".repeat(127));

        System.out.printf(
                 "%-9s %-8s %-9s %-16s %-10s %-15s %-7s %-17s %-15s %-12s%n",
                "Priority", "Reg ID", "Guest ID", "Guest Name", "Tier", "Requested Room", "Guests", "Request Time","Waiting Time", "Available"
        );

        System.out.println("-".repeat(127));

        for (int i = 0; i < registrations.length; i++) {
            String[] registration = registrations[i];
            int readyMatches = Integer.parseInt(registration[8]);

            System.out.printf(
                "%-9s %-8s %-9s %-16s %-10s %-15s %-7s %-17s %-15s %-12s%n",
                i + 1,
                registration[0],
                registration[1],
                shorten(registration[2], 16),
                registration[3],
                formatRoomType(registration[4]),
                Integer.parseInt(registration[5]),
                registration[6],
                registration[7],
                formatSuitableRoomCount(readyMatches)
        );
        }

        System.out.println("-".repeat(127));
    }

    private void searchVipRegistration() {
        displayScreenHeader("FIND VIP WAITING REGISTRATION", "Search one VIP room request that is still waiting for allocation.");

        if (!controller.hasWaitingVip()) {
            printEmptyState("No VIP guests are currently waiting for room allocation.", null);
            return;
        }

        System.out.println("SEARCH GUIDE");
        System.out.println("  Registration ID format : R followed by 4 digits (example: R0001)");
        System.out.println("  Search scope           : VIP room requests still waiting for allocation");
        System.out.println("  Enter 0                : Return without searching");

        String registrationId = readRegistrationIdOrCancel("\nEnter VIP Registration ID or 0 to return: ");

        if (registrationId == null) {
            printActionCancelled("Search cancelled. No data was changed.");
            return;
        }

        Utility.clearScreen();
        displayScreenHeader("VIP WAITING REGISTRATION SEARCH RESULT", "Search result for Registration ID " + registrationId + ".");

        if (!controller.waitingVipRegistrationExists(registrationId)) {
            Utility.printError("No waiting VIP registration was found for " + registrationId + ".");
            System.out.println("\nPossible reasons:");
            System.out.println("  - The Registration ID does not exist.");
            System.out.println("  - The VIP has already checked in.");
            System.out.println("  - The waiting request has already been cancelled.");
            return;
        }

        Utility.printSuccess("VIP waiting registration found.");
        System.out.println();
        displayVipRegistrationDetails(registrationId);

        int matches = controller.getMatchingReadyRoomCount(registrationId);
        System.out.println("\nCURRENT ALLOCATION STATUS");
        System.out.println("  Suitable READY Rooms  : " + matches);
        System.out.println("  Status                : " + (matches > 0 ? "READY TO ALLOCATE" : "WAITING FOR SUITABLE ROOM"));
    }

    private void updateVipRoomRequest() {
        displayScreenHeader("UPDATE VIP ROOM REQUEST", "Modify a waiting VIP's room request before check-in. Loyalty tier and service priority are system-controlled.");

        if (!controller.hasWaitingVip()) {
            printEmptyState("No VIP guests are currently waiting for room allocation.", null);
            return;
        }

        System.out.println("WHAT CAN BE UPDATED");
        System.out.println("  1. Requested Room Type      - changes the room category requested by the VIP");
        System.out.println("  2. Number of Guests         - must fit the selected room type capacity");
        System.out.println("  3. Expected Check-Out Date  - stay date only; hotel check-out time stays fixed at 12:00 PM");
        System.out.println("\nEnter 0 at the Registration ID prompt to cancel this action.");

        String registrationId = readRegistrationIdOrCancel("\nEnter VIP Registration ID to update (example R0001) or 0 to cancel: ");

        if (registrationId == null) {
            printActionCancelled("Update cancelled. No changes were made.");
            return;
        }

        String[] registration = controller.getWaitingVipRegistrationDisplayData(registrationId);

        if (registration == null) {
            Utility.printError("No waiting VIP registration was found for " + registrationId + ".");
            return;
        }

        Utility.clearScreen();
        displayScreenHeader("UPDATE VIP ROOM REQUEST", "Review the current request first, then select exactly what should change.");
        System.out.println("CURRENT VIP REQUEST");
        System.out.println("-".repeat(104));
        displayVipRegistrationDetails(registrationId);

        System.out.println("\nSELECT UPDATE ACTION");
        System.out.println("  1. Update Requested Room Type");
        System.out.println("  2. Update Number of Guests");
        System.out.println("  3. Update Length of Stay / Expected Check-Out Date");
        System.out.println("  4. Update All Room Request Fields");
        System.out.println("  0. Cancel Update");

        int choice = readMenuChoice("\nEnter update choice (0-4): ", 0, 4);

        if (choice == 0) {
            printActionCancelled("Update cancelled. No changes were saved.");
            return;
        }

        String currentRoomType = registration[7];
        int currentGuestCount = Integer.parseInt(registration[8]);
        LocalDateTime currentCheckOut = controller.getWaitingVipCheckOutDateTime(registrationId);

        String newRoomType = currentRoomType;
        int newGuestCount = currentGuestCount;
        LocalDateTime newCheckOut = currentCheckOut;

        if (choice == 1 || choice == 4) {
            newRoomType = readRoomType("SELECT NEW REQUESTED ROOM TYPE");

            int maximumCapacity = controller.getMaximumCapacityForRoomType(newRoomType);
            if (choice == 1 && newGuestCount > maximumCapacity) {
                System.out.println("\n[ATTENTION] Current party size " + newGuestCount + " exceeds the maximum capacity of " + formatRoomType(newRoomType) + " (" + maximumCapacity + "). A new valid party size is required.");
                newGuestCount = readGuestCountForRoomType(newRoomType);
            }
        }

        if (choice == 2 || choice == 4) {
            newGuestCount = readGuestCountForRoomType(newRoomType);
        }

        if (choice == 3 || choice == 4) {
            newCheckOut = readFutureCheckOutDate(registrationId);
        }

        System.out.println("\n" + "-".repeat(104));
        System.out.println("REVIEW CHANGES BEFORE SAVING");
        System.out.println("-".repeat(104));
        System.out.printf("%-30s %-30s %-30s%n", "Field", "Current Value", "New Value");
        System.out.println("-".repeat(104));
        System.out.printf("%-30s %-30s %-30s%n", "Requested Room Type", formatRoomType(currentRoomType), formatRoomType(newRoomType));
        System.out.printf("%-30s %-30s %-30s%n", "Number of Guests", currentGuestCount, newGuestCount);
        System.out.printf("%-30s %-30s %-30s%n", "Expected Check-Out", formatDateTime(currentCheckOut), formatDateTime(newCheckOut));
        System.out.println("-".repeat(104));
        System.out.println("Hotel standard check-out time remains fixed at 12:00 PM.");
        System.out.println("VIP priority will be recalculated automatically if request data affects queue ordering rules.");

        boolean confirm = readYesNo("\nSave these changes? (Y/N): ");

        if (!confirm) {
            printActionCancelled("Update cancelled. No changes were saved.");
            return;
        }

        boolean updated = controller.updateVipRegistrationRequest(registrationId, newRoomType, newGuestCount, newCheckOut);

        if (!updated) {
            Utility.printError("Unable to update the VIP request. Check room capacity, registration status and expected check-out date.");
            return;
        }

        String[] updatedRegistration = controller.getWaitingVipRegistrationDisplayData(registrationId);
        Utility.clearScreen();
        displayScreenHeader("VIP ROOM REQUEST UPDATED", "The waiting request was saved and remains synchronized with VIP priority data.");
        Utility.printSuccess("VIP room request updated successfully.");

        if (updatedRegistration != null) {
            System.out.println("\nUPDATED REQUEST");
            System.out.println("-".repeat(104));
            displayVipRegistrationDetails(registrationId);
            System.out.println("\nSuitable READY Rooms Now : " + controller.getMatchingReadyRoomCount(registrationId));
        }

        System.out.println("\nSystem note: Loyalty tier is not changed manually; the waiting order continues to follow the VIP priority rules.");
    }

    private void cancelVipRegistration() {
        displayScreenHeader("CANCEL VIP WAITING REGISTRATION", "Cancel one waiting VIP request without deleting the guest profile or loyalty information.");

        if (!controller.hasWaitingVip()) {
            printEmptyState("No VIP guests are currently waiting for room allocation.", null);
            return;
        }

        System.out.println("CANCELLATION EFFECT");
        System.out.println("  - Registration status changes from VIP_WAITING to CANCELLED.");
        System.out.println("  - The request is removed from the VIP priority queue.");
        System.out.println("  - Guest master data and loyalty tier are NOT deleted.");
        System.out.println("  - This action is only for guests who have not checked in yet.");
        System.out.println("\nEnter 0 to return without cancelling anything.");

        String registrationId = readRegistrationIdOrCancel("\nEnter VIP Registration ID to cancel (example R0001) or 0 to return: ");

        if (registrationId == null) {
            printActionCancelled("Cancellation action exited. No registration was changed.");
            return;
        }

        if (!controller.waitingVipRegistrationExists(registrationId)) {
            Utility.printError("No waiting VIP registration was found for " + registrationId + ".");
            return;
        }

        Utility.clearScreen();
        displayScreenHeader("CONFIRM VIP WAITING CANCELLATION", "Review the request carefully before removing it from the priority queue.");
        System.out.println("REGISTRATION TO CANCEL");
        System.out.println("-".repeat(104));
        displayVipRegistrationDetails(registrationId);
        System.out.println("\nWARNING: After confirmation, this registration will no longer be considered for VIP room allocation.");

        boolean confirm = readYesNo("\nConfirm cancellation? (Y/N): ");

        if (!confirm) {
            printActionCancelled("Cancellation aborted. The VIP remains in the waiting list.");
            return;
        }

        String[] cancelled = controller.cancelVipRegistrationDisplayData(registrationId);

        if (cancelled == null) {
            Utility.printError("Unable to cancel the VIP registration.");
            return;
        }

        Utility.clearScreen();
        displayScreenHeader("VIP WAITING REGISTRATION CANCELLED", "Cancellation completed successfully.");
        Utility.printSuccess("VIP waiting registration cancelled successfully.");
        System.out.println("\nCANCELLATION RESULT");
        System.out.println("  Registration ID     : " + cancelled[0]);
        System.out.println("  Guest ID            : " + cancelled[1]);
        System.out.println("  Guest Name          : " + cancelled[2]);
        System.out.println("  Loyalty Tier        : " + cancelled[3]);
        System.out.println("  New Status          : " + cancelled[4]);
        System.out.println("  VIPs Still Waiting  : " + cancelled[5]);
        System.out.println("\nGuest and loyalty profile remain available for future stays.");
    }

    private void displayCurrentVipRooms() {
        displayScreenHeader("CURRENT VIP IN-HOUSE ROOMS", "Shows VIP guests who are currently checked in and occupying rooms according to live room status.");

        String[][] currentVipBookings = controller.getCurrentVipRoomDisplayData();

        if (currentVipBookings.length == 0) {
            printEmptyState("No VIP guests are currently checked in.", "VIP rooms will appear here after successful allocation and disappear after Front Desk check-out.");
            return;
        }

        System.out.printf(
                "%-3s %-9s %-8s %-13s %-9s %-5s %-12s %-17s  %-17s%n",
                "No", "Confirm No", "Guest ID", "Guest Name", "Tier", "Room", "Room Type", "Check-In", "Exp. Check-Out");
        System.out.println("-".repeat(116));

        for (int i = 0; i < currentVipBookings.length; i++) {
            String[] booking = currentVipBookings[i];
            System.out.printf(
                    "%-3s %-9s %-8s %-13s %-9s %-5s %-12s %-17s  %-17s%n",
                    i + 1,
                    booking[0],
                    booking[1],
                    shorten(booking[2], 18),
                    booking[3],
                    booking[4],
                    "-".equals(booking[5]) ? "-" : formatRoomType(booking[5]),
                    booking[6],
                    booking[7]);
        }

        System.out.println("-".repeat(116));
        System.out.println("IN-HOUSE SUMMARY");
        System.out.println("  Current VIP Rooms Occupied : " + currentVipBookings.length);
        System.out.println("\nNext action: Use the Confirmation No. in Front Desk to process check-out.");
    }

    private void allocateRoom() {
        displayScreenHeader("ASSIGN READY ROOM & CHECK IN VIP", "Assign a suitable ready room to the highest-priority VIP who can be served now.");

        if (!controller.hasWaitingVip()) {
            printEmptyState("No VIP guests are currently waiting for room allocation.", null);
            return;
        }

        String highestPriorityVipId = controller.getNextVipRegistrationId();
        String nextRegistrationId = controller.getNextAllocatableVipRegistrationId();

        if (nextRegistrationId == null) {
            Utility.printError("VIP guests are waiting, but no suitable room is currently ready.");
            System.out.println("\nWAITING STATUS");
            System.out.println("  VIPs Waiting       : " + controller.getWaitingCount());
            System.out.println("  Ready Rooms Total  : " + controller.getVacantRoomCount());
            System.out.println("  Waiting List       : All requests remain active in their current service order.");
            System.out.println("\nNext action: Wait for Housekeeping to prepare a matching room, or update a room request if the guest chooses a different room type.");
            return;
        }

        if (highestPriorityVipId != null && !highestPriorityVipId.equalsIgnoreCase(nextRegistrationId)) {
            System.out.println("[SYSTEM NOTICE]");
            System.out.println("  Registration " + highestPriorityVipId + " (" + controller.getLoyaltyTierNameByRegistrationId(highestPriorityVipId) + ") has no suitable room ready now.");
            System.out.println("  This guest keeps the same service priority. The next VIP who can be accommodated is shown below.");
            System.out.println("-".repeat(104));
        }

        String[] nextRegistration = controller.getWaitingVipRegistrationDisplayData(nextRegistrationId);
        String[] suggestedRoom = controller.getSuggestedReadyRoomDisplayData(nextRegistrationId);
        int matchingRooms = controller.getMatchingReadyRoomCount(nextRegistrationId);

        System.out.println("VIP SELECTED FOR CHECK-IN");
        System.out.println("-".repeat(104));
        System.out.println("  Guest ID             : " + nextRegistration[0]);
        System.out.println("  Guest Name           : " + nextRegistration[1]);
        System.out.println("  Phone Number         : " + nextRegistration[2]);
        System.out.println("  VIP Tier             : " + nextRegistration[3]);
        System.out.println("  Registration ID      : " + nextRegistration[4]);
        System.out.println("  Request Time         : " + nextRegistration[6]);
        System.out.println("\nSTAY & ROOM REQUEST");
        System.out.println("-".repeat(104));
        System.out.println("  Requested Room Type  : " + formatRoomType(nextRegistration[7]));
        System.out.println("  Number of Guests     : " + nextRegistration[8]);
        System.out.println("  Expected Check-Out   : " + nextRegistration[9]);
        System.out.println("  Suitable Rooms Now   : " + formatRoomAvailability(matchingRooms));
        System.out.println("\nROOM TO BE ASSIGNED");
        System.out.println("-".repeat(104));
        if (suggestedRoom == null) {
            System.out.println("  No suitable room is currently available.");
            return;
        }
        System.out.println("  Room Number          : " + suggestedRoom[0]);
        System.out.println("  Floor                : " + suggestedRoom[3]);
        System.out.println("  Capacity             : " + suggestedRoom[2] + " guest(s)");
        System.out.println("  Room Status          : " + suggestedRoom[4]);

        String[] paymentPreview = controller.getNextVipPaymentPreviewDisplayData();

        if (paymentPreview == null) {
            Utility.printError("Unable to prepare VIP payment details at this time.");
            return;
        }

        System.out.println("\nPAYMENT SUMMARY");
        System.out.println("-".repeat(104));
        System.out.println("  Room Rate             : RM" + paymentPreview[0] + " / night");
        System.out.println("  Number of Nights      : " + paymentPreview[1]);
        System.out.println("  Total Amount          : RM" + paymentPreview[2]);
        System.out.println();
        boolean confirm = readYesNo("Confirm payment of RM" + paymentPreview[2] + " and proceed with VIP check-in? (Y/N): ");

        if (!confirm) {
            printActionCancelled("Payment cancelled. " + "The VIP remains in the waiting list.");
            return;
        }

        String[] booking = controller.allocateNextVipBookingDisplayData();

        if (booking == null) {
            Utility.printError("Unable to complete room assignment. Room availability may have changed; waiting VIPs remain queued.");
            return;
        }

        Utility.clearScreen();
        displayScreenHeader("VIP CHECK-IN COMPLETED", "Room assignment and check-in completed successfully.");
        Utility.printSuccess("VIP room allocated and guest checked in successfully.");

        System.out.println("\nCHECK-IN CONFIRMATION");
        System.out.println("-".repeat(72));
        System.out.println("  Confirmation No.      : " + booking[0]);
        System.out.println("  Registration ID       : " + booking[1]);
        System.out.println("  Guest ID              : " + booking[2]);
        System.out.println("  Guest Name            : " + booking[3]);
        System.out.println("  VIP Tier              : " + booking[4]);
        System.out.println("  Allocated Room        : " + booking[5]);
        System.out.println("  Room Type             : " + formatRoomType(booking[6]));
        System.out.println("  Check-In Time         : " + booking[7]);
        System.out.println("  Expected Check-Out    : " + booking[8]);
        System.out.println("  Payment ID            : " + booking[10]);
        System.out.println("  Payment Amount        : RM" + booking[11]);
        System.out.println("  Payment Status        : " + booking[12] + " (Completed)");
        System.out.println("-".repeat(72));
        System.out.println("\nVIP QUEUE STATUS");
        System.out.println("-".repeat(72));
        if ("0".equals(booking[9])) {
            System.out.println("  No VIP guests are currently waiting.");
        } else {
            System.out.println("  VIPs Still Waiting    : " + booking[9]);
        }
        System.out.println("-".repeat(72));

        System.out.println("Next step: Use Confirmation No. " + booking[0] + " at Front Desk when the guest checks out.");
    }

    private void generatePriorityAllocationPerformanceReport() {
        displayScreenHeader("VIP ROOM ALLOCATION & WAITING TIME REPORT - FILTER SETUP", "Generate a management report from current and historical VIP registration and booking records.");

        System.out.println("REPORT SCOPE");
        System.out.println("  - Uses saved VIP registration history, booking/check-in records and current loyalty tiers.");
        System.out.println("  - The report can be generated even when no VIP is currently waiting.");
        System.out.println("  - Press Enter for ALL where indicated.");
        System.out.println("-".repeat(104));
        System.out.println("REPORT ACTION");
        System.out.println("  1. Continue to Report Filter Setup");
        System.out.println("  0. Return to VIP Menu");

        int reportAction = readMenuChoice("Select action (0-1): ", 0, 1);
        if (reportAction == 0) {
            printActionCancelled("VIP Room Allocation & Waiting Time Report generation cancelled.");
            return;
        }

        String keyword = readOptionalString("Search keyword (Reg ID / Guest ID / Name / Confirmation No.; Enter = ALL): ");
        String tierFilter = readTierFilter();
        String roomTypeFilter = readRoomTypeFilter();
        String statusFilter = readVipRegistrationStatusFilter();

        System.out.println("\nFilter by Registration Date Range (format YYYY-MM-DD):");
        LocalDate startDate = readOptionalDate("Start Date (Enter = No Start Date): ");
        LocalDate endDate = readOptionalDate("End Date (Enter = No End Date): ");
        while (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            Utility.printError("End Date cannot be earlier than Start Date.");
            endDate = readOptionalDate("Re-enter End Date (Enter = No End Date): ");
        }

        int minimumGuests = readNonNegativeInteger("Minimum party size (0 = ALL): ");

        System.out.println("\nSort Report Display By:");
        System.out.println("1. VIP Tier Priority, then Earliest Registration");
        System.out.println("2. Allocation Waiting Time (Longest First)");
        System.out.println("3. Registration Time (Latest First)");
        System.out.println("4. Requested Room Type (A-Z)");
        int sortOption = readMenuChoice("Select sort option (1-4): ", 1, 4);

        Utility.clearScreen();
        controller.generatePriorityAllocationPerformanceReport(keyword, tierFilter, roomTypeFilter, statusFilter, startDate, endDate, minimumGuests, sortOption);
    }

    private void generateLoyaltyStayPerformanceReport() {
        displayScreenHeader("VIP LOYALTY ENGAGEMENT REPORT - FILTER SETUP", "Generate a management report from VIP loyalty profiles, booking history and current guest activity.");

        System.out.println("REPORT SCOPE");
        System.out.println("  - Uses all current VIP profiles, completed-stay totals and saved booking history.");
        System.out.println("  - Current activity can be used as an optional report filter.");
        System.out.println("  - The report does not depend on the current VIP waiting queue.");
        System.out.println("-".repeat(104));
        System.out.println("REPORT ACTION");
        System.out.println("  1. Continue to Report Filter Setup");
        System.out.println("  0. Return to VIP Menu");

        int reportAction = readMenuChoice("Select action (0-1): ", 0, 1);
        if (reportAction == 0) {
            printActionCancelled("VIP Loyalty Engagement Report generation cancelled.");
            return;
        }

        String keyword = readOptionalString("Search keyword (Guest ID / Name / Confirmation No.; Enter = ALL): ");
        String tierFilter = readTierFilter();
        String activityFilter = readVipActivityFilter();
        int minimumCompletedStays = readNonNegativeInteger("Minimum completed stays (0 = ALL): ");
        String roomTypeFilter = readRoomTypeFilter("Filter by Stay Room Type");

        System.out.println("\nFilter Booking/Stay History by Check-In Date (format YYYY-MM-DD):");
        LocalDate startDate = readOptionalDate("Start Date (Enter = No Start Date): ");
        LocalDate endDate = readOptionalDate("End Date (Enter = No End Date): ");
        while (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            Utility.printError("End Date cannot be earlier than Start Date.");
            endDate = readOptionalDate("Re-enter End Date (Enter = No End Date): ");
        }

        System.out.println("\nSort Report Display By:");
        System.out.println("1. Loyalty Tier Priority (DIAMOND > PLATINUM > ELITE)");
        System.out.println("2. Completed Stays (Highest First)");
        System.out.println("3. Most Recent Stay (Latest First)");
        System.out.println("4. Guest Name (A-Z)");
        System.out.println("5. Closest to Next Loyalty Tier");
        int sortOption = readMenuChoice("Select sort option (1-5): ", 1, 5);

        Utility.clearScreen();
        controller.generateLoyaltyStayPerformanceReport(
                keyword, tierFilter, activityFilter, minimumCompletedStays,
                roomTypeFilter, startDate, endDate, sortOption);
    }

    private void displayVipRegistrationDetails(String registrationId) {
        displayVipRegistrationDetails(registrationId, true);
    }

    private void displayVipRegistrationDetails(String registrationId, boolean showSuitableRooms) {
        String[] registration = controller.getWaitingVipRegistrationDisplayData(registrationId);
        if (registration == null) {
            return;
        }

        System.out.println("VIP PROFILE");
        System.out.println("  Guest ID              : " + registration[0]);
        System.out.println("  Guest Name            : " + registration[1]);
        System.out.println("  Phone Number          : " + registration[2]);
        System.out.println("  Loyalty Tier          : " + registration[3]);
        System.out.println("\nWAITING REGISTRATION");
        System.out.println("  Registration ID       : " + registration[4]);
        System.out.println("  Registration Status   : " + registration[5]);
        System.out.println("  Request Time         : " + registration[6]);
        System.out.println("\nROOM REQUEST");
        System.out.println("  Requested Room Type   : " + formatRoomType(registration[7]));
        System.out.println("  Number of Guests      : " + registration[8]);
        System.out.println("  Expected Check-Out    : " + registration[9]);
        System.out.println("  Standard Check-Out    : 12:00 PM");
        if (showSuitableRooms) {
            System.out.println("  Suitable Rooms Now    : " + formatRoomAvailability(Integer.parseInt(registration[10])));
        }
    }


    private String formatRoomAvailability(int roomCount) {
        if (roomCount <= 0) {
            return "No suitable room available";
        }
        if (roomCount == 1) {
            return "1 room available";
        }
        return roomCount + " rooms available";
    }

    private String formatSuitableRoomCount(int roomCount) {
        if (roomCount <= 0) {
            return "None";
        }
        if (roomCount == 1) {
            return "1 room";
        }
        return roomCount + " rooms";
    }


    private void displayScreenHeader(String title, String purpose) {
        System.out.println("=".repeat(104));
        System.out.println(" " + title);
        System.out.println("=".repeat(104));
        if (purpose != null && !purpose.isBlank()) {
            System.out.println(" " + purpose);
            System.out.println("-".repeat(104));
        }
    }

    private void displayModuleStatus() {
        int vipProfiles = controller.getVipProfileCount();
        int waiting = controller.getWaitingCount();
        int inHouse = controller.getCurrentVipRoomDisplayData().length;
        int readyRooms = controller.getVacantRoomCount();
        String nextRegistrationId = controller.getNextVipRegistrationId();
        String[] next = nextRegistrationId == null
                ? null : controller.getWaitingVipRegistrationDisplayData(nextRegistrationId);

        System.out.println("STATUS        : " + vipProfiles + " VIP Profiles" + " | " + waiting + " Waiting" + " | " + inHouse + " In House" + " | " + readyRooms + " Ready Rooms");
        System.out.println("NEXT PRIORITY : " + (next == null ? "NONE" : next[4] + " / " + next[0] + " / " + next[3]));
    }

    private void printEmptyState(String message, String guidance) {
        System.out.println(message);

        if (guidance != null && !guidance.isBlank()) {
            System.out.println("\n" + guidance);
        }
    }

    private void printActionCancelled(String message) {
        System.out.println("\n[ACTION CANCELLED] " + message);
    }

    private void pauseForUser() {
        System.out.println("\n" + "-".repeat(104));
        System.out.print("Press Enter to return to the VIP menu...");
        scanner.nextLine();
    }

    private int countMatchingReadyRooms(String registrationId) {
        return controller.getMatchingReadyRoomCount(registrationId);
    }

    private String getVipActivityStatus(String guestId) {
        String[][] profiles = controller.getAllVipProfileDisplayData();
        for (String[] profile : profiles) {
            if (profile[0].equalsIgnoreCase(guestId)) {
                return profile[5];
            }
        }
        return "PROFILE ONLY";
    }

    private String readTierFilter() {
        String[] tiers = controller.getVipTierNames();

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
        return readRoomTypeFilter("Filter by Requested Room Type");
    }

    private String readRoomTypeFilter(String heading) {
        String[] roomTypes = controller.getRoomTypeNames();

        while (true) {
            System.out.println("\n" + heading + ":");
            System.out.println("0. ALL");

            for (int i = 0; i < roomTypes.length; i++) {
                System.out.println((i + 1) + ". " + formatRoomType(roomTypes[i]));
            }

            int choice = readInteger("Enter room type filter choice (0-" + roomTypes.length + "): ");

            if (choice == 0) {
                return null;
            }

            if (choice >= 1 && choice <= roomTypes.length) {
                return roomTypes[choice - 1];
            }

            Utility.printError("Invalid room type filter. Choose a listed number.");
        }
    }

    private String readVipRegistrationStatusFilter() {
        while (true) {
            System.out.println("\nFilter by VIP Registration Status:");
            System.out.println("0. ALL");
            System.out.println("1. VIP WAITING");
            System.out.println("2. CHECKED IN");
            System.out.println("3. CHECKED OUT");
            System.out.println("4. CANCELLED");

            int choice = readInteger("Enter status filter choice (0-4): ");
            switch (choice) {
                case 0:
                    return "ALL";
                case 1:
                    return "VIP_WAITING";
                case 2:
                    return "CHECKED_IN";
                case 3:
                    return "CHECKED_OUT";
                case 4:
                    return "CANCELLED";
                default:
                    Utility.printError("Invalid status filter. Enter a number from 0 to 4.");
                    break;
            }
        }
    }

    private String readVipActivityFilter() {
        while (true) {
            System.out.println("\nFilter by Current VIP Activity:");
            System.out.println("0. ALL");
            System.out.println("1. WAITING");
            System.out.println("2. IN HOUSE");
            System.out.println("3. PROFILE ONLY");

            int choice = readInteger("Enter activity filter choice (0-3): ");
            switch (choice) {
                case 0:
                    return "ALL";
                case 1:
                    return "WAITING";
                case 2:
                    return "IN HOUSE";
                case 3:
                    return "PROFILE ONLY";
                default:
                    Utility.printError("Invalid activity filter. Enter a number from 0 to 3.");
                    break;
            }
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
        String[] roomTypes = controller.getRoomTypeNames();

        while (true) {
            System.out.println("\n" + heading + ":");

            for (int i = 0; i < roomTypes.length; i++) {
                int maximumCapacity = controller.getMaximumCapacityForRoomType(roomTypes[i]);
                System.out.printf(
                        "%d. %-16s | RM %.2f/day | Max Capacity: %d%n",
                        i + 1,
                        formatRoomType(roomTypes[i]),
                        controller.getRoomTypePricePerDay(roomTypes[i]),
                        maximumCapacity);
            }

            int choice = readInteger("Enter room type number (1-" + roomTypes.length + ", e.g. 1): ");

            if (choice >= 1 && choice <= roomTypes.length) {
                return roomTypes[choice - 1];
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

    private LocalDateTime readFutureCheckOutDate(String registrationId) {
        LocalDateTime registrationTime = controller.getWaitingVipRegistrationTime(registrationId);
        LocalDate arrivalDate = registrationTime == null ? LocalDate.now() : registrationTime.toLocalDate();

        LocalDate latestAllowedDate = arrivalDate.plusDays(MAX_STAY_NIGHTS);

        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDate earliestFutureCheckOutDate = now.toLocalDate();

        if (!LocalDateTime.of(earliestFutureCheckOutDate, STANDARD_CHECKOUT_TIME).isAfter(now)) {
            earliestFutureCheckOutDate = earliestFutureCheckOutDate.plusDays(1);
        }

        LocalDate earliestAllowedDate = arrivalDate.plusDays(1);
        if (earliestFutureCheckOutDate.isAfter(earliestAllowedDate)) {
            earliestAllowedDate = earliestFutureCheckOutDate;
        }

        System.out.println("\nEnter New Expected Check-Out Date");
        System.out.println("Hotel standard check-out time is fixed at 12:00 PM.");
        System.out.println("Maximum stay: " + MAX_STAY_NIGHTS + " nights from the registration/arrival date.");

        if (earliestAllowedDate.isAfter(latestAllowedDate)) {
            Utility.printError("No valid future check-out date remains within the " + MAX_STAY_NIGHTS + "-night stay limit.");
            return controller.getWaitingVipCheckOutDateTime(registrationId);
        }

        LocalDate newCheckOutDate = Utility.readCheckOutDateInRange(scanner, earliestAllowedDate, latestAllowedDate);
        return LocalDateTime.of(newCheckOutDate, STANDARD_CHECKOUT_TIME);
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

            Utility.printError("Invalid Registration ID. Use R followed by 4 digits (e.g. R0001), or enter 0 to return.");
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

    private LocalDate readOptionalDate(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                return null;
            }

            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException exception) {
                Utility.printError("Invalid date. Use YYYY-MM-DD (e.g. 2026-08-15), or press Enter for no date filter.");
            }
        }
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

            Utility.printError("Invalid choice. Enter a number from " + minimum + " to " + maximum + ".");
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