package com.example.utils;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtils {
    private static final String ALGORITHM = "AES";
    // 16 bytes key for AES
    private static final byte[] KEY_BYTES = new byte[]{
        'Z', 'e', 'n', 'N', 'o', 't', 'e', 'S', 'e', 'c', 'u', 'r', 'e', 'K', 'e', 'y'
    };

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return "";
        try {
            SecretKeySpec secretKey = new SecretKeySpec(KEY_BYTES, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return plainText;
        }
    }

    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) return "";
        try {
            SecretKeySpec secretKey = new SecretKeySpec(KEY_BYTES, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.decode(encryptedText, Base64.NO_WRAP);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return encryptedText;
        }
    }
}
