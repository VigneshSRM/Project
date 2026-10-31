package com.clinicflow.domain.entity;

import com.clinicflow.domain.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ClinicEnrollments")
@Getter
@Setter
@NoArgsConstructor
public class ClinicEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    private Long enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrolled_by", nullable = false)
    private User enrolledBy;

    @Column(name = "invite_token_hash", columnDefinition = "binary(32)")
    private byte[] inviteTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EnrollmentStatus status = EnrollmentStatus.PENDING;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt = LocalDateTime.now();

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;
}