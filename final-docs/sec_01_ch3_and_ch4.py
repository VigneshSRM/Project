"""
Chapter 3 (Modules & Detailed Module Descriptions) and Chapter 4 (Language,
Framework, Security, Servlet Container & SQL Server Database Specifications)
in exact SRMIST-DDE reference PDF format (matching Pages 23-53 of ref PDF).
"""


def populate_ch3_and_ch4(b):
    # ==========================================================================
    # CHAPTER 3: MODULES (Exact match to Pages 23-26 / ref_023.png)
    # ==========================================================================
    b.start_chapter(3, subtitle="MODULES")
    b.section("3.0", "MODULES", toc_label="3.0 MODULES", toc_indent=0.0,
              new_page=False, trailing_colon=False, render_heading=False)

    b.numbered_list([
        "Super Admin & Multi-Tenant Enrollment",
        "Identity, Auth Tokens & Security",
        "Clinic Administration & Patient Linking",
        "Doctor Schedule & Atomic Slot Booking",
        "Clinical Consultation & Signed Addenda",
        "Electronic Prescription (32-Char e-Rx)",
        "Pharmacy POS & Automated Finance Ledger",
        "Transactional Email Outbox & Audit Logging",
    ])

    b.section("3.1", "MODULE DESCRIPTION", toc_label="3.1 MODULE DESCRIPTION", toc_indent=14.0)

    # 3.1.1
    b.section("3.1.1", "Super Admin & Multi-Tenant Enrollment",
              toc_label="3.1.1 SUPER ADMIN & TENANT ENROLLMENT", toc_indent=28.0)
    b.p(
        "The Super Admin and Multi-Tenant Enrollment module in Infeara Medical System governs platform "
        "initialization, healthcare facility onboarding, pharmacy licensing verification, cross-clinic "
        "administrative oversight, and outbound mail infrastructure. Implemented across SetupController, "
        "SetupService, AdminController, AdminClinicService, AdminPharmacyService, AdminPatientService, "
        "AdminReportService, and SmtpSettingsService, it establishes the trust root of the entire deployment:"
    )
    b.p(
        "When the database contains zero user rows (userRepository.count() == 0), navigating to /setup "
        "renders a one-time Platform Owner wizard that validates password strength, creates the initial "
        "ACTIVE SUPER_ADMIN account, logs SETUP_COMPLETED in dbo.AuditLogs, and permanently locks /setup "
        "against all future invocations.",
        bold_prefix="Zero-Seed Platform Bootstrap:"
    )
    b.p(
        "Super Administrators enroll outpatient clinics (/admin/clinics) and retail dispensing pharmacies "
        "(/admin/pharmacies), recording facility addresses, contact metadata, and state drug license numbers "
        "(such as KL-12356H2-77) while automatically issuing 72-hour single-use STAFF_INVITE links to "
        "designated facility administrators.",
        bold_prefix="Clinic & Pharmacy Enrollment:"
    )
    b.p(
        "Super Administrators can transition any clinic or pharmacy between ACTIVE and SUSPENDED states, "
        "inspect live platform telemetry (/admin/reports), browse privacy-masked cross-clinic Patient Masters "
        "(/admin/patients where mobile and email strings are masked server-side), and configure STARTTLS/SSL "
        "SMTP credentials encrypted at rest using AES-256-GCM.",
        bold_prefix="Tenant Governance & Masked Oversight:"
    )

    # 3.1.2
    b.section("3.1.2", "Identity, Auth Tokens & Security",
              toc_label="3.1.2 IDENTITY, AUTH TOKENS & SECURITY", toc_indent=28.0)
    b.p(
        "The Identity, Auth Tokens, and Security module protects user authentication, credential recovery, "
        "staff onboarding, and role-segregated portal routing across all six RBAC actors. Engineered across "
        "SecurityConfig, LoginThrottleFilter, ClinicStatusFilter, AuthTokenService, TokenHasher, PasswordPolicy, "
        "RegistrationService, InvitationService, and EmailChangeService, it enforces defense-in-depth security:"
    )
    b.p(
        "Every password created during /setup, /register, /invite, or /reset-password is validated against "
        "PasswordPolicy (minimum 10 characters with at least one alphabetic letter and one numeric digit) "
        "and hashed irreversibly via Spring Security's BCryptPasswordEncoder.",
        bold_prefix="BCrypt Credential Hardening:"
    )
    b.p(
        "Verification links (24h TTL), password-reset links (60m TTL), staff invitations (72h TTL), and "
        "email-change confirmations (24h TTL) generate 256-bit SecureRandom URL-safe tokens. Only the 32-byte "
        "SHA-256 digest is stored in dbo.AuthTokens, and consumption executes under a pessimistic write lock "
        "(findForUpdate) so a link can never be replayed.",
        bold_prefix="Single-Use SHA-256 AuthToken Vault:"
    )
    b.p(
        "LoginThrottleFilter blocks brute-force password guessing after 5 failed attempts within 15 minutes "
        "per normalized email or client IP, while ClinicStatusFilter intercepts every /clinic/** and /doctor/** "
        "request and immediately invalidates the HTTP session if the underlying clinic is suspended.",
        bold_prefix="Brute-Force Throttling & Live Suspension Guard:"
    )

    # 3.1.3
    b.section("3.1.3", "Clinic Administration & Patient Linking",
              toc_label="3.1.3 CLINIC ADMIN & PATIENT LINKING", toc_indent=28.0)
    b.p(
        "The Clinic Administration and Patient Linking module empowers clinic reception desks and facility "
        "managers (/clinic/**) to register walk-in patients, resolve online patient account linkage requests, "
        "consolidate duplicate charts safely, and maintain standardized clinic drug formularies:"
    )
    b.p(
        "Reception staff register walk-in patients with formatted public identifiers (PAT-YYYY-NNNN), "
        "capturing demographics, blood group, known drug allergies, chronic conditions, and emergency contacts "
        "while automatically linking the chart to the active clinic via dbo.ClinicPatients.",
        bold_prefix="Walk-In Patient Master Management:"
    )
    b.p(
        "When a self-registered portal patient requests linkage to a clinic, ClinicLinkRequestService "
        "automatically scans existing clinic charts for matching Name and Date of Birth. Front-desk staff "
        "must verify physical government photo ID and check identityVerified=true before approving.",
        bold_prefix="Staff-Verified Identity Linking:"
    )
    b.p(
        "If the patient already possesses a prior walk-in chart with an identical Date of Birth, "
        "mergeIntoClinicRecord atomically transfers all Appointments, Prescriptions, and ClinicPatients "
        "rows onto the canonical chart, moves the User login reference, and stamps merged_into_patient_id "
        "on the retired row so historical AuditLogs foreign keys remain intact.",
        bold_prefix="Non-Destructive Patient Record Merge:"
    )
    b.p(
        "Clinic Administrators curate standardized clinic medicines (dbo.ClinicMedicines) specifying "
        "strength, dosage form, default frequency, duration, and quantity, with one-click RFC-4180 CSV "
        "bulk import and export (/clinic/inventory).",
        bold_prefix="Standardized Clinic Drug Formulary:"
    )

    # 3.1.4
    b.section("3.1.4", "Doctor Schedule & Atomic Slot Booking",
              toc_label="3.1.4 DOCTOR SCHEDULE & SLOT BOOKING", toc_indent=28.0)
    b.p(
        "The Doctor Schedule and Atomic Slot Booking module coordinates physician availability windows "
        "and patient/desk appointment reservations across SlotService, BookingService, ClinicDoctorService, "
        "ClinicAppointmentController, and PatientBookingController:"
    )
    b.p(
        "Clinic Administrators onboard doctors, link their practitioner logins via dbo.DoctorUserLinks, "
        "configure consultation fees, and define recurring weekday availability windows (start_time, end_time, "
        "slot_minutes, effective_from, effective_to) in dbo.DoctorSchedules.",
        bold_prefix="Weekday Availability Modeling:"
    )
    b.p(
        "SlotService dynamically computes candidate time slots for a requested date, subtracting past "
        "timestamps and any slot already occupied by an active appointment (BOOKED, CONFIRMED, or IN_CONSULTATION).",
        bold_prefix="Dynamic Candidate Slot Computation:"
    )
    b.p(
        "During booking or rescheduling, BookingService acquires a pessimistic database write lock on the "
        "Doctor row (doctorRepository.findForUpdate) and inserts the appointment under SQL Server's filtered "
        "unique index UX_Appt_Slot. Cancelled or no-show visits are excluded from UX_Appt_Slot, making freed "
        "slots immediately bookable by other patients.",
        bold_prefix="Dual-Guard Conflict-Free Reservation:"
    )

    # 3.1.5
    b.section("3.1.5", "Clinical Consultation & Signed Addenda",
              toc_label="3.1.5 CLINICAL CONSULTATION & ADDENDA", toc_indent=28.0)
    b.p(
        "The Clinical Consultation and Signed Addenda module (/doctor/appointments/{id}) provides practicing "
        "physicians with a medico-legally sound electronic encounter workspace implemented in "
        "DoctorConsultationController and DoctorConsultationService:"
    )
    b.p(
        "Doctors open checked-in encounters from their personal queue, review patient allergies and chronic "
        "conditions, and record clinical diagnoses, examination notes, and recommended follow-up dates in "
        "editable DRAFT status while the visit is underway.",
        bold_prefix="Draft Clinical Documentation:"
    )
    b.p(
        "Clicking Finalise transitions dbo.Consultations irreversibly to FINAL, stamps finalised_at in UTC, "
        "marks the parent appointment COMPLETED, and permanently locks diagnosis and primary notes against "
        "retrospective modification.",
        bold_prefix="Irreversible Encounter Finalization:"
    )
    b.p(
        "Any subsequent clinical clarification, lab-report note, or dosage remark is appended exclusively "
        "as an immutable dbo.ConsultationAddenda record storing author_user_id, reason, text, and UTC timestamp.",
        bold_prefix="Append-Only Signed Addenda:"
    )

    # 3.1.6
    b.section("3.1.6", "Electronic Prescription (32-Char e-Rx)",
              toc_label="3.1.6 ELECTRONIC PRESCRIPTION (E-RX)", toc_indent=28.0)
    b.p(
        "The Electronic Prescription module (PrescriptionService, DoctorPrescriptionController, "
        "ClinicPrescriptionController, and PatientPrescriptionController) eliminates paper prescription "
        "forgery and ambiguity through high-entropy cryptographic prescription identifiers:"
    )
    b.p(
        "PublicIdGenerator.prescriptionCode() draws 32 characters from a 32-symbol unambiguous uppercase "
        "alphanumeric alphabet (ABCDEFGHJKLMNPQRSTUVWXYZ23456789, omitting 0, O, 1, and I) using "
        "java.security.SecureRandom, yielding 160 bits of cryptographic entropy.",
        bold_prefix="160-Bit Unambiguous CSPRNG Code:"
    )
    b.p(
        "dbo.Prescriptions stores prescription_code as CHAR(32) COLLATE Latin1_General_100_BIN2, enforcing "
        "exact binary code-point case sensitivity so lowercase or altered strings are rejected immediately.",
        bold_prefix="Binary Case-Sensitive SQL Collation:"
    )
    b.p(
        "Each prescription line item (dbo.PrescriptionItems) captures medicine name, dosage, form, frequency, "
        "duration, chronic lifetime flag, prescribed quantity, and cumulative quantity_dispensed, while "
        "dbo.vw_PrescriptionFulfilment computes NOT_DISPENSED, PARTIALLY_DISPENSED, or FULLY_DISPENSED in real time.",
        bold_prefix="Granular Line-Item & Chronic Therapy Tracking:"
    )

    # 3.1.7
    b.section("3.1.7", "Pharmacy POS & Automated Finance Ledger",
              toc_label="3.1.7 PHARMACY POS & FINANCE LEDGER", toc_indent=28.0)
    b.p(
        "The Pharmacy Point of Sale and Automated Finance Ledger module (/pharmacy/pos, /pharmacy/prices, "
        "/pharmacy/finance) unifies prescription verification, retail counter billing, self-learning drug "
        "pricing, and financial accounting inside PharmacyService and PharmacyFinanceService:"
    )
    b.p(
        "Scanning or entering a 32-character Prescription ID executes binary case-sensitive lookup, checks "
        "cancellation, expiry_date, and remaining item balances, and logs an immutable dbo.PharmacyVerifications "
        "row containing the SHA-256 attempt_hash, client IP, and VerificationOutcome.",
        bold_prefix="Cryptographic Counter Verification:"
    )
    b.p(
        "When a valid prescription opens in the POS cart, IMS automatically pre-fills unit selling prices "
        "from dbo.PharmacyMedicinePrices. Any new or updated unit price entered during billing is saved "
        "automatically per pharmacy and medicine so repeat sales take seconds.",
        bold_prefix="Self-Learning Medicine Price Catalog:"
    )
    b.p(
        "Submitting a POS sale locks the prescription items (findForUpdate), blocks any quantity exceeding "
        "(quantity - quantity_dispensed), calculates exact DECIMAL(12,2) subtotal, discount, tax, total, "
        "tendered cash, and change due (RoundingMode.HALF_UP), generates a printable SALE receipt, and "
        "automatically inserts a receipt-linked INCOME entry into dbo.PharmacyFinanceEntries.",
        bold_prefix="Over-Dispense Guard & Automatic Finance Sync:"
    )

    # 3.1.8
    b.section("3.1.8", "Transactional Email Outbox & Audit Logging",
              toc_label="3.1.8 TRANSACTIONAL OUTBOX & AUDIT LOGGING", toc_indent=28.0)
    b.p(
        "The Transactional Email Outbox and Audit Logging module guarantees reliable notification delivery "
        "and tamper-evident regulatory traceability across all clinical and administrative operations:"
    )
    b.p(
        "Domain events (verification, password reset, staff invite, appointment confirmation/cancellation, "
        "and prescription issuance) write QUEUED rows with idempotency keys into dbo.EmailLogs inside the "
        "same database transaction. EmailDispatchWorker polls every 5 seconds and retries transient SMTP "
        "errors with 1-minute, 5-minute, and 15-minute exponential backoff.",
        bold_prefix="Resilient Outbox with Exponential Backoff:"
    )
    b.p(
        "AuditService records every authentication attempt, setup action, tenant status change, patient "
        "link/merge decision, consultation finalization, prescription action, and pharmacy dispense into "
        "dbo.AuditLogs with actor_user_id, actor_role, client IP, patient_id, resource_id, and outcome.",
        bold_prefix="Patient-Centric Compliance Audit Trail:"
    )

    # ==========================================================================
    # CHAPTER 4: LANGUAGE SPECIFICATIONS (Exact match to Pages 27-53 / ref_027.png)
    # ==========================================================================
    b.start_chapter(4)
    b.section("4.0", "LANGUAGE SPECIFICATIONS", toc_label="4.0 LANGUAGE SPECIFICATIONS", toc_indent=0.0)

    b.subheading("Java 27 Technology and the Java Platform")
    b.p(
        "Java technology serves as both the object-oriented programming language and the enterprise runtime "
        "platform powering the Infeara Medical System (clinicflow-app). Unlike languages compiled directly to "
        "a single processor instruction set, Java source files (.java) are compiled by javac into platform-neutral "
        "bytecode (.class) executed by the Java Virtual Machine (JVM) via Just-In-Time (JIT) native compilation. "
        "The Java language and runtime environment are characterized by the following core engineering attributes:",
        first_indent=36
    )
    for kw in [
        "Object-Oriented and Strongly Typed (Java Records, Sealed Hierarchies & Enums)",
        "Architecture Neutral and Portable Across Windows and Linux Servers",
        "High Performance via HotSpot Adaptive JIT Compilation",
        "Memory-Safe with Deterministic Automatic Garbage Collection",
        "Multithreaded Concurrency Support for High-Throughput Web Requests",
        "Built-In Cryptographic Architecture (java.security.SecureRandom & javax.crypto.Cipher)",
    ]:
        b.bullet(kw)

    b.p(
        "In Infeara Medical System, Java 27 language features directly improve clinical code safety and "
        "maintainability. Immutable Java record types model view DTOs, KPI cards (KpiCard), slot options "
        "(SlotService.DaySlots), and service outcomes (DecisionResult) without mutable boilerplate. Twenty-one "
        "strongly typed Java enum classes under com.clinicflow.domain.enums enforce compile-time exhaustiveness "
        "across switch expressions for roles, appointment states, consultation lifecycles, token purposes, "
        "verification outcomes, and email backoff schedules.",
        first_indent=36
    )

    b.subheading("Spring Boot 4.1.2 and Modular Monolith Architecture")
    b.p(
        "Spring Boot 4.1.2 builds atop the Spring Framework Inversion-of-Control (IoC) container to provide "
        "auto-configured enterprise infrastructure, declarative transaction management (@Transactional), "
        "Jakarta Bean Validation, scheduled background task execution (@EnableScheduling), and self-contained "
        "executable JAR packaging. Rather than dispersing domain invariants across loosely coupled scripts, "
        "IMS structures its 189 Java classes as a disciplined Modular Monolith under com.clinicflow—cleanly "
        "separating audit, common, config, domain, identity, notifications, security, and role-scoped web "
        "packages while executing cross-entity workflows inside single-database ACID transactions.",
        first_indent=36
    )
    b.p(
        "Declarative @Transactional boundaries in Spring ensure atomicity across multi-step clinical mutations. "
        "For example, when PharmacyService.dispense() updates cumulative dispensed quantities on "
        "dbo.PrescriptionItems, writes a dbo.PrescriptionDispenses receipt, upserts dbo.PharmacyMedicinePrices, "
        "and invokes PharmacyFinanceService to post an INCOME entry into dbo.PharmacyFinanceEntries, any "
        "validation or concurrency exception rolls back every SQL statement cleanly.",
        first_indent=36
    )

    b.subheading("Embedded Apache Tomcat Servlet Container")
    b.p(
        "Infeara Medical System embeds Apache Tomcat directly inside the Spring Boot runtime, listening on "
        "configurable HTTP/HTTPS connectors (PORT=8080 by default) without requiring external WAR deployment. "
        "Incoming browser requests enter Tomcat's Coyote HTTP connector, traverse the Servlet Filter pipeline "
        "managed by Spring Security's DelegatingFilterProxy, and dispatch through Spring's DispatcherServlet "
        "to @Controller handler methods. Session cookies are hardened with HttpOnly attributes, strict "
        "server-side session invalidation on logout or tenant suspension, and configurable timeout policies.",
        first_indent=36
    )

    b.subheading("Spring Security 6 and Cryptographic Primitives")
    b.p(
        "Spring Security 6 enforces authentication, authorization, session fixation protection, and Cross-Site "
        "Request Forgery (CSRF) defense across all 70+ HTTP endpoints in IMS. SecurityConfig defines strict "
        "URL-prefix authorization rules mapping /admin/** to ROLE_SUPER_ADMIN, /clinic/** to ROLE_CLINIC_ADMIN, "
        "/doctor/** to ROLE_DOCTOR, /patient/** to ROLE_PATIENT, and /pharmacy/** to ROLE_PHARMACY_ADMIN or "
        "ROLE_PHARMACIST, while routing unauthorized requests to a custom /error/403 view.",
        first_indent=36
    )
    b.p(
        "Three dedicated cryptographic mechanisms reinforce application security: (1) BCrypt adaptive salted "
        "hashing protects user login passwords in dbo.Users; (2) TokenHasher computes deterministic 32-byte "
        "SHA-256 digests over 256-bit SecureRandom tokens so raw verification, reset, and staff-invite tokens "
        "never touch disk; and (3) SecretCipher encrypts outbound SMTP relay passwords using 256-bit AES in "
        "Galois/Counter Mode (AES/GCM/NoPadding) with a fresh 12-byte random IV and 128-bit authentication tag.",
        first_indent=36
    )

    b.subheading("Thymeleaf 3.1 Server-Side Rendering, HTML5, CSS3 and ES6 JavaScript")
    b.p(
        "The presentation tier of IMS comprises 61 server-rendered HTML5 templates processed by Thymeleaf 3.1 "
        "and the Ultraq Layout Dialect. Shared master layouts—templates/layout/base.html for the authenticated "
        "ERP shell and templates/layout/auth.html for public authentication screens—render a cohesive Corporate "
        "Healthcare ERP visual language governed by 1,882 lines of custom CSS variables in static/css/app.css "
        "(#0F2744 dark-navy left sidebar, #1E4E8C primary actions, #0E8A8A clinical teal accents, and WCAG AA "
        "semantic status badges). By escaping dynamic expressions via th:text by default and embedding hidden "
        "_csrf synchronizer tokens into every HTML <form>, Thymeleaf neutralizes reflected/stored XSS and CSRF "
        "attacks while delivering sub-40 ms server render latency.",
        first_indent=36
    )

    b.subheading("Spring Data JPA, Hibernate 6 ORM and HikariCP JDBC Connectivity")
    b.p(
        "Java Database Connectivity (JDBC) provides the standardized Java API for executing parameterized SQL "
        "statements and transactions against relational database engines. Atop HikariCP—an ultra-low-latency "
        "JDBC connection pool—IMS layers Hibernate 6 ORM and 24 Spring Data JPA repository interfaces. "
        "Crucially, application.yml disables Open-Session-In-View (spring.jpa.open-in-view: false) so all "
        "relational queries execute predictably inside service-layer transactions without N+1 lazy-loading "
        "leaks during view rendering, and configures PhysicalNamingStrategyStandardImpl with hibernate.ddl-auto: "
        "validate so every JPA entity column is verified strictly against the SQL Server schema at startup.",
        first_indent=36
    )

    b.subheading("Flyway Migrations and Microsoft SQL Server 2022 Relational Database")
    b.p(
        "Microsoft SQL Server 2022 Express serves as the primary ACID relational database management system "
        "for Infeara Medical System, complemented by H2 in MSSQLServer compatibility mode (MODE=MSSQLServer) "
        "for fast offline automated testing. Nine sequential Flyway SQL scripts (V001__init_schema.sql through "
        "V009__pharmacy_pos_pricing.sql) provision 27 normalized relational tables and 1 SQL fulfilment view "
        "(dbo.vw_PrescriptionFulfilment) inside the dbo schema.",
        first_indent=36
    )
    b.p(
        "SQL Server's advanced T-SQL capabilities directly enforce clinical invariants at the storage engine "
        "level: CHAR(32) COLLATE Latin1_General_100_BIN2 enforces binary case-sensitive prescription lookup; "
        "the filtered unique index UX_Appt_Slot ON dbo.Appointments(doctor_id, slot_start) WHERE status NOT IN "
        "('CANCELLED','NO_SHOW') prevents double-booked slots while releasing cancelled appointments instantly; "
        "DECIMAL(12,2) and DECIMAL(18,2) guarantee exact monetary arithmetic across pharmacy POS billing and "
        "finance ledgers; and ROWVERSION columns provide hardware-atomic optimistic concurrency tracking.",
        first_indent=36
    )

    # Complete Relational Schema Summary Tables in clean B&W SRMIST style
    b.table(
        "Flyway Database Migration History (V001 – V009):",
        ["Migration", "Script File", "Relational Schema Objects Provisioned"],
        [
            ["V001", "V001__init_schema.sql", "Creates 24 core tables + view dbo.vw_PrescriptionFulfilment + UX_Appt_Slot + BIN2 Rx collation."],
            ["V002", "V002__identity_email_outbox.sql", "Adds subject, body, next_attempt_at to EmailLogs; adds UX_AuthTokens_token_hash."],
            ["V003", "V003__patient_link_request_resolution.sql", "Adds clinic_patient_record_id, resolved_by, and resolution_note to PatientLinkRequests."],
            ["V004", "V004__invites_contact_change_and_merge_marker.sql", "Adds Users.pending_email, Patients.merged_into_patient_id, and 4 AuthToken purposes."],
            ["V005", "V005__clinic_inventory.sql", "Creates dbo.ClinicMedicines formulary catalog with unique (clinic_id, medicine, dosage)."],
            ["V006", "V006__rename_inventory_unit_to_form.sql", "Renames ClinicMedicines.default_unit to default_form (Tablet, Capsule, Syrup, etc.)."],
            ["V007", "V007__prescription_item_lifetime.sql", "Adds lifetime BIT NOT NULL DEFAULT 0 column to dbo.PrescriptionItems."],
            ["V008", "V008__pharmacy_finance.sql", "Creates dbo.PharmacyFinanceEntries ledger table with INCOME / EXPENSE constraint."],
            ["V009", "V009__pharmacy_pos_pricing.sql", "Creates dbo.PharmacyMedicinePrices; adds POS sale columns to Dispenses; links Finance to dispense_id."],
        ],
        col_widths=[0.85, 2.25, 3.35]
    )

    b.table(
        "Complete 27-Table & 1-View SQL Server Schema Specification (dbo.*):",
        ["Table / View Name", "Primary Key & Key Constraints", "Core Columns & Clinical Responsibility"],
        [
            ["dbo.Clinics", "clinic_id BIGINT PK, UQ public_uuid", "name, registration_no, city, state, phone, email, status (ACTIVE/SUSPENDED), row_version"],
            ["dbo.ClinicEnrollments", "enrollment_id BIGINT PK, FK clinic_id", "submitted_by_email, licence_document, status (SUBMITTED/APPROVED/REJECTED), reviewed_by"],
            ["dbo.Users", "user_id BIGINT PK, UQ email, FK clinic_id", "password_hash (BCrypt), pending_email, role (6 RBAC roles), status, email_verified_at"],
            ["dbo.AuthTokens", "token_id BIGINT PK, UQ token_hash BINARY(32)", "user_id FK, purpose (VERIFY_EMAIL/RESET_PASSWORD/STAFF_INVITE/EMAIL_CHANGE), expires_at, used_at"],
            ["dbo.Patients", "patient_id BIGINT PK, UQ public_patient_id", "user_id UQ FK, merged_into_patient_id FK, full_name, dob, gender, mobile, allergies, conditions"],
            ["dbo.ClinicPatients", "clinic_patient_id BIGINT PK, UQ(clinic,patient)", "clinic_id FK, patient_id FK, clinic_mrn, linked_at, linked_by"],
            ["dbo.PatientLinkRequests", "link_request_id BIGINT PK, FK clinic & patient", "method, status (PENDING/APPROVED/REJECTED), clinic_patient_record_id, resolved_by, resolution_note"],
            ["dbo.Doctors", "doctor_id BIGINT PK, UQ public_uuid", "full_name, registration_no, specialization, qualification, status (ACTIVE/INACTIVE)"],
            ["dbo.ClinicDoctors", "clinic_doctor_id BIGINT PK, UQ(clinic,doctor)", "clinic_id FK, doctor_id FK, consultation_fee DECIMAL(10,2), active BIT"],
            ["dbo.DoctorUserLinks", "link_id BIGINT PK, UQ(user_id)", "user_id FK -> Users, doctor_id FK -> Doctors, clinic_id FK -> Clinics"],
            ["dbo.DoctorSchedules", "schedule_id BIGINT PK, CK weekday 1..7", "clinic_id, doctor_id, weekday, start_time, end_time, slot_minutes, effective_from/to"],
            ["dbo.Appointments", "appointment_id BIGINT PK, UX_Appt_Slot", "public_appointment_id (APT-YYYY-NNNNN), patient_id, clinic_id, doctor_id, slot_start/end, status"],
            ["dbo.Consultations", "consultation_id BIGINT PK, UQ appointment_id", "diagnosis, notes, follow_up, status (DRAFT -> irreversible FINAL), finalised_at, row_version"],
            ["dbo.ConsultationAddenda", "addendum_id BIGINT PK, FK consultation_id", "author_user_id FK, reason, text, created_at (immutable post-FINAL clinical clarifications)"],
            ["dbo.ClinicMedicines", "medicine_id BIGINT PK, UQ(clinic,med,dosage)", "medicine, dosage, default_form, default_frequency, default_duration, default_quantity, active"],
            ["dbo.Prescriptions", "prescription_id BIGINT PK, BIN2 CHAR(32) UQ", "prescription_code (32-char CSPRNG), consultation_id, patient_id, doctor_id, expiry_date, status"],
            ["dbo.PrescriptionItems", "item_id BIGINT PK, CK qty_dispensed <= qty", "prescription_id FK, line_no, medicine, dosage, form, frequency, lifetime BIT, quantity, quantity_dispensed"],
            ["dbo.vw_PrescriptionFulfilment", "SQL View grouped by prescription_id", "Computes NOT_DISPENSED, PARTIALLY_DISPENSED, or FULLY_DISPENSED dynamically from item balances"],
            ["dbo.Pharmacies", "pharmacy_id BIGINT PK, UQ licence_no", "name, licence_no, address, city, state, phone, email, status (PENDING/ACTIVE/SUSPENDED)"],
            ["dbo.PharmacyUsers", "pharmacy_user_id BIGINT PK, UQ(pharmacy,user)", "pharmacy_id FK, user_id FK, role_in_pharmacy (PHARMACY_ADMIN/PHARMACIST), status"],
            ["dbo.PharmacyVerifications", "verification_id BIGINT PK, attempt_hash", "pharmacy_id, verified_by, prescription_id NULL, outcome (VALID/NOT_FOUND/CANCELLED/EXPIRED/FULLY_DISPENSED)"],
            ["dbo.PharmacyMedicinePrices", "price_id BIGINT PK, UQ(pharmacy,medicine)", "unit_price DECIMAL(12,2) >= 0, updated_at (auto-remembered per-pharmacy POS catalog)"],
            ["dbo.PrescriptionDispenses", "dispense_id BIGINT PK, UQ verification_id", "pharmacy_id, prescription_id, dispense_status, subtotal, discount, tax_amount, total_amount, payment_method"],
            ["dbo.PrescriptionDispenseItems", "dispense_item_id BIGINT PK, FK dispense_id", "prescription_item_id FK, quantity_dispensed, unit_price DECIMAL(12,2), line_total DECIMAL(12,2)"],
            ["dbo.PharmacyFinanceEntries", "entry_id BIGINT PK, FK dispense_id NULL", "pharmacy_id FK, entry_type (INCOME/EXPENSE), amount DECIMAL(12,2), description, entry_date"],
            ["dbo.SmtpSettings", "smtp_id INT PK (=1 singleton)", "host, port, username, password_enc VARBINARY(512) (AES-256-GCM), security_mode, enabled"],
            ["dbo.EmailLogs", "email_id BIGINT PK, UQ idempotency_key", "template_code, recipient_email, subject, body, status (QUEUED/SENT/FAILED), attempts, next_attempt_at"],
            ["dbo.AuditLogs", "audit_id BIGINT PK, IX(patient_id,created_at)", "actor_user_id, actor_role, action_code, resource_type, resource_id, patient_id, outcome, ip_address"],
        ],
        col_widths=[1.45, 1.75, 3.25]
    )

    b.subheading("Java Virtual Machine (JVM) Execution Pipeline and Memory Architecture")
    b.p(
        "The Java Virtual Machine (JVM) provides the deterministic, hardware-independent execution substrate "
        "upon which Infeara Medical System runs in development, continuous integration, and production clinic "
        "deployments. When the Maven compiler (javac) builds the 189 source files under com.clinicflow, each "
        "Java class, record, and enum is compiled into verified .class bytecode instructions packaged inside "
        "an executable Spring Boot JAR archive. At runtime, the HotSpot JVM processes these classes through "
        "four coordinated subsystems:",
        first_indent=36
    )
    b.bullet("ClassLoader Subsystem:",
             "Loads application, Spring Boot, Hibernate, and JDBC driver classes dynamically through a "
             "delegation hierarchy (Bootstrap, Platform, and Application ClassLoaders) while performing "
             "strict bytecode verification to prevent illegal pointer casts, stack overflows, or access-modifier "
             "violations before any clinical logic executes.")
    b.bullet("Runtime Data Areas (Heap, Metaspace, and Thread Stacks):",
             "Stores short-lived HTTP request DTOs, Thymeleaf model maps, and JPA entity snapshots inside "
             "generational Heap regions, class metadata in native Metaspace, and per-request method activation "
             "frames in isolated Java thread stacks so concurrent clinic receptionists, doctors, and pharmacists "
             "never corrupt one another's local execution state.")
    b.bullet("Tiered Just-In-Time (JIT) Compilation (C1 & C2):",
             "Profiles hot clinical code paths—such as 32-character prescription verification, slot availability "
             "generation, and BigDecimal POS tax calculation—and compiles frequently executed bytecode directly "
             "into optimized x86-64/ARM64 machine instructions.")
    b.bullet("Garbage-First (G1GC) Low-Pause Memory Collector:",
             "Reclaims short-lived request allocations concurrently in millisecond pauses, ensuring predictable "
             "sub-100 ms HTTP response latencies across simultaneous multi-clinic consultations and pharmacy "
             "counter billing peaks.")

    b.subheading("Java Cryptography Architecture (JCA) in Infeara Medical System")
    b.p(
        "Because Infeara Medical System handles Protected Health Information (PHI), single-use onboarding "
        "credentials, e-prescription tokens, and outbound SMTP relay secrets, it relies directly on vetted "
        "primitives from the standard Java Cryptography Architecture (java.security and javax.crypto) rather "
        "than ad-hoc encoding schemes:",
        first_indent=36
    )
    b.bullet("CSPRNG Entropy (java.security.SecureRandom):",
             "PublicIdGenerator.prescriptionCode() draws cryptographically strong random indices over a "
             "31-character unambiguous uppercase alphabet (23456789ABCDEFGHJKMNPQRSTUVWXYZ, omitting visually "
             "confusable 0/O and 1/I/L) to produce 32-character case-sensitive prescription codes carrying "
             "approximately 158.5 bits of entropy. AuthTokenService similarly generates 32-byte (256-bit) "
             "Base64URL-encoded tokens for email verification, password reset, staff invites, and email changes.")
    b.bullet("One-Way Cryptographic Token Hashing (java.security.MessageDigest SHA-256):",
             "TokenHasher computes deterministic 32-byte SHA-256 digests stored in BINARY(32) columns "
             "(dbo.AuthTokens.token_hash and dbo.PharmacyVerifications.attempt_hash). Even in the event of a "
             "read-only database backup leak, an attacker cannot reconstruct active invitation or password-reset "
             "URLs from stored digests.")
    b.bullet("Authenticated Symmetric Encryption (javax.crypto.Cipher AES/GCM/NoPadding):",
             "SecretCipher encrypts outbound SMTP credentials stored in dbo.SmtpSettings.password_enc using "
             "256-bit AES in Galois/Counter Mode (GCM) with a freshly generated 12-byte (96-bit) random "
             "initialization vector prepended to each ciphertext and a 128-bit authentication tag that detects "
             "any bit-level tampering prior to decryption.")
    b.bullet("Adaptive Password Hashing (Spring Security BCrypt):",
             "PasswordEncoderFactories.createDelegatingPasswordEncoder() hashes all user passwords using salted "
             "BCrypt ({bcrypt}$2a$10$...), incorporating a unique 128-bit cryptographic salt per user and "
             "exponential key-stretching cost to resist GPU dictionary and rainbow-table attacks.")

    b.subheading("Jakarta Servlet 6.1 and Spring MVC Request Processing Lifecycle")
    b.p(
        "Every browser interaction in Infeara Medical System traverses a deterministic, layered Jakarta Servlet "
        "6.1 and Spring Web MVC pipeline hosted inside embedded Apache Tomcat 11. Understanding this end-to-end "
        "request lifecycle clarifies how security, tenant isolation, validation, database transactions, and "
        "HTML rendering cooperate seamlessly:",
        first_indent=36
    )
    b.bullet("Step 1 — Security & Rate-Limit Filter Chain:",
             "Incoming HTTP requests first pass through LoginThrottleFilter (per-email+IP sliding-window brute-force "
             "limiter on POST /login), CsrfFilter (synchronizer token validation on all state-mutating POST forms), "
             "UsernamePasswordAuthenticationFilter (ClinicUserDetails authentication), and ClinicStatusFilter "
             "(real-time check ensuring the authenticated staff member's clinic has not been SUSPENDED).")
    b.bullet("Step 2 — DispatcherServlet & Handler Mapping:",
             "Spring's central DispatcherServlet routes the authenticated request to one of the role-scoped "
             "@Controller classes (/admin/**, /clinic/**, /doctor/**, /patient/**, or /pharmacy/**) while "
             "enforcing URL-level RBAC matchers configured in SecurityConfig.")
    b.bullet("Step 3 — Bean Validation & Transactional Domain Service:",
             "Jakarta Bean Validation (@Valid, @NotBlank, @Email, @Min, @Max, @DecimalMin) validates form DTOs "
             "before invoking @Transactional service methods. Service methods derive clinic_id, doctor_id, "
             "patient_id, or pharmacy_id strictly from the server-side SecurityContext principal rather than "
             "client-supplied hidden inputs.")
    b.bullet("Step 4 — Hibernate/JDBC Persistence & Outbox Recording:",
             "Within a single ACID database transaction, Hibernate 6 flushes entity state changes alongside "
             "append-only dbo.AuditLogs records and dbo.EmailLogs outbox rows, guaranteeing atomicity between "
             "clinical state transitions and their compliance trail.")
    b.bullet("Step 5 — GlobalModelAdvice & Thymeleaf SSR View Rendering:",
             "For GET requests (or following a Post-Redirect-Get 302 redirect after POST mutations), "
             "GlobalModelAdvice injects role navigation metadata and flash banners before ThymeleafViewResolver "
             "renders the target HTML5 template with automatic contextual XSS escaping.")

    b.subheading("HTML5, CSS3 Custom Design System and Progressive Enhancement")
    b.p(
        "The user interface of Infeara Medical System is engineered around semantic HTML5 landmarks, Bootstrap "
        "5.3 responsive grid utilities, and 1,882 lines of bespoke design-system rules in static/css/app.css "
        "tailored specifically for high-density clinical and pharmacy workstations:",
        first_indent=36
    )
    b.bullet("Corporate Healthcare ERP Visual Hierarchy:",
             "A fixed 260 px dark-navy sidebar (#0F2744) anchors role-specific navigation, paired with a sticky "
             "60 px topbar displaying breadcrumb context, tenant badge, and sign-out controls over a calm cool-slate "
             "workspace (#F4F6F9) with crisp white (#FFFFFF) card surfaces and #DDE3EC hairline borders.")
    b.bullet("WCAG AA Semantic Clinical Status Badges:",
             "Appointments, consultations, prescriptions, and link requests render consistent high-contrast pill "
             "badges—Emerald (#0D7A5F) for ACTIVE/VALID/FINAL/APPROVED, Amber (#B45309) for PENDING/DRAFT/PARTIAL, "
             "and Crimson (#B91C1C) for SUSPENDED/CANCELLED/EXPIRED/REJECTED—always pairing color with explicit "
             "uppercase text labels.")
    b.bullet("Progressive Vanilla ES6 Enhancements (static/js/app.js):",
             "Core workflows operate reliably over pure HTML forms, while lightweight unobtrusive JavaScript "
             "enhances dynamic prescription line-item addition/removal, clinic medicine formulary autofill, "
             "one-click 32-character Rx code clipboard copying, and real-time POS line-total, discount, tax, "
             "and grand-total arithmetic on the pharmacist billing screen.")

    b.subheading("Apache Maven 3.9 Build Lifecycle and Environment Profiles")
    b.p(
        "Infeara Medical System is structured as a reproducible Apache Maven 3.9 project governed by "
        "clinicflow-app/pom.xml and four environment-specific Spring configuration files so developers, "
        "automated CI pipelines, and production servers share identical dependency versions:",
        first_indent=36
    )
    b.table(
        "Spring Boot Environment Profiles & Configuration Files in IMS:",
        ["Configuration File", "Active Profile", "Database, Security & Runtime Behavior"],
        [
            ["application.yml", "default (MSSQL)", "Connects to SQL Server 2022 via HikariCP, enables Flyway V001–V009, sets hibernate.ddl-auto=validate, disables open-in-view, configures HttpOnly/SameSite=Lax session cookies and AES-GCM secret key."],
            ["application-h2.yml", "h2 (Local Demo)", "Configures in-memory H2 in MODE=MSSQLServer; disables Flyway BIN2-specific T-SQL scripts and initializes schema cleanly via Hibernate for zero-dependency local evaluation."],
            ["application-test.yml", "test (JUnit / MockMvc)", "Isolated H2 test database per test suite with deterministic test AES/remember-me keys and fast scheduler intervals for integration verification."],
            ["application-prod.yml", "prod (Production)", "Enforces TLS secure cookies (server.servlet.session.cookie.secure=true), disables Thymeleaf cache-miss debug leaks, and requires externalized environment secrets."],
        ],
        col_widths=[1.65, 1.35, 3.45]
    )

