package hospital.model;

import java.time.LocalDateTime;

public record AppointmentChange(
        int logId,
        String appointmentNumber,
        String action,
        String details,
        LocalDateTime changedAt) { }
