package hospital.web;

import hospital.dao.HospitalDao;
import hospital.model.ServiceOrderSummary;
import hospital.model.StaffUser;
import hospital.security.CsrfToken;
import hospital.security.RoleAccess;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/medical-staff/results")
public class MedicalStaffServlet extends HttpServlet {
    private final HospitalDao dao=new HospitalDao();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        StaffUser user=medicalStaff(req,res);if(user==null)return;render(req,res,user,null);
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        StaffUser user=medicalStaff(req,res);if(user==null)return;
        if(!CsrfToken.isValid(req.getSession(false),req.getParameter("csrfToken"))){res.sendError(403,"Phiên biểu mẫu đã hết hạn.");return;}
        Integer orderId=hospital.validation.DoctorProfileValidator.positiveInteger(req.getParameter("orderId"));
        String findings=value(req,"findings"),details=value(req,"resultDetails");boolean release="release".equals(req.getParameter("action"));
        if(orderId==null||(!release&& !"save".equals(req.getParameter("action")))||findings.isBlank()||findings.length()>12000||details.length()>12000){
            req.setAttribute("actionError","Chọn chỉ định hợp lệ. Kết quả là bắt buộc; mỗi nội dung tối đa 12.000 ký tự.");req.setAttribute("postedFindings",findings);req.setAttribute("postedDetails",details);render(req,res,user,orderId);return;
        }
        try{dao.saveServiceResult(orderId,user.id(),findings,details,release);SessionFlash.put(req.getSession(),"medicalStaff.saved",release?"Đã phát hành kết quả dịch vụ vào database.":"Đã lưu bản nháp kết quả vào database.");res.sendRedirect(req.getContextPath()+"/dashboard?page=results");}
        catch(SQLException ex){getServletContext().log("Không thể lưu kết quả dịch vụ.",ex);req.setAttribute("actionError",ex.getMessage());req.setAttribute("postedFindings",findings);req.setAttribute("postedDetails",details);render(req,res,user,orderId);}
    }
    private void render(HttpServletRequest req,HttpServletResponse res,StaffUser user,Integer selected)throws ServletException,IOException{
        String page=req.getParameter("page");if(page==null)page=req.getParameter("view");if(page==null)page="results";
        if(!List.of("service-orders","patient-info","perform-service","results","service-history").contains(page))page="results";
        req.setAttribute("page",page);req.setAttribute("menus",RoleAccess.menusFor(user.role()));req.setAttribute("menuKeys",RoleAccess.menuKeysFor(user.role()));
        req.setAttribute("csrfToken",CsrfToken.getOrCreate(req.getSession()));req.setAttribute("savedMessage",SessionFlash.consume(req.getSession(false),"medicalStaff.saved"));
        req.setAttribute("selectedOrderId",selected==null?req.getParameter("orderId"):selected.toString());
        try{req.setAttribute("serviceOrders",dao.findServiceOrders());}catch(SQLException ex){getServletContext().log("Không thể tải chỉ định dịch vụ.",ex);req.setAttribute("loadError","Không thể tải chỉ định từ database.");}
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req,res);
    }
    private StaffUser medicalStaff(HttpServletRequest req,HttpServletResponse res)throws IOException{
        var session=req.getSession(false);StaffUser user=session==null?null:(StaffUser)session.getAttribute("staffUser");
        if(user==null||!"MedicalStaff".equals(user.role())){res.sendError(403,"Chỉ MedicalStaff được xử lý chỉ định dịch vụ.");return null;}return user;
    }
    private String value(HttpServletRequest req,String key){String v=req.getParameter(key);return v==null?"":v.trim();}
}
