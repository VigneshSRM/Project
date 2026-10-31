---
name: clinic-operations-and-masters
description: Clinic-side master data and dashboard for ClinicFlow: clinic profile (address, coordinates, hours, fees), doctors and specializations, doctor schedules, patient masters (including walk-ins), and the clinic dashboard. Use when building the Clinic role's Dashboard, Patient Masters or Doctors modules, or doctor availability.
---

# Clinic Operations and Masters

> Type: DOMAIN | Category: domain | Risk: MEDIUM | Security level: MEDIUM | Status: DRAFT | Version: 1.1.0

## Purpose

Own the data clinics maintain so patients can discover them and booking can work.

## When to Use

- Clinic Dashboard, Patient Masters, Doctors modules.
- Doctor schedules or clinic profile changes.

## When NOT to Use

- Booking and slot logic (booking skill).
- Consultation records.

## Inputs

Requirements, schema.

## Preconditions

Clinic user authenticated; clinic_id on session.

## Responsibilities

- Clinic profile: name, logo (optional), address, lat/long, contact, opening hours, specializations, consultation fees (optional).
- Doctors: CRUD, specialization, ClinicDoctors link, weekly DoctorSchedules plus date exceptions (leave). **Doctor login:** Clinic Admin can create the doctor's own login (email + invite link to set password); one login per doctor, never shared.
- Clinic is created by Super Admin enrollment; Clinic Admin completes the profile.
- Patient masters: search by name, mobile, Patient ID; clinic-created (walk-in) records not yet linked to an account; duplicate warning on name+DOB+mobile (warn, never auto-merge).
- Dashboard: today's appointments, status counts, check-in queue, recent prescriptions (widgets `REQUIRES-CONFIRMATION`).

## Procedure

1. Build clinic profile and doctor/schedule forms with validation (coordinates range, schedule overlaps rejected).
2. Patient master list with filters; patient detail shows profile, appointments, prescriptions issued by this clinic.
3. Schedule changes that affect booked slots show a conflict list; do not silently cancel bookings.
4. Dashboard KPIs link to filtered Appointment/Prescription lists.

## Decision Rules

- A clinic sees only patients that have a record at that clinic (ClinicPatients link) or booked with it.
- Deactivate, do not delete, doctors with history.
- Coordinates are required for a clinic to appear in nearby search.
- Clinics are enrolled only by the Super Admin (confirmed).

## Outputs

Clinic profile, Doctors, Schedules, Patient Masters, Dashboard.

## Dependencies

Hard dependencies (inputs): `clinicflow-architecture-and-data`, `healthcare-access-control-and-audit`

## Tools

File read/write inside the project workspace, test runner, linter. No production network, secrets, or credential access.

## Security Requirements

All queries scoped to clinic_id; patient master access is audited; exports of patient data need human approval.

## Validation

Clinic A cannot list clinic B's patients/doctors; schedule overlap rejected.

## Failure Handling

If an input is missing or marked `REQUIRES-CONFIRMATION` in `01-application-understanding.md`, state the assumption, proceed with a clearly labelled default, and list it in the handoff notes. Never invent policy values silently.

## Evaluation Criteria

Scoping, duplicate warning, schedule validation, dashboard-to-list linking.

Eval files: `evals/basic.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

Doctor leave on 5 Nov: dates blocked; existing bookings listed for staff to reschedule.

## Limitations

A doctor login belongs to one clinic in release 1 (INFERRED, RC-21); multi-branch clinics are out of scope.

## Related Skills

- `clinic-discovery-and-booking`
- `clinicflow-architecture-and-data`
- `clinicflow-qa-and-uat`
- `erp-screen-blueprints`
- `healthcare-access-control-and-audit`
- `patient-identity-and-linking`
- `platform-admin-console`

