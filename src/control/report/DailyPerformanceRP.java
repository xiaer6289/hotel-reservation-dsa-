package control.report;

import adt.linear.LinearADT;
import entity.TaskLogEntry;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Low Wei Shin
 */
public class DailyPerformanceRP {

    public void generateReport(LinearADT<TaskLogEntry> taskLog) {
        List<TaskLogEntry> list = new ArrayList<>();
        for (int i = 0; i < taskLog.size(); i++) {
            TaskLogEntry entry = taskLog.get(i);
            if (entry != null) {
                list.add(entry);
            }
        }

        System.out.println("\n=======================================================");
        System.out.println("       TARUMT RESORTS - HOUSEKEEPING STATUS FLOW");
        System.out.println("=======================================================");
        System.out.println("Generated on: " + java.time.LocalDateTime.now());
        System.out.println("-------------------------------------------------------");
        System.out.println();
        System.out.println("Cleaning Stage Flow & Estimated Time:");
        System.out.println();
        System.out.println("   Dirty -> Cleaning In Progress -> Inspected -> Ready");
        System.out.println("  (0 min)         (25-35 min)           (8-12 min)    (0 min)");
        System.out.println();
        System.out.println("-------------------------------------------------------");
        System.out.printf("%-6s | %-22s | %-10s | %-14s | %-6s%n",
                "Room", "Current Status", "Time Spent", "Est. Remaining", "Staff");
        System.out.println("-------|------------------------|------------|----------------|--------");

        for (int i = 0; i < list.size() - 1; i++) {
            for (int j = 0; j < list.size() - i - 1; j++) {
                if (list.get(j).getRoomNumber().compareTo(list.get(j + 1).getRoomNumber()) > 0) {
                    TaskLogEntry temp = list.get(j);
                    list.set(j, list.get(j + 1));
                    list.set(j + 1, temp);
                }
            }
        }

        int dirty = 0;
        int cleaning = 0;
        int inspected = 0;
        int ready = 0;

        for (TaskLogEntry t : list) {
            System.out.printf("%-6s | %-22s | %-10s | %-14s | %-6s%n",
                    t.getRoomNumber(),
                    t.getStatus(),
                    formatMinutes(t.getMinutesSpent()),
                    formatMinutes(t.getEstimatedMinutes()),
                    t.getStaffId() == null ? "-" : t.getStaffId());

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
                default:
                    break;
            }
        }

        System.out.println("-------------------------------------------------------");
        System.out.println("Summary:");
        System.out.println("Dirty                   : " + dirty + " room(s)");
        System.out.println("Cleaning In Progress    : " + cleaning + " room(s)");
        System.out.println("Inspected               : " + inspected + " room(s)");
        System.out.println("Ready for Check-in      : " + ready + " room(s)");
        System.out.println("Total Active Tasks      : " + list.size());
        System.out.println("=======================================================");
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
