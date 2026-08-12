package control.report;

import entity.LoyaltyTier;
import entity.Member;
import entity.Room;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Analyses whether each waiting VIP can currently be matched to a suitable
 * vacant room.
 *
 * Searching technique: linear search through VIP records and vacant rooms.
 * Sorting technique: selection sort by loyalty tier and arrival time.
 *
 * @author Low Enn Toong
 */
public class VipRoomReadinessRP {
    private static final String ALL = "ALL";
    private static final String MATCHED = "MATCHED";
    private static final String UNMATCHED = "UNMATCHED";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void generateReport(Member[] members, Room[] vacantRooms, String keyword, LoyaltyTier tierFilter, String roomTypeFilter, String readinessFilter, int minimumGuests) {
        if (members == null) {
            members = new Member[0];
        }

        if (vacantRooms == null) {
            vacantRooms = new Room[0];
        }

        Member[] temporaryMembers = new Member[members.length];
        int[] temporaryMatchCounts = new int[members.length];
        String[] temporarySuggestedRooms = new String[members.length];
        int count = 0;

        for (Member member : members) {
            if (member == null || member.getRegistration() == null) {
                continue;
            }

            int matchingRoomCount = countMatchingRooms(member, vacantRooms);
            String suggestedRoom = findSuggestedRoom(member, vacantRooms);
            boolean isMatched = matchingRoomCount > 0;

            if (matchesFilters(member, keyword, tierFilter, roomTypeFilter, readinessFilter, minimumGuests, isMatched)) {
                temporaryMembers[count] = member;
                temporaryMatchCounts[count] = matchingRoomCount;
                temporarySuggestedRooms[count] = suggestedRoom;
                count++;
            }
        }

        Member[] filteredMembers = new Member[count];
        int[] matchingRoomCounts = new int[count];
        String[] suggestedRooms = new String[count];

        System.arraycopy(temporaryMembers, 0, filteredMembers, 0, count);
        System.arraycopy(temporaryMatchCounts, 0, matchingRoomCounts, 0, count);
        System.arraycopy(temporarySuggestedRooms, 0, suggestedRooms, 0, count);

        sortByPriorityAndArrival(filteredMembers, matchingRoomCounts, suggestedRooms);

        printReport(filteredMembers, matchingRoomCounts, suggestedRooms, vacantRooms, keyword, tierFilter, roomTypeFilter, readinessFilter, minimumGuests);
    }

    private void printReport(Member[] members, int[] matchingRoomCounts, String[] suggestedRooms, Room[] vacantRooms, String keyword, LoyaltyTier tierFilter, String roomTypeFilter, String readinessFilter, int minimumGuests) {
        System.out.println("\n================================================================================================");
        System.out.println("                    VIP ROOM ALLOCATION READINESS REPORT");
        System.out.println("================================================================================================");
        System.out.println("Generated On       : " + LocalDateTime.now().format(DATE_TIME_FORMAT));
        System.out.println("Search Keyword     : " + displayFilter(keyword, ALL));
        System.out.println("Loyalty Tier       : " + (tierFilter == null ? ALL : tierFilter));
        System.out.println("Requested Room Type: " + (roomTypeFilter == null ? ALL : formatRoomType(roomTypeFilter)));
        System.out.println("Readiness Status   : " + normalizeReadinessFilter(readinessFilter));
        System.out.println("Minimum Guests     : " + minimumGuests);
        System.out.println("Search Technique   : Linear Search (VIP Records + Suitable Vacant Rooms)");
        System.out.println("Sorting Technique  : Selection Sort (Tier Descending, Arrival Time Ascending)");
        System.out.println("------------------------------------------------------------------------------------------------");

        if (members.length == 0) {
            System.out.println("No VIP allocation-readiness records match the selected criteria.");
            System.out.println("================================================================================================");
            return;
        }

        System.out.printf(
                "%-3s %-9s %-10s %-17s %-6s %-8s %-12s %-15s%n",
                "No.", "Reg ID", "Tier", "Requested Room", "Party", "Matches", "Suggested", "Readiness");
        System.out.println("------------------------------------------------------------------------------------------------");

        int matchedCount = 0;
        int unmatchedCount = 0;
        int totalMatchingRoomOptions = 0;

        for (int i = 0; i < members.length; i++) {
            boolean matched = matchingRoomCounts[i] > 0;

            System.out.printf(
                    "%-3d %-9s %-10s %-17s %-6d %-8d %-12s %-15s%n",
                    i + 1,
                    members[i].getRegistration().getRegistrationId(),
                    members[i].getTier(),
                    formatRoomType(members[i].getRegistration().getRequestedRoomType()),
                    members[i].getRegistration().getNumberOfGuests(),
                    matchingRoomCounts[i],
                    suggestedRooms[i],
                    matched ? "READY TO ALLOCATE" : "BLOCKED");

            if (matched) {
                matchedCount++;
            } else {
                unmatchedCount++;
            }

            totalMatchingRoomOptions += matchingRoomCounts[i];
        }

        double readinessRate = (double) matchedCount * 100.0 / members.length;

        System.out.println("------------------------------------------------------------------------------------------------");
        System.out.println("MANAGEMENT SUMMARY");
        System.out.println("Filtered VIP Demand       : " + members.length);
        System.out.println("Available Vacant Rooms    : " + vacantRooms.length);
        System.out.println("Ready for Allocation      : " + matchedCount);
        System.out.println("Blocked by Room Mismatch  : " + unmatchedCount);
        System.out.println("Total Suitable Room Options: " + totalMatchingRoomOptions);
        System.out.printf("Allocation Readiness Rate : %.2f%%%n", readinessRate);

        int nextReadyIndex = findFirstReadyIndex(matchingRoomCounts);

        System.out.println("Highest Priority Waiting  : "
                + members[0].getRegistration().getRegistrationId()
                + " / " + members[0].getGuest().getGuestId()
                + " (" + members[0].getTier() + ", "
                + (matchingRoomCounts[0] > 0 ? "READY" : "WAITING FOR SUITABLE ROOM") + ")");

        if (nextReadyIndex >= 0) {
            System.out.println("Next Allocatable VIP      : "
                    + members[nextReadyIndex].getRegistration().getRegistrationId()
                    + " / " + members[nextReadyIndex].getGuest().getGuestId()
                    + " (" + members[nextReadyIndex].getTier() + ", Room "
                    + suggestedRooms[nextReadyIndex] + ")");

            if (nextReadyIndex > 0) {
                System.out.println("Allocation Note           : Higher-priority VIP(s) without a suitable room remain waiting; the highest-priority eligible VIP may proceed.");
            }
        } else {
            System.out.println("Next Allocatable VIP      : NONE");
            System.out.println("Management Action         : Prepare a suitable ready room matching a waiting VIP's requested type and party size.");
        }

        System.out.println("================================================================================================");
    }

