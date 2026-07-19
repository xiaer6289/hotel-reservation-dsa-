
// Main.java (for testing your module)
public class Main {
    public static void main(String[] args) {
        Utility.clearScreen();
        Utility.printSuccess("Welcome to the Hotel Reservation System!");
        Utility.pauseScreen();
        System.out.println("Starting TARUMT Resorts Housekeeping System...");
        HousekeepingUI ui = new HousekeepingUI();
        ui.showMenu();
    
    }
}
