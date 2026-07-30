package entity;

/**
 *
 * @author Low Enn Toong
 */

public enum LoyaltyTier {
    ELITE(1),
    DIAMOND(2),
    PLATINUM(3);

    private final int priority;

    LoyaltyTier(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}