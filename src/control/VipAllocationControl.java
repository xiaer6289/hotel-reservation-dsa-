package control;

import adt.bst.PriorityHeapInterface;
import adt.bst.VipPriorityMaxHeap;
import dao.VipAllocationDao;
import entity.LoyaltyTier;
import entity.RoomAllocation;
import entity.VipGuest;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 *
 * @author Low Enn Toong
 */
public class VipAllocationControl {
    private PriorityHeapInterface<VipGuest> vipWaitingHeap;
    private RoomAllocation[] allocations;
    private int allocationCount;
    private VipAllocationDao dao;

    public VipAllocationControl() {
        vipWaitingHeap = new VipPriorityMaxHeap();
        allocations = new RoomAllocation[100];
        dao = new VipAllocationDao();
        dao.loadGuests(vipWaitingHeap);
        allocationCount = dao.loadAllocations(allocations);
    }

    public boolean registerVipGuest(String guestId, String guestName,
                                    LoyaltyTier loyaltyTier, String requestedRoomType) {
        if (guestId == null || guestId.isBlank()
                || guestName == null || guestName.isBlank()
                || loyaltyTier == null
                || requestedRoomType == null || requestedRoomType.isBlank()) {
            return false;
        }

        if (searchWaitingGuestById(guestId) != null || searchAllocatedGuestById(guestId) != null) {
            return false;
        }

        VipGuest guest = new VipGuest(
                guestId.trim().toUpperCase(),
                guestName.trim(),
                loyaltyTier,
                requestedRoomType.trim().toUpperCase(),
                LocalDateTime.now());

        boolean added = vipWaitingHeap.add(guest);
        if (added) {
            saveAllData();
        }
        return added;
    }

    public VipGuest viewNextPriorityGuest() {
        return vipWaitingHeap.getHighestPriority();
    }

    public RoomAllocation allocateNextGuest(String roomNumber) {
        if (roomNumber == null || roomNumber.isBlank() || vipWaitingHeap.isEmpty()) {
            return null;
        }

        expandAllocationArrayIfNeeded();
        VipGuest guest = vipWaitingHeap.removeHighestPriority();
        String allocationId = String.format("A%04d", allocationCount + 1);

        RoomAllocation allocation = new RoomAllocation(
                allocationId,
                guest,
                roomNumber.trim().toUpperCase(),
                LocalDateTime.now());

        allocations[allocationCount] = allocation;
        allocationCount++;
        saveAllData();
        return allocation;
    }

    public VipGuest searchWaitingGuestById(String guestId) {
        if (guestId == null) {
            return null;
        }

        for (int i = 0; i < vipWaitingHeap.getNumberOfEntries(); i++) {
            VipGuest guest = vipWaitingHeap.getEntry(i);
            if (guest.getGuestId().equalsIgnoreCase(guestId.trim())) {
                return guest;
            }
        }
        return null;
    }

    public VipGuest searchAllocatedGuestById(String guestId) {
        if (guestId == null) {
            return null;
        }

        for (int i = 0; i < allocationCount; i++) {
            if (allocations[i].getGuest().getGuestId().equalsIgnoreCase(guestId.trim())) {
                return allocations[i].getGuest();
            }
        }
        return null;
    }

    public VipGuest[] getWaitingGuestsSorted() {
        int size = vipWaitingHeap.getNumberOfEntries();
        VipGuest[] result = new VipGuest[size];

        for (int i = 0; i < size; i++) {
            result[i] = vipWaitingHeap.getEntry(i);
        }

        // Selection sort: tier descending, then request time ascending.
        for (int i = 0; i < size - 1; i++) {
            int bestIndex = i;
            for (int j = i + 1; j < size; j++) {
                if (hasHigherPriority(result[j], result[bestIndex])) {
                    bestIndex = j;
                }
            }

            VipGuest temporary = result[i];
            result[i] = result[bestIndex];
            result[bestIndex] = temporary;
        }

        return result;
    }

