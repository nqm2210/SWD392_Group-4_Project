package hospital.web;
import hospital.model.StaffUser;
import hospital.security.RoleAccess;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        String page=req.getParameter("page");
        if("schedule".equals(page)){req.getRequestDispatcher("/doctor/schedule").forward(req,res);return;}
        if("consultation".equals(page)){req.getRequestDispatcher("/doctor/consultation").forward(req,res);return;}
        if("patients".equals(page)){req.getRequestDispatcher("/doctor/consultation?view=patients").forward(req,res);return;}
        if("prescriptions".equals(page)||"lab-orders".equals(page)){req.getRequestDispatcher("/doctor/consultation?page="+page).forward(req,res);return;}
        if("staff".equals(page)){req.getRequestDispatcher("/staff").forward(req,res);return;}
        if("services".equals(page)){req.getRequestDispatcher("/services").forward(req,res);return;}
        if("medicines".equals(page)){req.getRequestDispatcher("/medicines").forward(req,res);return;}
        if("leave-approvals".equals(page)){req.getRequestDispatcher("/leave-approvals").forward(req,res);return;}
        if("leave-request".equals(page)){req.getRequestDispatcher("/leave-request").forward(req,res);return;}
        if("appointments".equals(page)){req.getRequestDispatcher("/reception/appointments").forward(req,res);return;}
        if("check-in".equals(page)){req.getRequestDispatcher("/reception/check-in").forward(req,res);return;}
        if("queue".equals(page)){req.getRequestDispatcher("/reception/queue").forward(req,res);return;}
        if("payments".equals(page)){req.getRequestDispatcher("/reception/payments").forward(req,res);return;}
        if("registration".equals(page)){req.getRequestDispatcher("/reception/registration").forward(req,res);return;}
        if("activity-report".equals(page)){req.getRequestDispatcher("/reports/activity").forward(req,res);return;}
        if("revenue-report".equals(page)){req.getRequestDispatcher("/reports/revenue").forward(req,res);return;}
        if(page!=null&&List.of("service-orders","patient-info","perform-service","results","service-history").contains(page)){req.getRequestDispatcher("/medical-staff/results?page="+page).forward(req,res);return;}
        StaffUser user=(StaffUser)req.getSession().getAttribute("staffUser");
        req.setAttribute("menus",RoleAccess.menusFor(user.role()));
        req.setAttribute("menuKeys",RoleAccess.menuKeysFor(user.role()));
        req.setAttribute("page",page==null?"overview":page);
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req,res);
    }
}
