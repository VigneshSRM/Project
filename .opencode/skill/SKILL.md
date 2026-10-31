---
name: platform-admin-console
description: Super Admin role for ClinicFlow: one dashboard, clinic enrollment, read-only view of patient masters, platform reports and SMTP settings. Use when building anything under /admin, enrolling clinics, configuring email delivery, or giving the platform owner cross-clinic visibility.
---

# Platform Admin Console (Super Admin)

> Type: DOMAIN | Category: domain | Risk: HIGH | Security level: HIGH | Status: DRAFT | Version: 1.0.0

## Purpose

Give the platform owner a single, safe console to onboard clinics, see platform health and patients, and configure email - without exposing clinical data.

## When to Use

- Any Super Admin screen or API.
- Clinic onboarding/suspension.
- SMTP configuration or email delivery diagnostics.

## When NOT to Use

- Clinic-level data entry (clinic-operations).
- Pharmacy enrollment (not in the confirmed Super Admin scope - RC-19).

## Inputs

Confirmed Super Admin scope: Dashboard, Clinic Enrollment, Patient Masters (view), Reports, SMTP Settings.

## Preconditions

Access control, email service and clinic tables exist. A Super Admin account exists via secure bootstrap (below).

## Responsibilities

- **Dashboard:** KPI cards (Clinics Active/Pending/Suspended, Patients, Appointments today/month, Prescriptions issued, Pharmacy verifications and fills, Email failures 24h), recent enrollments, SMTP status card; every card links to its filtered list/report.
- **Clinic Enrollment:** list (name, city, status, enrolled on, admin email), New Clinic form (clinic name, licence/registration no. (INFERRED), address, city, district, state, postal code, phone, email, first Clinic Admin name + email), detail, Suspend/Reactivate with reason, Resend invite. No delete.
- **Patient Masters (view):** cross-clinic search, read-only; contact fields masked with audited Reveal; no clinical content.
- **Reports:** enrollment trend, appointments by clinic/status, prescriptions by fulfilment status, pharmacy verifications/fills, email delivery rate; date range; on-screen only in release 1.
- **SMTP Settings:** host, port, security (STARTTLS/SSL), username, password (write-only), from name/address, Send Test Email, last test result.

## Procedure

1. Bootstrap: first Super Admin created by a human at deploy via environment/CLI with forced password change; no default password in code.
2. Enrollment: validate -> create Clinic (Pending) + Clinic Admin user -> queue invite email (single-use set-password link) -> clinic becomes Active when the admin sets a password; audit every step.
3. Patient view: query across clinics through a read-only service; mask mobile/email; Reveal writes an audit row (who, which patient, when).
4. Reports: parameterised aggregate queries, date-range limited; no patient-level rows.
5. SMTP: save encrypted (AES-GCM, key from environment/keystore), never return the password to the browser; require a successful Send Test before marking settings Active; changes audited with old/new non-secret values.

## Decision Rules

- Super Admin sees patient master data only - never consultations, diagnoses, prescription contents or appointment reasons.
- Read-only on patients; no edit/delete.
- Patient-level exports are out of release 1.
- Suspending a clinic blocks its logins and new bookings but preserves all records.
- MFA for Super Admin recommended (INFERRED - RC).

## Outputs

/admin screens, enrollment service, SMTP settings service, report queries, tests.

## Dependencies

Hard dependencies (inputs): `clinicflow-architecture-and-data`, `healthcare-access-control-and-audit`, `smtp-email-notifications`, `clinic-operations-and-masters`

## Tools

File read/write inside the project workspace, test runner, linter. No production network, secrets, or credential access.

## Security Requirements

Super Admin is the highest-privilege role: rate-limited login, session timeout, MFA recommended, all actions audited, SMTP secret encrypted at rest and write-only in UI, enrollment invite tokens hashed and single-use. The agent never enters real SMTP credentials or creates real admin accounts - the human Super Admin does so at runtime.

## Validation

Clinic/Doctor/Patient/Pharmacy roles get 404/redirect on /admin; password never appears in any response or log; Reveal is audited; suspended clinic cannot log in.

## Failure Handling

Invite email failure -> enrollment stays Pending with Resend; failed SMTP test -> settings stay inactive and the previous working configuration remains in use.

## Evaluation Criteria

Scope exactly as confirmed; masking and audit; SMTP secret handling; no clinical data exposure.

Eval files: `evals/basic.yaml` and `evals/security.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

Super Admin enrolls 'City Care Clinic' -> invite emailed to its admin -> clinic Active after password set -> appears in dashboard counts.

## Limitations

Pharmacy enrollment, MFA and platform-level patient exports are open questions.

## Related Skills

- `clinic-operations-and-masters`
- `clinicflow-architecture-and-data`
- `clinicflow-qa-and-uat`
- `erp-screen-blueprints`
- `healthcare-access-control-and-audit`
- `smtp-email-notifications`

