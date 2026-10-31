package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.ClinicEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicEnrollmentRepository extends JpaRepository<ClinicEnrollment, Long> {

    List<ClinicEnrollment> findByClinicClinicIdOrderByEnrolledAtDesc(Long clinicId);

    Optional<ClinicEnrollment> findFirstByClinicClinicIdOrderByEnrolledAtDesc(Long clinicId);
}