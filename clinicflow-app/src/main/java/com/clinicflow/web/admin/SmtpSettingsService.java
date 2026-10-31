package com.clinicflow.web.admin;

import com.clinicflow.audit.AuditService;
import com.clinicflow.common.SecretCipher;
import com.clinicflow.domain.entity.EmailLog;
import com.clinicflow.domain.entity.SmtpSettings;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.enums.SmtpSecurity;
import com.clinicflow.domain.repository.EmailLogRepository;
import com.clinicflow.domain.repository.SmtpSettingsRepository;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Properties;

@Service
public class SmtpSettingsService {

    private final SmtpSettingsRepository repository;
    private final EmailLogRepository emailLogRepository;
    private final SecretCipher cipher;
    private final AuditService auditService;

    public SmtpSettingsService(SmtpSettingsRepository repository, EmailLogRepository emailLogRepository,
                               SecretCipher cipher, AuditService auditService) {
        this.repository = repository;
        this.emailLogRepository = emailLogRepository;
        this.cipher = cipher;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public SmtpView view() {
        SmtpSettings active = repository.findFirstByActiveTrueOrderByUpdatedAtDesc().orElse(null);
        SmtpSettingsForm form = new SmtpSettingsForm();
        if (active != null) {
            form.setHost(active.getHost());
            form.setPort(active.getPort());
            form.setSecurity(active.getSecurity());
            form.setUsername(active.getUsername());
            form.setFromName(active.getFromName());
            form.setFromAddress(active.getFromAddress());
            form.setActive(active.getActive());
        }
        return new SmtpView(form, active, emailLogRepository.findTop50ByOrderByCreatedAtDesc(),
                active != null && active.getPasswordEncrypted() != null);
    }

    @Transactional
    public void save(SmtpSettingsForm form, Long actorUserId) {
        SmtpSettings settings = repository.findFirstByActiveTrueOrderByUpdatedAtDesc()
                .orElseGet(SmtpSettings::new);
        if (settings.getSettingsId() != null) {
            // Deactivate any other active rows so only one configuration is live.
            repository.findAll().stream()
                    .filter(other -> !other.getSettingsId().equals(settings.getSettingsId()))
                    .filter(SmtpSettings::getActive)
                    .forEach(other -> {
                        other.setActive(false);
                        repository.save(other);
                    });
        }
        settings.setHost(form.getHost().trim());
        settings.setPort(form.getPort());
        settings.setSecurity(form.getSecurity());
        settings.setUsername(trimToNull(form.getUsername()));
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            settings.setPasswordEncrypted(cipher.encrypt(form.getPassword()));
        }
        settings.setFromName(trimToNull(form.getFromName()));
        settings.setFromAddress(trimToNull(form.getFromAddress()));
        settings.setActive(form.isActive());
        settings.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        repository.save(settings);
        auditService.record(actorUserId, Role.SUPER_ADMIN, "SMTP_SETTINGS_SAVED", "SmtpSettings",
                String.valueOf(settings.getSettingsId()), null, AuditOutcome.SUCCESS);
    }

    @Transactional
    public TestResult test(SmtpSettingsForm form, Long actorUserId) {
        String recipient = form.getTestRecipient();
        if (recipient == null || recipient.isBlank()) {
            recipient = form.getFromAddress();
        }
        if (recipient == null || recipient.isBlank()) {
            return new TestResult(false, "Enter a test recipient or a from address.");
        }
        String result;
        boolean success;
        try {
            JavaMailSenderImpl sender = buildSender(form.getHost(), form.getPort(), form.getSecurity(),
                    form.getUsername(), form.getPassword(), form.getFromAddress());
            sender.testConnection();
            success = true;
            result = "Connection succeeded. No message was sent.";
        } catch (Exception ex) {
            success = false;
            result = "Connection failed: " + safeMessage(ex);
        }
        var active = repository.findFirstByActiveTrueOrderByUpdatedAtDesc();
        String summary = result;
        active.ifPresent(settings -> {
            settings.setLastTestAt(LocalDateTime.now(ZoneOffset.UTC));
            settings.setLastTestResult(truncate(summary, 200));
            repository.save(settings);
        });
        auditService.record(actorUserId, Role.SUPER_ADMIN, "SMTP_CONNECTION_TESTED", "SmtpSettings",
                "connection", null, success ? AuditOutcome.SUCCESS : AuditOutcome.FAILURE);
        return new TestResult(success, result);
    }

    @Transactional(readOnly = true)
    public JavaMailSenderImpl activeSender() {
        SmtpSettings active = repository.findFirstByActiveTrueOrderByUpdatedAtDesc().orElse(null);
        if (active == null) {
            return null;
        }
        return buildSender(active.getHost(), active.getPort(), active.getSecurity(), active.getUsername(),
                cipher.decrypt(active.getPasswordEncrypted()), active.getFromAddress());
    }

    public static JavaMailSenderImpl buildSender(String host, int port, SmtpSecurity security,
                                                  String username, String password, String from) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setDefaultEncoding("UTF-8");
        if (username != null && !username.isBlank()) {
            sender.setUsername(username);
            sender.setPassword(password);
        }
        if (from != null && !from.isBlank()) {
            sender.getJavaMailProperties().put("mail.smtp.from", from);
        }
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", String.valueOf(username != null && !username.isBlank()));
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        if (security == SmtpSecurity.STARTTLS) {
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
        }
        if (security == SmtpSecurity.SSL) {
            properties.put("mail.smtp.ssl.enable", "true");
        }
        return sender;
    }

    private static String safeMessage(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return message.replaceAll("[\\r\\n]+", " ");
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record SmtpView(SmtpSettingsForm form, SmtpSettings active, List<EmailLog> recentEmails,
                           boolean passwordConfigured) {
    }

    public record TestResult(boolean success, String message) {
    }
}
