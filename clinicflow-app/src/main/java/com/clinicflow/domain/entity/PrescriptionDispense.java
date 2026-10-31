package com.clinicflow.domain.entity;

import com.clinicflow.domain.enums.DispenseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "PrescriptionDispenses")
@Getter
@Setter
@NoArgsConstructor
public class PrescriptionDispense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dispense_id")
    private Long dispenseId;

    @Column(name = "public_uuid", nullable = false, unique = true)
    private UUID publicUuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacy_user_id", nullable = false)
    private PharmacyUser pharmacyUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_id", nullable = false)
    private PharmacyVerification verification;

    @Column(name = "dispensed_at", nullable = false)
    private LocalDateTime dispensedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DispenseStatus status = DispenseStatus.COMPLETED;

    @Column(length = 500)
    private String notes;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;

    @Column(name = "discount", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal discount = java.math.BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private java.math.BigDecimal taxRate = java.math.BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal taxAmount = java.math.BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal total = java.math.BigDecimal.ZERO;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "amount_tendered", precision = 12, scale = 2)
    private java.math.BigDecimal amountTendered;

    @Column(name = "change_due", precision = 12, scale = 2)
    private java.math.BigDecimal changeDue;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by")
    private PharmacyUser voidedBy;

    @Column(name = "void_reason", length = 300)
    private String voidReason;
}