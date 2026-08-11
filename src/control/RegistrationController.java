package control;

import adt.linear.DoublyLinkedList;
import adt.linear.LinearADT;
import dao.BookingDao;
import dao.GuestDao;
<<<<<<< HEAD
import dao.RoomDao;
import dao.WalkInRegistrationDao;
import entity.Booking;
=======
import dao.LoyaltyProfileDao;
import dao.WalkInRegistrationDao;
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
import entity.Guest;
import entity.LoyaltyProfile;
import entity.LoyaltyTier;
<<<<<<< HEAD
import entity.Room;
=======
import entity.RegistrationStatus;
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
import entity.WalkInRegistration;
import java.io.File;
import java.time.LocalDateTime;
import utility.Utility;

/**
 * Controls walk-in registrations, the standard FIFO queue and standard room
 * assignment. VIP registrations are routed to the shared VIP controller.
 *
 * @author Lai Jen Feng
 */
public class RegistrationController {

    private final LinearADT<WalkInRegistration> registrationQueue;
    private final LinearADT<WalkInRegistration> registrationRecords;

    private final GuestDao guestDao;
    private final WalkInRegistrationDao registrationDao;
    private final LoyaltyProfileDao loyaltyProfileDao;
    private Guest[] guests;
    private LoyaltyProfile[] loyaltyProfiles;

<<<<<<< HEAD
    /*
     * Saves and retrieves Walk-In Registration records.
     */
    private final WalkInRegistrationDao registrationDao;

    private final RoomDao roomDao;
    private final BookingDao bookingDao;

    private Room[] rooms;
    private Booking[] bookings;

    /* Shared with VipAllocationUI through Main. */
=======
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
    private final VipPriorityController vipPriorityController;

    public RegistrationController() {
        this(new VipPriorityController());
    }

    public RegistrationController(
            VipPriorityController vipPriorityController) {

        registrationQueue = new DoublyLinkedList<>();
        registrationRecords = new DoublyLinkedList<>();

        this.vipPriorityController = vipPriorityController;

        /*
         * Load saved registration records.
         */
        registrationDao = new WalkInRegistrationDao();

        WalkInRegistration[] savedRegistrations = registrationDao.loadExisting();

        for (WalkInRegistration registration : savedRegistrations) {

            if (registration == null) {
                continue;
            }

            /*
             * Restore all registration history.
             */
            registrationRecords.addLast(
                    registration);

            /*
             * Only Standard registrations that are
             * still WAITING are restored into the FIFO queue.
             *
             * CHECKED-IN and CANCELLED registrations
             * remain as history only.
             *
             * VIP-WAITING belongs to the VIP MaxHeap,
             * so it is not inserted into the Standard queue.
             */
            if ("WAITING".equalsIgnoreCase(
                    registration.getStatus())) {

                registrationQueue.addLast(
                        registration);
            }
        }

        guestDao = new GuestDao();
        registrationDao = new WalkInRegistrationDao();
        loyaltyProfileDao = new LoyaltyProfileDao();
        guests = guestDao.loadOrSeed();
        loyaltyProfiles = loyaltyProfileDao.loadOrSeed();

        if (guests == null) {
            guests = new Guest[0];
        }

<<<<<<< HEAD
        roomDao = new RoomDao();
        bookingDao = new BookingDao();

        rooms = roomDao.loadOrSeed();
        bookings = loadExistingBookings();
=======
        if (loyaltyProfiles == null) {
            loyaltyProfiles = new LoyaltyProfile[0];
        }

        loadSavedRegistrations();
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
    }

    /**
     * Searches an existing guest using Guest ID.
     */
    public Guest searchGuestById(
            String guestId) {

        if (guestId == null) {
            return null;
        }

        for (Guest guest : guests) {

            if (guest != null
                    && guest.getGuestId()
                            .equalsIgnoreCase(
                                    guestId.trim())) {

                return guest;
            }
        }

        return null;
    }

