package control;

import adt.linear.DoublyLinkedList;
import adt.linear.LinearADT;
import dao.GuestDao;
import dao.LoyaltyProfileDao;
import dao.WalkInRegistrationDao;
import entity.Guest;
import entity.LoyaltyProfile;
import entity.LoyaltyTier;
import entity.RegistrationStatus;
import entity.WalkInRegistration;

/**
 * Controls walk-in registrations and routes them to either the standard FIFO
 * queue or the VIP MaxHeap.
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
        registrationDao = new WalkInRegistrationDao();
        loyaltyProfileDao = new LoyaltyProfileDao();
        guests = guestDao.loadOrSeed();
        loyaltyProfiles = loyaltyProfileDao.loadOrSeed();

        if (guests == null) {
            guests = new Guest[0];
        }

        if (loyaltyProfiles == null) {
            loyaltyProfiles = new LoyaltyProfile[0];
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

    /**
     * Adds a normal guest to the standard DoublyLinkedList queue.
     */
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

    /**
     * Alias retained for existing registration code.
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

    public WalkInRegistration processNextRegistration() {
        if (registrationQueue.isEmpty()) {
            return null;
        }

        WalkInRegistration registration
                = registrationQueue.removeFirst();

        registration.setStatus(RegistrationStatus.PROCESSED);
        registrationDao.upsert(registration);

        return registration;
    }

    public WalkInRegistration searchRegistrationById(
            String registrationId) {

        if (registrationId == null) {
            return null;
        }

        for (int i = 0; i < registrationRecords.size(); i++) {
            WalkInRegistration registration
                    = registrationRecords.get(i);

            if (registration.getRegistrationId()
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
            WalkInRegistration registration
                    = registrationQueue.get(i);

            if (registration.getRegistrationId()
                    .equalsIgnoreCase(registrationId.trim())) {

                registrationQueue.removeAt(i);
                registration.setStatus(RegistrationStatus.CANCELLED);
                registrationDao.upsert(registration);
                return registration;
            }
        }

        return vipPriorityController
                .cancelVipRegistrationById(registrationId);
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