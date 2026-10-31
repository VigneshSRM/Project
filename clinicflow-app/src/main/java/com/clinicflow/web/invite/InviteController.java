package com.clinicflow.web.invite;

import com.clinicflow.identity.InvalidTokenException;
import com.clinicflow.identity.InvitationService;
import com.clinicflow.identity.PasswordPolicy;
import com.clinicflow.identity.WeakPasswordException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class InviteController {

    private final InvitationService invitationService;

    public InviteController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping("/invite")
    public String invite(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("pageTitle", "Accept invitation");
        if (token == null || token.isBlank()) {
            model.addAttribute("invalidInvite", true);
            return "auth/invite";
        }
        var context = invitationService.context(token);
        if (context.isEmpty()) {
            model.addAttribute("invalidInvite", true);
            return "auth/invite";
        }
        InviteForm form = new InviteForm();
        form.setToken(token);
        model.addAttribute("inviteForm", form);
        model.addAttribute("invite", context.get());
        return "auth/invite";
    }

    @PostMapping("/invite")
    public String accept(@Valid @ModelAttribute("inviteForm") InviteForm form,
                         BindingResult bindingResult,
                         Model model) {
        model.addAttribute("pageTitle", "Accept invitation");
        var context = invitationService.context(form.getToken());
        if (context.isEmpty()) {
            model.addAttribute("invalidInvite", true);
            return "auth/invite";
        }
        model.addAttribute("invite", context.get());
        if (form.getPassword() != null && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }
        if (form.getPassword() != null && !PasswordPolicy.isValid(form.getPassword())) {
            bindingResult.rejectValue("password", "password.weak", PasswordPolicy.message());
        }
        if (bindingResult.hasErrors()) {
            return "auth/invite";
        }
        try {
            invitationService.accept(form.getToken(), form.getPassword());
        } catch (InvalidTokenException ex) {
            model.addAttribute("invalidInvite", true);
            model.addAttribute("inviteError", ex.getMessage());
            return "auth/invite";
        } catch (WeakPasswordException ex) {
            bindingResult.rejectValue("password", "password.weak", ex.getMessage());
            return "auth/invite";
        }
        return "redirect:/login?invited=1";
    }
}
