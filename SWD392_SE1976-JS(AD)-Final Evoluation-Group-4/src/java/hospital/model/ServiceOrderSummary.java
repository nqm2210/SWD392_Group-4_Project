package hospital.model;

import java.time.LocalDateTime;

public record ServiceOrderSummary(int orderId,String orderNumber,String patientName,String appointmentNumber,
        String doctorName,String department,String serviceName,String priority,String instructions,
        String orderStatus,String resultFindings,String resultDetails,String resultStatus,LocalDateTime orderedAt) {}
