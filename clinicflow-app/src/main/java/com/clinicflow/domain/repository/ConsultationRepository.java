package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.Consultation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    Optional<Consultation> findByAppointmentAppointmentId(Long appointmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Consultation c where c.consultationId = :id")
    Optional<Consultation> findForUpdate(@Param("id") Long id);

    List<Consultation> findByAppointmentPatientPatientIdOrderByCreatedAtDesc(Long patientId);

    List<Consultation> findByAppointmentPatientPatientIdAndStatusOrderByFinalisedAtDesc(
            Long patientId, com.clinicflow.domain.enums.ConsultationStatus status);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByAppointmentClinicClinicIdAndCreatedAtBetween(Long clinicId, LocalDateTime from,
                                                             LocalDateTime to);
}