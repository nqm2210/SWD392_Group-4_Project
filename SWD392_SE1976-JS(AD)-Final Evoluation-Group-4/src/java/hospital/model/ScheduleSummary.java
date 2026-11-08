package hospital.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleSummary(
        int id,
        LocalDate workDate,
        String shiftName,
        String status,
        LocalTime startTime,
        LocalTime endTime,
        int slotCount,
        int bookedCount,
        int blockedCount) {}
