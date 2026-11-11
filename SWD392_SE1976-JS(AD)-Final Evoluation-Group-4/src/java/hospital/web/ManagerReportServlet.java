package hospital.web;

import hospital.dao.HospitalDao;
import hospital.model.StaffUser;
import hospital.security.RoleAccess;
import hospital.validation.ReportPeriodValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

@WebServlet(urlPatterns={"/reports/activity","/reports/revenue"})
public class ManagerReportServlet extends HttpServlet {
    private final HospitalDao dao=new HospitalDao();

    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        StaffUser user=currentManager(req,res);if(user==null)return;
        LocalDate today=LocalDate.now();String fromValue=req.getParameter("fromDate")==null?today.toString():req.getParameter("fromDate");String toValue=req.getParameter("toDate")==null?today.toString():req.getParameter("toDate");
        Map<String,String> errors=ReportPeriodValidator.validate(fromValue,toValue);if(!errors.isEmpty())req.setAttribute("formErrors",errors);
        String page="/reports/revenue".equals(req.getServletPath())?"revenue-report":"activity-report";
        req.setAttribute("page",page);req.setAttribute("menus",RoleAccess.menusFor(user.role()));req.setAttribute("menuKeys",RoleAccess.menuKeysFor(user.role()));
        req.setAttribute("fromDate",fromValue);req.setAttribute("toDate",toValue);
        if(errors.isEmpty())try{
            LocalDate from=LocalDate.parse(fromValue),to=LocalDate.parse(toValue);
            if("revenue-report".equals(page)){
                req.setAttribute("revenueRows",dao.findRevenueReport(from,to));
                req.setAttribute("detailedRevenueRows",dao.findDetailedRevenueReport(from,to));
            } else {
                req.setAttribute("activityRows",dao.findActivityReport(from,to));
                req.setAttribute("detailedActivityRows",dao.findDetailedActivityReport(from,to));
            }
        }catch(SQLException ex){getServletContext().log("Không thể tải báo cáo.",ex);req.setAttribute("loadError","Không thể đọc dữ liệu báo cáo từ database.");}
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req,res);
    }

    private StaffUser currentManager(HttpServletRequest req,HttpServletResponse res)throws IOException{
        jakarta.servlet.http.HttpSession session=req.getSession(false);StaffUser user=session==null?null:(StaffUser)session.getAttribute("staffUser");
        if(user==null||!"Manager".equals(user.role())){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Chỉ Manager được xem báo cáo.");return null;}return user;
    }
}
