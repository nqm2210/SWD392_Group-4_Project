package hospital.model;

import java.math.BigDecimal;

public record MedicalService(
        int id,
        String code,
        String name,
        int departmentId,
        String departmentName,
        BigDecimal unitPrice,
        String description,
        boolean active) {}
