package control;

import adt.heap.MaxHeap;
import adt.heap.PriorityQueueADT;
import dao.PaymentDao;
import dao.BookingDao;
import dao.GuestDao;
import dao.LoyaltyProfileDao;
import dao.RoomDao;
import dao.WalkInRegistrationDao;
import entity.Payment;
import entity.Booking;
import entity.Guest;
import entity.LoyaltyProfile;
import entity.LoyaltyTier;
import entity.RegistrationStatus;
import entity.Room;
import entity.RoomStatus;
import entity.RoomType;
import entity.WalkInRegistration;
import java.time.Duration;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import utility.Utility;

/**
 * Controls VIP registration priority and room allocation using a MaxHeap.
 *
 * Existing loyalty tier is supplied by the stored LoyaltyProfile. The heap is
 * shared by RegistrationUI and VipAllocationUI so VIP registrations remain
 * available even though Main creates the UIs separately.
 *
 * @author Low Enn Toong
 */
public class VipPriorityController {
    public static final int ADD_SUCCESS = 1;
    public static final int INVALID_INPUT = -1;
    public static final int REGISTRATION_ALREADY_QUEUED = -3;
    public static final int GUEST_ALREADY_QUEUED = -4;
    private static final int MAX_STAY_NIGHTS = 30;
    private static final LocalTime STANDARD_CHECKOUT_TIME = LocalTime.NOON;
    private static final LoyaltyProfileDao PRIORITY_LOYALTY_DAO = new LoyaltyProfileDao();
    /*
     * Heap comparisons happen many times during enqueue/dequeue. Keep an
     * in-memory snapshot of loyalty master data so a single comparison never
     * performs file I/O. The cache is refreshed whenever loyalty data may have
     * changed and before new VIP entries are enqueued.
     */
    private static LoyaltyProfile[] priorityProfileCache = PRIORITY_LOYALTY_DAO.loadOrSeed();
    private static final PriorityQueueADT<WalkInRegistration> PRIORITY_QUEUE = new MaxHeap<>(VipPriorityController::compareVipPriority);
    private static boolean waitingVipRegistrationsLoaded = false;
    private final WalkInRegistrationDao registrationDao;
    private final RoomDao roomDao;
    private final BookingDao bookingDao;
    private final GuestDao guestDao;
    private final LoyaltyProfileDao loyaltyProfileDao;
    private Room[] rooms;
    private Booking[] bookings;

    public VipPriorityController() {
        registrationDao = new WalkInRegistrationDao();
        roomDao = new RoomDao();
        bookingDao = new BookingDao();
        guestDao = new GuestDao();
        loyaltyProfileDao = new LoyaltyProfileDao();
        rooms = roomDao.loadOrSeed();
        bookings = loadExistingBookings();
        refreshPriorityProfileCache();
        loadWaitingVipRegistrationsOnce();
    }

    /**
     * Finds the stored loyalty profile for a guest. Loyalty master-data lookup
     * belongs to the VIP/Loyalty module rather than Standard registration.
     */
    public LoyaltyProfile searchLoyaltyProfileByGuestId(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            return null;
        }

        LoyaltyProfile[] profiles = loyaltyProfileDao.loadOrSeed();

        for (LoyaltyProfile profile : profiles) {
            if (profile != null && profile.getGuestId() != null && profile.getGuestId().equalsIgnoreCase(guestId.trim())) {
                return profile;
            }
        }

