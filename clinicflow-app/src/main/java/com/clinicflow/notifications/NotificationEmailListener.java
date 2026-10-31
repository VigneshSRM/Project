package com.clinicflow.notifications;

import com.clinicflow.identity.StaffInviteRequested;
import com.clinicflow.identity.EmailChangeRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEmailListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEmailListener.class);

    private final EmailOutboxService outbox;
    private final EmailTemplates templates;

    public NotificationEmailListener(EmailOutboxService outbox, EmailTemplates templates) {
        this.outbox = outbox;
        this.templates = templates;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStaffInvite(StaffInviteRequested event) {
        try {
            outbox.enqueue(EmailTypes.STAFF_INVITE, event.email(),
                    EmailTypes.STAFF_INVITE + ":" + event.userId() + ":" + event.rawToken().hashCode(),
                    templates.staffInvite(event.name(), event.organization(), event.roleLabel(),
                            event.rawToken()));
        } catch (RuntimeException ex) {
            log.error("Could not queue staff invitation for user {}", event.userId(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEmailChange(EmailChangeRequested event) {
        try {
            outbox.enqueue(EmailTypes.EMAIL_CHANGE, event.newEmail(),
                    EmailTypes.EMAIL_CHANGE + ":" + event.userId() + ":" + event.rawToken().hashCode(),
                    templates.emailChange(event.firstName(), event.newEmail(), event.rawToken()));
        } catch (RuntimeException ex) {
            log.error("Could not queue email-change verification for user {}", event.userId(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAppointmentBooked(AppointmentBookedEvent event) {
        if (event.patientEmail() == null || event.patientEmail().isBlank()) {
            return;
        }
        try {
            outbox.enqueue(EmailTypes.APPOINTMENT_CONFIRMATION, event.patientEmail(),
                    EmailTypes.APPOINTMENT_CONFIRMATION + ":" + event.appointmentId() + ":"
                            + event.when(),
                    templates.appointmentConfirmation(event.patientName(), event.clinicName(),
                            event.doctorName(), event.when(), event.reason(), event.appointmentPublicId()));
        } catch (RuntimeException ex) {
            log.error("Could not queue appointment confirmation for {}", event.appointmentId(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAppointmentCancelled(AppointmentCancelledEvent event) {
        if (event.patientEmail() == null || event.patientEmail().isBlank()) {
            return;
        }
        try {
            outbox.enqueue(EmailTypes.APPOINTMENT_CANCELLED, event.patientEmail(),
                    EmailTypes.APPOINTMENT_CANCELLED + ":" + event.appointmentId() + ":" + event.appointmentPublicId() + ":cancelled",
                    templates.appointmentCancelled(event.patientName(), event.clinicName(),
                            event.doctorName(), event.when(), event.appointmentPublicId(), event.reason()));
        } catch (RuntimeException ex) {
            log.error("Could not queue appointment cancellation for {}", event.appointmentId(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPrescriptionIssued(PrescriptionIssuedEvent event) {
        if (event.patientEmail() == null || event.patientEmail().isBlank()) {
            return;
        }
        try {
            outbox.enqueue(EmailTypes.PRESCRIPTION_ISSUED, event.patientEmail(),
                    EmailTypes.PRESCRIPTION_ISSUED + ":" + event.prescriptionId() + ":"
                            + (event.suffix() == null ? "issued" : event.suffix()),
                    templates.prescriptionIssued(event.patientName(), event.prescriptionCode(),
                            event.clinicName(), event.doctorName(), event.issueDate(), event.expiryDate()));
        } catch (RuntimeException ex) {
            log.error("Could not queue prescription email for {}", event.prescriptionId(), ex);
        }
    }
}
