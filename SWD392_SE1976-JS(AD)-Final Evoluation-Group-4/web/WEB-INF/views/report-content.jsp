<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,java.math.BigDecimal,java.time.format.DateTimeFormatter,hospital.model.ActivityReportRow,hospital.model.RevenueReportRow,hospital.model.ActivityDetailRow,hospital.model.RevenueDetailRow" %>
<%!
private String escReport(String value){if(value==null)return "";return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
private String reportError(Map<?,?> errors,String key){Object error=errors==null?null:errors.get(key);return error==null?"":"<span class=\"field-error\">"+escReport(error.toString())+"</span>";}
private String reportStatus(String status){
    if(status==null) return "—";
    switch(status){
        case "BOOKED": return "Đã đặt";
        case "CHECKED_IN": return "Đã đến / Chờ khám";
        case "IN_CONSULTATION": return "Đang khám";
        case "COMPLETED": return "Hoàn tất";
        case "CANCELLED": return "Đã hủy";
        case "NEEDS_PROCESSING": return "Cần xử lý";
        case "NO_SHOW": return "Không đến";
        default: return status;
    }
}
%>
<%
boolean revenue="revenue-report".equals(request.getAttribute("page"));
Map<?,?> errors=(Map<?,?>)request.getAttribute("formErrors");
List<ActivityReportRow> activityRows=(List<ActivityReportRow>)request.getAttribute("activityRows");
List<RevenueReportRow> revenueRows=(List<RevenueReportRow>)request.getAttribute("revenueRows");
List<ActivityDetailRow> detailedActivityRows=(List<ActivityDetailRow>)request.getAttribute("detailedActivityRows");
List<RevenueDetailRow> detailedRevenueRows=(List<RevenueDetailRow>)request.getAttribute("detailedRevenueRows");
String fromDate=(String)request.getAttribute("fromDate"),toDate=(String)request.getAttribute("toDate");
DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

BigDecimal totalRevenue = BigDecimal.ZERO;
int totalPayments = 0;
if(revenueRows != null){
    for(RevenueReportRow r : revenueRows){
        totalRevenue = totalRevenue.add(r.amount());
        totalPayments += r.paymentCount();
    }
}

int totalAppointments = 0, totalCompleted = 0, totalCheckedIn = 0, totalCancelled = 0;
if(activityRows != null){
    for(ActivityReportRow a : activityRows){
        totalAppointments += a.total();
        totalCompleted += a.completed();
        totalCheckedIn += a.checkedIn() + a.inConsultation();
        totalCancelled += a.cancelled();
    }
}
%>
<p class="muted">Báo cáo tổng hợp và chi tiết từ cơ sở dữ liệu bệnh viện theo khoảng thời gian được chọn.</p>
<%if(request.getAttribute("loadError")!=null){%><div class="notice notice-error" role="alert"><%=escReport(request.getAttribute("loadError").toString())%></div><%}%>

<form method="get" action="<%=request.getContextPath()%>/reports/<%=revenue?"revenue":"activity"%>" class="form-panel form-grid no-print">
    <div class="form-field"><label for="report-from">Từ ngày *</label><input type="date" id="report-from" name="fromDate" required value="<%=escReport(fromDate)%>"><%=reportError(errors,"fromDate")%></div>
    <div class="form-field"><label for="report-to">Đến ngày *</label><input type="date" id="report-to" name="toDate" required value="<%=escReport(toDate)%>"><%=reportError(errors,"toDate")%></div>
    <div class="form-field" style="display:flex;align-items:flex-end;gap:0.5rem;">
        <button class="button button-primary" type="submit">Tạo báo cáo</button>
        <button class="button button-outline" type="button" onclick="window.print()">In / Xuất PDF</button>
    </div>
</form>

<%if(revenue){%>
    <div class="stats-strip" style="margin:1rem 0;">
        <div class="stat-box"><strong><%=String.format("%,.0f", totalRevenue)%> ₫</strong><span>Tổng doanh thu thực thu</span></div>
        <div class="stat-box"><strong><%=totalPayments%></strong><span>Tổng lượt giao dịch thanh toán</span></div>
        <div class="stat-box"><strong><%=fromDate%> – <%=toDate%></strong><span>Kỳ báo cáo</span></div>
    </div>

    <h3>1. Báo cáo tổng hợp theo Khoa và Phương thức thanh toán</h3>
    <%if(revenueRows==null){%><div class="empty-state"><strong>Chưa có báo cáo doanh thu.</strong><p>Chọn khoảng ngày hợp lệ và nhấn "Tạo báo cáo".</p></div><%}
    else if(revenueRows.isEmpty()){%><div class="empty-state"><strong>Không có giao dịch thanh toán nào trong khoảng ngày này.</strong></div><%}
    else{%>
    <div class="table-scroll"><table class="data-table"><thead><tr><th>Khoa</th><th>Phương thức thanh toán</th><th>Số giao dịch</th><th>Tổng tiền đã thu</th></tr></thead><tbody>
    <%for(RevenueReportRow row:revenueRows){%>
    <tr>
        <td><strong><%=escReport(row.department())%></strong></td>
        <td><%=escReport(row.paymentMethod())%></td>
        <td><%=row.paymentCount()%></td>
        <td><strong><%=String.format("%,.0f",row.amount())%> ₫</strong></td>
    </tr>
    <%}%>
    <tr style="background:#f1f5f9;font-weight:bold;">
        <td colspan="2">TỔNG CỘNG</td>
        <td><%=totalPayments%></td>
        <td><%=String.format("%,.0f", totalRevenue)%> ₫</td>
    </tr>
    </tbody></table></div>
    <%}%>

    <h3 style="margin-top:2rem;">2. Báo cáo chi tiết từng giao dịch thanh toán</h3>
    <%if(detailedRevenueRows==null||detailedRevenueRows.isEmpty()){%>
        <div class="empty-state"><strong>Không có chi tiết giao dịch trong khoảng ngày này.</strong></div>
    <%}else{%>
    <div class="table-scroll"><table class="data-table"><thead><tr>
        <th>Thời gian</th><th>Mã biên nhận</th><th>Mã hóa đơn</th><th>Mã lịch</th><th>Bệnh nhân</th><th>Bác sĩ</th><th>Khoa</th><th>Tiền khám</th><th>Tiền DV</th><th>Đã thanh toán</th><th>Phương thức</th>
    </tr></thead><tbody>
    <%for(RevenueDetailRow d:detailedRevenueRows){%>
    <tr>
        <td><%=d.paidAt()==null?"—":d.paidAt().format(dtf)%></td>
        <td><strong><%=escReport(d.receiptNumber())%></strong></td>
        <td><%=escReport(d.billNumber())%></td>
        <td><%=escReport(d.appointmentNumber())%></td>
        <td><%=escReport(d.patientName())%></td>
        <td><%=escReport(d.doctorName())%></td>
        <td><%=escReport(d.department())%></td>
        <td><%=d.consultationFee()==null?"0 ₫":String.format("%,.0f",d.consultationFee())+" ₫"%></td>
        <td><%=d.serviceAmount()==null?"0 ₫":String.format("%,.0f",d.serviceAmount())+" ₫"%></td>
        <td><strong style="color:#0f766e;"><%=String.format("%,.0f",d.amountPaid())%> ₫</strong></td>
        <td><span class="badge"><%=escReport(d.paymentMethod())%></span></td>
    </tr>
    <%}%>
    </tbody></table></div>
    <%}%>

<%}else{%>
    <div class="stats-strip" style="margin:1rem 0;">
        <div class="stat-box"><strong><%=totalAppointments%></strong><span>Tổng số ca hẹn tiếp nhận</span></div>
        <div class="stat-box"><strong><%=totalCompleted%></strong><span>Ca khám hoàn tất</span></div>
        <div class="stat-box"><strong><%=totalCheckedIn%></strong><span>Đã đến / Đang khám</span></div>
        <div class="stat-box"><strong><%=totalCancelled%></strong><span>Ca đã hủy</span></div>
    </div>

    <h3>1. Báo cáo tổng hợp số lượng ca khám theo ngày</h3>
    <%if(activityRows==null){%><div class="empty-state"><strong>Chưa có báo cáo hoạt động.</strong><p>Chọn khoảng ngày hợp lệ và nhấn "Tạo báo cáo".</p></div><%}
    else if(activityRows.isEmpty()){%><div class="empty-state"><strong>Không có lịch hẹn nào trong khoảng ngày này.</strong></div><%}
    else{%>
    <div class="table-scroll"><table class="data-table"><thead><tr>
        <th>Ngày</th><th>Tổng ca</th><th>Đã đặt</th><th>Đã đến</th><th>Đang khám</th><th>Hoàn tất</th><th>Đã hủy</th><th>Cần xử lý</th><th>Không đến</th>
    </tr></thead><tbody>
    <%for(ActivityReportRow row:activityRows){%>
    <tr>
        <td><strong><%=row.date().format(df)%></strong></td>
        <td><strong><%=row.total()%></strong></td>
        <td><%=row.booked()%></td>
        <td><%=row.checkedIn()%></td>
        <td><%=row.inConsultation()%></td>
        <td><span class="badge badge-success"><%=row.completed()%></span></td>
        <td><%=row.cancelled()%></td>
        <td><%=row.needsProcessing()%></td>
        <td><%=row.noShow()%></td>
    </tr>
    <%}%>
    <tr style="background:#f1f5f9;font-weight:bold;">
        <td>TỔNG CỘNG</td>
        <td><%=totalAppointments%></td>
        <td>—</td>
        <td><%=totalCheckedIn%></td>
        <td>—</td>
        <td><%=totalCompleted%></td>
        <td><%=totalCancelled%></td>
        <td>—</td>
        <td>—</td>
    </tr>
    </tbody></table></div>
    <%}%>

    <h3 style="margin-top:2rem;">2. Báo cáo chi tiết từng ca khám bệnh</h3>
    <%if(detailedActivityRows==null||detailedActivityRows.isEmpty()){%>
        <div class="empty-state"><strong>Không có ca khám chi tiết trong khoảng ngày này.</strong></div>
    <%}else{%>
    <div class="table-scroll"><table class="data-table"><thead><tr>
        <th>Ngày khám</th><th>Giờ</th><th>Mã lịch hẹn</th><th>Bệnh nhân</th><th>Số ĐT</th><th>Bác sĩ phụ trách</th><th>Khoa</th><th>Lý do khám</th><th>Trạng thái</th>
    </tr></thead><tbody>
    <%for(ActivityDetailRow d:detailedActivityRows){
        String start = d.startTime() == null ? "—" : d.startTime().toString().substring(0, 5);
    %>
    <tr>
        <td><%=d.appointmentDate().format(df)%></td>
        <td><%=start%></td>
        <td><strong><%=escReport(d.appointmentNumber())%></strong></td>
        <td><%=escReport(d.patientName())%></td>
        <td><%=escReport(d.phone())%></td>
        <td><%=escReport(d.doctorName())%></td>
        <td><%=escReport(d.department())%></td>
        <td><%=escReport(d.reason())%></td>
        <td><span class="badge"><%=reportStatus(d.status())%></span></td>
    </tr>
    <%}%>
    </tbody></table></div>
    <%}%>
<%}%>
