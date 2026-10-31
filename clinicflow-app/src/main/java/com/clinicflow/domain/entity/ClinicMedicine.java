package com.clinicflow.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ClinicMedicines")
@Getter
@Setter
public class ClinicMedicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clinic_medicine_id")
    private Long clinicMedicineId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(name = "medicine", nullable = false, length = 200)
    private String medicine;

    @Column(name = "dosage", length = 100)
    private String dosage;

    @Column(name = "default_frequency", length = 100)
    private String defaultFrequency;

    @Column(name = "default_duration", length = 100)
    private String defaultDuration;

    @Column(name = "default_quantity")
    private Integer defaultQuantity;

    @Column(name = "default_form", length = 50)
    private String defaultForm;

    @Column(name = "default_instructions", length = 500)
    private String defaultInstructions;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
