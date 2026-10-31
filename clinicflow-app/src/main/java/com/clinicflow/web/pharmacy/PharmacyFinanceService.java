package com.clinicflow.web.pharmacy;

import com.clinicflow.domain.entity.Pharmacy;
import com.clinicflow.domain.entity.PharmacyFinanceEntry;
import com.clinicflow.domain.repository.PharmacyFinanceEntryRepository;
import com.clinicflow.domain.repository.PharmacyUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class PharmacyFinanceService {

    private final PharmacyUserRepository pharmacyUserRepository;
    private final PharmacyFinanceEntryRepository entryRepository;

    public PharmacyFinanceService(PharmacyUserRepository pharmacyUserRepository,
                                 PharmacyFinanceEntryRepository entryRepository) {
        this.pharmacyUserRepository = pharmacyUserRepository;
        this.entryRepository = entryRepository;
    }

    @Transactional(readOnly = true)
    public List<PharmacyFinanceEntry> list(Long userId) {
        return pharmacyUserRepository.findByUserUserId(userId)
                .map(link -> entryRepository.findByPharmacyPharmacyIdOrderByEntryDateDescCreatedAtDesc(
                        link.getPharmacy().getPharmacyId()))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public Pharmacy pharmacy(Long userId) {
        return pharmacyUserRepository.findByUserUserId(userId)
                .map(com.clinicflow.domain.entity.PharmacyUser::getPharmacy)
                .orElse(null);
    }

    @Transactional
    public void addEntry(Long userId, String type, BigDecimal amount, String description,
                         LocalDate entryDate, Long actorUserId) {
        addEntry(userId, type, amount, description, entryDate, actorUserId, null);
    }

    @Transactional
    public void addEntry(Long userId, String type, BigDecimal amount, String description,
                         LocalDate entryDate, Long actorUserId, Long dispenseId) {
        com.clinicflow.domain.entity.PharmacyUser link = pharmacyUserRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Pharmacy account is not linked."));
        if (!"INCOME".equals(type) && !"EXPENSE".equals(type)) {
            throw new IllegalArgumentException("Choose INCOME or EXPENSE.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Enter an amount greater than zero.");
        }
        if (entryDate == null) {
            entryDate = LocalDate.now();
        }
        PharmacyFinanceEntry entry = new PharmacyFinanceEntry();
        entry.setPharmacy(link.getPharmacy());
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setDescription(description == null || description.isBlank() ? null : description.trim());
        entry.setEntryDate(entryDate);
        entry.setCreatedBy(actorUserId);
        entry.setDispenseId(dispenseId);
        entry.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        entryRepository.save(entry);
    }

    @Transactional
    public void addDispenseIncome(Long userId, BigDecimal amount, String description,
                                  LocalDate entryDate, Long actorUserId, Long dispenseId) {
        addEntry(userId, "INCOME", amount, description, entryDate, actorUserId, dispenseId);
    }

    @Transactional(readOnly = true)
    public BigDecimal income(Long userId, LocalDate from, LocalDate to) {
        return pharmacyUserRepository.findByUserUserId(userId)
                .map(link -> entryRepository.sumByTypeAndDateRange(link.getPharmacy().getPharmacyId(),
                        "INCOME", from, to))
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public BigDecimal expense(Long userId, LocalDate from, LocalDate to) {
        return pharmacyUserRepository.findByUserUserId(userId)
                .map(link -> entryRepository.sumByTypeAndDateRange(link.getPharmacy().getPharmacyId(),
                        "EXPENSE", from, to))
                .orElse(BigDecimal.ZERO);
    }
}
