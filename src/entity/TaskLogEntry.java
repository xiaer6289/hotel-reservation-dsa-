package entity;

/**
 *
 * @author Low Wei Shin
 */
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;

public class TaskLogEntry implements Serializable {

    // Bumped so old tasklog.dat files are rejected cleanly instead of
    // throwing a cryptic InvalidClassException at runtime.
    private static final long serialVersionUID = 2L;

    // -- Core fields --------------------------------------------------------
    private String taskId;
    private String roomNumber;
    private String status;          // Dirty, Cleaning In Progress, Inspected, Ready
    private String staffId;
    private String remarks;
    private LocalDateTime createdTime;
    private LocalDateTime lastUpdatedTime;
    private int estimatedMinutes;   // remaining estimated time (legacy display)

    // -- Countdown / KPI fields ---------------------------------------------
    /**
     * Set when staff is auto-assigned and the task moves to
     * "Cleaning In Progress". Used to drive the 30-minute countdown.
     */
    private LocalDateTime cleaningStartTime;

    /**
     * Set to {@code true} when the task reaches "Ready" status
     * within {@value #CLEANING_TARGET_MINUTES} minutes of
     * {@link #cleaningStartTime}.
     */
    private boolean completedWithinTarget;

    /** Target cleaning duration in minutes (used for KPI). */
    public static final int CLEANING_TARGET_MINUTES = 30;

    // -- Constructors -------------------------------------------------------

    public TaskLogEntry(String taskId, String roomNumber, String status, String staffId) {
        this(taskId, roomNumber, status, staffId, null);
    }

    public TaskLogEntry(String taskId, String roomNumber, String status, String staffId, String remarks) {
        this.taskId           = taskId;
        this.roomNumber       = roomNumber;
        this.status           = status;
        this.staffId          = staffId;
        this.remarks          = remarks;
        this.createdTime      = LocalDateTime.now();
        this.lastUpdatedTime  = LocalDateTime.now();
        this.estimatedMinutes = calculateEstimatedMinutes(status);
        this.completedWithinTarget = false;
    }

    /**
     * Package-level constructor used by the seeder to backdate tasks
     * across multiple months for demonstration data.
     */
    public TaskLogEntry(String taskId, String roomNumber, String status,
                        String staffId, String remarks,
                        LocalDateTime createdTime, LocalDateTime lastUpdatedTime,
                        LocalDateTime cleaningStartTime, boolean completedWithinTarget) {
        this.taskId                = taskId;
        this.roomNumber            = roomNumber;
        this.status                = status;
        this.staffId               = staffId;
        this.remarks               = remarks;
        this.createdTime           = createdTime;
        this.lastUpdatedTime       = lastUpdatedTime;
        this.cleaningStartTime     = cleaningStartTime;
        this.completedWithinTarget = completedWithinTarget;
        this.estimatedMinutes      = calculateEstimatedMinutes(status);
    }

    // -- Private helpers ----------------------------------------------------

    private int calculateEstimatedMinutes(String status) {
        switch (status) {
            case "Dirty":               return 35;
            case "Cleaning In Progress": return 15;
            case "Inspected":           return 10;
            case "Ready":               return 0;
            default:                    return 0;
        }
    }

    // -- Getters ------------------------------------------------------------

    public String        getTaskId()            { return taskId; }
    public String        getRoomNumber()        { return roomNumber; }
    public String        getStatus()            { return status; }
    public String        getStaffId()           { return staffId; }
    public String        getRemarks()           { return remarks; }
    public LocalDateTime getCreatedTime()       { return createdTime; }
    public LocalDateTime getLastUpdatedTime()   { return lastUpdatedTime; }
    public int           getEstimatedMinutes()  { return estimatedMinutes; }
    public LocalDateTime getCleaningStartTime() { return cleaningStartTime; }
    public boolean       isCompletedWithinTarget() { return completedWithinTarget; }

    // -- Setters ------------------------------------------------------------

    public void setStatus(String status) {
        String old = this.status;
        this.status           = status;
        this.lastUpdatedTime  = LocalDateTime.now();
        this.estimatedMinutes = calculateEstimatedMinutes(status);

        // Start countdown when cleaning begins
        if ("Cleaning In Progress".equals(status) && !"Cleaning In Progress".equals(old)) {
            this.cleaningStartTime = LocalDateTime.now();
        }

        // Evaluate KPI when task is completed
        if ("Ready".equals(status) && cleaningStartTime != null) {
            long minutesTaken = Duration.between(cleaningStartTime, lastUpdatedTime).toMinutes();
            this.completedWithinTarget = minutesTaken <= CLEANING_TARGET_MINUTES;
        }
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    // -- Countdown helpers --------------------------------------------------

    /**
     * Returns {@code true} if this task is currently being cleaned
     * and the countdown is running.
     */
    public boolean isCleaningCountdownActive() {
        return "Cleaning In Progress".equals(status) && cleaningStartTime != null;
    }

    /**
     * Returns minutes remaining in the 30-minute cleaning target,
     * clamped to [0, 30]. Returns -1 if not in cleaning state.
     */
    public long getRemainingCleaningMinutes() {
        if (!isCleaningCountdownActive()) {
            return -1;
        }
        long elapsed = Duration.between(cleaningStartTime, LocalDateTime.now()).toMinutes();
        return Math.max(0, CLEANING_TARGET_MINUTES - elapsed);
    }

    /**
     * Returns the actual cleaning duration in minutes (start -> ready/now).
     * Returns 0 if cleaning never started.
     */
    public long getCleaningDurationMinutes() {
        if (cleaningStartTime == null) return 0;
        LocalDateTime end = "Ready".equals(status) ? lastUpdatedTime : LocalDateTime.now();
        return Math.max(0, Duration.between(cleaningStartTime, end).toMinutes());
    }

    // -- Legacy time-spent helper (from creation) ---------------------------

    /**
     * Returns total minutes from task creation to completion (for Ready tasks)
     * or to the current moment (for active tasks).
     * Using lastUpdatedTime for Ready tasks ensures historical records always
     * show the real duration, not the ever-growing time since they were created.
     */
    public long getMinutesSpent() {
        LocalDateTime endTime;
        if ("Ready".equals(status) && lastUpdatedTime != null) {
            endTime = lastUpdatedTime;   // historical task - use actual finish time
        } else {
            endTime = LocalDateTime.now(); // active task - show live elapsed time
        }
        return Math.max(0, Duration.between(createdTime, endTime).toMinutes());
    }

    public String getTimeSpentLabel() {
        long totalMinutes = getMinutesSpent();
        long hours   = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours <= 0) return minutes + " min";
        return hours + " hr " + minutes + " min";
    }

    // -- toString -----------------------------------------------------------

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s | Room %-4s | %-22s | Staff: %-5s | Spent: %s",
                taskId, roomNumber, status,
                staffId == null || staffId.isBlank() ? "QUEUE" : staffId,
                getTimeSpentLabel()));

        if (isCleaningCountdownActive()) {
            sb.append(String.format(" |  %d min left", getRemainingCleaningMinutes()));
        }
        if (remarks != null && !remarks.isBlank()) {
            sb.append(" | Note: ").append(remarks);
        }
        return sb.toString();
    }
}