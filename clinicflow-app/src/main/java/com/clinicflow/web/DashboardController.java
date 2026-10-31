package com.clinicflow.web;

import com.clinicflow.domain.entity.*;
import com.clinicflow.domain.enums.*;
import com.clinicflow.domain.repository.*;
import com.clinicflow.security.AppUserPrincipal;
import com.clinicflow.web.dashboard.ActivityRow;
import com.clinicflow.web.dashboard.DashboardView;
import com.clinicflow.web.dashboard.KpiCard;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
@Transactional(readOnly = true)
public class DashboardController {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final EmailLogRepository emailLogRepository;
    private final DoctorUserLinkRepository doctorUserLinkRepository;
    private final ClinicDoctorRepository clinicDoctorRepository;
    private final ClinicPatientRepository clinicPatientRepository;
    private final PharmacyUserRepository pharmacyUserRepository;
    private final PharmacyVerificationRepository pharmacyVerificationRepository;
    private final PrescriptionDispenseRepository prescriptionDispenseRepository;

    public DashboardController(ClinicRepository clinicRepository,
                               UserRepository userRepository,
                               PatientRepository patientRepository,
                               AppointmentRepository appointmentRepository,
                               PrescriptionRepository prescriptionRepository,
                               EmailLogRepository emailLogRepository,
                               DoctorUserLinkRepository doctorUserLinkRepository,
                               ClinicDoctorRepository clinicDoctorRepository,
                               ClinicPatientRepository clinicPatientRepository,
                               PharmacyUserRepository pharmacyUserRepository,
                               PharmacyVerificationRepository pharmacyVerificationRepository,
                               PrescriptionDispenseRepository prescriptionDispenseRepository) {
        this.clinicRepository = clinicRepository;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.emailLogRepository = emailLogRepository;
        this.doctorUserLinkRepository = doctorUserLinkRepository;
        this.clinicDoctorRepository = clinicDoctorRepository;
        this.clinicPatientRepository = clinicPatientRepository;
        this.pharmacyUserRepository = pharmacyUserRepository;
        this.pharmacyVerificationRepository = pharmacyVerificationRepository;
        this.prescriptionDispenseRepository = prescriptionDispenseRepository;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        long active = clinicRepository.countByStatus(ClinicStatus.ACTIVE);
        long pending = clinicRepository.countByStatus(ClinicStatus.PENDING);
        long suspended = clinicRepository.countByStatus(ClinicStatus.SUSPENDED);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<KpiCard> kpis = List.of(
                new KpiCard("Active clinics", String.valueOf(active), active + " of " + clinicRepository.count(), "/admin/clinics", "success"),
                new KpiCard("Pending enrollment", String.valueOf(pending), suspended + " suspended", "/admin/clinics", "warning"),
                new KpiCard("Registered patients", String.valueOf(patientRepository.count()), "cross-clinic master", "/admin/patients", "info"),
                new KpiCard("Appointments today", String.valueOf(appointmentRepository.countBySlotStartBetween(start, end)), "all clinics", "/admin/reports", "primary"),
                new KpiCard("Prescriptions issued", String.valueOf(prescriptionRepository.count()), "all time", "/admin/reports", "primary"),
                new KpiCard("Email failures", String.valueOf(emailLogRepository.countByStatus(EmailStatus.FAILED)), "24h delivery health", "/admin/smtp", "danger")
        );

        List<ActivityRow> activity = new ArrayList<>();
        clinicRepository.findAll().stream().limit(5).forEach(clinic ->
                activity.add(new ActivityRow(clinic.getName(), clinic.getCity() + " - " + clinic.getState(),
                        clinic.getStatus().name(), toneForClinic(clinic.getStatus()), "/admin/clinics")));
        if (activity.isEmpty()) {
            activity.add(new ActivityRow("No clinics enrolled yet", "Enroll the first clinic to get started", "", "neutral", "/admin/clinics/new"));
        }

        DashboardView view = new DashboardView(
                "Platform dashboard",
                "Onboard clinics, monitor platform health and configure delivery.",
                "Enroll clinic", "/admin/clinics/new",
                kpis, "Clinic enrollment", activity);
        model.addAttribute("dashboard", view);
        model.addAttribute("pageTitle", view.title());
        return "dashboard/index";
    }

