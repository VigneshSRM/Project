package com.clinicflow.web.clinic;

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
public class ClinicPrescriptionController {

    private final PrescriptionService prescriptionService;

    public ClinicPrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping("/clinic/prescriptions")
    public String list(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("prescriptions", prescriptionService.clinicList(principal.getClinicId()));
        model.addAttribute("pageTitle", "Prescriptions");
        return "clinic/prescriptions";
    }

    @GetMapping("/clinic/prescriptions/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("prescription",
                    prescriptionService.clinicDetail(principal.getClinicId(), id));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("prescriptionError", ex.getMessage());
            return "redirect:/clinic/prescriptions";
        }
        model.addAttribute("pageTitle", "Prescription");
        return "clinic/prescription-detail";
    }

    @PostMapping("/clinic/prescriptions/{id}/cancel")
    public String cancel(@PathVariable Long id, @RequestParam(required = false) String reason,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            prescriptionService.cancelByClinic(principal.getClinicId(), id, reason, principal.getUserId());
            redirectAttributes.addFlashAttribute("prescriptionSaved", "Prescription cancelled.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("prescriptionError", ex.getMessage());
        }
        return "redirect:/clinic/prescriptions/" + id;
    }

    @PostMapping("/clinic/prescriptions/{id}/email")
    public String email(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                        RedirectAttributes redirectAttributes) {
        try {
            prescriptionService.resendForClinic(principal.getClinicId(), id, principal.getUserId());
            redirectAttributes.addFlashAttribute("prescriptionSaved",
                    "Prescription email queued to the patient.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("prescriptionError", ex.getMessage());
        }
        return "redirect:/clinic/prescriptions/" + id;
    }
}
