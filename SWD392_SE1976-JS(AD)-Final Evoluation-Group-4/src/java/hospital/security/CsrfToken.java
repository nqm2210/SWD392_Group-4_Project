package hospital.security;

import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

public final class CsrfToken {
    private static final String SESSION_KEY = "formCsrfToken";

    private CsrfToken() {}

    public static String getOrCreate(HttpSession session) {
        String token = (String) session.getAttribute(SESSION_KEY);
        if (token == null) {
            token = UUID.randomUUID().toString() + UUID.randomUUID();
            session.setAttribute(SESSION_KEY, token);
        }
        return token;
    }

    public static boolean isValid(HttpSession session, String submitted) {
        if (session == null) return false;
        String expected = (String) session.getAttribute(SESSION_KEY);
        return expected != null && submitted != null
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), submitted.getBytes(StandardCharsets.UTF_8));
    }
}
