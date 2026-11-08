# Báo cáo rà soát và sửa lỗi

Ngày: 08/11/2026  
Project: `SWD392_SE1976-JS(AD)-Final Evoluation-Group-4`

## Tóm tắt

Đã rà SRS, SDS v3, mã Servlet/DAO/JSP, cấu hình Ant/NetBeans, metadata và số liệu SQL Server hiện có cùng log Tomcat được cung cấp. Giữ nguyên Java 17, Jakarta Servlet/JSP, Ant, JDBC SQL Server và Tomcat 10.1. Không tạo database, chạy migration, thêm dữ liệu mẫu hay thay đổi file `.env.example` đính kèm. Mật khẩu DB không được in vào báo cáo.

Các sửa lỗi chính: biên dịch JSP bằng Jasper; lọc trạng thái lễ tân an toàn khi null; luồng chỉ định/hoàn tất lượt khám và lập bill; đơn nghỉ theo vai trò; tra cứu bệnh nhân tại quầy; thu tiền mặt; cổng tra cứu/đổi lịch của bệnh nhân có xác minh; danh mục thuốc riêng cho Manager; UTF-8 cho tài nguyên tĩnh; và ngăn thông báo thành công giả bằng query string.

## Đã sửa và cách hoạt động

### Lỗi 500 và kiểm chứng JSP

- Các JSP Doctor, Receptionist và MedicalStaff dùng `switch` dạng statement tương thích source của Jasper, không dùng switch expression Java 14+.
- `ReceptionServlet` chuẩn hóa status null/rỗng trước khi kiểm tra danh sách trạng thái; các trang lịch hẹn, check-in và hàng đợi không còn gọi `Set.of(...).contains(null)`.
- Target `jsp-check` trong `build.xml` biên dịch JSP với Tomcat Jasper ở source/target 1.8. Target chỉ pass nếu Jasper báo chính xác `Generation completed with [0] errors`; nếu JSP lỗi, Ant fail thay vì chỉ in log lỗi nhưng vẫn báo thành công.
- Log hiện có vẫn chứa stack trace cũ (03:50–03:52) của lỗi switch expression và NPE trước sửa. File log không được xóa. Sau sửa, Jasper precompile toàn bộ JSP báo 0 lỗi.

### Doctor, chỉ định, hoàn tất lượt khám và bill

- Luồng chỉ định dịch vụ yêu cầu đã lưu chẩn đoán sơ bộ và hồ sơ đang `IN_PROGRESS`; kiểm tra bác sĩ sở hữu lượt khám và dịch vụ còn hoạt động.
- Không hoàn tất hồ sơ khi còn MedicalServiceOrder chưa `COMPLETED` hoặc kết quả chưa `RELEASED`.
- Hoàn tất MedicalRecord, cập nhật Appointment, tạo Bill trạng thái `UNPAID` (phí khám + dịch vụ đã hoàn tất/phát hành) và audit nằm trong cùng transaction.
- Bác sĩ vẫn có chức năng đơn thuốc, danh sách lượt khám và gửi đơn nghỉ theo menu/role hiện có.

### Danh mục thuốc và đơn thuốc

- Thuốc và dịch vụ y tế là hai danh mục khác nhau. Manager thêm/sửa/kích hoạt thuốc ở “Danh mục thuốc” qua bảng `Medicines`; form “Dịch vụ và giá” chỉ dành cho dịch vụ y tế. Không chèn thuốc mẫu.
- Khi mở form kê đơn của lượt khám `IN_PROGRESS`, Doctor lấy lựa chọn mới nhất từ `Medicines WHERE IsActive=1`. Danh mục rỗng hoặc lỗi đọc DB có trạng thái riêng; không hiện lựa chọn giả.
- Đơn thuốc đã lưu được đọc lại cùng giá snapshot; Doctor có thể cập nhật/xóa dòng khi hồ sơ còn `IN_PROGRESS`. Thông báo thành công chỉ được flash sau khi DAO commit.
- Chưa có phiên kiểm thử Manager/Doctor để xác nhận qua UI việc thêm thuốc rồi thấy thuốc trong dropdown; không có ghi thuốc thử vào DB. Vì vậy thao tác CRUD và dropdown chưa được đánh dấu kiểm thử tích hợp.

