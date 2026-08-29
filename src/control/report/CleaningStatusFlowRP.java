package control.report;

import adt.linear.LinearADT;
import entity.TaskLogEntry;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

/**
 * Cleaning Status Analysis Report.
 * Uses Linear Search for filtering and Selection Sort for report ordering.
 *
 * @author Low Wei Shin
 */
public class CleaningStatusFlowRP {

        private static final DateTimeFormatter GENERATED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        private static final DateTimeFormatter CREATED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        /**
         * Keeps the current controller working before
         * report filter options are added to the UI.
         */
        public void generateReport(
                        LinearADT<TaskLogEntry> taskLog) {

                generateReport(
                                taskLog,
                                "ALL",
                                "ALL",
                                "",
                                1);
        }

        /**
         * Generates the Cleaning Status Analysis Report.
         *
         * @param taskLog      Housekeeping task records
         * @param statusFilter ALL, Dirty, Cleaning In Progress,
         *                     Inspected, or Ready
         * @param staffFilter  ALL, SYSTEM, S001, S002, etc.
         * @param roomSearch   blank = all rooms
         * @param sortOption   1 = Room Number Ascending
         *                     2 = Time Spent Longest First
         */
        public void generateReport(
                        LinearADT<TaskLogEntry> taskLog,
                        String statusFilter,
                        String staffFilter,
                        String roomSearch,
                        int sortOption) {

                String normalizedStatus = normalizeFilter(statusFilter);

                String normalizedStaff = normalizeFilter(staffFilter);

                String normalizedRoom = roomSearch == null
                                ? ""
                                : roomSearch.trim();

                TaskLogEntry[] filtered = new TaskLogEntry[taskLog == null
                                ? 0
                                : taskLog.size()];

                int matchCount = linearSearch(
                                taskLog,
                                filtered,
                                normalizedStatus,
                                normalizedStaff,
                                normalizedRoom);

                selectionSort(
                                filtered,
                                matchCount,
                                sortOption);

                printReportHeader(
                                normalizedStatus,
                                normalizedStaff,
                                normalizedRoom,
                                sortOption);

                if (matchCount == 0) {

                        System.out.println(
                                        "No housekeeping tasks match "
                                                        + "the selected criteria.");

                        System.out.println(
                                        "============================================================================================");

                        return;
                }

                printTaskRows(
                                filtered,
                                matchCount);

                printManagementSummary(
                                filtered,
                                matchCount);
        }

        /**
         * LINEAR SEARCH
         *
         * Checks every housekeeping task sequentially
         * against the selected filter criteria.
         */
        private int linearSearch(
                        LinearADT<TaskLogEntry> taskLog,
                        TaskLogEntry[] filtered,
                        String statusFilter,
                        String staffFilter,
                        String roomSearch) {

                if (taskLog == null) {
                        return 0;
                }

                int matchCount = 0;

                for (int i = 0; i < taskLog.size(); i++) {

                        TaskLogEntry task = taskLog.get(i);

                        if (task == null) {
                                continue;
                        }

                        if (!matchesStatus(
                                        task,
                                        statusFilter)) {

                                continue;
                        }

                        if (!matchesStaff(
                                        task,
                                        staffFilter)) {

                                continue;
                        }

                        if (!matchesRoom(
                                        task,
                                        roomSearch)) {

                                continue;
                        }

                        filtered[matchCount++] = task;
                }

                return matchCount;
        }

        private boolean matchesStatus(
                        TaskLogEntry task,
                        String statusFilter) {

                if ("ALL".equalsIgnoreCase(
                                statusFilter)) {

                        return true;
                }

                return task.getStatus() != null
                                && task.getStatus()
                                                .equalsIgnoreCase(
                                                                statusFilter);
        }

        private boolean matchesStaff(
                        TaskLogEntry task,
                        String staffFilter) {

                if ("ALL".equalsIgnoreCase(
                                staffFilter)) {

                        return true;
                }

                String taskStaff = getDisplayStaffId(task);

                return taskStaff.equalsIgnoreCase(
                                staffFilter);
        }

        private boolean matchesRoom(
                        TaskLogEntry task,
                        String roomSearch) {

                if (roomSearch == null
                                || roomSearch.isBlank()) {

                        return true;
                }

                return task.getRoomNumber() != null
                                && task.getRoomNumber()
                                                .toLowerCase()
                                                .contains(
                                                                roomSearch
                                                                                .toLowerCase());
        }

