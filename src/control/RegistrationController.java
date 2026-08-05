package control;

import adt.linear.DoublyLinkedList;
import adt.linear.LinearADT;
import dao.GuestDao;
import entity.Guest;
import entity.LoyaltyTier;
import entity.WalkInRegistration;

/**
 * Controls walk-in registrations and routes them to either the standard FIFO
 * queue or the VIP MaxHeap.
 *
 * @author Lai Jen Feng
 */
public class RegistrationController {

    /* Standard guests waiting in chronological FIFO order. */
    private final LinearADT<WalkInRegistration> registrationQueue;

    /* All registrations, including standard and VIP records. */
    private final LinearADT<WalkInRegistration> registrationRecords;

    private final GuestDao guestDao;
    private Guest[] guests;

    /* Shared with VipAllocationUI through Main. */
    private final VipPriorityController vipPriorityController;

    public RegistrationController() {
        this(new VipPriorityController());
    }

    public RegistrationController(
            VipPriorityController vipPriorityController) {

        registrationQueue = new DoublyLinkedList<>();
        registrationRecords = new DoublyLinkedList<>();

        this.vipPriorityController = vipPriorityController;

        guestDao = new GuestDao();
        guests = guestDao.loadOrSeed();

        if (guests == null) {
            guests = new Guest[0];
        }
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

        for (int i = 0; i < guests.length; i++) {
            updatedGuests[i] = guests[i];
        }

        updatedGuests[guests.length] = newGuest;
        guests = updatedGuests;

        guestDao.saveToFile(guests);
        return newGuest;
    }

    /**
     * Adds a normal guest to the standard DoublyLinkedList queue.
     */
    public void addStandardRegistration(
            WalkInRegistration registration) {

        registration.setStatus("WAITING");
        registrationQueue.addLast(registration);
        registrationRecords.addLast(registration);
    }

    /**
     * Kept as an alias for existing code that treats a registration as standard.
     */
    public void addRegistration(
            WalkInRegistration registration) {

        addStandardRegistration(registration);
    }

    /**
     * Routes a completed registration into the shared VIP MaxHeap.
     */
    public int addVipRegistration(
            WalkInRegistration registration,
            String memberId,
            LoyaltyTier tier) {

        int result = vipPriorityController.addVipRegistration(
                memberId,
                registration,
                tier);

        if (result == VipPriorityController.ADD_SUCCESS) {
            registrationRecords.addLast(registration);
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

    public WalkInRegistration processNextRegistration() {
        if (registrationQueue.isEmpty()) {
            return null;
        }

        WalkInRegistration registration
                = registrationQueue.removeFirst();

        registration.setStatus("PROCESSED");
        return registration;
    }

    public WalkInRegistration searchRegistrationById(
            String registrationId) {

        for (int i = 0; i < registrationRecords.size(); i++) {
            WalkInRegistration registration
                    = registrationRecords.get(i);

            if (registration.getRegistrationId()
                    .equalsIgnoreCase(registrationId)) {

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

    /**
     * Cancels from the standard queue first; if it is not there, checks the VIP
     * MaxHeap through VipPriorityController.
     */
    public WalkInRegistration cancelRegistrationById(
            String registrationId) {

        for (int i = 0; i < registrationQueue.size(); i++) {
            WalkInRegistration registration
                    = registrationQueue.get(i);

            if (registration.getRegistrationId()
                    .equalsIgnoreCase(registrationId)) {

                registrationQueue.removeAt(i);
                registration.setStatus("CANCELLED");
                return registration;
            }
        }

        return vipPriorityController
                .cancelVipRegistrationById(registrationId);
    }
}