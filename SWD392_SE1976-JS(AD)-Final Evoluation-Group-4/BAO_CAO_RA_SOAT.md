# Báo cáo rà soát và sửa lỗi

Cập nhật: 09/11/2026

Project: `SWD392_SE1976-JS(AD)-Final Evoluation-Group-4`

## Tóm tắt

Đã rà SRS, SDS v3, mã Servlet/DAO/JSP, cấu hình Ant/NetBeans, metadata và số liệu SQL Server hiện có cùng log Tomcat được cung cấp. Giữ nguyên Java 17, Jakarta Servlet/JSP, Ant, JDBC SQL Server và Tomcat 10.1. Không tạo database, chạy migration, thêm dữ liệu mẫu hay thay đổi file `.env.example` đính kèm. Mật khẩu DB không được in vào báo cáo.

Các sửa lỗi chính: biên dịch JSP bằng Jasper; lọc trạng thái lễ tân an toàn khi null; luồng chỉ định/hoàn tất lượt khám và lập bill; đơn nghỉ theo vai trò; tra cứu bệnh nhân tại quầy; thu tiền mặt; cổng tra cứu/đổi lịch của bệnh nhân có xác minh; danh mục thuốc riêng cho Manager; UTF-8 cho tài nguyên tĩnh; và ngăn thông báo thành công giả bằng query string.

## Đã sửa và cách hoạt động

### Lỗi 500, bảo mật XSS và kiểm chứng JSP

- Các JSP Doctor, Receptionist và MedicalStaff dùng `switch` dạng statement tương thích source của Jasper, không dùng switch expression Java 14+.
- Đã khắc phục triệt để `ClassCastException` tại `reception-content.jsp` (ép kiểu an toàn `String.valueOf(request.getAttribute("queueNumber"))` thay vì `(String)`).
- Bổ sung hàm HTML escaping (`escDoctor`, `escStaff`, `escReception`, `escMedical`) cho toàn bộ thông báo `actionError`, `savedMessage`, `dateError`, `loadError` trên tất cả các JSP, triệt tiêu nguy cơ Reflected XSS.
- `ReceptionServlet` chuẩn hóa status null/rỗng trước khi kiểm tra danh sách trạng thái; các trang lịch hẹn, check-in và hàng đợi không còn gọi `Set.of(...).contains(null)`.
- Target `jsp-check` trong `build.xml` biên dịch JSP với Tomcat Jasper ở source/target 1.8. Target chỉ pass nếu Jasper báo chính xác `Generation completed with [0] errors`; nếu JSP lỗi, Ant fail thay vì chỉ in log lỗi nhưng vẫn báo thành công.
- Jasper precompile toàn bộ 100% JSP đạt chuẩn 0 lỗi.

### Doctor, chỉ định, xem hồ sơ hoàn tất và bill

- Luồng chỉ định dịch vụ yêu cầu đã lưu chẩn đoán sơ bộ và hồ sơ đang `IN_PROGRESS`; kiểm tra bác sĩ sở hữu lượt khám và dịch vụ còn hoạt động.
- Giữ lại đầy đủ các trường nhập liệu (`catalogId`, `priority`, `clinicalInstructions`) khi tạo chỉ định dịch vụ bị lỗi để bác sĩ không phải nhập lại.
- Tự động nạp đồng thời `prescriptionLines` và `appointmentServiceOrders` khi bác sĩ chọn bất kỳ lượt khám nào.
- Bổ sung nút "Xem hồ sơ" cho các ca khám đã `COMPLETED` tại các tab của Bác sĩ (`consultation`, `patients`, `prescriptions`, `lab-orders`). Khi mở hồ sơ đã hoàn tất, giao diện hiển thị hộp thông tin chỉ đọc (chẩn đoán, ghi chú lâm sàng, hướng điều trị) kèm bảng đơn thuốc và bảng chỉ định/kết quả dịch vụ đã thực hiện.
- Không hoàn tất hồ sơ khi còn MedicalServiceOrder chưa `COMPLETED` hoặc kết quả chưa `RELEASED`.
- Hoàn tất MedicalRecord, cập nhật Appointment, tạo Bill trạng thái `UNPAID` (phí khám + dịch vụ đã hoàn tất/phát hành) và audit nằm trong cùng transaction.

### Danh mục thuốc, dịch vụ và an toàn thông tin bác sĩ

