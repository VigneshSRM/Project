package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.ConsultationAddendum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultationAddendumRepository extends JpaRepository<ConsultationAddendum, Long> {

    List<ConsultationAddendum> findByConsultationConsultationIdOrderByCreatedAtAsc(Long consultationId);
}