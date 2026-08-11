package control;

import adt.heap.MaxHeap;
import adt.heap.PriorityQueueADT;
import dao.MemberDao;
import dao.RoomDao;
import dao.WalkInRegistrationDao;
import entity.LoyaltyTier;
import entity.Member;
import entity.RegistrationStatus;
import entity.Room;
import entity.WalkInRegistration;
import java.time.LocalDateTime;

/**
 * Controls VIP registration priority and room allocation.
 *
 * The MaxHeap is shared by all VipPriorityController objects. This allows the
 * existing Main.java to create RegistrationUI and VipAllocationUI separately
 * without losing the VIP registrations added by the Walk-In module.
 *
 * @author Low Enn Toong
 */
public class VipPriorityController {

    public static final int ADD_SUCCESS = 1;
    public static final int INVALID_INPUT = -1;
    public static final int DUPLICATE_MEMBER_ID = -2;
    public static final int REGISTRATION_ALREADY_QUEUED = -3;
    public static final int GUEST_ALREADY_QUEUED = -4;

    /*
     * Shared non-linear ADT.
     * Main.java creates separate UI/controller objects, so the heap must be
     * shared to keep both modules linked without changing Main.java.
     */
    private static final PriorityQueueADT<Member> PRIORITY_QUEUE
            = new MaxHeap<>();

    private static boolean waitingMembersLoaded = false;

    private final MemberDao memberDao;
    private final WalkInRegistrationDao registrationDao;
    private final RoomDao roomDao;

    private Room[] rooms;

    public VipPriorityController() {
        memberDao = new MemberDao();
        registrationDao = new WalkInRegistrationDao();
        roomDao = new RoomDao();

        rooms = roomDao.loadOrSeed();
        loadWaitingMembersOnce();
    }

    /**
     * Receives a completed WalkInRegistration from RegistrationController and
     * inserts it into the shared MaxHeap according to loyalty tier.
     */
    public int addVipRegistration(
            String memberId,
            WalkInRegistration registration,
            LoyaltyTier tier) {

        if (memberId == null
                || memberId.isBlank()
                || registration == null
                || registration.getGuest() == null
                || registration.getRegistrationId() == null
                || tier == null) {

            return INVALID_INPUT;
        }

        if (memberIdExists(memberId)) {
            return DUPLICATE_MEMBER_ID;
        }

        if (registrationAlreadyQueued(registration.getRegistrationId())) {
            return REGISTRATION_ALREADY_QUEUED;
        }

        if (guestAlreadyQueued(registration.getGuest().getGuestId())) {
            return GUEST_ALREADY_QUEUED;
        }

        Member member = new Member(
                memberId.trim(),
                registration,
                tier);

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
     * Allocates a suitable available room to the highest-priority VIP.
     *
     * Room.java is used exactly as provided by its author. No change to
     * Room.java, RoomStatus.java, RoomDao.java or PaymentDao.java is required.
     */
    public Room allocateNextVipRoom() {
        Member nextVip = PRIORITY_QUEUE.peek();

        if (nextVip == null) {
            return null;
        }

        WalkInRegistration registration = nextVip.getRegistration();
        Room suitableRoom = findSuitableVacantRoom(registration);

        if (suitableRoom == null) {
            /* Keep the VIP at the MaxHeap root when no room matches. */
            return null;
        }

        updateAllocatedRoom(suitableRoom, registration);
        registration.setStatus(RegistrationStatus.CHECKED_IN);

        roomDao.saveToFile(rooms);
        registrationDao.upsert(registration);

        /* Remove only after the room and registration have been updated. */
        PRIORITY_QUEUE.dequeue();
        saveWaitingMembers();

        return suitableRoom;
    }

    public Member[] getMembersByPriority() {
        PriorityQueueADT<Member> copiedQueue = PRIORITY_QUEUE.copy();
        Member[] members = new Member[copiedQueue.size()];

        for (int i = 0; i < members.length; i++) {
            members[i] = copiedQueue.dequeue();
        }

        return members;
    }

    public Room[] getVacantRooms() {
        int vacantCount = 0;

        for (Room room : rooms) {
            if (room != null && room.isAvailability()) {
                vacantCount++;
            }
        }

        Room[] vacantRooms = new Room[vacantCount];
        int index = 0;

        for (Room room : rooms) {
            if (room != null && room.isAvailability()) {
                vacantRooms[index++] = room;
            }
        }

        return vacantRooms;
    }

    /**
     * Removes a VIP registration when it is cancelled from RegistrationUI.
     */
    public WalkInRegistration cancelVipRegistrationById(
            String registrationId) {

        if (registrationId == null || registrationId.isBlank()) {
            return null;
        }

        PriorityQueueADT<Member> retainedMembers = new MaxHeap<>();
        WalkInRegistration removedRegistration = null;

        while (!PRIORITY_QUEUE.isEmpty()) {
            Member member = PRIORITY_QUEUE.dequeue();

            if (removedRegistration == null
                    && member.getRegistration().getRegistrationId()
                            .equalsIgnoreCase(registrationId.trim())) {

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
                if (member != null
                        && member.getRegistration() != null
                        && member.getRegistration().getStatus()
                                == RegistrationStatus.VIP_WAITING) {

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

    private boolean memberIdExists(String memberId) {
        PriorityQueueADT<Member> copiedQueue = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            Member member = copiedQueue.dequeue();

            if (member.getMemberId().equalsIgnoreCase(memberId.trim())) {
                return true;
            }
        }

        return false;
    }

    private boolean registrationAlreadyQueued(String registrationId) {
        PriorityQueueADT<Member> copiedQueue = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            Member member = copiedQueue.dequeue();

            if (member.getRegistration().getRegistrationId()
                    .equalsIgnoreCase(registrationId.trim())) {

                return true;
            }
        }

        return false;
    }

    private boolean guestAlreadyQueued(String guestId) {
        PriorityQueueADT<Member> copiedQueue = PRIORITY_QUEUE.copy();

        while (!copiedQueue.isEmpty()) {
            Member member = copiedQueue.dequeue();

            if (member.getGuest().getGuestId()
                    .equalsIgnoreCase(guestId.trim())) {

                return true;
            }
        }

        return false;
    }

    private Room findSuitableVacantRoom(
            WalkInRegistration registration) {

        for (Room room : rooms) {
            if (room == null || !room.isAvailability()) {
                continue;
            }

            boolean matchingRoomType = room.getRoomType()
                    .equalsIgnoreCase(
                            registration.getRequestedRoomType());

            boolean enoughCapacity = room.getNoOfGuest()
                    >= registration.getNumberOfGuests();

            if (matchingRoomType && enoughCapacity) {
                return room;
            }
        }

        return null;
    }

    private void updateAllocatedRoom(
            Room room,
            WalkInRegistration registration) {

        /* Use only the existing methods provided by Room.java. */
        room.setAvailability(false);
        room.setStatus('O');
        room.setBookingDate(LocalDateTime.now());
        room.setCheckInDateTime(registration.getCheckInDateTime());
        room.setCheckOutDateTime(registration.getCheckOutDateTime());
    }
}