        /**
         * SELECTION SORT
         *
         * Sorts only the records that matched
         * the selected filters.
         */
        private void selectionSort(
                        TaskLogEntry[] records,
                        int count,
                        int sortOption) {

                for (int i = 0; i < count - 1; i++) {

                        int selectedIndex = i;

                        for (int j = i + 1; j < count; j++) {

                                if (comesBefore(
                                                records[j],
                                                records[selectedIndex],
                                                sortOption)) {

                                        selectedIndex = j;
                                }
                        }

                        if (selectedIndex != i) {

                                TaskLogEntry temp = records[i];

                                records[i] = records[selectedIndex];

                                records[selectedIndex] = temp;
                        }
                }
        }

        private boolean comesBefore(
                        TaskLogEntry first,
                        TaskLogEntry second,
                        int sortOption) {

                /*
                 * Option 2:
                 * Longest time spent first.
                 */
                if (sortOption == 2) {

                        long firstMinutes = getTaskDurationMinutes(first);

                        long secondMinutes = getTaskDurationMinutes(second);

                        if (firstMinutes != secondMinutes) {

                                return firstMinutes > secondMinutes;
                        }
                }

                /*
                 * Option 1 or tie-break:
                 * Room Number ascending.
                 */
                int roomComparison = compareRoomNumbers(
                                first.getRoomNumber(),
                                second.getRoomNumber());

                if (roomComparison != 0) {

                        return roomComparison < 0;
                }

                /*
                 * Final tie-break by Task ID.
                 */
                return safeText(
                                first.getTaskId())
                                .compareToIgnoreCase(
                                                safeText(
                                                                second.getTaskId())) < 0;
        }

        private int compareRoomNumbers(
                        String firstRoom,
                        String secondRoom) {

                try {

                        int firstNumber = Integer.parseInt(
                                        safeText(
                                                        firstRoom));

                        int secondNumber = Integer.parseInt(
                                        safeText(
                                                        secondRoom));

                        return Integer.compare(
                                        firstNumber,
                                        secondNumber);

                } catch (NumberFormatException ex) {

                        return safeText(
                                        firstRoom)
                                        .compareToIgnoreCase(
                                                        safeText(
                                                                        secondRoom));
                }
        }

        private void printReportHeader(
                        String statusFilter,
                        String staffFilter,
                        String roomSearch,
                        int sortOption) {

                System.out.println(
                                "\n============================================================================================");

                System.out.println(
                                "                         CLEANING STATUS ANALYSIS REPORT");

                System.out.println(
                                "============================================================================================");

                System.out.println(
                                "Generated On       : "
                                                + LocalDateTime.now()
                                                                .format(
                                                                                GENERATED_FORMAT));

                System.out.println(
                                "Purpose            : Real-time snapshot of every housekeeping task.");

                System.out.println(
                                "Hotel Value        : Enables supervisors to spot blocked rooms and");
                System.out.println(
                                "                     dispatch staff before guests are impacted.");

                System.out.println(
                                "Status             : "
                                                + statusFilter);

                System.out.println(
                                "Staff              : "
                                                + staffFilter);

                System.out.println(
                                "Room Search        : "
                                                + (roomSearch == null
                                                                || roomSearch.isBlank()
                                                                                ? "ALL"
                                                                                : roomSearch));

                System.out.println(
                                "Search Technique   : "
                                                + "Linear Search");

                System.out.println(
                                "Sorting Technique  : "
                                                + getSortDescription(
                                                                sortOption));

                System.out.println(
                                "--------------------------------------------------------------------------------------------");

                System.out.printf(
                                "%-7s %-6s %-22s %-8s %-17s %-14s%n",
                                "Task ID",
                                "Room",
                                "Status",
                                "Staff",
                                "Created",
                                "Time Spent");

                System.out.println(
                                "--------------------------------------------------------------------------------------------");
        }

        private void printTaskRows(
                        TaskLogEntry[] records,
                        int count) {

                for (int i = 0; i < count; i++) {

                        TaskLogEntry task = records[i];

                        String created = task.getCreatedTime() == null
                                        ? "N/A"
                                        : task.getCreatedTime()
                                                        .format(
                                                                        CREATED_FORMAT);

                        System.out.printf(
                                        "%-7s %-6s %-22s %-8s %-17s %-14s%n",
                                        safeText(
                                                        task.getTaskId()),
                                        safeText(
                                                        task.getRoomNumber()),
                                        safeText(
                                                        task.getStatus()),
                                        getDisplayStaffId(
                                                        task),
                                        created,
                                        formatMinutes(getTaskDurationMinutes(task)));
                }

                System.out.println(
                                "--------------------------------------------------------------------------------------------");
        }

