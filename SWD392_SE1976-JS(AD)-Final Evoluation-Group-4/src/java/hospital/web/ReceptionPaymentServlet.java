package hospital.web;

import hospital.dao.HospitalDao;
import hospital.model.BillSummary;
import hospital.model.StaffUser;
import hospital.security.CsrfToken;
import hospital.security.RoleAccess;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/reception/payments")
public class ReceptionPaymentServlet extends HttpServlet {
    private final HospitalDao dao = new HospitalDao();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        StaffUser user = receptionist(req, res);
        if (user != null) render(req, res, user);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        StaffUser user = receptionist(req, res);
        if (user == null) return;
        if (!CsrfToken.isValid(req.getSession(false), req.getParameter("csrfToken"))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên biểu mẫu đã hết hạn.");
            return;
        }
        Integer billId = hospital.validation.DoctorProfileValidator.positiveInteger(req.getParameter("billId"));
        if (billId == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã hóa đơn không hợp lệ.");
            return;
        }
        try {
            String receipt = dao.takeCounterPayment(billId, user.id());
            req.getSession().setAttribute("paymentReceiptOnce", receipt);
            res.sendRedirect(req.getContextPath() + "/dashboard?page=payments");
        } catch (SQLException ex) {
            getServletContext().log("Không thể ghi nhận thu tiền tại quầy.", ex);
            req.setAttribute("paymentError", ex.getMessage() != null && (ex.getMessage().contains("không còn") || ex.getMessage().contains("đã có thanh toán"))
                    ? "Hóa đơn không còn chờ thanh toán hoặc đã có giao dịch. Hãy tải lại danh sách."
                    : "Không thể ghi nhận thanh toán trong database; hóa đơn vẫn chưa được xác nhận đã trả.");
            render(req, res, user);
        }
    }

    private StaffUser receptionist(HttpServletRequest req, HttpServletResponse res) throws IOException {
        var session = req.getSession(false);
        StaffUser user = session == null ? null : (StaffUser) session.getAttribute("staffUser");
        if (user == null || !"Receptionist".equals(user.role())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Receptionist được thu tiền tại quầy.");
            return null;
        }
        return user;
    }

    private void render(HttpServletRequest req, HttpServletResponse res, StaffUser user) throws ServletException, IOException {
        req.setAttribute("page", "payments");
        req.setAttribute("menus", RoleAccess.menusFor(user.role()));
        req.setAttribute("menuKeys", RoleAccess.menuKeysFor(user.role()));
        req.setAttribute("csrfToken", CsrfToken.getOrCreate(req.getSession()));
        var session=req.getSession(false);Object receipt=session==null?null:session.getAttribute("paymentReceiptOnce");
        req.setAttribute("paid",receipt instanceof String);req.setAttribute("paidReceipt",receipt);
        if(session!=null)session.removeAttribute("paymentReceiptOnce");
        try {
            List<BillSummary> bills = dao.findBillsForReceptionist();
            req.setAttribute("bills", bills);
        } catch (SQLException ex) {
            getServletContext().log("Không thể tải hóa đơn tại quầy.", ex);
            req.setAttribute("loadError", "Không thể tải danh sách hóa đơn từ database.");
        }
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req, res);
    }
}
