package hospital.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillSummary(
        Integer billId,
        String billNumber,
        String appointmentNumber,
        String appointmentStatus,
        Integer queueNumber,
        String patientName,
        String doctorName,
        String department,
        BigDecimal consultationFee,
        BigDecimal serviceAmount,
        BigDecimal medicineReferenceAmount,
        BigDecimal totalAmount,
        String paymentStatus,
        String receiptNumber,
        BigDecimal amountPaid,
        LocalDateTime paidAt) { }
