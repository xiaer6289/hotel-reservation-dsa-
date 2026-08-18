package boundary;

import control.RegistrationController;
import control.report.StandardFifoWaitingTimeRP;
import control.report.WalkInArrivalPatternRP;
import control.VipPriorityController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import utility.Utility;

/**
 * Handles the walk-in registration and Standard booking procedure.
 *
 * Assignment rule:
 * Standard guests are kept chronologically in a Linear ADT (FIFO). New
 * Standard requests are appended at the end and only the first waiting guest
 * can be processed for room assignment. Existing loyalty members are routed to
 * the separate VIP MaxHeap so they do not enter the Standard FIFO.
 *
 * @author Lai Jen Feng
 */
public class RegistrationUI {

    private static final int MAX_STAY_NIGHTS = 30;
    private static final LocalTime STANDARD_CHECKOUT_TIME = LocalTime.NOON;
    private static final DateTimeFormatter DATE_TIME_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RegistrationController controller;
    private final Scanner scanner;

    public RegistrationUI() {
        this(new RegistrationController(), new Scanner(System.in));
    }

    public RegistrationUI(RegistrationController controller) {
        this(controller, new Scanner(System.in));
    }

    public RegistrationUI(
            RegistrationController controller,
            Scanner scanner) {

        this.controller = controller;
        this.scanner = scanner;
    }

    public void run() {
        int choice;

        do {
            displayMenu();
            choice = readMenuChoice("  Enter choice (0-8): ", 0, 8);

            switch (choice) {
                case 1:
                    addWalkInRegistration();
                    break;
                case 2:
                    viewWaitingQueue();
                    break;
                case 3:
                    viewNextRegistration();
                    break;
                case 4:
                    checkInNextStandardGuest();
                    break;
                case 5:
                    searchRegistration();
                    break;
                case 6:
                    cancelRegistration();
                    break;
                case 7:
                    generateStandardFifoWaitingTimeReport();
                    break;
                case 8:
                    generateWalkInArrivalPatternReport();
                    break;
                case 0:
                    Utility.printInfo("Returning to Main Menu...");
                    break;
                default:
                    Utility.printError("Invalid choice. Please try again.");
                    break;
            }

            if (choice != 0) Utility.pauseScreen();
        } while (choice != 0);
    }

    private void displayMenu() {
        Utility.clearScreen();
        Utility.printHeader("WALK-IN REGISTRATION & STANDARD BOOKING");
        System.out.println();
        Utility.printSectionTitle("REGISTRATION");
        System.out.println("  1.  Register Walk-In Guest");
        System.out.println("  2.  View Standard Waiting Queue");
        System.out.println("  3.  View Next Standard Guest");
        System.out.println("  4.  Assign Ready Room & Check In Next Guest");
        System.out.println("  5.  Search Walk-In Registration by ID");
        System.out.println("  6.  Cancel Waiting Standard Registration");
        System.out.println();
        Utility.printSectionTitle("REPORTS");
        System.out.println("  7.  Standard FIFO Waiting Time Analysis");
        System.out.println("  8.  Walk-In Arrival Pattern Analysis");
        System.out.println();
        Utility.printDivider();
        System.out.println("  0.  Return to Main Menu");
        Utility.printDivider();
    }

