package hospital.model;

import java.math.BigDecimal;

public record DoctorProfile(
        int doctorId,
        int userId,
        String username,
        String fullName,
        String email,
        String phone,
        int departmentId,
        String departmentName,
        String specialization,
        String academicTitle,
        String roomNumber,
        BigDecimal consultationFee,
        String status,
        boolean published) {}
