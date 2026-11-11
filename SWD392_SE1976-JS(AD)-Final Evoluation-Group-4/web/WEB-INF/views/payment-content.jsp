<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,java.math.BigDecimal,hospital.model.BillSummary" %>
<%!
private String escBill(String value){if(value==null)return "";return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
private String billStatus(String paymentStatus,String appointmentStatus){
    if("PAID".equalsIgnoreCase(paymentStatus))return "Đã thanh toán";
    if("UNPAID".equalsIgnoreCase(paymentStatus))return "Đã chốt hóa đơn · Chưa thanh toán";
    if("IN_CONSULTATION".equalsIgnoreCase(appointmentStatus))return "Đang khám · Chờ bác sĩ hoàn tất để chốt bill";
    if("CHECKED_IN".equalsIgnoreCase(appointmentStatus))return "Đã check-in · Chờ bác sĩ khám xong để chốt bill";
    return "Chờ chốt hóa đơn";
}
private String statusBadgeClass(String paymentStatus,String appointmentStatus){
    if("PAID".equalsIgnoreCase(paymentStatus))return "badge badge-success";
    if("UNPAID".equalsIgnoreCase(paymentStatus))return "badge";
    return "badge";
}
%>
<%
List<BillSummary> bills=(List<BillSummary>)request.getAttribute("bills");String csrf=(String)request.getAttribute("csrfToken");
%>
<p class="muted">Sau khi lễ tân Check-in, lượt khám sẽ hiện ngay tại đây với trạng thái <strong>Chờ bác sĩ khám xong để chốt bill</strong> (kèm phí khám dự kiến). Do database quy định 1 lịch hẹn chỉ gắn tối đa 1 hóa đơn (<code>Bills</code>) và 1 lần thu tiền (<code>Payments</code>), hóa đơn chính thức (<code>UNPAID</code>) và nút thu tiền mặt sẽ mở ngay khi bác sĩ hoàn tất lượt khám và chốt toàn bộ chỉ định cận lâm sàng.</p>
<p class="muted">Tổng tiền hóa đơn theo schema <code>Bills</code>: <strong>Phí khám + Dịch vụ cận lâm sàng bác sĩ đã chỉ định</strong>. Dòng <strong>Tiền thuốc theo đơn (tham khảo)</strong> hiển thị tổng giá trị thuốc từ đơn bác sĩ kê để lễ tân đối chiếu (không tự cộng vào <code>Bills.TotalAmount</code> vì bảng <code>Bills</code> chưa có cột tiền thuốc riêng).</p>
<%if(Boolean.TRUE.equals(request.getAttribute("paid"))&&request.getAttribute("paidReceipt")!=null){%><div class="notice notice-success" role="status">Đã ghi nhận thanh toán. Số biên nhận: <strong><%=escBill(request.getAttribute("paidReceipt").toString())%></strong></div><%}%>
<%if(request.getAttribute("paymentError")!=null){%><div class="notice notice-error" role="alert"><%=escBill(request.getAttribute("paymentError").toString())%></div><%}%>
<%if(request.getAttribute("loadError")!=null){%><div class="notice notice-error" role="alert"><%=escBill(request.getAttribute("loadError").toString())%></div><%}else if(bills==null||bills.isEmpty()){%><div class="empty-state"><strong>Chưa có lượt check-in hoặc hóa đơn.</strong><p>Khi lễ tân Check-in cho bệnh nhân hoặc bác sĩ hoàn tất khám, trạng thái hóa đơn sẽ tự động xuất hiện tại đây.</p></div><%}else{%>
<div class="doctor-admin-list"><%for(BillSummary bill:bills){boolean hasOfficialBill=bill.billId()!=null;boolean isUnpaid=hasOfficialBill&&"UNPAID".equalsIgnoreCase(bill.paymentStatus());boolean isPaid=hasOfficialBill&&"PAID".equalsIgnoreCase(bill.paymentStatus());BigDecimal medRef=bill.medicineReferenceAmount()==null?BigDecimal.ZERO:bill.medicineReferenceAmount();%>
<article class="doctor-admin-card"><div class="doctor-admin-summary"><div>
<h3><%if(hasOfficialBill){%>Hóa đơn <%=escBill(bill.billNumber())%><%}else{%>Lịch <%=escBill(bill.appointmentNumber())%><%if(bill.queueNumber()!=null){%> · STT #<%=bill.queueNumber()%><%}%><%}%> · <span class="<%=statusBadgeClass(bill.paymentStatus(),bill.appointmentStatus())%>"><%=escBill(billStatus(bill.paymentStatus(),bill.appointmentStatus()))%></span></h3>
<p>Lịch <%=escBill(bill.appointmentNumber())%><%if(hasOfficialBill&&bill.queueNumber()!=null){%> · STT #<%=bill.queueNumber()%><%}%> · <%=escBill(bill.patientName())%></p>
<p><%=escBill(bill.doctorName())%><%=bill.department()==null?"":" · "+escBill(bill.department())%></p>
<p><%=hasOfficialBill?"Phí khám:":"Phí khám (dự kiến):"%> <%=String.format("%,.0f",bill.consultationFee())%> ₫ · <%=hasOfficialBill?"Dịch vụ:":"Dịch vụ tạm tính:"%> <%=String.format("%,.0f",bill.serviceAmount())%> ₫</p>
<p class="muted">Tiền thuốc theo đơn (tham khảo): <%=String.format("%,.0f",medRef)%> ₫</p>
<p><strong><%=hasOfficialBill?"Tổng tiền hóa đơn:":"Tổng hóa đơn tạm tính:"%> <%=String.format("%,.0f",bill.totalAmount())%> ₫</strong><%if(!isPaid&&medRef.signum()>0){%> <small class="muted">(Nếu tính kèm thuốc tham khảo: <%=String.format("%,.0f",bill.totalAmount().add(medRef))%> ₫)</small><%}%></p>
<%if(!hasOfficialBill){%><p class="muted"><%="IN_CONSULTATION".equalsIgnoreCase(bill.appointmentStatus())?"Bác sĩ đang khám; hóa đơn chính thức sẽ chốt sau khi bác sĩ Hoàn tất khám (có cộng thêm các dịch vụ bác sĩ đã chỉ định).":"Bệnh nhân đã check-in tại quầy; chờ vào khám và hoàn tất hồ sơ để mở thu tiền."%></p><%}%>
<%if(bill.receiptNumber()!=null){%><p class="notice notice-success">Biên nhận: <strong><%=escBill(bill.receiptNumber())%></strong> · Tiền thực thu: <%=bill.amountPaid()==null?"":String.format("%,.0f",bill.amountPaid())+" ₫"%><%if(bill.paidAt()!=null){%> · <%=bill.paidAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))%><%}%></p><%}%>
</div></div>
<%if(isUnpaid){%><form method="post" action="<%=request.getContextPath()%>/reception/payments"><input type="hidden" name="csrfToken" value="<%=escBill(csrf)%>"><input type="hidden" name="billId" value="<%=bill.billId()%>"><button class="button button-primary" type="submit">Thu tiền mặt và lập biên nhận</button></form><%}%>
</article><%}%></div><%}%>
