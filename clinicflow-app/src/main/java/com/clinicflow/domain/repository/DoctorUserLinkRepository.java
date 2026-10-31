package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.DoctorUserLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorUserLinkRepository extends JpaRepository<DoctorUserLink, Long> {

    Optional<DoctorUserLink> findByUserUserId(Long userId);

    Optional<DoctorUserLink> findByDoctorDoctorId(Long doctorId);
}