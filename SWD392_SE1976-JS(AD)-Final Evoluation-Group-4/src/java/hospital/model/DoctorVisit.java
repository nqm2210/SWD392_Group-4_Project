package hospital.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record DoctorVisit(int appointmentId, String appointmentNumber, String patientName, String phone,
        LocalDate date, LocalTime startTime, String status, Integer queueNumber,
        String diagnosis, String clinicalNotes, String treatmentPlan, String recordStatus) {}