    /**
     * Linear search through all vacant rooms to count suitable matches.
     */
    private int countMatchingRooms(Member member, Room[] vacantRooms) {
        int count = 0;

        for (Room room : vacantRooms) {
            if (isSuitableRoom(member, room)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Linear search that returns the first suitable room number.
     */
    private String findSuggestedRoom(Member member, Room[] vacantRooms) {
        for (Room room : vacantRooms) {
            if (isSuitableRoom(member, room)) {
                return room.getRoomNumber();
            }
        }

        return "-";
    }

    private boolean isSuitableRoom(Member member, Room room) {
        if (room == null || !room.isAssignable()) {
            return false;
        }

        boolean matchesType = room.getRoomType().equalsIgnoreCase(member.getRegistration().getRequestedRoomType());
        boolean enoughCapacity = room.getNoOfGuest() >= member.getRegistration().getNumberOfGuests();

        return matchesType && enoughCapacity;
    }

    private boolean matchesFilters(Member member, String keyword, LoyaltyTier tierFilter, String roomTypeFilter, String readinessFilter, int minimumGuests, boolean isMatched) {
        boolean matchesKeyword = matchesKeyword(member, keyword);
        boolean matchesTier = tierFilter == null || member.getTier() == tierFilter;
        boolean matchesRoomType = roomTypeFilter == null || member.getRegistration().getRequestedRoomType().equalsIgnoreCase(roomTypeFilter);
        boolean matchesGuestCount = member.getRegistration().getNumberOfGuests() >= minimumGuests;
        boolean matchesReadiness = matchesReadinessFilter(readinessFilter, isMatched);

        return matchesKeyword && matchesTier && matchesRoomType && matchesGuestCount && matchesReadiness;
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

    private boolean matchesReadinessFilter(String readinessFilter, boolean isMatched) {
        String normalized = normalizeReadinessFilter(readinessFilter);

        if (MATCHED.equals(normalized)) {
            return isMatched;
        }

        if (UNMATCHED.equals(normalized)) {
            return !isMatched;
        }

        return true;
    }

    /**
     * Selection sort using parallel arrays so each member stays together with
     * its matching-room information.
     */
    private void sortByPriorityAndArrival(Member[] members, int[] matchingRoomCounts, String[] suggestedRooms) {
        for (int i = 0; i < members.length - 1; i++) {
            int bestIndex = i;

            for (int j = i + 1; j < members.length; j++) {
                if (comesBefore(members[j], members[bestIndex])) {
                    bestIndex = j;
                }
            }

            swapMembers(members, i, bestIndex);
            swapIntegers(matchingRoomCounts, i, bestIndex);
            swapStrings(suggestedRooms, i, bestIndex);
        }
    }

    private boolean comesBefore(Member first, Member second) {
        int firstPriority = first.getTier().getPriority();
        int secondPriority = second.getTier().getPriority();

        if (firstPriority != secondPriority) {
            return firstPriority > secondPriority;
        }

        int timeComparison = first.getRegistration().getRegistrationTime().compareTo(second.getRegistration().getRegistrationTime());

        if (timeComparison != 0) {
            return timeComparison < 0;
        }

        return first.getRegistration().getRegistrationId().compareToIgnoreCase(second.getRegistration().getRegistrationId()) < 0;
    }

    private int findFirstReadyIndex(int[] matchingRoomCounts) {
        for (int i = 0; i < matchingRoomCounts.length; i++) {
            if (matchingRoomCounts[i] > 0) {
                return i;
            }
        }
        return -1;
    }

    private void swapMembers(Member[] values, int first, int second) {
        Member temporary = values[first];
        values[first] = values[second];
        values[second] = temporary;
    }

    private void swapIntegers(int[] values, int first, int second) {
        int temporary = values[first];
        values[first] = values[second];
        values[second] = temporary;
    }

    private void swapStrings(String[] values, int first, int second) {
        String temporary = values[first];
        values[first] = values[second];
        values[second] = temporary;
    }

    private String normalizeReadinessFilter(String readinessFilter) {
        if (readinessFilter == null || readinessFilter.isBlank()) {
            return ALL;
        }

        String value = readinessFilter.trim().toUpperCase();

        if (MATCHED.equals(value) || UNMATCHED.equals(value)) {
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
}