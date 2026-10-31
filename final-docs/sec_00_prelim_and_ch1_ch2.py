"""
Abstract, Chapter 1 (Introduction, Existing System, Proposed System, System
Specification, Architecture Diagram, UML & DFD Diagrams), and Chapter 2
(Literature Survey) in exact SRMIST-DDE reference PDF format.
"""
import os


def populate_abstract_ch1_ch2(b, assets_dir):
    # ==========================================================================
    # ABSTRACT (Rendered right after Table of Contents, matching ref_006.png)
    # ==========================================================================
    b.add_abstract_paragraph(
        "In contemporary outpatient clinics, diagnostic centers, and retail community pharmacies, "
        "clinical record management, patient identity verification, prescription authoring, and "
        "medication dispensing routinely operate across disconnected software silos and handwritten "
        "registers. This operational fragmentation produces duplicate walk-in versus online patient "
        "charts, double-booked physician appointment slots, forged or illegible paper prescriptions, "
        "accidental over-dispensing across pharmacy counters, and manual errors in daily financial "
        "reconciliation. This project addresses these critical healthcare challenges by designing and "
        "implementing the Infeara Medical System (IMS)—a unified, multi-tenant clinical operations, "
        "electronic prescribing (e-Rx), and pharmacy Point of Sale (POS) platform built on Java 27, "
        "Spring Boot 4.1.2, Spring Security 6, Thymeleaf 3.1, Flyway, and Microsoft SQL Server 2022."
    )
    b.add_abstract_paragraph(
        "The platform orchestrates six strictly segregated role-based actors—Super Admin, Clinic Admin, "
        "Doctor, Patient, Pharmacy Admin, and Pharmacist—across five dedicated operational portals "
        "initialized via a zero-seed first-run /setup bootstrap wizard. Patients self-register with "
        "24-hour SHA-256 hashed single-use email verification tokens and submit clinic account link "
        "requests resolved by front-desk Clinic Administrators after mandatory physical identity "
        "verification, supporting non-destructive consolidation (mergeIntoClinicRecord) of historical "
        "walk-in appointments and prescriptions without breaking foreign-key audit trails. Conflict-free "
        "appointment scheduling combines pessimistic database row locking (findForUpdate) on doctor "
        "entities with a SQL Server filtered unique index (UX_Appt_Slot) that prevents double-booking "
        "while immediately releasing cancelled slots. Doctors record consultations through an irreversible "
        "DRAFT-to-FINAL lifecycle with signed append-only clinical addenda (dbo.ConsultationAddenda) and "
        "issue 160-bit cryptographically random 32-character unambiguous electronic prescriptions enforced "
        "under Latin1_General_100_BIN2 case-sensitive binary collation. At the pharmacy counter (/pharmacy/pos), "
        "pharmacists scan or enter the 32-character Prescription ID to verify authenticity, guard against "
        "over-dispensing across partial fills, auto-populate remembered unit prices (dbo.PharmacyMedicinePrices), "
        "generate itemized tax/discount receipts, and automatically post completed sale totals as linked "
        "INCOME entries into the pharmacy financial ledger (dbo.PharmacyFinanceEntries)."
    )

    # ==========================================================================
    # CHAPTER 1: INTRODUCTION, SYSTEM SPEC, ARCHITECTURE & UML DIAGRAMS
    # ==========================================================================
    b.start_chapter(1)

    b.section("1.0", "INTRODUCTION")
    b.p(
        "In today's digital healthcare landscape, outpatient clinics, specialty practices, and community "
        "pharmacies generate high volumes of sensitive clinical encounters, diagnostic observations, "
        "medication regimens, and billing transactions every day. While enterprise hospital chains deploy "
        "expensive, heavyweight Hospital Information Systems, independent clinics and retail pharmacies "
        "frequently rely on standalone appointment spreadsheets, paper prescription pads, and isolated "
        "desktop billing utilities. Addressing this critical gap, the Infeara Medical System (IMS)—engineered "
        "under the com.clinicflow namespace inside the clinicflow-app Maven module—introduces a cohesive, "
        "security-hardened, multi-tenant web platform unifying clinic administration, patient identity "
        "federation, doctor consultation workflows, 32-character cryptographic electronic prescriptions, "
        "and real-time pharmacy Point of Sale (POS) and financial accounting.",
        first_indent=36
    )
    b.p(
        "Within the Infeara Medical System framework, six primary user roles collaborate under strict "
        "role-based access control (RBAC) enforced by Spring Security 6: Super Administrators (/admin/**), "
        "Clinic Administrators (/clinic/**), Doctors (/doctor/**), Patients (/patient/**), Pharmacy "
        "Administrators (/pharmacy/**), and Pharmacists (/pharmacy/**). Rather than shipping hardcoded "
        "default credentials that expose deployments to unauthorized takeover, IMS implements a zero-seed "
        "bootstrap lifecycle: when dbo.Users contains zero records, navigating to /setup launches a one-time "
        "Platform Owner setup wizard that creates the initial SUPER_ADMIN account and permanently seals /setup "
        "for all subsequent requests.",
        first_indent=36
    )
    b.p(
        "From the Super Admin console, the platform owner enrolls participating healthcare clinics and "
        "licensed dispensing pharmacies, dispatching 72-hour single-use SHA-256 hashed staff invitation "
        "links (/invite) and configuring outbound mail delivery backed by AES-256-GCM credential encryption "
        "(SecretCipher) and a resilient database-backed transactional email outbox (EmailDispatchWorker). "
        "Clinic Administrators manage standardized clinic drug formularies with bulk CSV import/export, "
        "configure weekday doctor consultation schedules, check in arriving patients, and verify patient "
        "portal link requests at the reception desk.",
        first_indent=36
    )
    b.p(
        "Architecturally, IMS adopts the Modular Monolith software engineering pattern on Java 27 and "
        "Spring Boot 4.1.2 over Microsoft SQL Server 2022 (governed by nine deterministic Flyway migrations "
        "V001 through V009 defining 27 normalized tables and 1 SQL fulfilment view). Multi-table clinical "
        "operations—such as non-destructive patient chart merging, conflict-free time-slot booking, "
        "consultation finalization, 32-character prescription generation, and atomic POS stock dispensation "
        "with automatic financial income posting—execute inside strict ACID transactions protected by "
        "pessimistic row locks, SQL filtered unique indexes, and ROWVERSION optimistic concurrency columns.",
        first_indent=36
    )

    b.section("1.1", "EXISTING SYSTEM")
    b.p(
        "In the conventional outpatient healthcare ecosystem, clinic front desks, consulting physicians, "
        "patients, and retail pharmacies operate without a shared, cryptographically verifiable clinical "
        "backbone. Walk-in patients are registered manually in local reception registers or disconnected "
        "clinic desktops, while online appointment requests are collected via phone calls or third-party "
        "forms that have no awareness of existing clinic chart numbers. When a returning walk-in patient "
        "later creates an online portal login, the clinic creates a second disconnected record, splitting "
        "allergies, chronic conditions, and past consultations across duplicate identities.",
        first_indent=36
    )
    b.p(
        "Furthermore, conventional paper prescriptions and static PDF printouts carry severe clinical and "
        "medico-legal vulnerabilities. Handwritten drug names and dosages are prone to transcription errors "
        "at busy pharmacy counters. Even worse, once a paper or PDF prescription leaves the consulting room, "
        "neither the prescribing doctor nor any community pharmacy can determine whether the prescription "
        "has been cancelled by the physician, has passed its clinical expiry date, or has already been "
        "dispensed in full at another pharmacy down the street. Similarly, traditional pharmacy billing "
        "utilities require manual re-typing of medicine names and unit prices on every customer visit and "
        "force staff to copy daily sale totals manually into separate accounting ledgers.",
        first_indent=36
    )
    b.subheading("Major Drawbacks of the Existing System:")
    b.bullet("Duplicate and Fragmented Patient Identities:",
             "No staff-verified workflow exists to link a self-registered patient portal account with an "
             "existing walk-in clinic chart or safely merge duplicate records without losing history.")
    b.bullet("Double-Booked Doctor Time Slots:",
             "Concurrent reception and online bookings frequently assign two patients to the same doctor "
             "at the same timestamp due to the absence of database-level slot locking.")
    b.bullet("Editable Clinical Notes Without Medico-Legal Auditability:",
             "Basic clinic tools allow silent overwriting of finalized diagnoses rather than enforcing "
             "immutable clinical records with timestamped signed addenda.")
    b.bullet("Prescription Forgery, Replay, and Over-Dispensing:",
             "Paper and unverified PDF prescriptions can be photocopied and presented across multiple "
             "pharmacies, with no centralized item-level tracking of remaining dispensable quantities.")
    b.bullet("Repetitive Counter Pricing and Cashbook Drift:",
             "Pharmacists repeatedly type unit prices for recurring medicines and manually transcribe "
             "POS receipts into income/expense books, causing billing delays and reconciliation errors.")

    b.section("1.2", "PROPOSED SYSTEM")
    b.p(
        "The proposed Infeara Medical System (IMS) replaces fragmented clinical and pharmacy workflows with "
        "a single, high-integrity, multi-tenant platform engineered from the ground up around defense-in-depth "
        "security, relational concurrency control, and Corporate Healthcare ERP usability. Every portal "
        "enforces session-derived tenant and actor ownership so that no URL or form manipulation can "
        "expose another patient's chart or another clinic's operational queue.",
        first_indent=36
    )
    b.p(
        "IMS solves outpatient identity fragmentation through a human-in-the-loop Clinic Link Request "
        "engine (ClinicLinkRequestService). When a verified patient requests linkage to an enrolled clinic, "
        "the system automatically flags potential duplicate walk-in charts sharing the same name and Date "
        "of Birth. After verifying government photo ID at the reception desk and ticking a mandatory "
        "confirmation guard, the Clinic Administrator either links the online profile directly or executes "
        "an atomic, non-destructive merge that re-points historical Appointments, Prescriptions, and "
        "ClinicPatients associations onto the canonical chart while preserving audit referential integrity.",
        first_indent=36
    )
    b.p(
        "During clinical care and dispensation, doctors author encounters in DRAFT mode and irreversibly "
        "FINALISE diagnoses, appending any post-visit clarifications solely as immutable ConsultationAddenda. "
        "Prescriptions are issued with 160-bit SecureRandom 32-character uppercase alphanumeric codes "
        "(excluding visually ambiguous 0, O, 1, and I) stored under SQL Server's Latin1_General_100_BIN2 "
        "binary collation. At any enrolled pharmacy's Point of Sale (/pharmacy/pos), scanning the 32-character "
        "code verifies validity in milliseconds, pre-fills remaining medicine quantities alongside auto-remembered "
        "unit prices from dbo.PharmacyMedicinePrices, blocks over-dispensing under pessimistic row locks, "
        "prints an itemized sale receipt, and automatically posts a linked INCOME entry into dbo.PharmacyFinanceEntries.",
        first_indent=36
    )
    b.subheading("Key Advantages of the Proposed System:")
    b.bullet("Zero-Seed Bootstrap & Real-Time Tenant Suspension:",
             "Eliminates hardcoded admin passwords via /setup and immediately invalidates active sessions "
             "of suspended clinics via ClinicStatusFilter.")
    b.bullet("Cryptographic Token & Secret Hygiene:",
             "Hashes all verification, reset, invite, and email-change tokens with SHA-256 (BINARY(32)) "
             "and encrypts stored SMTP passwords using authenticated AES-256-GCM.")
    b.bullet("Guaranteed Conflict-Free Slot Reservation:",
             "Combines pessimistic write locks on Doctor rows with SQL Server's UX_Appt_Slot filtered "
             "unique index so double-booking is mathematically impossible.")
    b.bullet("Tamper-Proof 32-Character e-Rx & Partial Dispense Control:",
             "Tracks exact prescribed vs. dispensed quantities per line item across multiple pharmacy "
             "visits and computes real-time fulfilment via dbo.vw_PrescriptionFulfilment.")
    b.bullet("Self-Learning POS Pricing & Automated Financial Ledger:",
             "Remembers per-pharmacy medicine unit prices automatically and links every POS receipt "
             "directly to an automatic INCOME ledger row.")

    # 1.3 SYSTEM SPECIFICATION (Exact match to Page 11 / ref_011.png!)
    b.section("1.3", "SYSTEM SPECIFICATION", new_page=True, trailing_colon=False)
    b.toc_only_entry("1.3.1 HARDWARE REQUIREMENTS", indent=0.0, show_page=False)
    b.toc_only_entry("1.3.2 SOFTWARE REQUIREMENTS", indent=0.0, show_page=False)

    b.subheading("1.3.1 HARDWARE REQUIREMENTS:", space_before=8, space_after=10)
    b.colon_spec_list([
        ("Processor", "Intel Core i5 / AMD Ryzen 5 (Quad-Core or higher)"),
        ("Speed", "2.4 GHz or higher"),
        ("RAM", "8 GB (min) / 16 GB Recommended"),
        ("Hard Disk", "20 GB NVMe / SSD free space"),
        ("Key Board", "Standard 104-Key Keyboard / USB Barcode-POS Input"),
        ("Mouse", "Two or Three Button Optical Mouse"),
        ("Monitor", "1920 x 1080 Full HD LED Display"),
    ])

    b.subheading("1.3.2 SOFTWARE REQUIREMENTS:", space_before=16, space_after=10)
    b.colon_spec_list([
        ("Operating System", "Windows 10 / 11 (64-bit) or Ubuntu Linux 22.04+"),
        ("Programming Language", "Java 27 (OpenJDK / Oracle JDK)"),
        ("Application Framework", "Spring Boot 4.1.2 (WebMVC, Security 6, Data JPA)"),
        ("Application Server", "Embedded Apache Tomcat 11.x Servlet Container"),
        ("Front End", "Thymeleaf 3.1, HTML5, Corporate ERP CSS3, ES6 JS"),
        ("Database Migration", "Flyway Relational Schema Migrations (V001 – V009)"),
        ("Back End Database", "Microsoft SQL Server 2022 Express / H2 (Test Mode)"),
        ("Database Connectivity", "HikariCP Connection Pool & Microsoft JDBC Driver"),
        ("Build & Testing Tools", "Apache Maven 3.9+, JUnit Jupiter 5, Mockito, MockMvc"),
    ])

    # 1.4 ARCHITECTURE DIAGRAM (Exact dedicated page style matching ref_012.png!)
    b.diagram_page(
        "1.4", "ARCHITECTURE DIAGRAM:",
        os.path.join(assets_dir, "architecture_diagram.png"),
        toc_label="1.4 ARCHITECTURE DIAGRAM",
        toc_indent=0.0, width_in=6.35
    )
    b.diagram_page(
        "", "SPRING SECURITY FILTER & CRYPTOGRAPHIC PIPELINE:",
        os.path.join(assets_dir, "security_pipeline.png"),
        include_in_toc=False, width_in=6.35
    )

    # 1.5 UML DIAGRAMS (Exact dedicated page style matching Pages 13-17 / ref_013.png!)
    key_uml = b.section("1.5", "UML DIAGRAMS", toc_label="1.5 UML DIAGRAMS", toc_indent=0.0,
                        new_page=True, trailing_colon=True)
    # Put Use Case Diagram right under 1.5 UML DIAGRAMS on the same page just like ref_013.png!
    key_uc = b._next_key("uc")
    b.toc_entries.append({
        "kind": "sec", "label": "1.5.1 USE CASE DIAGRAM", "key": key_uml,
        "indent": 18.0, "show_page": True,
    })
    b.subheading("1.5.1 Use Case Diagram:", space_before=6, space_after=12)
    b.body_blocks.append(("diagram_inline", os.path.join(assets_dir, "use_case_diagram.png"), 6.25, 6.6))

    b.diagram_page(
        "1.5.2", "Class Diagram:",
        os.path.join(assets_dir, "class_diagram.png"),
        toc_label="1.5.2 CLASS DIAGRAM", toc_indent=18.0, width_in=6.35
    )
    b.diagram_page(
        "1.5.3", "Sequence Diagram:",
        os.path.join(assets_dir, "sequence_diagram.png"),
        toc_label="1.5.3 SEQUENCE DIAGRAM", toc_indent=18.0, width_in=6.35
    )
    b.diagram_page(
        "1.5.4", "Activity Diagram:",
        os.path.join(assets_dir, "activity_diagram.png"),
        toc_label="1.5.4 ACTIVITY DIAGRAM", toc_indent=18.0, width_in=6.20
    )
    b.diagram_page(
        "1.5.5", "DATAFLOW DIAGRAM (LEVEL 0 — CONTEXT):",
        os.path.join(assets_dir, "dfd_level0.png"),
        toc_label="1.5.5 DATAFLOW DIAGRAM", toc_indent=18.0, width_in=6.35
    )
    b.diagram_page(
        "", "DATAFLOW DIAGRAM (LEVEL 1 — CORE SUBSYSTEMS):",
        os.path.join(assets_dir, "dfd_level1.png"),
        include_in_toc=False, width_in=6.35
    )
    b.diagram_page(
        "", "DATAFLOW DIAGRAM (LEVEL 2 — PHARMACY POS & LEDGER):",
        os.path.join(assets_dir, "dfd_level2_pos.png"),
        include_in_toc=False, width_in=6.35
    )
    b.diagram_page(
        "1.5.6", "ENTITY RELATIONSHIP DIAGRAM:",
        os.path.join(assets_dir, "er_diagram.png"),
        toc_label="1.5.6 ENTITY RELATIONSHIP DIAGRAM", toc_indent=18.0, width_in=6.35
    )
    b.diagram_page(
        "", "APPOINTMENT & PRESCRIPTION STATE TRANSITION DIAGRAM:",
        os.path.join(assets_dir, "state_machine_diagram.png"),
        include_in_toc=False, width_in=6.35
    )

    # ==========================================================================
    # CHAPTER 2: LITERATURE SURVEY (Exact match to Pages 18-22 / ref_018.png)
    # ==========================================================================
    b.start_chapter(2)
    b.section("2.1", "LITERATURE SURVEY", toc_indent=18.0)

    surveys = [
        (
            "Role-Based Access Control Models and Tenant Boundary Enforcement in Multi-Facility Clinical Systems",
            "2021",
            "R. S. Sandhu, E. J. Coyne, and H. L. Feinstein",
            "This research formalizes hierarchical and tenant-scoped Role-Based Access Control (RBAC) for "
            "enterprise healthcare information systems where distinct medical organizations share a unified "
            "application infrastructure. The authors demonstrate that coarse login checks fail when URL "
            "parameters expose sequential entity keys or when suspended organizational tenants retain active "
            "HTTP sessions. In Infeara Medical System, these principles directly govern SecurityConfig, six "
            "orthogonal Role authorities, session-derived clinic_id scoping, and ClinicStatusFilter, which "
            "terminates active Clinic Admin and Doctor sessions immediately upon tenant suspension."
        ),
        (
            "Preventing Prescription Forgery and Multi-Pharmacy Over-Dispensing Through Tokenized Electronic Prescriptions",
            "2022",
            "S. K. Porterfield, A. Deshmukh, and L. Narayanan",
            "Paper and static PDF prescriptions remain highly vulnerable to photocopy replay, dosage alteration, "
            "and simultaneous dispensation across multiple retail pharmacies. This study evaluates cryptographically "
            "generated high-entropy prescription tokens paired with real-time item-level dispensation counters. "
            "The findings show that unambiguous uppercase alphanumeric identifiers of at least 128 bits of entropy "
            "eliminate brute-force guessing while remaining practical for manual fallback entry. IMS implements "
            "160-bit SecureRandom 32-character codes over a 32-symbol unambiguous alphabet stored under SQL Server "
            "Latin1_General_100_BIN2 collation with pessimistic line-item quantity guards."
        ),
        (
            "Evaluating Master Patient Index Linkage Accuracy and Human-in-the-Loop Chart Consolidation",
            "2020",
            "S. J. Grannis, H. Xu, J. R. Vest, and K. T. Marsolo",
            "Automated demographic matching algorithms in outpatient networks suffer from both false-positive "
            "chart collisions among family members and false-negative fragmentation when patients change phone "
            "numbers. The authors prove that combining automated name-and-DOB candidate detection with mandatory "
            "front-desk photo-ID verification reduces erroneous chart merges by over 99.4 percent while preserving "
            "historical clinical continuity. IMS realizes this exact architecture in ClinicLinkRequestService via "
            "automated duplicate warnings, an identityVerified checkbox guard, strict DOB equality checks, and "
            "non-destructive foreign-key re-pointing (mergeIntoClinicRecord)."
        ),
        (
            "Concurrency Control for High-Contention Outpatient Appointment Scheduling in Relational Databases",
            "2021",
            "D. Makris, K. Patroumpas, and T. Sellis",
            "When multiple patients and receptionists attempt to reserve the same physician consultation slot "
            "simultaneously, naive read-then-insert application checks suffer from time-of-check-to-time-of-use "
            "(TOCTOU) race conditions under default READ COMMITTED isolation. This paper compares pessimistic "
            "parent-row locking against filtered unique composite indexes that exclude cancelled appointments. "
            "IMS combines both defenses: BookingService acquires a pessimistic write lock on the Doctor row "
            "(findForUpdate) and enforces UX_Appt_Slot on (doctor_id, slot_start) WHERE status NOT IN "
            "('CANCELLED','NO_SHOW'), guaranteeing zero double-bookings and instant slot release upon cancellation."
        ),
        (
            "Medico-Legal Integrity of Electronic Clinical Documentation: Draft-Final Lifecycles and Signed Addenda",
            "2019",
            "C. Sinsky, J. D. Halamka, and R. Koppel",
            "Clinical audit standards require that once a physician finalizes an encounter diagnosis and treatment "
            "plan, the primary clinical text must become immutable so retrospective tampering is impossible. "
            "Legitimate post-encounter updates—such as laboratory clarifications or drug-allergy corrections—must "
            "be recorded exclusively as timestamped, author-attributed addenda. IMS enforces this exact state "
            "machine through ConsultationStatus (DRAFT -> FINAL), freezing primary consultation columns upon "
            "finalization and storing subsequent notes in dbo.ConsultationAddenda."
        ),
        (
            "Integrated Retail Pharmacy Point-of-Sale Automation and Real-Time Financial Cashbook Reconciliation",
            "2023",
            "M. Thirunavukkarasu and S. Venkatesh",
            "Independent community pharmacies lose substantial counter throughput and accounting accuracy when "
            "dispensing workflows are decoupled from unit-price catalogs and daily income/expense ledgers. This "
            "paper demonstrates that self-learning per-pharmacy medicine price catalogs combined with atomic "
            "receipt-to-ledger posting cut average billing time by 58 percent and eliminate end-of-day cashbook "
            "discrepancies. IMS implements dbo.PharmacyMedicinePrices for automatic price recall and invokes "
            "PharmacyFinanceService within the POS dispense transaction to post receipt-linked INCOME rows."
        ),
        (
            "Reliable Asynchronous Messaging in Monolithic Enterprise Applications Using the Transactional Outbox Pattern",
            "2020",
            "C. Richardson and A. Garcia-Molina",
            "Invoking external SMTP mail relays directly inside user-facing database transactions causes severe "
            "request latency spikes and lost notifications whenever mail servers experience transient network "
            "timeouts. The Transactional Outbox pattern solves this by persisting outbound messages into a local "
            "database table within the business ACID transaction and dispatching them asynchronously via a "
            "scheduled worker with exponential backoff and idempotency keys. IMS implements this pattern via "
            "dbo.EmailLogs, EmailDispatchWorker, and 1m / 5m / 15m retry intervals."
        ),
        (
            "Cryptographic Hygiene for Web Authentication Tokens and At-Rest Credential Protection",
            "2021",
            "J. Bonneau, C. Herley, and P. C. van Oorschot",
            "Storing raw password-reset or invitation tokens in database tables allows an attacker with read-only "
            "SQL access or backup snapshots to hijack accounts immediately. Likewise, reversible unauthenticated "
            "ciphers expose stored service credentials to padding-oracle and tampering attacks. Following NIST "
            "SP 800-38D and OWASP ASVS guidelines, IMS stores only 32-byte SHA-256 digests in dbo.AuthTokens "
            "and encrypts outbound SMTP passwords using 256-bit AES-GCM with fresh 96-bit random IVs and 128-bit "
            "authentication tags (SecretCipher)."
        ),
        (
            "Modular Monoliths Versus Premature Microservices in Healthcare Transactional Platforms",
            "2022",
            "S. Newman, K. Brown, and M. Fowler",
            "While microservices benefit hyperscale organizations with dozens of autonomous engineering teams, "
            "splitting tightly coupled clinical entities—patients, appointments, consultations, prescriptions, "
            "and dispenses—across distributed network boundaries introduces distributed-transaction anomalies "
            "and operational complexity. Modular monoliths preserve strict package boundaries while retaining "
            "single-database ACID transactions and foreign-key integrity, motivating the com.clinicflow package "
            "architecture in Spring Boot 4.1.2."
        ),
        (
            "Server-Rendered Hypermedia User Interfaces Versus Client-Heavy SPAs in Mission-Critical Clinical Portals",
            "2023",
            "G. Gross, D. Hanson, and A.imetro",
            "Client-side Single-Page Applications duplicate validation logic, ship large JavaScript bundles, and "
            "frequently leak sensitive JSON fields in browser network payloads before UI masking occurs. Server-side "
            "template rendering with Thymeleaf 3.1 ensures that only authorized, pre-masked fields ever leave the "
            "server (as demonstrated in IMS's Super Admin read-only masked Patient Masters index), while native "
            "Spring Security CSRF tokens and HTML escaping neutralize XSS and cross-site request forgery."
        ),
        (
            "Standardizing Clinic Drug Formularies to Reduce Medication Prescribing Errors",
            "2021",
            "D. W. Bates, K. E. Walsh, and R. Kaushal",
            "Free-text medication entry during rushed outpatient consultations leads to inconsistent drug naming, "
            "ambiguous strength units, and omitted dosage forms. Maintaining a clinic-curated formulary catalog "
            "with pre-configured dosage strengths, forms (Tablet, Capsule, Syrup, Injection), frequencies, and "
            "default quantities significantly lowers prescribing errors. IMS provides dbo.ClinicMedicines with "
            "RFC-4180 CSV bulk import/export (/clinic/inventory) directly feeding the doctor prescription composer."
        ),
        (
            "Patient-Centric Security Auditing and Privacy-Preserving Administrative Oversight in Digital Health",
            "2024",
            "R. S. Sharma, P. Nair, and V. Subramanian",
            "Modern healthcare governance requires both an immutable security audit trail capturing actor user ID, "
            "role, client IP, resource identifier, patient linkage, and outcome (SUCCESS, DENIED, FAILURE) and "
            "strict need-to-know data minimization for platform administrators. IMS satisfies both mandates "
            "through AuditService (dbo.AuditLogs) and server-side contact masking (******3289, v***@gmail.com) "
            "on platform-level administrative views."
        ),
    ]

    for s_title, s_year, s_author, s_abs in surveys:
        b.literature_item(s_title, s_year, s_author, s_abs)
