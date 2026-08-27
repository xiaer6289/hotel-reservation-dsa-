package control.report;

import control.RegistrationController;
import entity.WalkInRegistration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates an analytical report that summarises walk-in arrival patterns
 * by time period for management decision-making.
 *
 * @author Lai Jen Feng
 */
public class WalkInArrivalPatternRP {

        private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        private static final String[] PERIOD_LABELS = {
                        "12:00 AM - 5:59 AM",
                        "6:00 AM  - 11:59 AM",
                        "12:00 PM - 5:59 PM",
                        "6:00 PM  - 11:59 PM"
        };

        public void generateReport(
                        RegistrationController controller,
                        LocalDate startDate,
                        LocalDate endDate) {

                WalkInRegistration[] records = collectRecordsByDateRange(
                                controller,
                                startDate,
                                endDate);

                int[] registrationCounts = new int[4];

                int[] guestCounts = new int[4];

                int totalGuests = 0;

                for (WalkInRegistration registration : records) {

                        int periodIndex = getPeriodIndex(
                                        registration
                                                        .getRegistrationTime()
                                                        .getHour());

                        registrationCounts[periodIndex]++;

                        guestCounts[periodIndex] += registration.getNumberOfGuests();

                        totalGuests += registration.getNumberOfGuests();
                }

                double averagePartySize = records.length == 0
                                ? 0.0
                                : (double) totalGuests
                                                / records.length;

                int peakCount = findPeakCount(
                                registrationCounts);

                double peakPercentage = records.length == 0
                                ? 0.0
                                : (double) peakCount
                                                / records.length
                                                * 100.0;

                System.out.println(
                                "\n========================================================================================");

                System.out.println(
                                "                      WALK-IN ARRIVAL PATTERN ANALYSIS REPORT");

                System.out.println(
                                "========================================================================================");

                System.out.println("REPORT PURPOSE");
                System.out.println(
                                "To identify peak walk-in arrival periods and guest demand patterns.");

                System.out.println();

                System.out.println("HOTEL VALUE");
                System.out.println(
                                "Helps hotel management plan front-desk staffing and room preparation before busy arrival periods.");

                System.out.println(
                                "----------------------------------------------------------------------------------------");

                System.out.println(
                                "Generated On          : "
                                                + LocalDateTime.now()
                                                                .format(DATE_TIME_FORMAT));

                System.out.println(
                                "Analysis Period       : "
                                                + displayDateRange(
                                                                startDate,
                                                                endDate));

                System.out.println(
                                "----------------------------------------------------------------------------------------");

                System.out.println(
                                "ARRIVAL PERIOD ANALYSIS");

                System.out.printf(
                                "%-24s %-16s %-14s %-15s%n",
                                "Time Period",
                                "Registrations",
                                "Total Guests",
                                "Avg Party Size");

                System.out.println(
                                "----------------------------------------------------------------------------------------");

                for (int i = 0; i < PERIOD_LABELS.length; i++) {

                        double periodAveragePartySize = registrationCounts[i] == 0
                                        ? 0.0
                                        : (double) guestCounts[i]
                                                        / registrationCounts[i];

                        System.out.printf(
                                        "%-24s %-16d %-14d %-15.2f%n",
                                        PERIOD_LABELS[i],
                                        registrationCounts[i],
                                        guestCounts[i],
                                        periodAveragePartySize);
                }

                System.out.println(
                                "----------------------------------------------------------------------------------------");

                System.out.println(
                                "MANAGEMENT SUMMARY");

                System.out.println(
                                "Total Registrations   : "
                                                + records.length);

                System.out.println(
                                "Total Walk-In Guests  : "
                                                + totalGuests);

                System.out.printf(
                                "Average Party Size    : %.2f guest(s)%n",
                                averagePartySize);

                System.out.println(
                                "Peak Arrival Period   : "
                                                + findPeakArrivalPeriod(
                                                                registrationCounts));

                System.out.printf(
                                "Peak Percentage       : %.2f%%%n",
                                peakPercentage);

                System.out.println(
                                "----------------------------------------------------------------------------------------");

                System.out.println(
                                "ANALYSIS & SUGGESTION");

                printAnalysisAndSuggestion(
                                records.length,
                                registrationCounts);

                System.out.println(
                                "========================================================================================");
        }

