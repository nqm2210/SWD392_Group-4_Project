package hospital.web;

import hospital.dao.HospitalDao;
import hospital.model.AppointmentSummary;
import hospital.model.StaffUser;
import hospital.security.CsrfToken;
import hospital.security.RoleAccess;
import hospital.validation.ReceptionStatusFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@WebServlet(urlPatterns={"/reception/appointments","/reception/check-in","/reception/queue"})
public class ReceptionServlet extends HttpServlet {
    private final HospitalDao dao=new HospitalDao();

    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        StaffUser user=currentReceptionist(req,res);if(user==null)return;render(req,res,user);
    }

    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        StaffUser user=currentReceptionist(req,res);if(user==null)return;
        if(!CsrfToken.isValid(req.getSession(false),req.getParameter("csrfToken"))){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Phiên biểu mẫu đã hết hạn. Hãy tải lại trang.");return;}
        Integer appointmentId=hospital.validation.DoctorProfileValidator.positiveInteger(req.getParameter("appointmentId"));
        if(appointmentId==null){res.sendError(HttpServletResponse.SC_BAD_REQUEST,"Mã lịch hẹn không hợp lệ.");return;}
        try{
            int queueNumber=dao.checkInAppointment(appointmentId,user.id(),LocalDate.now());
            var session=req.getSession();session.setAttribute("checkInQueueNumberOnce",queueNumber);
            res.sendRedirect(req.getContextPath()+"/dashboard?page=check-in");
        }catch(SQLException ex){
            getServletContext().log("Không thể check-in lịch hẹn.",ex);
            req.setAttribute("actionError","Không thể check-in: lịch hẹn có thể chưa đến ngày, đã được xử lý hoặc đã bị hủy.");render(req,res,user);
        }
    }

    private StaffUser currentReceptionist(HttpServletRequest req,HttpServletResponse res)throws IOException{
        jakarta.servlet.http.HttpSession session=req.getSession(false);StaffUser user=session==null?null:(StaffUser)session.getAttribute("staffUser");
        if(user==null||!"Receptionist".equals(user.role())){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Chỉ Receptionist được xử lý lịch hẹn tại quầy.");return null;}return user;
    }

    private void render(HttpServletRequest req,HttpServletResponse res,StaffUser user)throws ServletException,IOException{
        String path=req.getServletPath();String page="/reception/check-in".equals(path)?"check-in":"/reception/queue".equals(path)?"queue":"appointments";
        LocalDate date=LocalDate.now();
        if("appointments".equals(page)){try{if(req.getParameter("date")!=null)date=LocalDate.parse(req.getParameter("date"));}catch(Exception ignored){req.setAttribute("dateError","Ngày lọc không hợp lệ; đang dùng ngày hôm nay.");}}
        String status="appointments".equals(page)?req.getParameter("status"):"check-in".equals(page)?"BOOKED":"CHECKED_IN,IN_CONSULTATION";
        if("appointments".equals(page)){
            status=ReceptionStatusFilter.normalize(status);
        }
        String keyword="appointments".equals(page)?req.getParameter("keyword"):null;
        req.setAttribute("page",page);req.setAttribute("menus",RoleAccess.menusFor(user.role()));req.setAttribute("menuKeys",RoleAccess.menuKeysFor(user.role()));
        req.setAttribute("csrfToken",CsrfToken.getOrCreate(req.getSession()));req.setAttribute("date",date.toString());req.setAttribute("statusFilter",status==null?"":status);req.setAttribute("keyword",keyword==null?"":keyword);
        var session=req.getSession(false);Object queueNumber=session==null?null:session.getAttribute("checkInQueueNumberOnce");req.setAttribute("checkedIn",queueNumber instanceof Integer);req.setAttribute("queueNumber",queueNumber);if(session!=null)session.removeAttribute("checkInQueueNumberOnce");
        try{List<AppointmentSummary> appointments=dao.findAppointmentsForReceptionist(date,status,keyword);req.setAttribute("appointments",appointments);}
        catch(SQLException ex){getServletContext().log("Không thể tải danh sách lịch hẹn.",ex);req.setAttribute("loadError","Không thể tải lịch hẹn từ database.");}
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req,res);
    }
}
