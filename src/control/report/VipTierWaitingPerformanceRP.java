package control.report;

import entity.LoyaltyTier;
import entity.Member;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates a management-oriented analysis of VIP waiting performance.
 *
 * Searching technique: linear search with multiple filters.
 * Sorting technique: selection sort by waiting time, then loyalty priority.
 *
 * The normal VIP waiting-list screen shows individual operational records.
 * This report instead aggregates waiting-time and tier performance metrics so
 * management can review service pressure that is not obvious from the normal
 * operational screens.
 *
 * @author Low Enn Toong
 */
public class VipTierWaitingPerformanceRP {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void generateReport(
            Member[] members,
            String keyword,
            LoyaltyTier tierFilter,
            String roomTypeFilter,
            int minimumWaitingMinutes,
            int minimumGuests) {

        if (members == null) {
            members = new Member[0];
        }

        Member[] temporaryMembers = new Member[members.length];
        long[] temporaryWaitingMinutes = new long[members.length];
        int count = 0;

        // Linear search through all waiting VIP records while applying all
        // selected criteria at the same time.
        for (Member member : members) {
            if (member == null || member.getRegistration() == null) {
                continue;
            }

            long waitingMinutes = calculateWaitingMinutes(member);

            if (matchesFilters(
                    member,
                    waitingMinutes,
                    keyword,
                    tierFilter,
                    roomTypeFilter,
                    minimumWaitingMinutes,
                    minimumGuests)) {
                temporaryMembers[count] = member;
                temporaryWaitingMinutes[count] = waitingMinutes;
                count++;
            }
        }

        Member[] filteredMembers = new Member[count];
        long[] waitingMinutes = new long[count];
        System.arraycopy(temporaryMembers, 0, filteredMembers, 0, count);
        System.arraycopy(temporaryWaitingMinutes, 0, waitingMinutes, 0, count);

        // Sorting is used to identify the strongest waiting-time exceptions.
        sortByWaitingTimeAndPriority(filteredMembers, waitingMinutes);

        printReport(
                filteredMembers,
                waitingMinutes,
                keyword,
                tierFilter,
                roomTypeFilter,
                minimumWaitingMinutes,
                minimumGuests);
    }

