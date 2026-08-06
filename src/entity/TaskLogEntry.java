package entity;

/**
 *
 * @author Low Wei Shin
 */
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;

public class TaskLogEntry implements Serializable {

    private static final long serialVersionUID = 1L;
    private String taskId;
    private String roomNumber;
    private String status;          // Dirty, Cleaning In Progress, Inspected, Ready
    private String staffId;
    private String remarks;
    private LocalDateTime createdTime;
    private LocalDateTime lastUpdatedTime;
    private int estimatedMinutes;   // remaining estimated time

    public TaskLogEntry(String taskId, String roomNumber, String status, String staffId) {
        this(taskId, roomNumber, status, staffId, null);
    }

    public TaskLogEntry(String taskId, String roomNumber, String status, String staffId, String remarks) {
        this.taskId = taskId;
        this.roomNumber = roomNumber;
        this.status = status;
        this.staffId = staffId;
        this.remarks = remarks;
        this.createdTime = LocalDateTime.now();
        this.lastUpdatedTime = LocalDateTime.now();
        this.estimatedMinutes = calculateEstimatedMinutes(status);
    }

    private int calculateEstimatedMinutes(String status) {
        switch (status) {
            case "Dirty": return 35;
            case "Cleaning In Progress": return 15;
            case "Inspected": return 10;
            case "Ready": return 0;
            default: return 0;
        }
    }

    // Getters
    public String getTaskId() { return taskId; }
    public String getRoomNumber() { return roomNumber; }
    public String getStatus() { return status; }
    public String getStaffId() { return staffId; }
    public String getRemarks() { return remarks; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public LocalDateTime getLastUpdatedTime() { return lastUpdatedTime; }
    public int getEstimatedMinutes() { return estimatedMinutes; }

    // Setters
    public void setStatus(String status) {
        this.status = status;
        this.lastUpdatedTime = LocalDateTime.now();
        this.estimatedMinutes = calculateEstimatedMinutes(status);
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public long getMinutesSpent() {
        return Duration.between(createdTime, LocalDateTime.now()).toMinutes();
    }

    public String getTimeSpentLabel() {
        long totalMinutes = getMinutesSpent();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours <= 0) {
            return minutes + " min";
        }

        return hours + " hr " + minutes + " min";
    }

    @Override
    public String toString() {
        return String.format("%-6s | Room %-4s | %-22s | Staff: %-5s | Spent: %s%s",
            taskId,
            roomNumber,
            status,
            staffId,
                getTimeSpentLabel(),
            remarks == null || remarks.isBlank() ? "" : " | Note: " + remarks);
    }
}