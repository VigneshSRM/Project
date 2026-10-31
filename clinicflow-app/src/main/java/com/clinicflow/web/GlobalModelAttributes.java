package com.clinicflow.web;

import com.clinicflow.security.AppUserPrincipal;
import com.clinicflow.web.nav.NavigationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final NavigationService navigationService;

    public GlobalModelAttributes(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @ModelAttribute("currentUser")
    public AppUserPrincipal currentUser(@AuthenticationPrincipal AppUserPrincipal principal) {
        return principal;
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("navItems")
    public Object navItems(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null) {
            return java.util.List.of();
        }
        return navigationService.itemsFor(principal.getRole());
    }

    @ModelAttribute("breadcrumbs")
    public Object breadcrumbs(@AuthenticationPrincipal AppUserPrincipal principal, HttpServletRequest request) {
        if (principal == null) {
            return java.util.List.of();
        }
        return navigationService.breadcrumbsFor(principal.getRole(), request.getRequestURI());
    }

    @ModelAttribute("roleLabel")
    public String roleLabel(@AuthenticationPrincipal AppUserPrincipal principal) {
        return principal == null ? "" : navigationService.roleLabel(principal.getRole());
    }
}