    public VipGuest[] filterWaitingGuests(LoyaltyTier tier, String roomType) {
        VipGuest[] temporary = new VipGuest[vipWaitingHeap.getNumberOfEntries()];
        int count = 0;

        for (int i = 0; i < vipWaitingHeap.getNumberOfEntries(); i++) {
            VipGuest guest = vipWaitingHeap.getEntry(i);
            boolean tierMatches = tier == null || guest.getLoyaltyTier() == tier;
            boolean roomMatches = roomType == null || roomType.isBlank()
                    || guest.getRequestedRoomType().equalsIgnoreCase(roomType.trim());

            if (tierMatches && roomMatches) {
                temporary[count] = guest;
                count++;
            }
        }

        VipGuest[] result = new VipGuest[count];
        for (int i = 0; i < count; i++) {
            result[i] = temporary[i];
        }

        // Sort filtered result by priority.
        for (int i = 0; i < result.length - 1; i++) {
            int bestIndex = i;
            for (int j = i + 1; j < result.length; j++) {
                if (hasHigherPriority(result[j], result[bestIndex])) {
                    bestIndex = j;
                }
            }
            VipGuest temporaryGuest = result[i];
            result[i] = result[bestIndex];
            result[bestIndex] = temporaryGuest;
        }
        return result;
    }

    public int countWaitingByTier(LoyaltyTier tier) {
        int count = 0;
        for (int i = 0; i < vipWaitingHeap.getNumberOfEntries(); i++) {
            if (vipWaitingHeap.getEntry(i).getLoyaltyTier() == tier) {
                count++;
            }
        }
        return count;
    }

    public int countAllocatedByTier(LoyaltyTier tier) {
        int count = 0;
        for (int i = 0; i < allocationCount; i++) {
            if (allocations[i].getGuest().getLoyaltyTier() == tier) {
                count++;
            }
        }
        return count;
    }

    public double getAverageWaitingMinutes() {
        if (allocationCount == 0) {
            return 0;
        }

        long totalMinutes = 0;
        for (int i = 0; i < allocationCount; i++) {
            totalMinutes += Duration.between(
                    allocations[i].getGuest().getRequestTime(),
                    allocations[i].getAllocationTime()).toMinutes();
        }
        return (double) totalMinutes / allocationCount;
    }

    public RoomAllocation[] getAllocationsSortedByLatest() {
        RoomAllocation[] result = new RoomAllocation[allocationCount];
        for (int i = 0; i < allocationCount; i++) {
            result[i] = allocations[i];
        }

        // Bubble sort by allocation time descending.
        for (int pass = 0; pass < result.length - 1; pass++) {
            for (int i = 0; i < result.length - 1 - pass; i++) {
                if (result[i].getAllocationTime().isBefore(result[i + 1].getAllocationTime())) {
                    RoomAllocation temporary = result[i];
                    result[i] = result[i + 1];
                    result[i + 1] = temporary;
                }
            }
        }
        return result;
    }

    public int getWaitingCount() {
        return vipWaitingHeap.getNumberOfEntries();
    }

    public int getAllocationCount() {
        return allocationCount;
    }

    public void addSampleData() {
        registerSample("V001", "Alicia Tan", LoyaltyTier.ELITE, "DELUXE", 18);
        registerSample("V002", "Bryan Lee", LoyaltyTier.PLATINUM, "SUITE", 15);
        registerSample("V003", "Carmen Lim", LoyaltyTier.DIAMOND, "DELUXE", 12);
        registerSample("V004", "Daniel Wong", LoyaltyTier.PLATINUM, "EXECUTIVE", 8);
        registerSample("V005", "Emma Ng", LoyaltyTier.ELITE, "SUITE", 5);
        saveAllData();
    }

    private void registerSample(String id, String name, LoyaltyTier tier,
                                String roomType, int minutesAgo) {
        if (searchWaitingGuestById(id) == null && searchAllocatedGuestById(id) == null) {
            vipWaitingHeap.add(new VipGuest(
                    id,
                    name,
                    tier,
                    roomType,
                    LocalDateTime.now().minusMinutes(minutesAgo)));
        }
    }

    private boolean hasHigherPriority(VipGuest first, VipGuest second) {
        if (first.getPriority() != second.getPriority()) {
            return first.getPriority() > second.getPriority();
        }
        return first.getRequestTime().isBefore(second.getRequestTime());
    }

    private void expandAllocationArrayIfNeeded() {
        if (allocationCount < allocations.length) {
            return;
        }

        RoomAllocation[] largerArray = new RoomAllocation[allocations.length * 2];
        for (int i = 0; i < allocations.length; i++) {
            largerArray[i] = allocations[i];
        }
        allocations = largerArray;
    }

    private void saveAllData() {
        dao.saveGuests(vipWaitingHeap);
        dao.saveAllocations(allocations, allocationCount);
    }
}