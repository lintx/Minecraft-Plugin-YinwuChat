package org.lintx.plugins.yinwuchat.common.auth;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import java.security.spec.MGF1ParameterSpec;

public class CryptoUtil {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoUtil() {}

    public static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            // 4096-bit to allow larger auth payloads (OAEP with SHA-256 has size limits).
            generator.initialize(4096, SECURE_RANDOM);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key pair", e);
        }
    }

    public static String publicKeyToPem(PublicKey publicKey) {
        String base64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN PUBLIC KEY-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            int end = Math.min(i + 64, base64.length());
            sb.append(base64, i, end).append("\n");
        }
        sb.append("-----END PUBLIC KEY-----");
        return sb.toString();
    }

    public static byte[] rsaDecryptOaep(byte[] encrypted, PrivateKey privateKey, String hashName) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            javax.crypto.spec.OAEPParameterSpec spec = new javax.crypto.spec.OAEPParameterSpec(
                    hashName,
                    "MGF1",
                    new MGF1ParameterSpec(hashName),
                    javax.crypto.spec.PSource.PSpecified.DEFAULT
            );
            cipher.init(Cipher.DECRYPT_MODE, privateKey, spec);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt RSA payload", e);
        }
    }

    public static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            SecretKey secretKey = new SecretKeySpec(key, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC", e);
        }
    }

    public static byte[] aesGcmDecrypt(byte[] ciphertext, byte[] iv, byte[] key) {
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(128, iv);
            SecretKey secretKey = new SecretKeySpec(key, "AES");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, spec);
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt AES payload", e);
        }
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute SHA-256", e);
        }
    }

    public static byte[] randomBytes(int len) {
        byte[] bytes = new byte[len];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    public static String randomHex(int lenBytes) {
        return bytesToHex(randomBytes(lenBytes));
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
