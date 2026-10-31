---
name: clinicflow-architecture-and-data
description: Define the system architecture and relational schema for ClinicFlow: modular monolith, API layering, 12 source tables plus inferred supporting tables, constraints and migrations. Use when scaffolding the project, adding or changing a table, writing migrations, or deciding where logic belongs.
---

# ClinicFlow Architecture and Data

> Type: ENGINEERING | Category: architecture | Risk: MEDIUM | Security level: MEDIUM | Status: DRAFT | Version: 1.1.0

## Purpose

Give every domain skill a consistent structure, data model and rule for where business logic lives.

## When to Use

- Scaffolding backend/DB.
- Adding or altering tables, constraints, migrations.
- Deciding service boundaries.

## When NOT to Use

- UI layout (shell/design skills).
- Domain rules themselves (domain skills).

## Inputs

Requirements, stack choice (`REQUIRES-CONFIRMATION`).

## Preconditions

Product spec exists.

## Responsibilities

- Modular monolith: one deployable, one relational database, modules per domain.
- Layers: UI -> API (auth + validation) -> domain service -> repository.
- Schema outline in `references/schema-outline.md`.
- Versioned, reversible migrations; seed data (synthetic only).

## Procedure

1. Stack is confirmed: **Java + Microsoft SQL Server**. Apply `references/java-mssql-conventions.md` (framework defaults INFERRED - confirm).
2. Create modules: identity, platform-admin, clinic-ops, booking, consultation, prescription, pharmacy, notifications, audit.
3. Implement tables from the schema outline with constraints (below).
4. Expose services; controllers never query tables directly.
5. Add migration + rollback for each change; seed demo clinics, doctors, patients.

## Decision Rules

- Public IDs (`APT-...`, `RX-...`, patient ID) are separate from internal primary keys.
- Slot uniqueness enforced by a SQL Server **filtered unique index**, not only application code.
- SQL Server specifics: `DATETIME2` (UTC), `NVARCHAR`, `ROWVERSION` for optimistic locking, **case-sensitive binary collation on the Prescription ID column**.
- Clinical records (consultations, prescriptions) are never hard-deleted; use status + audit.
- Store timestamps as UTC; convert for display (display time zone `REQUIRES-CONFIRMATION`).
- Add tables not in the source (Pharmacies, ClinicPatients, PatientLinkRequests, AuditLogs, auth tokens) only as labelled `INFERRED`.

## Outputs

Project skeleton, migrations, schema docs, seed script.

## Dependencies

Hard dependencies (inputs): `clinicflow-product-spec`

## Tools

File read/write, Flyway migrations and a local SQL Server instance (e.g. container). No production database access.

## Security Requirements

Destructive or irreversible migrations (drop/alter with data loss) need human approval. Secrets only via environment variables. Passwords stored as salted hashes only.

## Validation

Migrations apply and roll back on a clean DB; constraint tests (double booking rejected, orphan rows rejected).

## Failure Handling

If the Java framework defaults are rejected, keep the schema and rules and replace only `java-mssql-conventions.md`.

## Evaluation Criteria

Schema covers all relationships in the source; constraints present; no direct controller-to-table access.

Eval files: `evals/basic.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

Adding `Pharmacies` produces a migration, FK from pharmacy users, and an `INFERRED` note pending confirmation.

## Limitations

Does not choose hosting or tune performance without scale numbers.

## Related Skills

- `clinic-discovery-and-booking`
- `clinic-operations-and-masters`
- `clinicflow-product-spec`
- `healthcare-access-control-and-audit`
- `patient-identity-and-linking`
- `platform-admin-console`
- `smtp-email-notifications`

