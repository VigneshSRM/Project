package com.clinicflow.audit;

import com.clinicflow.common.ClientIp;
import com.clinicflow.domain.entity.AuditLog;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.repository.AuditLogRepository;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.UserRepository;
import com.clinicflow.security.AppUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Append-only evidence for sensitive identity and clinical actions. The application
 * has no update/delete path for AuditLogs.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    public AuditService(AuditLogRepository auditLogRepository,
                        UserRepository userRepository,
                        PatientRepository patientRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional
    public void recordCurrentActor(String action, String resourceType, String resourceId,
                                   Long patientId, AuditOutcome outcome) {
        AppUserPrincipal principal = currentPrincipal();
        record(principal == null ? null : principal.getUserId(),
                principal == null ? null : principal.getRole(),
                action, resourceType, resourceId, patientId, outcome);
    }

    @Transactional
    public void record(Long actorUserId, Role role, String action, String resourceType,
                       String resourceId, Long patientId, AuditOutcome outcome) {
        AuditLog entry = new AuditLog();
        if (actorUserId != null) {
            entry.setActor(userRepository.getReferenceById(actorUserId));
        }
        entry.setRole(role == null ? null : role.name());
        entry.setAction(trim(action, 80));
        entry.setResourceType(trim(resourceType, 60));
        entry.setResourceId(trim(resourceId, 100));
        if (patientId != null) {
            entry.setPatient(patientRepository.getReferenceById(patientId));
        }
        entry.setIpAddress(currentIp());
        entry.setOutcome(outcome == null ? AuditOutcome.FAILURE : outcome);
        entry.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        auditLogRepository.save(entry);
    }

    private AppUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            return principal;
        }
        return null;
    }

    private String currentIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            return ClientIp.resolve(request);
        }
        return null;
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
