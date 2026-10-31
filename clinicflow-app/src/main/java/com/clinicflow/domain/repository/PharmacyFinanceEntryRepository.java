package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.PharmacyFinanceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PharmacyFinanceEntryRepository extends JpaRepository<PharmacyFinanceEntry, Long> {
    List<PharmacyFinanceEntry> findByPharmacyPharmacyIdOrderByEntryDateDescCreatedAtDesc(Long pharmacyId);

    @Query("select coalesce(sum(e.amount), 0) from PharmacyFinanceEntry e " +
           "where e.pharmacy.pharmacyId = :pharmacyId and e.entryType = :type " +
           "and e.entryDate between :from and :to")
    BigDecimal sumByTypeAndDateRange(@Param("pharmacyId") Long pharmacyId,
                                    @Param("type") String type,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);
}
