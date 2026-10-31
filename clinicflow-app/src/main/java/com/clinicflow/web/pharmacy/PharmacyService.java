package com.clinicflow.web.pharmacy;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.Pharmacy;
import com.clinicflow.domain.entity.PharmacyMedicinePrice;
import com.clinicflow.domain.entity.PharmacyUser;
import com.clinicflow.domain.entity.PharmacyVerification;
import com.clinicflow.domain.entity.Prescription;
import com.clinicflow.domain.entity.PrescriptionDispense;
import com.clinicflow.domain.entity.PrescriptionDispenseItem;
import com.clinicflow.domain.entity.PrescriptionItem;
import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.DispenseStatus;
import com.clinicflow.domain.enums.PharmacyStatus;
import com.clinicflow.domain.enums.PharmacyUserRole;
import com.clinicflow.domain.enums.PharmacyUserStatus;
import com.clinicflow.domain.enums.PrescriptionStatus;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.enums.UserStatus;
import com.clinicflow.domain.enums.VerificationOutcome;
import com.clinicflow.domain.repository.PharmacyUserRepository;
import com.clinicflow.domain.repository.PharmacyMedicinePriceRepository;
import com.clinicflow.domain.repository.PharmacyVerificationRepository;
import com.clinicflow.domain.repository.PrescriptionDispenseItemRepository;
import com.clinicflow.domain.repository.PrescriptionDispenseRepository;
import com.clinicflow.domain.repository.PrescriptionItemRepository;
import com.clinicflow.domain.repository.PrescriptionRepository;
import com.clinicflow.domain.repository.UserRepository;
import com.clinicflow.identity.InvitationService;
import com.clinicflow.web.prescription.PrescriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PharmacyService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM");

    private final PharmacyUserRepository pharmacyUserRepository;
    private final PharmacyVerificationRepository verificationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository itemRepository;
    private final PrescriptionDispenseRepository dispenseRepository;
    private final PrescriptionDispenseItemRepository dispenseItemRepository;
    private final PharmacyMedicinePriceRepository priceRepository;
    private final PharmacyFinanceService financeService;
    private final UserRepository userRepository;
    private final PrescriptionService prescriptionService;
    private final InvitationService invitationService;
    private final AuditService auditService;

    public PharmacyService(PharmacyUserRepository pharmacyUserRepository,
                           PharmacyVerificationRepository verificationRepository,
                           PrescriptionRepository prescriptionRepository,
                           PrescriptionItemRepository itemRepository,
                           PrescriptionDispenseRepository dispenseRepository,
                           PrescriptionDispenseItemRepository dispenseItemRepository,
                           PharmacyMedicinePriceRepository priceRepository,
                           PharmacyFinanceService financeService,
                           UserRepository userRepository,
                           PrescriptionService prescriptionService,
                           InvitationService invitationService,
                           AuditService auditService) {
        this.pharmacyUserRepository = pharmacyUserRepository;
        this.verificationRepository = verificationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.itemRepository = itemRepository;
        this.dispenseRepository = dispenseRepository;
        this.dispenseItemRepository = dispenseItemRepository;
        this.priceRepository = priceRepository;
        this.financeService = financeService;
        this.userRepository = userRepository;
        this.prescriptionService = prescriptionService;
        this.invitationService = invitationService;
        this.auditService = auditService;
    }

    @Transactional
    public VerificationResult verify(Long userId, String rawCode, String ipAddress, Long actorUserId) {
        PharmacyUser pharmacyUser = requireActiveUser(userId);
        String code = rawCode == null ? "" : rawCode.trim();
        Prescription prescription = prescriptionRepository.findByPrescriptionCode(code)
                .filter(found -> found.getPrescriptionCode().equals(code))
                .orElse(null);
        VerificationOutcome outcome;
        String message;
        boolean canDispense = false;
        PrescriptionService.PrescriptionView view = null;

        if (prescription == null) {
            outcome = VerificationOutcome.NOT_FOUND;
            message = "No prescription matches that ID exactly. Prescription IDs are case-sensitive; "
                    + "check every character and try again.";
        } else {
            view = prescriptionService.viewForPharmacy(prescription.getPrescriptionId());
            int remaining = remainingUnits(prescription.getPrescriptionId());
            if (prescription.getStatus() == PrescriptionStatus.CANCELLED) {
                outcome = VerificationOutcome.CANCELLED;
                message = "This prescription was cancelled by the clinic and cannot be dispensed.";
            } else if (prescription.getExpiryDate().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
                outcome = VerificationOutcome.EXPIRED;
                message = "This prescription expired on " + view.expires()
                        + " and cannot be dispensed.";
            } else if (remaining <= 0) {
                outcome = VerificationOutcome.FULLY_DISPENSED;
                message = "This prescription has already been fully dispensed.";
            } else {
                outcome = VerificationOutcome.VALID;
                canDispense = true;
                message = "Prescription verified. It is valid and has " + remaining
                        + " unit(s) still to dispense.";
            }
        }

        PharmacyVerification verification = new PharmacyVerification();
        verification.setPharmacy(pharmacyUser.getPharmacy());
        verification.setPharmacyUser(pharmacyUser);
        verification.setPrescription(prescription);
        verification.setAttemptHash(sha256(code));
        verification.setOutcome(outcome);
        verification.setVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));
        verification.setIpAddress(ipAddress == null ? null
                : ipAddress.substring(0, Math.min(45, ipAddress.length())));
        verification = verificationRepository.save(verification);

        auditService.record(actorUserId, Role.PHARMACY_ADMIN.equals(pharmacyUser.getUser().getRole())
                        ? Role.PHARMACY_ADMIN : Role.PHARMACIST,
                outcome == VerificationOutcome.VALID ? "PRESCRIPTION_VERIFIED"
                        : "PRESCRIPTION_VERIFICATION_FAILED",
                "Prescription", prescription == null ? hashPrefix(verification.getAttemptHash())
                        : prescription.getPrescriptionCode(),
                prescription == null ? null : prescription.getPatient().getPatientId(),
                outcome == VerificationOutcome.VALID ? AuditOutcome.SUCCESS : AuditOutcome.DENIED);
        return new VerificationResult(verification.getVerificationId(), outcome, message, canDispense,
                view);
    }

    @Transactional
    public DispenseResult dispense(Long userId, UUID verificationId, Map<Long, Integer> requested,
                                   Map<Long, BigDecimal> prices, BigDecimal discount, BigDecimal taxRate,
                                   String paymentMethod, BigDecimal amountTendered,
                                   String notes, Long actorUserId) {
        PharmacyUser pharmacyUser = requireActiveUser(userId);
        PharmacyVerification verification = verificationRepository.findById(verificationId)
                .filter(item -> item.getPharmacy().getPharmacyId()
                        .equals(pharmacyUser.getPharmacy().getPharmacyId()))
                .filter(item -> item.getPharmacyUser().getPharmacyUserId()
                        .equals(pharmacyUser.getPharmacyUserId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "This verification is not valid for your pharmacy. Verify the prescription again."));
        if (verification.getOutcome() != VerificationOutcome.VALID
                || verification.getPrescription() == null) {
            throw new IllegalArgumentException("Only a successful verification can be dispensed.");
        }
        if (dispenseRepository.existsByVerificationVerificationId(verificationId)) {
            throw new IllegalArgumentException(
                    "This verification has already been used. Verify the prescription again for another fill.");
        }
        Prescription prescription = prescriptionRepository
                .findForUpdate(verification.getPrescription().getPrescriptionId())
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found."));
        if (prescription.getStatus() == PrescriptionStatus.CANCELLED) {
            throw new IllegalArgumentException("This prescription is cancelled and cannot be dispensed.");
        }
        if (prescription.getExpiryDate().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException("This prescription has expired and cannot be dispensed.");
        }

        List<PrescriptionItem> items = itemRepository
                .findForUpdateByPrescriptionId(prescription.getPrescriptionId());
        Map<Long, Integer> accepted = new LinkedHashMap<>();
        int units = 0;
        for (PrescriptionItem item : items) {
            int quantity = requested.getOrDefault(item.getPrescriptionItemId(), 0);
            if (quantity < 0) {
                throw new IllegalArgumentException("Dispensed quantities cannot be negative.");
            }
            if (quantity > item.getRemainingQuantity()) {
                throw new IllegalArgumentException("Cannot dispense " + quantity + " of "
                        + item.getMedicine() + "; only " + item.getRemainingQuantity()
                        + " remain.");
            }
            if (quantity > 0) {
                accepted.put(item.getPrescriptionItemId(), quantity);
                units += quantity;
            }
        }
        if (accepted.isEmpty()) {
            throw new IllegalArgumentException(
                    "Enter at least one quantity greater than zero to record a dispense.");
        }

        Long pharmacyId = pharmacyUser.getPharmacy().getPharmacyId();
        Map<String, BigDecimal> remembered = new LinkedHashMap<>();
        for (PharmacyMedicinePrice price : priceRepository
                .findByPharmacyPharmacyIdOrderByMedicineAsc(pharmacyId)) {
            remembered.put(price.getMedicine().toLowerCase(java.util.Locale.ROOT),
                    price.getUnitPrice());
        }

        Map<Long, BigDecimal> unitPrices = new LinkedHashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (PrescriptionItem item : items) {
            Integer quantity = accepted.get(item.getPrescriptionItemId());
            if (quantity == null) {
                continue;
            }
            BigDecimal unitPrice = prices == null ? null
                    : prices.get(item.getPrescriptionItemId());
            if (unitPrice == null) {
                unitPrice = remembered.getOrDefault(
                        item.getMedicine().toLowerCase(java.util.Locale.ROOT), BigDecimal.ZERO);
            }
            unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
            if (unitPrice.signum() < 0) {
                throw new IllegalArgumentException("Unit prices cannot be negative.");
            }
            unitPrices.put(item.getPrescriptionItemId(), unitPrice);
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal safeDiscount = discount == null ? BigDecimal.ZERO
                : discount.setScale(2, RoundingMode.HALF_UP);
        if (safeDiscount.signum() < 0) {
            throw new IllegalArgumentException("Discount cannot be negative.");
        }
        if (safeDiscount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("Discount cannot be greater than the subtotal.");
        }
        BigDecimal safeTaxRate = taxRate == null ? BigDecimal.ZERO
                : taxRate.setScale(2, RoundingMode.HALF_UP);
        if (safeTaxRate.signum() < 0 || safeTaxRate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Tax rate must be between 0 and 100.");
        }
        BigDecimal taxable = subtotal.subtract(safeDiscount);
        BigDecimal taxAmount = taxable.multiply(safeTaxRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal total = taxable.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        String method = normalizePaymentMethod(paymentMethod);
        BigDecimal tendered = amountTendered == null ? null
                : amountTendered.setScale(2, RoundingMode.HALF_UP);
        if (tendered != null && tendered.signum() < 0) {
            throw new IllegalArgumentException("Amount tendered cannot be negative.");
        }
        if (tendered != null && tendered.compareTo(total) < 0) {
            throw new IllegalArgumentException("Amount tendered is less than the sale total.");
        }
        BigDecimal change = tendered == null ? null
                : tendered.subtract(total).setScale(2, RoundingMode.HALF_UP);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        PrescriptionDispense dispense = new PrescriptionDispense();
        dispense.setPrescription(prescription);
        dispense.setPharmacy(pharmacyUser.getPharmacy());
        dispense.setPharmacyUser(pharmacyUser);
        dispense.setVerification(verification);
        dispense.setDispensedAt(now);
        dispense.setStatus(DispenseStatus.COMPLETED);
        dispense.setNotes(notes == null || notes.isBlank() ? null
                : notes.substring(0, Math.min(500, notes.trim().length())));
        dispense.setSubtotal(subtotal);
        dispense.setDiscount(safeDiscount);
        dispense.setTaxRate(safeTaxRate);
        dispense.setTaxAmount(taxAmount);
        dispense.setTotal(total);
        dispense.setPaymentMethod(method);
        dispense.setAmountTendered(tendered);
        dispense.setChangeDue(change);
        dispense = dispenseRepository.save(dispense);

        List<DispenseLine> lines = new ArrayList<>();
        for (PrescriptionItem item : items) {
            Integer quantity = accepted.get(item.getPrescriptionItemId());
            if (quantity == null) {
                continue;
            }
            BigDecimal unitPrice = unitPrices.get(item.getPrescriptionItemId());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);
            PrescriptionDispenseItem line = new PrescriptionDispenseItem();
            line.setDispense(dispense);
            line.setPrescriptionItem(item);
            line.setQuantityDispensed(quantity);
            line.setUnitPrice(unitPrice);
            line.setLineTotal(lineTotal);
            dispenseItemRepository.save(line);
            item.setQuantityDispensed(item.getQuantityDispensed() + quantity);
            itemRepository.save(item);
            lines.add(new DispenseLine(item.getMedicine(), quantity, unitPrice, lineTotal));
        }

        for (PrescriptionItem item : items) {
            Integer quantity = accepted.get(item.getPrescriptionItemId());
            if (quantity == null) {
                continue;
            }
            BigDecimal unitPrice = unitPrices.get(item.getPrescriptionItemId());
            PharmacyMedicinePrice price = priceRepository
                    .findByPharmacyPharmacyIdAndMedicineIgnoreCase(pharmacyId, item.getMedicine())
                    .orElseGet(() -> {
                        PharmacyMedicinePrice created = new PharmacyMedicinePrice();
                        created.setPharmacy(pharmacyUser.getPharmacy());
                        created.setMedicine(item.getMedicine());
                        return created;
                    });
            price.setUnitPrice(unitPrice);
            price.setUpdatedAt(now);
            priceRepository.save(price);
        }

        if (total.signum() > 0) {
            financeService.addDispenseIncome(userId, total,
                    "POS sale " + prescription.getPrescriptionCode() + " · " + method,
                    LocalDate.now(), actorUserId, dispense.getDispenseId());
        }

        auditService.record(actorUserId, pharmacyUser.getUser().getRole(), "PRESCRIPTION_DISPENSED",
                "Prescription", prescription.getPrescriptionCode(), prescription.getPatient().getPatientId(),
                AuditOutcome.SUCCESS);
        return new DispenseResult(dispense.getDispenseId(), DATE_TIME.format(now),
                pharmacyUser.getPharmacy().getName(), prescription.getPrescriptionCode(),
                prescription.getPatient().getDisplayName(), units, lines,
                remainingUnits(prescription.getPrescriptionId()) <= 0,
                subtotal, safeDiscount, safeTaxRate, taxAmount, total, method, tendered, change);
    }

    @Transactional(readOnly = true)
    public PharmacyUser requireUser(Long userId) {
        PharmacyUser pharmacyUser = pharmacyUserRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Your pharmacy account is not linked. Ask the platform administrator."));
        // The controller renders pharmacy details after this transaction closes.
        pharmacyUser.getPharmacy().getName();
        return pharmacyUser;
    }

    @Transactional(readOnly = true)
    public VerificationResult verification(Long userId, UUID verificationId) {
        PharmacyUser pharmacyUser = requireActiveUser(userId);
        PharmacyVerification verification = verificationRepository.findById(verificationId)
                .filter(item -> item.getPharmacy().getPharmacyId()
                        .equals(pharmacyUser.getPharmacy().getPharmacyId()))
                .orElseThrow(() -> new IllegalArgumentException("Verification not found."));
        Prescription p = verification.getPrescription();
        PrescriptionService.PrescriptionView view = p == null ? null
                : prescriptionService.viewForPharmacy(p.getPrescriptionId());
        boolean canDispense = verification.getOutcome() == VerificationOutcome.VALID
                && p != null
                && !dispenseRepository.existsByVerificationVerificationId(verificationId)
                && p.getStatus() == PrescriptionStatus.VALID
                && !p.getExpiryDate().isBefore(LocalDateTime.now(ZoneOffset.UTC))
                && remainingUnits(p.getPrescriptionId()) > 0;
        String message = switch (verification.getOutcome()) {
            case VALID -> canDispense ? "Prescription verified. Enter the quantities to dispense."
                    : "This verification can no longer be used. Verify the prescription again.";
            case EXPIRED -> "This prescription has expired and cannot be dispensed.";
            case CANCELLED -> "This prescription was cancelled by the clinic and cannot be dispensed.";
            case FULLY_DISPENSED -> "This prescription has already been fully dispensed.";
            case NOT_FOUND -> "No prescription matches that ID exactly.";
            case RATE_LIMITED -> "Too many attempts. Wait a moment and try again.";
        };
        return new VerificationResult(verificationId, verification.getOutcome(), message, canDispense, view);
    }

    @Transactional(readOnly = true)
    public DispenseResult dispenseDetail(Long userId, Long dispenseId) {
        PharmacyUser pharmacyUser = requireActiveUser(userId);
        PrescriptionDispense dispense = dispenseRepository.findById(dispenseId)
                .filter(item -> item.getPharmacy().getPharmacyId()
                        .equals(pharmacyUser.getPharmacy().getPharmacyId()))
                .orElseThrow(() -> new IllegalArgumentException("Dispense record not found."));
        List<DispenseLine> lines = dispenseItemRepository
                .findByDispenseDispenseId(dispense.getDispenseId()).stream()
                .map(item -> new DispenseLine(item.getPrescriptionItem().getMedicine(),
                        item.getQuantityDispensed(), item.getUnitPrice(), item.getLineTotal()))
                .toList();
        int units = lines.stream().mapToInt(DispenseLine::quantity).sum();
        return new DispenseResult(dispense.getDispenseId(),
                DATE_TIME.format(dispense.getDispensedAt()), dispense.getPharmacy().getName(),
                dispense.getPrescription().getPrescriptionCode(),
                dispense.getPrescription().getPatient().getDisplayName(), units, lines,
                remainingUnits(dispense.getPrescription().getPrescriptionId()) <= 0,
                dispense.getSubtotal(), dispense.getDiscount(), dispense.getTaxRate(),
                dispense.getTaxAmount(), dispense.getTotal(), dispense.getPaymentMethod(),
                dispense.getAmountTendered(), dispense.getChangeDue());
    }

    @Transactional(readOnly = true)
    public List<UserRow> users(Long userId) {
        PharmacyUser current = requireUser(userId);
        boolean canManage = current.getRole() == PharmacyUserRole.PHARMACY_ADMIN
                && current.getStatus() == PharmacyUserStatus.ACTIVE;
        return pharmacyUserRepository.findByPharmacyPharmacyId(current.getPharmacy().getPharmacyId())
                .stream()
                .map(link -> new UserRow(link.getPharmacyUserId(), link.getUser().getUserId(),
                        link.getUser().getEmail(),
                        link.getRole() == PharmacyUserRole.PHARMACY_ADMIN
                                ? "Pharmacy administrator" : "Pharmacist",
                        link.getStatus().name(), link.getUser().getStatus().name(),
                        DATE_TIME.format(link.getCreatedAt()),
                        link.getPharmacyUserId().equals(current.getPharmacyUserId()), canManage))
                .toList();
    }

    @Transactional
    public void invitePharmacist(Long userId, String email, Long actorUserId) {
        PharmacyUser current = requireAdmin(userId);
        invitationService.invitePharmacist(email, current.getPharmacy(),
                current.getPharmacy().getName(), actorUserId);
        auditService.record(actorUserId, Role.PHARMACY_ADMIN, "PHARMACIST_INVITED", "Pharmacy",
                current.getPharmacy().getPublicUuid().toString(), null, AuditOutcome.SUCCESS);
    }

    @Transactional
    public void resendInvitation(Long userId, Long pharmacyUserId, Long actorUserId) {
        PharmacyUser current = requireAdmin(userId);
        PharmacyUser target = requireSamePharmacy(current, pharmacyUserId);
        if (target.getUser().getStatus() != UserStatus.PENDING) {
            throw new IllegalArgumentException("Only a pending invitation can be resent.");
        }
        invitationService.resend(target.getUser(), current.getPharmacy().getName(), "Pharmacist",
                actorUserId);
    }

    @Transactional
    public void setUserEnabled(Long userId, Long pharmacyUserId, boolean enabled, Long actorUserId) {
        PharmacyUser current = requireAdmin(userId);
        PharmacyUser target = requireSamePharmacy(current, pharmacyUserId);
        if (target.getPharmacyUserId().equals(current.getPharmacyUserId()) && !enabled) {
            throw new IllegalArgumentException("You cannot disable your own account.");
        }
        if (enabled && target.getUser().getStatus() == UserStatus.PENDING) {
            throw new IllegalArgumentException(
                    "This invitation has not been accepted yet. Resend the invitation instead.");
        }
        if (!enabled && target.getRole() == PharmacyUserRole.PHARMACY_ADMIN
                && target.getStatus() != PharmacyUserStatus.DISABLED) {
            long activeAdmins = pharmacyUserRepository
                    .countByPharmacyPharmacyIdAndRoleAndStatus(
                            current.getPharmacy().getPharmacyId(), PharmacyUserRole.PHARMACY_ADMIN,
                            PharmacyUserStatus.ACTIVE);
            if (activeAdmins <= 1) {
                throw new IllegalArgumentException(
                        "At least one active pharmacy administrator must remain.");
            }
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        target.setStatus(enabled ? PharmacyUserStatus.ACTIVE : PharmacyUserStatus.DISABLED);
        target.getUser().setStatus(enabled ? UserStatus.ACTIVE : UserStatus.DISABLED);
        target.getUser().setUpdatedAt(now);
        userRepository.save(target.getUser());
        pharmacyUserRepository.save(target);
        auditService.record(actorUserId, Role.PHARMACY_ADMIN,
                enabled ? "PHARMACY_USER_ENABLED" : "PHARMACY_USER_DISABLED",
                "PharmacyUser", target.getUser().getPublicUuid().toString(), null, AuditOutcome.SUCCESS);
    }

    @Transactional(readOnly = true)
    public PharmacyReport report(Long userId) {
        PharmacyUser current = requireUser(userId);
        Pharmacy pharmacy = current.getPharmacy();
        Long pharmacyId = pharmacy.getPharmacyId();
        LocalDate today = LocalDate.now();
        List<DailyActivity> daily = new ArrayList<>();
        LocalDate first = today.minusDays(6);
        long max = 1;
        for (LocalDate day = first; !day.isAfter(today); day = day.plusDays(1)) {
            LocalDateTime from = startOfDayUtc(day);
            LocalDateTime to = startOfDayUtc(day.plusDays(1));
            long verifications = verificationRepository
                    .countByPharmacyPharmacyIdAndVerifiedAtBetween(pharmacyId, from, to);
            long dispenses = dispenseRepository
                    .countByPharmacyPharmacyIdAndDispensedAtBetween(pharmacyId, from, to);
            max = Math.max(max, verifications);
            daily.add(new DailyActivity(DAY.format(day), verifications, dispenses));
        }
        final long maxValue = max;
        List<DailyActivity> scaled = daily.stream()
                .map(day -> new DailyActivity(day.day(), day.verifications(), day.dispenses(),
                        Math.max(4, Math.round(day.verifications() * 100f / maxValue))))
                .toList();

        LocalDateTime start = startOfDayUtc(today);
        LocalDateTime end = startOfDayUtc(today.plusDays(1));
        long verificationsToday = verificationRepository
                .countByPharmacyPharmacyIdAndVerifiedAtBetween(pharmacyId, start, end);
        long validToday = verificationRepository
                .countByPharmacyPharmacyIdAndOutcomeAndVerifiedAtBetween(
                        pharmacyId, VerificationOutcome.VALID, start, end);
        long notFoundToday = verificationRepository
                .countByPharmacyPharmacyIdAndOutcomeAndVerifiedAtBetween(
                        pharmacyId, VerificationOutcome.NOT_FOUND, start, end);
        List<PrescriptionDispense> todayDispenses = dispenseRepository
                .findByPharmacyPharmacyIdOrderByDispensedAtDesc(pharmacyId).stream()
                .filter(dispense -> !dispense.getDispensedAt().isBefore(start)
                        && dispense.getDispensedAt().isBefore(end))
                .toList();
        int unitsToday = todayDispenses.stream()
                .flatMap(dispense -> dispenseItemRepository
                        .findByDispenseDispenseId(dispense.getDispenseId()).stream())
                .mapToInt(PrescriptionDispenseItem::getQuantityDispensed).sum();

        List<VerificationRow> verifications = verificationRepository
                .findTop50ByPharmacyPharmacyIdOrderByVerifiedAtDesc(pharmacyId).stream()
                .map(verification -> new VerificationRow(DATE_TIME.format(verification.getVerifiedAt()),
                        verification.getPrescription() == null ? "—"
                                : verification.getPrescription().getPrescriptionCode(),
                        verification.getPrescription() == null ? "—"
                                : verification.getPrescription().getPatient().getDisplayName(),
                        verification.getPharmacyUser().getUser().getEmail(),
                        verification.getOutcome().name(), tone(verification.getOutcome())))
                .toList();

        List<DispenseRow> dispenses = dispenseRepository
                .findByPharmacyPharmacyIdOrderByDispensedAtDesc(pharmacyId).stream().limit(25)
                .map(dispense -> {
                    int units = dispenseItemRepository
                            .findByDispenseDispenseId(dispense.getDispenseId()).stream()
                            .mapToInt(PrescriptionDispenseItem::getQuantityDispensed).sum();
                    return new DispenseRow(DATE_TIME.format(dispense.getDispensedAt()),
                            dispense.getPrescription().getPrescriptionCode(),
                            dispense.getPrescription().getPatient().getDisplayName(),
                            dispense.getPharmacyUser().getUser().getEmail(), units,
                            dispense.getStatus().name());
                }).toList();

        return new PharmacyReport(pharmacy.getName(), pharmacy.getStatus() == PharmacyStatus.ACTIVE,
                verificationsToday, validToday, notFoundToday, todayDispenses.size(), unitsToday,
                scaled, verifications, dispenses);
    }

    @Transactional(readOnly = true)
    public List<PriceRow> priceRows(Long userId) {
        PharmacyUser user = requireUser(userId);
        return priceRepository.findByPharmacyPharmacyIdOrderByMedicineAsc(
                        user.getPharmacy().getPharmacyId()).stream()
                .map(price -> new PriceRow(price.getPriceId(), price.getMedicine(),
                        price.getUnitPrice(), DATE_TIME.format(price.getUpdatedAt())))
                .toList();
    }

    @Transactional
    public void savePrice(Long userId, Long priceId, String medicine, BigDecimal unitPrice,
                          Long actorUserId) {
        PharmacyUser user = requireActiveUser(userId);
        Long pharmacyId = user.getPharmacy().getPharmacyId();
        if (medicine == null || medicine.isBlank()) {
            throw new IllegalArgumentException("Enter the medicine name.");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Enter a unit price of zero or more.");
        }
        String cleanMedicine = medicine.trim();
        PharmacyMedicinePrice price = null;
        if (priceId != null) {
            price = priceRepository.findById(priceId)
                    .filter(item -> item.getPharmacy().getPharmacyId().equals(pharmacyId))
                    .orElseThrow(() -> new IllegalArgumentException("Price entry not found."));
        }
        PharmacyMedicinePrice existing = priceRepository
                .findByPharmacyPharmacyIdAndMedicineIgnoreCase(pharmacyId, cleanMedicine)
                .orElse(null);
        if (price == null) {
            price = existing == null ? new PharmacyMedicinePrice() : existing;
            if (price.getPriceId() == null) {
                price.setPharmacy(user.getPharmacy());
            }
        } else if (existing != null && !existing.getPriceId().equals(price.getPriceId())) {
            throw new IllegalArgumentException("A price for that medicine already exists.");
        }
        price.setMedicine(cleanMedicine);
        price.setUnitPrice(unitPrice.setScale(2, RoundingMode.HALF_UP));
        price.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        priceRepository.save(price);
        auditService.record(actorUserId, user.getUser().getRole(), "PHARMACY_PRICE_SAVED",
                "PharmacyMedicinePrice", cleanMedicine, null, AuditOutcome.SUCCESS);
    }

    @Transactional
    public void deletePrice(Long userId, Long priceId, Long actorUserId) {
        PharmacyUser user = requireActiveUser(userId);
        PharmacyMedicinePrice price = priceRepository.findById(priceId)
                .filter(item -> item.getPharmacy().getPharmacyId()
                        .equals(user.getPharmacy().getPharmacyId()))
                .orElseThrow(() -> new IllegalArgumentException("Price entry not found."));
        priceRepository.delete(price);
        auditService.record(actorUserId, user.getUser().getRole(), "PHARMACY_PRICE_DELETED",
                "PharmacyMedicinePrice", price.getMedicine(), null, AuditOutcome.SUCCESS);
    }

    @Transactional(readOnly = true)
    public PosStats posStats(Long userId) {
        PharmacyUser user = requireUser(userId);
        Long pharmacyId = user.getPharmacy().getPharmacyId();
        LocalDate today = LocalDate.now();
        LocalDateTime from = startOfDayUtc(today);
        LocalDateTime to = startOfDayUtc(today.plusDays(1));
        long salesToday = dispenseRepository
                .countByPharmacyPharmacyIdAndDispensedAtBetween(pharmacyId, from, to);
        BigDecimal incomeToday = dispenseRepository
                .sumTotalByPharmacyAndDateRange(pharmacyId, from, to);
        List<RecentSale> recentSales = dispenseRepository
                .findTop5ByPharmacyPharmacyIdOrderByDispensedAtDesc(pharmacyId).stream()
                .map(dispense -> new RecentSale(dispense.getDispenseId(),
                        String.format("SALE-%06d", dispense.getDispenseId()),
                        DATE_TIME.format(dispense.getDispensedAt()),
                        dispense.getPrescription().getPrescriptionCode(),
                        dispense.getPrescription().getPatient().getDisplayName(),
                        dispense.getTotal(), dispense.getPaymentMethod()))
                .toList();
        return new PosStats(salesToday, incomeToday == null ? BigDecimal.ZERO : incomeToday,
                recentSales);
    }

    private static String normalizePaymentMethod(String value) {
        if (value == null || value.isBlank()) {
            return "CASH";
        }
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "CASH", "CARD", "UPI", "INSURANCE", "OTHER" -> normalized;
            default -> throw new IllegalArgumentException("Choose a valid payment method.");
        };
    }

    private PharmacyUser requireActiveUser(Long userId) {
        PharmacyUser pharmacyUser = requireUser(userId);
        if (pharmacyUser.getPharmacy().getStatus() != PharmacyStatus.ACTIVE) {
            throw new IllegalStateException(
                    "This pharmacy is not active. Ask the platform administrator to activate it.");
        }
        if (pharmacyUser.getStatus() != PharmacyUserStatus.ACTIVE
                || pharmacyUser.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Your pharmacy account is disabled. Contact your administrator.");
        }
        return pharmacyUser;
    }

    private PharmacyUser requireAdmin(Long userId) {
        PharmacyUser current = requireActiveUser(userId);
        if (current.getRole() != PharmacyUserRole.PHARMACY_ADMIN) {
            throw new IllegalArgumentException("Only a pharmacy administrator can manage users.");
        }
        return current;
    }

    private PharmacyUser requireSamePharmacy(PharmacyUser current, Long pharmacyUserId) {
        return pharmacyUserRepository.findById(pharmacyUserId)
                .filter(target -> target.getPharmacy().getPharmacyId()
                        .equals(current.getPharmacy().getPharmacyId()))
                .orElseThrow(() -> new IllegalArgumentException("Pharmacy user not found."));
    }

    private int remainingUnits(Long prescriptionId) {
        return itemRepository.findByPrescriptionPrescriptionId(prescriptionId).stream()
                .mapToInt(PrescriptionItem::getRemainingQuantity).sum();
    }

    private static LocalDateTime startOfDayUtc(LocalDate day) {
        return day.atStartOfDay(java.time.ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static String hashPrefix(byte[] hash) {
        return "sha256:" + HexFormat.of().formatHex(hash, 0, 4);
    }

    private static String tone(VerificationOutcome outcome) {
        return switch (outcome) {
            case VALID -> "success";
            case EXPIRED, FULLY_DISPENSED -> "warning";
            case CANCELLED, NOT_FOUND, RATE_LIMITED -> "danger";
        };
    }

    public record VerificationResult(UUID verificationId, VerificationOutcome outcome, String message,
                                     boolean canDispense, PrescriptionService.PrescriptionView prescription) {
        public String tone() {
            return PharmacyService.tone(outcome);
        }

        public String outcomeLabel() {
            return outcome.name().replace('_', ' ');
        }
    }

    public record DispenseLine(String medicine, int quantity, BigDecimal unitPrice,
                               BigDecimal lineTotal) {
    }

    public record DispenseResult(Long dispenseId, String when, String pharmacy, String code,
                                 String patient, int units, List<DispenseLine> lines,
                                 boolean fullyDispensed, BigDecimal subtotal, BigDecimal discount,
                                 BigDecimal taxRate, BigDecimal taxAmount, BigDecimal total,
                                 String paymentMethod, BigDecimal amountTendered,
                                 BigDecimal changeDue) {
        public boolean hasSale() {
            return total != null && total.signum() > 0;
        }

        public String saleNumber() {
            return dispenseId == null ? "—" : String.format("SALE-%06d", dispenseId);
        }
    }

    public record UserRow(Long pharmacyUserId, Long userId, String email, String role, String status,
                          String userStatus, String created, boolean self, boolean canManage) {
    }

    public record DailyActivity(String day, long verifications, long dispenses, long percent) {
        public DailyActivity(String day, long verifications, long dispenses) {
            this(day, verifications, dispenses, 0);
        }
    }

    public record VerificationRow(String when, String code, String patient, String pharmacist,
                                  String outcome, String tone) {
    }

    public record DispenseRow(String when, String code, String patient, String pharmacist, int units,
                              String status) {
    }

    public record PriceRow(Long priceId, String medicine, BigDecimal unitPrice, String updated) {
    }

    public record RecentSale(Long dispenseId, String saleNumber, String when, String code,
                             String patient, BigDecimal total, String paymentMethod) {
    }

    public record PosStats(long salesToday, BigDecimal incomeToday, List<RecentSale> recentSales) {
    }

    public record PharmacyReport(String pharmacyName, boolean active, long verificationsToday,
                                 long validToday, long notFoundToday, long dispensesToday, int unitsToday,
                                 List<DailyActivity> daily, List<VerificationRow> verifications,
                                 List<DispenseRow> dispenses) {
    }
}
