package hospital.validation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SchedulePublicationValidator {
    private SchedulePublicationValidator() { }

    public static Map<String, String> validate(String dateValue, String shift, LocalDate today) {
        Map<String, String> errors = new LinkedHashMap<>();
        try {
            LocalDate date = LocalDate.parse(dateValue == null ? "" : dateValue);
            if (date.isBefore(today) || date.isAfter(today.plusYears(1))) {
                errors.put("workDate", "Chọn ngày từ hôm nay đến trong vòng một năm.");
            }
        } catch (DateTimeParseException ex) {
            errors.put("workDate", "Nhập ngày làm việc hợp lệ.");
        }
        if (!"AM".equals(shift) && !"PM".equals(shift)) {
            errors.put("shift", "Chọn ca sáng hoặc ca chiều.");
        }
        return errors;
    }
}
