package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.ClinicMedicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClinicMedicineRepository extends JpaRepository<ClinicMedicine, Long> {
    List<ClinicMedicine> findByClinicClinicIdOrderByMedicineAsc(Long clinicId);
    boolean existsByClinicClinicIdAndMedicineAndDosage(Long clinicId, String medicine, String dosage);
}
