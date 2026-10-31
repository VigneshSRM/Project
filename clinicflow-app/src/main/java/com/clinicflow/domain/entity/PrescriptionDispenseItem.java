package com.clinicflow.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PrescriptionDispenseItems")
@Getter
@Setter
@NoArgsConstructor
public class PrescriptionDispenseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dispense_item_id")
    private Long dispenseItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispense_id", nullable = false)
    private PrescriptionDispense dispense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_item_id", nullable = false)
    private PrescriptionItem prescriptionItem;

    @Column(name = "quantity_dispensed", nullable = false)
    private Integer quantityDispensed;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal unitPrice = java.math.BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal lineTotal = java.math.BigDecimal.ZERO;
}