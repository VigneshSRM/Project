package com.clinicflow.web.admin;

import com.clinicflow.domain.enums.AppointmentStatus;
import com.clinicflow.domain.enums.ClinicStatus;
import com.clinicflow.domain.enums.EmailStatus;
import com.clinicflow.domain.enums.LinkRequestStatus;
import com.clinicflow.domain.enums.PharmacyStatus;
import com.clinicflow.domain.enums.PrescriptionStatus;
import com.clinicflow.domain.repository.AppointmentRepository;
import com.clinicflow.domain.repository.ClinicRepository;
import com.clinicflow.domain.repository.EmailLogRepository;
import com.clinicflow.domain.repository.PatientLinkRequestRepository;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.PharmacyRepository;
import com.clinicflow.domain.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminReportService {

    private final ClinicRepository clinicRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final EmailLogRepository emailLogRepository;
    private final PatientLinkRequestRepository linkRequestRepository;

    public AdminReportService(ClinicRepository clinicRepository,
                              PharmacyRepository pharmacyRepository,
                              PatientRepository patientRepository,
                              AppointmentRepository appointmentRepository,
                              PrescriptionRepository prescriptionRepository,
                              EmailLogRepository emailLogRepository,
                              PatientLinkRequestRepository linkRequestRepository) {
        this.clinicRepository = clinicRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.emailLogRepository = emailLogRepository;
        this.linkRequestRepository = linkRequestRepository;
    }

    @Transactional(readOnly = true)
    public AdminReportView view() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<Metric> metrics = List.of(
                new Metric("Active clinics", clinicRepository.countByStatus(ClinicStatus.ACTIVE),
                        "All clinics", clinicRepository.count()),
                new Metric("Active pharmacies", pharmacyRepository.countByStatus(PharmacyStatus.ACTIVE),
                        "All pharmacies", pharmacyRepository.count()),
                new Metric("Patient masters", patientRepository.count(), "Visible records",
                        patientRepository.findByMergedIntoIsNullOrderByCreatedAtDesc().size()),
                new Metric("Appointments today", appointmentRepository.countBySlotStartBetween(start, end),
                        "All time", appointmentRepository.count()),
                new Metric("Prescriptions issued", prescriptionRepository.count(), "Cancelled",
                        prescriptionRepository.countByStatus(PrescriptionStatus.CANCELLED)),
                new Metric("Emails failed", emailLogRepository.countByStatus(EmailStatus.FAILED),
                        "Queued", emailLogRepository.countByStatus(EmailStatus.QUEUED))
        );
        List<StatusRow> appointmentStatuses = new ArrayList<>();
        for (AppointmentStatus status : AppointmentStatus.values()) {
            appointmentStatuses.add(new StatusRow(status.name(), appointmentRepository.countByStatus(status)));
        }
        List<StatusRow> emails = new ArrayList<>();
        for (EmailStatus status : EmailStatus.values()) {
            emails.add(new StatusRow(status.name(), emailLogRepository.countByStatus(status)));
        }
        return new AdminReportView(metrics, appointmentStatuses, emails,
                linkRequestRepository.countByStatus(LinkRequestStatus.PENDING),
                linkRequestRepository.countByStatus(LinkRequestStatus.APPROVED),
                linkRequestRepository.countByStatus(LinkRequestStatus.REJECTED));
    }

    public record Metric(String label, long value, String hint, long secondary) {
    }

    public record StatusRow(String label, long count) {
    }

    public record AdminReportView(List<Metric> metrics, List<StatusRow> appointmentStatuses,
                                  List<StatusRow> emails, long pendingLinks, long approvedLinks,
                                  long rejectedLinks) {
    }
}
