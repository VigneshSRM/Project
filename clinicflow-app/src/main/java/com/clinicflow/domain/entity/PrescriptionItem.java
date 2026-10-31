package com.clinicflow.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PrescriptionItems")
@Getter
@Setter
@NoArgsConstructor
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prescription_item_id")
    private Long prescriptionItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(nullable = false, length = 200)
    private String medicine;

    @Column(length = 100)
    private String dosage;

    @Column(length = 100)
    private String frequency;

    @Column(length = 100)
    private String duration;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "quantity_unit", length = 30)
    private String quantityUnit;

    @Column(name = "quantity_dispensed", nullable = false)
    private Integer quantityDispensed = 0;

    @Column(length = 500)
    private String instructions;

    @Column(name = "lifetime", nullable = false)
    private boolean lifetime;

    @Transient
    public int getRemainingQuantity() {
        return quantity - quantityDispensed;
    }
}