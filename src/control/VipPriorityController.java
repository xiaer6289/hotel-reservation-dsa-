package control;

import adt.heap.MaxHeap;
import adt.heap.PriorityQueueADT;
import dao.GuestDao;
import dao.RoomDao;
import entity.Guest;
import entity.LoyaltyTier;
import entity.Member;
import entity.Room;
import java.time.LocalDateTime;

/**
 *
 * @author Low Enn Toong
 */
public class VipPriorityController {
    public static final int ADD_SUCCESS = 1;
    public static final int INVALID_INPUT = -1;
    public static final int GUEST_NOT_FOUND = -2;
    public static final int DUPLICATE_MEMBER_ID = -3;
    public static final int GUEST_ALREADY_QUEUED = -4;
    private final PriorityQueueADT<Member> priorityQueue;
    // Reuse existing Guest entities.
    private final Guest[] guests;
    // Reuse existing Room entities.
    private final Room[] rooms;
    private final RoomDao roomDao;

    public VipPriorityController() {
        priorityQueue = new MaxHeap<>();
        GuestDao guestDao = new GuestDao();
        roomDao = new RoomDao();
        guests = guestDao.loadOrSeed();
        rooms = roomDao.loadOrSeed();
    }

    public int addVipMember(String memberId, String guestId, LoyaltyTier tier) {
        if (memberId == null || memberId.isBlank() || guestId == null || guestId.isBlank() || tier == null) {
            return INVALID_INPUT;
        }
        Guest guest = findGuestById(guestId);
        if (guest == null) {
            return GUEST_NOT_FOUND;
        }
        if (memberIdExists(memberId)) {
            return DUPLICATE_MEMBER_ID;
        }
        if (guestAlreadyQueued(guestId)) {
            return GUEST_ALREADY_QUEUED;
        }
        Member member = new Member(memberId.trim(), guest, tier);
        /*
         * enqueue() automatically uses reheapUp().
         * The highest loyalty tier moves towards the root.
         */
        priorityQueue.enqueue(member);
        return ADD_SUCCESS;
    }

    public Member peekNextVip() {
        // Returns the highest-tier member.
        return priorityQueue.peek();
    }

    public Room allocateNextVipRoom() {
        /*
         * Peek first.
         * Do not remove the member before confirming
         * that a vacant room exists.
         */
        Member nextVip = priorityQueue.peek();
        if (nextVip == null) {
            return null;
        }
        Room vacantRoom = findVacantRoom();
        if (vacantRoom == null) {
            /*
             * The member remains at the front
             * because no room is available.
             */
            return null;
        }
        /*
         * Remove the highest-tier member only after
         * a vacant room has been found.
         */
        priorityQueue.dequeue();
        vacantRoom.setAvailability(false);
        vacantRoom.setStatus('O');
        vacantRoom.setCheckInDateTime(LocalDateTime.now());
        roomDao.saveToFile(rooms);
        return vacantRoom;
    }

    public Member[] getMembersByPriority() {
        PriorityQueueADT<Member> copiedQueue = priorityQueue.copy();
        Member[] members = new Member[copiedQueue.size()];
        /*
         * Dequeue from the copy.
         * Result: highest tier to lowest tier.
         */
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
                vacantRooms[index] = room;
                index++;
            }
        }
        return vacantRooms;
    }

    private Guest findGuestById(String guestId) {
        for (Guest guest : guests) {
            if (guest != null && guest.getGuestId().equalsIgnoreCase(guestId.trim())) {
                return guest;
            }
        }
        return null;
    }

    private boolean memberIdExists(String memberId) {
        PriorityQueueADT<Member> copiedQueue = priorityQueue.copy();
        while (!copiedQueue.isEmpty()) {
            Member member = copiedQueue.dequeue();
            if (member.getMemberId().equalsIgnoreCase(memberId.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean guestAlreadyQueued(String guestId) {
        PriorityQueueADT<Member> copiedQueue = priorityQueue.copy();
        while (!copiedQueue.isEmpty()) {
            Member member = copiedQueue.dequeue();
            if (member.getGuest().getGuestId().equalsIgnoreCase(guestId.trim())) {
                return true;
            }
        }
        return false;
    }

    private Room findVacantRoom() {
        for (Room room : rooms) {
            if (room != null && room.isAvailability()) {
                return room;
            }
        }
        return null;
    }
}