    private void printReport(
            Member[] members,
            long[] waitingMinutes,
            String keyword,
            LoyaltyTier tierFilter,
            String roomTypeFilter,
            int minimumWaitingMinutes,
            int minimumGuests) {

        System.out.println("\n====================================================================================================");
        System.out.println("                         VIP TIER & WAITING PERFORMANCE ANALYSIS");
        System.out.println("====================================================================================================");
        System.out.println("Generated On         : " + LocalDateTime.now().format(DATE_TIME_FORMAT));
        System.out.println("Search Keyword       : " + displayFilter(keyword, "ALL"));
        System.out.println("Loyalty Tier         : " + (tierFilter == null ? "ALL" : tierFilter));
        System.out.println("Requested Room Type  : " + displayRoomType(roomTypeFilter));
        System.out.println("Minimum Waiting Time : " + (minimumWaitingMinutes == 0 ? "ALL" : minimumWaitingMinutes + " minute(s)"));
        System.out.println("Minimum Party Size   : " + (minimumGuests == 0 ? "ALL" : minimumGuests + " guest(s)"));
        System.out.println("Search Technique     : Linear Search with Multiple Filters");
        System.out.println("Sorting Technique    : Selection Sort (Waiting Time Descending, Tier Priority Descending)");
        System.out.println("----------------------------------------------------------------------------------------------------");

        if (members.length == 0) {
            System.out.println("No VIP waiting records match the selected analysis criteria.");
            System.out.println("====================================================================================================");
            return;
        }

        int[] tierCounts = new int[LoyaltyTier.values().length];
        int[] tierGuestDemand = new int[LoyaltyTier.values().length];
        long[] tierTotalWait = new long[LoyaltyTier.values().length];
        long[] tierLongestWait = new long[LoyaltyTier.values().length];

        int totalGuestDemand = 0;
        long totalWaitingMinutes = 0;
        long shortestWaitingMinutes = waitingMinutes[0];
        long longestWaitingMinutes = waitingMinutes[0];

        for (int i = 0; i < members.length; i++) {
            Member member = members[i];
            int tierIndex = member.getTier().ordinal();
            int partySize = member.getRegistration().getNumberOfGuests();
            long wait = waitingMinutes[i];

            tierCounts[tierIndex]++;
            tierGuestDemand[tierIndex] += partySize;
            tierTotalWait[tierIndex] += wait;
            tierLongestWait[tierIndex] = Math.max(tierLongestWait[tierIndex], wait);

            totalGuestDemand += partySize;
            totalWaitingMinutes += wait;
            shortestWaitingMinutes = Math.min(shortestWaitingMinutes, wait);
            longestWaitingMinutes = Math.max(longestWaitingMinutes, wait);
        }

        System.out.println("TIER PERFORMANCE SUMMARY");
        System.out.printf(
                "%-11s %-8s %-11s %-14s %-14s %-14s %-12s%n",
                "Tier", "VIPs", "% of VIPs", "Guest Demand", "Avg Wait", "Longest Wait", "Avg Party");
        System.out.println("----------------------------------------------------------------------------------------------------");

        printTierSummary(LoyaltyTier.DIAMOND, members.length, tierCounts, tierGuestDemand, tierTotalWait, tierLongestWait);
        printTierSummary(LoyaltyTier.PLATINUM, members.length, tierCounts, tierGuestDemand, tierTotalWait, tierLongestWait);
        printTierSummary(LoyaltyTier.ELITE, members.length, tierCounts, tierGuestDemand, tierTotalWait, tierLongestWait);

        double averageWaitingMinutes = (double) totalWaitingMinutes / members.length;
        double averagePartySize = (double) totalGuestDemand / members.length;
        LoyaltyTier mostRepresentedTier = findMostRepresentedTier(tierCounts);
        LoyaltyTier highestAverageWaitTier = findHighestAverageWaitTier(tierCounts, tierTotalWait);
        LoyaltyTier highestPriorityTierPresent = findHighestPriorityTierPresent(tierCounts);

        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.println("MANAGEMENT PERFORMANCE SUMMARY");
        System.out.println("Filtered VIP Registrations : " + members.length);
        System.out.println("Total VIP Guest Demand     : " + totalGuestDemand + " guest(s)");
        System.out.printf("Average VIP Waiting Time   : %.2f minute(s)%n", averageWaitingMinutes);
        System.out.println("Longest VIP Waiting Time   : " + longestWaitingMinutes + " minute(s)");
        System.out.println("Shortest VIP Waiting Time  : " + shortestWaitingMinutes + " minute(s)");
        System.out.printf("Average VIP Party Size     : %.2f guest(s)%n", averagePartySize);
        System.out.println("Highest Priority Tier Seen : " + displayTier(highestPriorityTierPresent));
        System.out.println("Most Represented Tier      : " + displayTier(mostRepresentedTier));
        System.out.println("Highest Avg-Wait Tier      : " + displayTier(highestAverageWaitTier));

        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.println("TOP WAITING-TIME EXCEPTIONS");
        System.out.printf("%-4s %-8s %-10s %-18s %-17s %-7s %-10s%n",
                "No.", "Reg ID", "Tier", "Guest", "Requested Room", "Party", "Wait Min");
        System.out.println("----------------------------------------------------------------------------------------------------");

        int exceptionCount = Math.min(3, members.length);
        for (int i = 0; i < exceptionCount; i++) {
            Member member = members[i];
            System.out.printf("%-4d %-8s %-10s %-18s %-17s %-7d %-10d%n",
                    i + 1,
                    member.getRegistration().getRegistrationId(),
                    member.getTier(),
                    shorten(member.getName(), 18),
                    formatRoomType(member.getRegistration().getRequestedRoomType()),
                    member.getRegistration().getNumberOfGuests(),
                    waitingMinutes[i]);
        }

        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.println("Management Insight         : " + buildManagementInsight(members, waitingMinutes, highestAverageWaitTier));
        System.out.println("====================================================================================================");
    }

    private void printTierSummary(
            LoyaltyTier tier,
            int totalMembers,
            int[] tierCounts,
            int[] tierGuestDemand,
            long[] tierTotalWait,
            long[] tierLongestWait) {

        int index = tier.ordinal();
        int count = tierCounts[index];
        double percentage = totalMembers == 0 ? 0.0 : (double) count * 100.0 / totalMembers;
        double averageWait = count == 0 ? 0.0 : (double) tierTotalWait[index] / count;
        double averageParty = count == 0 ? 0.0 : (double) tierGuestDemand[index] / count;

        String percentageText = String.format("%.2f%%", percentage);

        System.out.printf(
                "%-11s %-8d %-11s %-14d %-14.2f %-14d %-12.2f%n",
                tier,
                count,
                percentageText,
                tierGuestDemand[index],
                averageWait,
                tierLongestWait[index],
                averageParty);
    }