        return null;
    }

    /**
     * Recalculates a guest's loyalty progress from completed stays and updates
     * the stored tier. Guests below ELITE remain Standard and have no VIP profile.
     */
    public LoyaltyProfile refreshLoyaltyProfileByGuestId(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            return null;
        }

        LoyaltyProfile[] profiles = loyaltyProfileDao.loadOrSeed();
        LoyaltyProfile existingProfile = findLoyaltyProfile(profiles, guestId);
        int completedStays = getCompletedStayCount(guestId);

        if (existingProfile != null) {
            int before = existingProfile.getCompletedStays();
            LoyaltyTier beforeTier = existingProfile.getTier();

            existingProfile.updateCompletedStays(completedStays);

            if (before != existingProfile.getCompletedStays() || beforeTier != existingProfile.getTier()) {
                loyaltyProfileDao.saveToFile(profiles);
                refreshPriorityProfileCache();
            }

            return existingProfile;
        }

        LoyaltyTier qualifiedTier = LoyaltyProfile.determineTier(completedStays);

        if (qualifiedTier == null) {
            return null;
        }

        LoyaltyProfile newProfile = new LoyaltyProfile(guestId.trim(), completedStays);
        LoyaltyProfile[] updatedProfiles = new LoyaltyProfile[profiles.length + 1];
        System.arraycopy(profiles, 0, updatedProfiles, 0, profiles.length);
        updatedProfiles[profiles.length] = newProfile;
        loyaltyProfileDao.saveToFile(updatedProfiles);
        refreshPriorityProfileCache();

        return newProfile;
    }

    /**
     * Records one newly completed stay after Front Desk successfully checks a
     * guest out. Existing loyalty profiles are incremented by exactly one.
     * Standard guests continue to use checked-out registration history until
     * they qualify for ELITE, when a loyalty profile is created automatically.
     */
    public LoyaltyProfile recordCompletedStay(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            return null;
        }

        String normalizedGuestId = guestId.trim();
        LoyaltyProfile[] profiles = loyaltyProfileDao.loadOrSeed();
        LoyaltyProfile existingProfile = findLoyaltyProfile(profiles, normalizedGuestId);

        if (existingProfile != null) {
            existingProfile.updateCompletedStays(existingProfile.getCompletedStays() + 1);
            loyaltyProfileDao.saveToFile(profiles);
            refreshPriorityProfileCache();
            return existingProfile;
        }

        int completedStays = countCheckedOutRegistrations(normalizedGuestId);
        LoyaltyTier qualifiedTier = LoyaltyProfile.determineTier(completedStays);

        if (qualifiedTier == null) {
            return null;
        }

        LoyaltyProfile newProfile = new LoyaltyProfile(normalizedGuestId, completedStays);
        LoyaltyProfile[] updatedProfiles = new LoyaltyProfile[profiles.length + 1];
        System.arraycopy(profiles, 0, updatedProfiles, 0, profiles.length);
        updatedProfiles[profiles.length] = newProfile;
        loyaltyProfileDao.saveToFile(updatedProfiles);
        refreshPriorityProfileCache();

        return newProfile;
    }

    /**
     * Counts only stays that have completed the Front Desk checkout process.
     * Existing stored loyalty progress is preserved for seeded/legacy profiles.
     */
    public int getCompletedStayCount(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            return 0;
        }

        String normalizedGuestId = guestId.trim();
        int historicalCount = countCheckedOutRegistrations(normalizedGuestId);

        LoyaltyProfile profile = searchLoyaltyProfileByGuestId(normalizedGuestId);
        int storedCount = profile == null ? 0 : profile.getCompletedStays();

        return Math.max(historicalCount, storedCount);
    }

    private int countCheckedOutRegistrations(String guestId) {
        WalkInRegistration[] registrations = registrationDao.loadExisting();
        int completedCount = 0;

        for (WalkInRegistration registration : registrations) {
            if (registration == null || registration.getGuest() == null || registration.getGuest().getGuestId() == null) {
                continue;
            }

            boolean sameGuest = registration.getGuest().getGuestId().equalsIgnoreCase(guestId);
            boolean stayCompleted = registration.getStatus() == RegistrationStatus.CHECKED_OUT;

            if (sameGuest && stayCompleted) {
                completedCount++;
            }
        }

        return completedCount;
    }

    private LoyaltyProfile findLoyaltyProfile(LoyaltyProfile[] profiles, String guestId) {
        if (profiles == null || guestId == null) {
            return null;
        }

        for (LoyaltyProfile profile : profiles) {
            if (profile != null && profile.getGuestId() != null && profile.getGuestId().equalsIgnoreCase(guestId.trim())) {
                return profile;
            }
        }

        return null;
    }

    /**
     * Returns every guest who currently has a valid VIP loyalty tier.
     * This is master loyalty data and is separate from the VIP waiting heap.
     */
    public LoyaltyProfile[] getAllVipProfiles() {
        LoyaltyProfile[] profiles = loyaltyProfileDao.loadOrSeed();
        int vipCount = 0;

        for (LoyaltyProfile profile : profiles) {
            if (profile != null && profile.getTier() != null) {
                vipCount++;
            }
        }

        LoyaltyProfile[] vipProfiles = new LoyaltyProfile[vipCount];
        int index = 0;

        for (LoyaltyProfile profile : profiles) {
            if (profile != null && profile.getTier() != null) {
                vipProfiles[index++] = profile;
            }
        }

        return vipProfiles;
    }

    /**
     * Returns all persisted registrations for VIP management reporting.
     * Report classes use the full history instead of only the current MaxHeap.
     */
    public WalkInRegistration[] getAllRegistrationsForReport() {
        WalkInRegistration[] registrations = registrationDao.loadExisting();
        return registrations == null ? new WalkInRegistration[0] : registrations;
    }

    /**
     * Returns all persisted bookings for VIP management reporting.
     */
    public Booking[] getAllBookingsForReport() {
        Booking[] existingBookings = loadExistingBookings();
        return existingBookings == null ? new Booking[0] : existingBookings;
    }

    /**
     * Finds the guest master record that belongs to a loyalty profile.
     */
    public Guest findGuestById(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            return null;
        }

        Guest[] guests = guestDao.loadOrSeed();

        for (Guest guest : guests) {
            if (guest != null && guest.getGuestId() != null && guest.getGuestId().equalsIgnoreCase(guestId.trim())) {
                return guest;
            }
        }

        return null;
    }

    /**
     * Inserts an existing loyalty member into the MaxHeap. Higher tier wins;
     * for the same tier, earlier registration time wins.
     */
    public int addVipRegistration(WalkInRegistration registration, LoyaltyTier tier) {
        if (registration == null || registration.getGuest() == null || registration.getRegistrationId() == null || tier == null) {
            return INVALID_INPUT;
        }

        if (registrationAlreadyQueued(registration.getRegistrationId())) {
            return REGISTRATION_ALREADY_QUEUED;
        }

        if (guestAlreadyQueued(registration.getGuest().getGuestId())) {
            return GUEST_ALREADY_QUEUED;
        }

        registration.setStatus(RegistrationStatus.VIP_WAITING);
        refreshPriorityProfileCache();
        PRIORITY_QUEUE.enqueue(registration);

        registrationDao.upsert(registration);
        
        return ADD_SUCCESS;
    }

    public WalkInRegistration peekNextVip() {
        return PRIORITY_QUEUE.peek();
    }

    public int getWaitingCount() {
        return PRIORITY_QUEUE.size();
    }

    public boolean hasWaitingVip() {
        return !PRIORITY_QUEUE.isEmpty();
    }

    /**
     * Returns true only when at least one waiting VIP can actually use the
     * specified vacant room. This prevents unrelated VIP requests from
     * blocking Standard guests who need a different room type/capacity.
     */
    public boolean hasWaitingVipEligibleForRoom(Room room) {
        if (room == null || !room.isAssignable()) {
            return false;
        }

        PriorityQueueADT<WalkInRegistration> copiedQueue = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            WalkInRegistration registration = copiedQueue.dequeue();

            if (isRoomSuitableForRegistration(room, registration)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Counts rooms that an incoming VIP can currently access after protecting
     * one suitable room for every waiting VIP who has higher priority, or the
     * same tier but arrived earlier. Lower-tier waiting VIPs do not reduce the
     * incoming VIP's count because the MaxHeap will place the higher-tier guest
     * ahead of them for allocation.
     */
    public int getReadyRoomCountForIncomingVip(String roomType, int numberOfGuests, LoyaltyTier incomingTier) {
        if (roomType == null || roomType.isBlank() || numberOfGuests <= 0 || incomingTier == null) {
            return 0;
        }

        Room[] currentRooms = roomDao.loadOrSeed();
        boolean[] reservedForHigherOrEarlierVip = new boolean[currentRooms.length];
        PriorityQueueADT<WalkInRegistration> copiedQueue = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            WalkInRegistration waitingRegistration = copiedQueue.dequeue();
            LoyaltyTier waitingTier = getLoyaltyTier(waitingRegistration);

            if (waitingTier == null || waitingTier.getPriority() < incomingTier.getPriority()) {
                continue;
            }

            /*
             * Any already-waiting VIP of the same tier arrived before the new
             * registration, so that guest also remains ahead in the MaxHeap.
             * One waiting VIP protects only one suitable ready room.
             */
            for (int i = 0; i < currentRooms.length; i++) {
                if (!reservedForHigherOrEarlierVip[i] && isRoomSuitableForRegistration(currentRooms[i], waitingRegistration)) {
                    reservedForHigherOrEarlierVip[i] = true;
                    break;
                }
            }
        }

        int availableCount = 0;

        for (int i = 0; i < currentRooms.length; i++) {
            Room room = currentRooms[i];

            if (!reservedForHigherOrEarlierVip[i] && room != null && room.isAssignable() && room.getRoomType() != null && room.getRoomType().equalsIgnoreCase(roomType.trim()) && room.getNoOfGuest() >= numberOfGuests) {
                availableCount++;
            }
        }

        return availableCount;
    }

    public boolean isRoomReservedForWaitingVip(Room targetRoom) {
        if (targetRoom == null || !targetRoom.isAssignable()) {
            return false;
        }

        Room[] currentRooms = roomDao.loadOrSeed();
        boolean[] reserved = new boolean[currentRooms.length];

        PriorityQueueADT<WalkInRegistration> copiedQueue = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            WalkInRegistration registration = copiedQueue.dequeue();

            for (int i = 0; i < currentRooms.length; i++) {
                if (!reserved[i] && isRoomSuitableForRegistration(currentRooms[i], registration)) {
                    // One VIP registration only protects one room
                    reserved[i] = true;
                    break;
                }
            }
        }

        for (int i = 0; i < currentRooms.length; i++) {
            if (reserved[i] && currentRooms[i].getRoomNumber().equalsIgnoreCase(targetRoom.getRoomNumber())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds the highest-priority VIP who can currently be assigned a room.
     * A higher-priority VIP who needs a different unavailable room remains in
     * the heap and does not prevent another VIP from using a suitable room.
     */
    public WalkInRegistration peekNextAllocatableVip() {
        rooms = roomDao.loadOrSeed();
        PriorityQueueADT<WalkInRegistration> copiedQueue = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            WalkInRegistration registration = copiedQueue.dequeue();

            if (findSuitableVacantRoom(registration) != null) {
                return registration;
            }
        }

        return null;
    }

    /**
     * Allocates a clean/ready suitable room to the highest-priority VIP and
     * creates a Booking so the same guest can later be checked out by Front
     * Desk using a confirmation number.
     */
    public Booking allocateNextVipBooking() {
        if (PRIORITY_QUEUE.isEmpty()) {
            return null;
        }

        rooms = roomDao.loadOrSeed();
        bookings = loadExistingBookings();

        PriorityQueueADT<WalkInRegistration> retainedRegistrations = new MaxHeap<>(VipPriorityController::compareVipPriority);
        WalkInRegistration selectedRegistration = null;
        Room suitableRoom = null;

        /*
         * Dequeue in MaxHeap priority order until the highest-priority VIP
         * who can use a currently vacant room is found. Higher-priority VIPs
         * without a suitable room are temporarily retained and reinserted.
         */
        while (!PRIORITY_QUEUE.isEmpty()) {
            WalkInRegistration candidate = PRIORITY_QUEUE.dequeue();
            Room candidateRoom = findSuitableVacantRoom(candidate);

            if (candidateRoom != null) {
                selectedRegistration = candidate;
                suitableRoom = candidateRoom;
                break;
            }

            retainedRegistrations.enqueue(candidate);
        }

        while (!retainedRegistrations.isEmpty()) {
            PRIORITY_QUEUE.enqueue(retainedRegistrations.dequeue());
        }

        if (selectedRegistration == null) {
            return null;
        }

        WalkInRegistration registration = selectedRegistration;
        LocalDateTime actualCheckInTime = LocalDateTime.now().withSecond(0).withNano(0);
        updateAllocatedRoom(suitableRoom, registration, actualCheckInTime);
        registration.setCheckInDateTime(actualCheckInTime);
        registration.setStatus(RegistrationStatus.CHECKED_IN);
        long numberOfNights = calculateStayNights(actualCheckInTime, registration.getCheckOutDateTime());
        double amount = getRoomTypePricePerDay(suitableRoom.getRoomType()) * numberOfNights;
        String confirmationNo = generateUniqueConfirmationNo();
        Payment payment = new Payment(generateNextPaymentId(), amount, actualCheckInTime, 'C');
        Booking booking = new Booking(confirmationNo, registration.getGuest(), suitableRoom,payment);
        bookings = appendBooking(bookings, booking);

        PaymentDao paymentDao = new PaymentDao();

        Payment[] savedPayments = paymentDao.loadOrSeed();

        Payment[] updatedPayments =
                new Payment[savedPayments.length + 1];

        System.arraycopy(
                savedPayments,
                0,
                updatedPayments,
                0,
                savedPayments.length);

        updatedPayments[savedPayments.length] = payment;

        paymentDao.saveToFile(updatedPayments);

        roomDao.saveToFile(rooms);
        bookingDao.saveToFile(bookings);
        registrationDao.upsert(registration);

        /* selectedRegistration has already been removed from the heap above. */
        
        return booking;
    }

    /**
     * Compatibility method retained for existing code that only needs Room.
     */
    public Room allocateNextVipRoom() {
        Booking booking = allocateNextVipBooking();
        return booking == null ? null : booking.getRoom();
    }

    public WalkInRegistration[] getVipRegistrationsByPriority() {
        PriorityQueueADT<WalkInRegistration> copiedQueue = PRIORITY_QUEUE.copy();
        WalkInRegistration[] registrations = new WalkInRegistration[copiedQueue.size()];

        for (int i = 0; i < registrations.length; i++) {
            registrations[i] = copiedQueue.dequeue();
        }

        return registrations;
    }

    /**
     * Finds one waiting VIP by registration ID without removing the
     * registration from the MaxHeap.
     */
    public WalkInRegistration findWaitingVipRegistrationById(String registrationId) {
        if (!Utility.isValidRegistrationId(registrationId)) {
            return null;
        }

        PriorityQueueADT<WalkInRegistration> copiedQueue = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            WalkInRegistration registration = copiedQueue.dequeue();

            if (registration != null && registration.getStatus() == RegistrationStatus.VIP_WAITING && registration.getRegistrationId().equalsIgnoreCase(registrationId.trim())) {
                return registration;
            }
        }

        return null;
    }

    /**
     * Updates editable parts of a waiting VIP room request. Loyalty tier,
     * priority and registration time are intentionally not editable.
     * These fields do not affect heap priority, so the registration keeps the
     * same MaxHeap ordering after the request update.
     */
    public boolean updateVipRegistrationRequest(String registrationId, String requestedRoomType, int numberOfGuests, LocalDateTime checkOutDateTime) {
        WalkInRegistration registration = findWaitingVipRegistrationById(registrationId);

        if (registration == null || requestedRoomType == null || requestedRoomType.isBlank() || numberOfGuests <= 0 || checkOutDateTime == null) {
            return false;
        }

        String normalizedRoomType = requestedRoomType.trim().toUpperCase();

        try {
            RoomType.valueOf(normalizedRoomType);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        int maximumCapacity = getMaximumCapacityForRoomType(normalizedRoomType);
        if (maximumCapacity <= 0 || numberOfGuests > maximumCapacity) {
            return false;
        }

        LocalDate arrivalDate = registration.getRegistrationTime() == null ? LocalDate.now() : registration.getRegistrationTime().toLocalDate();
        LocalDate requestedCheckOutDate = checkOutDateTime.toLocalDate();

        if (!requestedCheckOutDate.isAfter(arrivalDate) || requestedCheckOutDate.isAfter(arrivalDate.plusDays(MAX_STAY_NIGHTS))) {
            return false;
        }

        // VIP request maintenance can change the departure date only. The hotel
        // standard check-out time remains fixed at 12:00 PM, matching RegistrationUI.
        LocalDateTime normalizedCheckOutDateTime = LocalDateTime.of(requestedCheckOutDate, STANDARD_CHECKOUT_TIME);

        if (!normalizedCheckOutDateTime.isAfter(LocalDateTime.now().withSecond(0).withNano(0))) {
            return false;
        }

        registration.setRequestedRoomType(normalizedRoomType);
        registration.setNumberOfGuests(numberOfGuests);
        registration.setCheckOutDateTime(normalizedCheckOutDateTime);

        registrationDao.upsert(registration);
        return true;
    }

    /**
     * Returns the largest configured room capacity for a room type. This keeps
     * UI validation aligned with the actual room inventory.
     */
    public int getMaximumCapacityForRoomType(String roomType) {
        if (roomType == null || roomType.isBlank()) {
            return 0;
        }

        rooms = roomDao.loadOrSeed();
        int maximumCapacity = 0;

        for (Room room : rooms) {
            if (room != null && room.getRoomType() != null && room.getRoomType().equalsIgnoreCase(roomType.trim()) && room.getNoOfGuest() > maximumCapacity) {
                maximumCapacity = room.getNoOfGuest();
            }
        }

        return maximumCapacity;
    }

    /**
     * Returns a currently ready room suitable for the supplied VIP, without
     * changing the heap or room data.
     */
    public Room findReadyRoomForRegistration(WalkInRegistration registration) {
        if (registration == null) {
            return null;
        }

        rooms = roomDao.loadOrSeed();
        return findSuitableVacantRoom(registration);
    }

    public Room[] getVacantRooms() {
        rooms = roomDao.loadOrSeed();
        int vacantCount = 0;

        for (Room room : rooms) {
            if (room != null && room.isAssignable()) {
                vacantCount++;
            }
        }

        Room[] vacantRooms = new Room[vacantCount];
        int index = 0;

        for (Room room : rooms) {
            if (room != null && room.isAssignable()) {
                vacantRooms[index++] = room;
            }
        }

        return vacantRooms;
    }

    /**
     * Returns bookings for VIP guests who are currently occupying rooms.
     * Current RoomDao state is checked so old booking snapshots are not treated
     * as active stays after checkout or later room reuse.
     */
    public Booking[] getCurrentVipRoomBookings() {
        rooms = roomDao.loadOrSeed();
        bookings = loadExistingBookings();

        int currentVipCount = 0;
        for (Booking booking : bookings) {
            if (isCurrentVipRoomBooking(booking)) {
                currentVipCount++;
            }
        }

        Booking[] currentVipBookings = new Booking[currentVipCount];
        int index = 0;

        for (Booking booking : bookings) {
            if (isCurrentVipRoomBooking(booking)) {
                currentVipBookings[index++] = booking;
            }
        }

        return currentVipBookings;
    }

    private boolean isCurrentVipRoomBooking(Booking booking) {
        if (booking == null
                || booking.getGuest() == null
                || booking.getGuest().getGuestId() == null
                || booking.getRoom() == null
                || booking.getRoom().getRoomNumber() == null) {

            return false;
        }

        LoyaltyProfile profile
                = searchLoyaltyProfileByGuestId(
                        booking.getGuest().getGuestId());

        if (profile == null || profile.getTier() == null) {
            return false;
        }

        Room currentRoom
                = findRoomByNumber(
                        booking.getRoom().getRoomNumber());

        if (currentRoom == null
                || currentRoom.getRoomStatus()
                != RoomStatus.OCCUPIED) {

            return false;
        }

        /*
        * Payment date/time represents the actual check-in time of
        * this specific booking.
        */
        LocalDateTime bookingCheckIn
                = booking.getPayment() == null
                ? null
                : booking.getPayment().getDateTime();

        LocalDateTime currentCheckIn
                = currentRoom.getCheckInDateTime();

        return bookingCheckIn != null
                && currentCheckIn != null
                && bookingCheckIn.equals(currentCheckIn);
    }

    public Room getCurrentRoomForBooking(Booking booking) {
        if (booking == null || booking.getRoom() == null || booking.getRoom().getRoomNumber() == null) {
            return null;
        }

        rooms = roomDao.loadOrSeed();
        return findRoomByNumber(booking.getRoom().getRoomNumber());
    }

    private Room findRoomByNumber(String roomNumber) {
        if (roomNumber == null) {
            return null;
        }

        for (Room room : rooms) {
            if (room != null && room.getRoomNumber() != null && room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return room;
            }
        }

        return null;
    }

    public WalkInRegistration cancelVipRegistrationById(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            return null;
        }

        /*
         * Find through a structural copy so the original priority order is not
         * disturbed, then use the MaxHeap's direct remove operation. This avoids
         * draining and rebuilding the whole heap for one cancellation.
         */
        WalkInRegistration removedRegistration = findWaitingVipRegistrationById(registrationId);

        if (removedRegistration == null || !PRIORITY_QUEUE.remove(removedRegistration)) {
            return null;
        }

        removedRegistration.setStatus(RegistrationStatus.CANCELLED);
        registrationDao.upsert(removedRegistration);
        return removedRegistration;
    }

    /**
     * Rebuilds the in-memory VIP MaxHeap from the persisted registration data.
     * VIP waiting state is stored only in walkin_registration.dat and the
     * MaxHeap is rebuilt from those VIP_WAITING records when the module starts.
     */
    private void loadWaitingVipRegistrationsOnce() {
        if (waitingVipRegistrationsLoaded) {
            return;
        }

        synchronized (VipPriorityController.class) {
            if (waitingVipRegistrationsLoaded) {
                return;
            }

            WalkInRegistration[] savedRegistrations = registrationDao.loadExisting();

            for (WalkInRegistration registration : savedRegistrations) {
                if (registration != null && registration.getStatus() == RegistrationStatus.VIP_WAITING && getLoyaltyTier(registration) != null) {
                    PRIORITY_QUEUE.enqueue(registration);
                }
            }

            waitingVipRegistrationsLoaded = true;
        }
    }

    private boolean registrationAlreadyQueued(String registrationId) {
        PriorityQueueADT<WalkInRegistration> copiedQueue = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            WalkInRegistration registration = copiedQueue.dequeue();

            if (registration.getRegistrationId().equalsIgnoreCase(registrationId.trim())) {
                return true;
            }
        }

        return false;
    }

    private boolean guestAlreadyQueued(String guestId) {
        PriorityQueueADT<WalkInRegistration> copiedQueue = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            WalkInRegistration registration = copiedQueue.dequeue();

            if (registration.getGuest().getGuestId().equalsIgnoreCase(guestId.trim())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the current stored loyalty tier for a VIP waiting registration.
     */
    public LoyaltyTier getLoyaltyTier(WalkInRegistration registration) {
        if (registration == null || registration.getGuest() == null) {
            return null;
        }

        LoyaltyProfile profile = searchLoyaltyProfileByGuestId(registration.getGuest().getGuestId());
        return profile == null ? null : profile.getTier();
    }

    public int getPriorityScore(WalkInRegistration registration) {
        LoyaltyTier tier = getLoyaltyTier(registration);
        return tier == null ? 0 : tier.getPriority();
    }

    /**
     * VIP MaxHeap comparison rule:
     * DIAMOND > PLATINUM > ELITE, then earlier registration time, then the
     * smaller registration ID as a deterministic final tie-breaker.
     */
    private static int compareVipPriority(WalkInRegistration first, WalkInRegistration second) {
        int firstPriority = getStoredPriority(first);
        int secondPriority = getStoredPriority(second);

        int tierComparison = Integer.compare(firstPriority, secondPriority);
        if (tierComparison != 0) {
            return tierComparison;
        }

        if (first.getRegistrationTime() != null && second.getRegistrationTime() != null) {
            int timeComparison = second.getRegistrationTime().compareTo(first.getRegistrationTime());

            if (timeComparison != 0) {
                return timeComparison;
            }
        }

        String firstId = first.getRegistrationId() == null ? "" : first.getRegistrationId();
        String secondId = second.getRegistrationId() == null ? "" : second.getRegistrationId();
        return secondId.compareToIgnoreCase(firstId);
    }

    private static int getStoredPriority(WalkInRegistration registration) {
        if (registration == null || registration.getGuest() == null || registration.getGuest().getGuestId() == null) {
            return 0;
        }

        String guestId = registration.getGuest().getGuestId();

        for (LoyaltyProfile profile : priorityProfileCache) {
            if (profile != null && profile.getGuestId() != null && profile.getGuestId().equalsIgnoreCase(guestId)) {
                LoyaltyTier tier = profile.getTier();
                return tier == null ? 0 : tier.getPriority();
            }
        }

        return 0;
    }

    private static synchronized void refreshPriorityProfileCache() {
        LoyaltyProfile[] profiles = PRIORITY_LOYALTY_DAO.loadOrSeed();
        priorityProfileCache = profiles == null ? new LoyaltyProfile[0] : profiles;
    }

    private Room findSuitableVacantRoom(WalkInRegistration registration) {
        for (Room room : rooms) {
            if (isRoomSuitableForRegistration(room, registration)) {
                return room;
            }
        }

        return null;
    }

    private boolean isRoomSuitableForRegistration(Room room, WalkInRegistration registration) {
        if (room == null || registration == null || !room.isAssignable()) {
            return false;
        }

        boolean matchingRoomType = room.getRoomType().equalsIgnoreCase(registration.getRequestedRoomType());
        boolean enoughCapacity = room.getNoOfGuest() >= registration.getNumberOfGuests();

        return matchingRoomType && enoughCapacity;
    }

    private void updateAllocatedRoom(Room room, WalkInRegistration registration, LocalDateTime actualCheckInTime) {
        room.setRoomStatus(RoomStatus.OCCUPIED);
        room.setBookingDate(actualCheckInTime);
        room.setCheckInDateTime(actualCheckInTime);
        room.setCheckOutDateTime(registration.getCheckOutDateTime());
    }

    private Booking[] loadExistingBookings() {
        File bookingFile = new File("booking.dat");

        if (!bookingFile.exists()) {
            return new Booking[0];
        }

        Booking[] loadedBookings = bookingDao.retrieveFromFile();
        return loadedBookings == null ? new Booking[0] : loadedBookings;
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
            if (booking != null && booking.getConfirmationNo() != null && booking.getConfirmationNo().equals(confirmationNo)) {
                return true;
            }
        }

        return false;
    }

    private Booking[] appendBooking(Booking[] original, Booking newBooking) {
        Booking[] safeOriginal = original == null ? new Booking[0] : original;
        Booking[] updated = new Booking[safeOriginal.length + 1];
        System.arraycopy(safeOriginal, 0, updated, 0, safeOriginal.length);
        updated[safeOriginal.length] = newBooking;

        return updated;
    }

    // ===== Boundary display/input helpers (ECB: Boundary does not access Entity objects) =====
    public String[] getVipTierNames() {
        return new String[] {
            LoyaltyTier.DIAMOND.name(),
            LoyaltyTier.PLATINUM.name(),
            LoyaltyTier.ELITE.name()
        };
    }

    public String[] getRoomTypeNames() {
        RoomType[] roomTypes = RoomType.values();
        String[] names = new String[roomTypes.length];
        for (int i = 0; i < roomTypes.length; i++) {
            names[i] = roomTypes[i].name();
        }
        return names;
    }

    public String[] getNextVipPaymentPreviewDisplayData() {
        String registrationId = getNextAllocatableVipRegistrationId();

        if (registrationId == null) {
            return null;
        }

        WalkInRegistration registration = findWaitingVipRegistrationById(registrationId);

        if (registration == null) {
            return null;
        }

        rooms = roomDao.loadOrSeed();
        Room suitableRoom = findSuitableVacantRoom(registration);

        if (suitableRoom == null) {
            return null;
        }

        LocalDateTime previewCheckInTime = LocalDateTime.now().withSecond(0).withNano(0);
        long numberOfNights = calculateStayNights(previewCheckInTime, registration.getCheckOutDateTime());
        double rate = getRoomTypePricePerDay(suitableRoom.getRoomType());
        double totalAmount = rate * numberOfNights;

        return new String[]{
            String.format("%.2f", rate),
            String.valueOf(numberOfNights),
            String.format("%.2f", totalAmount)
        };
    }

    private long calculateStayNights(LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        if (checkInTime == null || checkOutTime == null) {
            return 1;
        }

        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkInTime.toLocalDate(), checkOutTime.toLocalDate());
        return Math.max(nights, 1);
    }

    private String generateNextPaymentId() {
        int highestNumber = 0;
        Payment[] savedPayments = new PaymentDao().loadOrSeed();

        if (savedPayments != null) {
            for (Payment payment : savedPayments) {
                if (payment == null) {
                    continue;
                }

                int number = getSequentialPaymentNumber(payment.getPaymentId());

                if (number > highestNumber) {
                    highestNumber = number;
                }
            }
        }

        if (bookings != null) {
            for (Booking booking : bookings) {
                if (booking == null || booking.getPayment() == null) {
                    continue;
                }

                int number = getSequentialPaymentNumber(booking.getPayment().getPaymentId());

                if (number > highestNumber) {
                    highestNumber = number;
                }
            }
        }

        return String.format("PAY%03d", highestNumber + 1);
    }

    private int getSequentialPaymentNumber(String paymentId) {
        if (paymentId == null || !paymentId.matches("(?i)PAY\\d{3}")) {
            return -1;
        }

        try {
            return Integer.parseInt(paymentId.substring(3));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public double getRoomTypePricePerDay(String roomTypeName) {
        if (roomTypeName == null) {
            return 0.0;
        }
        try {
            return RoomType.valueOf(roomTypeName.trim().toUpperCase()).getPricePerDay();
        } catch (IllegalArgumentException ex) {
            return 0.0;
        }
    }

    public int getVipProfileCount() {
        return getAllVipProfiles().length;
    }

    public int getVacantRoomCount() {
        return getVacantRooms().length;
    }

    public String[][] getAllVipProfileDisplayData() {
        LoyaltyProfile[] profiles = getAllVipProfiles();
        String[][] temp = new String[profiles.length][6];
        int count = 0;

        for (LoyaltyProfile profile : profiles) {
            Guest guest = findGuestById(profile.getGuestId());
            if (guest == null) {
                continue;
            }
            temp[count][0] = guest.getGuestId();
            temp[count][1] = guest.getName();
            temp[count][2] = String.valueOf(guest.getPhoneNo());
            temp[count][3] = String.valueOf(profile.getTier());
            temp[count][4] = String.valueOf(profile.getCompletedStays());
            temp[count][5] = getVipActivityStatusForBoundary(guest.getGuestId());
            count++;
        }

        String[][] rows = new String[count][6];
        System.arraycopy(temp, 0, rows, 0, count);
        return rows;
    }

    public String getNextVipRegistrationId() {
        WalkInRegistration registration = peekNextVip();
        return registration == null ? null : registration.getRegistrationId();
    }

    public String getNextAllocatableVipRegistrationId() {
        WalkInRegistration registration = peekNextAllocatableVip();
        return registration == null ? null : registration.getRegistrationId();
    }

    public boolean waitingVipRegistrationExists(String registrationId) {
        return findWaitingVipRegistrationById(registrationId) != null;
    }

    public String[] getWaitingVipRegistrationDisplayData(String registrationId) {
        WalkInRegistration registration = findWaitingVipRegistrationById(registrationId);
        if (registration == null) {
            return null;
        }
        LoyaltyTier tier = getLoyaltyTier(registration);
        return new String[] {
            registration.getGuest().getGuestId(),
            registration.getGuest().getName(),
            String.valueOf(registration.getGuest().getPhoneNo()),
            String.valueOf(tier),
            registration.getRegistrationId(),
            String.valueOf(registration.getStatus()),
            formatBoundaryDateTime(registration.getRegistrationTime()),
            registration.getRequestedRoomType(),
            String.valueOf(registration.getNumberOfGuests()),
            formatBoundaryDateTime(registration.getCheckOutDateTime()),
            String.valueOf(countMatchingReadyRoomsForBoundary(registration)),
            formatWaitingTime(registration.getRegistrationTime())
        };
    }

    public String[][] getVipPriorityQueueDisplayData() {
        WalkInRegistration[] registrations = getVipRegistrationsByPriority();

        String[][] rows = new String[registrations.length][9];

        for (int i = 0; i < registrations.length; i++) {

            WalkInRegistration registration = registrations[i];

            rows[i][0] = registration.getRegistrationId();
            rows[i][1] = registration.getGuest().getGuestId();
            rows[i][2] = registration.getGuest().getName();
            rows[i][3] = String.valueOf(getLoyaltyTier(registration));
            rows[i][4] = registration.getRequestedRoomType();
            rows[i][5] = String.valueOf(registration.getNumberOfGuests());
            rows[i][6] = formatBoundaryDateTime(registration.getRegistrationTime());
            rows[i][7] = formatWaitingTime(registration.getRegistrationTime());
            rows[i][8] = String.valueOf(countMatchingReadyRoomsForBoundary(registration));
        }

        return rows;
    }

    public int getMatchingReadyRoomCount(String registrationId) {
        WalkInRegistration registration = findWaitingVipRegistrationById(registrationId);
        return countMatchingReadyRoomsForBoundary(registration);
    }

    public String[] getSuggestedReadyRoomDisplayData(String registrationId) {
        WalkInRegistration registration = findWaitingVipRegistrationById(registrationId);
        if (registration == null) {
            return null;
        }
        Room room = findReadyRoomForRegistration(registration);
        if (room == null) {
            return null;
        }
        return new String[] {
            room.getRoomNumber(),
            room.getRoomType(),
            String.valueOf(room.getNoOfGuest()),
            String.valueOf(room.getFloor()),
            room.getStatusLabel()
        };
    }

    public String getLoyaltyTierNameByRegistrationId(String registrationId) {
        WalkInRegistration registration = findWaitingVipRegistrationById(registrationId);
        if (registration == null) {
            // Historical/cancelled records are not in the heap; search saved records.
            WalkInRegistration[] records = registrationDao.loadExisting();
            for (WalkInRegistration record : records) {
                if (record != null && record.getRegistrationId() != null && record.getRegistrationId().equalsIgnoreCase(registrationId)) {
                    registration = record;
                    break;
                }
            }
        }
        LoyaltyTier tier = getLoyaltyTier(registration);
        return tier == null ? null : tier.name();
    }

    public String getWaitingVipRequestedRoomType(String registrationId) {
        WalkInRegistration registration = findWaitingVipRegistrationById(registrationId);
        return registration == null ? null : registration.getRequestedRoomType();
    }

    public int getWaitingVipGuestCount(String registrationId) {
        WalkInRegistration registration = findWaitingVipRegistrationById(registrationId);
        return registration == null ? 0 : registration.getNumberOfGuests();
    }

    public LocalDateTime getWaitingVipRegistrationTime(String registrationId) {
        WalkInRegistration registration = findWaitingVipRegistrationById(registrationId);
        return registration == null ? null : registration.getRegistrationTime();
    }

    public LocalDateTime getWaitingVipCheckOutDateTime(String registrationId) {
        WalkInRegistration registration = findWaitingVipRegistrationById(registrationId);
        return registration == null ? null : registration.getCheckOutDateTime();
    }

    public String[] cancelVipRegistrationDisplayData(String registrationId) {
        WalkInRegistration cancelled = cancelVipRegistrationById(registrationId);
        if (cancelled == null) {
            return null;
        }
        LoyaltyTier tier = getLoyaltyTier(cancelled);
        return new String[] {
            cancelled.getRegistrationId(),
            cancelled.getGuest().getGuestId(),
            cancelled.getGuest().getName(),
            String.valueOf(tier),
            String.valueOf(cancelled.getStatus()),
            String.valueOf(getWaitingCount())
        };
    }

    public String[][] getCurrentVipRoomDisplayData() {
        Booking[] currentBookings = getCurrentVipRoomBookings();
        String[][] rows = new String[currentBookings.length][8];
        for (int i = 0; i < currentBookings.length; i++) {
            Booking booking = currentBookings[i];
            Guest guest = booking.getGuest();
            Room room = getCurrentRoomForBooking(booking);
            LoyaltyProfile profile = searchLoyaltyProfileByGuestId(guest.getGuestId());
            rows[i][0] = booking.getConfirmationNo();
            rows[i][1] = guest.getGuestId();
            rows[i][2] = guest.getName();
            rows[i][3] = profile == null ? "-" : String.valueOf(profile.getTier());
            rows[i][4] = room == null ? "-" : room.getRoomNumber();
            rows[i][5] = room == null ? "-" : room.getRoomType();
            rows[i][6] = room == null ? "-" : formatBoundaryDateTimeDash(room.getCheckInDateTime());
            rows[i][7] = room == null ? "-" : formatBoundaryDateTimeDash(room.getCheckOutDateTime());
        }
        return rows;
    }

    public String[] allocateNextVipBookingDisplayData() {
        String registrationId = getNextAllocatableVipRegistrationId();
        if (registrationId == null) {
            return null;
        }
        WalkInRegistration registration = findWaitingVipRegistrationById(registrationId);
        if (registration == null) {
            return null;
        }
        String guestId = registration.getGuest().getGuestId();
        String guestName = registration.getGuest().getName();
        String tier = String.valueOf(getLoyaltyTier(registration));

        Booking booking = allocateNextVipBooking();
        if (booking == null || booking.getRoom() == null) {
            return null;
        }
        Room room = booking.getRoom();
        return new String[] {
            booking.getConfirmationNo(),
            registrationId,
            guestId,
            guestName,
            tier,
            room.getRoomNumber(),
            room.getRoomType(),
            formatBoundaryDateTime(room.getCheckInDateTime()),
            formatBoundaryDateTime(room.getCheckOutDateTime()),
            String.valueOf(getWaitingCount()),
            booking.getPayment() == null ? "N/A" : booking.getPayment().getPaymentId(),
            booking.getPayment() == null ? "0.00" : String.format("%.2f", booking.getPayment().getAmount()),
            booking.getPayment() == null ? "N/A" : String.valueOf(booking.getPayment().getStatus())
        };
    }

    public void generateRoomAllocationWaitingTimeReport(String keyword, String tierName, String roomTypeFilter, String statusFilter, LocalDate startDate, LocalDate endDate, int minimumGuests, int sortOption) {
        LoyaltyTier tier = parseBoundaryTier(tierName);
        new control.report.VipRoomAllocationWaitingTimeRP().generateReport(this, keyword, tier, roomTypeFilter, statusFilter, startDate, endDate, minimumGuests, sortOption);
    }

    public void generateLoyaltyEngagementReport(String keyword, String tierName, String activityFilter, int minimumCompletedStays, String roomTypeFilter, LocalDate startDate, LocalDate endDate, int sortOption) {
        LoyaltyTier tier = parseBoundaryTier(tierName);
        new control.report.VipLoyaltyEngagementRP().generateReport(this, keyword, tier, activityFilter, minimumCompletedStays, roomTypeFilter, startDate, endDate, sortOption);
    }

    private LoyaltyTier parseBoundaryTier(String tierName) {
        if (tierName == null || tierName.isBlank()) {
            return null;
        }
        try {
            return LoyaltyTier.valueOf(tierName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private int countMatchingReadyRoomsForBoundary(WalkInRegistration registration) {
        if (registration == null) {
            return 0;
        }
        int count = 0;
        Room[] readyRooms = getVacantRooms();
        for (Room room : readyRooms) {
            if (room == null || !room.isAssignable()) {
                continue;
            }
            boolean sameType = room.getRoomType() != null && registration.getRequestedRoomType() != null && room.getRoomType().equalsIgnoreCase(registration.getRequestedRoomType());
            boolean enoughCapacity = room.getNoOfGuest() >= registration.getNumberOfGuests();
            if (sameType && enoughCapacity) {
                count++;
            }
        }
        return count;
    }

    private String getVipActivityStatusForBoundary(String guestId) {
        WalkInRegistration[] waiting = getVipRegistrationsByPriority();
        for (WalkInRegistration registration : waiting) {
            if (registration != null && registration.getGuest() != null && registration.getGuest().getGuestId().equalsIgnoreCase(guestId)) {
                return "WAITING";
            }
        }

        Booking[] currentBookings = getCurrentVipRoomBookings();
        for (Booking booking : currentBookings) {
            if (booking != null && booking.getGuest() != null && booking.getGuest().getGuestId().equalsIgnoreCase(guestId)) {
                return "IN HOUSE";
            }
        }
        return "PROFILE ONLY";
    }

    private String formatWaitingTime(LocalDateTime requestTime) {

        if (requestTime == null) {
            return "-";
        }

        long totalMinutes = Math.max(
                0,
                Duration.between(
                        requestTime,
                        LocalDateTime.now())
                        .toMinutes()
        );

        if (totalMinutes < 60) {
            return totalMinutes
                    + (totalMinutes == 1
                    ? " minute"
                    : " minutes");
        }

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        String result = hours
                + (hours == 1
                ? " hour"
                : " hours");

        if (minutes > 0) {
            result += " "
                    + minutes
                    + (minutes == 1
                    ? " minute"
                    : " minutes");
        }

        return result;
    }

    private String formatBoundaryDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String formatBoundaryDateTimeDash(LocalDateTime value) {
        return value == null ? "-" : value.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}