/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control;

import adt.bst.Bst;
import adt.bst.BstInterface;
import dao.BookingDao;
import dao.GuestDao;
import dao.PaymentDao;
import dao.RoomDao;
import entity.Booking;
import entity.Guest;
import entity.Payment;
import entity.RegistrationStatus;
import entity.Room;
import entity.RoomStatus;
import entity.TaskLogEntry;
import entity.WalkInRegistration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 *
 * @author Lee Cheng Xuan
 */
public class FrontDeskControl implements RoomAvailabilityNotifier.RoomReadyListener {

    private BstInterface<String, Booking> bookingBst;
    private BookingDao bookingDao;
    private RoomDao roomDao;
    private PaymentDao paymentDao;
    private GuestDao guestDao;
    private Room[] rooms;
    private RegistrationController registrationController;
    private final HousekeepingController housekeepingController;
    
    public FrontDeskControl() {
        guestDao = new GuestDao();
        roomDao = new RoomDao();
        paymentDao = new PaymentDao();
        bookingDao = new BookingDao();
        registrationController = new RegistrationController();
        housekeepingController = new HousekeepingController();

        RoomAvailabilityNotifier.registerListener(this);

        Guest[] guests = guestDao.loadOrSeed();
        rooms = roomDao.loadOrSeed();
        Payment[] payments = paymentDao.loadOrSeed();
        Booking[] bookings = bookingDao.loadOrSeed(guests, rooms, payments);

        bookingBst = new Bst<>();
        for (Booking booking : bookings) {
            if (booking != null) {
            bookingBst.insert(booking.getConfirmationNo(), booking);
            }
        }
    }
    
    public Booking searchBookingByConfirmationNo(String confirmationNo) {
        return bookingBst.search(confirmationNo);
    }
    
    public boolean isRoomAvailable(String roomNumber) {
        Room room = findRoomByNumber(roomNumber);
        return room != null && room.isAssignable();
    }

