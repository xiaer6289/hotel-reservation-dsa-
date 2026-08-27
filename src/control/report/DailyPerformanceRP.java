package control.report;

import adt.linear.LinearADT;
import entity.TaskLogEntry;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Daily Housekeeping Performance Report.
 * Uses Linear Search for filtering and Selection Sort for report ordering.
 *
 * @author Low Wei Shin
 */
public class DailyPerformanceRP {

        private static final DateTimeFormatter GENERATED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

        /**
         * Keeps the existing controller working before
         * report filter options are connected to the UI.
         *
         * Default:
         * - Today
         * - All staff
         * - Minimum time = 0
         * - Earliest created task first
         */
        public void generateReport(LinearADT<TaskLogEntry> taskLog) {

                generateReport(taskLog, LocalDate.now(), "ALL", 0, 1);
        }

        /**
         * Generates the Daily Housekeeping Performance Report.
         *
         * @param taskLog        housekeeping task records
         * @param reportDate     selected report date
         * @param staffFilter    ALL, SYSTEM, S001, S002, etc.
         * @param minimumMinutes minimum task time
         * @param sortOption
         *                       1 = Created Time Earliest First
         *                       2 = Task Time Longest First
         */
        public void generateReport(LinearADT<TaskLogEntry> taskLog, LocalDate reportDate, String staffFilter,
                        long minimumMinutes, int sortOption) {

                String normalizedStaff = normalizeStaffFilter(staffFilter);

                if (reportDate == null) {
                        reportDate = LocalDate.now();
                }

                if (minimumMinutes < 0) {
                        minimumMinutes = 0;
                }

                TaskLogEntry[] filtered = new TaskLogEntry[taskLog == null ? 0 : taskLog.size()];

                int matchCount = linearSearch(taskLog, filtered, reportDate, normalizedStaff, minimumMinutes);

                selectionSort(filtered, matchCount, sortOption);

                printReportHeader(reportDate, normalizedStaff, minimumMinutes, sortOption);

                if (matchCount == 0) {

                        System.out.println("No housekeeping tasks match " + "the selected criteria.");

                        System.out.println(
                                        "================================================================================================");

                        return;
                }

                printTaskRows(filtered, matchCount);

                printManagementSummary(filtered, matchCount);
        }

        /**
         * LINEAR SEARCH
         *
         * Every task is checked sequentially against:
         * 1. Report Date
         * 2. Staff
         * 3. Minimum Task Time
         */
        private int linearSearch(LinearADT<TaskLogEntry> taskLog, TaskLogEntry[] filtered, LocalDate reportDate,
                        String staffFilter, long minimumMinutes) {

                if (taskLog == null) {
                        return 0;
                }

                int matchCount = 0;

                for (int i = 0; i < taskLog.size(); i++) {

                        TaskLogEntry task = taskLog.get(i);

                        if (task == null) {
                                continue;
                        }

                        if (!matchesDate(task, reportDate)) {
                                continue;
                        }

                        if (!matchesStaff(task, staffFilter)) {
                                continue;
                        }

                        if (!matchesMinimumTime(task, minimumMinutes)) {
                                continue;
                        }

                        filtered[matchCount++] = task;
                }

                return matchCount;
        }

        private boolean matchesDate(TaskLogEntry task, LocalDate reportDate) {

                if (task.getCreatedTime() == null) {
                        return false;
                }

                return task.getCreatedTime().toLocalDate().equals(reportDate);
        }

        private boolean matchesStaff(TaskLogEntry task, String staffFilter) {

                if ("ALL".equalsIgnoreCase(staffFilter)) {
                        return true;
                }

                return getDisplayStaffId(task).equalsIgnoreCase(staffFilter);
        }

        private boolean matchesMinimumTime(TaskLogEntry task, long minimumMinutes) {

                return getTaskDurationMinutes(task) >= minimumMinutes;
        }

