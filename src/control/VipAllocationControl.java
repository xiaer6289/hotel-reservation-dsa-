package control;

import adt.bst.PriorityHeapInterface;
import adt.bst.VipPriorityMaxHeap;
import dao.VipAllocationDao;
import entity.LoyaltyTier;
import entity.RoomAllocation;
import entity.RoomType;
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
    private static final int DEFAULT_GUEST_CAPACITY = 100;
    private static final int DEFAULT_ALLOCATION_CAPACITY = 100;

    public VipAllocationControl() {
        vipWaitingHeap = new VipPriorityMaxHeap();
        allocations = new RoomAllocation[DEFAULT_ALLOCATION_CAPACITY];
        dao = new VipAllocationDao();
        loadWaitingGuests();
        allocationCount = dao.loadAllocations(allocations);
    }

    private void loadWaitingGuests() {
        VipGuest[] loadedGuests = new VipGuest[DEFAULT_GUEST_CAPACITY];
        int guestCount = dao.loadGuests(loadedGuests);
        for (int i = 0; i < guestCount; i++) {
            if (loadedGuests[i] != null) {
                vipWaitingHeap.add(loadedGuests[i]);
            }
        }
    }

    public boolean registerVipGuest(String guestId, String guestName, Long phoneNo, LoyaltyTier loyaltyTier, RoomType requestedRoomType) {
        if (guestId == null || guestId.isBlank() || guestName == null || guestName.isBlank() || phoneNo == null || phoneNo <= 0 || loyaltyTier == null || requestedRoomType == null) {
            return false;
        }
        String formattedGuestId = guestId.trim().toUpperCase();
        if (searchWaitingGuestById(formattedGuestId) != null || searchAllocatedGuestById(formattedGuestId) != null) {
            return false;
        }
        VipGuest guest = new VipGuest(formattedGuestId, guestName.trim(), phoneNo, loyaltyTier, requestedRoomType, LocalDateTime.now());
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
        RoomAllocation allocation = new RoomAllocation(allocationId, guest, roomNumber.trim().toUpperCase(), LocalDateTime.now());
        allocations[allocationCount] = allocation;
        allocationCount++;
        saveAllData();
        return allocation;
    }

    public VipGuest searchWaitingGuestById(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            return null;
        }
        for (int i = 0; i < vipWaitingHeap.getNumberOfEntries(); i++) {
            VipGuest guest = vipWaitingHeap.getEntry(i);
            if (guest != null && guest.getGuestId().equalsIgnoreCase(guestId.trim())) {
                return guest;
            }
        }
        return null;
    }

    public VipGuest searchAllocatedGuestById(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            return null;
        }
        for (int i = 0; i < allocationCount; i++) {
            RoomAllocation allocation = allocations[i];
            if (allocation != null && allocation.getGuest().getGuestId().equalsIgnoreCase(guestId.trim())) {
                return allocation.getGuest();
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
        selectionSortGuests(result);
        return result;
    }

    public VipGuest[] filterWaitingGuests(LoyaltyTier tier, RoomType roomType) {
        int heapSize = vipWaitingHeap.getNumberOfEntries();
        VipGuest[] temporary = new VipGuest[heapSize];
        int count = 0;
        for (int i = 0; i < heapSize; i++) {
            VipGuest guest = vipWaitingHeap.getEntry(i);
            boolean tierMatches = tier == null || guest.getLoyaltyTier() == tier;
            boolean roomMatches = roomType == null || guest.getRequestedRoomType() == roomType;
            if (tierMatches && roomMatches) {
                temporary[count] = guest;
                count++;
            }
        }
        VipGuest[] result = new VipGuest[count];
        for (int i = 0; i < count; i++) {
            result[i] = temporary[i];
        }
        selectionSortGuests(result);
        return result;
    }

    private void selectionSortGuests(VipGuest[] guests) {
        for (int i = 0; i < guests.length - 1; i++) {
            int highestIndex = i;
            for (int j = i + 1; j < guests.length; j++) {
                if (hasHigherPriority(guests[j], guests[highestIndex])) {
                    highestIndex = j;
                }
            }
            if (highestIndex != i) {
                VipGuest temporary = guests[i];
                guests[i] = guests[highestIndex];
                guests[highestIndex] = temporary;
            }
        }
    }

    public int countWaitingByTier(LoyaltyTier tier) {
        int count = 0;
        for (int i = 0; i < vipWaitingHeap.getNumberOfEntries(); i++) {
            VipGuest guest = vipWaitingHeap.getEntry(i);
            if (guest.getLoyaltyTier() == tier) {
                count++;
            }
        }
        return count;
    }

    public int countAllocatedByTier(LoyaltyTier tier) {
        int count = 0;
        for (int i = 0; i < allocationCount; i++) {
            if (allocations[i].getGuest().getLoyaltyTier()== tier) {
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
            LocalDateTime requestTime = allocations[i].getGuest().getRequestTime();
            LocalDateTime allocationTime = allocations[i].getAllocationTime();
            totalMinutes += Duration.between(requestTime, allocationTime).toMinutes();
        }
        return (double) totalMinutes / allocationCount;
    }

    public RoomAllocation[] getAllocationsSortedByLatest() {
        RoomAllocation[] result = new RoomAllocation[allocationCount];
        for (int i = 0; i < allocationCount; i++) {
            result[i] = allocations[i];
        }
        bubbleSortAllocationsByLatest(result);
        return result;
    }

    private void bubbleSortAllocationsByLatest(RoomAllocation[] result) {
        for (int pass = 0; pass < result.length - 1; pass++) {
            boolean swapped = false;
            for (int i = 0; i < result.length - 1 - pass; i++) {
                if (result[i].getAllocationTime().isBefore(result[i + 1].getAllocationTime())) {
                    RoomAllocation temporary = result[i];
                    result[i] = result[i + 1];
                    result[i + 1] = temporary;
                    swapped = true;
                }
            }
            if (!swapped) {
                break;
            }
        }
    }

    public int getWaitingCount() {
        return vipWaitingHeap.getNumberOfEntries();
    }

    public int getAllocationCount() {
        return allocationCount;
    }

    public void addSampleData() {
        registerSample("V001", "Alicia Tan", 60123456781L, LoyaltyTier.ELITE, RoomType.DELUXE, 18);
        registerSample("V002", "Bryan Lee", 60123456782L, LoyaltyTier.PLATINUM, RoomType.SUPERIOR, 15);
        registerSample("V003", "Carmen Lim", 60123456783L, LoyaltyTier.DIAMOND, RoomType.DELUXE_TWIN, 12);
        registerSample("V004", "Daniel Wong", 60123456784L, LoyaltyTier.PLATINUM, RoomType.SUPERIOR_TWIN, 8);
        registerSample("V005", "Emma Ng", 60123456785L, LoyaltyTier.ELITE, RoomType.DELUXE, 5);
        saveAllData();
    }

    private void registerSample(String id, String name, Long phoneNo, LoyaltyTier tier, RoomType roomType, int minutesAgo) {
        if (searchWaitingGuestById(id) == null && searchAllocatedGuestById(id) == null) {
            VipGuest guest = new VipGuest(id, name, phoneNo, tier, roomType, LocalDateTime.now().minusMinutes(minutesAgo));
            vipWaitingHeap.add(guest);
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
        int waitingCount = vipWaitingHeap.getNumberOfEntries();
        VipGuest[] waitingGuests = new VipGuest[waitingCount];
        for (int i = 0; i < waitingCount; i++) {
            waitingGuests[i] = vipWaitingHeap.getEntry(i);
        }
        dao.saveGuests(waitingGuests, waitingCount);
        dao.saveAllocations(allocations, allocationCount);
    }
}