    /**
     * Realistic simplified hotel walk-in flow for this assignment:
     * identify guest -> detect loyalty -> collect party/stay request -> confirm
     * details -> create registration -> route Standard guests to FIFO or VIPs
     * to the MaxHeap. The actual check-in time is recorded only after a room is
     * successfully assigned.
     */
    private void addWalkInRegistration() {
        System.out.println("\n===== REGISTER WALK-IN GUEST =====");
        System.out.println("Guest ID and Registration ID are generated automatically.");

        boolean existingGuestOrMember = readYesNo("Is this an existing guest or loyalty member? (Y/N): ");

        String guestId = null;
        boolean newGuest = !existingGuestOrMember;
        String newGuestName = null;
        Long phoneNumber = null;
        String phoneDisplay = null;

        if (existingGuestOrMember) {
            guestId = selectExistingGuestByName();

            if (guestId == null) {
                System.out.println("Registration cancelled. No booking request was created.");
                return;
            }

            phoneDisplay = controller.getGuestPhoneRaw(guestId);

            if (controller.hasActiveRegistrationOrStay(guestId)) {
                Utility.printError(
                        "This guest already has a waiting registration or is currently checked in.");
                return;
            }
        } else {
            System.out.println("\n===== NEW GUEST REGISTRATION =====");
            System.out.println("A new guest profile will be created after the walk-in details are confirmed.");

            newGuestName = readGuestName("Enter Guest Full Name: ");
            phoneNumber = readPhoneNumber();
            phoneDisplay = String.valueOf(phoneNumber);

            String[] guestWithSamePhone = controller.searchGuestDisplayDataByPhoneNo(phoneNumber);
            if (guestWithSamePhone != null) {
                Utility.printError("This phone number is already linked to an existing guest profile.");
                System.out.println("Guest ID     : " + guestWithSamePhone[0]);
                System.out.println("Guest Name   : " + guestWithSamePhone[1]);
                System.out.println("Phone Number : " + formatPhoneNo(guestWithSamePhone[2]));
                System.out.println("Please register this person as an existing guest instead of creating a duplicate profile.");
                return;
            }
        }

        String loyaltyTier = null;

        if (!newGuest) {
            String beforeRefreshTier = controller.getLoyaltyTierNameByGuestId(guestId);
            loyaltyTier = controller.refreshLoyaltyTierNameByGuestId(guestId);

            if (beforeRefreshTier == null && loyaltyTier != null) {
                Utility.printSuccess("Loyalty requirement reached. VIP membership activated automatically.");
            }
        }

        displayGuestCategory(guestId, newGuest, loyaltyTier);

        int maximumOccupancy = controller.getMaximumRoomCapacity();
        if (maximumOccupancy <= 0) {
            Utility.printError("No room inventory is currently configured.");
            return;
        }

        int numberOfGuests = readIntegerInRange(
                "Enter Number of Guests (1-" + maximumOccupancy + ", one room per registration): ",
                1,
                maximumOccupancy);

        String roomType = readRoomType(numberOfGuests, loyaltyTier);

        if (roomType == null) {
            Utility.printError(
                    "No configured room type can accommodate this party size.");
            return;
        }

        int numberOfNights = readIntegerInRange(
                "Enter Number of Nights (1-" + MAX_STAY_NIGHTS + "): ",
                1,
                MAX_STAY_NIGHTS);

        LocalDateTime arrivalTime = LocalDateTime.now()
                .withSecond(0)
                .withNano(0);

        LocalDateTime expectedCheckOut = LocalDateTime.of(
                arrivalTime.toLocalDate().plusDays(numberOfNights),
                STANDARD_CHECKOUT_TIME);

        int readyRoomCount = loyaltyTier == null
                ? controller.getReadyRoomCountForStandardRequest(roomType, numberOfGuests)
                : controller.getReadyRoomCountForVipRequest(roomType, numberOfGuests, loyaltyTier);

        String guestName = newGuest ? newGuestName : controller.getGuestName(guestId);

        System.out.println("\n===== REGISTRATION SUMMARY =====");
        System.out.printf("%-20s: %s%n", "Guest Name", guestName);
        System.out.printf("%-20s: %s%n", "Phone Number", formatPhoneNo(phoneDisplay));
        System.out.printf("%-20s: %s%n", "Guest Category",
                loyaltyTier == null ? "STANDARD" : "VIP - " + loyaltyTier);

        System.out.println();

        System.out.printf("%-20s: %s%n", "Requested Room", formatRoomType(roomType));
        System.out.printf("%-20s: %d%n", "Number of Guests", numberOfGuests);
        System.out.printf("%-20s: %d night(s)%n", "Length of Stay", numberOfNights);
        System.out.printf("%-20s: %s%n", "Arrival Time", formatDateTime(arrivalTime));
        System.out.printf("%-20s: %s%n", "Expected Check-Out", formatDateTime(expectedCheckOut));

        System.out.println();

        System.out.printf("%-20s: %d%n", "Ready Rooms Now", readyRoomCount);

        if (loyaltyTier == null) {
            System.out.printf("%-20s: %s%n",
                    "Queue Policy",
                    "Standard FIFO order applies");
        } else {
            System.out.printf("%-20s: %s%n",
                    "Allocation Policy",
                    "VIP tier priority applies");
        }

        if (!readYesNo("Confirm this walk-in registration? (Y/N): ")) {
            System.out.println("Registration cancelled. No registration record was created.");
            return;
        }

        if (newGuest) {
            guestId = controller.addNewGuestAndReturnId(newGuestName, phoneNumber);

            if (guestId == null) {
                Utility.printError(
                        "Unable to create the new guest profile. The phone number may already be registered.");
                return;
            }

            
        }

        String registrationId = controller.generateRegistrationId();
        int result = controller.createAndRouteWalkInRegistration(
                registrationId,
                guestId,
                roomType,
                numberOfGuests,
                arrivalTime,
                expectedCheckOut);

        if (result == RegistrationController.DUPLICATE_REGISTRATION_ID) {
            Utility.printError(
                    "Registration ID already exists. Please try the registration again.");
            return;
        }

        if (result == RegistrationController.GUEST_ALREADY_ACTIVE) {
            Utility.printError(
                    "This guest already has a waiting registration or is currently checked in.");
            return;
        }

        if (loyaltyTier != null) {
            if (result != VipPriorityController.ADD_SUCCESS) {
                displayVipAddError(result);
                return;
            }

            Utility.printSuccess(
                    "VIP walk-in registered and routed to the VIP priority heap.");
            System.out.println("Registration ID : " + registrationId);
            System.out.println("Loyalty Tier    : " + loyaltyTier);
            System.out.println("Status          : " + controller.getRegistrationStatusName(registrationId));
            System.out.println("VIPs Waiting    : " + controller.getVipWaitingCount());
            System.out.println(
                    "Next step: Use the VIP Allocation module for tier-priority room assignment.");
        } else {
            if (result != VipPriorityController.ADD_SUCCESS) {
                Utility.printError("Unable to register the Standard walk-in request.");
                return;
            }

            System.out.println("\n===== REGISTRATION SUCCESSFUL =====");
            System.out.printf("%-18s: %s%n", "Guest ID", guestId);
            System.out.printf("%-18s: %s%n", "Registration ID", registrationId);
            System.out.printf("%-18s: %d%n", "Queue Position", controller.getWaitingCount());
            System.out.printf("%-18s: %s%n",
                    "Status",
                    controller.getRegistrationStatusName(registrationId));

            System.out.println();
            
        }
    }

