package hospital.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record PatientAppointment(
        int appointmentId,
        String appointmentNumber,
        int doctorId,
        String doctorName,
        String department,
        LocalDate appointmentDate,
        String shift,
        int slotId,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String reason,
        Integer billId,
        String billStatus,
        BigDecimal billAmount,
        boolean editable,
        boolean reschedulable) { }
