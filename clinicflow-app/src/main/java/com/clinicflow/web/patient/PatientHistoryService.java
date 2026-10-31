package com.clinicflow.web.patient;

import com.clinicflow.domain.entity.Appointment;
import com.clinicflow.domain.entity.Consultation;
import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.entity.Prescription;
import com.clinicflow.domain.enums.ConsultationStatus;
import com.clinicflow.domain.repository.AppointmentRepository;
import com.clinicflow.domain.repository.ConsultationRepository;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PatientHistoryService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;

    public PatientHistoryService(PatientRepository patientRepository,
                                 AppointmentRepository appointmentRepository,
                                 ConsultationRepository consultationRepository,
                                 PrescriptionRepository prescriptionRepository) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.consultationRepository = consultationRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    @Transactional(readOnly = true)
    public PatientHistory history(Long userId) {
        Patient patient = patientRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Your patient profile is not linked to this account."));
        List<Event> events = new ArrayList<>();

        for (Consultation consultation : consultationRepository
                .findByAppointmentPatientPatientIdOrderByCreatedAtDesc(patient.getPatientId())) {
            if (consultation.getStatus() != ConsultationStatus.FINAL) {
                continue;
            }
            Appointment appointment = consultation.getAppointment();
            events.add(new Event(consultation.getFinalisedAt() == null
                    ? consultation.getCreatedAt() : consultation.getFinalisedAt(),
                    "Consultation", appointment.getClinic().getName(),
                    "Diagnosis: " + (consultation.getDiagnosis() == null ? "—" : consultation.getDiagnosis()),
                    "FINAL", appointment.getDoctor().getName(),
                    "/patient/appointments/" + appointment.getAppointmentId(), "consultation"));
        }

        for (Prescription prescription : prescriptionRepository
                .findByPatientPatientIdOrderByIssueDateDesc(patient.getPatientId())) {
            events.add(new Event(prescription.getIssueDate(), "Prescription",
                    prescription.getClinic().getName(), prescription.getPrescriptionCode(),
                    prescription.getStatus().name(), prescription.getDoctor().getName(),
                    "/patient/prescriptions/" + prescription.getPrescriptionId(), "prescription"));
        }

        for (Appointment appointment : appointmentRepository
                .findByPatientPatientIdOrderBySlotStartDesc(patient.getPatientId())) {
            if (appointment.getStatus().name().equals("CANCELLED")
                    || appointment.getStatus().name().equals("NO_SHOW")) {
                events.add(new Event(appointment.getSlotStart(), "Appointment " + appointment.getStatus(),
                        appointment.getClinic().getName(),
                        appointment.getCancelReason() == null ? "Appointment record" : appointment.getCancelReason(),
                        appointment.getStatus().name(), appointment.getDoctor().getName(),
                        "/patient/appointments/" + appointment.getAppointmentId(), "appointment"));
            }
        }

        events.sort(Comparator.comparing(Event::sortDate,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return new PatientHistory(patient.getDisplayName(), patient.getPublicPatientId(), events);
    }

    public record Event(LocalDateTime sortDate, String type, String clinic, String detail, String status,
                        String doctor, String href, String kind) {
        public String when() {
            return sortDate == null ? "—" : DATE_TIME.format(sortDate);
        }
    }

    public record PatientHistory(String patientName, String publicPatientId, List<Event> events) {
    }
}
