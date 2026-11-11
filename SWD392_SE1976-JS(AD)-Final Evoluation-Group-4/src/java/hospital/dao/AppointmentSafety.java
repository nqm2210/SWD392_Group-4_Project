package hospital.dao;

import java.sql.SQLException;

public final class AppointmentSafety {
    private AppointmentSafety() { }
    public static void requireSlotReserved(int updatedRows) throws SQLException {
        if (updatedRows != 1) throw new SQLException("Khung giờ vừa được đặt hoặc không còn khả dụng.");
    }
    public static void requireNoConsultationConflict(boolean consultationExists) throws SQLException {
        if (consultationExists) throw new SQLException("LEAVE_CONFLICT_IN_CONSULTATION: Không thể duyệt nghỉ khi còn lượt khám đang diễn ra.");
    }
    public static void requireDoctorOwnership(boolean owned) throws SQLException {
        if (!owned) throw new SQLException("Bác sĩ không có quyền truy cập hoặc xử lý ca khám này.");
    }
    public static void requireRecordEditable(String recordStatus) throws SQLException {
        if ("COMPLETED".equalsIgnoreCase(recordStatus)) throw new SQLException("Hồ sơ đã hoàn tất ở chế độ chỉ đọc, không được chỉnh sửa.");
    }
    public static void requireOnlineCancellationWindow(boolean cancelable2Hours) throws SQLException {
        if (!cancelable2Hours) throw new SQLException("Thời gian hủy trực tuyến đã hết (phải trước giờ khám ít nhất 2 giờ). Vui lòng liên hệ tổng đài phòng khám.");
    }
    public static void requireAppointmentCancelableStatus(String status) throws SQLException {
        if (!"BOOKED".equalsIgnoreCase(status) && !"NEEDS_PROCESSING".equalsIgnoreCase(status)) {
            throw new SQLException("Chỉ lịch hẹn BOOKED hoặc NEEDS_PROCESSING mới có thể hủy.");
        }
    }
}
