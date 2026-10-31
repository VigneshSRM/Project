package com.clinicflow.domain.entity;

import com.clinicflow.domain.enums.EmailStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "EmailLogs")
@Getter
@Setter
@NoArgsConstructor
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_log_id")
    private Long emailLogId;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 254)
    private String recipient;

    @Column(length = 300)
    private String subject;

    @Column(columnDefinition = "nvarchar(max)")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EmailStatus status = EmailStatus.QUEUED;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}