package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.PharmacyVerification;
import com.clinicflow.domain.enums.VerificationOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PharmacyVerificationRepository extends JpaRepository<PharmacyVerification, UUID> {

    List<PharmacyVerification> findByPharmacyPharmacyIdOrderByVerifiedAtDesc(Long pharmacyId);

    List<PharmacyVerification> findTop50ByPharmacyPharmacyIdOrderByVerifiedAtDesc(Long pharmacyId);

    List<PharmacyVerification> findByPrescriptionPrescriptionId(Long prescriptionId);

    long countByPharmacyPharmacyIdAndVerifiedAtBetween(Long pharmacyId, LocalDateTime from, LocalDateTime to);

    long countByPharmacyPharmacyIdAndOutcomeAndVerifiedAtBetween(
            Long pharmacyId, VerificationOutcome outcome, LocalDateTime from, LocalDateTime to);
}