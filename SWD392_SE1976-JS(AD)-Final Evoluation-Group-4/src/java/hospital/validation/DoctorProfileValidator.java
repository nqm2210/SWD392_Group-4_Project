package hospital.validation;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DoctorProfileValidator {
    private DoctorProfileValidator() {}

    public static Map<String, String> validate(
            String fullName, String email, String phone, String departmentId,
            String specialization, String academicTitle, String roomNumber,
            String fee, String status) {
        Map<String, String> errors = new LinkedHashMap<>();
        requiredLength(fullName, "fullName", "Nhập họ tên.", 200, errors);
        requiredLength(email, "email", "Nhập email.", 100, errors);
        if (email != null && !email.isBlank() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) errors.put("email", "Email không hợp lệ.");
        requiredLength(phone, "phone", "Nhập số điện thoại.", 20, errors);
        if (positiveInteger(departmentId) == null) errors.put("departmentId", "Chọn khoa hợp lệ.");
        requiredLength(specialization, "specialization", "Nhập chuyên khoa.", 200, errors);
        optionalLength(academicTitle, "academicTitle", 100, errors);
        requiredLength(roomNumber, "roomNumber", "Nhập phòng khám.", 20, errors);
        try {
            BigDecimal value = new BigDecimal(fee);
            if (value.signum() < 0 || value.scale() > 2) errors.put("fee", "Phí khám phải không âm và tối đa 2 chữ số thập phân.");
        } catch (NumberFormatException | NullPointerException ex) {
            errors.put("fee", "Nhập phí khám hợp lệ.");
        }
        if (!"Available".equals(status) && !"Unavailable".equals(status)) errors.put("status", "Chọn trạng thái hợp lệ.");
        return errors;
    }

    public static Integer positiveInteger(String value) {
        try { int number = Integer.parseInt(value); return number > 0 ? number : null; }
        catch (NumberFormatException | NullPointerException ex) { return null; }
    }

    private static void requiredLength(String value, String key, String message, int max, Map<String, String> errors) {
        if (value == null || value.isBlank()) errors.put(key, message);
        else if (value.trim().length() > max) errors.put(key, "Tối đa " + max + " ký tự.");
    }

    private static void optionalLength(String value, String key, int max, Map<String, String> errors) {
        if (value != null && value.length() > max) errors.put(key, "Tối đa " + max + " ký tự.");
    }
}
