package com.clinicflow.web.prescription;

import com.clinicflow.audit.AuditService;
import com.clinicflow.common.PublicIdGenerator;
import com.clinicflow.domain.entity.Consultation;
import com.clinicflow.domain.entity.DoctorUserLink;
import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.entity.Prescription;
import com.clinicflow.domain.entity.PrescriptionDispense;
import com.clinicflow.domain.entity.PrescriptionDispenseItem;
import com.clinicflow.domain.entity.PrescriptionItem;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.PrescriptionStatus;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.repository.ConsultationRepository;
import com.clinicflow.domain.repository.DoctorUserLinkRepository;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.PrescriptionDispenseItemRepository;
import com.clinicflow.domain.repository.PrescriptionDispenseRepository;
import com.clinicflow.domain.repository.PrescriptionItemRepository;
import com.clinicflow.domain.repository.PrescriptionRepository;
import com.clinicflow.notifications.PrescriptionIssuedEvent;
import com.clinicflow.web.doctor.PrescriptionForm;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PrescriptionService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository itemRepository;
    private final PrescriptionDispenseRepository dispenseRepository;
    private final PrescriptionDispenseItemRepository dispenseItemRepository;
    private final ConsultationRepository consultationRepository;
    private final DoctorUserLinkRepository doctorUserLinkRepository;
    private final PatientRepository patientRepository;
    private final ApplicationEventPublisher events;
    private final AuditService auditService;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               PrescriptionItemRepository itemRepository,
                               PrescriptionDispenseRepository dispenseRepository,
                               PrescriptionDispenseItemRepository dispenseItemRepository,
                               ConsultationRepository consultationRepository,
                               DoctorUserLinkRepository doctorUserLinkRepository,
                               PatientRepository patientRepository,
                               ApplicationEventPublisher events,
                               AuditService auditService) {
        this.prescriptionRepository = prescriptionRepository;
        this.itemRepository = itemRepository;
        this.dispenseRepository = dispenseRepository;
        this.dispenseItemRepository = dispenseItemRepository;
        this.consultationRepository = consultationRepository;
        this.doctorUserLinkRepository = doctorUserLinkRepository;
        this.patientRepository = patientRepository;
        this.events = events;
        this.auditService = auditService;
    }

    @Transactional
    public Long create(Long userId, Long consultationId, PrescriptionForm form, Long actorUserId) {
        DoctorUserLink link = doctorUserLinkRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No doctor profile is linked to this account."));
        Consultation consultation = consultationRepository.findForUpdate(consultationId)
                .orElseThrow(() -> new IllegalArgumentException("Consultation not found."));
        if (!consultation.getAppointment().getDoctor().getDoctorId()
                .equals(link.getDoctor().getDoctorId())) {
            throw new IllegalArgumentException("Consultation not found.");
        }
        if (prescriptionRepository.existsByConsultationConsultationId(consultationId)) {
            throw new IllegalArgumentException("A prescription has already been issued for this consultation.");
        }
        List<ValidatedItem> items = validateItems(form);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Add at least one medicine with a quantity.");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Prescription prescription = new Prescription();
        prescription.setConsultation(consultation);
        prescription.setPatient(consultation.getAppointment().getPatient());
        prescription.setClinic(consultation.getAppointment().getClinic());
        prescription.setDoctor(consultation.getAppointment().getDoctor());
        prescription.setIssueDate(now);
        prescription.setExpiryDate(now.plusDays(form.getValidDays()));
        prescription.setStatus(PrescriptionStatus.VALID);
        prescription.setCreatedAt(now);
        prescription.setUpdatedAt(now);
        prescription.setPrescriptionCode(uniqueCode());
        prescription = prescriptionRepository.saveAndFlush(prescription);
        for (ValidatedItem item : items) {
            PrescriptionItem entity = new PrescriptionItem();
            entity.setPrescription(prescription);
            entity.setMedicine(item.medicine());
            entity.setDosage(item.dosage());
            entity.setFrequency(item.frequency());
            entity.setDuration(item.duration());
            entity.setQuantity(item.quantity());
            entity.setQuantityUnit(item.quantityUnit());
            entity.setQuantityDispensed(0);
            entity.setInstructions(item.instructions());
            entity.setLifetime(item.lifetime());
            itemRepository.save(entity);
        }
        auditService.record(actorUserId, Role.DOCTOR, "PRESCRIPTION_ISSUED", "Prescription",
                prescription.getPrescriptionCode(), prescription.getPatient().getPatientId(),
                AuditOutcome.SUCCESS);
        publish(prescription, "issued");
        return prescription.getPrescriptionId();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionRow> clinicList(Long clinicId) {
        return prescriptionRepository.findByClinicClinicIdOrderByIssueDateDesc(clinicId).stream()
                .map(this::toRow).toList();
    }

    @Transactional(readOnly = true)
    public PrescriptionView clinicDetail(Long clinicId, Long prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .filter(item -> item.getClinic().getClinicId().equals(clinicId))
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found."));
        return view(prescription);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionRow> doctorList(Long userId) {
        Long doctorId = doctorUserLinkRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No doctor profile is linked to this account."))
                .getDoctor().getDoctorId();
        return prescriptionRepository.findByDoctorDoctorIdOrderByIssueDateDesc(doctorId).stream()
                .map(this::toRow).toList();
    }

    @Transactional(readOnly = true)
    public PrescriptionView doctorDetail(Long userId, Long prescriptionId) {
        Long doctorId = doctorUserLinkRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No doctor profile is linked to this account."))
                .getDoctor().getDoctorId();
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .filter(item -> item.getDoctor().getDoctorId().equals(doctorId))
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found."));
        return view(prescription);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionRow> patientList(Long userId) {
        Patient patient = requirePatient(userId);
        return prescriptionRepository.findByPatientPatientIdOrderByIssueDateDesc(patient.getPatientId())
                .stream().map(this::toRow).toList();
    }

    @Transactional
    public PrescriptionView patientDetail(Long userId, Long prescriptionId, Long actorUserId) {
        Patient patient = requirePatient(userId);
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .filter(item -> item.getPatient().getPatientId().equals(patient.getPatientId()))
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found."));
        auditService.record(actorUserId, Role.PATIENT, "PRESCRIPTION_VIEWED", "Prescription",
                prescription.getPrescriptionCode(), patient.getPatientId(), AuditOutcome.SUCCESS);
        return view(prescription);
    }

    @Transactional
    public void cancelByClinic(Long clinicId, Long prescriptionId, String reason, Long actorUserId) {
        Prescription prescription = prescriptionRepository.findForUpdate(prescriptionId)
                .filter(item -> item.getClinic().getClinicId().equals(clinicId))
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found."));
        cancel(prescription, reason, actorUserId, Role.CLINIC_ADMIN);
    }

    @Transactional
    public void cancelByDoctor(Long userId, Long prescriptionId, String reason, Long actorUserId) {
        Long doctorId = doctorUserLinkRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No doctor profile is linked to this account."))
                .getDoctor().getDoctorId();
        Prescription prescription = prescriptionRepository.findForUpdate(prescriptionId)
                .filter(item -> item.getDoctor().getDoctorId().equals(doctorId))
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found."));
        cancel(prescription, reason, actorUserId, Role.DOCTOR);
    }

    @Transactional
    public void resendForPatient(Long userId, Long prescriptionId, Long actorUserId) {
        Patient patient = requirePatient(userId);
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .filter(item -> item.getPatient().getPatientId().equals(patient.getPatientId()))
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found."));
        resend(prescription, actorUserId, Role.PATIENT);
    }

    @Transactional
    public void resendForClinic(Long clinicId, Long prescriptionId, Long actorUserId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .filter(item -> item.getClinic().getClinicId().equals(clinicId))
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found."));
        resend(prescription, actorUserId, Role.CLINIC_ADMIN);
    }

    @Transactional(readOnly = true)
    public PrescriptionView viewForPharmacy(Long prescriptionId) {
        return prescriptionRepository.findById(prescriptionId).map(this::view)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found."));
    }

    private void cancel(Prescription prescription, String reason, Long actorUserId, Role role) {
        if (prescription.getStatus() == PrescriptionStatus.CANCELLED) {
            throw new IllegalArgumentException("This prescription is already cancelled.");
        }
        if (prescription.getExpiryDate().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException("An expired prescription cannot be cancelled.");
        }
        prescription.setStatus(PrescriptionStatus.CANCELLED);
        prescription.setCancelReason(reason == null || reason.isBlank()
                ? "Cancelled by " + role.name().replace('_', ' ').toLowerCase() : reason.trim());
        prescription.setCancelledAt(LocalDateTime.now(ZoneOffset.UTC));
        prescription.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        prescriptionRepository.save(prescription);
        auditService.record(actorUserId, role, "PRESCRIPTION_CANCELLED", "Prescription",
                prescription.getPrescriptionCode(), prescription.getPatient().getPatientId(),
                AuditOutcome.SUCCESS);
    }

    private void resend(Prescription prescription, Long actorUserId, Role role) {
        if (prescription.getPatient().getEmail() == null || prescription.getPatient().getEmail().isBlank()) {
            throw new IllegalArgumentException("This patient has no email address on record.");
        }
        publish(prescription, "manual-" + UUID.randomUUID());
        auditService.record(actorUserId, role, "PRESCRIPTION_EMAIL_QUEUED", "Prescription",
                prescription.getPrescriptionCode(), prescription.getPatient().getPatientId(),
                AuditOutcome.SUCCESS);
    }

    private void publish(Prescription prescription, String suffix) {
        events.publishEvent(new PrescriptionIssuedEvent(prescription.getPrescriptionId(),
                prescription.getPatient().getEmail(), prescription.getPatient().getDisplayName(),
                prescription.getPrescriptionCode(), prescription.getClinic().getName(),
                prescription.getDoctor().getName(), DATE.format(prescription.getIssueDate()),
                DATE.format(prescription.getExpiryDate()), suffix));
    }

    private PrescriptionRow toRow(Prescription prescription) {
        Fulfilment fulfilment = fulfilment(prescription.getPrescriptionId());
        return new PrescriptionRow(prescription.getPrescriptionId(), prescription.getPrescriptionCode(),
                DATE.format(prescription.getIssueDate()), DATE.format(prescription.getExpiryDate()),
                prescription.getPatient().getDisplayName(), prescription.getPatient().getPublicPatientId(),
                prescription.getDoctor().getName(), prescription.getStatus().name(),
                watermark(prescription), fulfilment.label(), fulfilment.tone());
    }

    private PrescriptionView view(Prescription prescription) {
        List<PrescriptionItem> items = itemRepository
                .findByPrescriptionPrescriptionId(prescription.getPrescriptionId());
        List<ItemView> itemViews = items.stream().map(item -> new ItemView(item.getPrescriptionItemId(),
                item.getMedicine(), item.getDosage(), item.getFrequency(), item.getDuration(),
                item.getQuantity(), item.getQuantityUnit(), item.getQuantityDispensed(),
                item.getRemainingQuantity(), item.getInstructions(), item.isLifetime())).toList();
        List<DispenseView> dispenses = dispenseRepository
                .findByPrescriptionPrescriptionId(prescription.getPrescriptionId()).stream()
                .map(this::toDispenseView).toList();
        Fulfilment fulfilment = fulfilment(prescription.getPrescriptionId());
        return new PrescriptionView(prescription.getPrescriptionId(), prescription.getPrescriptionCode(),
                DATE_TIME.format(prescription.getIssueDate()), DATE.format(prescription.getExpiryDate()),
                prescription.getStatus().name(), watermark(prescription),
                prescription.getPatient().getPatientId(),
                prescription.getPatient().getDisplayName(), prescription.getPatient().getPublicPatientId(),
                prescription.getPatient().getDob(), prescription.getPatient().getMobile(),
                prescription.getPatient().getEmail(), prescription.getClinic().getName(),
                prescription.getClinic().getAddressLine1(), prescription.getClinic().getCity(),
                prescription.getClinic().getPhone(), prescription.getDoctor().getName(),
                prescription.getDoctor().getSpecialization(), prescription.getConsultation().getConsultationId(),
                prescription.getCancelReason(),
                prescription.getCancelledAt() == null ? null : DATE_TIME.format(prescription.getCancelledAt()),
                fulfilment.label(), fulfilment.tone(), itemViews, dispenses);
    }

    private DispenseView toDispenseView(PrescriptionDispense dispense) {
        List<DispenseItemView> items = dispenseItemRepository
                .findByDispenseDispenseId(dispense.getDispenseId()).stream()
                .map(item -> new DispenseItemView(item.getPrescriptionItem().getMedicine(),
                        item.getQuantityDispensed())).toList();
        return new DispenseView(dispense.getDispenseId(), DATE_TIME.format(dispense.getDispensedAt()),
                dispense.getPharmacy().getName(), dispense.getPharmacyUser().getUser().getEmail(),
                dispense.getStatus().name(), dispense.getNotes(), items);
    }

    private Fulfilment fulfilment(Long prescriptionId) {
        List<PrescriptionItem> items = itemRepository.findByPrescriptionPrescriptionId(prescriptionId);
        int total = items.stream().mapToInt(PrescriptionItem::getQuantity).sum();
        int dispensed = items.stream().mapToInt(PrescriptionItem::getQuantityDispensed).sum();
        if (dispensed <= 0) {
            return new Fulfilment("Not dispensed", "neutral");
        }
        if (dispensed >= total) {
            return new Fulfilment("Fully dispensed", "success");
        }
        return new Fulfilment("Partially dispensed", "warning");
    }

    private static String watermark(Prescription prescription) {
        if (prescription.getStatus() == PrescriptionStatus.CANCELLED) {
            return "CANCELLED";
        }
        if (prescription.getExpiryDate().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            return "EXPIRED";
        }
        return null;
    }

    private Patient requirePatient(Long userId) {
        return patientRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Your patient profile is not linked to this account."));
    }

    private List<ValidatedItem> validateItems(PrescriptionForm form) {
        List<ValidatedItem> result = new ArrayList<>();
        for (PrescriptionForm.ItemForm item : form.getItems()) {
            if (item == null || item.getMedicine() == null || item.getMedicine().isBlank()) {
                continue;
            }
            if (item.getQuantity() == null || item.getQuantity() < 1 || item.getQuantity() > 10000) {
                throw new IllegalArgumentException(
                        "Enter a quantity between 1 and 10000 for " + item.getMedicine().trim() + ".");
            }
            result.add(new ValidatedItem(item.getMedicine().trim(), trimToNull(item.getDosage()),
                    trimToNull(item.getFrequency()), trimToNull(item.getDuration()),
                    item.getQuantity(), trimToNull(item.getQuantityUnit()),
                    trimToNull(item.getInstructions()), item.isLifetime()));
        }
        return result;
    }

    private String uniqueCode() {
        for (int attempt = 0; attempt < 8; attempt++) {
            String code = PublicIdGenerator.prescriptionCode();
            if (prescriptionRepository.findByPrescriptionCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Could not allocate a unique prescription ID. Try again.");
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ValidatedItem(String medicine, String dosage, String frequency, String duration,
                                 int quantity, String quantityUnit, String instructions, boolean lifetime) {
    }

    private record Fulfilment(String label, String tone) {
    }

    public record PrescriptionRow(Long id, String code, String issued, String expires, String patient,
                                  String patientPublicId, String doctor, String status, String watermark,
                                  String fulfilment, String fulfilmentTone) {
    }

    public record ItemView(Long id, String medicine, String dosage, String frequency, String duration,
                           int quantity, String unit, int dispensed, int remaining, String instructions,
                           boolean lifetime) {
    }

    public record DispenseItemView(String medicine, int quantity) {
    }

    public record DispenseView(Long id, String when, String pharmacy, String pharmacist, String status,
                               String notes, List<DispenseItemView> items) {
    }

    public record PrescriptionView(Long id, String code, String issued, String expires, String status,
                                   String watermark, Long patientId, String patientName, String patientPublicId,
                                   java.time.LocalDate patientDob, String patientMobile,
                                   String patientEmail, String clinicName, String clinicAddress,
                                   String clinicCity, String clinicPhone, String doctorName,
                                   String doctorSpecialization, Long consultationId, String cancelReason,
                                   String cancelledAt, String fulfilment, String fulfilmentTone,
                                   List<ItemView> items, List<DispenseView> dispenses) {
    }
}