    private void displayGuestCategory(
            String guestId,
            boolean newGuest,
            String loyaltyTier) {

        System.out.println("\n===== GUEST CATEGORY =====");

        if (newGuest) {
            System.out.println("Category: STANDARD (new guest / no existing loyalty profile)");
            return;
        }

        if (loyaltyTier != null) {
            System.out.println("Category       : VIP LOYALTY MEMBER");
            System.out.println("Loyalty Tier   : " + loyaltyTier);
            System.out.println("Completed Stays: " + controller.getLoyaltyCompletedStays(guestId));
            
            return;
        }

        int completedStays = controller.getCompletedStayCount(guestId);
        int staysNeeded = controller.getStaysNeededForElite(guestId);

        System.out.println("Category       : STANDARD");
        System.out.println("Completed Stays: " + completedStays);
        System.out.println("Stays to ELITE : " + staysNeeded);
        
    }

    private void viewWaitingQueue() {
        System.out.println("\n===== STANDARD WAITING QUEUE  =====");

        String[][] waitingRows = controller.getStandardWaitingQueueDisplayData();
        int waitingCount = waitingRows.length;

        if (waitingCount == 0) {
            System.out.println("No Standard walk-in registrations are waiting.");
            return;
        }

        System.out.printf(
                "%-4s %-7s %-10s %-20s %-17s %-6s %-16s%n",
                "Pos", "Reg ID", "Guest ID", "Guest Name", "Room Type", "Party", "Arrival");
        System.out.println(
                "----------------------------------------------------------------------------------------");

        for (int i = 0; i < waitingRows.length; i++) {
            String[] registration = waitingRows[i];
            System.out.printf(
                    "%-4d %-7s %-10s %-20s %-17s %-6d %-16s%n",
                    i + 1,
                    registration[0],
                    registration[1],
                    shorten(registration[2], 20),
                    formatRoomType(registration[3]),
                    Integer.parseInt(registration[4]),
                    registration[5]);
        }

        System.out.println("\nTotal Standard Guests Waiting: " + waitingCount);
        System.out.println(
                "FIFO Head / Next Standard Guest: "
                + controller.getNextRegistrationId());
    }

