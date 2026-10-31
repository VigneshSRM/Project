package com.clinicflow.security;

import com.clinicflow.domain.entity.Clinic;
import com.clinicflow.domain.enums.ClinicStatus;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.repository.ClinicRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * A session created before a clinic is suspended must stop working immediately.
 * The principal only stores the clinic id, so this filter rechecks the current
 * clinic state for every clinic or doctor request.
 */
@Component
public class ClinicStatusFilter extends OncePerRequestFilter {

    private final ClinicRepository clinicRepository;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public ClinicStatusFilter(ClinicRepository clinicRepository) {
        this.clinicRepository = clinicRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/clinic/") || path.startsWith("/doctor/")
                || path.equals("/post-login"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal
                && principal.getClinicId() != null
                && (principal.getRole() == Role.CLINIC_ADMIN || principal.getRole() == Role.DOCTOR)) {
            Optional<Clinic> clinic = clinicRepository.findById(principal.getClinicId());
            if (clinic.isEmpty() || clinic.get().getStatus() != ClinicStatus.ACTIVE) {
                logoutHandler.logout(request, response, authentication);
                response.sendRedirect(request.getContextPath() + "/login?clinicSuspended");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
