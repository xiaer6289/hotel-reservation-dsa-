package utility;


import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Scanner;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Lee Cheng Xuan
 */
public class Utility {
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void pauseScreen() {
        System.out.println("\nPress Enter to continue...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }
    
    public static void printError(String message) {
        System.out.println("[ERROR]: " + message);
    }
    
    public static void printSuccess(String message) {
        System.out.println("[SUCCESS]: " + message);
    }
    
    public static boolean isValidConfirmationNo(String confirmationNo) {
        return confirmationNo != null && confirmationNo.matches("\\d{8}");
    }
    
    public static boolean isValidPhoneNo(String phoneNo) {
        return phoneNo != null && phoneNo.matches("^01\\d{8,9}$");
    }
    
    public static boolean isValidRegistrationId(String registrationId) {
        return registrationId != null && registrationId.trim().matches("(?i)^R\\d{4}$");
    }

    public static boolean isValidYesNo(String input) {
        return input != null && input.trim().matches("(?i)^[YN]$");
    }

    public static String generateConfirmationNo() {
        int number = (int) (Math.random() * 90_000_000) + 10_000_000;
        return String.valueOf(number);
    }

    public static LocalDate readCheckOutDateInRange(Scanner scanner, LocalDate minimumDate, LocalDate maximumDate) {
        if (scanner == null || minimumDate == null || maximumDate == null) {
            throw new IllegalArgumentException("Scanner and date range cannot be null.");
        }

        if (minimumDate.isAfter(maximumDate)) {
            throw new IllegalArgumentException("Minimum date cannot be after maximum date.");
        }

        System.out.println("Allowed check-out date: " + minimumDate + " to " + maximumDate + ".");

        int minimumYear = minimumDate.getYear();
        int maximumYear = maximumDate.getYear();
        int year = readWholeNumberInRange(scanner, buildRangePrompt("Year", minimumYear, maximumYear), minimumYear, maximumYear, buildRangeError("year", minimumYear, maximumYear));
        int minimumMonth = year == minimumYear ? minimumDate.getMonthValue() : 1;
        int maximumMonth = year == maximumYear ? maximumDate.getMonthValue() : 12;
        int month = readWholeNumberInRange(scanner, buildRangePrompt("Month", minimumMonth, maximumMonth), minimumMonth, maximumMonth, buildRangeError("month", minimumMonth, maximumMonth));
        YearMonth selectedMonth = YearMonth.of(year, month);
        int minimumDay = selectedMonth.equals(YearMonth.from(minimumDate)) ? minimumDate.getDayOfMonth() : 1;
        int maximumDay = selectedMonth.equals(YearMonth.from(maximumDate)) ? maximumDate.getDayOfMonth() : selectedMonth.lengthOfMonth();
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        int day = readWholeNumberInRange(scanner, buildRangePrompt("Day", minimumDay, maximumDay), minimumDay, maximumDay, "Invalid day. " + monthName + " " + year + " only allows day " + formatRangeForMessage(minimumDay, maximumDay) + " for this stay.");

        return LocalDate.of(year, month, day);
    }

    private static int readWholeNumberInRange(Scanner scanner, String prompt, int minimum, int maximum, String errorMessage) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                int value = Integer.parseInt(input);
                if (value >= minimum && value <= maximum) {
                    return value;
                }
            } catch (NumberFormatException ex) {
                // The same clear validation message is shown below.
            }

            printError(errorMessage);
        }
    }

    private static String buildRangePrompt(String field, int minimum, int maximum) {
        return "Enter " + field + " (" + formatRange(minimum, maximum) + "): ";
    }

    private static String buildRangeError(String field, int minimum, int maximum) {
        if (minimum == maximum) {
            return "Invalid " + field + ". Please enter " + minimum + " only.";
        }

        return "Invalid " + field + ". Please enter a " + field + " from " + minimum + " to " + maximum + ".";
    }

    private static String formatRangeForMessage(int minimum, int maximum) {
        return minimum == maximum ? String.valueOf(minimum) : minimum + " to " + maximum;
    }

    private static String formatRange(int minimum, int maximum) {
        return minimum == maximum ? String.valueOf(minimum) : minimum + "-" + maximum;
    }

    public static boolean isValidPersonName(String name) {
        if (name == null) {
            return false;
        }

        String normalized = name.trim();
        return normalized.length() >= 2 && normalized.length() <= 50 && normalized.matches("[\\p{L}]+(?:[ '-][\\p{L}]+)*");
    }
}