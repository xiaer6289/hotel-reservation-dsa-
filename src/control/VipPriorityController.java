package control;

import adt.heap.MaxHeap;
import adt.heap.PriorityQueueADT;
import dao.BookingDao;
import dao.GuestDao;
import dao.PaymentDao;
import dao.RoomDao;
import entity.Booking;
import entity.Guest;
import entity.LoyaltyTier;
import entity.Member;
import entity.Payment;
import entity.Room;
import entity.RoomType;
import entity.WalkInRegistration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import utility.Utility;

/**
 * Controls VIP registration priority and room allocation.
 *
 * @author Low Enn Toong
 */
public class VipPriorityController {

    public static final int ADD_SUCCESS = 1;
    public static final int INVALID_INPUT = -1;
    public static final int DUPLICATE_MEMBER_ID = -2;
    public static final int REGISTRATION_ALREADY_QUEUED = -3;
    public static final int GUEST_ALREADY_QUEUED = -4;

    private final PriorityQueueADT<Member> priorityQueue;

    private final RoomDao roomDao;
    private final BookingDao bookingDao;
    private final PaymentDao paymentDao;

    private Room[] rooms;
    private Booking[] bookings;
    private Payment[] payments;

    private Booking lastCreatedBooking;

    public VipPriorityController() {
        priorityQueue = new MaxHeap<>();

        roomDao = new RoomDao();
        bookingDao = new BookingDao();
        paymentDao = new PaymentDao();

        rooms = roomDao.loadOrSeed();
        payments = paymentDao.loadOrSeed();

        Guest[] guests = new GuestDao().loadOrSeed();
        bookings = bookingDao.loadOrSeed(guests, rooms, payments);
    }

    /**
     * Receives a completed Walk-In Registration from RegistrationController and
     * inserts it into the MaxHeap according to loyalty tier.
     */
    public int addVipRegistration(
            String memberId,
            WalkInRegistration registration,
            LoyaltyTier tier) {

        if (memberId == null
                || memberId.isBlank()
                || registration == null
                || registration.getGuest() == null
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

        /* enqueue() automatically calls reheapUp(). */
        priorityQueue.enqueue(member);
        registration.setStatus("VIP-WAITING");

        return ADD_SUCCESS;
    }

    public Member peekNextVip() {
        return priorityQueue.peek();
    }

    public int getWaitingCount() {
        return priorityQueue.size();
    }

    public boolean hasWaitingVip() {
        return !priorityQueue.isEmpty();
    }

    /**
     * Allocates a suitable room to the highest-priority VIP.
     *
     * The member is removed only after a suitable room is found and the room,
     * registration, payment and booking records have been updated.
     */
    public Room allocateNextVipRoom() {
        lastCreatedBooking = null;

        Member nextVip = priorityQueue.peek();
        if (nextVip == null) {
            return null;
        }

        WalkInRegistration registration = nextVip.getRegistration();
        Room suitableRoom = findSuitableVacantRoom(registration);

        if (suitableRoom == null) {
            /* The VIP remains at the MaxHeap root. */
            return null;
        }

        updateAllocatedRoom(suitableRoom, registration);
        registration.setStatus("CHECKED-IN");

        Payment payment = createPendingPayment(suitableRoom, registration);
        Booking booking = new Booking(
                generateUniqueConfirmationNo(),
                registration.getGuest(),
                suitableRoom,
                payment);

        payments = appendPayment(payments, payment);
        bookings = appendBooking(bookings, booking);

        roomDao.saveToFile(rooms);
        paymentDao.saveToFile(payments);
        bookingDao.saveToFile(bookings);

        /* Remove only after the allocation records have been created. */
        priorityQueue.dequeue();
        lastCreatedBooking = booking;

        return suitableRoom;
    }

    public Booking getLastCreatedBooking() {
        return lastCreatedBooking;
    }

    public Member[] getMembersByPriority() {
        PriorityQueueADT<Member> copiedQueue = priorityQueue.copy();
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
     * Removes a VIP registration when it is cancelled from the Walk-In module.
     */
    public WalkInRegistration cancelVipRegistrationById(
            String registrationId) {

        if (registrationId == null || registrationId.isBlank()) {
            return null;
        }

        PriorityQueueADT<Member> retainedMembers = new MaxHeap<>();
        WalkInRegistration removedRegistration = null;

        while (!priorityQueue.isEmpty()) {
            Member member = priorityQueue.dequeue();

            if (removedRegistration == null
                    && member.getRegistration().getRegistrationId()
                            .equalsIgnoreCase(registrationId.trim())) {

                removedRegistration = member.getRegistration();
            } else {
                retainedMembers.enqueue(member);
            }
        }

        while (!retainedMembers.isEmpty()) {
            priorityQueue.enqueue(retainedMembers.dequeue());
        }

        if (removedRegistration != null) {
            removedRegistration.setStatus("CANCELLED");
        }

        return removedRegistration;
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

    private boolean registrationAlreadyQueued(String registrationId) {
        PriorityQueueADT<Member> copiedQueue = priorityQueue.copy();

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
        PriorityQueueADT<Member> copiedQueue = priorityQueue.copy();

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

        room.setAvailability(false);
        room.setStatus('O');
        room.setBookingDate(LocalDateTime.now());
        room.setCheckInDateTime(registration.getCheckInDateTime());
        room.setCheckOutDateTime(registration.getCheckOutDateTime());
    }

    private Payment createPendingPayment(
            Room room,
            WalkInRegistration registration) {

        long numberOfDays = ChronoUnit.DAYS.between(
                registration.getCheckInDateTime().toLocalDate(),
                registration.getCheckOutDateTime().toLocalDate());

        if (numberOfDays < 1) {
            numberOfDays = 1;
        }

        double pricePerDay = RoomType.valueOf(
                room.getRoomType().toUpperCase()).getPricePerDay();

        double amount = pricePerDay * numberOfDays;

        return new Payment(
                generateUniquePaymentId(),
                amount,
                LocalDateTime.now(),
                'P');
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
                    && booking.getConfirmationNo()
                            .equals(confirmationNo)) {

                return true;
            }
        }

        return false;
    }

    private String generateUniquePaymentId() {
        int number = payments.length + 1;
        String paymentId;

        do {
            paymentId = String.format("PAY%03d", number++);
        } while (paymentIdExists(paymentId));

        return paymentId;
    }

    private boolean paymentIdExists(String paymentId) {
        for (Payment payment : payments) {
            if (payment != null
                    && payment.getPaymentId()
                            .equalsIgnoreCase(paymentId)) {

                return true;
            }
        }

        return false;
    }

    private Payment[] appendPayment(
            Payment[] original,
            Payment newPayment) {

        Payment[] updated = new Payment[original.length + 1];

        for (int i = 0; i < original.length; i++) {
            updated[i] = original[i];
        }

        updated[original.length] = newPayment;
        return updated;
    }

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
}