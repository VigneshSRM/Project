---
name: clinicflow-build-orchestrator
description: Master entry point for developing the Clinic, Pharmacy and Patient Management application (working name ClinicFlow) end to end with a corporate-ERP-grade UI/UX. Use whenever the user asks to build, scaffold, continue, extend or review any part of this app, or says 'develop the application' - it sequences the other ClinicFlow skills, enforces the UI/UX Definition of Done (consistent shell, smooth navigation, breadcrumbs on every screen) and stops at human approval gates.
---

# ClinicFlow Build Orchestrator

> Type: CORE | Category: orchestration | Risk: MEDIUM | Security level: MEDIUM | Status: DRAFT | Version: 1.1.0

## Purpose

Turn the 17-skill library into one ordered build. This is the skill to invoke first: it decides what to build next, which skill owns it, and whether a module is genuinely done.

## When to Use

- Any request to build, continue, or review ClinicFlow.
- Starting a new module, or deciding what to build next.
- Before declaring a module complete (run the Definition of Done).

## When NOT to Use

- Single narrow questions fully owned by one skill (call that skill directly).
- Work on a different application.

## Inputs

- `01-application-understanding.md` and the open `REQUIRES-CONFIRMATION` list.
- Tech stack: **Java + Microsoft SQL Server (confirmed)**. Framework defaults are INFERRED: Java 21 LTS, Spring Boot 3, Spring Security, Spring Data JPA, Flyway, Spring Mail, Thymeleaf (see `clinicflow-architecture-and-data`).
- Roles (confirmed): Super Admin, Clinic, Doctor, Patient, Pharmacy.
- Current repository state.

## Preconditions

Product spec exists (`clinicflow-product-spec`). If not, run it first.

## Responsibilities

- Sequence skills by dependency wave (see `08-skill-dependency-graph.md`).
- Build a thin vertical slice first, then widen.
- Gate every module on the UI/UX Definition of Done and the QA skill.
- Escalate approval-gated actions to the human.

## Procedure

1. Read open questions; ask the user only the ones that block the current wave (remaining open questions in `01-application-understanding.md`, e.g. pharmacy enrollment, hosting).
2. Scaffold in wave order: spec -> architecture/data + design system -> access control + email -> app shell + clinic masters -> platform admin console + patient identity -> booking -> consultation/history -> prescription -> pharmacy workspace -> screens polish -> QA -> ops.
3. **Vertical slice first:** login -> role shell with sidebar + breadcrumbs -> super admin enrolls a clinic -> clinic admin creates a doctor + doctor login -> patient registers -> books -> doctor consults -> prescription issued (32-character ID) -> pharmacy verifies and partially dispenses. Every later feature extends this slice.
4. For each module: spec -> schema -> API with ownership checks -> screen from `erp-screen-blueprints` -> tests.
5. Run the **UI/UX Definition of Done** (below) before marking the module complete.
6. Report: what is built, what is gated, what is still unconfirmed.

**UI/UX Definition of Done (every screen):**
- Rendered inside the shared app shell: top bar, role sidebar with correct active item, breadcrumb bar, page header.
- Breadcrumb matches `navigation-map.md`; last crumb is the current page and is not a link.
- Page header has title, optional status badge, one primary action.
- Loading, empty, error and no-permission states exist.
- Forms: inline validation, required markers, disabled-while-saving, success toast, unsaved-changes guard.
- Destructive actions (cancel appointment, cancel prescription) use a confirm dialog that names the record.
- No dead ends: every screen has a way back (breadcrumb, Cancel, or Back to list).
- Keyboard reachable with visible focus; works at 360, 768 and 1280 px widths.
- Uses design tokens only; no ad-hoc colours or spacing.

## Decision Rules

- UI/UX is a gate, not polish: a working screen that skips the shell or breadcrumbs is not done.
- Prefer finishing a slice over starting a new module.
- If a `REQUIRES-CONFIRMATION` item changes the data model, ask before building that table; otherwise proceed with the labelled default.
- Never skip the QA or ops approval gates to save time.

## Outputs

- A running, navigable application increment per wave.
- A short handoff note per module: done, gated, assumptions.

## Dependencies

Hard dependencies (inputs): `clinicflow-product-spec`
Invokes (coordinates): all other ClinicFlow skills.

## Tools

File read/write inside the project workspace, test runner, linter. No production network, secrets, or credential access.

## Security Requirements

- Never use real patient data in development; seed synthetic data only.
- Production deploys, destructive migrations, credential/role changes and data exports require explicit human approval (owned by `clinicflow-delivery-and-ops` and `healthcare-access-control-and-audit`).

## Validation

Every module passes the Definition of Done and its skill's evals; `12-quality-gates.md` status updated.

## Failure Handling

If a wave is blocked by an unanswered question, build the unblocked parts of other waves in parallel and name exactly what is blocked and which question unblocks it.

## Evaluation Criteria

Correct wave ordering; slice-first behaviour; Definition of Done applied to each module; approval gates respected.

Eval files: `evals/basic.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

User: 'develop the application'. Orchestrator confirms the stack question, scaffolds shell + design tokens + auth, then builds the vertical slice, reporting after each wave.

## Limitations

Coordinates other skills; contains no domain rules of its own.

## Related Skills

- `clinicflow-product-spec`

