# 01 - Application Understanding

Tags: **U** = USER-PROVIDED, **I** = INFERRED, **RC** = REQUIRES-CONFIRMATION.
Working name "ClinicFlow" is **I** (rename freely).

## Interpretation of the request
"Skill for developing the application with clear UI and UX, smooth navigation, clear breadcrumbs, and like a corporate ERP application". "bar grimps" read as **breadcrumbs** (**I**). Result: a 17-skill library (v1.1) whose entry point is `clinicflow-build-orchestrator` and whose UI/UX layer (design system, app shell + navigation + breadcrumbs, screen blueprints) is a hard Definition-of-Done gate.

## Application model
| Item | Finding | Tag |
|---|---|---|
| Purpose | Simple three-role web app connecting patients, clinics, pharmacies through a digital healthcare workflow; demonstrates the flow without a full HIS | U |
| Context | "College project" | U |
| Roles | Clinic, Pharmacy, Patient (source) + Super Admin, Doctor (confirmed) | U |
| Further roles | Platform admin, clinic admin/staff/doctor, pharmacy admin/pharmacist | RC-02/03/04 |
| Industry | Healthcare / pharmacy workflow | U |
| Domains | See `03-domain-model.md` (6) | I |
| Workflows | A registration+booking, B consultation+prescription, C pharmacy verification, D medical history | U |
| Entities | 12 tables listed in source + inferred supporting tables | U/I |
| Business rules | No double booking; revalidate slot on save; ownership validated on backend; expired/cancelled block dispensing; no auto-link by name/email; clinical records not patient-editable | U |
| State transitions | Appointment (7 statuses), Prescription (Valid/Expired/Cancelled) | U |
| Integrations | SMTP email (U); browser geolocation (U, optional); maps, payments, SMS are Optional/out of scope | U |
| Authentication | Email+password, email verification, forgot/reset | U |
| Authorization | Role-based; patient sees only own data; pharmacy "when authorized" | U / RC-05 |
| Compliance | Health data is sensitive; specific regime and consent model unstated | RC-13 |
| Reporting | "Basic Reports" in pharmacy; content unstated | RC-12 |
| Notifications | Prescription ID by email; verification email | U |
| Background processing | Email queue/retry | I |
| Offline | Not mentioned | - |
| Performance / scale / availability | Not mentioned (college scale implied) | RC-01 |
| Observability / deployment | Not mentioned | RC-01 |
| Tech stack | **Java + Microsoft SQL Server** (confirmed) | U |
| Hosting | Not mentioned | RC-01 |
| UI/UX | Clear UI, smooth navigation, breadcrumbs, corporate ERP style | U (this request) |

## Decisions received (see `15-decisions-and-changes.md`)
| ID | Decision | Effect |
|---|---|---|
| D-01 | Tech stack: **Java + Microsoft SQL Server** | RC-01 partly resolved (framework, hosting, scale, time zone still open) |
| D-02 | **Super Admin** role: Dashboard, Clinic Enrollment, view Patient Masters, Reports, SMTP Settings | RC-02 resolved; new skill `platform-admin-console` |
| D-03 | **Pharmacy tables planned** | RC-04 resolved (`pharmacy-schema.md/.sql`) |
| D-04 | **Doctor login** (own account per doctor) | RC-03 resolved; Doctor role + nav |
| D-05 | **Partial fills allowed** | RC-06 resolved; dispense tables, fulfilment status |
| D-06 | **Prescription ID: 32 chars, random, mixed complex, not guessable** | RC-08 resolved; alphanumeric mixed-case, case-sensitive collation |

## Open REQUIRES-CONFIRMATION register
| ID | Question | Default used meanwhile | Blocks |
|---|---|---|---|
| RC-01 | Hosting, scale, display time zone, languages (stack is decided) | Local/dev only | ops |
| RC-05 | Pharmacy-visible fields (diagnosis?) | Hide diagnosis | pharmacy-workspace |
| RC-07 | Default prescription validity period | Clinic/doctor-set per prescription | prescription-lifecycle |
| RC-09 | Account-link proof method | One-time code to contact on clinic record + DOB, or clinic approval | patient-identity |
| RC-10 | OTP vs link; password policy; token expiry; SMS | Single-use link | patient-identity, smtp |
| RC-11 | Slot length, horizon, cancel cut-off, patient overlap rule | Proposed in skill, labelled | booking |
| RC-12 | Clinic dashboard widgets; report contents | Proposed in skills | clinic-operations, admin, pharmacy |
| RC-13 | Compliance regime, consent, retention | Technical controls only | access-control, ops |
| RC-14 | Consultation types list | General, Follow-Up | consultation-history |
| RC-15 | Prescription download format | PDF | prescription-lifecycle |
| RC-16 | Clinic coordinates source; distance calc location | Manual entry; app-level Haversine | clinic-operations, booking |
| RC-17 | Notification bell / global search | Omitted | app-shell |
| RC-18 | UI approach on Java: Thymeleaf (default) vs React SPA | Thymeleaf | app-shell, screens |
| RC-19 | Who enrolls pharmacies (Super Admin scope lists clinics only) | Planned `Pharmacies` table; enrollment path undecided | pharmacy-workspace, admin |
| RC-20 | Partial-fill rules: continue at other pharmacies? verification window? void policy? | Yes; 30 min; pharmacy admin void with reason | pharmacy-workspace |
| RC-21 | "One doctors login" read as one login per doctor; multi-clinic doctors | One clinic per doctor login | clinic-operations, access-control |
| RC-22 | Prescription ID alphabet (symbols?), QR/scanner support | Letters+digits, paste/scan-ready | prescription, pharmacy |

## Gaps in the source document
1. Admin/onboarding role - **resolved (D-02)**.
2. `Pharmacies` table - **resolved (D-03)**.
3. Doctor login - **resolved (D-04)**.
4. Dispensing state - **resolved (D-05)**.
5. Guessable Prescription IDs - **resolved (D-06)**; rate limiting still mandatory.
6. Consultation navigation: opens from the Doctor's Appointment detail.
7. Patient-to-clinic association and audit tables: added as inferred.
8. Pharmacy enrollment path remains open (RC-19).
