package control;

import adt.linear.DoublyLinkedList;
import adt.linear.LinearADT;
import dao.HousekeepingDao;
import dao.RoomDao;
import entity.Room;
import entity.RoomStatus;
import entity.TaskLogEntry;

/**
 *
 * @author Low Wei Shin
 */
public class HousekeepingController {

    private LinearADT<TaskLogEntry> taskLog;
    private final HousekeepingDao housekeepingDao;
    private final RoomDao roomDao;
    private Room[] rooms;

    public HousekeepingController() {
        this.housekeepingDao = new HousekeepingDao();
        this.roomDao = new RoomDao();
        this.taskLog = new DoublyLinkedList<>();
        this.rooms = roomDao.loadOrSeed();
        loadTaskLog();
    }

    private void loadTaskLog() {
        TaskLogEntry[] entries = housekeepingDao.loadOrSeed();
        taskLog.clear();
        for (TaskLogEntry entry : entries) {
            if (entry != null) {
                taskLog.addLast(entry);
            }
        }

        /*
         * IMPORTANT:
         * Do not rebuild the current Room status from historical housekeeping
         * tasks here. room.dat is the shared source of truth for the room's
         * CURRENT state. Replaying an old "Ready" task could otherwise change
         * a room that has since been assigned to a VIP/Standard guest from
         * OCCUPIED back to READY.
         *
         * Room status is still updated immediately when a NEW housekeeping
         * task is created or when an existing task is advanced/rolled back.
         */
    }

    private void persistTaskLog() {
        TaskLogEntry[] entries = new TaskLogEntry[taskLog.size()];
        final int[] index = {0};
        taskLog.traverse(entry -> entries[index[0]++] = entry);
        housekeepingDao.saveToFile(entries);
    }

    private void persistRooms() {
        roomDao.saveToFile(rooms);
    }

    /**
     * Maps a Housekeeping task status directly to a Room's entity status.
     * Also triggers any necessary cross-module notifications (e.g. telling Front Desk
     * that a room is now available for new guests).
     */
    private void applyTaskStatusToRoom(TaskLogEntry task, boolean notifyFrontDesk) {
        Room room = findRoomByNumber(task.getRoomNumber());
        if (room == null) {
            return;
        }

        switch (task.getStatus()) {
            case "Dirty":
                room.setRoomStatus(RoomStatus.DIRTY);
                break;
            case "Cleaning In Progress":
                room.setRoomStatus(RoomStatus.CLEANING_IN_PROGRESS);
                break;
            case "Inspected":
                room.setRoomStatus(RoomStatus.INSPECTED);
                break;
            case "Ready":
                room.setRoomStatus(RoomStatus.READY);
                if (notifyFrontDesk) {
                    RoomAvailabilityNotifier.notifyRoomReady(room);
                }
                break;
            default:
                break;
        }
    }

    private String nextTaskId() {
        int maxId = 0;
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task == null || task.getTaskId() == null) {
                continue;
            }

