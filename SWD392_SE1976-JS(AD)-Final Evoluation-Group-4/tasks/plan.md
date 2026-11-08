# Kế hoạch hoàn thiện nghiệp vụ bệnh viện

## Cơ sở và giới hạn

SRS v3 mô tả luồng bệnh nhân, bác sĩ, lễ tân, kỹ thuật viên xét nghiệm, thu ngân, nhân viên quản trị và quản lý. Database đang chạy có các role `Manager`, `Doctor`, `Receptionist`, `MedicalStaff`; không có role `Cashier` hoặc `Lab Technician` riêng. Bản triển khai sẽ dùng role thật trong bảng `Roles`, không gộp hay tự tạo role.

Schema hiện có các bảng cho lịch hẹn, bệnh nhân, khám bệnh, đơn thuốc, dịch vụ, kết quả dịch vụ, thanh toán, báo cáo nguồn và audit. Chưa có bảng phản hồi, gói dịch vụ, queue riêng hay hệ thống notification/payment gateway. Không chạy migration hoặc thêm dữ liệu mẫu.

Lịch trong schema là theo ngày (`Schedules.WorkDate`); không có cấu trúc lặp tuần/tháng. Ca bệnh nhân chọn sẽ tạo schedule/slots theo ngày đã chọn. Các chức năng phải kiểm tra role ở server, dùng `PreparedStatement`, CSRF cho POST có thay đổi và transaction cho cập nhật nhiều bảng.

## Các lát triển khai

### Giai đoạn 1: Quản lý và tiếp nhận

1. **Danh mục dịch vụ và giá (đã triển khai):** Manager xem, tạo, sửa, bật/tắt dịch vụ bằng `MedicalServices`; ghi audit vào `SystemLogs`. Bảng hiện rỗng nên chưa có bản ghi để kiểm tra thao tác cập nhật.
2. **Duyệt đơn nghỉ (đã triển khai):** Manager xem yêu cầu, duyệt/từ chối và ghi người quyết định; khi duyệt khóa slot trống, hủy schedule tương ứng và chuyển lịch hẹn bị ảnh hưởng sang `NEEDS_PROCESSING` cho Receptionist phối hợp. Không có dịch vụ notification.
3. **Lịch hẹn, check-in và hàng đợi (đã triển khai, cần phiên Receptionist để kiểm tra UI):** Receptionist tra cứu lịch theo ngày/trạng thái, check-in có khóa transaction, cấp `QueueNumber` theo bác sĩ/ngày.
4. **Đăng ký walk-in:** tạo bệnh nhân và lịch hẹn/check-in chỉ khi các trường bắt buộc và khả năng slot hiện có hỗ trợ luồng đó; giữ transaction và audit.

### Giai đoạn 2: Bác sĩ và chuyên môn

5. **Đơn nghỉ Doctor:** tạo và xem yêu cầu nghỉ của chính tài khoản đang đăng nhập.
6. **Hàng đợi và hồ sơ khám (đã triển khai):** Doctor chỉ xem lịch hẹn thuộc tài khoản, bắt đầu lượt đã check-in, lưu chẩn đoán/ghi chú/kế hoạch vào `MedicalRecords`, rồi hoàn tất lịch và hồ sơ trong transaction.
7. **Đơn thuốc và chỉ định dịch vụ:** lưu `Prescriptions`, `PrescriptionDetails` và `MedicalServiceOrders` trong transaction; kiểm tra bác sĩ sở hữu hồ sơ.
8. **Kết quả dịch vụ (đã triển khai):** `MedicalStaff` xem `MedicalServiceOrders`, lưu nháp/phát hành `MedicalServiceResults` và cập nhật trạng thái chỉ định trong transaction. Vì không có bảng phân công, role MedicalStaff có thể xem mọi chỉ định; chưa thể giới hạn theo nhóm/nhân viên.

### Giai đoạn 3: Quản lý, báo cáo và kiểm tra

9. **Báo cáo Manager (đã triển khai):** lọc khoảng ngày và tổng hợp lịch hẹn, doanh thu theo khoa/phương thức từ dữ liệu nguồn; dùng `Payments.PaidAt` và `Payments.AmountPaid` theo schema thật.
10. **Audit và kiểm thử vai trò:** các luồng ghi có audit; Ant 20 checks và JSPC toàn bộ JSP qua; Tomcat xác nhận Manager report và 403 tại URL Doctor/Receptionist/MedicalStaff. Các luồng dương của ba role đó cần tài khoản test để xác minh.
11. **Đánh giá chức năng bị chặn:** feedback không có bảng lưu; cashier/lab technician không có role riêng; cổng thanh toán và notification bên ngoài chưa cấu hình. Báo rõ phần nào cần schema hoặc dịch vụ ngoài.

## Thứ tự phụ thuộc

Danh mục dịch vụ và lịch nghỉ có thể hoàn thành độc lập. Check-in cần khóa lịch hẹn và cấp số queue. Khám bệnh phụ thuộc check-in; kê đơn/chỉ định phụ thuộc hồ sơ khám; nhập kết quả phụ thuộc chỉ định. Báo cáo phụ thuộc dữ liệu đã ghi từ các luồng trên.

## Tiêu chí hoàn thành mỗi lát

- Có trang và luồng ghi/đọc thật từ SQL Server, dùng trường đang tồn tại.
- Sai role truy cập trực tiếp bị trả 403; đầu vào sai hiển thị lỗi và giữ dữ liệu hợp lệ.
- Thay đổi nhiều bảng dùng transaction; thay đổi nghiệp vụ quan trọng ghi audit khi schema hỗ trợ.
- `ant clean test war` thành công; kiểm tra HTTP trên Tomcat trước khi báo hoạt động.
