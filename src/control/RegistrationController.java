package control;

import adt.linear.DoublyLinkedList;
import adt.linear.LinearADT;
import dao.BookingDao;
import dao.GuestDao;
import dao.RoomDao;
import dao.WalkInRegistrationDao;
import entity.Booking;
import entity.Guest;
import entity.LoyaltyProfile;
import entity.RegistrationStatus;
import entity.Room;
import entity.RoomStatus;
import entity.WalkInRegistration;
import java.io.File;
import java.time.LocalDateTime;
import utility.Utility;

/**
 * Controls walk-in registrations, standard FIFO room assignment and routing of
 * existing loyalty members to the VIP MaxHeap.
 *
 * @author Lai Jen Feng
 */
public class RegistrationController {

    private final LinearADT<WalkInRegistration> registrationQueue;
    private final LinearADT<WalkInRegistration> registrationRecords;

    private final GuestDao guestDao;
    private final WalkInRegistrationDao registrationDao;
    private final RoomDao roomDao;
    private final BookingDao bookingDao;
    private final VipPriorityController vipPriorityController;

    private Guest[] guests;
    private Room[] rooms;
    private Booking[] bookings;

    public RegistrationController() {
        this(new VipPriorityController());
    }

    public RegistrationController(
            VipPriorityController vipPriorityController) {

        registrationQueue = new DoublyLinkedList<>();
        registrationRecords = new DoublyLinkedList<>();

        this.vipPriorityController = vipPriorityController;
        guestDao = new GuestDao();
        registrationDao = new WalkInRegistrationDao();
        roomDao = new RoomDao();
        bookingDao = new BookingDao();

        guests = guestDao.loadOrSeed();
        rooms = roomDao.loadOrSeed();
        bookings = loadExistingBookings();

        if (guests == null) {
            guests = new Guest[0];
        }


        if (rooms == null) {
            rooms = new Room[0];
        }

        loadSavedRegistrations();
    }

    public Guest searchGuestById(String guestId) {
        if (guestId == null) {
            return null;
        }

        for (Guest guest : guests) {
            if (guest != null
                    && guest.getGuestId()
                            .equalsIgnoreCase(guestId.trim())) {

                return guest;
            }
        }

        return null;
    }

    /**
     * Finds every saved guest whose full name matches the staff input.
     * A two-pass array implementation is used so duplicate names can be
     * returned without relying on Java collection classes.
     */
    public Guest[] searchGuestsByName(String guestName) {
        if (guestName == null || guestName.isBlank()) {
            return new Guest[0];
        }

        String normalizedName = guestName.trim();
        int matchCount = 0;

        for (Guest guest : guests) {
            if (guest != null
                    && guest.getName() != null
                    && guest.getName().trim().equalsIgnoreCase(normalizedName)) {
                matchCount++;
            }
        }

        Guest[] matches = new Guest[matchCount];
        int index = 0;

        for (Guest guest : guests) {
            if (guest != null
                    && guest.getName() != null
                    && guest.getName().trim().equalsIgnoreCase(normalizedName)) {
                matches[index++] = guest;
            }
        }

        return matches;
    }

    /**
     * Finds an existing guest using the phone number entered at the counter.
     * Guest ID is an internal system identifier, so staff do not need to know
     * or manually enter it during a new walk-in registration.
     */
    public Guest searchGuestByPhoneNo(Long phoneNumber) {
        Long normalizedInput = normalizeStoredPhoneNumber(phoneNumber);

        if (normalizedInput == null) {
            return null;
        }

        for (Guest guest : guests) {
            if (guest == null || guest.getPhoneNo() == null) {
                continue;
            }

            Long normalizedStored
                    = normalizeStoredPhoneNumber(guest.getPhoneNo());

            if (normalizedInput.equals(normalizedStored)) {
                return guest;
            }
        }

        return null;
    }

    /**
     * Reads loyalty information through the VIP/Loyalty controller.
     * The public method is retained so RegistrationUI output and flow remain
     * unchanged while loyalty business rules stay inside the VIP module.
     */
    public LoyaltyProfile searchLoyaltyProfileByGuestId(String guestId) {
        return vipPriorityController.searchLoyaltyProfileByGuestId(guestId);
    }