    @GetMapping("/clinic/dashboard")
    public String clinicDashboard(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        if (principal.getClinicId() == null) {
            return "redirect:/login";
        }
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<Appointment> today = appointmentRepository
                .findByClinicClinicIdAndSlotStartBetweenOrderBySlotStartAsc(principal.getClinicId(), start, end);

        List<KpiCard> kpis = List.of(
                new KpiCard("Appointments today", String.valueOf(today.size()), "clinic queue", "/clinic/appointments", "primary"),
                new KpiCard("Patient masters", String.valueOf(clinicPatientRepository.findByClinicClinicId(principal.getClinicId()).size()), "clinic-scoped", "/clinic/patients", "info"),
                new KpiCard("Doctors", String.valueOf(clinicDoctorRepository.findByClinicClinicId(principal.getClinicId()).size()), "active panel", "/clinic/doctors", "success"),
                new KpiCard("Prescriptions", String.valueOf(prescriptionRepository.findByClinicClinicIdOrderByIssueDateDesc(principal.getClinicId()).size()), "issued by clinic", "/clinic/prescriptions", "primary")
        );

        List<ActivityRow> activity = new ArrayList<>();
        today.stream().limit(6).forEach(appointment -> activity.add(new ActivityRow(
                appointment.getPatient().getDisplayName(),
                appointment.getDoctor().getName() + " - " + TIME.format(appointment.getSlotStart()),
                appointment.getStatus().name(), toneForAppointment(appointment.getStatus()),
                "/clinic/appointments")));
        if (activity.isEmpty()) {
            activity.add(new ActivityRow("No appointments today", "Confirmed bookings will appear here", "", "neutral", "/clinic/appointments"));
        }

        DashboardView view = new DashboardView(
                "Clinic dashboard",
                "Today's queue, patient masters and doctor availability at a glance.",
                "View appointments", "/clinic/appointments",
                kpis, "Today's appointments", activity);
        model.addAttribute("dashboard", view);
        model.addAttribute("pageTitle", view.title());
        return "dashboard/index";
    }

    @GetMapping("/doctor/dashboard")
    public String doctorDashboard(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        DoctorUserLink link = doctorUserLinkRepository.findByUserUserId(principal.getUserId()).orElse(null);
        if (link == null) {
            model.addAttribute("dashboard", new DashboardView(
                    "Doctor dashboard", "Your doctor profile is not linked yet.",
                    "My appointments", "/doctor/appointments",
                    List.of(), "Today's appointments", List.of(
                            new ActivityRow("No doctor profile linked", "Ask your clinic administrator to link your login", "", "warning", "/doctor/dashboard"))));
            model.addAttribute("pageTitle", "Doctor dashboard");
            return "dashboard/index";
        }
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<Appointment> today = appointmentRepository
                .findByDoctorDoctorIdAndSlotStartBetweenOrderBySlotStartAsc(link.getDoctor().getDoctorId(), start, end);

        List<KpiCard> kpis = List.of(
                new KpiCard("Appointments today", String.valueOf(today.size()), "own schedule", "/doctor/appointments", "primary"),
                new KpiCard("Prescriptions issued", String.valueOf(prescriptionRepository.findByDoctorDoctorIdOrderByIssueDateDesc(link.getDoctor().getDoctorId()).size()), "own records", "/doctor/prescriptions", "success"),
                new KpiCard("Specialization", link.getDoctor().getSpecialization() == null ? "-" : link.getDoctor().getSpecialization(), link.getDoctor().getName(), "/doctor/dashboard", "info")
        );

        List<ActivityRow> activity = new ArrayList<>();
        today.stream().limit(6).forEach(appointment -> activity.add(new ActivityRow(
                appointment.getPatient().getDisplayName(),
                TIME.format(appointment.getSlotStart()) + " - " + appointment.getStatus().name(),
                appointment.getStatus().name(), toneForAppointment(appointment.getStatus()),
                "/doctor/appointments")));
        if (activity.isEmpty()) {
            activity.add(new ActivityRow("No appointments today", "Your booked patients will appear here", "", "neutral", "/doctor/appointments"));
        }

        DashboardView view = new DashboardView(
                "Doctor dashboard",
                "Your schedule, consultations and prescriptions.",
                "My appointments", "/doctor/appointments",
                kpis, "Today's appointments", activity);
        model.addAttribute("dashboard", view);
        model.addAttribute("pageTitle", view.title());
        return "dashboard/index";
    }

    @GetMapping("/patient/dashboard")
    public String patientDashboard(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        Patient patient = patientRepository.findByUserUserId(principal.getUserId()).orElse(null);
        if (patient == null) {
            model.addAttribute("dashboard", new DashboardView(
                    "Patient dashboard", "Complete your profile to start booking appointments.",
                    "My profile", "/patient/profile",
                    List.of(), "Upcoming appointments", List.of(
                            new ActivityRow("Profile not linked", "Ask your clinic to link your patient record", "", "warning", "/patient/profile"))));
            model.addAttribute("pageTitle", "Patient dashboard");
            return "dashboard/index";
        }
        List<Appointment> appointments = appointmentRepository.findByPatientPatientIdOrderBySlotStartDesc(patient.getPatientId());
        List<Appointment> upcoming = appointments.stream()
                .filter(a -> a.getSlotStart().isAfter(LocalDateTime.now().minusMinutes(1)))
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED
                        && a.getStatus() != AppointmentStatus.NO_SHOW
                        && a.getStatus() != AppointmentStatus.COMPLETED)
                .limit(5)
                .toList();
        List<Prescription> prescriptions = prescriptionRepository.findByPatientPatientIdOrderByIssueDateDesc(patient.getPatientId());

