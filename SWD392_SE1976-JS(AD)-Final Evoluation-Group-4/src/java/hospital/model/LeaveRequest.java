package hospital.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveRequest(
        int id,
        int userId,
        String username,
        String fullName,
        String department,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        String status,
        String managerResponseReason,
        LocalDateTime createdAt,
        String approvedByName,
        String requesterRole,
        int affectedAppointments) {}