    /**
     * Delegates loyalty qualification and tier refresh to the VIP/Loyalty
     * controller. Kept here only as an integration facade for existing UI code.
     */
    public LoyaltyProfile refreshLoyaltyProfileByGuestId(String guestId) {
        return vipPriorityController.refreshLoyaltyProfileByGuestId(guestId);
    }

    /**
     * Delegates completed-stay counting to the VIP/Loyalty module.
     */
    public int getCompletedStayCount(String guestId) {
        return vipPriorityController.getCompletedStayCount(guestId);
    }

    /**
     * Calculates the remaining completed stays needed before the Registration UI
     * displays the guest as eligible for ELITE. The completed-stay total itself
     * is supplied by the VIP/Loyalty module.
     */
    public int getStaysNeededForElite(String guestId) {
        return Math.max(
                0,
                LoyaltyProfile.ELITE_MIN_STAYS
                - vipPriorityController.getCompletedStayCount(guestId));
    }

    /**
     * Prevents duplicate active registrations and duplicate concurrent stays.
     */
    public boolean hasActiveRegistrationOrStay(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            return false;
        }

        String normalizedGuestId = guestId.trim();

        for (int i = 0; i < registrationRecords.size(); i++) {
            WalkInRegistration registration = registrationRecords.get(i);

            if (registration == null || registration.getGuest() == null) {
                continue;
            }

            boolean sameGuest = registration.getGuest().getGuestId()
                    .equalsIgnoreCase(normalizedGuestId);

            RegistrationStatus status = registration.getStatus();
            boolean stillWaiting = status == RegistrationStatus.WAITING
                    || status == RegistrationStatus.VIP_WAITING;

            if (sameGuest && stillWaiting) {
                return true;
            }
        }

        bookings = loadExistingBookings();

        for (Booking booking : bookings) {
            if (booking == null
                    || booking.getGuest() == null
                    || booking.getRoom() == null) {
                continue;
            }

            boolean sameGuest = booking.getGuest().getGuestId()
                    .equalsIgnoreCase(normalizedGuestId);

            boolean currentlyCheckedIn
                    = booking.getRoom().getRoomStatus() == RoomStatus.OCCUPIED;

            if (sameGuest && currentlyCheckedIn) {
                return true;
            }
        }