- Thuốc và dịch vụ y tế là hai danh mục khác nhau. Manager thêm/sửa/kích hoạt thuốc ở “Danh mục thuốc” qua bảng `Medicines`; form “Dịch vụ và giá” chỉ dành cho dịch vụ y tế. Không chèn thuốc mẫu.
- Chuẩn hóa validation hồ sơ bác sĩ (`DoctorProfileValidator`): giới hạn độ dài `fullName` (<= 100 ký tự), `specialization` (<= 100), `academicTitle` (<= 50) và chuẩn hóa regex phí khám `(?:0|[1-9][0-9]{0,15})(?:\.[0-9]{1,2})?` chặn số mũ (1E5) và từ chối 17 chữ số nguyên để tương thích chặt chẽ với kiểu `DECIMAL(18,2)`.
- Cập nhật `HospitalDao.updateDoctorProfile`: áp dụng mức cô lập `TRANSACTION_SERIALIZABLE` kèm khóa `WITH (UPDLOCK, HOLDLOCK)`, kiểm tra chống trùng lặp email bác sĩ với người dùng khác và ghi nhận lịch sử vào `SystemLogs`.
- Giao diện Manager (`staff-content.jsp`, `services-content.jsp`, `medicines-content.jsp`): giữ trạng thái mở `<details open>` khi có lỗi validation và cô lập phạm vi hiển thị lỗi (`activeErrors`) đúng theo dòng dữ liệu đang sửa.

### Nhân viên y tế (Medical Staff) và lịch sử chỉ định

- Tab `service-history` được lọc chính xác chỉ hiển thị các chỉ định đã kết thúc (`COMPLETED` hoặc `CANCELLED`).
- Bổ sung nút "Xem kết quả" và panel tra cứu chi tiết chỉ định cận lâm sàng ở chế độ chỉ đọc: hiển thị chỉ định lâm sàng của bác sĩ, kết luận xét nghiệm, chi tiết kỹ thuật và thời điểm phát hành.

### Hủy lịch hẹn và kiểm soát Race Condition

- Bổ sung phương thức `cancelAppointmentByReceptionist` và `cancelPatientAppointment` trong `HospitalDao`: kiểm tra trạng thái hợp lệ (`BOOKED` / `CHECKED_IN`), hủy lịch, giải phóng slot (`ScheduleSlots.IsBooked = 0`) an toàn có đối chiếu không còn cuộc hẹn nào khác chiếm giữ slot, đồng thời ghi audit log.
- Lễ tân: Thêm thao tác "Hủy lịch" có hộp thoại xác nhận tại tab danh sách lịch hẹn (`reception-content.jsp` và `ReceptionServlet.java`).
- Bệnh nhân: Thêm chức năng hủy lịch hẹn trực tiếp trên Cổng bệnh nhân (`patient.jsp` và `PatientPortalServlet.java`), cô lập lỗi theo từng thẻ lịch hẹn.
- Chống Race Condition khi tải slot: Chuyển đổi cơ chế fetch khung giờ khám trong `patient.jsp` và `registration-content.jsp` sang kỹ thuật `AbortController` và `requestVersion`, ngăn triệt để ghi đè dữ liệu khi người dùng chuyển ngày/ca liên tục.
- Thêm ràng buộc `max="<%=LocalDate.now()%>"` cho ngày sinh tại trang đăng ký tại quầy (`registration-content.jsp`).

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
- Kiểm tra runtime mới nhất sau deploy ngày 08/11/2026: GET `http://localhost:8080/HospitalManagement/slots?doctorId=1&date=2026-11-09&shift=AM` trả 7 slot, `AVAILABLE`; ca `PM` trả danh sách rỗng (`NOT_PUBLISHED`). Số slot hiện tại khác lần kiểm tra trước (8), phản ánh dữ liệu đang có trong DB; không tạo lịch hoặc giờ.
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

