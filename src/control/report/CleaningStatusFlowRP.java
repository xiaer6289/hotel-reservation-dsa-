package control.report;

import adt.linear.LinearADT;
import entity.TaskLogEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Low Wei Shin
 */
public class CleaningStatusFlowRP {

    public void generateReport(LinearADT<TaskLogEntry> taskLog) {
        List<TaskLogEntry> entries = new ArrayList<>();
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry entry = taskLog.get(i);
            if (entry != null) {
                entries.add(entry);
            }
        }

        System.out.println("\n=======================================================");
        System.out.println("     TARUMT RESORTS - DAILY HOUSEKEEPING ANALYSIS");
        System.out.println("=======================================================");
        System.out.println("Date: " + java.time.LocalDate.now());
        System.out.println("-------------------------------------------------------");

        System.out.println("\n1. Cleaning Time by Stage (Average)");
        System.out.println("-------------------------------------------------------");
        System.out.printf("%-26s | %-12s | %-4s | %-4s%n", "Stage", "Average Time", "Min", "Max");
        System.out.println("---------------------------|--------------|------|-----");

        Map<String, List<Long>> stageDurations = new LinkedHashMap<>();
        stageDurations.put("Dirty -> Cleaning Start", new ArrayList<>());
        stageDurations.put("Cleaning In Progress", new ArrayList<>());
        stageDurations.put("Inspection", new ArrayList<>());
        stageDurations.put("Total Cycle (Dirty->Ready)", new ArrayList<>());

        for (TaskLogEntry entry : entries) {
            long minutesSpent = entry.getMinutesSpent();
            switch (entry.getStatus()) {
                case "Dirty":
                    stageDurations.get("Dirty -> Cleaning Start").add(minutesSpent);
                    stageDurations.get("Total Cycle (Dirty->Ready)").add(minutesSpent);
                    break;
                case "Cleaning In Progress":
                    stageDurations.get("Cleaning In Progress").add(minutesSpent);
                    stageDurations.get("Total Cycle (Dirty->Ready)").add(minutesSpent);
                    break;
                case "Inspected":
                    stageDurations.get("Inspection").add(minutesSpent);
                    stageDurations.get("Total Cycle (Dirty->Ready)").add(minutesSpent);
                    break;
                case "Ready":
                    stageDurations.get("Total Cycle (Dirty->Ready)").add(minutesSpent);
                    break;
                default:
                    break;
            }
        }

        printStageRow("Dirty -> Cleaning Start", stageDurations.get("Dirty -> Cleaning Start"));
        printStageRow("Cleaning In Progress", stageDurations.get("Cleaning In Progress"));
        printStageRow("Inspection", stageDurations.get("Inspection"));
        printStageRow("Total Cycle (Dirty->Ready)", stageDurations.get("Total Cycle (Dirty->Ready" + ")"));
        System.out.println("-------------------------------------------------------");

        System.out.println("\n2. Staff Performance Today");
        System.out.println("-------------------------------------------------------");
        System.out.printf("%-9s | %-13s | %-9s | %-12s%n", "Staff ID", "Rooms Cleaned", "Avg Time", "On-Time Rate");
        System.out.println("----------|---------------|-----------|-------------");

        Map<String, List<TaskLogEntry>> tasksByStaff = new LinkedHashMap<>();
        for (TaskLogEntry entry : entries) {
            String staffId = entry.getStaffId();
            if (staffId == null || staffId.isBlank()) {
                staffId = "SYSTEM";
            }
            tasksByStaff.computeIfAbsent(staffId, key -> new ArrayList<>()).add(entry);
        }

        for (Map.Entry<String, List<TaskLogEntry>> staffEntry : tasksByStaff.entrySet()) {
            List<TaskLogEntry> staffTasks = staffEntry.getValue();
            long totalMinutes = 0;
            int readyCount = 0;

            for (TaskLogEntry task : staffTasks) {
                totalMinutes += task.getMinutesSpent();
                if ("Ready".equals(task.getStatus())) {
                    readyCount++;
                }
            }

            long averageMinutes = staffTasks.isEmpty() ? 0 : Math.round((double) totalMinutes / staffTasks.size());
            int onTimeRate = staffTasks.isEmpty() ? 0 : (int) Math.round((double) readyCount * 100 / staffTasks.size());

            System.out.printf("%-9s | %-13d | %-9s | %-12s%n",
                    staffEntry.getKey(),
                    staffTasks.size(),
                    formatMinutes(averageMinutes),
                    onTimeRate + "%");
        }

        System.out.println("-------------------------------------------------------");
        System.out.println("\n3. Overall Statistics");
        int readyRooms = countStatus(entries, "Ready");
        int pendingRooms = entries.size() - readyRooms;
        System.out.println("Total rooms cleaned today     : " + readyRooms);
        System.out.println("Rooms still pending           : " + pendingRooms);
        System.out.println("Average completion time       : " + formatMinutes(calculateAverageMinutesFromEntries(entries)));
        System.out.println("Rooms ready for new guests    : " + readyRooms);
        System.out.println("=======================================================");
    }

    private void printStageRow(String stage, List<Long> durations) {
        long average = calculateAverageMinutes(durations);
        long min = calculateMinMinutes(durations);
        long max = calculateMaxMinutes(durations);
        System.out.printf("%-26s | %-12s | %-4s | %-4s%n",
                stage,
                formatMinutes(average),
                min < 0 ? "-" : String.valueOf(min),
                max < 0 ? "-" : String.valueOf(max));
    }

    private long calculateAverageMinutes(List<Long> durations) {
        if (durations == null || durations.isEmpty()) {
            return 0;
        }

        long total = 0;
        for (Long duration : durations) {
            total += duration;
        }
        return Math.round((double) total / durations.size());
    }

    private long calculateAverageMinutesFromEntries(List<TaskLogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }

        long total = 0;
        for (TaskLogEntry entry : entries) {
            total += entry.getMinutesSpent();
        }

        return Math.round((double) total / entries.size());
    }

    private long calculateMinMinutes(List<Long> durations) {
        if (durations == null || durations.isEmpty()) {
            return -1;
        }

        long min = durations.get(0);
        for (Long duration : durations) {
            if (duration < min) {
                min = duration;
            }
        }
        return min;
    }

    private long calculateMaxMinutes(List<Long> durations) {
        if (durations == null || durations.isEmpty()) {
            return -1;
        }

        long max = durations.get(0);
        for (Long duration : durations) {
            if (duration > max) {
                max = duration;
            }
        }
        return max;
    }

    private int countStatus(List<TaskLogEntry> entries, String status) {
        int count = 0;
        for (TaskLogEntry entry : entries) {
            if (status.equals(entry.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private String formatMinutes(long minutes) {
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        if (hours <= 0) {
            return remainingMinutes + " min";
        }
        return hours + " hr " + remainingMinutes + " min";
    }
}