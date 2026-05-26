package com.mowtiie.messecure.util;

import android.util.Base64;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;

    public static String encrypt(String plainText, byte[] secretKey256) {
        try {
            if (plainText == null || secretKey256 == null || secretKey256.length != 32) {
                throw new IllegalArgumentException("Invalid data inputs or key length (must be 256-bit / 32 bytes).");
            }

            byte[] iv = new byte[IV_SIZE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey256, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

            byte[] combinedPayload = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combinedPayload, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combinedPayload, iv.length, encryptedBytes.length);

            return Base64.encodeToString(combinedPayload, Base64.NO_WRAP);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decrypt(String base64Payload, byte[] secretKey256) {
        try {
            if (base64Payload == null || secretKey256 == null || secretKey256.length != 32) {
                throw new IllegalArgumentException("Invalid cipher block or secret key definition.");
            }

            byte[] combinedPayload = Base64.decode(base64Payload, Base64.NO_WRAP);
            if (combinedPayload.length <= IV_SIZE) {
                return null;
            }

            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(combinedPayload, 0, iv, 0, iv.length);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            int ciphertextLength = combinedPayload.length - IV_SIZE;
            byte[] encryptedBytes = new byte[ciphertextLength];
            System.arraycopy(combinedPayload, IV_SIZE, encryptedBytes, 0, ciphertextLength);

            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey256, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, "UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