- Kiểm tra lại ngày 09/11/2026: `ant test` và `ant war jsp-check` đều `BUILD SUCCESSFUL`, 61 kiểm tra pass, Jasper báo toàn bộ JSP biên dịch không lỗi. JSP/CSS được cập nhật trong lúc rà soát nên đã chạy lại Jasper và tải trang mới trước khi kết luận. Đối chiếu 19 file trong `web` với bản Tomcat đang triển khai không có khác biệt ở thời điểm chốt.
- Lần chạy sạch mới nhất ngày 08/11/2026: `ant clean test war jsp-check deploy` trả `BUILD SUCCESSFUL`; TestSuite pass 61 kiểm tra; biên dịch 58 source Java; Jasper báo “Tomcat Jasper compiled all JSPs with zero errors”; WAR được chép vào Tomcat.
- Chạy target compile NetBeans sinh sẵn: `ant -f nbproject/build-impl.xml clean compile` thành công, biên dịch 58 source Java với classpath Servlet/JDBC từ `nbproject/project.properties`. Lượt chạy trong sandbox bị chặn đọc `servlet-api.jar` ngoài workspace; chạy lại với quyền đọc Tomcat thành công.
- Chạy `ant deploy`: `BUILD SUCCESSFUL`, WAR được chép vào `C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps`.
- Sau deploy sạch: GET `/` và `/patient` trả HTTP 200 với `text/html; charset=UTF-8`; `/assets/app.js?v=2` trả HTTP 200 với `text/javascript; charset=UTF-8`. HTML/JS kiểm tra không có ký tự thay thế `�`.
- Tìm bác sĩ theo `Bác sĩ Demo 01` trả HTTP 200 và có kết quả. Kiểm tra bằng UI ngày 09/11 với `QA_NO_SUCH_DOCTOR`: hiện sai thông báo “Chưa có bác sĩ được công khai”; dropdown Bác sĩ trong form đặt lịch cũng chỉ còn option “Chọn bác sĩ”, trong khi bỏ bộ lọc có 5 bác sĩ công khai.
- GET slots hiện trả 7 khung giờ ca sáng (`AVAILABLE`) và `[]` cho ca chiều (`NOT_PUBLISHED`). Test helper DAO với JDBC proxy xác nhận 2 lệnh SELECT và 0 thao tác ghi khi lịch chưa công bố; GET runtime cũng chỉ đọc.
- POST đặt lịch sai validation, có CSRF hợp lệ, trả HTTP 200; ngày sinh tương lai, điện thoại sai và thiếu slot hiện lỗi; tên nhập được giữ lại; không có thông báo thành công. Servlet kiểm tra lỗi trước lời gọi DAO; không thực hiện POST hợp lệ.
- POST xác minh bệnh nhân bằng email `.invalid` và access code sai nhưng đúng định dạng trả HTTP 200 với lỗi xác minh chung; không hiện lịch sử hoặc thông báo thành công. Đã kiểm tra lại trực tiếp trên UI ngày 09/11.
- Ngày 09/11: 16/16 URL nội bộ trong RoleFilter trả HTTP 302 về `/?login=required` khi chưa đăng nhập; truy cập trực tiếp `/WEB-INF/views/dashboard.jsp` trả 404. Đây là kiểm tra chưa đăng nhập, chưa thay thế kiểm tra trái role bằng phiên đã đăng nhập.
- Đã kiểm tra trực tiếp popup: mở bằng nút Đăng nhập, focus vào username, Shift+Tab vòng trong popup, X/Escape đóng và trả focus về nút mở. Tài khoản không tồn tại hiện lỗi trong popup trên trang chủ; DAO xác thực chỉ SELECT. Trên giao diện mới nhất đã kiểm tra lại mở/Escape và focus.
- Liên kết Thông tin bệnh nhân đi tới `/patient` trong cùng tab; browser vẫn chỉ có 1 tab. Ca sáng bác sĩ 1 ngày 09/11 nạp 7 slot; ca chiều khóa select và hiện thông báo chưa công bố lịch bằng tiếng Việt đúng. Gửi form còn thiếu thông tin/slot bị chặn tại trang hiện tại và giữ tên đã nhập. Không đặt lịch hợp lệ. Không ghi nhận console error trong tab ở lượt chốt.
- Query giả `/?booking=success` và `/patient?updated=1` không hiển thị thông báo thành công.
- Lần rà soát trước ghi nhận VS Code Problems provider báo unresolved Jakarta dù 58 source biên dịch pass với Ant/NetBeans. Lượt ngày 09/11 chưa chạy lại Problems provider; không coi cảnh báo IDE đó là kết quả runtime mới.
- URL các trang và luồng dữ liệu role-specific chưa thể kiểm thử end-to-end do không có tài khoản test Doctor/Receptionist/Manager/MedicalStaff. Không thực hiện POST nghiệp vụ hợp lệ để tránh ghi dữ liệu thử vào database thật.

## Lỗi còn cần sửa sau kiểm tra 09/11/2026

1. **Tìm kiếm ảnh hưởng form đặt lịch:** `HomeServlet` dùng chung attribute `doctors` cho kết quả tìm kiếm và dropdown của form trong `home.jsp`. Khi tìm không khớp, cả lựa chọn đặt lịch biến mất và thông báo rỗng mô tả sai tình huống. Cần tách nguồn dữ liệu dropdown khỏi danh sách đã lọc, báo không tìm thấy kết quả và có cách xóa bộ lọc rõ ràng.
2. **Validation API slot phân loại lỗi sai:** GET `/slots?doctorId=1&date=not-a-date&shift=AM` trả 503 và ghi `DateTimeParseException` vào log; `shift=NIGHT` trả 200 với `[]`. Cần kiểm tra doctorId/ngày/ca trước DAO, trả 400 cho input sai; giữ 503 cho sự cố tải dữ liệu thật. Stack trace mới ở `localhost.2026-11-09.log` lúc 00:09:08 là từ phép thử ngày sai này, không phải lỗi JSP.

