package hospital.web;

import hospital.dao.HospitalDao;
import hospital.model.StaffUser;
import hospital.security.CsrfToken;
import hospital.security.RoleAccess;
import hospital.validation.SchedulePublicationValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

@WebServlet("/doctor/schedule")
public class ScheduleServlet extends HttpServlet {
    private final HospitalDao dao = new HospitalDao();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        StaffUser user=currentDoctor(req,res);
        if(user==null)return;
        render(req,res,user);
    }

    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        StaffUser user=currentDoctor(req,res);
        if(user==null)return;
        if(!CsrfToken.isValid(req.getSession(false),req.getParameter("csrfToken"))){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Phiên biểu mẫu đã hết hạn. Hãy tải lại trang.");return;}
        String workDate=req.getParameter("workDate"),shift=req.getParameter("shift");
        Map<String,String> errors=SchedulePublicationValidator.validate(workDate,shift,LocalDate.now());
        if(!errors.isEmpty()){req.setAttribute("formErrors",errors);req.setAttribute("postedWorkDate",workDate);req.setAttribute("postedShift",shift);render(req,res,user);return;}
        try{dao.publishDoctorSchedule(user.id(),LocalDate.parse(workDate),shift);SessionFlash.put(req.getSession(),"schedule.saved","Đã công bố ca làm việc vào database.");res.sendRedirect(req.getContextPath()+"/dashboard?page=schedule");}
        catch(SQLException ex){getServletContext().log("Không thể công bố lịch bác sĩ.",ex);String message=ex.getMessage();String safe=message!=null&&message.contains("đã tồn tại")?"Ca này đã được tạo trước đó.":message!=null&&message.contains("nghỉ")?"Không thể công bố lịch trong ngày bác sĩ đã được duyệt nghỉ.":"Không thể công bố lịch lúc này. Vui lòng thử lại hoặc liên hệ quản trị hệ thống.";req.setAttribute("formError",safe);req.setAttribute("postedWorkDate",workDate);req.setAttribute("postedShift",shift);render(req,res,user);}
    }

    private StaffUser currentDoctor(HttpServletRequest req,HttpServletResponse res)throws IOException{
        var session=req.getSession(false);StaffUser user=session==null?null:(StaffUser)session.getAttribute("staffUser");
        if(user==null||!"Doctor".equals(user.role())){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Chỉ Doctor được quản lý lịch của mình.");return null;}
        return user;
    }

    private void render(HttpServletRequest req,HttpServletResponse res,StaffUser user)throws ServletException,IOException{
        req.setAttribute("menus", RoleAccess.menusFor(user.role()));
        req.setAttribute("menuKeys", RoleAccess.menuKeysFor(user.role()));
        req.setAttribute("page", "schedule");
        req.setAttribute("csrfToken",CsrfToken.getOrCreate(req.getSession()));
        req.setAttribute("savedMessage",SessionFlash.consume(req.getSession(false),"schedule.saved"));
        try {
            req.setAttribute("schedules", dao.findSchedulesForDoctor(user.id()));
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải lịch bác sĩ.", ex);
            req.setAttribute("loadError", "Không thể tải lịch làm việc từ database.");
        }
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req, res);
    }
}
