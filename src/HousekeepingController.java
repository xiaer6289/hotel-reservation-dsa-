
import HouseKeeping.TaskLogEntry;
import entity.Room;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Low Wei Shin
 */
public class HousekeepingController {

    private LinearADT<TaskLogEntry> taskLog;

    public HousekeepingController() {
        this.taskLog = new DoublyLinkedList<>();
        loadSampleData();
    }

    private void loadSampleData() {
        taskLog.addLast(new TaskLogEntry("T001", "101", "Dirty", "S001"));
        taskLog.addLast(new TaskLogEntry("T002", "205", "Cleaning In Progress", "S002"));
        taskLog.addLast(new TaskLogEntry("T003", "308", "Inspected", "S003"));
        taskLog.addLast(new TaskLogEntry("T004", "412", "Dirty", "S001"));
    }

    // 1. Log a new task
    public void logNewTask(String roomNumber, String staffId) {
        String taskId = "T" + (taskLog.size() + 1000);
        TaskLogEntry task = new TaskLogEntry(taskId, roomNumber, "Dirty", staffId);
        taskLog.addLast(task);
        System.out.println("✅ New task logged: " + task);
    }

    // 2. Update task status (Dirty → Cleaning → Inspected → Ready)
    public boolean updateTaskStatus(String taskId, String newStatus) {
        // Traverse the list to find the task
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task != null && task.getTaskId().equals(taskId)) {
                String oldStatus = task.getStatus();
                task.setStatus(newStatus);
                System.out.println("✅ Task " + taskId + " updated: " + oldStatus + " → " + newStatus);
                return true;
            }
        }
        System.out.println("❌ Task not found: " + taskId);
        return false;
    }

    // 3. Rollback last status change (simple version - reverts to previous logical
    // status)
    public boolean rollbackTask(String taskId) {
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task != null && task.getTaskId().equals(taskId)) {
                String current = task.getStatus();
                String previous = getPreviousStatus(current);
                if (previous != null) {
                    task.setStatus(previous);
                    System.out.println("🔄 Task " + taskId + " rolled back to: " + previous);
                    return true;
                }
            }
        }
        System.out.println("❌ Cannot rollback task: " + taskId);
        return false;
    }

    private String getPreviousStatus(String current) {
        switch (current) {
            case "Cleaning In Progress":
                return "Dirty";
            case "Inspected":
                return "Cleaning In Progress";
            case "Ready":
                return "Inspected";
            default:
                return null;
        }
    }

    // 4. Search task by Room Number
    public void searchByRoom(String roomNumber) {
        System.out.println("🔍 Tasks for Room " + roomNumber + ":");
        boolean found = false;
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task != null && task.getRoom() != null && task.getRoom().getRoomNumber() != null && task.getRoom().getRoomNumber().equals(roomNumber)) {
                System.out.println(task);
                found = true;
            }
        }
        if (!found)
            System.out.println("No tasks found for this room.");
    }

    // Report 1: Pending Tasks (sorted by Room Number)
    public void generatePendingTasksReport() {
        System.out.println("\n=== HOUSEKEEPING PENDING TASKS REPORT ===");
        System.out.println("Generated on: " + LocalDateTime.now());
        System.out.println("------------------------------------------------");

        List<TaskLogEntry> pending = new ArrayList<>();
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry t = taskLog.get(i);
            if (t != null && !"Ready".equals(t.getStatus())) {
                pending.add(t);
            }
        }

        // Simple bubble sort by room number
        pending.sort((a, b) -> a.getRoom().getRoomNumber().compareTo(b.getRoom().getRoomNumber()));

        for (TaskLogEntry t : pending) {
            System.out.println(t);
        }
        System.out.println("Total pending tasks: " + pending.size());
    }

    // Report 2: Daily Summary
    public void generateDailySummary() {
        System.out.println("\n=== DAILY HOUSEKEEPING SUMMARY ===");
        System.out.println("Generated on: " + LocalDateTime.now());
        System.out.println("------------------------------------------------");

        int dirty = 0, cleaning = 0, inspected = 0, ready = 0;

        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry t = taskLog.get(i);
            if (t == null)
                continue;
            switch (t.getStatus()) {
                case "Dirty":
                    dirty++;
                    break;
                case "Cleaning In Progress":
                    cleaning++;
                    break;
                case "Inspected":
                    inspected++;
                    break;
                case "Ready":
                    ready++;
                    break;
            }
        }

        System.out.println("Dirty rooms                  : " + dirty);
        System.out.println("Cleaning in progress         : " + cleaning);
        System.out.println("Inspected                    : " + inspected);
        System.out.println("Ready for check-in           : " + ready);
        System.out.println("Total tasks today            : " + taskLog.size());
        System.out.println("Rooms ready for new guests   : " + ready);
    }

    // Getter for UI access if needed
    public LinearADT<TaskLogEntry> getTaskLog() {
        return taskLog;
    }
}