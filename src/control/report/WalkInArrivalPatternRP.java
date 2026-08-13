package control.report;

import control.RegistrationController;
import entity.WalkInRegistration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Analyses walk-in arrival patterns by date, room type,
 * party size and arrival period.
 *
 * Searching / Filtering Technique: Linear Search
 * Sorting Technique: Selection Sort
 *
 * @author Lai Jen Feng
 */
public class WalkInArrivalPatternRP {

    private static final DateTimeFormatter DATE_TIME_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * sortOption:
     * 1 = Earliest arrival first
     * 2 = Latest arrival first
     * 3 = Largest party size first
     */
    public void generateReport(
            RegistrationController controller,
            LocalDate startDate,
            LocalDate endDate,
            String roomTypeFilter,
            int minimumGuests,
            int sortOption) {

        WalkInRegistration[] records = searchAndFilter(
                controller,
                startDate,
                endDate,
                roomTypeFilter,
                minimumGuests);

        sortRecords(records, sortOption);

        System.out.println(
                "\n================================================================================================");
        System.out.println(
                "                         WALK-IN ARRIVAL PATTERN ANALYSIS REPORT");
        System.out.println(
                "================================================================================================");

        System.out.println(
                "Generated On       : "
                + LocalDateTime.now().format(DATE_TIME_FORMAT));

        System.out.println(
                "Date Range         : "
                + displayDateRange(startDate, endDate));

        System.out.println(
                "Room Type          : "
                + displayRoomType(roomTypeFilter));

        System.out.println(
                "Minimum Party Size : "
                + minimumGuests);

        System.out.println(
                "Search Technique   : Linear Search");

        System.out.println(
                "Sorting Technique  : "
                + getSortDescription(sortOption));

        System.out.println(
                "------------------------------------------------------------------------------------------------");

        if (records.length == 0) {
            System.out.println(
                    "No walk-in registrations match the selected criteria.");

            System.out.println(
                    "================================================================================================");
            return;
        }

        System.out.printf(
                "%-8s %-9s %-18s %-17s %-7s %-17s%n",
                "Reg ID",
                "Guest ID",
                "Guest Name",
                "Room Type",
                "Party",
                "Arrival");

        System.out.println(
                "------------------------------------------------------------------------------------------------");

        int totalGuests = 0;

        for (WalkInRegistration registration : records) {

            System.out.printf(
                    "%-8s %-9s %-18s %-17s %-7d %-17s%n",
                    registration.getRegistrationId(),
                    registration.getGuest().getGuestId(),
                    shorten(
                            registration.getGuest().getName(),
                            18),
                    formatRoomType(
                            registration.getRequestedRoomType()),
                    registration.getNumberOfGuests(),
                    registration.getRegistrationTime()
                            .format(DATE_TIME_FORMAT));

            totalGuests
                    += registration.getNumberOfGuests();
        }

        System.out.println(
                "------------------------------------------------------------------------------------------------");

        printArrivalPeriodAnalysis(records);

        double averagePartySize
                = (double) totalGuests
                / records.length;

        System.out.println(
                "\nMANAGEMENT SUMMARY");

        System.out.println(
                "Matching Registrations : "
                + records.length);

        System.out.println(
                "Total Walk-In Guests   : "
                + totalGuests);

        System.out.printf(
                "Average Party Size     : %.2f guest(s)%n",
                averagePartySize);

        System.out.println(
                "Peak Arrival Period    : "
                + findPeakArrivalPeriod(records));

        System.out.println(
                "================================================================================================");
    }

    /**
     * Performs Linear Search with multiple filtering conditions.
     */
    private WalkInRegistration[] searchAndFilter(
            RegistrationController controller,
            LocalDate startDate,
            LocalDate endDate,
            String roomTypeFilter,
            int minimumGuests) {

        int totalRecords
                = controller.getTotalRegistrationCount();

        WalkInRegistration[] temporary
                = new WalkInRegistration[totalRecords];

        int count = 0;

        for (int i = 0; i < totalRecords; i++) {

            WalkInRegistration registration
                    = controller.getRecordAt(i);

            if (registration == null
                    || registration.getGuest() == null
                    || registration.getRegistrationTime() == null) {
                continue;
            }

            LocalDate registrationDate
                    = registration
                            .getRegistrationTime()
                            .toLocalDate();

            boolean matchesStartDate
                    = startDate == null
                    || !registrationDate.isBefore(startDate);

            boolean matchesEndDate
                    = endDate == null
                    || !registrationDate.isAfter(endDate);

            boolean matchesRoomType
                    = roomTypeFilter == null
                    || roomTypeFilter.isBlank()
                    || registration
                            .getRequestedRoomType()
                            .equalsIgnoreCase(
                                    roomTypeFilter.trim());

            boolean matchesPartySize
                    = registration.getNumberOfGuests()
                    >= minimumGuests;

            if (matchesStartDate
                    && matchesEndDate
                    && matchesRoomType
                    && matchesPartySize) {

                temporary[count++]
                        = registration;
            }
        }

        WalkInRegistration[] result
                = new WalkInRegistration[count];

        System.arraycopy(
                temporary,
                0,
                result,
                0,
                count);

        return result;
    }

