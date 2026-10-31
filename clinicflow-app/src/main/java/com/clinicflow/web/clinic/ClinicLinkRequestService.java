package com.clinicflow.web.clinic;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.Appointment;
import com.clinicflow.domain.entity.ClinicPatient;
import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.entity.PatientLinkRequest;
import com.clinicflow.domain.entity.Prescription;
import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.Gender;
import com.clinicflow.domain.enums.LinkRequestMethod;
import com.clinicflow.domain.enums.LinkRequestStatus;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.repository.AppointmentRepository;
import com.clinicflow.domain.repository.ClinicPatientRepository;
import com.clinicflow.domain.repository.PatientLinkRequestRepository;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.PrescriptionRepository;
import com.clinicflow.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Clinic-side half of account-to-clinic linking. Every read is scoped by the clinic
 * id from the authenticated clinic administrator's session, never from a request
 * parameter.
 *
 * <p>Approval has two modes. Without a clinic patient ID the account's own patient
 * row becomes the clinic patient. With a clinic patient ID the account is moved onto
 * that existing clinic record: appointments, prescriptions and prior link requests
 * are reassigned, missing profile fields are copied across, and the account's old
 * row is retained without a login so the audit trail is never broken.</p>
 */
@Service
public class ClinicLinkRequestService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final PatientLinkRequestRepository linkRequestRepository;
    private final ClinicPatientRepository clinicPatientRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ClinicLinkRequestService(PatientLinkRequestRepository linkRequestRepository,
                                    ClinicPatientRepository clinicPatientRepository,
                                    PatientRepository patientRepository,
                                    AppointmentRepository appointmentRepository,
                                    PrescriptionRepository prescriptionRepository,
                                    UserRepository userRepository,
                                    AuditService auditService) {
        this.linkRequestRepository = linkRequestRepository;
        this.clinicPatientRepository = clinicPatientRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ClinicLinkRequestListView list(Long clinicId) {
        List<PatientLinkRequest> requests = new ArrayList<>(
                linkRequestRepository.findByClinicClinicIdOrderByCreatedAtDesc(clinicId));
        requests.sort(Comparator
                .comparing((PatientLinkRequest request) -> request.getStatus() != LinkRequestStatus.PENDING)
                .thenComparing(PatientLinkRequest::getCreatedAt, Comparator.reverseOrder()));
        long pending = requests.stream()
                .filter(request -> request.getStatus() == LinkRequestStatus.PENDING)
                .count();
        List<ClinicLinkRequestListView.Row> rows = requests.stream()
                .map(this::toRow)
                .toList();
        return new ClinicLinkRequestListView(rows, pending, requests.size());
    }

    @Transactional(readOnly = true)
    public Optional<ClinicLinkRequestDetailView> detail(Long clinicId, Long requestId) {
        return linkRequestRepository.findById(requestId)
                .filter(request -> request.getClinic().getClinicId().equals(clinicId))
                .map(this::toDetail);
    }

    @Transactional
    public DecisionResult approve(Long clinicId, Long requestId, Long actorUserId,
                                  String clinicPatientPublicId, String note) {
        PatientLinkRequest request = findPending(clinicId, requestId);
        if (request == null) {
            return DecisionResult.error("This request is not available for your clinic or has already been resolved.");
        }

        Patient accountPatient = request.getPatient();
        Patient activePatient = accountPatient;
        boolean merged = false;

        if (clinicPatientPublicId != null && !clinicPatientPublicId.isBlank()) {
            String publicId = clinicPatientPublicId.trim();
            Patient selected = patientRepository.findByPublicPatientId(publicId).orElse(null);
            if (selected == null) {
                return DecisionResult.error("No patient record was found for clinic patient ID " + publicId + ".");
            }
            if (!selected.getPatientId().equals(accountPatient.getPatientId())) {
                if (!clinicPatientRepository.existsByClinicClinicIdAndPatientPatientId(
                        clinicId, selected.getPatientId())) {
                    return DecisionResult.error("That patient record is not linked to your clinic.");
                }
                User accountUser = accountPatient.getUser();
                if (accountUser == null) {
                    return DecisionResult.error("The account patient record is not attached to a user.");
                }
                if (selected.getUser() != null
                        && !selected.getUser().getUserId().equals(accountUser.getUserId())) {
                    return DecisionResult.error("That clinic record is already linked to another account.");
                }
                if (selected.getDob() == null || !selected.getDob().equals(accountPatient.getDob())) {
                    return DecisionResult.error("The selected clinic record has a different date of birth. "
                            + "Verify the identity or reject the request.");
                }
                activePatient = mergeIntoClinicRecord(accountPatient, selected, clinicId, request);
                merged = true;
            }
        }

        if (!clinicPatientRepository.existsByClinicClinicIdAndPatientPatientId(
                clinicId, activePatient.getPatientId())) {
            ClinicPatient link = new ClinicPatient();
            link.setClinic(request.getClinic());
            link.setPatient(activePatient);
            link.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            clinicPatientRepository.save(link);
        }

        request.setPatient(activePatient);
        request.setClinicPatientRecord(activePatient);
        request.setStatus(LinkRequestStatus.APPROVED);
        request.setResolvedAt(LocalDateTime.now(ZoneOffset.UTC));
        request.setResolvedBy(userRepository.findById(actorUserId).orElse(null));
        request.setResolutionNote(trimToNull(note));
        linkRequestRepository.save(request);

        if (merged) {
            auditService.record(actorUserId, Role.CLINIC_ADMIN, "PATIENT_RECORD_MERGED",
                    "Patient", accountPatient.getPublicPatientId(), activePatient.getPatientId(),
                    AuditOutcome.SUCCESS);
        }
        auditService.record(actorUserId, Role.CLINIC_ADMIN, "PATIENT_LINK_APPROVED",
                "PatientLinkRequest", String.valueOf(request.getLinkRequestId()),
                activePatient.getPatientId(), AuditOutcome.SUCCESS);

        return DecisionResult.success("The patient account is now linked to "
                + request.getClinic().getName() + ".");
    }

    @Transactional
    public DecisionResult reject(Long clinicId, Long requestId, Long actorUserId, String note) {
        PatientLinkRequest request = findPending(clinicId, requestId);
        if (request == null) {
            return DecisionResult.error("This request is not available for your clinic or has already been resolved.");
        }

        request.setStatus(LinkRequestStatus.REJECTED);
        request.setResolvedAt(LocalDateTime.now(ZoneOffset.UTC));
        request.setResolvedBy(userRepository.findById(actorUserId).orElse(null));
        request.setResolutionNote(trimToNull(note));
        linkRequestRepository.save(request);

        auditService.record(actorUserId, Role.CLINIC_ADMIN, "PATIENT_LINK_REJECTED",
                "PatientLinkRequest", String.valueOf(request.getLinkRequestId()),
                request.getPatient().getPatientId(), AuditOutcome.SUCCESS);

        return DecisionResult.success("The link request was rejected. The patient will see the reason on their profile.");
    }

    /**
     * Moves the account identity onto an existing clinic patient record. Keeps the
     * old account row (without a login) so audit logs that reference it remain valid.
     */
    private Patient mergeIntoClinicRecord(Patient account, Patient clinicRecord, Long clinicId,
                                          PatientLinkRequest currentRequest) {
        User accountUser = account.getUser();

        List<ClinicPatient> links = clinicPatientRepository.findByPatientPatientId(account.getPatientId());
        for (ClinicPatient link : links) {
            boolean sameClinic = link.getClinic().getClinicId().equals(clinicId);
            boolean targetAlreadyLinked = clinicPatientRepository.existsByClinicClinicIdAndPatientPatientId(
                    link.getClinic().getClinicId(), clinicRecord.getPatientId());
            if (sameClinic || targetAlreadyLinked) {
                clinicPatientRepository.delete(link);
            } else {
                link.setPatient(clinicRecord);
                clinicPatientRepository.save(link);
            }
        }

        List<Appointment> appointments = appointmentRepository
                .findByPatientPatientIdOrderBySlotStartDesc(account.getPatientId());
        appointments.forEach(appointment -> appointment.setPatient(clinicRecord));
        appointmentRepository.saveAll(appointments);

        List<Prescription> prescriptions = prescriptionRepository
                .findByPatientPatientIdOrderByIssueDateDesc(account.getPatientId());
        prescriptions.forEach(prescription -> prescription.setPatient(clinicRecord));
        prescriptionRepository.saveAll(prescriptions);

        List<PatientLinkRequest> requests = linkRequestRepository
                .findByPatientPatientIdOrderByCreatedAtDesc(account.getPatientId());
        requests.forEach(existing -> existing.setPatient(clinicRecord));
        linkRequestRepository.saveAll(requests);
        if (requests.stream().noneMatch(existing -> existing.getLinkRequestId().equals(currentRequest.getLinkRequestId()))) {
            currentRequest.setPatient(clinicRecord);
        }

        clinicRecord.setEmail(accountUser.getEmail());
        clinicRecord.setMobile(copyIfBlank(clinicRecord.getMobile(), account.getMobile()));
        if (clinicRecord.getGender() == null || clinicRecord.getGender() == Gender.UNKNOWN) {
            clinicRecord.setGender(account.getGender());
        }
        clinicRecord.setAddressLine1(copyIfBlank(clinicRecord.getAddressLine1(), account.getAddressLine1()));
        clinicRecord.setAddressLine2(copyIfBlank(clinicRecord.getAddressLine2(), account.getAddressLine2()));
        clinicRecord.setCity(copyIfBlank(clinicRecord.getCity(), account.getCity()));
        clinicRecord.setDistrict(copyIfBlank(clinicRecord.getDistrict(), account.getDistrict()));
        clinicRecord.setState(copyIfBlank(clinicRecord.getState(), account.getState()));
        clinicRecord.setPostalCode(copyIfBlank(clinicRecord.getPostalCode(), account.getPostalCode()));
        clinicRecord.setBloodGroup(copyIfBlank(clinicRecord.getBloodGroup(), account.getBloodGroup()));
        clinicRecord.setAllergies(copyIfBlank(clinicRecord.getAllergies(), account.getAllergies()));
        clinicRecord.setConditions(copyIfBlank(clinicRecord.getConditions(), account.getConditions()));
        clinicRecord.setEmergencyName(copyIfBlank(clinicRecord.getEmergencyName(), account.getEmergencyName()));
        clinicRecord.setEmergencyPhone(copyIfBlank(clinicRecord.getEmergencyPhone(), account.getEmergencyPhone()));
        clinicRecord.setUser(accountUser);
        clinicRecord.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        patientRepository.save(clinicRecord);

        account.setUser(null);
        account.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        patientRepository.save(account);

        return clinicRecord;
    }

    private PatientLinkRequest findPending(Long clinicId, Long requestId) {
        PatientLinkRequest request = linkRequestRepository.findById(requestId).orElse(null);
        if (request == null
                || !request.getClinic().getClinicId().equals(clinicId)
                || request.getStatus() != LinkRequestStatus.PENDING) {
            return null;
        }
        return request;
    }

    private ClinicLinkRequestListView.Row toRow(PatientLinkRequest request) {
        Patient patient = request.getPatient();
        return new ClinicLinkRequestListView.Row(
                request.getLinkRequestId(),
                patient.getDisplayName(),
                patient.getPublicPatientId(),
                patient.getMobile() == null ? "-" : patient.getMobile(),
                formatDateTime(request.getCreatedAt()),
                methodLabel(request.getMethod()),
                statusLabel(request.getStatus()),
                statusTone(request.getStatus()));
    }

    private ClinicLinkRequestDetailView toDetail(PatientLinkRequest request) {
        Patient patient = request.getPatient();
        List<ClinicLinkRequestDetailView.DuplicateMatch> duplicates =
                clinicPatientRepository.findByClinicClinicId(request.getClinic().getClinicId()).stream()
                        .map(ClinicPatient::getPatient)
                        .filter(other -> !other.getPatientId().equals(patient.getPatientId()))
                        .filter(other -> sameNameAndDob(other, patient))
                        .map(other -> new ClinicLinkRequestDetailView.DuplicateMatch(
                                other.getPublicPatientId(), other.getDisplayName(),
                                other.getDob() == null ? "-" : DATE.format(other.getDob()),
                                other.getMobile() == null ? "-" : other.getMobile(),
                                other.getCreatedAt() == null ? "-" : DATE.format(other.getCreatedAt())))
                        .sorted(Comparator.comparing(ClinicLinkRequestDetailView.DuplicateMatch::publicPatientId))
                        .toList();

        return new ClinicLinkRequestDetailView(
                request.getLinkRequestId(),
                request.getClinic().getName(),
                patient.getDisplayName(),
                patient.getPublicPatientId(),
                patient.getDob() == null ? "-" : DATE.format(patient.getDob()),
                genderLabel(patient.getGender()),
                patient.getMobile() == null ? "-" : patient.getMobile(),
                patient.getEmail() == null ? "-" : patient.getEmail(),
                blankToDash(patient.getAddressLine1()),
                blankToDash(patient.getAddressLine2()),
                blankToDash(patient.getCity()),
                blankToDash(patient.getDistrict()),
                blankToDash(patient.getState()),
                blankToDash(patient.getPostalCode()),
                methodLabel(request.getMethod()),
                statusLabel(request.getStatus()),
                statusTone(request.getStatus()),
                formatDateTime(request.getCreatedAt()),
                request.getResolvedAt() == null ? "-" : formatDateTime(request.getResolvedAt()),
                request.getResolvedBy() == null ? "-" : request.getResolvedBy().getEmail(),
                request.getResolutionNote(),
                request.getStatus() == LinkRequestStatus.PENDING,
                duplicates);
    }

    private static boolean sameNameAndDob(Patient left, Patient right) {
        return left.getFirstName().equalsIgnoreCase(right.getFirstName())
                && left.getLastName().equalsIgnoreCase(right.getLastName())
                && left.getDob() != null && left.getDob().equals(right.getDob());
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME.format(value);
    }

    private static String genderLabel(Gender gender) {
        if (gender == null) {
            return "-";
        }
        return switch (gender) {
            case MALE -> "Male";
            case FEMALE -> "Female";
            case OTHER -> "Other";
            case UNKNOWN -> "Prefer not to say";
        };
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

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String copyIfBlank(String target, String source) {
        return target == null || target.isBlank() ? source : target;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record DecisionResult(boolean success, String message) {

        public static DecisionResult success(String message) {
            return new DecisionResult(true, message);
        }

        public static DecisionResult error(String message) {
            return new DecisionResult(false, message);
        }
    }
}
