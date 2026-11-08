package hospital.web;
import hospital.dao.HospitalDao;
import hospital.model.StaffUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final HospitalDao dao=new HospitalDao();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException{
        res.sendRedirect(req.getContextPath()+"/#staff-login");
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        if(!hospital.security.CsrfToken.isValid(req.getSession(false),req.getParameter("csrfToken"))){res.sendRedirect(req.getContextPath()+"/?login=expired");return;}
        String username=req.getParameter("username"),password=req.getParameter("password");
        if(username==null||password==null||username.isBlank()||password.isEmpty()){res.sendRedirect(req.getContextPath()+"/?login=failed");return;}
        try{StaffUser user=dao.authenticate(username.trim(),password);if(user==null){res.sendRedirect(req.getContextPath()+"/?login=failed");return;}if(!hospital.security.RoleAccess.isSupported(user.role())){res.sendError(403,"Tài khoản chưa được gán vai trò được hỗ trợ.");return;}HttpSession session=req.getSession(true);req.changeSessionId();session.setAttribute("staffUser",user);session.setMaxInactiveInterval(30*60);res.sendRedirect(req.getContextPath()+"/dashboard");}
        catch(SQLException ex){getServletContext().log("Không thể xác thực tài khoản nhân viên.",ex);res.sendRedirect(req.getContextPath()+"/?login=unavailable");}
    }
}