        private void printManagementSummary(
                        TaskLogEntry[] records,
                        int count) {

                int dirtyCount = 0;
                int cleaningCount = 0;
                int inspectedCount = 0;
                int readyCount = 0;

                for (int i = 0; i < count; i++) {

                        String status = records[i].getStatus();

                        if ("Dirty".equals(
                                        status)) {

                                dirtyCount++;

                        } else if ("Cleaning In Progress"
                                        .equals(status)) {

                                cleaningCount++;

                        } else if ("Inspected"
                                        .equals(status)) {

                                inspectedCount++;

                        } else if ("Ready"
                                        .equals(status)) {

                                readyCount++;
                        }
                }

                System.out.println(
                                "MANAGEMENT SUMMARY");

                System.out.println(
                                "Matching Tasks          : "
                                                + count);

                System.out.println(
                                "Dirty                   : "
                                                + dirtyCount);

                System.out.println(
                                "Cleaning In Progress    : "
                                                + cleaningCount);

                System.out.println(
                                "Inspected               : "
                                                + inspectedCount);

                System.out.println(
                                "Ready                   : "
                                                + readyCount);

                System.out.println(
                                "Pending Tasks           : "
                                                + (count - readyCount));

                System.out.println(
                                "============================================================================================");
                System.out.println(
                                "MANAGEMENT INTERPRETATION");
                int pendingCount = count - readyCount;
                double pendingRate = count == 0 ? 0 : (double) pendingCount * 100.0 / count;
                if (pendingRate > 50) {
                        System.out.println(
                                        "  * ALERT: More than half of matched tasks are still pending (" + pendingCount + "/" + count + ").");
                        System.out.println(
                                        "    Room pipeline is congested - guest check-in may be delayed.");
                } else if (pendingRate > 0) {
                        System.out.println(
                                        "  * Some rooms remain pending (" + pendingCount + "/" + count + "). Cleaning is progressing.");
                } else {
                        System.out.println(
                                        "  * All matched rooms are READY. No pending cleaning tasks remain.");
                }
                System.out.println(
                                "RECOMMENDED ACTION");
                if (pendingRate > 50) {
                        System.out.println(
                                        "  -> Dispatch additional staff immediately to clear Dirty/In-Progress rooms.");
                        System.out.println(
                                        "  -> Escalate overdue tasks to housekeeping supervisor.");
                } else if (pendingRate > 0) {
                        System.out.println(
                                        "  -> Monitor cleaning progress; flag any task exceeding 30 minutes.");
                } else {
                        System.out.println(
                                        "  -> All rooms ready - no action required. Continue monitoring.");
                }
                System.out.println(
                                "============================================================================================");
        }

        private String getSortDescription(
                        int sortOption) {

                if (sortOption == 2) {

                        return "Selection Sort "
                                        + "(Time Spent - Longest First)";
                }

                return "Selection Sort "
                                + "(Room Number - Ascending)";
        }

        private String normalizeFilter(
                        String value) {

                if (value == null
                                || value.isBlank()) {

                        return "ALL";
                }

                return value.trim();
        }

        private String getDisplayStaffId(
                        TaskLogEntry task) {

                if (task.getStaffId() == null
                                || task.getStaffId()
                                                .isBlank()) {

                        return "SYSTEM";
                }

                return task.getStaffId();
        }

        private String safeText(
                        String value) {

                return value == null
                                ? "N/A"
                                : value;
        }

        private long getTaskDurationMinutes(
                        TaskLogEntry task) {

                if (task == null
                                || task.getCreatedTime() == null) {

                        return 0;
                }

                LocalDateTime endTime;

                if ("Ready".equals(task.getStatus())
                                && task.getLastUpdatedTime() != null) {

                        endTime = task.getLastUpdatedTime();

                } else {

                        endTime = LocalDateTime.now();
                }

                long minutes = Duration.between(
                                task.getCreatedTime(),
                                endTime)
                                .toMinutes();

                return Math.max(minutes, 0);
        }

        private String formatMinutes(
                        long totalMinutes) {

                long hours = totalMinutes / 60;

                long minutes = totalMinutes % 60;

                if (hours <= 0) {
                        return minutes + " min";
                }

                return hours
                                + " hr "
                                + minutes
                                + " min";
        }
}