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
 * Management report for VIP loyalty and stay performance.
 * Combines loyalty profiles, booking history and current VIP activity.
 * Linear search/filtering and Selection Sort are implemented explicitly.
 *
 * @author Low Enn Toong
 */
public class VipLoyaltyStayPerformanceRP {
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
        System.out.println("\n" + "=".repeat(142));
        System.out.println("                                         VIP LOYALTY ENGAGEMENT REPORT");
        
        System.out.println("=".repeat(142));

        System.out.println("REPORT PURPOSE");
        System.out.println("To identify valuable repeat VIP guests and monitor " + "their loyalty tier progress.");
        System.out.println();
        System.out.println("HOTEL VALUE");
        System.out.println("Helps the hotel recognise loyal repeat guests, " + "provide personalised service and identify guests " + "who are close to the next loyalty tier." );
        System.out.println("-".repeat(142));

        System.out.println("Generated On        : " + LocalDateTime.now().format(DATE_TIME_FORMAT));
        System.out.println("Stay History Period : " + formatPeriod(startDate, endDate));
        System.out.println("Search Keyword      : " + displayFilter(keyword));
        System.out.println("Loyalty Tier        : " + (tierFilter == null ? ALL : tierFilter));
        System.out.println("Current Activity    : " + normalizeActivity(activityFilter));
        System.out.println("Minimum Total Stays : " + (minimumCompletedStays == 0 ? ALL : minimumCompletedStays));
        System.out.println("Stay Room Type      : " + (roomTypeFilter == null ? ALL : formatRoomType(roomTypeFilter)));
        System.out.println("Search Technique    : Linear Search across VIP Profiles + Booking History with Multiple Filters");
        System.out.println("Sorting Technique   : Selection Sort - " + sortDescription(sortOption));
        System.out.println("-".repeat(142));

        if (entries.length == 0) {
            System.out.println("No VIP loyalty/stay records match the selected report criteria.");
            System.out.println("The report is based on the VIP guest master and booking history, not only the waiting queue.");
            System.out.println("=".repeat(142));
            return;
        }

       System.out.printf(
            "%-4s %-8s %-18s %-10s %-12s %-13s %-20s%n",
            "No.","Guest ID","Guest Name","Tier","Total Stays","Period Stays","Next Tier Progress");
        System.out.println("-".repeat(142));

        int[] tierCounts = new int[LoyaltyTier.values().length];
        int totalCompletedStays = 0;
        int totalPeriodStays = 0;
        int nearUpgradeCount = 0;
        LoyaltyEntry mostActive = null;

        for (int i = 0; i < entries.length; i++) {
            LoyaltyEntry entry = entries[i];
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

            System.out.printf(
                "%-4d %-8s %-18s %-10s %-12d %-13d %-20s%n",
                i + 1,
                entry.guest.getGuestId(),
                shorten(entry.guest.getName(), 18),
                profile.getTier(),
                profile.getCompletedStays(),
                entry.periodStayCount,
                nextTierProgress(profile)
        );
    }

        System.out.println("-".repeat(142));
        System.out.println("LOYALTY TIER SUMMARY");
        System.out.printf("%-12s %-14s %-22s%n", "Tier", "VIP Guests", "Share of Matching VIPs");
        for (LoyaltyTier tier : new LoyaltyTier[]{LoyaltyTier.DIAMOND, LoyaltyTier.PLATINUM, LoyaltyTier.ELITE}) {
            int count = tierCounts[tier.ordinal()];
            if (count == 0) {
                continue;
            }
            double share = (double) count * 100.0 / entries.length;
            String shareText = String.format("%.1f%%", share);
            System.out.printf("%-12s %-14d %-22s%n", tier, count, shareText);
        }

        

        System.out.println("\nMANAGEMENT SUMMARY");
        System.out.println("Matching VIP Guests         : " + entries.length);
        System.out.println("DIAMOND                     : " + tierCounts[LoyaltyTier.DIAMOND.ordinal()]);
        System.out.println("PLATINUM                    : " + tierCounts[LoyaltyTier.PLATINUM.ordinal()]);
        System.out.println("ELITE                       : " + tierCounts[LoyaltyTier.ELITE.ordinal()]);
        System.out.println("Total Completed VIP Stays   : " + totalCompletedStays);
        System.out.printf("Average Completed Stays     : %.1f stay(s)%n", (double) totalCompletedStays / entries.length);
        System.out.println("Matching Booking Stays      : " + totalPeriodStays);
        System.out.println("VIPs Near Next Tier (<=2)   : " + nearUpgradeCount);
        System.out.println("Most Active VIP             : " + (mostActive == null ? "-" : mostActive.guest.getName() + " (" + mostActive.guest.getGuestId() + ", " + mostActive.profile.getCompletedStays() + " stays)"));
        System.out.println("=".repeat(142));

        System.out.println("\nMANAGEMENT SUGGESTION");

        if (nearUpgradeCount > 0) {

            System.out.println( nearUpgradeCount + " VIP guest(s) are close to the next loyalty tier.");

            System.out.println("Suggestion: Offer personalised promotions or stay packages " + "to encourage repeat visits.");

        } else {

            System.out.println("No VIP guests are currently close to the next loyalty tier.");

            System.out.println("Suggestion: Continue monitoring VIP stay frequency "+ "and loyalty activity.");
        }
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