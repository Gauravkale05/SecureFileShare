package com.secureshare.app.encryption;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-128 ECB Encryption Helper
 * - generateKey(): creates a random 128-bit key, returns Base64 string
 * - encrypt(): encrypts bytes using AES/ECB/PKCS5Padding
 * - decrypt(): decrypts bytes using the same key
 */
public class AESHelper {

    public static String generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128); // 128-bit key
        SecretKey secretKey = keyGen.generateKey();
        return Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);
    }

    public static byte[] encrypt(byte[] data, String base64Key) throws Exception {
        byte[] keyBytes = Base64.decode(base64Key, Base64.DEFAULT);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] encryptedData, String base64Key) throws Exception {
        byte[] keyBytes = Base64.decode(base64Key, Base64.DEFAULT);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(encryptedData);
    }
}
