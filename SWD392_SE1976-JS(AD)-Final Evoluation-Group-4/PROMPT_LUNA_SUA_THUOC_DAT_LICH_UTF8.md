# Prompt sửa thuốc, đặt lịch và lỗi UTF-8 cho Luna / VS Code

Bạn là lập trình viên Java Web chịu trách nhiệm sửa trực tiếp project hiện có. Hãy thực hiện code và kiểm chứng, không chỉ trả lời kế hoạch hoặc mã minh họa.

## 1. Project và giới hạn bắt buộc

Project chính:
`H:\SWD392_SE1976-JS(AD)-Final Evoluation-Group-4\SWD392_Group-4_Project\SWD392_SE1976-JS(AD)-Final Evoluation-Group-4`

Đọc trước khi sửa:

- `README.md`, `BAO_CAO_RA_SOAT.md`, hướng dẫn `AGENTS.md` nếu có.
- SRS: `C:\Users\quang\Downloads\SWD392 - Report 1 (Overview & SRS) - SE1976-JS(AD)  - Group 6  _v3.0.docx`.
- SDS: `C:\Users\quang\Downloads\SWD392 - Report 2 (SDS) - SE1976-JS(AD) - Group 4 _v3.0.docx`.
- Mã Servlet/DAO/JSP/JS, schema SQL Server thực tế và log Tomcat liên quan.

Giữ Java 17, Jakarta Servlet/JSP, HTML/CSS/JavaScript thuần, JDBC SQL Server, Apache Ant, NetBeans, Tomcat 10.1. Website hiện chạy tại `http://localhost:8080/HospitalManagement/`.

Không tạo project/database mới, đổi framework, chạy migration, sửa/xóa bảng, tự thêm role hoặc chèn dữ liệu mẫu. Không sửa file đính kèm chỉ đọc, reset mật khẩu, in mật khẩu/hash, hoặc ghi bí mật vào source. Giữ các thay đổi hiện có của người khác. Chỉ sửa source trong project; không chỉnh trực tiếp bản exploded trong Tomcat để coi là đã sửa source.

## 2. Bằng chứng đã xác minh ngày 08/11/2026

Các dữ kiện dưới đây là kết quả đọc source, metadata SQL Server và gọi HTTP. Hãy kiểm tra lại vì dữ liệu có thể thay đổi.

