package com.clinicflow.web.doctor;

import com.clinicflow.security.AppUserPrincipal;
import com.clinicflow.web.prescription.PrescriptionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DoctorPrescriptionController {

    private final PrescriptionService prescriptionService;

    public DoctorPrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping("/doctor/prescriptions")
    public String list(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("prescriptions", prescriptionService.doctorList(principal.getUserId()));
        model.addAttribute("pageTitle", "Prescriptions");
        return "doctor/prescriptions";
    }

    @GetMapping("/doctor/prescriptions/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("prescription",
                    prescriptionService.doctorDetail(principal.getUserId(), id));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("prescriptionError", ex.getMessage());
            return "redirect:/doctor/prescriptions";
        }
        model.addAttribute("pageTitle", "Prescription");
        return "doctor/prescription-detail";
    }

    @PostMapping("/doctor/prescriptions/{id}/cancel")
    public String cancel(@PathVariable Long id, @RequestParam(required = false) String reason,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            prescriptionService.cancelByDoctor(principal.getUserId(), id, reason, principal.getUserId());
            redirectAttributes.addFlashAttribute("prescriptionSaved", "Prescription cancelled.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("prescriptionError", ex.getMessage());
        }
        return "redirect:/doctor/prescriptions/" + id;
    }

    @PostMapping("/doctor/prescriptions/{id}/email")
    public String email(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                        RedirectAttributes redirectAttributes) {
        try {
            prescriptionService.resendForPatient(principal.getUserId(), id, principal.getUserId());
            redirectAttributes.addFlashAttribute("prescriptionSaved",
                    "Prescription email queued to the patient.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("prescriptionError", ex.getMessage());
        }
        return "redirect:/doctor/prescriptions/" + id;
    }
}
