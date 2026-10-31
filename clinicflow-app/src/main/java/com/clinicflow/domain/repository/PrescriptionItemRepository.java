package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.PrescriptionItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {

    List<PrescriptionItem> findByPrescriptionPrescriptionId(Long prescriptionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PrescriptionItem i where i.prescription.prescriptionId = :prescriptionId")
    List<PrescriptionItem> findForUpdateByPrescriptionId(@Param("prescriptionId") Long prescriptionId);
}