### Receptionist và bệnh nhân tại quầy

- Danh sách lịch vẫn lọc theo ngày/trạng thái/tên/số điện thoại. Check-in chỉ nhận lịch `BOOKED` đúng ngày, cấp QueueNumber và ghi audit trong transaction; check-in lặp bị từ chối.
- Bổ sung tra cứu hồ sơ chính xác theo số điện thoại, số giấy tờ hoặc PatientId. Nếu trùng nhiều hồ sơ, yêu cầu dùng mã hồ sơ/đối chiếu trực tiếp. Kết quả không tiết lộ AccessCode.
- Đăng ký tại quầy vẫn tự kiểm tra tên/ngày sinh khi số điện thoại đã tồn tại. Hồ sơ mới có mã truy cập một lần sau commit; hồ sơ cũ không được cấp lại mã chỉ nhờ số điện thoại.
- Thành công check-in, đăng ký và thu tiền được chuyển qua session flash một lần; tự nhập query string `checkedIn=1`, `saved=1` hoặc `paid=1` không tự tạo thông báo đã lưu.

### Bill và thanh toán

- Chỉ tạo Bill khi lượt khám hoàn tất, không tạo bill khi đăng ký lịch.
- Thêm trang thanh toán tại quầy cho Receptionist: đọc bill thật, hiển thị `UNPAID`/`PAID`, số tiền và biên nhận; thu tiền mặt ghi `Payments`, đổi bill sang `PAID`, audit và commit cùng transaction. Khóa và kiểm tra trạng thái cùng unique `Payments.BillId` chống thu trùng.
- Không mô phỏng cổng VNPay/MoMo và không tạo role Cashier.

### Đơn nghỉ

- Doctor, Receptionist và MedicalStaff chỉ xem/gửi/sửa đơn của chính mình; chỉ đơn Pending mới sửa. Kiểm tra ngày, ngày quá khứ, trùng với Pending/Approved khác và audit cập nhật trong transaction.
- Manager xem đơn cần duyệt/lịch sử quyết định; không thể tự duyệt đơn của mình. Với đơn Doctor, đang có appointment `IN_CONSULTATION` thì chặn duyệt; `BOOKED`/`CHECKED_IN` thành `NEEDS_PROCESSING`, slot trống bị khóa và schedule bị hủy trong transaction. Đơn của Receptionist/MedicalStaff không khóa lịch bác sĩ.
- SMS/email chưa có provider; các lượt bị ảnh hưởng cần nhân viên xử lý thủ công.

### Cổng bệnh nhân

- Liên kết “Thông tin bệnh nhân” mở trang riêng `/patient`; form đặt lịch vẫn ở trang chủ.
- Tra cứu lịch cần POST số điện thoại hoặc email cùng AccessCode. Mã mới có độ dài 43 ký tự, lưu SHA-256 Base64URL hash vừa cột `varchar(50)`, chỉ hiển thị một lần sau commit; không có mã trong URL, JavaScript hay localStorage.
- Hồ sơ chỉ hiển thị cho PatientId đã xác minh trong HttpSession. Lịch chỉ hiện trạng thái bill nếu có Bills thật. Lịch `BOOKED` tương lai cho sửa lý do; đổi bác sĩ/ngày/slot phải trước hơn 12 giờ, tối đa hai lần, không trùng lịch cùng bệnh nhân, chỉ dùng slot công bố/còn trống và cập nhật/nhả slot trong transaction. Lịch sử thay đổi có ngày, bác sĩ và giờ cũ/mới.
- Mã truy cập của hồ sơ cũ chưa được bệnh nhân nhận không thể khôi phục an toàn qua điện thoại/email; cần xác minh tại quầy.

### Đặt lịch công khai và lỗi tiếng Việt

