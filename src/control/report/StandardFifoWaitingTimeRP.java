package control.report;

import control.RegistrationController;
import entity.WalkInRegistration;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates an analytical report for Standard guests currently waiting
 * in the FIFO registration queue.
 *
 * Searching technique: Linear Search with multiple filtering criteria.
 * Sorting technique: Selection Sort.
 *
 * @author Lai Jen Feng
 */
public class StandardFifoWaitingTimeRP {

    private static final DateTimeFormatter DATE_TIME_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * sortOption:
     * 1 = FIFO order / earliest arrival first
     * 2 = longest waiting time first
     * 3 = largest party size first
     */
    public void generateReport(
            RegistrationController controller,
            String keyword,
            String roomTypeFilter,
            int minimumGuests,
            long minimumWaitingMinutes,
            int sortOption) {

        WaitingEntry[] entries = searchAndFilter(
                controller,
                keyword,
                roomTypeFilter,
                minimumGuests,
                minimumWaitingMinutes);

        sortEntries(entries, sortOption);

        System.out.println(
                "\n================================================================================================");
        System.out.println(
                "                      STANDARD FIFO WAITING TIME ANALYSIS REPORT");
        System.out.println(
                "================================================================================================");

        System.out.println(
                "Generated On          : "
                + LocalDateTime.now().format(DATE_TIME_FORMAT));

        System.out.println(
                "Search Keyword        : "
                + displayFilter(keyword, "ALL"));

        System.out.println(
                "Room Type             : "
                + displayRoomType(roomTypeFilter));

        System.out.println(
                "Minimum Guests        : "
                + minimumGuests);

        System.out.println(
                "Minimum Waiting Time  : "
                + minimumWaitingMinutes
                + " minute(s)");

        System.out.println(
                "Search Technique      : Linear Search");

        System.out.println(
                "Sorting Technique     : "
                + getSortDescription(sortOption));

        System.out.println(
                "------------------------------------------------------------------------------------------------");

        if (entries.length == 0) {
            System.out.println(
                    "No Standard waiting registrations match the selected criteria.");

            System.out.println(
                    "================================================================================================");
            return;
        }

        System.out.printf(
                "%-5s %-7s %-8s %-18s %-17s %-7s %-16s %-9s%n",
                "FIFO",
                "Reg ID",
                "Guest ID",
                "Guest Name",
                "Room Type",
                "Party",
                "Arrival",
                "Wait Min");

        System.out.println(
                "------------------------------------------------------------------------------------------------");

        int totalGuests = 0;
        long totalWaitingMinutes = 0;

        long longestWaitingMinutes = -1;
        WaitingEntry longestWaitingEntry = null;

        for (WaitingEntry entry : entries) {

            WalkInRegistration registration
                    = entry.registration;

            System.out.printf(
                    "%-5d %-7s %-8s %-18s %-17s %-7d %-16s %-9d%n",
                    entry.fifoPosition,
                    registration.getRegistrationId(),
                    registration.getGuest().getGuestId(),
                    shorten(
                            registration.getGuest().getName(),
                            18),
                    formatRoomType(
                            registration.getRequestedRoomType()),
                    registration.getNumberOfGuests(),
                    registration.getRegistrationTime()
                            .format(DATE_TIME_FORMAT),
                    entry.waitingMinutes);

            totalGuests
                    += registration.getNumberOfGuests();

            totalWaitingMinutes
                    += entry.waitingMinutes;

            if (entry.waitingMinutes
                    > longestWaitingMinutes) {

                longestWaitingMinutes
                        = entry.waitingMinutes;

                longestWaitingEntry = entry;
            }
        }

        double averageWaitingMinutes
                = (double) totalWaitingMinutes
                / entries.length;

        double averagePartySize
                = (double) totalGuests
                / entries.length;

        System.out.println(
                "------------------------------------------------------------------------------------------------");

        System.out.println(
                "MANAGEMENT SUMMARY");

        System.out.println(
                "Total Standard Waiting     : "
                + controller.getWaitingCount());

        System.out.println(
                "Matching Records            : "
                + entries.length);

        System.out.println(
                "Total Guest Demand          : "
                + totalGuests);

        System.out.printf(
                "Average Party Size          : %.2f guest(s)%n",
                averagePartySize);

        System.out.printf(
                "Average Waiting Time        : %.2f minute(s)%n",
                averageWaitingMinutes);

        System.out.println(
                "Longest Waiting Time        : "
                + longestWaitingMinutes
                + " minute(s)");

        if (longestWaitingEntry != null) {
            System.out.println(
                    "Longest Waiting Registration: "
                    + longestWaitingEntry.registration
                            .getRegistrationId()
                    + " / "
                    + longestWaitingEntry.registration
                            .getGuest()
                            .getName());
        }

        WalkInRegistration fifoHead
                = controller.getNextRegistration();

        if (fifoHead != null) {
            System.out.println(
                    "Current FIFO Head           : "
                    + fifoHead.getRegistrationId()
                    + " / "
                    + fifoHead.getGuest().getName());
        }

        System.out.println(
                "================================================================================================");
    }

