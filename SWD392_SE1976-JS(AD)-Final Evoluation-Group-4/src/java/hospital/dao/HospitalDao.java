package hospital.dao;

import hospital.config.Database;
import hospital.model.Doctor;
import hospital.model.Department;
import hospital.model.DoctorProfile;
import hospital.model.ScheduleSummary;
import hospital.model.StaffUser;
import hospital.model.TimeSlot;
import hospital.model.MedicalService;
import hospital.model.LeaveRequest;
import hospital.model.AppointmentSummary;
import hospital.model.ActivityReportRow;
import hospital.model.RevenueReportRow;
import hospital.model.DoctorVisit;
import hospital.model.ServiceOrderSummary;
import hospital.model.MedicineOption;
import hospital.model.Medicine;
import hospital.model.PrescriptionLine;
import hospital.security.PasswordHasher;
import hospital.security.PatientAccessCode;
import hospital.model.PatientBookingResult;
import hospital.model.PatientAppointment;
import hospital.model.AppointmentChange;
import hospital.model.BillSummary;
import hospital.model.PatientLookupResult;
import java.sql.*;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class HospitalDao {
    private final ConnectionProvider connectionProvider;

    public HospitalDao() { this(Database::connect); }

    public HospitalDao(ConnectionProvider connectionProvider) { this.connectionProvider = connectionProvider; }
    public List<Medicine> findMedicines(String search) throws SQLException {
        String sql="SELECT MedicineId,MedicineCode,MedicineName,Unit,UnitPrice,IsActive FROM Medicines WHERE (?=N'' OR MedicineCode LIKE ? OR MedicineName LIKE ? OR Unit LIKE ?) ORDER BY MedicineName";
        String term=search==null?"":"%"+search.trim()+"%";
        List<Medicine> medicines=new ArrayList<>();
        try(Connection c=connectionProvider.open();PreparedStatement s=c.prepareStatement(sql)){
            s.setNString(1,search==null?"":search.trim());s.setString(2,term);s.setNString(3,term);s.setNString(4,term);
            try(ResultSet r=s.executeQuery()){while(r.next())medicines.add(new Medicine(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getBigDecimal(5),r.getBoolean(6)));}
        }
        return medicines;
    }

    public void saveMedicine(int medicineId,String code,String name,String unit,java.math.BigDecimal price,
            boolean active,int managerUserId)throws SQLException{
        try(Connection c=connectionProvider.open()){
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try{
                String oldValue="";
                if(medicineId>0){
                    String lock="SELECT MedicineCode,MedicineName,Unit,UnitPrice,IsActive FROM Medicines WITH (UPDLOCK,HOLDLOCK) WHERE MedicineId=?";
                    try(PreparedStatement s=c.prepareStatement(lock)){s.setInt(1,medicineId);try(ResultSet r=s.executeQuery()){
                        if(!r.next())throw new SQLException("MEDICINE_NOT_FOUND");
                        oldValue="MedicineCode="+r.getString(1)+"; MedicineName="+r.getString(2)+"; Unit="+r.getString(3)+"; UnitPrice="+r.getBigDecimal(4)+"; IsActive="+r.getBoolean(5);
                    }}
                }
                String codeLock="SELECT MedicineId FROM Medicines WITH (UPDLOCK,HOLDLOCK) WHERE MedicineCode=?";
                try(PreparedStatement s=c.prepareStatement(codeLock)){s.setString(1,code);try(ResultSet r=s.executeQuery()){if(r.next()&&r.getInt(1)!=medicineId)throw new SQLException("MEDICINE_CODE_EXISTS");}}
                if(medicineId>0){
                    String update="UPDATE Medicines SET MedicineCode=?,MedicineName=?,Unit=?,UnitPrice=?,IsActive=? WHERE MedicineId=?";
                    try(PreparedStatement s=c.prepareStatement(update)){s.setString(1,code);s.setNString(2,name);s.setNString(3,unit);s.setBigDecimal(4,price);s.setBoolean(5,active);s.setInt(6,medicineId);if(s.executeUpdate()!=1)throw new SQLException("MEDICINE_NOT_FOUND");}
                }else{
                    String insert="INSERT INTO Medicines(MedicineCode,MedicineName,Unit,UnitPrice,IsActive) VALUES(?,?,?,?,?)";
                    try(PreparedStatement s=c.prepareStatement(insert,Statement.RETURN_GENERATED_KEYS)){s.setString(1,code);s.setNString(2,name);s.setNString(3,unit);s.setBigDecimal(4,price);s.setBoolean(5,active);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){if(!r.next())throw new SQLException("Không tạo được mã thuốc.");medicineId=r.getInt(1);}}
                }
                String newValue="MedicineCode="+code+"; MedicineName="+name+"; Unit="+unit+"; UnitPrice="+price+"; IsActive="+active;
                String audit="INSERT INTO SystemLogs(UserId,Action,EntityName,RecordId,Details,Reason) VALUES(?,?,?,?,?,?)";
                try(PreparedStatement s=c.prepareStatement(audit)){s.setInt(1,managerUserId);s.setString(2,oldValue.isEmpty()?"CREATE":"UPDATE");s.setString(3,"Medicines");s.setString(4,String.valueOf(medicineId));s.setNString(5,(oldValue.isEmpty()?"":"Old: "+oldValue+" | ")+"New: "+newValue);s.setNString(6,"Manager updated medicine catalog");s.executeUpdate();}
                c.commit();
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    public List<MedicineOption> findActiveMedicines() throws SQLException {
        List<MedicineOption> medicines=new ArrayList<>();
        try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement("SELECT MedicineId,MedicineName,Unit,UnitPrice FROM Medicines WHERE IsActive=1 ORDER BY MedicineName");ResultSet r=s.executeQuery()){
            while(r.next())medicines.add(new MedicineOption(r.getInt(1),r.getString(2),r.getString(3),r.getBigDecimal(4)));
        }return medicines;
    }

    public List<PrescriptionLine> findPrescriptionLines(int appointmentId,int doctorUserId)throws SQLException{
        String sql="SELECT pd.DetailId,m.MedicineId,m.MedicineCode,m.MedicineName,m.Unit,pd.Quantity,pd.Dosage,pd.UnitPrice,p.DoctorInstructions "
                +"FROM MedicalRecords mr JOIN Doctors d ON d.DoctorId=mr.DoctorId JOIN Prescriptions p ON p.RecordId=mr.RecordId "
                +"JOIN PrescriptionDetails pd ON pd.PrescriptionId=p.PrescriptionId JOIN Medicines m ON m.MedicineId=pd.MedicineId "
                +"WHERE mr.AppointmentId=? AND d.UserId=? ORDER BY pd.DetailId";
        List<PrescriptionLine> lines=new ArrayList<>();
        try(Connection c=connectionProvider.open();PreparedStatement s=c.prepareStatement(sql)){
            s.setInt(1,appointmentId);s.setInt(2,doctorUserId);
            try(ResultSet r=s.executeQuery()){while(r.next())lines.add(new PrescriptionLine(r.getInt(1),r.getInt(2),r.getString(3),r.getString(4),r.getString(5),r.getInt(6),r.getString(7),r.getBigDecimal(8),r.getString(9)));}
        }
        return lines;
    }

    public void addPrescriptionLine(int appointmentId,int doctorUserId,int medicineId,int quantity,String dosage,String instructions)throws SQLException{
        try(Connection c=Database.connect()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                int recordId,doctorId;String recordStatus;
                try(PreparedStatement s=c.prepareStatement("SELECT mr.RecordId,mr.DoctorId,mr.Status FROM MedicalRecords mr WITH (UPDLOCK,HOLDLOCK) JOIN Doctors d ON d.DoctorId=mr.DoctorId WHERE mr.AppointmentId=? AND d.UserId=?")){s.setInt(1,appointmentId);s.setInt(2,doctorUserId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Hãy lưu hồ sơ khám trước khi kê đơn.");recordId=r.getInt(1);doctorId=r.getInt(2);recordStatus=r.getString(3);}}
                if(!"IN_PROGRESS".equalsIgnoreCase(recordStatus))throw new SQLException("Chỉ được kê đơn khi hồ sơ đang trong tiến trình khám.");
                java.math.BigDecimal price;try(PreparedStatement s=c.prepareStatement("SELECT UnitPrice FROM Medicines WHERE MedicineId=? AND IsActive=1")){s.setInt(1,medicineId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Thuốc không tồn tại hoặc đã ngừng sử dụng.");price=r.getBigDecimal(1);}}
                int prescriptionId=0;try(PreparedStatement s=c.prepareStatement("SELECT PrescriptionId FROM Prescriptions WITH (UPDLOCK,HOLDLOCK) WHERE RecordId=?")){s.setInt(1,recordId);try(ResultSet r=s.executeQuery()){if(r.next())prescriptionId=r.getInt(1);}}
                if(prescriptionId==0){try(PreparedStatement s=c.prepareStatement("INSERT INTO Prescriptions(RecordId,DoctorInstructions) VALUES(?,?)",Statement.RETURN_GENERATED_KEYS)){s.setInt(1,recordId);s.setNString(2,instructions);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){if(!r.next())throw new SQLException("Không tạo được đơn thuốc.");prescriptionId=r.getInt(1);}}}
                else try(PreparedStatement s=c.prepareStatement("UPDATE Prescriptions SET DoctorInstructions=? WHERE PrescriptionId=?")){s.setNString(1,instructions);s.setInt(2,prescriptionId);s.executeUpdate();}
                int detailId=0;try(PreparedStatement s=c.prepareStatement("SELECT DetailId FROM PrescriptionDetails WITH (UPDLOCK,HOLDLOCK) WHERE PrescriptionId=? AND MedicineId=?")){s.setInt(1,prescriptionId);s.setInt(2,medicineId);try(ResultSet r=s.executeQuery()){if(r.next())detailId=r.getInt(1);}}
                if(detailId==0){try(PreparedStatement s=c.prepareStatement("INSERT INTO PrescriptionDetails(PrescriptionId,MedicineId,Quantity,Dosage,UnitPrice) VALUES(?,?,?,?,?)")){s.setInt(1,prescriptionId);s.setInt(2,medicineId);s.setInt(3,quantity);s.setNString(4,dosage);s.setBigDecimal(5,price);s.executeUpdate();}}
                else{try(PreparedStatement s=c.prepareStatement("UPDATE PrescriptionDetails SET Quantity=?,Dosage=? WHERE DetailId=? AND PrescriptionId=?")){s.setInt(1,quantity);s.setNString(2,dosage);s.setInt(3,detailId);s.setInt(4,prescriptionId);if(s.executeUpdate()!=1)throw new SQLException("Không cập nhật được dòng thuốc.");}}
                audit(c,doctorUserId,detailId==0?"ADD_PRESCRIPTION_ITEM":"UPDATE_PRESCRIPTION_ITEM",appointmentId,"PrescriptionId="+prescriptionId+"; MedicineId="+medicineId+"; Quantity="+quantity);c.commit();
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    public void updatePrescriptionLine(int appointmentId,int doctorUserId,int detailId,int quantity,String dosage,String instructions)throws SQLException{
        try(Connection c=Database.connect()){
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try{
                int prescriptionId=lockInProgressPrescription(c,appointmentId,doctorUserId);
                try(PreparedStatement s=c.prepareStatement("UPDATE PrescriptionDetails SET Quantity=?,Dosage=? WHERE DetailId=? AND PrescriptionId=?")){s.setInt(1,quantity);s.setNString(2,dosage);s.setInt(3,detailId);s.setInt(4,prescriptionId);if(s.executeUpdate()!=1)throw new SQLException("Không tìm thấy dòng thuốc trong đơn của lượt khám này.");}
                try(PreparedStatement s=c.prepareStatement("UPDATE Prescriptions SET DoctorInstructions=? WHERE PrescriptionId=?")){s.setNString(1,instructions);s.setInt(2,prescriptionId);if(s.executeUpdate()!=1)throw new SQLException("Không cập nhật được hướng dẫn của đơn thuốc.");}
                audit(c,doctorUserId,"UPDATE_PRESCRIPTION_ITEM",appointmentId,"PrescriptionId="+prescriptionId+"; DetailId="+detailId+"; Quantity="+quantity);c.commit();
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    public void deletePrescriptionLine(int appointmentId,int doctorUserId,int detailId)throws SQLException{
        try(Connection c=Database.connect()){
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try{
                int prescriptionId=lockInProgressPrescription(c,appointmentId,doctorUserId);
                try(PreparedStatement s=c.prepareStatement("DELETE FROM PrescriptionDetails WHERE DetailId=? AND PrescriptionId=?")){s.setInt(1,detailId);s.setInt(2,prescriptionId);if(s.executeUpdate()!=1)throw new SQLException("Không tìm thấy dòng thuốc trong đơn của lượt khám này.");}
                audit(c,doctorUserId,"DELETE_PRESCRIPTION_ITEM",appointmentId,"PrescriptionId="+prescriptionId+"; DetailId="+detailId);c.commit();
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    private int lockInProgressPrescription(Connection c,int appointmentId,int doctorUserId)throws SQLException{
        int recordId;String status;
        try(PreparedStatement s=c.prepareStatement("SELECT mr.RecordId,mr.Status FROM MedicalRecords mr WITH (UPDLOCK,HOLDLOCK) JOIN Doctors d ON d.DoctorId=mr.DoctorId WHERE mr.AppointmentId=? AND d.UserId=?")){
            s.setInt(1,appointmentId);s.setInt(2,doctorUserId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Hãy mở hồ sơ thuộc lượt khám của bác sĩ đang đăng nhập.");recordId=r.getInt(1);status=r.getString(2);}
        }
        if(!"IN_PROGRESS".equalsIgnoreCase(status))throw new SQLException("Chỉ được sửa đơn khi hồ sơ đang IN_PROGRESS.");
        try(PreparedStatement s=c.prepareStatement("SELECT PrescriptionId FROM Prescriptions WITH (UPDLOCK,HOLDLOCK) WHERE RecordId=?")){
            s.setInt(1,recordId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Không tìm thấy đơn thuốc của hồ sơ này.");return r.getInt(1);}
        }
    }

    public void orderMedicalService(int appointmentId,int doctorUserId,int serviceId,String priority,String instructions)throws SQLException{
        try(Connection c=Database.connect()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                int recordId;String recordStatus;
                String preliminaryDiagnosis;
                try(PreparedStatement s=c.prepareStatement("SELECT mr.RecordId,mr.Status,mr.Diagnosis FROM MedicalRecords mr WITH (UPDLOCK,HOLDLOCK) JOIN Doctors d ON d.DoctorId=mr.DoctorId WHERE mr.AppointmentId=? AND d.UserId=?")){s.setInt(1,appointmentId);s.setInt(2,doctorUserId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Hãy lưu hồ sơ khám trước khi chỉ định dịch vụ.");recordId=r.getInt(1);recordStatus=r.getString(2);preliminaryDiagnosis=r.getString(3);}}
                if(!"IN_PROGRESS".equalsIgnoreCase(recordStatus))throw new SQLException("Chỉ được chỉ định khi hồ sơ đang trong tiến trình khám.");
                if(preliminaryDiagnosis==null||preliminaryDiagnosis.isBlank())throw new SQLException("Cần lưu chẩn đoán sơ bộ trước khi tạo chỉ định dịch vụ.");
                java.math.BigDecimal price;try(PreparedStatement s=c.prepareStatement("SELECT UnitPrice FROM MedicalServices WHERE ServiceId=? AND IsActive=1")){s.setInt(1,serviceId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Dịch vụ không tồn tại hoặc đã ngừng sử dụng.");price=r.getBigDecimal(1);}}
                String orderNumber="ORD-"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase(Locale.ROOT);
                int orderId;try(PreparedStatement s=c.prepareStatement("INSERT INTO MedicalServiceOrders(OrderNumber,RecordId,ServiceId,Priority,ClinicalInstructions,UnitPrice) VALUES(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){s.setString(1,orderNumber);s.setInt(2,recordId);s.setInt(3,serviceId);s.setString(4,priority);s.setNString(5,instructions);s.setBigDecimal(6,price);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){if(!r.next())throw new SQLException("Không tạo được chỉ định dịch vụ.");orderId=r.getInt(1);}}
                audit(c,doctorUserId,"ORDER_MEDICAL_SERVICE",appointmentId,"OrderId="+orderId+"; ServiceId="+serviceId+"; Priority="+priority);c.commit();
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    public List<ServiceOrderSummary> findServiceOrders() throws SQLException {
        String sql="SELECT o.OrderId,o.OrderNumber,p.FullName,a.AppointmentNumber,du.FullName,dep.DepartmentName,svc.ServiceName,o.Priority,o.ClinicalInstructions,o.Status,r.Findings,r.ResultDetails,r.Status,o.OrderedAt "
                +"FROM MedicalServiceOrders o JOIN MedicalRecords mr ON mr.RecordId=o.RecordId JOIN Appointments a ON a.AppointmentId=mr.AppointmentId JOIN Patients p ON p.PatientId=mr.PatientId JOIN Doctors d ON d.DoctorId=mr.DoctorId JOIN Users du ON du.UserId=d.UserId LEFT JOIN Departments dep ON dep.DepartmentId=d.DepartmentId JOIN MedicalServices svc ON svc.ServiceId=o.ServiceId LEFT JOIN MedicalServiceResults r ON r.OrderId=o.OrderId ORDER BY CASE WHEN o.Status='ORDERED' THEN 0 WHEN o.Status='IN_PROGRESS' THEN 1 ELSE 2 END,o.OrderedAt DESC";
        List<ServiceOrderSummary> orders=new ArrayList<>();
        try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement(sql);ResultSet r=s.executeQuery()){
            while(r.next()){Timestamp ordered=r.getTimestamp(14);orders.add(new ServiceOrderSummary(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getString(8),r.getString(9),r.getString(10),r.getString(11),r.getString(12),r.getString(13),ordered==null?null:ordered.toLocalDateTime()));}
        }
        return orders;
    }

    public void saveServiceResult(int orderId,int staffUserId,String findings,String details,boolean release)throws SQLException{
        try(Connection c=Database.connect()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                String orderStatus;
                try(PreparedStatement s=c.prepareStatement("SELECT Status FROM MedicalServiceOrders WITH (UPDLOCK,HOLDLOCK) WHERE OrderId=?")){s.setInt(1,orderId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Không tìm thấy chỉ định dịch vụ.");orderStatus=r.getString(1);}}
                if("COMPLETED".equalsIgnoreCase(orderStatus)||"CANCELLED".equalsIgnoreCase(orderStatus))throw new SQLException("Chỉ định đã đóng, không thể cập nhật kết quả.");
                int resultId=0;String resultStatus=null;
                try(PreparedStatement s=c.prepareStatement("SELECT ResultId,Status FROM MedicalServiceResults WITH (UPDLOCK,HOLDLOCK) WHERE OrderId=?")){s.setInt(1,orderId);try(ResultSet r=s.executeQuery()){if(r.next()){resultId=r.getInt(1);resultStatus=r.getString(2);}}}
                if("RELEASED".equalsIgnoreCase(resultStatus))throw new SQLException("Kết quả đã phát hành, không thể sửa.");
                String next=release?"RELEASED":"DRAFT";
                if(resultId==0){String insert="INSERT INTO MedicalServiceResults(OrderId,PerformedBy,Findings,ResultDetails,Status,ResultDate,ReleasedAt) VALUES(?,?,?,?,?,GETDATE(),CASE WHEN ?=1 THEN GETDATE() ELSE NULL END)";
                    try(PreparedStatement s=c.prepareStatement(insert)){s.setInt(1,orderId);s.setInt(2,staffUserId);s.setNString(3,findings);s.setNString(4,details);s.setString(5,next);s.setBoolean(6,release);s.executeUpdate();}}
                else {String update="UPDATE MedicalServiceResults SET PerformedBy=?,Findings=?,ResultDetails=?,Status=?,ResultDate=GETDATE(),ReleasedAt=CASE WHEN ?=1 THEN GETDATE() ELSE NULL END WHERE ResultId=?";
                    try(PreparedStatement s=c.prepareStatement(update)){s.setInt(1,staffUserId);s.setNString(2,findings);s.setNString(3,details);s.setString(4,next);s.setBoolean(5,release);s.setInt(6,resultId);if(s.executeUpdate()!=1)throw new SQLException("Không lưu được kết quả dịch vụ.");}}
                try(PreparedStatement s=c.prepareStatement("UPDATE MedicalServiceOrders SET Status=?,CompletedAt=CASE WHEN ?=1 THEN GETDATE() ELSE NULL END WHERE OrderId=?")){s.setString(1,release?"COMPLETED":"IN_PROGRESS");s.setBoolean(2,release);s.setInt(3,orderId);if(s.executeUpdate()!=1)throw new SQLException("Không cập nhật được trạng thái chỉ định.");}
                try(PreparedStatement s=c.prepareStatement("INSERT INTO SystemLogs(UserId,Action,EntityName,RecordId,Details,Reason) VALUES(?,?,?,?,?,?)")){s.setInt(1,staffUserId);s.setString(2,release?"RELEASE_SERVICE_RESULT":"SAVE_SERVICE_RESULT");s.setString(3,"MedicalServiceOrders");s.setString(4,String.valueOf(orderId));s.setNString(5,"ResultStatus="+next);s.setNString(6,"Medical staff result entry");s.executeUpdate();}
                c.commit();
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    public List<DoctorVisit> findDoctorVisits(int doctorUserId, LocalDate date) throws SQLException {
        String sql = "SELECT a.AppointmentId,a.AppointmentNumber,p.FullName,p.Phone,a.AppointmentDate,ss.StartTime,a.Status,a.QueueNumber, "
                + "mr.Diagnosis,mr.ClinicalNotes,mr.TreatmentPlan,mr.Status FROM Appointments a "
                + "JOIN Doctors d ON d.DoctorId=a.DoctorId JOIN Patients p ON p.PatientId=a.PatientId "
                + "LEFT JOIN ScheduleSlots ss ON ss.SlotId=a.SlotId LEFT JOIN MedicalRecords mr ON mr.AppointmentId=a.AppointmentId "
                + "WHERE d.UserId=? AND a.AppointmentDate=? AND a.Status IN('CHECKED_IN','IN_CONSULTATION','COMPLETED') "
                + "ORDER BY CASE WHEN a.Status='IN_CONSULTATION' THEN 0 WHEN a.Status='CHECKED_IN' THEN 1 ELSE 2 END,ss.StartTime,a.QueueNumber";
        List<DoctorVisit> visits = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, doctorUserId); s.setDate(2, Date.valueOf(date));
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    Time start=r.getTime(6); int queue=r.getInt(8); Integer queueNumber=r.wasNull()?null:queue;
                    visits.add(new DoctorVisit(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getDate(5).toLocalDate(),
                            start==null?null:start.toLocalTime(),r.getString(7),queueNumber,r.getString(9),r.getString(10),r.getString(11),r.getString(12)));
                }
            }
        }
        return visits;
    }

    public void startDoctorConsultation(int appointmentId,int doctorUserId) throws SQLException {
        try(Connection c=Database.connect()){
            c.setAutoCommit(false); c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                String update="UPDATE a SET Status='IN_CONSULTATION' FROM Appointments a JOIN Doctors d ON d.DoctorId=a.DoctorId WHERE a.AppointmentId=? AND d.UserId=? AND a.Status='CHECKED_IN' AND a.AppointmentDate=CONVERT(date,GETDATE())";
                try(PreparedStatement s=c.prepareStatement(update)){s.setInt(1,appointmentId);s.setInt(2,doctorUserId);if(s.executeUpdate()!=1)throw new SQLException("Lịch không thuộc bác sĩ này hoặc chưa check-in hôm nay.");}
                audit(c,doctorUserId,"START_CONSULTATION",appointmentId,"Status=IN_CONSULTATION"); c.commit();
            } catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    public void saveDoctorConsultation(int appointmentId,int doctorUserId,String diagnosis,String notes,String plan,boolean complete) throws SQLException {
        try(Connection c=Database.connect()){
            c.setAutoCommit(false); c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                int patientId,doctorId; String appointmentStatus;
                String lock="SELECT a.PatientId,a.DoctorId,a.Status FROM Appointments a WITH (UPDLOCK,HOLDLOCK) JOIN Doctors d ON d.DoctorId=a.DoctorId WHERE a.AppointmentId=? AND d.UserId=?";
                try(PreparedStatement s=c.prepareStatement(lock)){s.setInt(1,appointmentId);s.setInt(2,doctorUserId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Không tìm thấy lịch thuộc bác sĩ đang đăng nhập.");patientId=r.getInt(1);doctorId=r.getInt(2);appointmentStatus=r.getString(3);}}
                if(!"IN_CONSULTATION".equals(appointmentStatus))throw new SQLException("Hãy bắt đầu khám trước khi lưu hồ sơ.");
                int recordId=0;
                try(PreparedStatement s=c.prepareStatement("SELECT RecordId,Status FROM MedicalRecords WITH (UPDLOCK,HOLDLOCK) WHERE AppointmentId=?")){s.setInt(1,appointmentId);try(ResultSet r=s.executeQuery()){if(r.next()){recordId=r.getInt(1);if("COMPLETED".equalsIgnoreCase(r.getString(2)))throw new SQLException("Hồ sơ đã hoàn tất, không thể sửa.");}}}
                if(complete){
                    try(PreparedStatement s=c.prepareStatement("SELECT COUNT(*) FROM MedicalServiceOrders o JOIN MedicalRecords mr ON mr.RecordId=o.RecordId LEFT JOIN MedicalServiceResults sr ON sr.OrderId=o.OrderId WHERE mr.AppointmentId=? AND (o.Status<>'COMPLETED' OR ISNULL(sr.Status,'')<>'RELEASED')")){s.setInt(1,appointmentId);try(ResultSet r=s.executeQuery()){r.next();AppointmentSafety.requireServiceOrdersReviewed(r.getInt(1));}}
                }
                String status=complete?"COMPLETED":"IN_PROGRESS";
                if(recordId==0){String insert="INSERT INTO MedicalRecords(AppointmentId,PatientId,DoctorId,ClinicalNotes,Diagnosis,TreatmentPlan,Status,CompletedAt) VALUES(?,?,?,?,?,?,?,CASE WHEN ?=1 THEN GETDATE() ELSE NULL END)";
                    try(PreparedStatement s=c.prepareStatement(insert)){s.setInt(1,appointmentId);s.setInt(2,patientId);s.setInt(3,doctorId);s.setNString(4,notes);s.setNString(5,diagnosis);s.setNString(6,plan);s.setString(7,status);s.setBoolean(8,complete);s.executeUpdate();}}
                else {String update="UPDATE MedicalRecords SET ClinicalNotes=?,Diagnosis=?,TreatmentPlan=?,Status=?,CompletedAt=CASE WHEN ?=1 THEN GETDATE() ELSE NULL END WHERE RecordId=? AND DoctorId=?";
                    try(PreparedStatement s=c.prepareStatement(update)){s.setNString(1,notes);s.setNString(2,diagnosis);s.setNString(3,plan);s.setString(4,status);s.setBoolean(5,complete);s.setInt(6,recordId);s.setInt(7,doctorId);if(s.executeUpdate()!=1)throw new SQLException("Không thể cập nhật hồ sơ khám.");}}
                if(complete){
                    try(PreparedStatement s=c.prepareStatement("UPDATE Appointments SET Status='COMPLETED' WHERE AppointmentId=? AND Status='IN_CONSULTATION'")){s.setInt(1,appointmentId);if(s.executeUpdate()!=1)throw new SQLException("Không thể hoàn tất lịch khám.");}
                    java.math.BigDecimal consultationFee,serviceAmount;
                    try(PreparedStatement s=c.prepareStatement("SELECT ConsultationFee FROM Appointments WHERE AppointmentId=?")){s.setInt(1,appointmentId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Không tìm thấy phí khám cho lịch.");consultationFee=r.getBigDecimal(1);}}
                    String serviceTotal="SELECT COALESCE(SUM(o.UnitPrice),0) FROM MedicalServiceOrders o JOIN MedicalRecords mr ON mr.RecordId=o.RecordId JOIN MedicalServiceResults sr ON sr.OrderId=o.OrderId WHERE mr.AppointmentId=? AND o.Status='COMPLETED' AND sr.Status='RELEASED'";
                    try(PreparedStatement s=c.prepareStatement(serviceTotal)){s.setInt(1,appointmentId);try(ResultSet r=s.executeQuery()){r.next();serviceAmount=r.getBigDecimal(1);}}
                    String billNumber="BILL-"+UUID.randomUUID().toString().replace("-","").substring(0,25).toUpperCase(Locale.ROOT);
                    String bill="INSERT INTO Bills(BillNumber,AppointmentId,ConsultationFee,TotalServiceAmount,TotalAmount,PaymentStatus,FinalizedAt) VALUES(?,?,?,?,?,'UNPAID',GETDATE())";
                    try(PreparedStatement s=c.prepareStatement(bill)){s.setString(1,billNumber);s.setInt(2,appointmentId);s.setBigDecimal(3,consultationFee);s.setBigDecimal(4,serviceAmount);s.setBigDecimal(5,consultationFee.add(serviceAmount));if(s.executeUpdate()!=1)throw new SQLException("Không tạo được hóa đơn sau khi hoàn tất lượt khám.");}
                }
                audit(c,doctorUserId,complete?"COMPLETE_CONSULTATION":"SAVE_CONSULTATION",appointmentId,"MedicalRecords.Status="+status);c.commit();
            } catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    private void audit(Connection c,int userId,String action,int appointmentId,String details)throws SQLException{
        String sql="INSERT INTO SystemLogs(UserId,Action,EntityName,RecordId,Details,Reason) VALUES(?,?,?,?,?,?)";
        try(PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,userId);s.setString(2,action);s.setString(3,"Appointments");s.setString(4,String.valueOf(appointmentId));s.setNString(5,details);s.setNString(6,"Doctor workflow");s.executeUpdate();}
    }

    public List<ActivityReportRow> findActivityReport(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT AppointmentDate, COUNT(*), SUM(CASE WHEN Status='BOOKED' THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN Status='CHECKED_IN' THEN 1 ELSE 0 END), SUM(CASE WHEN Status='IN_CONSULTATION' THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN Status='COMPLETED' THEN 1 ELSE 0 END), SUM(CASE WHEN Status='CANCELLED' THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN Status='NEEDS_PROCESSING' THEN 1 ELSE 0 END), SUM(CASE WHEN Status='NO_SHOW' THEN 1 ELSE 0 END) "
                + "FROM Appointments WHERE AppointmentDate BETWEEN ? AND ? GROUP BY AppointmentDate ORDER BY AppointmentDate";
        List<ActivityReportRow> rows = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setDate(1, Date.valueOf(from));
            s.setDate(2, Date.valueOf(to));
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) rows.add(new ActivityReportRow(r.getDate(1).toLocalDate(), r.getInt(2),
                        r.getInt(3), r.getInt(4), r.getInt(5), r.getInt(6), r.getInt(7), r.getInt(8), r.getInt(9)));
            }
        }
        return rows;
    }

    public List<RevenueReportRow> findRevenueReport(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT COALESCE(dep.DepartmentName,N'Chưa phân khoa'), COALESCE(pay.PaymentMethod,N'Không rõ'), "
                + "COUNT(*), SUM(pay.AmountPaid) FROM Payments pay JOIN Bills b ON b.BillId=pay.BillId "
                + "JOIN Appointments a ON a.AppointmentId=b.AppointmentId JOIN Doctors d ON d.DoctorId=a.DoctorId "
                + "LEFT JOIN Departments dep ON dep.DepartmentId=d.DepartmentId "
                + "WHERE pay.PaidAt>=? AND pay.PaidAt<? "
                + "GROUP BY dep.DepartmentName,pay.PaymentMethod ORDER BY dep.DepartmentName,pay.PaymentMethod";
        List<RevenueReportRow> rows = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            s.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) rows.add(new RevenueReportRow(r.getString(1), r.getString(2), r.getInt(3), r.getBigDecimal(4)));
            }
        }
        return rows;
    }

    public List<Doctor> findPublicDoctors(String keyword) throws SQLException {
        String search="%"+(keyword==null?"":keyword.trim())+"%";
        String sql="SELECT d.DoctorId,u.FullName,d.AcademicTitle,d.Specialization,dep.DepartmentName,d.RoomNumber FROM Doctors d JOIN Users u ON u.UserId=d.UserId JOIN Departments dep ON dep.DepartmentId=d.DepartmentId WHERE ISNULL(u.IsActive,0)=1 AND ISNULL(d.IsPublished,0)=1 AND d.Status=N'Available' AND (u.FullName LIKE ? OR d.Specialization LIKE ? OR dep.DepartmentName LIKE ?) ORDER BY u.FullName";
        List<Doctor> results=new ArrayList<>();
        try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement(sql)){
            s.setString(1,search);s.setString(2,search);s.setString(3,search);
            try(ResultSet r=s.executeQuery()){while(r.next())results.add(new Doctor(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6)));}
        }return results;
    }

    public List<TimeSlot> findAvailableSlots(int doctorId,LocalDate date,String shift) throws SQLException {
        if(date==null||(!"AM".equals(shift)&&!"PM".equals(shift)))return List.of();
        String sql="SELECT ss.SlotId,CONVERT(varchar(5),ss.StartTime,108),CONVERT(varchar(5),ss.EndTime,108) FROM Schedules sc JOIN ScheduleSlots ss ON ss.ScheduleId=sc.ScheduleId JOIN Doctors d ON d.DoctorId=sc.DoctorId JOIN Users u ON u.UserId=d.UserId JOIN Roles r ON r.RoleId=u.RoleId WHERE sc.DoctorId=? AND sc.WorkDate=? AND sc.ShiftName=? AND sc.Status='PUBLISHED' AND r.RoleName=N'Doctor' AND ISNULL(u.IsActive,0)=1 AND ISNULL(d.IsPublished,0)=1 AND d.Status=N'Available' AND NOT EXISTS(SELECT 1 FROM LeaveRequests lr WHERE lr.UserId=u.UserId AND lr.Status=N'Approved' AND sc.WorkDate BETWEEN lr.StartDate AND lr.EndDate) AND (sc.WorkDate>CONVERT(date,GETDATE()) OR ss.StartTime>CONVERT(time,GETDATE())) AND ISNULL(ss.IsBooked,0)=0 AND ISNULL(ss.IsBlocked,0)=0 ORDER BY ss.StartTime";
        List<TimeSlot> slots=new ArrayList<>();
        try(Connection c=connectionProvider.open();PreparedStatement s=c.prepareStatement(sql)){s.setInt(1,doctorId);s.setDate(2,Date.valueOf(date));s.setNString(3,"AM".equals(shift)?"Ca sáng":"Ca chiều");try(ResultSet r=s.executeQuery()){while(r.next())slots.add(new TimeSlot(r.getInt(1),r.getString(2),r.getString(3)));}}
        return slots;
    }

    public boolean hasPublishedSchedule(int doctorId,LocalDate date,String shift) throws SQLException {
        if(date==null||(!"AM".equals(shift)&&!"PM".equals(shift)))return false;
        String sql="SELECT TOP 1 1 FROM Schedules WHERE DoctorId=? AND WorkDate=? AND ShiftName=? AND Status='PUBLISHED'";
        try(Connection c=connectionProvider.open();PreparedStatement s=c.prepareStatement(sql)){
            s.setInt(1,doctorId);s.setDate(2,Date.valueOf(date));s.setNString(3,"AM".equals(shift)?"Ca sáng":"Ca chiều");
            try(ResultSet r=s.executeQuery()){return r.next();}
        }
    }

    public List<AppointmentSummary> findAppointmentsForReceptionist(LocalDate date,String status,String keyword)throws SQLException{
        String[] statuses=status==null||status.isBlank()?new String[0]:status.split(",");
        StringBuilder sql=new StringBuilder("SELECT a.AppointmentId,a.AppointmentNumber,p.FullName,p.Phone,doctorUser.FullName,dep.DepartmentName,a.AppointmentDate,ss.StartTime,ss.EndTime,a.Status,a.QueueNumber FROM Appointments a JOIN Patients p ON p.PatientId=a.PatientId JOIN Doctors d ON d.DoctorId=a.DoctorId JOIN Users doctorUser ON doctorUser.UserId=d.UserId LEFT JOIN Departments dep ON dep.DepartmentId=d.DepartmentId LEFT JOIN ScheduleSlots ss ON ss.SlotId=a.SlotId WHERE a.AppointmentDate=?");
        if(statuses.length==1)sql.append(" AND a.Status=?");
        else if(statuses.length>1)sql.append(" AND a.Status IN (").append(String.join(",",java.util.Collections.nCopies(statuses.length,"?"))).append(")");
        if(keyword!=null&&!keyword.isBlank())sql.append(" AND (a.AppointmentNumber LIKE ? OR p.FullName LIKE ? OR p.Phone LIKE ?)");
        sql.append(" ORDER BY ss.StartTime, a.AppointmentId");
        List<AppointmentSummary> appointments=new ArrayList<>();
        try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement(sql.toString())){
            int index=1;s.setDate(index++,Date.valueOf(date));for(String value:statuses)s.setString(index++,value);
            if(keyword!=null&&!keyword.isBlank()){String term="%"+keyword.trim()+"%";s.setString(index++,term);s.setString(index++,term);s.setString(index,term);}
            try(ResultSet r=s.executeQuery()){while(r.next()){
                Time start=r.getTime(8),end=r.getTime(9);int queue=r.getInt(11);Integer queueNumber=r.wasNull()?null:queue;
                appointments.add(new AppointmentSummary(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),
                        r.getDate(7).toLocalDate(),start==null?null:start.toLocalTime(),end==null?null:end.toLocalTime(),r.getString(10),queueNumber));
            }}
        }
        return appointments;
    }

    public int checkInAppointment(int appointmentId,int receptionistUserId,LocalDate today)throws SQLException{
        try(Connection c=Database.connect()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                int doctorId;LocalDate appointmentDate;String status;
                String lock="SELECT DoctorId,AppointmentDate,Status FROM Appointments WITH (UPDLOCK,HOLDLOCK) WHERE AppointmentId=?";
                try(PreparedStatement s=c.prepareStatement(lock)){s.setInt(1,appointmentId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Không tìm thấy lịch hẹn.");doctorId=r.getInt(1);appointmentDate=r.getDate(2).toLocalDate();status=r.getString(3);}}
                if(!"BOOKED".equalsIgnoreCase(status))throw new SQLException("Lịch hẹn không còn ở trạng thái đã đặt.");
                if(!today.equals(appointmentDate))throw new SQLException("Chỉ check-in lịch hẹn trong ngày.");
                int queueNumber;
                String next="SELECT ISNULL(MAX(QueueNumber),0)+1 FROM Appointments WITH (UPDLOCK,HOLDLOCK) WHERE DoctorId=? AND AppointmentDate=?";
                try(PreparedStatement s=c.prepareStatement(next)){s.setInt(1,doctorId);s.setDate(2,Date.valueOf(appointmentDate));try(ResultSet r=s.executeQuery()){r.next();queueNumber=r.getInt(1);}}
                String update="UPDATE Appointments SET Status='CHECKED_IN',QueueNumber=?,CheckedInAt=GETDATE() WHERE AppointmentId=? AND Status='BOOKED'";
                try(PreparedStatement s=c.prepareStatement(update)){s.setInt(1,queueNumber);s.setInt(2,appointmentId);if(s.executeUpdate()!=1)throw new SQLException("Lịch hẹn vừa được check-in bởi người khác.");}
                String audit="INSERT INTO SystemLogs(UserId,Action,EntityName,RecordId,Details,Reason) VALUES(?,?,?,?,?,?)";
                try(PreparedStatement s=c.prepareStatement(audit)){s.setInt(1,receptionistUserId);s.setString(2,"CHECK_IN");s.setString(3,"Appointments");s.setString(4,String.valueOf(appointmentId));s.setNString(5,"Old: Status=BOOKED; New: Status=CHECKED_IN; QueueNumber="+queueNumber);s.setNString(6,"Receptionist checked in the appointment");s.executeUpdate();}
                c.commit();return queueNumber;
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    public void publishDoctorSchedule(int doctorUserId,LocalDate date,String shift)throws SQLException{
        if(date==null||date.isBefore(LocalDate.now())||date.isAfter(LocalDate.now().plusYears(1)))throw new SQLException("Ngày làm việc phải từ hôm nay đến trong vòng một năm.");
        if(!"AM".equals(shift)&&!"PM".equals(shift))throw new SQLException("Ca khám không hợp lệ.");
        String shiftName="AM".equals(shift)?"Ca sáng":"Ca chiều";
        LocalTime start="AM".equals(shift)?LocalTime.of(8,0):LocalTime.of(13,0);
        LocalTime end="AM".equals(shift)?LocalTime.of(12,0):LocalTime.of(17,0);
        try(Connection c=Database.connect()){
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                int doctorId;
                String validDoctor="SELECT d.DoctorId FROM Doctors d JOIN Users u ON u.UserId=d.UserId JOIN Roles r ON r.RoleId=u.RoleId WHERE u.UserId=? AND r.RoleName=N'Doctor' AND ISNULL(u.IsActive,0)=1";
                try(PreparedStatement s=c.prepareStatement(validDoctor)){s.setInt(1,doctorUserId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Tài khoản không phải Doctor đang hoạt động.");doctorId=r.getInt(1);}}
                String approvedLeave="SELECT RequestId FROM LeaveRequests WITH (UPDLOCK,HOLDLOCK) WHERE UserId=? AND Status=N'Approved' AND ? BETWEEN StartDate AND EndDate";
                try(PreparedStatement s=c.prepareStatement(approvedLeave)){s.setInt(1,doctorUserId);s.setDate(2,Date.valueOf(date));try(ResultSet r=s.executeQuery()){if(r.next())throw new SQLException("Không thể mở lịch trong ngày nghỉ đã được duyệt.");}}
                String existing="SELECT ScheduleId FROM Schedules WITH (UPDLOCK,HOLDLOCK) WHERE DoctorId=? AND WorkDate=? AND ShiftName=?";
                try(PreparedStatement s=c.prepareStatement(existing)){s.setInt(1,doctorId);s.setDate(2,Date.valueOf(date));s.setNString(3,shiftName);try(ResultSet r=s.executeQuery()){if(r.next())throw new SQLException("Ca khám ngày này đã tồn tại; không thể tạo trùng.");}}
                int scheduleId;
                String insert="INSERT INTO Schedules(DoctorId,WorkDate,ShiftName,Status,CreatedBy) VALUES(?,?,?,'PUBLISHED',?)";
                try(PreparedStatement s=c.prepareStatement(insert,Statement.RETURN_GENERATED_KEYS)){s.setInt(1,doctorId);s.setDate(2,Date.valueOf(date));s.setNString(3,shiftName);s.setInt(4,doctorUserId);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){if(!r.next())throw new SQLException("Không tạo được ca khám.");scheduleId=r.getInt(1);}}
                String insertSlot="INSERT INTO ScheduleSlots(ScheduleId,StartTime,EndTime,IsBooked,IsBlocked) VALUES(?,?,?,0,0)";
                try(PreparedStatement s=c.prepareStatement(insertSlot)){LocalTime cursor=start;while(cursor.isBefore(end)){LocalTime next=cursor.plusMinutes(30);s.setInt(1,scheduleId);s.setTime(2,Time.valueOf(cursor));s.setTime(3,Time.valueOf(next));s.addBatch();cursor=next;}s.executeBatch();}
                audit(c,doctorUserId,"PUBLISH_DOCTOR_SCHEDULE",scheduleId,"WorkDate="+date+"; Shift="+shiftName+"; Slots="+java.time.Duration.between(start,end).toMinutes()/30);
                c.commit();
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    public List<ScheduleSummary> findSchedulesForDoctor(int userId) throws SQLException {
        String sql="SELECT sc.ScheduleId,sc.WorkDate,sc.ShiftName,sc.Status,MIN(ss.StartTime),MAX(ss.EndTime),COUNT(ss.SlotId),SUM(CASE WHEN ISNULL(ss.IsBooked,0)=1 THEN 1 ELSE 0 END),SUM(CASE WHEN ISNULL(ss.IsBlocked,0)=1 THEN 1 ELSE 0 END) "
                +"FROM Doctors d JOIN Schedules sc ON sc.DoctorId=d.DoctorId LEFT JOIN ScheduleSlots ss ON ss.ScheduleId=sc.ScheduleId "
                +"WHERE d.UserId=? AND sc.WorkDate>=DATEADD(day,-7,CONVERT(date,GETDATE())) GROUP BY sc.ScheduleId,sc.WorkDate,sc.ShiftName,sc.Status ORDER BY sc.WorkDate,sc.ShiftName";
        List<ScheduleSummary> schedules=new ArrayList<>();
        try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement(sql)){
            s.setInt(1,userId);
            try(ResultSet r=s.executeQuery()){
                while(r.next()){
                    Time start=r.getTime(5),end=r.getTime(6);
                    schedules.add(new ScheduleSummary(r.getInt(1),r.getDate(2).toLocalDate(),r.getString(3),r.getString(4),
                            start==null?null:start.toLocalTime(),end==null?null:end.toLocalTime(),r.getInt(7),r.getInt(8),r.getInt(9)));
                }
            }
        }
        return schedules;
    }

    public StaffUser authenticate(String username,String password) throws SQLException {
        String sql="SELECT u.UserId,u.Username,u.FullName,r.RoleName,dep.DepartmentName,u.PasswordHash FROM Users u JOIN Roles r ON r.RoleId=u.RoleId LEFT JOIN Departments dep ON dep.DepartmentId=u.DepartmentId WHERE u.Username=? AND ISNULL(u.IsActive,0)=1";
        try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement(sql)){s.setString(1,username);try(ResultSet r=s.executeQuery()){if(!r.next()||!verifyPassword(password,r.getString("PasswordHash")))return null;return new StaffUser(r.getInt("UserId"),r.getString("Username"),r.getString("FullName"),r.getString("RoleName"),r.getString("DepartmentName"));}}
    }

    public PatientBookingResult bookAppointment(String fullName,LocalDate birthDate,String gender,String phone,String email,String reason,int doctorId,LocalDate date,String shift,int slotId) throws SQLException {
        return bookAppointment(fullName,birthDate,gender,phone,email,reason,doctorId,date,shift,slotId,null);
    }

    public PatientBookingResult bookAppointment(String fullName,LocalDate birthDate,String gender,String phone,String email,String reason,int doctorId,LocalDate date,String shift,int slotId,Integer createdBy) throws SQLException {
        String patientSql="INSERT INTO Patients(FullName,DateOfBirth,Gender,Phone,Email,AccessCode) VALUES(?,?,?,?,?,?)";
        String doctorSql="SELECT d.ConsultationFee FROM Doctors d JOIN Users u ON u.UserId=d.UserId JOIN Roles r ON r.RoleId=u.RoleId WHERE d.DoctorId=? AND r.RoleName=N'Doctor' AND ISNULL(u.IsActive,0)=1 AND d.Status=N'Available' AND d.IsPublished=1 AND NOT EXISTS(SELECT 1 FROM LeaveRequests lr WHERE lr.UserId=u.UserId AND lr.Status=N'Approved' AND ? BETWEEN lr.StartDate AND lr.EndDate)";
        String lockSlot="UPDATE ss SET IsBooked=1 FROM ScheduleSlots ss JOIN Schedules sc ON sc.ScheduleId=ss.ScheduleId JOIN Doctors d ON d.DoctorId=sc.DoctorId JOIN Users u ON u.UserId=d.UserId WHERE ss.SlotId=? AND ss.IsBooked=0 AND ISNULL(ss.IsBlocked,0)=0 AND sc.DoctorId=? AND sc.WorkDate=? AND sc.ShiftName=? AND sc.Status='PUBLISHED' AND ISNULL(u.IsActive,0)=1 AND d.Status=N'Available' AND d.IsPublished=1 AND NOT EXISTS(SELECT 1 FROM LeaveRequests lr WHERE lr.UserId=u.UserId AND lr.Status=N'Approved' AND sc.WorkDate BETWEEN lr.StartDate AND lr.EndDate) AND (sc.WorkDate>CONVERT(date,GETDATE()) OR ss.StartTime>CONVERT(time,GETDATE()))";
        String appointmentSql="INSERT INTO Appointments(AppointmentNumber,PatientId,DoctorId,SlotId,AppointmentDate,Reason,ConsultationFee) VALUES(?,?,?,?,?,?,?)";
        String newAccessCode=null,appointmentNumber=null;
        try(Connection c=Database.connect()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                if(date==null||date.isBefore(LocalDate.now())||(!"AM".equals(shift)&&!"PM".equals(shift)))throw new SQLException("Ngày hoặc ca khám không hợp lệ.");
                int patientId=0;
                String findPatient="SELECT PatientId,FullName,DateOfBirth FROM Patients WITH (UPDLOCK,HOLDLOCK) WHERE Phone=?";
                try(PreparedStatement s=c.prepareStatement(findPatient)){s.setString(1,phone.trim());try(ResultSet r=s.executeQuery()){if(r.next()){
                    if(createdBy==null)throw new SQLException("PATIENT_ALREADY_EXISTS: Số điện thoại đã có hồ sơ. Để bảo vệ thông tin cá nhân, vui lòng liên hệ lễ tân để xác minh và đặt lịch.");
                    java.sql.Date storedBirthDate=r.getDate(3);
                    if(storedBirthDate==null||!fullName.trim().equalsIgnoreCase(r.getString(2).trim())||!birthDate.equals(storedBirthDate.toLocalDate()))throw new SQLException("PATIENT_IDENTITY_MISMATCH: Số điện thoại đã gắn với hồ sơ khác. Vui lòng đối chiếu trực tiếp giấy tờ và thông tin bệnh nhân trước khi đăng ký.");
                    patientId=r.getInt(1);
                }}}
                if(patientId==0){newAccessCode=PatientAccessCode.generate();try(PreparedStatement s=c.prepareStatement(patientSql,Statement.RETURN_GENERATED_KEYS)){s.setNString(1,fullName.trim());s.setDate(2,Date.valueOf(birthDate));s.setNString(3,gender);s.setString(4,phone.trim());s.setString(5,email==null||email.isBlank()?null:email.trim());s.setString(6,PatientAccessCode.hash(newAccessCode));s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){if(!r.next())throw new SQLException("Không tạo được hồ sơ bệnh nhân.");patientId=r.getInt(1);}}}
                try(PreparedStatement fee=c.prepareStatement(doctorSql)){fee.setInt(1,doctorId);fee.setDate(2,Date.valueOf(date));try(ResultSet fr=fee.executeQuery()){if(!fr.next())throw new SQLException("Bác sĩ không còn nhận đặt lịch hoặc đang nghỉ trong ngày đã chọn.");java.math.BigDecimal amount=fr.getBigDecimal(1);
                        try(PreparedStatement lock=c.prepareStatement(lockSlot)){lock.setInt(1,slotId);lock.setInt(2,doctorId);lock.setDate(3,Date.valueOf(date));lock.setNString(4,"AM".equals(shift)?"Ca sáng":"Ca chiều");AppointmentSafety.requireSlotReserved(lock.executeUpdate());}
                        appointmentNumber="APT-"+date.toString().replace("-","")+"-"+UUID.randomUUID().toString().substring(0,6).toUpperCase(Locale.ROOT);
                        try(PreparedStatement appointment=c.prepareStatement(appointmentSql,Statement.RETURN_GENERATED_KEYS)){appointment.setString(1,appointmentNumber);appointment.setInt(2,patientId);appointment.setInt(3,doctorId);appointment.setInt(4,slotId);appointment.setDate(5,Date.valueOf(date));appointment.setNString(6,reason);appointment.setBigDecimal(7,amount);appointment.executeUpdate();
                            try(ResultSet keys=appointment.getGeneratedKeys()){if(!keys.next())throw new SQLException("Không lấy được mã lịch hẹn.");int appointmentId=keys.getInt(1);if(createdBy!=null){try(PreparedStatement log=c.prepareStatement("INSERT INTO SystemLogs(UserId,Action,EntityName,RecordId,Details,Reason) VALUES(?,?,?,?,?,?)")){log.setInt(1,createdBy);log.setString(2,"REGISTER_WALK_IN");log.setString(3,"Appointments");log.setString(4,String.valueOf(appointmentId));log.setNString(5,"PatientId="+patientId+"; AppointmentNumber="+appointmentNumber);log.setNString(6,"Receptionist registered an appointment");log.executeUpdate();}}}
                        }
                }}
                c.commit();return new PatientBookingResult(appointmentNumber,newAccessCode);
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    public Integer verifyPatientAccess(String contact,boolean email,String rawCode)throws SQLException{
        String sql=email?"SELECT PatientId,AccessCode FROM Patients WHERE Email=?":"SELECT PatientId,AccessCode FROM Patients WHERE Phone=?";
        try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement(sql)){
            s.setString(1,contact.trim());
            try(ResultSet r=s.executeQuery()){
                while(r.next())if(PatientAccessCode.verify(rawCode,r.getString(2)))return r.getInt(1);
            }
        }
        return null;
    }

    public PatientLookupResult findPatientForReception(String query) throws SQLException {
        String value = query == null ? "" : query.trim();
        Integer patientId = hospital.validation.PatientLookupValidator.patientIdCandidate(value);
        boolean numericId = patientId != null;
        String sql = "SELECT TOP 2 PatientId,FullName,DateOfBirth,Gender,Phone,Email,IdentityNumber FROM Patients WHERE Phone=? OR IdentityNumber=?"
                + (numericId ? " OR PatientId=?" : "");
        try (Connection c = Database.connect(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, value);
            s.setString(2, value);
            if (numericId) s.setInt(3, patientId);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return null;
                PatientLookupResult result = new PatientLookupResult(r.getInt("PatientId"), r.getString("FullName"),
                        r.getDate("DateOfBirth").toLocalDate(), r.getString("Gender"), r.getString("Phone"),
                        r.getString("Email"), r.getString("IdentityNumber"));
                if (r.next()) throw new SQLException("PATIENT_LOOKUP_AMBIGUOUS: Thông tin tra cứu khớp nhiều hồ sơ.");
                return result;
            }
        }
    }

    public List<PatientAppointment> findPatientAppointments(int patientId)throws SQLException{
        String appointmentStart="DATEADD(SECOND,DATEDIFF(SECOND,CONVERT(time,'00:00:00'),ss.StartTime),CONVERT(datetime,a.AppointmentDate))";
        String sql="SELECT a.AppointmentId,a.AppointmentNumber,d.DoctorId,du.FullName,dep.DepartmentName,a.AppointmentDate,sc.ShiftName,ss.SlotId,ss.StartTime,ss.EndTime,a.Status,a.Reason,b.BillId,b.PaymentStatus,b.TotalAmount,CASE WHEN a.Status='BOOKED' AND "+appointmentStart+">GETDATE() THEN 1 ELSE 0 END,CASE WHEN a.Status='BOOKED' AND DATEADD(HOUR,12,GETDATE())<"+appointmentStart+" THEN 1 ELSE 0 END "
                +"FROM Appointments a JOIN Doctors d ON d.DoctorId=a.DoctorId JOIN Users du ON du.UserId=d.UserId LEFT JOIN Departments dep ON dep.DepartmentId=d.DepartmentId LEFT JOIN ScheduleSlots ss ON ss.SlotId=a.SlotId LEFT JOIN Schedules sc ON sc.ScheduleId=ss.ScheduleId LEFT JOIN Bills b ON b.AppointmentId=a.AppointmentId WHERE a.PatientId=? ORDER BY a.AppointmentDate DESC,ss.StartTime DESC,a.AppointmentId DESC";
        List<PatientAppointment> appointments=new ArrayList<>();
        try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement(sql)){
            s.setInt(1,patientId);try(ResultSet r=s.executeQuery()){while(r.next()){
                Time start=r.getTime(9),end=r.getTime(10);int billValue=r.getInt(13);Integer billId=r.wasNull()?null:billValue;
                appointments.add(new PatientAppointment(r.getInt(1),r.getString(2),r.getInt(3),r.getString(4),r.getString(5),r.getDate(6).toLocalDate(),r.getString(7),r.getInt(8),start==null?null:start.toLocalTime(),end==null?null:end.toLocalTime(),r.getString(11),r.getString(12),billId,r.getString(14),r.getBigDecimal(15),r.getBoolean(16),r.getBoolean(17)));
            }}
        }
        return appointments;
    }

    public List<AppointmentChange> findPatientAppointmentChanges(int patientId)throws SQLException{
        String sql="SELECT l.LogId,a.AppointmentNumber,l.Action,l.Details,l.CreatedAt FROM SystemLogs l JOIN Appointments a ON l.RecordId=CONVERT(varchar(50),a.AppointmentId) WHERE a.PatientId=? AND l.EntityName='Appointments' AND l.Action IN('PATIENT_UPDATE_REASON','PATIENT_RESCHEDULE') ORDER BY l.CreatedAt DESC,l.LogId DESC";
        List<AppointmentChange> changes=new ArrayList<>();
        try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement(sql)){
            s.setInt(1,patientId);try(ResultSet r=s.executeQuery()){while(r.next()){Timestamp changed=r.getTimestamp(5);changes.add(new AppointmentChange(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),changed==null?null:changed.toLocalDateTime()));}}
        }
        return changes;
    }

    public void updatePatientAppointment(int patientId,int appointmentId,String reason,Integer requestedDoctorId,LocalDate requestedDate,String requestedShift,Integer requestedSlotId)throws SQLException{
        if(reason==null||reason.trim().isEmpty()||reason.trim().length()>255)throw new SQLException("Lý do khám là bắt buộc và tối đa 255 ký tự.");
        try(Connection c=Database.connect()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                int oldDoctorId,oldSlotId;LocalDate oldDate;String status,oldDoctorName;Time oldStart,oldEnd;boolean futureEditable,reschedulable;
                String startExpr="DATEADD(SECOND,DATEDIFF(SECOND,CONVERT(time,'00:00:00'),ss.StartTime),CONVERT(datetime,a.AppointmentDate))";
                String lock="SELECT a.DoctorId,a.SlotId,a.AppointmentDate,a.Status,CASE WHEN "+startExpr+">GETDATE() THEN 1 ELSE 0 END,CASE WHEN DATEADD(HOUR,12,GETDATE())<"+startExpr+" THEN 1 ELSE 0 END,u.FullName,ss.StartTime,ss.EndTime FROM Appointments a WITH (UPDLOCK,HOLDLOCK) JOIN ScheduleSlots ss ON ss.SlotId=a.SlotId JOIN Doctors d ON d.DoctorId=a.DoctorId JOIN Users u ON u.UserId=d.UserId WHERE a.AppointmentId=? AND a.PatientId=?";
                try(PreparedStatement s=c.prepareStatement(lock)){s.setInt(1,appointmentId);s.setInt(2,patientId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Không tìm thấy lịch hẹn thuộc hồ sơ đã xác minh.");oldDoctorId=r.getInt(1);oldSlotId=r.getInt(2);oldDate=r.getDate(3).toLocalDate();status=r.getString(4);futureEditable=r.getBoolean(5);reschedulable=r.getBoolean(6);oldDoctorName=r.getString(7);oldStart=r.getTime(8);oldEnd=r.getTime(9);}}
                if(!"BOOKED".equalsIgnoreCase(status)||!futureEditable)throw new SQLException("Chỉ lịch BOOKED trong tương lai mới được thay đổi.");
                boolean noScheduleChange=requestedDoctorId==null&&requestedDate==null&&requestedShift==null&&requestedSlotId==null;
                boolean scheduleUnchanged=noScheduleChange||(requestedDoctorId!=null&&requestedDate!=null&&requestedSlotId!=null&&requestedDoctorId==oldDoctorId&&requestedDate.equals(oldDate)&&requestedSlotId==oldSlotId);
                if(scheduleUnchanged){
                    try(PreparedStatement s=c.prepareStatement("UPDATE Appointments SET Reason=? WHERE AppointmentId=? AND PatientId=? AND Status='BOOKED'")){s.setNString(1,reason.trim());s.setInt(2,appointmentId);s.setInt(3,patientId);if(s.executeUpdate()!=1)throw new SQLException("Lịch vừa được xử lý và không còn sửa được.");}
                    auditPatientAppointment(c,appointmentId,"PATIENT_UPDATE_REASON","ReasonUpdated=true");c.commit();return;
                }
                if(!reschedulable)throw new SQLException("Chỉ được đổi giờ khi còn hơn 12 giờ trước giờ khám.");
                if(requestedDoctorId==null||requestedDate==null||requestedSlotId==null||requestedShift==null||(!"AM".equals(requestedShift)&&!"PM".equals(requestedShift)))throw new SQLException("Chọn lại bác sĩ, ngày, ca và khung giờ hợp lệ.");
                if(requestedDate.isBefore(LocalDate.now()))throw new SQLException("Không thể đổi sang ngày đã qua.");
                String countSql="SELECT COUNT(*) FROM SystemLogs WITH (UPDLOCK,HOLDLOCK) WHERE EntityName='Appointments' AND RecordId=? AND Action='PATIENT_RESCHEDULE'";
                try(PreparedStatement s=c.prepareStatement(countSql)){s.setString(1,String.valueOf(appointmentId));try(ResultSet r=s.executeQuery()){r.next();if(r.getInt(1)>=2)throw new SQLException("Lịch hẹn đã được đổi tối đa 2 lần.");}}
                String shiftName="AM".equals(requestedShift)?"Ca sáng":"Ca chiều";
                String reserve="UPDATE ss SET IsBooked=1 FROM ScheduleSlots ss JOIN Schedules sc ON sc.ScheduleId=ss.ScheduleId JOIN Doctors d ON d.DoctorId=sc.DoctorId JOIN Users u ON u.UserId=d.UserId JOIN Roles role ON role.RoleId=u.RoleId WHERE ss.SlotId=? AND ISNULL(ss.IsBooked,0)=0 AND ISNULL(ss.IsBlocked,0)=0 AND sc.DoctorId=? AND sc.WorkDate=? AND sc.ShiftName=? AND sc.Status='PUBLISHED' AND role.RoleName=N'Doctor' AND ISNULL(u.IsActive,0)=1 AND d.Status=N'Available' AND d.IsPublished=1 AND NOT EXISTS(SELECT 1 FROM LeaveRequests lr WHERE lr.UserId=u.UserId AND lr.Status=N'Approved' AND sc.WorkDate BETWEEN lr.StartDate AND lr.EndDate) AND (sc.WorkDate>CONVERT(date,GETDATE()) OR ss.StartTime>CONVERT(time,GETDATE()))";
                try(PreparedStatement s=c.prepareStatement(reserve)){s.setInt(1,requestedSlotId);s.setInt(2,requestedDoctorId);s.setDate(3,Date.valueOf(requestedDate));s.setNString(4,shiftName);AppointmentSafety.requireSlotReserved(s.executeUpdate());}
                Time newStart,newEnd;java.math.BigDecimal fee;String newDoctorName;
                String target="SELECT ss.StartTime,ss.EndTime,d.ConsultationFee,u.FullName FROM ScheduleSlots ss JOIN Schedules sc ON sc.ScheduleId=ss.ScheduleId JOIN Doctors d ON d.DoctorId=sc.DoctorId JOIN Users u ON u.UserId=d.UserId WHERE ss.SlotId=? AND sc.DoctorId=? AND sc.WorkDate=? AND sc.ShiftName=?";
                try(PreparedStatement s=c.prepareStatement(target)){s.setInt(1,requestedSlotId);s.setInt(2,requestedDoctorId);s.setDate(3,Date.valueOf(requestedDate));s.setNString(4,shiftName);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Khung giờ không còn thuộc lịch đã công bố.");newStart=r.getTime(1);newEnd=r.getTime(2);fee=r.getBigDecimal(3);newDoctorName=r.getString(4);}}
                String overlap="SELECT TOP 1 a.AppointmentId FROM Appointments a WITH (UPDLOCK,HOLDLOCK) JOIN ScheduleSlots ss ON ss.SlotId=a.SlotId WHERE a.PatientId=? AND a.AppointmentId<>? AND a.AppointmentDate=? AND a.Status IN('BOOKED','CHECKED_IN','IN_CONSULTATION') AND ss.StartTime<? AND ss.EndTime>?";
                try(PreparedStatement s=c.prepareStatement(overlap)){s.setInt(1,patientId);s.setInt(2,appointmentId);s.setDate(3,Date.valueOf(requestedDate));s.setTime(4,newEnd);s.setTime(5,newStart);try(ResultSet r=s.executeQuery()){if(r.next())throw new SQLException("Khung giờ bị trùng với một lịch hẹn khác của bạn.");}}
                try(PreparedStatement s=c.prepareStatement("UPDATE Appointments SET DoctorId=?,SlotId=?,AppointmentDate=?,ConsultationFee=?,Reason=? WHERE AppointmentId=? AND PatientId=? AND Status='BOOKED'")){s.setInt(1,requestedDoctorId);s.setInt(2,requestedSlotId);s.setDate(3,Date.valueOf(requestedDate));s.setBigDecimal(4,fee);s.setNString(5,reason.trim());s.setInt(6,appointmentId);s.setInt(7,patientId);if(s.executeUpdate()!=1)throw new SQLException("Lịch vừa được xử lý và không còn đổi được.");}
                try(PreparedStatement s=c.prepareStatement("UPDATE ScheduleSlots SET IsBooked=0 WHERE SlotId=? AND NOT EXISTS(SELECT 1 FROM Appointments WHERE SlotId=? AND AppointmentId<>?)")){s.setInt(1,oldSlotId);s.setInt(2,oldSlotId);s.setInt(3,appointmentId);s.executeUpdate();}
                String details="Ngày cũ="+oldDate+"; Bác sĩ cũ="+oldDoctorName+"; Giờ cũ="+oldStart+"–"+oldEnd+" | Ngày mới="+requestedDate+"; Bác sĩ mới="+newDoctorName+"; Giờ mới="+newStart+"–"+newEnd;
                auditPatientAppointment(c,appointmentId,"PATIENT_RESCHEDULE",details);c.commit();
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    private void auditPatientAppointment(Connection c,int appointmentId,String action,String details)throws SQLException{
        try(PreparedStatement s=c.prepareStatement("INSERT INTO SystemLogs(UserId,Action,EntityName,RecordId,Details,Reason) VALUES(NULL,?,'Appointments',?,?,?)")){s.setString(1,action);s.setString(2,String.valueOf(appointmentId));s.setNString(3,details);s.setNString(4,"Patient self-service appointment update");s.executeUpdate();}
    }

    public List<BillSummary> findBillsForReceptionist()throws SQLException{
        String sql="SELECT b.BillId,b.BillNumber,a.AppointmentNumber,p.FullName,du.FullName,dep.DepartmentName,b.ConsultationFee,b.TotalServiceAmount,b.TotalAmount,b.PaymentStatus,pay.ReceiptNumber,pay.AmountPaid,pay.PaidAt FROM Bills b JOIN Appointments a ON a.AppointmentId=b.AppointmentId JOIN Patients p ON p.PatientId=a.PatientId JOIN Doctors d ON d.DoctorId=a.DoctorId JOIN Users du ON du.UserId=d.UserId LEFT JOIN Departments dep ON dep.DepartmentId=d.DepartmentId LEFT JOIN Payments pay ON pay.BillId=b.BillId ORDER BY CASE WHEN b.PaymentStatus='UNPAID' THEN 0 ELSE 1 END,b.CreatedAt DESC,b.BillId DESC";
        List<BillSummary> bills=new ArrayList<>();
        try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement(sql);ResultSet r=s.executeQuery()){
            while(r.next()){Timestamp paid=r.getTimestamp(13);bills.add(new BillSummary(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getBigDecimal(7),r.getBigDecimal(8),r.getBigDecimal(9),r.getString(10),r.getString(11),r.getBigDecimal(12),paid==null?null:paid.toLocalDateTime()));}
        }
        return bills;
    }

    public String takeCounterPayment(int billId,int receptionistUserId)throws SQLException{
        try(Connection c=Database.connect()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                java.math.BigDecimal amount;String status;
                try(PreparedStatement s=c.prepareStatement("SELECT TotalAmount,PaymentStatus FROM Bills WITH (UPDLOCK,HOLDLOCK) WHERE BillId=?")){s.setInt(1,billId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Không tìm thấy hóa đơn.");amount=r.getBigDecimal(1);status=r.getString(2);}}
                if(!"UNPAID".equalsIgnoreCase(status))throw new SQLException("Hóa đơn không còn ở trạng thái UNPAID.");
                try(PreparedStatement s=c.prepareStatement("SELECT PaymentId FROM Payments WITH (UPDLOCK,HOLDLOCK) WHERE BillId=?")){s.setInt(1,billId);try(ResultSet r=s.executeQuery()){if(r.next())throw new SQLException("Hóa đơn đã có thanh toán, không thể thu lần hai.");}}
                String receipt="RCP-"+UUID.randomUUID().toString().replace("-","").substring(0,26).toUpperCase(Locale.ROOT);
                try(PreparedStatement s=c.prepareStatement("INSERT INTO Payments(BillId,ReceiptNumber,AmountPaid,PaymentMethod,ReceivedBy,Notes) VALUES(?,?,?,'Cash',?,?)")){s.setInt(1,billId);s.setString(2,receipt);s.setBigDecimal(3,amount);s.setInt(4,receptionistUserId);s.setNString(5,"Thu tiền mặt tại quầy");if(s.executeUpdate()!=1)throw new SQLException("Không ghi nhận được payment.");}
                try(PreparedStatement s=c.prepareStatement("UPDATE Bills SET PaymentStatus='PAID' WHERE BillId=? AND PaymentStatus='UNPAID'")){s.setInt(1,billId);if(s.executeUpdate()!=1)throw new SQLException("Hóa đơn vừa được thanh toán bởi yêu cầu khác.");}
                try(PreparedStatement s=c.prepareStatement("INSERT INTO SystemLogs(UserId,Action,EntityName,RecordId,Details,Reason) VALUES(?,?,?,?,?,?)")){s.setInt(1,receptionistUserId);s.setString(2,"RECEIVE_CASH_PAYMENT");s.setString(3,"Bills");s.setString(4,String.valueOf(billId));s.setNString(5,"ReceiptNumber="+receipt+"; Amount="+amount);s.setNString(6,"Receptionist received cash at counter");s.executeUpdate();}
                c.commit();return receipt;
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    public List<DoctorProfile> findDoctorProfiles() throws SQLException {
        String sql="SELECT d.DoctorId,u.UserId,u.Username,u.FullName,u.Email,u.Phone,d.DepartmentId,dep.DepartmentName,d.Specialization,d.AcademicTitle,d.RoomNumber,d.ConsultationFee,d.Status,d.IsPublished "
                +"FROM Doctors d JOIN Users u ON u.UserId=d.UserId JOIN Roles r ON r.RoleId=u.RoleId JOIN Departments dep ON dep.DepartmentId=d.DepartmentId WHERE r.RoleName=N'Doctor' ORDER BY u.FullName";
        List<DoctorProfile> doctors=new ArrayList<>();
        try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement(sql);ResultSet r=s.executeQuery()){
            while(r.next())doctors.add(new DoctorProfile(r.getInt(1),r.getInt(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getInt(7),r.getString(8),r.getString(9),r.getString(10),r.getString(11),r.getBigDecimal(12),r.getString(13),r.getBoolean(14)));
        }
        return doctors;
    }

    public List<Department> findDepartments() throws SQLException {
        List<Department> departments=new ArrayList<>();
        try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement("SELECT DepartmentId,DepartmentName FROM Departments ORDER BY DepartmentName");ResultSet r=s.executeQuery()){
            while(r.next())departments.add(new Department(r.getInt(1),r.getString(2)));
        }
        return departments;
    }

    public void updateDoctorProfile(int doctorId,String fullName,String email,String phone,int departmentId,String specialization,
            String academicTitle,String roomNumber,java.math.BigDecimal fee,String status,boolean published)throws SQLException{
        try(Connection c=Database.connect()){
            c.setAutoCommit(false);
            try{
                int userId;
                String find="SELECT d.UserId FROM Doctors d JOIN Users u ON u.UserId=d.UserId JOIN Roles r ON r.RoleId=u.RoleId WHERE d.DoctorId=? AND r.RoleName=N'Doctor'";
                try(PreparedStatement s=c.prepareStatement(find)){s.setInt(1,doctorId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Không tìm thấy hồ sơ bác sĩ.");userId=r.getInt(1);}}
                String updateUser="UPDATE Users SET FullName=?,Email=?,Phone=?,DepartmentId=? WHERE UserId=?";
                try(PreparedStatement s=c.prepareStatement(updateUser)){s.setNString(1,fullName.trim());s.setString(2,email.trim());s.setString(3,phone.trim());s.setInt(4,departmentId);s.setInt(5,userId);if(s.executeUpdate()!=1)throw new SQLException("Không cập nhật được thông tin tài khoản bác sĩ.");}
                String updateDoctor="UPDATE Doctors SET DepartmentId=?,Specialization=?,AcademicTitle=?,RoomNumber=?,ConsultationFee=?,Status=?,IsPublished=? WHERE DoctorId=? AND UserId=?";
                try(PreparedStatement s=c.prepareStatement(updateDoctor)){
                    s.setInt(1,departmentId);s.setNString(2,specialization.trim());
                    if(academicTitle==null||academicTitle.isBlank())s.setNull(3,Types.NVARCHAR);else s.setNString(3,academicTitle.trim());
                    s.setString(4,roomNumber.trim());s.setBigDecimal(5,fee);s.setNString(6,status);s.setBoolean(7,published);s.setInt(8,doctorId);s.setInt(9,userId);
                    if(s.executeUpdate()!=1)throw new SQLException("Không cập nhật được hồ sơ chuyên môn.");
                }
                c.commit();
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    public List<MedicalService> findMedicalServices() throws SQLException {
        String sql="SELECT s.ServiceId,s.ServiceCode,s.ServiceName,s.DepartmentId,d.DepartmentName,s.UnitPrice,s.Description,s.IsActive "
                +"FROM MedicalServices s JOIN Departments d ON d.DepartmentId=s.DepartmentId ORDER BY s.ServiceName";
        List<MedicalService> services=new ArrayList<>();
        try(Connection c=Database.connect();PreparedStatement statement=c.prepareStatement(sql);ResultSet result=statement.executeQuery()){
            while(result.next())services.add(new MedicalService(result.getInt(1),result.getString(2),result.getString(3),
                    result.getInt(4),result.getString(5),result.getBigDecimal(6),result.getString(7),result.getBoolean(8)));
        }
        return services;
    }

    public void saveMedicalService(int serviceId,String code,String name,int departmentId,java.math.BigDecimal price,
            String description,boolean active,int managerUserId)throws SQLException{
        try(Connection c=Database.connect()){
            c.setAutoCommit(false);
            try{
                String oldValue="";
                if(serviceId>0){
                    String select="SELECT ServiceCode,ServiceName,DepartmentId,UnitPrice,Description,IsActive FROM MedicalServices WITH (UPDLOCK) WHERE ServiceId=?";
                    try(PreparedStatement s=c.prepareStatement(select)){s.setInt(1,serviceId);try(ResultSet r=s.executeQuery()){
                        if(!r.next())throw new SQLException("Không tìm thấy dịch vụ cần cập nhật.");
                        oldValue="ServiceCode="+r.getString(1)+"; ServiceName="+r.getString(2)+"; DepartmentId="+r.getInt(3)+"; UnitPrice="+r.getBigDecimal(4)+"; Description="+r.getString(5)+"; IsActive="+r.getBoolean(6);
                    }}
                    String update="UPDATE MedicalServices SET ServiceCode=?,ServiceName=?,DepartmentId=?,UnitPrice=?,Description=?,IsActive=? WHERE ServiceId=?";
                    try(PreparedStatement s=c.prepareStatement(update)){s.setString(1,code);s.setNString(2,name);s.setInt(3,departmentId);s.setBigDecimal(4,price);
                        if(description==null||description.isBlank())s.setNull(5,Types.NVARCHAR);else s.setNString(5,description.trim());
                        s.setBoolean(6,active);s.setInt(7,serviceId);if(s.executeUpdate()!=1)throw new SQLException("Không cập nhật được dịch vụ.");}
                }else{
                    String insert="INSERT INTO MedicalServices(ServiceCode,ServiceName,DepartmentId,UnitPrice,Description,IsActive) VALUES(?,?,?,?,?,?)";
                    try(PreparedStatement s=c.prepareStatement(insert,Statement.RETURN_GENERATED_KEYS)){s.setString(1,code);s.setNString(2,name);s.setInt(3,departmentId);s.setBigDecimal(4,price);
                        if(description==null||description.isBlank())s.setNull(5,Types.NVARCHAR);else s.setNString(5,description.trim());
                        s.setBoolean(6,active);s.executeUpdate();try(ResultSet keys=s.getGeneratedKeys()){if(!keys.next())throw new SQLException("Không tạo được dịch vụ.");serviceId=keys.getInt(1);}}
                }
                String newValue="ServiceCode="+code+"; ServiceName="+name+"; DepartmentId="+departmentId+"; UnitPrice="+price+"; Description="+description+"; IsActive="+active;
                String action=oldValue.isEmpty()?"CREATE":"UPDATE";
                String audit="INSERT INTO SystemLogs(UserId,Action,EntityName,RecordId,Details,Reason) VALUES(?,?,?,?,?,?)";
                try(PreparedStatement s=c.prepareStatement(audit)){s.setInt(1,managerUserId);s.setString(2,action);s.setString(3,"MedicalServices");s.setString(4,String.valueOf(serviceId));
                    s.setNString(5,(oldValue.isEmpty()?"":("Old: "+oldValue+" | "))+"New: "+newValue);s.setNString(6,"Manager updated service catalog");s.executeUpdate();}
                c.commit();
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    public List<LeaveRequest> findLeaveRequestsForDoctor(int userId)throws SQLException{
        return findLeaveRequestsForUser(userId);
    }

    public List<LeaveRequest> findLeaveRequestsForUser(int userId)throws SQLException{
        String sql=leaveSelect()+" WHERE lr.UserId=? ORDER BY lr.CreatedAt DESC,lr.RequestId DESC";
        return findLeaveRequests(sql,userId);
    }

    public List<LeaveRequest> findLeaveRequestsForManager()throws SQLException{
        String sql=leaveSelect()+" ORDER BY CASE WHEN lr.Status=N'Pending' THEN 0 ELSE 1 END,lr.CreatedAt DESC,lr.RequestId DESC";
        return findLeaveRequests(sql,null);
    }

    private String leaveSelect(){
        return "SELECT lr.RequestId,lr.UserId,u.Username,u.FullName,dep.DepartmentName,lr.StartDate,lr.EndDate,lr.Reason,lr.Status,lr.ManagerResponseReason,lr.CreatedAt,approver.FullName,role.RoleName,"
                +"CASE WHEN role.RoleName=N'Doctor' THEN (SELECT COUNT(*) FROM Appointments a JOIN Doctors affectedDoctor ON affectedDoctor.DoctorId=a.DoctorId WHERE affectedDoctor.UserId=lr.UserId AND a.AppointmentDate BETWEEN lr.StartDate AND lr.EndDate AND a.Status IN('BOOKED','CHECKED_IN','IN_CONSULTATION')) ELSE 0 END "
                +"FROM LeaveRequests lr JOIN Users u ON u.UserId=lr.UserId JOIN Roles role ON role.RoleId=u.RoleId LEFT JOIN Departments dep ON dep.DepartmentId=u.DepartmentId LEFT JOIN Users approver ON approver.UserId=lr.ApprovedBy";
    }

    private List<LeaveRequest> findLeaveRequests(String sql,Integer userId)throws SQLException{
        List<LeaveRequest> requests=new ArrayList<>();
        try(Connection c=Database.connect();PreparedStatement s=c.prepareStatement(sql)){
            if(userId!=null)s.setInt(1,userId);
            try(ResultSet r=s.executeQuery()){while(r.next()){
                Timestamp created=r.getTimestamp(11);
                requests.add(new LeaveRequest(r.getInt(1),r.getInt(2),r.getString(3),r.getString(4),r.getString(5),
                        r.getDate(6).toLocalDate(),r.getDate(7).toLocalDate(),r.getString(8),r.getString(9),r.getString(10),
                        created==null?null:created.toLocalDateTime(),r.getString(12),r.getString(13),r.getInt(14)));
            }}
        }
        return requests;
    }

    public void createLeaveRequest(int userId,LocalDate startDate,LocalDate endDate,String reason)throws SQLException{
        try(Connection c=Database.connect()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                ensureLeaveRole(c,userId);
                String overlap="SELECT RequestId FROM LeaveRequests WITH (UPDLOCK,HOLDLOCK) WHERE UserId=? AND Status IN(N'Pending',N'Approved') AND StartDate<=? AND EndDate>=?";
                try(PreparedStatement s=c.prepareStatement(overlap)){s.setInt(1,userId);s.setDate(2,Date.valueOf(endDate));s.setDate(3,Date.valueOf(startDate));try(ResultSet r=s.executeQuery()){if(r.next())throw new SQLException("Đã có yêu cầu nghỉ đang chờ trong khoảng ngày này.");}}
                String insert="INSERT INTO LeaveRequests(UserId,StartDate,EndDate,Reason,Status) VALUES(?,?,?,?,'Pending')";
                int requestId;
                try(PreparedStatement s=c.prepareStatement(insert,Statement.RETURN_GENERATED_KEYS)){s.setInt(1,userId);s.setDate(2,Date.valueOf(startDate));s.setDate(3,Date.valueOf(endDate));s.setNString(4,reason.trim());s.executeUpdate();try(ResultSet keys=s.getGeneratedKeys()){if(!keys.next())throw new SQLException("Không tạo được yêu cầu nghỉ.");requestId=keys.getInt(1);}}
                String audit="INSERT INTO SystemLogs(UserId,Action,EntityName,RecordId,Details,Reason) VALUES(?,?,?,?,?,?)";
                try(PreparedStatement s=c.prepareStatement(audit)){s.setInt(1,userId);s.setString(2,"CREATE");s.setString(3,"LeaveRequests");s.setString(4,String.valueOf(requestId));s.setNString(5,"New: Status=Pending; StartDate="+startDate+"; EndDate="+endDate);s.setNString(6,"Staff member submitted a leave request");s.executeUpdate();}
                c.commit();
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    public void updatePendingLeaveRequest(int requestId,int userId,LocalDate startDate,LocalDate endDate,String reason)throws SQLException{
        try(Connection c=Database.connect()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                ensureLeaveRole(c,userId);
                String own="SELECT StartDate,EndDate FROM LeaveRequests WITH (UPDLOCK,HOLDLOCK) WHERE RequestId=? AND UserId=? AND Status=N'Pending'";
                LocalDate oldStart,oldEnd;
                try(PreparedStatement s=c.prepareStatement(own)){s.setInt(1,requestId);s.setInt(2,userId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Chỉ được sửa đơn Pending của chính mình.");oldStart=r.getDate(1).toLocalDate();oldEnd=r.getDate(2).toLocalDate();}}
                String overlap="SELECT RequestId FROM LeaveRequests WITH (UPDLOCK,HOLDLOCK) WHERE UserId=? AND RequestId<>? AND Status IN(N'Pending',N'Approved') AND StartDate<=? AND EndDate>=?";
                try(PreparedStatement s=c.prepareStatement(overlap)){s.setInt(1,userId);s.setInt(2,requestId);s.setDate(3,Date.valueOf(endDate));s.setDate(4,Date.valueOf(startDate));try(ResultSet r=s.executeQuery()){if(r.next())throw new SQLException("Đơn nghỉ bị trùng với một đơn Pending hoặc Approved khác.");}}
                try(PreparedStatement s=c.prepareStatement("UPDATE LeaveRequests SET StartDate=?,EndDate=?,Reason=? WHERE RequestId=? AND UserId=? AND Status=N'Pending'")){s.setDate(1,Date.valueOf(startDate));s.setDate(2,Date.valueOf(endDate));s.setNString(3,reason.trim());s.setInt(4,requestId);s.setInt(5,userId);if(s.executeUpdate()!=1)throw new SQLException("Đơn không còn ở trạng thái Pending.");}
                try(PreparedStatement s=c.prepareStatement("INSERT INTO SystemLogs(UserId,Action,EntityName,RecordId,Details,Reason) VALUES(?,?,?,?,?,?)")){s.setInt(1,userId);s.setString(2,"UPDATE_PENDING");s.setString(3,"LeaveRequests");s.setString(4,String.valueOf(requestId));s.setNString(5,"Old: StartDate="+oldStart+"; EndDate="+oldEnd+" | New: StartDate="+startDate+"; EndDate="+endDate);s.setNString(6,"Requester edited a pending leave request");s.executeUpdate();}
                c.commit();
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    private void ensureLeaveRole(Connection c,int userId)throws SQLException{
        try(PreparedStatement s=c.prepareStatement("SELECT r.RoleName FROM Users u JOIN Roles r ON r.RoleId=u.RoleId WHERE u.UserId=? AND ISNULL(u.IsActive,0)=1")){s.setInt(1,userId);try(ResultSet r=s.executeQuery()){if(!r.next()||!List.of("Doctor","Receptionist","MedicalStaff").contains(r.getString(1)))throw new SQLException("Tài khoản không được phép gửi yêu cầu nghỉ.");}}
    }

    public int decideLeaveRequest(int requestId,int managerUserId,String decision,String managerReason)throws SQLException{
        try(Connection c=Database.connect()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                int requesterUserId;LocalDate startDate,endDate;String requesterRole;
                String find="SELECT lr.UserId,lr.StartDate,lr.EndDate,role.RoleName FROM LeaveRequests lr WITH (UPDLOCK,HOLDLOCK) JOIN Users u ON u.UserId=lr.UserId JOIN Roles role ON role.RoleId=u.RoleId WHERE lr.RequestId=? AND lr.Status=N'Pending'";
                try(PreparedStatement s=c.prepareStatement(find)){s.setInt(1,requestId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Yêu cầu không còn ở trạng thái chờ duyệt.");requesterUserId=r.getInt(1);startDate=r.getDate(2).toLocalDate();endDate=r.getDate(3).toLocalDate();requesterRole=r.getString(4);}}
                if(requesterUserId==managerUserId)throw new SQLException("Không được tự duyệt đơn nghỉ của chính mình.");
                int affected=0;
                String status="APPROVE".equals(decision)?"Approved":"Rejected";
                if("APPROVE".equals(decision)&&"Doctor".equals(requesterRole)){
                    String activeConsultation="SELECT a.AppointmentId FROM Appointments a WITH (UPDLOCK,HOLDLOCK) JOIN Doctors d ON d.DoctorId=a.DoctorId WHERE d.UserId=? AND a.AppointmentDate BETWEEN ? AND ? AND a.Status='IN_CONSULTATION'";
                    try(PreparedStatement s=c.prepareStatement(activeConsultation)){s.setInt(1,requesterUserId);s.setDate(2,Date.valueOf(startDate));s.setDate(3,Date.valueOf(endDate));try(ResultSet r=s.executeQuery()){AppointmentSafety.requireNoConsultationConflict(r.next());}}
                    String markAppointments="UPDATE a SET Status='NEEDS_PROCESSING' FROM Appointments a JOIN Doctors d ON d.DoctorId=a.DoctorId WHERE d.UserId=? AND a.AppointmentDate BETWEEN ? AND ? AND a.Status IN('BOOKED','CHECKED_IN')";
                    try(PreparedStatement s=c.prepareStatement(markAppointments)){s.setInt(1,requesterUserId);s.setDate(2,Date.valueOf(startDate));s.setDate(3,Date.valueOf(endDate));affected=s.executeUpdate();}
                    String blockSlots="UPDATE ss SET IsBlocked=1 FROM ScheduleSlots ss JOIN Schedules sc ON sc.ScheduleId=ss.ScheduleId JOIN Doctors d ON d.DoctorId=sc.DoctorId WHERE d.UserId=? AND sc.WorkDate BETWEEN ? AND ? AND sc.Status='PUBLISHED' AND ISNULL(ss.IsBooked,0)=0";
                    try(PreparedStatement s=c.prepareStatement(blockSlots)){s.setInt(1,requesterUserId);s.setDate(2,Date.valueOf(startDate));s.setDate(3,Date.valueOf(endDate));s.executeUpdate();}
                    String cancelSchedules="UPDATE sc SET Status='CANCELLED' FROM Schedules sc JOIN Doctors d ON d.DoctorId=sc.DoctorId WHERE d.UserId=? AND sc.WorkDate BETWEEN ? AND ? AND sc.Status='PUBLISHED'";
                    try(PreparedStatement s=c.prepareStatement(cancelSchedules)){s.setInt(1,requesterUserId);s.setDate(2,Date.valueOf(startDate));s.setDate(3,Date.valueOf(endDate));s.executeUpdate();}
                }
                String update="UPDATE LeaveRequests SET Status=?,ApprovedBy=?,ManagerResponseReason=? WHERE RequestId=? AND Status=N'Pending'";
                try(PreparedStatement s=c.prepareStatement(update)){s.setNString(1,status);s.setInt(2,managerUserId);if(managerReason==null||managerReason.isBlank())s.setNull(3,Types.NVARCHAR);else s.setNString(3,managerReason.trim());s.setInt(4,requestId);if(s.executeUpdate()!=1)throw new SQLException("Yêu cầu đã được xử lý.");}
                String audit="INSERT INTO SystemLogs(UserId,Action,EntityName,RecordId,Details,Reason) VALUES(?,?,?,?,?,?)";
                try(PreparedStatement s=c.prepareStatement(audit)){s.setInt(1,managerUserId);s.setString(2,status.toUpperCase(Locale.ROOT));s.setString(3,"LeaveRequests");s.setString(4,String.valueOf(requestId));s.setNString(5,"RequesterRole="+requesterRole+"; Old: Status=Pending; New: Status="+status+"; AffectedAppointments="+affected);s.setNString(6,managerReason==null||managerReason.isBlank()?"Manager decision":"Manager decision: "+managerReason.trim());s.executeUpdate();}
                c.commit();return affected;
            }catch(SQLException|RuntimeException ex){c.rollback();throw ex;}
        }
    }

    private boolean verifyPassword(String candidate,String stored){
        return PasswordHasher.verify(candidate, stored);
    }
}
