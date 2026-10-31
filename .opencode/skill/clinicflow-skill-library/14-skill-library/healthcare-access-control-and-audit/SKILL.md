---
name: healthcare-access-control-and-audit
description: Role-based access control, record-ownership enforcement, authentication hardening, PHI minimisation and audit logging for ClinicFlow. Use when implementing login or sessions, adding any endpoint that returns patient, appointment or prescription data, reviewing for IDOR/enumeration, or defining roles and permissions.
---

# Healthcare Access Control and Audit

> Type: SECURITY | Category: security | Risk: HIGH | Security level: HIGH | Status: DRAFT | Version: 1.1.0

## Purpose

Guarantee that each person sees only the records they are entitled to, and that sensitive actions leave an audit trail.

## When to Use

- Any endpoint or screen exposing patient data.
- Auth, session, password-reset work.
- Security review.

## When NOT to Use

- Visual design.
- Domain workflow rules.

## Inputs

Role list, endpoint list, data model.

## Preconditions

Schema exists.

## Responsibilities

- Roles: `SUPER_ADMIN` and `DOCTOR` (confirmed); `PATIENT`, `CLINIC_ADMIN` (clinic staff, single role in release 1), `PHARMACY_ADMIN`, `PHARMACIST` (INFERRED - confirm).
- Server-side ownership checks on every request (patient_id, clinic_id, pharmacy_id derived from the session, never from client input).
- Append-only audit log.
- Auth hardening and rate limits.

## Procedure

1. Build a permission matrix (role x resource x action) and test it.
2. Add an authorization layer called by every service method: `can(user, action, resource)`.
3. Scope queries by tenant: patients -> own `patient_id`; clinic roles -> own `clinic_id`; pharmacy roles -> own `pharmacy_id`.
4. Return 404 (not 403) for records the caller does not own, to prevent enumeration.
5. Passwords: Argon2id or bcrypt; policy `REQUIRES-CONFIRMATION`; sessions in HttpOnly, Secure, SameSite cookies; CSRF protection.
6. Rate-limit login, OTP/verification, password reset and prescription lookup; lock out after repeated failures.
7. Write audit entries: actor, role, action, resource type/ID, patient ID if any, timestamp, IP, outcome.
8. Keep PHI out of URLs, logs and error messages.

## Decision Rules

- Never trust IDs from the URL, body or query for ownership.
- Least privilege: a pharmacist sees prescription data, not appointments or history.
- Super Admin: patient masters read-only and masked, aggregate reports, no clinical content; every view audited.
- Doctor: only own appointments, consultations and prescriptions within their clinic.
- Implement with Spring Security (`@PreAuthorize` plus the ownership service); the Prescription ID is never a URL parameter.
- Audit reads of clinical data by staff, not only writes.
- Changing roles or permissions is a human-approved action.

## Outputs

Auth module, permission matrix, audit log, security tests.

## Dependencies

Hard dependencies (inputs): `clinicflow-architecture-and-data`

## Tools

File read/write inside the project workspace, test runner, linter. No production network, secrets, or credential access.

## Security Requirements

Passes IDOR tests for every resource; secrets from environment only; role or permission changes and credential resets require approval and evidence; audit log cannot be edited through the app.

## Validation

Automated tests: each role against each endpoint; patient A cannot read patient B by changing any ID; failed-login throttling works.

## Failure Handling

Deny by default; on authorization error, log and return a generic response.

## Evaluation Criteria

Zero cross-tenant reads; audit entries present; generic errors; rate limits effective.

Eval files: `evals/basic.yaml` and `evals/security.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

Patient requests `/api/prescriptions/{uuid}` belonging to another patient -> 404 and an audit entry.

## Limitations

Legal compliance regime (e.g. India DPDP Act and health-data rules) is `REQUIRES-CONFIRMATION`; this skill implements technical controls, not legal advice.

## Related Skills

- `clinic-discovery-and-booking`
- `clinic-operations-and-masters`
- `clinicflow-architecture-and-data`
- `clinicflow-delivery-and-ops`
- `consultation-and-medical-history`
- `erp-app-shell-navigation`
- `patient-identity-and-linking`
- `pharmacy-workspace`
- `platform-admin-console`
- `prescription-lifecycle`