    private void viewNextRegistration() {
        System.out.println("\n===== NEXT STANDARD GUEST  =====");

        String registrationId = controller.getNextRegistrationId();

        if (registrationId == null) {
            System.out.println("No Standard walk-in registrations are waiting.");
            return;
        }

        displayRegistrationDetails(registrationId);
    }

    private void checkInNextStandardGuest() {
        System.out.println("\n===== ASSIGN ROOM & CHECK IN NEXT STANDARD GUEST =====");

        String registrationId = controller.getNextRegistrationId();

        if (registrationId == null) {
            System.out.println("No Standard registration is waiting.");
            return;
        }

        String[] registration = controller.getRegistrationDisplayData(registrationId);

        System.out.println("\nNext Standard Guest:");
        displayRegistrationDetails(registrationId);

        String[][] suitableRooms = controller.getSuitableRoomsForNextStandardDisplayData();

        if (suitableRooms.length == 0) {
            Utility.printError(
                    "No suitable ready room can currently be assigned.");
            System.out.println(
                    "Possible reasons: matching rooms are occupied/not ready, or a waiting VIP has priority for the room.");
            
            return;
        }

        System.out.println("\n===== SUITABLE READY ROOMS =====");

        for (int i = 0; i < suitableRooms.length; i++) {
            String[] room = suitableRooms[i];
            System.out.printf(
                    "%d. Room %-5s | Type: %-17s | Floor: %-3s | Max Guests: %d | Status: %s%n",
                    i + 1,
                    room[0],
                    formatRoomType(room[1]),
                    room[2],
                    Integer.parseInt(room[3]),
                    room[4]);
        }

        System.out.println("0. Cancel Room Selection");

        int choice = readIntegerInRange(
                "Select Room (0-" + suitableRooms.length + "): ",
                0,
                suitableRooms.length);

        if (choice == 0) {
            System.out.println("Room selection cancelled. The guest remains at the FIFO head.");
            return;
        }

        String[] selectedRoom = suitableRooms[choice - 1];

        System.out.println("\nSelected Room: " + selectedRoom[0]
                + " (" + formatRoomType(selectedRoom[1]) + ")");

        String[] paymentPreview
            = controller
                    .getStandardPaymentPreviewDisplayData(
                            selectedRoom[0]);

        if (paymentPreview == null) {

            Utility.printError(
                    "Unable to prepare payment details for the selected room.");

            System.out.println(
                    "The registration remains in the FIFO queue.");

            return;
        }

        System.out.println(
                "\n===== PAYMENT SUMMARY =====");

        System.out.println(
                "Guest Name       : "
                + paymentPreview[0]);

        System.out.println(
                "Room Number      : "
                + paymentPreview[1]);

        System.out.println(
                "Room Type        : "
                + formatRoomType(paymentPreview[2]));

        System.out.println(
                "Room Rate        : RM"
                + paymentPreview[3]
                + " / night");

        System.out.println(
                "Number of Nights : "
                + paymentPreview[4]);

        System.out.println(
                "Total Amount     : RM"
                + paymentPreview[5]);

        if (!readYesNo(
                "Confirm payment of RM"
                + paymentPreview[5]
                + " and proceed with check-in? (Y/N): ")) {

            System.out.println(
                    "Payment cancelled. "
                    + "The registration remains in the FIFO queue.");

            return;
        }

        String[] booking
                = controller
                        .checkInNextStandardDisplayData(
                                selectedRoom[0]);

        if (booking == null) {
            Utility.printError(
                    "Unable to assign the selected room. Room readiness or VIP priority may have changed.");
            System.out.println("The registration remains in the FIFO queue.");
            return;
        }

        Utility.printSuccess("Room assigned and guest checked in successfully.");
        System.out.println("Registration ID    : " + registrationId);
        System.out.println("Confirmation Number: " + booking[0]);
        System.out.println("Guest Name         : " + booking[1]);
        System.out.println("Room Number        : " + booking[2]);
        System.out.println("Room Type          : " + formatRoomType(booking[3]));
        System.out.println("Actual Check-In    : " + booking[4]);
        System.out.println("Expected Check-Out : " + booking[5]);
        System.out.println("Registration Status: " + booking[6]);
        System.out.println("Payment ID         : " + booking[7]);
        System.out.println("Payment Amount     : RM" + booking[8]);
        System.out.println("Payment Status     : " + booking[9] + " (Completed)");
        System.out.println("Standard Guests Remaining: " + controller.getWaitingCount());
    }