- Điều tra thấy tài nguyên JavaScript từng được Tomcat phục vụ không khớp bản trong workspace; sau deploy, response thiếu `charset=UTF-8`. `EncodingFilter` hiện đặt UTF-8 cho cả tài nguyên tĩnh và trang động; URL script có version `?v=2` để tránh cache bản cũ.
- Sau deploy, HTTP `/assets/app.js?v=2` trả `text/javascript;charset=UTF-8`; bytes HTTP khớp SHA-256/nội dung với file workspace và các chuỗi giao diện được giải mã đúng tiếng Việt.
- Kiểm tra chỉ đọc DB tại `http://localhost:8080/HospitalManagement/slots?doctorId=1&date=2026-11-09&shift=AM`: 8 slot, `AVAILABLE`. Ca `PM`: 0 slot, `NOT_PUBLISHED`. Trên trình duyệt, ca sáng nạp 8 thời điểm; ca chiều khóa lựa chọn và hiện “Bác sĩ chưa công bố lịch cho ngày và ca này...”. Không tự tạo lịch hoặc giờ.
- Ảnh người dùng gửi có ngày sinh 09/11/2026 (sau ngày kiểm tra 08/11/2026) và chọn ca chiều không có lịch công bố; cả hai điều kiện đều khiến không thể đặt. POST kiểm tra với ngày sinh tương lai, số điện thoại sai và thiếu slot trả lỗi từng trường, giữ lại nội dung nhập, và dừng trước khi gọi DAO. Không gửi POST hợp lệ nên chưa chứng minh ghi lịch hẹn thành công trên DB.
- Nếu số điện thoại đã thuộc hồ sơ khác, booking công khai bị từ chối có chủ đích; chỉ Receptionist được tái sử dụng hồ sơ sau khi xác minh. Không nới lỏng kiểm tra này để “làm cho đặt được”.

## File chính đã sửa

| File | Thay đổi |
|---|---|
| `build.xml` | Thêm kiểm tra Jasper và điều kiện fail nếu không có báo cáo zero-error. |
| `nbproject/project.properties` | Khai báo Servlet API và SQL Server JDBC trong classpath Ant/NetBeans, không chứa bí mật. |
| `src/java/hospital/dao/HospitalDao.java` | Luồng slot/lịch, đơn nghỉ, MedicalServiceOrder, bill, payment, tra cứu hồ sơ và đổi lịch bệnh nhân bằng PreparedStatement/transaction. |
| `src/java/hospital/web/ReceptionServlet.java` | Chuẩn hóa status null, kiểm soát check-in, thông báo kết quả qua session flash. |
| `src/java/hospital/web/ReceptionRegistrationServlet.java` | Tra cứu bệnh nhân tại quầy, kiểm tra đăng ký, mã truy cập một lần. |
| `src/java/hospital/web/ReceptionPaymentServlet.java` | Màn thu tiền quầy, chống hiển thị thành công giả. |
| `src/java/hospital/web/PatientPortalServlet.java`, `src/java/hospital/web/HomeServlet.java`, `src/java/hospital/web/AppointmentServlet.java` | Xác minh hồ sơ, lịch sử/đổi lịch, mã truy cập và kết quả đặt lịch một lần. |
| `src/java/hospital/web/DoctorWorkflowServlet.java`, `src/java/hospital/web/MedicineServlet.java`, `src/java/hospital/web/LeaveServlet.java`, `src/java/hospital/web/RoleFilter.java`, `src/java/hospital/web/DashboardServlet.java` | Luồng Doctor/đơn thuốc, danh mục thuốc Manager, đơn nghỉ và kiểm tra role ở phía server. |
| `src/java/hospital/model/Medicine.java`, `PrescriptionLine.java`, `src/java/hospital/validation/MedicineValidator.java`, `web/WEB-INF/views/medicines-content.jsp`, `doctor-orders-content.jsp` | Thêm thuốc thật qua Manager, kiểm tra dữ liệu, đọc lại và quản lý dòng thuốc trong đơn đang lập. |
| `src/java/hospital/validation/ReceptionStatusFilter.java`, `src/java/hospital/validation/PatientLookupValidator.java`, `src/java/hospital/dao/AppointmentSafety.java`, `src/java/hospital/security/PatientAccessCode.java` | Chuẩn hóa/kiểm tra input, điều kiện an toàn giao dịch và mã bệnh nhân. |
| `src/java/hospital/model/PatientLookupResult.java`, `PatientAppointment.java`, `AppointmentChange.java`, `PatientBookingResult.java`, `BillSummary.java`, `LeaveRequest.java` | Dữ liệu cho tra cứu, lịch sử và thanh toán. |
| `web/WEB-INF/views/doctor-content.jsp`, `reception-content.jsp`, `medical-staff-content.jsp`, `registration-content.jsp`, `patient.jsp`, `payment-content.jsp`, `leave-content.jsp`, `home.jsp` | Sửa JSP, giao diện và trạng thái rỗng/lỗi/thành công. |
| `test/hospital/TestSuite.java`, `test/hospital/HospitalDaoReadOnlyTest.java` | Thêm kiểm tra status null, lookup validation, AccessCode, slot chỉ đọc, xung đột đơn nghỉ, chỉ định đang chờ và điều kiện giữ slot. |
| `README.md` | Cập nhật hướng dẫn mở/chạy, module và giới hạn. |
| `BAO_CAO_RA_SOAT.md` | Báo cáo bàn giao hiện tại. |

