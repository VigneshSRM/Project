---
name: patient-identity-and-linking
description: Patient self-registration, email verification, login/logout, password reset, profile management, account status, and safe linking of a patient account to existing clinic records. Use when building registration, login, My Profile, or the account-linking flow.
---

# Patient Identity and Linking

> Type: DOMAIN | Category: domain | Risk: HIGH | Security level: HIGH | Status: DRAFT | Version: 1.0.0

## Purpose

Let patients create and manage their own accounts safely and connect them to clinic records only after real identity verification.

## When to Use

- Registration, verification, login, reset, profile.
- Linking an account to clinic records.

## When NOT to Use

- Staff/pharmacy user management (pharmacy-workspace, clinic-operations).

## Inputs

Registration fields from the source (personal, address, optional medical).

## Preconditions

Email service and Patients/Users tables exist.

## Responsibilities

- Auto-generated Patient ID.
- Registration in 3 grouped sections: Personal, Address, Optional Medical.
- Email verification (link or OTP: `REQUIRES-CONFIRMATION`; default single-use link).
- Profile edit; email/mobile change requires re-verification.
- Account status: Pending Verification, Active, Suspended, Deactivated.
- Linking workflow.

## Procedure

1. Validate: required fields, DOB not in future, mobile format (country rules `REQUIRES-CONFIRMATION`), password match/policy, unique email.
2. Create user in Pending Verification; send verification email; block login until verified.
3. Forgot/reset password with single-use expiring token; generic response whether or not the email exists.
4. Profile page shows patient-entered data labelled as such.
5. Linking: patient requests link -> system creates PatientLinkRequest -> verification proof (default proposal: one-time code sent to the mobile/email held in the clinic record + DOB match, or clinic staff approval) -> on success link records -> audit.
6. Allow unlink/dispute handling through the clinic.

## Decision Rules

- Never auto-link on name or email match alone (source rule).
- A clinic record may link to one account only.
- Linked history appears only after link is approved.
- Patient-entered medical info is never presented as doctor-recorded.

## Outputs

Auth screens, profile module, link workflow, tests.

## Dependencies

Hard dependencies (inputs): `clinicflow-architecture-and-data`, `healthcare-access-control-and-audit`, `smtp-email-notifications`, `clinic-operations-and-masters`

## Tools

File read/write inside the project workspace, test runner, linter. No production network, secrets, or credential access.

## Security Requirements

Enumeration-safe responses, rate limits, token expiry, audit of link events; no PHI in verification emails; password never logged.

## Validation

Cannot log in unverified; reset token single-use; mismatched DOB cannot link; two accounts cannot claim the same record.

## Failure Handling

Verification email failed -> allow Resend; link proof failed -> counted attempt, cooldown after limit.

## Evaluation Criteria

All source registration fields; no auto-link; verified-only login; generic auth errors.

Eval files: `evals/basic.yaml` and `evals/security.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

Patient registers with email matching a clinic record -> no data appears until proof step passes.

## Limitations

Link proof method and SMS are `REQUIRES-CONFIRMATION`.

## Related Skills

- `clinic-discovery-and-booking`
- `clinic-operations-and-masters`
- `clinicflow-architecture-and-data`
- `clinicflow-qa-and-uat`
- `erp-screen-blueprints`
- `healthcare-access-control-and-audit`
- `smtp-email-notifications`