        List<KpiCard> kpis = List.of(
                new KpiCard("Upcoming visits", String.valueOf(upcoming.size()), "next appointments", "/patient/appointments", "primary"),
                new KpiCard("Prescriptions", String.valueOf(prescriptions.size()), "active and past", "/patient/prescriptions", "success"),
                new KpiCard("Patient ID", patient.getPublicPatientId(), "your master identifier", "/patient/profile", "info"),
                new KpiCard("Blood group", patient.getBloodGroup() == null ? "-" : patient.getBloodGroup(), "medical profile", "/patient/profile", "neutral")
        );

        List<ActivityRow> activity = new ArrayList<>();
        upcoming.forEach(appointment -> activity.add(new ActivityRow(
                appointment.getClinic().getName(),
                appointment.getDoctor().getName() + " - " + TIME.format(appointment.getSlotStart()),
                appointment.getStatus().name(), toneForAppointment(appointment.getStatus()),
                "/patient/appointments")));
        if (activity.isEmpty()) {
            activity.add(new ActivityRow("No upcoming appointments", "Find a nearby clinic to book one", "", "neutral", "/patient/clinics"));
        }

        DashboardView view = new DashboardView(
                "Patient dashboard",
                "Your appointments, prescriptions and medical history in one place.",
                "Find a clinic", "/patient/clinics",
                kpis, "Upcoming appointments", activity);
        model.addAttribute("dashboard", view);
        model.addAttribute("pageTitle", view.title());
        return "dashboard/index";
    }

    @GetMapping("/pharmacy/dashboard")
    public String pharmacyDashboard(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        PharmacyUser pharmacyUser = pharmacyUserRepository.findByUserUserId(principal.getUserId()).orElse(null);
        if (pharmacyUser == null) {
            model.addAttribute("dashboard", new DashboardView(
                    "Pharmacy dashboard", "Your pharmacy account is not linked yet.",
                    "Verify prescription", "/pharmacy/verify",
                    List.of(), "Verification activity", List.of(
                            new ActivityRow("No pharmacy linked", "Ask the platform administrator to link your account", "", "warning", "/pharmacy/dashboard"))));
            model.addAttribute("pageTitle", "Pharmacy dashboard");
            return "dashboard/index";
        }
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Long pharmacyId = pharmacyUser.getPharmacy().getPharmacyId();

        List<KpiCard> kpis = List.of(
                new KpiCard("Verifications today", String.valueOf(pharmacyVerificationRepository.countByPharmacyPharmacyIdAndVerifiedAtBetween(pharmacyId, start, end)), "prescription lookups", "/pharmacy/verify", "primary"),
                new KpiCard("Dispenses today", String.valueOf(prescriptionDispenseRepository.countByPharmacyPharmacyIdAndDispensedAtBetween(pharmacyId, start, end)), "fills completed", "/pharmacy/reports", "success"),
                new KpiCard("Pharmacy users", String.valueOf(pharmacyUserRepository.findByPharmacyPharmacyId(pharmacyId).size()), "team accounts", "/pharmacy/users", "info")
        );

        DashboardView view = new DashboardView(
                "Pharmacy dashboard",
                "Verify prescription IDs, dispense medicines and monitor fills.",
                "Verify prescription", "/pharmacy/verify",
                kpis, "Recent activity", List.of(
                        new ActivityRow("Ready to verify", "Enter a 32-character Prescription ID to begin", "", "neutral", "/pharmacy/verify")));
        model.addAttribute("dashboard", view);
        model.addAttribute("pageTitle", view.title());
        return "dashboard/index";
    }

    private String toneForAppointment(AppointmentStatus status) {
        return switch (status) {
            case BOOKED -> "info";
            case CONFIRMED -> "primary";
            case CHECKED_IN -> "warning";
            case IN_CONSULTATION -> "warning";
            case COMPLETED -> "success";
            case CANCELLED, NO_SHOW -> "danger";
        };
    }

    private String toneForClinic(ClinicStatus status) {
        return switch (status) {
            case ACTIVE -> "success";
            case PENDING -> "warning";
            case SUSPENDED -> "danger";
        };
    }
}