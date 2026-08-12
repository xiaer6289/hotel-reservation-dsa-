package entity;

import java.io.Serializable;

/**
 * Stores a hotel's loyalty profile using Guest ID as the profile identifier.
 *
 * Loyalty is intentionally simple for this assignment: a guest becomes a VIP
 * after reaching the required number of completed stays. The VIP room
 * allocation module still uses the resulting tier in the MaxHeap.
 *
 * Tier thresholds:
 * 3-5 completed stays  -> ELITE
 * 6-9 completed stays  -> PLATINUM
 * 10+ completed stays  -> DIAMOND
 *
 * @author Low Enn Toong
 */
public class LoyaltyProfile implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int ELITE_MIN_STAYS = 3;
    public static final int PLATINUM_MIN_STAYS = 6;
    public static final int DIAMOND_MIN_STAYS = 10;
    private final String guestId;
    private int completedStays;
    private LoyaltyTier tier;

    /**
     * Main constructor used by the simplified loyalty qualification logic.
     */
    public LoyaltyProfile(String guestId, int completedStays) {
        this.guestId = guestId;
        updateCompletedStays(completedStays);
    }

    /**
     * Compatibility constructor for older code/data that already stores a tier.
     */
    public LoyaltyProfile(String guestId, LoyaltyTier tier) {
        this.guestId = guestId;
        this.tier = tier;
        this.completedStays = minimumStaysForTier(tier);
    }


    public String getGuestId() {
        return guestId;
    }

    public int getCompletedStays() {
        return completedStays;
    }

    public LoyaltyTier getTier() {
        return tier;
    }

    /**
     * Completed stays never decrease. The tier is recalculated automatically.
     */
    public void updateCompletedStays(int newCompletedStays) {
        int safeCompletedStays = Math.max(0, newCompletedStays);

        if (safeCompletedStays < completedStays) {
            safeCompletedStays = completedStays;
        }

        completedStays = safeCompletedStays;
        tier = determineTier(completedStays);
    }

    /**
     * Supports older serialized LoyaltyProfile objects that did not yet store
     * completedStays. Their previous tier is converted to the minimum matching
     * stay count instead of losing the existing membership.
     */
    public boolean normalizeLegacyCompletedStays() {
        if (completedStays > 0 || tier == null) {
            return false;
        }

        completedStays = minimumStaysForTier(tier);
        return true;
    }

    public int getStaysUntilNextTier() {
        if (tier == null) {
            return Math.max(0, ELITE_MIN_STAYS - completedStays);
        }

        switch (tier) {
            case ELITE:
                return Math.max(0, PLATINUM_MIN_STAYS - completedStays);
            case PLATINUM:
                return Math.max(0, DIAMOND_MIN_STAYS - completedStays);
            case DIAMOND:
            default:
                return 0;
        }
    }

    public String getNextTierName() {
        if (tier == null) {
            return LoyaltyTier.ELITE.name();
        }

        switch (tier) {
            case ELITE:
                return LoyaltyTier.PLATINUM.name();
            case PLATINUM:
                return LoyaltyTier.DIAMOND.name();
            case DIAMOND:
            default:
                return "MAXIMUM TIER";
        }
    }

    public static LoyaltyTier determineTier(int completedStays) {
        if (completedStays >= DIAMOND_MIN_STAYS) {
            return LoyaltyTier.DIAMOND;
        }

        if (completedStays >= PLATINUM_MIN_STAYS) {
            return LoyaltyTier.PLATINUM;
        }

        if (completedStays >= ELITE_MIN_STAYS) {
            return LoyaltyTier.ELITE;
        }

        return null;
    }

    private static int minimumStaysForTier(LoyaltyTier tier) {
        if (tier == null) {
            return 0;
        }

        switch (tier) {
            case DIAMOND:
                return DIAMOND_MIN_STAYS;
            case PLATINUM:
                return PLATINUM_MIN_STAYS;
            case ELITE:
            default:
                return ELITE_MIN_STAYS;
        }
    }

    @Override
    public String toString() {
        return String.format(
                "Guest ID: %s | Completed Stays: %d | Loyalty Tier: %s",
                guestId, completedStays, tier);
    }
}