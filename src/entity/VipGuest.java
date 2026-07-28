package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author Low Enn Toong
 */
public class VipGuest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String guestId;
    private String guestName;
    private LoyaltyTier loyaltyTier;
    private String requestedRoomType;
    private LocalDateTime requestTime;

    public VipGuest(String guestId, String guestName, LoyaltyTier loyaltyTier,
                    String requestedRoomType, LocalDateTime requestTime) {
        this.guestId = guestId;
        this.guestName = guestName;
        this.loyaltyTier = loyaltyTier;
        this.requestedRoomType = requestedRoomType;
        this.requestTime = requestTime;
    }

    public String getGuestId() {
        return guestId;
    }

    public String getGuestName() {
        return guestName;
    }

    public LoyaltyTier getLoyaltyTier() {
        return loyaltyTier;
    }

    public String getRequestedRoomType() {
        return requestedRoomType;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public int getPriority() {
        return loyaltyTier.getPriority();
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return String.format("%-8s %-20s %-10s %-15s %-16s",
                guestId,
                guestName,
                loyaltyTier,
                requestedRoomType,
                requestTime.format(formatter));
    }
}