package com.clinicflow.web.setup;

import com.clinicflow.identity.WeakPasswordException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SetupController {

    private final SetupService setupService;

    public SetupController(SetupService setupService) {
        this.setupService = setupService;
    }

    @GetMapping("/setup")
    public String setup(Model model) {
        if (!setupService.needsSetup()) {
            return "redirect:/login";
        }
        if (!model.containsAttribute("setupForm")) {
            model.addAttribute("setupForm", new SetupForm());
        }
        model.addAttribute("pageTitle", "First-time setup");
        return "setup/index";
    }

    @PostMapping("/setup")
    public String create(@Valid @ModelAttribute("setupForm") SetupForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!setupService.needsSetup()) {
            return "redirect:/login";
        }
        if (form.getPassword() != null && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "First-time setup");
            return "setup/index";
        }
        try {
            setupService.createAdministrator(form);
        } catch (WeakPasswordException ex) {
            bindingResult.reject("setup.failed", ex.getMessage());
            model.addAttribute("pageTitle", "First-time setup");
            return "setup/index";
        } catch (IllegalStateException ex) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("setupComplete",
                "Platform administrator created. Sign in to enroll the first clinic.");
        return "redirect:/login";
    }
}
