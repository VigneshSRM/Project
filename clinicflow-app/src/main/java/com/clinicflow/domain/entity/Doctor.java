package com.clinicflow.domain.entity;

import com.clinicflow.domain.enums.DoctorStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Doctors")
@Getter
@Setter
@NoArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "public_uuid", nullable = false, unique = true)
    private UUID publicUuid = UUID.randomUUID();

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String specialization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private DoctorStatus status = DoctorStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}