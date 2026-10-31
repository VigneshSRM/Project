package com.clinicflow.domain.entity;

import com.clinicflow.domain.enums.ConsultationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Consultations")
@Getter
@Setter
@NoArgsConstructor
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consultation_id")
    private Long consultationId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(length = 2000)
    private String diagnosis;

    @Column(columnDefinition = "nvarchar(max)")
    private String notes;

    @Column(name = "follow_up")
    private LocalDateTime followUp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ConsultationStatus status = ConsultationStatus.DRAFT;

    @Column(name = "finalised_at")
    private LocalDateTime finalisedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}