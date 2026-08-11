package entity;

/**
 * Loyalty tiers used for VIP room-allocation priority.
 * A larger priority value means a higher position in the MaxHeap.
 *
 * Priority order: DIAMOND > PLATINUM > ELITE.
 *
 * @author Low Enn Toong
 */
public enum LoyaltyTier {
    ELITE(1),
    PLATINUM(2),
    DIAMOND(3);

    private final int priority;

    LoyaltyTier(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}