            String taskId = task.getTaskId().trim();
            if (taskId.startsWith("T")) {
                try {
                    maxId = Math.max(maxId, Integer.parseInt(taskId.substring(1)));
                } catch (NumberFormatException ex) {
                    // Ignore malformed task IDs.
                }
            }
        }
        return String.format("T%03d", maxId + 1);
    }

    private Room findRoomByNumber(String roomNumber) {
        refreshRooms();
        for (Room room : rooms) {
            if (room != null && room.getRoomNumber() != null && room.getRoomNumber().equals(roomNumber)) {
                return room;
            }
        }
        return null;
    }

    private TaskLogEntry findTaskById(String taskId) {
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task != null && task.getTaskId() != null && task.getTaskId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }

    private boolean hasActiveTaskForRoom(String roomNumber) {
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task == null || task.getRoomNumber() == null) {
                continue;
            }
            if (task.getRoomNumber().equals(roomNumber) && !"Ready".equals(task.getStatus())) {
                return true;
            }
        }
        return false;
    }

    private String mapRoomStatusToTaskStatus(RoomStatus roomStatus) {
        if (roomStatus == null) {
            return null;
        }

        switch (roomStatus) {
            case DIRTY:
                return "Dirty";
            case CLEANING_IN_PROGRESS:
                return "Cleaning In Progress";
            case INSPECTED:
                return "Inspected";
            case READY:
            case AVAILABLE:
                return "Ready";
            default:
                return null;
        }
    }

    private void syncRoomState(TaskLogEntry task) {
        applyTaskStatusToRoom(task, true);
        persistRooms();
    }

    /**
     * Logs a brand new housekeeping task manually. By default, new tasks are 
     * created with a 'Dirty' status. It also prevents duplicate active tasks 
     * for the same room.
     */
    public boolean logNewTask(String roomNumber, String staffId) {
        return logNewTask(roomNumber, staffId, null);
    }

    public boolean logNewTask(String roomNumber, String staffId, String remarks) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null) {
            System.out.println("❌ Room not found: " + roomNumber);
            return false;
        }
        if (hasActiveTaskForRoom(roomNumber)) {
            System.out.println("❌ Room " + roomNumber + " already has an active housekeeping task.");
            return false;
        }

        TaskLogEntry task = new TaskLogEntry(nextTaskId(), roomNumber, "Dirty", staffId, remarks);
        taskLog.addLast(task);
        syncRoomState(task);
        persistTaskLog();
        System.out.println("✅ New task logged for Room " + roomNumber + ": " + task.getTaskId());
        return true;
    }

    /**
     * Specialized method called by the Front Desk or Registration module when 
     * a guest checks out. It automatically creates a 'Dirty' task so housekeepers 
     * know the room needs to be cleaned for the next guest.
     */
    public TaskLogEntry createCheckoutTask(String roomNumber, String staffId) {
        return createCheckoutTask(roomNumber, staffId, null);
    }

    public TaskLogEntry createCheckoutTask(String roomNumber, String staffId, String remarks) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null) {
            System.out.println("❌ Room not found: " + roomNumber);
            return null;
        }

        if (hasActiveTaskForRoom(roomNumber)) {
            System.out.println("❌ Room " + roomNumber + " already has an active housekeeping task.");
            return null;
        }

        TaskLogEntry task = new TaskLogEntry(nextTaskId(), roomNumber, "Dirty", staffId, remarks);
        taskLog.addLast(task);
        syncRoomState(task);
        persistTaskLog();

        System.out.println("✅ Check-out processed for Room " + roomNumber + ". Housekeeping task created: " + task.getTaskId());
        return task;
    }

    public boolean resetToDefaultData() {
        taskLog.clear();
        rooms = roomDao.resetToDefaultReadyRooms();
        persistTaskLog();
        System.out.println("✅ Housekeeping data reset to default rooms with Ready status.");
        return true;
    }

    /**
     * Updates a task to a specific new status. It enforces a strict linear state 
     * flow: Dirty → Cleaning In Progress → Inspected → Ready.
     * 
     * @param taskId The unique ID of the task to update (e.g., T001)
     * @param newStatus The target status
     * @return true if update was successful, false if invalid transition
     */
    public boolean updateTaskStatus(String taskId, String newStatus) {
        TaskLogEntry task = findTaskById(taskId);
        if (task != null) {
            if (!isValidTransition(task.getStatus(), newStatus)) {
                System.out.println("❌ Invalid status transition: " + task.getStatus() + " → " + newStatus);
                return false;
            }
            String oldStatus = task.getStatus();
            task.setStatus(newStatus);
            syncRoomState(task);
            persistTaskLog();
            System.out.println("✅ Task " + taskId + " updated: " + oldStatus + " → " + newStatus);
            return true;
        }
        System.out.println("❌ Task not found: " + taskId);
        return false;
    }

    /**
     * Reverts a task to its previous logical status in case a housekeeper made a mistake.
     * Prevents rolling back past the initial 'Dirty' state.
     */
    public boolean rollbackTask(String taskId) {
        TaskLogEntry task = findTaskById(taskId);
        if (task != null) {
            String current = task.getStatus();
            String previous = getPreviousStatus(current);
            if (previous != null) {
                task.setStatus(previous);
                syncRoomState(task);
                persistTaskLog();
                System.out.println("🔄 Task " + taskId + " rolled back to: " + previous);
                return true;
            } else {
                System.out.println("❌ Cannot rollback task from initial status (Dirty).");
                return false;
            }
        }
        System.out.println("❌ Task not found: " + taskId);
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

    private String getNextStatus(String current) {
        switch (current) {
            case "Dirty":
                return "Cleaning In Progress";
            case "Cleaning In Progress":
                return "Inspected";
            case "Inspected":
                return "Ready";
            default:
                return null;
        }
    }

    private boolean isValidTransition(String current, String next) {
        return next != null && next.equals(getNextStatus(current));
    }

    // 4. Search task by Room Number
    public void searchByRoom(String roomNumber) {
        System.out.println("🔍 Tasks for Room " + roomNumber + ":");
        boolean found = false;
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task != null && task.getRoomNumber() != null && task.getRoomNumber().equals(roomNumber)) {
                System.out.println(task);
                found = true;
            }
        }
        if (!found) {
            System.out.println("No tasks found for this room.");
        }
    }

    public void displayRoomStatus() {
        refreshRooms();
        System.out.println("\n=== ROOM HOUSEKEEPING STATUS ===");
        System.out.printf("%-8s | %-22s | %-10s | %-22s%n", "Room", "Room Status", "Available", "Housekeeping");
        System.out.println("--------------------------------------------------------------------------");
        for (Room room : rooms) {
            if (room == null) {
                continue;
            }
            String housekeepingStatus = mapRoomStatusToTaskStatus(room.getRoomStatus());
            System.out.printf("%-8s | %-22s | %-10s | %-22s%n",
                    room.getRoomNumber(),
                    describeRoomStatus(room.getRoomStatus()),
                    room.isAvailability() ? "Yes" : "No",
                    housekeepingStatus == null ? "N/A" : housekeepingStatus);
        }
        System.out.println("--------------------------------------------------------------------------");
    }

    public void displayDirtyRooms() {
        displayRoomsByStatus(RoomStatus.DIRTY.getCode(), "DIRTY ROOMS");
    }

    public void displayReadyRooms() {
        displayRoomsByStatus(RoomStatus.READY.getCode(), "READY ROOMS");
    }

    private void displayRoomsByStatus(char status, String title) {
        refreshRooms();
        System.out.println("\n=== " + title + " ===");
        System.out.printf("%-8s | %-22s | %-10s%n", "Room", "Room Status", "Available");
        System.out.println("-----------------------------------------------");

        boolean found = false;
        for (Room room : rooms) {
            if (room == null) {
                continue;
            }
            if (room.getStatus() == status) {
                System.out.printf("%-8s | %-22s | %-10s%n",
                        room.getRoomNumber(),
                        describeRoomStatus(room.getRoomStatus()),
                        room.isAvailability() ? "Yes" : "No");
                found = true;
            }
        }

        if (!found) {
            System.out.println("No rooms found for this filter.");
        }
        System.out.println("-----------------------------------------------");
    }

    public void showTasksForRoom(String roomNumber) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null) {
            System.out.println("❌ Room not found: " + roomNumber);
            return;
        }

        System.out.println("\nRoom " + roomNumber + " status: " + describeRoomStatus(room.getRoomStatus()));
        searchByRoom(roomNumber);
    }

    private String describeRoomStatus(RoomStatus status) {
        return status == null ? "Unknown" : status.getDisplayName();
    }

    public Room[] getRooms() {
        refreshRooms();
        return rooms;
    }

    private void refreshRooms() {
        rooms = roomDao.loadOrSeed();
    }

    // Getter for UI access if needed
    public LinearADT<TaskLogEntry> getTaskLog() {
        return taskLog;
    }

    // ===== Boundary display helpers (ECB: Boundary does not access Entity/ADT objects) =====
    public String[][] getRoomSelectionDisplayData() {
        Room[] currentRooms = getRooms();
        String[][] rows = new String[currentRooms == null ? 0 : currentRooms.length][2];
        if (currentRooms == null) {
            return rows;
        }
        for (int i = 0; i < currentRooms.length; i++) {
            if (currentRooms[i] != null) {
                rows[i][0] = currentRooms[i].getRoomNumber();
                rows[i][1] = currentRooms[i].getStatusLabel();
            }
        }
        return rows;
    }

    public String[][] getTaskSelectionDisplayData() {
        String[][] rows = new String[taskLog == null ? 0 : taskLog.size()][3];
        if (taskLog == null) {
            return rows;
        }
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task != null) {
                rows[i][0] = task.getTaskId();
                rows[i][1] = task.getRoomNumber();
                rows[i][2] = task.getStatus();
            }
        }
        return rows;
    }

    public void generateCleaningStatusReport() {
        new control.report.CleaningStatusFlowRP().generateReport(taskLog);
    }

    public void generateDailyPerformanceReport() {
        new control.report.DailyPerformanceRP().generateReport(taskLog);
    }

    public void displayAllTasks() {
        taskLog.display();
    }
}