package HouseKeeping;


import entity.Room;

/**
 *
 * @author Low Wei Shin
 */

public class TaskLogEntry {

    private String taskId;
    private Room room;
    private String status; // Dirty, Cleaning In Progress, Inspected, Ready
    private String staffId;
    private String timestamp;
    private String notes;

    public TaskLogEntry(String taskId, Room room, String status, String staffId) {
        this.taskId = taskId;
        this.room = room;
        this.status = status;
        this.staffId = staffId;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

    public TaskLogEntry(String taskId2, String string, String status2, String staffId2) {
        //TODO Auto-generated constructor stub
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        return taskId + " | Room " + roomNumber + " | " + status + " | " + staffId;
    }
}
