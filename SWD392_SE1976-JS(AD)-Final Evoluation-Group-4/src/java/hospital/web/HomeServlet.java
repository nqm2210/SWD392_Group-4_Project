package hospital.web;
import hospital.dao.HospitalDao;
import hospital.model.Doctor;
import hospital.model.TimeSlot;
import hospital.security.CsrfToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
@WebServlet(urlPatterns={"","/slots"})
public class HomeServlet extends HttpServlet {
    private final HospitalDao dao=new HospitalDao();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        req.setAttribute("csrfToken",CsrfToken.getOrCreate(req.getSession()));
        if("/slots".equals(req.getServletPath())){loadSlots(req,res);return;}
        HttpSession bookingSession=req.getSession(false);
        if(bookingSession!=null&&bookingSession.getAttribute("bookingAppointmentNumber") instanceof String appointmentNumber){
            req.setAttribute("bookingAppointmentNumber",appointmentNumber);bookingSession.removeAttribute("bookingAppointmentNumber");
            req.setAttribute("bookingSuccess","Đặt lịch thành công. Mã lịch hẹn được hiển thị bên dưới.");
        }
        try{req.setAttribute("doctors",dao.findPublicDoctors(req.getParameter("q")));}catch(SQLException ex){getServletContext().log("Không thể tải danh sách bác sĩ.",ex);req.setAttribute("doctorError","Không thể tải danh sách bác sĩ. Vui lòng thử lại sau.");}
        req.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(req,res);
    }
    private void loadSlots(HttpServletRequest req,HttpServletResponse res)throws IOException{
        res.setContentType("application/json;charset=UTF-8");
        try{
            int doctor=Integer.parseInt(req.getParameter("doctorId"));LocalDate date=LocalDate.parse(req.getParameter("date"));String shift=req.getParameter("shift");
            List<TimeSlot> slots=dao.findAvailableSlots(doctor,date,shift);
            res.setHeader("X-Slot-Availability",slots.isEmpty()?(dao.hasPublishedSchedule(doctor,date,shift)?"NO_AVAILABLE":"NOT_PUBLISHED"):"AVAILABLE");
            StringBuilder json=new StringBuilder("[");for(TimeSlot slot:slots){if(json.length()>1)json.append(',');json.append("{\"id\":").append(slot.id()).append(",\"label\":\"").append(slot.start()).append(" - ").append(slot.end()).append("\"}");}res.getWriter().write(json.append(']').toString());
        }catch(Exception ex){getServletContext().log("Không thể tải khung giờ đặt khám.",ex);res.setStatus(503);res.setHeader("X-Slot-Availability","UNAVAILABLE");res.getWriter().write("[]");}
    }
}
