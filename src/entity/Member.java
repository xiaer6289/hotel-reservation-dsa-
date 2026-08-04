package entity;

/**
 *
 * @author Low Enn Toong
 */
public class Member implements Comparable<Member> {
    private String memberId;
    // Reuse the existing Guest entity.
    private Guest guest;
    // Reuse the existing LoyaltyTier enum.
    private LoyaltyTier tier;
    public Member(String memberId, Guest guest, LoyaltyTier tier) {
        this.memberId = memberId;
        this.guest = guest;
        this.tier = tier;
    }

    @Override
    public int compareTo(Member other) {
        // A higher tier priority becomes the MaxHeap root.
        return Integer.compare(this.tier.getPriority(), other.tier.getPriority());
    }

    public String getMemberId() {
        return memberId;
    }

    public Guest getGuest() {
        return guest;
    }

    public String getName() {
        return guest.getName();
    }

    public LoyaltyTier getTier() {
        return tier;
    }

    public int getPriorityScore() {
        return tier.getPriority();
    }

    @Override
    public String toString() {
        return String.format("%-8s | Guest ID: %-5s | %-18s | " + "%-10s | Priority: %d", memberId, guest.getGuestId(), guest.getName(), tier, tier.getPriority());
    }
}