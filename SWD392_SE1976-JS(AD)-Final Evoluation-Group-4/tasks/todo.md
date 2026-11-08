# Việc cần làm

- [x] Fix ClassCastException tại reception-content.jsp và thêm HTML escaping cho toàn bộ thông báo hệ thống.
- [x] Chuẩn hóa DoctorProfileValidator (độ dài, fee regex DECIMAL(18,2)), SERIALIZABLE transaction và cô lập phạm vi lỗi hiển thị Manager.
- [x] Doctor nạp và hiển thị đồng thời cả đơn thuốc và chỉ định dịch vụ; thêm nút "Xem hồ sơ" cho ca khám đã COMPLETED.
- [x] MedicalStaff xem kết quả chỉ định lịch sử đã kết thúc ở chế độ chỉ đọc.
- [x] Lễ tân và Bệnh nhân hủy lịch hẹn an toàn, nhả slot có kiểm tra giao dịch; chống Race Condition khi fetch slot khám.
- [x] Bổ sung 6 unit tests cho DoctorProfileValidator trong TestSuite.java (đạt 61 checks pass).
- [x] Tomcat Jasper precompile kiểm tra 100% JSP với 0 lỗi và tự động deploy war.
