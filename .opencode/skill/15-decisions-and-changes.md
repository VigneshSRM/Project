# 15 - Decisions and Changes

Date: 2026-10-24. Library version moves to 1.1.0 for changed skills.

| ID | Your decision | How it was applied | Skills touched |
|---|---|---|---|
| D-01 | Tech stack Java + MSSQL | Conventions file (Spring Boot/Flyway/Thymeleaf defaults, INFERRED); SQL Server filtered unique index for slots; DATETIME2/NVARCHAR; case-sensitive collation for Rx code; Flyway; SQL Server backup plan | architecture-and-data, booking, ops, shell, smtp |
| D-02 | Super Admin: one dashboard, clinic enrollment, view patient masters, reports, SMTP settings | **New skill** `platform-admin-console`; `/admin` nav (5 items); `SUPER_ADMIN` role; patient masters read-only, masked, audited; SMTP password write-only + AES-GCM | platform-admin-console, access-control, smtp, clinic-operations, screens, shell, qa |
| D-03 | Plan pharmacy tables | `pharmacy-schema.md` + `pharmacy-schema.sql` (Pharmacies, PharmacyUsers, PharmacyVerifications, PrescriptionDispenses, PrescriptionDispenseItems, quantity columns, fulfilment view) | pharmacy-workspace, architecture-and-data |
| D-04 | One doctors login | Interpreted as: each doctor has their own login (Doctor role) created by Clinic Admin; never a shared account (accountability/audit). `/doctor` nav; consultation and prescription creation move to Doctor | consultation-history, clinic-operations, access-control, shell, screens |
| D-05 | Allow partial fills | Fulfilment status Not/Partially/Fully Dispensed; dispense dialog with remaining qty; CHECK + row locking prevents over-dispensing; void with reason; fill history visible to patient | pharmacy-workspace, prescription-lifecycle, qa |
| D-06 | Prescription ID not guessable, 32-char mixed complex | 32 chars, SecureRandom, letters (both cases) + digits (~190 bits); symbols excluded so IDs survive URLs, QR, email and copy/paste; case-sensitive column; UUID in URLs; masked in lists, full on detail; lookup normalises only spaces/hyphens | prescription-lifecycle, pharmacy-workspace, access-control, qa, screens |

## Interpretations to confirm
1. "One doctors login" -> one login **per doctor** (not a single shared login).
2. "Mixed complex" -> upper + lower + digits, **no symbols** (RC-22). Say so if you want symbols.
3. Super Admin scope lists Clinic Enrollment only; **pharmacy enrollment is undecided** (RC-19).
4. Doctor sidebar (Dashboard, My Appointments, Prescriptions) is INFERRED.
5. Typing a 32-character ID is error-prone; the design assumes copy/paste (ID shown with Copy button, emailed) and optionally QR/scanner (RC-22).

## Source-document behaviour changed
- Source Workflow B "Prescription ID Sent by SMTP": retained, but the email now contains a sensitive 32-char ID.
- Source list/detail "Prescription ID" columns: lists show shortened `first4…last4`; detail shows full.
- Source pharmacy "Dispense blocked when invalid": extended with Fully Dispensed.
