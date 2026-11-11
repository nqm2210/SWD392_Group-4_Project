package hospital.web;

import hospital.dao.HospitalDao;
import hospital.model.StaffUser;
import hospital.security.CsrfToken;
import hospital.security.RoleAccess;
import hospital.validation.DoctorProfileValidator;
import hospital.validation.SchedulePublicationValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

@WebServlet(urlPatterns={"/doctor/schedule","/reception/schedules"})
public class ScheduleServlet extends HttpServlet {
    private final HospitalDao dao = new HospitalDao();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        StaffUser user = currentUser(req, res);
        if (user == null) return;
        String path = req.getServletPath();
        if ("/reception/schedules".equals(path)) {
            if (!"Receptionist".equals(user.role())) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Receptionist được tạo lịch cho bác sĩ.");
                return;
            }
            renderReceptionist(req, res, user);
        } else {
            if (!"Doctor".equals(user.role())) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Doctor được xem lịch của mình.");
                return;
            }
            renderDoctor(req, res, user);
        }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        StaffUser user = currentUser(req, res);
        if (user == null) return;
        String path = req.getServletPath();
        if ("/doctor/schedule".equals(path)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Chức năng tạo lịch hiện do Lễ tân phụ trách.");
            return;
        }
        if (!"Receptionist".equals(user.role())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Receptionist được tạo lịch cho bác sĩ.");
            return;
        }
        if (!CsrfToken.isValid(req.getSession(false), req.getParameter("csrfToken"))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên biểu mẫu đã hết hạn. Hãy tải lại trang.");
            return;
        }

        Integer doctorId = DoctorProfileValidator.positiveInteger(req.getParameter("doctorId"));
        String workDate = req.getParameter("workDate"), shift = req.getParameter("shift");
        Map<String, String> errors = SchedulePublicationValidator.validate(workDate, shift, LocalDate.now());
        if (doctorId == null) errors.put("doctorId", "Chọn bác sĩ cần tạo ca khám.");
        if (!errors.isEmpty()) {
            req.setAttribute("formErrors", errors);
            req.setAttribute("postedDoctorId", doctorId == null ? "" : doctorId.toString());
            req.setAttribute("postedWorkDate", workDate);
            req.setAttribute("postedShift", shift);
            renderReceptionist(req, res, user);
            return;
        }

        try {
            dao.publishDoctorScheduleForReceptionist(doctorId, user.id(), LocalDate.parse(workDate), shift);
            SessionFlash.put(req.getSession(), "schedule.saved", "Đã tạo và công bố ca làm việc cho bác sĩ thành công.");
            res.sendRedirect(req.getContextPath() + "/dashboard?page=doctor-schedules&doctorId=" + doctorId);
        } catch (SQLException ex) {
            getServletContext().log("Lễ tân không thể công bố lịch bác sĩ.", ex);
            String message = ex.getMessage();
            String safe = message != null && message.contains("đã tồn tại") ? "Ca này của bác sĩ đã được tạo trước đó."
                    : message != null && message.contains("nghỉ") ? "Không thể tạo lịch trong ngày bác sĩ đã được duyệt nghỉ phép."
                    : "Không thể tạo lịch lúc này. Vui lòng kiểm tra lại thông tin.";
            req.setAttribute("formError", safe);
            req.setAttribute("postedDoctorId", doctorId == null ? "" : doctorId.toString());
            req.setAttribute("postedWorkDate", workDate);
            req.setAttribute("postedShift", shift);
            renderReceptionist(req, res, user);
        }
    }

    private StaffUser currentUser(HttpServletRequest req, HttpServletResponse res) throws IOException {
        var session = req.getSession(false);
        StaffUser user = session == null ? null : (StaffUser) session.getAttribute("staffUser");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/?login=required");
            return null;
        }
        return user;
    }

    private void renderDoctor(HttpServletRequest req, HttpServletResponse res, StaffUser user) throws ServletException, IOException {
        req.setAttribute("menus", RoleAccess.menusFor(user.role()));
        req.setAttribute("menuKeys", RoleAccess.menuKeysFor(user.role()));
        req.setAttribute("page", "schedule");
        req.setAttribute("isReceptionistScheduler", false);
        req.setAttribute("csrfToken", CsrfToken.getOrCreate(req.getSession()));
        req.setAttribute("savedMessage", SessionFlash.consume(req.getSession(false), "schedule.saved"));
        try {
            req.setAttribute("schedules", dao.findSchedulesForDoctor(user.id()));
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải lịch bác sĩ.", ex);
            req.setAttribute("loadError", "Không thể tải lịch làm việc từ database.");
        }
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req, res);
    }

    private void renderReceptionist(HttpServletRequest req, HttpServletResponse res, StaffUser user) throws ServletException, IOException {
        req.setAttribute("menus", RoleAccess.menusFor(user.role()));
        req.setAttribute("menuKeys", RoleAccess.menuKeysFor(user.role()));
        req.setAttribute("page", "doctor-schedules");
        req.setAttribute("isReceptionistScheduler", true);
        req.setAttribute("csrfToken", CsrfToken.getOrCreate(req.getSession()));
        req.setAttribute("savedMessage", SessionFlash.consume(req.getSession(false), "schedule.saved"));
        Integer selectedDoctorId = DoctorProfileValidator.positiveInteger(req.getParameter("doctorId"));
        req.setAttribute("selectedDoctorId", selectedDoctorId);
        try {
            req.setAttribute("doctors", dao.findPublicDoctors(""));
            req.setAttribute("schedules", dao.findSchedulesForReceptionist(selectedDoctorId));
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải danh sách ca làm việc cho lễ tân.", ex);
            req.setAttribute("loadError", "Không thể tải danh sách ca làm việc từ database.");
        }
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req, res);
    }
}
