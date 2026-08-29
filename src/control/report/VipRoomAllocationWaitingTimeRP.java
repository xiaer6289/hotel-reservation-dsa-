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
        final int reportWidth = calculateReportWidth(entries);
        String border = "=".repeat(reportWidth);

        System.out.println("\n" + border);
        printCentered("VIP ROOM ALLOCATION & WAITING TIME REPORT", reportWidth);
        printCentered("Operational Performance Report", reportWidth);
        System.out.println(border);

        printSection("REPORT INFORMATION");
        printWrappedKeyValue("Generated On", LocalDateTime.now().format(DATE_TIME_FORMAT), 18, reportWidth);
        printWrappedKeyValue("Purpose", "Monitor VIP waiting time and evaluate priority-based room allocation performance.", 18, reportWidth);
        printWrappedKeyValue("Hotel Value", "Highlights long waits, room shortages and allocation bottlenecks for faster operational action.", 18, reportWidth);
        System.out.println();

        printSection("REPORT SCOPE");
        printWrappedKeyValue("Report Period", formatPeriod(startDate, endDate), 18, reportWidth);
        printWrappedKeyValue("Keyword", displayFilter(keyword), 18, reportWidth);
        printWrappedKeyValue("Loyalty Tier", tierFilter == null ? ALL : tierFilter.toString(), 18, reportWidth);
        printWrappedKeyValue("Room Type", roomTypeFilter == null ? ALL : formatRoomType(roomTypeFilter), 18, reportWidth);
        printWrappedKeyValue("Status", normalizeStatus(statusFilter), 18, reportWidth);
        printWrappedKeyValue("Minimum Party", minimumGuests == 0 ? ALL : minimumGuests + " guest(s)", 18, reportWidth);
        System.out.println();

        printSection("ANALYSIS METHOD");
        printWrappedKeyValue("Search", "Linear Search", 18, reportWidth);
        printWrappedKeyValue("Sort", "Selection Sort - " + sortDescription(sortOption), 18, reportWidth);
        System.out.println();

        if (entries.length == 0) {
            printSection("REPORT RESULT");
            printWrappedText("No VIP allocation records match the selected report criteria.", 2, reportWidth);
            printWrappedText("The report uses historical and current VIP registrations, so it can run even when the waiting queue is empty.", 2, reportWidth);
            System.out.println();
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
        long averageWait = allocatedWithWait == 0 ? -1 : Math.round((double) totalAllocatedWait / allocatedWithWait);

        printSection("KEY ALLOCATION INDICATORS");
        printWrappedKeyValue("Matching VIP Requests", String.valueOf(entries.length), 29, reportWidth);
        printWrappedKeyValue("Successfully Allocated", String.valueOf(allocated), 29, reportWidth);
        printWrappedKeyValue("Currently VIP Waiting", String.valueOf(waiting), 29, reportWidth);
        printWrappedKeyValue("Cancelled Requests", String.valueOf(cancelled), 29, reportWidth);
        printWrappedKeyValue("Allocation Success Rate", String.format("%.1f%%", successRate), 29, reportWidth);
        printWrappedKeyValue("Average Allocation Wait", averageWait < 0 ? "-" : formatWaitingTime(averageWait), 29, reportWidth);
        printWrappedKeyValue("Longest Allocation Wait", longestEntry == null ? "-" : formatWaitingTime(longestWait) + " - " + longestEntry.registration.getRegistrationId() + " / " + longestEntry.registration.getGuest().getName(), 29, reportWidth);
        printWrappedKeyValue("Highest Demand VIP Tier", highestDemandTier == null ? "-" : highestDemandTier.toString(), 29, reportWidth);
        printWrappedKeyValue("Highest Demand Room Type", highestDemandRoom == null ? "-" : formatRoomType(highestDemandRoom.name()), 29, reportWidth);
        System.out.println();

        printSection("VIP REQUEST DETAIL");
        int guestNameWidth = getGuestNameWidth(entries);
        int roomWidth = getRoomWidth(entries);
        int statusWidth = getStatusWidth(entries);
        int waitWidth = getWaitWidth(entries);
        String detailFormat = "%-3s  %-7s  %-8s  %-" + guestNameWidth + "s  %-10s  %-" + roomWidth + "s  %-" + statusWidth + "s  %-16s  %-" + waitWidth + "s%n";
        System.out.printf(detailFormat, "No.", "Reg ID", "Guest ID", "Guest Name", "Tier", "Room Request", "Status", "Request Time", "Waiting Time");
        int detailTableWidth = 3 + 7 + 8 + guestNameWidth + 10 + roomWidth + statusWidth + 16 + waitWidth + (8 * 2);
        System.out.println("-".repeat(detailTableWidth));

        for (int i = 0; i < entries.length; i++) {
            AllocationEntry entry = entries[i];
            WalkInRegistration registration = entry.registration;
            System.out.printf(detailFormat,
                    i + 1,
                    registration.getRegistrationId(),
                    registration.getGuest().getGuestId(),
                    registration.getGuest().getName(),
                    entry.profile.getTier(),
                    formatRoomType(registration.getRequestedRoomType()),
                    registration.getStatus().toString(),
                    registration.getRegistrationTime().format(DATE_TIME_FORMAT),
                    entry.waitMinutes < 0 ? "-" : formatWaitingTime(entry.waitMinutes));
        }
        System.out.println();

        printSection("TIER PERFORMANCE");
        System.out.printf("%-14s  %10s  %11s  %13s  %-20s%n", "Tier", "Requests", "Allocated", "Not Allocated", "Average Wait");
        System.out.println("-".repeat(76));
        for (LoyaltyTier tier : new LoyaltyTier[]{LoyaltyTier.DIAMOND, LoyaltyTier.PLATINUM, LoyaltyTier.ELITE}) {
            int index = tier.ordinal();
            if (tierRequests[index] == 0) {
                continue;
            }
            String average = tierWaitCount[index] == 0 ? "-" : formatWaitingTime(Math.round((double) tierWaitTotal[index] / tierWaitCount[index]));
            System.out.printf("%-14s  %10d  %11d  %13d  %-20s%n",
                    tier,
                    tierRequests[index],
                    tierAllocated[index],
                    tierRequests[index] - tierAllocated[index],
                    average);
        }
        System.out.println();

        printSection("ROOM TYPE PERFORMANCE");
        System.out.printf("%-24s  %10s  %11s  %17s%n", "Requested Room Type", "Requests", "Allocated", "Allocation Rate");
        System.out.println("-".repeat(68));
        for (RoomType roomType : RoomType.values()) {
            int index = roomType.ordinal();
            if (roomRequests[index] == 0) {
                continue;
            }
            double rate = (double) roomAllocated[index] * 100.0 / roomRequests[index];
            System.out.printf("%-24s  %10d  %11d  %16.1f%%%n",
                    formatRoomType(roomType.name()),
                    roomRequests[index],
                    roomAllocated[index],
                    rate);
        }
        System.out.println();

        printSection("MANAGEMENT INTERPRETATION");
        if (averageWait >= 60) {
            printWrappedText("Average VIP allocation waiting time is above 1 hour, indicating a significant room-readiness delay.", 2, reportWidth);
        } else if (waiting > 0) {
            printWrappedText(waiting + " VIP guest(s) are still waiting for room allocation, so current room readiness requires attention.", 2, reportWidth);
        } else {
            printWrappedText("No VIP guest is currently waiting and the observed allocation waiting time is under control.", 2, reportWidth);
        }
        System.out.println();

        printSection("RECOMMENDED ACTION");
        if (averageWait >= 60) {
            printWrappedText("Coordinate earlier room preparation with Housekeeping and prioritise ready rooms for high-priority VIP requests.", 2, reportWidth);
        } else if (waiting > 0) {
            printWrappedText("Review the requested room types of waiting VIPs and prepare suitable rooms before the queue grows further.", 2, reportWidth);
        } else {
            printWrappedText("Maintain the current priority-allocation process and continue monitoring room readiness and VIP waiting time.", 2, reportWidth);
        }

        System.out.println();
        System.out.println(border);
    }

    private int calculateReportWidth(AllocationEntry[] entries) {
        int guestNameWidth = getGuestNameWidth(entries);
        int roomWidth = getRoomWidth(entries);
        int statusWidth = getStatusWidth(entries);
        int waitWidth = getWaitWidth(entries);
        int detailWidth = 3 + 7 + 8 + guestNameWidth + 10 + roomWidth + statusWidth + 16 + waitWidth + (8 * 2);
        return Math.max(116, detailWidth);
    }

    private int getGuestNameWidth(AllocationEntry[] entries) {
        int width = "Guest Name".length();
        for (AllocationEntry entry : entries) {
            if (entry != null && entry.registration != null && entry.registration.getGuest() != null) {
                width = Math.max(width, safeText(entry.registration.getGuest().getName()).length());
            }
        }
        return Math.max(width, 14);
    }

    private int getRoomWidth(AllocationEntry[] entries) {
        int width = "Room Request".length();
        for (AllocationEntry entry : entries) {
            if (entry != null && entry.registration != null) {
                width = Math.max(width, formatRoomType(entry.registration.getRequestedRoomType()).length());
            }
        }
        return Math.max(width, 14);
    }

    private int getStatusWidth(AllocationEntry[] entries) {
        int width = "Status".length();
        for (AllocationEntry entry : entries) {
            if (entry != null && entry.registration != null && entry.registration.getStatus() != null) {
                width = Math.max(width, entry.registration.getStatus().toString().length());
            }
        }
        return Math.max(width, 11);
    }

    private int getWaitWidth(AllocationEntry[] entries) {
        int width = "Waiting Time".length();
        for (AllocationEntry entry : entries) {
            if (entry != null) {
                String value = entry.waitMinutes < 0 ? "-" : formatWaitingTime(entry.waitMinutes);
                width = Math.max(width, value.length());
            }
        }
        return Math.max(width, 16);
    }

    private void printSection(String title) {
        System.out.println();
        System.out.println("[ " + title + " ]");
        System.out.println();
    }

    private void printCentered(String text, int width) {
        int padding = Math.max(0, (width - text.length()) / 2);
        System.out.println(" ".repeat(padding) + text);
    }

    private void printWrappedKeyValue(String label, String value, int labelWidth, int reportWidth) {
        String safeLabel = safeText(label);
        String safeValue = safeText(value);
        String prefix = String.format("%-" + labelWidth + "s : ", safeLabel);
        String continuation = " ".repeat(labelWidth + 3);
        int availableWidth = Math.max(20, reportWidth - prefix.length());
        printWrappedWithPrefix(prefix, continuation, safeValue, availableWidth);
    }

    private void printWrappedWithPrefix(String firstPrefix, String continuationPrefix, String text, int availableWidth) {
        String[] words = text.trim().isEmpty() ? new String[]{"-"} : text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        boolean firstLine = true;

        for (String word : words) {
            if (line.length() > 0 && line.length() + 1 + word.length() > availableWidth) {
                System.out.println((firstLine ? firstPrefix : continuationPrefix) + line);
                firstLine = false;
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        System.out.println((firstLine ? firstPrefix : continuationPrefix) + line);
    }

    private void printWrappedText(String text, int indent, int reportWidth) {
        String prefix = " ".repeat(Math.max(0, indent));
        int availableWidth = Math.max(20, reportWidth - prefix.length());
        printWrappedWithPrefix(prefix, prefix, safeText(text), availableWidth);
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatWaitingTime(long totalMinutes) {
        if (totalMinutes < 0) {
            return "-";
        }
        if (totalMinutes < 60) {
            return totalMinutes + (totalMinutes == 1 ? " minute" : " minutes");
        }

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        String result = hours + (hours == 1 ? " hour" : " hours");
        if (minutes > 0) {
            result += " " + minutes + (minutes == 1 ? " minute" : " minutes");
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

    private Booking findBookingForRegistration(WalkInRegistration registration, Booking[] bookings) {
        if (registration == null || registration.getGuest() == null || registration.getCheckInDateTime() == null) {
            return null;
        }

        for (Booking booking : bookings) {
            if (booking == null || booking.getGuest() == null || booking.getGuest().getGuestId() == null || booking.getPayment() == null || booking.getPayment().getDateTime() == null) {
                continue;
            }

            boolean sameGuest = booking.getGuest().getGuestId().equalsIgnoreCase(registration.getGuest().getGuestId());
            boolean sameCheckIn = booking.getPayment().getDateTime().equals(registration.getCheckInDateTime());

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