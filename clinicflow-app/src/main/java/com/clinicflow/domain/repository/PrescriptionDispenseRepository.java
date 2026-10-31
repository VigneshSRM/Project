package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.PrescriptionDispense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PrescriptionDispenseRepository extends JpaRepository<PrescriptionDispense, Long> {

    List<PrescriptionDispense> findByPrescriptionPrescriptionId(Long prescriptionId);

    List<PrescriptionDispense> findByPharmacyPharmacyIdOrderByDispensedAtDesc(Long pharmacyId);

    List<PrescriptionDispense> findTop5ByPharmacyPharmacyIdOrderByDispensedAtDesc(Long pharmacyId);

    java.util.Optional<PrescriptionDispense> findByPublicUuid(UUID publicUuid);

    long countByPharmacyPharmacyIdAndDispensedAtBetween(Long pharmacyId,
                                                        java.time.LocalDateTime from,
                                                        java.time.LocalDateTime to);

    @org.springframework.data.jpa.repository.Query(
            "select coalesce(sum(d.total), 0) from PrescriptionDispense d "
                    + "where d.pharmacy.pharmacyId = :pharmacyId and d.dispensedAt between :from and :to")
    java.math.BigDecimal sumTotalByPharmacyAndDateRange(
            @org.springframework.data.repository.query.Param("pharmacyId") Long pharmacyId,
            @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to);

    long countByPrescriptionPrescriptionIdAndStatus(Long prescriptionId,
                                                    com.clinicflow.domain.enums.DispenseStatus status);

    boolean existsByVerificationVerificationId(UUID verificationId);
}