1. `Medicines=0`, `MedicalServices=0`; `Bills=0`, `Payments=0`, `MedicalServiceOrders=0`. Có 3 Schedules và 24 ScheduleSlots.
2. Với `DoctorId=1`, ngày `2026-11-09`: ca sáng `PUBLISHED`, có 8 slot trống; ca chiều không có lịch. API `/slots?...&shift=AM` trả 8 slot; `shift=PM` trả `[]`. Vì vậy ảnh chọn ca chiều không có slot chưa chứng minh lỗi ghi appointment.
3. Ảnh đặt lịch nhập ngày sinh 09/11/2026, là ngày tương lai so với 08/11/2026. Server đang từ chối ngày sinh tương lai; giao diện cần phát hiện sớm và báo rõ.
4. Các file `web/assets/app.js`, `build/web/assets/app.js` và file exploded trong Tomcat có chữ UTF-8 đúng, dài 3918 byte. HTTP `/assets/app.js` trả `text/javascript; charset=UTF-8` nhưng body dài 4107 byte, có `KhÃ´ng...`, không khớp source. Biến đổi source qua bộ giải mã Windows-1252 kiểu JDK (byte không định nghĩa thành U+FFFD), rồi mã hóa UTF-8 cho ra đúng body HTTP lỗi. Đây là lỗi chuyển mã khi phục vụ tài nguyên tĩnh, không phải chỉ thiếu font hay thiếu `<meta charset>`.
5. `EncodingFilter` map `/*` và gọi `response.setCharacterEncoding("UTF-8")` với cả tài nguyên tĩnh. DefaultServlet Tomcat chưa có init-param `fileEncoding` trong cấu hình mặc định đã đọc. Tài liệu Apache xác nhận `fileEncoding` mặc định là encoding của platform: https://tomcat.apache.org/tomcat-10.1-doc/default-servlet.html . Cần kiểm tra phối hợp filter/DefaultServlet thay vì re-encode hàng loạt source đang đúng.
6. `/services` ghi `MedicalServices`; dropdown thuốc của Doctor đọc `Medicines WHERE IsActive=1`. Hiện không có luồng INSERT/UPDATE danh mục Medicines hoặc menu quản lý thuốc. Thêm “thuốc” vào MedicalServices không làm nó xuất hiện trong danh sách thuốc.
7. Ảnh nhập `Thuốc giảm đau` vào **Mã dịch vụ** bị validator từ chối đúng theo regex `[A-Za-z0-9][A-Za-z0-9_-]{0,19}`. Mã kỹ thuật và tên hiển thị là hai trường khác nhau. Dropdown **Khoa** là danh sách Departments, không phải danh sách thuốc.
8. Form thêm dịch vụ không gửi trường `active`; `ServiceServlet` truyền `req.getParameter("active") != null` vào DAO. DAO INSERT rõ `IsActive=false`, ghi đè default 1 của DB. Sau khi sửa mã và thêm thành công, dịch vụ mới vẫn bị ẩn khỏi lựa chọn dịch vụ hoạt động.
9. POST rỗng/thiếu `gender` tới `/appointment` đã tái hiện HTTP 500. `AppointmentValidator` gọi `Set.of(...).contains(gender)` với null; `AppointmentServlet` còn gọi validator trước kiểm tra CSRF.
10. `app.js` không có cơ chế hủy/bỏ response cũ: đổi bác sĩ/ngày/ca nhanh có thể để response cũ ghi đè slot mới.
11. Luồng kê đơn hiện chỉ thêm dòng. Chưa thấy truy vấn đọc lại PrescriptionDetails hoặc giao diện danh sách dòng đơn thuốc đã lưu; nhiều input kê thuốc/chỉ định không được giữ khi lỗi. Một số màn còn lấy thông báo “đã lưu” trực tiếp từ `?saved=1`.
12. Roles thực tế: Manager, Doctor, Receptionist, MedicalStaff. Có tài khoản đang hoạt động của 3 role đầu nhưng chưa có mật khẩu test được bàn giao; MedicalStaff hiện có 0 user. Mật khẩu SQL Server/Tomcat không được coi là mật khẩu ứng dụng.

Schema thuốc đã xác nhận, vẫn phải đọc lại trước viết SQL:

- Medicines: `MedicineId int` PK; `MedicineCode varchar(20)` NOT NULL UNIQUE; `MedicineName nvarchar(100)` NOT NULL; `Unit nvarchar(50)` NOT NULL; `UnitPrice decimal(18,2)` NOT NULL; `IsActive bit NULL DEFAULT 1`.
- Prescriptions: `PrescriptionId`, `RecordId` (mỗi hồ sơ tối đa một đơn theo SDS), `DoctorInstructions nvarchar(max)`, `CreatedAt`.
- PrescriptionDetails: `DetailId`, `PrescriptionId`, `MedicineId`, `Quantity int`, `Dosage nvarchar(255)`, `UnitPrice decimal(18,2)` là giá chụp tại lúc kê.
- MedicalServices có `ServiceCode varchar(20)` UNIQUE, `ServiceName nvarchar(100)`, `DepartmentId`, `UnitPrice decimal(18,2)`, `Description nvarchar(255)`, `IsActive`.
- Schema Medicines hiện không có cột tồn kho/hoạt chất/tương tác thuốc. Không tự đoán các cột này.

## 3. Sửa theo thứ tự

### A. Sửa lỗi UTF-8 tài nguyên tĩnh trước

- Xác định đúng Tomcat instance và context đang nghe cổng 8080.
- Đối chiếu byte source, WAR, exploded và HTTP; kiểm tra EncodingFilter, DefaultServlet `fileEncoding`, Content-Type và cache.
- Sửa trong cấu hình/source project bằng cách nhỏ nhất phù hợp Tomcat hiện tại: không buộc chuyển mã tài nguyên tĩnh sai encoding; nếu cấu hình DefaultServlet ở cấp ứng dụng thì dùng UTF-8 và giữ chế độ phục vụ file chỉ đọc. Giữ UTF-8 đúng cho request, JSP, JSON và response động.
- Không re-encode toàn bộ file Java/JSP/JS hoặc dữ liệu SQL khi chúng đang đúng. Không coi đổi font-family, thêm meta charset, `URIEncoding` hoặc `-Dfile.encoding` đơn lẻ là đủ nếu chưa đo lại response thật.
- Sau deploy phải xác nhận body HTTP của app.js đọc UTF-8 ra đúng tiếng Việt và khớp source/deployed asset không bị biến đổi. Kiểm tra cả các chuỗi chỉ xuất hiện sau khi JS chạy: đang tải, không có slot, lỗi tải, thiếu slot, đang xử lý.

