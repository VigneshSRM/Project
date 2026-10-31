package com.clinicflow.notifications;

import com.clinicflow.domain.entity.EmailLog;
import com.clinicflow.domain.enums.EmailStatus;
import com.clinicflow.domain.repository.EmailLogRepository;
import com.clinicflow.web.admin.SmtpSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Transactional outbox delivery. Kept separate from the scheduled trigger so the
 * row locks and status updates always run inside a Spring transaction proxy.
 */
@Service
public class EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

    private final EmailLogRepository repository;
    private final JavaMailSender mailSender;
    private final SmtpSettingsService smtpSettingsService;
    private final String mode;
    private final String fromAddress;
    private final int maxAttempts;
    private final int batchSize;

    public EmailDeliveryService(EmailLogRepository repository,
                                JavaMailSender mailSender,
                                SmtpSettingsService smtpSettingsService,
                                @Value("${clinicflow.mail.mode:log}") String mode,
                                @Value("${clinicflow.mail.from:no-reply@clinicflow.local}") String fromAddress,
                                @Value("${clinicflow.mail.max-attempts:3}") int maxAttempts,
                                @Value("${clinicflow.mail.worker-batch:20}") int batchSize) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.smtpSettingsService = smtpSettingsService;
        this.mode = mode;
        this.fromAddress = fromAddress;
        this.maxAttempts = maxAttempts;
        this.batchSize = batchSize;
    }

    @Transactional
    public void dispatchBatch() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<EmailLog> due = repository.findDueForUpdate(
                EmailStatus.QUEUED, now, PageRequest.of(0, Math.max(1, batchSize)));
        if (due.isEmpty()) {
            return;
        }

        for (EmailLog email : due) {
            if (email.getAttempts() != null && email.getAttempts() >= maxAttempts) {
                email.setStatus(EmailStatus.FAILED);
                continue;
            }
            try {
                deliver(email);
                email.setStatus(EmailStatus.SENT);
                email.setSentAt(now);
                email.setLastError(null);
                email.setNextAttemptAt(null);
            } catch (RuntimeException ex) {
                int attempts = (email.getAttempts() == null ? 0 : email.getAttempts()) + 1;
                email.setAttempts(attempts);
                email.setLastError(truncate(ex.getMessage(), 1000));
                if (attempts >= maxAttempts) {
                    email.setStatus(EmailStatus.FAILED);
                } else {
                    email.setNextAttemptAt(now.plus(backoff(attempts)));
                }
                log.warn("Email {} to {} failed on attempt {}/{}: {}",
                        email.getType(), email.getRecipient(), attempts, maxAttempts, ex.getMessage());
            }
        }
        repository.saveAll(due);
    }

    private void deliver(EmailLog email) {
        if ("smtp".equalsIgnoreCase(mode)) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email.getRecipient());
            message.setSubject(email.getSubject());
            message.setText(email.getBody());
            JavaMailSender sender = smtpSettingsService.activeSender();
            if (sender != null) {
                sender.send(message);
            } else {
                message.setFrom(fromAddress);
                mailSender.send(message);
            }
            return;
        }

        // Local development default: no SMTP catcher required. The link is visible in
        // the application log so the registration/reset flow can be completed.
        log.info("DEV EMAIL type={} to={} subject={}\n{}",
                email.getType(), email.getRecipient(), email.getSubject(), email.getBody());
    }

    private static Duration backoff(int attempts) {
        return switch (attempts) {
            case 1 -> Duration.ofMinutes(1);
            case 2 -> Duration.ofMinutes(5);
            default -> Duration.ofMinutes(15);
        };
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
