# 13 - Implementation Plan (UI-first vertical slice)

| Phase | Deliverable | Skills | Exit criteria |
|---|---|---|---|
| 0 | Stack decided (Java + SQL Server); confirm RC-18, RC-19, RC-20 | spec | Framework defaults accepted |
| 1 | Repo, CI skeleton, tokens, base components | architecture, design-system | Gallery of components renders with tokens |
| 2 | Auth + roles + audit, email service | access-control, smtp | Login for 3 roles, verification mail in catcher |
| 3 | **App shell**: top bar, sidebar per role, breadcrumbs, page header, guards, empty dashboards | app-shell | Navigation test passes for all stub routes |
| 4 | Super Admin console: enrollment, SMTP settings, dashboard; clinic masters, doctors + doctor logins | platform-admin-console, clinic-operations, screens | Super Admin enrolls clinic; Clinic Admin creates doctor with login |
| 5 | Patient registration, profile, linking | patient-identity | Workflow A first half |
| 6 | Find clinics + booking wizard + clinic appointment queue | booking | Workflow A complete, concurrency test passes |
| 7 | Consultation + history | consultation-history | Workflow D |
| 8 | Prescriptions (32-char ID) + email | prescription-lifecycle | Workflow B, ID tests pass |
| 9 | Pharmacy verification, partial dispensing, users, reports | pharmacy-workspace | Workflow C+G incl. watermarks and over-dispense race test |
| 10 | Dashboards, polish, accessibility, responsive | screens, qa | UX Definition of Done on all screens |
| 11 | UAT per role, staging, release | qa, ops | Human-approved release |

Rule: the shell (phase 3) precedes any feature screen, so nothing is built outside the ERP layout.
