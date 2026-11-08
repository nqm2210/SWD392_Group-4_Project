package hospital.web;

import hospital.dao.HospitalDao;
import hospital.model.StaffUser;
import hospital.security.CsrfToken;
import hospital.security.RoleAccess;
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

@WebServlet("/staff")
public class StaffServlet extends HttpServlet {
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

        int doctorId = DoctorProfileValidator.positiveInteger(req.getParameter("doctorId")) == null
                ? 0 : Integer.parseInt(req.getParameter("doctorId"));
        Integer departmentId = DoctorProfileValidator.positiveInteger(req.getParameter("departmentId"));
        Map<String, String> errors = DoctorProfileValidator.validate(
                req.getParameter("fullName"), req.getParameter("email"), req.getParameter("phone"), req.getParameter("departmentId"),
                req.getParameter("specialization"), req.getParameter("academicTitle"), req.getParameter("roomNumber"),
                req.getParameter("fee"), req.getParameter("status"));
        if (doctorId == 0) errors = new java.util.LinkedHashMap<>(errors);
        if (doctorId == 0) errors.put("doctorId", "Hồ sơ bác sĩ không hợp lệ.");

        if (!errors.isEmpty()) {
            req.setAttribute("formErrors", errors);
            render(req, res, user);
            return;
        }

        try {
            dao.updateDoctorProfile(doctorId, req.getParameter("fullName"), req.getParameter("email"), req.getParameter("phone"),
                    departmentId, req.getParameter("specialization"), req.getParameter("academicTitle"), req.getParameter("roomNumber"),
                    new BigDecimal(req.getParameter("fee")), req.getParameter("status"), req.getParameter("published") != null);
            SessionFlash.put(req.getSession(), "staff.saved", "Đã lưu thay đổi hồ sơ bác sĩ vào database.");
            res.sendRedirect(req.getContextPath() + "/dashboard?page=staff");
        } catch (SQLException | NumberFormatException ex) {
            getServletContext().log("Không thể cập nhật hồ sơ bác sĩ.", ex);
            req.setAttribute("formError", "Không thể lưu hồ sơ. Kiểm tra email có bị trùng và thử lại.");
            render(req, res, user);
        }
    }

    private StaffUser currentManager(HttpServletRequest req, HttpServletResponse res) throws IOException {
        jakarta.servlet.http.HttpSession session = req.getSession(false);
        StaffUser user = session == null ? null : (StaffUser) session.getAttribute("staffUser");
        if (user == null || !"Manager".equals(user.role())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Manager được quản lý hồ sơ bác sĩ.");
            return null;
        }
        return user;
    }

    private void render(HttpServletRequest req, HttpServletResponse res, StaffUser user) throws ServletException, IOException {
        req.setAttribute("menus", RoleAccess.menusFor(user.role()));
        req.setAttribute("menuKeys", RoleAccess.menuKeysFor(user.role()));
        req.setAttribute("page", "staff");
        req.setAttribute("csrfToken", CsrfToken.getOrCreate(req.getSession()));
        req.setAttribute("savedMessage", SessionFlash.consume(req.getSession(false), "staff.saved"));
        try {
            req.setAttribute("doctors", dao.findDoctorProfiles());
            req.setAttribute("departments", dao.findDepartments());
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải hồ sơ bác sĩ cho Manager.", ex);
            req.setAttribute("loadError", "Không thể tải hồ sơ bác sĩ từ database.");
        }
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req, res);
    }
}
