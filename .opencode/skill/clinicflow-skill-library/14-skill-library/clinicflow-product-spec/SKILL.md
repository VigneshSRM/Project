---
name: clinicflow-product-spec
description: Convert the ClinicFlow application document into testable requirements with IDs, acceptance criteria and a traceability matrix, and track unresolved questions. Use at project start, when the source document changes, when a requirement is ambiguous, or when someone asks 'what exactly should this feature do'.
---

# ClinicFlow Product Spec

> Type: CORE | Category: product | Risk: LOW | Security level: LOW | Status: DRAFT | Version: 1.1.0

## Purpose

Be the single source of truth for what ClinicFlow must do, in testable form, and the owner of the open-questions list.

## When to Use

- Starting the project or a new module.
- A stakeholder changes or adds a requirement.
- A skill needs a rule that the document does not state.

## When NOT to Use

- Choosing technology (architecture skill).
- Writing code.

## Inputs

Source document (patient modules, workflows A-D, tables, scope table); stakeholder answers.

## Preconditions

None.

## Responsibilities

- Assign stable IDs (e.g. `PAT-REG-01`, `APT-BOOK-04`, `RX-VER-02`).
- Write acceptance criteria in Given/When/Then.
- Maintain the traceability chain requirement -> domain -> workflow -> skill -> eval.
- Own and update the `REQUIRES-CONFIRMATION` register and the decision log (`15-decisions-and-changes.md`).
- Apply change management: diff old vs new, list affected skills and evals.

## Procedure

1. Extract every 'must/should/required' statement from the source into a requirement row.
2. Classify each as `USER-PROVIDED`, `INFERRED` or `REQUIRES-CONFIRMATION`.
3. Write acceptance criteria; avoid vague words - if no measurable value is given, mark `REQUIRES-CONFIRMATION`.
4. Map each requirement to a skill; flag orphans and unmet requirements.
5. On change: produce an impact list (skills, evals, security controls) and recommend semver bumps.

## Decision Rules

- Do not invent numbers (slot length, OTP expiry, validity days); propose a default and label it.
- The source's Required/Optional scope table decides priority; Optional items (maps, payments, SMS) stay out of the first release.

## Outputs

`02-requirements.md`, updated register in `01-application-understanding.md`, traceability matrix.

## Dependencies

None.

## Tools

File read/write inside the project workspace, test runner, linter. No production network, secrets, or credential access.

## Security Requirements

Security and privacy requirements are first-class rows (ownership checks, audit, minimal disclosure), not footnotes.

## Validation

Every requirement has an owner skill and at least one acceptance criterion; no orphan skills.

## Failure Handling

Contradictions in the source are listed as questions with the options and a recommended default; work continues on unaffected requirements.

## Evaluation Criteria

Coverage of all six patient modules and workflows A-D; correct tagging; honest open-questions list.

Eval files: `evals/basic.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

Source: 'Cancelled appointments should release their slots when appropriate.' -> `APT-CAN-03` + question: what makes it 'appropriate' (cut-off time)?

## Limitations

Cannot answer stakeholder policy questions; only surfaces them.

## Related Skills

- `clinicflow-architecture-and-data`
- `clinicflow-build-orchestrator`
- `erp-design-system`

