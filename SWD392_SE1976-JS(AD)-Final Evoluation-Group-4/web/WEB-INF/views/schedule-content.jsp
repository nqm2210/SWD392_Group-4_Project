<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,java.time.LocalDate,java.time.format.DateTimeFormatter,hospital.model.ScheduleSummary,hospital.model.Doctor" %>
<%!
private String escSchedule(String value){if(value==null)return "";return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
private String scheduleError(Map<?,?> errors,String key){Object value=errors==null?null:errors.get(key);return value==null?"":"<span class=\"field-error\">"+escSchedule(value.toString())+"</span>";}
%>
<%
boolean isReceptionist = Boolean.TRUE.equals(request.getAttribute("isReceptionistScheduler"));
List<ScheduleSummary> schedules=(List<ScheduleSummary>)request.getAttribute("schedules");
List<Doctor> doctors = (List<Doctor>)request.getAttribute("doctors");
Integer selectedDocId = (Integer)request.getAttribute("selectedDoctorId");
String csrf=(String)request.getAttribute("csrfToken");
Map<?,?> formErrors=(Map<?,?>)request.getAttribute("formErrors");
String workDate=(String)request.getAttribute("postedWorkDate");
String postedShift=(String)request.getAttribute("postedShift");
String postedDoctorId=(String)request.getAttribute("postedDoctorId");
if(workDate==null) workDate=LocalDate.now().toString();
%>

<%if(isReceptionist){%>
<p class="muted">Bộ phận Lễ tân phụ trách tiếp nhận, tạo và công bố ca làm việc cho Bác sĩ. Mỗi ca gồm các khung 30 phút; bệnh nhân chỉ có thể đặt lịch khi ca đã được công bố.</p>
<%}else{%>
<p class="muted">Ca làm việc của bạn được tiếp nhận và công bố bởi bộ phận Lễ tân. Bảng dưới đây thể hiện tình trạng lịch khám và số lượng khung giờ của bạn.</p>
<%}%>

<%if(request.getAttribute("savedMessage")!=null){%><div class="notice notice-success" role="status"><%=escSchedule(request.getAttribute("savedMessage").toString())%></div><%}%>
<%if(request.getAttribute("formError")!=null){%><div class="notice notice-error" role="alert"><%=escSchedule(request.getAttribute("formError").toString())%></div><%}%>

<%if(isReceptionist){%>
<section class="form-panel">
<h3>Tạo ca làm việc mới cho Bác sĩ</h3>
<form method="post" action="<%=request.getContextPath()%>/reception/schedules" class="form-grid">
<input type="hidden" name="csrfToken" value="<%=escSchedule(csrf)%>">
<div class="form-field"><label for="schedule-doctor">Bác sĩ *</label>
<select id="schedule-doctor" name="doctorId" required>
<option value="">Chọn bác sĩ</option>
<%if(doctors!=null)for(Doctor d:doctors){String val=String.valueOf(d.id());boolean sel=val.equals(postedDoctorId);%>
<option value="<%=d.id()%>" <%=sel?"selected":""%>><%=escSchedule(d.title())%> <%=escSchedule(d.fullName())%> — <%=escSchedule(d.specialization())%> (<%=escSchedule(d.department())%>)</option>
<%}%>
</select>
<%=scheduleError(formErrors,"doctorId")%>
</div>
<div class="form-field"><label for="schedule-work-date">Ngày làm việc *</label>
<input id="schedule-work-date" type="date" name="workDate" required min="<%=LocalDate.now()%>" max="<%=LocalDate.now().plusYears(1)%>" value="<%=escSchedule(workDate)%>">
<%=scheduleError(formErrors,"workDate")%>
</div>
<div class="form-field"><label for="schedule-shift">Ca khám *</label>
<select id="schedule-shift" name="shift" required>
<option value="">Chọn ca khám</option>
<option value="AM" <%="AM".equals(postedShift)?"selected":""%>>Ca sáng · 08:00–12:00</option>
<option value="PM" <%="PM".equals(postedShift)?"selected":""%>>Ca chiều · 13:00–17:00</option>
</select>
<%=scheduleError(formErrors,"shift")%>
</div>
<div class="form-field"><span>&nbsp;</span><button class="button button-primary" type="submit">Tạo và công bố ca</button></div>
</form>
</section>

<form method="get" action="<%=request.getContextPath()%>/reception/schedules" class="form-panel form-grid" style="margin-bottom:1rem;">
<div class="form-field"><label for="filter-doctor">Lọc danh sách theo Bác sĩ</label>
<select id="filter-doctor" name="doctorId">
<option value="">Tất cả bác sĩ</option>
<%if(doctors!=null)for(Doctor d:doctors){boolean sel=selectedDocId!=null&&selectedDocId.equals(d.id());%>
<option value="<%=d.id()%>" <%=sel?"selected":""%>><%=escSchedule(d.title())%> <%=escSchedule(d.fullName())%></option>
<%}%>
</select>
</div>
<div class="form-field"><span>&nbsp;</span><button class="button button-outline" type="submit">Lọc ca làm việc</button></div>
</form>
<%}%>

<h3>Danh sách ca làm việc <%=isReceptionist?"đã công bố":"của bạn"%></h3>
<%if(request.getAttribute("loadError")!=null){%><div class="notice notice-error" role="alert"><%=escSchedule(request.getAttribute("loadError").toString())%></div><%}
else if(schedules==null||schedules.isEmpty()){%><div class="empty-state"><strong>Chưa có ca khám nào.</strong><p><%=isReceptionist?"Hãy dùng biểu mẫu phía trên để tạo ca làm việc cho bác sĩ.":"Bộ phận lễ tân sẽ tạo ca khám khi có kế hoạch phân công."%></p></div><%}
else{%>
<div class="table-scroll"><table class="data-table"><thead><tr>
<%if(isReceptionist){%><th>Bác sĩ</th><th>Chuyên khoa</th><%}%>
<th>Ngày</th><th>Ca</th><th>Thời gian</th><th>Khung giờ</th><th>Đã đặt</th><th>Đang mở</th><th>Trạng thái</th></tr></thead><tbody>
<%for(ScheduleSummary item:schedules){int available=Math.max(0,item.slotCount()-item.bookedCount()-item.blockedCount());%>
<tr>
<%if(isReceptionist){%>
<td><strong><%=escSchedule(item.doctorName()==null?"—":item.doctorName())%></strong></td>
<td><%=escSchedule(item.specialization()==null?"—":item.specialization())%></td>
<%}%>
<td><%=item.workDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))%></td>
<td><%=item.shiftName()%></td>
<td><%=item.startTime()==null?"—":item.startTime().toString()%> – <%=item.endTime()==null?"—":item.endTime().toString()%></td>
<td><%=item.slotCount()%></td>
<td><%=item.bookedCount()%></td>
<td><%=available%></td>
<td><%="PUBLISHED".equals(item.status())?"<span class=\"badge badge-success\">Đang mở</span>":"<span class=\"badge\">Đã đóng</span>"%></td>
</tr><%}%>
</tbody></table></div><%}%>
