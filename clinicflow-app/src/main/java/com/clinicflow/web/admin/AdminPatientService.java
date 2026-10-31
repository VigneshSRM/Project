package com.clinicflow.web.admin;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.repository.AppointmentRepository;
import com.clinicflow.domain.repository.ClinicPatientRepository;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminPatientService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final PatientRepository patientRepository;
    private final ClinicPatientRepository clinicPatientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AuditService auditService;

    public AdminPatientService(PatientRepository patientRepository,
                               ClinicPatientRepository clinicPatientRepository,
                               AppointmentRepository appointmentRepository,
                               PrescriptionRepository prescriptionRepository,
                               AuditService auditService) {
        this.patientRepository = patientRepository;
        this.clinicPatientRepository = clinicPatientRepository;
        this.appointmentRepository = appointmentRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PatientRow> search(String query) {
        String normalized = query == null || query.isBlank() ? null : query.trim();
        return patientRepository.searchMasters(normalized).stream().map(this::toRow).toList();
    }

    @Transactional
    public PatientDetail detail(Long patientId, Long actorUserId) {
        Patient patient = patientRepository.findById(patientId)
                .filter(candidate -> candidate.getMergedInto() == null)
                .orElseThrow();
        List<String> clinics = clinicPatientRepository.findByPatientPatientId(patientId).stream()
                .map(link -> link.getClinic().getName())
                .sorted()
                .toList();
        long appointments = appointmentRepository.findByPatientPatientIdOrderBySlotStartDesc(patientId).size();
        long prescriptions = prescriptionRepository.findByPatientPatientIdOrderByIssueDateDesc(patientId).size();
        auditService.record(actorUserId, Role.SUPER_ADMIN, "ADMIN_PATIENT_VIEWED", "Patient",
                patient.getPublicPatientId(), patientId, AuditOutcome.SUCCESS);
        return new PatientDetail(patient.getPublicPatientId(), patient.getDisplayName(),
                patient.getDob() == null ? "-" : DATE.format(patient.getDob()),
                patient.getGender() == null ? "-" : patient.getGender().name(),
                maskEmail(patient.getEmail()), maskMobile(patient.getMobile()),
                patient.getBloodGroup() == null ? "-" : patient.getBloodGroup(),
                patient.getAllergies() == null ? "-" : patient.getAllergies(),
                patient.getConditions() == null ? "-" : patient.getConditions(),
                clinics, appointments, prescriptions,
                patient.getCreatedAt() == null ? "-" : DATE.format(patient.getCreatedAt()));
    }

    private PatientRow toRow(Patient patient) {
        List<String> clinics = clinicPatientRepository.findByPatientPatientId(patient.getPatientId()).stream()
                .map(link -> link.getClinic().getName())
                .sorted()
                .toList();
        return new PatientRow(patient.getPatientId(), patient.getPublicPatientId(),
                patient.getDisplayName(),
                patient.getDob() == null ? "-" : DATE.format(patient.getDob()),
                maskMobile(patient.getMobile()), maskEmail(patient.getEmail()),
                String.join(", ", clinics));
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "-";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private static String maskMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return "-";
        }
        String digits = mobile.replaceAll("[^0-9]", "");
        if (digits.length() <= 4) {
            return "****";
        }
        return "******" + digits.substring(digits.length() - 4);
    }

    public record PatientRow(Long id, String publicPatientId, String name, String dob, String mobile,
                             String email, String clinics) {
    }

    public record PatientDetail(String publicPatientId, String name, String dob, String gender,
                                String email, String mobile, String bloodGroup, String allergies,
                                String conditions, List<String> clinics, long appointments,
                                long prescriptions, String registeredOn) {
    }
}