        /**
         * SELECTION SORT
         */
        private void selectionSort(TaskLogEntry[] records, int count, int sortOption) {

                for (int i = 0; i < count - 1; i++) {

                        int selectedIndex = i;

                        for (int j = i + 1; j < count; j++) {

                                if (comesBefore(records[j], records[selectedIndex], sortOption)) {

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

        private boolean comesBefore(TaskLogEntry first, TaskLogEntry second, int sortOption) {

                /*
                 * Option 2:
                 * Longest task time first.
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
                 * Earliest created task first.
                 */
                LocalDateTime firstCreated = first.getCreatedTime();

                LocalDateTime secondCreated = second.getCreatedTime();

                if (firstCreated != null && secondCreated != null && !firstCreated.equals(secondCreated)) {

                        return firstCreated.isBefore(secondCreated);
                }

                /*
                 * Final tie-break by Task ID.
                 */
                return safeText(first.getTaskId()).compareToIgnoreCase(safeText(second.getTaskId())) < 0;
        }

        private void printReportHeader(LocalDate reportDate, String staffFilter, long minimumMinutes, int sortOption) {

                System.out.println(
                                "\n================================================================================================");

                System.out.println("                         DAILY HOUSEKEEPING PERFORMANCE REPORT");

                System.out.println(
                                "================================================================================================");

                System.out.println("Generated On       : " + LocalDateTime.now().format(GENERATED_FORMAT));

                System.out.println("Report Date        : " + reportDate);

                System.out.println("Staff              : " + staffFilter);

                System.out.println("Minimum Task Time  : " + minimumMinutes + " minute(s)");

                System.out.println("Search Technique   : " + "Linear Search");

                System.out.println("Sorting Technique  : " + getSortDescription(sortOption));

                System.out.println(
                                "------------------------------------------------------------------------------------------------");

                System.out.printf("%-7s %-6s %-8s %-22s %-10s %-14s%n",
                                "Task ID", "Room", "Staff", "Status", "Created", "Task Time");

                System.out.println(
                                "------------------------------------------------------------------------------------------------");
        }

        private void printTaskRows(TaskLogEntry[] records, int count) {

                for (int i = 0; i < count; i++) {

                        TaskLogEntry task = records[i];

                        String createdTime = task.getCreatedTime() == null ? "N/A" : task.getCreatedTime().format(TIME_FORMAT);

                        System.out.printf("%-7s %-6s %-8s %-22s %-10s %-14s%n",safeText(task.getTaskId()),safeText(task.getRoomNumber()),getDisplayStaffId(task),safeText(task.getStatus()),createdTime,formatMinutes(getTaskDurationMinutes(task)));
                }

                System.out.println("------------------------------------------------------------------------------------------------");
        }

        /**
         * Management statistics based only on
         * records matching the selected filters.
         */
        private void printManagementSummary(
                        TaskLogEntry[] records,
                        int count) {

                int completedTasks = 0;
                int pendingTasks = 0;

                long totalMinutes = 0;

                for (int i = 0; i < count; i++) {

                        TaskLogEntry task = records[i];

                        if ("Ready".equals(task.getStatus())) {

                                completedTasks++;

                        } else {

                                pendingTasks++;
                        }

                        totalMinutes += getTaskDurationMinutes(task);
                }

                double completionRate = count == 0 ? 0.0 : (double) completedTasks * 100.0 / count;

                double averageTaskTime = count == 0 ? 0.0 : (double) totalMinutes / count;

                System.out.println("MANAGEMENT SUMMARY");

                System.out.println("Matching Tasks       : " + count);

                System.out.println("Completed Tasks      : " + completedTasks);

                System.out.println("Pending Tasks        : " + pendingTasks);

                System.out.printf("Completion Rate      : %.2f%%%n",completionRate);

                System.out.printf("Average Task Time    : %.2f minute(s)%n",averageTaskTime);

                System.out.println("================================================================================================");
        }

        /**
         * Calculates task duration.
         *
         * Ready task:
         * created time -> last updated time
         *
         * Active task:
         * created time -> current time
         */
        private long getTaskDurationMinutes(TaskLogEntry task) {

                if (task == null || task.getCreatedTime() == null) {

                        return 0;
                }

                LocalDateTime endTime;

                if ("Ready".equals(task.getStatus()) && task.getLastUpdatedTime() != null) {

                        endTime = task.getLastUpdatedTime();

                } else {

                        endTime = LocalDateTime.now();
                }

                long minutes = Duration.between(task.getCreatedTime(),endTime).toMinutes();

                return Math.max(minutes,0);
        }

        private String getSortDescription(int sortOption) {

                if (sortOption == 2) {

                        return "Selection Sort (Task Time - Longest First)";
                }

                return "Selection Sort (Created Time - Earliest First)";
        }

        private String normalizeStaffFilter(String value) {

                if (value == null || value.isBlank()) {

                        return "ALL";
                }

                return value.trim();
        }

        private String getDisplayStaffId(TaskLogEntry task) {

                if (task.getStaffId() == null || task.getStaffId().isBlank()) {

                        return "SYSTEM";
                }

                return task.getStaffId();
        }

        private String safeText(String value) {

                return value == null ? "N/A" : value;
        }

        private String formatMinutes(long totalMinutes) {

                long hours = totalMinutes / 60;

                long minutes = totalMinutes % 60;

                if (hours <= 0) {

                        return minutes + " min";
                }

                return hours + " hr " + minutes + " min";
        }
}