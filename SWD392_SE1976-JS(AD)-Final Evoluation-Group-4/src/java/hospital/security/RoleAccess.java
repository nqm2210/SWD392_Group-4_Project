package hospital.security;
import java.util.*;
public final class RoleAccess {
    private static final Map<String,List<String>> MENUS=Map.of(
        "Manager",List.of("Duyệt đơn nghỉ","Quản lý nhân sự","Dịch vụ và giá","Danh mục thuốc","Báo cáo hoạt động","Báo cáo doanh thu"),
        "Doctor",List.of("Lịch làm việc","Yêu cầu nghỉ","Danh sách bệnh nhân","Khám bệnh","Đơn thuốc","Chỉ định xét nghiệm"),
        "Receptionist",List.of("Danh sách lịch hẹn","Tạo lịch bác sĩ","Đăng ký bệnh nhân","Check-in","Quản lý hàng đợi","Đơn nghỉ của tôi","Thanh toán tại quầy"));
    private static final Map<String,List<String>> KEYS=Map.of(
        "Manager",List.of("leave-approvals","staff","services","medicines","activity-report","revenue-report"),
        "Doctor",List.of("schedule","leave-request","patients","consultation","prescriptions","lab-orders"),
        "Receptionist",List.of("appointments","doctor-schedules","registration","check-in","queue","leave-request","payments"));
    private RoleAccess(){}
    public static boolean isSupported(String role){return MENUS.containsKey(role);}
    public static List<String> menusFor(String role){return MENUS.getOrDefault(role,List.of());}
    public static List<String> menuKeysFor(String role){return KEYS.getOrDefault(role,List.of());}
    public static List<String> pagesFor(String role){return menuKeysFor(role);}
    public static String defaultPageFor(String role){
        List<String> keys=menuKeysFor(role);
        return keys.isEmpty()?"":keys.get(0);
    }
}
