package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.PharmacyUser;
import com.clinicflow.domain.enums.PharmacyUserRole;
import com.clinicflow.domain.enums.PharmacyUserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PharmacyUserRepository extends JpaRepository<PharmacyUser, Long> {

    Optional<PharmacyUser> findByUserUserId(Long userId);

    List<PharmacyUser> findByPharmacyPharmacyId(Long pharmacyId);

    List<PharmacyUser> findByPharmacyPharmacyIdAndStatus(Long pharmacyId, PharmacyUserStatus status);

    long countByPharmacyPharmacyIdAndRoleAndStatus(Long pharmacyId, PharmacyUserRole role,
                                                   PharmacyUserStatus status);
}