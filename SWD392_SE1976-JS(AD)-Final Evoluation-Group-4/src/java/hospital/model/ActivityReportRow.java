package hospital.model;

import java.time.LocalDate;

public record ActivityReportRow(LocalDate date,int total,int booked,int checkedIn,int inConsultation,
        int completed,int cancelled,int needsProcessing,int noShow) {}
