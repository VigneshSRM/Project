package com.clinicflow.identity;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.AuthToken;
import com.clinicflow.domain.entity.Clinic;
import com.clinicflow.domain.entity.ClinicEnrollment;
import com.clinicflow.domain.entity.Doctor;
import com.clinicflow.domain.entity.DoctorUserLink;
import com.clinicflow.domain.entity.Pharmacy;
import com.clinicflow.domain.entity.PharmacyUser;
import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.AuthTokenPurpose;
import com.clinicflow.domain.enums.EnrollmentStatus;
import com.clinicflow.domain.enums.PharmacyStatus;
import com.clinicflow.domain.enums.PharmacyUserRole;
import com.clinicflow.domain.enums.PharmacyUserStatus;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.enums.UserStatus;
import com.clinicflow.domain.repository.AuthTokenRepository;
import com.clinicflow.domain.repository.ClinicEnrollmentRepository;
import com.clinicflow.domain.repository.ClinicRepository;
import com.clinicflow.domain.repository.DoctorUserLinkRepository;
import com.clinicflow.domain.repository.PharmacyRepository;
import com.clinicflow.domain.repository.PharmacyUserRepository;
import com.clinicflow.domain.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Creates staff accounts in PENDING state and sends a single-use invitation. No
 * password exists until the invited person chooses one through the link.
 */
@Service
public class InvitationService {

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final ClinicRepository clinicRepository;
    private final ClinicEnrollmentRepository enrollmentRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PharmacyUserRepository pharmacyUserRepository;
    private final DoctorUserLinkRepository doctorUserLinkRepository;
    private final AuthTokenService authTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final AuditService auditService;

    public InvitationService(UserRepository userRepository,
                             AuthTokenRepository authTokenRepository,
                             ClinicRepository clinicRepository,
                             ClinicEnrollmentRepository enrollmentRepository,
                             PharmacyRepository pharmacyRepository,
                             PharmacyUserRepository pharmacyUserRepository,
                             DoctorUserLinkRepository doctorUserLinkRepository,
                             AuthTokenService authTokenService,
                             PasswordEncoder passwordEncoder,
                             ApplicationEventPublisher events,
                             AuditService auditService) {
        this.userRepository = userRepository;
        this.authTokenRepository = authTokenRepository;
        this.clinicRepository = clinicRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.pharmacyUserRepository = pharmacyUserRepository;
        this.doctorUserLinkRepository = doctorUserLinkRepository;
        this.authTokenService = authTokenService;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.auditService = auditService;
    }

    @Transactional
    public User inviteClinicAdministrator(String email, Clinic clinic, String organization,
                                          Long actorUserId) {
        User user = createPendingUser(email, Role.CLINIC_ADMIN, clinic, null, null);
        sendInvite(user, organization, "Clinic administrator", actorUserId);
        return user;
    }

    @Transactional
    public User invitePharmacyAdministrator(String email, Pharmacy pharmacy, String organization,
                                             Long actorUserId) {
        User user = createPendingUser(email, Role.PHARMACY_ADMIN, null, pharmacy, null);
        sendInvite(user, organization, "Pharmacy administrator", actorUserId);
        return user;
    }

    @Transactional
    public User invitePharmacist(String email, Pharmacy pharmacy, String organization,
                                 Long actorUserId) {
        User user = createPendingUser(email, Role.PHARMACIST, null, pharmacy, null);
        sendInvite(user, organization, "Pharmacist", actorUserId);
        return user;
    }

    @Transactional
    public User inviteDoctor(String email, Doctor doctor, Clinic clinic, String organization,
                             Long actorUserId) {
        User user = createPendingUser(email, Role.DOCTOR, clinic, null, doctor);
        sendInvite(user, organization, "Doctor", actorUserId);
        return user;
    }

