package boundary;

import control.RegistrationController;
import control.VipPriorityController;
import entity.Booking;
import entity.Guest;
<<<<<<< HEAD
import entity.LoyaltyTier;
import entity.Room;
=======
import entity.LoyaltyProfile;
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
import entity.RoomType;
import entity.WalkInRegistration;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Scanner;
import utility.Utility;

/**
 * Handles walk-in registration, Standard FIFO viewing and Standard room
 * assignment/check-in.
 *
 * @author Lai Jen Feng
 */
public class RegistrationUI {

    private final RegistrationController controller;
    private final Scanner scanner;

<<<<<<< HEAD
    public RegistrationUI() {
        this(
                new RegistrationController(),
                new Scanner(System.in)
        );
    }
=======
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1

    public RegistrationUI(
            RegistrationController controller) {

        this(
                controller,
                new Scanner(System.in)
        );
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
            choice = readInteger("Enter choice: ");

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
                    System.out.println(
                            "Returning to Main Menu...");
                    break;

                default:
                    Utility.printError(
                            "Invalid choice. Please try again.");
                    break;
            }

            System.out.println();

        } while (choice != 0);
    }

    private void displayMenu() {
        System.out.println(
                "==========================================");

        System.out.println(
                " WALK-IN REGISTRATION & STANDARD BOOKING");

        System.out.println(
                "==========================================");

        System.out.println(
                "1. Add Walk-In Registration");

        System.out.println(
                "2. View Standard Waiting Queue");

        System.out.println(
                "3. View Next Standard Registration");

        System.out.println(
                "4. Assign Room & Check In Next Standard Guest");

        System.out.println(
                "5. Search Registration by ID");

        System.out.println(
                "6. Cancel Registration");

        System.out.println(
                "0. Return to Main Menu");
    }

    private void addWalkInRegistration() {
        System.out.println(
                "\n===== ADD WALK-IN REGISTRATION =====");

        String guestId = readNonEmptyString(
                "Enter Guest ID: ");

        /*
         * Search the Guest record saved by the shared GuestDao.
         */
        Guest guest = controller.searchGuestById(
                guestId);

        if (guest != null) {
            System.out.println(
                    "\nExisting guest found.");

            System.out.println(
                    "Guest ID: "
                    + guest.getGuestId());

            System.out.println(
                    "Guest Name: "
                    + guest.getName());

            System.out.println(
                    "Phone Number: "
                    + guest.getPhoneNo());

             if (controller.hasActiveRegistrationOrStay(   
                        guest.getGuestId())) {

                Utility.printError(
                        "This guest already has a waiting registration "
                        + "or is currently checked in.");

                return;
                }

<<<<<<< HEAD
        } else {
            System.out.println(
                    "\nGuest not found.");
=======
                LoyaltyProfile loyaltyProfile
                                = controller.searchLoyaltyProfileByGuestId(
                                                guest.getGuestId());

                if (loyaltyProfile != null) {
                        System.out.println("\nExisting loyalty membership detected.");
                        System.out.println(
                                        "Member ID: "
                                                        + loyaltyProfile.getMemberId());
                        System.out.println(
                                        "Loyalty Tier: "
                                                        + loyaltyProfile.getTier());
                        System.out.println(
                                        "VIP Priority: "
                                                        + loyaltyProfile.getTier()
                                                                        .getPriority());
                } else {
                        System.out.println(
                                        "\nLoyalty Status: STANDARD / NON-MEMBER");
                        System.out.println(
                                        "No existing loyalty tier was found. "
                                                        + "This registration will use the standard queue.");
                }

                String roomType = readRoomType();
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1

            System.out.println(
                    "Registering a new guest.");

            String guestName = readAlphabeticString(
                    "Enter Guest Name: ");

            Long phoneNumber = readPhoneNumber();

            guest = controller.addNewGuest(
                    guestId,
                    guestName,
                    phoneNumber);

            if (guest == null) {
                Utility.printError(
                        "Unable to register new guest.");
                return;
            }

<<<<<<< HEAD
            Utility.printSuccess(
                    "New guest saved successfully.");
=======
                        System.out.println(
                                        "Check-in date and time must be "
                                                        + "in the future.");
                }

                LocalDateTime checkOutDateTime;

                do {
                        checkOutDateTime = readDateTimeParts("Check-Out");

                        if (!checkOutDateTime.isAfter(
                                        checkInDateTime)) {

                                System.out.println(
                                                "Check-out date and time must be "
                                                                + "after check-in date and time.");
                        }

                } while (!checkOutDateTime.isAfter(
                                checkInDateTime));

                String registrationId = generateRegistrationId();

                WalkInRegistration registration = new WalkInRegistration(
                                registrationId,
                                guest,
                                roomType,
                                numberOfGuests,
                                checkInDateTime,
                                checkOutDateTime);

                if (loyaltyProfile != null) {
                        int result = controller.addVipRegistration(
                                        registration,
                                        loyaltyProfile);

                        if (result != VipPriorityController.ADD_SUCCESS) {
                                displayVipAddError(result);
                                return;
                        }

                        Utility.printSuccess(
                                        "Existing loyalty member detected. "
                                                        + "Registration added to the VIP priority heap.");
                        System.out.println(
                                        "Registration ID: " + registrationId);
                        System.out.println(
                                        "Member ID: " + loyaltyProfile.getMemberId());
                        System.out.println(
                                        "Loyalty Tier: " + loyaltyProfile.getTier());
                        System.out.println(
                                        "VIP Members Waiting: "
                                                        + controller.getVipWaitingCount());
                        System.out.println(
                                        "Status: " + registration.getStatus());

                } else {
                        controller.addStandardRegistration(registration);

                        Utility.printSuccess(
                                        "No loyalty membership detected. "
                                                        + "Registration added to the standard queue.");
                        System.out.println(
                                        "Registration ID: " + registrationId);
                        System.out.println(
                                        "Standard Queue Position: "
                                                        + controller.getWaitingCount());
                        System.out.println(
                                        "Status: " + registration.getStatus());
                }
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
        }

        String roomType = readRoomType();

        int numberOfGuests = readPositiveInteger(
                "Enter Number of Guests: ");

        /*
         * A walk-in guest arrives for immediate registration.
         * The actual check-in time is updated again when a room
         * is successfully assigned.
         */
        LocalDateTime checkInDateTime
                = LocalDateTime.now()
                        .withSecond(0)
                        .withNano(0);

        System.out.println(
                "Check-In Time: "
                + checkInDateTime);

        LocalDateTime checkOutDateTime;

        do {
            checkOutDateTime
                    = readDateTimeParts(
                            "Expected Check-Out");

            if (!checkOutDateTime.isAfter(
                    checkInDateTime)) {

                Utility.printError(
                        "Check-out date and time must be "
                        + "after check-in date and time.");
            }

        } while (!checkOutDateTime.isAfter(
                checkInDateTime));

        String registrationId
                = controller.generateRegistrationId();

        WalkInRegistration registration
                = new WalkInRegistration(
                        registrationId,
                        guest,
                        roomType,
                        numberOfGuests,
                        checkInDateTime,
                        checkOutDateTime);

        boolean isVip = readYesNo(
                "Is this guest a VIP / "
                + "Loyalty member? (Y/N): ");

        if (isVip) {
            String memberId = readNonEmptyString(
                    "Enter Member ID: ");

            LoyaltyTier tier
                    = readLoyaltyTier();

            int result
                    = controller.addVipRegistration(
                            registration,
                            memberId,
                            tier);

            if (result
                    != VipPriorityController.ADD_SUCCESS) {

                displayVipAddError(result);
                return;
            }

            Utility.printSuccess(
                    "Registration added to "
                    + "the VIP priority heap.");

            System.out.println(
                    "Registration ID: "
                    + registrationId);

            System.out.println(
                    "Member ID: "
                    + memberId);

            System.out.println(
                    "Loyalty Tier: "
                    + tier);

            System.out.println(
                    "VIP Members Waiting: "
                    + controller.getVipWaitingCount());

            System.out.println(
                    "Status: "
                    + registration.getStatus());

        } else {
            controller.addStandardRegistration(
                    registration);

            Utility.printSuccess(
                    "Registration added to "
                    + "the standard queue.");

            System.out.println(
                    "Registration ID: "
                    + registrationId);

            System.out.println(
                    "Standard Queue Position: "
                    + controller.getWaitingCount());

            System.out.println(
                    "Status: "
                    + registration.getStatus());
        }
    }

    private void viewWaitingQueue() {
        System.out.println(
                "\n===== STANDARD WAITING "
                + "REGISTRATION QUEUE =====");

        int waitingCount
                = controller.getWaitingCount();

        if (waitingCount == 0) {
            System.out.println(
                    "No walk-in registrations are waiting.");
            return;
        }

        for (int i = 0;
                i < waitingCount;
                i++) {

            WalkInRegistration registration
                    = controller.getRegistrationAt(i);

            System.out.println(
                    "\nQueue Position: "
                    + (i + 1));

            System.out.println(
                    registration);

            System.out.println(
                    "------------------------------------------");
        }

        System.out.println(
                "Total Waiting Registrations: "
                + waitingCount);
    }

    private void viewNextRegistration() {
        System.out.println(
                "\n===== NEXT STANDARD REGISTRATION =====");

        WalkInRegistration registration
                = controller.getNextRegistration();

        if (registration == null) {
            System.out.println(
                    "No walk-in registrations are waiting.");
            return;
        }

        System.out.println(
                registration);
    }

    private void checkInNextStandardGuest() {
        System.out.println(
                "\n===== STANDARD ROOM ASSIGNMENT "
                + "& CHECK-IN =====");

        /*
         * Standard guests cannot receive rooms while
         * a VIP registration is still waiting.
         */
        if (controller.hasWaitingVip()) {
            Utility.printError(
                    "VIP members are waiting. "
                    + "Allocate the highest-priority "
                    + "VIP first.");
            return;
        }

        WalkInRegistration registration
                = controller.getNextRegistration();

        if (registration == null) {
            System.out.println(
                    "No standard registration is waiting.");
            return;
        }

        System.out.println(
                "\nNext Standard Guest:");

        System.out.println(
                registration);

        Room[] suitableRooms
                = controller
                        .getSuitableRoomsForNextStandard();

        if (suitableRooms.length == 0) {
            Utility.printError(
                    "No suitable room is "
                    + "currently available.");

            System.out.println(
                    "The registration remains "
                    + "in the FIFO queue.");
            return;
        }

        System.out.println(
                "\n===== SUITABLE AVAILABLE ROOMS =====");

        for (int i = 0;
                i < suitableRooms.length;
                i++) {

            Room room = suitableRooms[i];

            System.out.printf(
                    "%d. Room %-5s | "
                    + "Type: %-15s | "
                    + "Floor: %-3s | "
                    + "Capacity: %d%n",
                    i + 1,
                    room.getRoomNumber(),
                    formatRoomType(
                            room.getRoomType()),
                    room.getFloor(),
                    room.getNoOfGuest());
        }

        System.out.println(
                "0. Cancel Room Selection");

        int choice;

        while (true) {
            choice = readInteger(
                    "Select Room: ");

            if (choice >= 0
                    && choice <= suitableRooms.length) {

                break;
            }

            Utility.printError(
                    "Invalid room selection.");
        }

        if (choice == 0) {
            System.out.println(
                    "Room selection cancelled.");

            System.out.println(
                    "The registration remains "
                    + "in the FIFO queue.");
            return;
        }

        Room selectedRoom
                = suitableRooms[choice - 1];

        Booking booking
                = controller.checkInNextStandard(
                        selectedRoom.getRoomNumber());

        if (booking == null) {
            Utility.printError(
                    "Unable to assign the selected room. "
                    + "It may no longer be available.");

            System.out.println(
                    "The registration remains "
                    + "in the FIFO queue.");
            return;
        }

        Utility.printSuccess(
                "Room assigned and guest "
                + "checked in successfully.");

        System.out.println(
                "Registration ID: "
                + registration.getRegistrationId());

        System.out.println(
                "Confirmation Number: "
                + booking.getConfirmationNo());

        System.out.println(
                "Guest Name: "
                + booking.getGuest().getName());

        System.out.println(
                "Room Number: "
                + booking.getRoom().getRoomNumber());

        System.out.println(
                "Room Type: "
                + formatRoomType(
                        booking.getRoom()
                                .getRoomType()));

        System.out.println(
                "Check-In Time: "
                + booking.getRoom()
                        .getCheckInDateTime());

        System.out.println(
                "Expected Check-Out Time: "
                + booking.getRoom()
                        .getCheckOutDateTime());

        System.out.println(
                "Registration Status: "
                + registration.getStatus());

        System.out.println(
                "Remaining Standard Registrations: "
                + controller.getWaitingCount());
    }

    private void searchRegistration() {
        System.out.println(
                "\n===== SEARCH REGISTRATION =====");

        String registrationId
                = readNonEmptyString(
                        "Enter Registration ID: ");

        WalkInRegistration registration
                = controller.searchRegistrationById(
                        registrationId);

        if (registration == null) {
            System.out.println(
                    "Registration not found.");
            return;
        }

        System.out.println(
                "\nRegistration found:");

        System.out.println(
                registration);
    }

    private void cancelRegistration() {
        System.out.println(
                "\n===== CANCEL REGISTRATION =====");

        String registrationId
                = readNonEmptyString(
                        "Enter Registration ID: ");

        WalkInRegistration registration
                = controller.cancelRegistrationById(
                        registrationId);

        if (registration == null) {
            System.out.println(
                    "Waiting registration not found.");
            return;
        }

        Utility.printSuccess(
                "Registration cancelled successfully.");

        System.out.println();

        System.out.println(
                registration);
    }

    private boolean readYesNo(
            String message) {

        while (true) {
            System.out.print(message);

            String input
                    = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("Y")) {
                return true;
            }

            if (input.equalsIgnoreCase("N")) {
                return false;
            }

            Utility.printError(
                    "Please enter Y or N.");
        }
    }

    private LoyaltyTier readLoyaltyTier() {
        LoyaltyTier[] tiers
                = LoyaltyTier.values();

        while (true) {
            System.out.println(
                    "\nSelect Loyalty Tier:");

            for (int i = 0;
                    i < tiers.length;
                    i++) {

                System.out.println(
                        (i + 1)
                        + ". "
                        + tiers[i]);
            }

            int choice
                    = readInteger("Choice: ");

            if (choice >= 1
                    && choice <= tiers.length) {

                return tiers[choice - 1];
            }

            Utility.printError(
                    "Invalid loyalty tier.");
        }
    }

    private void displayVipAddError(
            int result) {

        switch (result) {
            case VipPriorityController
                    .DUPLICATE_MEMBER_ID:

                Utility.printError(
                        "Member ID already exists "
                        + "in the VIP heap.");
                break;

            case VipPriorityController
                    .REGISTRATION_ALREADY_QUEUED:

                Utility.printError(
                        "This registration is already "
                        + "in the VIP heap.");
                break;

            case VipPriorityController
                    .GUEST_ALREADY_QUEUED:

                Utility.printError(
                        "This guest is already waiting "
                        + "in the VIP heap.");
                break;

            default:
                Utility.printError(
                        "Unable to add the VIP registration. "
                        + "Check all fields.");
                break;
        }
    }

