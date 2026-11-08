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
    public static void requireServiceOrdersReviewed(int pendingCount) throws SQLException {
        if (pendingCount > 0) throw new SQLException("SERVICE_ORDERS_PENDING: Còn chỉ định chưa hoàn tất hoặc chưa phát hành kết quả.");
    }
}
