# 06 - Skill Taxonomy

17 skills (16 + `platform-admin-console`, added for the confirmed Super Admin role). Count is justified by domain and UI needs, not target size. Merges done: discovery+booking, consultation+history, pharmacy verification+users+reports, architecture+data. Gap-driven additions: `clinic-operations-and-masters` (clinic modules were otherwise unowned), `smtp-email-notifications` (shared by 3 domains).

| # | Skill | Type | Risk | Why it exists | Traces to |
|---|---|---|---|---|---|
| 1 | clinicflow-build-orchestrator | CORE | MEDIUM | The "develop the app" entry point; sequences and gates | all |
| 2 | clinicflow-product-spec | CORE | LOW | Testable requirements, open questions | 02 |
| 3 | clinicflow-architecture-and-data | ENGINEERING | MEDIUM | Structure + schema | NFR, tables |
| 4 | erp-design-system | PLATFORM | LOW | Corporate visual language | UX-04 |
| 5 | erp-app-shell-navigation | PLATFORM | LOW | Shell, sidebar, breadcrumbs, routes | UX-01..03 |
| 6 | erp-screen-blueprints | ENGINEERING | LOW | Per-screen specs and states | UX-04, all screens |
| 7 | healthcare-access-control-and-audit | SECURITY | HIGH | Ownership, auth, audit | SEC-01/02 |
| 8 | smtp-email-notifications | INTEGRATION | MEDIUM | Email delivery | RX-04, PAT-REG-02 |
| 9 | clinic-operations-and-masters | DOMAIN | MEDIUM | Clinic modules | CLN-OPS-01 |
| 10 | platform-admin-console | DOMAIN | HIGH | Super Admin: enrollment, patient view, reports, SMTP | ADM-* |
| 11 | patient-identity-and-linking | DOMAIN | HIGH | Registration, profile, linking | PAT-* |
| 12 | clinic-discovery-and-booking | DOMAIN | MEDIUM | Search + appointments | CLN-DIS, APT-* |
| 13 | consultation-and-medical-history | DOMAIN | HIGH | Clinical records + timeline + Doctor workspace | CON, HIS, DOC |
| 14 | prescription-lifecycle | DOMAIN | HIGH | Prescriptions, 32-char ID, fulfilment | RX-* |
| 15 | pharmacy-workspace | INDUSTRY | HIGH | Verification, partial dispensing, tables | RX-VER, RX-DSP, PHA |
| 16 | clinicflow-qa-and-uat | QUALITY | LOW | Evidence incl. UX tests | UX-05 |
| 17 | clinicflow-delivery-and-ops | OPERATIONS | HIGH | Release safety | NFR-01 |

Type counts: CORE 2, DOMAIN 6, INDUSTRY 1, PLATFORM 2, ENGINEERING 2, SECURITY 1, INTEGRATION 1, QUALITY 1, OPERATIONS 1.
Not created (no justified requirement): billing/POS-payments, inventory, maps, SMS, payments (all Optional or absent), META-skills (use `skill-creator`).
