package hospital.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RevenueDetailRow(
        int paymentId,
        String receiptNumber,
        String billNumber,
        String appointmentNumber,
        String patientName,
        String doctorName,
        String department,
        BigDecimal consultationFee,
        BigDecimal serviceAmount,
        BigDecimal amountPaid,
        String paymentMethod,
        LocalDateTime paidAt) {}

