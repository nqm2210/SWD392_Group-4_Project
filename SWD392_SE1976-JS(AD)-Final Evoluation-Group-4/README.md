# Hospital Management Web

Dự án Java Web hiện có, dùng Java 17, Jakarta Servlet/JSP, JDBC SQL Server, Apache Ant, NetBeans và Tomcat 10.1. Không dùng Maven/Spring/React và không cần tạo database mới.

## Mở và chạy

Mở thư mục `SWD392_SE1976-JS(AD)-Final Evoluation-Group-4` (thư mục có `build.xml` và `nbproject`) trong NetBeans 17 bằng **Open Project**.

Trong PowerShell ở thư mục dự án:

```powershell
& 'C:\Program Files\NetBeans-17\netbeans\extide\ant\bin\ant.bat' clean test war jsp-check
& 'C:\Program Files\NetBeans-17\netbeans\extide\ant\bin\ant.bat' deploy
```

Hoặc chạy các target tương ứng từ cửa sổ Ant trong NetBeans. Target `jsp-check` dùng Jasper của Tomcat để biên dịch tất cả JSP trước khi deploy; build Java thành công riêng lẻ không thay thế kiểm tra này. Tomcat trong môi trường hiện tại nghe cổng `8080`; URL là `http://localhost:8080/HospitalManagement/`.

Đặt đường dẫn Tomcat và JDBC driver trong `nbproject/private/local.properties` (không commit file riêng tư). Có thể tạo file đó từ `nbproject/local.properties.example`. Mật khẩu DB phải được cung cấp cho tiến trình Tomcat bằng biến môi trường/JNDI; không ghi vào source. `.env` không được Tomcat tự nạp.

## Đã triển khai

- Trang chủ công khai, danh sách bác sĩ đang hoạt động/được công bố, tìm kiếm, thông báo rỗng/lỗi và form đặt lịch.
- Popup đăng nhập nhân viên, HttpSession, lỗi đăng nhập tại trang chủ, đăng xuất và phân quyền phía server.
- Đặt lịch dùng slot thật từ lịch `PUBLISHED`; GET `/slots` chỉ đọc. Không có lịch thì trả `[]`. Đặt lịch cập nhật slot bằng điều kiện chưa đặt và transaction để chống hai lượt chiếm cùng slot.
- Doctor công bố lịch theo ngày, ca sáng/chiều qua các bảng lịch hiện có; chưa có lịch tuần/tháng.
- Doctor: hàng đợi/lượt khám, hồ sơ khám, đơn thuốc, chỉ định dịch vụ; xem lại hồ sơ các ca khám đã hoàn tất (chẩn đoán, ghi chú, đơn thuốc, chỉ định/kết quả); hoàn tất lượt khám chỉ khi chỉ định đã hoàn tất và kết quả đã phát hành. Kê đơn chỉ dùng thuốc đang hoạt động trong bảng `Medicines`, và chỉ sau khi mở lượt khám có hồ sơ `IN_PROGRESS`. Bill `UNPAID` được tạo trong transaction hoàn tất lượt khám.
- Manager: quản lý bác sĩ (bảo vệ transaction SERIALIZABLE, kiểm tra trùng lặp email và audit log), dịch vụ và danh mục thuốc (hai danh mục độc lập), duyệt đơn nghỉ, báo cáo hoạt động/doanh thu. Thuốc phải được thêm/kích hoạt ở “Danh mục thuốc” thì mới xuất hiện trong lựa chọn kê đơn; không có thuốc mẫu. “Phản hồi” vẫn hiển thị là chưa triển khai vì schema hiện tại không có bảng lưu phản hồi.
- Receptionist: lọc lịch hẹn, hủy lịch hẹn kèm giải phóng slot an toàn, check-in trong ngày với số thứ tự, đăng ký tại quầy, tra cứu hồ sơ theo số điện thoại/số giấy tờ/mã hồ sơ, đơn nghỉ của mình và thu tiền mặt từ bill `UNPAID`/biên nhận.
- MedicalStaff: danh sách chỉ định, nhập/phát hành kết quả, tra cứu chi tiết kết quả chỉ định lịch sử và đơn nghỉ của mình.
- Đơn nghỉ của Doctor/Receptionist/MedicalStaff được giới hạn theo người gửi; đơn Pending có thể sửa và kiểm tra trùng. Manager không thể tự duyệt đơn của chính mình. Khi duyệt nghỉ Doctor, lượt đang khám chặn quyết định; lịch `BOOKED`/`CHECKED_IN` chuyển `NEEDS_PROCESSING` trong transaction. Đơn nghỉ nhân viên khác không khóa lịch bác sĩ.
- Cổng bệnh nhân riêng `/patient`: yêu cầu số điện thoại/email đã đăng ký và mã truy cập; chỉ cho xem lịch sau xác minh. Mã mới được cấp một lần sau đăng ký và chỉ lưu hash. Lịch `BOOKED` tương lai có thể sửa lý do hoặc hủy lịch trực tiếp (giải phóng slot); đổi lịch cần còn hơn 12 giờ và tối đa hai lần, giữ/nhả slot trong transaction, ghi lịch sử.
- Chống Race Condition trong lấy slot lịch khám bằng `AbortController` và `requestVersion` trên cả cổng bệnh nhân và màn hình tiếp nhận tại quầy.

