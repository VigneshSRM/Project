package com.clinicflow.domain.entity;

import com.clinicflow.domain.enums.VerificationOutcome;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "PharmacyVerifications")
@Getter
@Setter
@NoArgsConstructor
public class PharmacyVerification {

    @Id
    @Column(name = "verification_id", nullable = false)
    private UUID verificationId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacy_user_id", nullable = false)
    private PharmacyUser pharmacyUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Column(name = "attempt_hash", columnDefinition = "binary(32)")
    private byte[] attemptHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationOutcome outcome;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt = LocalDateTime.now();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}