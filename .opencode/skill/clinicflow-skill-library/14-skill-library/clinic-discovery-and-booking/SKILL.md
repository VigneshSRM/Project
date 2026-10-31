---
name: clinic-discovery-and-booking
description: Nearby-clinic search (name, city, area, postal code, distance from browser location) and the appointment lifecycle: slot generation, double-booking prevention, booking wizard, status machine, cancel and reschedule. Use when building Find Nearby Clinics, patient or clinic Appointments modules, slot logic, or appointment statuses.
---

# Clinic Discovery and Appointment Booking

> Type: DOMAIN | Category: domain | Risk: MEDIUM | Security level: MEDIUM | Status: DRAFT | Version: 1.1.0

## Purpose

Help patients find a clinic and book a slot that is guaranteed free, and let clinics manage the resulting appointments.

## When to Use

- Clinic search, clinic details, booking, rescheduling, cancellation, clinic appointment queue.

## When NOT to Use

- Consultation notes or prescriptions.

## Inputs

Clinic coordinates, DoctorSchedules, patient session.

## Preconditions

Clinics have coordinates; doctors have schedules.

## Responsibilities

- Search + distance sorting (Haversine in app or DB).
- Slot generation from schedules (slot length `REQUIRES-CONFIRMATION`).
- Booking transaction with recheck and DB uniqueness.
- Appointment ID (e.g. `APT-YYYY-NNNNN`, INFERRED).
- Status machine and clinic actions.

## Procedure

1. Search by clinic name/city/area/postal code; 'Near me' requests browser location with a clear prompt; if denied, fall back to text search without error.
2. Clinic card/detail per source fields; Next Available Appointment computed from open slots.
3. Booking wizard: Clinic -> Doctor -> Date & Slot -> Reason for visit -> Confirm.
4. On confirm, in one transaction: recheck slot free, insert appointment; SQL Server filtered unique index on (doctor, slot start) for active statuses; on conflict show 'slot just taken' and refresh slots.
5. Status transitions: Booked -> Confirmed -> Checked In -> In Consultation -> Completed; Booked/Confirmed -> Cancelled; Confirmed -> No-Show. Only allowed actors can transition (clinic for check-in/no-show; patient or clinic for cancel).
6. Cancel releases the slot subject to clinic rules (cut-off `REQUIRES-CONFIRMATION`); reschedule = atomic release + book.

## Decision Rules

- Never show or accept an unavailable slot; validate again on save.
- Completed, Cancelled and No-Show are terminal.
- A patient cannot hold two active appointments at the same time (INFERRED, confirm).
- Location is used only in the request; do not store the patient's coordinates.

## Outputs

Search, booking wizard, appointment lists/detail for patient and clinic, concurrency tests.

## Dependencies

Hard dependencies (inputs): `clinicflow-architecture-and-data`, `healthcare-access-control-and-audit`, `clinic-operations-and-masters`, `patient-identity-and-linking`

## Tools

File read/write inside the project workspace, test runner, linter. No production network, secrets, or credential access.

## Security Requirements

Patients see only their appointments; clinics only their own; location data not persisted; booking endpoint rate-limited.

## Validation

Concurrency test: two simultaneous bookings, exactly one succeeds; cancel frees slot; illegal transitions rejected.

## Failure Handling

If an input is missing or marked `REQUIRES-CONFIRMATION` in `01-application-understanding.md`, state the assumption, proceed with a clearly labelled default, and list it in the handoff notes. Never invent policy values silently.

## Evaluation Criteria

No double booking; fallback without location; transitions per table; slot recheck.

Eval files: `evals/basic.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

Two patients confirm 10:30 with Dr. Kumar at once -> one confirmed, other sees conflict and new slots.

## Limitations

Maps and online payment are Optional and excluded.

## Related Skills

- `clinic-operations-and-masters`
- `clinicflow-architecture-and-data`
- `clinicflow-qa-and-uat`
- `consultation-and-medical-history`
- `erp-screen-blueprints`
- `healthcare-access-control-and-audit`
- `patient-identity-and-linking`

