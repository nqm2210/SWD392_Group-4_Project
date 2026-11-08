package hospital.validation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LeaveRequestValidator {
    private LeaveRequestValidator() {}

    public static Map<String, String> validate(String startDate, String endDate, String reason, LocalDate today) {
        Map<String, String> errors = new LinkedHashMap<>();
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        String safeReason = reason == null ? "" : reason.trim();
        if (start == null) errors.put("startDate", "Chọn ngày bắt đầu hợp lệ.");
        else if (start.isBefore(today)) errors.put("startDate", "Ngày bắt đầu không được ở quá khứ.");
        if (end == null) errors.put("endDate", "Chọn ngày kết thúc hợp lệ.");
        else if (start != null && end.isBefore(start)) errors.put("endDate", "Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        if (safeReason.isEmpty() || safeReason.length() > 255) {
            errors.put("reason", "Lý do là bắt buộc và tối đa 255 ký tự.");
        }
        return errors;
    }

    private static LocalDate parseDate(String value) {
        try { return LocalDate.parse(value == null ? "" : value.trim()); }
        catch (DateTimeParseException ex) { return null; }
    }
}
