package com.clinicflow.identity;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.AuthToken;
import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.AuthTokenPurpose;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.enums.UserStatus;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class PasswordResetService {

    private final AuthTokenService authTokenService;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final AuditService auditService;

    public PasswordResetService(AuthTokenService authTokenService,
                                UserRepository userRepository,
                                PatientRepository patientRepository,
                                PasswordEncoder passwordEncoder,
                                ApplicationEventPublisher events,
                                AuditService auditService) {
        this.authTokenService = authTokenService;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.auditService = auditService;
    }

    /**
     * Always returns normally. The controller shows a generic confirmation whether
     * or not the email belongs to an active account.
     */
    @Transactional
    public void request(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        Optional<User> maybeUser = userRepository.findByEmailIgnoreCase(email.trim());
        if (maybeUser.isEmpty() || maybeUser.get().getStatus() != UserStatus.ACTIVE) {
            return;
        }
        User user = maybeUser.get();
        Optional<Patient> patient = patientRepository.findByUserUserId(user.getUserId());
        String firstName = patient.map(Patient::getFirstName)
                .orElseGet(() -> user.getEmail().split("@")[0]);
        IssuedToken token = authTokenService.issue(user, AuthTokenPurpose.RESET_PASSWORD);
        events.publishEvent(new PasswordResetEmailRequested(
                user.getUserId(), user.getEmail(), firstName, token.rawToken()));
        auditService.record(user.getUserId(), Role.PATIENT, "PASSWORD_RESET_REQUESTED", "User",
                user.getPublicUuid().toString(), patient.map(Patient::getPatientId).orElse(null),
                AuditOutcome.SUCCESS);
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        if (!PasswordPolicy.isValid(newPassword)) {
            throw new WeakPasswordException(PasswordPolicy.message());
        }
        AuthToken token = authTokenService.consume(rawToken, AuthTokenPurpose.RESET_PASSWORD)
                .orElseThrow(() -> new InvalidTokenException("This password reset link is invalid or has expired."));

        User user = token.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidTokenException("This account cannot reset its password.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);

        Long patientId = patientRepository.findByUserUserId(user.getUserId())
                .map(Patient::getPatientId)
                .orElse(null);
        auditService.record(user.getUserId(), Role.PATIENT, "PASSWORD_RESET", "User",
                user.getPublicUuid().toString(), patientId, AuditOutcome.SUCCESS);
    }
}
