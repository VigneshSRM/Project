---
name: pharmacy-workspace
description: Pharmacy role: planned pharmacy tables, dashboard, prescription verification by the 32-character Prescription ID, EXPIRED/CANCELLED watermarks that block dispensing, partial-fill dispensing with remaining-quantity tracking, verification log, pharmacy users and reports. Use when building any Pharmacy module, dispensing, or pharmacy database tables.
---

# Pharmacy Workspace, Verification and Partial Dispensing

> Type: INDUSTRY | Category: domain | Risk: HIGH | Security level: HIGH | Status: DRAFT | Version: 1.1.0

## Purpose

Let pharmacists verify a prescription and dispense it in one or several partial fills, never exceeding what was prescribed and never when invalid.

## When to Use

- Pharmacy Dashboard, Verification/POS, Dispensing, Users, Reports.
- Pharmacy tables and migrations.

## When NOT to Use

- Creating or editing prescriptions.

## Inputs

32-character Prescription ID (typed, pasted, or scanned), pharmacist session.

## Preconditions

Pharmacy tables from `references/pharmacy-schema.md` migrated; pharmacy users exist.

## Responsibilities

- **Tables (planned):** Pharmacies, PharmacyUsers, PharmacyVerifications, PrescriptionDispenses, PrescriptionDispenseItems, plus quantity columns on PrescriptionItems (see schema file and DDL).
- Verification screen: one large ID field (autofocus, paste friendly).
- Result states: Valid, Expired, Cancelled, Fully Dispensed, Not Found.
- Dispense dialog per item: Prescribed | Dispensed so far | Remaining | Dispense now (max = remaining).
- Fill history on the result and on the patient's prescription.
- Users (pharmacy admin manages pharmacists), reports.

## Procedure

1. Normalise input: remove spaces and hyphens only; **do not change case**; require exactly 32 chars of [A-Za-z0-9] else return the same generic Not Found (no format hints). Rate-limit per user and IP; lock out after repeated misses.
2. Look up by the case-sensitive unique column; write a PharmacyVerifications row (outcome; for Not Found store a SHA-256 hash of the attempt, not the raw text).
3. Redirect to `/pharmacy/verify/result/{verificationId}` (never the Prescription ID in the URL).
4. Valid -> status banner + details (minimum necessary: patient name, doctor, clinic, medicines, quantities, dates, fill history; diagnosis hidden by default - RC-05).
5. Expired/Cancelled -> diagonal watermark over the card, banner, dispensing disabled with reason. Fully Dispensed -> 'FULLY DISPENSED' banner/watermark, dispensing disabled.
6. Dispense (partial allowed): in one transaction lock the prescription's item rows (`UPDLOCK, ROWLOCK`), re-check Valid/not expired/not cancelled, check each quantity <= remaining, insert dispense + dispense items, update `quantity_dispensed`, write audit. Verification must be recent (default 30 min, RC-20).
7. Void a dispense (pharmacy admin, reason) restores quantities in the same transaction; records are never deleted.
8. Dashboard: today's verifications, outcomes, fills (partial vs full); reports by day/outcome/user/pharmacy.

## Decision Rules

- Watermark is paired with real text/ARIA status, never visual only.
- Any pharmacy may continue a partially filled prescription (default, INFERRED - RC-20); each fill shows pharmacy name and date.
- Over-dispensing is impossible by CHECK constraint plus locking, not UI alone.
- Pharmacies cannot browse prescriptions; lookup by ID only.

## Outputs

Pharmacy migrations, screens, dispensing service, verification log, reports, tests.

## Dependencies

Hard dependencies (inputs): `prescription-lifecycle`, `healthcare-access-control-and-audit`, `erp-design-system`

## Tools

File read/write inside the project workspace, test runner, linter. No production network, secrets, or credential access.

## Security Requirements

Strict rate limiting and lockout; every lookup and dispense audited; pharmacy scoping; ID never in URL/logs (last 4 only); sensitive report export needs approval evidence; user-management changes audited.

## Validation

Concurrent fills by two pharmacies cannot exceed prescribed quantity; Expired/Cancelled/Fully Dispensed never dispense; case changes in ID fail lookup; hyphen/space grouping accepted.

## Failure Handling

Lock timeout -> ask to retry; stale quantities -> refresh dialog with new remaining values.

## Evaluation Criteria

Correct states, partial-fill arithmetic, concurrency safety, case-sensitive lookup, minimum-necessary data, logging.

Eval files: `evals/basic.yaml` and `evals/security.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

Prescribed 10 tablets: pharmacy A dispenses 4 (Partially Dispensed), pharmacy B later dispenses 6 (Fully Dispensed); a further attempt shows FULLY DISPENSED.

## Limitations

No inventory, billing or payments. Pharmacy enrollment path is open (RC-19).

## Related Skills

- `clinicflow-qa-and-uat`
- `erp-design-system`
- `erp-screen-blueprints`
- `healthcare-access-control-and-audit`
- `prescription-lifecycle`