    private void searchRegistration() {
        System.out.println("\n===== SEARCH WALK-IN REGISTRATION =====");

        String registrationId = readRegistrationId(
                "Enter Registration ID (e.g. R0001): ");

        if (!controller.registrationExists(registrationId)) {
            Utility.printError("Registration not found.");
            return;
        }

        System.out.println("\nRegistration found:");
        displayRegistrationDetails(registrationId);
    }

    private void cancelRegistration() {
        System.out.println("\n===== CANCEL WAITING STANDARD REGISTRATION =====");
        System.out.println("Hint: Only a registration with Standard WAITING status can be cancelled here.");

        String registrationId = readRegistrationId(
                "Enter Standard Registration ID (e.g. R0001): ");

        String status = controller.getRegistrationStatusName(registrationId);

        if (status == null) {
            Utility.printError("Registration not found.");
            return;
        }

        if ("VIP_WAITING".equals(status)) {
            Utility.printError(
                    "This is a VIP waiting registration. Cancel it from the VIP Allocation module.");
            return;
        }

        if (!"WAITING".equals(status)) {
            Utility.printError(
                    "Only a waiting Standard registration can be cancelled. Current status: "
                    + status);
            return;
        }

        System.out.println("\nStandard Registration to Cancel:");
        displayRegistrationDetails(registrationId);

        if (!readYesNo("Confirm cancellation? (Y/N): ")) {
            System.out.println("Cancellation aborted. The guest remains in the FIFO queue.");
            return;
        }

        String[] cancelled = controller.cancelStandardRegistrationDisplayData(registrationId);

        if (cancelled == null) {
            Utility.printError("Unable to cancel the Standard waiting registration.");
            return;
        }

        Utility.printSuccess("Standard waiting registration cancelled successfully.");
        System.out.println("Registration ID: " + cancelled[0]);
        System.out.println("Guest Name     : " + cancelled[1]);
        System.out.println("Status         : " + cancelled[2]);
    }

    private void generateStandardFifoWaitingTimeReport() {
        System.out.println(
                "\n===== STANDARD FIFO WAITING TIME REPORT OPTIONS =====");

        if (controller.getWaitingCount() == 0) {
            Utility.printInfo("No guests are currently in the FIFO waiting queue. Showing empty report...");
            new StandardFifoWaitingTimeRP().generateReport(controller, "", "", 0, 0, 1);
            return;
        }


        System.out.println(
                "Press Enter without typing anything to include all records.");

        System.out.print(
                "Search by Registration ID, Guest ID or Guest Name "
                + "(Enter = All): ");

        String keyword = scanner.nextLine().trim();
        String[] roomTypes = controller.getRoomTypeNames();

        System.out.println("\nFilter by Room Type:");
        System.out.println("0. All Room Types");

        for (int i = 0; i < roomTypes.length; i++) {
            System.out.println((i + 1) + ". " + formatRoomType(roomTypes[i]));
        }

        int roomTypeChoice = readIntegerInRange(
                "Select Room Type (0-" + roomTypes.length + "): ",
                0,
                roomTypes.length);

        String roomTypeFilter = "";
        if (roomTypeChoice > 0) {
            roomTypeFilter = roomTypes[roomTypeChoice - 1];
        }

        int maximumOccupancy = controller.getMaximumRoomCapacity();

        int minimumGuests = readIntegerInRange(
                "Minimum Party Size (0 = All, 1-" + maximumOccupancy + "): ",
                0,
                maximumOccupancy);

        int minimumWaitingMinutes = readNonNegativeInteger(
                "Minimum Waiting Time in Minutes (0 = All): ");

        System.out.println("\nSort Report By:");
        System.out.println("1. FIFO Order / Earliest Arrival First");
        System.out.println("2. Largest Party Size First");

        int sortOption = readIntegerInRange(
                "Select Sort Option (1-2): ", 1, 2);

        StandardFifoWaitingTimeRP report = new StandardFifoWaitingTimeRP();
        report.generateReport(
                controller,
                keyword,
                roomTypeFilter,
                minimumGuests,
                minimumWaitingMinutes,
                sortOption);
    }