<<<<<<< HEAD
    private String readNonEmptyString(
            String message) {

        String input;

        do {
            System.out.print(message);
            input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                Utility.printError(
                        "Input cannot be empty.");
            }

        } while (input.isEmpty());

        return input;
    }

    private String readAlphabeticString(
            String message) {

        while (true) {
            String input
                    = readNonEmptyString(message);

            if (input.matches(
                    "[A-Za-z]+(?: [A-Za-z]+)*")) {
=======
        private void displayVipAddError(int result) {
                switch (result) {
                        case VipPriorityController.DUPLICATE_MEMBER_ID:
                                Utility.printError(
                                                "Member ID already exists in the VIP heap.");
                                break;

                        case VipPriorityController.REGISTRATION_ALREADY_QUEUED:
                                Utility.printError(
                                                "This registration is already in the VIP heap.");
                                break;

                        case VipPriorityController.GUEST_ALREADY_QUEUED:
                                Utility.printError(
                                                "This guest is already waiting in the VIP heap.");
                                break;

                        default:
                                Utility.printError(
                                                "Unable to add the VIP registration. Check all fields.");
                                break;
                }
        }

        private String generateRegistrationId() {
                return controller.generateNextRegistrationId();
        }

        private String readNonEmptyString(
                        String message) {

                String input;

                do {
                        System.out.print(message);
                        input = scanner.nextLine().trim();

                        if (input.isEmpty()) {
                                System.out.println(
                                                "Input cannot be empty.");
                        }

                } while (input.isEmpty());
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1

                return input;
            }

            Utility.printError(
                    "Guest name can contain "
                    + "letters and spaces only.");
        }
    }

    private int readInteger(
            String message) {

        while (true) {
            System.out.print(message);

            try {
                return Integer.parseInt(
                        scanner.nextLine().trim());

            } catch (NumberFormatException ex) {
                Utility.printError(
                        "Please enter a valid number.");
            }
        }
    }

    private int readPositiveInteger(
            String message) {

        int number;

        do {
            number = readInteger(message);

            if (number <= 0) {
                Utility.printError(
                        "The number must be "
                        + "greater than zero.");
            }

        } while (number <= 0);

        return number;
    }

    private Long readPhoneNumber() {
        while (true) {
            String input
                    = readNonEmptyString(
                            "Enter Phone Number: ");

            if (!Utility.isValidPhoneNo(input)) {
                Utility.printError(
                        "Invalid phone number. Enter "
                        + "10 or 11 digits starting with 01.");
                continue;
            }

            try {
                return Long.valueOf(input);

            } catch (NumberFormatException ex) {
                Utility.printError(
                        "Phone number is too long.");
            }
        }
    }

    private String readRoomType() {
        RoomType[] roomTypes
                = RoomType.values();

        while (true) {
            System.out.println(
                    "\nAvailable Room Types:");

            for (int i = 0;
                    i < roomTypes.length;
                    i++) {

                System.out.printf(
                        "%d. %s - RM %.2f per day%n",
                        i + 1,
                        formatRoomType(
                                roomTypes[i].name()),
                        roomTypes[i]
                                .getPricePerDay());
            }

            int choice
                    = readInteger(
                            "Select Room Type: ");

            if (choice >= 1
                    && choice <= roomTypes.length) {

                return roomTypes[
                        choice - 1].name();
            }

            Utility.printError(
                    "Invalid room type selection.");
        }
    }

    private String formatRoomType(
            String roomType) {

        return roomType.replace(
                "_",
                " ");
    }

    private LocalDateTime readDateTimeParts(
            String dateTimeType) {

        while (true) {
            System.out.println(
                    "\nEnter "
                    + dateTimeType
                    + " Date and Time");

            int year = readInteger(
                    "Enter Year (yyyy): ");

            int month = readInteger(
                    "Enter Month (1-12): ");

            int day = readInteger(
                    "Enter Day: ");

            int hour = readInteger(
                    "Enter Hour (0-23): ");

            int minute = readInteger(
                    "Enter Minute (0-59): ");

            try {
                return LocalDateTime.of(
                        year,
                        month,
                        day,
                        hour,
                        minute);

            } catch (DateTimeException ex) {
                Utility.printError(
                        "Invalid date or time. "
                        + "Please enter again.");
            }
        }
    }
}