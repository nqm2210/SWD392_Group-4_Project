package hospital.web;

import hospital.dao.HospitalDao;
import hospital.model.StaffUser;
import hospital.security.CsrfToken;
import hospital.security.RoleAccess;
import hospital.validation.DoctorProfileValidator;
import hospital.validation.MedicineValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/medicines")
public class MedicineServlet extends HttpServlet {
    private final HospitalDao dao = new HospitalDao();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        StaffUser manager = manager(req, res);
        if (manager != null) render(req, res, manager);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        StaffUser manager = manager(req, res);
        if (manager == null) return;
        if (!CsrfToken.isValid(req.getSession(false), req.getParameter("csrfToken"))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên biểu mẫu đã hết hạn. Hãy tải lại trang.");
            return;
        }
        Integer id = req.getParameter("medicineId") == null ? null : DoctorProfileValidator.positiveInteger(req.getParameter("medicineId"));
        int medicineId = id == null ? 0 : id;
        Map<String, String> errors = new LinkedHashMap<>(MedicineValidator.validate(req.getParameter("medicineCode"),
                req.getParameter("medicineName"), req.getParameter("unit"), req.getParameter("unitPrice")));
        if (req.getParameter("medicineId") != null && !"0".equals(req.getParameter("medicineId")) && id == null) {
            errors.put("medicineId", "Mã thuốc cần cập nhật không hợp lệ.");
        }
        if (!errors.isEmpty()) {
            req.setAttribute("formErrors", errors);
            render(req, res, manager);
            return;
        }
        try {
            dao.saveMedicine(medicineId, req.getParameter("medicineCode").trim().toUpperCase(java.util.Locale.ROOT),
                    req.getParameter("medicineName").trim(), req.getParameter("unit").trim(),
                    new BigDecimal(req.getParameter("unitPrice").trim()), req.getParameter("active") != null, manager.id());
            SessionFlash.put(req.getSession(), "medicine.saved", "Đã lưu danh mục thuốc vào database.");
            res.sendRedirect(req.getContextPath() + "/dashboard?page=medicines");
        } catch (SQLException ex) {
            getServletContext().log("Không thể lưu danh mục thuốc.", ex);
            if ("MEDICINE_CODE_EXISTS".equals(ex.getMessage())) {
                errors.put("medicineCode", "Mã thuốc đã tồn tại. Hãy nhập mã khác.");
                req.setAttribute("formErrors", errors);
            } else if ("MEDICINE_NOT_FOUND".equals(ex.getMessage())) {
                req.setAttribute("formError", "Không tìm thấy thuốc cần cập nhật. Danh sách chưa được thay đổi.");
            } else {
                req.setAttribute("formError", "Không thể lưu thuốc vào database. Chưa xác nhận thay đổi nào được lưu.");
            }
            render(req, res, manager);
        }
    }

    private StaffUser manager(HttpServletRequest req, HttpServletResponse res) throws IOException {
        var session = req.getSession(false);
        StaffUser user = session == null ? null : (StaffUser) session.getAttribute("staffUser");
        if (user == null || !"Manager".equals(user.role())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Manager được quản lý danh mục thuốc.");
            return null;
        }
        return user;
    }

    private void render(HttpServletRequest req, HttpServletResponse res, StaffUser manager) throws ServletException, IOException {
        req.setAttribute("menus", RoleAccess.menusFor(manager.role()));
        req.setAttribute("menuKeys", RoleAccess.menuKeysFor(manager.role()));
        req.setAttribute("page", "medicines");
        req.setAttribute("csrfToken", CsrfToken.getOrCreate(req.getSession()));
        req.setAttribute("savedMessage", SessionFlash.consume(req.getSession(false), "medicine.saved"));
        try {
            req.setAttribute("medicines", dao.findMedicines(req.getParameter("q")));
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải danh mục thuốc.", ex);
            req.setAttribute("loadError", "Không thể tải danh mục thuốc từ database.");
        }
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req, res);
    }
}
