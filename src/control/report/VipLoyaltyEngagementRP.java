package control.report;

import control.VipPriorityController;
import entity.Booking;
import entity.Guest;
import entity.LoyaltyProfile;
import entity.LoyaltyTier;
import entity.RegistrationStatus;
import entity.RoomType;
import entity.WalkInRegistration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Management report for VIP loyalty engagement.
 * Combines loyalty profiles, booking history and current VIP activity.
 * Linear search/filtering and Selection Sort are implemented explicitly.
 *
 * @author Low Enn Toong
 */
public class VipLoyaltyEngagementRP {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String ALL = "ALL";

    public void generateReport(VipPriorityController controller, String keyword, LoyaltyTier tierFilter, String activityFilter, int minimumCompletedStays, String roomTypeFilter, LocalDate startDate, LocalDate endDate, int sortOption) {
        LoyaltyProfile[] profiles = controller.getAllVipProfiles();
        Booking[] bookings = controller.getAllBookingsForReport();
        WalkInRegistration[] registrations = controller.getAllRegistrationsForReport();
        Booking[] currentVipBookings = controller.getCurrentVipRoomBookings();
        LoyaltyEntry[] entries = searchAndFilter(controller, profiles, bookings, registrations, currentVipBookings, keyword, tierFilter, activityFilter, minimumCompletedStays, roomTypeFilter, startDate, endDate);
        selectionSort(entries, sortOption);
        printReport(entries, bookings, keyword, tierFilter, activityFilter, minimumCompletedStays, roomTypeFilter, startDate, endDate, sortOption);
    }

    private LoyaltyEntry[] searchAndFilter(VipPriorityController controller, LoyaltyProfile[] profiles, Booking[] bookings, WalkInRegistration[] registrations, Booking[] currentVipBookings, String keyword, LoyaltyTier tierFilter, String activityFilter, int minimumCompletedStays, String roomTypeFilter, LocalDate startDate, LocalDate endDate) {
        LoyaltyEntry[] temporary = new LoyaltyEntry[profiles.length];
        int count = 0;
        boolean stayHistoryFilterUsed = roomTypeFilter != null || startDate != null || endDate != null;

        for (LoyaltyProfile profile : profiles) {
            if (profile == null || profile.getTier() == null) {
                continue;
            }

            Guest guest = controller.findGuestById(profile.getGuestId());
            if (guest == null) {
                continue;
            }

            String activity = findActivity(guest.getGuestId(), registrations, currentVipBookings);
            Booking lastBooking = findLatestBooking(guest.getGuestId(), bookings);
            int periodStayCount = countMatchingBookings(guest.getGuestId(), bookings, roomTypeFilter, startDate, endDate);
            boolean keywordMatches = matchesKeyword(guest, bookings, keyword);

            if (!keywordMatches || (tierFilter != null && profile.getTier() != tierFilter) || !matchesActivity(activity, activityFilter) || profile.getCompletedStays() < minimumCompletedStays || (stayHistoryFilterUsed && periodStayCount == 0)) {
                continue;
            }

            temporary[count++] = new LoyaltyEntry(guest, profile, activity, periodStayCount, lastBooking);
        }

        LoyaltyEntry[] result = new LoyaltyEntry[count];
        System.arraycopy(temporary, 0, result, 0, count);
        return result;
    }

