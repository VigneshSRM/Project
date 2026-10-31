package com.clinicflow.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler trigger only. Delivery logic lives in EmailDeliveryService so the
 * transactional proxy is applied on every run.
 */
@Component
public class EmailDispatchWorker {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatchWorker.class);

    private final EmailDeliveryService deliveryService;

    public EmailDispatchWorker(EmailDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @Scheduled(fixedDelayString = "${clinicflow.mail.worker-delay-ms:5000}",
            initialDelayString = "${clinicflow.mail.worker-initial-delay-ms:3000}")
    public void dispatch() {
        try {
            deliveryService.dispatchBatch();
        } catch (RuntimeException ex) {
            // A transient database/SMTP problem must not kill the scheduler.
            log.error("Email outbox dispatch failed", ex);
        }
    }
}
