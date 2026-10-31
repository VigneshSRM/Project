package com.clinicflow.identity;

public record EmailChangeRequested(Long userId, String firstName, String newEmail, String rawToken) {
}
