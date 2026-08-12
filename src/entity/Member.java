package entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A loyalty member waiting for room allocation.
 *
 * The complete WalkInRegistration is stored in this entity so the VIP heap can
 * compare priority and still access the requested room details.
 *
 * @author Low Enn Toong
 */
public class Member implements Comparable<Member>, Serializable {
    private static final long serialVersionUID = 1L;
    private final String memberId;
    private final WalkInRegistration registration;
    private final LoyaltyTier tier;

    public Member(String memberId, WalkInRegistration registration, LoyaltyTier tier) {
        this.memberId = memberId;
        this.registration = registration;
        this.tier = tier;
    }

    @Override
    public int compareTo(Member other) {
        int tierComparison = Integer.compare(tier.getPriority(), other.tier.getPriority());

        if (tierComparison != 0) {
            return tierComparison;
        }

        LocalDateTime thisTime = registration.getRegistrationTime();
        LocalDateTime otherTime = other.registration.getRegistrationTime();

        int timeComparison = otherTime.compareTo(thisTime);
        if (timeComparison != 0) {
            return timeComparison;
        }

        return other.registration.getRegistrationId().compareToIgnoreCase(registration.getRegistrationId());
    }

    public String getMemberId() {
        return memberId;
    }

    public WalkInRegistration getRegistration() {
        return registration;
    }

    public Guest getGuest() {
        return registration.getGuest();
    }

    public String getName() {
        return getGuest().getName();
    }

    public LoyaltyTier getTier() {
        return tier;
    }

    public int getPriorityScore() {
        return tier.getPriority();
    }

    @Override
    public String toString() {
        return String.format(
                "%-8s | Reg ID: %-6s | Guest ID: %-5s | " + "%-18s | %-10s | Priority: %d | Room: %s",
                memberId,
                registration.getRegistrationId(),
                getGuest().getGuestId(),
                getGuest().getName(),
                tier,
                tier.getPriority(),
                registration.getRequestedRoomType());
    }
}