# 02 - Requirements (testable)

Priority from source scope table (Required unless noted). Owner = skill. G/W/T abbreviated.

| ID | Requirement | Acceptance criterion | Owner |
|---|---|---|---|
| PAT-REG-01 | Patient can self-register with personal, address, optional medical info | Valid form creates Pending account with auto Patient ID | patient-identity |
| PAT-REG-02 | Email verification before login | Login rejected until verified | patient-identity |
| PAT-REG-03 | Login/logout, forgot and reset password | Reset token single-use and expiring; generic response | patient-identity |
| PAT-REG-04 | Profile edit incl. email/mobile change | Change triggers re-verification | patient-identity |
| PAT-LNK-01 | Link account to clinic records only after identity verification | Name/email match alone never links | patient-identity |
| CLN-DIS-01 | Search by name, city, area, postal code, near me | Results sorted by distance when location allowed | clinic-discovery-booking |
| CLN-DIS-02 | Location denied still works | Text search usable, no error | clinic-discovery-booking |
| CLN-DIS-03 | Clinic card and details fields per source | All listed fields present or hidden if empty | clinic-discovery-booking / screens |
| APT-BOOK-01 | Book appointment via 5-step flow | Appointment ID generated, visible to patient and clinic | clinic-discovery-booking |
| APT-BOOK-02 | No two patients on same doctor+slot | Concurrent test: exactly one succeeds | clinic-discovery-booking |
| APT-BOOK-03 | Availability rechecked on save | Stale slot returns friendly conflict | clinic-discovery-booking |
| APT-CAN-01 | Cancel/reschedule per clinic rules; cancel releases slot | Slot bookable again after cancel | clinic-discovery-booking |
| APT-STA-01 | Seven appointment statuses with valid transitions | Illegal transition rejected | clinic-discovery-booking |
| CLN-OPS-01 | Clinic manages patients, doctors, schedules, dashboard | CRUD with clinic scoping | clinic-operations |
| CON-01 | Doctor records consultation from appointment | Finalised record immutable | consultation-history |
| CON-02 | Corrections via authorised workflow | Addendum with reason + audit | consultation-history |
| HIS-01 | Medical history newest first with prescription link | Order and links verified | consultation-history |
| HIS-02 | Patient-entered vs doctor-recorded distinguished | Source label visible | consultation-history |
| RX-01 | Doctor creates prescription with items, ID generated | Unique ID, Valid status | prescription-lifecycle |
| RX-02 | Statuses Valid/Expired/Cancelled; expiry shown | Day-after-expiry shows Expired | prescription-lifecycle |
| RX-03 | Patient views, copies ID, downloads/prints own prescriptions | Foreign ID returns 404 | prescription-lifecycle |
| RX-04 | Prescription ID emailed via SMTP, logged | EmailLogs row; failure retried | smtp-email |
| RX-VER-01 | Pharmacist verifies by 32-char ID after login | Valid shows details | pharmacy-workspace |
| RX-VER-02 | Expired -> EXPIRED watermark, dispensing blocked | Dispense disabled | pharmacy-workspace |
| RX-VER-03 | Cancelled -> CANCELLED watermark, dispensing blocked | Dispense disabled | pharmacy-workspace |
| RX-VER-04 | Every verification logged | PharmacyVerifications row | pharmacy-workspace |
| PHA-USR-01 | Pharmacy user management and basic reports | Scoped to pharmacy | pharmacy-workspace |
| SEC-01 | Ownership validated on backend for every request | IDOR sweep passes | access-control |
| SEC-02 | Audit of sensitive access/actions | Entries present | access-control |
| UX-01 | All screens inside shared ERP shell | Navigation test passes | app-shell-navigation |
| UX-02 | Breadcrumb on every page matching map | Route table test passes | app-shell-navigation |
| UX-03 | Role sidebar in source order; active state; responsive drawer | Visual + test | app-shell-navigation |
| UX-04 | Consistent tokens, statuses, states (loading/empty/error) | Review checklist | design-system / screens |
| UX-05 | Accessibility AA, keyboard, 360/768/1280 | axe + screenshots | qa-and-uat |
| ADM-01 | Super Admin dashboard (one) | KPIs link to filtered lists | platform-admin-console |
| ADM-02 | Clinic enrollment by Super Admin with first Clinic Admin invite | Pending -> Active on password set; audited | platform-admin-console |
| ADM-03 | Super Admin views patient masters read-only | Masked contacts, audited Reveal, no clinical data | platform-admin-console |
| ADM-04 | Super Admin reports | Aggregate only, date range | platform-admin-console |
| ADM-05 | SMTP settings by Super Admin | Password encrypted, write-only, test send | platform-admin-console / smtp |
| DOC-01 | Each doctor has own login and workspace | Scoped to own appointments/prescriptions | consultation-history / access-control |
| RX-ID-01 | Prescription ID is 32 chars, random, mixed-case alphanumeric, not guessable | Format/entropy/uniqueness tests; case-sensitive lookup | prescription-lifecycle |
| RX-ID-02 | ID never in URLs or logs | URL uses UUID; logs last 4 only | prescription-lifecycle / access-control |
| RX-DSP-01 | Partial fills allowed | Dispensed <= prescribed enforced by CHECK + lock | pharmacy-workspace |
| RX-DSP-02 | Fulfilment status Not/Partially/Fully Dispensed | Derived view correct | prescription-lifecycle |
| RX-DSP-03 | Fully dispensed blocks further dispensing | Dispense disabled with banner | pharmacy-workspace |
| PHA-DB-01 | Pharmacy tables | Migrations from `pharmacy-schema.sql` apply cleanly | pharmacy-workspace / architecture |
| ENV-01 | Java + SQL Server stack | Conventions applied | architecture |
| NFR-01 | Performance, availability targets | RC-01 | architecture / ops |
| OPT-01 | Interactive maps, payments, SMS | Out of release 1 | - |

Unmet requirements: none. Orphan skills: none (orchestrator is a coordinator; delivery-ops covers NFR-01).
