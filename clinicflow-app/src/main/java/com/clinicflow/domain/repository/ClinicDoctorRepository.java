package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.ClinicDoctor;
import com.clinicflow.domain.enums.DoctorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicDoctorRepository extends JpaRepository<ClinicDoctor, Long> {

    List<ClinicDoctor> findByClinicClinicId(Long clinicId);

    List<ClinicDoctor> findByClinicClinicIdAndStatus(Long clinicId, DoctorStatus status);

    List<ClinicDoctor> findByDoctorDoctorId(Long doctorId);

    Optional<ClinicDoctor> findByClinicClinicIdAndDoctorDoctorId(Long clinicId, Long doctorId);
}