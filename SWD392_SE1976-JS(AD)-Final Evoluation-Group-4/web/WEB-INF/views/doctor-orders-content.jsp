<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,hospital.model.MedicineOption,hospital.model.MedicalService,hospital.model.PrescriptionLine,hospital.model.ServiceOrderSummary" %>
<%! private String escOrder(String value){if(value==null)return "";return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");} %>
<%
String pageKey=(String)request.getAttribute("doctorPageKey"),csrf=(String)request.getAttribute("csrfToken");
hospital.model.DoctorVisit visit=(hospital.model.DoctorVisit)request.getAttribute("orderVisit");
String action=request.getParameter("action");
boolean isCompleted="COMPLETED".equalsIgnoreCase(visit.recordStatus());
List<ServiceOrderSummary> apptOrders=(List<ServiceOrderSummary>)request.getAttribute("appointmentServiceOrders");
boolean showPrescriptions="prescriptions".equals(pageKey)||(isCompleted&&!"lab-orders".equals(pageKey));
boolean showLabOrders="lab-orders".equals(pageKey)||(isCompleted&&!"prescriptions".equals(pageKey));
%>
<div class="form-panel">
<%if(showPrescriptions){
    List<MedicineOption> medicines=(List<MedicineOption>)request.getAttribute("medicines");
    List<PrescriptionLine> lines=(List<PrescriptionLine>)request.getAttribute("prescriptionLines");
%>
<h3>Đơn thuốc · <%=escOrder(visit.patientName())%></h3>
<%if(lines==null||lines.isEmpty()){%><div class="empty-state" role="status"><strong>Chưa có thuốc trong đơn này.</strong></div><%}
else{%>
<div class="table-scroll"><table class="data-table"><thead><tr><th>Tên thuốc</th><th>Mã</th><th>Đơn vị</th><th>Số lượng</th><th>Liều dùng</th><th>Đơn giá tại thời điểm kê</th><th>Hướng dẫn</th><%if(!isCompleted){%><th>Thao tác</th><%}%></tr></thead><tbody>
<%for(PrescriptionLine line:lines){
    boolean editing="medicine-update".equals(action)&&String.valueOf(line.detailId()).equals(request.getParameter("detailId"));
    String quantity=editing&&request.getParameter("quantity")!=null?request.getParameter("quantity"):String.valueOf(line.quantity());
    String dosage=editing&&request.getParameter("dosage")!=null?request.getParameter("dosage"):line.dosage();
    String instructions=editing&&request.getParameter("doctorInstructions")!=null?request.getParameter("doctorInstructions"):line.doctorInstructions();
%>
<tr>
    <td><strong><%=escOrder(line.medicineName())%></strong></td>
    <td><%=escOrder(line.medicineCode())%></td>
    <td><%=escOrder(line.unit())%></td>
    <td><%=line.quantity()%></td>
    <td><%=escOrder(line.dosage())%></td>
    <td><%=String.format("%,.0f",line.unitPrice())%> ₫</td>
    <td><%=escOrder(line.doctorInstructions()==null?"":line.doctorInstructions())%></td>
    <%if(!isCompleted){%>
    <td>
        <details class="doctor-edit"><summary>Sửa</summary>
        <form method="post" action="<%=request.getContextPath()%>/doctor/consultation" class="form-grid">
            <input type="hidden" name="csrfToken" value="<%=escOrder(csrf)%>"><input type="hidden" name="appointmentId" value="<%=visit.appointmentId()%>"><input type="hidden" name="view" value="prescriptions"><input type="hidden" name="action" value="medicine-update"><input type="hidden" name="detailId" value="<%=line.detailId()%>">
            <div class="form-field"><label for="quantity-<%=line.detailId()%>">Số lượng *</label><input type="number" id="quantity-<%=line.detailId()%>" name="quantity" min="1" max="10000" required value="<%=escOrder(quantity)%>"></div>
            <div class="form-field"><label for="dosage-<%=line.detailId()%>">Liều dùng *</label><input id="dosage-<%=line.detailId()%>" name="dosage" maxlength="255" required value="<%=escOrder(dosage)%>"></div>
            <div class="form-field full-width"><label for="instructions-<%=line.detailId()%>">Hướng dẫn</label><textarea id="instructions-<%=line.detailId()%>" name="doctorInstructions" maxlength="4000"><%=escOrder(instructions)%></textarea></div>
            <div class="form-field full-width"><button class="button button-outline" type="submit">Lưu</button></div>
        </form>
        </details>
        <form method="post" action="<%=request.getContextPath()%>/doctor/consultation" onsubmit="return confirm('Xóa thuốc này khỏi đơn đang lập?')" style="display:inline-block;">
            <input type="hidden" name="csrfToken" value="<%=escOrder(csrf)%>"><input type="hidden" name="appointmentId" value="<%=visit.appointmentId()%>"><input type="hidden" name="view" value="prescriptions"><input type="hidden" name="action" value="medicine-delete"><input type="hidden" name="detailId" value="<%=line.detailId()%>">
            <button class="button button-outline" type="submit">Xóa</button>
        </form>
    </td>
    <%}%>
</tr>
<%}%>
</tbody></table></div>
<%}
if(!isCompleted){
    if(medicines==null){%><div class="empty-state" role="status"><strong>Không tải được danh mục thuốc.</strong></div><%}
    else if(medicines.isEmpty()){%><div class="empty-state" role="status"><strong>Danh mục chưa có thuốc đang hoạt động.</strong><p>Manager cần thêm hoặc kích hoạt thuốc trong mục “Danh mục thuốc”.</p></div><%}
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
<%}}%>
<%}%>
<%if(showLabOrders){%>
<h3>Chỉ định &amp; Kết quả dịch vụ · <%=escOrder(visit.patientName())%></h3>
<%if(apptOrders!=null&&!apptOrders.isEmpty()){%>
<div class="table-scroll"><table class="data-table"><thead><tr><th>Mã chỉ định</th><th>Dịch vụ</th><th>Ưu tiên</th><th>Hướng dẫn lâm sàng</th><th>Trạng thái</th><th>Kết quả</th><th>Kết luận / Chi tiết</th></tr></thead><tbody>
<%for(ServiceOrderSummary o:apptOrders){%>
<tr>
    <td><%=escOrder(o.orderNumber())%></td>
    <td><%=escOrder(o.serviceName())%></td>
    <td><%="URGENT".equals(o.priority())?"Khẩn":"Bình thường"%></td>
    <td><%=escOrder(o.instructions())%></td>
    <td><%=escOrder(o.orderStatus())%></td>
    <td><%=escOrder(o.resultStatus()==null?"Chưa có":o.resultStatus())%></td>
    <td><%if(o.resultFindings()!=null){%><strong><%=escOrder(o.resultFindings())%></strong><%if(o.resultDetails()!=null&&!o.resultDetails().isBlank()){%><br><small class="muted"><%=escOrder(o.resultDetails())%></small><%}%><%}else{%>—<%}%></td>
</tr>
<%}%>
</tbody></table></div>
<%}else{%><div class="empty-state" role="status"><strong>Chưa có chỉ định dịch vụ nào cho lượt khám này.</strong></div><%}
if(!isCompleted){
    List<MedicalService> services=(List<MedicalService>)request.getAttribute("medicalServices");
    if(services==null){%><div class="empty-state" role="status"><strong>Không tải được danh mục dịch vụ.</strong></div><%}
    else{boolean hasService=false;for(MedicalService item:services)if(item.active())hasService=true;
        if(!hasService){%><div class="empty-state" role="status"><strong>Chưa có dịch vụ đang hoạt động.</strong><p>Manager cần khai báo dịch vụ trong mục “Dịch vụ và giá” trước khi bác sĩ chỉ định.</p></div><%}
        else{
            String postedCatalogId=(String)request.getAttribute("postedCatalogId");
            String postedPriority=(String)request.getAttribute("postedPriority");
            String postedInstructions=(String)request.getAttribute("postedInstructions");
%>
<form method="post" action="<%=request.getContextPath()%>/doctor/consultation" class="form-grid">
    <input type="hidden" name="csrfToken" value="<%=escOrder(csrf)%>"><input type="hidden" name="appointmentId" value="<%=visit.appointmentId()%>"><input type="hidden" name="view" value="lab-orders">
    <div class="form-field"><label for="medical-service">Dịch vụ *</label><select id="medical-service" name="catalogId" required><option value="">Chọn dịch vụ</option><%for(MedicalService item:services)if(item.active()){%><option value="<%=item.id()%>" <%=String.valueOf(item.id()).equals(postedCatalogId)?"selected":""%>><%=escOrder(item.name()+" · "+String.format("%,.0f",item.unitPrice())+" ₫")%></option><%}%></select></div>
    <div class="form-field"><label for="priority">Mức ưu tiên</label><select id="priority" name="priority"><option value="NORMAL" <%="NORMAL".equals(postedPriority)?"selected":""%>>Bình thường</option><option value="URGENT" <%="URGENT".equals(postedPriority)?"selected":""%>>Khẩn</option></select></div>
    <div class="form-field full-width"><label for="clinical-instructions">Hướng dẫn lâm sàng</label><textarea id="clinical-instructions" name="clinicalInstructions" maxlength="255"><%=escOrder(postedInstructions==null?"":postedInstructions)%></textarea></div>
    <div class="form-field full-width"><button class="button button-primary" name="action" value="service" type="submit">Tạo chỉ định</button></div>
</form>
<%}}}%>
<%}%>
</div>
