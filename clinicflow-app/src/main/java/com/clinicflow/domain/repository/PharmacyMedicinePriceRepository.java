package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.PharmacyMedicinePrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PharmacyMedicinePriceRepository extends JpaRepository<PharmacyMedicinePrice, Long> {

    List<PharmacyMedicinePrice> findByPharmacyPharmacyIdOrderByMedicineAsc(Long pharmacyId);

    Optional<PharmacyMedicinePrice> findByPharmacyPharmacyIdAndMedicineIgnoreCase(Long pharmacyId,
                                                                                 String medicine);
}
