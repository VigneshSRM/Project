package com.clinicflow.common;

import java.security.SecureRandom;
import java.time.Year;
import java.util.Locale;

public final class PublicIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private PublicIdGenerator() {
    }

    public static String patientId(long sequence) {
        return String.format(Locale.ROOT, "PAT-%d-%04d", Year.now().getValue(), sequence);
    }

    public static String appointmentId(long sequence) {
        return String.format(Locale.ROOT, "APT-%d-%05d", Year.now().getValue(), sequence);
    }

    public static String prescriptionCode() {
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}