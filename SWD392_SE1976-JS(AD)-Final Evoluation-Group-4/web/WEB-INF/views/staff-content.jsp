<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,java.math.BigDecimal,jakarta.servlet.http.HttpServletRequest,hospital.model.DoctorProfile,hospital.model.Department" %>
<%!
private String escStaff(String value){if(value==null)return "";return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
private String postedValue(HttpServletRequest req,DoctorProfile doctor,String field,String fallback){try{if("POST".equals(req.getMethod())&&Integer.parseInt(req.getParameter("doctorId"))==doctor.doctorId()){String value=req.getParameter(field);if(value!=null)return value;}}catch(Exception ignored){}return fallback==null?"":fallback;}
private String fieldError(Map<?,?> errors,String key){Object error=errors==null?null:errors.get(key);return error==null?"":"<span class=\"field-error\">"+escStaff(error.toString())+"</span>";}
%>
<%
List<DoctorProfile> doctors=(List<DoctorProfile>)request.getAttribute("doctors");
List<Department> departments=(List<Department>)request.getAttribute("departments");
Map<?,?> errors=(Map<?,?>)request.getAttribute("formErrors");
%>
<p class="muted">Xem và cập nhật thông tin liên hệ, khoa, chuyên môn và trạng thái công khai của bác sĩ.</p>
<%if(request.getAttribute("savedMessage")!=null){%><div class="notice notice-success" role="status"><%=request.getAttribute("savedMessage")%></div><%}%>
<%if(request.getAttribute("formError")!=null){%><div class="notice notice-error" role="alert"><%=escStaff(request.getAttribute("formError").toString())%></div><%}%>
<%if(request.getAttribute("loadError")!=null){%><div class="notice notice-error" role="alert"><%=escStaff(request.getAttribute("loadError").toString())%></div><%}else if(doctors==null||doctors.isEmpty()){%><div class="empty-state"><strong>Chưa có hồ sơ bác sĩ.</strong><p>Hồ sơ bác sĩ sẽ xuất hiện tại đây sau khi được thêm vào database.</p></div><%}else{%>
<div class="doctor-admin-list">
<%for(DoctorProfile doctor:doctors){String fullName=postedValue(request,doctor,"fullName",doctor.fullName());String email=postedValue(request,doctor,"email",doctor.email());String phone=postedValue(request,doctor,"phone",doctor.phone());String departmentId=postedValue(request,doctor,"departmentId",String.valueOf(doctor.departmentId()));String specialization=postedValue(request,doctor,"specialization",doctor.specialization());String title=postedValue(request,doctor,"academicTitle",doctor.academicTitle());String room=postedValue(request,doctor,"roomNumber",doctor.roomNumber());String fee=postedValue(request,doctor,"fee",doctor.consultationFee().toPlainString());String status=postedValue(request,doctor,"status",doctor.status());boolean edited="POST".equals(request.getMethod())&&String.valueOf(doctor.doctorId()).equals(request.getParameter("doctorId"));boolean published=edited?request.getParameter("published")!=null:doctor.published();%>
<article class="doctor-admin-card"><div class="doctor-admin-summary"><div><h3><%=escStaff(doctor.academicTitle())%> <%=escStaff(doctor.fullName())%></h3><p><%=escStaff(doctor.specialization())%> · <%=escStaff(doctor.departmentName())%> · Phòng <%=escStaff(doctor.roomNumber())%></p><p class="muted">Tài khoản <%=escStaff(doctor.username())%> · <%=doctor.published()?"Đang công khai":"Chưa công khai"%></p></div><strong><%=String.format("%,.0f",doctor.consultationFee())%> ₫</strong></div>
<details class="doctor-edit"><summary>Chỉnh sửa hồ sơ</summary><form method="post" action="<%=request.getContextPath()%>/staff" class="staff-edit-form">
<input type="hidden" name="csrfToken" value="<%=escStaff((String)request.getAttribute("csrfToken"))%>"><input type="hidden" name="doctorId" value="<%=doctor.doctorId()%>">
<div class="form-grid"><div class="form-field"><label for="name-<%=doctor.doctorId()%>">Họ tên</label><input id="name-<%=doctor.doctorId()%>" name="fullName" maxlength="200" required value="<%=escStaff(fullName)%>"><%=fieldError(errors,"fullName")%></div>
<div class="form-field"><label for="email-<%=doctor.doctorId()%>">Email</label><input id="email-<%=doctor.doctorId()%>" type="email" name="email" maxlength="100" required value="<%=escStaff(email)%>"><%=fieldError(errors,"email")%></div>
<div class="form-field"><label for="phone-<%=doctor.doctorId()%>">Số điện thoại</label><input id="phone-<%=doctor.doctorId()%>" name="phone" maxlength="20" required value="<%=escStaff(phone)%>"><%=fieldError(errors,"phone")%></div>
<div class="form-field"><label for="dept-<%=doctor.doctorId()%>">Khoa</label><select id="dept-<%=doctor.doctorId()%>" name="departmentId" required><%for(Department department:departments){%><option value="<%=department.id()%>" <%=String.valueOf(department.id()).equals(departmentId)?"selected":""%>><%=escStaff(department.name())%></option><%}%></select><%=fieldError(errors,"departmentId")%></div>
<div class="form-field"><label for="specialty-<%=doctor.doctorId()%>">Chuyên khoa</label><input id="specialty-<%=doctor.doctorId()%>" name="specialization" maxlength="200" required value="<%=escStaff(specialization)%>"><%=fieldError(errors,"specialization")%></div>
<div class="form-field"><label for="title-<%=doctor.doctorId()%>">Chức danh</label><input id="title-<%=doctor.doctorId()%>" name="academicTitle" maxlength="100" value="<%=escStaff(title)%>"><%=fieldError(errors,"academicTitle")%></div>
<div class="form-field"><label for="room-<%=doctor.doctorId()%>">Phòng khám</label><input id="room-<%=doctor.doctorId()%>" name="roomNumber" maxlength="20" required value="<%=escStaff(room)%>"><%=fieldError(errors,"roomNumber")%></div>
<div class="form-field"><label for="fee-<%=doctor.doctorId()%>">Phí khám (₫)</label><input id="fee-<%=doctor.doctorId()%>" type="number" name="fee" min="0" step="0.01" required value="<%=escStaff(fee)%>"><%=fieldError(errors,"fee")%></div>
<div class="form-field"><label for="status-<%=doctor.doctorId()%>">Trạng thái</label><select id="status-<%=doctor.doctorId()%>" name="status"><option value="Available" <%="Available".equals(status)?"selected":""%>>Đang nhận khám</option><option value="Unavailable" <%="Unavailable".equals(status)?"selected":""%>>Tạm ngưng</option></select><%=fieldError(errors,"status")%></div>
<label class="checkbox-field"><input type="checkbox" name="published" <%=published?"checked":""%>> Công khai trên trang chủ</label></div>
<button class="button button-primary" type="submit">Lưu hồ sơ</button></form></details></article>
<%}%></div><%}%>
