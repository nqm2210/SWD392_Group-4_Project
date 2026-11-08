package hospital.web;
import hospital.dao.HospitalDao;
import hospital.validation.AppointmentValidator;
import hospital.model.PatientBookingResult;
import hospital.security.CsrfToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
@WebServlet("/appointment")
public class AppointmentServlet extends HttpServlet {
    private final HospitalDao dao=new HospitalDao();
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        if(!CsrfToken.isValid(req.getSession(false),req.getParameter("csrfToken"))){
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.setAttribute("bookingError","Phiên biểu mẫu đã hết hạn. Thông tin chưa được gửi; vui lòng tải lại trang và thử lại.");
            renderHome(req,res);
            return;
        }
        Map<String,String> errors=AppointmentValidator.validate(req.getParameter("fullName"),req.getParameter("dateOfBirth"),req.getParameter("gender"),req.getParameter("phone"),req.getParameter("email"),req.getParameter("reason"),req.getParameter("doctorId"),req.getParameter("appointmentDate"),req.getParameter("slotId"));
        String shift=req.getParameter("shift");
        if(!"AM".equals(shift)&&!"PM".equals(shift))errors.put("shift","Vui lòng chọn ca khám hợp lệ.");
        if(errors.isEmpty()){
            try{PatientBookingResult booking=dao.bookAppointment(req.getParameter("fullName").trim(),LocalDate.parse(req.getParameter("dateOfBirth")),req.getParameter("gender"),req.getParameter("phone").trim(),value(req,"email"),req.getParameter("reason").trim(),Integer.parseInt(req.getParameter("doctorId")),LocalDate.parse(req.getParameter("appointmentDate")),shift,Integer.parseInt(req.getParameter("slotId")));HttpSession session=req.getSession(true);session.setAttribute("bookingAppointmentNumber",booking.appointmentNumber());if(booking.oneTimeAccessCode()!=null)session.setAttribute("oneTimePatientAccessCode",booking.oneTimeAccessCode());res.sendRedirect(req.getContextPath()+"/?booking=success");return;}
            catch(SQLException ex){req.setAttribute("bookingError","Không thể hoàn tất đặt lịch: "+safeMessage(ex));}
        }else req.setAttribute("fieldErrors",errors);
        renderHome(req,res);
    }
    private void renderHome(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        try{req.setAttribute("doctors",dao.findPublicDoctors(""));}catch(SQLException ex){req.setAttribute("doctorError","Không thể tải danh sách bác sĩ.");}
        req.setAttribute("csrfToken",CsrfToken.getOrCreate(req.getSession()));
        req.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(req,res);
    }
    private String value(HttpServletRequest req,String key){String value=req.getParameter(key);return value==null?"":value.trim();}
    private String safeMessage(SQLException ex){String message=ex.getMessage();if(message!=null&&message.contains("PATIENT_ALREADY_EXISTS"))return "Số điện thoại đã có hồ sơ. Vui lòng liên hệ lễ tân để xác minh và hỗ trợ đặt lịch.";if(message!=null&&message.contains("Khung giờ"))return "Khung giờ vừa được chọn không còn trống. Vui lòng chọn giờ khác.";return "Không thể hoàn tất đặt lịch. Vui lòng kiểm tra thông tin và thử lại.";}
}