    /**
     * Performs linear searching and multiple-condition filtering.
     */
    private WaitingEntry[] searchAndFilter(
            RegistrationController controller,
            String keyword,
            String roomTypeFilter,
            int minimumGuests,
            long minimumWaitingMinutes) {

        int waitingCount
                = controller.getWaitingCount();

        WaitingEntry[] temporary
                = new WaitingEntry[waitingCount];

        int count = 0;

        for (int i = 0; i < waitingCount; i++) {

            WalkInRegistration registration
                    = controller.getRegistrationAt(i);

            if (registration == null
                    || registration.getGuest() == null
                    || registration.getRegistrationTime() == null) {
                continue;
            }

            long waitingMinutes
                    = calculateWaitingMinutes(
                            registration);

            boolean matchesKeyword
                    = matchesKeyword(
                            registration,
                            keyword);

            boolean matchesRoomType
                    = roomTypeFilter == null
                    || roomTypeFilter.isBlank()
                    || registration
                            .getRequestedRoomType()
                            .equalsIgnoreCase(
                                    roomTypeFilter.trim());

            boolean matchesGuests
                    = registration.getNumberOfGuests()
                    >= minimumGuests;

            boolean matchesWaitingTime
                    = waitingMinutes
                    >= minimumWaitingMinutes;

            if (matchesKeyword
                    && matchesRoomType
                    && matchesGuests
                    && matchesWaitingTime) {

                temporary[count++]
                        = new WaitingEntry(
                                registration,
                                i + 1,
                                waitingMinutes);
            }
        }

        WaitingEntry[] result
                = new WaitingEntry[count];

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
    private void sortEntries(
            WaitingEntry[] entries,
            int sortOption) {

        for (int i = 0;
                i < entries.length - 1;
                i++) {

            int selectedIndex = i;

            for (int j = i + 1;
                    j < entries.length;
                    j++) {

                if (comesBefore(
                        entries[j],
                        entries[selectedIndex],
                        sortOption)) {

                    selectedIndex = j;
                }
            }

            WaitingEntry temp
                    = entries[i];

            entries[i]
                    = entries[selectedIndex];

            entries[selectedIndex]
                    = temp;
        }
    }

    private boolean comesBefore(
            WaitingEntry first,
            WaitingEntry second,
            int sortOption) {

        switch (sortOption) {

            case 2:
                if (first.waitingMinutes
                        != second.waitingMinutes) {

                    return first.waitingMinutes
                            > second.waitingMinutes;
                }

                return first.fifoPosition
                        < second.fifoPosition;

            case 3:
                int firstParty
                        = first.registration
                                .getNumberOfGuests();

                int secondParty
                        = second.registration
                                .getNumberOfGuests();

                if (firstParty != secondParty) {
                    return firstParty
                            > secondParty;
                }

                return first.fifoPosition
                        < second.fifoPosition;

            case 1:
            default:
                return first.fifoPosition
                        < second.fifoPosition;
        }
    }

    private boolean matchesKeyword(
            WalkInRegistration registration,
            String keyword) {

        if (keyword == null
                || keyword.isBlank()) {
            return true;
        }

        String value
                = keyword.trim()
                        .toLowerCase();

        return registration
                .getRegistrationId()
                .toLowerCase()
                .contains(value)
                || registration
                        .getGuest()
                        .getGuestId()
                        .toLowerCase()
                        .contains(value)
                || registration
                        .getGuest()
                        .getName()
                        .toLowerCase()
                        .contains(value);
    }

    private long calculateWaitingMinutes(
            WalkInRegistration registration) {

        long minutes
                = Duration.between(
                        registration.getRegistrationTime(),
                        LocalDateTime.now())
                        .toMinutes();

        return Math.max(minutes, 0);
    }

    private String getSortDescription(
            int sortOption) {

        switch (sortOption) {
            case 2:
                return "Selection Sort "
                        + "(Longest Waiting Time First)";

            case 3:
                return "Selection Sort "
                        + "(Largest Party Size First)";

            case 1:
            default:
                return "Selection Sort "
                        + "(FIFO / Earliest Arrival First)";
        }
    }

    private String displayFilter(
            String value,
            String allLabel) {

        return value == null
                || value.isBlank()
                ? allLabel
                : value.trim();
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

    /**
     * Stores one filtered waiting registration together with
     * its original FIFO position and calculated waiting time.
     */
    private static class WaitingEntry {

        private final WalkInRegistration registration;
        private final int fifoPosition;
        private final long waitingMinutes;

        private WaitingEntry(
                WalkInRegistration registration,
                int fifoPosition,
                long waitingMinutes) {

            this.registration = registration;
            this.fifoPosition = fifoPosition;
            this.waitingMinutes = waitingMinutes;
        }
    }
}