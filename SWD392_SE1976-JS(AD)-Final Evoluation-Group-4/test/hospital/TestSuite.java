package hospital;
import hospital.security.RoleAccess;
import hospital.security.PasswordHasher;
import hospital.security.CsrfToken;
import hospital.security.PatientAccessCode;
import hospital.dao.AppointmentSafety;
import hospital.validation.AppointmentValidator;
import hospital.validation.DoctorProfileValidator;
import hospital.validation.MedicalServiceValidator;
import hospital.validation.MedicineValidator;
import hospital.validation.LeaveRequestValidator;
import hospital.validation.ReportPeriodValidator;
import hospital.validation.SchedulePublicationValidator;
import hospital.validation.ReceptionStatusFilter;
import hospital.validation.PatientLookupValidator;
import java.time.LocalDate;
public class TestSuite {
    private static int checks;
    public static void main(String[] args){
        var valid=AppointmentValidator.validate("Nguyễn Văn An","1990-01-01","Nam","0901234567","an@example.com","Đau đầu","1",LocalDate.now().plusDays(1).toString(),"1");
        require(valid.isEmpty(),"Dữ liệu hợp lệ phải được chấp nhận");
        var invalid=AppointmentValidator.validate("","2999-01-01","x","abc","bad","","0","2020-01-01","");
        require(invalid.size()==9,"Phải bắt đủ 9 lỗi đầu vào");
        var missingGender=AppointmentValidator.validate("Nguyễn Văn An","1990-01-01",null,"0901234567","","Đau đầu","1",LocalDate.now().plusDays(1).toString(),"1");
        require(missingGender.containsKey("gender"),"Thiếu giới tính phải trả lỗi validation thay vì HTTP 500");
        var futureBirthDate=AppointmentValidator.validate("Nguyễn Văn An",LocalDate.now().plusDays(1).toString(),"Nam","0901234567","","Đau đầu","1",LocalDate.now().plusDays(2).toString(),"1");
        require(futureBirthDate.containsKey("dateOfBirth"),"Không cho đặt lịch với ngày sinh ở tương lai như form đang báo lỗi");
        var missingSlot=AppointmentValidator.validate("Nguyễn Văn An","1990-01-01","Nam","0901234567","","Đau đầu","1",LocalDate.now().plusDays(1).toString(),"");
        require(missingSlot.containsKey("slotId"),"Không cho gửi lịch hẹn khi chưa có khung giờ thật");
        require(RoleAccess.menusFor("Receptionist").contains("Check-in"),"Receptionist phải có quyền check-in");
        require(RoleAccess.menusFor("Manager").contains("Danh mục thuốc"),"Danh mục thuốc chỉ được thêm dưới Manager theo yêu cầu hiện tại");
        require(!RoleAccess.pagesFor("Doctor").contains("medicines"),"Doctor không được quản trị danh mục thuốc");
        require(!RoleAccess.isSupported("Cashier"),"Không được tự thêm role không có trong database");
        String hash=PasswordHasher.hash("test-password-2026");
        require(PasswordHasher.verify("test-password-2026",hash),"Hash PBKDF2 phải xác thực đúng mật khẩu");
        require(!PasswordHasher.verify("wrong-password",hash),"Hash PBKDF2 phải từ chối mật khẩu sai");
        require(!PasswordHasher.verify("anything","1"),"Hash bị hỏng phải bị từ chối");
        String patientCode=PatientAccessCode.generate();String patientCodeHash=PatientAccessCode.hash(patientCode);
        require(patientCode.length()==43&&patientCodeHash.length()==43,"AccessCode ngẫu nhiên và hash phải vừa cột varchar(50)");
        require(!patientCode.equals(patientCodeHash)&&PatientAccessCode.verify(patientCode,patientCodeHash),"Mã bệnh nhân chỉ lưu hash nhưng xác minh được");
        require(!PatientAccessCode.verify("wrong-code",patientCodeHash),"Mã bệnh nhân sai phải bị từ chối");
        require(!CsrfToken.isValid(null,"forged-token"),"Thiếu session phải từ chối CSRF thay vì phát sinh lỗi");
        require("".equals(ReceptionStatusFilter.normalize(null)),"Bộ lọc lịch lễ tân phải chuẩn hóa status null mà không lỗi");
        require("".equals(ReceptionStatusFilter.normalize("  ")),"Bộ lọc lịch lễ tân phải xem status rỗng là tất cả");
        require("BOOKED".equals(ReceptionStatusFilter.normalize(" BOOKED ")),"Bộ lọc lịch lễ tân phải chuẩn hóa trạng thái hợp lệ");
        require("".equals(ReceptionStatusFilter.normalize("INVALID")),"Bộ lọc lịch lễ tân phải bỏ trạng thái lạ");
        require(PatientLookupValidator.isValidQuery("0901234567"),"Cho phép tra cứu hồ sơ bằng số điện thoại");
        require(PatientLookupValidator.isValidQuery("ID-12345"),"Cho phép tra cứu hồ sơ bằng mã giấy tờ");
        require(!PatientLookupValidator.isValidQuery("  "),"Từ chối giá trị tra cứu rỗng");
        require(!PatientLookupValidator.isValidQuery("x".repeat(21)),"Từ chối giá trị tra cứu vượt giới hạn schema");
        require(Integer.valueOf(12345).equals(PatientLookupValidator.patientIdCandidate("12345")),"Nhận mã PatientId số hợp lệ");
        require(PatientLookupValidator.patientIdCandidate("9999999999")==null,"Không để mã PatientId tràn số gây lỗi request");
        require(DoctorProfileValidator.validate("Bác sĩ Demo","doctor@example.test","DEMO-1000","4","Nội khoa","BS.","NOI-10","150000","Available").isEmpty(),"Hồ sơ bác sĩ hợp lệ phải được chấp nhận");
        require(!DoctorProfileValidator.validate("","bad-email","","0","","","","-1","Other").isEmpty(),"Hồ sơ bác sĩ sai phải bị từ chối");
        require(MedicalServiceValidator.validate("SRV-01","Xét nghiệm máu","1","120000","Mô tả").isEmpty(),"Dịch vụ hợp lệ phải được chấp nhận");
        require(!MedicalServiceValidator.validate("","","0","-1","x".repeat(256)).isEmpty(),"Dịch vụ sai phải bị từ chối");
        require(MedicalServiceValidator.validate("SRV-02","Xét nghiệm","1","10000000000000000","").containsKey("unitPrice"),"DECIMAL(18,2) phải từ chối 17 chữ số phần nguyên");
        require(MedicalServiceValidator.validate("SRV-03","Xét nghiệm","1","1E3","").containsKey("unitPrice"),"Giá dạng số mũ phải bị từ chối rõ ràng");
        require(MedicineValidator.validate("MED-001","Thuốc giảm đau","viên","100000").isEmpty(),"Thông tin thuốc hợp lệ phải được chấp nhận");
        require(MedicineValidator.validate("Thuốc giảm đau","Thuốc giảm đau","viên","100000").containsKey("medicineCode"),"Mã thuốc chỉ nhận mã kỹ thuật không dấu, không khoảng trắng");
        require(MedicineValidator.validate("MED-002","Thuốc giảm đau","viên","10000000000000000").containsKey("unitPrice"),"Giá thuốc phải nằm trong giới hạn DECIMAL(18,2)");
        require(LeaveRequestValidator.validate("2026-11-10","2026-11-12","Nghỉ phép",LocalDate.parse("2026-11-08")).isEmpty(),"Yêu cầu nghỉ hợp lệ phải được chấp nhận");
        require(!LeaveRequestValidator.validate("2026-11-13","2026-11-12","Nghỉ phép",LocalDate.parse("2026-11-08")).isEmpty(),"Khoảng ngày nghỉ ngược phải bị từ chối");
        require(ReportPeriodValidator.validate("2026-11-01","2026-11-08").isEmpty(),"Khoảng báo cáo hợp lệ phải được chấp nhận");
        require(!ReportPeriodValidator.validate("2026-11-09","2026-11-08").isEmpty(),"Ngày kết thúc trước ngày bắt đầu phải bị từ chối");
        require(!ReportPeriodValidator.validate("2010-01-01","2026-11-08").isEmpty(),"Khoảng báo cáo quá 5 năm phải bị từ chối");
        require(RoleAccess.menusFor("Manager").contains("Báo cáo hoạt động"),"Manager phải thấy báo cáo hoạt động");
        require(RoleAccess.menusFor("Doctor").contains("Đơn thuốc"),"Doctor phải thấy chức năng đơn thuốc");
        require(RoleAccess.menusFor("Receptionist").contains("Đăng ký bệnh nhân"),"Receptionist phải thấy đăng ký tại quầy");
        require(RoleAccess.menusFor("MedicalStaff").contains("Nhập kết quả"),"MedicalStaff phải thấy nhập kết quả");
        require(SchedulePublicationValidator.validate(LocalDate.now().plusDays(1).toString(),"AM",LocalDate.now()).isEmpty(),"Lịch bác sĩ tương lai với ca hợp lệ phải được chấp nhận");
        require(!SchedulePublicationValidator.validate(LocalDate.now().minusDays(1).toString(),"PM",LocalDate.now()).isEmpty(),"Không được công bố lịch trong quá khứ");
        require(!SchedulePublicationValidator.validate(LocalDate.now().toString(),"NIGHT",LocalDate.now()).isEmpty(),"Không được tạo ca không nằm trong lịch hỗ trợ");
        require(!SchedulePublicationValidator.validate("not-a-date","AM",LocalDate.now()).isEmpty(),"Ngày công bố sai định dạng phải bị từ chối");
        try { HospitalDaoReadOnlyTest.verify(); require(true,"GET slots chỉ đọc database"); } catch (Exception ex) { throw new AssertionError("GET slots không được ghi database", ex); }
        passedIfThrows(() -> AppointmentSafety.requireNoConsultationConflict(true), "Đơn nghỉ phải bị chặn khi lượt khám đang diễn ra");
        try { AppointmentSafety.requireNoConsultationConflict(false); require(true,"Không có lượt đang khám thì duyệt nghỉ được tiếp tục"); } catch (Exception ex) { throw new AssertionError("Không có lượt đang khám thì không được chặn duyệt nghỉ", ex); }
        passedIfThrows(() -> AppointmentSafety.requireSlotReserved(0), "Đặt cùng slot lần hai phải bị từ chối");
        try { AppointmentSafety.requireSlotReserved(1); require(true,"Slot được đặt thành công đúng một lần"); } catch (Exception ex) { throw new AssertionError("Lần cập nhật slot đầu tiên phải thành công", ex); }
        passedIfThrows(() -> AppointmentSafety.requireServiceOrdersReviewed(1), "Không hoàn tất khi còn chỉ định hoặc kết quả chưa phát hành");
        try { AppointmentSafety.requireServiceOrdersReviewed(0); require(true,"Không có chỉ định đang chờ thì hồ sơ có thể hoàn tất"); } catch (Exception ex) { throw new AssertionError("Không có chỉ định đang chờ thì có thể hoàn tất", ex); }
        System.out.println("PASS: " + checks + " kiểm tra validation, phân quyền, CSRF, AccessCode, GET chỉ đọc và bảo vệ transaction");
    }
    private static void passedIfThrows(CheckedAction action,String message){try{action.run();throw new AssertionError(message);}catch(java.sql.SQLException expected){checks++;}}
    @FunctionalInterface private interface CheckedAction{void run()throws java.sql.SQLException;}
    private static void require(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
}