        return false;
    }

    public Guest addNewGuest(
            String guestName,
            Long phoneNumber) {

        Long normalizedPhoneNumber
                = normalizeStoredPhoneNumber(phoneNumber);

        if (!Utility.isValidPersonName(guestName)
                || normalizedPhoneNumber == null
                || searchGuestByPhoneNo(normalizedPhoneNumber) != null) {
            return null;
        }

        String guestId = generateNextGuestId();

        Guest newGuest = new Guest(
                guestId,
                guestName.trim(),
                normalizedPhoneNumber);

        Guest[] updatedGuests = new Guest[guests.length + 1];
        System.arraycopy(guests, 0, updatedGuests, 0, guests.length);
        updatedGuests[guests.length] = newGuest;

        guests = updatedGuests;
        guestDao.saveToFile(guests);
        return newGuest;
    }

    public void addStandardRegistration(
            WalkInRegistration registration) {

        if (registration == null) {
            return;
        }

        registration.setStatus(RegistrationStatus.WAITING);
        registrationQueue.addLast(registration);
        addRecordIfAbsent(registration);
        registrationDao.upsert(registration);
    }

    public void addRegistration(
            WalkInRegistration registration) {

        addStandardRegistration(registration);
    }

    public int addVipRegistration(
            WalkInRegistration registration,
            LoyaltyProfile loyaltyProfile) {

        if (loyaltyProfile == null) {
            return VipPriorityController.INVALID_INPUT;
        }

        int result = vipPriorityController.addVipRegistration(
                registration,
                loyaltyProfile.getTier());

        if (result == VipPriorityController.ADD_SUCCESS) {
            addRecordIfAbsent(registration);
        }

        return result;
    }

    public int getWaitingCount() {
        return registrationQueue.size();
    }

    /**
     * Returns the highest single-room occupancy supported by the current hotel
     * inventory. One walk-in registration represents one room request.
     */
    public int getMaximumRoomCapacity() {
        rooms = roomDao.loadOrSeed();
        int maximum = 0;

        for (Room room : rooms) {
            if (room != null && room.getNoOfGuest() > maximum) {
                maximum = room.getNoOfGuest();
            }
        }

        return maximum;
    }

    /**
     * Returns the maximum occupancy offered by a particular room type.
     * A value of 0 means the room type is not present in the current inventory.
     */
    public int getMaximumCapacityForRoomType(String roomType) {
        if (roomType == null || roomType.isBlank()) {
            return 0;
        }

        rooms = roomDao.loadOrSeed();
        int maximum = 0;

        for (Room room : rooms) {
            if (room != null
                    && room.getRoomType().equalsIgnoreCase(roomType.trim())
                    && room.getNoOfGuest() > maximum) {

                maximum = room.getNoOfGuest();
            }
        }

        return maximum;
    }

    /**
     * Counts physically ready/assignable rooms for the request summary shown by
     * RegistrationUI. VIP priority itself remains handled by the VIP MaxHeap.
     */
    public int getReadyRoomCountForRequest(
            String roomType,
            int numberOfGuests) {

        rooms = roomDao.loadOrSeed();
        int count = 0;

        for (Room room : rooms) {
            if (isSuitableRoomForRequest(room, roomType, numberOfGuests)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Counts rooms a Standard guest could currently use after respecting the
     * assignment rule that waiting VIPs receive first access to suitable rooms.
     */
    public int getReadyRoomCountForStandardRequest(
            String roomType,
            int numberOfGuests) {

        rooms = roomDao.loadOrSeed();
        int count = 0;

        for (Room room : rooms) {
            if (isSuitableRoomForRequest(room, roomType, numberOfGuests)
                    && !vipPriorityController
                            .hasWaitingVipEligibleForRoom(room)) {
                count++;
            }
        }

        return count;
    }

    public int getVipWaitingCount() {
        return vipPriorityController.getWaitingCount();
    }

    public boolean hasWaitingVip() {
        return vipPriorityController.hasWaitingVip();
    }

    public WalkInRegistration getRegistrationAt(int index) {
        if (index < 0 || index >= registrationQueue.size()) {
            return null;
        }

        return registrationQueue.get(index);
    }

    public WalkInRegistration getNextRegistration() {
        if (registrationQueue.isEmpty()) {
            return null;
        }

        return registrationQueue.get(0);
    }

    /**
     * Returns rooms that are clean/ready, of the requested type, large enough
     * for the first Standard guest, and not currently needed by a waiting VIP
     * who can use that specific room.
     */
    public Room[] getSuitableRoomsForNextStandard() {
        WalkInRegistration registration = getNextRegistration();

        if (registration == null) {
            return new Room[0];
        }

        rooms = roomDao.loadOrSeed();
        int suitableCount = 0;

        for (Room room : rooms) {
            if (isSuitableRoom(room, registration)
                    && !vipPriorityController
                            .hasWaitingVipEligibleForRoom(room)) {
                suitableCount++;
            }
        }

        Room[] suitableRooms = new Room[suitableCount];
        int index = 0;

        for (Room room : rooms) {
            if (isSuitableRoom(room, registration)
                    && !vipPriorityController
                            .hasWaitingVipEligibleForRoom(room)) {
                suitableRooms[index++] = room;
            }
        }

        return suitableRooms;
    }

    /**
     * Assigns a selected room and creates the booking needed later by Front
     * Desk checkout. The Standard guest is removed from the FIFO only after the
     * room and booking are saved successfully.
     */
    public Booking checkInNextStandard(String selectedRoomNumber) {
        if (registrationQueue.isEmpty()) {
            return null;
        }

        WalkInRegistration registration = registrationQueue.get(0);
        rooms = roomDao.loadOrSeed();
        bookings = loadExistingBookings();

        Room selectedRoom = findSelectedSuitableRoom(
                selectedRoomNumber,
                registration);

        if (selectedRoom == null) {
            return null;
        }

        /*
         * VIP priority is room-specific. A Standard guest is blocked only
         * when a waiting VIP can actually use this selected vacant room.
         */
        if (vipPriorityController
                .hasWaitingVipEligibleForRoom(selectedRoom)) {
            return null;
        }

        LocalDateTime actualCheckInTime = LocalDateTime.now()
                .withSecond(0)
                .withNano(0);

        selectedRoom.setRoomStatus(RoomStatus.OCCUPIED);
        selectedRoom.setBookingDate(actualCheckInTime);
        selectedRoom.setCheckInDateTime(actualCheckInTime);
        selectedRoom.setCheckOutDateTime(registration.getCheckOutDateTime());

        registration.setCheckInDateTime(actualCheckInTime);
        registration.setStatus(RegistrationStatus.CHECKED_IN);

        Booking booking = new Booking(
                generateUniqueConfirmationNo(),
                registration.getGuest(),
                selectedRoom,
                null);

        bookings = appendBooking(bookings, booking);

        roomDao.saveToFile(rooms);
        bookingDao.saveToFile(bookings);
        registrationQueue.removeFirst();
        registrationDao.upsert(registration);

        return booking;
    }

    /**
     * Retained only for compatibility. A real check-in should not complete
     * without an assigned room and booking.
     */
    @Deprecated
    public WalkInRegistration processNextRegistration() {
        return null;
    }

    public WalkInRegistration markGuestCheckedOut(String guestId) {

        if (guestId == null || guestId.isBlank()) {
            return null;
        }

        String normalizedGuestId = guestId.trim();

        // Search from newest record backwards.
        for (int i = registrationRecords.size() - 1; i >= 0; i--) {

            WalkInRegistration registration
                    = registrationRecords.get(i);

            if (registration == null
                    || registration.getGuest() == null) {
                continue;
            }

            boolean sameGuest
                    = registration.getGuest()
                            .getGuestId()
                            .equalsIgnoreCase(normalizedGuestId);

            boolean currentlyCheckedIn
                    = registration.getStatus()
                            == RegistrationStatus.CHECKED_IN;

            if (sameGuest && currentlyCheckedIn) {

                registration.setStatus(
                        RegistrationStatus.CHECKED_OUT);

                registrationDao.upsert(registration);

                // Notify the VIP/Loyalty module after a completed stay.
                vipPriorityController.refreshLoyaltyProfileByGuestId(
                        normalizedGuestId);

                return registration;
            }
        }

        return null;
    }

    public WalkInRegistration searchRegistrationById(
            String registrationId) {

        if (registrationId == null) {
            return null;
        }

        for (int i = 0; i < registrationRecords.size(); i++) {
            WalkInRegistration registration = registrationRecords.get(i);

            if (registration != null
                    && registration.getRegistrationId()
                            .equalsIgnoreCase(registrationId.trim())) {

                return registration;
            }
        }

        return null;
    }

    public int getTotalRegistrationCount() {
        return registrationRecords.size();
    }

    public WalkInRegistration getRecordAt(int index) {
        if (index < 0 || index >= registrationRecords.size()) {
            return null;
        }

        return registrationRecords.get(index);
    }

    public WalkInRegistration cancelRegistrationById(
            String registrationId) {

        if (!Utility.isValidRegistrationId(registrationId)) {
            return null;
        }

        for (int i = 0; i < registrationQueue.size(); i++) {
            WalkInRegistration registration = registrationQueue.get(i);

            if (registration != null
                    && registration.getRegistrationId()
                            .equalsIgnoreCase(registrationId.trim())) {

                registrationQueue.removeAt(i);
                registration.setStatus(RegistrationStatus.CANCELLED);
                registrationDao.upsert(registration);
                return registration;
            }
        }

        /*
         * VIP waiting requests are intentionally not cancelled here. They are
         * maintained in the separate VIP module so each assignment module owns
         * its own ADT operations.
         */
        return null;
    }

    public String generateRegistrationId() {
        return generateNextRegistrationId();
    }

    public String generateNextRegistrationId() {
        int highestNumber = 0;

        for (int i = 0; i < registrationRecords.size(); i++) {
            WalkInRegistration registration = registrationRecords.get(i);

            if (registration == null) {
                continue;
            }

            String registrationId = registration.getRegistrationId();

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
                = registrationDao.loadExisting();

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

        if (registration != null
                && searchRegistrationById(
                        registration.getRegistrationId()) == null) {

            registrationRecords.addLast(registration);
        }
    }

    private Booking[] loadExistingBookings() {
        File bookingFile = new File("booking.dat");

        if (!bookingFile.exists()) {
            return new Booking[0];
        }

        Booking[] loadedBookings = bookingDao.retrieveFromFile();
        return loadedBookings == null ? new Booking[0] : loadedBookings;
    }

    private boolean isSuitableRoom(
            Room room,
            WalkInRegistration registration) {

        if (room == null || registration == null) {
            return false;
        }

        boolean roomReady = room.isAssignable();
        boolean matchingRoomType = room.getRoomType()
                .equalsIgnoreCase(registration.getRequestedRoomType());
        boolean enoughCapacity = room.getNoOfGuest()
                >= registration.getNumberOfGuests();

        return roomReady && matchingRoomType && enoughCapacity;
    }

    private Room findSelectedSuitableRoom(
            String roomNumber,
            WalkInRegistration registration) {

        if (roomNumber == null) {
            return null;
        }

        for (Room room : rooms) {
            if (room != null
                    && room.getRoomNumber()
                            .equalsIgnoreCase(roomNumber.trim())
                    && isSuitableRoom(room, registration)) {

                return room;
            }
        }

        return null;
    }

    private boolean isSuitableRoomForRequest(
            Room room,
            String roomType,
            int numberOfGuests) {

        if (room == null
                || roomType == null
                || roomType.isBlank()
                || numberOfGuests <= 0) {
            return false;
        }

        return room.isAssignable()
                && room.getRoomType().equalsIgnoreCase(roomType.trim())
                && room.getNoOfGuest() >= numberOfGuests;
    }

    /**
     * Normalizes both current and legacy Guest.phoneNo values to 60... format.
     * Older records may have lost the leading 0 when a domestic number was
     * converted directly to Long (e.g. 0123456789 -> 123456789).
     */
    private Long normalizeStoredPhoneNumber(Long phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        String digits = String.valueOf(phoneNumber);

        if (digits.startsWith("60")
                && (digits.length() == 11 || digits.length() == 12)) {
            return phoneNumber;
        }

        if (digits.startsWith("1")
                && (digits.length() == 9 || digits.length() == 10)) {
            try {
                return Long.valueOf("60" + digits);
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return phoneNumber;
    }

    /**
     * Generates an internal guest ID such as G0001, G0002, ... .
     * Numeric legacy guest IDs are also considered so new IDs do not restart
     * from G0001 when old sample data contains IDs such as 1, 2, 3.
     */
    private String generateNextGuestId() {
        int highestNumber = 0;

        for (Guest guest : guests) {
            if (guest == null || guest.getGuestId() == null) {
                continue;
            }

            String guestId = guest.getGuestId().trim();
            int number;

            try {
                if (guestId.matches("(?i)G\\d{4}")) {
                    number = Integer.parseInt(guestId.substring(1));
                } else if (guestId.matches("\\d+")) {
                    number = Integer.parseInt(guestId);
                } else {
                    continue;
                }
            } catch (NumberFormatException exception) {
                continue;
            }

            if (number > highestNumber) {
                highestNumber = number;
            }
        }

        return String.format("G%04d", highestNumber + 1);
    }

    private String generateUniqueConfirmationNo() {
        String confirmationNo;

        do {
            confirmationNo = Utility.generateConfirmationNo();
        } while (confirmationNoExists(confirmationNo));

        return confirmationNo;
    }

    private boolean confirmationNoExists(String confirmationNo) {
        for (Booking booking : bookings) {
            if (booking != null
                    && booking.getConfirmationNo() != null
                    && booking.getConfirmationNo()
                            .equals(confirmationNo)) {

                return true;
            }
        }

        return false;
    }

    private Booking[] appendBooking(
            Booking[] original,
            Booking newBooking) {

        Booking[] safeOriginal = original == null
                ? new Booking[0]
                : original;

        Booking[] updated = new Booking[safeOriginal.length + 1];
        System.arraycopy(
                safeOriginal,
                0,
                updated,
                0,
                safeOriginal.length);

        updated[safeOriginal.length] = newBooking;
        return updated;
    }
}