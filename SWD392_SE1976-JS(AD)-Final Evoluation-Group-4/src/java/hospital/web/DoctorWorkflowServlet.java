package hospital.web;

import hospital.dao.HospitalDao;
import hospital.model.DoctorVisit;
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
import java.time.LocalDate;
import java.util.List;

@WebServlet("/doctor/consultation")
public class DoctorWorkflowServlet extends HttpServlet {
    private final HospitalDao dao=new HospitalDao();

    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        StaffUser user=doctor(req,res);if(user==null)return;render(req,res,user,null);
    }

    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        StaffUser user=doctor(req,res);if(user==null)return;
        if(!CsrfToken.isValid(req.getSession(false),req.getParameter("csrfToken"))){res.sendError(403,"Phiên biểu mẫu đã hết hạn.");return;}
        String action=req.getParameter("action");Integer id=hospital.validation.DoctorProfileValidator.positiveInteger(req.getParameter("appointmentId"));
        if(id==null||action==null||!List.of("start","save","complete","medicine","medicine-update","medicine-delete","service").contains(action)){res.sendError(400,"Yêu cầu không hợp lệ.");return;}
        try{
            if("start".equals(action))dao.startDoctorConsultation(id,user.id());
            else if(List.of("medicine","service","medicine-update","medicine-delete").contains(action)){
                Integer catalogId=null;
                if("medicine".equals(action)||"service".equals(action)){
                    catalogId=hospital.validation.DoctorProfileValidator.positiveInteger(req.getParameter("catalogId"));
                    if(catalogId==null)throw new SQLException("Hãy chọn một mục trong danh mục đang hoạt động.");
                }
                if("medicine".equals(action)){
                    Integer quantity=hospital.validation.DoctorProfileValidator.positiveInteger(req.getParameter("quantity"));String dosage=value(req,"dosage"),instructions=value(req,"doctorInstructions");
                    if(quantity==null||quantity>10000||dosage.isBlank()||dosage.length()>255||instructions.length()>4000)throw new SQLException("Số lượng, liều dùng hoặc hướng dẫn chưa hợp lệ.");
                    dao.addPrescriptionLine(id,user.id(),catalogId,quantity,dosage,instructions);
                }else if("medicine-update".equals(action)){
                    Integer detailId=hospital.validation.DoctorProfileValidator.positiveInteger(req.getParameter("detailId"));
                    Integer quantity=hospital.validation.DoctorProfileValidator.positiveInteger(req.getParameter("quantity"));String dosage=value(req,"dosage"),instructions=value(req,"doctorInstructions");
                    if(detailId==null||quantity==null||quantity>10000||dosage.isBlank()||dosage.length()>255||instructions.length()>4000)throw new SQLException("Số lượng, liều dùng, hướng dẫn hoặc dòng thuốc chưa hợp lệ.");
                    dao.updatePrescriptionLine(id,user.id(),detailId,quantity,dosage,instructions);
                }else if("medicine-delete".equals(action)){
                    Integer detailId=hospital.validation.DoctorProfileValidator.positiveInteger(req.getParameter("detailId"));
                    if(detailId==null)throw new SQLException("Dòng thuốc cần xóa không hợp lệ.");
                    dao.deletePrescriptionLine(id,user.id(),detailId);
                }else{
                    String priority=value(req,"priority"),instructions=value(req,"clinicalInstructions");
                    if(!"NORMAL".equals(priority)&&!"URGENT".equals(priority)||instructions.length()>255)throw new SQLException("Ưu tiên hoặc hướng dẫn chỉ định chưa hợp lệ.");
                    dao.orderMedicalService(id,user.id(),catalogId,priority,instructions);
                }
            }
            else {
                String diagnosis=value(req,"diagnosis"),notes=value(req,"clinicalNotes"),plan=value(req,"treatmentPlan");
                if(diagnosis.isBlank()||diagnosis.length()>4000||notes.length()>12000||plan.length()>12000){
                    req.setAttribute("actionError","Chẩn đoán là bắt buộc (tối đa 4.000 ký tự); ghi chú và hướng điều trị tối đa 12.000 ký tự.");
                    req.setAttribute("postedDiagnosis",diagnosis);req.setAttribute("postedNotes",notes);req.setAttribute("postedPlan",plan);render(req,res,user,id);return;
                }
                dao.saveDoctorConsultation(id,user.id(),diagnosis,notes,plan,"complete".equals(action));
            }
            String targetPage=req.getParameter("view");if(targetPage==null||!List.of("consultation","patients","prescriptions","lab-orders").contains(targetPage))targetPage="consultation";
            SessionFlash.put(req.getSession(),"doctor.saved",successMessage(action));
            res.sendRedirect(req.getContextPath()+"/dashboard?page="+targetPage+("complete".equals(action)?"":"&appointmentId="+id));
        }catch(SQLException ex){
            getServletContext().log("Không thể cập nhật hồ sơ khám.",ex);
            req.setAttribute("actionError",safeError(ex));
            if("service".equals(action)){
                req.setAttribute("postedCatalogId",req.getParameter("catalogId"));
                req.setAttribute("postedPriority",req.getParameter("priority"));
                req.setAttribute("postedInstructions",req.getParameter("clinicalInstructions"));
            }
            render(req,res,user,id);
        }
    }

    private void render(HttpServletRequest req,HttpServletResponse res,StaffUser user,Integer selected)throws ServletException,IOException{
        LocalDate date=LocalDate.now();try{if(req.getParameter("date")!=null)date=LocalDate.parse(req.getParameter("date"));}catch(Exception ex){req.setAttribute("dateError","Ngày lọc không hợp lệ.");}
        String page=req.getParameter("page");if(page==null)page=req.getParameter("view");if(page==null||!List.of("patients","consultation","prescriptions","lab-orders").contains(page))page="consultation";
        req.setAttribute("page",page);
        req.setAttribute("viewPatients","patients".equals(page));
        req.setAttribute("menus",RoleAccess.menusFor(user.role()));req.setAttribute("menuKeys",RoleAccess.menuKeysFor(user.role()));
        req.setAttribute("date",date.toString());req.setAttribute("selectedAppointmentId",selected==null?req.getParameter("appointmentId"):selected.toString());
        req.setAttribute("csrfToken",CsrfToken.getOrCreate(req.getSession()));req.setAttribute("savedMessage",SessionFlash.consume(req.getSession(false),"doctor.saved"));
        try{
            req.setAttribute("visits",dao.findDoctorVisits(user.id(),date));
            Integer appointmentId=selected==null?hospital.validation.DoctorProfileValidator.positiveInteger(req.getParameter("appointmentId")):selected;
            if(appointmentId!=null){
                req.setAttribute("prescriptionLines",dao.findPrescriptionLines(appointmentId,user.id()));
                req.setAttribute("appointmentServiceOrders",dao.findServiceOrdersForAppointment(appointmentId,user.id()));
            }
            if("prescriptions".equals(page)){
                req.setAttribute("medicines",dao.findActiveMedicines());
            }
            if("lab-orders".equals(page))req.setAttribute("medicalServices",dao.findMedicalServices());
        }catch(SQLException ex){getServletContext().log("Không thể tải dữ liệu chuyên môn của bác sĩ.",ex);req.setAttribute("loadError","Không thể tải dữ liệu chuyên môn từ database.");}
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req,res);
    }
    private StaffUser doctor(HttpServletRequest req,HttpServletResponse res)throws IOException{
        var session=req.getSession(false);StaffUser user=session==null?null:(StaffUser)session.getAttribute("staffUser");
        if(user==null||!"Doctor".equals(user.role())){res.sendError(403,"Chỉ Doctor được truy cập hồ sơ khám.");return null;}return user;
    }
    private String successMessage(String action){
        if("complete".equals(action))return "Đã hoàn tất lượt khám và cập nhật hồ sơ.";
        if("medicine".equals(action))return "Đã lưu đơn thuốc vào database.";
        if("medicine-update".equals(action))return "Đã cập nhật dòng thuốc vào database.";
        if("medicine-delete".equals(action))return "Đã xóa dòng thuốc khỏi đơn.";
        if("service".equals(action))return "Đã tạo chỉ định dịch vụ vào database.";
        if("start".equals(action))return "Đã bắt đầu lượt khám.";
        return "Đã lưu hồ sơ khám vào database.";
    }
    private String safeError(SQLException ex){
        String message=ex.getMessage();
        if(message!=null&&message.contains("SERVICE_ORDERS_PENDING"))return "Chưa thể hoàn tất lượt khám: còn chỉ định chưa hoàn tất hoặc kết quả chưa được phát hành.";
        if(message!=null&&List.of("Hãy ","Hồ sơ","Số lượng","Dòng thuốc","Ưu tiên","Thuốc không tồn tại","Chỉ được","Không tìm thấy dòng thuốc").stream().anyMatch(message::startsWith))return message;
        return "Không thể lưu hồ sơ vào database. Không có thay đổi nào được xác nhận.";
    }
    private String value(HttpServletRequest req,String key){String v=req.getParameter(key);return v==null?"":v.trim();}
}