### B. Bổ sung danh mục thuốc, tách khỏi dịch vụ

Yêu cầu bổ sung trực tiếp trong prompt này: Manager được quản lý **Danh mục thuốc** bằng role Manager hiện có. Không suy diễn rằng SRS đã định nghĩa đầy đủ quyền quản trị thuốc; ghi quyết định này trong báo cáo.

- Thêm menu/route, Servlet, DAO và JSP tối thiểu cho Manager: xem/tìm thuốc; thêm; sửa mã/tên/đơn vị/giá; bật/tắt hoạt động.
- Dùng bảng Medicines hiện có. Code không dấu, không khoảng trắng, tối đa 20 ký tự; tên/đơn vị hỗ trợ tiếng Việt theo độ dài cột; giá dùng BigDecimal, vừa DECIMAL(18,2). Thuốc mới phải có trạng thái hoạt động rõ ràng, không lặp lại lỗi checkbox bị thiếu của form dịch vụ.
- Kiểm tra trùng MedicineCode ở server/DB; dùng PreparedStatement; ghi audit trong transaction cho thay đổi. Nếu mã trùng, giữ form và báo cạnh trường mã. Không xóa cứng thuốc đã có PrescriptionDetails.
- Kiểm tra quyền ở Filter/Servlet; Doctor/Receptionist/MedicalStaff và anonymous không được sửa danh mục bằng URL/POST trực tiếp.
- Doctor lấy thuốc hoạt động từ Medicines. Thuốc ngừng hoạt động không được thêm mới vào đơn, nhưng các dòng thuốc cũ vẫn hiển thị để bảo toàn lịch sử.
- Không tự nhập danh mục thuốc giả để lấp dropdown. Giao diện phải cho Manager khai báo thuốc thật qua thao tác được người dùng thực hiện.
- Màn “Dịch vụ và giá” chỉ quản lý dịch vụ khám/xét nghiệm/chẩn đoán, không dùng để thay thế Medicines. Không thêm “Thuốc” vào Departments như một cách sửa dropdown.

### C. Sửa form dịch vụ và validation tiền

- Thêm trạng thái hoạt động rõ ràng cho form tạo dịch vụ hoặc gán mặc định active=true cho thao tác tạo theo quy tắc đã công bố; không lấy việc trường checkbox vắng mặt làm inactive ngoài ý muốn. Form sửa vẫn cho bật/tắt.
- Mã dịch vụ cần hướng dẫn/placeholder rõ: mã Latin không dấu, không khoảng trắng, ví dụ `DV_XN_001`; tên dịch vụ mới là nội dung tiếng Việt. Giữ validation mã và unique constraint, không nới mã thành tên tự do để che lỗi nhập sai.
- Sửa kiểm tra giá cho đúng **16 chữ số phần nguyên + 2 chữ số phần thập phân** của DECIMAL(18,2). Kiểm tra chỉ `precision <= 18` và `scale <= 2` hiện vẫn lọt số nguyên 17 chữ số hoặc dạng mũ lớn. Chọn chính sách nhập dạng mũ rõ ràng; không để overflow tới DB mới báo lỗi chung.
- Giá mới chỉ áp dụng dòng phát sinh sau; không sửa snapshot UnitPrice trong đơn/chỉ định đã ghi.

### D. Hoàn thiện chọn lịch thật và thông báo đặt lịch