    /**
     * Selection Sort.
     */
    private void sortRecords(
            WalkInRegistration[] records,
            int sortOption) {

        for (int i = 0;
                i < records.length - 1;
                i++) {

            int selectedIndex = i;

            for (int j = i + 1;
                    j < records.length;
                    j++) {

                if (comesBefore(
                        records[j],
                        records[selectedIndex],
                        sortOption)) {

                    selectedIndex = j;
                }
            }

            WalkInRegistration temp
                    = records[i];

            records[i]
                    = records[selectedIndex];

            records[selectedIndex]
                    = temp;
        }
    }

    private boolean comesBefore(
            WalkInRegistration first,
            WalkInRegistration second,
            int sortOption) {

        switch (sortOption) {

            case 2:
                return first.getRegistrationTime()
                        .isAfter(
                                second.getRegistrationTime());

            case 3:

                if (first.getNumberOfGuests()
                        != second.getNumberOfGuests()) {

                    return first.getNumberOfGuests()
                            > second.getNumberOfGuests();
                }

                return first.getRegistrationTime()
                        .isBefore(
                                second.getRegistrationTime());

            case 1:
            default:
                return first.getRegistrationTime()
                        .isBefore(
                                second.getRegistrationTime());
        }
    }

    private void printArrivalPeriodAnalysis(
            WalkInRegistration[] records) {

        int[] registrationCounts
                = new int[4];

        int[] guestCounts
                = new int[4];

        for (WalkInRegistration registration : records) {

            int hour
                    = registration
                            .getRegistrationTime()
                            .getHour();

            int periodIndex
                    = getPeriodIndex(hour);

            registrationCounts[periodIndex]++;

            guestCounts[periodIndex]
                    += registration.getNumberOfGuests();
        }

        String[] periodLabels = {
            "12:00 AM - 5:59 AM",
            "6:00 AM  - 11:59 AM",
            "12:00 PM - 5:59 PM",
            "6:00 PM  - 11:59 PM"
        };

        System.out.println(
                "ARRIVAL PERIOD ANALYSIS");

        System.out.printf(
                "%-24s %-16s %-14s %-15s%n",
                "Time Period",
                "Registrations",
                "Total Guests",
                "Avg Party Size");

        System.out.println(
                "--------------------------------------------------------------------------");

        for (int i = 0;
                i < periodLabels.length;
                i++) {

            double averagePartySize
                    = registrationCounts[i] == 0
                    ? 0.0
                    : (double) guestCounts[i]
                    / registrationCounts[i];

            System.out.printf(
                    "%-24s %-16d %-14d %-15.2f%n",
                    periodLabels[i],
                    registrationCounts[i],
                    guestCounts[i],
                    averagePartySize);
        }
    }

    private String findPeakArrivalPeriod(
            WalkInRegistration[] records) {

        int[] counts
                = new int[4];

        for (WalkInRegistration registration : records) {

            int periodIndex
                    = getPeriodIndex(
                            registration
                                    .getRegistrationTime()
                                    .getHour());

            counts[periodIndex]++;
        }

        String[] periodLabels = {
            "12:00 AM - 5:59 AM",
            "6:00 AM - 11:59 AM",
            "12:00 PM - 5:59 PM",
            "6:00 PM - 11:59 PM"
        };

        int highestIndex = 0;

        for (int i = 1;
                i < counts.length;
                i++) {

            if (counts[i]
                    > counts[highestIndex]) {

                highestIndex = i;
            }
        }

        return periodLabels[highestIndex]
                + " ("
                + counts[highestIndex]
                + " registration(s))";
    }

    private int getPeriodIndex(int hour) {

        if (hour < 6) {
            return 0;
        }

        if (hour < 12) {
            return 1;
        }

        if (hour < 18) {
            return 2;
        }

        return 3;
    }

    private String getSortDescription(
            int sortOption) {

        switch (sortOption) {

            case 2:
                return "Selection Sort "
                        + "(Latest Arrival First)";

            case 3:
                return "Selection Sort "
                        + "(Largest Party Size First)";

            case 1:
            default:
                return "Selection Sort "
                        + "(Earliest Arrival First)";
        }
    }

    private String displayDateRange(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null
                && endDate == null) {
            return "ALL DATES";
        }

        if (startDate != null
                && endDate != null) {

            return startDate
                    + " to "
                    + endDate;
        }

        if (startDate != null) {
            return "From " + startDate;
        }

        return "Until " + endDate;
    }

    private String displayRoomType(
            String roomType) {

        if (roomType == null
                || roomType.isBlank()) {
            return "ALL";
        }

        return formatRoomType(roomType);
    }

    private String formatRoomType(
            String roomType) {

        return roomType == null
                ? "-"
                : roomType.replace('_', ' ');
    }

    private String shorten(
            String value,
            int maximumLength) {

        if (value == null) {
            return "-";
        }

        if (value.length()
                <= maximumLength) {
            return value;
        }

        return value.substring(
                0,
                maximumLength - 3)
                + "...";
    }
}