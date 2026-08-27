TARUMT HOTEL RESERVATION SYSTEM
================================

1. PROJECT DESCRIPTION
----------------------
This is a Java console-based Hotel Reservation System developed using the Entity-Control-Boundary (ECB) architecture and custom Abstract Data Types (ADTs).

The system contains the following main modules:
1. Front Desk Service
2. Walk-In Registration & Standard Booking
3. VIP & Loyalty Tier Priority Room Allocation
4. Housekeeping and Task Log


2. SOFTWARE REQUIREMENTS
------------------------
- Apache NetBeans IDE
- Java Development Kit (JDK) 24


3. HOW TO OPEN THE PROJECT IN NETBEANS
--------------------------------------
1. Extract the submitted project folder if it is in a ZIP file.
2. Open Apache NetBeans IDE.
3. Select File > Open Project.
4. Browse to and select the "hotel-reservation-dsa-" project folder.
5. Click Open Project.
6. Wait for NetBeans to load the project and its source files.


4. HOW TO RUN THE APPLICATION
-----------------------------
Method 1 - Run the project:
1. In the Projects panel, right-click the project.
2. Select Clean and Build.
3. Right-click the project again and select Run.
4. If NetBeans asks for the Main Class, select "Main" and confirm.

Method 2 - Run Main.java directly:
1. Expand Source Packages in the Projects panel.
2. Locate Main.java under the default package.
3. Right-click Main.java and select Run File.

The Hotel Reservation System main menu will then be displayed in the Output window. Enter the menu number shown on screen to use the required module.


5. DATA FILES
-------------
The application uses .dat files to store and retrieve system data. The supplied data files must remain in the PROJECT ROOT DIRECTORY because the program accesses them using relative file paths.

Included data files:
- guest.dat               : Stores guest records
- room.dat                : Stores room records and room status
- booking.dat             : Stores booking records
- payment.dat             : Stores payment records
- loyalty_profile.dat     : Stores VIP/loyalty profile records
- tasklog.dat              : Stores housekeeping task log records

The application may also create/use the following file during execution:
- walkin_registration.dat : Stores walk-in and waiting registration records

Do not move, rename, or delete the data files unless the stored data is intended to be reset.


6. BASIC SYSTEM USAGE
---------------------
After running Main.java, the following main menu is displayed:

1. Front Desk Service
2. Walk-In Registration & Standard Booking
3. VIP & Loyalty Tier Priority Room Allocation
4. Housekeeping and Task Log
0. Exit System

Enter the corresponding number and follow the instructions displayed in the console. Input validation is provided by the system for menu choices and required user inputs.


7. IMPORTANT NOTES
------------------
- Run the application from the NetBeans project directory so that all .dat files can be read and updated correctly.
- Keep all supplied .dat files together with the project files.
- The program is console-based; all input and output are handled through the NetBeans Output window.
- If the application cannot start because no main class is configured, select "Main" as the Main Class when prompted by NetBeans.
