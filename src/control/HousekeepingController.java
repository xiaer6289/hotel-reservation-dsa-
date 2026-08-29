package control;

import adt.linear.DoublyLinkedList;
import adt.linear.LinearADT;
import dao.HousekeepingDao;
import dao.RoomDao;
import entity.HousekeepingStaff;
import entity.Room;
import entity.RoomStatus;
import entity.TaskLogEntry;
import java.time.LocalDate;

/**
 * Controls all housekeeping operations.
 *
 * <p>Key design decisions in this revision:</p>
 * <ul>
 *   <li><b>FIFO Dirty-Room Queue</b> – when a room goes dirty and all staff
 *       are busy, the room number is enqueued in {@link #dirtyRoomQueue}
 *       ({@link LinkedQueue} backed by {@link DoublyLinkedList}).  A staff
 *       member is auto-assigned the moment one becomes free (see
 *       {@link #dispatchQueue()}).</li>
 *   <li><b>No manual assignment</b> – staff selection has been removed from
 *       the public API.  The caller only marks a room dirty; the controller
 *       picks the next free staff member automatically.</li>
 *   <li><b>30-minute countdown</b> – {@code cleaningStartTime} is recorded
 *       inside {@link TaskLogEntry} when cleaning begins.  The UI reads
 *       {@link TaskLogEntry#getRemainingCleaningMinutes()} to display the
 *       live countdown without any background threads.</li>
 *   <li><b>Early-finish override</b> – {@link #markStaffReady(String)} lets a
 *       staff member declare themselves finished before the 30 minutes elapse;
 *       the controller marks the task Ready, frees the staff, and immediately
 *       dispatches the next queued room.</li>
 * </ul>
 *
 * @author Low Wei Shin
 */
public class HousekeepingController {

    // ── ADTs ───────────────────────────────────────────────────────────────

    /** Historical + active task records (Doubly Linked List). */
    private LinearADT<TaskLogEntry> taskLog;

    /**
     * FIFO queue of dirty room numbers waiting for a free staff member.
     * Backed by {@link DoublyLinkedList}.
     */
    private LinearADT<String> dirtyRoomQueue;

    /**
     * Pool of all housekeeping staff (Doubly Linked List).
     * The controller iterates this to find the next free member.
     */
    private LinearADT<HousekeepingStaff> staffPool;

    // ── DAOs ───────────────────────────────────────────────────────────────

    private final HousekeepingDao housekeepingDao;
    private final RoomDao roomDao;
    private Room[] rooms;

    // ── Constructor ────────────────────────────────────────────────────────

    public HousekeepingController() {
        this.housekeepingDao  = new HousekeepingDao();
        this.roomDao          = new RoomDao();
        this.taskLog          = new DoublyLinkedList<>();
        this.dirtyRoomQueue   = new DoublyLinkedList<>();
        this.staffPool        = new DoublyLinkedList<>();
        this.rooms            = roomDao.loadOrSeed();

        initStaffPool();
        loadTaskLog();
        rebuildStaffBusyState(); // reconcile staff state from persisted tasks
    }

    // ── Initialisation ─────────────────────────────────────────────────────

    /** Populates the staff pool with the three known housekeeping staff. */
    private void initStaffPool() {
        staffPool.addLast(new HousekeepingStaff("S001", "Tan"));
        staffPool.addLast(new HousekeepingStaff("S002", "Choo"));
        staffPool.addLast(new HousekeepingStaff("S003", "Michelle"));
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

    /**
     * After loading persisted tasks, reconcile which staff members are
     * currently busy (i.e., have an active "Cleaning In Progress" task).
     * Also rebuilds the dirty-room queue for any rooms still in "Dirty"
     * status with no assigned staff.
     */
    private void rebuildStaffBusyState() {
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task == null) continue;

            if ("Cleaning In Progress".equals(task.getStatus())
                    && task.getStaffId() != null && !task.getStaffId().isBlank()) {
                HousekeepingStaff staff = findStaffById(task.getStaffId());
                if (staff != null && !staff.isBusy()) {
                    staff.assignTo(task.getRoomNumber());
                }
            }

            // Rooms that are dirty but unassigned belong back in the queue
            if ("Dirty".equals(task.getStatus())
                    && (task.getStaffId() == null || task.getStaffId().isBlank())) {
                if (!isAlreadyInQueue(task.getRoomNumber())) {
                    dirtyRoomQueue.addLast(task.getRoomNumber());
                }
            }
        }

