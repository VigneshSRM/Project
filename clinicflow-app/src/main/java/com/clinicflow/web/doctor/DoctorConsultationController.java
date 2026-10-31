package com.clinicflow.web.doctor;

import com.clinicflow.domain.entity.ClinicMedicine;
import com.clinicflow.domain.enums.AppointmentStatus;
import com.clinicflow.security.AppUserPrincipal;
import com.clinicflow.web.clinic.ClinicInventoryService;
import com.clinicflow.web.prescription.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class DoctorConsultationController {

    private final DoctorConsultationService consultationService;
    private final PrescriptionService prescriptionService;
    private final ClinicInventoryService inventoryService;

    public DoctorConsultationController(DoctorConsultationService consultationService,
                                        PrescriptionService prescriptionService,
                                        ClinicInventoryService inventoryService) {
        this.consultationService = consultationService;
        this.prescriptionService = prescriptionService;
        this.inventoryService = inventoryService;
    }

    @GetMapping("/doctor/appointments")
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(required = false) AppointmentStatus status,
                       @RequestParam(required = false, defaultValue = "day") String scope,
                       @AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        boolean allDates = "all".equals(scope);
        LocalDate selected = allDates ? null : (date == null ? LocalDate.now() : date);
        model.addAttribute("appointments", consultationService.list(principal.getUserId(), selected, status));
        model.addAttribute("selectedDate", selected);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("allDates", allDates);
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("pageTitle", "My Appointments");
        return "doctor/appointments";
    }

    @GetMapping("/doctor/appointments/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes redirectAttributes) {
        DoctorConsultationService.DoctorAppointmentDetail appointment;
        try {
            appointment = consultationService.detail(principal.getUserId(), id, principal.getUserId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("appointmentError", ex.getMessage());
            return "redirect:/doctor/appointments";
        }
        if (!model.containsAttribute("consultationForm")) {
            ConsultationForm form = new ConsultationForm();
            form.setDiagnosis(appointment.diagnosis());
            form.setNotes(appointment.notes());
            form.setFollowUp(appointment.followUpValue());
            model.addAttribute("consultationForm", form);
        }
        model.addAttribute("appointment", appointment);
        model.addAttribute("pageTitle", "Consultation");
        return "doctor/consultation";
    }

    @PostMapping("/doctor/appointments/{id}/start")
    public String start(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                        RedirectAttributes redirectAttributes) {
        try {
            consultationService.start(principal.getUserId(), id, principal.getUserId());
            redirectAttributes.addFlashAttribute("consultationSaved", "Consultation started.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("consultationError", ex.getMessage());
        }
        return "redirect:/doctor/appointments/" + id;
    }

    @PostMapping("/doctor/appointments/{id}/draft")
    public String saveDraft(@PathVariable Long id,
                            @Valid @ModelAttribute("consultationForm") ConsultationForm form,
                            BindingResult bindingResult,
                            @AuthenticationPrincipal AppUserPrincipal principal,
                            Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("appointment", consultationService.detail(principal.getUserId(), id,
                    principal.getUserId()));
            model.addAttribute("pageTitle", "Consultation");
            return "doctor/consultation";
        }
        try {
            consultationService.saveDraft(principal.getUserId(), id, form, principal.getUserId());
            redirectAttributes.addFlashAttribute("consultationSaved", "Draft saved.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("consultationError", ex.getMessage());
        }
        return "redirect:/doctor/appointments/" + id;
    }

    @PostMapping("/doctor/appointments/{id}/finalise")
    public String finalise(@PathVariable Long id,
                           @Valid @ModelAttribute("consultationForm") ConsultationForm form,
                           BindingResult bindingResult,
                           @AuthenticationPrincipal AppUserPrincipal principal,
                           Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("appointment", consultationService.detail(principal.getUserId(), id,
                    principal.getUserId()));
            model.addAttribute("pageTitle", "Consultation");
            return "doctor/consultation";
        }
        try {
            consultationService.finalise(principal.getUserId(), id, form, principal.getUserId());
            redirectAttributes.addFlashAttribute("consultationSaved",
                    "Consultation finalised. The appointment is completed.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("consultationError", ex.getMessage());
        }
        return "redirect:/doctor/appointments/" + id;
    }

    @PostMapping("/doctor/appointments/{id}/addenda")
    public String addendum(@PathVariable Long id,
                           @RequestParam String addendumReason,
                           @RequestParam String addendumText,
                           @AuthenticationPrincipal AppUserPrincipal principal,
                           RedirectAttributes redirectAttributes) {
        try {
            consultationService.addAddendum(principal.getUserId(), id, addendumReason, addendumText,
                    principal.getUserId());
            redirectAttributes.addFlashAttribute("consultationSaved", "Addendum added.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("consultationError", ex.getMessage());
        }
        return "redirect:/doctor/appointments/" + id;
    }

    private void addInventoryModel(Model model, Long userId) {
        List<java.util.Map<String, Object>> inventory = inventoryService.optionsForDoctor(userId);
        model.addAttribute("inventory", inventory);
        model.addAttribute("medicineOptions", inventory.stream()
                .map(item -> item.get("medicine"))
                .filter(value -> value != null && !String.valueOf(value).isBlank())
                .map(String::valueOf).distinct().sorted().toList());
        model.addAttribute("dosageOptions", inventory.stream()
                .map(item -> item.get("dosage"))
                .filter(value -> value != null && !String.valueOf(value).isBlank())
                .map(String::valueOf).distinct().sorted().toList());
    }

    @GetMapping("/doctor/appointments/{id}/prescription/new")
    public String prescriptionForm(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                                   Model model, RedirectAttributes redirectAttributes) {
        DoctorConsultationService.DoctorAppointmentDetail appointment;
        try {
            appointment = consultationService.detail(principal.getUserId(), id, principal.getUserId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("appointmentError", ex.getMessage());
            return "redirect:/doctor/appointments";
        }
        if (appointment.consultationId() == null) {
            redirectAttributes.addFlashAttribute("consultationError", "Start the consultation first.");
            return "redirect:/doctor/appointments/" + id;
        }
        if (appointment.prescriptionCode() != null) {
            redirectAttributes.addFlashAttribute("consultationError",
                    "A prescription already exists for this consultation.");
            return "redirect:/doctor/appointments/" + id;
        }
        if (!model.containsAttribute("prescriptionForm")) {
            model.addAttribute("prescriptionForm", new PrescriptionForm());
        }
        model.addAttribute("appointment", appointment);
        addInventoryModel(model, principal.getUserId());
        model.addAttribute("pageTitle", "New prescription");
        return "doctor/prescription-form";
    }

    @PostMapping("/doctor/appointments/{id}/prescription")
    public String createPrescription(@PathVariable Long id,
                                     @Valid @ModelAttribute("prescriptionForm") PrescriptionForm form,
                                     BindingResult bindingResult,
                                     @AuthenticationPrincipal AppUserPrincipal principal,
                                     Model model, RedirectAttributes redirectAttributes) {
        DoctorConsultationService.DoctorAppointmentDetail appointment;
        try {
            appointment = consultationService.detail(principal.getUserId(), id, principal.getUserId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("appointmentError", ex.getMessage());
            return "redirect:/doctor/appointments";
        }
        if (bindingResult.hasErrors() || appointment.consultationId() == null) {
            model.addAttribute("appointment", appointment);
            addInventoryModel(model, principal.getUserId());
            model.addAttribute("pageTitle", "New prescription");
            if (appointment.consultationId() == null) {
                model.addAttribute("prescriptionError", "Start the consultation first.");
            }
            return "doctor/prescription-form";
        }
        try {
            Long prescriptionId = prescriptionService.create(principal.getUserId(),
                    appointment.consultationId(), form, principal.getUserId());
            redirectAttributes.addFlashAttribute("prescriptionSaved", "Prescription issued.");
            return "redirect:/doctor/prescriptions/" + prescriptionId;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("appointment", appointment);
            addInventoryModel(model, principal.getUserId());
            model.addAttribute("prescriptionError", ex.getMessage());
            model.addAttribute("pageTitle", "New prescription");
            return "doctor/prescription-form";
        }
    }
}
