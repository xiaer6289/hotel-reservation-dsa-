/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


import boundary.FrontDeskUI;
import boundary.HousekeepingUI;
import boundary.RegistrationUI;
import boundary.VipAllocationUI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import utility.Utility;

/**
 *
 * @author Lee Cheng Xuan
 */
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            Utility.clearScreen();
            printMainMenu();

            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException ex) {
                choice = -1;
            }

            switch (choice) {
                case 1:
                    new FrontDeskUI().run();
                    break;
                case 2:
                    new RegistrationUI().run();
                    break;
                case 3:
                    new VipAllocationUI().run();
                    break;
                case 4:
                    new HousekeepingUI().showMenu();
                    break;
                case 0:
                    Utility.clearScreen();
                    System.out.println("  Thank you for using TARUMT Hotel System. Goodbye!");
                    System.out.println();
                    break;
                default:
                    Utility.printError("Invalid choice. Please enter a number from the menu.");
                    Utility.pauseScreen();
                    break;
            }
        } while (choice != 0);
    }

    private static void printMainMenu() {
        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"));
        String border = "=".repeat(Utility.UI_WIDTH);
        String dash   = "-".repeat(Utility.UI_WIDTH);

        System.out.println(border);
        System.out.println("       TARUMT HOTEL RESERVATION SYSTEM");
        System.out.println("             " + date);
        System.out.println(border);
        System.out.println();
        System.out.println("  [ MODULES ]");
        System.out.println("  1.  Front Desk Service");
        System.out.println("  2.  Walk-In Registration & Standard Booking");
        System.out.println("  3.  VIP & Loyalty Tier Priority Room Allocation");
        System.out.println("  4.  Housekeeping and Task Log");
        System.out.println();
        System.out.println(dash);
        System.out.println("  0.  Exit System");
        System.out.println(border);
        System.out.print("  Enter choice: ");
    }
}
