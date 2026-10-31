package com.clinicflow.identity;

public record VerificationEmailRequested(Long userId, String email, String firstName, String rawToken) {
}
