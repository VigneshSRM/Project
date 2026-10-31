# 11 - Evaluation Strategy

Every skill has `evals/basic.yaml` (normal, edge/ambiguous, adversarial where relevant); the 8 skills below also have `evals/security.yaml`: access-control, platform-admin-console, patient-identity, consultation-history, prescription-lifecycle, pharmacy-workspace, delivery-ops, smtp-email.

## Dimensions
Trigger correctness · input handling · output quality · tool usage/trajectory · requirement adherence · security · failure handling · edge cases · regression.

## Cross-skill acceptance suites (owned by `clinicflow-qa-and-uat`)
1. E2E workflows A-D.
2. Route table test: title, breadcrumb, active nav item, back path for every route in `navigation-map.md`.
3. Role x endpoint matrix and IDOR sweep.
4. Concurrency (double booking; two pharmacies dispensing the last quantity).
5. Expiry boundary tests; Prescription ID format/entropy/case-sensitivity; Super Admin masking; Doctor scoping.
6. Accessibility (axe) and screenshots at 360/768/1280.
7. Email failure injection.

## Status
All evals are **written, not executed** -> all skills remain DRAFT until run. Suggested next step: hand any skill folder to `skill-creator` for trigger-accuracy and output evals.