    public String getRoomAvailabilityMessage(String roomNumber) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null) {
            return "Room not found.";
        }

        switch (room.getRoomStatus()) {
            case AVAILABLE:
            case READY:
                return "Room " + roomNumber + " is ready for assignment.";
            case OCCUPIED:
                return "Room " + roomNumber + " is occupied.";
            case DIRTY:
            case CLEANING_IN_PROGRESS:
            case INSPECTED:
                return "Room " + roomNumber + " is not ready yet and still needs cleaning.";
            default:
                return "Room " + roomNumber + " is not available.";
        }
    }

    public Room[] getAssignableRooms() {
        refreshRooms();
        int count = 0;

        for (Room room : rooms) {
            if (room != null && room.isAssignable()) {
                count++;
            }
        }

        Room[] assignableRooms = new Room[count];
        int index = 0;
        for (Room room : rooms) {
            if (room != null && room.isAssignable()) {
                assignableRooms[index++] = room;
            }
        }

        return assignableRooms;
    }

    public TaskLogEntry processCheckout(String confirmationNo, String staffId) {
        return processCheckout(confirmationNo, staffId, null);
    }

    public TaskLogEntry processCheckout(String confirmationNo, String staffId, String remarks) {
        if (confirmationNo == null || confirmationNo.isBlank()) {
            return null;
        }

        Booking booking = searchBookingByConfirmationNo(confirmationNo.trim());
        if (booking == null) {
            return null;
        }

        if (booking.getRoom() == null || booking.getRoom().getRoomNumber() == null) {
            return null;
        }

        /*
         * A historical Booking must not be allowed to check out again. A
         * guest is considered currently checked in only when the matching
         * WalkInRegistration is still CHECKED_IN and the live room record is
         * still OCCUPIED for the same check-in time.
         */
        WalkInRegistration activeRegistration
                = findCheckedInRegistrationForBooking(booking);

        if (activeRegistration == null) {
            return null;
        }

        /*
         * Booking.room is a serialized snapshot of the room at booking time.
         * Never use that snapshot as the current room-state object. The
         * Housekeeping controller updates the shared RoomDao room instead.
         */
        String roomNumber = booking.getRoom().getRoomNumber();
        Room currentRoom = findRoomByNumber(roomNumber);
        if (currentRoom == null) {
            return null;
        }

        boolean sameLiveStay
                = currentRoom.getRoomStatus() == RoomStatus.OCCUPIED
                && currentRoom.getCheckInDateTime() != null
                && activeRegistration.getCheckInDateTime() != null
                && currentRoom.getCheckInDateTime()
                        .equals(activeRegistration.getCheckInDateTime());

        if (!sameLiveStay) {
            return null;
        }

        TaskLogEntry task = housekeepingController.createCheckoutTask(roomNumber, staffId, remarks);
        if (task == null) {
            return null;
        }

        refreshRooms();
        
        if (booking.getGuest() != null) {
            registrationController.markGuestCheckedOut(
                booking.getGuest().getGuestId());
        }

        return task;
    }

    /**
     * Returns only bookings that represent guests who are currently staying
     * in the hotel. Historical booking room snapshots are deliberately not
     * used as the source of truth for current occupancy.
     */
    public Booking[] getCurrentCheckedInBookings() {
        Booking[] allBookings = sortBooking();
        refreshRooms();

        Booking[] matches = new Booking[allBookings.length];
        int count = 0;

        for (Booking booking : allBookings) {
            if (booking == null
                    || booking.getRoom() == null
                    || booking.getRoom().getRoomNumber() == null) {
                continue;
            }

            WalkInRegistration registration
                    = findCheckedInRegistrationForBooking(booking);

            if (registration == null) {
                continue;
            }

            Room currentRoom = findRoomByNumberWithoutRefresh(
                    booking.getRoom().getRoomNumber());

            if (currentRoom == null
                    || currentRoom.getRoomStatus() != RoomStatus.OCCUPIED
                    || currentRoom.getCheckInDateTime() == null
                    || registration.getCheckInDateTime() == null
                    || !currentRoom.getCheckInDateTime()
                            .equals(registration.getCheckInDateTime())) {
                continue;
            }

            matches[count++] = booking;
        }

        Booking[] currentBookings = new Booking[count];
        System.arraycopy(matches, 0, currentBookings, 0, count);
        return currentBookings;
    }

    /**
     * Finds the CHECKED_IN registration that belongs to one Booking by Guest
     * ID and the actual check-in timestamp. This also lets FrontDeskUI show
     * the Registration ID without storing duplicate data in Booking.
     */
    public WalkInRegistration getCheckedInRegistrationForBooking(Booking booking) {
        return findCheckedInRegistrationForBooking(booking);
    }

    private WalkInRegistration findCheckedInRegistrationForBooking(Booking booking) {
        if (booking == null
                || booking.getGuest() == null
                || booking.getGuest().getGuestId() == null
                || booking.getRoom() == null
                || booking.getRoom().getCheckInDateTime() == null) {
            return null;
        }

        String guestId = booking.getGuest().getGuestId();

        for (int i = registrationController.getTotalRegistrationCount() - 1;
                i >= 0; i--) {

            WalkInRegistration registration
                    = registrationController.getRecordAt(i);

            if (registration == null
                    || registration.getGuest() == null
                    || registration.getGuest().getGuestId() == null
                    || registration.getCheckInDateTime() == null
                    || registration.getStatus() != RegistrationStatus.CHECKED_IN) {
                continue;
            }

            boolean sameGuest = registration.getGuest().getGuestId()
                    .equalsIgnoreCase(guestId);

            boolean sameCheckInTime = registration.getCheckInDateTime()
                    .equals(booking.getRoom().getCheckInDateTime());

            if (sameGuest && sameCheckInTime) {
                return registration;
            }
        }

        return null;
    }
    
    public Booking[] sortBooking() {
        Booking[] result = new Booking[bookingBst.size()];
        final int[] INDEX = {0};
        bookingBst.inorderTraversal(booking -> {
            result[INDEX[0]] = booking;
            INDEX[0]++;
        });
        return result;
    }
    
    public boolean save() {
        Booking[] bookings = sortBooking();
        refreshRooms();
        Room[] roomsToSave = rooms;
        Payment[] payments = new Payment[bookings.length];
        
        for (int i = 0; i < bookings.length; i++) {
            payments[i] = bookings[i].getPayment();
        }
        
        roomDao.saveToFile(roomsToSave);
        paymentDao.saveToFile(payments);
        bookingDao.saveToFile(bookings);
        return true;
    }

    @Override
    public void onRoomReady(Room room) {
        if (room == null) {
            return;
        }

        System.out.println("[Front Desk Notification] Room " + room.getRoomNumber()
                + " is READY and can be assigned to a new guest.");
    }

    public Room[] getNotifiedReadyRooms() {
        refreshRooms();
        Room[] notifications = RoomAvailabilityNotifier.getReadyRoomNotifications();
        Room[] temp = new Room[notifications.length];
        int count = 0;

        for (Room notification : notifications) {
            if (notification == null || notification.getRoomNumber() == null) {
                continue;
            }

            Room currentRoom = findRoomByNumberWithoutRefresh(notification.getRoomNumber());
            if (currentRoom != null && currentRoom.isAssignable()) {
                temp[count++] = currentRoom;
            }
        }

        Room[] readyRooms = new Room[count];
        System.arraycopy(temp, 0, readyRooms, 0, count);
        return readyRooms;
    }

    public Room[] getCurrentRooms() {
        refreshRooms();
        return rooms;
    }

    private Room findRoomByNumber(String roomNumber) {
        refreshRooms();
        return findRoomByNumberWithoutRefresh(roomNumber);
    }

    private Room findRoomByNumberWithoutRefresh(String roomNumber) {
        if (roomNumber == null) {
            return null;
        }

        for (Room room : rooms) {
            if (room != null && room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return room;
            }
        }
        return null;
    }

    private void refreshRooms() {
        rooms = roomDao.loadOrSeed();
    }

    // ===== Boundary display helpers (ECB: Boundary does not access Entity objects) =====
    public String[] getBookingDisplayData(String confirmationNo) {
        Booking booking = searchBookingByConfirmationNo(confirmationNo);
        if (booking == null) {
            return null;
        }

        String paymentAmount = booking.getPayment() == null
                ? "N/A" : String.valueOf(booking.getPayment().getAmount());
        String paymentStatus = booking.getPayment() == null
                ? "N/A" : String.valueOf(booking.getPayment().getStatus());

        return new String[] {
            booking.getConfirmationNo(),
            booking.getGuest() == null ? "null" : booking.getGuest().getName(),
            booking.getGuest() == null ? "null" : String.valueOf(booking.getGuest().getPhoneNo()),
            booking.getRoom() == null ? "null" : booking.getRoom().getRoomNumber(),
            booking.getRoom() == null ? "null" : String.valueOf(booking.getRoom().getRoomType()),
            paymentAmount,
            paymentStatus
        };
    }

    public String[][] getAllBookingDisplayRows() {
        Booking[] bookings = sortBooking();
        String[][] rows = new String[bookings.length][3];
        for (int i = 0; i < bookings.length; i++) {
            Booking booking = bookings[i];
            rows[i][0] = booking.getConfirmationNo();
            rows[i][1] = String.valueOf(booking.getGuest());
            rows[i][2] = booking.getRoom() == null ? "null" : booking.getRoom().getRoomNumber();
        }
        return rows;
    }

    public String[][] getCurrentCheckedInDisplayRows() {
        Booking[] bookings = getCurrentCheckedInBookings();
        String[][] rows = new String[bookings.length][6];
        for (int i = 0; i < bookings.length; i++) {
            Booking booking = bookings[i];
            WalkInRegistration registration = findCheckedInRegistrationForBooking(booking);
            rows[i][0] = booking.getRoom().getRoomNumber();
            rows[i][1] = booking.getGuest().getGuestId();
            rows[i][2] = booking.getGuest().getName();
            rows[i][3] = registration == null ? "N/A" : registration.getRegistrationId();
            rows[i][4] = booking.getConfirmationNo();
            rows[i][5] = formatBoundaryDateTime(registration == null
                    ? booking.getRoom().getCheckOutDateTime()
                    : registration.getCheckOutDateTime());
        }
        return rows;
    }

    public String[] getCurrentCheckedInStayDisplayData(String roomNumber) {
        Booking[] bookings = getCurrentCheckedInBookings();
        for (Booking booking : bookings) {
            if (booking != null && booking.getRoom() != null
                    && booking.getRoom().getRoomNumber().equalsIgnoreCase(roomNumber)) {
                WalkInRegistration registration = findCheckedInRegistrationForBooking(booking);
                return new String[] {
                    booking.getRoom().getRoomNumber(),
                    booking.getGuest().getGuestId(),
                    booking.getGuest().getName(),
                    String.valueOf(booking.getGuest().getPhoneNo()),
                    registration == null ? "N/A" : registration.getRegistrationId(),
                    booking.getConfirmationNo(),
                    formatBoundaryDateTime(registration == null
                            ? booking.getRoom().getCheckInDateTime()
                            : registration.getCheckInDateTime()),
                    formatBoundaryDateTime(registration == null
                            ? booking.getRoom().getCheckOutDateTime()
                            : registration.getCheckOutDateTime())
                };
            }
        }
        return null;
    }

    public String processCheckoutAndGetTaskId(String confirmationNo, String staffId, String remarks) {
        TaskLogEntry task = processCheckout(confirmationNo, staffId, remarks);
        return task == null ? null : task.getTaskId();
    }

    public String[][] getReadyRoomNotificationDisplayRows() {
        Room[] readyRooms = getNotifiedReadyRooms();
        String[][] rows = new String[readyRooms.length][2];
        for (int i = 0; i < readyRooms.length; i++) {
            rows[i][0] = readyRooms[i].getRoomNumber();
            rows[i][1] = readyRooms[i].getStatusLabel();
        }
        return rows;
    }
    
    public boolean updatePaymentStatus(String confirmationNo, char newStatus) {
        Booking booking = bookingBst.search(confirmationNo);
        if (booking == null || booking.getPayment() == null) return false;
        
        booking.getPayment().setStatus(newStatus);
        bookingBst.insert(confirmationNo, booking);
        return true;
    }
    
    public String[][] searchGuestsByIdOrName(String guestIdOrName) {
        Booking[] matches = findBookingsByGuestIdOrName(guestIdOrName.trim());
        String[][] rows = new String[matches.length][3];
        for (int i = 0; i < matches.length; i++) {
            rows[i][0] = matches[i].getConfirmationNo();
            rows[i][1] = matches[i].getGuest().getGuestId();
            rows[i][2] = matches[i].getGuest().getName();
        }
        return rows;
    }

    public boolean updateGuestPhoneNoByConfirmationNo(String confirmationNo, long newPhoneNo) {
        Booking booking = bookingBst.search(confirmationNo);
        if (booking == null || booking.getGuest() == null) return false;

        Long normalized = normalizeStoredPhoneNo(newPhoneNo);
        if (normalized == null) return false;

        if (isPhoneNoTakenByOtherGuest(normalized, confirmationNo)) return false;

        booking.getGuest().setPhoneNo(normalized);
        bookingBst.insert(confirmationNo, booking);
        return true;
    }
    
    public String[] getRoomOccupancySummary(String roomTypeFilter, int rangeOption, int selectedMonth) {
        LocalDate[] range = computeDateRange(rangeOption, selectedMonth);
        long daysInRange = ChronoUnit.DAYS.between(range[0], range[1]) + 1;

        refreshRooms();
        int matchingRoomCount = 0;
        for (Room room : rooms) {
            if (room != null && (roomTypeFilter == null || String.valueOf(room.getRoomType()).equals(roomTypeFilter))) {
                matchingRoomCount++;
            }
        }

        control.report.RoomOccupancyRP reportGenerator = new control.report.RoomOccupancyRP();
        Booking[] filtered = reportGenerator.generateReport(sortBooking(), roomTypeFilter, range[0], range[1]);

        long occupiedRoomDays = 0;
        for (Booking booking : filtered) {
            occupiedRoomDays += countOverlapDays(
                    booking.getRoom().getCheckInDateTime(),
                    booking.getRoom().getCheckOutDateTime(),
                    range[0], range[1]);
        }

        long totalRoomDays = matchingRoomCount * daysInRange;
        double occupancyRate = totalRoomDays == 0 ? 0 : (occupiedRoomDays * 100.0 / totalRoomDays);

        return new String[] {
            String.valueOf(filtered.length),
            String.valueOf(matchingRoomCount),
            String.valueOf(daysInRange),
            String.valueOf(occupiedRoomDays),
            String.valueOf(totalRoomDays),
            String.format("%.1f", occupancyRate)
        };
    }

    public String[][] generateRoomOccupancyReportDisplay(String roomTypeFilter, int rangeOption, int selectedMonth) {
        LocalDate[] range = computeDateRange(rangeOption, selectedMonth);
        control.report.RoomOccupancyRP reportGenerator = new control.report.RoomOccupancyRP();
        Booking[] report = reportGenerator.generateReport(sortBooking(), roomTypeFilter, range[0], range[1]);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS");
        String[][] rows = new String[report.length][7];
        for (int i = 0; i < report.length; i++) {
            Booking booking = report[i];
            rows[i][0] = booking.getRoom().getRoomNumber();
            rows[i][1] = String.valueOf(booking.getRoom().getRoomType());
            rows[i][2] = String.valueOf(booking.getRoom().getFloor());
            rows[i][3] = booking.getGuest().getName();
            rows[i][4] = booking.getRoom().getCheckInDateTime() == null ? "N/A" : booking.getRoom().getCheckInDateTime().format(formatter);
            rows[i][5] = booking.getRoom().getCheckOutDateTime() == null ? "N/A" : booking.getRoom().getCheckOutDateTime().format(formatter);
            rows[i][6] = booking.getRoom().isAvailability() ? "Available" : "Occupied";
        }
        return rows;
    }
    
    public String[] getBillingSummaryBreakdown(char statusFilter) {
        control.report.BillSummaryRP reportGenerator = new control.report.BillSummaryRP();
        Booking[] filtered = reportGenerator.generateReport(sortBooking(), statusFilter);
        int completed = reportGenerator.countByStatus(filtered, 'C');
        int pending = reportGenerator.countByStatus(filtered, 'P');
        int cancelled = reportGenerator.countByStatus(filtered, 'X');
        int refunded = reportGenerator.countByStatus(filtered, 'R');
        double totalRevenue = reportGenerator.calcTotalRevenue(filtered);
        return new String[] {
            String.valueOf(filtered.length),
            String.valueOf(completed),
            String.valueOf(pending),
            String.valueOf(cancelled),
            String.valueOf(refunded),
            String.format("%.2f", totalRevenue)
        };
    }

    public String[][] generateBillingSummaryReportDisplay(char statusFilter) {
        control.report.BillSummaryRP reportGenerator = new control.report.BillSummaryRP();
        Booking[] report = reportGenerator.generateReport(sortBooking(), statusFilter);
        String[][] rows = new String[report.length][5];
        for (int i = 0; i < report.length; i++) {
            Booking booking = report[i];
            rows[i][0] = booking.getConfirmationNo();
            rows[i][1] = booking.getGuest().getName();
            rows[i][2] = String.valueOf(booking.getRoom().getRoomType());
            rows[i][3] = String.valueOf(booking.getPayment().getAmount());
            rows[i][4] = String.valueOf(booking.getPayment().getStatus());
        }
        return rows;
    }

    public double getBillingSummaryTotal(char statusFilter) {
        control.report.BillSummaryRP reportGenerator = new control.report.BillSummaryRP();
        Booking[] report = reportGenerator.generateReport(sortBooking(), statusFilter);
        return reportGenerator.calcTotalRevenue(report);
    }
    
    public String getMonthNameLabel(int month) {
        return getMonthName(month);
    }
    
    private boolean isPhoneNoTakenByOtherGuest(Long normalizedPhoneNo, String excludeConfirmationNo) {
        final boolean[] taken = {false};
        bookingBst.inorderTraversal(booking -> {
            if (booking.getConfirmationNo().equals(excludeConfirmationNo)) return;
            if (booking.getGuest() == null || booking.getGuest().getPhoneNo() == null) return;

            Long existingNormalized = normalizeStoredPhoneNo(booking.getGuest().getPhoneNo());
            if (existingNormalized != null && existingNormalized.equals(normalizedPhoneNo)) {
                taken[0] = true;
            }
        });
        return taken[0];
    }
    
    private Long normalizeStoredPhoneNo(Long phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String digits = String.valueOf(phoneNumber);
        if (digits.startsWith("60")
                && (digits.length() == 11 || digits.length() == 12)) {
            return phoneNumber;
        }
        if (digits.startsWith("1")
                && (digits.length() == 9 || digits.length() == 10)) {
            try {
                return Long.valueOf("60" + digits);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return phoneNumber;
    }
    
    private Booking[] findBookingsByGuestIdOrName(String guestIdOrName) {
        Booking[] temp = new Booking[bookingBst.size()];
        final int[] count = {0};

        bookingBst.inorderTraversal(booking -> {
            if (booking.getGuest() == null) return;

            boolean matchesId = booking.getGuest().getGuestId() != null
                    && booking.getGuest().getGuestId().equalsIgnoreCase(guestIdOrName);
            boolean matchesName = booking.getGuest().getName() != null
                    && booking.getGuest().getName().equalsIgnoreCase(guestIdOrName);

            if (matchesId || matchesName) {
                temp[count[0]++] = booking;
            }
        });

        Booking[] result = new Booking[count[0]];
        System.arraycopy(temp, 0, result, 0, count[0]);
        return result;
    }
    
    private LocalDate[] computeDateRange(int rangeOption, int selectedMonth) {
        LocalDate today = LocalDate.now();
        int year = today.getYear();

        switch (rangeOption) {
            case 1:
                return new LocalDate[]{today, today};
            case 2:
                LocalDate firstOfMonth = LocalDate.of(year, selectedMonth, 1);
                return new LocalDate[]{firstOfMonth, firstOfMonth.withDayOfMonth(firstOfMonth.lengthOfMonth())};
            case 3:
                return new LocalDate[]{LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)};
            default:
                return new LocalDate[]{today, today};
        }
    }
    
    private long countOverlapDays(java.time.LocalDateTime checkIn, java.time.LocalDateTime checkOut, LocalDate rangeStart, LocalDate rangeEnd) {
        if (checkIn == null || checkOut == null) return 0;
        LocalDate bookingStart = checkIn.toLocalDate();
        LocalDate bookingEnd = checkOut.toLocalDate();
        LocalDate overlapStart = bookingStart.isAfter(rangeStart) ? bookingStart : rangeStart;
        LocalDate overlapEnd = bookingEnd.isBefore(rangeEnd) ? bookingEnd : rangeEnd;
        if (overlapStart.isAfter(overlapEnd)) return 0;
        return ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
    }
    
    private String getMonthName(int month) {
        switch (month) {
            case 1: return "JANUARY";
            case 2: return "FEBRUARY";
            case 3: return "MARCH";
            case 4: return "APRIL";
            case 5: return "MAY";
            case 6: return "JUNE";
            case 7: return "JULY";
            case 8: return "AUGUST";
            case 9: return "SEPTEMBER";
            case 10: return "OCTOBER";
            case 11: return "NOVEMBER";
            case 12: return "DECEMBER";
            default: return "UNKNOWN";
        }
    }

    private String formatBoundaryDateTime(java.time.LocalDateTime value) {
        return value == null ? "N/A"
                : value.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}