    @Transactional
    public void resend(User user, String organization, String roleLabel, Long actorUserId) {
        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalStateException("Only pending invitations can be resent.");
        }
        sendInvite(user, organization, roleLabel, actorUserId);
    }

    @Transactional
    public void accept(String rawToken, String password) {
        if (!PasswordPolicy.isValid(password)) {
            throw new WeakPasswordException(PasswordPolicy.message());
        }
        AuthToken token = authTokenService.consume(rawToken, AuthTokenPurpose.STAFF_INVITE)
                .orElseThrow(() -> new InvalidTokenException(
                        "This invitation is invalid, expired or already used. Ask an administrator for a new one."));
        User user = token.getUser();
        if (user.getStatus() != UserStatus.PENDING) {
            throw new InvalidTokenException("This invitation has already been accepted.");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        if (user.getRole() == Role.CLINIC_ADMIN && user.getClinic() != null) {
            Clinic clinic = user.getClinic();
            clinic.setStatus(com.clinicflow.domain.enums.ClinicStatus.ACTIVE);
            clinic.setUpdatedAt(now);
            clinicRepository.save(clinic);
            enrollmentRepository.findFirstByClinicClinicIdOrderByEnrolledAtDesc(clinic.getClinicId())
                    .ifPresent(enrollment -> {
                        enrollment.setStatus(EnrollmentStatus.ACTIVE);
                        enrollment.setActivatedAt(now);
                        enrollmentRepository.save(enrollment);
                    });
        }
        if ((user.getRole() == Role.PHARMACY_ADMIN || user.getRole() == Role.PHARMACIST)) {
            pharmacyUserRepository.findByUserUserId(user.getUserId()).ifPresent(link -> {
                Pharmacy pharmacy = link.getPharmacy();
                if (user.getRole() == Role.PHARMACY_ADMIN) {
                    pharmacy.setStatus(PharmacyStatus.ACTIVE);
                    pharmacyRepository.save(pharmacy);
                }
                link.setStatus(PharmacyUserStatus.ACTIVE);
                pharmacyUserRepository.save(link);
            });
        }
        auditService.record(user.getUserId(), user.getRole(), "STAFF_INVITE_ACCEPTED", "User",
                user.getPublicUuid().toString(), null, AuditOutcome.SUCCESS);
    }

    @Transactional(readOnly = true)
    public Optional<InviteContext> context(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return authTokenRepository.findByTokenHashAndPurpose(
                        TokenHasher.sha256(rawToken), AuthTokenPurpose.STAFF_INVITE)
                .filter(token -> token.getUsedAt() == null)
                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC)))
                .map(token -> {
                    User user = token.getUser();
                    return new InviteContext(user.getEmail(), displayName(user), roleLabel(user),
                            organization(user));
                });
    }

    private User createPendingUser(String email, Role role, Clinic clinic, Pharmacy pharmacy,
                                   Doctor doctor) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new WeakPasswordException("Enter an email address for the invitation.");
        }
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            throw new WeakPasswordException("An account with this email already exists.");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        User user = new User();
        user.setEmail(normalized);
        // Random unusable hash. The account is PENDING and cannot sign in until the
        // invitation is accepted and a real password is chosen.
        user.setPasswordHash(passwordEncoder.encode("INVITE-" + UUID.randomUUID()));
        user.setRole(role);
        user.setStatus(UserStatus.PENDING);
        user.setClinic(clinic);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user = userRepository.saveAndFlush(user);

        if (pharmacy != null) {
            PharmacyUser link = new PharmacyUser();
            link.setUser(user);
            link.setPharmacy(pharmacy);
            link.setRole(role == Role.PHARMACY_ADMIN ? PharmacyUserRole.PHARMACY_ADMIN : PharmacyUserRole.PHARMACIST);
            link.setStatus(PharmacyUserStatus.DISABLED);
            link.setCreatedAt(now);
            pharmacyUserRepository.save(link);
        }
        if (doctor != null) {
            DoctorUserLink link = new DoctorUserLink();
            link.setUser(user);
            link.setDoctor(doctor);
            link.setCreatedAt(now);
            doctorUserLinkRepository.save(link);
        }
        auditService.record(null, null, "STAFF_INVITED", "User", user.getPublicUuid().toString(),
                null, AuditOutcome.SUCCESS);
        return user;
    }

    private void sendInvite(User user, String organization, String roleLabel, Long actorUserId) {
        IssuedToken token = authTokenService.issue(user, AuthTokenPurpose.STAFF_INVITE);
        events.publishEvent(new StaffInviteRequested(user.getUserId(), user.getEmail(),
                displayName(user), organization, roleLabel, token.rawToken()));
        auditService.record(actorUserId, null, "STAFF_INVITE_SENT", "User",
                user.getPublicUuid().toString(), null, AuditOutcome.SUCCESS);
    }

    private static String displayName(User user) {
        if (user.getRole() == Role.DOCTOR) {
            return "Doctor";
        }
        if (user.getClinic() != null) {
            return user.getClinic().getName();
        }
        return user.getEmail();
    }

    private static String roleLabel(User user) {
        return switch (user.getRole()) {
            case CLINIC_ADMIN -> "Clinic administrator";
            case DOCTOR -> "Doctor";
            case PHARMACY_ADMIN -> "Pharmacy administrator";
            case PHARMACIST -> "Pharmacist";
            default -> user.getRole().name();
        };
    }

    private static String organization(User user) {
        if (user.getClinic() != null) {
            return user.getClinic().getName();
        }
        return user.getRole() == Role.PHARMACIST || user.getRole() == Role.PHARMACY_ADMIN
                ? "your pharmacy" : "IMS";
    }

    public record InviteContext(String email, String name, String roleLabel, String organization) {
    }
}