- Giữ GET `/slots` hoàn toàn chỉ đọc; chỉ trả slot còn chỗ, chưa khóa, lịch PUBLISHED, bác sĩ có quyền nhận lịch và không nghỉ.
- Hiển thị/ngăn chọn ca không có lịch đúng theo DB, hoặc giải thích rõ sau khi chọn ca. Phân biệt tối thiểu “chưa mở lịch”, “không còn slot khả dụng” và “không tải được dữ liệu”; không khẳng định chưa công bố nếu thực tế lịch đã đầy.
- Ca sáng 09/11/2026 của DoctorId=1 hiện phải hiện 8 slot; ca chiều hiện phải báo không có slot. Không tạo lịch/slot để làm bài test xanh; chỉ Doctor có quyền mới được công bố qua luồng đã có.
- Nếu đổi JSON contract `/slots`, phải cập nhật đồng bộ trang chủ, đăng ký tại quầy, cổng bệnh nhân và test. Ưu tiên giữ contract hiện có và thay đổi nhỏ.
- Thêm max ngày sinh bằng ngày hiện tại; kiểm tra lại server. Giải thích định dạng ngày dd/MM/yyyy trong giao diện Việt; giá trị gửi về server vẫn ISO yyyy-MM-dd.
- Hủy request cũ bằng AbortController hoặc dùng số phiên request để chỉ response mới nhất cập nhật dropdown. Xóa lựa chọn slot khi bác sĩ/ngày/ca thay đổi; chỉ khôi phục lựa chọn đã nhập nếu slot vẫn thuộc đúng ngữ cảnh và còn khả dụng.
- Không cho gửi khi thiếu slot; giữ tất cả trường khi lỗi. Lỗi SQL/slot vừa bị đặt không được làm mất form hoặc hiện thành công.
- Kiểm tra luồng patient + appointment + giữ slot trong transaction; chống hai request chiếm một slot và rollback toàn bộ khi thất bại.
- Luồng public hiện chặn số điện thoại đã có hồ sơ là giới hạn bảo vệ dữ liệu, cần hướng dẫn rõ sang xác minh/tiếp nhận. Không bỏ chặn để tái sử dụng bệnh nhân chỉ dựa số điện thoại. Nếu bổ sung đặt thêm lịch cho hồ sơ cũ, phải dùng verifiedPatientId đã xác minh trong session, không nhận PatientId tùy ý từ form.

### E. Chặn HTTP 500 do input thiếu

- Sửa AppointmentValidator null-safe, đặc biệt gender; rà các `Set.of`, `List.of`, `Map.of` cùng input có thể null.
- Kiểm tra CSRF trước luồng validate/DAO phù hợp từng Servlet; CSRF sai trả 403/thông báo phiên hết hạn rõ ràng, không 500. Đầu vào thiếu/sai phải có validation có chủ đích.
- Với request hợp lệ CSRF nhưng thiếu field, hiển thị lỗi cạnh trường và giữ input, không gọi DAO ghi dữ liệu.
- Đọc stack trace mới tạo để xác nhận lỗi gốc thay vì chỉ sửa trang include. Không bọc mọi lỗi bằng catch rồi trả thành công.

### F. Hoàn thiện mức tối thiểu màn kê đơn

- Đọc lại và hiển thị các dòng PrescriptionDetails thật của hồ sơ đang chọn: thuốc, đơn vị, số lượng, liều dùng, đơn giá đã chụp, hướng dẫn.
- Cho bác sĩ sở hữu hồ sơ thêm/sửa/xóa dòng khi trạng thái lượt khám/hồ sơ cho phép. Hồ sơ đã hoàn tất chỉ đọc. Kiểm tra lại ownership và trạng thái trong transaction khi ghi.
- Giữ quantity/dosage/instructions và lựa chọn thuốc/dịch vụ khi validation hoặc DAO lỗi. Không chỉ render lại form trắng.
- Quy định rõ cùng một thuốc thêm lần nữa là gộp/sửa hay một dòng riêng; tránh nhân đôi vô tình do gửi lại POST. Thông báo và danh sách sau commit phải phản ánh DB thật.
- Nếu làm in đơn, dùng bản in HTML/JSP đơn giản từ dữ liệu đã lưu, không báo đã in/hoàn tất giả.
- Chưa có schema tồn kho/hoạt chất/tương tác thuốc thì ghi rõ phần SRS UC-12 đó chưa triển khai; không bịa số tồn, tự động cảnh báo tương tác hay dữ liệu y khoa. Không tự cộng tiền thuốc vào TotalServiceAmount để giả vờ đã thống nhất billing.

### G. Sửa các thông báo còn có thể giả thành công

