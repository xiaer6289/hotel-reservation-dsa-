package entity;

import java.io.Serializable;

/**
 * Stores an existing hotel loyalty membership profile.
 *
 * This profile is separate from a walk-in registration. Registration staff do
 * not choose a VIP tier during check-in; the system looks up the guest's
 * existing membership profile and uses the stored loyalty tier automatically.
 *
 * @author Low Enn Toong
 */
public class LoyaltyProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String memberId;
    private final String guestId;
    private final LoyaltyTier tier;

    public LoyaltyProfile(
            String memberId,
            String guestId,
            LoyaltyTier tier) {

        this.memberId = memberId;
        this.guestId = guestId;
        this.tier = tier;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getGuestId() {
        return guestId;
    }

    public LoyaltyTier getTier() {
        return tier;
    }

    @Override
    public String toString() {
        return String.format(
                "Member ID: %s | Guest ID: %s | Loyalty Tier: %s",
                memberId,
                guestId,
                tier);
    }
}