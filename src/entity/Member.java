package entity;

/**
 *
 * @author Low Enn Toong
 */

public class Member implements Comparable<Member> {

    private String memberId;
    private String name;
    private String tier;          // Platinum, Gold, Silver, Regular
    private int priorityScore;
    private String roomPreference; // Standard, Deluxe, Suite

    public Member(String memberId, String name, String tier, String roomPreference) {
        this.memberId = memberId;
        this.name = name;
        this.tier = tier;
        this.roomPreference = roomPreference;
        this.priorityScore = calculatePriority(tier);
    }

    private int calculatePriority(String tier) {
        switch (tier.toUpperCase()) {
            case "PLATINUM": return 4;
            case "GOLD":     return 3;
            case "SILVER":   return 2;
            default:         return 1; // Regular
        }
    }

    @Override
    public int compareTo(Member other) {
        // Higher priorityScore comes first
        return Integer.compare(this.priorityScore, other.priorityScore);
    }

    // Getters
    public String getMemberId() { return memberId; }
    public String getName() { return name; }
    public String getTier() { return tier; }
    public int getPriorityScore() { return priorityScore; }
    public String getRoomPreference() { return roomPreference; }

    @Override
    public String toString() {
        return String.format("%-8s | %-18s | %-10s | Priority: %d | Pref: %s",
                memberId, name, tier, priorityScore, roomPreference);
    }
}