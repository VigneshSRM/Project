package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(Role role);

    long countByStatus(com.clinicflow.domain.enums.UserStatus status);

    List<User> findByClinicClinicIdOrderByCreatedAtDesc(Long clinicId);

    List<User> findByRoleOrderByCreatedAtDesc(Role role);

    Optional<User> findByPendingEmailIgnoreCase(String pendingEmail);
}