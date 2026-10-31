package com.clinicflow.web.patient;

import com.clinicflow.domain.enums.Gender;
import com.clinicflow.domain.enums.UserStatus;
import com.clinicflow.identity.EmailChangeService;
import com.clinicflow.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/patient/profile")
public class PatientProfileController {

    private final PatientProfileService profileService;
    private final PatientLinkService linkService;
    private final EmailChangeService emailChangeService;

    public PatientProfileController(PatientProfileService profileService, PatientLinkService linkService,
                                    EmailChangeService emailChangeService) {
        this.profileService = profileService;
        this.linkService = linkService;
        this.emailChangeService = emailChangeService;
    }

    @GetMapping
    public String view(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        Optional<PatientProfileView> view = profileService.load(principal.getUserId());
        if (view.isEmpty()) {
            model.addAttribute("pageTitle", "My Profile");
            model.addAttribute("missingProfile", true);
            return "patient/profile";
        }
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", PatientProfileForm.from(view.get().patient()));
        }
        addPageModel(model, view.get(), principal);
        return "patient/profile";
    }

    @PostMapping
    public String update(@Valid @ModelAttribute("profileForm") PatientProfileForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Optional<PatientProfileView> view = profileService.load(principal.getUserId());
            if (view.isEmpty()) {
                redirectAttributes.addFlashAttribute("profileError",
                        "Your patient record is not available yet.");
                return "redirect:/patient/dashboard";
            }
            addPageModel(model, view.get(), principal);
            return "patient/profile";
        }

        if (!profileService.update(principal.getUserId(), form)) {
            redirectAttributes.addFlashAttribute("profileError",
                    "Your patient record is not available yet.");
            return "redirect:/patient/dashboard";
        }
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/patient/profile";
    }

    @PostMapping("/email")
    public String changeEmail(@Valid @ModelAttribute("emailChangeForm") EmailChangeForm form,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal AppUserPrincipal principal,
                              Model model, RedirectAttributes redirectAttributes) {
        Optional<PatientProfileView> view = profileService.load(principal.getUserId());
        if (view.isEmpty()) {
            redirectAttributes.addFlashAttribute("profileError",
                    "Your patient record is not available yet.");
            return "redirect:/patient/dashboard";
        }
        String error = null;
        if (bindingResult.hasErrors()) {
            error = bindingResult.getAllErrors().getFirst().getDefaultMessage();
        } else {
            try {
                emailChangeService.request(principal.getUserId(), form.getNewEmail(),
                        form.getCurrentPassword(), principal.getUserId());
                redirectAttributes.addFlashAttribute("emailChangeRequested",
                        "Check the new inbox for a verification link. Your login email stays "
                                + "unchanged until the link is opened.");
                return "redirect:/patient/profile";
            } catch (IllegalArgumentException ex) {
                error = ex.getMessage();
            }
        }
        model.addAttribute("emailChangeError", error);
        model.addAttribute("emailChangeForm", form);
        addPageModel(model, view.get(), principal);
        return "patient/profile";
    }

    private void addPageModel(Model model, PatientProfileView view, AppUserPrincipal principal) {
        model.addAttribute("profile", view);
        model.addAttribute("genders", Gender.values());
        model.addAttribute("pageTitle", "My Profile");
        model.addAttribute("statusLabel", statusLabel(principal.getStatus()));
        model.addAttribute("statusTone", statusTone(principal.getStatus()));
        model.addAttribute("linkRequestForm", new PatientLinkRequestForm());
        model.addAttribute("clinicOptions", linkService.availableClinics(principal.getUserId()));
        if (!model.containsAttribute("emailChangeForm")) {
            model.addAttribute("emailChangeForm", new EmailChangeForm());
        }
    }

    private static String statusLabel(UserStatus status) {
        return switch (status) {
            case ACTIVE -> "Active";
            case PENDING -> "Pending verification";
            case SUSPENDED -> "Suspended";
            case DISABLED -> "Disabled";
        };
    }

    private static String statusTone(UserStatus status) {
        return switch (status) {
            case ACTIVE -> "success";
            case PENDING -> "warning";
            case SUSPENDED -> "danger";
            case DISABLED -> "neutral";
        };
    }
}
