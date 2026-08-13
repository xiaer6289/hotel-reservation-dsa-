package control.report;

import entity.LoyaltyProfile;
import entity.LoyaltyTier;
import entity.WalkInRegistration;
import entity.Room;
import entity.RoomType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates a management-oriented analysis of current VIP room-allocation
 * demand compared with the ready-room supply relevant to that demand.
 *
 * Searching technique: linear search through VIP records and ready rooms.
 * Sorting technique: selection sort by blocked VIP count, room-count gap and
 * request volume.
 *
 * The report deliberately stays within the VIP allocation scope. It does not
 * analyse hotel-wide occupancy, billing, cleaning time or housekeeping staff
 * performance.
 *
 * @author Low Enn Toong
 */
public class VipRoomAllocationDemandRP {
    private LoyaltyProfile[] loyaltyProfiles = new LoyaltyProfile[0];
    private static final String ALL = "ALL";
    private static final String READY = "READY";
    private static final String BLOCKED = "BLOCKED";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void generateReport(
            WalkInRegistration[] members,
            LoyaltyProfile[] loyaltyProfiles,
            Room[] readyRooms,
            String keyword,
            LoyaltyTier tierFilter,
            String roomTypeFilter,
            String readinessFilter,
            int minimumGuests) {

        if (members == null) {
            members = new WalkInRegistration[0];
        }

        this.loyaltyProfiles = loyaltyProfiles == null
                ? new LoyaltyProfile[0]
                : loyaltyProfiles;

        if (readyRooms == null) {
            readyRooms = new Room[0];
        }

        WalkInRegistration[] filteredMembers = new WalkInRegistration[members.length];
        int[] memberMatchCounts = new int[members.length];
        int filteredCount = 0;

        // Linear search over VIP records. For each VIP, another linear search
        // checks the ready-room supply to determine allocation readiness.
        for (WalkInRegistration member : members) {
            if (member == null || member.getGuest() == null) {
                continue;
            }

            int matchingRoomCount = countMatchingRooms(member, readyRooms);
            boolean hasReadyMatch = matchingRoomCount > 0;

            if (matchesFilters(
                    member,
                    keyword,
                    tierFilter,
                    roomTypeFilter,
                    readinessFilter,
                    minimumGuests,
                    hasReadyMatch)) {
                filteredMembers[filteredCount] = member;
                memberMatchCounts[filteredCount] = matchingRoomCount;
                filteredCount++;
            }
        }

        if (filteredCount == 0) {
            printEmptyReport(keyword, tierFilter, roomTypeFilter, readinessFilter, minimumGuests);
            return;
        }

        RoomDemandSummary[] summaries = createRoomTypeSummaries();

        for (int i = 0; i < filteredCount; i++) {
            WalkInRegistration member = filteredMembers[i];
            RoomDemandSummary summary = findSummary(
                    summaries,
                    member.getRequestedRoomType());

            if (summary == null) {
                continue;
            }

            summary.vipRequests++;
            summary.guestDemand += member.getNumberOfGuests();

            if (memberMatchCounts[i] > 0) {
                summary.vipWithReadyMatch++;
            } else {
                summary.blockedVipRequests++;
            }
        }

        // Linear search through ready rooms to build supply counts by room type.
        for (Room room : readyRooms) {
            if (room == null || !room.isAssignable()) {
                continue;
            }

            RoomDemandSummary summary = findSummary(summaries, room.getRoomType());
            if (summary != null) {
                summary.readyRooms++;
            }
        }

        RoomDemandSummary[] relevantSummaries = selectRelevantSummaries(summaries, roomTypeFilter);
        sortByAllocationPressure(relevantSummaries);

        printReport(
                relevantSummaries,
                filteredMembers,
                memberMatchCounts,
                filteredCount,
                keyword,
                tierFilter,
                roomTypeFilter,
                readinessFilter,
                minimumGuests);
    }

