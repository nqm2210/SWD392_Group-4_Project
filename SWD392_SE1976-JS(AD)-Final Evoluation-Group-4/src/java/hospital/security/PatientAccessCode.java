package hospital.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class PatientAccessCode {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PatientAccessCode() { }

    public static String generate() {
        byte[] value = new byte[32];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    public static String hash(String rawCode) {
        if (rawCode == null) throw new IllegalArgumentException("Access code is required.");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawCode.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    public static boolean verify(String candidate, String storedHash) {
        if (candidate == null || storedHash == null || storedHash.isBlank()) return false;
        byte[] expected = storedHash.getBytes(StandardCharsets.US_ASCII);
        byte[] actual = hash(candidate).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual);
    }
}