    /**
<<<<<<< HEAD
     * Checks whether the guest already has a waiting
     * registration or is currently staying in the hotel.
     */
    public boolean hasActiveRegistrationOrStay(
            String guestId) {

        if (guestId == null) {
            return false;
        }

        /*
         * Check registration records that are
         * still waiting.
         */
        for (int i = 0; i < registrationRecords.size(); i++) {

            WalkInRegistration registration = registrationRecords.get(i);

            if (registration == null
                    || registration.getGuest() == null) {

                continue;
            }

            boolean sameGuest = registration.getGuest()
                    .getGuestId()
                    .equalsIgnoreCase(
                            guestId.trim());

            String status = registration.getStatus();

            boolean stillWaiting = "WAITING".equalsIgnoreCase(status)
                    || "VIP-WAITING".equalsIgnoreCase(status);

            if (sameGuest && stillWaiting) {
                return true;
            }
        }

        /*
         * Reload latest Booking data and check
         * whether the guest is occupying a room.
         */
        Booking[] latestBookings = loadExistingBookings();

        for (Booking booking : latestBookings) {

            if (booking == null
                    || booking.getGuest() == null
                    || booking.getRoom() == null) {

                continue;
            }

            boolean sameGuest = booking.getGuest()
                    .getGuestId()
                    .equalsIgnoreCase(
                            guestId.trim());

            boolean currentlyCheckedIn = booking.getRoom()
                    .getStatus() == 'O'
                    && !booking.getRoom()
                            .isAvailability();

            if (sameGuest
                    && currentlyCheckedIn) {

                return true;
            }
        }

        return false;
    }

    /**
     * Creates and saves a new Guest.
     */
=======
     * Looks up the guest's existing loyalty membership. Registration staff do
     * not manually choose a VIP tier during check-in.
     */
    public LoyaltyProfile searchLoyaltyProfileByGuestId(String guestId) {
        if (guestId == null) {
            return null;
        }

        for (LoyaltyProfile profile : loyaltyProfiles) {
            if (profile != null
                    && profile.getGuestId()
                            .equalsIgnoreCase(guestId.trim())) {

                return profile;
            }
        }

        return null;
    }

>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
    public Guest addNewGuest(
            String guestId,
            String guestName,
            Long phoneNumber) {

        if (searchGuestById(
                guestId) != null) {

            return null;
        }

        Guest newGuest = new Guest(
                guestId,
                guestName,
                phoneNumber);

        Guest[] updatedGuests = new Guest[guests.length + 1];
<<<<<<< HEAD

        for (int i = 0; i < guests.length; i++) {

            updatedGuests[i] = guests[i];
        }
=======
        System.arraycopy(guests, 0, updatedGuests, 0, guests.length);
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1

        updatedGuests[guests.length] = newGuest;

        guests = updatedGuests;

        guestDao.saveToFile(
                guests);

        return newGuest;
    }

    /**
     * Adds a normal guest to the Standard
     * DoublyLinkedList FIFO queue.
     */
    public void addStandardRegistration(
            WalkInRegistration registration) {

        if (registration == null) {
            return;
        }

<<<<<<< HEAD
        registration.setStatus(
                "WAITING");

        /*
         * FIFO:
         * new registration joins at the end.
         */
        registrationQueue.addLast(
                registration);

        /*
         * Keep a complete registration record.
         */
        registrationRecords.addLast(
                registration);

        /*
         * Save registration history.
         */
        saveRegistrationRecords();
    }

    /**
     * Alias retained for existing code that treats
     * a registration as Standard.
=======
        registration.setStatus(RegistrationStatus.WAITING);
        registrationQueue.addLast(registration);
        addRecordIfAbsent(registration);
        registrationDao.upsert(registration);
    }

    /**
     * Alias retained for existing registration code.
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
     */
    public void addRegistration(
            WalkInRegistration registration) {

        addStandardRegistration(
                registration);
    }

    /**
     * Routes a completed registration into the
     * shared VIP MaxHeap.
     */
    public int addVipRegistration(
            WalkInRegistration registration,
            LoyaltyProfile loyaltyProfile) {

        if (loyaltyProfile == null) {
            return VipPriorityController.INVALID_INPUT;
        }

        return addVipRegistration(
                registration,
                loyaltyProfile.getMemberId(),
                loyaltyProfile.getTier());
    }

    public int addVipRegistration(
            WalkInRegistration registration,
            String memberId,
            LoyaltyTier tier) {

        int result = vipPriorityController
                .addVipRegistration(
                        memberId,
                        registration,
                        tier);

        if (result == VipPriorityController.ADD_SUCCESS) {
<<<<<<< HEAD

            /*
             * Keep VIP registration in the
             * overall registration records too.
             */
            registrationRecords.addLast(
                    registration);

            saveRegistrationRecords();
=======
            addRecordIfAbsent(registration);
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
        }

        return result;
    }

