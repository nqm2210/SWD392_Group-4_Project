package hospital.validation;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MedicineValidator {
    private MedicineValidator() { }

    public static Map<String, String> validate(String code, String name, String unit, String price) {
        Map<String, String> errors = new LinkedHashMap<>();
        String safeCode = code == null ? "" : code.trim();
        String safeName = name == null ? "" : name.trim();
        String safeUnit = unit == null ? "" : unit.trim();
        String safePrice = price == null ? "" : price.trim();
        if (!safeCode.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,19}")) {
            errors.put("medicineCode", "Mã thuốc gồm tối đa 20 chữ/số, dấu gạch ngang hoặc gạch dưới; không dấu, không khoảng trắng.");
        }
        if (safeName.isEmpty() || safeName.length() > 100) {
            errors.put("medicineName", "Tên thuốc là bắt buộc và tối đa 100 ký tự.");
        }
        if (safeUnit.isEmpty() || safeUnit.length() > 50) {
            errors.put("unit", "Đơn vị thuốc là bắt buộc và tối đa 50 ký tự.");
        }
        try {
            if (!safePrice.matches("(?:0|[1-9][0-9]{0,15})(?:\\.[0-9]{1,2})?")) {
                errors.put("unitPrice", "Giá phải lớn hơn 0, tối đa 16 chữ số phần nguyên và 2 chữ số thập phân; không dùng dạng số mũ.");
            } else if (new BigDecimal(safePrice).signum() <= 0) {
                errors.put("unitPrice", "Giá phải lớn hơn 0.");
            }
        } catch (NumberFormatException ex) {
            errors.put("unitPrice", "Nhập giá thuốc hợp lệ.");
        }
        return errors;
    }
}
