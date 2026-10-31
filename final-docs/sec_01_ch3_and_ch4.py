"""
Chapter 3 (System Analysis) + Chapter 4 (System Design, 11 Architectural/UML Diagrams,
and Full 27-Table SQL Server 2022 Relational Data Dictionary).
"""
import os


def populate_ch3_and_ch4(b, assets_dir):
    # ==========================================================================
    # CHAPTER 3: SYSTEM ANALYSIS
    # ==========================================================================
    b.start_chapter(3, "SYSTEM ANALYSIS")

    b.h2("3.1", "Existing System Analysis")
    b.p(
        "In conventional outpatient healthcare environments across towns and metropolitan neighborhoods, clinical "
        "administration, doctor consultations, patient record-keeping, and retail pharmacy dispensing operate "
        "through disconnected manual or semi-digital workflows. Typically, a clinic reception desk maintains a "
        "local spreadsheet or standalone desktop billing register where returning patients are looked up by phone "
        "number or entered anew when staff cannot immediately locate an earlier chart. Doctor schedules are managed "
        "informally through wall calendars or reception phone logs, without automated slot-length calculation or "
        "real-time online booking synchronization."
    )
    b.p(
        "During clinical consultations, physicians either write diagnoses and medication regimens on paper pads "
        "or type free-text notes into unversioned local forms that permit unrestricted post-hoc overwriting. Once "
        "the patient exits the consulting room, the prescription travels exclusively as a physical slip or static "
        "WhatsApp/PDF image. At the retail pharmacy counter, the pharmacist visually inspects the slip, manually "
        "types medicine names and quantities into an isolated billing utility, searches paper price lists or memory "
        "for unit rates, computes discounts and taxes by hand, and at the close of business transcribes the day's "
        "cash and UPI totals into a separate accounting ledger."
    )

    b.h2("3.2", "Limitations of Existing System")
    b.p(
        "A systematic root-cause evaluation of the existing workflow exposes critical clinical, operational, "
        "security, and financial deficiencies:"
    )
    b.bullet("Fragmented Multi-Clinic Medical History:",
             "Patients visiting multiple clinics possess no unified longitudinal view of past diagnoses, follow-up "
             "dates, allergies, or chronic lifetime medications.")
    b.bullet("Duplicate Walk-In vs. Online Patient Charts:",
             "Without a staff-verified identity linking and non-destructive merge workflow, walk-in registrations "
             "remain permanently separated from online patient accounts—or worse, naive auto-merging exposes "
             "records to unauthorized individuals sharing a name or phone number.")
    b.bullet("Frequent Appointment Double-Bookings:",
             "Concurrent phone and counter bookings routinely assign two patients to the same doctor time slot due "
             "to the absence of database-level slot locking and filtered unique constraints.")
    b.bullet("Lack of Clinical Immutability and Auditability:",
             "Overwritable clinical notes leave zero forensic trail of who altered a diagnosis or when, violating "
             "fundamental medico-legal accountability principles.")
    b.bullet("Prescription Forgery, Illegibility, and Over-Dispensing:",
             "Handwritten prescriptions cause transcription errors at pharmacies and provide zero technical guard "
             "against expired, cancelled, or already-dispensed prescriptions being filled multiple times.")
    b.bullet("Slow Manual Pharmacy Pricing and Cashbook Drift:",
             "Re-entering medicine unit prices on every sale slows counter throughput, while manual end-of-day "
             "posting into finance books introduces arithmetic errors and unlinked receipts.")
    b.bullet("Weak Credential Hygiene and Absent Tenant Controls:",
             "Legacy clinic tools frequently store weak passwords, lack login brute-force throttling, expose "
             "plaintext SMTP credentials, and cannot instantaneously revoke sessions of a suspended clinic.")

    b.table(
        "3.1", "Structural Comparison Between Legacy Outpatient Workflows and Proposed IMS",
        ["Operational Area", "Existing Legacy Workflow", "Proposed Infeara Medical System (IMS)"],
        [
            ["System Bootstrap", "Hardcoded default admin/admin credentials", "Zero-seed /setup wizard creating first SUPER_ADMIN securely"],
            ["Patient Identity", "Isolated clinic charts; duplicate patient IDs", "Global PAT-YYYY-NNNN ID + Staff-Verified Link & Non-Destructive Merge"],
            ["Slot Scheduling", "Manual diary; frequent double-booking", "Dynamic SlotService + Pessimistic Doctor Lock + UX_Appt_Slot Index"],
            ["Consultation Record", "Paper pad or mutable text field", "DRAFT -> FINAL state machine + Immutable ConsultationAddenda"],
            ["Prescription Medium", "Handwritten slip or static unverified PDF", "32-Char CSPRNG Code (Latin1_General_100_BIN2) + Real-Time Status"],
            ["Dispensing Control", "No tracking across partial fills", "Item-level quantity_dispensed check + Over-dispense prevention"],
            ["POS Pricing & Tax", "Manual rate lookup on every customer", "PharmacyMedicinePrices auto-prefill & automatic price memory"],
            ["Finance Accounting", "Manual end-of-day cashbook tally", "Automatic INCOME posting to PharmacyFinanceEntries linked to receipt"],
            ["Email Notifications", "None or blocking synchronous SMTP calls", "Transactional EmailLogs Outbox + Scheduled 1m/5m/15m Retry Worker"],
        ],
        col_widths=[1.35, 2.20, 2.47]
    )

    b.h2("3.3", "Proposed System")
    b.p(
        "The proposed Infeara Medical System (IMS) replaces disconnected clinic, patient, and pharmacy tools with "
        "a unified, security-hardened web platform built on Spring Boot 4.1.2 and Microsoft SQL Server 2022. Every "
        "human participant interacts with IMS through a dedicated, role-scoped workspace rendered inside a shared "
        "Corporate Healthcare ERP shell. Super Administrators govern tenant onboarding by enrolling clinics and "
        "pharmacies and dispatching hashed 72-hour single-use staff invitations, while configuring platform-wide "
        "SMTP delivery whose credentials are encrypted at rest via AES-256-GCM."
    )
    b.p(
        "Patients self-register through a structured three-section onboarding form (Personal, Address, and Optional "
        "Medical disclosures), verify ownership of their email via a 24-hour single-use SHA-256 hashed link, and "
        "gain access to active clinic discovery, real-time doctor slot booking, appointment rescheduling, digital "
        "prescription viewing, and longitudinal medical history. When a patient already has a walk-in record at an "
        "enrolled clinic, they submit a Clinic Link Request from My Profile; the clinic administrator verifies "
        "physical photo ID and Date of Birth, reviews automated duplicate warnings, and either links the account "
        "directly or executes a non-destructive record merge that transfers all appointments, consultations, and "
        "prescriptions onto the canonical clinic record while retaining the superseded row with merged_into_patient_id "
        "for audit integrity."
    )
    b.p(
        "Doctors work from a focused clinical queue where appointments progress cleanly from BOOKED/CHECKED_IN to "
        "IN_CONSULTATION and COMPLETED. Consultations are editable while in DRAFT and permanently frozen upon "
        "finalization, after which supplementary clinical observations are appended as signed ConsultationAddenda. "
        "From a finalized consultation, the doctor issues a digital prescription aided by the clinic's standardized "
        "medicine formulary (ClinicMedicines), producing a 32-character unambiguous alphanumeric code. At any "
        "enrolled pharmacy, scanning or typing that 32-character code into /pharmacy/pos verifies authenticity, "
        "expiry, cancellation status, and remaining dispensable units, pre-fills unit prices from "
        "PharmacyMedicinePrices, calculates discounts, tax, and change due, records partial or full dispensing, "
        "and automatically posts the net sale total as an INCOME entry in PharmacyFinanceEntries."
    )

    b.h2("3.4", "Advantages of Proposed System")
    b.bullet("End-to-End Clinical & Financial Continuity:",
             "Connects clinic reception, consulting rooms, patient self-service, pharmacy POS billing, and "
             "pharmacy cashflow accounting within one ACID-compliant relational database.")
    b.bullet("Zero-Trust Ownership & Tenant Isolation:",
             "Resolves patient and staff context strictly from the authenticated Spring Security principal, "
             "preventing horizontal privilege escalation (IDOR) and terminating suspended-clinic sessions instantly.")
    b.bullet("Mathematically Guaranteed Fraud & Concurrency Resistance:",
             "32-character CSPRNG prescription codes provide 160 bits of entropy (32^32 combinations), while "
             "pessimistic row locks and SQL filtered unique indexes eliminate double-booking and over-dispensing.")
    b.bullet("High Counter Velocity at Retail Pharmacies:",
             "Remembered medicine prices, instant prescription line population, automated GST/tax calculation, "
             "and zero-touch finance ledger posting cut pharmacy billing time to seconds.")
    b.bullet("Zero-Setup Portability & Enterprise Scalability:",
             "Runs out-of-the-box on an in-memory/file H2 compatibility profile for rapid development and testing, "
             "and scales seamlessly to Microsoft SQL Server 2022 with Flyway migrations in production.")

    b.h2("3.5", "Software Requirements Specification (SRS)")
    b.p(
        "Tables 3.2 and 3.3 formalize the Functional Requirements (FR-01 through FR-18) and Non-Functional "
        "Requirements (NFR-01 through NFR-10) implemented and verified in the clinicflow-app codebase."
    )
    b.table(
        "3.2", "Functional Requirements Specification Matrix (FR-01 to FR-18)",
        ["Req. ID", "Module", "Functional Requirement Specification", "Priority"],
        [
            ["FR-01", "Bootstrap", "Allow creation of the initial SUPER_ADMIN via /setup only when zero users exist in dbo.Users.", "Critical"],
            ["FR-02", "Identity", "Support patient self-registration with unique email check and password policy (>=10 chars, letter + digit).", "Critical"],
            ["FR-03", "Auth Tokens", "Issue single-use SHA-256 hashed tokens for VERIFY_EMAIL (24h), RESET_PASSWORD (60m), STAFF_INVITE (72h), EMAIL_CHANGE (24h).", "Critical"],
            ["FR-04", "Admin", "Enable SUPER_ADMIN to enroll, activate, and suspend Clinics and Pharmacies and dispatch admin invitations.", "High"],
            ["FR-05", "Admin SMTP", "Allow SUPER_ADMIN to configure, AES-256-GCM encrypt, activate, and live-test SMTP server settings.", "High"],
            ["FR-06", "Clinic Masters", "Allow CLINIC_ADMIN to create/edit walk-in Patient Masters, onboard Doctors, and configure weekday DoctorSchedules.", "Critical"],
            ["FR-07", "Formulary", "Provide Clinic Medicine Formulary CRUD plus bulk CSV template download, CSV import, and CSV export.", "Medium"],
            ["FR-08", "Patient Link", "Enable patients to request clinic links and require CLINIC_ADMIN ID verification + DOB check to approve or reject.", "Critical"],
            ["FR-09", "Record Merge", "Support non-destructive merging of an online patient record into an existing clinic patient record preserving all FKs.", "Critical"],
            ["FR-10", "Discovery", "Allow patients to search and filter ACTIVE clinics by city/keyword and inspect doctors, fees, and opening hours.", "High"],
            ["FR-11", "Slot Booking", "Compute available future slots dynamically and book/reschedule/cancel appointments without double-booking.", "Critical"],
            ["FR-12", "Front Desk", "Allow CLINIC_ADMIN to confirm, check in, cancel, or mark appointments as NO_SHOW from the clinic queue.", "High"],
            ["FR-13", "Consultation", "Enable DOCTOR to start consultations, save mutable DRAFT notes, finalise irreversibly, and append signed addenda.", "Critical"],
            ["FR-14", "e-Prescription", "Generate 32-character case-sensitive prescriptions with validity days, item quantities, instructions, and lifetime flag.", "Critical"],
            ["FR-15", "Rx Verify", "Verify 32-char Rx codes at pharmacy with outcomes VALID, NOT_FOUND, CANCELLED, EXPIRED, or FULLY_DISPENSED.", "Critical"],
            ["FR-16", "Pharmacy POS", "Pre-fill POS cart from PharmacyMedicinePrices, compute subtotal/discount/tax/change, and enforce remaining-qty bounds.", "Critical"],
            ["FR-17", "Finance Sync", "Automatically post completed POS sale totals as INCOME in PharmacyFinanceEntries linked to dispense_id; allow manual expenses.", "High"],
            ["FR-18", "Email Outbox", "Persist outbound notifications in EmailLogs with idempotency keys and deliver asynchronously with exponential retry.", "High"],
        ],
        col_widths=[0.72, 0.98, 3.62, 0.70]
    )

    b.table(
        "3.3", "Non-Functional Requirements Specification Matrix (NFR-01 to NFR-10)",
        ["NFR ID", "Category", "Target Engineering Requirement & Implementation Mechanism"],
        [
            ["NFR-01", "Security (Auth)", "Passwords hashed via Spring DelegatingPasswordEncoder (BCrypt); CSRF enabled on all state-mutating POST forms."],
            ["NFR-02", "Brute-Force Defense", "LoginThrottleFilter & AuthRateLimiter block repeated failures (5 failed logins per 15-minute window per email/IP)."],
            ["NFR-03", "Tenant Isolation", "Immediate logout via ClinicStatusFilter whenever a clinic transitions from ACTIVE to SUSPENDED."],
            ["NFR-04", "Concurrency", "Zero appointment slot collisions and zero prescription over-dispenses under concurrent multi-user transactions."],
            ["NFR-05", "Data Integrity", "open-in-view=false; explicit @Transactional boundaries; SQL CHECK constraints and ROWVERSION optimistic locking."],
            ["NFR-06", "Auditability", "Every authentication, linking, clinical, prescribing, and dispensing action recorded in dbo.AuditLogs with actor & IP."],
            ["NFR-07", "Performance", "HikariCP connection pooling (max 10, min-idle 2) and indexed lookup queries delivering <150 ms median page latency."],
            ["NFR-08", "Resilience", "Transient SMTP outages isolated from user transactions via scheduled EmailDispatchWorker (5s poll, 3 retries)."],
            ["NFR-09", "Maintainability", "Schema changes strictly version-controlled via Flyway SQL migrations V001 through V009."],
            ["NFR-10", "Usability", "Consistent Corporate ERP layout with keyboard-accessible forms, semantic status badges, and contextual breadcrumbs."],
        ],
        col_widths=[0.75, 1.25, 4.02]
    )

    b.h2("3.6", "Feasibility Study")
    b.p(
        "Prior to full-scale implementation, a comprehensive three-pillar feasibility study was conducted to "
        "evaluate technical viability, financial cost-effectiveness, and day-to-day operational adoption across "
        "target clinics and pharmacies."
    )

    b.h3("3.6.1", "Technical Feasibility")
    b.p(
        "Infeara Medical System relies exclusively on mature, enterprise-proven, open-standards technologies. "
        "Spring Boot 4.1.2 on Java 27 provides battle-tested dependency injection, declarative transaction "
        "management, scheduled background execution, and Spring Security filter pipelines without requiring "
        "external message brokers or complex container orchestration. Microsoft SQL Server 2022 Express supplies "
        "production-grade ACID guarantees, binary collations (Latin1_General_100_BIN2), filtered unique indexes, "
        "and row-versioning free of licensing fees up to 10 GB per database, while H2's MSSQLServer compatibility "
        "mode enables zero-dependency developer execution and automated CI testing. On the client tier, standard "
        "HTML5, CSS3 custom properties, and lightweight progressive JavaScript run smoothly on any modern desktop "
        "or tablet browser without client-side build chains or GPU requirements. Consequently, the system is "
        "100% technically feasible."
    )

    b.h3("3.6.2", "Economic Feasibility")
    b.p(
        "Because the entire software stack—OpenJDK 27, Apache Maven, Spring Boot, Thymeleaf, Flyway Core, H2, and "
        "Microsoft SQL Server 2022 Express—is available at zero software licensing cost, capital expenditure is "
        "minimal. A single modest virtual private server or on-premise clinic mini-server can host multiple "
        "clinics and pharmacies simultaneously. Eliminating pre-printed multi-part prescription pads, preventing "
        "revenue leakage at pharmacy counters, and saving 3–5 minutes of receptionist and pharmacist clerical "
        "effort per patient encounter yields rapid return on investment, as summarized in Table 3.4."
    )
    b.table(
        "3.4", "Estimated Annual Cost-Benefit & Economic Feasibility Summary (Small 3-Clinic Network)",
        ["Cost / Benefit Component", "Legacy Manual / Siloed Setup", "Proposed IMS Platform", "Net Impact"],
        [
            ["Software Licensing Fees", "Rs. 45,000 / yr (3 desktop licenses)", "Rs. 0 (Open-Source Java + SQL Express)", "Saves Rs. 45,000 / yr"],
            ["Cloud / Server Hosting", "Local fragile PCs (unbacked)", "Rs. 18,000 / yr (Managed VPS + Backups)", "Predictable low OpEx"],
            ["Paper Pads & Registers", "Rs. 24,000 / yr", "Rs. 3,000 / yr (Digital e-Rx primary)", "Saves Rs. 21,000 / yr"],
            ["Pharmacy Billing & Tally Labor", "~2.5 hrs/day manual price/ledger work", "Automated POS price memory & ledger", ">75% time reduction"],
            ["Over-Dispense / Billing Errors", "2–4% unrecorded or mispriced items", "<0.1% (Strict DB & BigDecimal guards)", "Direct margin recovery"],
        ],
        col_widths=[1.65, 1.50, 1.55, 1.32]
    )

    b.h3("3.6.3", "Operational Feasibility")
    b.p(
        "Operational feasibility measures how naturally clinic receptionists, doctors, patients, and pharmacists "
        "adopt the system without disruptive retraining. IMS mirrors real clinical mental models: receptionists "
        "see an intuitive Today's Appointments queue and guided identity-verification checkboxes; doctors use "
        "clinic-formulary autocomplete to prescribe drugs in seconds; patients book visible time-slot pills just "
        "like familiar consumer portals; and pharmacists scan or paste a single 32-character code into a bold "
        "POS input bar where unit prices are remembered automatically after the first sale. Hence, operational "
        "friction is exceptionally low and user acceptance is high."
    )

    # ==========================================================================
    # CHAPTER 4: SYSTEM DESIGN
    # ==========================================================================
    b.start_chapter(4, "SYSTEM DESIGN")

    b.h2("4.1", "Design Overview and Architectural Principles")
    b.p(
        "System design translates the functional and non-functional requirements of Chapter 3 into concrete "
        "software structures, security pipelines, data flow topologies, object-oriented domain entities, and "
        "normalized relational database schemas. Five core architectural principles govern every class and SQL "
        "script inside Infeara Medical System:"
    )
    b.bullet("1. Modular Monolith with Explicit Transaction Boundaries:",
             "Domain logic is organized into cohesive vertical and horizontal packages (audit, common, config, "
             "domain, identity, notifications, security, web). Spring JPA's open-in-view anti-pattern is explicitly "
             "disabled (spring.jpa.open-in-view=false) so lazy-loading never leaks into Thymeleaf view rendering "
             "and every read/write operation executes inside a well-defined @Transactional service method.")
    b.bullet("2. Dual-Layer Defense-in-Depth Concurrency:",
             "Critical clinical invariants are enforced both in Java domain services and in SQL Server DDL—for "
             "example, pessimistic row locking (UPDLOCK) plus filtered unique index UX_Appt_Slot on appointments, "
             "and Java case equality plus Latin1_General_100_BIN2 binary collation on prescription codes.")
    b.bullet("3. Session-Derived Ownership (Zero IDOR Surface):",
             "Controllers never accept a raw patientId, clinicId, or pharmacyId to identify the acting user; "
             "ownership is resolved exclusively from the authenticated AppUserPrincipal in the server HTTP session.")
    b.bullet("4. Append-Only Clinical & Forensic History:",
             "Finalized consultations cannot be mutated (only extended via ConsultationAddenda), merged patient "
             "records are preserved dormantly via merged_into_patient_id, and every sensitive action writes an "
             "immutable row to dbo.AuditLogs.")
    b.bullet("5. Asynchronous Side-Effect Decoupling via Transactional Outbox:",
             "Domain events (registration verification, staff invites, booking confirmations, prescription issuance) "
             "write queued EmailLogs records inside the business transaction, decoupling user response latency "
             "from external SMTP server availability.")

    b.h2("4.2", "System Architecture")
    b.p(
        "Figure 4.1 illustrates the six-tier Modular Monolith architecture of Infeara Medical System. Requests "
        "originating from desktop or tablet web browsers traverse the Spring Security 6 gateway filter chain "
        "(Tier 2), route into role-isolated Spring WebMVC controllers and Thymeleaf layout templates (Tier 3), "
        "invoke @Transactional domain services and event listeners (Tier 4), execute parameterized ORM and "
        "pessimistic-locking queries via Spring Data JPA / Hibernate 6 (Tier 5), and persist ACID state in "
        "Microsoft SQL Server 2022 alongside the @Scheduled EmailDispatchWorker (Tier 6)."
    )
    b.figure("4.1", "Infeara Medical System (IMS) Six-Tier Modular Monolith Architecture",
             os.path.join(assets_dir, "architecture_diagram.png"), width_in=5.95)

    b.p(
        "Complementing the macro tier structure, Figure 4.2 details the defense-in-depth Spring Security filter "
        "pipeline and cryptographic services (LoginThrottleFilter, BCrypt authentication, ClinicStatusFilter, "
        "SHA-256 AuthTokens, AES-256-GCM SecretCipher, and AuditService)."
    )
    b.figure("4.2", "IMS Defense-in-Depth Security Filter Chain and Cryptographic Controls",
             os.path.join(assets_dir, "security_pipeline.png"), width_in=5.95)

    b.h2("4.3", "Data Flow Diagrams (DFD)")
    b.p(
        "Data Flow Diagrams model how clinical, administrative, cryptographic, and financial information enters, "
        "transforms within, and persists across the system boundary without coupling prematurely to object syntax."
    )

    b.h3("4.3.1", "Level 0 DFD (System Context Diagram)")
    b.p(
        "At Level 0 (Figure 4.3), the entire Infeara Medical System is represented as central Process 0.0 "
        "exchanging structured request and response data streams with five external actor classes: Super Admin, "
        "Clinic Admin, Doctor, Patient, and Pharmacy Staff."
    )
    b.figure("4.3", "Data Flow Diagram — Level 0 (System Context Diagram)",
             os.path.join(assets_dir, "dfd_level0.png"), width_in=5.85)

    b.h3("4.3.2", "Level 1 DFD (Subsystem Decomposition)")
    b.p(
        "Decomposing Process 0.0 yields six major functional processes (Processes 1.0 through 6.0) interacting "
        "with five logical relational data stores (D1 through D5), as depicted in Figure 4.4: Identity, RBAC & "
        "Patient Linking (1.0), Clinic Discovery & Slot Booking (2.0), Consultation & Addenda Engine (3.0), "
        "32-Character e-Prescription Issuance & Outbox (4.0), Pharmacy Verification & POS Billing (5.0), and "
        "Finance Ledger & Audit Reporting (6.0)."
    )
    b.figure("4.4", "Data Flow Diagram — Level 1 (IMS Functional Decomposition)",
             os.path.join(assets_dir, "dfd_level1.png"), width_in=5.95)

    b.h3("4.3.3", "Level 2 DFD (Pharmacy POS Verification, Pricing & Ledger Automation)")
    b.p(
        "Because Process 5.0 encapsulates critical real-time prescription validation, partial-dispensing math, "
        "self-learning catalog pricing, and automated cashbook synchronization, Figure 4.5 expands Process 5.0 "
        "into five granular sub-processes (5.1 through 5.5)."
    )
    b.figure("4.5", "Data Flow Diagram — Level 2 (Pharmacy POS Dispensing and Automated Finance Sync)",
             os.path.join(assets_dir, "dfd_level2_pos.png"), width_in=5.90)

    b.h2("4.4", "Use Case Diagram and Actor Specifications")
    b.p(
        "Figure 4.6 presents the Unified Modeling Language (UML) Use Case Diagram capturing thirteen core "
        "use cases (UC-01 through UC-13) across the six security roles defined in com.clinicflow.domain.enums.Role."
    )
    b.figure("4.6", "IMS Unified UML Use Case Diagram Across Five Actor Portals",
             os.path.join(assets_dir, "use_case_diagram.png"), width_in=5.90)

    b.table(
        "4.1", "Detailed Actor-to-Use-Case Responsibility Specification",
        ["Actor / Role Enum", "Portal Prefix", "Authorized Use Cases & Domain Responsibilities"],
        [
            ["Super Admin (SUPER_ADMIN)", "/admin/**",
             "Bootstraps system via /setup; enrolls, activates, and suspends Clinics & Pharmacies; dispatches 72h admin invites; views global Patient Masters & KPI reports; manages AES-GCM encrypted SMTP settings."],
            ["Clinic Admin (CLINIC_ADMIN)", "/clinic/**",
             "Maintains clinic Patient Masters; verifies physical ID & DOB to approve/reject/merge PatientLinkRequests; manages Doctors & weekday DoctorSchedules; operates Appointment check-in desk; imports/exports CSV ClinicMedicines."],
            ["Doctor (DOCTOR)", "/doctor/**",
             "Views assigned daily appointment queue; transitions encounters to IN_CONSULTATION; authors DRAFT diagnoses/notes, finalizes consultations irreversibly, appends signed ConsultationAddenda, and issues 32-char e-Prescriptions."],
            ["Patient (PATIENT)", "/patient/**",
             "Self-registers & verifies email; edits profile & requests verified email changes; requests Clinic Links; discovers active clinics; books, reschedules, or cancels conflict-free slots; views 32-char prescriptions & longitudinal history."],
            ["Pharmacy Admin & Pharmacist (PHARMACY_ADMIN, PHARMACIST)", "/pharmacy/**",
             "Verifies 32-char prescription codes; executes POS cart billing with auto-remembered unit prices, discounts, taxes, and over-dispense protection; prints sale receipts; manages Finance Ledger, staff invites (Admin), and reports."],
        ],
        col_widths=[1.45, 0.95, 3.62]
    )

    b.p(
        "To formalize actor interactions at an engineering specification level, Tables 4.1a through 4.1d document "
        "the complete preconditions, main success scenarios, security exception flows, and transactional "
        "postconditions for the four highest-criticality workflows in Infeara Medical System."
    )

    uc_specs = [
        ("4.1a", "Use Case Specification — UC-05: Staff-Verified Patient Link & Non-Destructive Record Merge", [
            ["Primary Actor & Trigger", "CLINIC_ADMIN opens /clinic/link-requests/{id} after PATIENT submits link request."],
            ["Preconditions", "Clinic is ACTIVE; PatientLinkRequest status is PENDING; Clinic Admin belongs to target clinic_id."],
            ["Main Success Scenario", "1. System displays account patient's name, verified DOB, email, mobile & duplicate warnings.\n"
                                      "2. Clinic Admin inspects physical photo ID at front desk and checks identityVerified=true.\n"
                                      "3. Admin optionally enters walk-in public_patient_id (e.g., PAT-2026-0014) to merge.\n"
                                      "4. Service verifies exact DOB match, re-points Appointments, Prescriptions & ClinicPatients to target row, moves User FK, and sets merged_into_patient_id on old row.\n"
                                      "5. Status updates to APPROVED and AuditLogs records PATIENT_RECORD_MERGED + PATIENT_LINK_APPROVED."],
            ["Exception / Security Flows", "• Missing identityVerified checkbox -> HTTP 200 form error; state stays PENDING.\n"
                                           "• Target record DOB differs from account DOB -> Merge rejected immediately.\n"
                                           "• Admin from another clinic requests URL -> HTTP 404 / tenant isolation enforced."],
            ["Postconditions", "Single unified patient chart linked to online User login; zero orphaned foreign keys."],
        ]),
        ("4.1b", "Use Case Specification — UC-08: Atomic Conflict-Free Doctor Slot Booking", [
            ["Primary Actor & Trigger", "Authenticated PATIENT selects clinic, doctor, date, and 15-min slot on /patient/book."],
            ["Preconditions", "Patient email verified; ClinicStatus == ACTIVE; DoctorStatus == ACTIVE; ClinicDoctor active."],
            ["Main Success Scenario", "1. SlotService computes future slots from weekday DoctorSchedules minus active Appointments.\n"
                                      "2. Patient submits POST /patient/book with valid CSRF token.\n"
                                      "3. BookingService acquires pessimistic write lock (doctorRepository.findForUpdate).\n"
                                      "4. Service re-verifies slot availability, inserts BOOKED Appointment under UX_Appt_Slot, assigns APT-YYYY-NNNNN, ensures ClinicPatient link, and queues confirmation email."],
            ["Exception / Security Flows", "• Competing request took slot milliseconds earlier -> Lock serializes or UX_Appt_Slot throws DataIntegrityViolationException -> clean 'Slot just taken' message.\n"
                                           "• Past timestamp or unscheduled weekday -> Rejected before insert."],
            ["Postconditions", "Exactly one active appointment occupies (doctor_id, slot_start); AuditLogs & EmailLogs updated."],
        ]),
        ("4.1c", "Use Case Specification — UC-09 & UC-10: Immutable Clinical Consultation & 32-Char e-Rx", [
            ["Primary Actor & Trigger", "Authenticated DOCTOR opens checked-in encounter at /doctor/appointments/{id}."],
            ["Preconditions", "DoctorUserLink maps session user to appointment's doctor_id; Clinic is ACTIVE."],
            ["Main Success Scenario", "1. Doctor clicks Start Consultation -> status moves to IN_CONSULTATION; DRAFT Consultation created.\n"
                                      "2. Doctor enters diagnosis, vitals/notes, and follow-up date; clicks Finalise.\n"
                                      "3. Consultation transitions irreversibly to FINAL (finalised_at stamped); Appointment marked COMPLETED.\n"
                                      "4. Any subsequent note is appended via ConsultationAddenda with author & reason.\n"
                                      "5. Doctor selects formulary medicines, sets validity days, and issues 32-char CSPRNG Prescription."],
            ["Exception / Security Flows", "• Attempt to edit diagnosis after FINAL -> Blocked; only addenda permitted.\n"
                                           "• Duplicate prescription on same consultation -> Blocked (1-to-1 consultation Rx rule)."],
            ["Postconditions", "Immutable clinical record + VALID 32-char binary-collated Prescription + queued patient email."],
        ]),
        ("4.1d", "Use Case Specification — UC-11, UC-12 & UC-13: Pharmacy POS Billing & Auto-Ledger Sync", [
            ["Primary Actor & Trigger", "PHARMACY_ADMIN or PHARMACIST scans 32-char code on /pharmacy/pos."],
            ["Preconditions", "Pharmacy status is ACTIVE; PharmacyUser status is ACTIVE."],
            ["Main Success Scenario", "1. PharmacyService performs binary case-sensitive lookup, checks status/expiry/remaining units, and logs PharmacyVerification (outcome=VALID).\n"
                                      "2. POS cart auto-populates remaining quantities and remembered unit prices from PharmacyMedicinePrices.\n"
                                      "3. Pharmacist confirms quantities, discount, tax rate, payment method (Cash/UPI/Card), and tendered amount.\n"
                                      "4. Service locks PrescriptionItems, increments quantity_dispensed, saves PrescriptionDispense receipt, updates remembered prices, and auto-posts INCOME to PharmacyFinanceEntries."],
            ["Exception / Security Flows", "• Wrong case / unknown code -> NOT_FOUND logged with SHA-256 attempt_hash.\n"
                                           "• Requested qty > remaining -> Over-dispense blocked; transaction rolled back.\n"
                                           "• Reusing consumed verificationId -> Rejected (one verification per dispense)."],
            ["Postconditions", "Updated item fulfilment, printable SALE receipt, remembered catalog prices, and linked INCOME ledger row."],
        ]),
    ]
    for utnum, utcap, utrows in uc_specs:
        b.table(utnum, utcap, ["Specification Element", "Engineering & Clinical Detail"], utrows, col_widths=[1.65, 4.37])

    b.h2("4.5", "Object-Oriented & Behavioral UML Models (Class, Sequence, Activity & State)")
    b.p(
        "Figure 4.7 illustrates the UML Domain Entity Class Diagram mapped directly to com.clinicflow.domain.entity.*. "
        "Entities encapsulate surrogate BIGINT keys, public UUIDs or formatted human-readable IDs (PAT-YYYY-NNNN, "
        "APT-YYYY-NNNNN), @Version byte[] optimistic row versions where concurrent edits occur, and strongly typed "
        "enumerated status fields."
    )
    b.figure("4.7", "IMS Core Domain Entity UML Class Diagram (Spring Data JPA / Hibernate 6)",
             os.path.join(assets_dir, "class_diagram.png"), width_in=5.95)

    b.p(
        "Figure 4.8 traces the fifteen-step temporal interaction sequence across Patient Browser, Doctor Workspace, "
        "Spring Boot Transactional Services, SQL Server 2022, and Pharmacy POS Terminal—spanning atomic slot "
        "reservation, clinical consultation finalization, 32-character prescription issuance, binary-collated POS "
        "lookup, itemized dispensing, price-list learning, and automated finance-ledger posting."
    )
    b.figure("4.8", "UML Sequence Diagram — End-to-End Clinical Consultation, e-Rx & Pharmacy POS Flow",
             os.path.join(assets_dir, "sequence_diagram.png"), width_in=5.95)

    b.p(
        "Figure 4.9 models the branching control flow of patient registration, front-desk identity verification "
        "(including rejection vs. approval/record-merge branches), clinical consultation, prescription generation, "
        "and POS fulfillment, while Figure 4.10 formalizes the finite-state machines governing AppointmentStatus "
        "and Prescription / vw_PrescriptionFulfilment lifecycles."
    )
    b.figure("4.9", "UML Activity Workflow Diagram — Patient Linking, Clinical Care & Dispensing",
             os.path.join(assets_dir, "activity_diagram.png"), width_in=5.55)
    b.figure("4.10", "UML Lifecycle State Machine Diagram — AppointmentStatus & Prescription Fulfilment",
             os.path.join(assets_dir, "state_machine_diagram.png"), width_in=5.90)

    b.h2("4.6", "Entity Relationship (ER) Diagram")
    b.p(
        "Figure 4.11 depicts the complete relational Entity-Relationship architecture spanning all 27 normalized "
        "tables across Flyway migrations V001 through V009, organized cleanly into five functional domains: "
        "(1) Identity & Tenant Enrollment, (2) Patient Federation & Link Merging, (3) Clinical Scheduling & "
        "Consultations, (4) Electronic Prescriptions & Clinic Formulary, and (5) Pharmacy POS, Price Memory, "
        "Finance Ledger, Outbox & Audit."
    )
    b.figure("4.11", "IMS Relational Database Entity-Relationship (ER) Schema (27 Tables, V001–V009)",
             os.path.join(assets_dir, "er_diagram.png"), width_in=5.95)

    b.h2("4.7", "Complete Relational Database Design and Data Dictionary")
    b.p(
        "The persistence schema of Infeara Medical System is governed deterministically by nine sequential Flyway "
        "SQL migration scripts located in clinicflow-app/src/main/resources/db/migration/ (V001 through V009). "
        "All tables reside in the dbo schema on Microsoft SQL Server 2022, utilizing BIGINT IDENTITY(1,1) surrogate "
        "primary keys for fast internal joins alongside UNIQUEIDENTIFIER (NEWID()) or formatted public identifiers "
        "so sequential primary keys are never exposed in public URLs. Timestamps standardize on DATETIME2(3) in "
        "UTC (SYSUTCDATETIME()), monetary figures use exact fixed-point DECIMAL(12,2) or DECIMAL(18,2) arithmetic, "
        "and concurrency-critical tables include ROWVERSION columns."
    )

    b.table(
        "4.2", "Flyway Database Migration History (V001 through V009)",
        ["Version", "Migration Script Filename", "Schema Objects Created / Altered"],
        [
            ["V001", "V001__init_schema.sql", "Creates 24 core tables + SQL view dbo.vw_PrescriptionFulfilment + filtered unique index UX_Appt_Slot + BIN2 Rx collation."],
            ["V002", "V002__identity_email_outbox.sql", "Adds subject, body, next_attempt_at to EmailLogs; creates UX_AuthTokens_token_hash and IX_EmailLogs_status_next_attempt."],
            ["V003", "V003__patient_link_request_resolution.sql", "Adds clinic_patient_record_id, resolved_by, and resolution_note FKs/columns to PatientLinkRequests."],
            ["V004", "V004__invites_contact_change_and_merge_marker.sql", "Adds Users.pending_email, Patients.merged_into_patient_id self-FK, and expands AuthTokens purposes to 4 types."],
            ["V005", "V005__clinic_inventory.sql", "Creates dbo.ClinicMedicines table for standardized clinic drug formularies with unique (clinic_id, medicine, dosage)."],
            ["V006", "V006__rename_inventory_unit_to_form.sql", "Renames ClinicMedicines.default_unit to default_form to accurately reflect dosage form (Tablet, Capsule, Syrup)."],
            ["V007", "V007__prescription_item_lifetime.sql", "Adds lifetime BIT NOT NULL DEFAULT 0 column to dbo.PrescriptionItems for chronic/lifetime therapies."],
            ["V008", "V008__pharmacy_finance.sql", "Creates dbo.PharmacyFinanceEntries ledger table with INCOME / EXPENSE check constraint and date index."],
            ["V009", "V009__pharmacy_pos_pricing.sql", "Creates dbo.PharmacyMedicinePrices; adds subtotal, discount, tax, total, payment columns to Dispenses; links Finance to dispense_id."],
        ],
        col_widths=[0.65, 2.15, 3.22]
    )

    # Full Data Dictionary of all 27 tables!
    dd_tables = [
        ("4.3", "Data Dictionary — dbo.Clinics (Multi-Tenant Healthcare Facilities)", [
            ["clinic_id", "BIGINT IDENTITY(1,1)", "PK, NOT NULL", "Internal surrogate primary key for the clinic tenant."],
            ["public_uuid", "UNIQUEIDENTIFIER", "UQ, DEFAULT NEWID()", "Public opaque UUID exposed in patient clinic-discovery URLs."],
            ["name", "NVARCHAR(200)", "NOT NULL", "Official display name of the enrolled clinic or polyclinic."],
            ["address_line1 / line2", "NVARCHAR(200)", "line1 NOT NULL", "Street address and suite/landmark details."],
            ["city / area / district", "NVARCHAR(100)", "city NOT NULL, IX", "Indexed city, locality, and district for patient clinic search."],
            ["state / postal_code", "NVARCHAR(100 / 20)", "NOT NULL", "State and postal PIN code."],
            ["latitude / longitude", "DECIMAL(9,6)", "NULL", "Optional geographic coordinates for proximity discovery."],
            ["phone / email", "NVARCHAR(30 / 254)", "NULL", "Public reception phone number and contact email."],
            ["opening_hours / fees", "NVARCHAR(500 / 200)", "NULL", "Published operating schedule and baseline consultation fee text."],
            ["status", "VARCHAR(12)", "CK IN (PENDING,ACTIVE,SUSPENDED)", "Lifecycle state enforced on every request by ClinicStatusFilter."],
            ["created_at / updated_at", "DATETIME2(3)", "NOT NULL, UTC", "Audit creation and last-modification UTC timestamps."],
        ]),
        ("4.4", "Data Dictionary — dbo.Users (Unified Security Principals)", [
            ["user_id", "BIGINT IDENTITY(1,1)", "PK, NOT NULL", "Internal surrogate key referenced by sessions and audit logs."],
            ["public_uuid", "UNIQUEIDENTIFIER", "UQ, DEFAULT NEWID()", "External immutable UUID for user principal."],
            ["email", "NVARCHAR(254)", "UQ, NOT NULL", "Primary verified login email address (unique across platform)."],
            ["pending_email", "NVARCHAR(254)", "NULL (Added V004)", "Unverified target email address awaiting EMAIL_CHANGE token confirmation."],
            ["password_hash", "NVARCHAR(255)", "NOT NULL", "DelegatingPasswordEncoder BCrypt hash ({bcrypt}$2a$...)."],
            ["role", "VARCHAR(20)", "CK (6 Roles), IX", "SUPER_ADMIN, CLINIC_ADMIN, DOCTOR, PATIENT, PHARMACY_ADMIN, PHARMACIST."],
            ["status", "VARCHAR(12)", "CK (4 States)", "PENDING, ACTIVE, SUSPENDED, or DISABLED."],
            ["email_verified_at", "DATETIME2(3)", "NULL", "UTC timestamp set when VERIFY_EMAIL or STAFF_INVITE token is consumed."],
            ["clinic_id", "BIGINT", "FK -> Clinics, NULL", "Tenant clinic binding for CLINIC_ADMIN and DOCTOR accounts."],
            ["row_ver", "ROWVERSION", "Optimistic Lock", "SQL Server automatic binary counter preventing lost concurrent updates."],
        ]),
        ("4.5", "Data Dictionary — dbo.Patients (Master & Self-Registered Patient Profiles)", [
            ["patient_id", "BIGINT IDENTITY(1,1)", "PK, NOT NULL", "Surrogate key for clinical foreign-key relationships."],
            ["public_patient_id", "NVARCHAR(20)", "UQ, NOT NULL", "Formatted identifier PAT-YYYY-NNNN generated via PublicIdGenerator."],
            ["user_id", "BIGINT", "FK -> Users, NULL, IX", "Linked online User account (NULL for unlinked walk-in clinic records)."],
            ["merged_into_patient_id", "BIGINT", "Self-FK -> Patients (V004)", "Non-NULL when this dormant row was merged into a canonical clinic record."],
            ["first_name / last_name", "NVARCHAR(100)", "NOT NULL", "Verified legal given name and family surname."],
            ["dob", "DATE", "NOT NULL", "Date of birth used as mandatory anti-mismatch guard during link merges."],
            ["gender", "VARCHAR(10)", "CK (MALE,FEMALE,OTHER,UNKNOWN)", "Optional self-reported or clinic-recorded gender."],
            ["mobile / email", "NVARCHAR(30 / 254)", "NULL", "Patient contact phone and notification address."],
            ["address_line1..postal_code", "NVARCHAR", "NULL", "Residential street, city, district, state, and PIN code."],
            ["blood_group", "NVARCHAR(10)", "NULL", "ABO/Rh blood group displayed in Doctor consultation sidebar."],
            ["allergies / conditions", "NVARCHAR(1000)", "NULL", "Drug/food allergies and chronic medical conditions."],
            ["emergency_name / phone", "NVARCHAR(200 / 30)", "NULL", "Emergency next-of-kin contact details."],
            ["source_flag", "VARCHAR(20)", "CK (PATIENT_ENTERED, CLINIC_ENTERED)", "Distinguishes online self-registrations from front-desk walk-in charts."],
            ["row_ver", "ROWVERSION", "Optimistic Lock", "Prevents concurrent profile overwrite anomalies."],
        ]),
        ("4.6", "Data Dictionary — dbo.Doctors, dbo.ClinicDoctors, dbo.DoctorSchedules & dbo.DoctorUserLinks", [
            ["Doctors.doctor_id", "BIGINT IDENTITY(1,1)", "PK, NOT NULL", "Primary key of practitioner master entity (public_uuid, name, specialization, status)."],
            ["ClinicDoctors.clinic_doctor_id", "BIGINT IDENTITY(1,1)", "PK, UQ(clinic_id,doctor_id)", "Associates a Doctor with a Clinic, consultation fee DECIMAL(10,2), and ACTIVE/INACTIVE status."],
            ["DoctorSchedules.schedule_id", "BIGINT IDENTITY(1,1)", "PK, IX(doctor_id,clinic_id)", "Defines weekly consulting window: weekday (0..6), start_time, end_time, slot_minutes (default 15), effective_from/to."],
            ["DoctorUserLinks.link_id", "BIGINT IDENTITY(1,1)", "PK, UQ(user_id)", "Binds a DOCTOR login account (Users.user_id) 1-to-1 to their practitioner profile (Doctors.doctor_id)."],
        ]),
        ("4.7", "Data Dictionary — dbo.Appointments (Conflict-Free Slot Reservations)", [
            ["appointment_id", "BIGINT IDENTITY(1,1)", "PK, NOT NULL", "Internal surrogate key for appointment encounter."],
            ["public_appointment_id", "NVARCHAR(20)", "UQ, NOT NULL", "Human-readable booking code APT-YYYY-NNNNN."],
            ["patient_id / clinic_id / doctor_id", "BIGINT", "FKs, NOT NULL, IX", "References Patient, Clinic, and Doctor; indexed for queue & slot lookups."],
            ["slot_start / slot_end", "DATETIME2(3)", "NOT NULL, UX_Appt_Slot", "Filtered unique index UX_Appt_Slot(doctor_id, slot_start) on active statuses prevents double-booking."],
            ["reason", "NVARCHAR(500)", "NULL", "Chief complaint or visit reason entered during booking."],
            ["status", "VARCHAR(20)", "CK (7 States)", "BOOKED, CONFIRMED, CHECKED_IN, IN_CONSULTATION, COMPLETED, CANCELLED, NO_SHOW."],
            ["booked_at / cancelled_at", "DATETIME2(3)", "UTC", "Booking creation and optional cancellation timestamps + cancel_reason NVARCHAR(300)."],
            ["row_ver", "ROWVERSION", "Optimistic Lock", "Protects concurrent status transitions between front desk and doctor."],
        ]),
        ("4.8", "Data Dictionary — dbo.Consultations & dbo.ConsultationAddenda (Clinical Records)", [
            ["Consultations.consultation_id", "BIGINT IDENTITY(1,1)", "PK, NOT NULL", "Primary key of clinical encounter note."],
            ["Consultations.appointment_id", "BIGINT", "UQ, FK -> Appointments", "Strict 1-to-1 link ensuring at most one consultation record per appointment."],
            ["Consultations.diagnosis", "NVARCHAR(2000)", "NULL", "Primary clinical diagnosis recorded by consulting physician."],
            ["Consultations.notes", "NVARCHAR(MAX)", "NULL", "Detailed examination findings, vitals, and care instructions."],
            ["Consultations.follow_up", "DATETIME2(3)", "NULL", "Recommended clinical review/follow-up date."],
            ["Consultations.status / finalised_at", "VARCHAR(10) / DATETIME2(3)", "CK (DRAFT, FINAL)", "Once set to FINAL, diagnosis and notes become permanently immutable."],
            ["ConsultationAddenda.addendum_id", "BIGINT IDENTITY(1,1)", "PK, FK -> Consultations", "Append-only post-finalization clinical note storing author_user_id, reason NVARCHAR(300), text NVARCHAR(MAX), created_at."],
        ]),
        ("4.9", "Data Dictionary — dbo.Prescriptions, dbo.PrescriptionItems & dbo.ClinicMedicines", [
            ["Prescriptions.prescription_id", "BIGINT IDENTITY(1,1)", "PK, NOT NULL", "Internal primary key of electronic prescription header."],
            ["Prescriptions.prescription_code", "CHAR(32) BIN2", "UQ, CK Alphanumeric", "32-char CSPRNG code under Latin1_General_100_BIN2 case-sensitive binary collation."],
            ["Prescriptions.consultation_id", "BIGINT", "FK -> Consultations", "Links prescription to its originating clinical consultation, patient_id, clinic_id, and doctor_id."],
            ["Prescriptions.issue_date / expiry_date", "DATETIME2(3)", "NOT NULL, UTC", "Issuance timestamp and mandatory expiration boundary enforced by PharmacyService."],
            ["Prescriptions.status", "VARCHAR(10)", "CK (VALID, CANCELLED)", "Allows clinic/doctor revocation with cancel_reason, cancelled_at, and cancelled_by."],
            ["PrescriptionItems.prescription_item_id", "BIGINT IDENTITY(1,1)", "PK, FK -> Prescriptions", "Line item storing medicine, dosage, frequency, duration, instructions, and ROWVERSION."],
            ["PrescriptionItems.quantity / dispensed", "INT", "CK (0 <= disp <= qty)", "Prescribed units (>0) and cumulative quantity_dispensed across partial pharmacy fills."],
            ["PrescriptionItems.lifetime", "BIT", "NOT NULL DEFAULT 0 (V007)", "Marks chronic/lifetime maintenance medications in patient history."],
            ["ClinicMedicines.clinic_medicine_id", "BIGINT IDENTITY(1,1)", "PK, UQ(clinic,med,dosage)", "Clinic formulary (V005/V006): medicine, dosage, default_frequency, default_duration, default_quantity, default_form, default_instructions."],
        ]),
        ("4.10", "Data Dictionary — dbo.Pharmacies, dbo.PharmacyUsers, dbo.PharmacyVerifications & dbo.PharmacyMedicinePrices", [
            ["Pharmacies.pharmacy_id", "BIGINT IDENTITY(1,1)", "PK, UQ(licence_number)", "Enrolled retail pharmacy tenant with public_uuid, name, licence_number, address, status (PENDING/ACTIVE/SUSPENDED)."],
            ["PharmacyUsers.pharmacy_user_id", "BIGINT IDENTITY(1,1)", "PK, UQ(user_id)", "Maps Users account to a Pharmacy with role (PHARMACY_ADMIN, PHARMACIST) and status (ACTIVE, DISABLED)."],
            ["PharmacyVerifications.verification_id", "UNIQUEIDENTIFIER", "PK DEFAULT NEWID()", "Immutable verification token & log storing pharmacy_id, pharmacy_user_id, prescription_id, SHA-256 attempt_hash BINARY(32), outcome (6 values), verified_at, ip_address."],
            ["PharmacyMedicinePrices.price_id", "BIGINT IDENTITY(1,1)", "PK, UQ(pharmacy_id,medicine)", "Self-learning per-pharmacy price catalog (V009) storing unit_price DECIMAL(12,2) >= 0 and updated_at."],
        ]),
        ("4.11", "Data Dictionary — dbo.PrescriptionDispenses, dbo.PrescriptionDispenseItems & dbo.PharmacyFinanceEntries", [
            ["PrescriptionDispenses.dispense_id", "BIGINT IDENTITY(1,1)", "PK, UQ(public_uuid)", "POS sale & dispense receipt linked to prescription_id, pharmacy_id, pharmacy_user_id, and single-use verification_id."],
            ["subtotal / discount / tax_amount / total", "DECIMAL(12,2)", "NOT NULL, CK >= 0 (V009)", "Exact BigDecimal monetary totals plus tax_rate DECIMAL(5,2), payment_method, amount_tendered, and change_due."],
            ["status / void metadata", "VARCHAR(10)", "CK (COMPLETED, VOIDED)", "Enforces voided_at, voided_by, and void_reason whenever status='VOIDED'."],
            ["PrescriptionDispenseItems.dispense_item_id", "BIGINT IDENTITY(1,1)", "PK, UQ(dispense,rx_item)", "Records quantity_dispensed (>0), unit_price DECIMAL(12,2), and line_total DECIMAL(12,2) per medicine line."],
            ["PharmacyFinanceEntries.entry_id", "BIGINT IDENTITY(1,1)", "PK, IX(pharmacy_id,date)", "Accounting ledger (V008/V009): entry_type (INCOME/EXPENSE), amount DECIMAL(18,2), description, entry_date, created_by, and FK dispense_id."],
        ]),
        ("4.12", "Data Dictionary — Enrollment, Patient Linking, AuthTokens, Outbox, SMTP & Audit Tables", [
            ["ClinicEnrollments.enrollment_id", "BIGINT IDENTITY(1,1)", "PK, FK -> Clinics, Users", "Tracks clinic onboarding by SUPER_ADMIN, invite_token_hash BINARY(32), status, enrolled_at, activated_at."],
            ["ClinicPatients.clinic_patient_id", "BIGINT IDENTITY(1,1)", "PK, UQ(clinic_id,patient_id)", "Many-to-many link authorizing a clinic to view/manage a linked patient chart."],
            ["PatientLinkRequests.link_request_id", "BIGINT IDENTITY(1,1)", "PK, IX(clinic,status)", "Stores patient_id, clinic_id, method (CLINIC_APPROVAL/OTP/CODE), status (PENDING/APPROVED/REJECTED/EXPIRED), clinic_patient_record_id, resolved_by, resolution_note."],
            ["AuthTokens.token_id", "BIGINT IDENTITY(1,1)", "PK, UX(token_hash)", "Single-use token vault: user_id, purpose (VERIFY_EMAIL, RESET_PASSWORD, STAFF_INVITE, EMAIL_CHANGE), SHA-256 token_hash BINARY(32), expires_at, used_at."],
            ["EmailLogs.email_log_id", "BIGINT IDENTITY(1,1)", "PK, UQ(idempotency_key)", "Transactional email outbox (V001/V002): type, recipient, subject, body, status (QUEUED/SENT/FAILED), attempts, next_attempt_at, last_error, sent_at."],
            ["SmtpSettings.settings_id", "BIGINT IDENTITY(1,1)", "PK", "Platform SMTP relay config: host, port, security (NONE/STARTTLS/SSL), username, AES-GCM password_encrypted VARBINARY(512), from_address, active, last_test_result."],
            ["AuditLogs.audit_id", "BIGINT IDENTITY(1,1)", "PK, IX(patient_id,actor)", "Forensic security & clinical log: actor_user_id, role, action, resource_type, resource_id, patient_id, ip_address, outcome (SUCCESS/FAILURE/DENIED), created_at."],
        ]),
    ]

    for tnum, tcap, trows in dd_tables:
        b.table(tnum, tcap, ["Column / Object", "SQL Data Type", "Constraints & Indexes", "Clinical / Technical Purpose"],
                trows, col_widths=[1.45, 1.15, 1.25, 2.17])

    b.h2("4.8", "Input Design, Output Design and Corporate ERP UI Design System")
    b.p(
        "To eliminate clerical errors across high-volume reception desks, consulting rooms, and pharmacy counters, "
        "IMS implements a unified server-rendered design system defined in static/css/app.css, templates/layout/base.html, "
        "and templates/layout/auth.html."
    )
    b.bullet("Input Design & Dual-Layer Validation:",
             "Every HTML form binds to a dedicated Jakarta Bean Validation DTO (such as RegistrationForm, "
             "ClinicEnrollmentForm, PatientProfileForm, BookingForm, ConsultationForm, PrescriptionForm, and "
             "LinkApprovalForm). Inputs group related clinical fields logically, preserve user entries on validation "
             "errors, trim whitespace server-side, attach automatic Spring Security _csrf hidden tokens, and use "
             "explicit confirmation checkboxes on irreversible actions such as patient record merges and consultation finalization.")
    b.bullet("Output Design & Clinical Readability:",
             "Outputs combine top-of-page KPI metric cards (KpiCard record), semantic color-coded status pills "
             "(ACTIVE/VALID in #E3F4EA green, PENDING/PARTIAL in #FFF1DB amber, SUSPENDED/CANCELLED in #FBE7E5 red), "
             "monospace high-contrast 32-character prescription banners, printable POS dispense receipts showing "
             "itemized unit prices, discounts, tax, tendered cash, and change due, and exportable RFC-4180 CSV "
             "formulary files.")
    b.bullet("Consistent ERP Navigation & Breadcrumb Engine:",
             "NavigationService dynamically computes role-specific left-sidebar items and hierarchical breadcrumbs "
             "(Home / Parent Module / Current Detail) alongside a sticky 56 px #0F2744 top bar displaying the active "
             "organization name, role badge, user avatar chip, and one-click session sign-out.")

    b.p(
        "Complementing the relational schema, all state machines, roles, outcomes, and security modes in "
        "com.clinicflow.domain.enums are modeled as strongly typed Java enums mapped via @Enumerated(EnumType.STRING) "
        "and enforced at the database layer via matching SQL CHECK constraints (Table 4.13)."
    )
    b.table(
        "4.13", "Complete Java Domain Enumeration Reference (com.clinicflow.domain.enums.*)",
        ["Java Enum Class", "Permitted Constants", "Governing Entity / Table"],
        [
            ["Role", "SUPER_ADMIN, CLINIC_ADMIN, DOCTOR, PATIENT, PHARMACY_ADMIN, PHARMACIST", "Users.role / SecurityFilterChain"],
            ["UserStatus", "PENDING, ACTIVE, LOCKED, DISABLED", "Users.status"],
            ["ClinicStatus", "PENDING_ENROLLMENT, ACTIVE, SUSPENDED, REJECTED", "Clinics.status"],
            ["DoctorStatus", "ACTIVE, INACTIVE", "Doctors.status"],
            ["PatientSource", "SELF, CLINIC", "Patients.created_source"],
            ["Gender", "MALE, FEMALE, OTHER, UNKNOWN", "Patients.gender"],
            ["AppointmentStatus", "BOOKED, CONFIRMED, IN_CONSULTATION, COMPLETED, CANCELLED, NO_SHOW", "Appointments.status"],
            ["ConsultationStatus", "DRAFT, FINAL", "Consultations.status"],
            ["PrescriptionStatus", "VALID, CANCELLED", "Prescriptions.status"],
            ["PharmacyStatus", "PENDING, ACTIVE, SUSPENDED", "Pharmacies.status"],
            ["PharmacyUserRole", "PHARMACY_ADMIN, PHARMACIST", "PharmacyUsers.role_in_pharmacy"],
            ["PharmacyUserStatus", "PENDING, ACTIVE, DISABLED", "PharmacyUsers.status"],
            ["VerificationOutcome", "VALID, NOT_FOUND, CANCELLED, EXPIRED, FULLY_DISPENSED", "PharmacyVerifications.outcome"],
            ["DispenseStatus", "PARTIAL, FULL", "PrescriptionDispenses.dispense_status"],
            ["AuthTokenPurpose", "VERIFY_EMAIL, RESET_PASSWORD, STAFF_INVITE, EMAIL_CHANGE", "AuthTokens.purpose"],
            ["EmailStatus", "QUEUED, SENT, FAILED", "EmailLogs.status"],
            ["SmtpSecurity", "NONE, STARTTLS, SSL_TLS", "SmtpSettings.security_mode"],
            ["EnrollmentStatus", "SUBMITTED, APPROVED, REJECTED", "ClinicEnrollments.status"],
            ["LinkRequestMethod", "PATIENT_PORTAL, CLINIC_DESK, EMAIL_MATCH", "PatientLinkRequests.method"],
            ["LinkRequestStatus", "PENDING, APPROVED, REJECTED, EXPIRED", "PatientLinkRequests.status"],
            ["AuditOutcome", "SUCCESS, DENIED, FAILURE", "AuditLogs.outcome"],
        ],
        col_widths=[1.45, 2.75, 1.82]
    )

    b.p(
        "Table 4.14 documents the Corporate ERP visual design tokens engineered in "
        "src/main/resources/static/css/app.css (1,882 lines) governing layout geometry, contrast-verified "
        "healthcare color semantics, and typography across all 61 Thymeleaf templates."
    )
    b.table(
        "4.14", "Corporate ERP User Interface Design System Tokens (static/css/app.css)",
        ["CSS Custom Property", "Token Value", "UI Architectural Application"],
        [
            ["--c-primary / --c-primary-hover", "#1E4E8C / #163B6B", "Primary medical navy buttons, active links, focus rings & table accents"],
            ["--c-sidebar-bg / --c-sidebar-active", "#0F2744 / #1E4E8C", "Fixed dark-navy left ERP navigation rail & active route indicator"],
            ["--c-accent / --c-accent-soft", "#0E8A8A / #E6F4F4", "Clinical teal accent badges, secondary highlights & active icons"],
            ["--c-bg / --c-surface / --c-surface-muted", "#F4F6F9 / #FFFFFF / #F8FAFC", "App workspace canvas, elevated white cards & zebra table headers"],
            ["--c-success / --c-warning / --c-danger", "#157F3B / #B45309 / #B91C1C", "WCAG AA status pills (ACTIVE/VALID, PENDING/PARTIAL, CANCELLED/EXPIRED)"],
            ["--font-sans / --font-mono", "Inter, Segoe UI / Cascadia Mono, Consolas", "Crisp UI typography + unambiguous monospace 32-char Rx & public IDs"],
            ["--topbar-h / --sidebar-w / --subbar-h", "56px / 256px / 40px", "Fixed ERP shell dimensions ensuring stable zero-shift workspace"],
        ],
        col_widths=[1.85, 1.65, 2.52]
    )
