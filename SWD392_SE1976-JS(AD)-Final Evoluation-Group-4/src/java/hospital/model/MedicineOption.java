package hospital.model;

import java.math.BigDecimal;

public record MedicineOption(int id,String name,String unit,BigDecimal unitPrice) {}
