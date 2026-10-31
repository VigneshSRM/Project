package com.clinicflow.domain.entity;

import com.clinicflow.domain.enums.LinkRequestMethod;
import com.clinicflow.domain.enums.LinkRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "PatientLinkRequests")
@Getter
@Setter
@NoArgsConstructor
public class PatientLinkRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_request_id")
    private Long linkRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_patient_record_id")
    private Patient clinicPatientRecord;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LinkRequestMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private LinkRequestStatus status = LinkRequestStatus.PENDING;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;
}