package hospital;

import hospital.dao.HospitalDao;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class HospitalDaoReadOnlyTest {
    private HospitalDaoReadOnlyTest() { }

    public static void verify() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        AtomicInteger writes = new AtomicInteger();
        Connection connection = proxy(Connection.class, (method, args) -> {
            if ("prepareStatement".equals(method)) {
                String sql = (String) args[0];
                if (!sql.stripLeading().toUpperCase().startsWith("SELECT")) writes.incrementAndGet();
                return proxy(PreparedStatement.class, (statementMethod, statementArgs) -> {
                    if ("executeQuery".equals(statementMethod)) {
                        reads.incrementAndGet();
                        return proxy(ResultSet.class, (resultMethod, resultArgs) -> "next".equals(resultMethod) ? false : null);
                    }
                    if ("execute".equals(statementMethod) || "executeUpdate".equals(statementMethod) || "executeBatch".equals(statementMethod)) {
                        writes.incrementAndGet();
                    }
                    return null;
                });
            }
            return null;
        });

        List<?> slots = new HospitalDao(() -> connection).findAvailableSlots(7, LocalDate.now().plusDays(1), "AM");
        boolean published = new HospitalDao(() -> connection).hasPublishedSchedule(7, LocalDate.now().plusDays(1), "AM");
        if (!slots.isEmpty() || published || reads.get() != 2 || writes.get() != 0) {
            throw new AssertionError("GET slots và kiểm tra lịch đã công bố chỉ được SELECT, không được gọi thao tác ghi.");
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, SqlInvocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) return null;
            return invocation.call(method.getName(), args);
        });
    }

    @FunctionalInterface
    private interface SqlInvocation {
        Object call(String method, Object[] args) throws Throwable;
    }
}
