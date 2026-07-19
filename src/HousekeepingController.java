

import HouseKeeping.TaskLogEntry;
import entity.Room;

/**
 *
 * @author Low Wei Shin
 */

public class HousekeepingController {
    private LinearADT<TaskLogEntry> taskLog;   // Your team's ADT

    public HousekeepingController() {
        taskLog = new DoublyLinkedList<>();
        loadSampleData();                     // or read from file
    }

    private void loadSampleData() {
        Room room101 = new Room();
        room101.setRoomNumber("101");

        Room room205 = new Room();
        room205.setRoomNumber("205");

        taskLog.addLast(new TaskLogEntry("T001", room101, "Dirty", "S001"));
        taskLog.addLast(new TaskLogEntry("T002", room205, "Cleaning In Progress", "S002"));
    }

    public void logNewTask(TaskLogEntry task) {
        taskLog.addLast(task);
        System.out.println("Task logged successfully.");
    }

    public void updateTaskStatus(String taskId, String newStatus) {
        // Traverse and update (demo of ADT usage)
        // Implementation depends on your ADT's search/remove
    }

    public void rollbackTask(String taskId) {
        // Find and revert status or remove
    }

    public void generatePendingTasksReport() {
        System.out.println("=== Pending Housekeeping Tasks ===");
        taskLog.display();
    }

    public void generateDailySummary() {
        // Filter + count logic
        System.out.println("=== Daily Housekeeping Summary ===");
        // Add sorting/filtering here
    }
}

