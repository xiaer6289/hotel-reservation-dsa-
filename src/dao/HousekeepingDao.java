package dao;

import entity.TaskLogEntry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * DAO for persisting and loading housekeeping task log entries.
 *
 * <p>
 * On first run (or when tasklog.dat is absent / stale), the seeder
 * generates realistic demo data spread across June, July, and August 2026
 * so that all reports display meaningful information immediately.
 * </p>
 *
 * @author Low Wei Shin
 */
public class HousekeepingDao {

  private final String fileName = "tasklog.dat";

  // ── Persistence ────────────────────────────────────────────────────────

  public void saveToFile(TaskLogEntry[] taskLogEntries) {
    try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
      if (taskLogEntries == null) {
        return;
      }

      for (TaskLogEntry task : taskLogEntries) {
        if (task == null) {
          continue;
        }

        writer.println(
            safe(task.getTaskId()) + "|"
                + safe(task.getRoomNumber()) + "|"
                + safe(task.getStatus()) + "|"
                + safe(task.getStaffId()) + "|"
                + safe(task.getRemarks()) + "|"
                + formatTime(task.getCreatedTime()) + "|"
                + formatTime(task.getLastUpdatedTime()) + "|"
                + formatTime(task.getCleaningStartTime()) + "|"
                + task.isCompletedWithinTarget());
      }
    } catch (IOException ex) {
      System.out.println("\nCannot save to " + fileName);
    }
  }

  public TaskLogEntry[] retrieveFromFile() {
    File file = new File(fileName);
    if (!file.exists()) {
      return null;
    }

    TaskLogEntry[] temp = new TaskLogEntry[200];
    int count = 0;

    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;

      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) {
          continue;
        }

        String[] parts = line.split("\\|", -1);
        if (parts.length != 9) {
          continue;
        }

        temp[count++] = new TaskLogEntry(
            parts[0],
            parts[1],
            parts[2],
            emptyToNull(parts[3]),
            emptyToNull(parts[4]),
            parseTime(parts[5]),
            parseTime(parts[6]),
            parseTime(parts[7]),
            Boolean.parseBoolean(parts[8]));
      }
    } catch (IOException | RuntimeException ex) {
      System.out.println("\n[HousekeepingDao] Cannot read " + fileName
          + " - demo data will be reseeded.");
      return null;
    }

    TaskLogEntry[] result = new TaskLogEntry[count];
    System.arraycopy(temp, 0, result, 0, count);
    return result;
  }

  /**
   * Loads existing data; if unavailable, seeds and persists demo data.
   */
  public TaskLogEntry[] loadOrSeed() {
    TaskLogEntry[] loaded = retrieveFromFile();
    if (loaded != null && loaded.length > 0) {
      return loaded;
    }
    System.out.println("[HousekeepingDao] Seeding demo housekeeping data...");
    TaskLogEntry[] seeded = seedDemoData();
    saveToFile(seeded);
    return seeded;
  }

  // ── Text File Helpers ─────────────────────────────────────────────────

  private String safe(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("|", "/")
        .replace("\n", " ")
        .replace("\r", " ");
  }

  private String emptyToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private String formatTime(LocalDateTime value) {
    return value == null ? "" : value.toString();
  }

  private LocalDateTime parseTime(String value) {
    return value == null || value.isBlank()
        ? null
        : LocalDateTime.parse(value);
  }

  // ── Demo Data Seeder ───────────────────────────────────────────────────

  /**
   * Generates ~30 backdated task entries spread across June – August 2026,
   * covering all three staff members with a variety of completion times and
   * outcomes (on-time, late, still in progress, waiting in queue).
   *
   * <p>
   * This ensures that every report – Cleaning Status, Daily Performance,
   * and KPI – displays meaningful data on the very first run.
   * </p>
   */
  private TaskLogEntry[] seedDemoData() {
    // We pre-build all entries in a plain array for simplicity.
    // Fields: taskId, roomNumber, status, staffId, remarks,
    // createdTime, lastUpdatedTime, cleaningStartTime,
    // completedWithinTarget
    Object[][] raw = {

        // ── JUNE 2026 ─────────────────────────────────────────────────

        // June 10 – S001 cleans 5 rooms (meets KPI), some on-time
        { "T001", "101", "Ready", "S001", null,
            ldt(2026, 6, 10, 8, 0), ldt(2026, 6, 10, 8, 25), ldt(2026, 6, 10, 8, 5), true },
        { "T002", "102", "Ready", "S001", null,
            ldt(2026, 6, 10, 8, 30), ldt(2026, 6, 10, 9, 5), ldt(2026, 6, 10, 8, 35), false },
        { "T003", "103", "Ready", "S001", null,
            ldt(2026, 6, 10, 9, 10), ldt(2026, 6, 10, 9, 35), ldt(2026, 6, 10, 9, 15), true },
        { "T004", "104", "Ready", "S001", null,
            ldt(2026, 6, 10, 9, 40), ldt(2026, 6, 10, 10, 5), ldt(2026, 6, 10, 9, 45), true },
        { "T005", "105", "Ready", "S001", null,
            ldt(2026, 6, 10, 10, 10), ldt(2026, 6, 10, 10, 42), ldt(2026, 6, 10, 10, 12), false },

        // June 10 – S002 cleans 4 rooms (MISSES KPI)
        { "T006", "106", "Ready", "S002", null,
            ldt(2026, 6, 10, 8, 0), ldt(2026, 6, 10, 8, 28), ldt(2026, 6, 10, 8, 5), true },
        { "T007", "107", "Ready", "S002", null,
            ldt(2026, 6, 10, 8, 35), ldt(2026, 6, 10, 9, 10), ldt(2026, 6, 10, 8, 40), false },
        { "T008", "108", "Ready", "S002", null,
            ldt(2026, 6, 10, 9, 15), ldt(2026, 6, 10, 9, 42), ldt(2026, 6, 10, 9, 20), true },
        { "T009", "109", "Ready", "S002", null,
            ldt(2026, 6, 10, 9, 50), ldt(2026, 6, 10, 10, 18), ldt(2026, 6, 10, 9, 52), true },

        // June 10 – S003 cleans 3 rooms (MISSES KPI)
        { "T010", "110", "Ready", "S003", null,
            ldt(2026, 6, 10, 8, 0), ldt(2026, 6, 10, 8, 20), ldt(2026, 6, 10, 8, 5), true },
        { "T011", "101", "Ready", "S003", null,
            ldt(2026, 6, 10, 8, 25), ldt(2026, 6, 10, 9, 0), ldt(2026, 6, 10, 8, 30), false },
        { "T012", "102", "Ready", "S003", null,
            ldt(2026, 6, 10, 9, 5), ldt(2026, 6, 10, 9, 33), ldt(2026, 6, 10, 9, 10), true },

        // ── JULY 2026 ─────────────────────────────────────────────────

        // July 15 – all three staff meet KPI
        { "T013", "103", "Ready", "S001", null,
            ldt(2026, 7, 15, 8, 0), ldt(2026, 7, 15, 8, 22), ldt(2026, 7, 15, 8, 5), true },
        { "T014", "104", "Ready", "S001", null,
            ldt(2026, 7, 15, 8, 28), ldt(2026, 7, 15, 8, 55), ldt(2026, 7, 15, 8, 30), true },
        { "T015", "105", "Ready", "S001", null,
            ldt(2026, 7, 15, 9, 0), ldt(2026, 7, 15, 9, 28), ldt(2026, 7, 15, 9, 5), true },
        { "T016", "106", "Ready", "S001", null,
            ldt(2026, 7, 15, 9, 35), ldt(2026, 7, 15, 10, 5), ldt(2026, 7, 15, 9, 37), false },
        { "T017", "107", "Ready", "S001", null,
            ldt(2026, 7, 15, 10, 10), ldt(2026, 7, 15, 10, 38), ldt(2026, 7, 15, 10, 12), true },
        { "T018", "108", "Ready", "S002", null,
            ldt(2026, 7, 15, 8, 0), ldt(2026, 7, 15, 8, 25), ldt(2026, 7, 15, 8, 5), true },
        { "T019", "109", "Ready", "S002", null,
            ldt(2026, 7, 15, 8, 30), ldt(2026, 7, 15, 8, 58), ldt(2026, 7, 15, 8, 35), true },
        { "T020", "110", "Ready", "S002", null,
            ldt(2026, 7, 15, 9, 5), ldt(2026, 7, 15, 9, 40), ldt(2026, 7, 15, 9, 10), false },
        { "T021", "101", "Ready", "S002", null,
            ldt(2026, 7, 15, 9, 45), ldt(2026, 7, 15, 10, 12), ldt(2026, 7, 15, 9, 48), true },
        { "T022", "102", "Ready", "S002", null,
            ldt(2026, 7, 15, 10, 18), ldt(2026, 7, 15, 10, 45), ldt(2026, 7, 15, 10, 20), true },
        { "T023", "103", "Ready", "S003", null,
            ldt(2026, 7, 15, 8, 0), ldt(2026, 7, 15, 8, 30), ldt(2026, 7, 15, 8, 5), true },
        { "T024", "104", "Ready", "S003", null,
            ldt(2026, 7, 15, 8, 35), ldt(2026, 7, 15, 9, 15), ldt(2026, 7, 15, 8, 40), false },
        { "T025", "105", "Ready", "S003", null,
            ldt(2026, 7, 15, 9, 20), ldt(2026, 7, 15, 9, 48), ldt(2026, 7, 15, 9, 25), true },
        { "T026", "106", "Ready", "S003", null,
            ldt(2026, 7, 15, 9, 55), ldt(2026, 7, 15, 10, 22), ldt(2026, 7, 15, 10, 0), true },
        { "T027", "107", "Ready", "S003", null,
            ldt(2026, 7, 15, 10, 28), ldt(2026, 7, 15, 10, 55), ldt(2026, 7, 15, 10, 30), true },

        // July 20 – partial day (S001 misses KPI, S002 meets, S003 meets)
        { "T028", "108", "Ready", "S001", null,
            ldt(2026, 7, 20, 8, 0), ldt(2026, 7, 20, 8, 28), ldt(2026, 7, 20, 8, 5), true },
        { "T029", "109", "Ready", "S001", null,
            ldt(2026, 7, 20, 8, 35), ldt(2026, 7, 20, 9, 10), ldt(2026, 7, 20, 8, 40), false },
        { "T030", "110", "Ready", "S001", null,
            ldt(2026, 7, 20, 9, 15), ldt(2026, 7, 20, 9, 40), ldt(2026, 7, 20, 9, 20), true },
        { "T031", "101", "Ready", "S002", null,
            ldt(2026, 7, 20, 8, 0), ldt(2026, 7, 20, 8, 25), ldt(2026, 7, 20, 8, 5), true },
        { "T032", "102", "Ready", "S002", null,
            ldt(2026, 7, 20, 8, 30), ldt(2026, 7, 20, 8, 58), ldt(2026, 7, 20, 8, 35), true },
        { "T033", "103", "Ready", "S002", null,
            ldt(2026, 7, 20, 9, 5), ldt(2026, 7, 20, 9, 35), ldt(2026, 7, 20, 9, 10), true },
        { "T034", "104", "Ready", "S002", null,
            ldt(2026, 7, 20, 9, 40), ldt(2026, 7, 20, 10, 8), ldt(2026, 7, 20, 9, 45), true },
        { "T035", "105", "Ready", "S002", null,
            ldt(2026, 7, 20, 10, 15), ldt(2026, 7, 20, 10, 42), ldt(2026, 7, 20, 10, 18), true },
        { "T036", "106", "Ready", "S003", null,
            ldt(2026, 7, 20, 8, 0), ldt(2026, 7, 20, 8, 27), ldt(2026, 7, 20, 8, 5), true },
        { "T037", "107", "Ready", "S003", null,
            ldt(2026, 7, 20, 8, 33), ldt(2026, 7, 20, 9, 5), ldt(2026, 7, 20, 8, 37), false },
        { "T038", "108", "Ready", "S003", null,
            ldt(2026, 7, 20, 9, 10), ldt(2026, 7, 20, 9, 38), ldt(2026, 7, 20, 9, 15), true },
        { "T039", "109", "Ready", "S003", null,
            ldt(2026, 7, 20, 9, 45), ldt(2026, 7, 20, 10, 12), ldt(2026, 7, 20, 9, 50), true },
        { "T040", "110", "Ready", "S003", null,
            ldt(2026, 7, 20, 10, 18), ldt(2026, 7, 20, 10, 46), ldt(2026, 7, 20, 10, 22), true },

        // ── AUGUST 2026 ───────────────────────────────────────────────

        // Aug 5 – a busy day: all staff clean 6 rooms each (above KPI)
        { "T041", "101", "Ready", "S001", null,
            ldt(2026, 8, 5, 7, 50), ldt(2026, 8, 5, 8, 18), ldt(2026, 8, 5, 7, 55), true },
        { "T042", "102", "Ready", "S001", null,
            ldt(2026, 8, 5, 8, 25), ldt(2026, 8, 5, 8, 52), ldt(2026, 8, 5, 8, 28), true },
        { "T043", "103", "Ready", "S001", null,
            ldt(2026, 8, 5, 9, 0), ldt(2026, 8, 5, 9, 35), ldt(2026, 8, 5, 9, 5), false },
        { "T044", "104", "Ready", "S001", null,
            ldt(2026, 8, 5, 9, 40), ldt(2026, 8, 5, 10, 5), ldt(2026, 8, 5, 9, 42), true },
        { "T045", "105", "Ready", "S001", null,
            ldt(2026, 8, 5, 10, 12), ldt(2026, 8, 5, 10, 40), ldt(2026, 8, 5, 10, 15), true },
        { "T046", "106", "Ready", "S001", null,
            ldt(2026, 8, 5, 10, 45), ldt(2026, 8, 5, 11, 8), ldt(2026, 8, 5, 10, 48), true },
        { "T047", "107", "Ready", "S002", null,
            ldt(2026, 8, 5, 7, 50), ldt(2026, 8, 5, 8, 20), ldt(2026, 8, 5, 7, 55), true },
        { "T048", "108", "Ready", "S002", null,
            ldt(2026, 8, 5, 8, 25), ldt(2026, 8, 5, 9, 0), ldt(2026, 8, 5, 8, 30), false },
        { "T049", "109", "Ready", "S002", null,
            ldt(2026, 8, 5, 9, 5), ldt(2026, 8, 5, 9, 30), ldt(2026, 8, 5, 9, 8), true },
        { "T050", "110", "Ready", "S002", null,
            ldt(2026, 8, 5, 9, 35), ldt(2026, 8, 5, 10, 2), ldt(2026, 8, 5, 9, 38), true },
        { "T051", "101", "Ready", "S002", null,
            ldt(2026, 8, 5, 10, 8), ldt(2026, 8, 5, 10, 35), ldt(2026, 8, 5, 10, 11), true },
        { "T052", "102", "Ready", "S002", null,
            ldt(2026, 8, 5, 10, 40), ldt(2026, 8, 5, 11, 5), ldt(2026, 8, 5, 10, 43), true },
        { "T053", "103", "Ready", "S003", null,
            ldt(2026, 8, 5, 7, 50), ldt(2026, 8, 5, 8, 15), ldt(2026, 8, 5, 7, 55), true },
        { "T054", "104", "Ready", "S003", null,
            ldt(2026, 8, 5, 8, 20), ldt(2026, 8, 5, 8, 55), ldt(2026, 8, 5, 8, 25), false },
        { "T055", "105", "Ready", "S003", null,
            ldt(2026, 8, 5, 9, 0), ldt(2026, 8, 5, 9, 28), ldt(2026, 8, 5, 9, 3), true },
        { "T056", "106", "Ready", "S003", null,
            ldt(2026, 8, 5, 9, 35), ldt(2026, 8, 5, 10, 0), ldt(2026, 8, 5, 9, 38), true },
        { "T057", "107", "Ready", "S003", null,
            ldt(2026, 8, 5, 10, 5), ldt(2026, 8, 5, 10, 32), ldt(2026, 8, 5, 10, 8), true },
        { "T058", "108", "Ready", "S003", null,
            ldt(2026, 8, 5, 10, 38), ldt(2026, 8, 5, 11, 2), ldt(2026, 8, 5, 10, 41), true },

        // ── TODAY (current run) – dynamic offsets from LocalDateTime.now() ──
        // These use ago() so countdowns are always live and realistic no
        // matter when the application is started.

        // S001 completed 2 rooms earlier today (contributes to today's KPI)
        { "T059", "109", "Ready", "S001", null,
            ago(95), ago(68), ago(92), true },
        { "T060", "110", "Ready", "S001", null,
            ago(62), ago(38), ago(59), true },

        // S002 completed 1 room, currently cleaning Room 202
        // (started 20 min ago → 10 min left on countdown)
        { "T061", "101", "Ready", "S002", null,
            ago(90), ago(62), ago(87), true },
        { "T062", "102", "Cleaning In Progress", "S002", "Auto-assigned",
            ago(22), null, ago(20), false },

        // S003 currently cleaning Room 302
        // (started 5 min ago → 25 min left on countdown)
        { "T063", "103", "Cleaning In Progress", "S003", "Auto-assigned",
            ago(7), null, ago(5), false },

        // Room 401 is dirty and waiting in queue for S001 (who is now free)
        { "T064", "104", "Dirty", null, "Waiting in queue",
            ago(3), null, null, false },
    };

    TaskLogEntry[] entries = new TaskLogEntry[raw.length];
    for (int i = 0; i < raw.length; i++) {
      Object[] r = raw[i];
      entries[i] = new TaskLogEntry(
          (String) r[0], // taskId
          (String) r[1], // roomNumber
          (String) r[2], // status
          (String) r[3], // staffId
          (String) r[4], // remarks
          (LocalDateTime) r[5], // createdTime
          r[6] != null ? (LocalDateTime) r[6] : LocalDateTime.now(), // lastUpdatedTime
          (LocalDateTime) r[7], // cleaningStartTime
          (boolean) r[8] // completedWithinTarget
      );
    }
    return entries;
  }

  // ── Time helpers ───────────────────────────────────────────────────────

  /** Returns a LocalDateTime that is {@code minutesAgo} minutes before now. */
  private static LocalDateTime ago(long minutesAgo) {
    return LocalDateTime.now().minusMinutes(minutesAgo);
  }

  private static LocalDateTime ldt(int yr, int mo, int day, int hr, int min) {
    return LocalDateTime.of(yr, mo, day, hr, min);
  }
}