## Bằng chứng build và runtime

- Chạy `ant clean test war jsp-check deploy`: `BUILD SUCCESSFUL`; 55 kiểm tra trong TestSuite pass; Jasper target báo “Tomcat Jasper compiled all JSPs with zero errors”; WAR đã deploy vào Tomcat.
- Chạy target compile NetBeans sinh sẵn: `ant -f nbproject/build-impl.xml clean compile` thành công, biên dịch 58 source Java với classpath Servlet/JDBC từ `nbproject/project.properties`. Lượt chạy trong sandbox bị chặn đọc `servlet-api.jar` ngoài workspace; chạy lại với quyền đọc Tomcat thành công.
- Chạy `ant deploy`: `BUILD SUCCESSFUL`, WAR được chép vào `C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps`.
- Sau deploy: GET `/HospitalManagement/` = HTTP 200. `/assets/app.js?v=2` = HTTP 200, `text/javascript;charset=UTF-8`; bytes phản hồi khớp file nguồn và chuỗi tiếng Việt không mojibake. GET `/patient` trước đó trả HTTP 200.
- Lần kiểm tra lại sau deploy: trang chủ và JavaScript đều HTTP 200, response JS có `charset=UTF-8`, HTML chứa tiếng Việt đúng và không có ký tự `�`; GET slot vẫn trả 8 slot ca sáng (`AVAILABLE`) và 0 slot ca chiều (`NOT_PUBLISHED`). Terminal Ant/PowerShell hiện còn hiển thị một số ký tự tiếng Việt sai mã; chưa chỉnh encoding stdout của terminal, không phải lỗi encoding quan sát được trong HTTP/trình duyệt.
- Kiểm tra GET slot trên DB hiện tại: bác sĩ 1, 09/11/2026, ca sáng = 8 slot và `AVAILABLE`; ca chiều = 0 slot và `NOT_PUBLISHED`. Trên trình duyệt tích hợp, chọn ca sáng nạp 8 lựa chọn; ca chiều hiển thị thông báo chưa công bố lịch và vô hiệu hóa select giờ.
- POST đặt lịch sai validation trả HTTP 200 với lỗi ngày sinh/số điện thoại/slot; tên người gửi vẫn nằm trong form. Validation xảy ra trước DAO, không có ghi DB. Không gửi POST hợp lệ.
- Các request chưa đăng nhập tới `/dashboard?page=consultation`, `/doctor/consultation` và `/reception/payments` trả HTTP 302 về login.
- Trình duyệt thật: trang chủ hiển thị dữ liệu bác sĩ từ DB và form; popup có focus vào tên đăng nhập, mở/đóng được bằng X và Escape. POST đăng nhập với tài khoản thử không tồn tại giữ nguyên trang chủ, mở lại popup và hiện lỗi ngay trong popup.
- POST xác minh bệnh nhân với mã sai bị từ chối và không hiển thị lịch.
- Query giả `/?booking=success` và `/patient?updated=1` không hiển thị thông báo thành công.
- VS Code Problems provider trong phiên này vẫn liệt kê unresolved Jakarta trên `EncodingFilter` và `DoctorWorkflowServlet` (trước đó cũng ghi nhận `LeaveServlet`/`ReceptionRegistrationServlet`), dù 58 source đã biên dịch pass với Ant/NetBeans target. Đây là sai khác classpath/model của provider trong VS Code; NetBeans Ant compile dùng được `servlet-api.jar`. Nếu NetBeans IDE vẫn báo tương tự, cần refresh server libraries.
- URL các trang và luồng dữ liệu role-specific chưa thể kiểm thử end-to-end do không có tài khoản test Doctor/Receptionist/Manager/MedicalStaff. Không thực hiện POST nghiệp vụ hợp lệ để tránh ghi dữ liệu thử vào database thật.

