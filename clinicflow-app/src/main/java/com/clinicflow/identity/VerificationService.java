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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class VerificationService {

    private final AuthTokenService authTokenService;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final AuditService auditService;

    public VerificationService(AuthTokenService authTokenService,
                               UserRepository userRepository,
                               PatientRepository patientRepository,
                               AuditService auditService) {
        this.authTokenService = authTokenService;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void verify(String rawToken) {
        AuthToken token = authTokenService.consume(rawToken, AuthTokenPurpose.VERIFY_EMAIL)
                .orElseThrow(() -> new InvalidTokenException("This verification link is invalid or has expired."));

        User user = token.getUser();
        if (user.getStatus() == UserStatus.ACTIVE) {
            return;
        }
        if (user.getStatus() != UserStatus.PENDING) {
            throw new InvalidTokenException("This account cannot be activated.");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        Long patientId = patientRepository.findByUserUserId(user.getUserId())
                .map(Patient::getPatientId)
                .orElse(null);
        auditService.record(user.getUserId(), Role.PATIENT, "EMAIL_VERIFIED", "User",
                user.getPublicUuid().toString(), patientId, AuditOutcome.SUCCESS);
    }
}
