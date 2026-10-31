package com.clinicflow.identity;

public record StaffInviteRequested(Long userId, String email, String name, String organization,
                                   String roleLabel, String rawToken) {
}
