package control.report;

import control.VipPriorityController;
import entity.Booking;
import entity.LoyaltyProfile;
import entity.LoyaltyTier;
import entity.RegistrationStatus;
import entity.RoomType;
import entity.WalkInRegistration;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Management report for VIP room allocation and waiting-time performance.
 * Uses historical/current registrations plus booking records rather than only
 * the current VIP waiting heap. Linear search/filtering and Selection Sort are
 * implemented explicitly to satisfy the report-generation requirements.
 *
 * @author Low Enn Toong
 */
public class VipRoomAllocationWaitingTimeRP {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String ALL = "ALL";

    public void generateReport(VipPriorityController controller, String keyword, LoyaltyTier tierFilter, String roomTypeFilter, String statusFilter, LocalDate startDate, LocalDate endDate, int minimumGuests, int sortOption) {
        WalkInRegistration[] registrations = controller.getAllRegistrationsForReport();
        Booking[] bookings = controller.getAllBookingsForReport();
        LoyaltyProfile[] profiles = controller.getAllVipProfiles();
        AllocationEntry[] entries = searchAndFilter(registrations, bookings, profiles, keyword, tierFilter, roomTypeFilter, statusFilter, startDate, endDate, minimumGuests);
        selectionSort(entries, sortOption);
        printReport(entries, keyword, tierFilter, roomTypeFilter, statusFilter, startDate, endDate, minimumGuests, sortOption);
    }

    private AllocationEntry[] searchAndFilter(WalkInRegistration[] registrations, Booking[] bookings, LoyaltyProfile[] profiles, String keyword, LoyaltyTier tierFilter, String roomTypeFilter, String statusFilter, LocalDate startDate, LocalDate endDate, int minimumGuests) {
        AllocationEntry[] temporary = new AllocationEntry[registrations.length];
        int count = 0;

        for (WalkInRegistration registration : registrations) {
            if (registration == null || registration.getGuest() == null || registration.getGuest().getGuestId() == null || registration.getRegistrationTime() == null) {
                continue;
            }

            LoyaltyProfile profile = findProfile(profiles, registration.getGuest().getGuestId());

            if (profile == null || profile.getTier() == null || !isVipReportStatus(registration.getStatus())) {
                continue;
            }

            Booking booking = findBookingForRegistration(registration, bookings);
            AllocationEntry entry = new AllocationEntry(registration, profile, booking, calculateWaitMinutes(registration));

            if (!matchesKeyword(entry, keyword) || (tierFilter != null && profile.getTier() != tierFilter) || !matchesRoomType(registration, roomTypeFilter) || !matchesStatus(registration.getStatus(), statusFilter) || !matchesDate(registration.getRegistrationTime().toLocalDate(), startDate, endDate) || registration.getNumberOfGuests() < minimumGuests) {
                continue;
            }

            temporary[count++] = entry;
        }

        AllocationEntry[] result = new AllocationEntry[count];
        System.arraycopy(temporary, 0, result, 0, count);
        return result;
    }

    private boolean isVipReportStatus(RegistrationStatus status) {
        return status == RegistrationStatus.VIP_WAITING || status == RegistrationStatus.CHECKED_IN || status == RegistrationStatus.CHECKED_OUT || status == RegistrationStatus.CANCELLED;
    }

