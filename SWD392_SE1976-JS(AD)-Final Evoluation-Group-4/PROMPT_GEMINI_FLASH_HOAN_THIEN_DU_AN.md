# Kế hoạch Rà soát Toàn diện & Prompt Chi tiết cho Gemini Flash (Phiên bản v2.0)

**Ngày cập nhật**: 08/11/2026  
**Thư mục làm việc chuẩn xác**:  
`H:\SWD392_SE1976-JS(AD)-Final Evoluation-Group-4\SWD392_Group-4_Project\SWD392_SE1976-JS(AD)-Final Evoluation-Group-4`

---

## Phần 1: Báo cáo Rà soát Độc lập & Phân loại Phát hiện Chính xác

Dựa trên việc đối chiếu thực tế giữa mã nguồn hiện có, schema SQL Server (`HospitalManagementDB` port 51433), tài liệu đặc tả SRS UC-04 / UC-05 và tài liệu thiết kế phần mềm SDS v3:

### 1. Phân loại chính xác các phát hiện kỹ thuật (Khách quan, không phóng đại)

- **Về hồ sơ Bác sĩ (`updateDoctorProfile` - HospitalDao.java:604)**:
  - Mã nguồn hiện hữu đã có `setAutoCommit(false)`, `commit()`, và `rollback()` đầy đủ. Bảng `Users` trong cơ sở dữ liệu đã có ràng buộc duy nhất `UQ__Users__A9D10534D96691FB` trên cột `Email`.
  - Việc bổ sung kiểm tra trùng email trước khi UPDATE, áp dụng mức cô lập `TRANSACTION_SERIALIZABLE` với khóa `WITH (UPDLOCK, HOLDLOCK)`, và ghi `SystemLogs` là **biện pháp nâng cao an toàn phòng vệ (Defensive Programming & Concurrency Hardening)** giúp trả lỗi thân thiện ("Email đã được sử dụng") thay vì lỗi SQL duplicate key thô, không phải sửa chữa một lỗi sập logic nghiêm trọng.
  - Về độ dài chuỗi và kiểu tiền tệ: Cần chuẩn hóa `DoctorProfileValidator.java` để khớp đúng giới hạn schema (`fullName` <= 100, `specialization` <= 100, `academicTitle` <= 50, và `fee` định dạng `DECIMAL(18,2)` chặn số mũ `1E5` và số nguyên vượt 16 chữ số) nhằm ngăn ngừa ngoại lệ cắt ngắn dữ liệu (`String or binary data would be truncated`).

- **Về việc hiển thị chuỗi trong JSP (XSS Defense-in-Depth)**:
  - Các biến thông báo `savedMessage`, `loadError`, `actionError` bắt nguồn từ code phía máy chủ (hardcoded string hoặc nội dung ngoại lệ đã qua lọc).
  - Việc bổ sung hàm thoát ký tự HTML (`esc...`) là **tăng cường bảo vệ theo chiều sâu (Defense-in-Depth)** để triệt tiêu mọi nguy cơ chèn mã tiềm ẩn nếu sau này có dữ liệu người dùng phản hồi qua query parameter hay exception, không xem đây là lỗ hổng XSS đã khai thác được trong thực tế.

- **Về lỗi Crash thực tế (`ClassCastException`)**:
  - Tại `web/WEB-INF/views/reception-content.jsp:14`: Ép kiểu `(String)request.getAttribute("queueNumber")` gây ra `ClassCastException` trực tiếp khi `ReceptionServlet` đặt thuộc tính là `Integer`. Đây là bug chức năng bắt buộc sửa bằng `String.valueOf(...)`.

### 2. Các điểm chưa thống nhất về Nghiệp vụ & Kiến trúc trong Tài liệu

