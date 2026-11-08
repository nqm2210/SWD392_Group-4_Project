package hospital.model;

import java.math.BigDecimal;

public record RevenueReportRow(String department,String paymentMethod,int paymentCount,BigDecimal amount) {}