        /**
         * Collects valid walk-in registrations within the selected date range.
         */
        private WalkInRegistration[] collectRecordsByDateRange(
                        RegistrationController controller,
                        LocalDate startDate,
                        LocalDate endDate) {

                int totalRecords = controller.getTotalRegistrationCount();

                WalkInRegistration[] temporary = new WalkInRegistration[totalRecords];

                int count = 0;

                for (int i = 0; i < totalRecords; i++) {

                        WalkInRegistration registration = controller.getRecordAt(i);

                        if (registration == null
                                        || registration.getGuest() == null
                                        || registration.getRegistrationTime() == null) {

                                continue;
                        }

                        LocalDate registrationDate = registration
                                        .getRegistrationTime()
                                        .toLocalDate();

                        boolean withinStartDate = startDate == null
                                        || !registrationDate.isBefore(
                                                        startDate);

                        boolean withinEndDate = endDate == null
                                        || !registrationDate.isAfter(
                                                        endDate);

                        if (withinStartDate
                                        && withinEndDate) {

                                temporary[count++] = registration;
                        }
                }

                WalkInRegistration[] result = new WalkInRegistration[count];

                System.arraycopy(
                                temporary,
                                0,
                                result,
                                0,
                                count);

                return result;
        }

        private int getPeriodIndex(
                        int hour) {

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

        private int findPeakCount(
                        int[] registrationCounts) {

                if (registrationCounts == null
                                || registrationCounts.length == 0) {

                        return 0;
                }

                int highestCount = 0;

                for (int count : registrationCounts) {

                        if (count > highestCount) {

                                highestCount = count;
                        }
                }

                return highestCount;
        }

        private String findPeakArrivalPeriod(
                        int[] registrationCounts) {

                int highestCount = findPeakCount(
                                registrationCounts);

                if (highestCount == 0) {

                        return "N/A";
                }

                StringBuilder peakPeriods = new StringBuilder();

                for (int i = 0; i < registrationCounts.length; i++) {

                        if (registrationCounts[i] == highestCount) {

                                if (peakPeriods.length() > 0) {

                                        peakPeriods.append(
                                                        " & ");
                                }

                                peakPeriods.append(
                                                PERIOD_LABELS[i]);
                        }
                }

                return peakPeriods
                                + " ("
                                + highestCount
                                + " registration(s))";
        }

        private void printAnalysisAndSuggestion(
                        int totalRegistrations,
                        int[] registrationCounts) {

                if (totalRegistrations == 0) {

                        System.out.println(
                                        "Analysis   : No walk-in arrival data is available for the selected period.");

                        System.out.println(
                                        "Suggestion : Collect more walk-in registration data before making staffing decisions.");

                        return;
                }

                String peakPeriod = findPeakArrivalPeriod(
                                registrationCounts);

                int peakCount = findPeakCount(
                                registrationCounts);

                double peakPercentage = (double) peakCount
                                / totalRegistrations
                                * 100.0;

                System.out.println(
                                "Analysis   : The highest walk-in demand occurs during "
                                                + peakPeriod
                                                + ".");

                if (peakPercentage >= 50.0) {

                        System.out.println(
                                        "Suggestion : Prepare more ready rooms and front-desk capacity before the peak arrival period.");

                } else {

                        System.out.println(
                                        "Suggestion : Maintain balanced room preparation while giving extra attention to the peak period.");
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

                        return "From "
                                        + startDate;
                }

                return "Until "
                                + endDate;
        }
}