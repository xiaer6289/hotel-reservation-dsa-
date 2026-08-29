package entity;

import java.io.Serializable;

/**
 * Represents a housekeeping staff member.
 *
 * <p>Tracks whether the staff member is currently assigned to a room
 * so the auto-dispatch logic can select only free staff.</p>
 *
 * @author Low Wei Shin
 */
public class HousekeepingStaff implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String staffId;
    private final String name;
    private boolean busy;           // true = currently cleaning a room
    private String assignedRoom;    // room number currently being cleaned, null if free

    public HousekeepingStaff(String staffId, String name) {
        this.staffId = staffId;
        this.name    = name;
        this.busy    = false;
        this.assignedRoom = null;
    }

    // -- Getters ------------------------------------------------------------

    public String getStaffId()      { return staffId; }
    public String getName()         { return name; }
    public boolean isBusy()         { return busy; }
    public String getAssignedRoom() { return assignedRoom; }

    // -- Setters ------------------------------------------------------------

    public void assignTo(String roomNumber) {
        this.busy         = true;
        this.assignedRoom = roomNumber;
    }

    public void markFree() {
        this.busy         = false;
        this.assignedRoom = null;
    }

    // -- Display ------------------------------------------------------------

    @Override
    public String toString() {
        if (busy) {
            return String.format("%-5s %-12s BUSY  (cleaning Room %s)", staffId, name, assignedRoom);
        }
        return String.format("%-5s %-12s FREE", staffId, name);
    }
}
