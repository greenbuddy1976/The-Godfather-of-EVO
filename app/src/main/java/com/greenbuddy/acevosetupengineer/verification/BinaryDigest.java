package com.greenbuddy.acevosetupengineer.verification;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class BinaryDigest {
    private BinaryDigest() { }

    public static String sha256(byte[] bytes) {
        if (bytes == null) throw new IllegalArgumentException("bytes");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            char[] output = new char[digest.length * 2];
            char[] alphabet = "0123456789abcdef".toCharArray();
            for (int index = 0; index < digest.length; index++) {
                int value = digest[index] & 0xff;
                output[index * 2] = alphabet[value >>> 4];
                output[index * 2 + 1] = alphabet[value & 0x0f];
            }
            return new String(output);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
