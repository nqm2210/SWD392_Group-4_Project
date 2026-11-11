package hospital.validation;
import java.time.LocalDate;
import java.util.*;
public final class AppointmentValidator {
    private static final Set<String> GENDERS=Set.of("Nam","Nữ","Khác");
    private AppointmentValidator() {}
    public static Map<String,String> validate(String fullName,String dateOfBirth,String gender,String phone,String email,String reason,String doctorId,String appointmentDate,String slotId){
        Map<String,String> errors=new LinkedHashMap<>();
        if(blank(fullName)||fullName.trim().length()>100) errors.put("fullName","Họ tên là bắt buộc và không quá 100 ký tự.");
        LocalDate birth=parseDate(dateOfBirth); if(birth==null||birth.isAfter(LocalDate.now())) errors.put("dateOfBirth","Ngày sinh không hợp lệ.");
        if(gender==null||!GENDERS.contains(gender)) errors.put("gender","Vui lòng chọn giới tính hợp lệ.");
        if(blank(phone)||!phone.matches("^(0|\\+84)[0-9]{9,10}$")) errors.put("phone","Số điện thoại không hợp lệ.");
        if(blank(email)||!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")||email.length()>100) errors.put("email","Email là bắt buộc và phải đúng định dạng.");
        if(blank(reason)||reason.trim().length()>255) errors.put("reason","Lý do khám là bắt buộc và không quá 255 ký tự.");
        if(!positive(doctorId)) errors.put("doctorId","Vui lòng chọn bác sĩ.");
        LocalDate visit=parseDate(appointmentDate); if(visit==null||visit.isBefore(LocalDate.now())) errors.put("appointmentDate","Ngày khám không được ở trong quá khứ.");
        if(!positive(slotId)) errors.put("slotId","Vui lòng chọn khung giờ còn trống."); return errors;
    }
    private static boolean blank(String value){return value==null||value.isBlank();}
    private static boolean positive(String value){try{return Integer.parseInt(value)>0;}catch(Exception ex){return false;}}
    private static LocalDate parseDate(String value){try{return LocalDate.parse(value);}catch(Exception ex){return null;}}
}
