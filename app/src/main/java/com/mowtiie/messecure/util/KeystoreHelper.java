package com.mowtiie.messecure.util;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class KeystoreHelper {

    private static final String KEY_ALIAS    = "messecure_aes_key";
    private static final String KEYSTORE     = "AndroidKeyStore";
    private static final String ALGORITHM    = "AES/GCM/NoPadding";
    private static final int    GCM_TAG_LEN  = 128;
    private static final int    IV_LENGTH    = 12;

    public static void generateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);

            keyGenerator.init(new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());

            keyGenerator.generateKey();
        }
    }

    public static String encrypt(String plainText) throws Exception {
        SecretKey key = getKey();

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] iv         = cipher.getIV();
        byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Combine IV + ciphertext into one array
        byte[] combined = new byte[iv.length + cipherBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

        return Base64.encodeToString(combined, Base64.DEFAULT);
    }

    public static String decrypt(String cipherText) throws Exception {
        SecretKey key = getKey();

        byte[] combined    = Base64.decode(cipherText, Base64.DEFAULT);
        byte[] iv          = Arrays.copyOfRange(combined, 0, IV_LENGTH);
        byte[] cipherBytes = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LEN, iv));

        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    private static SecretKey getKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        keyStore.load(null);
        return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
    }
}
