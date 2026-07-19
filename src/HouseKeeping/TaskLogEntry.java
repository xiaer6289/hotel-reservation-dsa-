package HouseKeeping;

import entity.Room;

/**
 *
 * @author Low Wei Shin
 */

public class TaskLogEntry {

    private String taskId;
    private Room room;
    private String staffId;
    private String timestamp;
    private String notes;

    public TaskLogEntry(String taskId, Room room, String status, String staffId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("taskId cannot be null or empty");
        }
        if (room == null) {
            throw new IllegalArgumentException("room cannot be null");
        }
        if (staffId == null || staffId.trim().isEmpty()) {
            throw new IllegalArgumentException("staffId cannot be null or empty");
        }
        this.taskId = taskId;
        this.room = room;
        this.room.setStatus(status);
        this.staffId = staffId;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("taskId cannot be null or empty");
        }
        this.taskId = taskId;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("room cannot be null");
        }
        String currentStatus = getStatus();
        this.room = room;
        if (currentStatus != null) {
            this.room.setStatus(currentStatus);
        }
    }

    public String getStatus() {
        return room == null ? null : room.getStatus();
    }

    public void setStatus(String status) {
        if (room == null) {
            throw new IllegalStateException("room is required before setting status");
        }
        room.setStatus(status);
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        String roomNumber = room == null ? "N/A" : room.getRoomNumber();
        String status = getStatus();
        return taskId + " | Room " + roomNumber + " | " + status + " | " + staffId;
    }
}
