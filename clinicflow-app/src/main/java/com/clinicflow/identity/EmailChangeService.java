package com.clinicflow.identity;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.AuthToken;
import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.AuthTokenPurpose;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

/**
 * Email is a login identifier, so a change is not applied until the new address
 * proves ownership by opening a single-use link. The current password is required
 * to start the change.
 */
@Service
public class EmailChangeService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final AuthTokenService authTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final AuditService auditService;

    public EmailChangeService(UserRepository userRepository, PatientRepository patientRepository,
                              AuthTokenService authTokenService, PasswordEncoder passwordEncoder,
                              ApplicationEventPublisher events, AuditService auditService) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.authTokenService = authTokenService;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.auditService = auditService;
    }

    @Transactional
    public void request(Long userId, String newEmail, String currentPassword, Long actorUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
        String normalized = newEmail == null ? "" : newEmail.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (normalized.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("That is already your login email address.");
        }
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            throw new IllegalArgumentException("Another account already uses that email address.");
        }
        if (currentPassword == null || currentPassword.isBlank()
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Your current password is incorrect.");
        }
        user.setPendingEmail(normalized);
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);
        IssuedToken token = authTokenService.issue(user, AuthTokenPurpose.EMAIL_CHANGE);
        String firstName = patientRepository.findByUserUserId(userId)
                .map(Patient::getFirstName).orElse("there");
        events.publishEvent(new EmailChangeRequested(userId, firstName, normalized,
                token.rawToken()));
        auditService.record(actorUserId, user.getRole(), "EMAIL_CHANGE_REQUESTED", "User",
                user.getPublicUuid().toString(), null, AuditOutcome.SUCCESS);
    }

    @Transactional
    public String confirm(String rawToken) {
        AuthToken token = authTokenService.consume(rawToken, AuthTokenPurpose.EMAIL_CHANGE)
                .orElseThrow(() -> new InvalidTokenException(
                        "This email-change link is invalid, expired or already used. Request a new one."));
        User user = token.getUser();
        String pending = user.getPendingEmail();
        if (pending == null || pending.isBlank()) {
            throw new InvalidTokenException("There is no pending email change for this account.");
        }
        if (userRepository.existsByEmailIgnoreCase(pending)) {
            user.setPendingEmail(null);
            userRepository.save(user);
            throw new InvalidTokenException("That email address is now used by another account. "
                    + "Request the change again with a different address.");
        }
        String previous = user.getEmail();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        user.setEmail(pending);
        user.setPendingEmail(null);
        user.setEmailVerifiedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
        patientRepository.findByUserUserId(user.getUserId()).ifPresent(patient -> {
            patient.setEmail(pending);
            patient.setUpdatedAt(now);
            patientRepository.save(patient);
        });
        auditService.record(user.getUserId(), user.getRole(), "EMAIL_CHANGED", "User",
                user.getPublicUuid().toString(), null, AuditOutcome.SUCCESS);
        return pending;
    }
}
