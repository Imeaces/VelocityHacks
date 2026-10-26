package org.imeaces.levellogin.auth.password;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 密码哈希工具类（SHA-256 + 盐）
 */
public class PasswordHashUtil {

    public static String hashPassword(String password, String salt, String serverSecret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String salted = password + serverSecret + salt;
            byte[] hash = md.digest(salted.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static boolean verifyPassword(String password, String salt, String serverSecret, String storedHash) {
        String computedHash = hashPassword(password, salt, serverSecret);
        return computedHash.equals(storedHash);
    }
}
