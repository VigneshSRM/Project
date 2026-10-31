package com.clinicflow.web.admin;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.Clinic;
import com.clinicflow.domain.entity.ClinicEnrollment;
import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.ClinicStatus;
import com.clinicflow.domain.enums.EnrollmentStatus;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.enums.UserStatus;
import com.clinicflow.domain.repository.AppointmentRepository;
import com.clinicflow.domain.repository.ClinicDoctorRepository;
import com.clinicflow.domain.repository.ClinicEnrollmentRepository;
import com.clinicflow.domain.repository.ClinicRepository;
import com.clinicflow.domain.repository.DoctorScheduleRepository;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.PrescriptionRepository;
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
public class AdminClinicService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ClinicRepository clinicRepository;
    private final ClinicEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final ClinicDoctorRepository clinicDoctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final InvitationService invitationService;
    private final AuditService auditService;

    public AdminClinicService(ClinicRepository clinicRepository,
                              ClinicEnrollmentRepository enrollmentRepository,
                              UserRepository userRepository,
                              ClinicDoctorRepository clinicDoctorRepository,
                              DoctorScheduleRepository doctorScheduleRepository,
                              PatientRepository patientRepository,
                              AppointmentRepository appointmentRepository,
                              PrescriptionRepository prescriptionRepository,
                              InvitationService invitationService,
                              AuditService auditService) {
        this.clinicRepository = clinicRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.clinicDoctorRepository = clinicDoctorRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.invitationService = invitationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ClinicRow> list() {
        return clinicRepository.findAllByOrderByNameAsc().stream().map(this::toRow).toList();
    }

    @Transactional(readOnly = true)
    public ClinicDetail detail(Long clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId).orElseThrow();
        List<User> admins = userRepository.findByClinicClinicIdOrderByCreatedAtDesc(clinicId).stream()
                .filter(user -> user.getRole() == Role.CLINIC_ADMIN)
                .toList();
        List<ClinicEnrollment> enrollments = enrollmentRepository
                .findByClinicClinicIdOrderByEnrolledAtDesc(clinicId);
        enrollments.forEach(enrollment -> {
            enrollment.getEnrolledBy().getEmail();
            enrollment.getStatus();
            enrollment.getEnrolledAt();
            enrollment.getActivatedAt();
        });
        long doctors = clinicDoctorRepository.findByClinicClinicId(clinicId).size();
        long schedules = doctorScheduleRepository.findByClinicClinicId(clinicId).size();
        long patients = patientRepository.searchClinicPatients(clinicId, null).size();
        long appointments = appointmentRepository.findByClinicClinicIdOrderBySlotStartDesc(clinicId).size();
        long prescriptions = prescriptionRepository.findByClinicClinicIdOrderByIssueDateDesc(clinicId).size();
        return new ClinicDetail(clinic, admins, enrollments, doctors, schedules, patients, appointments,
                prescriptions);
    }

    @Transactional
    public Clinic enroll(ClinicEnrollmentForm form, Long actorUserId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Clinic clinic = new Clinic();
        clinic.setName(form.getName().trim());
        clinic.setAddressLine1(form.getAddressLine1().trim());
        clinic.setAddressLine2(trimToNull(form.getAddressLine2()));
        clinic.setCity(form.getCity().trim());
        clinic.setArea(trimToNull(form.getArea()));
        clinic.setDistrict(trimToNull(form.getDistrict()));
        clinic.setState(form.getState().trim());
        clinic.setPostalCode(form.getPostalCode().trim());
        clinic.setLatitude(form.getLatitude());
        clinic.setLongitude(form.getLongitude());
        clinic.setPhone(trimToNull(form.getPhone()));
        clinic.setEmail(trimToNull(form.getEmail()));
        clinic.setOpeningHours(trimToNull(form.getOpeningHours()));
        clinic.setFees(trimToNull(form.getFees()));
        clinic.setStatus(ClinicStatus.PENDING);
        clinic.setCreatedAt(now);
        clinic.setUpdatedAt(now);
        clinic = clinicRepository.saveAndFlush(clinic);

        User admin = userRepository.findById(actorUserId).orElseThrow();
        ClinicEnrollment enrollment = new ClinicEnrollment();
        enrollment.setClinic(clinic);
        enrollment.setEnrolledBy(admin);
        enrollment.setStatus(EnrollmentStatus.PENDING);
        enrollment.setEnrolledAt(now);
        enrollmentRepository.save(enrollment);

        try {
            invitationService.inviteClinicAdministrator(form.getAdminEmail(), clinic, clinic.getName(),
                    actorUserId);
        } catch (WeakPasswordException ex) {
            throw ex;
        }
        auditService.record(actorUserId, Role.SUPER_ADMIN, "CLINIC_ENROLLED", "Clinic",
                String.valueOf(clinic.getClinicId()), null, AuditOutcome.SUCCESS);
        return clinic;
    }

    @Transactional
    public void changeStatus(Long clinicId, ClinicStatus status, Long actorUserId) {
        Clinic clinic = clinicRepository.findById(clinicId).orElseThrow();
        clinic.setStatus(status);
        clinic.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        clinicRepository.save(clinic);
        enrollmentRepository.findFirstByClinicClinicIdOrderByEnrolledAtDesc(clinicId).ifPresent(enrollment -> {
            enrollment.setStatus(status == ClinicStatus.ACTIVE ? EnrollmentStatus.ACTIVE
                    : status == ClinicStatus.SUSPENDED ? EnrollmentStatus.SUSPENDED : EnrollmentStatus.PENDING);
            enrollmentRepository.save(enrollment);
        });
        auditService.record(actorUserId, Role.SUPER_ADMIN,
                status == ClinicStatus.SUSPENDED ? "CLINIC_SUSPENDED" : "CLINIC_ACTIVATED",
                "Clinic", String.valueOf(clinicId), null, AuditOutcome.SUCCESS);
    }

    @Transactional
    public boolean resendInvite(Long clinicId, Long actorUserId) {
        User admin = userRepository.findByClinicClinicIdOrderByCreatedAtDesc(clinicId).stream()
                .filter(user -> user.getRole() == Role.CLINIC_ADMIN)
                .filter(user -> user.getStatus() == UserStatus.PENDING)
                .findFirst().orElse(null);
        if (admin == null) {
            return false;
        }
        Clinic clinic = clinicRepository.findById(clinicId).orElseThrow();
        invitationService.resend(admin, clinic.getName(), "Clinic administrator", actorUserId);
        return true;
    }

    private ClinicRow toRow(Clinic clinic) {
        User admin = userRepository.findByClinicClinicIdOrderByCreatedAtDesc(clinic.getClinicId()).stream()
                .filter(user -> user.getRole() == Role.CLINIC_ADMIN)
                .findFirst().orElse(null);
        long doctors = clinicDoctorRepository.findByClinicClinicId(clinic.getClinicId()).size();
        long patients = patientRepository.searchClinicPatients(clinic.getClinicId(), null).size();
        return new ClinicRow(clinic.getClinicId(), clinic.getName(), clinic.getCity(), clinic.getState(),
                clinic.getStatus(), clinic.getStatus().name(), tone(clinic.getStatus()),
                admin == null ? "-" : admin.getEmail(),
                admin == null ? "-" : admin.getStatus().name(),
                doctors, patients, DATE.format(clinic.getCreatedAt()));
    }

    private static String tone(ClinicStatus status) {
        return switch (status) {
            case ACTIVE -> "success";
            case PENDING -> "warning";
            case SUSPENDED -> "danger";
        };
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record ClinicRow(Long id, String name, String city, String state, ClinicStatus status,
                            String statusLabel, String statusTone, String adminEmail, String adminStatus,
                            long doctors, long patients, String enrolledOn) {
    }

    public record ClinicDetail(Clinic clinic, List<User> admins, List<ClinicEnrollment> enrollments,
                               long doctors, long schedules, long patients, long appointments,
                               long prescriptions) {
    }
}
