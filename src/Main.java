/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


import boundary.FrontDeskUI;
import java.util.Scanner;

/**
 *
 * @author Lee Cheng Xuan
 */
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("===== HOTEL RESERVATION SYSTEM =====");
            System.out.println("1. Front Desk Service");
            System.out.println("2. Walk-In Registration & Standard Booking Procedure"); 
            System.out.println("3. VIP & Loyalty Tier Priority Room Allocation");
            System.out.println("4. Housekeeping and Task Log"); 
            System.out.println("0. Exit System");
            System.out.print("Enter choice: ");

            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException ex) {
                choice = -1;
            }

            switch (choice) {
                case 1: new FrontDeskUI().run();
                case 2: {} //todo 
                case 3: {} //todo
                case 4: {} //todo
                case 0: System.out.println("Exiting system...");
                default: System.out.println("Invalid choice, try again.");
            }
        } while (choice != 0);
    }
}
