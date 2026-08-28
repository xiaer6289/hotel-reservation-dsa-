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
 * Management report for VIP priority room-allocation performance.
 * Uses historical/current registrations plus booking records rather than only
 * the current VIP waiting heap. Linear search/filtering and Selection Sort are
 * implemented explicitly to satisfy the report-generation requirements.
 *
 * @author Low Enn Toong
 */
public class VipPriorityAllocationPerformanceRP {
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
        System.out.println("\n" + "=".repeat(142));
        System.out.println("                                      VIP ROOM ALLOCATION & WAITING TIME REPORT");
        System.out.println("=".repeat(142));
        System.out.println("REPORT PURPOSE");
        System.out.println("To monitor VIP waiting time and evaluate the effectiveness " + "of priority-based room allocation." );
        System.out.println();
        System.out.println("HOTEL VALUE");
        System.out.println("Helps Front Desk and hotel management identify long VIP " + "waiting times, room shortages and allocation bottlenecks.");
        System.out.println("-".repeat(142));
        System.out.println("Generated On        : " + LocalDateTime.now().format(DATE_TIME_FORMAT));
        System.out.println("Report Period       : " + formatPeriod(startDate, endDate));
        System.out.println("Search Keyword      : " + displayFilter(keyword));
        System.out.println("Loyalty Tier        : " + (tierFilter == null ? ALL : tierFilter));
        System.out.println("Requested Room Type : " + (roomTypeFilter == null ? ALL : formatRoomType(roomTypeFilter)));
        System.out.println("Registration Status : " + normalizeStatus(statusFilter));
        System.out.println("Minimum Party Size  : " + (minimumGuests == 0 ? ALL : minimumGuests + " guest(s)"));
        System.out.println("Search Technique    : Linear Search with Multiple Criteria Filters");
        System.out.println("Sorting Technique   : Selection Sort - " + sortDescription(sortOption));
        System.out.println("-".repeat(142));

        if (entries.length == 0) {
            System.out.println("No VIP allocation records match the selected report criteria.");
            System.out.println("The report can still be generated even when the current VIP waiting queue is empty.");
            System.out.println("=".repeat(142));
            return;
        }

        System.out.printf(
                "%-4s %-7s %-8s %-17s %-10s %-16s %-12s %-6s %-12s %-16s %-9s%n",
                "No.", "Reg ID", "Guest ID", "Guest Name", "Tier", "Room Request", "Status", "Room", "Confirm No.", "Request Time", "Waiting Time");
        System.out.println("-".repeat(142));

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

        for (int i = 0; i < entries.length; i++) {
            AllocationEntry entry = entries[i];
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

            System.out.printf(
                    "%-4d %-7s %-8s %-17s %-10s %-16s %-12s %-6s %-12s %-16s %-9s%n",
                    i + 1,
                    registration.getRegistrationId(),
                    registration.getGuest().getGuestId(),
                    shorten(registration.getGuest().getName(), 17),
                    entry.profile.getTier(),
                    shorten(formatRoomType(registration.getRequestedRoomType()), 16),
                    shorten(registration.getStatus().toString(), 12),
                    getRoomNumber(entry),
                    getConfirmation(entry),
                    registration.getRegistrationTime().format(DATE_TIME_FORMAT),
                    entry.waitMinutes < 0 ? "-": formatWaitingTime(entry.waitMinutes));
        }

        System.out.println("-".repeat(142));
        System.out.println("TIER PERFORMANCE SUMMARY");
        System.out.printf("%-12s %-10s %-11s %-15s %-20s%n", "Tier", "Requests", "Allocated", "Not Allocated", "Avg Allocation Wait");

        for (LoyaltyTier tier : new LoyaltyTier[]{LoyaltyTier.DIAMOND, LoyaltyTier.PLATINUM, LoyaltyTier.ELITE}) {
            int index = tier.ordinal();
            if (tierRequests[index] == 0) {
                continue;
            }
            String average = tierWaitCount[index] == 0? "-": formatWaitingTime(Math.round((double) tierWaitTotal[index] / tierWaitCount[index]));
            System.out.printf("%-12s %-10d %-11d %-15d %-20s%n", tier, tierRequests[index], tierAllocated[index], tierRequests[index] - tierAllocated[index], average);
        }

        System.out.println("\nROOM TYPE ALLOCATION SUMMARY");
        System.out.printf("%-22s %-10s %-11s %-16s%n", "Requested Room Type", "Requests", "Allocated", "Allocation Rate");
        for (RoomType roomType : RoomType.values()) {
            int index = roomType.ordinal();
            if (roomRequests[index] == 0) {
                continue;
            }
            double rate = (double) roomAllocated[index] * 100.0 / roomRequests[index];
            String rateText = String.format("%.1f%%", rate);
            System.out.printf("%-22s %-10d %-11d %-16s%n", formatRoomType(roomType.name()), roomRequests[index], roomAllocated[index], rateText);
        }

        LoyaltyTier highestDemandTier = findHighestDemandTier(tierRequests);
        RoomType highestDemandRoom = findHighestDemandRoom(roomRequests);
        double successRate = entries.length == 0 ? 0.0 : (double) allocated * 100.0 / entries.length;

        System.out.println("\nMANAGEMENT SUMMARY");
        System.out.println("Matching VIP Requests       : " + entries.length);
        System.out.println("Successfully Allocated      : " + allocated);
        System.out.println("Currently VIP Waiting       : " + waiting);
        System.out.println("Cancelled Requests          : " + cancelled);
        System.out.printf("Allocation Success Rate     : %.1f%%%n", successRate);
        
        long averageWait =
        allocatedWithWait == 0
        ? -1
        : Math.round((double) totalAllocatedWait/ allocatedWithWait);

        System.out.println( "Average Allocation Wait     : " + (averageWait < 0 ? "-" : formatWaitingTime(averageWait)));
        System.out.println("Longest Allocation Wait     : "+ (longestEntry == null ? "-" : formatWaitingTime(longestWait) + " - " + longestEntry.registration.getRegistrationId() + " / " + longestEntry.registration.getGuest().getName()));
        System.out.println("Highest Demand VIP Tier     : " + (highestDemandTier == null ? "-" : highestDemandTier));
        System.out.println("Highest Demand Room Type    : " + (highestDemandRoom == null ? "-" : formatRoomType(highestDemandRoom.name())));

        System.out.println("\nMANAGEMENT SUGGESTION");

if (allocatedWithWait > 0
        && ((double) totalAllocatedWait / allocatedWithWait) >= 60) {

    System.out.println(
            "Average VIP waiting time exceeds 1 hour."
    );

    System.out.println(
            "Suggestion: Improve room preparation and prioritise "
            + "ready rooms for VIP requests."
    );

} else if (waiting > 0) {

    System.out.println(
            "There are still VIP guests waiting for room allocation.");

    System.out.println("Suggestion: Review requested room types and coordinate " + "with Housekeeping to prepare suitable rooms earlier.");

} else {

    System.out.println("VIP allocation waiting time is currently under control.");

    System.out.println( "Suggestion: Maintain the current priority allocation " + "and room readiness process.");
}

        System.out.println("=".repeat(142));
    }

    private String formatWaitingTime(long totalMinutes) {

            if (totalMinutes < 0) {
                return "-";
            }

            if (totalMinutes < 60) {
                return totalMinutes + " min";
            }

            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;

            if (minutes == 0) {
                return hours + (hours == 1 ? " hr" : " hrs");
            }

            return hours
                    + (hours == 1 ? " hr " : " hrs ")
                    + minutes
                    + " min";
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