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
- Doctor: hàng đợi/lượt khám, hồ sơ khám, đơn thuốc, chỉ định dịch vụ; hoàn tất lượt khám chỉ khi chỉ định đã hoàn tất và kết quả đã phát hành. Kê đơn chỉ dùng thuốc đang hoạt động trong bảng `Medicines`, và chỉ sau khi mở lượt khám có hồ sơ `IN_PROGRESS`. Bill `UNPAID` được tạo trong transaction hoàn tất lượt khám.
- Manager: quản lý bác sĩ, dịch vụ và danh mục thuốc (hai danh mục độc lập), duyệt đơn nghỉ, báo cáo hoạt động/doanh thu. Thuốc phải được thêm/kích hoạt ở “Danh mục thuốc” thì mới xuất hiện trong lựa chọn kê đơn; không có thuốc mẫu. “Phản hồi” vẫn hiển thị là chưa triển khai vì schema hiện tại không có bảng lưu phản hồi.
- Receptionist: lọc lịch hẹn, check-in trong ngày với số thứ tự, đăng ký tại quầy, tra cứu hồ sơ theo số điện thoại/số giấy tờ/mã hồ sơ, đơn nghỉ của mình và thu tiền mặt từ bill `UNPAID`/biên nhận.
- MedicalStaff: danh sách chỉ định, nhập/phát hành kết quả và đơn nghỉ của mình.
- Đơn nghỉ của Doctor/Receptionist/MedicalStaff được giới hạn theo người gửi; đơn Pending có thể sửa và kiểm tra trùng. Manager không thể tự duyệt đơn của chính mình. Khi duyệt nghỉ Doctor, lượt đang khám chặn quyết định; lịch `BOOKED`/`CHECKED_IN` chuyển `NEEDS_PROCESSING` trong transaction. Đơn nghỉ nhân viên khác không khóa lịch bác sĩ.
- Cổng bệnh nhân riêng `/patient`: yêu cầu số điện thoại/email đã đăng ký và mã truy cập; chỉ cho xem lịch sau xác minh. Mã mới được cấp một lần sau đăng ký và chỉ lưu hash. Lịch `BOOKED` tương lai có thể sửa lý do; đổi lịch cần còn hơn 12 giờ và tối đa hai lần, giữ/nhả slot trong transaction, ghi lịch sử.

## Giới hạn và việc cần xác minh thêm

- SRS nhắc phí thuốc, còn SDS v3 và bảng `Bills` chỉ có phí khám và dịch vụ. Bill hiện tính phí khám cộng các dịch vụ có kết quả `RELEASED`; chưa tính thuốc cho tới khi nghiệp vụ/schema thống nhất.
- Hệ thống hiện chỉ có role `Manager`, `Doctor`, `Receptionist`, `MedicalStaff`; không tự tạo `Cashier` hay `Lab Technician`. Thanh toán tại quầy là tiền mặt, chưa tích hợp VNPay/MoMo. Chưa có dịch vụ SMS/email cho thông báo nghỉ.
- Lễ tân có thể tái sử dụng hồ sơ khi tên và ngày sinh khớp số điện thoại, nhưng đây không phải xác minh danh tính mạnh. Hồ sơ cũ chưa được bàn giao mã truy cập phải xác minh tại quầy; hệ thống không cấp lại mã chỉ dựa trên số điện thoại/email.
- Không dùng dữ liệu giả để thử giao dịch. DB đã kiểm tra có `Appointments`/`Patients` nhưng hiện không có bill, payment hay dịch vụ/chỉ định để xác minh các luồng ghi thành công trên dữ liệu thật.
- Đặt lịch công khai chỉ nhận slot đã được công bố và số điện thoại chưa gắn hồ sơ. Nếu bệnh nhân đã có hồ sơ, yêu cầu lễ tân xác minh; không cho dùng riêng số điện thoại để truy cập hồ sơ. Trong lần kiểm tra gần nhất, ảnh lỗi có ngày sinh ở tương lai và chọn ca chiều chưa công bố lịch; chọn ca sáng có slot thật.
- Chưa có tài khoản kiểm thử Manager/Doctor nên chưa kiểm chứng end-to-end việc thêm thuốc rồi thấy thuốc ở form kê đơn. Nếu danh mục chưa có thuốc `IsActive=1`, form bác sĩ hiển thị trạng thái rỗng và không cho gửi đơn thuốc.
- Môi trường hiện không có tài khoản test cho từng role. Runtime đã xác nhận trang công khai, slot rỗng, lỗi đăng nhập và chặn request chưa đăng nhập; chưa xác nhận từng dashboard/URL sai role bằng phiên đăng nhập tương ứng.
- Gửi SMS, lịch lặp tuần/tháng, phản hồi, cổng thanh toán online và xác minh OTP chưa triển khai do thiếu provider/schema/role hoặc nghiệp vụ được xác nhận.

## Kiểm tra gần nhất

`ant clean test war jsp-check deploy` thành công: 55 kiểm tra pass, Tomcat Jasper báo `Generation completed with [0] errors`, WAR được chép vào Tomcat. Trang chủ trả HTTP 200; tài nguyên `/assets/app.js?v=2` trả `text/javascript;charset=UTF-8` và nội dung UTF-8 khớp file trong workspace. Trên DB hiện tại, `/slots` cho bác sĩ 1 ngày 09/11/2026 trả 8 slot ca sáng; ca chiều trả rỗng với trạng thái `NOT_PUBLISHED`. Trình duyệt hiển thị đúng tiếng Việt; chọn ca sáng nạp 8 lựa chọn, chọn ca chiều khóa trường giờ và giải thích bác sĩ chưa công bố lịch. POST đặt lịch với ngày sinh tương lai, số điện thoại sai và thiếu slot bị từ chối, giữ lại nội dung đã nhập và hiện lỗi gần trường; không đi vào thao tác ghi. Chưa gửi POST đặt lịch hợp lệ để tránh tạo dữ liệu thử trong DB thật. Chi tiết và giới hạn kiểm chứng được ghi trong [BAO_CAO_RA_SOAT.md](./BAO_CAO_RA_SOAT.md).
