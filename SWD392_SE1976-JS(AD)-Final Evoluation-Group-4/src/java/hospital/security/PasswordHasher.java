package hospital.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Password hashing that uses algorithms included with Java 17. */
public final class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final int MIN_ITERATIONS = 100_000;
    private static final int MAX_ITERATIONS = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    public static String hash(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return "pbkdf2-sha256$" + ITERATIONS + "$" + encoder.encodeToString(salt)
                + "$" + encoder.encodeToString(derived);
    }

    public static boolean verify(String password, String encodedHash) {
        if (password == null || encodedHash == null) return false;
        String[] parts = encodedHash.split("\\$", -1);
        if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) return false;
            Base64.Decoder decoder = Base64.getUrlDecoder();
            byte[] salt = decoder.decode(parts[2]);
            byte[] expected = decoder.decode(parts[3]);
            if (salt.length != SALT_BYTES || expected.length != HASH_BITS / 8) return false;
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Java không hỗ trợ PBKDF2-HMAC-SHA256.", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
