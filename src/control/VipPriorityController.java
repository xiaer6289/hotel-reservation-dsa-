package control;

import adt.heap.MaxHeap;
import adt.heap.PriorityQueueADT;
import dao.BookingDao;
import dao.MemberDao;
import dao.RoomDao;
import dao.WalkInRegistrationDao;
import entity.Booking;
import entity.LoyaltyTier;
import entity.Member;
import entity.RegistrationStatus;
import entity.Room;
import entity.RoomStatus;
import entity.RoomType;
import entity.WalkInRegistration;
import java.io.File;
import java.time.LocalDateTime;
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
    private static final PriorityQueueADT<Member> PRIORITY_QUEUE = new MaxHeap<>();
    private static boolean waitingMembersLoaded = false;
    private final MemberDao memberDao;
    private final WalkInRegistrationDao registrationDao;
    private final RoomDao roomDao;
    private final BookingDao bookingDao;
    private Room[] rooms;
    private Booking[] bookings;

    public VipPriorityController() {
        memberDao = new MemberDao();
        registrationDao = new WalkInRegistrationDao();
        roomDao = new RoomDao();
        bookingDao = new BookingDao();
        rooms = roomDao.loadOrSeed();
        bookings = loadExistingBookings();
        loadWaitingMembersOnce();
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

        Member member = new Member(registration, tier);

        registration.setStatus(RegistrationStatus.VIP_WAITING);
        PRIORITY_QUEUE.enqueue(member);

        registrationDao.upsert(registration);
        saveWaitingMembers();

        return ADD_SUCCESS;
    }

    public Member peekNextVip() {
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

        PriorityQueueADT<Member> copiedQueue = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            Member member = copiedQueue.dequeue();

            if (isRoomSuitableForRegistration(room, member.getRegistration())) {
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
    public Member peekNextAllocatableVip() {
        rooms = roomDao.loadOrSeed();
        PriorityQueueADT<Member> copiedQueue = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            Member member = copiedQueue.dequeue();

            if (findSuitableVacantRoom(member.getRegistration()) != null) {
                return member;
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

        PriorityQueueADT<Member> retainedMembers = new MaxHeap<>();
        Member selectedMember = null;
        Room suitableRoom = null;

        /*
         * Dequeue in MaxHeap priority order until the highest-priority VIP
         * who can use a currently vacant room is found. Higher-priority VIPs
         * without a suitable room are temporarily retained and reinserted.
         */
        while (!PRIORITY_QUEUE.isEmpty()) {
            Member candidate = PRIORITY_QUEUE.dequeue();
            Room candidateRoom = findSuitableVacantRoom(candidate.getRegistration());

            if (candidateRoom != null) {
                selectedMember = candidate;
                suitableRoom = candidateRoom;
                break;
            }

            retainedMembers.enqueue(candidate);
        }

        while (!retainedMembers.isEmpty()) {
            PRIORITY_QUEUE.enqueue(retainedMembers.dequeue());
        }

        if (selectedMember == null) {
            saveWaitingMembers();
            return null;
        }

        WalkInRegistration registration = selectedMember.getRegistration();

        LocalDateTime actualCheckInTime = LocalDateTime.now().withSecond(0).withNano(0);

        updateAllocatedRoom(suitableRoom, registration, actualCheckInTime);

        registration.setCheckInDateTime(actualCheckInTime);
        registration.setStatus(RegistrationStatus.CHECKED_IN);

        Booking booking = new Booking(generateUniqueConfirmationNo(), registration.getGuest(), suitableRoom, null);

        bookings = appendBooking(bookings, booking);

        roomDao.saveToFile(rooms);
        bookingDao.saveToFile(bookings);
        registrationDao.upsert(registration);

        /* selectedMember has already been removed from the heap above. */
        saveWaitingMembers();

        return booking;
    }

    /**
     * Compatibility method retained for existing code that only needs Room.
     */
    public Room allocateNextVipRoom() {
        Booking booking = allocateNextVipBooking();
        return booking == null ? null : booking.getRoom();
    }

    public Member[] getMembersByPriority() {
        PriorityQueueADT<Member> copiedQueue = PRIORITY_QUEUE.copy();
        Member[] members = new Member[copiedQueue.size()];

        for (int i = 0; i < members.length; i++) {
            members[i] = copiedQueue.dequeue();
        }

        return members;
    }

    /**
     * Finds one waiting VIP by registration ID without removing the member
     * from the MaxHeap. The copied heap contains the same member references,
     * so this is a read-only priority search.
     */
    public Member findWaitingMemberByRegistrationId(String registrationId) {
        if (!Utility.isValidRegistrationId(registrationId)) {
            return null;
        }

        PriorityQueueADT<Member> copiedQueue = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            Member member = copiedQueue.dequeue();

            if (member != null && member.getRegistration() != null && member.getRegistration().getStatus() == RegistrationStatus.VIP_WAITING && member.getRegistration().getRegistrationId().equalsIgnoreCase(registrationId.trim())) {
                return member;
            }
        }

        return null;
    }

    /**
     * Updates editable parts of a waiting VIP room request. Loyalty tier,
     * priority and registration time are intentionally not editable.
     * These fields do not affect heap priority, so the member keeps the same
     * MaxHeap ordering after the request update.
     */
    public boolean updateVipRegistrationRequest(String registrationId, String requestedRoomType, int numberOfGuests, LocalDateTime checkOutDateTime) {
        Member member = findWaitingMemberByRegistrationId(registrationId);

        if (member == null || requestedRoomType == null || requestedRoomType.isBlank() || numberOfGuests <= 0 || checkOutDateTime == null) {
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

        WalkInRegistration registration = member.getRegistration();
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        if (!checkOutDateTime.isAfter(now)) {
            return false;
        }

        if (registration.getRegistrationTime() != null && !checkOutDateTime.isAfter(registration.getRegistrationTime())) {
            return false;
        }

        registration.setRequestedRoomType(normalizedRoomType);
        registration.setNumberOfGuests(numberOfGuests);
        registration.setCheckOutDateTime(checkOutDateTime);

        registrationDao.upsert(registration);
        saveWaitingMembers();
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
    public Room findReadyRoomForMember(Member member) {
        if (member == null || member.getRegistration() == null) {
            return null;
        }

        rooms = roomDao.loadOrSeed();
        return findSuitableVacantRoom(member.getRegistration());
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

    public WalkInRegistration cancelVipRegistrationById(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            return null;
        }

        PriorityQueueADT<Member> retainedMembers = new MaxHeap<>();
        WalkInRegistration removedRegistration = null;

        while (!PRIORITY_QUEUE.isEmpty()) {
            Member member = PRIORITY_QUEUE.dequeue();

            if (removedRegistration == null && member.getRegistration().getRegistrationId().equalsIgnoreCase(registrationId.trim())) {
                removedRegistration = member.getRegistration();
            } else {
                retainedMembers.enqueue(member);
            }
        }

        while (!retainedMembers.isEmpty()) {
            PRIORITY_QUEUE.enqueue(retainedMembers.dequeue());
        }

        if (removedRegistration != null) {
            removedRegistration.setStatus(RegistrationStatus.CANCELLED);
            registrationDao.upsert(removedRegistration);
            saveWaitingMembers();
        }

        return removedRegistration;
    }

    private void loadWaitingMembersOnce() {
        if (waitingMembersLoaded) {
            return;
        }

        synchronized (VipPriorityController.class) {
            if (waitingMembersLoaded) {
                return;
            }

            Member[] savedMembers = memberDao.retrieveFromFile();

            for (Member member : savedMembers) {
                if (member != null && member.getRegistration() != null && member.getRegistration().getStatus() == RegistrationStatus.VIP_WAITING) {
                    PRIORITY_QUEUE.enqueue(member);
                }
            }

            waitingMembersLoaded = true;
        }
    }

    private void saveWaitingMembers() {
        Member[] waitingMembers = getMembersByPriority();
        memberDao.saveToFile(waitingMembers);
    }

    private boolean registrationAlreadyQueued(String registrationId) {
        PriorityQueueADT<Member> copiedQueue = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            Member member = copiedQueue.dequeue();

            if (member.getRegistration().getRegistrationId().equalsIgnoreCase(registrationId.trim())) {
                return true;
            }
        }

        return false;
    }

    private boolean guestAlreadyQueued(String guestId) {
        PriorityQueueADT<Member> copiedQueue = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            Member member = copiedQueue.dequeue();

            if (member.getGuest().getGuestId().equalsIgnoreCase(guestId.trim())) {
                return true;
            }
        }

        return false;
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