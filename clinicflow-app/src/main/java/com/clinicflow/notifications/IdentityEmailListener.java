package com.clinicflow.notifications;

import com.clinicflow.identity.PasswordResetEmailRequested;
import com.clinicflow.identity.TokenHasher;
import com.clinicflow.identity.VerificationEmailRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class IdentityEmailListener {

    private static final Logger log = LoggerFactory.getLogger(IdentityEmailListener.class);

    private final EmailOutboxService outbox;
    private final EmailTemplates templates;

    public IdentityEmailListener(EmailOutboxService outbox, EmailTemplates templates) {
        this.outbox = outbox;
        this.templates = templates;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onVerificationRequested(VerificationEmailRequested event) {
        try {
            EmailContent content = templates.verification(event.firstName(), event.rawToken());
            outbox.enqueue(EmailTypes.VERIFY_EMAIL, event.email(), idempotencyKey(EmailTypes.VERIFY_EMAIL, event),
                    content);
        } catch (RuntimeException ex) {
            // The account is still created; the patient can request a new link.
            log.error("Could not queue verification email for user {}", event.userId(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPasswordResetRequested(PasswordResetEmailRequested event) {
        try {
            EmailContent content = templates.passwordReset(event.firstName(), event.rawToken());
            outbox.enqueue(EmailTypes.RESET_PASSWORD, event.email(), idempotencyKey(EmailTypes.RESET_PASSWORD, event),
                    content);
        } catch (RuntimeException ex) {
            log.error("Could not queue password reset email for user {}", event.userId(), ex);
        }
    }

    private static String idempotencyKey(String type, VerificationEmailRequested event) {
        return type + ":" + event.userId() + ":" + TokenHasher.sha256Hex(event.rawToken());
    }

    private static String idempotencyKey(String type, PasswordResetEmailRequested event) {
        return type + ":" + event.userId() + ":" + TokenHasher.sha256Hex(event.rawToken());
    }
}
