package com.clinicflow.web.doctor;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.Appointment;
import com.clinicflow.domain.entity.Consultation;
import com.clinicflow.domain.entity.ConsultationAddendum;
import com.clinicflow.domain.entity.DoctorUserLink;
import com.clinicflow.domain.entity.Prescription;
import com.clinicflow.domain.enums.AppointmentStatus;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.ConsultationStatus;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.repository.AppointmentRepository;
import com.clinicflow.domain.repository.ConsultationAddendumRepository;
import com.clinicflow.domain.repository.ConsultationRepository;
import com.clinicflow.domain.repository.DoctorUserLinkRepository;
import com.clinicflow.domain.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DoctorConsultationService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final DoctorUserLinkRepository doctorUserLinkRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final ConsultationAddendumRepository addendumRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AuditService auditService;

    public DoctorConsultationService(DoctorUserLinkRepository doctorUserLinkRepository,
                                     AppointmentRepository appointmentRepository,
                                     ConsultationRepository consultationRepository,
                                     ConsultationAddendumRepository addendumRepository,
                                     PrescriptionRepository prescriptionRepository,
                                     AuditService auditService) {
        this.doctorUserLinkRepository = doctorUserLinkRepository;
        this.appointmentRepository = appointmentRepository;
        this.consultationRepository = consultationRepository;
        this.addendumRepository = addendumRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DoctorAppointmentRow> list(Long userId, LocalDate date, AppointmentStatus status) {
        Long doctorId = requireDoctorId(userId);
        List<Appointment> appointments;
        if (date == null) {
            appointments = appointmentRepository.findByDoctorDoctorIdOrderBySlotStartDesc(doctorId);
        } else {
            appointments = appointmentRepository.findByDoctorDoctorIdAndSlotStartBetweenOrderBySlotStartAsc(
                    doctorId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        }
        return appointments.stream()
                .filter(appointment -> status == null || appointment.getStatus() == status)
                .map(this::toRow)
                .toList();
    }

    @Transactional
    public DoctorAppointmentDetail detail(Long userId, Long appointmentId, Long actorUserId) {
        DoctorUserLink link = requireDoctorLink(userId);
        Appointment appointment = requireOwned(link, appointmentId);
        Consultation consultation = consultationRepository.findByAppointmentAppointmentId(appointmentId)
                .orElse(null);
        Prescription prescription = consultation == null ? null
                : prescriptionRepository.findByConsultationConsultationId(consultation.getConsultationId())
                .orElse(null);
        List<ConsultationAddendum> addenda = consultation == null ? List.of()
                : addendumRepository.findByConsultationConsultationIdOrderByCreatedAtAsc(
                consultation.getConsultationId());
        auditService.record(actorUserId, Role.DOCTOR, "CONSULTATION_VIEWED", "Appointment",
                appointment.getPublicAppointmentId(), appointment.getPatient().getPatientId(),
                AuditOutcome.SUCCESS);
        return new DoctorAppointmentDetail(appointment.getAppointmentId(),
                appointment.getPublicAppointmentId(), appointment.getPatient().getPatientId(),
                appointment.getPatient().getPublicPatientId(), appointment.getPatient().getDisplayName(),
                appointment.getPatient().getDob(), appointment.getPatient().getGender(),
                appointment.getPatient().getMobile(), appointment.getPatient().getEmail(),
                appointment.getPatient().getBloodGroup(), appointment.getPatient().getAllergies(),
                appointment.getPatient().getConditions(), appointment.getClinic().getName(),
                DATE_TIME.format(appointment.getSlotStart()), appointment.getStatus(),
                tone(appointment.getStatus().name()), appointment.getReason(),
                consultation == null ? null : consultation.getConsultationId(),
                consultation == null ? null : consultation.getStatus(),
                consultation == null ? null : consultation.getDiagnosis(),
                consultation == null ? null : consultation.getNotes(),
                consultation == null || consultation.getFollowUp() == null ? null
                        : DATE_TIME.format(consultation.getFollowUp()),
                consultation == null ? null : consultation.getFollowUp(),
                consultation == null || consultation.getFinalisedAt() == null ? null
                        : DATE_TIME.format(consultation.getFinalisedAt()),
                prescription == null ? null : prescription.getPrescriptionId(),
                prescription == null ? null : prescription.getPrescriptionCode(),
                addenda.stream().map(addendum -> new AddendumRow(DATE_TIME.format(addendum.getCreatedAt()),
                        addendum.getReason(), addendum.getText())).toList());
    }

    @Transactional
    public Long start(Long userId, Long appointmentId, Long actorUserId) {
        DoctorUserLink link = requireDoctorLink(userId);
        Appointment appointment = requireOwned(link, appointmentId);
        Consultation existing = consultationRepository.findByAppointmentAppointmentId(appointmentId)
                .orElse(null);
        if (existing != null) {
            if (appointment.getStatus() == AppointmentStatus.IN_CONSULTATION) {
                return existing.getConsultationId();
            }
            if (existing.getStatus() == ConsultationStatus.FINAL) {
                throw new IllegalArgumentException("This consultation is already final.");
            }
        } else {
            if (appointment.getStatus() != AppointmentStatus.CHECKED_IN
                    && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
                throw new IllegalArgumentException(
                        "Only a checked-in or confirmed appointment can start a consultation.");
            }
            Consultation consultation = new Consultation();
            consultation.setAppointment(appointment);
            consultation.setStatus(ConsultationStatus.DRAFT);
            consultation.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            consultation.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            existing = consultationRepository.save(consultation);
        }
        if (appointment.getStatus() != AppointmentStatus.IN_CONSULTATION) {
            appointment.setStatus(AppointmentStatus.IN_CONSULTATION);
            appointment.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            appointmentRepository.save(appointment);
        }
        auditService.record(actorUserId, Role.DOCTOR, "CONSULTATION_STARTED", "Consultation",
                String.valueOf(existing.getConsultationId()), appointment.getPatient().getPatientId(),
                AuditOutcome.SUCCESS);
        return existing.getConsultationId();
    }

    @Transactional
    public void saveDraft(Long userId, Long appointmentId, ConsultationForm form, Long actorUserId) {
        DoctorUserLink link = requireDoctorLink(userId);
        Appointment appointment = requireOwned(link, appointmentId);
        Consultation consultation = requireDraft(appointment);
        consultation.setDiagnosis(trimToNull(form.getDiagnosis()));
        consultation.setNotes(trimToNull(form.getNotes()));
        consultation.setFollowUp(form.getFollowUp());
        consultation.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        consultationRepository.save(consultation);
        auditService.record(actorUserId, Role.DOCTOR, "CONSULTATION_DRAFT_SAVED", "Consultation",
                String.valueOf(consultation.getConsultationId()), appointment.getPatient().getPatientId(),
                AuditOutcome.SUCCESS);
    }

    @Transactional
    public void finalise(Long userId, Long appointmentId, ConsultationForm form, Long actorUserId) {
        DoctorUserLink link = requireDoctorLink(userId);
        Appointment appointment = requireOwned(link, appointmentId);
        Consultation consultation = requireDraft(appointment);
        if (form.getDiagnosis() == null || form.getDiagnosis().isBlank()) {
            throw new IllegalArgumentException("A diagnosis is required to finalise the consultation.");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        consultation.setDiagnosis(form.getDiagnosis().trim());
        consultation.setNotes(trimToNull(form.getNotes()));
        consultation.setFollowUp(form.getFollowUp());
        consultation.setStatus(ConsultationStatus.FINAL);
        consultation.setFinalisedAt(now);
        consultation.setUpdatedAt(now);
        consultationRepository.save(consultation);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setUpdatedAt(now);
        appointmentRepository.save(appointment);
        auditService.record(actorUserId, Role.DOCTOR, "CONSULTATION_FINALISED", "Consultation",
                String.valueOf(consultation.getConsultationId()), appointment.getPatient().getPatientId(),
                AuditOutcome.SUCCESS);
    }

    @Transactional
    public void addAddendum(Long userId, Long appointmentId, String reason, String text, Long actorUserId) {
        DoctorUserLink link = requireDoctorLink(userId);
        Appointment appointment = requireOwned(link, appointmentId);
        Consultation consultation = consultationRepository.findByAppointmentAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Start the consultation first."));
        if (consultation.getStatus() != ConsultationStatus.FINAL) {
            throw new IllegalArgumentException("Addenda are only allowed after the consultation is final.");
        }
        if (reason == null || reason.isBlank() || text == null || text.isBlank()) {
            throw new IllegalArgumentException("Enter both an addendum reason and text.");
        }
        ConsultationAddendum addendum = new ConsultationAddendum();
        addendum.setConsultation(consultation);
        addendum.setAuthor(link.getUser());
        addendum.setReason(reason.trim());
        addendum.setText(text.trim());
        addendum.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        addendumRepository.save(addendum);
        auditService.record(actorUserId, Role.DOCTOR, "CONSULTATION_ADDENDUM_ADDED", "Consultation",
                String.valueOf(consultation.getConsultationId()), appointment.getPatient().getPatientId(),
                AuditOutcome.SUCCESS);
    }

    private DoctorUserLink requireDoctorLink(Long userId) {
        return doctorUserLinkRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No doctor profile is linked to this account."));
    }

    private Long requireDoctorId(Long userId) {
        return requireDoctorLink(userId).getDoctor().getDoctorId();
    }

    private Appointment requireOwned(DoctorUserLink link, Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .filter(appointment -> appointment.getDoctor().getDoctorId()
                        .equals(link.getDoctor().getDoctorId()))
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));
    }

    private Consultation requireDraft(Appointment appointment) {
        Consultation consultation = consultationRepository
                .findByAppointmentAppointmentId(appointment.getAppointmentId())
                .orElseThrow(() -> new IllegalArgumentException("Start the consultation first."));
        if (consultation.getStatus() != ConsultationStatus.DRAFT) {
            throw new IllegalArgumentException("This consultation is final and can only receive addenda.");
        }
        if (appointment.getStatus() != AppointmentStatus.IN_CONSULTATION) {
            throw new IllegalArgumentException("The appointment is not in consultation.");
        }
        return consultation;
    }

    private DoctorAppointmentRow toRow(Appointment appointment) {
        Consultation consultation = consultationRepository
                .findByAppointmentAppointmentId(appointment.getAppointmentId()).orElse(null);
        return new DoctorAppointmentRow(appointment.getAppointmentId(),
                appointment.getPublicAppointmentId(), DATE_TIME.format(appointment.getSlotStart()),
                appointment.getPatient().getDisplayName(), appointment.getPatient().getPublicPatientId(),
                appointment.getStatus().name(), tone(appointment.getStatus().name()),
                consultation == null ? "Not started" : consultation.getStatus().name());
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

    public record DoctorAppointmentRow(Long id, String publicId, String when, String patient,
                                       String patientPublicId, String status, String tone,
                                       String consultationStatus) {
    }

    public record AddendumRow(String when, String reason, String text) {
    }

    public record DoctorAppointmentDetail(Long id, String publicId, Long patientId,
                                          String patientPublicId, String patientName, LocalDate dob,
                                          com.clinicflow.domain.enums.Gender gender, String mobile,
                                          String email, String bloodGroup, String allergies,
                                          String conditions, String clinicName, String when,
                                          AppointmentStatus status, String statusTone, String reason,
                                          Long consultationId, ConsultationStatus consultationStatus,
                                          String diagnosis, String notes, String followUp,
                                          LocalDateTime followUpValue,
                                          String finalisedAt, Long prescriptionId,
                                          String prescriptionCode, List<AddendumRow> addenda) {
    }
}