    private void generateWalkInArrivalPatternReport() {
        System.out.println(
                "\n===== WALK-IN ARRIVAL PATTERN REPORT OPTIONS =====");

        if (controller.getTotalRegistrationCount() == 0) {
            Utility.printInfo("No walk-in registration records are available. Showing empty report...");
            new WalkInArrivalPatternRP().generateReport(controller, null, null, "", 0, 1);
            return;
        }

        System.out.println("Date format: YYYY-MM-DD");
        System.out.println("Press Enter to include all dates.");

        LocalDate startDate = readOptionalDate(
                "Start Date (Enter = No Start Date): ");
        LocalDate endDate = readOptionalDate(
                "End Date (Enter = No End Date): ");

        while (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            Utility.printError("End Date cannot be earlier than Start Date.");
            endDate = readOptionalDate(
                    "Re-enter End Date (Enter = No End Date): ");
        }

        String[] roomTypes = controller.getRoomTypeNames();

        System.out.println("\nFilter by Room Type:");
        System.out.println("0. All Room Types");

        for (int i = 0; i < roomTypes.length; i++) {
            System.out.println((i + 1) + ". " + formatRoomType(roomTypes[i]));
        }

        int roomTypeChoice = readIntegerInRange(
                "Select Room Type (0-" + roomTypes.length + "): ",
                0,
                roomTypes.length);

        String roomTypeFilter = "";
        if (roomTypeChoice > 0) {
            roomTypeFilter = roomTypes[roomTypeChoice - 1];
        }

        int maximumOccupancy = controller.getMaximumRoomCapacity();
        int minimumGuests = readIntegerInRange(
                "Minimum Party Size (0 = All, 1-" + maximumOccupancy + "): ",
                0,
                maximumOccupancy);

        System.out.println("\nSort Report Display By:");
        System.out.println("1. Arrival Time (Earliest First)");
        System.out.println("2. Arrival Time (Latest First)");
        System.out.println("3. Party Size (Largest First)");

        int sortOption = readIntegerInRange(
                "Select Sort Option (1-3): ", 1, 3);

        WalkInArrivalPatternRP report = new WalkInArrivalPatternRP();
        report.generateReport(
                controller,
                startDate,
                endDate,
                roomTypeFilter,
                minimumGuests,
                sortOption);
    }

    private void displayVipAddError(int result) {
        switch (result) {
            case VipPriorityController.REGISTRATION_ALREADY_QUEUED:
                Utility.printError(
                        "This registration is already in the VIP priority heap.");
                break;
            case VipPriorityController.GUEST_ALREADY_QUEUED:
                Utility.printError(
                        "This guest is already waiting in the VIP priority heap.");
                break;
            default:
                Utility.printError(
                        "Unable to route the VIP registration. Please verify the registration details.");
                break;
        }
    }

    private void displayRegistrationDetails(String registrationId) {
        String[] registration = controller.getRegistrationDisplayData(registrationId);
        if (registration == null) {
            return;
        }

        System.out.println("Registration ID   : " + registration[0]);
        System.out.println("Guest ID          : " + registration[1]);
        System.out.println("Guest Name        : " + registration[2]);
        System.out.println("Phone Number      : " + formatPhoneNo(registration[3]));
        System.out.println("Requested Room    : " + formatRoomType(registration[4]));
        System.out.println("Assigned Room     : " + registration[5]);
        System.out.println("Number of Guests  : " + registration[6]);
        System.out.println("Registration Time : " + registration[7]);
        System.out.println("Actual Check-In   : " + registration[8]);
        System.out.println("Expected Check-Out: " + registration[9]);
        System.out.println("Actual Check-Out  : " + registration[10]);
        System.out.println("Status            : " + registration[11]);
    }

