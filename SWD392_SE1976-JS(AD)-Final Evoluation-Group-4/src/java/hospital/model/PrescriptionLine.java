package hospital.model;

import java.math.BigDecimal;

public record PrescriptionLine(int detailId, int medicineId, String medicineCode, String medicineName,
        String unit, int quantity, String dosage, BigDecimal unitPrice, String doctorInstructions) { }
