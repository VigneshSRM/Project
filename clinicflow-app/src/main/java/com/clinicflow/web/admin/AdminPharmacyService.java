package com.clinicflow.web.admin;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.Pharmacy;
import com.clinicflow.domain.entity.PharmacyUser;
import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.PharmacyStatus;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.enums.UserStatus;
import com.clinicflow.domain.repository.PharmacyRepository;
import com.clinicflow.domain.repository.PharmacyUserRepository;
import com.clinicflow.domain.repository.UserRepository;
import com.clinicflow.identity.InvitationService;
import com.clinicflow.identity.WeakPasswordException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminPharmacyService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final PharmacyRepository pharmacyRepository;
    private final PharmacyUserRepository pharmacyUserRepository;
    private final UserRepository userRepository;
    private final InvitationService invitationService;
    private final AuditService auditService;

    public AdminPharmacyService(PharmacyRepository pharmacyRepository,
                                PharmacyUserRepository pharmacyUserRepository,
                                UserRepository userRepository,
                                InvitationService invitationService,
                                AuditService auditService) {
        this.pharmacyRepository = pharmacyRepository;
        this.pharmacyUserRepository = pharmacyUserRepository;
        this.userRepository = userRepository;
        this.invitationService = invitationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PharmacyRow> list() {
        return pharmacyRepository.findAllByOrderByNameAsc().stream().map(this::toRow).toList();
    }

    @Transactional(readOnly = true)
    public PharmacyDetail detail(Long pharmacyId) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId).orElseThrow();
        List<PharmacyUser> users = pharmacyUserRepository.findByPharmacyPharmacyId(pharmacyId);
        users.forEach(link -> {
            link.getUser().getEmail();
            link.getUser().getStatus();
            link.getRole();
            link.getStatus();
        });
        return new PharmacyDetail(pharmacy, users);
    }

    @Transactional
    public Pharmacy enroll(PharmacyEnrollmentForm form, Long actorUserId) {
        if (pharmacyRepository.findByLicenceNumber(form.getLicenceNumber().trim()).isPresent()) {
            throw new WeakPasswordException("A pharmacy with this licence number is already enrolled.");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setName(form.getName().trim());
        pharmacy.setLicenceNumber(form.getLicenceNumber().trim());
        pharmacy.setAddressLine1(form.getAddressLine1().trim());
        pharmacy.setAddressLine2(trimToNull(form.getAddressLine2()));
        pharmacy.setCity(form.getCity().trim());
        pharmacy.setDistrict(trimToNull(form.getDistrict()));
        pharmacy.setState(form.getState().trim());
        pharmacy.setPostalCode(form.getPostalCode().trim());
        pharmacy.setPhone(trimToNull(form.getPhone()));
        pharmacy.setEmail(trimToNull(form.getEmail()));
        pharmacy.setStatus(PharmacyStatus.PENDING);
        pharmacy.setCreatedAt(now);
        pharmacy = pharmacyRepository.saveAndFlush(pharmacy);
        invitationService.invitePharmacyAdministrator(form.getAdminEmail(), pharmacy, pharmacy.getName(),
                actorUserId);
        auditService.record(actorUserId, Role.SUPER_ADMIN, "PHARMACY_ENROLLED", "Pharmacy",
                String.valueOf(pharmacy.getPharmacyId()), null, AuditOutcome.SUCCESS);
        return pharmacy;
    }

    @Transactional
    public void changeStatus(Long pharmacyId, PharmacyStatus status, Long actorUserId) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId).orElseThrow();
        pharmacy.setStatus(status);
        pharmacyRepository.save(pharmacy);
        auditService.record(actorUserId, Role.SUPER_ADMIN,
                status == PharmacyStatus.SUSPENDED ? "PHARMACY_SUSPENDED" : "PHARMACY_ACTIVATED",
                "Pharmacy", String.valueOf(pharmacyId), null, AuditOutcome.SUCCESS);
    }

    @Transactional
    public boolean resendInvite(Long pharmacyId, Long actorUserId) {
        PharmacyUser adminLink = pharmacyUserRepository.findByPharmacyPharmacyId(pharmacyId).stream()
                .filter(link -> link.getRole() == com.clinicflow.domain.enums.PharmacyUserRole.PHARMACY_ADMIN)
                .filter(link -> link.getUser().getStatus() == UserStatus.PENDING)
                .findFirst().orElse(null);
        if (adminLink == null) {
            return false;
        }
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId).orElseThrow();
        invitationService.resend(adminLink.getUser(), pharmacy.getName(), "Pharmacy administrator",
                actorUserId);
        return true;
    }

    private PharmacyRow toRow(Pharmacy pharmacy) {
        List<PharmacyUser> users = pharmacyUserRepository.findByPharmacyPharmacyId(pharmacy.getPharmacyId());
        PharmacyUser admin = users.stream()
                .filter(link -> link.getRole() == com.clinicflow.domain.enums.PharmacyUserRole.PHARMACY_ADMIN)
                .findFirst().orElse(null);
        User adminUser = admin == null ? null : admin.getUser();
        return new PharmacyRow(pharmacy.getPharmacyId(), pharmacy.getName(), pharmacy.getLicenceNumber(),
                pharmacy.getCity(), pharmacy.getState(), pharmacy.getStatus(), pharmacy.getStatus().name(),
                tone(pharmacy.getStatus()), adminUser == null ? "-" : adminUser.getEmail(),
                adminUser == null ? "-" : adminUser.getStatus().name(), users.size(),
                DATE.format(pharmacy.getCreatedAt()));
    }

    private static String tone(PharmacyStatus status) {
        return switch (status) {
            case ACTIVE -> "success";
            case PENDING -> "warning";
            case SUSPENDED -> "danger";
        };
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record PharmacyRow(Long id, String name, String licenceNumber, String city, String state,
                              PharmacyStatus status, String statusLabel, String statusTone,
                              String adminEmail, String adminStatus, long users, String enrolledOn) {
    }

    public record PharmacyDetail(Pharmacy pharmacy, List<PharmacyUser> users) {
    }
}
