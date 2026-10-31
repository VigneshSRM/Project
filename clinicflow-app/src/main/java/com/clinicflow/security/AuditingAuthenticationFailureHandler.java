package com.clinicflow.security;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.enums.AuditOutcome;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuditingAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(AuditingAuthenticationFailureHandler.class);

    private final LoginAttemptService attempts;
    private final AuditService auditService;

    public AuditingAuthenticationFailureHandler(LoginAttemptService attempts, AuditService auditService) {
        this.attempts = attempts;
        this.auditService = auditService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("email");
        attempts.onFailure(attempts.key(request, email));
        try {
            auditService.record(null, null, "LOGIN", "User", email, null, AuditOutcome.FAILURE);
        } catch (RuntimeException auditFailure) {
            // Never let the audit store turn an authentication response into a 500.
            log.error("Could not write login failure audit entry", auditFailure);
        }
        response.sendRedirect(request.getContextPath() + "/login?error");
    }
}
