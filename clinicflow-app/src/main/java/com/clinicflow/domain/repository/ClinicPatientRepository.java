package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.ClinicPatient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicPatientRepository extends JpaRepository<ClinicPatient, Long> {

    List<ClinicPatient> findByClinicClinicId(Long clinicId);

    List<ClinicPatient> findByPatientPatientId(Long patientId);

    boolean existsByClinicClinicIdAndPatientPatientId(Long clinicId, Long patientId);

    Optional<ClinicPatient> findByClinicClinicIdAndPatientPatientId(Long clinicId, Long patientId);
}
