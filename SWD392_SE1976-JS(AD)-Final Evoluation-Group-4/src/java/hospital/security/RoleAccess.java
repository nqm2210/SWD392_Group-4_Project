package hospital.security;
import java.util.*;
public final class RoleAccess {
    private static final Map<String,List<String>> MENUS=Map.of(
        "Manager",List.of("Tổng quan","Duyệt đơn nghỉ","Quản lý nhân sự","Dịch vụ và giá","Danh mục thuốc","Báo cáo hoạt động","Báo cáo doanh thu","Phản hồi"),
        "Doctor",List.of("Tổng quan","Lịch làm việc","Yêu cầu nghỉ","Danh sách bệnh nhân","Khám bệnh","Đơn thuốc","Chỉ định xét nghiệm"),
        "Receptionist",List.of("Tổng quan","Danh sách lịch hẹn","Đăng ký bệnh nhân","Check-in","Quản lý hàng đợi","Đơn nghỉ của tôi","Thanh toán tại quầy"),
        "MedicalStaff",List.of("Tổng quan","Chỉ định dịch vụ","Thông tin bệnh nhân","Thực hiện dịch vụ","Nhập kết quả","Lịch sử dịch vụ","Đơn nghỉ của tôi"));
    private static final Map<String,List<String>> KEYS=Map.of(
        "Manager",List.of("overview","leave-approvals","staff","services","medicines","activity-report","revenue-report","feedback"),
        "Doctor",List.of("overview","schedule","leave-request","patients","consultation","prescriptions","lab-orders"),
        "Receptionist",List.of("overview","appointments","registration","check-in","queue","leave-request","payments"),
        "MedicalStaff",List.of("overview","service-orders","patient-info","perform-service","results","service-history","leave-request"));
    private RoleAccess(){}
    public static boolean isSupported(String role){return MENUS.containsKey(role);}
    public static List<String> menusFor(String role){return MENUS.getOrDefault(role,List.of());}
    public static List<String> menuKeysFor(String role){return KEYS.getOrDefault(role,List.of());}
    public static List<String> pagesFor(String role){return switch(role){
        case "Manager"->List.of("overview","leave-approvals","staff","services","medicines","activity-report","revenue-report","feedback");
        case "Doctor"->List.of("overview","schedule","leave-request","patients","consultation","prescriptions","lab-orders");
        case "Receptionist"->List.of("overview","appointments","registration","check-in","queue","leave-request","payments");
        case "MedicalStaff"->List.of("overview","service-orders","patient-info","perform-service","results","service-history","leave-request");
        default->List.of();};}
}