    private void printReport(
            RoomDemandSummary[] summaries,
            WalkInRegistration[] filteredMembers,
            int[] memberMatchCounts,
            int filteredCount,
            String keyword,
            LoyaltyTier tierFilter,
            String roomTypeFilter,
            String readinessFilter,
            int minimumGuests) {

        System.out.println("\n==============================================================================================================");
        System.out.println("                              VIP ROOM ALLOCATION DEMAND ANALYSIS");
        System.out.println("==============================================================================================================");
        printFilterHeader(keyword, tierFilter, roomTypeFilter, readinessFilter, minimumGuests);
        System.out.println("--------------------------------------------------------------------------------------------------------------");
        System.out.println("DEMAND VS READY-ROOM SUPPLY BY REQUESTED ROOM TYPE");
        System.out.printf(
                "%-18s %-10s %-13s %-12s %-15s %-12s %-10s%n",
                "Room Type", "VIP Req.", "Guest Demand", "Ready Rooms", "VIPs w/ Match", "Blocked VIPs", "Room Gap");
        System.out.println("--------------------------------------------------------------------------------------------------------------");

        int totalRequests = 0;
        int totalGuestDemand = 0;
        int totalReadyRooms = 0;
        int totalWithMatch = 0;
        int totalBlocked = 0;

        for (RoomDemandSummary summary : summaries) {
            int roomGap = Math.max(0, summary.vipRequests - summary.readyRooms);

            System.out.printf(
                    "%-18s %-10d %-13d %-12d %-15d %-12d %-10d%n",
                    formatRoomType(summary.roomType),
                    summary.vipRequests,
                    summary.guestDemand,
                    summary.readyRooms,
                    summary.vipWithReadyMatch,
                    summary.blockedVipRequests,
                    roomGap);

            totalRequests += summary.vipRequests;
            totalGuestDemand += summary.guestDemand;
            totalReadyRooms += summary.readyRooms;
            totalWithMatch += summary.vipWithReadyMatch;
            totalBlocked += summary.blockedVipRequests;
        }

        double matchCoverageRate = totalRequests == 0
                ? 0.0
                : (double) totalWithMatch * 100.0 / totalRequests;

        RoomDemandSummary highestDemand = findHighestDemandSummary(summaries);
        RoomDemandSummary mostConstrained = findMostConstrainedSummary(summaries);

        System.out.println("--------------------------------------------------------------------------------------------------------------");
        System.out.println("MANAGEMENT ALLOCATION SUMMARY");
        System.out.println("Filtered VIP Requests       : " + totalRequests);
        System.out.println("Total VIP Guest Demand      : " + totalGuestDemand + " guest(s)");
        System.out.println("Ready Rooms for Shown Types : " + totalReadyRooms);
        System.out.println("VIPs With Ready Match       : " + totalWithMatch);
        System.out.println("Blocked VIP Requests        : " + totalBlocked);
        System.out.printf("Ready-Match Coverage Rate   : %.2f%%%n", matchCoverageRate);
        System.out.println("Highest-Demand Room Type    : " + displaySummaryType(highestDemand));
        System.out.println("Most Constrained Room Type  : " + displaySummaryType(mostConstrained));

        int highestPriorityBlockedIndex = findHighestPriorityBlockedVip(filteredMembers, memberMatchCounts, filteredCount);
        if (highestPriorityBlockedIndex >= 0) {
            WalkInRegistration blocked = filteredMembers[highestPriorityBlockedIndex];
            System.out.println("Highest-Priority Blocked VIP: "
                    + blocked.getRegistrationId()
                    + " / " + blocked.getGuest().getGuestId()
                    + " (" + getTier(blocked) + ", "
                    + formatRoomType(blocked.getRequestedRoomType()) + ")");
        } else {
            System.out.println("Highest-Priority Blocked VIP: NONE");
        }

        System.out.println("--------------------------------------------------------------------------------------------------------------");
        System.out.println("Management Insight          : " + buildManagementInsight(totalRequests, totalBlocked, mostConstrained));
        System.out.println("==============================================================================================================");
    }

    private void printEmptyReport(
            String keyword,
            LoyaltyTier tierFilter,
            String roomTypeFilter,
            String readinessFilter,
            int minimumGuests) {

        System.out.println("\n==============================================================================================================");
        System.out.println("                              VIP ROOM ALLOCATION DEMAND ANALYSIS");
        System.out.println("==============================================================================================================");
        printFilterHeader(keyword, tierFilter, roomTypeFilter, readinessFilter, minimumGuests);
        System.out.println("--------------------------------------------------------------------------------------------------------------");
        System.out.println("No VIP allocation-demand records match the selected analysis criteria.");
        System.out.println("==============================================================================================================");
    }

    private void printFilterHeader(
            String keyword,
            LoyaltyTier tierFilter,
            String roomTypeFilter,
            String readinessFilter,
            int minimumGuests) {

        System.out.println("Generated On        : " + LocalDateTime.now().format(DATE_TIME_FORMAT));
        System.out.println("Search Keyword      : " + displayFilter(keyword, ALL));
        System.out.println("Loyalty Tier        : " + (tierFilter == null ? ALL : tierFilter));
        System.out.println("Requested Room Type : " + (roomTypeFilter == null ? ALL : formatRoomType(roomTypeFilter)));
        System.out.println("Allocation Status   : " + normalizeReadinessFilter(readinessFilter));
        System.out.println("Minimum Party Size  : " + (minimumGuests == 0 ? "ALL" : minimumGuests + " guest(s)"));
        System.out.println("Search Technique    : Linear Search (VIP Records + Ready Rooms) with Multiple Filters");
        System.out.println("Sorting Technique   : Selection Sort (Blocked VIPs Descending, Room Gap Descending, Demand Descending)");
    }

