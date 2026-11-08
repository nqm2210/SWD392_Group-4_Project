# Việc cần làm

- [x] Danh mục dịch vụ Manager: xem, tạo, sửa, bật/tắt và audit. Trang đã render trên Tomcat; chưa kiểm thử ghi vì bảng hiện không có dịch vụ để sửa và không thêm dữ liệu mẫu.
- [x] Duyệt đơn nghỉ Manager: duyệt/từ chối, khóa slot và gắn lịch hẹn bị ảnh hưởng vào trạng thái cần xử lý.
- [x] Doctor tạo/xem yêu cầu nghỉ của chính mình; validation đã kiểm tra bằng Ant.
- [x] Receptionist đăng ký walk-in, tra cứu lịch hẹn, check-in và cấp số hàng đợi; các JSP qua JSPC, chưa kiểm thử lưu bằng phiên Receptionist.
- [x] Doctor xem hàng đợi/lượt khám của mình, bắt đầu lượt, lưu chẩn đoán/ghi chú/kế hoạch và hoàn tất hồ sơ theo transaction.
- [x] Doctor thêm thuốc vào đơn và tạo chỉ định dịch vụ theo hồ sơ đang khám; thao tác có transaction và kiểm tra danh mục đang hoạt động.
- [x] MedicalStaff xem các chỉ định dịch vụ và lưu nháp/phát hành kết quả theo transaction; schema chưa có bảng phân công theo nhân viên.
- [x] Manager xem báo cáo hoạt động và doanh thu từ dữ liệu thật; truy vấn đã đối chiếu tên cột SQL Server.
- [x] Tomcat: Manager login, dashboard, báo cáo, empty states; Manager bị 403 tại URL Doctor/Receptionist/MedicalStaff. Chưa có phiên các role kia để thử luồng dương.
- [x] Ghi nhận giới hạn: feedback, Cashier/Lab Technician role riêng, payment gateway và thông báo ngoài chưa có schema/cấu hình.

## Chờ dữ liệu/phiên kiểm thử

- [x] Doctor kê đơn/chỉ định dùng danh mục hoạt động và hồ sơ IN_PROGRESS, thao tác transaction; chưa thể kiểm thử ghi vì danh mục hiện rỗng.
