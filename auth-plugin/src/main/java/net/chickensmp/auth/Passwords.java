package net.chickensmp.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

final class Passwords {
    private static final SecureRandom RNG = new SecureRandom();
    private static final int ITER = 210_000;

    static String hash(String pw) {
        byte[] salt = new byte[16];
        RNG.nextBytes(salt);
        return "pbkdf2$" + ITER + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(derive(pw, salt, ITER));
    }

    static boolean verify(String pw, String stored) {
        try {
            String[] p = stored.split("\\$");
            if (p.length != 4 || !p[0].equals("pbkdf2")) return false;
            int it = Integer.parseInt(p[1]);
            byte[] salt = Base64.getDecoder().decode(p[2]);
            byte[] want = Base64.getDecoder().decode(p[3]);
            return MessageDigest.isEqual(want, derive(pw, salt, it));
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] derive(String pw, byte[] salt, int it) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, it, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