    private RoomDemandSummary[] createRoomTypeSummaries() {
        RoomType[] roomTypes = RoomType.values();
        RoomDemandSummary[] summaries = new RoomDemandSummary[roomTypes.length];

        for (int i = 0; i < roomTypes.length; i++) {
            summaries[i] = new RoomDemandSummary(roomTypes[i].name());
        }

        return summaries;
    }

    private RoomDemandSummary[] selectRelevantSummaries(RoomDemandSummary[] summaries, String roomTypeFilter) {
        int count = 0;

        for (RoomDemandSummary summary : summaries) {
            boolean requestedTypeSelected = roomTypeFilter == null
                    || summary.roomType.equalsIgnoreCase(roomTypeFilter);
            boolean hasFilteredDemand = summary.vipRequests > 0;

            if (requestedTypeSelected && hasFilteredDemand) {
                count++;
            }
        }

        RoomDemandSummary[] relevant = new RoomDemandSummary[count];
        int index = 0;

        for (RoomDemandSummary summary : summaries) {
            boolean requestedTypeSelected = roomTypeFilter == null
                    || summary.roomType.equalsIgnoreCase(roomTypeFilter);
            boolean hasFilteredDemand = summary.vipRequests > 0;

            if (requestedTypeSelected && hasFilteredDemand) {
                relevant[index++] = summary;
            }
        }

        return relevant;
    }

    private RoomDemandSummary findSummary(RoomDemandSummary[] summaries, String roomType) {
        if (roomType == null) {
            return null;
        }

        for (RoomDemandSummary summary : summaries) {
            if (summary.roomType.equalsIgnoreCase(roomType)) {
                return summary;
            }
        }

        return null;
    }

    private int countMatchingRooms(WalkInRegistration member, Room[] readyRooms) {
        int count = 0;

        for (Room room : readyRooms) {
            if (isSuitableRoom(member, room)) {
                count++;
            }
        }

        return count;
    }

    private boolean isSuitableRoom(WalkInRegistration member, Room room) {
        if (room == null || !room.isAssignable()) {
            return false;
        }

        boolean matchesType = room.getRoomType().equalsIgnoreCase(
                member.getRequestedRoomType());
        boolean enoughCapacity = room.getNoOfGuest() >= member.getNumberOfGuests();

        return matchesType && enoughCapacity;
    }

    private boolean matchesFilters(
            WalkInRegistration member,
            String keyword,
            LoyaltyTier tierFilter,
            String roomTypeFilter,
            String readinessFilter,
            int minimumGuests,
            boolean hasReadyMatch) {

        boolean matchesKeyword = matchesKeyword(member, keyword);
        boolean matchesTier = tierFilter == null || getTier(member) == tierFilter;
        boolean matchesRoomType = roomTypeFilter == null
                || member.getRequestedRoomType().equalsIgnoreCase(roomTypeFilter);
        boolean matchesGuestCount = member.getNumberOfGuests() >= minimumGuests;
        boolean matchesReadiness = matchesReadinessFilter(readinessFilter, hasReadyMatch);

        return matchesKeyword
                && matchesTier
                && matchesRoomType
                && matchesGuestCount
                && matchesReadiness;
    }

    private boolean matchesKeyword(WalkInRegistration member, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        String value = keyword.trim().toLowerCase();

        return member.getRegistrationId().toLowerCase().contains(value)
                || member.getGuest().getGuestId().toLowerCase().contains(value)
                || member.getGuest().getName().toLowerCase().contains(value);
    }

    private boolean matchesReadinessFilter(String readinessFilter, boolean hasReadyMatch) {
        String normalized = normalizeReadinessFilter(readinessFilter);

        if (READY.equals(normalized)) {
            return hasReadyMatch;
        }

        if (BLOCKED.equals(normalized)) {
            return !hasReadyMatch;
        }

        return true;
    }

    /**
     * Selection sort by management pressure: room types with more blocked VIP
     * requests are shown first, followed by room-count gap and request volume.
     */
    private void sortByAllocationPressure(RoomDemandSummary[] summaries) {
        for (int i = 0; i < summaries.length - 1; i++) {
            int bestIndex = i;

            for (int j = i + 1; j < summaries.length; j++) {
                if (comesBefore(summaries[j], summaries[bestIndex])) {
                    bestIndex = j;
                }
            }

            RoomDemandSummary temporary = summaries[i];
            summaries[i] = summaries[bestIndex];
            summaries[bestIndex] = temporary;
        }
    }

