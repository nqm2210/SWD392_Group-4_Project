package hospital.web;
import hospital.model.StaffUser;
import hospital.security.RoleAccess;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
@WebFilter(urlPatterns={"/dashboard","/staff","/doctor/schedule","/doctor/consultation","/medical-staff/results","/services","/medicines","/leave-request","/leave-approvals","/reception/registration","/reception/appointments","/reception/check-in","/reception/queue","/reception/payments","/reports/activity","/reports/revenue"})
public class RoleFilter implements Filter {
    @Override public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain)throws IOException,ServletException{
        HttpServletRequest req=(HttpServletRequest)request;HttpServletResponse res=(HttpServletResponse)response;
        HttpSession session=req.getSession(false);
        StaffUser user=session==null?null:(StaffUser)session.getAttribute("staffUser");
        if(user==null){res.sendRedirect(req.getContextPath()+"/?login=required");return;}
        if(!RoleAccess.isSupported(user.role())){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Vai trò chưa được cấu hình.");return;}
        String path=req.getServletPath();
        if("/staff".equals(path)){if(!"Manager".equals(user.role())){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Chỉ Manager được quản lý hồ sơ bác sĩ.");return;}chain.doFilter(request,response);return;}
        if("/doctor/schedule".equals(path)){if(!"Doctor".equals(user.role())){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Chỉ Doctor được xem lịch của mình.");return;}chain.doFilter(request,response);return;}
        if("/doctor/consultation".equals(path)){if(!"Doctor".equals(user.role())){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Chỉ Doctor được xem và cập nhật hồ sơ khám.");return;}chain.doFilter(request,response);return;}
        if("/medical-staff/results".equals(path)){if(!"MedicalStaff".equals(user.role())){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Chỉ MedicalStaff được truy cập chỉ định và kết quả.");return;}chain.doFilter(request,response);return;}
        if("/services".equals(path)){if(!"Manager".equals(user.role())){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Chỉ Manager được quản lý dịch vụ.");return;}chain.doFilter(request,response);return;}
        if("/medicines".equals(path)){if(!"Manager".equals(user.role())){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Chỉ Manager được quản lý danh mục thuốc.");return;}chain.doFilter(request,response);return;}
        if("/leave-request".equals(path)){if(!List.of("Doctor","Receptionist","MedicalStaff").contains(user.role())){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Vai trò này không được gửi yêu cầu nghỉ.");return;}chain.doFilter(request,response);return;}
        if("/leave-approvals".equals(path)){if(!"Manager".equals(user.role())){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Chỉ Manager được duyệt yêu cầu nghỉ.");return;}chain.doFilter(request,response);return;}
        if(path.startsWith("/reports/")){if(!"Manager".equals(user.role())){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Chỉ Manager được xem báo cáo.");return;}chain.doFilter(request,response);return;}
        if(path.startsWith("/reception/")){if(!"Receptionist".equals(user.role())){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Chỉ Receptionist được xử lý lịch hẹn tại quầy.");return;}chain.doFilter(request,response);return;}
        String page=req.getParameter("page");String role=user.role();
        boolean allowed=page==null||page.equals("overview")||page.equals("logout")||switch(role){
            case "Manager"->page.equals("leave-approvals")||page.equals("staff")||page.equals("services")||page.equals("medicines")||page.equals("activity-report")||page.equals("revenue-report")||page.equals("feedback");
            case "Doctor"->page.equals("schedule")||page.equals("leave-request")||page.equals("patients")||page.equals("consultation")||page.equals("prescriptions")||page.equals("lab-orders");
            case "Receptionist"->page.equals("appointments")||page.equals("registration")||page.equals("check-in")||page.equals("queue")||page.equals("leave-request")||page.equals("payments");
            case "MedicalStaff"->page.equals("service-orders")||page.equals("patient-info")||page.equals("perform-service")||page.equals("results")||page.equals("service-history")||page.equals("leave-request");
            default->false;
        };
        if(!allowed){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Bạn không có quyền truy cập chức năng này.");return;}chain.doFilter(request,response);
    }
}