    private void printReport(LoyaltyEntry[] entries, Booking[] bookings, String keyword, LoyaltyTier tierFilter, String activityFilter, int minimumCompletedStays, String roomTypeFilter, LocalDate startDate, LocalDate endDate, int sortOption) {
        final int reportWidth = 118;
        String border = "=".repeat(reportWidth);
        String divider = "-".repeat(reportWidth);

        System.out.println("\n" + border);
        printCentered("VIP LOYALTY ENGAGEMENT REPORT", reportWidth);
        printCentered("Management Report", reportWidth);
        System.out.println(border);

        printKeyValue("Generated On", LocalDateTime.now().format(DATE_TIME_FORMAT), 16);
        printKeyValue("Purpose", "Identify repeat VIP guests and monitor progress toward the next loyalty tier.", 16);
        printKeyValue("Hotel Value", "Supports guest retention and helps staff recognise VIPs who are close to a tier upgrade.", 16);

        printSection("REPORT SCOPE", divider);
        printTwoColumnHeader("Parameter", "Selected Value", 20);
        printTwoColumnRow("Stay Period", formatPeriod(startDate, endDate), 20);
        printTwoColumnRow("Keyword", displayFilter(keyword), 20);
        printTwoColumnRow("Loyalty Tier", tierFilter == null ? ALL : tierFilter.toString(), 20);
        printTwoColumnRow("Activity", normalizeActivity(activityFilter), 20);
        printTwoColumnRow("Minimum Stays", minimumCompletedStays == 0 ? ALL : String.valueOf(minimumCompletedStays), 20);
        printTwoColumnRow("Room Type", roomTypeFilter == null ? ALL : formatRoomType(roomTypeFilter), 20);

        printSection("ANALYSIS METHOD", divider);
        printTwoColumnHeader("Method", "Implementation", 20);
        printTwoColumnRow("Search", "Linear Search", 20);
        printTwoColumnRow("Sort", "Selection Sort - " + sortDescription(sortOption), 20);

        if (entries.length == 0) {
            printSection("REPORT RESULT", divider);
            System.out.println("No VIP loyalty records match the selected report criteria.");
            System.out.println("The report uses VIP profiles and booking history, so it can still run when no VIP is currently waiting.");
            System.out.println(border);
            return;
        }

        int[] tierCounts = new int[LoyaltyTier.values().length];
        int totalCompletedStays = 0;
        int totalPeriodStays = 0;
        int nearUpgradeCount = 0;
        LoyaltyEntry mostActive = null;

        for (LoyaltyEntry entry : entries) {
            LoyaltyProfile profile = entry.profile;
            tierCounts[profile.getTier().ordinal()]++;
            totalCompletedStays += profile.getCompletedStays();
            totalPeriodStays += entry.periodStayCount;

            int staysUntilNext = profile.getStaysUntilNextTier();
            if (staysUntilNext > 0 && staysUntilNext <= 2) {
                nearUpgradeCount++;
            }

            if (mostActive == null || profile.getCompletedStays() > mostActive.profile.getCompletedStays()) {
                mostActive = entry;
            }
        }

        printSection("KEY LOYALTY INDICATORS", divider);
        printTwoColumnHeader("Indicator", "Result", 31);
        printTwoColumnRow("Matching VIP Guests", String.valueOf(entries.length), 31);
        printTwoColumnRow("Total Completed Stays", String.valueOf(totalCompletedStays), 31);
        printTwoColumnRow("Matching Period Stays", String.valueOf(totalPeriodStays), 31);
        printTwoColumnRow("Average Completed Stays", String.format("%.1f", (double) totalCompletedStays / entries.length), 31);
        printTwoColumnRow("VIPs Near Next Tier (<=2)", String.valueOf(nearUpgradeCount), 31);
        printTwoColumnRow("Most Active VIP", mostActive == null ? "-" : mostActive.guest.getName() + " (" + mostActive.guest.getGuestId() + ", " + mostActive.profile.getCompletedStays() + " stays)", 31);
        printTwoColumnRow("Tier Distribution", "DIAMOND " + tierCounts[LoyaltyTier.DIAMOND.ordinal()]
                + " | PLATINUM " + tierCounts[LoyaltyTier.PLATINUM.ordinal()]
                + " | ELITE " + tierCounts[LoyaltyTier.ELITE.ordinal()], 31);

        printSection("VIP LOYALTY DETAIL", divider);
        System.out.printf("%-4s  %-9s  %-22s  %-10s  %11s  %12s  %-20s%n",
                "No.", "Guest ID", "Guest Name", "Tier", "Total Stays", "Period Stays", "Next Tier");
        System.out.println("-".repeat(105));

        for (int i = 0; i < entries.length; i++) {
            LoyaltyEntry entry = entries[i];
            LoyaltyProfile profile = entry.profile;
            System.out.printf("%-4d  %-9s  %-22s  %-10s  %11d  %12d  %-20s%n",
                    i + 1,
                    entry.guest.getGuestId(),
                    shorten(entry.guest.getName(), 22),
                    profile.getTier(),
                    profile.getCompletedStays(),
                    entry.periodStayCount,
                    nextTierProgress(profile));
        }

        printSection("LOYALTY TIER DISTRIBUTION", divider);
        System.out.printf("%-14s  %12s  %12s%n", "Tier", "VIP Guests", "Share");
        System.out.println("-".repeat(42));
        for (LoyaltyTier tier : new LoyaltyTier[]{LoyaltyTier.DIAMOND, LoyaltyTier.PLATINUM, LoyaltyTier.ELITE}) {
            int count = tierCounts[tier.ordinal()];
            if (count == 0) {
                continue;
            }
            double share = (double) count * 100.0 / entries.length;
            System.out.printf("%-14s  %12d  %11.1f%%%n", tier, count, share);
        }

        printSection("MANAGEMENT INTERPRETATION", divider);
        if (nearUpgradeCount > 0) {
            System.out.println(nearUpgradeCount + " VIP guest(s) are within 2 completed stays of the next loyalty tier.");
            System.out.println("These guests are strong candidates for targeted retention offers because an upgrade is within reach.");
        } else {
            System.out.println("No matching VIP guest is currently within 2 completed stays of the next loyalty tier.");
            System.out.println("Current loyalty activity should continue to be monitored for future upgrade opportunities.");
        }

        printSection("RECOMMENDED ACTION", divider);
        if (nearUpgradeCount > 0) {
            System.out.println("Offer personalised stay packages or loyalty rewards to guests who are close to a tier upgrade.");
        } else {
            System.out.println("Maintain regular VIP engagement and review completed-stay patterns in the next reporting period.");
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
        System.out.printf("%-" + leftWidth + "s  %s%n", "-".repeat(leftWidth), "-".repeat(72));
    }

    private void printTwoColumnRow(String leftValue, String rightValue, int leftWidth) {
        System.out.printf("%-" + leftWidth + "s  %s%n", leftValue, rightValue);
    }

    private String findActivity(String guestId, WalkInRegistration[] registrations, Booking[] currentVipBookings) {
        for (WalkInRegistration registration : registrations) {
            if (registration != null && registration.getGuest() != null && registration.getGuest().getGuestId().equalsIgnoreCase(guestId) && registration.getStatus() == RegistrationStatus.VIP_WAITING) {
                return "WAITING";
            }
        }

        for (Booking booking : currentVipBookings) {
            if (booking != null && booking.getGuest() != null && booking.getGuest().getGuestId().equalsIgnoreCase(guestId)) {
                return "IN HOUSE";
            }
        }

        return "PROFILE ONLY";
    }

    private Booking findLatestBooking(String guestId, Booking[] bookings) {
        Booking latest = null;

        for (Booking booking : bookings) {
            if (!isBookingForGuest(booking, guestId)
                    || booking.getPayment() == null
                    || booking.getPayment().getDateTime() == null) {
                continue;
            }

            if (latest == null
                    || latest.getPayment() == null
                    || latest.getPayment().getDateTime() == null
                    || latest.getPayment().getDateTime()
                            .isBefore(booking.getPayment().getDateTime())) {
                latest = booking;
            }
        }

        return latest;
    }

    private int countMatchingBookings(
            String guestId,
            Booking[] bookings,
            String roomTypeFilter,
            LocalDate startDate,
            LocalDate endDate) {

        int count = 0;

        for (Booking booking : bookings) {
            if (!isBookingForGuest(booking, guestId)
                    || booking.getPayment() == null
                    || booking.getPayment().getDateTime() == null) {
                continue;
            }

            LocalDate checkInDate
                    = booking.getPayment().getDateTime().toLocalDate();

            boolean matchesRoom
                    = roomTypeFilter == null
                    || booking.getRoom().getRoomType()
                            .equalsIgnoreCase(roomTypeFilter);

            boolean matchesDate
                    = (startDate == null
                    || !checkInDate.isBefore(startDate))
                    && (endDate == null
                    || !checkInDate.isAfter(endDate));

            if (matchesRoom && matchesDate) {
                count++;
            }
        }

        return count;
    }

    private boolean matchesKeyword(Guest guest, Booking[] bookings, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String value = keyword.trim().toLowerCase();
        if (guest.getGuestId().toLowerCase().contains(value) || guest.getName().toLowerCase().contains(value)) {
            return true;
        }
        for (Booking booking : bookings) {
            if (isBookingForGuest(booking, guest.getGuestId()) && booking.getConfirmationNo() != null && booking.getConfirmationNo().toLowerCase().contains(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesActivity(String activity, String activityFilter) {
        String normalized = normalizeActivity(activityFilter);
        return ALL.equals(normalized) || activity.equalsIgnoreCase(normalized);
    }

    private boolean isBookingForGuest(Booking booking, String guestId) {
        return booking != null && booking.getGuest() != null && booking.getGuest().getGuestId() != null && booking.getGuest().getGuestId().equalsIgnoreCase(guestId) && booking.getRoom() != null;
    }

    private void selectionSort(LoyaltyEntry[] entries, int sortOption) {
        for (int i = 0; i < entries.length - 1; i++) {
            int selected = i;
            for (int j = i + 1; j < entries.length; j++) {
                if (comesBefore(entries[j], entries[selected], sortOption)) {
                    selected = j;
                }
            }
            LoyaltyEntry temporary = entries[i];
            entries[i] = entries[selected];
            entries[selected] = temporary;
        }
    }

    private boolean comesBefore(LoyaltyEntry first, LoyaltyEntry second, int sortOption) {
        switch (sortOption) {
            case 2:
                if (first.profile.getCompletedStays() != second.profile.getCompletedStays()) {
                    return first.profile.getCompletedStays() > second.profile.getCompletedStays();
                }
                break;
            case 3:
                LocalDateTime firstDate = lastStayDate(first.lastBooking);
                LocalDateTime secondDate = lastStayDate(second.lastBooking);
                if (firstDate == null) {
                    return false;
                }
                if (secondDate == null) {
                    return true;
                }
                if (!firstDate.equals(secondDate)) {
                    return firstDate.isAfter(secondDate);
                }
                break;
            case 4:
                return first.guest.getName().compareToIgnoreCase(second.guest.getName()) < 0;
            case 5:
                int firstGap = first.profile.getStaysUntilNextTier();
                int secondGap = second.profile.getStaysUntilNextTier();
                if (firstGap == 0) {
                    firstGap = Integer.MAX_VALUE;
                }
                if (secondGap == 0) {
                    secondGap = Integer.MAX_VALUE;
                }
                if (firstGap != secondGap) {
                    return firstGap < secondGap;
                }
                break;
            case 1:
            default:
                int firstPriority = first.profile.getTier().getPriority();
                int secondPriority = second.profile.getTier().getPriority();
                if (firstPriority != secondPriority) {
                    return firstPriority > secondPriority;
                }
                break;
        }
        return first.guest.getGuestId().compareToIgnoreCase(second.guest.getGuestId()) < 0;
    }

    private RoomType findMostUsedRoomType(
            LoyaltyEntry[] entries,
            Booking[] bookings,
            String roomTypeFilter,
            LocalDate startDate,
            LocalDate endDate) {

        int[] counts = new int[RoomType.values().length];

        for (LoyaltyEntry entry : entries) {
            for (Booking booking : bookings) {

                if (!isBookingForGuest(
                        booking,
                        entry.guest.getGuestId())
                        || booking.getPayment() == null
                        || booking.getPayment().getDateTime() == null) {
                    continue;
                }

                LocalDate date
                        = booking.getPayment()
                                .getDateTime()
                                .toLocalDate();

                boolean matchesDate
                        = (startDate == null
                        || !date.isBefore(startDate))
                        && (endDate == null
                        || !date.isAfter(endDate));

                boolean matchesRoom
                        = roomTypeFilter == null
                        || booking.getRoom()
                                .getRoomType()
                                .equalsIgnoreCase(roomTypeFilter);

                if (!matchesDate || !matchesRoom) {
                    continue;
                }

                try {
                    RoomType type = RoomType.valueOf(
                            booking.getRoom()
                                    .getRoomType()
                                    .toUpperCase());

                    counts[type.ordinal()]++;

                } catch (IllegalArgumentException exception) {
                    // Ignore unknown legacy room types in the summary.
                }
            }
        }

        RoomType best = null;
        int bestCount = 0;

        for (RoomType type : RoomType.values()) {
            if (counts[type.ordinal()] > bestCount) {
                best = type;
                bestCount = counts[type.ordinal()];
            }
        }

        return best;
    }

    private LocalDateTime lastStayDate(Booking booking) {
        return booking == null
                || booking.getPayment() == null
                ? null
                : booking.getPayment().getDateTime();
    }

    private String getLastRoom(Booking booking) {
        if (booking == null || booking.getRoom() == null) {
            return "-";
        }
        return booking.getRoom().getRoomNumber() + " " + formatRoomType(booking.getRoom().getRoomType());
    }

    private String nextTierProgress(LoyaltyProfile profile) {
        if (profile.getTier() == LoyaltyTier.DIAMOND) {
            return "MAX TIER";
        }
        return profile.getStaysUntilNextTier() + " to " + profile.getNextTierName();
    }

    private String normalizeActivity(String value) {
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
                return "Completed Stays (Highest First)";
            case 3:
                return "Most Recent Stay (Latest First)";
            case 4:
                return "Guest Name (A-Z)";
            case 5:
                return "Closest to Next Loyalty Tier";
            case 1:
            default:
                return "Loyalty Tier Priority (DIAMOND > PLATINUM > ELITE)";
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

    private static class LoyaltyEntry {
        private final Guest guest;
        private final LoyaltyProfile profile;
        private final String activity;
        private final int periodStayCount;
        private final Booking lastBooking;

        private LoyaltyEntry(Guest guest, LoyaltyProfile profile, String activity, int periodStayCount, Booking lastBooking) {
            this.guest = guest;
            this.profile = profile;
            this.activity = activity;
            this.periodStayCount = periodStayCount;
            this.lastBooking = lastBooking;
        }
    }
}