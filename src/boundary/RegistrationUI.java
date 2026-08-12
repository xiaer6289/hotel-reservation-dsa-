package boundary;

import control.RegistrationController;
import control.VipPriorityController;
import entity.Booking;
import entity.Guest;
import entity.LoyaltyProfile;
import entity.RegistrationStatus;
import entity.Room;
import entity.RoomType;
import entity.WalkInRegistration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
            choice = readMenuChoice("Enter choice (0-6): ", 0, 6);

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
                case 0:
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    break;
            }

            System.out.println();
        } while (choice != 0);
    }

    private void displayMenu() {
        System.out.println("============================================");
        System.out.println(" WALK-IN REGISTRATION & STANDARD BOOKING");
        System.out.println("============================================");
        System.out.println("1. Register Walk-In Guest");
        System.out.println("2. View Standard Waiting Queue (FIFO)");
        System.out.println("3. View Next Standard Guest (FIFO Head)");
        System.out.println("4. Assign Ready Room & Check In Next Standard Guest");
        System.out.println("5. Search Walk-In Registration by ID");
        System.out.println("6. Cancel Waiting Standard Registration");
        System.out.println("0. Return to Main Menu");
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
        System.out.println("Hint: Existing guests/members are searched by full name. New guests will register a new profile.");

        boolean existingGuestOrMember = readYesNo(
                "Is this an existing guest or loyalty member? (Y/N): ");

        Guest guest = null;
        boolean newGuest = !existingGuestOrMember;
        String newGuestName = null;
        Long phoneNumber = null;

        if (existingGuestOrMember) {
            guest = selectExistingGuestByName();

            if (guest == null) {
                System.out.println("Registration cancelled. No booking request was created.");
                return;
            }

            phoneNumber = guest.getPhoneNo();

            if (controller.hasActiveRegistrationOrStay(guest.getGuestId())) {
                Utility.printError(
                        "This guest already has a waiting registration or is currently checked in.");
                return;
            }
        } else {
            System.out.println("\n===== NEW GUEST REGISTRATION =====");
            System.out.println("A new guest profile will be created after the walk-in details are confirmed.");

            newGuestName = readGuestName(
                    "Enter Guest Full Name (e.g. Tan Wei Jie): ");
            phoneNumber = readPhoneNumber();

            Guest guestWithSamePhone = controller.searchGuestByPhoneNo(phoneNumber);
            if (guestWithSamePhone != null) {
                Utility.printError(
                        "This phone number is already linked to an existing guest profile.");
                System.out.println("Guest ID     : " + guestWithSamePhone.getGuestId());
                System.out.println("Guest Name   : " + guestWithSamePhone.getName());
                System.out.println("Phone Number : " + formatPhoneNo(guestWithSamePhone.getPhoneNo()));
                System.out.println("Please register this person as an existing guest instead of creating a duplicate profile.");
                return;
            }
        }

        LoyaltyProfile loyaltyProfile = null;

        if (!newGuest) {
            LoyaltyProfile beforeRefresh
                    = controller.searchLoyaltyProfileByGuestId(guest.getGuestId());

            loyaltyProfile
                    = controller.refreshLoyaltyProfileByGuestId(guest.getGuestId());

            if (beforeRefresh == null && loyaltyProfile != null) {
                Utility.printSuccess(
                        "Loyalty requirement reached. VIP membership activated automatically.");
            }
        }

        displayGuestCategory(guest, newGuest, loyaltyProfile);

        int maximumOccupancy = controller.getMaximumRoomCapacity();
        if (maximumOccupancy <= 0) {
            Utility.printError("No room inventory is currently configured.");
            return;
        }

        int numberOfGuests = readIntegerInRange(
                "Enter Number of Guests (1-" + maximumOccupancy
                + ", one room per registration): ",
                1,
                maximumOccupancy);

        String roomType = readRoomType(numberOfGuests);

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

        int readyRoomCount = loyaltyProfile == null
                ? controller.getReadyRoomCountForStandardRequest(
                        roomType, numberOfGuests)
                : controller.getReadyRoomCountForRequest(
                        roomType, numberOfGuests);

        System.out.println("\n===== REGISTRATION SUMMARY =====");
        System.out.println("Guest Name        : "
                + (newGuest ? newGuestName : guest.getName()));
        System.out.println("Phone Number      : "
                + formatPhoneNo(phoneNumber));
        System.out.println("Guest Category    : "
                + (loyaltyProfile == null
                        ? "STANDARD"
                        : "VIP - " + loyaltyProfile.getTier()));
        System.out.println("Requested Room    : " + formatRoomType(roomType));
        System.out.println("Number of Guests  : " + numberOfGuests);
        System.out.println("Length of Stay    : " + numberOfNights + " night(s)");
        System.out.println("Arrival Time      : " + formatDateTime(arrivalTime));
        System.out.println("Expected Check-Out: " + formatDateTime(expectedCheckOut));
        System.out.println("Hotel Check-Out   : 12:00 PM");
        System.out.println("Suitable Ready Rooms Now: " + readyRoomCount);

        if (readyRoomCount == 0) {
            if (loyaltyProfile == null) {
                System.out.println(
                        "Room Status        : No suitable room is ready now; guest will wait in the Standard FIFO queue.");
            } else {
                System.out.println(
                        "Room Status        : No suitable room is ready now; guest will wait in the VIP priority heap.");
            }
        } else if (loyaltyProfile == null) {
            System.out.println(
                    "Room Status        : Suitable room exists, but existing Standard FIFO order still applies.");
        } else {
            System.out.println(
                    "Room Status        : Suitable room exists; allocation will follow VIP tier priority.");
        }

        if (!readYesNo("Confirm this walk-in registration? (Y/N): ")) {
            System.out.println("Registration cancelled. No registration record was created.");
            return;
        }

        if (newGuest) {
            guest = controller.addNewGuest(newGuestName, phoneNumber);

            if (guest == null) {
                Utility.printError(
                        "Unable to create the new guest profile. The phone number may already be registered.");
                return;
            }

            Utility.printSuccess("New guest profile created successfully.");
            System.out.println("Generated Guest ID: " + guest.getGuestId());
        }

        String registrationId = controller.generateRegistrationId();

        WalkInRegistration registration = new WalkInRegistration(
                registrationId,
                guest,
                roomType,
                numberOfGuests,
                null,
                expectedCheckOut);

        registration.setRegistrationTime(arrivalTime);

        if (loyaltyProfile != null) {
            int result = controller.addVipRegistration(
                    registration,
                    loyaltyProfile);

            if (result != VipPriorityController.ADD_SUCCESS) {
                displayVipAddError(result);
                return;
            }

            Utility.printSuccess(
                    "VIP walk-in registered and routed to the VIP priority heap.");
            System.out.println("Registration ID : " + registrationId);
            System.out.println("Member ID       : " + loyaltyProfile.getMemberId());
            System.out.println("Loyalty Tier    : " + loyaltyProfile.getTier());
            System.out.println("Status          : " + registration.getStatus());
            System.out.println("VIPs Waiting    : " + controller.getVipWaitingCount());
            System.out.println(
                    "Next step: Use the VIP Allocation module for tier-priority room assignment.");
        } else {
            controller.addStandardRegistration(registration);

            Utility.printSuccess(
                    "Standard walk-in registered and appended to the FIFO queue.");
            System.out.println("Registration ID : " + registrationId);
            System.out.println("Queue Position  : " + controller.getWaitingCount());
            System.out.println("Status          : " + registration.getStatus());
            System.out.println(
                    "Next step: Process the FIFO head using menu option 4 when a suitable room is ready.");
        }
    }

    private void displayGuestCategory(
            Guest guest,
            boolean newGuest,
            LoyaltyProfile loyaltyProfile) {

        System.out.println("\n===== GUEST CATEGORY =====");

        if (newGuest) {
            System.out.println("Category: STANDARD (new guest / no existing loyalty profile)");
            return;
        }

        if (loyaltyProfile != null) {
            System.out.println("Category       : VIP LOYALTY MEMBER");
            System.out.println("Member ID      : " + loyaltyProfile.getMemberId());
            System.out.println("Loyalty Tier   : " + loyaltyProfile.getTier());
            System.out.println("Completed Stays: " + loyaltyProfile.getCompletedStays());
            System.out.println(
                    "Routing        : VIP priority heap (not the Standard FIFO queue)");
            return;
        }

        int completedStays = controller.getCompletedStayCount(guest.getGuestId());
        int staysNeeded = controller.getStaysNeededForElite(guest.getGuestId());

        System.out.println("Category       : STANDARD");
        System.out.println("Completed Stays: " + completedStays);
        System.out.println("Stays to ELITE : " + staysNeeded);
        System.out.println("Routing        : Standard FIFO queue");
    }

    private void viewWaitingQueue() {
        System.out.println("\n===== STANDARD WAITING QUEUE (FIFO) =====");
        System.out.println(
                "FIFO rule: earlier Standard registrations stay ahead of later Standard registrations.");

        int waitingCount = controller.getWaitingCount();

        if (waitingCount == 0) {
            System.out.println("No Standard walk-in registrations are waiting.");
            return;
        }

        System.out.printf(
                "%-4s %-7s %-10s %-20s %-17s %-6s %-16s%n",
                "Pos", "Reg ID", "Guest ID", "Guest Name", "Room Type", "Party", "Arrival");
        System.out.println(
                "----------------------------------------------------------------------------------------");

        for (int i = 0; i < waitingCount; i++) {
            WalkInRegistration registration = controller.getRegistrationAt(i);

            System.out.printf(
                    "%-4d %-7s %-10s %-20s %-17s %-6d %-16s%n",
                    i + 1,
                    registration.getRegistrationId(),
                    registration.getGuest().getGuestId(),
                    shorten(registration.getGuest().getName(), 20),
                    formatRoomType(registration.getRequestedRoomType()),
                    registration.getNumberOfGuests(),
                    registration.getRegistrationTime().format(DATE_TIME_FORMAT));
        }

        System.out.println("\nTotal Standard Guests Waiting: " + waitingCount);
        System.out.println(
                "FIFO Head / Next Standard Guest: "
                + controller.getNextRegistration().getRegistrationId());
    }

    private void viewNextRegistration() {
        System.out.println("\n===== NEXT STANDARD GUEST (FIFO HEAD) =====");

        WalkInRegistration registration = controller.getNextRegistration();

        if (registration == null) {
            System.out.println("No Standard walk-in registrations are waiting.");
            return;
        }

        displayRegistrationDetails(registration);
    }

    private void checkInNextStandardGuest() {
        System.out.println("\n===== ASSIGN ROOM & CHECK IN NEXT STANDARD GUEST =====");
        System.out.println(
                "Only the FIFO head is processed; a later Standard guest cannot bypass it.");

        WalkInRegistration registration = controller.getNextRegistration();

        if (registration == null) {
            System.out.println("No Standard registration is waiting.");
            return;
        }

        System.out.println("\nNext Standard Guest:");
        displayRegistrationDetails(registration);

        Room[] suitableRooms = controller.getSuitableRoomsForNextStandard();

        if (suitableRooms.length == 0) {
            Utility.printError(
                    "No suitable ready room can currently be assigned to the FIFO head.");
            System.out.println(
                    "Possible reasons: matching rooms are occupied/not ready, or a waiting VIP has priority for the room.");
            System.out.println("The Standard registration remains at the FIFO head.");
            return;
        }

        System.out.println("\n===== SUITABLE READY ROOMS =====");

        for (int i = 0; i < suitableRooms.length; i++) {
            Room room = suitableRooms[i];

            System.out.printf(
                    "%d. Room %-5s | Type: %-17s | Floor: %-3s | Max Guests: %d | Status: %s%n",
                    i + 1,
                    room.getRoomNumber(),
                    formatRoomType(room.getRoomType()),
                    room.getFloor(),
                    room.getNoOfGuest(),
                    room.getStatusLabel());
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

        Room selectedRoom = suitableRooms[choice - 1];

        System.out.println("\nSelected Room: " + selectedRoom.getRoomNumber()
                + " (" + formatRoomType(selectedRoom.getRoomType()) + ")");

        if (!readYesNo(
                "Confirm room assignment and check-in for "
                + registration.getGuest().getName() + "? (Y/N): ")) {

            System.out.println("Check-in cancelled. The registration remains in the FIFO queue.");
            return;
        }

        Booking booking = controller.checkInNextStandard(
                selectedRoom.getRoomNumber());

        if (booking == null) {
            Utility.printError(
                    "Unable to assign the selected room. Room readiness or VIP priority may have changed.");
            System.out.println("The registration remains in the FIFO queue.");
            return;
        }

        Utility.printSuccess("Room assigned and guest checked in successfully.");
        System.out.println("Registration ID    : " + registration.getRegistrationId());
        System.out.println("Confirmation Number: " + booking.getConfirmationNo());
        System.out.println("Guest Name         : " + booking.getGuest().getName());
        System.out.println("Room Number        : " + booking.getRoom().getRoomNumber());
        System.out.println("Room Type          : "
                + formatRoomType(booking.getRoom().getRoomType()));
        System.out.println("Actual Check-In    : "
                + formatDateTime(booking.getRoom().getCheckInDateTime()));
        System.out.println("Expected Check-Out : "
                + formatDateTime(booking.getRoom().getCheckOutDateTime()));
        System.out.println("Registration Status: " + registration.getStatus());
        System.out.println("Standard Guests Remaining: " + controller.getWaitingCount());
    }

    private void searchRegistration() {
        System.out.println("\n===== SEARCH WALK-IN REGISTRATION =====");
        System.out.println("Hint: Registration ID format is R followed by 4 digits, e.g. R0001.");

        String registrationId = readRegistrationId(
                "Enter Registration ID (e.g. R0001): ");

        WalkInRegistration registration
                = controller.searchRegistrationById(registrationId);

        if (registration == null) {
            Utility.printError("Registration not found.");
            return;
        }

        System.out.println("\nRegistration found:");
        displayRegistrationDetails(registration);
    }

    private void cancelRegistration() {
        System.out.println("\n===== CANCEL WAITING STANDARD REGISTRATION =====");
        System.out.println("Hint: Only a registration with Standard WAITING status can be cancelled here.");

        String registrationId = readRegistrationId(
                "Enter Standard Registration ID (e.g. R0001): ");

        WalkInRegistration registration
                = controller.searchRegistrationById(registrationId);

        if (registration == null) {
            Utility.printError("Registration not found.");
            return;
        }

        if (registration.getStatus() == RegistrationStatus.VIP_WAITING) {
            Utility.printError(
                    "This is a VIP waiting registration. Cancel it from the VIP Allocation module.");
            return;
        }

        if (registration.getStatus() != RegistrationStatus.WAITING) {
            Utility.printError(
                    "Only a waiting Standard registration can be cancelled. Current status: "
                    + registration.getStatus());
            return;
        }

        System.out.println("\nStandard Registration to Cancel:");
        displayRegistrationDetails(registration);

        if (!readYesNo("Confirm cancellation? (Y/N): ")) {
            System.out.println("Cancellation aborted. The guest remains in the FIFO queue.");
            return;
        }

        WalkInRegistration cancelled
                = controller.cancelRegistrationById(registrationId);

        if (cancelled == null) {
            Utility.printError("Unable to cancel the Standard waiting registration.");
            return;
        }

        Utility.printSuccess("Standard waiting registration cancelled successfully.");
        System.out.println("Registration ID: " + cancelled.getRegistrationId());
        System.out.println("Guest Name     : " + cancelled.getGuest().getName());
        System.out.println("Status         : " + cancelled.getStatus());
    }

    private void displayVipAddError(int result) {
        switch (result) {
            case VipPriorityController.DUPLICATE_MEMBER_ID:
                Utility.printError(
                        "This loyalty member is already waiting in the VIP priority heap.");
                break;
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

    private void displayRegistrationDetails(
            WalkInRegistration registration) {

        System.out.println("Registration ID : " + registration.getRegistrationId());
        System.out.println("Guest ID        : " + registration.getGuest().getGuestId());
        System.out.println("Guest Name      : " + registration.getGuest().getName());
        System.out.println("Phone Number    : "
                + formatPhoneNo(registration.getGuest().getPhoneNo()));
        System.out.println("Requested Room  : "
                + formatRoomType(registration.getRequestedRoomType()));
        System.out.println("Number of Guests: " + registration.getNumberOfGuests());
        System.out.println("Registration Time: "
                + formatDateTime(registration.getRegistrationTime()));
        System.out.println("Actual Check-In : "
                + (registration.getCheckInDateTime() == null
                        ? "Pending room assignment"
                        : formatDateTime(registration.getCheckInDateTime())));
        System.out.println("Expected Check-Out: "
                + formatDateTime(registration.getCheckOutDateTime()));
        System.out.println("Status           : " + registration.getStatus());
    }

    /**
     * Searches all saved guest profiles by full name. If several guests share
     * the same name, the staff member must select the correct profile before
     * the registration continues.
     */
    private Guest selectExistingGuestByName() {
        while (true) {
            String guestName = readGuestName(
                    "Enter Existing Guest/Member Full Name (e.g. Tan Wei Jie): ");

            Guest[] matches = controller.searchGuestsByName(guestName);

            if (matches.length == 0) {
                Utility.printError("No existing guest or member was found with that name.");

                if (readYesNo("Search using another name? (Y/N): ")) {
                    continue;
                }

                return null;
            }

            if (matches.length == 1) {
                Guest match = matches[0];
                System.out.println("\nExisting guest profile found:");
                displayGuestLookupDetails(match);

                if (readYesNo("Use this guest profile? (Y/N): ")) {
                    return match;
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
                Guest match = matches[i];
                LoyaltyProfile profile
                        = controller.searchLoyaltyProfileByGuestId(match.getGuestId());
                String category = profile == null
                        ? "STANDARD"
                        : "VIP - " + profile.getTier();

                System.out.printf(
                        "%-4d %-10s %-24s %-16s %-18s%n",
                        i + 1,
                        match.getGuestId(),
                        shorten(match.getName(), 24),
                        formatPhoneNo(match.getPhoneNo()),
                        category);
            }

            int selection = readIntegerInRange(
                    "Select Guest (1-" + matches.length + "): ",
                    1,
                    matches.length);

            Guest selectedGuest = matches[selection - 1];
            System.out.println("\nSelected guest profile:");
            displayGuestLookupDetails(selectedGuest);
            return selectedGuest;
        }
    }

    private void displayGuestLookupDetails(Guest guest) {
        LoyaltyProfile profile
                = controller.searchLoyaltyProfileByGuestId(guest.getGuestId());

        System.out.println("Guest ID     : " + guest.getGuestId());
        System.out.println("Guest Name   : " + guest.getName());
        System.out.println("Phone Number : " + formatPhoneNo(guest.getPhoneNo()));
        System.out.println("Category     : "
                + (profile == null ? "STANDARD" : "VIP - " + profile.getTier()));

        if (profile != null) {
            System.out.println("Member ID    : " + profile.getMemberId());
        }
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
        if (phoneNo == null) {
            return "N/A";
        }

        String digits = String.valueOf(phoneNo);

        if (digits.startsWith("60")) {
            return "+" + digits;
        }

        if (digits.startsWith("1")
                && (digits.length() == 9 || digits.length() == 10)) {
            return "0" + digits;
        }

        return digits;
    }

    private String readRoomType(int numberOfGuests) {
        RoomType[] allTypes = RoomType.values();
        RoomType[] eligibleTypes = new RoomType[allTypes.length];
        int eligibleCount = 0;

        for (RoomType roomType : allTypes) {
            int capacity = controller.getMaximumCapacityForRoomType(
                    roomType.name());

            if (capacity >= numberOfGuests) {
                eligibleTypes[eligibleCount++] = roomType;
            }
        }

        if (eligibleCount == 0) {
            return null;
        }

        while (true) {
            System.out.println("\nRoom Types Suitable for " + numberOfGuests + " Guest(s):");

            for (int i = 0; i < eligibleCount; i++) {
                RoomType roomType = eligibleTypes[i];
                int capacity = controller.getMaximumCapacityForRoomType(
                        roomType.name());

                System.out.printf(
                        "%d. %-17s | Max Occupancy: %d | Rate: RM %.2f/night%n",
                        i + 1,
                        formatRoomType(roomType.name()),
                        capacity,
                        roomType.getPricePerDay());
            }

            int choice = readInteger(
                    "Select Room Type (1-" + eligibleCount + "): ");

            if (choice >= 1 && choice <= eligibleCount) {
                return eligibleTypes[choice - 1].name();
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