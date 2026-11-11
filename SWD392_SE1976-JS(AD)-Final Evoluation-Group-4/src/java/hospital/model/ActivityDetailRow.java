package hospital.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record ActivityDetailRow(
        int appointmentId,
        String appointmentNumber,
        LocalDate appointmentDate,
        LocalTime startTime,
        String patientName,
        String phone,
        String doctorName,
        String department,
        String status,
        String reason) {}