    private boolean comesBefore(RoomDemandSummary first, RoomDemandSummary second) {
        if (first.blockedVipRequests != second.blockedVipRequests) {
            return first.blockedVipRequests > second.blockedVipRequests;
        }

        int firstGap = Math.max(0, first.vipRequests - first.readyRooms);
        int secondGap = Math.max(0, second.vipRequests - second.readyRooms);

        if (firstGap != secondGap) {
            return firstGap > secondGap;
        }

        if (first.vipRequests != second.vipRequests) {
            return first.vipRequests > second.vipRequests;
        }

        return first.roomType.compareToIgnoreCase(second.roomType) < 0;
    }

    private RoomDemandSummary findHighestDemandSummary(RoomDemandSummary[] summaries) {
        RoomDemandSummary best = null;

        for (RoomDemandSummary summary : summaries) {
            if (best == null
                    || summary.vipRequests > best.vipRequests
                    || (summary.vipRequests == best.vipRequests
                    && summary.blockedVipRequests > best.blockedVipRequests)) {
                best = summary;
            }
        }

        return best;
    }

    private RoomDemandSummary findMostConstrainedSummary(RoomDemandSummary[] summaries) {
        RoomDemandSummary best = null;

        for (RoomDemandSummary summary : summaries) {
            if (best == null || comesBefore(summary, best)) {
                best = summary;
            }
        }

        if (best != null
                && best.blockedVipRequests == 0
                && Math.max(0, best.vipRequests - best.readyRooms) == 0) {
            return null;
        }

        return best;
    }

    private int findHighestPriorityBlockedVip(WalkInRegistration[] members, int[] matchCounts, int count) {
        int bestIndex = -1;

        for (int i = 0; i < count; i++) {
            if (matchCounts[i] > 0) {
                continue;
            }

            if (bestIndex < 0 || higherVipPriority(members[i], members[bestIndex])) {
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private boolean higherVipPriority(WalkInRegistration first, WalkInRegistration second) {
        int firstPriority = getTier(first).getPriority();
        int secondPriority = getTier(second).getPriority();

        if (firstPriority != secondPriority) {
            return firstPriority > secondPriority;
        }

        int timeComparison = first.getRegistrationTime()
                .compareTo(second.getRegistrationTime());

        if (timeComparison != 0) {
            return timeComparison < 0;
        }

        return first.getRegistrationId()
                .compareToIgnoreCase(second.getRegistrationId()) < 0;
    }

    private LoyaltyTier getTier(WalkInRegistration registration) {
        if (registration == null || registration.getGuest() == null
                || registration.getGuest().getGuestId() == null) {
            return null;
        }

        String guestId = registration.getGuest().getGuestId();

        for (LoyaltyProfile profile : loyaltyProfiles) {
            if (profile != null
                    && profile.getGuestId() != null
                    && profile.getGuestId().equalsIgnoreCase(guestId)) {
                return profile.getTier();
            }
        }

        return null;
    }

    private String buildManagementInsight(
            int totalRequests,
            int totalBlocked,
            RoomDemandSummary mostConstrained) {

        if (totalBlocked == 0) {
            return "Every filtered VIP currently has at least one suitable ready-room match.";
        }

        if (mostConstrained == null) {
            return totalBlocked + " filtered VIP request(s) currently have no suitable ready-room match.";
        }

        return totalBlocked + " filtered VIP request(s) are blocked; "
                + formatRoomType(mostConstrained.roomType)
                + " currently shows the greatest allocation pressure among the displayed room types.";
    }

    private String displaySummaryType(RoomDemandSummary summary) {
        return summary == null ? "NONE" : formatRoomType(summary.roomType);
    }

    private String normalizeReadinessFilter(String readinessFilter) {
        if (readinessFilter == null || readinessFilter.isBlank()) {
            return ALL;
        }

        String value = readinessFilter.trim().toUpperCase();

        if (READY.equals(value) || BLOCKED.equals(value)) {
            return value;
        }

        return ALL;
    }

    private String displayFilter(String value, String allLabel) {
        return value == null || value.isBlank() ? allLabel : value.trim();
    }

    private String formatRoomType(String roomType) {
        return roomType == null ? "-" : roomType.replace('_', ' ');
    }

    private static class RoomDemandSummary {
        private final String roomType;
        private int vipRequests;
        private int guestDemand;
        private int readyRooms;
        private int vipWithReadyMatch;
        private int blockedVipRequests;

        private RoomDemandSummary(String roomType) {
            this.roomType = roomType;
        }
    }
}