package com.clinicflow.web.setup;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.enums.UserStatus;
import com.clinicflow.domain.repository.UserRepository;
import com.clinicflow.identity.PasswordPolicy;
import com.clinicflow.identity.WeakPasswordException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

/**
 * First-run bootstrap. There is deliberately no shipped administrator account:
 * the first visitor creates the platform owner once, while the database has no
 * SUPER_ADMIN. After that the page is permanently unavailable.
 */
@Service
public class SetupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public SetupService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public boolean needsSetup() {
        return userRepository.countByRole(Role.SUPER_ADMIN) == 0;
    }

    @Transactional
    public void createAdministrator(SetupForm form) {
        if (!needsSetup()) {
            throw new IllegalStateException("IMS is already set up.");
        }
        if (!PasswordPolicy.isValid(form.getPassword())) {
            throw new WeakPasswordException(PasswordPolicy.message());
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new WeakPasswordException("Passwords do not match.");
        }
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new WeakPasswordException("An account with this email already exists.");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.SUPER_ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user = userRepository.saveAndFlush(user);
        auditService.record(user.getUserId(), Role.SUPER_ADMIN, "PLATFORM_SETUP", "User",
                user.getPublicUuid().toString(), null, AuditOutcome.SUCCESS);
    }
}
