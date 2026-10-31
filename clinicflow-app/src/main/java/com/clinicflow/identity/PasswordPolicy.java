package com.clinicflow.identity;

/**
 * Development default for RC-10 (password policy was not stated in the source):
 * at least 10 characters with at least one letter and one digit.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            hasLetter |= Character.isLetter(c);
            hasDigit |= Character.isDigit(c);
        }
        return hasLetter && hasDigit;
    }

    public static String message() {
        return "Use at least " + MIN_LENGTH + " characters including a letter and a number.";
    }
}
