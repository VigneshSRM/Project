---
name: erp-app-shell-navigation
description: Build the corporate-ERP application shell for ClinicFlow: top bar, collapsible role-based sidebar, breadcrumb bar, page header, responsive behaviour and the route map for Super Admin, Clinic, Doctor, Patient and Pharmacy. Use whenever adding a page or route, changing navigation, implementing breadcrumbs, or when a user reports confusing navigation.
---

# ERP App Shell and Navigation

> Type: PLATFORM | Category: frontend | Risk: LOW | Security level: LOW | Status: DRAFT | Version: 1.1.0

## Purpose

Make navigation predictable and smooth: the user always knows where they are, how they got there, and how to go back.

## When to Use

- Creating the layout or any new route.
- Implementing or fixing breadcrumbs, sidebar, back behaviour.

## When NOT to Use

- Styling components (design system).
- Page content (screen blueprints).

## Inputs

Role-permission matrix (from access-control), route list, design tokens.

## Preconditions

Design system tokens exist; roles defined.

## Responsibilities

- One shared shell wrapping every authenticated page.
- Sidebar items and routes per role (`references/navigation-map.md`).
- Breadcrumb rules (`references/breadcrumb-spec.md`).
- Layout anatomy and responsive rules (`references/shell-layout.md`).

## Procedure

1. Implement the shell once; pages declare only `title`, `breadcrumbs[]`, `primaryAction`. Default on Java: Thymeleaf layout fragment + a `PageMeta` model object set by each controller (`references/java-thymeleaf-mapping.md`); a React SPA is the alternative (RC-18).
2. Render the sidebar from a role-keyed config; hide items the role cannot access and also enforce on the server.
3. Build the breadcrumb component from route metadata; resolve record names (e.g. clinic name) with a fallback to the ID.
4. Highlight the active sidebar item by route prefix so child pages keep the parent highlighted.
5. Add guards: unauthenticated -> `/login` with return URL; wrong role -> role home with a notice.
6. Add unsaved-changes guard on forms; preserve list filters when returning from a detail page.
7. Test every route against the navigation map (table-driven).

## Decision Rules

- Sidebar = modules (max one level, no nesting deeper than the module).
- Breadcrumbs = hierarchy, not history. Sub-pages (detail, edit, book) appear only in breadcrumbs.
- The last crumb is plain text and equals the page title.
- Never put PHI (names, diagnoses) in URLs; use opaque IDs. Breadcrumb may display the record's friendly name.
- Same positions and behaviours in all three roles.

## Outputs

Shell component, route config, breadcrumb component, navigation tests.

## Dependencies

Hard dependencies (inputs): `erp-design-system`, `healthcare-access-control-and-audit`

## Tools

File read/write inside the project workspace, test runner, linter. No production network, secrets, or credential access.

## Security Requirements

Client-side hiding is convenience only; every route and API enforces authorization server-side. Return URLs are validated to be same-origin.

## Validation

Every route in the map renders the correct breadcrumb and active item; keyboard and screen-reader landmarks work.

## Failure Handling

If a record name cannot be resolved, show the ID in the crumb; if the route has no metadata, fail the build in CI rather than rendering a blank crumb.

## Evaluation Criteria

Correct crumbs for every route; correct active state; back-navigation preserves filters; guards work.

Eval files: `evals/basic.yaml`. Status: DRAFT - cases are written but not yet executed.

## Examples

`/patient/appointments/APT-2026-00045` -> `Home > Appointments > APT-2026-00045`, sidebar 'Appointments' highlighted.

## Limitations

Global search and notifications centre are not in the source and are not included.

## Related Skills

- `erp-design-system`
- `erp-screen-blueprints`
- `healthcare-access-control-and-audit`

