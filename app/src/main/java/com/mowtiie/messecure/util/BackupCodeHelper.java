package com.mowtiie.messecure.util;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class BackupCodeHelper {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public static String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("MSEC-");
        for (int group = 0; group < 3; group++) {
            for (int i = 0; i < 4; i++) {
                sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            if (group < 2) sb.append('-');
        }
        return sb.toString();
    }

    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    public static String hashCode(String code, String salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(Base64.decode(salt, Base64.NO_WRAP));
        byte[] hashed = digest.digest(code.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(hashed, Base64.NO_WRAP);
    }
}

