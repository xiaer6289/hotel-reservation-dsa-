package control.report;

import entity.LoyaltyTier;
import entity.Member;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates an analytical report for VIP members currently waiting in the
 * MaxHeap.
 *
 * Searching technique: linear search with multiple filters.
 * Sorting technique: selection sort by loyalty priority and arrival time.
 *
 * @author Low Enn Toong
 */
public class VipPriorityQueueRP {

    private static final DateTimeFormatter DATE_TIME_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void generateReport(
            Member[] members,
            String keyword,
            LoyaltyTier tierFilter,
            String roomTypeFilter,
            int minimumGuests) {

        Member[] filteredMembers = searchAndFilter(
                members,
                keyword,
                tierFilter,
                roomTypeFilter,
                minimumGuests);

        sortByPriorityAndArrival(filteredMembers);

        System.out.println("\n===============================================================================================");
        System.out.println("                    VIP PRIORITY QUEUE ANALYSIS REPORT");
        System.out.println("===============================================================================================");
        System.out.println("Generated On       : "
                + LocalDateTime.now().format(DATE_TIME_FORMAT));
        System.out.println("Search Keyword     : "
                + displayFilter(keyword, "ALL"));
        System.out.println("Loyalty Tier       : "
                + (tierFilter == null ? "ALL" : tierFilter));
        System.out.println("Requested Room Type: "
                + displayRoomType(roomTypeFilter));
        System.out.println("Minimum Guests     : " + minimumGuests);
        System.out.println("Search Technique   : Linear Search");
        System.out.println("Sorting Technique  : Selection Sort (Tier Descending, Arrival Time Ascending)");
        System.out.println("-----------------------------------------------------------------------------------------------");

        if (filteredMembers.length == 0) {
            System.out.println("No VIP waiting records match the selected criteria.");
            System.out.println("===============================================================================================");
            return;
        }

        System.out.printf(
                "%-3s %-9s %-7s %-16s %-10s %-17s %-6s %-16s %-8s%n",
                "No.",
                "Member",
                "Reg ID",
                "Guest",
                "Tier",
                "Requested Room",
                "Guests",
                "Registered At",
                "Wait Min");
        System.out.println("-----------------------------------------------------------------------------------------------");

        int eliteCount = 0;
        int diamondCount = 0;
        int platinumCount = 0;
        int totalGuests = 0;
        long totalWaitingMinutes = 0;
        long longestWaitingMinutes = 0;

        for (int i = 0; i < filteredMembers.length; i++) {
            Member member = filteredMembers[i];
            long waitingMinutes = calculateWaitingMinutes(member);

            System.out.printf(
                    "%-3d %-9s %-7s %-16s %-10s %-17s %-6d %-16s %-8d%n",
                    i + 1,
                    member.getMemberId(),
                    member.getRegistration().getRegistrationId(),
                    shorten(member.getName(), 16),
                    member.getTier(),
                    formatRoomType(
                            member.getRegistration().getRequestedRoomType()),
                    member.getRegistration().getNumberOfGuests(),
                    member.getRegistration().getRegistrationTime()
                            .format(DATE_TIME_FORMAT),
                    waitingMinutes);

            switch (member.getTier()) {
                case ELITE:
                    eliteCount++;
                    break;
                case DIAMOND:
                    diamondCount++;
                    break;
                case PLATINUM:
                    platinumCount++;
                    break;
                default:
                    break;
            }

            totalGuests += member.getRegistration().getNumberOfGuests();
            totalWaitingMinutes += waitingMinutes;

            if (waitingMinutes > longestWaitingMinutes) {
                longestWaitingMinutes = waitingMinutes;
            }
        }

        double averagePartySize
                = (double) totalGuests / filteredMembers.length;
        double averageWaitingMinutes
                = (double) totalWaitingMinutes / filteredMembers.length;

        System.out.println("-----------------------------------------------------------------------------------------------");
        System.out.println("MANAGEMENT SUMMARY");
        System.out.println("Matching VIP Members : " + filteredMembers.length);
        System.out.println("Platinum Members     : " + platinumCount);
        System.out.println("Diamond Members      : " + diamondCount);
        System.out.println("Elite Members        : " + eliteCount);
        System.out.println("Total Guest Demand   : " + totalGuests);
        System.out.printf("Average Party Size   : %.2f guest(s)%n", averagePartySize);
        System.out.printf("Average Waiting Time : %.2f minute(s)%n", averageWaitingMinutes);
        System.out.println("Longest Waiting Time : "
                + longestWaitingMinutes + " minute(s)");
        System.out.println("Next Allocation      : "
                + filteredMembers[0].getMemberId()
                + " (" + filteredMembers[0].getTier() + ")");
        System.out.println("===============================================================================================");
    }

