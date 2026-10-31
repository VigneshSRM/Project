---
name: consultation-and-medical-history
description: Consultation records and the patient's chronological medical history timeline. Use when building the doctor's consultation screen (opened from an appointment), recording diagnoses and follow-ups, handling corrections, or building My Medical History.
---

# Consultation and Medical History

> Type: DOMAIN | Category: domain | Risk: HIGH | Security level: HIGH | Status: DRAFT | Version: 1.1.0

## Purpose

Create trustworthy clinical records at the point of care and present them to patients read-only and in order.

## When to Use

- Doctor workspace (Doctor login): dashboard, My Appointments, consultation, My Prescriptions.
- Starting/finishing a consultation.
- My Medical History.
- Correcting a clinical record.

## When NOT to Use

- Prescription content (prescription-lifecycle).

## Inputs

Appointment in In Consultation state, doctor session.

## Preconditions

Appointment checked in.

## Responsibilities

- Consultation: diagnosis, notes, follow-up recommendation, type (list `REQUIRES-CONFIRMATION`).
- Finalise -> immutable; corrections via signed addendum.
- History read model: reverse-chronological timeline with clinic, doctor, consultation type, linked prescription and status.
- Source labelling: patient-entered vs doctor-recorded.

## Procedure

1. Doctor opens Doctor > Appointments > detail > 'Start consultation' (sets In Consultation); Clinic Admin handles check-in at the front desk and has no consultation access.
2. Save draft; 'Finalise' marks the appointment Completed and unlocks 'Create prescription'.
3. Corrections: authorised clinic role adds an addendum with reason; original preserved; audit.
4. History timeline groups by date, newest first; filters by date range and clinic; paginate; each entry opens its visit detail and associated prescription.

## Decision Rules

- Patients cannot edit diagnoses or prescriptions.
- Only the treating Doctor can finalise; amendments by that Doctor, or a Clinic Admin with reason (audited).
- History shows only records linked to the patient's verified account.

## Outputs

Consultation screen, history timeline, addendum workflow.

## Dependencies

Hard dependencies (inputs): `clinic-discovery-and-booking`, `healthcare-access-control-and-audit`

## Tools

File read/write inside the project workspace, test runner, linter. No production network, secrets, or credential access.

## Security Requirements

Reads and amendments audited; ownership enforced server-side; diagnosis hidden from pharmacy role by default.

## Validation

Finalised record cannot be updated in place; timeline order correct; patient cannot access another patient's visit.

## Failure Handling

If an input is missing or marked `REQUIRES-CONFIRMATION` in `01-application-understanding.md`, state the assumption, proceed with a clearly labelled default, and list it in the handoff notes. Never invent policy values silently.

## Evaluation Criteria

Immutability, addendum audit, ordering, source labelling.

Eval files: `evals/basic.yaml` and `evals/security.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

Doctor corrects diagnosis -> addendum shown under original, both timestamped.

## Limitations

Doctors have their own logins (confirmed); Clinic Admin does not enter consultations on a doctor's behalf (accountability).

## Related Skills

- `clinic-discovery-and-booking`
- `clinicflow-qa-and-uat`
- `erp-screen-blueprints`
- `healthcare-access-control-and-audit`
- `prescription-lifecycle`

