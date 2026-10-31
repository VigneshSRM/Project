package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.SmtpSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SmtpSettingsRepository extends JpaRepository<SmtpSettings, Long> {

    Optional<SmtpSettings> findFirstByActiveTrueOrderByUpdatedAtDesc();
}