    /**
     * Performs a linear search and applies all selected criteria.
     */
    private Member[] searchAndFilter(
            Member[] members,
            String keyword,
            LoyaltyTier tierFilter,
            String roomTypeFilter,
            int minimumGuests) {

        if (members == null) {
            return new Member[0];
        }

        Member[] temporary = new Member[members.length];
        int count = 0;

        for (Member member : members) {
            if (member == null || member.getRegistration() == null) {
                continue;
            }

            boolean matchesKeyword = matchesKeyword(member, keyword);
            boolean matchesTier = tierFilter == null
                    || member.getTier() == tierFilter;
            boolean matchesRoomType = roomTypeFilter == null
                    || member.getRegistration().getRequestedRoomType()
                            .equalsIgnoreCase(roomTypeFilter);
            boolean matchesGuestCount
                    = member.getRegistration().getNumberOfGuests()
                    >= minimumGuests;

            if (matchesKeyword
                    && matchesTier
                    && matchesRoomType
                    && matchesGuestCount) {

                temporary[count++] = member;
            }
        }

        Member[] filteredMembers = new Member[count];
        System.arraycopy(temporary, 0, filteredMembers, 0, count);
        return filteredMembers;
    }

    private boolean matchesKeyword(Member member, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        String value = keyword.trim().toLowerCase();

        return member.getMemberId().toLowerCase().contains(value)
                || member.getRegistration().getRegistrationId()
                        .toLowerCase().contains(value)
                || member.getGuest().getGuestId()
                        .toLowerCase().contains(value)
                || member.getName().toLowerCase().contains(value);
    }

    /**
     * Selection sort: highest tier first; for equal tiers, earlier arrival
     * first.
     */
    private void sortByPriorityAndArrival(Member[] members) {
        for (int i = 0; i < members.length - 1; i++) {
            int highestIndex = i;

            for (int j = i + 1; j < members.length; j++) {
                if (comesBefore(members[j], members[highestIndex])) {
                    highestIndex = j;
                }
            }

            Member temporary = members[i];
            members[i] = members[highestIndex];
            members[highestIndex] = temporary;
        }
    }

    private boolean comesBefore(Member first, Member second) {
        int firstPriority = first.getTier().getPriority();
        int secondPriority = second.getTier().getPriority();

        if (firstPriority != secondPriority) {
            return firstPriority > secondPriority;
        }

        int timeComparison = first.getRegistration().getRegistrationTime()
                .compareTo(second.getRegistration().getRegistrationTime());

        if (timeComparison != 0) {
            return timeComparison < 0;
        }

        return first.getRegistration().getRegistrationId()
                .compareToIgnoreCase(
                        second.getRegistration().getRegistrationId()) < 0;
    }

    private long calculateWaitingMinutes(Member member) {
        LocalDateTime registrationTime
                = member.getRegistration().getRegistrationTime();

        if (registrationTime == null) {
            return 0;
        }

        long minutes = Duration.between(
                registrationTime,
                LocalDateTime.now()).toMinutes();

        return Math.max(minutes, 0);
    }

    private String displayFilter(String value, String allLabel) {
        return value == null || value.isBlank() ? allLabel : value.trim();
    }

    private String displayRoomType(String roomType) {
        return roomType == null ? "ALL" : formatRoomType(roomType);
    }

    private String formatRoomType(String roomType) {
        return roomType == null ? "-" : roomType.replace('_', ' ');
    }

    private String shorten(String value, int maximumLength) {
        if (value == null) {
            return "-";
        }

        if (value.length() <= maximumLength) {
            return value;
        }

        return value.substring(0, maximumLength - 3) + "...";
    }
}