    private boolean matchesFilters(
            Member member,
            long waitingMinutes,
            String keyword,
            LoyaltyTier tierFilter,
            String roomTypeFilter,
            int minimumWaitingMinutes,
            int minimumGuests) {

        boolean matchesKeyword = matchesKeyword(member, keyword);
        boolean matchesTier = tierFilter == null || member.getTier() == tierFilter;
        boolean matchesRoomType = roomTypeFilter == null
                || member.getRegistration().getRequestedRoomType().equalsIgnoreCase(roomTypeFilter);
        boolean matchesWaitingTime = waitingMinutes >= minimumWaitingMinutes;
        boolean matchesGuestCount = member.getRegistration().getNumberOfGuests() >= minimumGuests;

        return matchesKeyword
                && matchesTier
                && matchesRoomType
                && matchesWaitingTime
                && matchesGuestCount;
    }

    private boolean matchesKeyword(Member member, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        String value = keyword.trim().toLowerCase();

        return member.getRegistration().getRegistrationId().toLowerCase().contains(value)
                || member.getGuest().getGuestId().toLowerCase().contains(value)
                || member.getName().toLowerCase().contains(value);
    }

    /**
     * Selection sort with parallel waiting-time data. Longest-waiting VIPs are
     * placed first; ties are broken by higher loyalty priority and then by
     * earlier registration time.
     */
    private void sortByWaitingTimeAndPriority(Member[] members, long[] waitingMinutes) {
        for (int i = 0; i < members.length - 1; i++) {
            int bestIndex = i;

            for (int j = i + 1; j < members.length; j++) {
                if (comesBefore(members[j], waitingMinutes[j], members[bestIndex], waitingMinutes[bestIndex])) {
                    bestIndex = j;
                }
            }

            swapMembers(members, i, bestIndex);
            swapLongs(waitingMinutes, i, bestIndex);
        }
    }

    private boolean comesBefore(Member first, long firstWait, Member second, long secondWait) {
        if (firstWait != secondWait) {
            return firstWait > secondWait;
        }

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
                .compareToIgnoreCase(second.getRegistration().getRegistrationId()) < 0;
    }

    private long calculateWaitingMinutes(Member member) {
        LocalDateTime registrationTime = member.getRegistration().getRegistrationTime();

        if (registrationTime == null) {
            return 0;
        }

        long minutes = Duration.between(registrationTime, LocalDateTime.now()).toMinutes();
        return Math.max(minutes, 0);
    }

    private LoyaltyTier findMostRepresentedTier(int[] tierCounts) {
        LoyaltyTier best = null;
        int bestCount = 0;

        for (LoyaltyTier tier : LoyaltyTier.values()) {
            int count = tierCounts[tier.ordinal()];
            if (count > bestCount || (count == bestCount && count > 0
                    && (best == null || tier.getPriority() > best.getPriority()))) {
                best = tier;
                bestCount = count;
            }
        }

        return best;
    }

    private LoyaltyTier findHighestAverageWaitTier(int[] tierCounts, long[] tierTotalWait) {
        LoyaltyTier best = null;
        double bestAverage = -1.0;

        for (LoyaltyTier tier : LoyaltyTier.values()) {
            int index = tier.ordinal();
            if (tierCounts[index] == 0) {
                continue;
            }

            double average = (double) tierTotalWait[index] / tierCounts[index];
            if (average > bestAverage
                    || (Double.compare(average, bestAverage) == 0
                    && (best == null || tier.getPriority() > best.getPriority()))) {
                best = tier;
                bestAverage = average;
            }
        }

        return best;
    }

    private LoyaltyTier findHighestPriorityTierPresent(int[] tierCounts) {
        LoyaltyTier best = null;

        for (LoyaltyTier tier : LoyaltyTier.values()) {
            if (tierCounts[tier.ordinal()] > 0
                    && (best == null || tier.getPriority() > best.getPriority())) {
                best = tier;
            }
        }

        return best;
    }

    private String buildManagementInsight(Member[] members, long[] waitingMinutes, LoyaltyTier highestAverageWaitTier) {
        if (members.length == 1) {
            return "Only one VIP matches the selected filters; continue monitoring the waiting time before drawing a broader trend.";
        }

        long longest = waitingMinutes[0];
        long shortest = waitingMinutes[waitingMinutes.length - 1];
        long spread = longest - shortest;

        return displayTier(highestAverageWaitTier)
                + " currently has the highest average waiting time; the filtered waiting-time spread is "
                + spread + " minute(s).";
    }

    private String displayTier(LoyaltyTier tier) {
        return tier == null ? "NONE" : tier.toString();
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

    private void swapMembers(Member[] values, int first, int second) {
        Member temporary = values[first];
        values[first] = values[second];
        values[second] = temporary;
    }

    private void swapLongs(long[] values, int first, int second) {
        long temporary = values[first];
        values[first] = values[second];
        values[second] = temporary;
    }
}