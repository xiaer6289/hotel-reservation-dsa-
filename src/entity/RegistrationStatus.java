package entity;

/**
 * Fixed status values used by WalkInRegistration.
 *
 * @author Lai Jen Feng
 */
public enum RegistrationStatus {
    WAITING("Waiting"),
    VIP_WAITING("VIP Waiting"),
    PROCESSED("Processed"),
    CHECKED_IN("Checked-In"),
    CHECKED_OUT("Checked-Out"),
    CANCELLED("Cancelled");

    private final String displayName;

    RegistrationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}