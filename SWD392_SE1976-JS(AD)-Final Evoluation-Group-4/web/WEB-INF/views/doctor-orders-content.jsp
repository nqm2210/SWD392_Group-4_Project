<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,hospital.model.MedicineOption,hospital.model.MedicalService,hospital.model.PrescriptionLine" %>
<%! private String escOrder(String value){if(value==null)return "";return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");} %>
<%
String pageKey=(String)request.getAttribute("doctorPageKey"),csrf=(String)request.getAttribute("csrfToken");
hospital.model.DoctorVisit visit=(hospital.model.DoctorVisit)request.getAttribute("orderVisit");
String action=request.getParameter("action");
%>
<div class="form-panel">
<h3><%="prescriptions".equals(pageKey)?"Kê đơn thuốc":"Chỉ định dịch vụ"%> · <%=escOrder(visit.patientName())%></h3>
<%if("prescriptions".equals(pageKey)){
    List<MedicineOption> medicines=(List<MedicineOption>)request.getAttribute("medicines");
    List<PrescriptionLine> lines=(List<PrescriptionLine>)request.getAttribute("prescriptionLines");
    if(lines==null){%><div class="empty-state" role="status"><strong>Chọn một lượt khám đang diễn ra để xem đơn thuốc.</strong></div><%}
    else if(lines.isEmpty()){%><div class="empty-state" role="status"><strong>Chưa có thuốc trong đơn này.</strong><p>Thêm thuốc ở biểu mẫu bên dưới; thay đổi chỉ được ghi khi database xác nhận thành công.</p></div><%}
    else{for(PrescriptionLine line:lines){
        boolean editing="medicine-update".equals(action)&&String.valueOf(line.detailId()).equals(request.getParameter("detailId"));
        String quantity=editing&&request.getParameter("quantity")!=null?request.getParameter("quantity"):String.valueOf(line.quantity());
        String dosage=editing&&request.getParameter("dosage")!=null?request.getParameter("dosage"):line.dosage();
        String instructions=editing&&request.getParameter("doctorInstructions")!=null?request.getParameter("doctorInstructions"):line.doctorInstructions();
%>
<article class="form-panel">
    <p><strong><%=escOrder(line.medicineName())%></strong> · <%=escOrder(line.medicineCode())%> · <%=escOrder(line.unit())%> · Giá tại thời điểm kê: <%=String.format("%,.0f",line.unitPrice())%> ₫</p>
    <form method="post" action="<%=request.getContextPath()%>/doctor/consultation" class="form-grid">
        <input type="hidden" name="csrfToken" value="<%=escOrder(csrf)%>"><input type="hidden" name="appointmentId" value="<%=visit.appointmentId()%>"><input type="hidden" name="view" value="prescriptions"><input type="hidden" name="action" value="medicine-update"><input type="hidden" name="detailId" value="<%=line.detailId()%>">
        <div class="form-field"><label for="quantity-<%=line.detailId()%>">Số lượng *</label><input type="number" id="quantity-<%=line.detailId()%>" name="quantity" min="1" max="10000" required value="<%=escOrder(quantity)%>"></div>
        <div class="form-field"><label for="dosage-<%=line.detailId()%>">Liều dùng *</label><input id="dosage-<%=line.detailId()%>" name="dosage" maxlength="255" required value="<%=escOrder(dosage)%>"></div>
        <div class="form-field full-width"><label for="instructions-<%=line.detailId()%>">Hướng dẫn chung</label><textarea id="instructions-<%=line.detailId()%>" name="doctorInstructions" maxlength="4000"><%=escOrder(instructions)%></textarea></div>
        <div class="form-field full-width"><button class="button button-outline" type="submit">Lưu dòng thuốc</button></div>
    </form>
    <form method="post" action="<%=request.getContextPath()%>/doctor/consultation" onsubmit="return confirm('Xóa thuốc này khỏi đơn đang lập?')">
        <input type="hidden" name="csrfToken" value="<%=escOrder(csrf)%>"><input type="hidden" name="appointmentId" value="<%=visit.appointmentId()%>"><input type="hidden" name="view" value="prescriptions"><input type="hidden" name="action" value="medicine-delete"><input type="hidden" name="detailId" value="<%=line.detailId()%>">
        <button class="button button-outline" type="submit">Xóa khỏi đơn</button>
    </form>
</article>
<%}}%>
<%if(medicines==null){%><div class="empty-state" role="status"><strong>Không tải được danh mục thuốc.</strong><p>Hãy tải lại trang sau; đơn thuốc hiện có không bị thay đổi.</p></div><%}
else if(medicines.isEmpty()){%><div class="empty-state" role="status"><strong>Danh mục chưa có thuốc đang hoạt động.</strong><p>Manager cần thêm hoặc kích hoạt thuốc trong mục “Danh mục thuốc”. Không dùng mục “Dịch vụ và giá” để khai báo thuốc.</p></div><%}
else{
    boolean addForm="medicine".equals(action);
    String postedMedicine=addForm?request.getParameter("catalogId"):null;
%>
<form method="post" action="<%=request.getContextPath()%>/doctor/consultation" class="form-grid">
    <input type="hidden" name="csrfToken" value="<%=escOrder(csrf)%>"><input type="hidden" name="appointmentId" value="<%=visit.appointmentId()%>"><input type="hidden" name="view" value="prescriptions">
    <div class="form-field"><label for="medicine">Thuốc *</label><select id="medicine" name="catalogId" required><option value="">Chọn thuốc</option><%for(MedicineOption medicine:medicines){%><option value="<%=medicine.id()%>" <%=String.valueOf(medicine.id()).equals(postedMedicine)?"selected":""%>><%=escOrder(medicine.name()+" · "+medicine.unit()+" · "+String.format("%,.0f",medicine.unitPrice())+" ₫")%></option><%}%></select></div>
    <div class="form-field"><label for="quantity">Số lượng *</label><input type="number" id="quantity" name="quantity" min="1" max="10000" required value="<%=escOrder(addForm?request.getParameter("quantity"):"")%>"></div>
    <div class="form-field"><label for="dosage">Liều dùng *</label><input id="dosage" name="dosage" maxlength="255" required value="<%=escOrder(addForm?request.getParameter("dosage"):"")%>"></div>
    <div class="form-field full-width"><label for="instructions">Hướng dẫn chung</label><textarea id="instructions" name="doctorInstructions" maxlength="4000"><%=escOrder(addForm?request.getParameter("doctorInstructions"):"")%></textarea></div>
    <div class="form-field full-width"><button class="button button-primary" name="action" value="medicine" type="submit">Thêm thuốc vào đơn</button></div>
</form>
<%}%>
<%}else{
    List<MedicalService> services=(List<MedicalService>)request.getAttribute("medicalServices");
    if(services==null){%><div class="empty-state" role="status"><strong>Không tải được danh mục dịch vụ.</strong></div><%}
    else{boolean hasService=false;for(MedicalService item:services)if(item.active())hasService=true;
        if(!hasService){%><div class="empty-state" role="status"><strong>Chưa có dịch vụ đang hoạt động.</strong><p>Manager cần khai báo dịch vụ trong mục “Dịch vụ và giá” trước khi bác sĩ chỉ định.</p></div><%}
        else{%>
<form method="post" action="<%=request.getContextPath()%>/doctor/consultation" class="form-grid">
    <input type="hidden" name="csrfToken" value="<%=escOrder(csrf)%>"><input type="hidden" name="appointmentId" value="<%=visit.appointmentId()%>"><input type="hidden" name="view" value="lab-orders">
    <div class="form-field"><label for="medical-service">Dịch vụ *</label><select id="medical-service" name="catalogId" required><option value="">Chọn dịch vụ</option><%for(MedicalService item:services)if(item.active()){%><option value="<%=item.id()%>"><%=escOrder(item.name()+" · "+String.format("%,.0f",item.unitPrice())+" ₫")%></option><%}%></select></div>
    <div class="form-field"><label for="priority">Mức ưu tiên</label><select id="priority" name="priority"><option value="NORMAL">Bình thường</option><option value="URGENT">Khẩn</option></select></div>
    <div class="form-field full-width"><label for="clinical-instructions">Hướng dẫn lâm sàng</label><textarea id="clinical-instructions" name="clinicalInstructions" maxlength="255"></textarea></div>
    <div class="form-field full-width"><button class="button button-primary" name="action" value="service" type="submit">Tạo chỉ định</button></div>
</form>
<%}}}%>
</div>
