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
        requiredLength(fullName, "fullName", "Nhập họ tên.", 100, errors);
        requiredLength(email, "email", "Nhập email.", 100, errors);
        if (email != null && !email.isBlank() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) errors.put("email", "Email không hợp lệ.");
        requiredLength(phone, "phone", "Nhập số điện thoại.", 20, errors);
        if (positiveInteger(departmentId) == null) errors.put("departmentId", "Chọn khoa hợp lệ.");
        requiredLength(specialization, "specialization", "Nhập chuyên khoa.", 100, errors);
        optionalLength(academicTitle, "academicTitle", 50, errors);
        requiredLength(roomNumber, "roomNumber", "Nhập phòng khám.", 20, errors);
        String safeFee = fee == null ? "" : fee.trim();
        if (!safeFee.matches("(?:0|[1-9][0-9]{0,15})(?:\\.[0-9]{1,2})?")) {
            errors.put("fee", "Phí khám phải không âm, tối đa 16 chữ số phần nguyên và 2 chữ số thập phân; không dùng dạng số mũ.");
        } else {
            try {
                BigDecimal value = new BigDecimal(safeFee);
                if (value.signum() < 0 || value.scale() > 2) errors.put("fee", "Phí khám phải không âm và tối đa 2 chữ số thập phân.");
            } catch (NumberFormatException ex) {
                errors.put("fee", "Nhập phí khám hợp lệ.");
            }
        }
        if (!"Available".equals(status) && !"Unavailable".equals(status)) errors.put("status", "Chọn trạng thái hợp lệ.");
        return errors;
    }

    public static Map<String, String> validate(
            String username, String newPassword, String fullName, String email,
            String phone, String departmentId, String specialization,
            String academicTitle, String roomNumber, String fee, String status) {
        Map<String, String> errors = validate(fullName, email, phone, departmentId,
                specialization, academicTitle, roomNumber, fee, status);
        validateCredentials(username, newPassword, errors);
        return errors;
    }

    public static Map<String, String> validateStaffAccount(
            String username, String newPassword, String fullName,
            String email, String phone, String departmentId) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateCredentials(username, newPassword, errors);
        requiredLength(fullName, "fullName", "Nhập họ tên.", 100, errors);
        requiredLength(email, "email", "Nhập email.", 100, errors);
        String safeEmail = email == null ? "" : email.trim();
        if (!safeEmail.isEmpty() && !safeEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            errors.put("email", "Email không hợp lệ.");
        }
        requiredLength(phone, "phone", "Nhập số điện thoại.", 20, errors);
        if (departmentId != null && !departmentId.isEmpty() && positiveInteger(departmentId) == null) {
            errors.put("departmentId", "Chọn khoa hợp lệ.");
        }
        return errors;
    }

    private static void validateCredentials(String username, String newPassword, Map<String, String> errors) {
        String safeUsername = username == null ? "" : username.trim();
        if (!safeUsername.matches("^[A-Za-z0-9._-]{3,50}$")) {
            errors.put("username", "Tên đăng nhập gồm 3–50 ký tự (chữ không dấu, số, dấu chấm, gạch ngang, gạch dưới).");
        }
        if (newPassword != null && !newPassword.isEmpty()) {
            int passwordLength = newPassword.trim().length();
            if (passwordLength < 6 || passwordLength > 72) {
                errors.put("newPassword", "Mật khẩu mới phải từ 6 đến 72 ký tự (để trống nếu giữ nguyên).");
            }
        }
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
