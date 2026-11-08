package hospital.web;

import jakarta.servlet.http.HttpSession;

public final class SessionFlash {
    private static final String PREFIX = "hospital.flash.";

    private SessionFlash() { }

    public static void put(HttpSession session, String key, String message) {
        session.setAttribute(PREFIX + key, message);
    }

    public static String consume(HttpSession session, String key) {
        if (session == null) return null;
        String attribute = PREFIX + key;
        Object message = session.getAttribute(attribute);
        session.removeAttribute(attribute);
        return message instanceof String text ? text : null;
    }
}
