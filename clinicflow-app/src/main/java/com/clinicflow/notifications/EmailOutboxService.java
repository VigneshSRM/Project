package com.clinicflow.notifications;

import com.clinicflow.domain.entity.EmailLog;
import com.clinicflow.domain.enums.EmailStatus;
import com.clinicflow.domain.repository.EmailLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class EmailOutboxService {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxService.class);

    private final EmailLogRepository repository;

    public EmailOutboxService(EmailLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Queues one email. The idempotency key is unique in the database, so replayed
     * events (or a double-clicked resend) do not send twice.
     */
    // REQUIRES_NEW is deliberate: @TransactionalEventListener AFTER_COMMIT runs while
    // the original transaction is still registered as completed. Joining it would
    // leave the EntityManager without an active transaction.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueue(String type, String recipient, String idempotencyKey, EmailContent content) {
        if (repository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return;
        }
        EmailLog email = new EmailLog();
        email.setType(type);
        email.setRecipient(recipient);
        email.setSubject(content.subject());
        email.setBody(content.body());
        email.setStatus(EmailStatus.QUEUED);
        email.setAttempts(0);
        email.setIdempotencyKey(idempotencyKey);
        email.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        email.setNextAttemptAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            repository.saveAndFlush(email);
        } catch (DataIntegrityViolationException duplicate) {
            log.debug("Email already queued for idempotency key {}", idempotencyKey);
        }
    }
}
