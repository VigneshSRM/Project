package com.clinicflow.web;

import com.clinicflow.audit.AuditService;
import com.clinicflow.common.ClientIp;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.Gender;
import com.clinicflow.identity.DuplicateEmailException;
import com.clinicflow.identity.InvalidTokenException;
import com.clinicflow.identity.PasswordPolicy;
import com.clinicflow.identity.RegistrationForm;
import com.clinicflow.identity.RegistrationService;
import com.clinicflow.identity.VerificationService;
import com.clinicflow.identity.WeakPasswordException;
import com.clinicflow.security.AuthRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;

@Controller
public class RegistrationController {

    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

    private final RegistrationService registrationService;
    private final VerificationService verificationService;
    private final AuditService auditService;
    private final AuthRateLimiter rateLimiter;

    public RegistrationController(RegistrationService registrationService,
                                  VerificationService verificationService,
                                  AuditService auditService,
                                  AuthRateLimiter rateLimiter) {
        this.registrationService = registrationService;
        this.verificationService = verificationService;
        this.auditService = auditService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/register")
    public String form(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        addRegistrationModel(model);
        return "auth/register";
    }

    @PostMapping("/register")
    public String submit(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        if (!rateLimiter.allow("register|" + ClientIp.resolve(request), 10, Duration.ofMinutes(15))) {
            model.addAttribute("rateLimited", true);
            addRegistrationModel(model);
            return "auth/register";
        }

        if (form.getPassword() != null && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match.");
        }
        if (form.getPassword() != null && !PasswordPolicy.isValid(form.getPassword())) {
            bindingResult.rejectValue("password", "password.weak", PasswordPolicy.message());
        }
        if (bindingResult.hasErrors()) {
            addRegistrationModel(model);
            return "auth/register";
        }

        try {
            registrationService.register(form);
        } catch (DuplicateEmailException duplicate) {
            bindingResult.rejectValue("email", "email.exists", duplicate.getMessage());
            addRegistrationModel(model);
            auditFailure(form.getEmail());
            return "auth/register";
        } catch (WeakPasswordException weak) {
            bindingResult.rejectValue("password", "password.weak", weak.getMessage());
            addRegistrationModel(model);
            return "auth/register";
        }

        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        redirectAttributes.addFlashAttribute("registeredEmail", email);
        return "redirect:/register/success";
    }

    @GetMapping("/register/success")
    public String success(@ModelAttribute("registeredEmail") String registeredEmail, Model model) {
        if (registeredEmail == null || registeredEmail.isBlank()) {
            return "redirect:/register";
        }
        model.addAttribute("pageTitle", "Check your email");
        model.addAttribute("authWide", false);
        return "auth/register-success";
    }

    @PostMapping("/register/resend")
    public String resend(@RequestParam(required = false) String email,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        String normalized = email == null ? "" : email.trim();
        if (normalized.isEmpty()) {
            redirectAttributes.addFlashAttribute("resendError", "Enter the email address you registered with.");
            return "redirect:/verify-email";
        }
        if (rateLimiter.allow("resend|" + ClientIp.resolve(request) + "|" + normalized.toLowerCase(Locale.ROOT),
                5, Duration.ofMinutes(15))) {
            registrationService.resendVerification(normalized);
        }
        redirectAttributes.addFlashAttribute("registeredEmail", normalized.toLowerCase(Locale.ROOT));
        redirectAttributes.addFlashAttribute("resendMessage",
                "If that address is waiting for verification, a new link has been sent.");
        return "redirect:/register/success";
    }

    @GetMapping("/verify-email")
    public String verify(@RequestParam(required = false) String token,
                         @RequestParam(required = false) String verified,
                         @RequestParam(required = false) String resent,
                         Model model) {
        model.addAttribute("pageTitle", "Verify email");
        model.addAttribute("authWide", false);
        if (token != null && !token.isBlank()) {
            try {
                verificationService.verify(token);
                return "redirect:/verify-email?verified=1";
            } catch (InvalidTokenException ex) {
                model.addAttribute("verified", false);
                model.addAttribute("verifyError", ex.getMessage());
            }
        }
        if ("1".equals(verified)) {
            model.addAttribute("verified", true);
        }
        // Do not add a false value here: it would shadow the "resent" flash
        // attribute set by the resend POST when it redirects back to this page.
        if (resent != null) {
            model.addAttribute("resent", true);
        }
        return "auth/verify-email";
    }

    @PostMapping("/verify-email/resend")
    public String resendFromVerificationPage(@RequestParam(required = false) String email,
                                             RedirectAttributes redirectAttributes,
                                             HttpServletRequest request) {
        String normalized = email == null ? "" : email.trim();
        if (normalized.isEmpty()) {
            redirectAttributes.addFlashAttribute("resendError",
                    "Enter the email address you registered with.");
            return "redirect:/verify-email";
        }
        if (rateLimiter.allow("resend|" + ClientIp.resolve(request) + "|" + normalized.toLowerCase(Locale.ROOT),
                5, Duration.ofMinutes(15))) {
            registrationService.resendVerification(normalized);
        }
        redirectAttributes.addFlashAttribute("resent", true);
        return "redirect:/verify-email";
    }

    private void addRegistrationModel(Model model) {
        model.addAttribute("pageTitle", "Create account");
        model.addAttribute("authWide", true);
        model.addAttribute("genders", Gender.values());
        model.addAttribute("today", LocalDate.now().toString());
    }

    private void auditFailure(String email) {
        try {
            auditService.record(null, null, "PATIENT_REGISTRATION", "User", email, null, AuditOutcome.FAILURE);
        } catch (RuntimeException ex) {
            log.error("Could not write registration failure audit entry", ex);
        }
    }
}
