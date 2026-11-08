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

        if ("updateStaff".equals(req.getParameter("action"))) {
            Integer userId = DoctorProfileValidator.positiveInteger(req.getParameter("userId"));
            Map<String, String> errors = DoctorProfileValidator.validateStaffAccount(
                    req.getParameter("username"), req.getParameter("newPassword"), req.getParameter("fullName"),
                    req.getParameter("email"), req.getParameter("phone"), req.getParameter("departmentId"));
            if (userId == null) {
                errors = new java.util.LinkedHashMap<>(errors);
                errors.put("userId", "Tài khoản nhân viên không hợp lệ.");
            }
            if (!errors.isEmpty()) {
                req.setAttribute("staffFormErrors", errors);
                render(req, res, user);
                return;
            }
            try {
                dao.updateStaffAccount(userId, req.getParameter("username"), req.getParameter("newPassword"),
                        req.getParameter("fullName"), req.getParameter("email"), req.getParameter("phone"),
                        DoctorProfileValidator.positiveInteger(req.getParameter("departmentId")),
                        req.getParameter("active") != null, user.id());
                SessionFlash.put(req.getSession(), "staff.saved", "Đã cập nhật tài khoản nhân viên vào database.");
                res.sendRedirect(req.getContextPath() + "/dashboard?page=staff");
            } catch (SQLException ex) {
                getServletContext().log("Không thể cập nhật tài khoản nhân viên.", ex);
                String message = ex.getMessage();
                if ("STAFF_USERNAME_EXISTS".equals(message)) {
                    req.setAttribute("formError", "Tên đăng nhập đã được sử dụng.");
                } else if ("STAFF_EMAIL_EXISTS".equals(message)) {
                    req.setAttribute("formError", "Email đã được sử dụng.");
                } else if ("STAFF_NOT_FOUND".equals(message)) {
                    req.setAttribute("formError", "Không tìm thấy tài khoản nhân viên cần cập nhật.");
                } else {
                    req.setAttribute("formError", "Không thể lưu tài khoản nhân viên. Vui lòng thử lại.");
                }
                render(req, res, user);
            }
            return;
        }

        Integer parsedDoctorId = DoctorProfileValidator.positiveInteger(req.getParameter("doctorId"));
        int doctorId = parsedDoctorId == null ? 0 : parsedDoctorId;
        Integer departmentId = DoctorProfileValidator.positiveInteger(req.getParameter("departmentId"));
        Map<String, String> errors = DoctorProfileValidator.validate(
                req.getParameter("username"), req.getParameter("newPassword"),
                req.getParameter("fullName"), req.getParameter("email"), req.getParameter("phone"), req.getParameter("departmentId"),
                req.getParameter("specialization"), req.getParameter("academicTitle"), req.getParameter("roomNumber"),
                req.getParameter("fee"), req.getParameter("status"));
        if (doctorId == 0) {
            errors = new java.util.LinkedHashMap<>(errors);
            errors.put("doctorId", "Hồ sơ bác sĩ không hợp lệ.");
        }

        if (!errors.isEmpty()) {
            req.setAttribute("formErrors", errors);
            render(req, res, user);
            return;
        }

        try {
            dao.updateDoctorProfile(doctorId, req.getParameter("username"), req.getParameter("newPassword"),
                    req.getParameter("fullName"), req.getParameter("email"), req.getParameter("phone"),
                    departmentId, req.getParameter("specialization"), req.getParameter("academicTitle"), req.getParameter("roomNumber"),
                    new BigDecimal(req.getParameter("fee")), req.getParameter("status"), req.getParameter("published") != null, user.id());
            SessionFlash.put(req.getSession(), "staff.saved", "Đã lưu thay đổi hồ sơ bác sĩ vào database.");
            res.sendRedirect(req.getContextPath() + "/dashboard?page=staff");
        } catch (SQLException | NumberFormatException ex) {
            getServletContext().log("Không thể cập nhật hồ sơ bác sĩ.", ex);
            String message = ex.getMessage();
            if ("STAFF_USERNAME_EXISTS".equals(message)) {
                req.setAttribute("formError", "Tên đăng nhập đã được sử dụng.");
            } else if ("DOCTOR_EMAIL_EXISTS".equals(message)) {
                req.setAttribute("formError", "Email đã được sử dụng.");
            } else {
                req.setAttribute("formError", "Không thể lưu hồ sơ bác sĩ. Vui lòng thử lại.");
            }
            render(req, res, user);
        }
    }

    private StaffUser currentManager(HttpServletRequest req, HttpServletResponse res) throws IOException {
        jakarta.servlet.http.HttpSession session = req.getSession(false);
        StaffUser user = session == null ? null : (StaffUser) session.getAttribute("staffUser");
        if (user == null || !"Manager".equals(user.role())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Manager được quản lý nhân sự.");
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
            req.setAttribute("staffAccounts", dao.findStaffAccounts());
            req.setAttribute("departments", dao.findDepartments());
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải danh sách nhân sự cho Manager.", ex);
            req.setAttribute("loadError", "Không thể tải danh sách nhân sự từ database.");
        }
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req, res);
    }
}
