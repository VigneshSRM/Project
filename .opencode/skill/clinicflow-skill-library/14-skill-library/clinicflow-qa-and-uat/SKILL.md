---
name: clinicflow-qa-and-uat
description: Test strategy and acceptance for ClinicFlow: unit/integration/E2E for workflows A-D, role-access matrix, concurrency tests, and a UI/UX acceptance checklist (breadcrumbs, navigation, responsiveness, accessibility). Use before declaring any module or release complete.
---

# ClinicFlow QA and UAT

> Type: QUALITY | Category: quality | Risk: LOW | Security level: LOW | Status: DRAFT | Version: 1.1.0

## Purpose

Prove the app works and that the UI/UX bar is met, with evidence.

## When to Use

- Module completion, release candidate, regression after change.

## When NOT to Use

- Writing domain code.

## Inputs

Requirements, navigation map, seed data.

## Preconditions

Features implemented.

## Responsibilities

- E2E for Workflows A-D.
- Table-driven route/breadcrumb tests from `navigation-map.md`.
- Role x endpoint access tests, IDOR sweeps.
- Concurrency: double-booking and over-dispensing (two pharmacies filling one item).
- Prescription ID tests (length 32, charset, uniqueness, case-sensitivity); Super Admin masking/audit; Doctor scoping.
- Accessibility (axe) and responsive screenshots at 360/768/1280.
- UAT scripts per role.

## Procedure

1. Seed synthetic data.
2. Run unit and integration suites.
3. Run E2E: A (register -> book), B (consult -> prescribe -> email), C (pharmacy verify: valid/expired/cancelled), D (history).
4. Run navigation tests: every route has correct title, breadcrumb, active sidebar item, back path.
5. Run security matrix and concurrency tests.
6. Produce a QA report; update `12-quality-gates.md`.

## Decision Rules

- A UI/UX failure is a failing test, not a nice-to-have.
- Flaky tests are fixed or quarantined with an owner.

## Outputs

Test suites, QA report, UAT scripts.

## Dependencies

Hard dependencies (inputs): `erp-screen-blueprints`, `clinic-discovery-and-booking`, `consultation-and-medical-history`, `prescription-lifecycle`, `pharmacy-workspace`, `patient-identity-and-linking`, `clinic-operations-and-masters`, `platform-admin-console`

## Tools

File read/write inside the project workspace, test runner, linter. No production network, secrets, or credential access.

## Security Requirements

Synthetic data only; test accounts never exist in production.

## Validation

All gates in `12-quality-gates.md` evidenced.

## Failure Handling

If an input is missing or marked `REQUIRES-CONFIRMATION` in `01-application-understanding.md`, state the assumption, proceed with a clearly labelled default, and list it in the handoff notes. Never invent policy values silently.

## Evaluation Criteria

Workflow coverage, route coverage, security matrix completeness.

Eval files: `evals/basic.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

Navigation test fails because `/clinic/doctors/:id/schedule` lacks a breadcrumb entry.

## Limitations

Load testing needs scale numbers (`REQUIRES-CONFIRMATION`).

## Related Skills

- `clinic-discovery-and-booking`
- `clinic-operations-and-masters`
- `clinicflow-delivery-and-ops`
- `consultation-and-medical-history`
- `erp-screen-blueprints`
- `patient-identity-and-linking`
- `pharmacy-workspace`
- `platform-admin-console`
- `prescription-lifecycle`

