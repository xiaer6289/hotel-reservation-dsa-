package control;

import adt.heap.MaxHeap;
import adt.heap.PriorityQueueADT;
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
import entity.RoomType;
import entity.WalkInRegistration;
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
    private static LoyaltyProfile[] priorityProfileCache
            = PRIORITY_LOYALTY_DAO.loadOrSeed();
    private static final PriorityQueueADT<WalkInRegistration> PRIORITY_QUEUE
            = new MaxHeap<>(VipPriorityController::compareVipPriority);
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
            if (profile != null
                    && profile.getGuestId() != null
                    && profile.getGuestId().equalsIgnoreCase(guestId.trim())) {
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

            if (before != existingProfile.getCompletedStays()
                    || beforeTier != existingProfile.getTier()) {
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
        LoyaltyProfile existingProfile
                = findLoyaltyProfile(profiles, normalizedGuestId);

        if (existingProfile != null) {
            existingProfile.updateCompletedStays(
                    existingProfile.getCompletedStays() + 1);
            loyaltyProfileDao.saveToFile(profiles);
            refreshPriorityProfileCache();
            return existingProfile;
        }

        int completedStays = countCheckedOutRegistrations(normalizedGuestId);
        LoyaltyTier qualifiedTier = LoyaltyProfile.determineTier(completedStays);

        if (qualifiedTier == null) {
            return null;
        }

        LoyaltyProfile newProfile
                = new LoyaltyProfile(normalizedGuestId, completedStays);
        LoyaltyProfile[] updatedProfiles
                = new LoyaltyProfile[profiles.length + 1];
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
            if (registration == null
                    || registration.getGuest() == null
                    || registration.getGuest().getGuestId() == null) {
                continue;
            }

            boolean sameGuest = registration.getGuest().getGuestId()
                    .equalsIgnoreCase(guestId);
            boolean stayCompleted = registration.getStatus()
                    == RegistrationStatus.CHECKED_OUT;

            if (sameGuest && stayCompleted) {
                completedCount++;
            }
        }

        return completedCount;
    }

    private LoyaltyProfile findLoyaltyProfile(
            LoyaltyProfile[] profiles,
            String guestId) {

        if (profiles == null || guestId == null) {
            return null;
        }

        for (LoyaltyProfile profile : profiles) {
            if (profile != null
                    && profile.getGuestId() != null
                    && profile.getGuestId().equalsIgnoreCase(guestId.trim())) {
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
     * Finds the guest master record that belongs to a loyalty profile.
     */
    public Guest findGuestById(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            return null;
        }

        Guest[] guests = guestDao.loadOrSeed();

        for (Guest guest : guests) {
            if (guest != null && guest.getGuestId() != null
                    && guest.getGuestId().equalsIgnoreCase(guestId.trim())) {
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
    public int getReadyRoomCountForIncomingVip(
            String roomType,
            int numberOfGuests,
            LoyaltyTier incomingTier) {

        if (roomType == null || roomType.isBlank()
                || numberOfGuests <= 0 || incomingTier == null) {
            return 0;
        }

        Room[] currentRooms = roomDao.loadOrSeed();
        boolean[] reservedForHigherOrEarlierVip
                = new boolean[currentRooms.length];

        PriorityQueueADT<WalkInRegistration> copiedQueue
                = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            WalkInRegistration waitingRegistration = copiedQueue.dequeue();
            LoyaltyTier waitingTier = getLoyaltyTier(waitingRegistration);

            if (waitingTier == null
                    || waitingTier.getPriority() < incomingTier.getPriority()) {
                continue;
            }

            /*
             * Any already-waiting VIP of the same tier arrived before the new
             * registration, so that guest also remains ahead in the MaxHeap.
             * One waiting VIP protects only one suitable ready room.
             */
            for (int i = 0; i < currentRooms.length; i++) {
                if (!reservedForHigherOrEarlierVip[i]
                        && isRoomSuitableForRegistration(
                                currentRooms[i],
                                waitingRegistration)) {

                    reservedForHigherOrEarlierVip[i] = true;
                    break;
                }
            }
        }

        int availableCount = 0;

        for (int i = 0; i < currentRooms.length; i++) {
            Room room = currentRooms[i];

            if (!reservedForHigherOrEarlierVip[i]
                    && room != null
                    && room.isAssignable()
                    && room.getRoomType() != null
                    && room.getRoomType().equalsIgnoreCase(roomType.trim())
                    && room.getNoOfGuest() >= numberOfGuests) {

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

        PriorityQueueADT<WalkInRegistration> copiedQueue
                = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {

            WalkInRegistration registration
                    = copiedQueue.dequeue();

            for (int i = 0; i < currentRooms.length; i++) {

                if (!reserved[i]
                        && isRoomSuitableForRegistration(
                                currentRooms[i],
                                registration)) {

                    // One VIP registration only protects one room
                    reserved[i] = true;
                    break;
                }
            }
        }

        for (int i = 0; i < currentRooms.length; i++) {

            if (reserved[i]
                    && currentRooms[i].getRoomNumber()
                            .equalsIgnoreCase(
                                    targetRoom.getRoomNumber())) {

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

        Booking booking = new Booking(generateUniqueConfirmationNo(), registration.getGuest(), suitableRoom, null);

        bookings = appendBooking(bookings, booking);

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

            if (registration != null
                    && registration.getStatus() == RegistrationStatus.VIP_WAITING
                    && registration.getRegistrationId().equalsIgnoreCase(registrationId.trim())) {
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
    public boolean updateVipRegistrationRequest(
            String registrationId,
            String requestedRoomType,
            int numberOfGuests,
            LocalDateTime checkOutDateTime) {

        WalkInRegistration registration = findWaitingVipRegistrationById(registrationId);

        if (registration == null
                || requestedRoomType == null
                || requestedRoomType.isBlank()
                || numberOfGuests <= 0
                || checkOutDateTime == null) {
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

        LocalDate arrivalDate = registration.getRegistrationTime() == null
                ? LocalDate.now()
                : registration.getRegistrationTime().toLocalDate();
        LocalDate requestedCheckOutDate = checkOutDateTime.toLocalDate();

        if (!requestedCheckOutDate.isAfter(arrivalDate)
                || requestedCheckOutDate.isAfter(arrivalDate.plusDays(MAX_STAY_NIGHTS))) {
            return false;
        }

        // VIP request maintenance can change the departure date only. The hotel
        // standard check-out time remains fixed at 12:00 PM, matching RegistrationUI.
        LocalDateTime normalizedCheckOutDateTime
                = LocalDateTime.of(requestedCheckOutDate, STANDARD_CHECKOUT_TIME);

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

        LoyaltyProfile profile = searchLoyaltyProfileByGuestId(
                booking.getGuest().getGuestId());
        if (profile == null || profile.getTier() == null) {
            return false;
        }

        Room currentRoom = findRoomByNumber(booking.getRoom().getRoomNumber());
        if (currentRoom == null
                || currentRoom.getRoomStatus() != RoomStatus.OCCUPIED) {
            return false;
        }

        LocalDateTime bookingCheckIn = booking.getRoom().getCheckInDateTime();
        LocalDateTime currentCheckIn = currentRoom.getCheckInDateTime();

        return bookingCheckIn != null
                && currentCheckIn != null
                && bookingCheckIn.equals(currentCheckIn);
    }

    public Room getCurrentRoomForBooking(Booking booking) {
        if (booking == null
                || booking.getRoom() == null
                || booking.getRoom().getRoomNumber() == null) {
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
            if (room != null
                    && room.getRoomNumber() != null
                    && room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
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
        WalkInRegistration removedRegistration
                = findWaitingVipRegistrationById(registrationId);

        if (removedRegistration == null
                || !PRIORITY_QUEUE.remove(removedRegistration)) {
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
                if (registration != null
                        && registration.getStatus() == RegistrationStatus.VIP_WAITING
                        && getLoyaltyTier(registration) != null) {
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

        LoyaltyProfile profile = searchLoyaltyProfileByGuestId(
                registration.getGuest().getGuestId());
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
    private static int compareVipPriority(
            WalkInRegistration first,
            WalkInRegistration second) {

        int firstPriority = getStoredPriority(first);
        int secondPriority = getStoredPriority(second);

        int tierComparison = Integer.compare(firstPriority, secondPriority);
        if (tierComparison != 0) {
            return tierComparison;
        }

        if (first.getRegistrationTime() != null
                && second.getRegistrationTime() != null) {
            int timeComparison = second.getRegistrationTime()
                    .compareTo(first.getRegistrationTime());

            if (timeComparison != 0) {
                return timeComparison;
            }
        }

        String firstId = first.getRegistrationId() == null ? "" : first.getRegistrationId();
        String secondId = second.getRegistrationId() == null ? "" : second.getRegistrationId();
        return secondId.compareToIgnoreCase(firstId);
    }

    private static int getStoredPriority(WalkInRegistration registration) {
        if (registration == null || registration.getGuest() == null
                || registration.getGuest().getGuestId() == null) {
            return 0;
        }

        String guestId = registration.getGuest().getGuestId();

        for (LoyaltyProfile profile : priorityProfileCache) {
            if (profile != null
                    && profile.getGuestId() != null
                    && profile.getGuestId().equalsIgnoreCase(guestId)) {
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
}