package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.PatientLinkRequest;
import com.clinicflow.domain.enums.LinkRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientLinkRequestRepository extends JpaRepository<PatientLinkRequest, Long> {

    List<PatientLinkRequest> findByPatientPatientIdOrderByCreatedAtDesc(Long patientId);

    List<PatientLinkRequest> findByClinicClinicIdOrderByCreatedAtDesc(Long clinicId);

    List<PatientLinkRequest> findByClinicClinicIdAndStatusOrderByCreatedAtDesc(Long clinicId,
                                                                              LinkRequestStatus status);

    boolean existsByPatientPatientIdAndClinicClinicIdAndStatus(Long patientId, Long clinicId,
                                                               LinkRequestStatus status);

    Optional<PatientLinkRequest> findFirstByPatientPatientIdAndClinicClinicIdAndStatusOrderByCreatedAtDesc(
            Long patientId, Long clinicId, LinkRequestStatus status);

    long countByClinicClinicIdAndStatus(Long clinicId, LinkRequestStatus status);

    long countByStatus(LinkRequestStatus status);
}
