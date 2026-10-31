package com.clinicflow.web.clinic;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.Appointment;
import com.clinicflow.domain.entity.Consultation;
import com.clinicflow.domain.entity.Prescription;
import com.clinicflow.domain.enums.AppointmentStatus;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.repository.AppointmentRepository;
import com.clinicflow.domain.repository.ConsultationRepository;
import com.clinicflow.domain.repository.PrescriptionRepository;
import com.clinicflow.notifications.AppointmentCancelledEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClinicAppointmentService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ApplicationEventPublisher events;
    private final AuditService auditService;

    public ClinicAppointmentService(AppointmentRepository appointmentRepository,
                                    ConsultationRepository consultationRepository,
                                    PrescriptionRepository prescriptionRepository,
                                    ApplicationEventPublisher events,
                                    AuditService auditService) {
        this.appointmentRepository = appointmentRepository;
        this.consultationRepository = consultationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.events = events;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AppointmentRow> list(Long clinicId, LocalDate date, AppointmentStatus status) {
        List<Appointment> appointments;
        if (date == null) {
            appointments = appointmentRepository.findByClinicClinicIdOrderBySlotStartDesc(clinicId);
        } else {
            appointments = appointmentRepository.findByClinicClinicIdAndSlotStartBetweenOrderBySlotStartAsc(
                    clinicId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        }
        return appointments.stream()
                .filter(appointment -> status == null || appointment.getStatus() == status)
                .map(this::toRow)
                .toList();
    }

    @Transactional
    public AppointmentDetail detail(Long clinicId, Long appointmentId, Long actorUserId) {
        Appointment appointment = requireAppointment(clinicId, appointmentId);
        Consultation consultation = consultationRepository.findByAppointmentAppointmentId(appointmentId)
                .orElse(null);
        Prescription prescription = consultation == null ? null :
                prescriptionRepository.findByConsultationConsultationId(consultation.getConsultationId()).orElse(null);
        auditService.record(actorUserId, Role.CLINIC_ADMIN, "APPOINTMENT_VIEWED", "Appointment",
                appointment.getPublicAppointmentId(), appointment.getPatient().getPatientId(),
                AuditOutcome.SUCCESS);
        return new AppointmentDetail(appointment.getAppointmentId(), appointment.getPublicAppointmentId(),
                appointment.getPatient().getPatientId(), appointment.getPatient().getPublicPatientId(),
                appointment.getPatient().getDisplayName(), appointment.getPatient().getMobile(),
                appointment.getDoctor().getDoctorId(), appointment.getDoctor().getName(),
                DATE_TIME.format(appointment.getSlotStart()), appointment.getStatus(), tone(appointment.getStatus().name()),
                appointment.getReason(), appointment.getCancelReason(),
                appointment.getCancelledAt() == null ? null : DATE_TIME.format(appointment.getCancelledAt()),
                consultation == null ? null : consultation.getConsultationId(),
                consultation == null ? null : consultation.getStatus().name(),
                consultation == null ? null : consultation.getDiagnosis(),
                prescription == null ? null : prescription.getPrescriptionId(),
                prescription == null ? null : prescription.getPrescriptionCode(),
                actionsFor(appointment.getStatus()));
    }

    @Transactional
    public void transition(Long clinicId, Long appointmentId, String action, String reason,
                           Long actorUserId) {
        Appointment appointment = requireAppointment(clinicId, appointmentId);
        AppointmentStatus before = appointment.getStatus();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        switch (action) {
            case "confirm" -> requireStatus(before, AppointmentStatus.BOOKED);
            case "check-in" -> {
                if (before != AppointmentStatus.BOOKED && before != AppointmentStatus.CONFIRMED) {
                    throw new IllegalArgumentException("Only booked or confirmed appointments can be checked in.");
                }
            }
            case "no-show" -> {
                if (before != AppointmentStatus.BOOKED && before != AppointmentStatus.CONFIRMED) {
                    throw new IllegalArgumentException("Only booked or confirmed appointments can be marked no-show.");
                }
            }
            case "cancel" -> {
                if (before == AppointmentStatus.COMPLETED || before == AppointmentStatus.CANCELLED
                        || before == AppointmentStatus.NO_SHOW || before == AppointmentStatus.IN_CONSULTATION) {
                    throw new IllegalArgumentException("This appointment cannot be cancelled in its current state.");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported appointment action.");
        }

        switch (action) {
            case "confirm" -> appointment.setStatus(AppointmentStatus.CONFIRMED);
            case "check-in" -> appointment.setStatus(AppointmentStatus.CHECKED_IN);
            case "no-show" -> appointment.setStatus(AppointmentStatus.NO_SHOW);
            case "cancel" -> {
                appointment.setStatus(AppointmentStatus.CANCELLED);
                appointment.setCancelledAt(now);
                appointment.setCancelReason(reason == null || reason.isBlank()
                        ? "Cancelled by clinic" : reason.trim());
            }
            default -> throw new IllegalArgumentException("Unsupported appointment action.");
        }
        appointment.setUpdatedAt(now);
        appointmentRepository.save(appointment);
        auditService.record(actorUserId, Role.CLINIC_ADMIN,
                "APPOINTMENT_" + action.toUpperCase().replace('-', '_'), "Appointment",
                appointment.getPublicAppointmentId(), appointment.getPatient().getPatientId(),
                AuditOutcome.SUCCESS);
        if (action.equals("cancel")) {
            events.publishEvent(new AppointmentCancelledEvent(appointment.getAppointmentId(),
                    appointment.getPatient().getEmail(), appointment.getPatient().getDisplayName(),
                    appointment.getClinic().getName(), appointment.getDoctor().getName(),
                    DATE_TIME.format(appointment.getSlotStart()), appointment.getPublicAppointmentId(),
                    appointment.getCancelReason()));
        }
    }

    private Appointment requireAppointment(Long clinicId, Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .filter(appointment -> appointment.getClinic().getClinicId().equals(clinicId))
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));
    }

    private static void requireStatus(AppointmentStatus actual, AppointmentStatus expected) {
        if (actual != expected) {
            throw new IllegalArgumentException("This action is not allowed from status " + actual + ".");
        }
    }

    private AppointmentRow toRow(Appointment appointment) {
        return new AppointmentRow(appointment.getAppointmentId(), appointment.getPublicAppointmentId(),
                DATE_TIME.format(appointment.getSlotStart()), appointment.getPatient().getDisplayName(),
                appointment.getDoctor().getName(), appointment.getStatus().name(),
                tone(appointment.getStatus().name()));
    }

    private static List<Action> actionsFor(AppointmentStatus status) {
        List<Action> actions = new ArrayList<>();
        switch (status) {
            case BOOKED -> {
                actions.add(new Action("confirm", "Confirm", "primary"));
                actions.add(new Action("check-in", "Check in", "secondary"));
                actions.add(new Action("no-show", "No show", "danger"));
                actions.add(new Action("cancel", "Cancel", "danger"));
            }
            case CONFIRMED -> {
                actions.add(new Action("check-in", "Check in", "primary"));
                actions.add(new Action("no-show", "No show", "danger"));
                actions.add(new Action("cancel", "Cancel", "danger"));
            }
            case CHECKED_IN -> actions.add(new Action("cancel", "Cancel", "danger"));
            case NO_SHOW -> {
            }
            case COMPLETED -> {
            }
            case CANCELLED -> {
            }
            case IN_CONSULTATION -> {
            }
        }
        return actions;
    }

    private static String tone(String status) {
        return switch (status) {
            case "BOOKED", "CONFIRMED", "CHECKED_IN", "IN_CONSULTATION" -> "info";
            case "COMPLETED" -> "success";
            case "CANCELLED", "NO_SHOW" -> "danger";
            default -> "neutral";
        };
    }

    public record Action(String key, String label, String tone) {
    }

    public record AppointmentRow(Long id, String publicId, String when, String patient, String doctor,
                                 String status, String tone) {
    }

    public record AppointmentDetail(Long id, String publicId, Long patientId, String patientPublicId,
                                    String patientName, String patientMobile, Long doctorId, String doctorName,
                                    String when, AppointmentStatus status, String statusTone, String reason,
                                    String cancelReason, String cancelledAt, Long consultationId,
                                    String consultationStatus, String diagnosis, Long prescriptionId,
                                    String prescriptionCode, List<Action> actions) {
    }
}
