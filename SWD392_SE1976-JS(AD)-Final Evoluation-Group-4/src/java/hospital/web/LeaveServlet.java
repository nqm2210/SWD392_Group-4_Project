package hospital.web;

import hospital.dao.HospitalDao;
import hospital.model.LeaveRequest;
import hospital.model.StaffUser;
import hospital.security.CsrfToken;
import hospital.security.RoleAccess;
import hospital.validation.LeaveRequestValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

@WebServlet(urlPatterns={"/leave-request","/leave-approvals"})
public class LeaveServlet extends HttpServlet {
    private final HospitalDao dao = new HospitalDao();

    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        StaffUser user=currentUser(req,res);
        if(user==null)return;
        render(req,res,user);
    }

    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        StaffUser user=currentUser(req,res);
        if(user==null)return;
        if(!CsrfToken.isValid(req.getSession(false),req.getParameter("csrfToken"))){
            res.sendError(HttpServletResponse.SC_FORBIDDEN,"Phiên biểu mẫu đã hết hạn. Hãy tải lại trang.");return;
        }
        if("/leave-request".equals(req.getServletPath())){
            if("update".equals(req.getParameter("action")))updateLeave(req,res,user);
            else submitLeave(req,res,user);
        }
        else decideLeave(req,res,user);
    }

    private void submitLeave(HttpServletRequest req,HttpServletResponse res,StaffUser user)throws ServletException,IOException{
        Map<String,String> errors=LeaveRequestValidator.validate(req.getParameter("startDate"),req.getParameter("endDate"),req.getParameter("reason"),LocalDate.now());
        if(!errors.isEmpty()){req.setAttribute("formErrors",errors);render(req,res,user);return;}
        try{
            dao.createLeaveRequest(user.id(),LocalDate.parse(req.getParameter("startDate")),LocalDate.parse(req.getParameter("endDate")),req.getParameter("reason"));
            SessionFlash.put(req.getSession(),"leave.message","Đã gửi yêu cầu nghỉ.");
            res.sendRedirect(req.getContextPath()+"/dashboard?page=leave-request");
        }catch(SQLException ex){
            getServletContext().log("Không thể tạo yêu cầu nghỉ.",ex);
            req.setAttribute("formError","Không thể gửi yêu cầu. Có thể đã tồn tại yêu cầu nghỉ trùng ngày.");render(req,res,user);
        }
    }

    private void updateLeave(HttpServletRequest req,HttpServletResponse res,StaffUser user)throws ServletException,IOException{
        Integer requestId=hospital.validation.DoctorProfileValidator.positiveInteger(req.getParameter("requestId"));
        Map<String,String> errors=LeaveRequestValidator.validate(req.getParameter("startDate"),req.getParameter("endDate"),req.getParameter("reason"),LocalDate.now());
        if(requestId==null){req.setAttribute("formError","Mã đơn nghỉ không hợp lệ.");render(req,res,user);return;}
        if(!errors.isEmpty()){req.setAttribute("formErrors",errors);render(req,res,user);return;}
        try{
            dao.updatePendingLeaveRequest(requestId,user.id(),LocalDate.parse(req.getParameter("startDate")),LocalDate.parse(req.getParameter("endDate")),req.getParameter("reason"));
            SessionFlash.put(req.getSession(),"leave.message","Đã cập nhật yêu cầu nghỉ Pending.");
            res.sendRedirect(req.getContextPath()+"/dashboard?page=leave-request");
        }catch(SQLException ex){
            getServletContext().log("Không thể cập nhật yêu cầu nghỉ.",ex);
            req.setAttribute("editLeaveId",requestId.toString());
            req.setAttribute("formError",ex.getMessage()!=null&&ex.getMessage().contains("trùng")?"Khoảng nghỉ bị trùng với một đơn Pending hoặc Approved khác.":"Không thể sửa đơn. Chỉ đơn Pending của chính bạn mới được cập nhật.");
            render(req,res,user);
        }
    }

    private void decideLeave(HttpServletRequest req,HttpServletResponse res,StaffUser user)throws ServletException,IOException{
        String decision=req.getParameter("decision");
        int requestId=hospital.validation.DoctorProfileValidator.positiveInteger(req.getParameter("requestId"))==null?0:Integer.parseInt(req.getParameter("requestId"));
        String reason=req.getParameter("managerResponseReason");
        if(requestId==0||!("APPROVE".equals(decision)||"REJECT".equals(decision))){
            req.setAttribute("formError","Yêu cầu hoặc quyết định không hợp lệ.");render(req,res,user);return;
        }
        if("REJECT".equals(decision)&&(reason==null||reason.trim().isEmpty())){
            req.setAttribute("formError","Nhập lý do từ chối trước khi gửi.");render(req,res,user);return;
        }
        try{
            int affected=dao.decideLeaveRequest(requestId,user.id(),decision,reason);
            SessionFlash.put(req.getSession(),"leave.message","APPROVE".equals(decision)?"Đã duyệt yêu cầu. "+affected+" lịch hẹn cần bộ phận tiếp nhận xử lý.":"Đã từ chối yêu cầu nghỉ.");
            res.sendRedirect(req.getContextPath()+"/dashboard?page=leave-approvals");
        }catch(SQLException ex){
            getServletContext().log("Không thể xử lý yêu cầu nghỉ.",ex);
            String message=ex.getMessage();req.setAttribute("formError",message!=null&&message.contains("LEAVE_CONFLICT_IN_CONSULTATION")?"Không thể duyệt đơn: bác sĩ đang khám một lượt trong khoảng ngày này. Hãy để lượt khám hoàn tất hoặc xử lý bệnh nhân trước.":"Không thể xử lý yêu cầu. Có thể yêu cầu đã được quyết định bởi người khác.");render(req,res,user);
        }
    }

    private StaffUser currentUser(HttpServletRequest req,HttpServletResponse res)throws IOException{
        jakarta.servlet.http.HttpSession session=req.getSession(false);
        StaffUser user=session==null?null:(StaffUser)session.getAttribute("staffUser");
        boolean managerRoute="/leave-approvals".equals(req.getServletPath());
        boolean allowed=user!=null&&(managerRoute?"Manager".equals(user.role()):java.util.List.of("Doctor","Receptionist").contains(user.role()));
        if(!allowed){res.sendError(HttpServletResponse.SC_FORBIDDEN,"Không có quyền thực hiện chức năng này.");return null;}
        return user;
    }

    private void render(HttpServletRequest req,HttpServletResponse res,StaffUser user)throws ServletException,IOException{
        boolean manager="Manager".equals(user.role());
        String page=manager?"leave-approvals":"leave-request";
        req.setAttribute("menus",RoleAccess.menusFor(user.role()));
        req.setAttribute("menuKeys",RoleAccess.menuKeysFor(user.role()));
        req.setAttribute("page",page);
        req.setAttribute("csrfToken",CsrfToken.getOrCreate(req.getSession()));
        req.setAttribute("leaveMessage",SessionFlash.consume(req.getSession(false),"leave.message"));
        req.setAttribute("editLeaveId",req.getAttribute("editLeaveId")!=null?req.getAttribute("editLeaveId"):req.getParameter("edit"));
        try{
            req.setAttribute("leaveRequests",manager?dao.findLeaveRequestsForManager():dao.findLeaveRequestsForUser(user.id()));
        }catch(SQLException ex){
            getServletContext().log("Không thể tải yêu cầu nghỉ.",ex);
            req.setAttribute("loadError","Không thể tải yêu cầu nghỉ từ database.");
        }
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req,res);
    }
}