    private void printReport(AllocationEntry[] entries, String keyword, LoyaltyTier tierFilter, String roomTypeFilter, String statusFilter, LocalDate startDate, LocalDate endDate, int minimumGuests, int sortOption) {
        final int reportWidth = 124;
        String border = "=".repeat(reportWidth);
        String divider = "-".repeat(reportWidth);

        System.out.println("\n" + border);
        printCentered("VIP ROOM ALLOCATION & WAITING TIME REPORT", reportWidth);
        printCentered("Operational Performance Report", reportWidth);
        System.out.println(border);

        printKeyValue("Generated On", LocalDateTime.now().format(DATE_TIME_FORMAT), 16);
        printKeyValue("Purpose", "Monitor VIP waiting time and evaluate priority-based room allocation performance.", 16);
        printKeyValue("Hotel Value", "Highlights long waits, room shortages and allocation bottlenecks for faster operational action.", 16);

        printSection("REPORT SCOPE", divider);
        printTwoColumnHeader("Parameter", "Selected Value", 20);
        printTwoColumnRow("Report Period", formatPeriod(startDate, endDate), 20);
        printTwoColumnRow("Keyword", displayFilter(keyword), 20);
        printTwoColumnRow("Loyalty Tier", tierFilter == null ? ALL : tierFilter.toString(), 20);
        printTwoColumnRow("Room Type", roomTypeFilter == null ? ALL : formatRoomType(roomTypeFilter), 20);
        printTwoColumnRow("Status", normalizeStatus(statusFilter), 20);
        printTwoColumnRow("Minimum Party", minimumGuests == 0 ? ALL : minimumGuests + " guest(s)", 20);

        printSection("ANALYSIS METHOD", divider);
        printTwoColumnHeader("Method", "Implementation", 20);
        printTwoColumnRow("Search", "Linear Search", 20);
        printTwoColumnRow("Sort", "Selection Sort - " + sortDescription(sortOption), 20);

        if (entries.length == 0) {
            printSection("REPORT RESULT", divider);
            System.out.println("No VIP allocation records match the selected report criteria.");
            System.out.println("The report uses historical and current VIP registrations, so it can run even when the waiting queue is empty.");
            System.out.println(border);
            return;
        }

        int allocated = 0;
        int waiting = 0;
        int cancelled = 0;
        long totalAllocatedWait = 0;
        int allocatedWithWait = 0;
        long longestWait = -1;
        AllocationEntry longestEntry = null;

        int[] tierRequests = new int[LoyaltyTier.values().length];
        int[] tierAllocated = new int[LoyaltyTier.values().length];
        long[] tierWaitTotal = new long[LoyaltyTier.values().length];
        int[] tierWaitCount = new int[LoyaltyTier.values().length];
        int[] roomRequests = new int[RoomType.values().length];
        int[] roomAllocated = new int[RoomType.values().length];

        for (AllocationEntry entry : entries) {
            WalkInRegistration registration = entry.registration;
            boolean wasAllocated = registration.getCheckInDateTime() != null;

            if (wasAllocated) {
                allocated++;
                if (entry.waitMinutes >= 0) {
                    totalAllocatedWait += entry.waitMinutes;
                    allocatedWithWait++;
                    if (entry.waitMinutes > longestWait) {
                        longestWait = entry.waitMinutes;
                        longestEntry = entry;
                    }
                }
            } else if (registration.getStatus() == RegistrationStatus.VIP_WAITING) {
                waiting++;
            } else if (registration.getStatus() == RegistrationStatus.CANCELLED) {
                cancelled++;
            }

            int tierIndex = entry.profile.getTier().ordinal();
            tierRequests[tierIndex]++;
            if (wasAllocated) {
                tierAllocated[tierIndex]++;
                if (entry.waitMinutes >= 0) {
                    tierWaitTotal[tierIndex] += entry.waitMinutes;
                    tierWaitCount[tierIndex]++;
                }
            }

            int roomIndex = roomTypeIndex(registration.getRequestedRoomType());
            if (roomIndex >= 0) {
                roomRequests[roomIndex]++;
                if (wasAllocated) {
                    roomAllocated[roomIndex]++;
                }
            }
        }

        LoyaltyTier highestDemandTier = findHighestDemandTier(tierRequests);
        RoomType highestDemandRoom = findHighestDemandRoom(roomRequests);
        double successRate = (double) allocated * 100.0 / entries.length;
        long averageWait = allocatedWithWait == 0
                ? -1
                : Math.round((double) totalAllocatedWait / allocatedWithWait);

        printSection("KEY ALLOCATION INDICATORS", divider);
        printTwoColumnHeader("Indicator", "Result", 31);
        printTwoColumnRow("Matching VIP Requests", String.valueOf(entries.length), 31);
        printTwoColumnRow("Successfully Allocated", String.valueOf(allocated), 31);
        printTwoColumnRow("Currently VIP Waiting", String.valueOf(waiting), 31);
        printTwoColumnRow("Cancelled Requests", String.valueOf(cancelled), 31);
        printTwoColumnRow("Allocation Success Rate", String.format("%.1f%%", successRate), 31);
        printTwoColumnRow("Average Allocation Wait", averageWait < 0 ? "-" : formatWaitingTime(averageWait), 31);
        printTwoColumnRow("Longest Allocation Wait", longestEntry == null
                ? "-"
                : formatWaitingTime(longestWait) + " - " + longestEntry.registration.getRegistrationId()
                + " / " + longestEntry.registration.getGuest().getName(), 31);
        printTwoColumnRow("Highest Demand VIP Tier", highestDemandTier == null ? "-" : highestDemandTier.toString(), 31);
        printTwoColumnRow("Highest Demand Room Type", highestDemandRoom == null ? "-" : formatRoomType(highestDemandRoom.name()), 31);

        printSection("VIP REQUEST DETAIL", divider);
        System.out.printf("%-4s  %-7s  %-24s  %-10s  %-16s  %-13s  %-16s  %-16s%n",
                "No.", "Reg ID", "Guest (ID)", "Tier", "Room Request", "Status", "Request Time", "Waiting Time");
        System.out.println("-".repeat(120));

        for (int i = 0; i < entries.length; i++) {
            AllocationEntry entry = entries[i];
            WalkInRegistration registration = entry.registration;
            String guestDisplay = registration.getGuest().getName() + " (" + registration.getGuest().getGuestId() + ")";
            System.out.printf("%-4d  %-7s  %-24s  %-10s  %-16s  %-13s  %-16s  %-16s%n",
                    i + 1,
                    registration.getRegistrationId(),
                    shorten(guestDisplay, 24),
                    entry.profile.getTier(),
                    shorten(formatRoomType(registration.getRequestedRoomType()), 16),
                    shorten(registration.getStatus().toString(), 13),
                    registration.getRegistrationTime().format(DATE_TIME_FORMAT),
                    entry.waitMinutes < 0 ? "-" : formatWaitingTime(entry.waitMinutes));
        }

        printSection("TIER PERFORMANCE", divider);
        System.out.printf("%-14s   %10s   %11s   %14s   %-20s%n",
                "Tier", "Requests", "Allocated", "Not Allocated", "Avg Wait");
        System.out.println("-".repeat(85));
        for (LoyaltyTier tier : new LoyaltyTier[]{LoyaltyTier.DIAMOND, LoyaltyTier.PLATINUM, LoyaltyTier.ELITE}) {
            int index = tier.ordinal();
            if (tierRequests[index] == 0) {
                continue;
            }
            String average = tierWaitCount[index] == 0
                    ? "-"
                    : formatWaitingTime(Math.round((double) tierWaitTotal[index] / tierWaitCount[index]));
            System.out.printf("%-14s   %10d   %11d   %14d   %-20s%n",
                    tier,
                    tierRequests[index],
                    tierAllocated[index],
                    tierRequests[index] - tierAllocated[index],
                    average);
        }

        printSection("ROOM TYPE PERFORMANCE", divider);
        System.out.printf("%-24s   %10s   %11s   %17s%n",
                "Requested Room Type", "Requests", "Allocated", "Allocation Rate");
        System.out.println("-".repeat(76));
        for (RoomType roomType : RoomType.values()) {
            int index = roomType.ordinal();
            if (roomRequests[index] == 0) {
                continue;
            }
            double rate = (double) roomAllocated[index] * 100.0 / roomRequests[index];
            System.out.printf("%-24s   %10d   %11d   %16.1f%%%n",
                    formatRoomType(roomType.name()),
                    roomRequests[index],
                    roomAllocated[index],
                    rate);
        }

        printSection("MANAGEMENT INTERPRETATION", divider);
        if (averageWait >= 60) {
            System.out.println("Average VIP allocation waiting time is above 1 hour, indicating a significant room-readiness delay.");
        } else if (waiting > 0) {
            System.out.println(waiting + " VIP guest(s) are still waiting for room allocation, so current room readiness requires attention.");
        } else {
            System.out.println("No VIP guest is currently waiting and the observed allocation waiting time is under control.");
        }

        printSection("RECOMMENDED ACTION", divider);
        if (averageWait >= 60) {
            System.out.println("Coordinate earlier room preparation with Housekeeping and prioritise ready rooms for high-priority VIP requests.");
        } else if (waiting > 0) {
            System.out.println("Review the requested room types of waiting VIPs and prepare suitable rooms before the queue grows further.");
        } else {
            System.out.println("Maintain the current priority-allocation process and continue monitoring room readiness and VIP waiting time.");
        }

        System.out.println(border);
    }

