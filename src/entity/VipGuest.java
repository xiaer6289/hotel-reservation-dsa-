package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author Low Enn Toong
 */
public class VipGuest extends Guest implements Serializable {

    private static final long serialVersionUID = 1L;

    private LoyaltyTier loyaltyTier;
    private RoomType requestedRoomType;
    private LocalDateTime requestTime;

    public VipGuest(
            String guestId,
            String guestName,
            Long phoneNo,
            LoyaltyTier loyaltyTier,
            RoomType requestedRoomType,
            LocalDateTime requestTime) {

        super(guestId, guestName, phoneNo);

        this.loyaltyTier = loyaltyTier;
        this.requestedRoomType = requestedRoomType;
        this.requestTime = requestTime;
    }

    public LoyaltyTier getLoyaltyTier() {
        return loyaltyTier;
    }

    public void setLoyaltyTier(LoyaltyTier loyaltyTier) {
        this.loyaltyTier = loyaltyTier;
    }

    public RoomType getRequestedRoomType() {
        return requestedRoomType;
    }

    public void setRequestedRoomType(RoomType requestedRoomType) {
        this.requestedRoomType = requestedRoomType;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public int getPriority() {
        return loyaltyTier.getPriority();
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return String.format(
                "%-8s %-20s %-14s %-10s %-18s %-16s",
                getGuestId(),
                getName(),
                getPhoneNo(),
                loyaltyTier,
                requestedRoomType,
                requestTime.format(formatter)
        );
    }
}