- **Quy tắc Hủy Lịch hẹn (UC-04 vs UC-05)**:
  - **Quy tắc thời gian (BR-01)**: Trong SRS UC-04 (Cancel Appointment) nêu rõ: *Bệnh nhân chỉ được tự hủy trực tuyến trước giờ khám ít nhất 2 giờ* (`scheduled start time - 2 hours`). Nếu dưới 2 giờ (Alternative Flow 3a), hệ thống phải từ chối và hướng dẫn liên hệ hotline. Trong khi đó, tại UC-05 lại mô tả điều kiện chung: *"The patient has an appointment in an incomplete status (Before examination time)"*.
  - **Chính sách phạt hủy lịch**: SRS có quy định: *"Accounts that abruptly cancel appointments more than 3 times/month will be locked out of the online booking feature for 30 days."* Tuy nhiên, hệ thống hiện tại chưa có bảng/cột lưu vết số lần vi phạm hàng tháng và cơ chế khóa đặt lịch của tài khoản.
  - **Kết luận kiến trúc**: Giữ nguyên tắc an toàn: luồng bệnh nhân tự hủy trên web áp dụng điều kiện trước giờ khám tối thiểu 2 giờ; ghi nhận các chính sách chưa thống nhất vào tài liệu và chưa tự ý đặt quy tắc ngoài đặc tả.

- **Lịch sử Thay đổi của Bệnh nhân (`findPatientAppointmentChanges` - HospitalDao.java:510)**:
  - Hiện tại câu lệnh SQL đang giới hạn: `IN('PATIENT_UPDATE_REASON','PATIENT_RESCHEDULE')`.
  - Khi bổ sung hành động hủy lịch (`PATIENT_CANCEL`, `RECEPTIONIST_CANCEL`), cần mở rộng điều kiện `IN(...)` của SQL và cập nhật nhãn hiển thị tại `web/WEB-INF/views/patient.jsp` để bệnh nhân thấy rõ vết hủy lịch cùng lý do.

- **Khả năng Sinh Lịch định kỳ (Periodic Schedule) & Tiền thuốc trong Hóa đơn**:
  - Bảng `Schedules` (`WorkDate`, `ShiftName`, `MaxPatients`) hoàn toàn có thể lưu trữ các ca khám được sinh ra từ mẫu định kỳ (thứ 2 đến thứ 6 hàng tuần). Sự thiếu vắng chức năng này là do **chưa phát triển module sinh lịch tự động và quy tắc phát hiện trùng lịch**, không phải do cấu trúc cơ sở dữ liệu ngăn cản.
  - Tiền thuốc: Trong SRS có nhắc đến chi phí thuốc, nhưng trong SDS v3 và schema bảng `Bills` chỉ thiết kế `ConsultationFee`, `TotalServiceAmount`, `TotalAmount`. Đây là sự chưa đồng nhất giữa SRS và SDS v3 (thuốc thanh toán tại quầy thuốc độc lập hay tính gộp vào bill điều trị), cần quyết định nghiệp vụ trước khi phát triển mở rộng.

---

## Phần 2: Kế hoạch Triển khai Chia làm 3 Đợt Độc lập

- **Đợt 1**: Sửa Lỗi Crash, Chuẩn hóa Validation, An toàn UI & Hiển thị Form.
- **Đợt 2**: Hoàn thiện Luồng Xem Hồ sơ, Đơn thuốc & Chỉ định Cận lâm sàng.
- **Đợt 3**: Quy tắc Hủy Lịch hẹn, Lịch sử Bệnh nhân & Kiểm soát Concurrency.

---

## Phần 3: Prompt Master Chi tiết cho Gemini 3.8 Flash

