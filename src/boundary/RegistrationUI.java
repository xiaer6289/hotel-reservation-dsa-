package boundary;

import control.RegistrationController;
import entity.Guest;
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

        private int nextRegistrationNumber = 1;

        public RegistrationUI() {
                controller = new RegistrationController();
                scanner = new Scanner(System.in);
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
                System.out.println("2. View Waiting Queue");
                System.out.println("3. View Next Registration");
                System.out.println("4. Process Next Registration");
                System.out.println("0. Return to Main Menu");
        }

        private void addWalkInRegistration() {
                System.out.println(
                                "\n===== ADD WALK-IN REGISTRATION =====");

                String guestId = readNonEmptyString(
                                "Enter Guest ID: ");

                String guestName = readNonEmptyString(
                                "Enter Guest Name: ");

                Long phoneNumber = readPhoneNumber();

                String roomType = readRoomType();

                int numberOfGuests = readPositiveInteger(
                                "Enter Number of Guests: ");

                LocalDateTime checkInDateTime = readDateTimeParts("Check-In");

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

                Guest guest = new Guest(
                                guestId,
                                guestName,
                                phoneNumber);

                WalkInRegistration registration = new WalkInRegistration(
                                registrationId,
                                guest,
                                roomType,
                                numberOfGuests,
                                checkInDateTime,
                                checkOutDateTime);

                controller.addRegistration(registration);

                System.out.println(
                                "\nRegistration added successfully.");
                System.out.println(
                                "Registration ID: " + registrationId);
                System.out.println(
                                "Queue Position: "
                                                + controller.getWaitingCount());
                System.out.println(
                                "Status: " + registration.getStatus());
        }

        private void viewWaitingQueue() {
                System.out.println(
                                "\n===== WAITING REGISTRATION QUEUE =====");

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
                                "\n===== NEXT REGISTRATION =====");

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
                                "\n===== PROCESS NEXT REGISTRATION =====");

                WalkInRegistration registration = controller.processNextRegistration();

                if (registration == null) {
                        System.out.println(
                                        "No walk-in registrations are waiting.");
                        return;
                }

                registration.setStatus("PROCESSED");

                System.out.println(
                                "Registration processed successfully.");
                System.out.println();
                System.out.println(registration);

                System.out.println(
                                "\nRemaining Waiting Registrations: "
                                                + controller.getWaitingCount());
        }

        private String generateRegistrationId() {
                String registrationId = String.format(
                                "R%04d",
                                nextRegistrationNumber);

                nextRegistrationNumber++;

                return registrationId;
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

                        if (!input.matches("\\d+")) {
                                System.out.println(
                                                "Phone number must contain "
                                                                + "digits only.");
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