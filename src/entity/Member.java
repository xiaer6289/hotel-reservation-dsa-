package entity;

import java.time.LocalDateTime;

/**
 * A loyalty member waiting for a room allocation.
 *
 * The member keeps the related WalkInRegistration so the VIP heap can use the
 * guest's requested room type, number of guests, check-in and check-out time.
 *
 * @author Low Enn Toong
 */
public class Member implements Comparable<Member> {

    private final String memberId;
    private final WalkInRegistration registration;
    private final LoyaltyTier tier;

    public Member(
            String memberId,
            WalkInRegistration registration,
            LoyaltyTier tier) {

        this.memberId = memberId;
        this.registration = registration;
        this.tier = tier;
    }

    @Override
    public int compareTo(Member other) {
        /*
         * First priority: loyalty tier.
         * A larger priority value must move nearer to the MaxHeap root.
         */
        int tierComparison = Integer.compare(
                this.tier.getPriority(),
                other.tier.getPriority());

        if (tierComparison != 0) {
            return tierComparison;
        }

        /*
         * Tie-breaker only when both members have the same tier:
         * the earlier registration receives the higher priority.
         */
        LocalDateTime thisTime = registration.getRegistrationTime();
        LocalDateTime otherTime = other.registration.getRegistrationTime();

        int timeComparison = otherTime.compareTo(thisTime);
        if (timeComparison != 0) {
            return timeComparison;
        }

        /* Lower registration ID is treated as earlier when timestamps match. */
        return other.registration.getRegistrationId()
                .compareToIgnoreCase(registration.getRegistrationId());
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
                "%-8s | Reg ID: %-6s | Guest ID: %-5s | %-18s | %-10s | Priority: %d | Room: %s",
                memberId,
                registration.getRegistrationId(),
                getGuest().getGuestId(),
                getGuest().getName(),
                tier,
                tier.getPriority(),
                registration.getRequestedRoomType());
    }
}