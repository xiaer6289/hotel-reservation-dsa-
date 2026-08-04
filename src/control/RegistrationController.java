package control;

import adt.linear.DoublyLinkedList;
import adt.linear.LinearADT;
import dao.GuestDao;
import entity.Guest;
import entity.WalkInRegistration;

/**
 *
 * @author Lai Jen Feng
 */
public class RegistrationController {

    // 只保存正在等待的 registrations
    private final LinearADT<WalkInRegistration> registrationQueue;

    // 保存全部 registrations，包括 WAITING、PROCESSED 和 CANCELLED
    private final LinearADT<WalkInRegistration> registrationRecords;

    // 使用 teammate 已经建立的 GuestDao 和 guest.dat
    private final GuestDao guestDao;
    private Guest[] guests;

    public RegistrationController() {
        registrationQueue = new DoublyLinkedList<>();
        registrationRecords = new DoublyLinkedList<>();

        guestDao = new GuestDao();
        guests = guestDao.loadOrSeed();

        if (guests == null) {
            guests = new Guest[0];
        }
    }

    /**
     * 根据 Guest ID 搜索 guest.dat 中的 Guest。
     *
     * @param guestId Guest ID
     * @return 找到的 Guest，找不到则返回 null
     */
    public Guest searchGuestById(String guestId) {
        if (guestId == null) {
            return null;
        }

        for (Guest guest : guests) {
            if (guest != null
                    && guest.getGuestId()
                            .equalsIgnoreCase(guestId)) {

                return guest;
            }
        }

        return null;
    }

    /**
     * 建立新的 Guest，并保存进 guest.dat。
     *
     * @param guestId Guest ID
     * @param guestName Guest Name
     * @param phoneNumber Phone Number
     * @return 新 Guest；ID 已存在则返回 null
     */
    public Guest addNewGuest(
            String guestId,
            String guestName,
            Long phoneNumber) {

        // 防止重复 Guest ID
        if (searchGuestById(guestId) != null) {
            return null;
        }

        Guest newGuest = new Guest(
                guestId,
                guestName,
                phoneNumber);

        // 创建长度多一个的新 array
        Guest[] updatedGuests
                = new Guest[guests.length + 1];

        // 复制原本所有 Guest
        for (int i = 0; i < guests.length; i++) {
            updatedGuests[i] = guests[i];
        }

        // 把新 Guest 放在最后
        updatedGuests[guests.length] = newGuest;

        // 更新 Controller 里的 Guest array
        guests = updatedGuests;

        // 保存完整 Guest array，避免覆盖后只剩一个 Guest
        guestDao.saveToFile(guests);

        return newGuest;
    }

    public void addRegistration(
            WalkInRegistration registration) {

        registrationQueue.addLast(registration);
        registrationRecords.addLast(registration);
    }

    public int getWaitingCount() {
        return registrationQueue.size();
    }

    public WalkInRegistration getRegistrationAt(int index) {
        if (index < 0
                || index >= registrationQueue.size()) {

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

        for (int i = 0;
                i < registrationRecords.size();
                i++) {

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
        if (index < 0
                || index >= registrationRecords.size()) {

            return null;
        }

        return registrationRecords.get(index);
    }

    public WalkInRegistration cancelRegistrationById(
            String registrationId) {

        for (int i = 0;
                i < registrationQueue.size();
                i++) {

            WalkInRegistration registration
                    = registrationQueue.get(i);

            if (registration.getRegistrationId()
                    .equalsIgnoreCase(registrationId)) {

                registrationQueue.removeAt(i);
                registration.setStatus("CANCELLED");

                return registration;
            }
        }

        return null;
    }
}