## Schema/config đã đọc và điểm chưa thống nhất

- SQL Server dùng database `HospitalManagementDB` qua JNDI Tomcat, URL đang cấu hình `jdbc:sqlserver://localhost:51433;databaseName=HospitalManagementDB;encrypt=true;trustServerCertificate=true;loginTimeout=5`. Không đọc/in mật khẩu và không sửa file `.env.example` đính kèm. URL này khác instance `SQLEXPRESS04` trong file đính kèm; không dùng giá trị `.env.example` thay cho JNDI đang chạy.
- Metadata đã xác nhận: `Patients.AccessCode` là `varchar(50)`, `Phone` unique; `Bills.AppointmentId` unique; `Payments.BillId` unique; `SystemLogs.UserId` nullable. Không có schema SQL dump trong workspace; metadata đọc trực tiếp, không chạy DDL.
- Số lượng ở thời điểm kiểm tra: `Patients=1`, `Appointments=1`, `Bills=0`, `Payments=0`, `MedicalServices=0`, `MedicalServiceOrders=0`. Không có dữ liệu nghiệp vụ được thêm trong lần sửa này.
- Roles thực tế: `Manager`, `Doctor`, `Receptionist`, `MedicalStaff`. Không gộp hoặc thêm Cashier/Lab Technician.
- SRS nói phí thuốc trong billing, trong khi SDS v3 và schema Bills chỉ mô tả phí khám + dịch vụ; cần chốt quy tắc/schema trước khi cộng tiền thuốc.
- SRS mô tả lịch lặp, còn luồng hiện tại chỉ công bố lịch theo ngày với ca sáng/chiều. “Phản hồi” không có bảng lưu; SMS/email/cổng thanh toán không có provider. Các mục này chưa triển khai.

## Kiểm thử tích hợp còn bị chặn

- Không test được hai request booking đồng thời cùng một slot trên database, approve leave có lượt đang khám, check-in, lập bill và thu tiền bằng transaction thật vì không có slot/bill/dữ liệu/phiên test được phép dùng. Logic conditional update/transaction được kiểm thử ở mức helper hoặc code review, không được coi là tích hợp DB đã chứng minh.
- Chưa dùng phiên đúng role để xác minh từng URL được phép và URL trái role; request chưa đăng nhập đã được kiểm tra bị chuyển về login.
- Các luồng DB ghi thật chỉ nên test tiếp bằng tài khoản và dữ liệu sandbox được cấp riêng. Không dùng bệnh nhân thật làm fixture.

## Chạy lại

```powershell
& 'C:\Program Files\NetBeans-17\netbeans\extide\ant\bin\ant.bat' clean test war jsp-check deploy
```

Mở project con có `build.xml` trong NetBeans. URL hiện tại: `http://localhost:8080/HospitalManagement/`.
