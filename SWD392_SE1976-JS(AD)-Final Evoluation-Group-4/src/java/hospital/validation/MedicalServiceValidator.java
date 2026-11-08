package hospital.validation;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MedicalServiceValidator {
    private MedicalServiceValidator() {}

    public static Map<String, String> validate(String code, String name, String departmentId,
            String price, String description) {
        Map<String, String> errors = new LinkedHashMap<>();
        String safeCode = code == null ? "" : code.trim();
        String safeName = name == null ? "" : name.trim();
        String safeDescription = description == null ? "" : description.trim();
        if (!safeCode.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,19}")) {
            errors.put("serviceCode", "Mã dịch vụ gồm tối đa 20 chữ/số, dấu gạch ngang hoặc gạch dưới.");
        }
        if (safeName.isEmpty() || safeName.length() > 100) {
            errors.put("serviceName", "Tên dịch vụ là bắt buộc và tối đa 100 ký tự.");
        }
        Integer department = DoctorProfileValidator.positiveInteger(departmentId);
        if (department == null) errors.put("departmentId", "Hãy chọn khoa hợp lệ.");
        try {
            String value = price == null ? "" : price.trim();
            if (!value.matches("(?:0|[1-9][0-9]{0,15})(?:\\.[0-9]{1,2})?")) {
                errors.put("unitPrice", "Giá phải lớn hơn 0, tối đa 16 chữ số phần nguyên và 2 chữ số thập phân; không dùng dạng số mũ.");
            } else {
                BigDecimal amount = new BigDecimal(value);
                if (amount.signum() <= 0) {
                    errors.put("unitPrice", "Giá phải lớn hơn 0, tối đa 16 chữ số phần nguyên và 2 chữ số thập phân.");
                }
            }
        } catch (NumberFormatException ex) {
            errors.put("unitPrice", "Nhập giá dịch vụ hợp lệ.");
        }
        if (safeDescription.length() > 255) {
            errors.put("description", "Mô tả tối đa 255 ký tự.");
        }
        return errors;
    }
}
