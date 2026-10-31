package com.clinicflow.web;

import com.clinicflow.identity.EmailChangeService;
import com.clinicflow.identity.InvalidTokenException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EmailChangeVerificationController {

    private final EmailChangeService emailChangeService;

    public EmailChangeVerificationController(EmailChangeService emailChangeService) {
        this.emailChangeService = emailChangeService;
    }

    @GetMapping("/verify-email-change")
    public String verify(@RequestParam(required = false) String token,
                         HttpServletRequest request, Model model) {
        try {
            String email = emailChangeService.confirm(token);
            model.addAttribute("changedEmail", email);
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
        } catch (InvalidTokenException | IllegalArgumentException ex) {
            model.addAttribute("verificationError", ex.getMessage());
        }
        model.addAttribute("pageTitle", "Email change");
        return "auth/email-change-result";
    }
}
