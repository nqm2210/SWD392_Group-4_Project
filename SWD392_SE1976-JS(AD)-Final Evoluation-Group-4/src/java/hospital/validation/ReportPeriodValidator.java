package hospital.validation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ReportPeriodValidator {
    private ReportPeriodValidator() {}

    public static Map<String,String> validate(String fromValue,String toValue){
        Map<String,String> errors=new LinkedHashMap<>();
        LocalDate from=parse(fromValue),to=parse(toValue);
        if(from==null)errors.put("fromDate","Chọn ngày bắt đầu hợp lệ.");
        if(to==null)errors.put("toDate","Chọn ngày kết thúc hợp lệ.");
        if(from!=null&&to!=null&&to.isBefore(from))errors.put("toDate","Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        if(from!=null&&to!=null&&from.isBefore(to.minusYears(5)))errors.put("fromDate","Khoảng báo cáo tối đa là 5 năm.");
        return errors;
    }
    private static LocalDate parse(String value){try{return LocalDate.parse(value==null?"":value.trim());}catch(DateTimeParseException ex){return null;}}
}
