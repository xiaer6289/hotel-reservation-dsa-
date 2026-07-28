package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author Low Enn Toong
 */
public class RoomAllocation implements Serializable {

    private static final long serialVersionUID = 1L;

    private String allocationId;
    private VipGuest guest;
    private String roomNumber;
    private LocalDateTime allocationTime;

    public RoomAllocation(String allocationId, VipGuest guest, String roomNumber, LocalDateTime allocationTime) {
        this.allocationId = allocationId;
        this.guest = guest;
        this.roomNumber = roomNumber;
        this.allocationTime = allocationTime;
    }

    public String getAllocationId() {
        return allocationId;
    }

    public void setAllocationId(String allocationId) {
        this.allocationId = allocationId;
    }

    public VipGuest getGuest() {
        return guest;
    }

    public void setGuest(VipGuest guest) {
        this.guest = guest;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public LocalDateTime getAllocationTime() {
        return allocationTime;
    }

    public void setAllocationTime(LocalDateTime allocationTime) {
        this.allocationTime = allocationTime;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return String.format("%-8s %-8s %-20s %-10s %-18s %-10s %-16s", allocationId, guest.getGuestId(), guest.getName(), guest.getLoyaltyTier(), guest.getRequestedRoomType(), roomNumber, allocationTime.format(formatter));
    }
}