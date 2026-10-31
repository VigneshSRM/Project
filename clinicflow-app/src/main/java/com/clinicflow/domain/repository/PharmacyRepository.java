package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.Pharmacy;
import com.clinicflow.domain.enums.PharmacyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {

    Optional<Pharmacy> findByPublicUuid(UUID publicUuid);

    Optional<Pharmacy> findByLicenceNumber(String licenceNumber);

    List<Pharmacy> findAllByOrderByNameAsc();

    List<Pharmacy> findByStatusOrderByNameAsc(PharmacyStatus status);

    long countByStatus(PharmacyStatus status);
}