---
name: erp-screen-blueprints
description: Screen-by-screen UI specifications for all ClinicFlow modules (super admin, clinic, doctor, patient, pharmacy): layout pattern, content, actions and required states, using reusable list/detail/form/wizard/dashboard patterns. Use when building or reviewing any screen, so the UI is clear, consistent and ERP-like.
---

# ERP Screen Blueprints

> Type: ENGINEERING | Category: frontend | Risk: LOW | Security level: LOW | Status: DRAFT | Version: 1.1.0

## Purpose

Prevent ad-hoc screens: each module is built from a small set of proven page patterns with explicit content and states.

## When to Use

- Implementing a module's UI.
- Reviewing a screen against the standard.

## When NOT to Use

- Business rules (domain skills).
- Shell and tokens.

## Inputs

Domain skill field lists, shell, design system.

## Preconditions

Shell and tokens exist.

## Responsibilities

- Page patterns: Dashboard, List (filter + table), Detail (summary header + tabs), Form, Wizard (stepper), Verification (status-first).
- Per-screen table in `references/screen-specs.md`.

## Procedure

1. Pick the pattern for the screen; do not invent a new one.
2. Fill content from the screen table; field names come from the domain skill.
3. Implement the four states: loading skeleton, empty, error with retry, no-permission.
4. List screens: search, 1-3 filters, sortable columns, pagination (default 20), row click opens detail, mobile -> cards.
5. Detail screens: header (ID, status badge, key facts), tabs for sections, action bar.
6. Wizards: stepper with progress, Back/Next, summary before confirm, resumable on refresh.

## Decision Rules

- Dashboards: max 4 KPI cards in the first row, then two lists (upcoming, recent); every card links to its filtered list.
- Primary action top-right of the page header; one only.
- Show the most decision-relevant status first (status banner on prescription and verification).
- Patient wording is plain language; clinic/pharmacy wording is compact and operational.

## Outputs

Implemented screens and a screen checklist per module.

## Dependencies

Hard dependencies (inputs): `erp-app-shell-navigation`, `erp-design-system`, `patient-identity-and-linking`, `clinic-discovery-and-booking`, `consultation-and-medical-history`, `prescription-lifecycle`, `pharmacy-workspace`, `clinic-operations-and-masters`, `platform-admin-console`

## Tools

File read/write inside the project workspace, test runner, linter. No production network, secrets, or credential access.

## Security Requirements

Screens render only server-authorized data; no client-side filtering of data that the role must not see (e.g. other patients' rows).

## Validation

Each screen passes the orchestrator's UI/UX Definition of Done.

## Failure Handling

If an input is missing or marked `REQUIRES-CONFIRMATION` in `01-application-understanding.md`, state the assumption, proceed with a clearly labelled default, and list it in the handoff notes. Never invent policy values silently.

## Evaluation Criteria

Pattern fidelity; all states present; field coverage matches the source document.

Eval files: `evals/basic.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

Clinic > Appointments uses the List pattern with date, doctor, status filters and status badges; row click opens Detail.

## Limitations

Wireframes are textual; visual mock-ups are produced separately if wanted.

## Related Skills

- `clinic-discovery-and-booking`
- `clinic-operations-and-masters`
- `clinicflow-qa-and-uat`
- `consultation-and-medical-history`
- `erp-app-shell-navigation`
- `erp-design-system`
- `patient-identity-and-linking`
- `pharmacy-workspace`
- `platform-admin-console`
- `prescription-lifecycle`

