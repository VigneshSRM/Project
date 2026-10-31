package com.clinicflow.web.patient;

import com.clinicflow.audit.AuditService;
import com.clinicflow.common.PublicIdGenerator;
import com.clinicflow.domain.entity.Appointment;
import com.clinicflow.domain.entity.Clinic;
import com.clinicflow.domain.entity.ClinicPatient;
import com.clinicflow.domain.entity.Consultation;
import com.clinicflow.domain.entity.Doctor;
import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.entity.Prescription;
import com.clinicflow.domain.enums.AppointmentStatus;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.ClinicStatus;
import com.clinicflow.domain.enums.ConsultationStatus;
import com.clinicflow.domain.enums.DoctorStatus;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.repository.AppointmentRepository;
import com.clinicflow.domain.repository.ClinicDoctorRepository;
import com.clinicflow.domain.repository.ClinicPatientRepository;
import com.clinicflow.domain.repository.ClinicRepository;
import com.clinicflow.domain.repository.ConsultationRepository;
import com.clinicflow.domain.repository.DoctorRepository;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.PrescriptionRepository;
import com.clinicflow.notifications.AppointmentBookedEvent;
import com.clinicflow.notifications.AppointmentCancelledEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final ClinicDoctorRepository clinicDoctorRepository;
    private final ClinicPatientRepository clinicPatientRepository;
    private final PatientRepository patientRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final SlotService slotService;
    private final ApplicationEventPublisher events;
    private final AuditService auditService;

    public BookingService(AppointmentRepository appointmentRepository, ClinicRepository clinicRepository,
                          DoctorRepository doctorRepository, ClinicDoctorRepository clinicDoctorRepository,
                          ClinicPatientRepository clinicPatientRepository, PatientRepository patientRepository,
                          ConsultationRepository consultationRepository,
                          PrescriptionRepository prescriptionRepository, SlotService slotService,
                          ApplicationEventPublisher events, AuditService auditService) {
        this.appointmentRepository = appointmentRepository;
        this.clinicRepository = clinicRepository;
        this.doctorRepository = doctorRepository;
        this.clinicDoctorRepository = clinicDoctorRepository;
        this.clinicPatientRepository = clinicPatientRepository;
        this.patientRepository = patientRepository;
        this.consultationRepository = consultationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.slotService = slotService;
        this.events = events;
        this.auditService = auditService;
    }

    @Transactional
    public Long book(Long patientUserId, BookingForm form) {
        Patient patient = patientRepository.findByUserUserId(patientUserId)
                .orElseThrow(() -> new IllegalStateException("Your patient profile is not linked to this account."));
        Clinic clinic = clinicRepository.findById(form.getClinicId())
                .filter(candidate -> candidate.getStatus() == ClinicStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Clinic is not available."));
        // Lock the doctor row. This serialises competing bookings for the same doctor
        // and makes the availability check + insert atomic. The filtered unique index
        // on Appointments is the final database-level guard.
        Doctor doctor = doctorRepository.findForUpdate(form.getDoctorId())
                .filter(candidate -> candidate.getStatus() == DoctorStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Doctor is not available."));
        clinicDoctorRepository.findByClinicClinicIdAndDoctorDoctorId(clinic.getClinicId(),
                        doctor.getDoctorId())
                .filter(link -> link.getStatus() == DoctorStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Doctor is not available at this clinic."));

        if (form.getSlotStart() == null || !form.getSlotStart().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Choose an available future slot.");
        }
        if (!slotService.isValidSlot(clinic.getClinicId(), doctor.getDoctorId(), form.getSlotStart())) {
            throw new IllegalArgumentException("That slot is no longer available. Choose another.");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setClinic(clinic);
        appointment.setDoctor(doctor);
        appointment.setSlotStart(form.getSlotStart());
        appointment.setSlotEnd(form.getSlotStart().plusMinutes(
                slotService.slotLengthMinutes(clinic.getClinicId(), doctor.getDoctorId(),
                        form.getSlotStart())));
        appointment.setReason(form.getReason() == null || form.getReason().isBlank()
                ? null : form.getReason().trim());
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setBookedAt(LocalDateTime.now(ZoneOffset.UTC));
        appointment.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        appointment.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        appointment.setPublicAppointmentId("APT" + UUID.randomUUID().toString().substring(0, 12));
        try {
            appointment = appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("That slot was just taken. Choose another.");
        }
        appointment.setPublicAppointmentId(PublicIdGenerator.appointmentId(appointment.getAppointmentId()));
        appointmentRepository.save(appointment);

        if (!clinicPatientRepository.existsByClinicClinicIdAndPatientPatientId(
                clinic.getClinicId(), patient.getPatientId())) {
            ClinicPatient link = new ClinicPatient();
            link.setClinic(clinic);
            link.setPatient(patient);
            link.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            clinicPatientRepository.save(link);
        }

        auditService.record(patientUserId, Role.PATIENT, "APPOINTMENT_BOOKED", "Appointment",
                appointment.getPublicAppointmentId(), patient.getPatientId(), AuditOutcome.SUCCESS);
        events.publishEvent(new AppointmentBookedEvent(appointment.getAppointmentId(),
                patient.getEmail(), patient.getDisplayName(), clinic.getName(), doctor.getName(),
                DATE_TIME.format(appointment.getSlotStart()), appointment.getReason(),
                appointment.getPublicAppointmentId()));
        return appointment.getAppointmentId();
    }

    @Transactional(readOnly = true)
    public List<PatientAppointmentRow> list(Long patientUserId) {
        Patient patient = requirePatient(patientUserId);
        return appointmentRepository.findByPatientPatientIdOrderBySlotStartDesc(patient.getPatientId())
                .stream().map(this::toRow).toList();
    }

    @Transactional
    public PatientAppointmentDetail detail(Long patientUserId, Long appointmentId, Long actorUserId) {
        Patient patient = requirePatient(patientUserId);
        Appointment appointment = requireOwned(patient, appointmentId);
        Consultation consultation = consultationRepository.findByAppointmentAppointmentId(appointmentId)
                .orElse(null);
        Prescription prescription = consultation == null ? null :
                prescriptionRepository.findByConsultationConsultationId(consultation.getConsultationId())
                        .orElse(null);
        auditService.record(actorUserId, Role.PATIENT, "APPOINTMENT_VIEWED", "Appointment",
                appointment.getPublicAppointmentId(), patient.getPatientId(), AuditOutcome.SUCCESS);
        boolean canChange = appointment.getStatus() == AppointmentStatus.BOOKED
                || appointment.getStatus() == AppointmentStatus.CONFIRMED;
        boolean finalConsultation = consultation != null
                && consultation.getStatus() == ConsultationStatus.FINAL;
        return new PatientAppointmentDetail(appointment.getAppointmentId(),
                appointment.getPublicAppointmentId(), appointment.getClinic().getClinicId(),
                appointment.getDoctor().getDoctorId(), appointment.getClinic().getName(),
                appointment.getClinic().getAddressLine1(), appointment.getClinic().getCity(),
                appointment.getClinic().getPhone(), appointment.getDoctor().getName(),
                appointment.getDoctor().getSpecialization(), DATE_TIME.format(appointment.getSlotStart()),
                appointment.getStatus(), tone(appointment.getStatus().name()), appointment.getReason(),
                appointment.getCancelReason(), canChange && appointment.getSlotStart().isAfter(LocalDateTime.now()),
                finalConsultation, finalConsultation ? consultation.getDiagnosis() : null,
                finalConsultation ? consultation.getNotes() : null,
                prescription == null ? null : prescription.getPrescriptionId(),
                prescription == null ? null : prescription.getPrescriptionCode());
    }

    @Transactional
    public void cancel(Long patientUserId, Long appointmentId, String reason, Long actorUserId) {
        Patient patient = requirePatient(patientUserId);
        Appointment appointment = requireOwned(patient, appointmentId);
        if (appointment.getStatus() != AppointmentStatus.BOOKED
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalArgumentException("This appointment can no longer be cancelled online. Contact the clinic.");
        }
        if (appointment.getSlotStart().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("A past appointment cannot be cancelled online.");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(LocalDateTime.now(ZoneOffset.UTC));
        appointment.setCancelReason(reason == null || reason.isBlank()
                ? "Cancelled by patient" : reason.trim());
        appointment.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        appointmentRepository.save(appointment);
        auditService.record(actorUserId, Role.PATIENT, "APPOINTMENT_CANCELLED", "Appointment",
                appointment.getPublicAppointmentId(), patient.getPatientId(), AuditOutcome.SUCCESS);
        events.publishEvent(new AppointmentCancelledEvent(appointment.getAppointmentId(),
                patient.getEmail(), patient.getDisplayName(), appointment.getClinic().getName(),
                appointment.getDoctor().getName(), DATE_TIME.format(appointment.getSlotStart()),
                appointment.getPublicAppointmentId(), appointment.getCancelReason()));
    }

    @Transactional
    public void reschedule(Long patientUserId, Long appointmentId, BookingForm form, Long actorUserId) {
        Patient patient = requirePatient(patientUserId);
        Appointment appointment = requireOwned(patient, appointmentId);
        if (appointment.getStatus() != AppointmentStatus.BOOKED
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalArgumentException("This appointment can no longer be rescheduled online. Contact the clinic.");
        }
        Doctor doctor = doctorRepository.findForUpdate(form.getDoctorId())
                .filter(candidate -> candidate.getStatus() == DoctorStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Doctor is not available."));
        if (!doctor.getDoctorId().equals(appointment.getDoctor().getDoctorId())
                || !appointment.getClinic().getClinicId().equals(form.getClinicId())) {
            throw new IllegalArgumentException("Choose a new slot with the same doctor and clinic.");
        }
        if (!slotService.isValidSlot(appointment.getClinic().getClinicId(), doctor.getDoctorId(),
                form.getSlotStart())) {
            throw new IllegalArgumentException("That slot is no longer available. Choose another.");
        }
        appointment.setSlotStart(form.getSlotStart());
        appointment.setSlotEnd(form.getSlotStart().plusMinutes(
                slotService.slotLengthMinutes(appointment.getClinic().getClinicId(),
                        doctor.getDoctorId(), form.getSlotStart())));
        appointment.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("That slot was just taken. Choose another.");
        }
        auditService.record(actorUserId, Role.PATIENT, "APPOINTMENT_RESCHEDULED", "Appointment",
                appointment.getPublicAppointmentId(), patient.getPatientId(), AuditOutcome.SUCCESS);
        events.publishEvent(new AppointmentBookedEvent(appointment.getAppointmentId(),
                patient.getEmail(), patient.getDisplayName(), appointment.getClinic().getName(),
                appointment.getDoctor().getName(), DATE_TIME.format(appointment.getSlotStart()),
                appointment.getReason(), appointment.getPublicAppointmentId()));
    }

    private Patient requirePatient(Long userId) {
        return patientRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Your patient profile is not linked to this account."));
    }

    private Appointment requireOwned(Patient patient, Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .filter(appointment -> appointment.getPatient().getPatientId().equals(patient.getPatientId()))
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));
    }

    private PatientAppointmentRow toRow(Appointment appointment) {
        return new PatientAppointmentRow(appointment.getAppointmentId(),
                appointment.getPublicAppointmentId(), DATE_TIME.format(appointment.getSlotStart()),
                appointment.getClinic().getName(), appointment.getDoctor().getName(),
                appointment.getStatus().name(), tone(appointment.getStatus().name()));
    }

    private static String tone(String status) {
        return switch (status) {
            case "BOOKED", "CONFIRMED", "CHECKED_IN", "IN_CONSULTATION" -> "info";
            case "COMPLETED" -> "success";
            case "CANCELLED", "NO_SHOW" -> "danger";
            default -> "neutral";
        };
    }

    public record PatientAppointmentRow(Long id, String publicId, String when, String clinic,
                                        String doctor, String status, String tone) {
    }

    public record PatientAppointmentDetail(Long id, String publicId, Long clinicId, Long doctorId,
                                           String clinicName, String clinicAddress,
                                           String clinicCity, String clinicPhone, String doctorName,
                                           String doctorSpecialization, String when,
                                           AppointmentStatus status, String statusTone, String reason,
                                           String cancelReason, boolean canChange, boolean consultationFinal,
                                           String diagnosis, String notes, Long prescriptionId,
                                           String prescriptionCode) {
    }
}
