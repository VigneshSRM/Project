---
name: erp-design-system
description: Design tokens and component rules that give ClinicFlow a clean, consistent corporate-ERP look: colour, type, spacing, data tables, forms, status badges, cards, dialogs, toasts, and loading/empty/error states. Use whenever styling, creating a UI component, choosing colours, or reviewing visual consistency.
---

# ERP Design System

> Type: PLATFORM | Category: frontend | Risk: LOW | Security level: LOW | Status: DRAFT | Version: 1.0.0

## Purpose

One visual language for all three roles so the product feels like a single enterprise application.

## When to Use

- Creating or styling any component or screen.
- Reviewing UI consistency or accessibility.

## When NOT to Use

- Layout of the shell and breadcrumbs (`erp-app-shell-navigation`).
- Which fields a screen shows (`erp-screen-blueprints`).

## Inputs

Brand preferences (optional; default corporate palette provided).

## Preconditions

None.

## Responsibilities

- Tokens in `assets/tokens.css` (colour, type, spacing, radius, elevation).
- Component rules and status colour mapping in `references/tokens-and-components.md`.
- Accessibility baseline (WCAG 2.1 AA).

## Procedure

1. Import `assets/tokens.css` once at the app root.
2. Build components from tokens only; no raw hex values in components.
3. Use the status-badge map for appointment and prescription statuses (colour + icon + text).
4. Build the shared set: Button, Input, Select, DatePicker, DataTable, Badge, Card, Tabs, Dialog, Toast, Stepper, EmptyState, Skeleton, Alert, Watermark overlay.
5. Run the accessibility checklist per component.

## Decision Rules

- Dense ERP defaults: 14px body, 40px table rows, 4px spacing grid.
- Colour is never the only signal; every status has text and an icon.
- One primary button per page header; secondary actions are neutral.
- Patient screens may use the 'comfortable' density; clinic and pharmacy default to 'compact'.

## Outputs

`tokens.css`, component library, Storybook-style gallery (if stack allows).

## Dependencies

Hard dependencies (inputs): `clinicflow-product-spec`

## Tools

File read/write inside the project workspace, test runner, linter. No production network, secrets, or credential access.

## Security Requirements

No remote font/CDN loading of unreviewed assets; self-host fonts. Watermark text must also exist as real text for assistive technology.

## Validation

Contrast AA check on all token pairs; every status has text + icon; keyboard focus visible.

## Failure Handling

If an input is missing or marked `REQUIRES-CONFIRMATION` in `01-application-understanding.md`, state the assumption, proceed with a clearly labelled default, and list it in the handoff notes. Never invent policy values silently.

## Evaluation Criteria

Token-only styling; consistent status mapping; AA contrast; all state components exist.

Eval files: `evals/basic.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

Prescription status `Expired` renders an amber badge with a clock icon and the word 'Expired'.

## Limitations

Dark mode and localisation are out of scope until confirmed.

## Related Skills

- `clinicflow-product-spec`
- `erp-app-shell-navigation`
- `erp-screen-blueprints`
- `pharmacy-workspace`

