package com.clinicflow.web;

import com.clinicflow.common.ClientIp;
import com.clinicflow.domain.enums.AuthTokenPurpose;
import com.clinicflow.identity.AuthTokenService;
import com.clinicflow.identity.InvalidTokenException;
import com.clinicflow.identity.PasswordPolicy;
import com.clinicflow.identity.PasswordResetService;
import com.clinicflow.identity.WeakPasswordException;
import com.clinicflow.security.AuthRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.util.Locale;

@Controller
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final AuthTokenService authTokenService;
    private final AuthRateLimiter rateLimiter;

    public PasswordResetController(PasswordResetService passwordResetService,
                                   AuthTokenService authTokenService,
                                   AuthRateLimiter rateLimiter) {
        this.passwordResetService = passwordResetService;
        this.authTokenService = authTokenService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/forgot-password")
    public String forgotForm(Model model) {
        model.addAttribute("pageTitle", "Forgot password");
        model.addAttribute("authWide", false);
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestReset(@RequestParam(required = false) String email,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        String normalized = email == null ? "" : email.trim();
        if (!normalized.isEmpty()
                && rateLimiter.allow("forgot|" + ClientIp.resolve(request) + "|" + normalized.toLowerCase(Locale.ROOT),
                5, Duration.ofMinutes(15))) {
            passwordResetService.request(normalized);
        }
        redirectAttributes.addFlashAttribute("resetEmail", normalized);
        return "redirect:/forgot-password/sent";
    }

    @GetMapping("/forgot-password/sent")
    public String sent(Model model) {
        model.addAttribute("pageTitle", "Check your email");
        model.addAttribute("authWide", false);
        return "auth/forgot-password-sent";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("pageTitle", "Reset password");
        model.addAttribute("authWide", false);
        if (token == null || token.isBlank()
                || !authTokenService.isValid(token, AuthTokenPurpose.RESET_PASSWORD)) {
            model.addAttribute("invalidToken", true);
            return "auth/reset-password";
        }
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(@RequestParam(required = false) String token,
                        @RequestParam(required = false) String newPassword,
                        @RequestParam(required = false) String confirmPassword,
                        HttpServletRequest request,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        model.addAttribute("pageTitle", "Reset password");
        model.addAttribute("authWide", false);

        if (!rateLimiter.allow("reset|" + ClientIp.resolve(request), 10, Duration.ofMinutes(15))) {
            model.addAttribute("token", token);
            model.addAttribute("resetError", "Too many attempts. Try again in a few minutes.");
            return "auth/reset-password";
        }

        if (newPassword == null || confirmPassword == null || !newPassword.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("resetError", "Passwords do not match.");
            return "auth/reset-password";
        }
        if (!PasswordPolicy.isValid(newPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("resetError", PasswordPolicy.message());
            return "auth/reset-password";
        }

        try {
            passwordResetService.reset(token, newPassword);
        } catch (InvalidTokenException | WeakPasswordException ex) {
            model.addAttribute("invalidToken", true);
            model.addAttribute("resetError", ex.getMessage());
            return "auth/reset-password";
        }

        redirectAttributes.addFlashAttribute("passwordReset", true);
        return "redirect:/login?reset";
    }
}
