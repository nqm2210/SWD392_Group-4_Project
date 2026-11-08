package hospital.web;

import hospital.dao.HospitalDao;
import hospital.model.MedicalService;
import hospital.model.StaffUser;
import hospital.security.CsrfToken;
import hospital.security.RoleAccess;
import hospital.validation.MedicalServiceValidator;
import hospital.validation.DoctorProfileValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;

@WebServlet("/services")
public class ServiceServlet extends HttpServlet {
    private final HospitalDao dao = new HospitalDao();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        StaffUser user = currentManager(req, res);
        if (user == null) return;
        render(req, res, user);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        StaffUser user = currentManager(req, res);
        if (user == null) return;
        if (!CsrfToken.isValid(req.getSession(false), req.getParameter("csrfToken"))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên biểu mẫu đã hết hạn. Hãy tải lại trang.");
            return;
        }
        int serviceId = DoctorProfileValidator.positiveInteger(req.getParameter("serviceId")) == null
                ? 0 : Integer.parseInt(req.getParameter("serviceId"));
        Map<String, String> errors = MedicalServiceValidator.validate(req.getParameter("serviceCode"),
                req.getParameter("serviceName"), req.getParameter("departmentId"),
                req.getParameter("unitPrice"), req.getParameter("description"));
        if (!errors.isEmpty()) {
            req.setAttribute("formErrors", errors);
            render(req, res, user);
            return;
        }
        try {
            int departmentId = Integer.parseInt(req.getParameter("departmentId"));
            BigDecimal price = new BigDecimal(req.getParameter("unitPrice").trim());
            dao.saveMedicalService(serviceId, req.getParameter("serviceCode").trim().toUpperCase(java.util.Locale.ROOT),
                    req.getParameter("serviceName").trim(), departmentId, price,
                    req.getParameter("description"), req.getParameter("active") != null, user.id());
            SessionFlash.put(req.getSession(), "service.saved", "Đã lưu dịch vụ vào database.");
            res.sendRedirect(req.getContextPath() + "/dashboard?page=services");
        } catch (SQLException | NumberFormatException ex) {
            getServletContext().log("Không thể lưu danh mục dịch vụ.", ex);
            req.setAttribute("formError", "Không thể lưu dịch vụ. Kiểm tra mã dịch vụ có bị trùng và khoa đã chọn còn tồn tại.");
            render(req, res, user);
        }
    }

    private StaffUser currentManager(HttpServletRequest req, HttpServletResponse res) throws IOException {
        jakarta.servlet.http.HttpSession session = req.getSession(false);
        StaffUser user = session == null ? null : (StaffUser) session.getAttribute("staffUser");
        if (user == null || !"Manager".equals(user.role())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Manager được quản lý danh mục dịch vụ.");
            return null;
        }
        return user;
    }

    private void render(HttpServletRequest req, HttpServletResponse res, StaffUser user) throws ServletException, IOException {
        req.setAttribute("menus", RoleAccess.menusFor(user.role()));
        req.setAttribute("menuKeys", RoleAccess.menuKeysFor(user.role()));
        req.setAttribute("page", "services");
        req.setAttribute("csrfToken", CsrfToken.getOrCreate(req.getSession()));
        req.setAttribute("savedMessage", SessionFlash.consume(req.getSession(false), "service.saved"));
        try {
            req.setAttribute("medicalServices", dao.findMedicalServices());
            req.setAttribute("departments", dao.findDepartments());
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải danh mục dịch vụ.", ex);
            req.setAttribute("loadError", "Không thể tải danh mục dịch vụ từ database.");
        }
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req, res);
    }
}
