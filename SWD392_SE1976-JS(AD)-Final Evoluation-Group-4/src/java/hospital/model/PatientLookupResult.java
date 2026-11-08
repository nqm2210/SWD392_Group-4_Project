package hospital.model;

import java.time.LocalDate;

public record PatientLookupResult(
        int patientId,
        String fullName,
        LocalDate dateOfBirth,
        String gender,
        String phone,
        String email,
        String identityNumber) { }