    /**
     * Searches all saved guest profiles by full name. If several guests share
     * the same name, the staff member must select the correct profile before
     * the registration continues.
     */
    private String selectExistingGuestByName() {
        while (true) {
            String guestName = readGuestName(
                    "Enter Existing Guest/Member Full Name (e.g. Tan Wei Jie): ");

            String[][] matches = controller.searchGuestDisplayDataByName(guestName);

            if (matches.length == 0) {
                Utility.printError("No existing guest or member was found with that name.");

                if (readYesNo("Search using another name? (Y/N): ")) {
                    continue;
                }

                return null;
            }

            if (matches.length == 1) {
                String[] match = matches[0];
                System.out.println("\nExisting guest profile found:");
                displayGuestLookupDetails(match[0]);

                if (readYesNo("Use this guest profile? (Y/N): ")) {
                    return match[0];
                }

                System.out.println("Please search again using the guest's full name.");
                continue;
            }

            System.out.println("\nMultiple guests have the same name.");
            System.out.println("Select the correct guest using the details below:");
            System.out.printf(
                    "%-4s %-10s %-24s %-16s %-18s%n",
                    "No.", "Guest ID", "Guest Name", "Phone Number", "Category");
            System.out.println(
                    "----------------------------------------------------------------------------");

            for (int i = 0; i < matches.length; i++) {
                String[] match = matches[i];
                System.out.printf(
                        "%-4d %-10s %-24s %-16s %-18s%n",
                        i + 1,
                        match[0],
                        shorten(match[1], 24),
                        formatPhoneNo(match[2]),
                        match[3]);
            }

            int selection = readIntegerInRange(
                    "Select Guest (1-" + matches.length + "): ",
                    1,
                    matches.length);

            String selectedGuestId = matches[selection - 1][0];
            System.out.println("\nSelected guest profile:");
            displayGuestLookupDetails(selectedGuestId);
            return selectedGuestId;
        }
    }

    private void displayGuestLookupDetails(String guestId) {
        String[] guest = controller.getGuestDisplayDataById(guestId);
        if (guest == null) {
            return;
        }

        System.out.println("Guest ID     : " + guest[0]);
        System.out.println("Guest Name   : " + guest[1]);
        System.out.println("Phone Number : " + formatPhoneNo(guest[2]));
        System.out.println("Category     : " + guest[3]);
    }

    private String readGuestName(String message) {
        while (true) {
            String input = readNonEmptyString(message);

            if (Utility.isValidPersonName(input)) {
                return input;
            }

            Utility.printError(
                    "Enter a valid name (2-50 characters; letters, spaces, apostrophes or hyphens only).");
        }
    }

    private Long readPhoneNumber() {
        while (true) {
            String input = readNonEmptyString(
                    "Enter Phone Number (e.g. 0123456789): ");

            // Reuse the existing phone-number validation in Utility.java.
            if (!Utility.isValidPhoneNo(input)) {
                Utility.printError(
                        "Invalid phone number. Please enter 10-11 digits starting with 01 (e.g. 0123456789).");
                continue;
            }

            try {
                // Guest.phoneNo is stored as Long, so the leading 0 is removed here.
                // RegistrationController normalizes it when comparing/saving records.
                return Long.valueOf(input);
            } catch (NumberFormatException exception) {
                Utility.printError("Unable to read the phone number. Please try again.");
            }
        }
    }

    /**
     * Display helper only. Phone-number validation continues to use
     * Utility.isValidPhoneNo().
     */
    private String formatPhoneNo(Long phoneNo) {
        return phoneNo == null ? "N/A" : formatPhoneNo(String.valueOf(phoneNo));
    }
    private String formatPhoneNo(String phoneNo) {
        if (phoneNo == null) {
            return "N/A";
        }

        String digits = phoneNo;
        if (digits.startsWith("60")) {
            return "+" + digits;
        }
        if (digits.startsWith("1")
                && (digits.length() == 9 || digits.length() == 10)) {
            return "0" + digits;
        }
        return digits;
    }


