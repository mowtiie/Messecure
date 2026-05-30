package com.mowtiie.messecure.util;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ConversationCrypto {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LEN = 128;
    private static final int IV_LENGTH = 12;

    public static String generateConversationKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey key = keyGen.generateKey();
        return Base64.encodeToString(key.getEncoded(), Base64.NO_WRAP);
    }

    public static String encrypt(String plainText, String base64Key) throws Exception {
        SecretKey key = decodeKey(base64Key);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] iv = cipher.getIV();
        byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + cipherBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    public static String decrypt(String cipherText, String base64Key) throws Exception {
        SecretKey key = decodeKey(base64Key);

        byte[] combined = Base64.decode(cipherText, Base64.NO_WRAP);
        byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
        byte[] cipherBytes = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LEN, iv));

        return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
    }

    private static SecretKey decodeKey(String base64Key) {
        byte[] keyBytes = Base64.decode(base64Key, Base64.NO_WRAP);
        return new SecretKeySpec(keyBytes, "AES");
    }
}