Lượt kiểm tra này cập nhật báo cáo/README; chưa sửa hai lỗi nghiệp vụ/giao diện nêu trên.

## Schema/config đã đọc và điểm chưa thống nhất

- SQL Server dùng database `HospitalManagementDB` qua JNDI Tomcat, URL đang cấu hình `jdbc:sqlserver://localhost:51433;databaseName=HospitalManagementDB;encrypt=true;trustServerCertificate=true;loginTimeout=5`. Kiểm tra chỉ đọc lấy cấu hình từ JNDI cục bộ; không đưa mật khẩu vào source, câu lệnh, log hoặc báo cáo và không sửa file `.env.example` đính kèm. URL này khác instance `SQLEXPRESS04` trong file đính kèm; không dùng giá trị `.env.example` thay cho JNDI đang chạy.
- Metadata đã xác nhận: `Patients.AccessCode` là `varchar(50)`, `Phone` unique; `Bills.AppointmentId` unique; `Payments.BillId` unique; `SystemLogs.UserId` nullable. Không có schema SQL dump trong workspace; metadata đọc trực tiếp, không chạy DDL.
- Snapshot SELECT ngày 09/11/2026: `Patients=2`, `Appointments=2`, `MedicalRecords=0`, `Prescriptions=0`, `Medicines=1` (đang hoạt động: 1), `MedicalServices=0`, `MedicalServiceOrders=0`, `Bills=0`, `Payments=0`, `LeaveRequests=1`; có 5 bác sĩ công khai. Lượt kiểm tra không chèn/sửa/xóa dữ liệu nghiệp vụ.
- Danh mục hiện đã có 1 thuốc active. Mục Khoa của form Dịch vụ không phải danh mục thuốc; thuốc được quản lý qua Manager → Danh mục thuốc và kê ở trang Doctor → Đơn thuốc của lượt khám đang tiến hành. `MedicalRecords=0`, nên chưa có hồ sơ khám để kiểm chứng kê đơn hợp lệ trên DB hiện tại; chưa dùng phiên Doctor để xác nhận dropdown.
- Roles thực tế: `Manager`, `Doctor`, `Receptionist`, `MedicalStaff`. Không gộp hoặc thêm Cashier/Lab Technician.
- SRS nói phí thuốc trong billing, trong khi SDS v3 và schema Bills chỉ mô tả phí khám + dịch vụ; cần chốt quy tắc/schema trước khi cộng tiền thuốc.
- SRS mô tả lịch lặp, còn luồng hiện tại chỉ công bố lịch theo ngày với ca sáng/chiều. “Phản hồi” không có bảng lưu; SMS/email/cổng thanh toán không có provider. Các mục này chưa triển khai.

## Kiểm thử tích hợp còn bị chặn

- Chưa test hai request booking đồng thời, approve leave có lượt đang khám, check-in, lập bill và thu tiền bằng transaction thật vì chưa có tài khoản và bộ dữ liệu được phép thay đổi. Slot hiện có nhưng không dùng lịch thật làm fixture. Logic conditional update/transaction chỉ được kiểm tra ở mức helper hoặc code review.
- Chưa dùng phiên đúng role để xác minh từng URL được phép và URL trái role; request chưa đăng nhập đã được kiểm tra bị chuyển về login.
- Phê duyệt tự động từ chối lệnh dự kiến POST sửa/hủy `appointmentId=1` trong phiên bệnh nhân chưa xác minh: nếu phân quyền có lỗi, phép thử có thể thay đổi lịch thật. Lệnh đó không được thực thi; sau đó chỉ chạy các GET. Cần tài khoản và lịch thử được xác nhận là có thể thay đổi để hoàn tất phép kiểm tra này.
- Các luồng DB ghi thật chỉ nên test tiếp bằng tài khoản và dữ liệu sandbox được cấp riêng. Không dùng bệnh nhân thật làm fixture.

## Chạy lại

```powershell
& 'C:\Program Files\NetBeans-17\netbeans\extide\ant\bin\ant.bat' clean test war jsp-check deploy
```

Mở project con có `build.xml` trong NetBeans. URL hiện tại: `http://localhost:8080/HospitalManagement/`.
