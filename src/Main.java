
public static void main(String[] args) {
    // Example usage of Utility class
    Utility.clearScreen();
    Utility.printSuccess("Welcome to the Hotel Reservation System!");
    Utility.pauseScreen();
    // Example in Main.java or a central controller
    HousekeepingUI hkUI = new HousekeepingUI();
    hkUI.showMenu();
}