    private String readRoomType(
            int numberOfGuests,
            String loyaltyTier) {
        String[] allTypes = controller.getRoomTypeNames();
        String[] eligibleTypes = new String[allTypes.length];
        int eligibleCount = 0;

        for (String roomType : allTypes) {
            int capacity = controller.getMaximumCapacityForRoomType(roomType);
            if (capacity >= numberOfGuests) {
                eligibleTypes[eligibleCount++] = roomType;
            }
        }

        if (eligibleCount == 0) {
            return null;
        }

        while (true) {
            System.out.println(
                    "\nRoom Types Suitable for "
                    + numberOfGuests
                    + " Guest(s):");

            for (int i = 0; i < eligibleCount; i++) {
                String roomType = eligibleTypes[i];
                int capacity = controller.getMaximumCapacityForRoomType(roomType);

                int readyNow = loyaltyTier != null
                        ? controller.getReadyRoomCountForVipRequest(
                                roomType,
                                numberOfGuests,
                                loyaltyTier)
                        : controller.getReadyRoomCountForStandardRequest(
                                roomType,
                                numberOfGuests);

                System.out.printf(
                        "%d. %-17s | Max Occupancy: %d"
                        + " | Rate: RM %.2f/night"
                        + " | Ready Now: %d%n",
                        i + 1,
                        formatRoomType(roomType),
                        capacity,
                        controller.getRoomTypePricePerDay(roomType),
                        readyNow);
            }

            int choice = readInteger(
                    "Select Room Type (1-"
                    + eligibleCount
                    + "): ");

            if (choice >= 1 && choice <= eligibleCount) {
                return eligibleTypes[choice - 1];
            }

            Utility.printError("Invalid room type selection.");
        }
    }

    private String readRegistrationId(String message) {
        while (true) {
            String input = readNonEmptyString(message).toUpperCase();

            if (Utility.isValidRegistrationId(input)) {
                return input;
            }

            Utility.printError(
                    "Invalid Registration ID. Use R followed by 4 digits, e.g. R0001.");
        }
    }

    private boolean readYesNo(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();

            if (Utility.isValidYesNo(input)) {
                return input.equalsIgnoreCase("Y");
            }

            Utility.printError("Please enter Y for Yes or N for No.");
        }
    }

    private String readNonEmptyString(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();

            if (!input.isEmpty()) {
                return input;
            }

            Utility.printError("Input cannot be empty.");
        }
    }

    private int readMenuChoice(
            String message,
            int minimum,
            int maximum) {

        return readIntegerInRange(message, minimum, maximum);
    }

    private int readIntegerInRange(
            String message,
            int minimum,
            int maximum) {

        while (true) {
            int number = readInteger(message);

            if (number >= minimum && number <= maximum) {
                return number;
            }

            Utility.printError(
                    "Enter a number from " + minimum + " to " + maximum + ".");
        }
    }

    private int readNonNegativeInteger(String message) {

        while (true) {

            int number = readInteger(message);

            if (number >= 0) {
                return number;
            }

            Utility.printError(
                    "Please enter 0 or a positive whole number.");
        }
    }

    private LocalDate readOptionalDate(
            String message) {

        while (true) {

            System.out.print(message);

            String input
                    = scanner.nextLine().trim();

            if (input.isEmpty()) {
                return null;
            }

            try {

                return LocalDate.parse(input);

            } catch (DateTimeParseException e) {

                Utility.printError(
                        "Invalid date. "
                        + "Please use YYYY-MM-DD, "
                        + "for example 2026-08-13.");
            }
        }
    }

    private int readInteger(String message) {
        while (true) {
            System.out.print(message);

            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException exception) {
                Utility.printError("Please enter a valid whole number.");
            }
        }
    }

    private String formatRoomType(String roomType) {
        return roomType == null ? "N/A" : roomType.replace("_", " ");
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "N/A" : dateTime.format(DATE_TIME_FORMAT);
    }

    private String shorten(String text, int maximumLength) {
        if (text == null) {
            return "";
        }

        if (text.length() <= maximumLength) {
            return text;
        }

        return text.substring(0, Math.max(0, maximumLength - 3)) + "...";
    }
}