    private void printSection(String title, String divider) {
        System.out.println();
        System.out.println(divider);
        System.out.println(title);
        System.out.println(divider);
    }

    private void printCentered(String text, int width) {
        int padding = Math.max(0, (width - text.length()) / 2);
        System.out.println(" ".repeat(padding) + text);
    }

    private void printKeyValue(String label, String value, int labelWidth) {
        System.out.printf("%-" + labelWidth + "s : %s%n", label, value);
    }

    private void printTwoColumnHeader(String leftHeader, String rightHeader, int leftWidth) {
        System.out.printf("%-" + leftWidth + "s  %s%n", leftHeader, rightHeader);
        System.out.printf("%-" + leftWidth + "s  %s%n", "-".repeat(leftWidth), "-".repeat(76));
    }

    private void printTwoColumnRow(String leftValue, String rightValue, int leftWidth) {
        System.out.printf("%-" + leftWidth + "s  %s%n", leftValue, rightValue);
    }

    private String formatWaitingTime(long totalMinutes) {

        if (totalMinutes < 0) {
            return "-";
        }

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

    private LoyaltyProfile findProfile(LoyaltyProfile[] profiles, String guestId) {
        for (LoyaltyProfile profile : profiles) {
            if (profile != null && profile.getGuestId() != null && profile.getGuestId().equalsIgnoreCase(guestId)) {
                return profile;
            }
        }
        return null;
    }

    private Booking findBookingForRegistration(
            WalkInRegistration registration,
            Booking[] bookings) {

        if (registration == null
                || registration.getGuest() == null
                || registration.getCheckInDateTime() == null) {
            return null;
        }

        for (Booking booking : bookings) {

            if (booking == null
                    || booking.getGuest() == null
                    || booking.getGuest().getGuestId() == null
                    || booking.getPayment() == null
                    || booking.getPayment().getDateTime() == null) {
                continue;
            }

            boolean sameGuest
                    = booking.getGuest()
                            .getGuestId()
                            .equalsIgnoreCase(
                                    registration.getGuest().getGuestId());

            boolean sameCheckIn
                    = booking.getPayment()
                            .getDateTime()
                            .equals(
                                    registration.getCheckInDateTime());

            if (sameGuest && sameCheckIn) {
                return booking;
            }
        }

        return null;
    }

    private long calculateWaitMinutes(WalkInRegistration registration) {
        if (registration.getRegistrationTime() == null) {
            return -1;
        }
        LocalDateTime end;
        if (registration.getCheckInDateTime() != null) {
            end = registration.getCheckInDateTime();
        } else if (registration.getStatus() == RegistrationStatus.VIP_WAITING) {
            end = LocalDateTime.now();
        } else {
            return -1;
        }
        return Math.max(0, Duration.between(registration.getRegistrationTime(), end).toMinutes());
    }

    private boolean matchesKeyword(AllocationEntry entry, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String value = keyword.trim().toLowerCase();
        WalkInRegistration registration = entry.registration;
        return registration.getRegistrationId().toLowerCase().contains(value) || registration.getGuest().getGuestId().toLowerCase().contains(value) || registration.getGuest().getName().toLowerCase().contains(value) || getConfirmation(entry).toLowerCase().contains(value);
    }

    private boolean matchesRoomType(WalkInRegistration registration, String roomTypeFilter) {
        return roomTypeFilter == null || roomTypeFilter.isBlank() || registration.getRequestedRoomType().equalsIgnoreCase(roomTypeFilter);
    }

    private boolean matchesStatus(RegistrationStatus status, String statusFilter) {
        String normalized = normalizeStatus(statusFilter);
        if (ALL.equals(normalized)) {
            return true;
        }
        return status != null && status.name().equalsIgnoreCase(normalized);
    }

    private boolean matchesDate(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return (startDate == null || !date.isBefore(startDate)) && (endDate == null || !date.isAfter(endDate));
    }

    private void selectionSort(AllocationEntry[] entries, int sortOption) {
        for (int i = 0; i < entries.length - 1; i++) {
            int selected = i;
            for (int j = i + 1; j < entries.length; j++) {
                if (comesBefore(entries[j], entries[selected], sortOption)) {
                    selected = j;
                }
            }
            AllocationEntry temporary = entries[i];
            entries[i] = entries[selected];
            entries[selected] = temporary;
        }
    }

    private boolean comesBefore(AllocationEntry first, AllocationEntry second, int sortOption) {
        switch (sortOption) {
            case 2:
                if (first.waitMinutes != second.waitMinutes) {
                    return first.waitMinutes > second.waitMinutes;
                }
                break;
            case 3:
                return first.registration.getRegistrationTime().isAfter(second.registration.getRegistrationTime());
            case 4:
                int roomCompare = first.registration.getRequestedRoomType().compareToIgnoreCase(second.registration.getRequestedRoomType());
                if (roomCompare != 0) {
                    return roomCompare < 0;
                }
                break;
            case 1:
            default:
                int firstPriority = first.profile.getTier().getPriority();
                int secondPriority = second.profile.getTier().getPriority();
                if (firstPriority != secondPriority) {
                    return firstPriority > secondPriority;
                }
                return first.registration.getRegistrationTime().isBefore(second.registration.getRegistrationTime());
        }
        return first.registration.getRegistrationTime().isBefore(second.registration.getRegistrationTime());
    }

    private LoyaltyTier findHighestDemandTier(int[] counts) {
        LoyaltyTier best = null;
        int bestCount = 0;
        for (LoyaltyTier tier : LoyaltyTier.values()) {
            if (counts[tier.ordinal()] > bestCount) {
                best = tier;
                bestCount = counts[tier.ordinal()];
            }
        }
        return best;
    }

    private RoomType findHighestDemandRoom(int[] counts) {
        RoomType best = null;
        int bestCount = 0;
        for (RoomType roomType : RoomType.values()) {
            if (counts[roomType.ordinal()] > bestCount) {
                best = roomType;
                bestCount = counts[roomType.ordinal()];
            }
        }
        return best;
    }

    private int roomTypeIndex(String roomType) {
        if (roomType == null) {
            return -1;
        }
        try {
            return RoomType.valueOf(roomType.toUpperCase()).ordinal();
        } catch (IllegalArgumentException exception) {
            return -1;
        }
    }

    private String getRoomNumber(AllocationEntry entry) {
        return entry.booking == null || entry.booking.getRoom() == null ? "-" : entry.booking.getRoom().getRoomNumber();
    }

    private String getConfirmation(AllocationEntry entry) {
        return entry.booking == null || entry.booking.getConfirmationNo() == null ? "-" : entry.booking.getConfirmationNo();
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase(ALL)) {
            return ALL;
        }
        return value.trim().toUpperCase();
    }

