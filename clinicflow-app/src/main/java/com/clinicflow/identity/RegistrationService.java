package com.clinicflow.identity;

import com.clinicflow.audit.AuditService;
import com.clinicflow.common.PublicIdGenerator;
import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.AuthTokenPurpose;
import com.clinicflow.domain.enums.Gender;
import com.clinicflow.domain.enums.PatientSource;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.enums.UserStatus;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final ApplicationEventPublisher events;
    private final AuditService auditService;

    public RegistrationService(UserRepository userRepository,
                               PatientRepository patientRepository,
                               PasswordEncoder passwordEncoder,
                               AuthTokenService authTokenService,
                               ApplicationEventPublisher events,
                               AuditService auditService) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.events = events;
        this.auditService = auditService;
    }

    @Transactional
    public void register(RegistrationForm form) {
        if (!PasswordPolicy.isValid(form.getPassword())) {
            throw new WeakPasswordException(PasswordPolicy.message());
        }

        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException("An account with this email already exists.");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.PATIENT);
        user.setStatus(UserStatus.PENDING);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException race) {
            throw new DuplicateEmailException("An account with this email already exists.");
        }

        Patient patient = new Patient();
        patient.setUser(user);
        // Temporary, unique 20-character value; replaced with the public PAT-... id
        // once the identity column has been assigned. This keeps generation safe
        // under concurrent registrations without a separate sequence table.
        patient.setPublicPatientId("TMP" + UUID.randomUUID().toString().replace("-", "").substring(0, 17));
        patient.setFirstName(form.getFirstName().trim());
        patient.setLastName(form.getLastName().trim());
        patient.setDob(form.getDob());
        patient.setGender(form.getGender() == null ? Gender.UNKNOWN : form.getGender());
        patient.setMobile(trimToNull(form.getMobile()));
        patient.setEmail(email);
        patient.setAddressLine1(trimToNull(form.getAddressLine1()));
        patient.setAddressLine2(trimToNull(form.getAddressLine2()));
        patient.setCity(trimToNull(form.getCity()));
        patient.setDistrict(trimToNull(form.getDistrict()));
        patient.setState(trimToNull(form.getState()));
        patient.setPostalCode(trimToNull(form.getPostalCode()));
        patient.setBloodGroup(trimToNull(form.getBloodGroup()));
        patient.setAllergies(trimToNull(form.getAllergies()));
        patient.setConditions(trimToNull(form.getConditions()));
        patient.setEmergencyName(trimToNull(form.getEmergencyName()));
        patient.setEmergencyPhone(trimToNull(form.getEmergencyPhone()));
        patient.setSourceFlag(PatientSource.PATIENT_ENTERED);
        patient.setCreatedAt(now);
        patient.setUpdatedAt(now);
        patient = patientRepository.saveAndFlush(patient);
        patient.setPublicPatientId(PublicIdGenerator.patientId(patient.getPatientId()));
        patientRepository.save(patient);

        IssuedToken token = authTokenService.issue(user, AuthTokenPurpose.VERIFY_EMAIL);
        events.publishEvent(new VerificationEmailRequested(
                user.getUserId(), user.getEmail(), patient.getFirstName(), token.rawToken()));

        auditService.record(user.getUserId(), Role.PATIENT, "PATIENT_REGISTERED", "Patient",
                patient.getPublicPatientId(), patient.getPatientId(), AuditOutcome.SUCCESS);
    }

    /**
     * Enumeration-safe: the caller always shows the same confirmation. A new link is
     * only issued for an existing account that is still pending verification.
     */
    @Transactional
    public void resendVerification(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        Optional<User> maybeUser = userRepository.findByEmailIgnoreCase(email.trim());
        if (maybeUser.isEmpty() || maybeUser.get().getStatus() != UserStatus.PENDING) {
            return;
        }
        User user = maybeUser.get();
        Optional<Patient> patient = patientRepository.findByUserUserId(user.getUserId());
        String firstName = patient.map(Patient::getFirstName)
                .orElseGet(() -> user.getEmail().split("@")[0]);
        IssuedToken token = authTokenService.issue(user, AuthTokenPurpose.VERIFY_EMAIL);
        events.publishEvent(new VerificationEmailRequested(
                user.getUserId(), user.getEmail(), firstName, token.rawToken()));
        auditService.record(user.getUserId(), Role.PATIENT, "VERIFICATION_RESENT", "User",
                user.getPublicUuid().toString(), patient.map(Patient::getPatientId).orElse(null),
                AuditOutcome.SUCCESS);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
