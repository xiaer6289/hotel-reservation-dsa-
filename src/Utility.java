
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
    
    public static String generateConfirmationNo() {
        int number = (int) (Math.random() * 90_000_000) + 10_000_000;
        return String.valueOf(number);
    }
}
