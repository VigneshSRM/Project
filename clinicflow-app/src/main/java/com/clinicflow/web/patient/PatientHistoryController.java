package com.clinicflow.web.patient;

import com.clinicflow.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PatientHistoryController {

    private final PatientHistoryService historyService;

    public PatientHistoryController(PatientHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/patient/history")
    public String history(@AuthenticationPrincipal AppUserPrincipal principal, Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("history", historyService.history(principal.getUserId()));
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("profileError", ex.getMessage());
            return "redirect:/patient/profile";
        }
        model.addAttribute("pageTitle", "My Medical History");
        return "patient/history";
    }
}
