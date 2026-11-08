package hospital.web;

import hospital.dao.HospitalDao;
import hospital.model.PatientAppointment;
import hospital.security.CsrfToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/patient")
public class PatientPortalServlet extends HttpServlet {
    private final HospitalDao dao = new HospitalDao();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        render(req, res);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (!CsrfToken.isValid(session, req.getParameter("csrfToken"))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên biểu mẫu đã hết hạn. Hãy tải lại trang.");
            return;
        }
        String action = req.getParameter("action");
        if ("verify".equals(action)) {
            verifyPatient(req, res);
        } else if ("logout".equals(action)) {
            session.removeAttribute("verifiedPatientId");
            res.sendRedirect(req.getContextPath() + "/patient");
        } else if ("updateAppointment".equals(action)) {
            updateAppointment(req, res);
        } else {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thao tác không hợp lệ.");
        }
    }

    private void verifyPatient(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String contact = value(req, "contact");
        String code = value(req, "accessCode");
        boolean email = contact.contains("@");
        if (contact.isBlank() || contact.length() > 100 || code.length() > 50 || !code.matches("[A-Za-z0-9_-]{43}")) {
            req.setAttribute("accessError", "Nhập số điện thoại hoặc email đã đăng ký cùng mã truy cập 43 ký tự.");
            render(req, res);
            return;
        }
        if (email && !contact.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            req.setAttribute("accessError", "Email chưa đúng định dạng.");
            render(req, res);
            return;
        }
        if (!email && !contact.matches("[0-9+() .-]{7,20}")) {
            req.setAttribute("accessError", "Số điện thoại chưa đúng định dạng.");
            render(req, res);
            return;
        }
        try {
            Integer patientId = dao.verifyPatientAccess(contact, email, code);
            if (patientId == null) {
                req.setAttribute("accessError", "Không xác minh được thông tin. Nếu hồ sơ cũ chưa nhận mã truy cập, vui lòng xác minh trực tiếp tại quầy.");
                render(req, res);
                return;
            }
            HttpSession session = req.getSession(true);
            req.changeSessionId();
            session.setAttribute("verifiedPatientId", patientId);
            res.sendRedirect(req.getContextPath() + "/patient");
        } catch (SQLException ex) {
            getServletContext().log("Không thể xác minh hồ sơ bệnh nhân.", ex);
            req.setAttribute("accessError", "Không thể kết nối để xác minh hồ sơ. Chưa hiển thị lịch hẹn; vui lòng thử lại sau.");
            render(req, res);
        }
    }

    private void updateAppointment(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Integer patientId = session == null ? null : (Integer) session.getAttribute("verifiedPatientId");
        if (patientId == null) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Hãy xác minh hồ sơ bệnh nhân trước.");
            return;
        }
        Map<String, String> errors = new LinkedHashMap<>();
        Integer appointmentId = hospital.validation.DoctorProfileValidator.positiveInteger(req.getParameter("appointmentId"));
        String doctorValue = value(req, "doctorId"), slotValue = value(req, "slotId"), dateValue = value(req, "appointmentDate");
        String shift = value(req, "shift");
        boolean scheduleProvided = !doctorValue.isBlank() || !slotValue.isBlank() || !dateValue.isBlank() || !shift.isBlank();
        Integer doctorId = scheduleProvided ? hospital.validation.DoctorProfileValidator.positiveInteger(doctorValue) : null;
        Integer slotId = scheduleProvided ? hospital.validation.DoctorProfileValidator.positiveInteger(slotValue) : null;
        LocalDate date = null;
        if (scheduleProvided) { try { date = LocalDate.parse(dateValue); } catch (RuntimeException ex) { errors.put("appointmentDate", "Chọn ngày hợp lệ."); } }
        String reason = value(req, "reason");
        if (appointmentId == null) errors.put("appointmentId", "Lịch hẹn không hợp lệ.");
        if (scheduleProvided && doctorId == null) errors.put("doctorId", "Chọn bác sĩ.");
        if (scheduleProvided && date != null && date.isBefore(LocalDate.now())) errors.put("appointmentDate", "Không thể đổi sang ngày đã qua.");
        if (scheduleProvided && !"AM".equals(shift) && !"PM".equals(shift)) errors.put("shift", "Chọn ca khám hợp lệ.");
        if (scheduleProvided && slotId == null) errors.put("slotId", "Chọn khung giờ còn trống.");
        if (reason.isBlank() || reason.length() > 255) errors.put("reason", "Lý do khám là bắt buộc, tối đa 255 ký tự.");
        if (!errors.isEmpty()) {
            req.setAttribute("fieldErrors", errors);
            req.setAttribute("updateError", "Thông tin thay đổi chưa hợp lệ; chưa ghi dữ liệu.");
            render(req, res);
            return;
        }
        try {
            dao.updatePatientAppointment(patientId, appointmentId, reason, doctorId, date, shift, slotId);
            session.setAttribute("patientAppointmentUpdatedOnce", Boolean.TRUE);
            res.sendRedirect(req.getContextPath() + "/patient");
        } catch (SQLException ex) {
            getServletContext().log("Không thể cập nhật lịch hẹn tự phục vụ.", ex);
            String message = ex.getMessage();
            req.setAttribute("updateError", safeAppointmentMessage(message));
            render(req, res);
        }
    }

    private void render(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        HttpSession session = req.getSession(true);
        req.setAttribute("csrfToken", CsrfToken.getOrCreate(session));
        req.setAttribute("updated", Boolean.TRUE.equals(session.getAttribute("patientAppointmentUpdatedOnce")));
        session.removeAttribute("patientAppointmentUpdatedOnce");
        Integer patientId = (Integer) session.getAttribute("verifiedPatientId");
        if (patientId != null) {
            try {
                req.setAttribute("patientAppointments", dao.findPatientAppointments(patientId));
                req.setAttribute("appointmentChanges", dao.findPatientAppointmentChanges(patientId));
                req.setAttribute("doctors", dao.findPublicDoctors(""));
            } catch (SQLException ex) {
                getServletContext().log("Không thể tải lịch sử của bệnh nhân đã xác minh.", ex);
                req.setAttribute("loadError", "Không thể tải lịch hẹn từ database. Không có dữ liệu nào được hiển thị từ bộ nhớ tạm.");
            }
        }
        req.getRequestDispatcher("/WEB-INF/views/patient.jsp").forward(req, res);
    }

    private String safeAppointmentMessage(String message) {
        if (message == null) return "Không thể cập nhật lịch. Dữ liệu chưa được lưu.";
        if (message.contains("12 giờ")) return "Chỉ đổi lịch trước giờ khám hơn 12 tiếng theo quy định.";
        if (message.contains("tối đa 2")) return "Lịch này đã được đổi tối đa 2 lần.";
        if (message.contains("trùng")) return "Khung giờ bị trùng với lịch khác của bạn.";
        if (message.contains("không còn") || message.contains("không thuộc") || message.contains("không còn trống")) return "Khung giờ vừa chọn không còn khả dụng. Hãy tải lại danh sách giờ.";
        if (message.contains("BOOKED") || message.contains("tương lai") || message.contains("ngày đã qua")) return message;
        return "Không thể cập nhật lịch hẹn trong database. Dữ liệu chưa được lưu.";
    }

    private String value(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null ? "" : value.trim();
    }
}
