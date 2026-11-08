package hospital.config;
import java.sql.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
public final class Database {
    private Database() {}
    public static Connection connect() throws SQLException {
        String url=setting("DB_URL","db.url"), user=setting("DB_USER","db.user"), password=setting("DB_PASSWORD","db.password");
        if(url==null||user==null||password==null){
            try {
                InitialContext context = new InitialContext();
                url = (String) context.lookup("java:comp/env/db.url");
                user = (String) context.lookup("java:comp/env/db.user");
                password = (String) context.lookup("java:comp/env/db.password");
            } catch (NamingException ex) {
                throw new SQLException("Chưa cấu hình DB_URL/DB_USER/DB_PASSWORD hoặc JNDI db.url, db.user, db.password.", ex);
            }
        }
        if(url==null||user==null||password==null) throw new SQLException("Chưa cấu hình DB_URL, DB_USER và DB_PASSWORD cho ứng dụng.");
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("Không tìm thấy Microsoft SQL Server JDBC Driver trong ứng dụng.", ex);
        }
        return DriverManager.getConnection(url,user,password);
    }
    private static String setting(String env,String property){String value=System.getenv(env);return value==null||value.isBlank()?System.getProperty(property):value;}
}
