package hospital.web;

import hospital.dao.HospitalDao;
import hospital.model.StaffUser;
import hospital.model.PatientBookingResult;
import hospital.model.PatientLookupResult;
import hospital.security.CsrfToken;
import hospital.security.RoleAccess;
import hospital.validation.AppointmentValidator;
import hospital.validation.PatientLookupValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

@WebServlet("/reception/registration")
public class ReceptionRegistrationServlet extends HttpServlet {
    private final HospitalDao dao=new HospitalDao();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        StaffUser user=receptionist(req,res);if(user==null)return;render(req,res,user);
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        StaffUser user=receptionist(req,res);if(user==null)return;
        if(!CsrfToken.isValid(req.getSession(false),req.getParameter("csrfToken"))){res.sendError(403,"Phiên biểu mẫu đã hết hạn.");return;}
        if("lookupPatient".equals(req.getParameter("action"))){
            String query=value(req,"patientLookup");
            if(!PatientLookupValidator.isValidQuery(query)){
                req.setAttribute("lookupError","Nhập số điện thoại, mã giấy tờ hoặc mã hồ sơ hợp lệ (tối đa 20 ký tự).");
            }else{
                try{
                    PatientLookupResult patient=dao.findPatientForReception(query);
                    if(patient==null)req.setAttribute("lookupMessage","Không tìm thấy hồ sơ khớp chính xác. Có thể đăng ký hồ sơ mới sau khi xác minh giấy tờ.");
                    else req.setAttribute("lookupPatient",patient);
                }catch(SQLException ex){
                    getServletContext().log("Không thể tra cứu hồ sơ tại quầy.",ex);
                    req.setAttribute("lookupError",ex.getMessage()!=null&&ex.getMessage().contains("PATIENT_LOOKUP_AMBIGUOUS")
                            ?"Thông tin khớp nhiều hồ sơ. Hãy tra cứu bằng mã hồ sơ hoặc kiểm tra trực tiếp giấy tờ."
                            :"Không thể tra cứu hồ sơ từ database. Chưa có dữ liệu nào được thay đổi.");
                }
            }
            req.setAttribute("lookupQuery",query);
            render(req,res,user);
            return;
        }
        Map<String,String> errors=AppointmentValidator.validate(req.getParameter("fullName"),req.getParameter("dateOfBirth"),req.getParameter("gender"),req.getParameter("phone"),req.getParameter("email"),req.getParameter("reason"),req.getParameter("doctorId"),req.getParameter("appointmentDate"),req.getParameter("slotId"));
        String shift=value(req,"shift");if(!"AM".equals(shift)&&!"PM".equals(shift))errors.put("shift","Chọn ca khám hợp lệ.");
        if(!errors.isEmpty()){req.setAttribute("fieldErrors",errors);render(req,res,user);return;}
        try{PatientBookingResult result=dao.bookAppointment(value(req,"fullName"),LocalDate.parse(req.getParameter("dateOfBirth")),value(req,"gender"),value(req,"phone"),value(req,"email"),value(req,"reason"),Integer.parseInt(req.getParameter("doctorId")),LocalDate.parse(req.getParameter("appointmentDate")),shift,Integer.parseInt(req.getParameter("slotId")),user.id());var session=req.getSession();session.setAttribute("registrationAppointmentOnce",result.appointmentNumber());res.sendRedirect(req.getContextPath()+"/dashboard?page=registration");}
        catch(SQLException ex){getServletContext().log("Receptionist không thể tạo lịch hẹn.",ex);String message=ex.getMessage();String safe=message!=null&&message.contains("PATIENT_IDENTITY_MISMATCH")?"Thông tin tên và ngày sinh không khớp hồ sơ gắn với số điện thoại. Vui lòng kiểm tra giấy tờ trực tiếp.":message!=null&&message.contains("Khung giờ")?"Khung giờ vừa được đặt. Hãy chọn khung giờ khác.":"Không thể đăng ký: hãy kiểm tra thông tin hoặc kết nối database.";req.setAttribute("formError",safe);render(req,res,user);}
    }
    private StaffUser receptionist(HttpServletRequest req,HttpServletResponse res)throws IOException{
        var session=req.getSession(false);StaffUser user=session==null?null:(StaffUser)session.getAttribute("staffUser");
        if(user==null||!"Receptionist".equals(user.role())){res.sendError(403,"Chỉ Receptionist được đăng ký tại quầy.");return null;}return user;
    }
    private void render(HttpServletRequest req,HttpServletResponse res,StaffUser user)throws ServletException,IOException{
        var session=req.getSession();req.setAttribute("page","registration");req.setAttribute("menus",RoleAccess.menusFor(user.role()));req.setAttribute("menuKeys",RoleAccess.menuKeysFor(user.role()));req.setAttribute("csrfToken",CsrfToken.getOrCreate(session));
        Object appointmentNumber=session.getAttribute("registrationAppointmentOnce");boolean saved=appointmentNumber instanceof String;req.setAttribute("saved",saved);
        if(saved){req.setAttribute("savedAppointmentNumber",appointmentNumber);session.removeAttribute("registrationAppointmentOnce");}
        try{req.setAttribute("doctors",dao.findPublicDoctors(""));}catch(SQLException ex){getServletContext().log("Không tải được bác sĩ cho đăng ký tại quầy.",ex);req.setAttribute("loadError","Không thể tải danh sách bác sĩ.");}
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req,res);
    }
    private String value(HttpServletRequest req,String key){String v=req.getParameter(key);return v==null?"":v.trim();}
}
