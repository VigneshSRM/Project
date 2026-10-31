"""
Chapter 5 (Technologies & Tools Used) + Chapter 6 (Implementation:
8 Modules, Cryptographic & Transactional Algorithms, Pseudocode,
Authentic Code Listings, 11 Annotated System Screenshots, and Integration).
"""
import os


def populate_ch5_and_ch6(b, assets_dir):
    # ==========================================================================
    # CHAPTER 5: TECHNOLOGIES & TOOLS USED
    # ==========================================================================
    b.start_chapter(5, "TECHNOLOGIES & TOOLS USED")

    b.h2("5.1", "Software Requirements")
    b.p(
        "Infeara Medical System (IMS) is engineered on a modern, zero-licensing-cost enterprise Java software "
        "stack configured via clinicflow-app/pom.xml and clinicflow-app/src/main/resources/application.yml. "
        "Table 5.1 specifies the exact versions and roles of every software component required across development, "
        "automated testing, and production deployment."
    )
    b.table(
        "5.1", "Software Requirements Specification for Infeara Medical System (IMS)",
        ["Layer / Category", "Software / Library", "Version in Repo", "Role in IMS Architecture"],
        [
            ["Operating System", "Windows 10/11 Pro, Windows Server, or Linux x86_64", "64-bit OS", "Host OS for JVM, Maven build tools, and SQL Server 2022."],
            ["Language Runtime", "Java SE Development Kit (OpenJDK)", "Java 27", "Executes Spring Boot application with records & switch expressions."],
            ["Application Framework", "Spring Boot Starter Parent", "4.1.2", "Auto-configuration, embedded Tomcat HTTP server, Actuator health."],
            ["Web & View Engine", "Spring WebMVC + Thymeleaf + Layout Dialect", "4.1.2 / 4.0.1", "Server-rendered HTML5 views, reusable base.html & auth.html layouts."],
            ["Security Framework", "Spring Security + Thymeleaf Security6 Extras", "6.x / 3.1.5", "Session authentication, BCrypt, CSRF tokens, 6-role URL guards."],
            ["Persistence & ORM", "Spring Data JPA + Hibernate ORM + HikariCP", "6.x", "Entity mapping for 23 JPA entities, pessimistic locks, connection pool."],
            ["Schema Migration", "Flyway Core + Flyway SQL Server", "Managed", "Deterministic execution of SQL migrations V001 through V009."],
            ["Primary RDBMS", "Microsoft SQL Server 2022 Express", "16.x (2022)", "ACID persistence, BIN2 collation, filtered unique index, ROWVERSION."],
            ["Zero-Setup Test DB", "H2 Database Engine (MODE=MSSQLServer)", "Runtime", "In-memory / file-backed local run and MockMvc integration tests."],
            ["Email Subsystem", "Spring Boot Starter Mail (Jakarta Mail)", "4.1.2", "STARTTLS/SSL SMTP dispatch driven by transactional EmailLogs outbox."],
            ["Build & Dependency", "Apache Maven (Bundled in tools/)", "3.9.11", "Lifecycle compilation, Lombok annotation processing, JUnit runner."],
            ["Testing Stack", "JUnit Jupiter 5, Mockito, AssertJ, Spring Security Test", "Managed", "Unit, service-ownership, and full HTTP MockMvc security verification."],
        ],
        col_widths=[1.25, 1.85, 0.90, 2.02]
    )

    b.h2("5.2", "Hardware Requirements")
    b.p(
        "Because IMS compiles to a self-contained Spring Boot application with server-side Thymeleaf rendering "
        "and efficient HikariCP database connection pooling, hardware resource demands remain modest across "
        "developer machines, clinic/cloud servers, and client workstations, as detailed in Table 5.2."
    )
    b.table(
        "5.2", "Minimum and Recommended Hardware Specifications",
        ["Deployment Tier", "Processor (CPU)", "Memory (RAM)", "Storage & Network"],
        [
            ["Developer Workstation", "Intel Core i5 / AMD Ryzen 5 (4+ cores, 2.5 GHz+)", "8 GB min (16 GB recommended)", "20 GB SSD free space for JDK, Maven cache & SQL Express"],
            ["Production Application + DB Server", "4 vCPU x86_64 Server Processor", "8 GB – 16 GB ECC / Cloud RAM", "50 GB NVMe SSD (RAID/Snapshot backed), 100 Mbps NIC"],
            ["Clinic / Doctor Client PC or Tablet", "Dual-core 1.8 GHz+ (Desktop, Laptop, or iPad/Tablet)", "4 GB RAM", "1366x768+ display, modern browser (Chrome/Edge/Firefox)"],
            ["Pharmacy POS Counter Terminal", "Dual-core x86 PC + Optional USB HID 1D/2D Scanner", "4 GB RAM", "Thermal/Laser receipt printer + broadband/LAN link"],
        ],
        col_widths=[1.45, 1.55, 1.25, 1.77]
    )

    b.h2("5.3", "Programming Languages Used")
    b.h3("5.3.1", "Java 27 (Backend Domain, Security, and Web Layer)")
    b.p(
        "All 189 production source files and 8 test suites in clinicflow-app are written in modern Java (configured "
        "at <java.version>27</java.version> in pom.xml). Modern Java language constructs materially improve clarity "
        "and safety throughout the codebase: immutable Java records (such as KpiCard, ActivityRow, NavItem, "
        "Breadcrumb, IssuedToken, EmailContent, SlotOption, and domain event payloads) eliminate mutable DTO "
        "boilerplate; exhaustive switch expressions (used in NavigationService, RoleHomeResolver, AuthTokenService, "
        "and EmailDeliveryService) guarantee at compile time that every enum constant of Role or AuthTokenPurpose "
        "is handled; and pattern matching for instanceof simplifies principal inspection in ClinicStatusFilter."
    )

    b.h3("5.3.2", "Transact-SQL (T-SQL for Microsoft SQL Server 2022)")
    b.p(
        "Database DDL across V001 through V009 leverages rich Transact-SQL capabilities unavailable in generic "
        "auto-generated ORM schemas: explicit named CHECK constraints on every status and monetary column, "
        "Latin1_General_100_BIN2 binary code-point collation on Prescriptions.prescription_code to enforce strict "
        "case sensitivity at the index level, filtered unique indexes (WHERE status IN (...) and WHERE dispense_id "
        "IS NOT NULL) to combine business flexibility with relational uniqueness, ROWVERSION columns for hardware-fast "
        "optimistic concurrency, and aggregated SQL views (dbo.vw_PrescriptionFulfilment)."
    )

    b.h3("5.3.3", "HTML5, CSS3 Custom Properties, SVG, and Vanilla ES6 JavaScript")
    b.p(
        "Client markup across all 61 templates adheres to semantic, accessible HTML5 (ARIA landmarks, aria-current, "
        "associated <label> elements, and keyboard focus rings). Styling in static/css/app.css (1,882 lines) uses "
        "CSS3 Custom Properties (--c-primary, --c-sidebar-bg, --c-accent, spacing tokens --s-1..--s-6) with zero "
        "external CSS framework bloat, paired with an inline SVG icon sprite (static/icons.svg) and unobtrusive "
        "ES6 JavaScript (static/js/app.js) for responsive sidebar toggling and dynamic prescription/POS row helpers."
    )

    b.h2("5.4", "Frameworks and Libraries")
    b.bullet("Spring Boot 4.1.2 & Spring WebMVC:",
             "Provides convention-over-configuration bootstrap, externalized YAML property binding, Jakarta Bean "
             "Validation integration, custom HTTP 403/404/500 error routing, and Actuator /actuator/health probes.")
    b.bullet("Spring Security 6:",
             "Implements stateful HTTP session authentication, DelegatingPasswordEncoder (BCrypt default), automatic "
             "synchronizer-token CSRF protection, 7-day Remember-Me cookies, custom authentication success/failure "
             "handlers, and filter-chain hooks for LoginThrottleFilter and ClinicStatusFilter.")
    b.bullet("Spring Data JPA & Hibernate 6:",
             "Backs all 24 repository interfaces with compile-checked query methods, custom JPQL @Lock("
             "LockModeType.PESSIMISTIC_WRITE) row locks for atomic token consumption, doctor slot booking, and "
             "pharmacy item dispensing, and PhysicalNamingStrategyStandardImpl for exact SQL Server column alignment.")
    b.bullet("Thymeleaf & Ultraq Layout Dialect 4.0.1:",
             "Composes all portal pages cleanly from layout/base.html and layout/auth.html fragments while "
             "escaping untrusted text by default (th:text) to neutralize Cross-Site Scripting (XSS).")
    b.bullet("Flyway Database Migrations:",
             "Configured with baseline-on-migrate=true and baseline-version=1 so existing SQL Server schemas "
             "upgrade deterministically through V002–V009 on startup without manual DDL drift.")
    b.bullet("JUnit 5, Mockito & AssertJ:",
             "Powers fast, isolated unit tests of cryptographic/policy utilities alongside full @SpringBootTest "
             "+ @AutoConfigureMockMvc web security and multi-step workflow integration tests." )

    b.h2("5.5", "Development and Runtime Environment")
    b.p(
        "IMS supports two clean runtime profiles out of the box. The default profile connects via the Microsoft "
        "JDBC Driver (mssql-jdbc) over TCP port 1433 to localhost\\SQLEXPRESS (database ClinicFlow_Dev) with "
        "HikariCP connection pooling and Flyway enabled. For instant developer onboarding or offline automated "
        "testing where SQL Server TCP/IP is unavailable, activating -Dspring-boot.run.profiles=h2 switches "
        "transparently to H2 in MSSQLServer compatibility mode and logs outbound verification/invitation links "
        "via clinicflow.mail.mode=log. Build reproducibility is guaranteed by bundling Apache Maven 3.9.11 "
        "directly inside tools/apache-maven-3.9.11 alongside PowerShell launch scripts in clinicflow-app/scripts/."
    )

    # ==========================================================================
    # CHAPTER 6: IMPLEMENTATION
    # ==========================================================================
    b.start_chapter(6, "IMPLEMENTATION")

    b.h2("6.1", "Module Description")
    b.p(
        "The implementation of Infeara Medical System spans 189 Java classes across eight tightly cohesive "
        "functional modules under src/main/java/com/clinicflow/. Each module encapsulates its own controllers, "
        "form validation DTOs, @Transactional domain services, domain events, and audit instrumentation."
    )

    b.h3("6.1.1", "Module 1: Zero-Seed Platform Bootstrap (/setup) & Super Admin Console (/admin/**)")
    b.p(
        "Implemented in SetupController, SetupService, AdminController, AdminClinicService, AdminPharmacyService, "
        "AdminPatientService, AdminReportService, and SmtpSettingsService, this module governs tenant lifecycle "
        "from day zero. IMS intentionally ships with zero hardcoded accounts. When userRepository.count() == 0, "
        "GET /setup renders the initial owner bootstrap screen; submitting SetupForm validates password policy, "
        "creates the first ACTIVE SUPER_ADMIN user, logs SETUP_COMPLETED in AuditLogs, and permanently locks "
        "/setup against subsequent calls. From /admin/**, the Super Admin enrolls Clinics and Pharmacies, triggers "
        "72-hour STAFF_INVITE tokens via InvitationService, toggles tenant statuses (ACTIVE / SUSPENDED), inspects "
        "platform-wide KPI metrics, and configures live SMTP credentials encrypted via SecretCipher."
    )

    b.h3("6.1.2", "Module 2: Identity, Single-Use SHA-256 Auth Tokens, Invitations & Login Throttling")
    b.p(
        "Implemented across RegistrationService, VerificationService, PasswordResetService, InvitationService, "
        "EmailChangeService, AuthTokenService, TokenHasher, PasswordPolicy, LoginAttemptService, and AuthRateLimiter, "
        "this module enforces healthcare-grade identity hygiene. Patient self-registration (/register) collects "
        "personal, address, and optional medical fields, enforces >=10-character alphanumeric passwords, creates "
        "a PENDING User and PATIENT_ENTERED Patient row (PAT-YYYY-NNNN), and issues a 24-hour VERIFY_EMAIL token. "
        "Raw tokens are 32-byte SecureRandom URL-safe strings sent only to the user's email; dbo.AuthTokens "
        "persists only the 32-byte SHA-256 hash. Re-issuing a token deletes prior unused tokens for that user and "
        "purpose, and consuming a token acquires a pessimistic write lock (findForUpdate) before setting used_at."
    )

    b.h3("6.1.3", "Module 3: Patient Self-Service Portal, Clinic Discovery & Atomic Slot Booking")
    b.p(
        "Implemented in PatientProfileController, PatientProfileService, ClinicDiscoveryService, SlotService, "
        "BookingService, PatientBookingController, and PatientHistoryService, this module empowers authenticated "
        "patients while eliminating IDOR vulnerabilities. Every controller method resolves the acting Patient via "
        "patientRepository.findByUserUserId(principal.getUserId()). ClinicDiscoveryService lists only ACTIVE "
        "clinics with city/name search; SlotService computes available future time slots by subtracting active "
        "Appointments from weekday DoctorSchedules; and BookingService serializes bookings by locking the Doctor "
        "entity (doctorRepository.findForUpdate) before inserting into Appointments under UX_Appt_Slot."
    )

    b.h3("6.1.4", "Module 4: Staff-Verified Patient Account Linking & Non-Destructive Record Merge")
    b.p(
        "Implemented in PatientLinkService, ClinicLinkRequestController, and ClinicLinkRequestService (390 lines), "
        "this module bridges online accounts with pre-existing clinic walk-in charts safely. A patient submits a "
        "link request to an active clinic from My Profile. In /clinic/link-requests/{id}, ClinicLinkRequestService "
        "automatically scans the clinic's Patient Masters for matching names and Dates of Birth to surface "
        "duplicate warnings. Approval requires ticking the physical identity-verification checkbox. If the "
        "administrator supplies an existing clinic patient ID to merge into, the service verifies exact DOB "
        "equality, re-points all Appointments, Prescriptions, ClinicPatients, and PatientLinkRequests to the "
        "target record, detaches user_id from the old row, marks merged_into_patient_id on the superseded record "
        "so audit foreign keys stay valid, and records PATIENT_RECORD_MERGED."
    )

    b.h3("6.1.5", "Module 5: Clinic Administration, Doctor Schedules & CSV Medicine Formulary")
    b.p(
        "Implemented in ClinicPatientService, ClinicDoctorService, ClinicAppointmentService, and "
        "ClinicInventoryService, this module equips CLINIC_ADMIN staff with operational masters. Administrators "
        "onboard practitioners, set clinic-specific consultation fees in ClinicDoctors, define granular weekday "
        "DoctorSchedules (start time, end time, slot length in minutes, effective date bounds), and dispatch "
        "doctor login invitations. ClinicInventoryService manages standardized ClinicMedicines (drug name, dosage, "
        "default form, frequency, duration, quantity, instructions) with RFC-4180 CSV template download, bulk "
        "CSV import with upsert deduplication, and CSV export."
    )

    b.h3("6.1.6", "Module 6: Doctor Clinical Workspace — Consultations, Addenda & 32-Char e-Prescriptions")
    b.p(
        "Implemented in DoctorConsultationController, DoctorConsultationService, DoctorPrescriptionController, "
        "and PrescriptionService (390 lines), this module governs clinical encounters. Starting an appointment "
        "transitions status to IN_CONSULTATION and creates a DRAFT Consultation. While in DRAFT, diagnosis, "
        "examination notes, and follow-up date remain editable. Clicking Finalise sets status=FINAL, stamps "
        "finalised_at, marks the Appointment COMPLETED, and permanently locks the primary clinical fields; any "
        "subsequent clinical clarification is saved exclusively as an author-attributed ConsultationAddendum. "
        "PrescriptionService then validates prescribed items (including chronic lifetime flags), enforces one "
        "prescription per consultation, generates a unique 32-character code via PublicIdGenerator.prescriptionCode(), "
        "and publishes PrescriptionIssuedEvent."
    )

    b.h3("6.1.7", "Module 7: Pharmacy Workspace — Verification, Auto-Pricing POS & Finance Ledger")
    b.p(
        "Implemented in PharmacyController, PharmacyService (768 lines), PharmacyFinanceController, and "
        "PharmacyFinanceService, this module powers retail pharmacy operations. Scanning or entering a 32-character "
        "code executes a binary case-sensitive lookup AND Java exact-string filter, logs a PharmacyVerification "
        "row with a SHA-256 attempt_hash, and evaluates status, UTC expiry, and remaining item units. Inside "
        "/pharmacy/pos, verified prescription items auto-populate a billing cart pre-filled with remembered unit "
        "prices from PharmacyMedicinePrices. On sale completion, PharmacyService locks PrescriptionItems for "
        "update, verifies requested quantities do not exceed remaining units, computes BigDecimal(HALF_UP) "
        "subtotal, discount, percentage tax, net total, and change due, saves PrescriptionDispense and line items, "
        "updates remembered catalog prices, and invokes financeService to auto-post an INCOME ledger entry "
        "foreign-keyed to dispense_id."
    )

    b.h3("6.1.8", "Module 8: Transactional Email Outbox Worker & Encrypted SMTP Engine")
    b.p(
        "Implemented across IdentityEmailListener, NotificationEmailListener, EmailOutboxService, EmailTemplates, "
        "EmailDeliveryService, EmailDispatchWorker, and SmtpSettingsService, outbound notifications are never "
        "sent directly on HTTP request threads. Domain listeners render structured plain-text clinical/security "
        "emails and insert QUEUED rows into dbo.EmailLogs guarded by unique idempotency_key values. Every 5 seconds, "
        "EmailDispatchWorker invokes EmailDeliveryService.dispatchBatch(), which locks due rows, sends via the "
        "active decrypted SmtpSettings JavaMailSender (or logs in dev mode), and applies 1-minute, 5-minute, and "
        "15-minute retry backoff up to 3 attempts."
    )

    b.h2("6.2", "Core Algorithms and Cryptographic Mechanisms")
    b.bullet("Algorithm 1 — Unambiguous 32-Character CSPRNG Prescription Code Generation:",
             "PublicIdGenerator draws 32 indices from java.security.SecureRandom over the 32-character unambiguous "
             "alphabet ABCDEFGHJKLMNPQRSTUVWXYZ23456789 (deliberately omitting visually confusable 0/O and 1/I). "
             "Each character carries log2(32) = 5 bits of entropy, yielding 32 x 5 = 160 bits of cryptographic "
             "randomness (1.46 x 10^48 possibilities), making brute-force guessing computationally infeasible.")
    b.bullet("Algorithm 2 — Single-Use SHA-256 Token Issuance & Atomic Consumption:",
             "TokenHasher generates 32 random bytes (256 bits), Base64URL-encodes them without padding for email "
             "links, and stores MessageDigest('SHA-256') as BINARY(32) under unique index UX_AuthTokens_token_hash. "
             "Consumption locks the row via PESSIMISTIC_WRITE, verifies used_at IS NULL and expires_at > UTC_NOW, "
             "and stamps used_at atomically.")
    b.bullet("Algorithm 3 — AES-256-GCM Authenticated Secret Encryption:",
             "SecretCipher derives a 256-bit AES key by hashing the deployment secret with SHA-256, generates a "
             "fresh 12-byte (96-bit) SecureRandom IV per encryption, encrypts plaintext under AES/GCM/NoPadding "
             "with a 128-bit authentication tag, and prepends the 12-byte IV to the ciphertext stored in "
             "SmtpSettings.password_encrypted.")
    b.bullet("Algorithm 4 — Dual-Guard Atomic Slot Allocation:",
             "BookingService acquires a pessimistic write lock on the target Doctor entity (serializing concurrent "
             "requests for the same practitioner), re-verifies slot validity via SlotService against active "
             "appointments, and flushes the new Appointment under UX_Appt_Slot, translating any residual "
             "DataIntegrityViolationException into a clean user-facing conflict message.")
    b.bullet("Algorithm 5 — Non-Destructive Patient Record Merge:",
             "ClinicLinkRequestService verifies exact DOB match between the online account patient row P_src and "
             "the clinic master patient row P_dst, migrates ClinicPatients, Appointments, Prescriptions, and "
             "PatientLinkRequests from P_src to P_dst, moves user_id from P_src to P_dst, and sets "
             "P_src.merged_into_patient_id = P_dst.patient_id without issuing DELETE on P_src.")
    b.bullet("Algorithm 6 — Self-Learning Pharmacy POS Pricing & Auto-Ledger Sync:",
             "PharmacyService resolves unit prices from user input or falls back to case-insensitive lookup in "
             "PharmacyMedicinePrices, computes line_total = round(unit_price * qty, 2), taxable = subtotal - discount, "
             "tax_amount = round(taxable * tax_rate / 100, 2), and total = taxable + tax_amount, upserts new unit "
             "prices back into PharmacyMedicinePrices, and inserts a linked INCOME row in PharmacyFinanceEntries.")

    b.h2("6.3", "Pseudocode and Logic Flows")
    b.code("Pseudocode 6.1 — Staff-Verified Patient Link Approval & Non-Destructive Merge", """PROCEDURE ApprovePatientLinkRequest(adminUserId, clinicId, requestId, form):
  1. IF form.identityVerified == FALSE THEN
       THROW ValidationException("Confirm physical identity verification before approving.")
  2. req <- PatientLinkRequestRepository.findForUpdateByClinic(requestId, clinicId)
  3. ASSERT req.status == PENDING
  4. accountPatient <- req.patient
  5. IF form.existingClinicPatientId IS BLANK THEN
       EnsureClinicPatientLink(clinicId, accountPatient)
       req.clinicPatientRecord <- accountPatient
       req.status <- APPROVED; req.resolvedBy <- adminUserId; req.resolvedAt <- UTC_NOW()
       AuditService.record(adminUserId, CLINIC_ADMIN, "PATIENT_LINK_APPROVED", accountPatient)
     ELSE
       targetPatient <- ResolveClinicPatientByPublicOrNumericId(clinicId, form.existingClinicPatientId)
       IF targetPatient.dob != accountPatient.dob THEN
         THROW ValidationException("Date of Birth mismatch; merge blocked for clinical safety.")
       ReassignAppointmentsAndPrescriptions(from=accountPatient, to=targetPatient)
       MergeClinicPatientMemberships(from=accountPatient, to=targetPatient)
       targetPatient.user <- accountPatient.user
       accountPatient.user <- NULL
       accountPatient.mergedIntoPatientId <- targetPatient.patientId
       req.patient <- targetPatient; req.clinicPatientRecord <- targetPatient
       req.status <- APPROVED; req.resolvedBy <- adminUserId; req.resolvedAt <- UTC_NOW()
       AuditService.record(adminUserId, CLINIC_ADMIN, "PATIENT_RECORD_MERGED", targetPatient)
  6. COMMIT TRANSACTION""")

    b.code("Pseudocode 6.2 — Pharmacy POS Verification, Partial Dispensing & Auto-Finance Sync", """PROCEDURE CompletePosSaleAndDispense(pharmUserId, verificationId, qtyMap, priceMap, discount, taxRate, payMethod):
  1. pharmUser <- RequireActivePharmacyUser(pharmUserId)
  2. ver <- VerificationRepository.findById(verificationId)
  3. ASSERT ver.pharmacyId == pharmUser.pharmacyId AND ver.outcome == VALID
  4. ASSERT NOT DispenseRepository.existsByVerificationId(verificationId)
  5. rx <- PrescriptionRepository.findForUpdate(ver.prescriptionId)
  6. ASSERT rx.status == VALID AND rx.expiryDate > UTC_NOW()
  7. items <- PrescriptionItemRepository.findForUpdateByPrescriptionId(rx.prescriptionId)
  8. subtotal <- 0.00
  9. FOR EACH item IN items:
       q <- qtyMap.getOrDefault(item.id, 0)
       IF q > (item.quantity - item.quantityDispensed) THEN
         THROW IllegalArgumentException("Over-dispense blocked for " + item.medicine)
       IF q > 0 THEN
         uPrice <- Coalesce(priceMap.get(item.id), RememberedCatalogPrice(pharmUser.pharmacyId, item.medicine), 0.00)
         subtotal <- subtotal + RoundHalfUp(uPrice * q, 2)
  10. taxable <- subtotal - discount
  11. taxAmt  <- RoundHalfUp(taxable * taxRate / 100, 2)
  12. total   <- taxable + taxAmt
  13. receipt <- SavePrescriptionDispense(rx, pharmUser, ver, subtotal, discount, taxRate, taxAmt, total, payMethod)
  14. FOR EACH accepted line (item, q, uPrice):
        item.quantityDispensed <- item.quantityDispensed + q
        SaveDispenseItem(receipt, item, q, uPrice, RoundHalfUp(uPrice * q, 2))
        UpsertPharmacyMedicinePrice(pharmUser.pharmacyId, item.medicine, uPrice)
  15. IF total > 0 THEN
        PharmacyFinanceService.recordPosIncome(pharmUser.pharmacy, receipt, total, rx.prescriptionCode)
  16. AuditService.record(pharmUserId, pharmUser.role, "DISPENSE_COMPLETED", receipt.publicUuid)
  17. COMMIT TRANSACTION & RETURN receipt""")

    b.h2("6.4", "Key Source Code Implementations")
    b.p(
        "The following authentic Java source listings are excerpted directly from the clinicflow-app repository to "
        "illustrate how security rules, cryptographic primitives, atomic booking, 32-character prescribing, and "
        "transactional outbox delivery are realized in production code."
    )

    b.code("Listing 6.1 — Spring Security 6 RBAC & Tenant Filter Chain (SecurityConfig.java)", """@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/", "/login", "/setup", "/setup/**", "/invite", "/invite/**",
                "/register", "/register/**", "/verify-email", "/verify-email/**",
                "/verify-email-change", "/verify-email-change/**",
                "/forgot-password", "/forgot-password/**",
                "/reset-password", "/reset-password/**",
                "/css/**", "/js/**", "/images/**", "/actuator/health", "/error", "/error/**"
            ).permitAll()
            .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
            .requestMatchers("/clinic/**").hasRole("CLINIC_ADMIN")
            .requestMatchers("/doctor/**").hasRole("DOCTOR")
            .requestMatchers("/patient/**").hasRole("PATIENT")
            .requestMatchers("/pharmacy/**").hasAnyRole("PHARMACY_ADMIN", "PHARMACIST")
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login").loginProcessingUrl("/login")
            .usernameParameter("email").passwordParameter("password")
            .successHandler(successHandler).failureHandler(failureHandler).permitAll()
        )
        .exceptionHandling(ex -> ex.accessDeniedPage("/error/403"))
        .addFilterBefore(loginThrottleFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(clinicStatusFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}""")

    b.code("Listing 6.2 — 32-Character CSPRNG Prescription Code & AES-256-GCM Cipher (PublicIdGenerator.java & SecretCipher.java)", """// PublicIdGenerator.java — 160-bit unambiguous 32-character prescription identifier
public final class PublicIdGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    public static String patientId(long sequence) {
        return String.format(Locale.ROOT, "PAT-%d-%04d", Year.now().getValue(), sequence);
    }
    public static String appointmentId(long sequence) {
        return String.format(Locale.ROOT, "APT-%d-%05d", Year.now().getValue(), sequence);
    }
    public static String prescriptionCode() {
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}

// SecretCipher.java — Authenticated AES-256-GCM encryption for stored SMTP credentials
public byte[] encrypt(String plaintext) {
    if (plaintext == null || plaintext.isEmpty()) return null;
    byte[] iv = new byte[12];
    random.nextBytes(iv);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
    byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
    byte[] result = new byte[iv.length + encrypted.length];
    System.arraycopy(iv, 0, result, 0, iv.length);
    System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
    return result;
}""")

    b.code("Listing 6.3 — Single-Use SHA-256 Hashed Token Vault (AuthTokenService.java)", """@Transactional
public IssuedToken issue(User user, AuthTokenPurpose purpose) {
    repository.deleteByUserAndPurpose(user.getUserId(), purpose);
    String rawToken = TokenHasher.newToken();
    AuthToken token = new AuthToken();
    token.setUser(user);
    token.setPurpose(purpose);
    token.setTokenHash(TokenHasher.sha256(rawToken));
    token.setExpiresAt(now().plus(ttlFor(purpose)));
    token.setCreatedAt(now());
    repository.save(token);
    return new IssuedToken(rawToken, token);
}

@Transactional
public Optional<AuthToken> consume(String rawToken, AuthTokenPurpose purpose) {
    if (rawToken == null || rawToken.isBlank()) return Optional.empty();
    LocalDateTime now = now();
    return repository.findForUpdate(TokenHasher.sha256(rawToken), purpose)
            .filter(token -> token.getUsedAt() == null)
            .filter(token -> token.getExpiresAt().isAfter(now))
            .map(token -> { token.setUsedAt(now); return token; });
}""")

    b.code("Listing 6.4 — Atomic Conflict-Free Slot Reservation (BookingService.java)", """@Transactional
public Long book(Long patientUserId, BookingForm form) {
    Patient patient = patientRepository.findByUserUserId(patientUserId)
            .orElseThrow(() -> new IllegalStateException("Your patient profile is not linked."));
    Clinic clinic = clinicRepository.findById(form.getClinicId())
            .filter(c -> c.getStatus() == ClinicStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Clinic is not available."));
    // Pessimistic write lock serialises competing bookings for the same doctor
    Doctor doctor = doctorRepository.findForUpdate(form.getDoctorId())
            .filter(d -> d.getStatus() == DoctorStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Doctor is not available."));
    if (!slotService.isValidSlot(clinic.getClinicId(), doctor.getDoctorId(), form.getSlotStart())) {
        throw new IllegalArgumentException("That slot is no longer available. Choose another.");
    }
    Appointment appointment = new Appointment();
    appointment.setPatient(patient); appointment.setClinic(clinic); appointment.setDoctor(doctor);
    appointment.setSlotStart(form.getSlotStart());
    appointment.setSlotEnd(form.getSlotStart().plusMinutes(
            slotService.slotLengthMinutes(clinic.getClinicId(), doctor.getDoctorId(), form.getSlotStart())));
    appointment.setStatus(AppointmentStatus.BOOKED);
    try {
        appointment = appointmentRepository.saveAndFlush(appointment);
    } catch (DataIntegrityViolationException ex) {
        throw new IllegalArgumentException("That slot was just taken. Choose another.");
    }
    appointment.setPublicAppointmentId(PublicIdGenerator.appointmentId(appointment.getAppointmentId()));
    return appointmentRepository.save(appointment).getAppointmentId();
}""")

    b.code("Listing 6.5 — Non-Destructive Patient Chart Consolidation (ClinicLinkRequestService.java)", """private Patient mergeIntoClinicRecord(Patient account, Patient clinicRecord, Long clinicId,
                                      PatientLinkRequest currentRequest) {
    User accountUser = account.getUser();

    List<ClinicPatient> links = clinicPatientRepository.findByPatientPatientId(account.getPatientId());
    for (ClinicPatient link : links) {
        boolean sameClinic = link.getClinic().getClinicId().equals(clinicId);
        boolean targetAlreadyLinked = clinicPatientRepository.existsByClinicClinicIdAndPatientPatientId(
                link.getClinic().getClinicId(), clinicRecord.getPatientId());
        if (sameClinic || targetAlreadyLinked) {
            clinicPatientRepository.delete(link);
        } else {
            link.setPatient(clinicRecord);
            clinicPatientRepository.save(link);
        }
    }
    appointmentRepository.findByPatientPatientIdOrderBySlotStartDesc(account.getPatientId())
            .forEach(a -> a.setPatient(clinicRecord));
    prescriptionRepository.findByPatientPatientIdOrderByIssueDateDesc(account.getPatientId())
            .forEach(rx -> rx.setPatient(clinicRecord));

    clinicRecord.setEmail(accountUser.getEmail());
    clinicRecord.setMobile(copyIfBlank(clinicRecord.getMobile(), account.getMobile()));
    clinicRecord.setAllergies(copyIfBlank(clinicRecord.getAllergies(), account.getAllergies()));
    clinicRecord.setConditions(copyIfBlank(clinicRecord.getConditions(), account.getConditions()));
    clinicRecord.setUser(accountUser);
    clinicRecord.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
    patientRepository.save(clinicRecord);

    account.setUser(null); // Retain historical row so foreign keys in AuditLogs stay valid
    account.setMergedInto(clinicRecord);
    patientRepository.save(account);
    return clinicRecord;
}""")

    b.code("Listing 6.6 — Receipt-Linked Pharmacy Cashbook Ledger Service (PharmacyFinanceService.java)", """@Transactional
public void addEntry(Long userId, String type, BigDecimal amount, String description,
                     LocalDate entryDate, Long actorUserId, Long dispenseId) {
    PharmacyUser link = pharmacyUserRepository.findByUserUserId(userId)
            .orElseThrow(() -> new IllegalStateException("Pharmacy account is not linked."));
    if (!"INCOME".equals(type) && !"EXPENSE".equals(type)) {
        throw new IllegalArgumentException("Choose INCOME or EXPENSE.");
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("Enter an amount greater than zero.");
    }
    PharmacyFinanceEntry entry = new PharmacyFinanceEntry();
    entry.setPharmacy(link.getPharmacy());
    entry.setEntryType(type);
    entry.setAmount(amount);
    entry.setDescription(description == null || description.isBlank() ? null : description.trim());
    entry.setEntryDate(entryDate != null ? entryDate : LocalDate.now());
    entry.setCreatedBy(actorUserId);
    entry.setDispenseId(dispenseId);
    entry.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
    entryRepository.save(entry);
}""")

    b.table(
        "6.1", "Complete Thymeleaf View & Fragment Directory Inventory (61 HTML5 Templates)",
        ["Template Directory", "Count", "Representative Templates & Clinical Views"],
        [
            ["templates/layout/ & fragments/", "5", "base.html (ERP shell), auth.html (split login/register), brand.html (IMS SVG mark), page-header.html, status-badge.html"],
            ["templates/auth/ & error/", "16", "setup.html, login.html, register.html, register-check-email.html, verify-result.html, forgot-password.html, reset-password.html, invite-accept.html, error/403, 404, 500"],
            ["templates/admin/", "10", "dashboard.html, clinics.html, clinic-new.html, clinic-detail.html, pharmacies.html, pharmacy-new.html, pharmacy-detail.html, patients.html, reports.html, smtp.html"],
            ["templates/clinic/", "12", "dashboard.html, patients.html, patient-new.html, patient-detail.html, link-requests.html, link-request-detail.html, doctors.html, doctor-schedules.html, appointments.html, inventory.html, prescriptions.html"],
            ["templates/doctor/", "5", "dashboard.html, appointments.html, consultation.html (draft/final + addenda), prescription-form.html, prescriptions.html"],
            ["templates/patient/", "7", "dashboard.html, profile.html (verified DOB + clinic link requests), clinics.html, clinic-detail.html, book.html, appointments.html, prescriptions.html, history.html"],
            ["templates/pharmacy/", "6", "dashboard.html, pos.html (live 32-char Rx POS), verify.html, dispense-receipt.html, prices.html, finance.html, users.html, reports.html"],
        ],
        col_widths=[1.55, 0.55, 3.92]
    )

    b.h2("6.5", "Screenshots of Live Application Across Operational Workspaces")
    b.p(
        "Figures 6.1 through 6.14 present authentic full-resolution screenshots captured directly from the live "
        "running Infeara Medical System (IMS v0.1) deployment across the Authentication Gateway, Super Admin "
        "Platform Governance Portal, Doctor Clinical Workspace, and Pharmacy POS & Finance Workspace. Every screen "
        "reflects live SQL Server relational records, active role-scoped ERP navigation, real 32-character CSPRNG "
        "prescription identifiers, and automated POS-to-Finance ledger synchronization."
    )

    screenshots = [
        ("6.1", "IMS Unified Sign-In & Session Lifecycle Gateway (/login)", "real_01_signin.png",
         "Displays the live IMS authentication card rendered via templates/layout/auth.html with the medical-cross "
         "brand mark, post-logout confirmation banner ('You have been signed out.'), BCrypt-backed email/password "
         "inputs (mvignesh479@gmail.com), 7-day Remember-Me checkbox, password recovery link, email verification "
         "resend trigger, and self-service patient registration entry point."),
        ("6.2", "Super Admin — Platform Governance Dashboard (/admin/dashboard)", "real_02_admin_dashboard.png",
         "Shows the live Super Admin command center featuring six real-time KPI metric cards (Active Clinics: 1, "
         "Pending Enrollment: 0, Registered Patients: 2, Appointments Today: 0, Prescriptions Issued: 2, Email "
         "Failures: 0) alongside the enrolled facility table listing Vignesh Clinics (ACTIVE) and quick Enroll Clinic CTA."),
        ("6.3", "Super Admin — Multi-Tenant Clinic Enrollment Console (/admin/clinics)", "real_03_admin_clinics.png",
         "Captures the live clinic tenant directory showing Vignesh Clinics (Chennai), active Clinic Administrator "
         "vstv4k2002@gmail.com, linked doctor count (1), linked patient count (2), ACTIVE tenant status badge, "
         "and enrollment date (25 Oct 2026)."),
        ("6.4", "Super Admin — Dispensing Pharmacy Enrollment & Licence Registry (/admin/pharmacies)", "real_04_admin_pharmacies.png",
         "Depicts the live pharmacy tenant registry listing Vignesh Pharmacy with verified drug licence number "
         "KL-12356H2-77 (Chennai, Tamilnadu), active Pharmacy Administrator mvignesh479@gmail.com, staff count, "
         "and ACTIVE operational status."),
        ("6.5", "Super Admin — Privacy-Masked Cross-Clinic Patient Masters (/admin/patients)", "real_05_admin_patients.png",
         "Demonstrates IMS's privacy-by-design READ ONLY cross-clinic patient index. While Super Admin can search "
         "public IDs (PAT-2026-0004, PAT-2026-0003) and inspect clinic associations (Vignesh Clinics), contact PII "
         "is automatically masked server-side (******3289, ******4546, v***@gmail.com)."),
        ("6.6", "Super Admin — Live Platform Reports & Outbox Delivery Telemetry (/admin/reports)", "real_06_admin_reports.png",
         "Displays live cross-tenant operational totals across clinics, pharmacies, patient masters, completed "
         "appointments (COMPLETED: 2), transactional email outbox health (SENT: 13, QUEUED: 0, FAILED: 0), and "
         "patient clinic-linking verification counters."),
        ("6.7", "Super Admin — AES-256-GCM Encrypted SMTP Configuration & Mail Audit (/admin/smtp)", "real_07_admin_smtp.png",
         "Shows the live mail-relay configuration form (Host, Port 587, STARTTLS, From name IMS) enforcing at-rest "
         "AES-256-GCM password encryption ('Passwords are encrypted at rest and never shown again') paired with the "
         "live Recent Email Activity feed logging SENT events for PRESCRIPTION_ISSUED, APPOINTMENT_CONFIRMATION, "
         "RESET_PASSWORD, and VERIFY_EMAIL."),
        ("6.8", "Doctor Workspace — Practitioner Clinical Dashboard (/doctor/dashboard)", "real_08_doctor_dashboard.png",
         "Captures the role-scoped Doctor Workspace for practitioner Kiran (General Medicine), displaying personal "
         "schedule KPIs (Appointments Today: 0, Prescriptions Issued: 2, Specialization card) and today's patient queue."),
        ("6.9", "Doctor Workspace — Date & Status Filtered Appointment Queue (/doctor/appointments)", "real_09_doctor_appts_today.png",
         "Illustrates the practitioner's date-filtered daily queue (2026-10-31) with status selector, Today / All "
         "Dates quick filters, and contextual front-desk check-in workflow guidance."),
        ("6.10", "Doctor Workspace — Completed Encounters & Finalised Consultations (/doctor/appointments?all)", "real_10_doctor_appts_all.png",
         "Displays historical clinical encounters APT-2026-00004 (30 Oct 2026, 10:00) and APT-2026-00003 (26 Oct "
         "2026, 10:10) for patient vignesh m (PAT-2026-0004), both showing COMPLETED appointment status and "
         "irreversibly locked FINAL consultation status."),
        ("6.11", "Doctor Workspace — Issued 32-Character Prescriptions & Fulfilment States (/doctor/prescriptions)", "real_11_doctor_prescriptions.png",
         "Showcases two live 32-character CSPRNG electronic prescriptions authored by Dr. Kiran for PAT-2026-0004: "
         "JUEB9AGPAKBTXGWNX3MQKNCUFA3DMXRP (PARTIALLY DISPENSED, VALID until 29 Nov 2026) and "
         "VEYHFFWEGYVZUWWWQHGY28RSUT8F59WT (NOT DISPENSED, automatically marked EXPIRED on 29 Oct 2026)."),
        ("6.12", "Pharmacy Workspace — Dispensing Operations Dashboard (/pharmacy/dashboard)", "real_12_pharmacy_dashboard.png",
         "Presents Vignesh Pharmacy's operational home screen with live daily counters (Verifications Today, "
         "Dispenses Today, Pharmacy Users: 1), left-rail POS/Price List/Finance navigation, and one-click "
         "32-character prescription verification shortcut."),
        ("6.13", "Pharmacy Workspace — Live 32-Character Prescription Point of Sale (/pharmacy/pos)", "real_13_pharmacy_pos.png",
         "Captures the live Pharmacy POS terminal featuring the case-sensitive 32-character Prescription ID scanner "
         "bar, Analyze Prescription & Price List actions, live counter telemetry, and completed POS sales receipts "
         "SALE-000006 (Rs. 12.50 CASH) and SALE-000005 (Rs. 62.50 UPI) against JUEB9AGPAKBTXGWNX3MQKNCUFA3DMXRP."),
        ("6.14", "Pharmacy Workspace — Automated POS Income & Expense Finance Ledger (/pharmacy/finance)", "real_14_pharmacy_finance.png",
         "Demonstrates end-to-end POS-to-Finance accounting integration: five KPI cards track Today & 30-Day Income "
         "(Rs. 75.00), Expenses (Rs. 100.00), and Net Cashflow (-Rs. 25.00), while the Finance Ledger displays "
         "auto-posted receipt-linked POS SALE income (Rs. 12.50 CASH) alongside manual operating expenses (Electricity bill)."),
    ]

    for fnum, fcap, fname, fdesc in screenshots:
        b.p(fdesc, bold_prefix=f"Screen {fnum} Walkthrough:")
        b.figure(fnum, fcap, os.path.join(assets_dir, fname), width_in=5.92)

    b.h2("6.6", "Integration of Modules and Cross-Cutting Event Flow")
    b.p(
        "All eight modules integrate cleanly inside a single Spring ApplicationContext through three mechanisms: "
        "(1) shared GlobalModelAttributes (@ControllerAdvice) that inject navigation items, breadcrumbs, active "
        "principal metadata, and organization display names into every Thymeleaf view; (2) synchronous Spring "
        "ApplicationEventPublisher events (VerificationEmailRequested, PasswordResetEmailRequested, StaffInviteRequested, "
        "EmailChangeRequested, AppointmentBookedEvent, AppointmentCancelledEvent, and PrescriptionIssuedEvent) "
        "consumed within the same database transaction by IdentityEmailListener and NotificationEmailListener to "
        "populate dbo.EmailLogs atomically; and (3) AuditService, which enriches every security and clinical "
        "milestone with client IP resolution (ClientIp) and patient-centric indexing."
    )
