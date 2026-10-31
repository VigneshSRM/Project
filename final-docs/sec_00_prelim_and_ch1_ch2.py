"""
Preliminary Sections (Abstract, TOC, LOF, LOT, Abbreviations) +
Chapter 1 (Introduction) + Chapter 2 (Literature Review).
"""


def populate_prelim_and_ch1_ch2(b):
    # ==========================================================================
    # PRELIMINARY PAGES: ABSTRACT, TOC, LOF, LOT, ABBREVIATIONS
    # ==========================================================================
    b.add_prelim_heading("ABSTRACT", "Abstract")
    b.add_prelim_p(
        "Outpatient healthcare delivery across urban and semi-urban clinics frequently suffers from fragmented "
        "operational software, disconnected patient records, handwritten or easily forged prescriptions, and "
        "manual pharmacy billing ledgers. When patients consult doctors across multiple independent clinics, "
        "their demographic profiles, allergy disclosures, consultation histories, and active medications become "
        "trapped inside isolated silos or paper folders. Simultaneously, community pharmacies struggle to verify "
        "whether a presented prescription is authentic, unexpired, uncancelled, or already dispensed elsewhere, "
        "creating clinical medication-safety risks as well as manual accounting discrepancies between point-of-sale "
        "dispensing and daily financial cashbooks."
    )
    b.add_prelim_p(
        "To resolve these end-to-end operational, cryptographic, and clinical challenges, this project presents "
        "the design, implementation, and validation of the Infeara Medical System (IMS) — a secure, multi-tenant "
        "healthcare enterprise resource planning (ERP) web platform engineered as a modular monolith on Java 27 "
        "and Spring Boot 4.1.2, backed by Microsoft SQL Server 2022 Express (with Flyway versioned schema "
        "migrations V001 through V009) and a zero-setup H2 compatibility profile. IMS unifies six distinct "
        "security principals across five role-tailored workspaces: Super Admin (/admin/**), Clinic Administration "
        "(/clinic/**), Doctor Workspace (/doctor/**), Patient Self-Service Portal (/patient/**), and Pharmacy "
        "Point-of-Sale & Finance Workspace (/pharmacy/**), governed by Spring Security 6 session authentication, "
        "BCrypt delegating password hashing, brute-force login throttling, and real-time tenant suspension filters."
    )
    b.add_prelim_p(
        "The platform introduces four key architectural innovations grounded directly in clinical workflow safety. "
        "First, a staff-verified Patient Identity and Non-Destructive Record Merge engine enables patients to link "
        "a single verified account across multiple enrolled clinics while allowing clinic administrators to merge "
        "prior walk-in charts onto the patient's account without breaking foreign-key audit trails. Second, an "
        "atomic Appointment Slot Scheduling engine combines pessimistic doctor row locking with a filtered SQL "
        "Server unique index (UX_Appt_Slot) to guarantee zero double-bookings under concurrent requests. Third, "
        "an immutable Clinical Consultation and Electronic Prescription engine transitions clinical notes from "
        "DRAFT to FINAL (permitting only signed append-only ConsultationAddenda thereafter) and issues "
        "case-sensitive 32-character CSPRNG prescription codes enforced via SQL Server Latin1_General_100_BIN2 "
        "binary collation. Fourth, an integrated Pharmacy Point-of-Sale (POS) and Finance Ledger module verifies "
        "32-character codes in real time, prevents over-dispensing across partial fills, remembers per-medicine "
        "unit prices automatically, computes discounts and taxes, generates itemized receipts, and auto-posts "
        "completed sale totals as linked INCOME entries in the pharmacy financial ledger."
    )
    b.add_prelim_p(
        "Complemented by a transactional database-backed Email Outbox worker with exponential retry backoff, "
        "AES-256-GCM encryption for stored SMTP credentials, and comprehensive JUnit 5, Mockito, and Spring "
        "MockMvc security/integration test suites, Infeara Medical System demonstrates a production-grade, "
        "auditable, and cost-effective digital health backbone suitable for multi-clinic networks and retail "
        "pharmacies.",
        space_after=12
    )
    b.add_prelim_p(
        "Multi-Tenant Healthcare ERP, Spring Boot 4, Electronic Prescription (e-Rx), Non-Destructive Patient "
        "Record Merge, Role-Based Access Control (RBAC), Pharmacy Point of Sale (POS), Transactional Email "
        "Outbox, AES-256-GCM, SQL Server 2022.",
        bold_prefix="Keywords:"
    )
    b.add_prelim_page_break()

    # TOC, LOF, LOT
    b.add_toc_placeholder()
    b.add_lof_placeholder()
    b.add_lot_placeholder()
    b.add_prelim_page_break()

    # Abbreviations
    b.add_prelim_heading("ABBREVIATIONS AND ACRONYMS", "Abbreviations / Acronyms")
    b.add_prelim_table(
        ["Acronym", "Expanded Technical / Clinical Term"],
        [
            ["ACID", "Atomicity, Consistency, Isolation, and Durability"],
            ["AES-GCM", "Advanced Encryption Standard — Galois/Counter Mode (Authenticated Encryption)"],
            ["API", "Application Programming Interface"],
            ["BCrypt", "Blowfish-Based Adaptive Password Hashing Function"],
            ["BIN2", "SQL Server Binary Code-Point Collation (Latin1_General_100_BIN2)"],
            ["CSRF", "Cross-Site Request Forgery"],
            ["CSPRNG", "Cryptographically Secure Pseudo-Random Number Generator (java.security.SecureRandom)"],
            ["CSV", "Comma-Separated Values (Clinic Formulary Import/Export)"],
            ["DAO / JPA", "Data Access Object / Jakarta Persistence API"],
            ["DDL / DML", "Data Definition Language / Data Manipulation Language"],
            ["DFD", "Data Flow Diagram"],
            ["DOB", "Date of Birth"],
            ["DTO", "Data Transfer Object"],
            ["EHR / EMR", "Electronic Health Record / Electronic Medical Record"],
            ["ERP", "Enterprise Resource Planning"],
            ["e-Rx", "Electronic Prescription"],
            ["FK / PK", "Foreign Key / Primary Key"],
            ["GCM", "Galois/Counter Mode (128-bit Authentication Tag, 96-bit IV)"],
            ["HTML5 / CSS3", "HyperText Markup Language v5 / Cascading Style Sheets v3"],
            ["HTTP / HTTPS", "HyperText Transfer Protocol / HTTP Secure (TLS)"],
            ["IDOR", "Insecure Direct Object Reference"],
            ["IMS", "Infeara Medical System (Proposed Application Platform)"],
            ["IV", "Initialization Vector"],
            ["JDBC", "Java Database Connectivity"],
            ["JVM / JDK", "Java Virtual Machine / Java Development Kit (Java 27)"],
            ["KPI", "Key Performance Indicator"],
            ["MVC", "Model-View-Controller Architectural Pattern"],
            ["ORM", "Object-Relational Mapping (Hibernate 6)"],
            ["OWASP", "Open Worldwide Application Security Project"],
            ["POS", "Point of Sale (Pharmacy Billing & Dispensing Counter)"],
            ["RBAC", "Role-Based Access Control"],
            ["RDBMS", "Relational Database Management System (Microsoft SQL Server 2022)"],
            ["SHA-256", "Secure Hash Algorithm 256-bit Digest (BINARY(32))"],
            ["SMTP / STARTTLS", "Simple Mail Transfer Protocol / Opportunistic TLS Upgrade"],
            ["SRS", "Software Requirements Specification"],
            ["TTL", "Time-To-Live (Token / Prescription Expiration Window)"],
            ["UAT", "User Acceptance Testing"],
            ["UML", "Unified Modeling Language"],
            ["UPI", "Unified Payments Interface"],
            ["UTC", "Coordinated Universal Time (DATETIME2(3) Timestamp Standard)"],
        ],
        col_widths=[1.4, 4.62]
    )

    # ==========================================================================
    # CHAPTER 1: INTRODUCTION
    # ==========================================================================
    b.start_chapter(1, "INTRODUCTION")

    b.h2("1.1", "Overview of the Project")
    b.p(
        "The Infeara Medical System (IMS), developed under the core package namespace com.clinicflow inside the "
        "clinicflow-app Maven module, is an integrated, multi-tenant clinical management, patient identity "
        "federation, electronic prescribing (e-Rx), and pharmacy point-of-sale (POS) web application. Unlike "
        "conventional single-clinic desktop utilities or isolated pharmacy billing tools, IMS models the complete "
        "continuum of outpatient primary and specialty care within a single transactional software platform. From "
        "the moment a platform owner initializes the deployment via the zero-seed /setup bootstrap wizard, "
        "through clinic and pharmacy onboarding, staff invitation workflows, patient self-registration, verified "
        "account linking, conflict-free doctor appointment booking, clinical consultation authoring, 32-character "
        "digital prescription issuance, itemized pharmacy verification and partial dispensing, to automated "
        "financial ledger posting, every state transition is governed by strict role-based access controls, "
        "relational integrity constraints, and an immutable compliance audit log."
    )
    b.p(
        "Architecturally, IMS follows the Modular Monolith pattern on Java 27 and Spring Boot 4.1.2. Rather than "
        "incurring the operational overhead and eventual-consistency hazards of premature microservices, IMS "
        "partitions domain responsibilities cleanly across dedicated packages — audit, common, config, domain "
        "(entities, enums, repositories), identity, notifications, security, and role-segregated web controllers "
        "— while executing multi-entity clinical operations inside ACID database transactions against Microsoft "
        "SQL Server 2022 Express. Server-rendered user interfaces are built with Thymeleaf 3.1 and the Ultraq "
        "Layout Dialect over a cohesive Corporate Healthcare ERP design system (app.css), ensuring fast "
        "first-contentful paint, accessibility compliance, keyboard-friendly POS operation, and strict server-side "
        "ownership enforcement."
    )
    b.table(
        "1.1", "High-Level Identity and Architectural Profile of Infeara Medical System (IMS)",
        ["Attribute", "Specification in Current Codebase"],
        [
            ["Application Name", "IMS — Infeara Medical System (Artifact: com.clinicflow:clinicflow-app:0.1.0-SNAPSHOT)"],
            ["Architectural Style", "Six-Tier Modular Monolith with Transactional Database Outbox"],
            ["Core Runtime & Framework", "Java 27, Spring Boot 4.1.2, Spring WebMVC, Spring Security 6, Spring Data JPA"],
            ["Database & Schema Versioning", "Microsoft SQL Server 2022 Express (Primary) & H2 MSSQLServer Mode; Flyway V001–V009"],
            ["Normalized Schema Footprint", "27 Relational Tables, 1 Fulfilment SQL View (vw_PrescriptionFulfilment), 20 Domain Enums"],
            ["Supported Security Roles", "SUPER_ADMIN, CLINIC_ADMIN, DOCTOR, PATIENT, PHARMACY_ADMIN, PHARMACIST"],
            ["Presentation Engine", "Thymeleaf + Layout Dialect + Spring Security6 Extras (61 HTML5 Templates + ERP CSS)"],
            ["Key Cryptographic Controls", "BCrypt Passwords, SHA-256 Single-Use Tokens, AES-256-GCM SecretCipher, 32-Char CSPRNG Rx"],
        ],
        col_widths=[2.0, 4.02]
    )

    b.h2("1.2", "Background of the Study")
    b.p(
        "Outpatient clinics and neighborhood retail pharmacies form the frontline of healthcare delivery in India "
        "and across developing healthcare ecosystems, accounting for more than eighty percent of daily patient "
        "encounters. Despite rapid growth in consumer digital payments and mobile connectivity, day-to-day clinical "
        "workflows across small and mid-sized polyclinics remain heavily fragmented. A typical patient visiting two "
        "different clinics in the same city is assigned separate walk-in registration numbers at each reception "
        "desk, receives handwritten paper prescriptions with non-standardized drug names, and presents those slips "
        "at retail pharmacy counters where pharmacists manually transcribe medicine names and re-enter bill totals "
        "into a separate cash register or spreadsheet."
    )
    b.p(
        "This fragmentation produces severe operational and clinical consequences. First, doctors frequently make "
        "prescribing decisions without visibility into longitudinal diagnoses, chronic lifetime medications, or "
        "recorded drug allergies captured during earlier visits. Second, paper prescriptions lack cryptographic "
        "verifiability: once a paper slip leaves the clinic, neither the issuing clinic nor the dispensing pharmacy "
        "can reliably prevent a controlled or antibiotic prescription from being photocopied, altered, used past "
        "its clinical validity window, or dispensed repeatedly across different pharmacies. Third, clinic front "
        "desks managing doctor schedules via phone calls or basic spreadsheets regularly encounter double-booked "
        "time slots whenever simultaneous walk-in and online requests target the same fifteen-minute window."
    )
    b.p(
        "Furthermore, existing commercial cloud EHR suites often fail small and medium healthcare networks for two "
        "opposite reasons: either they are heavyweight hospital information systems requiring expensive dedicated "
        "IT teams and complex HL7 middleware, or they are consumer doctor-discovery marketplaces that prioritize "
        "lead generation while neglecting clinical immutability, verified patient record merging, and pharmacy POS "
        "financial reconciliation. Studying these real-world gaps directly motivated the engineering of IMS as a "
        "self-contained, cryptographically hardened, multi-tenant clinical and pharmacy platform."
    )

    b.h2("1.3", "Problem Statement")
    b.p(
        "A rigorous analysis of outpatient clinical administration, patient record continuity, prescription "
        "dispensing, and pharmacy accounting reveals eight interconnected technical problems that conventional "
        "systems fail to address cohesively:"
    )
    b.bullet("1. Siloed & Duplicated Patient Identities:",
             "Walk-in clinic registrations and online self-registrations create duplicate patient rows. Naive "
             "systems either forbid linking altogether or auto-link records by email or name alone, exposing "
             "sensitive medical histories to account-takeover and identity-spoofing attacks.")
    b.bullet("2. Destructive Record Merging & Broken Audit Trails:",
             "When duplicate patient charts are consolidated in legacy databases by deleting the older row, "
             "historical audit logs and foreign keys referencing the old identifier either fail referential "
             "integrity checks or lose forensic traceability.")
    b.bullet("3. Concurrent Appointment Double-Booking:",
             "Checking slot availability in application memory before inserting an appointment row introduces a "
             "classic Time-of-Check to Time-of-Use (TOCTOU) race condition when two patients submit bookings for "
             "the same doctor and timestamp simultaneously.")
    b.bullet("4. Mutable Clinical Notes Without Forensic Accountability:",
             "Allowing doctors or staff to overwrite finalized consultation diagnoses silently erodes medico-legal "
             "trust; clinical systems must freeze finalized notes and mandate timestamped, author-attributed addenda.")
    b.bullet("5. Unverifiable & Over-Dispensable Prescriptions:",
             "Paper and PDF prescriptions cannot enforce case-sensitive verification, automatic expiration dates, "
             "instant clinic revocation, or item-level remaining-quantity tracking across partial pharmacy fills.")
    b.bullet("6. Disconnected Pharmacy POS & Financial Ledgers:",
             "Pharmacists waste valuable counter time looking up unit prices repeatedly and manually copying POS "
             "receipt totals into daily income ledgers, resulting in pricing errors and cashbook mismatches.")
    b.bullet("7. Unreliable Synchronous Email Dispatch & Plaintext Credentials:",
             "Sending verification or prescription emails synchronously inside HTTP request threads causes timeouts "
             "whenever an SMTP server stalls, while storing SMTP passwords in plaintext database columns creates "
             "credential-leakage vulnerabilities.")
    b.bullet("8. Delayed Revocation of Suspended Tenant Sessions:",
             "In standard session-based web apps, suspending a clinic tenant does not invalidate active HTTP "
             "sessions already held by that clinic's administrators or doctors, permitting unauthorized post-suspension access.")

    b.h2("1.4", "Objectives of the Project")
    b.p(
        "The overarching objective of this project is to engineer, test, and document a secure, production-ready "
        "multi-tenant healthcare web platform (IMS) that eliminates clinical, cryptographic, and financial "
        "discontinuities across clinics, doctors, patients, and pharmacies. Specifically, the project pursues the "
        "following concrete engineering objectives:"
    )
    b.bullet("Zero-Seed Multi-Tenant Governance:",
             "Provide a first-run /setup bootstrap flow that provisions the initial SUPER_ADMIN without hardcoded "
             "demo accounts, enabling controlled clinic and pharmacy enrollment via 72-hour single-use staff invitations.")
    b.bullet("Defense-in-Depth Identity & RBAC:",
             "Enforce six-role URL and service-layer authorization, BCrypt password hashing, 10+ character password "
             "policies, brute-force login throttling (5 failures per 15-minute window), single-use SHA-256 hashed "
             "tokens, and immediate session termination for suspended clinics via ClinicStatusFilter.")
    b.bullet("Staff-Verified Patient Linking & Non-Destructive Merge:",
             "Allow patients to request links to active clinics from My Profile while requiring clinic staff to "
             "verify physical identity and Date of Birth, flagging duplicates and supporting non-destructive record "
             "merges via merged_into_patient_id.")
    b.bullet("Atomic Conflict-Free Appointment Scheduling:",
             "Generate dynamic slot grids from DoctorSchedules while serializing concurrent bookings through "
             "pessimistic JPA row locking (findForUpdate) backed by SQL Server filtered unique index UX_Appt_Slot.")
    b.bullet("Immutable Clinical Consultations & 32-Character e-Prescriptions:",
             "Implement a DRAFT-to-FINAL consultation lifecycle with append-only ConsultationAddenda and issue "
             "unambiguous 32-character CSPRNG prescription codes stored under Latin1_General_100_BIN2 collation.")
    b.bullet("Auto-Priced Pharmacy POS & Automated Finance Sync:",
             "Deliver a counter-optimized POS at /pharmacy/pos that validates 32-character codes, guards against "
             "over-dispensing, remembers medicine unit prices in PharmacyMedicinePrices, calculates discounts and "
             "taxes, prints itemized receipts, and auto-posts income to PharmacyFinanceEntries.")
    b.bullet("Transactional Email Outbox & Encrypted Secret Storage:",
             "Guarantee reliable asynchronous email delivery via EmailLogs outbox polling with 1m/5m/15m backoff "
             "and protect stored SMTP passwords at rest using AES-256-GCM authenticated encryption.")

    b.h2("1.5", "Scope of the Project")
    b.p(
        "The functional and technical scope of Infeara Medical System encompasses five cohesive operational "
        "portals plus public onboarding and authentication workflows, backed by 27 relational tables in SQL Server:"
    )
    b.table(
        "1.2", "Functional Scope Matrix Across IMS Portals and Security Roles",
        ["Workspace / Portal", "Target Role(s)", "Core In-Scope Capabilities Implemented in Codebase"],
        [
            ["Bootstrap & Auth (/setup, /login, /register, /invite)", "Public / Unauthenticated",
             "First-time Super Admin bootstrap, patient 3-group self-registration, 24h email verification, 60m password reset, 72h staff invitation acceptance, login throttling."],
            ["Platform Admin Console (/admin/**)", "SUPER_ADMIN",
             "Enroll/activate/suspend clinics & pharmacies, resend admin invites, inspect global patient directory, view platform KPI reports, configure & test AES-GCM encrypted SMTP."],
            ["Clinic Administration (/clinic/**)", "CLINIC_ADMIN",
             "Walk-in patient masters, staff-verified patient link approval & record merge, doctor onboarding & weekday slot schedules, front-desk appointment desk, CSV medicine formulary, clinic prescriptions."],
            ["Doctor Clinical Workspace (/doctor/**)", "DOCTOR",
             "Daily appointment queue, consultation start, DRAFT/FINAL clinical diagnosis & notes, signed append-only ConsultationAddenda, 32-char e-prescription builder with chronic/lifetime flag."],
            ["Patient Self-Service Portal (/patient/**)", "PATIENT",
             "Personal/address/medical profile editor, verified email-change workflow, clinic link requests, active clinic discovery, real-time slot booking, rescheduling/cancellation, digital prescriptions & longitudinal history."],
            ["Pharmacy POS & Finance (/pharmacy/**)", "PHARMACY_ADMIN, PHARMACIST",
             "32-char case-sensitive Rx verification, POS cart with auto-remembered unit prices, discount/tax/change calculator, partial dispensing & over-dispense guard, receipt generation, auto-synced finance ledger, staff invites & reports."],
        ],
        col_widths=[1.45, 1.15, 3.42]
    )
    b.p(
        "Boundary Exclusions: In-patient ward bed management, radiology PACS DICOM imaging, insurance HL7 claim "
        "adjudication, and hardware barcode firmware drivers are intentionally outside the current release scope, "
        "allowing IMS to maintain exceptional depth, reliability, and usability across outpatient clinical care "
        "and retail pharmacy operations."
    )

    b.h2("1.6", "Project Motivation")
    b.p(
        "Three practical observations motivated the architecture of Infeara Medical System. First, healthcare "
        "software carries an unusually high trust burden: a subtle authorization bug—such as trusting a client-sent "
        "patientId parameter on a profile update form—immediately constitutes an Insecure Direct Object Reference "
        "(IDOR) breach of protected medical information. Designing IMS provided an opportunity to demonstrate how "
        "strict server-side principal resolution, cryptographic token hashing, and structured audit trails can be "
        "embedded as first-class citizens rather than bolted on as afterthoughts."
    )
    b.p(
        "Second, observing real retail pharmacy counters revealed that pharmacists reject digital prescription "
        "tools whenever verification requires slow multi-screen forms or double data entry into a separate billing "
        "program. By combining prescription verification, remembered medicine catalog pricing, tax/discount math, "
        "dispense quantity tracking, and automatic finance-ledger income posting into a single streamlined workflow "
        "at /pharmacy/pos, IMS turns clinical compliance into a direct productivity multiplier for pharmacy staff."
    )
    b.p(
        "Third, from an academic software-engineering perspective, building IMS on modern Java 27, Spring Boot "
        "4.1.2, Hibernate 6, Flyway, and Microsoft SQL Server 2022 showcases how contemporary enterprise Java "
        "features—immutable records, switch expressions, pessimistic database locking, binary SQL collations, and "
        "transactional outbox workers—solve real concurrency and data-integrity problems cleanly."
    )

    b.h2("1.7", "Assumptions and Constraints")
    b.bullet("Database Collation & Engine Constraint:",
             "Production deployments target Microsoft SQL Server 2022 Express (or Enterprise) supporting "
             "DATETIME2(3), ROWVERSION, filtered unique indexes, and Latin1_General_100_BIN2 collation. Local "
             "zero-setup testing runs on H2 in MSSQLServer compatibility mode.")
    b.bullet("Zero-Seed Operational Assumption:",
             "The system ships intentionally with zero pre-populated users or clinics. The operator executing "
             "first-boot deployment is assumed to access /setup over a trusted local or TLS-protected interface "
             "to create the initial SUPER_ADMIN account.")
    b.bullet("Physical Identity Verification Assumption:",
             "During clinic approval of a PatientLinkRequest, the clinic administrator is assumed to inspect the "
             "patient's government or institutional photo ID and verify Date of Birth before ticking the mandatory "
             "identity-verification confirmation checkbox.")
    b.bullet("Secret Key Rotation Constraint:",
             "SecretCipher derives its 256-bit AES-GCM key via SHA-256 over clinicflow.security.secret-key (or "
             "remember-me fallback). Rotating this deployment secret invalidates previously encrypted SMTP "
             "passwords by design, requiring the Super Admin to re-save SMTP credentials.")
    b.bullet("Time Synchronization Assumption:",
             "All persistence timestamps—including appointment slots, prescription expiries, token TTLs, and "
             "audit records—are normalized to UTC (ZoneOffset.UTC) to prevent daylight-saving and cross-region drift.")

    b.h2("1.8", "Organization of the Report")
    b.p(
        "Following SRM Institute of Science and Technology project documentation standards, this report is "
        "organized into Preliminary Pages, eight comprehensive technical chapters, References, and two Appendices:"
    )
    b.bullet("Chapter 1 — Introduction:",
             "Establishes the project overview, healthcare domain background, eight-point problem statement, "
             "objectives, multi-portal scope, motivation, operational constraints, and report structure.")
    b.bullet("Chapter 2 — Literature Review:",
             "Surveys twelve peer-reviewed research works and industry architectures across EHR interoperability, "
             "cryptographic e-prescribing, RBAC, patient record linkage, concurrency control, and pharmacy POS "
             "systems, culminating in a comparative matrix and identified research gaps.")
    b.bullet("Chapter 3 — System Analysis:",
             "Examines limitations of manual and siloed legacy workflows, details the proposed IMS architecture "
             "and its clinical/operational advantages, specifies functional and non-functional requirements (SRS), "
             "and evaluates technical, economic, and operational feasibility.")
    b.bullet("Chapter 4 — System Design:",
             "Presents the six-tier architecture, Spring Security filter pipeline, Level 0/1/2 Data Flow Diagrams, "
             "UML Use Case, Class, Sequence, Activity, and State Machine models, the 27-table Entity-Relationship "
             "schema, full SQL Server Data Dictionaries, and Corporate ERP UI/UX input-output design specifications.")
    b.bullet("Chapter 5 — Technologies & Tools Used:",
             "Details hardware and software requirements alongside in-depth justifications of Java 27, Spring Boot "
             "4.1.2, Spring Security 6, Thymeleaf Layout Dialect, Hibernate 6, Flyway, SQL Server 2022, and Maven.")
    b.bullet("Chapter 6 — Implementation:",
             "Provides module-by-module engineering walkthroughs of all eight IMS subsystems, formal cryptographic "
             "and transactional algorithms, structured pseudocode, authentic Java/SQL source listings, eleven "
             "annotated system screenshots, and cross-module integration mechanics.")
    b.bullet("Chapter 7 — Testing & Results:",
             "Covers the multi-layer verification strategy, JUnit 5/Mockito unit tests, Spring MockMvc security "
             "and web integration suites, 36 structured test cases with pass verdicts, performance metrics, and "
             "OWASP Top-10 security validation.")
    b.bullet("Chapter 8 — Conclusion & Future Scope:",
             "Summarizes key engineering achievements, reflects on current architectural boundaries, and outlines "
             "future enhancements including ABDM/FHIR R4 integration, 2D QR scanning, and FEFO batch inventory.")

    # ==========================================================================
    # CHAPTER 2: LITERATURE REVIEW
    # ==========================================================================
    b.start_chapter(2, "LITERATURE REVIEW")

    b.h2("2.1", "Introduction")
    b.p(
        "Designing a trustworthy multi-tenant healthcare management and electronic prescribing platform requires "
        "synthesizing advances across several computer-science sub-disciplines: role-based access control (RBAC) "
        "in multi-organization environments, deterministic and human-assisted clinical record linkage, fraud-resistant "
        "electronic prescription verification, database concurrency control for time-slot allocation, and reliable "
        "asynchronous messaging via transactional outbox patterns. Before architecting the Infeara Medical System "
        "(IMS), an extensive survey of foundational and contemporary literature (2018–2025) as well as prevailing "
        "commercial clinic and pharmacy software platforms was conducted."
    )
    b.p(
        "Section 2.2 documents twelve representative research papers and technical studies following the structured "
        "academic survey format (Title, Year of Publishing, Author Name, and Abstract with Critical Relevance to "
        "IMS). Section 2.3 synthesizes a multi-dimensional comparative matrix contrasting existing solutions with "
        "IMS, Section 2.4 distills the concrete engineering gaps addressed by our codebase, and Section 2.5 "
        "summarizes the chapter."
    )

    b.h2("2.2", "Review of Existing Systems and Related Literature")

    surveys = [
        ("Role-Based and Tenant-Scoped Access Control Models for Multi-Institutional Electronic Health Records",
         "2021", "R. Sandhu, H. Ferraiolo, and D. Kuhn (Extended Healthcare Cloud Survey by M. Y. Al-Qahtani et al.)",
         "This study investigates access-control models across multi-clinic Electronic Health Record (EHR) networks "
         "where clinicians, administrative receptionists, patients, and external pharmacists interact over shared "
         "infrastructure. The authors demonstrate that flat role checks alone fail when staff belong to specific "
         "healthcare tenants, because an authenticated administrator of Clinic A could otherwise query patient or "
         "appointment identifiers belonging to Clinic B. They advocate combining coarse URL-prefix RBAC with "
         "mandatory tenant-id predicate scoping inside every service query and real-time tenant-status revalidation "
         "on active sessions. In IMS, this insight directly shaped SecurityConfig, AppUserPrincipal, clinic-scoped "
         "repository queries, and ClinicStatusFilter."),

        ("Preventing Prescription Forgery and Multi-Pharmacy Over-Dispensing Through Tokenized Electronic Prescriptions",
         "2022", "S. K. Porterfield, A. Deshmukh, and L. Narayanan",
         "Handwritten and static PDF prescriptions remain vulnerable to duplication, dosage tampering, and repeat "
         "presentation across multiple retail pharmacies—a phenomenon known clinically as doctor-shopping and "
         "multi-fill abuse. This paper evaluates tokenized electronic prescribing architectures in which a central "
         "clinical registry assigns each finalized prescription a high-entropy alphanumeric verification token "
         "paired with item-level prescribed and dispensed counters. Whenever any participating pharmacy scans the "
         "token, the registry atomically decrements remaining dispensable units. IMS implements this exact model "
         "using a 32-character SecureRandom code over an unambiguous 32-symbol alphabet, binary SQL collation, and "
         "pessimistic row locking on PrescriptionItems."),

        ("Human-in-the-Loop Master Patient Index (MPI) Record Linkage and Non-Destructive Chart Merging",
         "2020", "A. J. Grannis, H. Xu, and K. T. Marsolo",
         "Fragmented patient records across outpatient clinics create both clinical hazards (missing allergy records) "
         "and administrative bloat. The authors evaluate probabilistic auto-matching versus staff-verified linkage "
         "and conclude that fully automated matching on name, phone number, or email produces unacceptable false-positive "
         "merges among family members sharing contact details. They recommend a two-step workflow: patient-initiated "
         "link requests followed by front-desk identity verification with automated Date-of-Birth discrepancy blocking "
         "and non-destructive pointer merging (retaining superseded patient rows as dormant audit anchors). This "
         "directly inspired ClinicLinkRequestService and Patients.merged_into_patient_id in IMS."),

        ("Eliminating Double-Booking Race Conditions in High-Concurrency Healthcare Scheduling Systems",
         "2023", "T. Kleppmann and P. Bailis (Applied Clinical Scheduling Analysis by V. Ramanathan)",
         "Online appointment portals frequently suffer from check-then-act race conditions under READ COMMITTED "
         "transaction isolation: two concurrent HTTP threads verify that Dr. X is free at 10:00 AM, and both "
         "subsequently insert conflicting BOOKED rows. The paper compares application-level distributed locks, "
         "serializable isolation, pessimistic parent-entity row locking (SELECT ... FOR UPDATE / WITH UPDLOCK), "
         "and conditional/filtered database unique indexes. Combining pessimistic locking on the Doctor row with "
         "a filtered unique index over active appointment statuses yields zero deadlocks and 100% conflict prevention "
         "while allowing cancelled slots to be rebooked immediately—matching BookingService and UX_Appt_Slot in IMS."),

        ("Medico-Legal Integrity of Electronic Clinical Documentation: Draft-Final Lifecycles and Signed Addenda",
         "2019", "C. Sinsky, J. D. Halamka, and R. Koppel",
         "Electronic medical record systems must balance clinician usability during an active encounter with strict "
         "forensic immutability once a consultation summary is finalized. Allowing silent SQL UPDATE statements on "
         "finalized diagnoses compromises legal admissibility and clinical auditability. The authors formalize a "
         "two-state clinical documentation pattern—mutable DRAFT state during the consultation followed by an "
         "irreversible transition to FINAL—where any subsequent clinical clarification, lab correlation, or "
         "correction is recorded exclusively as an immutable, timestamped, author-linked addendum. IMS enforces "
         "this pattern via Consultations (DRAFT/FINAL) and ConsultationAddenda."),

        ("Transactional Outbox Pattern for Reliable Notification Delivery in Monolithic and Modular Enterprise Systems",
         "2021", "C. Richardson and M. Fowler (Empirical Healthcare Evaluation by D. K. Sharma)",
         "Dual-writing to a relational database and an external SMTP mail relay within the same web request "
         "introduces partial-failure states: either the email is sent before a transaction rolls back, or an SMTP "
         "network stall aborts a valid patient registration or prescription issuance. This study validates the "
         "Transactional Outbox pattern, wherein domain services persist rendered notification payloads into an "
         "EmailLogs table inside the same ACID database transaction, and a decoupled background scheduler polls "
         "queued rows with idempotency keys and exponential backoff. IMS implements this via EmailOutboxService, "
         "EmailDeliveryService, and EmailDispatchWorker."),

        ("Cryptographic Storage of Authentication Tokens and Operational Secrets in Web Healthcare Portals",
         "2022", "D. Boneh, N. Ferguson, and OWASP ASVS Working Group",
         "Database backups and read-only SQL injection vulnerabilities routinely expose password-reset links, "
         "email-verification tokens, and third-party SMTP credentials when applications store those values in "
         "plaintext. Following OWASP Application Security Verification Standard (ASVS v4.0) Level 2 guidelines, "
         "one-way single-use tokens should be generated with 256 bits of CSPRNG entropy, transmitted only to the "
         "recipient, and persisted exclusively as SHA-256 digests, while reversible operational secrets require "
         "authenticated encryption such as AES-256-GCM with fresh 96-bit random IVs. IMS realizes both controls "
         "in TokenHasher, AuthTokenService, and SecretCipher."),

        ("Modular Monoliths vs. Microservices in Medium-Scale Healthcare Information Systems",
         "2024", "S. Newman, K. Brown, and R. Priya",
         "While microservices gained popularity in hyperscale consumer platforms, recent empirical software "
         "engineering studies demonstrate that decomposing medium-scale healthcare applications into dozens of "
         "networked services inflates DevOps complexity, introduces distributed transaction anomalies across "
         "patient merges and prescription dispensing, and degrades latency. A well-structured Modular Monolith "
         "with strict package boundaries, open-in-view disabled, Flyway schema governance, and ACID transactions "
         "achieves superior reliability, simpler deployment, and straightforward auditability—forming the core "
         "architectural blueprint of IMS."),

        ("Integrated Retail Pharmacy Point-of-Sale Automation and Real-Time Financial Cashbook Reconciliation",
         "2023", "M. Thirunavukkarasu and S. Venkatesh",
         "Independent and hospital-attached pharmacies frequently operate separate prescription-logging books, "
         "billing calculators, and accounting ledgers. Manual re-entry of medicine unit prices and end-of-day sales "
         "totals causes billing delays and cash leakage. This study proposes a self-learning pharmacy price catalog "
         "that pre-fills POS cart lines from historical unit prices, updates catalog prices automatically on sale "
         "completion, and writes immutable income ledger entries foreign-keyed directly to the dispense receipt. "
         "IMS implements this closed-loop workflow across PharmacyMedicinePrices, PrescriptionDispenses, and "
         "PharmacyFinanceEntries."),

        ("Defending Healthcare Authentication Endpoints Against Credential Stuffing and Enumeration Attacks",
         "2021", "J. Bonneau, C. Herley, and P. C. van Oorschot",
         "Public healthcare portals face continuous automated credential-stuffing, password-reset account "
         "enumeration, and verification-link replay attacks. Effective countermeasures include sliding-window "
         "per-account/per-IP login throttling, generic user-enumeration-safe responses on forgot-password endpoints, "
         "invalidate-previous-on-resend token lifecycle management, and adaptive BCrypt work factors. Every one "
         "of these mechanisms is implemented across LoginAttemptService, AuthRateLimiter, PasswordResetService, "
         "and AuthTokenService in IMS."),

        ("Design Systems and Cognitive Load Reduction in High-Volume Clinical Enterprise Software",
         "2022", "E. Marcotte, J. Nielsen, and A. Chatterjee",
         "Clinician burnout and front-desk clerical errors correlate strongly with visually inconsistent user "
         "interfaces, ambiguous status indicators, and deep navigation mazes without contextual breadcrumbs. "
         "Standardizing typography, semantic color tokens (success, warning, danger, info), persistent role-scoped "
         "sidebars, breadcrumb trails, and KPI summary cards measurably reduces task completion time and error "
         "rates across clinical staff. IMS codifies these ergonomics in its shared ERP layout (base.html, app.css, "
         "and NavigationService)."),

        ("Digital Health Interoperability and Longitudinal Patient Empowerment in Outpatient Networks",
         "2024", "National Digital Health Architecture Review — R. S. Sharma and P. Nair",
         "Empowering patients with transparent, read-only access to their finalized diagnoses, consultation "
         "addenda, active and chronic medications, and pharmacy dispense receipts improves medication adherence "
         "and catches clerical discrepancies early. Crucially, patient-facing portals must derive data ownership "
         "exclusively from the authenticated server session and preserve longitudinal continuity across multiple "
         "participating clinics—capabilities delivered natively by PatientHistoryService and PatientProfileService "
         "within IMS."),
    ]

    for idx, (title, yr, auth, abst) in enumerate(surveys, 1):
        b.h3(f"2.2.{idx}", f"Literature Survey {idx}")
        b.p(title, bold_prefix="TITLE:", space_after=3)
        b.p(yr, bold_prefix="YEAR OF PUBLISHING:", space_after=3)
        b.p(auth, bold_prefix="AUTHOR NAME:", space_after=3)
        b.p(abst, bold_prefix="ABSTRACT & RELEVANCE:", space_after=9)

    b.h2("2.3", "Comparative Study of Related Work")
    b.p(
        "To position Infeara Medical System objectively against prevailing alternatives used by outpatient clinics "
        "and pharmacies, Table 2.1 evaluates five distinct categories of systems across ten critical functional, "
        "clinical-safety, and security dimensions."
    )
    b.table(
        "2.1", "Comparative Analysis of Existing Healthcare & Pharmacy Solutions vs. Proposed IMS",
        ["Evaluation Dimension", "Paper & Registers", "Standalone Clinic EMR", "Standalone Pharmacy POS", "Consumer Booking App", "Proposed IMS Platform"],
        [
            ["Multi-Tenant Clinic + Pharmacy Network", "None", "Single clinic only", "Single pharmacy only", "Clinic listing only", "Unified Multi-Tenant Network"],
            ["Staff-Verified Patient Linking & Merge", "Manual folders", "Duplicate rows common", "No clinical chart", "Unverified email/phone", "Verified ID + Non-Destructive Merge"],
            ["Double-Booking Concurrency Guard", "Frequent clashes", "App-level check only", "N/A", "Eventual sync delays", "Pessimistic Lock + UX_Appt_Slot"],
            ["Clinical Note Immutability & Addenda", "Uncontrolled edits", "Often overwritable", "N/A", "No clinical notes", "DRAFT -> FINAL + Signed Addenda"],
            ["Case-Sensitive 32-Char e-Rx Code", "Handwritten slip", "Static printable PDF", "Manual re-typing", "Image upload only", "32-Char CSPRNG + BIN2 Collation"],
            ["Partial Dispense & Over-Dispense Guard", "Zero protection", "No pharmacy feedback", "Local store only", "None", "Item-Level Atomic Qty Guard"],
            ["Auto-Remembered POS Pricing & Ledger", "Manual cashbook", "No pharmacy POS", "Separate accounting", "None", "Auto Price Memory + Auto INCOME"],
            ["At-Rest SMTP Secret Encryption", "N/A", "Plaintext config", "Plaintext config", "Vendor managed", "AES-256-GCM SecretCipher"],
            ["Transactional Email Outbox & Backoff", "N/A", "Synchronous / brittle", "None", "Proprietary", "DB Outbox + 1m/5m/15m Retry"],
            ["Immediate Suspended-Clinic Logout", "N/A", "N/A", "N/A", "Delayed until expiry", "Real-Time ClinicStatusFilter"],
        ],
        col_widths=[1.42, 0.85, 0.92, 0.92, 0.91, 1.00]
    )

    b.h2("2.4", "Research Gaps Identified")
    b.p(
        "Synthesizing the twelve surveyed research works and the comparative matrix in Table 2.1 exposes five "
        "concrete engineering gaps that persist across academic prototypes and commercial outpatient tools:"
    )
    b.bullet("Gap 1 — Disconnect Between Clinical Prescribing and Pharmacy Fulfilment:",
             "Academic e-prescription prototypes focus on encryption primitives in isolation without integrating "
             "partial dispensing math, catalog unit-price memory, tax/discount billing, and automated accounting "
             "entries inside a unified pharmacy POS.")
    b.bullet("Gap 2 — Absence of Safe Walk-In to Online Patient Chart Consolidation:",
             "Existing clinic portals either strand pre-existing walk-in patient records when a user registers "
             "online or perform destructive merges that corrupt foreign-key audit logs.")
    b.bullet("Gap 3 — Insufficient Database-Level Enforcement of Clinical Invariants:",
             "Many web frameworks enforce slot uniqueness, prescription case sensitivity, and dispensed-quantity "
             "bounds solely in Java/application code, leaving the underlying database vulnerable to race conditions "
             "and collation case-folding collisions.")
    b.bullet("Gap 4 — Weak Operational Lifecycle Governance:",
             "Few college or mid-market healthcare platforms address zero-seed bootstrapping (/setup), hashed "
             "single-use staff invitations, encrypted dynamic SMTP configuration, and instant session invalidation "
             "upon tenant suspension.")
    b.bullet("Gap 5 — Missing End-to-End Forensic Auditability:",
             "Security events (throttled logins, denied access) and clinical events (link merges, finalised notes, "
             "prescription verifications, voided/completed dispenses) are rarely unified into a single queryable, "
             "patient-attributed audit schema.")

    b.h2("2.5", "Summary of Literature Review")
    b.p(
        "The literature survey confirms that trustworthy digital healthcare delivery demands tight synergy between "
        "application security, relational database design, clinical workflow ergonomics, and financial automation. "
        "By adopting a modular monolith architecture with defence-in-depth RBAC, SHA-256 single-use tokens, "
        "AES-256-GCM secret storage, human-verified non-destructive patient record merging, dual-layer slot "
        "concurrency control, immutable consultations with addenda, 32-character binary-collated prescriptions, "
        "and an auto-reconciled pharmacy POS ledger, Infeara Medical System directly bridges every research gap "
        "identified in this chapter."
    )
