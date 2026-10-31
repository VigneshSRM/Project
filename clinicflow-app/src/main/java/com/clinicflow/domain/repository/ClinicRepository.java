package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.Clinic;
import com.clinicflow.domain.enums.ClinicStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    List<Clinic> findByNameContainingIgnoreCaseOrCityContainingIgnoreCase(String name, String city);

    Optional<Clinic> findByPublicUuid(UUID publicUuid);

    List<Clinic> findByStatusOrderByNameAsc(ClinicStatus status);

    List<Clinic> findAllByOrderByNameAsc();

    long countByStatus(ClinicStatus status);
}