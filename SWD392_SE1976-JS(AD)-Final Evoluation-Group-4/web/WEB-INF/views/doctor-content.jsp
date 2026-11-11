<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,java.time.LocalDate,hospital.model.DoctorVisit" %>
<%!
private String escDoctor(String value){if(value==null)return "";return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
private String doctorStatus(String value){switch(value==null?"":value){case "BOOKED":return "Đã đặt";case "CHECKED_IN":return "Đã check-in";case "IN_CONSULTATION":return "Đang khám";case "COMPLETED":return "Hoàn tất";default:return value==null?"":value;}}
%>
<%
List<DoctorVisit> visits=(List<DoctorVisit>)request.getAttribute("visits");
String pageKey=(String)request.getAttribute("page"),date=(String)request.getAttribute("date"),selected=(String)request.getAttribute("selectedAppointmentId"),csrf=(String)request.getAttribute("csrfToken");
boolean patientView="patients".equals(pageKey);
String postedDiagnosis=(String)request.getAttribute("postedDiagnosis"),postedNotes=(String)request.getAttribute("postedNotes"),postedPlan=(String)request.getAttribute("postedPlan");
%>
<p class="muted"><%=patientView?"Lịch sử lượt khám theo bác sĩ đang đăng nhập.":"Hàng đợi lấy từ lịch đã check-in; hồ sơ, kê đơn và chỉ định được gắn với từng lượt khám."%></p>
<%if(request.getAttribute("savedMessage")!=null){%><div class="notice notice-success" role="status"><%=escDoctor(request.getAttribute("savedMessage").toString())%></div><%}%>
<%if(request.getAttribute("actionError")!=null){%><div class="notice notice-error" role="alert"><%=escDoctor(request.getAttribute("actionError").toString())%></div><%}%>
<%if(request.getAttribute("dateError")!=null){%><div class="notice notice-error" role="alert"><%=escDoctor(request.getAttribute("dateError").toString())%></div><%}%>
<%if(request.getAttribute("loadError")!=null){%><div class="notice notice-error" role="alert"><%=escDoctor(request.getAttribute("loadError").toString())%></div><%}%>

<form method="get" action="<%=request.getContextPath()%>/doctor/consultation" class="form-panel form-grid">
    <input type="hidden" name="view" value="<%=pageKey%>">
    <div class="form-field"><label for="doctor-date">Ngày</label><input type="date" id="doctor-date" name="date" value="<%=escDoctor(date)%>"></div>
    <div class="form-field"><span>&nbsp;</span><button class="button button-primary" type="submit">Tải danh sách</button></div>
</form>

<%if(visits==null){%><div class="empty-state">Chưa tải được dữ liệu.</div><%}
else if(visits.isEmpty()){%><div class="empty-state"><strong>Không có lượt khám trong ngày này.</strong></div><%}
else{%>
<div class="table-scroll">
<table class="data-table">
<thead>
    <tr>
        <th>Giờ</th>
        <th>Số thứ tự</th>
        <th>Mã lịch</th>
        <th>Bệnh nhân</th>
        <th>Điện thoại</th>
        <th>Trạng thái</th>
        <th>Hồ sơ</th>
        <th>Thao tác</th>
    </tr>
</thead>
<tbody>
<%for(DoctorVisit visit:visits){
    String time=visit.startTime()==null?"":visit.startTime().toString().substring(0,5);
    boolean active=false;
    try{active=visit.appointmentId()==Integer.parseInt(selected==null?"-1":selected);}catch(NumberFormatException ignored){}
%>
<tr>
    <td><%=escDoctor(time)%></td>
    <td><%=visit.queueNumber()==null?"—":visit.queueNumber()%></td>
    <td><%=escDoctor(visit.appointmentNumber())%></td>
    <td><%=escDoctor(visit.patientName())%></td>
    <td><%=escDoctor(visit.phone())%></td>
    <td><%=doctorStatus(visit.status())%></td>
    <td><%=escDoctor(visit.recordStatus()==null?"Chưa lập":visit.recordStatus())%></td>
    <td>
        <%if("IN_PROGRESS".equalsIgnoreCase(visit.recordStatus())&&"prescriptions".equals(pageKey)){%>
            <a class="button button-outline" href="<%=request.getContextPath()%>/doctor/consultation?page=prescriptions&amp;date=<%=date%>&amp;appointmentId=<%=visit.appointmentId()%>">Kê đơn</a>
        <%}else if("IN_PROGRESS".equalsIgnoreCase(visit.recordStatus())&&"lab-orders".equals(pageKey)){%>
            <a class="button button-outline" href="<%=request.getContextPath()%>/doctor/consultation?page=lab-orders&amp;date=<%=date%>&amp;appointmentId=<%=visit.appointmentId()%>">Chỉ định</a>
        <%}else if(!patientView&&("CHECKED_IN".equals(visit.status())||"IN_CONSULTATION".equals(visit.status()))){%>
            <a class="button button-outline" href="<%=request.getContextPath()%>/doctor/consultation?date=<%=date%>&amp;appointmentId=<%=visit.appointmentId()%>">Mở hồ sơ</a>
        <%}else if("COMPLETED".equalsIgnoreCase(visit.recordStatus())||"COMPLETED".equalsIgnoreCase(visit.status())){%>
            <a class="button button-outline" href="<%=request.getContextPath()%>/doctor/consultation?page=<%=escDoctor(pageKey)%>&amp;date=<%=date%>&amp;appointmentId=<%=visit.appointmentId()%>">Xem hồ sơ</a>
        <%}%>
    </td>
</tr>
<%if(active){%>
<tr>
    <td colspan="8">
        <div class="form-panel">
            <h3>Hồ sơ: <%=escDoctor(visit.patientName())%></h3>
            <%if("COMPLETED".equalsIgnoreCase(visit.recordStatus())||"COMPLETED".equalsIgnoreCase(visit.status())){%>
                <div class="read-only-box" style="margin-bottom:1rem;background:#f8fafc;padding:1rem;border-radius:6px;border:1px solid #e2e8f0;">
                    <p><strong>Trạng thái hồ sơ:</strong> <span class="badge badge-success">Đã hoàn tất</span></p>
                    <p><strong>Chẩn đoán:</strong> <%=escDoctor(visit.diagnosis()==null?"(Chưa có thông tin)":visit.diagnosis())%></p>
                    <p><strong>Ghi chú lâm sàng:</strong> <%=escDoctor(visit.clinicalNotes()==null?"(Chưa có thông tin)":visit.clinicalNotes())%></p>
                    <p><strong>Hướng điều trị:</strong> <%=escDoctor(visit.treatmentPlan()==null?"(Chưa có thông tin)":visit.treatmentPlan())%></p>
                </div>
                <%request.setAttribute("orderVisit",visit);request.setAttribute("doctorPageKey",pageKey);%>
                <jsp:include page="doctor-orders-content.jsp" />
            <%}else if(!patientView){%>
                <%if("CHECKED_IN".equals(visit.status())){%>
                    <form method="post" action="<%=request.getContextPath()%>/doctor/consultation">
                        <input type="hidden" name="csrfToken" value="<%=escDoctor(csrf)%>">
                        <input type="hidden" name="appointmentId" value="<%=visit.appointmentId()%>">
                        <input type="hidden" name="date" value="<%=escDoctor(date)%>">
                        <input type="hidden" name="view" value="consultation">
                        <input type="hidden" name="action" value="start">
                        <button class="button button-primary" type="submit">Bắt đầu khám</button>
                    </form>
                <%}else{%>
                    <form method="post" action="<%=request.getContextPath()%>/doctor/consultation" class="form-grid">
                        <input type="hidden" name="csrfToken" value="<%=escDoctor(csrf)%>">
                        <input type="hidden" name="appointmentId" value="<%=visit.appointmentId()%>">
                        <input type="hidden" name="date" value="<%=escDoctor(date)%>">
                        <input type="hidden" name="view" value="consultation">
                        <div class="form-field">
                            <label for="diagnosis-<%=visit.appointmentId()%>">Chẩn đoán *</label>
                            <textarea id="diagnosis-<%=visit.appointmentId()%>" name="diagnosis" maxlength="4000" required><%=escDoctor(postedDiagnosis==null?visit.diagnosis():postedDiagnosis)%></textarea>
                        </div>
                        <div class="form-field">
                            <label for="notes-<%=visit.appointmentId()%>">Ghi chú lâm sàng</label>
                            <textarea id="notes-<%=visit.appointmentId()%>" name="clinicalNotes" maxlength="12000"><%=escDoctor(postedNotes==null?visit.clinicalNotes():postedNotes)%></textarea>
                        </div>
                        <div class="form-field">
                            <label for="plan-<%=visit.appointmentId()%>">Hướng điều trị</label>
                            <textarea id="plan-<%=visit.appointmentId()%>" name="treatmentPlan" maxlength="12000"><%=escDoctor(postedPlan==null?visit.treatmentPlan():postedPlan)%></textarea>
                        </div>
                        <div class="form-field">
                            <span>&nbsp;</span>
                            <div class="button-row">
                                <button class="button button-outline" name="action" value="save" type="submit">Lưu hồ sơ</button>
                                <button class="button button-primary" name="action" value="complete" type="submit">Lưu và hoàn tất</button>
                            </div>
                        </div>
                    </form>
                <%}%>
                <%if("IN_PROGRESS".equalsIgnoreCase(visit.recordStatus())&&List.of("prescriptions","lab-orders").contains(pageKey)){
                    request.setAttribute("orderVisit",visit);request.setAttribute("doctorPageKey",pageKey);%>
                    <jsp:include page="doctor-orders-content.jsp" />
                <%}%>
            <%}%>
        </div>
    </td>
</tr>
<%}%>
<%}%>
</tbody>
</table>
</div>
<%}%>
