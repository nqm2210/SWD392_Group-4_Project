package hospital.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillSummary(
        int billId,
        String billNumber,
        String appointmentNumber,
        String patientName,
        String doctorName,
        String department,
        BigDecimal consultationFee,
        BigDecimal serviceAmount,
        BigDecimal totalAmount,
        String paymentStatus,
        String receiptNumber,
        BigDecimal amountPaid,
        LocalDateTime paidAt) { }
