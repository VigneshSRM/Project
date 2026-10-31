# IMS — Infeara Medical System

Spring Boot 4.1.2 modular monolith on Java 27 with Thymeleaf, Spring Security, Spring Data JPA,
Flyway and Microsoft SQL Server 2022 Express.

## Run

SQL Server profile (default):

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-27"
$env:PATH = "$env:JAVA_HOME\bin;D:\JAVA_APP\PROJECT\tools\apache-maven-3.9.11\bin;$env:PATH"
cd D:\JAVA_APP\PROJECT\clinicflow-app
mvn spring-boot:run
```

SQL Server TCP/IP and SQL Server Browser must be enabled for the JDBC driver. The default
profile now connects to the local `SQLEXPRESS` instance when those are enabled (see
`../db/README.md`). For a zero-setup local run, use the H2 profile:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=h2"
```

Open http://localhost:8080/setup to create the first Super Admin. The H2 profile keeps its
data in `clinicflow-app/data/clinicflow.*`, so it no longer resets on every start, and it
logs verification/reset links as `DEV EMAIL ...` in the console.

## Test

```powershell
mvn -B test
```

Tests use JUnit 5, AssertJ and Mockito; no database or SMTP server is required.

## First-time setup

IMS ships without demo accounts or seed data. On a fresh database, open
`http://localhost:8080/setup` and create the first Super Admin. That account can then
enroll clinics and pharmacies and invite their administrators.

## Local test accounts used for browser validation

These accounts were created through the real application flows during local validation.
They may not exist in a fresh SQL Server database; recreate them through `/setup`,
clinic/pharmacy enrollment and registration. Do not use them in a shared deployment;
change or remove them before any non-local use.

| Role | Email | Password |
|---|---|---|
| Super Admin | owner@clinicflow.test | OwnerPass123 |
| Clinic Admin | clinicadmin@riverside.test | ClinicPass123 |
| Doctor | doctor@riverside.test | DoctorPass123 |
| Patient | alice.new@example.test | PatientPass123 |
| Pharmacy Admin | pharmacyadmin@centralcare.test | PharmacyPass123 |

## Account linking

1. The patient signs in, opens **My Profile** and requests a link to an active clinic.
2. The request appears in that clinic's **Link Requests** queue for a clinic administrator.
3. The administrator verifies the patient's identity and either approves the account's own patient
   record or enters an existing clinic patient ID to move the account onto that record.
4. A transfer preserves appointments, prescriptions, clinic links and link-request history. The
   old account patient row is retained without a login so audit entries remain valid.
5. Every request, approval and rejection is audited. No link is created from a name or email alone.

The required identity-verification checkbox prevents accidental approval. A duplicate warning lists
clinic records with the same name and date of birth, and a different date of birth is rejected.
Clinic staff can also search the clinic's **Patient Masters** records directly when choosing an
existing patient ID for a merge.

## Pharmacy POS and finance

The pharmacy workspace includes a billing-style point of sale at `/pharmacy/pos`.

1. Scan or type the 32-character Prescription ID and choose **Analyze Prescription**.
2. The POS builds a sale cart from the prescription lines, pre-filling quantities and unit prices.
3. Unit prices come from the pharmacy **Price List** (`/pharmacy/prices`) and are remembered after
   every sale, so repeat dispensing is fully automatic.
4. Adjust quantities, price, discount or tax; choose Cash, Card, UPI, Insurance or Other; optionally
   enter the amount tendered to calculate change.
5. **Complete sale & dispense** records the dispense, stores the sale totals and payment method on
   the receipt, and posts the total to **Finance** as income automatically. The finance entry is
   linked to the receipt, so it can be opened from the ledger.

The **Finance** page shows income and expenses for today and the last 30 days, a net total, manual
income/expense entry, and the full ledger with POS-sale references. Expenses are manual; POS income
is never entered by hand.

## Configuration

Database and SMTP values can be overridden with environment variables:

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`,
`SMTP_PASSWORD`, `REMEMBER_ME_KEY`, `PORT`.

Defaults point at `localhost\SQLEXPRESS`, database `ClinicFlow_Dev`, login `SA`
(development credentials only - see `.opencode/skill/Passwords.txt`). There is no seed
profile; clinics, pharmacies, doctors, patients and staff are created through the
application.

## Implemented in this increment

- Maven/Spring Boot project skeleton, Flyway migrations, JPA entities for all 24 tables.
- Repository layer for every aggregate.
- Session login with BCrypt (delegating encoder), role-based authorization and redirects.
- Shared corporate ERP shell: top bar, role sidebar, breadcrumb bar, page header, design tokens.
- Role dashboards for Super Admin, Clinic, Doctor, Patient and Pharmacy with KPI cards and activity.
- Navigable module placeholders for every route in the navigation map (no dead ends).
- No seed data or demo accounts: `/setup` bootstraps the first Super Admin, and clinics,
  pharmacies, doctors, schedules, patients and staff are enrolled or registered through the
  application.
- Patient self-registration in Personal / Address / Optional medical groups, unique email checks and
  policy validation (10+ characters with a letter and a number).
- Single-use email verification (24-hour TTL), resend, login blocking until verified.
- Password reset with generic responses, single-use 60-minute tokens and password update.
- DB-backed email outbox with retry/backoff, `EmailLogs` subject/body/next-attempt columns and a
  dev log mode; SMTP delivery is selected with `MAIL_MODE=smtp`.
- Login throttling and public-auth rate limiting, audit entries for registration, verification,
  password reset, login success/failure and profile updates.
- Patient My Profile: editable personal/address/optional medical data, read-only verified identity
  fields, linked-clinic panel and link-request history. Ownership is always resolved from the
  session user, never from a client-supplied patient ID.
- Clinic staff approval linking: patient request from My Profile, clinic-scoped request queue,
  approve/reject with a required identity-verification step, duplicate warnings, resolution notes,
  merge onto an existing clinic record (appointments, prescriptions, clinic links and request
  history preserved) and audit entries (`PATIENT_LINK_REQUESTED`, `PATIENT_LINK_APPROVED`,
  `PATIENT_RECORD_MERGED`, `PATIENT_LINK_REJECTED`).
- Unit and web tests for public ID generation, password policy, token hashing, role home routing,
  patient-profile ownership/audit, profile rendering/saving, linking ownership/approval/rejection/
  merge and role-based access denial (`25` tests).

## Completed in this increment

1. Auth tokens, email verification, password reset, password policy and login throttling.
2. Patient profile, account linking with merge, and role-based access control.
3. Super Admin clinic/pharmacy enrollment, platform reports and SMTP settings.
4. Clinic masters (patients, doctors, schedules), appointment desk and booking wizard.
5. Consultation draft/finalise/addenda and 32-character prescriptions with expiry.
6. Pharmacy verification, partial dispensing, over-dispense protection and reports.
7. Email change verification, clinic suspension enforcement and role-access denial pages.
8. Browser end-to-end validation of registration, linking, booking, consultation,
   prescription, dispensing, reports, cancellation, rescheduling and profile updates.