## Giới hạn và việc cần xác minh thêm

- SRS nhắc phí thuốc, còn SDS v3 và bảng `Bills` chỉ có phí khám và dịch vụ. Bill hiện tính phí khám cộng các dịch vụ có kết quả `RELEASED`; chưa tính thuốc cho tới khi nghiệp vụ/schema thống nhất.
- Hệ thống hiện chỉ có role `Manager`, `Doctor`, `Receptionist`, `MedicalStaff`; không tự tạo `Cashier` hay `Lab Technician`. Thanh toán tại quầy là tiền mặt, chưa tích hợp VNPay/MoMo. Chưa có dịch vụ SMS/email cho thông báo nghỉ.
- Lễ tân có thể tái sử dụng hồ sơ khi tên và ngày sinh khớp số điện thoại, nhưng đây không phải xác minh danh tính mạnh. Hồ sơ cũ chưa được bàn giao mã truy cập phải xác minh tại quầy; hệ thống không cấp lại mã chỉ dựa trên số điện thoại/email.
- Không dùng dữ liệu giả để thử giao dịch. DB đã kiểm tra có `Appointments`/`Patients` nhưng hiện không có bill, payment hay dịch vụ/chỉ định để xác minh các luồng ghi thành công trên dữ liệu thật.
- Đặt lịch công khai chỉ nhận slot đã được công bố và số điện thoại chưa gắn hồ sơ. Nếu bệnh nhân đã có hồ sơ, yêu cầu lễ tân xác minh; không cho dùng riêng số điện thoại để truy cập hồ sơ. Trong lần kiểm tra gần nhất, ảnh lỗi có ngày sinh ở tương lai và chọn ca chiều chưa công bố lịch; chọn ca sáng có slot thật.
- Chưa có tài khoản kiểm thử Manager/Doctor nên chưa kiểm chứng end-to-end việc thêm thuốc rồi thấy thuốc ở form kê đơn. SELECT ngày 09/11/2026 xác nhận hiện có 1 thuốc `IsActive=1`, nhưng chưa có MedicalRecords để thử kê đơn hợp lệ. Nếu danh mục không có thuốc active, form bác sĩ hiển thị trạng thái rỗng và không cho gửi đơn thuốc.
- Môi trường hiện không có tài khoản test cho từng role. Runtime đã xác nhận trang công khai, slot rỗng, lỗi đăng nhập và chặn request chưa đăng nhập; chưa xác nhận từng dashboard/URL sai role bằng phiên đăng nhập tương ứng.
- Gửi SMS, lịch lặp tuần/tháng, phản hồi, cổng thanh toán online và xác minh OTP chưa triển khai do thiếu provider/schema/role hoặc nghiệp vụ được xác nhận.

## Kiểm tra gần nhất

Ngày 09/11/2026 chạy lại `ant test` và `ant war jsp-check`: 61 kiểm tra pass, Jasper biên dịch toàn bộ JSP không lỗi. Bản clean/build/deploy trước đó ngày 08/11 cũng thành công. Đối chiếu 19 file web với Tomcat không thấy khác biệt ở thời điểm chốt. Trang chủ/JavaScript phục vụ UTF-8; kiểm tra trình duyệt thấy tiếng Việt đúng, popup mở/đóng bằng X/Esc, focus bàn phím đúng và tài khoản sai hiện lỗi trong popup. Liên kết bệnh nhân sử dụng cùng tab; 16/16 URL nội bộ chưa đăng nhập chuyển về login.

Ca sáng bác sĩ 1 ngày 09/11 trả 7 slot; ca chiều rỗng, khóa select và giải thích chưa công bố lịch. Form thiếu slot bị chặn và giữ nội dung; mã bệnh nhân sai không mở lịch sử. Còn hai lỗi đã xác nhận: tìm kiếm không khớp làm rỗng cả dropdown Bác sĩ của form đặt lịch và dùng thông báo rỗng sai; API `/slots` chưa trả đúng HTTP 400 cho input không hợp lệ. Chưa gửi POST nghiệp vụ hợp lệ hoặc dùng phiên đúng role để kiểm tra dữ liệu thật. Chi tiết bằng chứng, dữ liệu hiện tại và phần bị chặn nằm trong [BAO_CAO_RA_SOAT.md](./BAO_CAO_RA_SOAT.md).
