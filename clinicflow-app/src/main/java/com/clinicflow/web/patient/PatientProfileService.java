package com.clinicflow.web.patient;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.ClinicPatient;
import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.entity.PatientLinkRequest;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.Gender;
import com.clinicflow.domain.enums.LinkRequestMethod;
import com.clinicflow.domain.enums.LinkRequestStatus;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.repository.ClinicPatientRepository;
import com.clinicflow.domain.repository.PatientLinkRequestRepository;
import com.clinicflow.domain.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class PatientProfileService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final PatientRepository patientRepository;
    private final ClinicPatientRepository clinicPatientRepository;
    private final PatientLinkRequestRepository linkRequestRepository;
    private final AuditService auditService;

    public PatientProfileService(PatientRepository patientRepository,
                                 ClinicPatientRepository clinicPatientRepository,
                                 PatientLinkRequestRepository linkRequestRepository,
                                 AuditService auditService) {
        this.patientRepository = patientRepository;
        this.clinicPatientRepository = clinicPatientRepository;
        this.linkRequestRepository = linkRequestRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Optional<PatientProfileView> load(Long userId) {
        return patientRepository.findByUserUserId(userId).map(this::toView);
    }

    @Transactional
    public boolean update(Long userId, PatientProfileForm form) {
        Optional<Patient> maybePatient = patientRepository.findByUserUserId(userId);
        if (maybePatient.isEmpty()) {
            return false;
        }
        Patient patient = maybePatient.get();
        patient.setFirstName(form.getFirstName().trim());
        patient.setLastName(form.getLastName().trim());
        patient.setGender(form.getGender() == null ? Gender.UNKNOWN : form.getGender());
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
        patient.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        patientRepository.save(patient);

        auditService.record(userId, Role.PATIENT, "PATIENT_PROFILE_UPDATED", "Patient",
                patient.getPublicPatientId(), patient.getPatientId(), AuditOutcome.SUCCESS);
        return true;
    }

    private PatientProfileView toView(Patient patient) {
        List<PatientProfileView.LinkedClinicView> linked = clinicPatientRepository
                .findByPatientPatientId(patient.getPatientId()).stream()
                .map(this::toLinkedClinic)
                .toList();
        List<PatientProfileView.LinkRequestView> requests = linkRequestRepository
                .findByPatientPatientIdOrderByCreatedAtDesc(patient.getPatientId()).stream()
                .map(this::toLinkRequest)
                .toList();
        return new PatientProfileView(patient, linked, requests);
    }

    private PatientProfileView.LinkedClinicView toLinkedClinic(ClinicPatient link) {
        return new PatientProfileView.LinkedClinicView(
                link.getClinic().getName(), link.getClinic().getCity(),
                link.getCreatedAt() == null ? "-" : DATE.format(link.getCreatedAt()));
    }

    private PatientProfileView.LinkRequestView toLinkRequest(PatientLinkRequest request) {
        return new PatientProfileView.LinkRequestView(
                request.getClinic().getName(), methodLabel(request.getMethod()),
                statusLabel(request.getStatus()), statusTone(request.getStatus()),
                request.getCreatedAt() == null ? "-" : DATE_TIME.format(request.getCreatedAt()),
                request.getResolvedAt() == null ? "-" : DATE_TIME.format(request.getResolvedAt()),
                request.getResolutionNote());
    }

    private static String methodLabel(LinkRequestMethod method) {
        return switch (method) {
            case OTP -> "One-time code";
            case CLINIC_APPROVAL -> "Clinic approval";
            case CODE -> "Link code";
        };
    }

    private static String statusLabel(LinkRequestStatus status) {
        return switch (status) {
            case PENDING -> "Pending";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            case EXPIRED -> "Expired";
        };
    }

    private static String statusTone(LinkRequestStatus status) {
        return switch (status) {
            case PENDING -> "warning";
            case APPROVED -> "success";
            case REJECTED -> "danger";
            case EXPIRED -> "neutral";
        };
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