        // IMPORTANT: after reconciling all busy/queue state from persisted data,
        // immediately try to assign any queued rooms to currently free staff.
        // Without this call, free staff would sit idle even when dirty rooms
        // are waiting in the queue after an app restart.
        dispatchQueue();
    }

    // ── Persistence ────────────────────────────────────────────────────────

    private void persistTaskLog() {
        TaskLogEntry[] entries = new TaskLogEntry[taskLog.size()];
        final int[] index = { 0 };
        taskLog.traverse(entry -> entries[index[0]++] = entry);
        housekeepingDao.saveToFile(entries);
    }

    private void persistRooms() {
        roomDao.saveToFile(rooms);
    }

    // ── Room helpers ───────────────────────────────────────────────────────

    /**
     * Maps a housekeeping task status directly to a Room's entity status.
     * Also triggers cross-module notifications when a room becomes ready.
     */
    private void applyTaskStatusToRoom(TaskLogEntry task, boolean notifyFrontDesk) {
        Room room = findRoomByNumber(task.getRoomNumber());
        if (room == null) return;

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

    private void syncRoomState(TaskLogEntry task) {
        applyTaskStatusToRoom(task, true);
        persistRooms();
    }

    private Room findRoomByNumber(String roomNumber) {
        refreshRooms();
        for (Room room : rooms) {
            if (room != null && room.getRoomNumber() != null
                    && room.getRoomNumber().equals(roomNumber)) {
                return room;
            }
        }
        return null;
    }

    private void refreshRooms() {
        rooms = roomDao.loadOrSeed();
        autoCompleteOverdueTasks();
    }

    /**
     * Automatically completes any "Cleaning In Progress" task whose
     * 30-minute countdown has expired.
     *
     * <p>Called transparently on every {@link #refreshRooms()} invocation so
     * that overdue tasks are resolved whenever the UI re-renders, without
     * requiring background threads.</p>
     */
    private void autoCompleteOverdueTasks() {
        boolean anyCompleted = false;
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task == null) continue;
            if (!"Cleaning In Progress".equals(task.getStatus())) continue;
            if (!task.isCleaningCountdownActive()) continue;

            // Countdown has expired when remaining minutes == 0
            if (task.getRemainingCleaningMinutes() == 0) {
                String roomNumber = task.getRoomNumber();
                String staffId   = task.getStaffId();

                // Advance Cleaning In Progress -> Inspected -> Ready
                task.setStatus("Inspected");
                task.setStatus("Ready");   // also sets completedWithinTarget = false (overdue)
                task.setRemarks("Auto-completed (Timeout)");


                // Free the assigned staff member
                if (staffId != null && !staffId.isBlank()) {
                    HousekeepingStaff staff = findStaffById(staffId);
                    if (staff != null && staff.isBusy()) {
                        staff.markFree();
                    }
                }

                syncRoomState(task);
                anyCompleted = true;

                System.out.println("⚠️  Room " + roomNumber
                        + " auto-completed after 30-min timeout (Task " + task.getTaskId() + ").");
            }
        }

        if (anyCompleted) {
            persistTaskLog();
            // Dispatch any queued rooms now that staff may be free
            dispatchQueue();
        }
    }

    // ── Task helpers ───────────────────────────────────────────────────────

    private String nextTaskId() {
        int maxId = 0;
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task == null || task.getTaskId() == null) continue;
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

    private TaskLogEntry findTaskById(String taskId) {
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task != null && task.getTaskId() != null && task.getTaskId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }

    /**
     * Returns the most recent housekeeping task for a given room.
     * Older tasks are historical records and must not change the room's
     * current status.
     */
    private TaskLogEntry findLatestTaskForRoom(String roomNumber) {
        for (int i = taskLog.size() - 1; i >= 0; i--) {
            TaskLogEntry task = taskLog.get(i);
            if (task != null && task.getRoomNumber() != null
                    && task.getRoomNumber().equals(roomNumber)) {
                return task;
            }
        }
        return null;
    }

    private boolean hasActiveTaskForRoom(String roomNumber) {
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task == null || task.getRoomNumber() == null) continue;
            if (task.getRoomNumber().equals(roomNumber) && !"Ready".equals(task.getStatus())) {
                return true;
            }
        }
        return false;
    }

    // ── Staff helpers ──────────────────────────────────────────────────────

    private HousekeepingStaff findStaffById(String staffId) {
        for (int i = 0; i < staffPool.size(); i++) {
            HousekeepingStaff s = staffPool.get(i);
            if (s != null && s.getStaffId().equalsIgnoreCase(staffId)) {
                return s;
            }
        }
        return null;
    }

    /** Returns the first free (not-busy) staff member, or null if all busy. */
    private HousekeepingStaff findFreeStaff() {
        for (int i = 0; i < staffPool.size(); i++) {
            HousekeepingStaff s = staffPool.get(i);
            if (s != null && !s.isBusy()) {
                return s;
            }
        }
        return null;
    }

    private boolean isAlreadyInQueue(String roomNumber) {
        return dirtyRoomQueue.contains(roomNumber);
    }

    // ── FIFO Dispatch ──────────────────────────────────────────────────────

    /**
     * Core auto-dispatch loop.
     *
     * <p>Dequeues rooms from the dirty-room FIFO queue and assigns them to
     * free staff members, one at a time, until either the queue is empty or
     * all staff are busy.</p>
     *
     * <p>ADT interaction: {@link DoublyLinkedList#removeFirst()} (O(1)) pulls the
     * next room; {@link DoublyLinkedList} traversal finds a free staff
     * member.</p>
     */
    private void dispatchQueue() {
        while (!dirtyRoomQueue.isEmpty()) {
            HousekeepingStaff freeStaff = findFreeStaff();
            if (freeStaff == null) {
                // All staff busy – rooms remain in queue
                break;
            }

            String roomNumber = dirtyRoomQueue.removeFirst();

            // Find the existing Dirty task for this room and assign it
            TaskLogEntry task = findLatestTaskForRoom(roomNumber);
            if (task == null || !"Dirty".equals(task.getStatus())) {
                // Room may have been cancelled or already handled – skip
                continue;
            }

            task.setStaffId(freeStaff.getStaffId());
            task.setStatus("Cleaning In Progress"); // also sets cleaningStartTime

            freeStaff.assignTo(roomNumber);

            syncRoomState(task);
            persistTaskLog();

            System.out.println("🧹 Auto-assigned Room " + roomNumber
                    + " to " + freeStaff.getName() + " (" + freeStaff.getStaffId() + ")."
                    + " Countdown: " + TaskLogEntry.CLEANING_TARGET_MINUTES + " min.");
        }

        int queued = dirtyRoomQueue.size();
        if (queued > 0) {
            System.out.println("⏳ " + queued + " room(s) waiting in queue (all staff busy).");
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Marks a room as dirty and triggers automatic staff assignment.
     *
     * <p>If a free staff member is available the room is assigned immediately
     * (status -> "Cleaning In Progress"). Otherwise the room is enqueued in
     * the FIFO dirty-room queue to wait its turn.</p>
     *
     * @param roomNumber the room that needs cleaning
     * @return true if the dirty task was created successfully
     */
    public boolean markRoomDirty(String roomNumber) {
        return markRoomDirty(roomNumber, null);
    }

    public boolean markRoomDirty(String roomNumber, String remarks) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null) {
            System.out.println("❌ Room not found: " + roomNumber);
            return false;
        }
        if (hasActiveTaskForRoom(roomNumber)) {
            System.out.println("❌ Room " + roomNumber + " already has an active housekeeping task.");
            return false;
        }
        if (isAlreadyInQueue(roomNumber)) {
            System.out.println("❌ Room " + roomNumber + " is already in the cleaning queue.");
            return false;
        }

        // Create the dirty task (no staff assigned yet)
        TaskLogEntry task = new TaskLogEntry(nextTaskId(), roomNumber, "Dirty", null, remarks);
        taskLog.addLast(task);
        syncRoomState(task);
        persistTaskLog();

        System.out.println("🛏  Room " + roomNumber + " marked DIRTY. Task " + task.getTaskId() + " created.");

        // Add to queue then try to dispatch immediately
        dirtyRoomQueue.addLast(roomNumber);
        dispatchQueue();

        return true;
    }

    /**
     * Called by the Front Desk / Registration module when a guest checks out.
     * Equivalent to {@link #markRoomDirty(String)} but preserves the original
     * method name used by the checkout flow.
     */
    public TaskLogEntry createCheckoutTask(String roomNumber) {
        return createCheckoutTask(roomNumber, null);
    }

    /**
     * @deprecated Use {@link #createCheckoutTask(String)} instead.
     *             The staffId parameter is ignored; assignment is automatic.
     */
    @Deprecated
    public TaskLogEntry createCheckoutTask(String roomNumber, String staffId) {
        return createCheckoutTask(roomNumber, staffId, null);
    }

    /**
     * @deprecated Use {@link #createCheckoutTask(String)} instead.
     *             The staffId parameter is ignored; assignment is automatic.
     */
    @Deprecated
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

        TaskLogEntry task = new TaskLogEntry(nextTaskId(), roomNumber, "Dirty", null, remarks);
        taskLog.addLast(task);
        syncRoomState(task);
        persistTaskLog();

        System.out.println("✅ Check-out processed for Room " + roomNumber
                + ". Housekeeping task created: " + task.getTaskId());

        dirtyRoomQueue.addLast(roomNumber);
        dispatchQueue();

        return task;
    }

    /**
     * Allows a staff member to declare themselves finished before the
     * 30-minute countdown expires (early-finish override).
     *
     * <p>The task is advanced to "Ready" (via Inspected intermediate step if
     * needed), the staff member is freed, and the queue dispatcher runs
     * immediately to assign the next waiting room.</p>
     *
     * @param staffId the staff member marking themselves ready
     * @return true if successful
     */
    public boolean markStaffReady(String staffId) {
        HousekeepingStaff staff = findStaffById(staffId);
        if (staff == null) {
            System.out.println("❌ Staff not found: " + staffId);
            return false;
        }
        if (!staff.isBusy()) {
            System.out.println("❌ " + staff.getName() + " (" + staffId + ") is not currently cleaning any room.");
            return false;
        }

        // Find the active "Cleaning In Progress" task for this staff member
        TaskLogEntry task = null;
        for (int i = taskLog.size() - 1; i >= 0; i--) {
            TaskLogEntry t = taskLog.get(i);
            if (t != null && staffId.equalsIgnoreCase(t.getStaffId())
                    && "Cleaning In Progress".equals(t.getStatus())) {
                task = t;
                break;
            }
        }

        if (task == null) {
            System.out.println("❌ No active cleaning task found for " + staffId + ".");
            return false;
        }

        long remaining = task.getRemainingCleaningMinutes();
        String roomNumber = task.getRoomNumber();

        // Advance through Inspected -> Ready in one go (early finish)
        task.setStatus("Inspected");
        task.setStatus("Ready");    // also computes completedWithinTarget

        staff.markFree();

        syncRoomState(task);
        persistTaskLog();

        if (remaining > 0) {
            System.out.println("✅ " + staff.getName() + " finished Room " + roomNumber
                    + " early (" + remaining + " min remaining on countdown).");
        } else {
            System.out.println("✅ " + staff.getName() + " finished cleaning Room " + roomNumber + ".");
        }

        // Dispatch next room from queue now that a staff member is free
        dispatchQueue();
        return true;
    }

    /**
     * Manually updates a task status (for correction / inspection flows).
     *
     * <p>If a "Cleaning In Progress" task is advanced to "Inspected" or "Ready"
     * via this method, the assigned staff is also freed.</p>
     *
     * @param taskId    the task to update
     * @param newStatus the target status
     * @return true if the transition was valid and applied
     */
    public boolean updateTaskStatus(String taskId, String newStatus) {
        TaskLogEntry task = findTaskById(taskId);
        if (task == null) {
            System.out.println("❌ Task not found: " + taskId);
            return false;
        }
        if (!isValidTransition(task.getStatus(), newStatus)) {
            System.out.println("❌ Invalid status transition: " + task.getStatus() + " -> " + newStatus);
            return false;
        }

        String oldStatus = task.getStatus();
        task.setStatus(newStatus);

        // If cleaning just completed, free the staff member
        if ("Ready".equals(newStatus) && task.getStaffId() != null) {
            HousekeepingStaff staff = findStaffById(task.getStaffId());
            if (staff != null && staff.isBusy()
                    && task.getRoomNumber().equals(staff.getAssignedRoom())) {
                staff.markFree();
                dispatchQueue(); // try to assign next queued room
            }
        }

        syncRoomState(task);
        persistTaskLog();
        System.out.println("✅ Task " + taskId + " updated: " + oldStatus + " -> " + newStatus);
        return true;
    }

    /**
     * Reverts a task to its previous logical status (correction flow).
     * Cannot rollback past "Dirty" or a historical (non-latest) task.
     */
    public boolean rollbackTask(String taskId) {
        TaskLogEntry task = findTaskById(taskId);
        if (task == null) {
            System.out.println("Task not found: " + taskId);
            return false;
        }

        TaskLogEntry latestTask = findLatestTaskForRoom(task.getRoomNumber());
        if (latestTask == null || !task.getTaskId().equals(latestTask.getTaskId())) {
            System.out.println("❌ Cannot rollback historical task " + taskId
                    + ". Only the latest task for Room " + task.getRoomNumber() + " can be rolled back.");
            return false;
        }

        Room room = findRoomByNumber(task.getRoomNumber());
        if (room == null) {
            System.out.println("❌ Room not found: " + task.getRoomNumber());
            return false;
        }

        String currentRoomTaskStatus = mapRoomStatusToTaskStatus(room.getRoomStatus());
        if (currentRoomTaskStatus == null || !task.getStatus().equals(currentRoomTaskStatus)) {
            System.out.println("❌ Cannot rollback task " + taskId + " because Room "
                    + task.getRoomNumber() + " is currently "
                    + describeRoomStatus(room.getRoomStatus())
                    + ". The task is no longer the room's active housekeeping state.");
            return false;
        }

        String current  = task.getStatus();
        String previous = getPreviousStatus(current);
        if (previous == null) {
            System.out.println("❌ Cannot rollback task from initial status (Dirty).");
            return false;
        }

        // If rolling back from Cleaning In Progress -> Dirty, free the staff
        if ("Cleaning In Progress".equals(current) && task.getStaffId() != null) {
            HousekeepingStaff staff = findStaffById(task.getStaffId());
            if (staff != null) staff.markFree();
            task.setStaffId(null);
            // Put room back in queue front? For simplicity, re-enqueue at rear.
            dirtyRoomQueue.addLast(task.getRoomNumber());
        }

        task.setStatus(previous);
        syncRoomState(task);
        persistTaskLog();
        System.out.println("🔄 Task " + taskId + " rolled back: " + current + " -> " + previous);
        return true;
    }

    // ── Status helpers ─────────────────────────────────────────────────────

    private String getPreviousStatus(String current) {
        switch (current) {
            case "Cleaning In Progress": return "Dirty";
            case "Inspected":            return "Cleaning In Progress";
            case "Ready":                return "Inspected";
            default:                     return null;
        }
    }

    private String getNextStatus(String current) {
        switch (current) {
            case "Dirty":               return "Cleaning In Progress";
            case "Cleaning In Progress": return "Inspected";
            case "Inspected":           return "Ready";
            default:                    return null;
        }
    }

    private boolean isValidTransition(String current, String next) {
        return next != null && next.equals(getNextStatus(current));
    }

    private String mapRoomStatusToTaskStatus(RoomStatus roomStatus) {
        if (roomStatus == null) return null;
        switch (roomStatus) {
            case DIRTY:                return "Dirty";
            case CLEANING_IN_PROGRESS: return "Cleaning In Progress";
            case INSPECTED:            return "Inspected";
            case READY:
            case AVAILABLE:            return "Ready";
            default:                   return null;
        }
    }

    // ── Search ─────────────────────────────────────────────────────────────

    public void searchByRoom(String roomNumber) {
        System.out.println("🔍 Tasks for Room " + roomNumber + ":");
        boolean found = false;
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task != null && task.getRoomNumber() != null
                    && task.getRoomNumber().equals(roomNumber)) {
                System.out.println(task);
                found = true;
            }
        }
        if (!found) System.out.println("No tasks found for this room.");
    }

    // ── Display ────────────────────────────────────────────────────────────

    public void displayRoomStatus() {
        refreshRooms();
        System.out.println("\n=== ROOM HOUSEKEEPING STATUS ===");
        System.out.printf("%-8s | %-22s | %-10s | %-22s | %-15s%n",
                "Room", "Room Status", "Available", "Housekeeping", "Countdown");
        System.out.println("----------------------------------------------------------------------------------------");
        for (Room room : rooms) {
            if (room == null) continue;

            String housekeepingStatus = mapRoomStatusToTaskStatus(room.getRoomStatus());
            String countdown = "-";

            // Show countdown for in-progress rooms
            if (room.getRoomStatus() == RoomStatus.CLEANING_IN_PROGRESS) {
                TaskLogEntry latest = findLatestTaskForRoom(room.getRoomNumber());
                if (latest != null && latest.isCleaningCountdownActive()) {
                    long mins = latest.getRemainingCleaningMinutes();
                    countdown = mins == 0 ? "⚠ Overdue" : mins + " min left";
                }
            }

            System.out.printf("%-8s | %-22s | %-10s | %-22s | %-15s%n",
                    room.getRoomNumber(),
                    describeRoomStatus(room.getRoomStatus()),
                    room.isAvailability() ? "Yes" : "No",
                    housekeepingStatus == null ? "N/A" : housekeepingStatus,
                    countdown);
        }
        System.out.println("----------------------------------------------------------------------------------------");
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
            if (room == null) continue;
            if (room.getStatus() == status) {
                System.out.printf("%-8s | %-22s | %-10s%n",
                        room.getRoomNumber(),
                        describeRoomStatus(room.getRoomStatus()),
                        room.isAvailability() ? "Yes" : "No");
                found = true;
            }
        }
        if (!found) System.out.println("No rooms found for this filter.");
        System.out.println("-----------------------------------------------");
    }

    /** Displays the current contents of the dirty-room FIFO queue and staff status. */
    public void displayQueueAndStaffStatus() {
        System.out.println("\n=== CLEANING QUEUE & STAFF STATUS ===");

        // Staff status
        System.out.println("\n--- Staff Status ---");
        System.out.printf("%-5s %-12s %-6s %-12s%n", "ID", "Name", "Status", "Room");
        System.out.println("----------------------------------");
        for (int i = 0; i < staffPool.size(); i++) {
            HousekeepingStaff s = staffPool.get(i);
            if (s == null) continue;

            long timeLeft = -1;
            if (s.isBusy() && s.getAssignedRoom() != null) {
                TaskLogEntry task = findLatestTaskForRoom(s.getAssignedRoom());
                if (task != null && task.isCleaningCountdownActive()) {
                    timeLeft = task.getRemainingCleaningMinutes();
                }
            }

            String statusLabel = s.isBusy() ? "BUSY" : "FREE";
            String roomLabel   = s.isBusy() ? s.getAssignedRoom() : "-";
            String timerLabel  = s.isBusy() && timeLeft >= 0
                    ? (timeLeft == 0 ? " ⚠ Overdue" : " (" + timeLeft + " min left)") : "";

            System.out.printf("%-5s %-12s %-6s %-12s%s%n",
                    s.getStaffId(), s.getName(), statusLabel, roomLabel, timerLabel);
        }
        System.out.println("----------------------------------");

        // Queue contents
        System.out.println("\n--- Dirty-Room Queue (FIFO) ---");
        if (dirtyRoomQueue.isEmpty()) {
            System.out.println("  Queue is empty.");
        } else {
            System.out.println("  Position | Room");
            System.out.println("  ---------+------");
            for (int i = 0; i < dirtyRoomQueue.size(); i++) {
                System.out.printf("  %-9d| %s%n", (i + 1), dirtyRoomQueue.get(i));
            }
        }
        System.out.println("-------------------------------");
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

    public void displayAllTasks() {
        taskLog.display();
    }

    // ── Misc ───────────────────────────────────────────────────────────────

    private String describeRoomStatus(RoomStatus status) {
        return status == null ? "Unknown" : status.getDisplayName();
    }

    public boolean resetToDefaultData() {
        taskLog.clear();
        dirtyRoomQueue.clear();
        rooms = roomDao.resetToDefaultReadyRooms();
        persistTaskLog();
        // Re-free all staff
        for (int i = 0; i < staffPool.size(); i++) {
            HousekeepingStaff s = staffPool.get(i);
            if (s != null) s.markFree();
        }
        System.out.println("✅ Housekeeping data reset to default rooms with Ready status.");
        return true;
    }

    // ── Boundary display helpers (ECB: Boundary does not access Entity/ADT) ──

    public Room[] getRooms() {
        refreshRooms();
        return rooms;
    }

    public String[][] getRoomSelectionDisplayData() {
        Room[] currentRooms = getRooms();
        String[][] rows = new String[currentRooms == null ? 0 : currentRooms.length][2];
        if (currentRooms == null) return rows;
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
        if (taskLog == null) return rows;
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

    /**
     * Returns display rows for only active (non-Ready) tasks plus their countdown.
     * Columns: [taskId, roomNumber, status, staffId, countdown]
     */
    public String[][] getActiveTaskDisplayData() {
        String[][] temp = new String[taskLog.size()][5];
        int count = 0;
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry task = taskLog.get(i);
            if (task == null || "Ready".equals(task.getStatus())) continue;
            String countdown = "-";
            if (task.isCleaningCountdownActive()) {
                long mins = task.getRemainingCleaningMinutes();
                countdown = mins == 0 ? "⚠ Overdue" : mins + " min left";
            }
            temp[count][0] = task.getTaskId();
            temp[count][1] = task.getRoomNumber();
            temp[count][2] = task.getStatus();
            temp[count][3] = task.getStaffId() == null || task.getStaffId().isBlank()
                    ? "In Queue" : task.getStaffId();
            temp[count][4] = countdown;
            count++;
        }
        String[][] rows = new String[count][5];
        System.arraycopy(temp, 0, rows, 0, count);
        return rows;
    }

    /** Returns names and IDs of all staff for UI selection. Columns: [staffId, name, status]. */
    public String[][] getStaffDisplayData() {
        String[][] rows = new String[staffPool.size()][3];
        for (int i = 0; i < staffPool.size(); i++) {
            HousekeepingStaff s = staffPool.get(i);
            if (s != null) {
                rows[i][0] = s.getStaffId();
                rows[i][1] = s.getName();
                rows[i][2] = s.isBusy() ? "BUSY (Room " + s.getAssignedRoom() + ")" : "FREE";
            }
        }
        return rows;
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public LinearADT<TaskLogEntry> getTaskLog() {
        return taskLog;
    }

    public LinearADT<String> getDirtyRoomQueue() {
        return dirtyRoomQueue;
    }

    // ── Report generators ──────────────────────────────────────────────────

    public void generateCleaningStatusReport() {
        new control.report.CleaningStatusFlowRP().generateReport(taskLog);
    }

    public void generateCleaningStatusReport(String statusFilter, String staffFilter,
            String roomSearch, int sortOption) {
        new control.report.CleaningStatusFlowRP()
                .generateReport(taskLog, statusFilter, staffFilter, roomSearch, sortOption);
    }

    public void generateDailyPerformanceReport() {
        new control.report.DailyPerformanceRP().generateReport(taskLog);
    }

    public void generateDailyPerformanceReport(LocalDate reportDate, String staffFilter,
            long minimumMinutes, int sortOption) {
        new control.report.DailyPerformanceRP()
                .generateReport(taskLog, reportDate, staffFilter, minimumMinutes, sortOption);
    }

    public void generateKpiReport() {
        new control.report.HousekeepingKpiRP().generateReport(taskLog);
    }

    public void generateKpiReport(LocalDate reportDate) {
        new control.report.HousekeepingKpiRP().generateReport(taskLog, reportDate);
    }
}