    private String displayFilter(String value) {
        return value == null || value.isBlank() ? ALL : value.trim();
    }

    private String formatPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return ALL;
        }
        return (startDate == null ? "Beginning" : startDate) + " to " + (endDate == null ? "Present" : endDate);
    }

    private String sortDescription(int option) {
        switch (option) {
            case 2:
                return "Allocation Waiting Time (Longest First)";
            case 3:
                return "Registration Time (Latest First)";
            case 4:
                return "Requested Room Type (A-Z)";
            case 1:
            default:
                return "VIP Tier Priority, then Earliest Registration";
        }
    }

    private String formatRoomType(String roomType) {
        return roomType == null ? "-" : roomType.replace('_', ' ');
    }

    private String shorten(String value, int maximumLength) {
        if (value == null) {
            return "-";
        }
        return value.length() <= maximumLength ? value : value.substring(0, maximumLength - 3) + "...";
    }

    private static class AllocationEntry {
        private final WalkInRegistration registration;
        private final LoyaltyProfile profile;
        private final Booking booking;
        private final long waitMinutes;

        private AllocationEntry(WalkInRegistration registration, LoyaltyProfile profile, Booking booking, long waitMinutes) {
            this.registration = registration;
            this.profile = profile;
            this.booking = booking;
            this.waitMinutes = waitMinutes;
        }
    }
}