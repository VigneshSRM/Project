---
name: smtp-email-notifications
description: SMTP email integration for ClinicFlow: templates, queued sending, retries, idempotency and the EmailLogs table for email verification, password reset and prescription ID delivery. Use when sending any email, configuring SMTP, debugging delivery, or adding a notification template.
---

# SMTP Email Notifications

> Type: INTEGRATION | Category: integration | Risk: MEDIUM | Security level: MEDIUM | Status: DRAFT | Version: 1.1.0

## Purpose

Reliable, auditable, minimal-disclosure email that never blocks the user's action.

## When to Use

- Verification/OTP, reset, prescription ID mail.
- Delivery failures.

## When NOT to Use

- SMS or push (Optional, out of scope until confirmed).

## Inputs

SMTP host/port/credentials (environment), templates, event payload.

## Preconditions

EmailLogs table exists.

## Responsibilities

- Template set: verify-email, password-reset, prescription-issued, appointment-confirmation (INFERRED).
- Queue + worker, retries with backoff, idempotency key per (event, recipient).
- SMTP configuration comes from the Super Admin SMTP Settings screen (`SmtpSettings` table, password encrypted with AES-GCM, key from environment/keystore, never returned to the UI); environment values are only a bootstrap fallback.
- Java: `JavaMailSender` built from the stored settings; a DB outbox table + scheduled worker (no message broker needed).
- EmailLogs: status (queued/sent/failed), attempts, last error, provider response.

## Procedure

1. Enqueue from the domain event; never send inside the user's request thread.
2. Render the template with the minimum data (the prescription mail carries the full 32-character Prescription ID, clinic and doctor names, expiry - no diagnosis or medicines; treat this email as sensitive).
3. Send over TLS; on failure retry 3 times with exponential backoff (values `REQUIRES-CONFIRMATION`), then mark failed.
4. Expose a 'Resend' action to authorised clinic users and an in-app copy of the Prescription ID so email failure never blocks the patient.
5. In development/test, use a local mail catcher; never real addresses.

## Decision Rules

- Email failure never rolls back a prescription; it is logged and retryable.
- Verification links/OTPs are single-use and expire (duration `REQUIRES-CONFIRMATION`).
- Do not include PHI beyond the minimum in subject lines.

## Outputs

Mailer module, templates, EmailLogs, retry worker.

## Dependencies

Hard dependencies (inputs): `clinicflow-architecture-and-data`

## Tools

File read/write, local mail catcher. No production SMTP credentials.

## Security Requirements

SMTP credentials only from environment/secret store; no credentials in code or logs; header-injection-safe templating; rate-limit per recipient.

## Validation

Failed send retried and logged; duplicate event sends once; template renders without PHI leakage.

## Failure Handling

If an input is missing or marked `REQUIRES-CONFIRMATION` in `01-application-understanding.md`, state the assumption, proceed with a clearly labelled default, and list it in the handoff notes. Never invent policy values silently.

## Evaluation Criteria

Idempotency, retry correctness, minimal content, log completeness.

Eval files: `evals/basic.yaml` and `evals/security.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

Prescription saved -> event -> email queued -> sent; SMTP down -> 3 retries -> failed log + Resend button.

## Limitations

No provider is chosen (`REQUIRES-CONFIRMATION`); deliverability (SPF/DKIM) is an ops task.

## Related Skills

- `clinicflow-architecture-and-data`
- `clinicflow-delivery-and-ops`
- `patient-identity-and-linking`
- `platform-admin-console`
- `prescription-lifecycle`

