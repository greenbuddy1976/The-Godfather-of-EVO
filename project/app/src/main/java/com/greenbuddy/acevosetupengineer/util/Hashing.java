package com.greenbuddy.acevosetupengineer.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Hashing {
    private Hashing() {}

    public static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) result.append(String.format("%02x", value & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 ist nicht verfügbar", ex);
        }
    }
}
