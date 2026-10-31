---
name: clinicflow-delivery-and-ops
description: Build, configuration, deployment, backup, monitoring and runbooks for ClinicFlow, with human approval gates for production deploys and destructive migrations. Use when setting up environments or CI, preparing a release, deploying, restoring data, or handling an incident.
---

# ClinicFlow Delivery and Operations

> Type: OPERATIONS | Category: operations | Risk: HIGH | Security level: HIGH | Status: DRAFT | Version: 1.1.0

## Purpose

Ship safely and keep the application observable and recoverable.

## When to Use

- Environments, CI/CD, release, backups, incidents.

## When NOT to Use

- Feature development.

## Inputs

Passing QA report, hosting choice (`REQUIRES-CONFIRMATION`).

## Preconditions

QA gates passed.

## Responsibilities

- Dev/staging/prod separation; config via environment.
- Java: Maven/Gradle build, executable JAR/WAR; SQL Server login with least privilege; Flyway run as a controlled release step in prod; SQL Server backups (full + differential + log) with restore drill; encryption at rest (RC).
- CI: lint, tests, migrations dry run.
- Backups and restore drill; health checks; structured logs without PHI; error monitoring.

## Procedure

1. Prepare staging identical to prod, synthetic data.
2. Run migrations on staging with rollback test.
3. **Request human approval** with evidence (QA report, migration plan, rollback plan).
4. Deploy after approval; run smoke tests; monitor.
5. Document runbooks: SMTP failure, DB restore, locked-out admin, suspected data exposure.

## Decision Rules

- Agent never deploys to production, runs destructive migrations, rotates credentials, or exports real patient data autonomously.
- Approval record includes approver, time, evidence and rollback steps.

## Outputs

CI config, deployment guide, runbooks, approval record template.

## Dependencies

Hard dependencies (inputs): `clinicflow-qa-and-uat`, `healthcare-access-control-and-audit`, `smtp-email-notifications`

## Tools

File read/write, CI config. Production access only through a human-run step.

## Security Requirements

Secrets in a secret manager; least-privilege deploy credentials; TLS everywhere; backups encrypted; access to production logs restricted.

## Validation

Restore drill succeeds; rollback tested; health checks green.

## Failure Handling

If an input is missing or marked `REQUIRES-CONFIRMATION` in `01-application-understanding.md`, state the assumption, proceed with a clearly labelled default, and list it in the handoff notes. Never invent policy values silently.

## Evaluation Criteria

Approval gate honoured; no PHI in logs; restore verified.

Eval files: `evals/basic.yaml` and `evals/security.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

Release request -> QA evidence attached -> approval requested -> human deploys -> smoke tests.

## Limitations

Hosting, scale and uptime targets are `REQUIRES-CONFIRMATION`.

## Related Skills

- `clinicflow-qa-and-uat`
- `healthcare-access-control-and-audit`
- `smtp-email-notifications`

