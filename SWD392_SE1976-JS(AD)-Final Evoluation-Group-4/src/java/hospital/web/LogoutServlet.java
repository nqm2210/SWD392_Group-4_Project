package hospital.web;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws IOException{HttpSession session=req.getSession(false);if(session!=null)session.invalidate();res.sendRedirect(req.getContextPath()+"/");}
}
