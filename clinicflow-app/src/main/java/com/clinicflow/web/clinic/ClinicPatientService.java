package com.clinicflow.web.clinic;

import com.clinicflow.audit.AuditService;
import com.clinicflow.common.PublicIdGenerator;
import com.clinicflow.domain.entity.Appointment;
import com.clinicflow.domain.entity.Clinic;
import com.clinicflow.domain.entity.ClinicPatient;
import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.entity.Prescription;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.PatientSource;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.repository.AppointmentRepository;
import com.clinicflow.domain.repository.ClinicPatientRepository;
import com.clinicflow.domain.repository.ClinicRepository;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class ClinicPatientService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final ClinicPatientRepository clinicPatientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AuditService auditService;

    public ClinicPatientService(PatientRepository patientRepository, ClinicRepository clinicRepository,
                                ClinicPatientRepository clinicPatientRepository,
                                AppointmentRepository appointmentRepository,
                                PrescriptionRepository prescriptionRepository,
                                AuditService auditService) {
        this.patientRepository = patientRepository;
        this.clinicRepository = clinicRepository;
        this.clinicPatientRepository = clinicPatientRepository;
        this.appointmentRepository = appointmentRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PatientRow> list(Long clinicId, String query) {
        String normalized = query == null || query.isBlank() ? null : query.trim();
        return patientRepository.searchClinicPatients(clinicId, normalized).stream()
                .map(patient -> toRow(patient, clinicId))
                .toList();
    }

    @Transactional
    public PatientDetail detail(Long clinicId, Long patientId, Long actorUserId) {
        Patient patient = requireClinicPatient(clinicId, patientId);
        List<Appointment> appointments = appointmentRepository
                .findByPatientPatientIdOrderBySlotStartDesc(patientId).stream()
                .filter(appointment -> appointment.getClinic().getClinicId().equals(clinicId))
                .toList();
        List<Prescription> prescriptions = prescriptionRepository
                .findByPatientPatientIdOrderByIssueDateDesc(patientId).stream()
                .filter(prescription -> prescription.getClinic().getClinicId().equals(clinicId))
                .toList();
        auditService.record(actorUserId, Role.CLINIC_ADMIN, "PATIENT_MASTER_VIEWED", "Patient",
                patient.getPublicPatientId(), patientId, AuditOutcome.SUCCESS);
        return new PatientDetail(patient.getPatientId(), patient.getPublicPatientId(),
                patient.getDisplayName(), patient.getDob(), patient.getGender(), patient.getMobile(),
                patient.getEmail(), patient.getAddressLine1(), patient.getAddressLine2(),
                patient.getCity(), patient.getDistrict(), patient.getState(), patient.getPostalCode(),
                patient.getBloodGroup(), patient.getAllergies(), patient.getConditions(),
                patient.getEmergencyName(), patient.getEmergencyPhone(),
                patient.getUser() == null ? null : patient.getUser().getEmail(),
                appointments.stream().map(appointment -> new AppointmentRow(
                        appointment.getAppointmentId(), appointment.getPublicAppointmentId(),
                        DATE_TIME.format(appointment.getSlotStart()), appointment.getDoctor().getName(),
                        appointment.getStatus().name(), tone(appointment.getStatus().name()))).toList(),
                prescriptions.stream().map(prescription -> new PrescriptionRow(
                        prescription.getPrescriptionId(), prescription.getPrescriptionCode(),
                        DATE.format(prescription.getIssueDate()), prescription.getStatus().name(),
                        prescription.getExpiryDate().isBefore(LocalDateTime.now()) ? "EXPIRED" : null)).toList());
    }

    @Transactional
    public Long create(Long clinicId, PatientForm form, Long actorUserId) {
        Clinic clinic = clinicRepository.findById(clinicId).orElseThrow();
        Patient patient = new Patient();
        apply(patient, form);
        patient.setSourceFlag(PatientSource.CLINIC_ENTERED);
        patient.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        patient.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        // The public ID is derived from the generated identity value. Persist with a
        // unique placeholder first, then replace it in the same transaction.
        patient.setPublicPatientId("TMP" + UUID.randomUUID().toString().substring(0, 12));
        patient = patientRepository.saveAndFlush(patient);
        patient.setPublicPatientId(PublicIdGenerator.patientId(patient.getPatientId()));
        patientRepository.save(patient);
        link(clinic, patient);
        auditService.record(actorUserId, Role.CLINIC_ADMIN, "PATIENT_MASTER_CREATED", "Patient",
                patient.getPublicPatientId(), patient.getPatientId(), AuditOutcome.SUCCESS);
        return patient.getPatientId();
    }

    @Transactional
    public void update(Long clinicId, Long patientId, PatientForm form, Long actorUserId) {
        Patient patient = requireClinicPatient(clinicId, patientId);
        apply(patient, form);
        patient.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        patientRepository.save(patient);
        auditService.record(actorUserId, Role.CLINIC_ADMIN, "PATIENT_MASTER_UPDATED", "Patient",
                patient.getPublicPatientId(), patientId, AuditOutcome.SUCCESS);
    }

    @Transactional
    public Patient linkExisting(Long clinicId, String publicPatientId, Long actorUserId) {
        Patient patient = patientRepository.findByPublicPatientId(publicPatientId == null ? "" : publicPatientId.trim())
                .orElseThrow(() -> new IllegalArgumentException("No patient exists with that public patient ID."));
        if (patient.getMergedInto() != null) {
            throw new IllegalArgumentException("This patient record was merged into another record.");
        }
        if (!clinicPatientRepository.existsByClinicClinicIdAndPatientPatientId(clinicId, patient.getPatientId())) {
            Clinic clinic = clinicRepository.findById(clinicId).orElseThrow();
            link(clinic, patient);
            auditService.record(actorUserId, Role.CLINIC_ADMIN, "PATIENT_LINKED_TO_CLINIC", "Patient",
                    patient.getPublicPatientId(), patient.getPatientId(), AuditOutcome.SUCCESS);
        }
        return patient;
    }

    @Transactional(readOnly = true)
    public PatientForm formFor(Long clinicId, Long patientId) {
        Patient patient = requireClinicPatient(clinicId, patientId);
        PatientForm form = new PatientForm();
        form.setFirstName(patient.getFirstName());
        form.setLastName(patient.getLastName());
        form.setDob(patient.getDob());
        form.setGender(patient.getGender());
        form.setMobile(patient.getMobile());
        form.setEmail(patient.getEmail());
        form.setAddressLine1(patient.getAddressLine1());
        form.setAddressLine2(patient.getAddressLine2());
        form.setCity(patient.getCity());
        form.setDistrict(patient.getDistrict());
        form.setState(patient.getState());
        form.setPostalCode(patient.getPostalCode());
        form.setBloodGroup(patient.getBloodGroup());
        form.setAllergies(patient.getAllergies());
        form.setConditions(patient.getConditions());
        form.setEmergencyName(patient.getEmergencyName());
        form.setEmergencyPhone(patient.getEmergencyPhone());
        return form;
    }

    private Patient requireClinicPatient(Long clinicId, Long patientId) {
        if (!clinicPatientRepository.existsByClinicClinicIdAndPatientPatientId(clinicId, patientId)) {
            throw new IllegalArgumentException("Patient is not part of this clinic.");
        }
        return patientRepository.findById(patientId)
                .filter(patient -> patient.getMergedInto() == null)
                .orElseThrow(() -> new IllegalArgumentException("Patient record not found."));
    }

    private void link(Clinic clinic, Patient patient) {
        ClinicPatient link = new ClinicPatient();
        link.setClinic(clinic);
        link.setPatient(patient);
        link.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        clinicPatientRepository.save(link);
    }

    private static void apply(Patient patient, PatientForm form) {
        patient.setFirstName(form.getFirstName().trim());
        patient.setLastName(form.getLastName().trim());
        patient.setDob(form.getDob());
        patient.setGender(form.getGender());
        patient.setMobile(trimToNull(form.getMobile()));
        patient.setEmail(trimToNull(form.getEmail()));
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
    }

    private PatientRow toRow(Patient patient, Long clinicId) {
        return new PatientRow(patient.getPatientId(), patient.getPublicPatientId(),
                patient.getDisplayName(), DATE.format(patient.getDob()),
                patient.getGender() == null ? "—" : patient.getGender().name(),
                patient.getMobile() == null ? "—" : patient.getMobile(),
                patient.getEmail() == null ? "—" : patient.getEmail(),
                patient.getUser() == null ? "Walk-in" : "Account linked",
                clinicPatientRepository.findByClinicClinicIdAndPatientPatientId(clinicId, patient.getPatientId())
                        .map(link -> DATE.format(link.getCreatedAt())).orElse("—"));
    }

    private static String tone(String status) {
        return switch (status) {
            case "BOOKED", "CONFIRMED", "CHECKED_IN", "IN_CONSULTATION" -> "info";
            case "COMPLETED" -> "success";
            case "CANCELLED", "NO_SHOW" -> "danger";
            default -> "neutral";
        };
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record PatientRow(Long id, String publicPatientId, String name, String dob, String gender,
                             String mobile, String email, String account, String linkedOn) {
    }

    public record AppointmentRow(Long id, String publicId, String when, String doctor, String status,
                                 String tone) {
    }

    public record PrescriptionRow(Long id, String code, String issued, String status, String watermark) {
    }

    public record PatientDetail(Long id, String publicPatientId, String name, java.time.LocalDate dob,
                                com.clinicflow.domain.enums.Gender gender, String mobile, String email,
                                String addressLine1, String addressLine2, String city, String district,
                                String state, String postalCode, String bloodGroup, String allergies,
                                String conditions, String emergencyName, String emergencyPhone,
                                String accountEmail, List<AppointmentRow> appointments,
                                List<PrescriptionRow> prescriptions) {
    }
}
