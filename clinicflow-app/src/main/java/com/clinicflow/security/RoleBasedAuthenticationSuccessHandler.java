package com.clinicflow.security;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.enums.AuditOutcome;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(RoleBasedAuthenticationSuccessHandler.class);

    private final LoginAttemptService attempts;
    private final AuditService auditService;

    public RoleBasedAuthenticationSuccessHandler(LoginAttemptService attempts, AuditService auditService) {
        this.attempts = attempts;
        this.auditService = auditService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            attempts.onSuccess(attempts.key(request, principal.getEmail()));
            try {
                auditService.record(principal.getUserId(), principal.getRole(), "LOGIN", "User",
                        principal.getEmail(), null, AuditOutcome.SUCCESS);
            } catch (RuntimeException auditFailure) {
                log.error("Could not write login success audit entry", auditFailure);
            }
            response.sendRedirect(request.getContextPath() + RoleHomeResolver.homeFor(principal.getRole()));
            return;
        }
        response.sendRedirect(request.getContextPath() + "/");
    }
}
