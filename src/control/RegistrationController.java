package control;

import adt.linear.DoublyLinkedList;
import adt.linear.LinearADT;
import dao.BookingDao;
import dao.GuestDao;
import dao.LoyaltyProfileDao;
import dao.RoomDao;
import dao.WalkInRegistrationDao;
import entity.Booking;
import entity.Guest;
import entity.LoyaltyProfile;
import entity.LoyaltyTier;
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
    private final LoyaltyProfileDao loyaltyProfileDao;
    private final RoomDao roomDao;
    private final BookingDao bookingDao;
    private final VipPriorityController vipPriorityController;

    private Guest[] guests;
    private LoyaltyProfile[] loyaltyProfiles;
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
        loyaltyProfileDao = new LoyaltyProfileDao();
        roomDao = new RoomDao();
        bookingDao = new BookingDao();

        guests = guestDao.loadOrSeed();
        loyaltyProfiles = loyaltyProfileDao.loadOrSeed();
        rooms = roomDao.loadOrSeed();
        bookings = loadExistingBookings();

        if (guests == null) {
            guests = new Guest[0];
        }

        if (loyaltyProfiles == null) {
            loyaltyProfiles = new LoyaltyProfile[0];
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
     * Reads an existing loyalty profile. Front-desk staff do not manually
     * create a Diamond/Platinum/Elite tier during registration.
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
            String guestId,
            String guestName,
            Long phoneNumber) {

        if (searchGuestById(guestId) != null) {
            return null;
        }

        Guest newGuest = new Guest(
                guestId,
                guestName,
                phoneNumber);

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

        return addVipRegistration(
                registration,
                loyaltyProfile.getMemberId(),
                loyaltyProfile.getTier());
    }

    public int addVipRegistration(
            WalkInRegistration registration,
            String memberId,
            LoyaltyTier tier) {

        int result = vipPriorityController.addVipRegistration(
                memberId,
                registration,
                tier);

        if (result == VipPriorityController.ADD_SUCCESS) {
            addRecordIfAbsent(registration);
        }

        return result;
    }

    public int getWaitingCount() {
        return registrationQueue.size();
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

        if (registrationId == null) {
            return null;
        }

        for (int i = 0; i < registrationQueue.size(); i++) {
            WalkInRegistration registration = registrationQueue.get(i);

            if (registration.getRegistrationId()
                    .equalsIgnoreCase(registrationId.trim())) {

                registrationQueue.removeAt(i);
                registration.setStatus(RegistrationStatus.CANCELLED);
                registrationDao.upsert(registration);
                return registration;
            }
        }

        return vipPriorityController
                .cancelVipRegistrationById(registrationId.trim());
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