package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.Prescription;
import com.clinicflow.domain.enums.PrescriptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByPublicUuid(UUID publicUuid);

    Optional<Prescription> findByPrescriptionCode(String prescriptionCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Prescription p where p.prescriptionId = :id")
    Optional<Prescription> findForUpdate(@Param("id") Long id);

    Optional<Prescription> findByConsultationConsultationId(Long consultationId);

    boolean existsByConsultationConsultationId(Long consultationId);

    List<Prescription> findByPatientPatientIdOrderByIssueDateDesc(Long patientId);

    List<Prescription> findByClinicClinicIdOrderByIssueDateDesc(Long clinicId);

    List<Prescription> findByDoctorDoctorIdOrderByIssueDateDesc(Long doctorId);

    long countByStatus(PrescriptionStatus status);

    long countByClinicClinicIdAndStatus(Long clinicId, PrescriptionStatus status);

    long countByDoctorDoctorIdAndStatus(Long doctorId, PrescriptionStatus status);

    long countByIssueDateBetween(LocalDateTime from, LocalDateTime to);
}