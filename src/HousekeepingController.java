

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
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task.getTaskId().equals(taskId)) {
                taskLog.removeAt(i);
                task.setStatus(newStatus);
                taskLog.addAt(i, task);
                System.out.println("Task " + taskId + " updated to " + newStatus + ".");
                return;
            }
        }

        System.out.println("Task not found: " + taskId);
    }

    public void rollbackTask(String taskId) {
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task.getTaskId().equals(taskId)) {
                String previousStatus = previousStatus(task.getStatus());
                taskLog.removeAt(i);

                if (previousStatus == null) {
                    System.out.println("Task " + taskId + " removed from the log.");
                } else {
                    task.setStatus(previousStatus);
                    taskLog.addAt(i, task);
                    System.out.println("Task " + taskId + " rolled back to " + previousStatus + ".");
                }
                return;
            }
        }

        System.out.println("Task not found: " + taskId);
    }

    private String previousStatus(String status) {
        if ("Ready".equals(status)) {
            return "Inspected";
        }
        if ("Inspected".equals(status)) {
            return "Cleaning In Progress";
        }
        if ("Cleaning In Progress".equals(status)) {
            return "Dirty";
        }
        return null;
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
