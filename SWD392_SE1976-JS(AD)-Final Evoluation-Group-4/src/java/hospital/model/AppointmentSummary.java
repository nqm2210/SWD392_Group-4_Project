package hospital.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentSummary(
        int id,
        String appointmentNumber,
        String patientName,
        String phone,
        String doctorName,
        String department,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        Integer queueNumber) {}