    /**
     * Returns the number of Standard guests
     * currently waiting.
     */
    public int getWaitingCount() {

        return registrationQueue.size();
    }

    /**
     * Returns the number of VIP guests
     * currently waiting.
     */
    public int getVipWaitingCount() {

        return vipPriorityController
                .getWaitingCount();
    }

    /**
     * Returns true when a VIP is still waiting
     * for room allocation.
     */
    public boolean hasWaitingVip() {

        return vipPriorityController
                .hasWaitingVip();
    }

    /**
     * Retrieves a Standard registration
     * by its queue position.
     */
    public WalkInRegistration getRegistrationAt(
            int index) {

        if (index < 0
                || index >= registrationQueue.size()) {

            return null;
        }

        return registrationQueue.get(
                index);
    }

    /**
     * Returns the first Standard guest
     * in the FIFO queue.
     */
    public WalkInRegistration getNextRegistration() {

        if (registrationQueue.isEmpty()) {
            return null;
        }

        return registrationQueue.get(0);
    }

    /**
     * Generates the next registration ID based
     * on existing registration records.
     *
     * Example:
     * R0001
     * R0002
     * R0003
     */
    public String generateRegistrationId() {

        int highestNumber = 0;

        for (int i = 0; i < registrationRecords.size(); i++) {

            WalkInRegistration registration = registrationRecords.get(i);

            if (registration == null) {
                continue;
            }

            String registrationId = registration
                    .getRegistrationId();

            if (registrationId != null
                    && registrationId.matches(
                            "R\\d{4}")) {

                int number = Integer.parseInt(
                        registrationId
                                .substring(1));

                if (number > highestNumber) {

                    highestNumber = number;
                }
            }
        }

        return String.format(
                "R%04d",
                highestNumber + 1);
    }

    /**
     * Retrieves suitable available rooms for
     * the Standard guest at the front of the
     * FIFO queue.
     */
    public Room[] getSuitableRoomsForNextStandard() {

        WalkInRegistration registration = getNextRegistration();

        if (registration == null) {
            return new Room[0];
        }

        /*
         * Reload latest Room data because
         * VIP, Front Desk or Housekeeping
         * may have updated the rooms.
         */
        rooms = roomDao.loadOrSeed();

        int suitableCount = 0;

        /*
         * First pass:
         * calculate number of suitable rooms.
         */
        for (Room room : rooms) {

            if (isSuitableRoom(
                    room,
                    registration)) {

                suitableCount++;
            }
        }

        Room[] suitableRooms = new Room[suitableCount];

        int index = 0;

        /*
         * Second pass:
         * store suitable rooms.
         */
        for (Room room : rooms) {

            if (isSuitableRoom(
                    room,
                    registration)) {

                suitableRooms[index] = room;

                index++;
            }
        }

        return suitableRooms;
    }

    /**
     * Assigns a selected room, creates a Booking
     * without Payment and checks in the first
     * Standard guest.
     */
    public Booking checkInNextStandard(
            String selectedRoomNumber) {

        /*
         * No Standard guest waiting.
         */
        if (registrationQueue.isEmpty()) {
            return null;
        }

        /*
         * VIP guests have priority over
         * Standard guests.
         */
        if (vipPriorityController
                .hasWaitingVip()) {

<<<<<<< HEAD
            return null;
        }

        /*
         * Peek at the first Standard registration.
         *
         * Do NOT remove it yet.
         */
        WalkInRegistration registration = registrationQueue.get(0);

        /*
         * Reload latest shared Room
         * and Booking data.
         */
        rooms = roomDao.loadOrSeed();

        bookings = loadExistingBookings();

        Room selectedRoom = findSelectedSuitableRoom(
                selectedRoomNumber,
                registration);

        /*
         * Room was no longer available
         * or was not suitable.
         */
        if (selectedRoom == null) {
            return null;
        }

        LocalDateTime actualCheckInTime = LocalDateTime.now()
                .withSecond(0)
                .withNano(0);

        /*
         * Walk-in guest immediately occupies
         * the selected room.
         */
        selectedRoom.setAvailability(
                false);

        selectedRoom.setStatus(
                'O');

        selectedRoom.setBookingDate(
                actualCheckInTime);

        selectedRoom.setCheckInDateTime(
                actualCheckInTime);

        selectedRoom.setCheckOutDateTime(
                registration
                        .getCheckOutDateTime());

        /*
         * Update actual registration
         * check-in time.
         */
        registration.setCheckInDateTime(
                actualCheckInTime);

        /*
         * Registration creates the Booking.
         *
         * Payment is NOT handled by this module,
         * so null is passed to the existing
         * Booking constructor.
         */
        Booking booking = new Booking(
                generateUniqueConfirmationNo(),
                registration.getGuest(),
                selectedRoom,
                null);

        bookings = appendBooking(
                bookings,
                booking);

        /*
         * Save Room and Booking so other
         * modules can retrieve them.
         */
        roomDao.saveToFile(
                rooms);

        bookingDao.saveToFile(
                bookings);

        /*
         * Guest has successfully completed
         * Standard registration and check-in.
         */
        registration.setStatus(
                "CHECKED-IN");

        /*
         * FIFO dequeue.
         *
         * Remove the registration only after
         * room assignment and Booking creation
         * are successful.
         */
        registrationQueue.removeFirst();

        /*
         * Update registration.dat with
         * CHECKED-IN status.
         */
        saveRegistrationRecords();

        return booking;
=======
        registration.setStatus(RegistrationStatus.PROCESSED);
        registrationDao.upsert(registration);

        return registration;
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
    }

