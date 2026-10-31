package com.clinicflow.web.patient;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.Clinic;
import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.entity.PatientLinkRequest;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.ClinicStatus;
import com.clinicflow.domain.enums.LinkRequestMethod;
import com.clinicflow.domain.enums.LinkRequestStatus;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.repository.ClinicPatientRepository;
import com.clinicflow.domain.repository.ClinicRepository;
import com.clinicflow.domain.repository.PatientLinkRequestRepository;
import com.clinicflow.domain.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Patient-side half of the account-to-clinic linking workflow. A request is only a
 * request: no clinic link is created until a clinic administrator approves it.
 */
@Service
public class PatientLinkService {

    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final ClinicPatientRepository clinicPatientRepository;
    private final PatientLinkRequestRepository linkRequestRepository;
    private final AuditService auditService;

    public PatientLinkService(PatientRepository patientRepository,
                              ClinicRepository clinicRepository,
                              ClinicPatientRepository clinicPatientRepository,
                              PatientLinkRequestRepository linkRequestRepository,
                              AuditService auditService) {
        this.patientRepository = patientRepository;
        this.clinicRepository = clinicRepository;
        this.clinicPatientRepository = clinicPatientRepository;
        this.linkRequestRepository = linkRequestRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ClinicOption> availableClinics(Long userId) {
        Patient patient = patientRepository.findByUserUserId(userId).orElse(null);
        if (patient == null) {
            return List.of();
        }

        Set<Long> linked = new HashSet<>();
        clinicPatientRepository.findByPatientPatientId(patient.getPatientId())
                .forEach(link -> linked.add(link.getClinic().getClinicId()));

        Set<Long> pending = new HashSet<>();
        linkRequestRepository.findByPatientPatientIdOrderByCreatedAtDesc(patient.getPatientId()).stream()
                .filter(request -> request.getStatus() == LinkRequestStatus.PENDING)
                .forEach(request -> pending.add(request.getClinic().getClinicId()));

        return clinicRepository.findByStatusOrderByNameAsc(ClinicStatus.ACTIVE).stream()
                .filter(clinic -> !linked.contains(clinic.getClinicId()))
                .filter(clinic -> !pending.contains(clinic.getClinicId()))
                .map(clinic -> new ClinicOption(clinic.getClinicId(), clinic.getName(), clinic.getCity()))
                .toList();
    }

    @Transactional
    public RequestResult request(Long userId, Long clinicId) {
        Patient patient = patientRepository.findByUserUserId(userId).orElse(null);
        if (patient == null) {
            return RequestResult.error("Your patient record is not available yet.");
        }

        Clinic clinic = clinicRepository.findById(clinicId).orElse(null);
        if (clinic == null || clinic.getStatus() != ClinicStatus.ACTIVE) {
            return RequestResult.error("Select an active clinic.");
        }

        if (clinicPatientRepository.existsByClinicClinicIdAndPatientPatientId(
                clinic.getClinicId(), patient.getPatientId())) {
            return RequestResult.error("You are already linked to " + clinic.getName() + ".");
        }

        if (linkRequestRepository.existsByPatientPatientIdAndClinicClinicIdAndStatus(
                patient.getPatientId(), clinic.getClinicId(), LinkRequestStatus.PENDING)) {
            return RequestResult.error("A link request for " + clinic.getName() + " is already pending.");
        }

        PatientLinkRequest request = new PatientLinkRequest();
        request.setPatient(patient);
        request.setClinic(clinic);
        request.setMethod(LinkRequestMethod.CLINIC_APPROVAL);
        request.setStatus(LinkRequestStatus.PENDING);
        request.setAttempts(0);
        request.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        linkRequestRepository.save(request);

        auditService.record(userId, Role.PATIENT, "PATIENT_LINK_REQUESTED", "PatientLinkRequest",
                String.valueOf(request.getLinkRequestId()), patient.getPatientId(), AuditOutcome.SUCCESS);

        return RequestResult.success("Link request sent to " + clinic.getName()
                + ". The clinic will verify your identity before approving it.");
    }

    public record RequestResult(boolean success, String message) {

        public static RequestResult success(String message) {
            return new RequestResult(true, message);
        }

        public static RequestResult error(String message) {
            return new RequestResult(false, message);
        }
    }
}
