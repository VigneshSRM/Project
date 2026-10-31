package com.clinicflow.web;

import com.clinicflow.security.AppUserPrincipal;
import com.clinicflow.security.RoleHomeResolver;
import com.clinicflow.web.setup.SetupService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    private final SetupService setupService;

    public AuthController(SetupService setupService) {
        this.setupService = setupService;
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        if (principal != null) {
            return "redirect:" + RoleHomeResolver.homeFor(principal.getRole());
        }
        model.addAttribute("pageTitle", "Sign in");
        model.addAttribute("setupRequired", setupService.needsSetup());
        return "auth/login";
    }

    @GetMapping("/post-login")
    public String postLogin(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        return "redirect:" + RoleHomeResolver.homeFor(principal.getRole());
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null) {
            return setupService.needsSetup() ? "redirect:/setup" : "redirect:/login";
        }
        return "redirect:" + RoleHomeResolver.homeFor(principal.getRole());
    }

    @GetMapping("/error/403")
    public String forbidden(Model model) {
        model.addAttribute("pageTitle", "Access denied");
        return "error/403";
    }
}