    /**
     * Temporarily retained for compatibility
     * with older code.
     */
    @Deprecated
    public WalkInRegistration processNextRegistration() {

        return null;
    }

    /**
     * Searches any historical registration
     * using Registration ID.
     */
    public WalkInRegistration searchRegistrationById(
            String registrationId) {

        if (registrationId == null) {
            return null;
        }
<<<<<<< HEAD

        for (int i = 0; i < registrationRecords.size(); i++) {

            WalkInRegistration registration = registrationRecords.get(i);

            if (registration != null
                    && registration
                            .getRegistrationId()
                            .equalsIgnoreCase(
                                    registrationId
                                            .trim())) {
=======

        for (int i = 0; i < registrationRecords.size(); i++) {
            WalkInRegistration registration
                    = registrationRecords.get(i);

            if (registration.getRegistrationId()
                    .equalsIgnoreCase(registrationId.trim())) {
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1

                return registration;
            }
        }

        return null;
    }

    /**
     * Returns total historical registrations.
     */
    public int getTotalRegistrationCount() {

        return registrationRecords.size();
    }

    /**
     * Returns a historical registration record
     * using its index.
     */
    public WalkInRegistration getRecordAt(
            int index) {

        if (index < 0
                || index >= registrationRecords.size()) {

            return null;
        }

        return registrationRecords.get(
                index);
    }

<<<<<<< HEAD
    /**
     * Cancels a waiting Standard registration.
     *
     * If it is not in the Standard FIFO queue,
     * the shared VIP MaxHeap is checked.
     */
=======
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
    public WalkInRegistration cancelRegistrationById(
            String registrationId) {

        if (registrationId == null) {
            return null;
        }

<<<<<<< HEAD
        /*
         * Search Standard FIFO queue first.
         */
=======
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
        for (int i = 0; i < registrationQueue.size(); i++) {

<<<<<<< HEAD
            WalkInRegistration registration = registrationQueue.get(i);

            if (registration
                    .getRegistrationId()
                    .equalsIgnoreCase(
                            registrationId
                                    .trim())) {

                registrationQueue.removeAt(
                        i);

                registration.setStatus(
                        "CANCELLED");

                /*
                 * Save updated CANCELLED status.
                 */
                saveRegistrationRecords();

=======
            if (registration.getRegistrationId()
                    .equalsIgnoreCase(registrationId.trim())) {

                registrationQueue.removeAt(i);
                registration.setStatus(RegistrationStatus.CANCELLED);
                registrationDao.upsert(registration);
>>>>>>> 573bc92e6bf34aedc6401f512d2c31e28eb0aaf1
                return registration;
            }
        }

        /*
         * Not found in Standard queue.
         * Check VIP MaxHeap.
         */
        WalkInRegistration vipRegistration = vipPriorityController
                .cancelVipRegistrationById(
                        registrationId.trim());

        /*
         * VIP controller updates the same
         * WalkInRegistration object.
         *
         * Save the changed status into
         * registration.dat.
         */
        if (vipRegistration != null) {

            saveRegistrationRecords();
        }

        return vipRegistration;
    }

    /**
     * Reads existing Booking records without
     * requiring PaymentDao.
     */
    private Booking[] loadExistingBookings() {

        File bookingFile = new File(
                "booking.dat");

        if (!bookingFile.exists()) {

            return new Booking[0];
        }

        Booking[] loadedBookings = bookingDao.retrieveFromFile();

        if (loadedBookings == null) {

            return new Booking[0];
        }

        return loadedBookings;
    }

    /**
     * Checks whether a room matches the
     * Standard guest requirements.
     */
    private boolean isSuitableRoom(
            Room room,
            WalkInRegistration registration) {

        if (room == null
                || registration == null) {

            return false;
        }

        /*
         * Room must currently be available.
         */
        boolean roomAvailable = room.isAvailability();

        /*
         * Room type must match what
         * the guest requested.
         */
        boolean matchingRoomType = room.getRoomType()
                .equalsIgnoreCase(
                        registration
                                .getRequestedRoomType());

        /*
         * Room capacity must be sufficient.
         */
        boolean enoughCapacity = room.getNoOfGuest() >= registration
                .getNumberOfGuests();

        return roomAvailable
                && matchingRoomType
                && enoughCapacity;
    }

    /**
     * Finds and revalidates the room
     * selected by the customer.
     */
    private Room findSelectedSuitableRoom(
            String roomNumber,
            WalkInRegistration registration) {

        if (roomNumber == null) {
            return null;
        }

        for (Room room : rooms) {

            if (room != null
                    && room.getRoomNumber()
                            .equalsIgnoreCase(
                                    roomNumber.trim())
                    && isSuitableRoom(
                            room,
                            registration)) {

                return room;
            }
        }

        return null;
    }

    /**
     * Generates an unused eight-digit
     * Booking confirmation number.
     */
    private String generateUniqueConfirmationNo() {

        String confirmationNo;

        do {
            confirmationNo = Utility
                    .generateConfirmationNo();

        } while (confirmationNoExists(
                confirmationNo));

        return confirmationNo;
    }

    /**
     * Checks whether a confirmation number
     * already exists.
     */
    private boolean confirmationNoExists(
            String confirmationNo) {

        for (Booking booking : bookings) {

            if (booking != null
                    && booking
                            .getConfirmationNo()
                            .equals(
                                    confirmationNo)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Adds a Booking into a normal array
     * without Java Collections Framework.
     */
    private Booking[] appendBooking(
            Booking[] original,
            Booking newBooking) {

        Booking[] updated = new Booking[original.length + 1];

        for (int i = 0; i < original.length; i++) {

            updated[i] = original[i];
        }

        updated[original.length] = newBooking;

        return updated;
    }

    /**
     * Converts registrationRecords from the
     * custom DoublyLinkedList into an array
     * and saves it to registration.dat.
     */
    private void saveRegistrationRecords() {

        WalkInRegistration[] registrations = new WalkInRegistration[registrationRecords.size()];

        for (int i = 0; i < registrationRecords.size(); i++) {

            registrations[i] = registrationRecords.get(i);
        }

        registrationDao.saveToFile(
                registrations);
    }

    /**
     * Generates the next ID from saved records, so reopening RegistrationUI does
     * not restart from R0001.
     */
    public String generateNextRegistrationId() {
        int highestNumber = 0;

        for (int i = 0; i < registrationRecords.size(); i++) {
            String registrationId
                    = registrationRecords.get(i).getRegistrationId();

            if (registrationId == null
                    || !registrationId.matches("(?i)R\\d{4}")) {
                continue;
            }

            int number = Integer.parseInt(registrationId.substring(1));
            if (number > highestNumber) {
                highestNumber = number;
            }
        }

        return String.format("R%04d", highestNumber + 1);
    }

    private void loadSavedRegistrations() {
        WalkInRegistration[] savedRegistrations
                = registrationDao.retrieveFromFile();

        for (WalkInRegistration registration : savedRegistrations) {
            if (registration == null) {
                continue;
            }

            registrationRecords.addLast(registration);

            if (registration.getStatus() == RegistrationStatus.WAITING) {
                registrationQueue.addLast(registration);
            }
        }
    }

    private void addRecordIfAbsent(
            WalkInRegistration registration) {

        if (searchRegistrationById(registration.getRegistrationId()) == null) {
            registrationRecords.addLast(registration);
        }
    }
}