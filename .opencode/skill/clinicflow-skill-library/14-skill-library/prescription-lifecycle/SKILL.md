---
name: prescription-lifecycle
description: Digital prescription creation by the Doctor, the 32-character random Prescription ID, medicine items with quantities, validity/expiry, cancellation, fulfilment status (not/partially/fully dispensed) and patient viewing, printing and email. Use when building Doctor or Clinic Prescriptions, Patient > My Prescriptions, ID generation, statuses or expiry logic.
---

# Prescription Lifecycle

> Type: DOMAIN | Category: domain | Risk: HIGH | Security level: HIGH | Status: DRAFT | Version: 1.1.0

## Purpose

Issue prescriptions that are correct, tamper-resistant, unguessable and verifiable by pharmacies, with an accurate view of how much has been dispensed.

## When to Use

- Creating, viewing, cancelling, printing, emailing prescriptions.
- Generating or displaying Prescription IDs.
- Fulfilment status.

## When NOT to Use

- Pharmacy lookup and dispensing screens (pharmacy-workspace).

## Inputs

Finalised consultation, Doctor session, medicine entries.

## Preconditions

Consultation finalised.

## Responsibilities

- Header + items (medicine, dosage, frequency, duration, **quantity (+ unit)**, instructions).
- **Prescription ID:** 32 characters, cryptographically random, mixed-case letters + digits (62-symbol alphabet, about 190 bits). Confirmed: must not be guessable.
- Validity status: Valid, Expired (derived), Cancelled.
- Fulfilment status (derived from dispenses): Not Dispensed, Partially Dispensed, Fully Dispensed.
- Separate internal UUID for URLs so the Prescription ID never appears in links or logs.

## Procedure

1. Doctor creates from a finalised consultation; require at least one item with quantity > 0.
2. Generate the ID with `java.security.SecureRandom` (`nextInt(62)` per character, no modulo bias, no time/sequence input); insert; on unique-index violation regenerate (retry up to 5).
3. Set issue and expiry dates (default validity `REQUIRES-CONFIRMATION`), status Valid, fulfilment Not Dispensed.
4. Publish event -> email the full Prescription ID (best effort; treat the email as sensitive).
5. Expiry computed at read time (`expiry_date < today`); Cancelled stored. Cancel by issuing Doctor or the clinic's Clinic Admin, with reason, audited. No edits after issue - correction = cancel + reissue (a cancel is blocked if any quantity was already dispensed unless a Clinic Admin confirms; INFERRED).
6. Display: lists show a shortened form (first 4 + last 4, e.g. `Xk3F…9aQz`); detail shows the full ID in 4 groups of 8 with a Copy button; routes use the internal UUID.
7. Print/PDF includes EXPIRED, CANCELLED or FULLY DISPENSED marking where applicable.

## Decision Rules

- ID is case-sensitive; the database column must use a binary/case-sensitive collation (see schema notes) or entropy is silently lost.
- Alphabet excludes symbols on purpose (URL, QR, email and copy/paste safe); still far beyond guessable. Symbols/QR/scanner: RC-22.
- The ID is a lookup credential, not a login: pharmacy lookup still needs authentication, rate limiting and audit.
- Items immutable after issue except the system-maintained `quantity_dispensed`.
- Never log the full ID; log the last 4 characters.

## Outputs

Doctor prescription form, lists, detail, PDF/print, status logic, ID generator, tests.

## Dependencies

Hard dependencies (inputs): `consultation-and-medical-history`, `smtp-email-notifications`, `healthcare-access-control-and-audit`

## Tools

File read/write inside the project workspace, test runner, linter. No production network, secrets, or credential access.

## Security Requirements

Ownership validated on every request; IDs not enumerable; no ID in URLs, logs, analytics or file names; email content minimal; all changes audited.

## Validation

ID length 32, charset [A-Za-z0-9], uniqueness under 100k generations, statistical sanity (no sequence); expiry boundary; cancelled cannot revert; patient cannot fetch others' by changing UUID.

## Failure Handling

ID collision -> regenerate; email failure -> prescription stays issued, resend available.

## Evaluation Criteria

Unguessable ID, collation correctness, expiry correctness, immutability, fulfilment status accuracy.

Eval files: `evals/basic.yaml` and `evals/security.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

Issued ID looks like `q7Rk2mZp9XfA1dLw5TnB3vYc8HgS6uJe` (shown `q7Rk2mZp-9XfA1dLw-5TnB3vYc-8HgS6uJe`).

## Limitations

Refills (new fills after Fully Dispensed), controlled-substance rules and e-signature are not in scope. Partial fills are in scope (confirmed).

## Related Skills

- `clinicflow-qa-and-uat`
- `consultation-and-medical-history`
- `erp-screen-blueprints`
- `healthcare-access-control-and-audit`
- `pharmacy-workspace`
- `smtp-email-notifications`