- Rà ServiceServlet, ScheduleServlet, DoctorWorkflowServlet, LeaveServlet, MedicalStaffServlet, StaffServlet: nhiều chỗ còn set `saved` chỉ từ query `?saved=1`.
- Thay bằng session flash chỉ được ghi sau khi transaction commit, tiêu thụ một lần. Query URL đơn lẻ/refresh không được làm hiện thông báo thao tác vừa lưu thành công.
- Giữ message nghiệp vụ cụ thể nhưng không hiển thị SQL exception, mật khẩu hoặc dữ liệu người khác. AccessCode chỉ được hiển thị ở bước cấp mã một lần sau commit theo luồng đã quy định; không ghi mã vào log/URL hoặc lộ lại trong thông báo lỗi.

## 4. Kiểm chứng bắt buộc, bằng chứng phải phân biệt cấp độ

1. Thêm test tập trung cho: validator null/missing gender; biên giá DECIMAL(18,2); trạng thái dịch vụ/thuốc mới; quyền CRUD thuốc; giữ snapshot giá; các điều kiện lịch/ownership/rollback liên quan.
2. Test raw HTTP app.js và chuỗi runtime tiếng Việt, không chỉ đọc file UTF-8 hoặc kiểm tra JSP. Response phải khớp byte nội dung dự kiến, header hợp lệ, không còn mojibake/replacement character.
3. Browser: đổi nhanh bác sĩ/ngày/ca; ca không có lịch; ca có slot thật; ngày sinh tương lai; thiếu slot; lỗi validation giữ dữ liệu. Không gửi booking hợp lệ vào DB thật nếu chưa có dữ liệu được phép thử.
4. Browser Manager: form thuốc tách khỏi dịch vụ, code tiếng Việt/sai định dạng báo đúng, trạng thái active rõ. Kiểm tra request trái role bằng phiên được cung cấp và anonymous.
5. Khi có tài khoản/dữ liệu thử được người dùng cho phép, thực hiện và đối chiếu bản ghi trước/sau: tạo thuốc qua UI -> Doctor thấy đúng thuốc -> lưu dòng đơn -> đọc lại -> đổi giá danh mục không làm đổi dòng cũ; công bố lịch qua Doctor -> bệnh nhân đặt slot -> hai request cùng slot chỉ một thành công; rollback không để hồ sơ/lịch/slot dở dang.
6. Không tự seed thuốc/bệnh nhân/tài khoản hoặc reset password để đủ fixture. Dùng thông tin test được cấp; nếu thiếu, hoàn tất mọi phần độc lập và báo đúng bài test nào chưa chạy. UserMedicalStaff=0 là một blocker riêng; không gọi đó là đã test role.
7. Build từ thư mục project:

```powershell
& 'C:\Program Files\NetBeans-17\netbeans\extide\ant\bin\ant.bat' clean test war jsp-check
& 'C:\Program Files\NetBeans-17\netbeans\extide\ant\bin\ant.bat' deploy
```

8. Jasper phải báo 0 lỗi; kiểm tra Tomcat cổng thực tế 8080 và log mới sau deploy. Test/helper/JSP compile pass không thay thế việc chạy Servlet và kiểm tra transaction thật.
9. Số kiểm tra trong README/báo cáo phải khớp số chạy thực tế, không chỉ đổi chuỗi `PASS: ...` để tạo bằng chứng.

## 5. Bàn giao

- Cập nhật `BAO_CAO_RA_SOAT.md` và README: file đã sửa/lý do; kết quả build/Jasper/HTTP/browser/DB riêng biệt; các bước tái hiện trước/sau.
- Nêu rõ module đã code và module đã được xác minh end-to-end; không đồng nhất hai trạng thái đó.
- Liệt kê phần vẫn thiếu và nguyên nhân: billing tiền thuốc SRS/SDS chưa thống nhất; stock/tương tác/hoạt chất chưa có schema; SMS/email/payment gateway chưa có provider; feedback chưa có bảng; lịch lặp chưa có thiết kế phù hợp. Các phần này không được tự mở rộng schema/role trong lần sửa này.
- Báo đường dẫn project, command Ant, URL và cách kiểm thử tiếp. Tiếp tục phần đã được giao cho đến khi hoàn tất; chỉ hỏi khi cần quyết định nghiệp vụ/schema/quyền thực sự chưa được phép.
