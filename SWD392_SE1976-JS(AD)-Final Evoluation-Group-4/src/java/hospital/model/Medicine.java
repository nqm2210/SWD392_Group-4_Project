package hospital.model;

import java.math.BigDecimal;

public record Medicine(int id, String code, String name, String unit, BigDecimal unitPrice, boolean active) { }
