package boundary;

import utility.Utility;
import control.RegistrationController;
import control.VipPriorityController;
import entity.Guest;
import entity.LoyaltyProfile;
import entity.RoomType;
import entity.WalkInRegistration;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Scanner;

/**
 *
 * @author Lai Jen Feng
 */
public class RegistrationUI {

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
                                        processNextRegistration();
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
                                        System.out.println(
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
                System.out.println("1. Add Walk-In Registration");
                System.out.println("2. View Standard Waiting Queue");
                System.out.println("3. View Next Standard Registration");
                System.out.println("4. Process Next Standard Registration");
                System.out.println("5. Search Registration by ID");
                System.out.println("6. Cancel Registration");
                System.out.println("0. Return to Main Menu");
        }

        private void addWalkInRegistration() {
                System.out.println(
                                "\n===== ADD WALK-IN REGISTRATION =====");

                String guestId = readNonEmptyString(
                                "Enter Guest ID: ");

                /*
                 * Search existing Guest from guest.dat through Controller.
                 */
                Guest guest = controller.searchGuestById(guestId);

                if (guest != null) {
                        System.out.println(
                                        "\nExisting guest found.");
                        System.out.println(
                                        "Guest ID: " + guest.getGuestId());
                        System.out.println(
                                        "Guest Name: " + guest.getName());
                        System.out.println(
                                        "Phone Number: " + guest.getPhoneNo());

                } else {
                        System.out.println(
                                        "\nGuest not found.");
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
                                System.out.println(
                                                "Unable to register new guest.");
                                return;
                        }

                        System.out.println(
                                        "New guest saved successfully.");
                }

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

                int numberOfGuests = readPositiveInteger(
                                "Enter Number of Guests: ");

                LocalDateTime checkInDateTime;

                while (true) {
                        checkInDateTime = readDateTimeParts("Check-In");

                        if (checkInDateTime.isAfter(
                                        LocalDateTime.now())) {

                                break;
                        }

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
        }

        private void viewWaitingQueue() {
                System.out.println(
                                "\n===== STANDARD WAITING REGISTRATION QUEUE =====");

                int waitingCount = controller.getWaitingCount();

                if (waitingCount == 0) {
                        System.out.println(
                                        "No walk-in registrations are waiting.");
                        return;
                }

                for (int i = 0; i < waitingCount; i++) {
                        WalkInRegistration registration = controller.getRegistrationAt(i);

                        System.out.println(
                                        "\nQueue Position: " + (i + 1));
                        System.out.println(registration);
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

                WalkInRegistration registration = controller.getNextRegistration();

                if (registration == null) {
                        System.out.println(
                                        "No walk-in registrations are waiting.");
                        return;
                }

                System.out.println(registration);
        }

        private void processNextRegistration() {
                System.out.println(
                                "\n===== PROCESS NEXT STANDARD REGISTRATION =====");

                if (controller.hasWaitingVip()) {
                        Utility.printError(
                                        "VIP members are waiting. Allocate the highest-priority VIP first.");
                        return;
                }

                WalkInRegistration registration = controller.processNextRegistration();

                if (registration == null) {
                        System.out.println(
                                        "No walk-in registrations are waiting.");
                        return;
                }

                System.out.println(
                                "Registration processed successfully.");
                System.out.println();
                System.out.println(registration);

                System.out.println(
                                "\nRemaining Waiting Registrations: "
                                                + controller.getWaitingCount());
        }

        private void searchRegistration() {
                System.out.println(
                                "\n===== SEARCH REGISTRATION =====");

                String registrationId = readNonEmptyString(
                                "Enter Registration ID: ");

                WalkInRegistration registration = controller.searchRegistrationById(
                                registrationId);

                if (registration == null) {
                        System.out.println(
                                        "Registration not found.");
                        return;
                }

                System.out.println(
                                "\nRegistration found:");
                System.out.println(registration);
        }

        private void cancelRegistration() {
                System.out.println(
                                "\n===== CANCEL REGISTRATION =====");

                String registrationId = readNonEmptyString(
                                "Enter Registration ID: ");

                WalkInRegistration registration = controller.cancelRegistrationById(
                                registrationId);

                if (registration == null) {
                        System.out.println(
                                        "Waiting registration not found.");
                        return;
                }

                System.out.println(
                                "Registration cancelled successfully.");
                System.out.println();
                System.out.println(registration);
        }

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

                return input;
        }

        private String readAlphabeticString(
                        String message) {

                while (true) {
                        String input = readNonEmptyString(message);

                        if (input.matches(
                                        "[A-Za-z]+(?: [A-Za-z]+)*")) {

                                return input;
                        }

                        System.out.println(
                                        "Guest name can contain "
                                                        + "letters and spaces only.");
                }
        }

        private int readInteger(String message) {
                while (true) {
                        System.out.print(message);

                        try {
                                return Integer.parseInt(
                                                scanner.nextLine().trim());

                        } catch (NumberFormatException ex) {
                                System.out.println(
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
                                System.out.println(
                                                "The number must be "
                                                                + "greater than zero.");
                        }

                } while (number <= 0);

                return number;
        }

        private Long readPhoneNumber() {
                while (true) {
                        String input = readNonEmptyString(
                                        "Enter Phone Number: ");

                        if (!Utility.isValidPhoneNo(input)) {
                                System.out.println(
                                                "Invalid phone number. Please enter "
                                                                + "10 or 11 digits starting with 01.");
                                continue;
                        }

                        try {
                                return Long.valueOf(input);

                        } catch (NumberFormatException ex) {
                                System.out.println(
                                                "Phone number is too long.");
                        }
                }
        }

        private String readRoomType() {
                RoomType[] roomTypes = RoomType.values();

                while (true) {
                        System.out.println(
                                        "\nAvailable Room Types:");

                        for (int i = 0; i < roomTypes.length; i++) {

                                System.out.printf(
                                                "%d. %s - RM %.2f per day%n",
                                                i + 1,
                                                formatRoomType(
                                                                roomTypes[i].name()),
                                                roomTypes[i].getPricePerDay());
                        }

                        int choice = readInteger(
                                        "Select Room Type: ");

                        if (choice >= 1
                                        && choice <= roomTypes.length) {

                                return roomTypes[choice - 1].name();
                        }

                        System.out.println(
                                        "Invalid room type selection.");
                }
        }

        private String formatRoomType(
                        String roomType) {

                return roomType.replace("_", " ");
        }

        private LocalDateTime readDateTimeParts(
                        String dateTimeType) {

                while (true) {
                        System.out.println(
                                        "\nEnter " + dateTimeType
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
                                System.out.println(
                                                "Invalid date or time. "
                                                                + "Please enter again.");
                        }
                }
        }
}