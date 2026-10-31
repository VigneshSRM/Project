package com.clinicflow.identity;

public record PasswordResetEmailRequested(Long userId, String email, String firstName, String rawToken) {
}
