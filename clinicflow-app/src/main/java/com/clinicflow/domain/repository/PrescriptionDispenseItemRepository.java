package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.PrescriptionDispenseItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionDispenseItemRepository extends JpaRepository<PrescriptionDispenseItem, Long> {

    List<PrescriptionDispenseItem> findByDispenseDispenseId(Long dispenseId);

    List<PrescriptionDispenseItem> findByDispensePrescriptionPrescriptionId(Long prescriptionId);

    long countByDispensePrescriptionPrescriptionId(Long prescriptionId);
}