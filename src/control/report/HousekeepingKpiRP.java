package control.report;

import adt.linear.LinearADT;
import adt.linear.DoublyLinkedList;
import entity.TaskLogEntry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Housekeeping KPI Report.
 *
 * <p>KPI Rule: each staff member must clean (reach "Ready" status)
 * at least {@value #KPI_MIN_ROOMS} rooms per day.</p>
 *
 * <p>The report also tracks on-time completion: whether the cleaning
 * was finished within the {@value entity.TaskLogEntry#CLEANING_TARGET_MINUTES}-minute
 * target.</p>
 *
 * <p>ADT used: {@link DoublyLinkedList} to accumulate per-staff
 * statistics during the linear scan, demonstrating the ADT in the
 * report layer.</p>
 *
 * @author Low Wei Shin
 */
public class HousekeepingKpiRP {

    /** Minimum rooms a staff member must clean per day to meet KPI. */
    public static final int KPI_MIN_ROOMS = 5;

    private static final DateTimeFormatter GENERATED_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ── Known staff (matching the staff pool in HousekeepingController) ────
    private static final String[][] STAFF_INFO = {
        { "S001", "Tan"      },
        { "S002", "Choo"     },
        { "S003", "Michelle" }
    };

    // ── Inner record for per-staff stats ───────────────────────────────────

    private static class StaffStat {
        final String staffId;
        final String name;
        int roomsCleaned      = 0;   // reached "Ready"
        int onTimeCount       = 0;   // cleaned within target
        int totalCleaningMins = 0;   // sum of cleaning durations

        StaffStat(String staffId, String name) {
            this.staffId = staffId;
            this.name    = name;
        }

        boolean meetsKpi() { return roomsCleaned >= KPI_MIN_ROOMS; }

        double onTimeRate() {
            return roomsCleaned == 0 ? 0.0 : (double) onTimeCount * 100.0 / roomsCleaned;
        }

        double avgCleaningMins() {
            return roomsCleaned == 0 ? 0.0 : (double) totalCleaningMins / roomsCleaned;
        }
    }

    // ── Public entry points ────────────────────────────────────────────────

    /** Generates the KPI report for today with all staff. */
    public void generateReport(LinearADT<TaskLogEntry> taskLog) {
        generateReport(taskLog, LocalDate.now());
    }

    /**
     * Generates the KPI report for a specific date.
     *
     * @param taskLog  all housekeeping task records
     * @param date     the report date (tasks created on this date are included)
     */
    public void generateReport(LinearADT<TaskLogEntry> taskLog, LocalDate date) {
        if (date == null) date = LocalDate.now();

        // ── Build per-staff stat objects in a DoublyLinkedList ─────────────
        DoublyLinkedList<StaffStat> statList = new DoublyLinkedList<>();
        for (String[] info : STAFF_INFO) {
            statList.addLast(new StaffStat(info[0], info[1]));
        }

        // ── Linear scan: accumulate stats for tasks on the report date ──────
        int totalCompleted = 0;
        int totalOnTime    = 0;

        if (taskLog != null) {
            for (int i = 0; i < taskLog.size(); i++) {
                TaskLogEntry task = taskLog.get(i);
                if (task == null) continue;

                // Only count tasks that reached "Ready" on this date
                if (!"Ready".equals(task.getStatus())) continue;
                if (task.getLastUpdatedTime() == null) continue;
                if (!task.getLastUpdatedTime().toLocalDate().equals(date)) continue;

                totalCompleted++;
                if (task.isCompletedWithinTarget()) totalOnTime++;

                // Find matching stat entry
                for (int j = 0; j < statList.size(); j++) {
                    StaffStat stat = statList.get(j);
                    if (stat.staffId.equalsIgnoreCase(task.getStaffId())) {
                        stat.roomsCleaned++;
                        if (task.isCompletedWithinTarget()) stat.onTimeCount++;
                        stat.totalCleaningMins += (int) task.getCleaningDurationMinutes();
                        break;
                    }
                }
            }
        }

        // ── Sort stat list by roomsCleaned ascending (Selection Sort) ───────
        // So the KPI-failure section is ordered worst -> best
        selectionSortByRoomsCleaned(statList);

        // ── Print report ───────────────────────────────────────────────────
        printHeader(date);
        printStaffTable(statList);
        printKpiFailureSection(statList);
        printOverallSummary(totalCompleted, totalOnTime);
    }

    // ── Sorting ────────────────────────────────────────────────────────────

    /**
     * Selection Sort (ascending by roomsCleaned).
     * Demonstrates the Selection Sort algorithm in the report layer.
     */
    private void selectionSortByRoomsCleaned(DoublyLinkedList<StaffStat> list) {
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                if (list.get(j).roomsCleaned < list.get(minIdx).roomsCleaned) {
                    minIdx = j;
                }
            }
            if (minIdx != i) {
                // Swap by exchanging the data references inside the DLL
                StaffStat temp = list.get(i);
                // Replace i with minIdx value, then minIdx with temp
                // We achieve this via a helper swap
                swapInList(list, i, minIdx);
            }
        }
    }

    /**
     * Swaps two elements in the DoublyLinkedList by removing and re-inserting.
     * This approach stays within the LinearADT interface contract.
     */
    private void swapInList(DoublyLinkedList<StaffStat> list, int i, int j) {
        StaffStat a = list.removeAt(j);  // remove higher index first
        StaffStat b = list.removeAt(i);  // then lower index
        list.addAt(i, a);
        list.addAt(j, b);
    }

    // ── Print helpers ──────────────────────────────────────────────────────

    private void printHeader(LocalDate date) {
        System.out.println("\n================================================================================");
        System.out.println("                  HOUSEKEEPING KPI REPORT");
        System.out.println("================================================================================");
        System.out.println("Generated On  : " + LocalDateTime.now().format(GENERATED_FORMAT));
        System.out.println("Purpose       : Measure whether each staff member meets the daily room-");
        System.out.println("                cleaning target and tracks on-time performance.");
        System.out.println("Hotel Value   : Provides accountability metrics for staff appraisals and");
        System.out.println("                highlights systematic bottlenecks in cleaning throughput.");
        System.out.println("Report Date   : " + date);
        System.out.println("KPI Target    : \u2265 " + KPI_MIN_ROOMS + " rooms cleaned per staff per day");
        System.out.println("Time Target   : \u2264 " + TaskLogEntry.CLEANING_TARGET_MINUTES + " minutes per room");
        System.out.println("Sort Technique: Selection Sort (rooms cleaned ascending)");
        System.out.println("--------------------------------------------------------------------------------");
    }

    private void printStaffTable(DoublyLinkedList<StaffStat> statList) {
        System.out.printf("%-6s %-12s %-14s %-16s %-12s %-10s%n",
                "Staff", "Name", "Rooms Cleaned", "On-Time (≤30min)", "Avg Time", "KPI Met?");
        System.out.println("--------------------------------------------------------------------------------");
        for (int i = 0; i < statList.size(); i++) {
            StaffStat s = statList.get(i);
            String kpiMet = s.meetsKpi() ? "✅ YES" : "❌ NO";
            System.out.printf("%-6s %-12s %-14d %-16s %-12s %-10s%n",
                    s.staffId,
                    s.name,
                    s.roomsCleaned,
                    s.onTimeCount + " / " + s.roomsCleaned + String.format(" (%.0f%%)", s.onTimeRate()),
                    String.format("%.1f min", s.avgCleaningMins()),
                    kpiMet);
        }
        System.out.println("--------------------------------------------------------------------------------");
    }

    private void printKpiFailureSection(DoublyLinkedList<StaffStat> statList) {
        // Count failures
        int failures = 0;
        for (int i = 0; i < statList.size(); i++) {
            if (!statList.get(i).meetsKpi()) failures++;
        }

        System.out.println("\n⚠  KPI FAILURE LIST (staff who did NOT clean ≥ " + KPI_MIN_ROOMS + " rooms today)");
        System.out.println("--------------------------------------------------------------------------------");

        if (failures == 0) {
            System.out.println("   ✅ All staff met the KPI target today!");
        } else {
            System.out.printf("   %-6s %-12s %-14s %-10s%n", "Staff", "Name", "Rooms Cleaned", "Short By");
            for (int i = 0; i < statList.size(); i++) {
                StaffStat s = statList.get(i);
                if (!s.meetsKpi()) {
                    int shortBy = KPI_MIN_ROOMS - s.roomsCleaned;
                    System.out.printf("   %-6s %-12s %-14d %-10d%n",
                            s.staffId, s.name, s.roomsCleaned, shortBy);
                }
            }
        }
        System.out.println("--------------------------------------------------------------------------------");
    }

    private void printOverallSummary(int totalCompleted, int totalOnTime) {
        double overallOnTimeRate = totalCompleted == 0
                ? 0.0 : (double) totalOnTime * 100.0 / totalCompleted;

        System.out.println("\nOVERALL SUMMARY");
        System.out.println("Total Rooms Cleaned (Ready) : " + totalCompleted);
        System.out.println("On-Time Completions         : " + totalOnTime + " / " + totalCompleted
                + String.format(" (%.1f%%)", overallOnTimeRate));
        System.out.println("================================================================================");
        System.out.println("MANAGEMENT INTERPRETATION");
        if (totalCompleted == 0) {
            System.out.println("  * No rooms were cleaned to Ready status on this date.");
            System.out.println("    Either no checkout tasks were raised or the date has no data.");
        } else if (overallOnTimeRate >= 80) {
            System.out.println("  * On-time rate is STRONG (" + String.format("%.1f", overallOnTimeRate) + "%).");
            System.out.println("    The team is consistently meeting the 30-minute cleaning target.");
        } else if (overallOnTimeRate >= 50) {
            System.out.println("  * On-time rate is MODERATE (" + String.format("%.1f", overallOnTimeRate) + "%).");
            System.out.println("    A meaningful share of cleanings overran the target time.");
        } else {
            System.out.println("  * On-time rate is LOW (" + String.format("%.1f", overallOnTimeRate) + "%).");
            System.out.println("    Most cleanings exceeded the 30-minute KPI - root-cause investigation needed.");
        }
        System.out.println("RECOMMENDED ACTION");
        if (totalCompleted == 0) {
            System.out.println("  -> Verify that the selected date had active housekeeping tasks.");
        } else if (overallOnTimeRate >= 80) {
            System.out.println("  -> Acknowledge staff performance in daily briefing.");
            System.out.println("  -> Use this as the benchmark for future KPI comparisons.");
        } else if (overallOnTimeRate >= 50) {
            System.out.println("  -> Review task assignments for staff with below-average on-time rates.");
            System.out.println("  -> Investigate whether supply/linen availability caused delays.");
        } else {
            System.out.println("  -> Conduct immediate debrief with housekeeping team.");
            System.out.println("  -> Consider additional training on efficient cleaning procedures.");
            System.out.println("  -> Review room scheduling to reduce simultaneous task load.");
        }
        System.out.println("================================================================================");
    }
}
