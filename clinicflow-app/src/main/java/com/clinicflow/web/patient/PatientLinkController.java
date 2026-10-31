package com.clinicflow.web.patient;

import com.clinicflow.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The form lives on the profile page. A dedicated POST route keeps the profile
 * controller focused on profile fields and lets the link request be audited on its
 * own path.
 */
@Controller
@RequestMapping("/patient/profile/link-requests")
public class PatientLinkController {

    private final PatientLinkService linkService;

    public PatientLinkController(PatientLinkService linkService) {
        this.linkService = linkService;
    }

    @PostMapping
    public String request(@Valid @ModelAttribute("linkRequestForm") PatientLinkRequestForm form,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal AppUserPrincipal principal,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("linkError", "Select a clinic before sending a request.");
            return "redirect:/patient/profile";
        }

        PatientLinkService.RequestResult result = linkService.request(principal.getUserId(), form.getClinicId());
        if (result.success()) {
            redirectAttributes.addFlashAttribute("linkRequested", result.message());
        } else {
            redirectAttributes.addFlashAttribute("linkError", result.message());
        }
        return "redirect:/patient/profile";
    }
}
