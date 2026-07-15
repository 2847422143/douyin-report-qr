package com.example.douyinreportqr;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class AdminAuth {
    private AdminAuth() {
    }

    static String hash(String account, String password) {
        return sha256("douyin-report-admin:" + account + ":" + password);
    }

    static boolean verify(String account, String password, String expectedHash) {
        return hash(account, password).equals(expectedHash);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) builder.append(String.format("%02x", b));
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