```markdown
Bạn là kỹ sư Java Web chịu trách nhiệm thực thi mã nguồn trực tiếp trên dự án tại:
H:\SWD392_SE1976-JS(AD)-Final Evoluation-Group-4\SWD392_Group-4_Project\SWD392_SE1976-JS(AD)-Final Evoluation-Group-4

### Ràng buộc Bắt buộc:
1. Tuyệt đối KHÔNG chạy lệnh DDL/migration, KHÔNG thay đổi cấu trúc bảng SQL Server, KHÔNG chèn dữ liệu mẫu giả vào DB.
2. Mọi JSP phải tương thích Jasper source/target 1.8 (KHÔNG dùng cú pháp Java 14+ switch expression `->` trong scriptlet).
3. Đảm bảo toàn bộ lệnh Ant chạy từ thư mục dự án: `ant clean test war jsp-check deploy`.

---

### ĐỢT 1: Sửa Lỗi Crash, Chuẩn hóa Validation, An toàn UI & Hiển thị Form

#### 1.1. Sửa lỗi ClassCastException tại reception-content.jsp
- Tệp: `web/WEB-INF/views/reception-content.jsp`
- Vị trí: Dòng 14.
- Thao tác: Thay thế ép kiểu `(String)request.getAttribute("queueNumber")` bằng `escReception(String.valueOf(request.getAttribute("queueNumber")))`.

#### 1.2. Chuẩn hóa DoctorProfileValidator.java & An toàn hiển thị Manager
- Tệp: `src/java/hospital/validation/DoctorProfileValidator.java`
- Thao tác:
  - Cập nhật độ dài hợp lệ: `fullName` <= 100, `specialization` <= 100, `academicTitle` <= 50.
  - Phí khám `fee`: kiểm tra bằng biểu thức chính quy `(?:0|[1-9][0-9]{0,15})(?:\.[0-9]{1,2})?` (chặn số mũ `1E5`, số âm, và số nguyên vượt quá 16 chữ số để tương thích chính xác `DECIMAL(18,2)`).
- Tệp: `web/WEB-INF/views/staff-content.jsp`, `services-content.jsp`, `medicines-content.jsp`
- Thao tác:
  - Cô lập lỗi validation theo đúng dòng bản ghi đang sửa (`activeErrors` so sánh với `doctorId`/`serviceId`/`medicineId`).
  - Thêm thuộc tính `open` cho thẻ `<details>` khi bản ghi có lỗi để form không bị đóng lại.

#### 1.3. Bổ sung HTML Escaping phòng ngừa Reflected XSS (Defense-in-Depth)
- Áp dụng hàm escaping cho các biến `savedMessage`, `actionError`, `loadError`, `dateError` tại:
  - `web/WEB-INF/views/doctor-content.jsp`
  - `web/WEB-INF/views/medical-staff-content.jsp`
  - `web/WEB-INF/views/schedule-content.jsp`
  - `web/WEB-INF/views/staff-content.jsp`

#### Tiêu chí kiểm chứng Đợt 1:
- `ant compile test jsp-check` thành công với `Generation completed with [0] errors`.
- Thêm unit test kiểm tra validator bác sĩ trong `TestSuite.java` (độ dài chuỗi và regex tiền tệ).

---

### ĐỢT 2: Hoàn thiện Luồng Xem Hồ sơ, Đơn thuốc & Chỉ định Cận lâm sàng

#### 2.1. Bổ sung truy vấn Chỉ định & Kết quả dịch vụ theo Lượt khám
- Tệp: `src/java/hospital/dao/HospitalDao.java`
- Thao tác: Thêm phương thức `findServiceOrdersForAppointment(int appointmentId, int doctorUserId)` đọc danh sách chỉ định gồm mã chỉ định, tên dịch vụ, ưu tiên, hướng dẫn lâm sàng, trạng thái đơn, trạng thái kết quả, kết luận (`Findings`) và chi tiết (`ResultDetails`).
- Đảm bảo kiểm tra quyền sở hữu ca khám: `appointmentId` phải do đúng `doctorUserId` phụ trách.

#### 2.2. Nạp dữ liệu đầy đủ và Giữ Form khi có lỗi tại DoctorWorkflowServlet.java
- Tệp: `src/java/hospital/web/DoctorWorkflowServlet.java`
- Thao tác:
  - Trong phương thức `render`: khi có `appointmentId`, tự động nạp cả `prescriptionLines` và `appointmentServiceOrders`.
  - Khi thêm chỉ định dịch vụ thất bại: lưu lại các giá trị đã nhập (`catalogId`, `priority`, `clinicalInstructions`) vào request attribute để form không bị xóa trắng.

#### 2.3. Hỗ trợ Xem Hồ sơ Ca khám đã Hoàn tất (Chế độ Chỉ đọc)
- Tệp: `web/WEB-INF/views/doctor-content.jsp` & `doctor-orders-content.jsp`
- Thao tác:
  - Thêm nút "Xem hồ sơ" cho các ca có trạng thái `COMPLETED` tại các tab của Bác sĩ (`consultation`, `patients`, `prescriptions`, `lab-orders`).
  - Khi xem ca `COMPLETED`: hiển thị bảng thông tin chỉ đọc gồm chẩn đoán, ghi chú lâm sàng, phác đồ điều trị, bảng đơn thuốc đã kê và bảng kết quả chỉ định cận lâm sàng (ẩn các nút thêm/sửa/xóa).

#### 2.4. Phân tách Tab và Xem Kết quả Chỉ định cho Nhân viên Y tế
- Tệp: `web/WEB-INF/views/medical-staff-content.jsp`
- Thao tác:
  - Tab `service-history`: chỉ hiển thị các chỉ định đã kết thúc (`COMPLETED` hoặc `CANCELLED`).
  - Bổ sung nút "Xem kết quả" và panel xem chi tiết chỉ định cận lâm sàng ở chế độ chỉ đọc (hướng dẫn lâm sàng, kết luận, chi tiết kỹ thuật, thời điểm phát hành).

#### Tiêu chí kiểm chứng Đợt 2:
- Bác sĩ chỉ xem được hồ sơ thuộc ca khám của mình.
- Ca khám `COMPLETED` hiển thị đầy đủ thông tin ở dạng chỉ đọc.
- Giữ lại dữ liệu nhập khi thêm chỉ định lỗi.

---

### ĐỢT 3: Quy tắc Hủy Lịch hẹn, Lịch sử Bệnh nhân & Kiểm soát Concurrency

#### 3.1. Chốt Quy tắc Hủy Lịch theo SRS UC-04
- **Bệnh nhân tự hủy trực tuyến**:
  - Chỉ áp dụng cho lịch `BOOKED`.
  - Phải trước giờ khám ít nhất 2 giờ (`DATEADD(HOUR, 2, GETDATE()) < StartDateTime`).
  - Nếu dưới 2 giờ: từ chối và hiển thị thông báo hướng dẫn liên hệ hotline.
- **Lễ tân hủy lịch tại quầy**:
  - Áp dụng cho lịch `BOOKED` hoặc `NEEDS_PROCESSING`.
  - Từ chối hủy nếu lịch đã chuyển sang `IN_CONSULTATION` hoặc `COMPLETED`.

#### 3.2. Cập nhật Truy vấn Lịch sử Bệnh nhân (HospitalDao.java:510)
- Tệp: `src/java/hospital/dao/HospitalDao.java`
- Thao tác: Trong phương thức `findPatientAppointmentChanges(int patientId)`, mở rộng điều kiện SQL:
  `l.Action IN ('PATIENT_UPDATE_REASON', 'PATIENT_RESCHEDULE', 'PATIENT_CANCEL', 'RECEPTIONIST_CANCEL')`
- Tệp: `web/WEB-INF/views/patient.jsp`
- Thao tác: Cập nhật nhãn hiển thị tại bảng Lịch sử thay đổi:
  - `PATIENT_CANCEL` -> "Bệnh nhân hủy lịch"
  - `RECEPTIONIST_CANCEL` -> "Lễ tân hủy lịch"
  - `PATIENT_RESCHEDULE` -> "Đổi lịch khám"
  - `PATIENT_UPDATE_REASON` -> "Cập nhật lý do"

#### 3.3. Thực hiện Hủy Lịch An toàn trong Transaction & Giải phóng Slot
- Thao tác tại `HospitalDao.java`:
  - Mức cô lập `TRANSACTION_SERIALIZABLE` kèm khóa `WITH (UPDLOCK, HOLDLOCK)`.
  - Cập nhật `Appointments`: `Status = 'CANCELLED'`, `CancellationReason = ?`, `CancelledAt = GETDATE()`.
  - Giải phóng slot: Đặt `ScheduleSlots.IsBooked = 0` **chỉ khi** không còn cuộc hẹn nào khác ở trạng thái `BOOKED`, `CHECKED_IN`, hoặc `IN_CONSULTATION` đang giữ cùng slot đó.
  - Ghi nhận `SystemLogs` tương ứng.

#### 3.4. Phòng ngừa Race Condition khi Tải Khung giờ Khám
- Tệp: `web/WEB-INF/views/patient.jsp` & `registration-content.jsp`
- Thao tác: Áp dụng kỹ thuật `AbortController` và biến đếm `requestVersion` khi gửi request `/slots`, tránh ghi đè dữ liệu do phản hồi về chậm khi chuyển nhanh giữa các ngày/ca khám.
- Thêm ràng buộc `max="<%=LocalDate.now()%>"` cho trường ngày sinh tại `registration-content.jsp`.

#### Tiêu chí kiểm chứng Đợt 3:
- Kiểm thử tự động logic điều kiện 2 giờ và hủy lịch an toàn.
- Lịch sử bệnh nhân hiển thị đúng các mục hủy lịch.
- Rollback toàn vẹn nếu có lỗi xảy ra trong quá trình hủy.
- Với các tính năng chưa có tài khoản test trên DB thật, báo cáo ghi rõ: "Chưa kiểm chứng tích hợp".
```

