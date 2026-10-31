package com.clinicflow.web.patient;

import com.clinicflow.security.AppUserPrincipal;
import com.clinicflow.web.prescription.PrescriptionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PatientPrescriptionController {

    private final PrescriptionService prescriptionService;

    public PatientPrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping("/patient/prescriptions")
    public String list(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("prescriptions", prescriptionService.patientList(principal.getUserId()));
        model.addAttribute("pageTitle", "My Prescriptions");
        return "patient/prescriptions";
    }

    @GetMapping("/patient/prescriptions/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("prescription", prescriptionService.patientDetail(principal.getUserId(), id,
                    principal.getUserId()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("prescriptionError", ex.getMessage());
            return "redirect:/patient/prescriptions";
        }
        model.addAttribute("pageTitle", "Prescription");
        return "patient/prescription-detail";
    }

    @PostMapping("/patient/prescriptions/{id}/email")
    public String email(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                        RedirectAttributes redirectAttributes) {
        try {
            prescriptionService.resendForPatient(principal.getUserId(), id, principal.getUserId());
            redirectAttributes.addFlashAttribute("prescriptionSaved",
                    "Prescription email queued to your verified address.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("prescriptionError", ex.getMessage());
        }
        return "redirect:/patient/prescriptions/" + id;
    }
}
