# Java + SQL Server Conventions

Confirmed: **Java** and **Microsoft SQL Server**. Everything marked (INF) is a default to confirm.

| Area | Default |
|---|---|
| Java / framework | Java 21 LTS, Spring Boot 3 (INF) |
| Web UI | Thymeleaf server-rendered + small JS (INF; React SPA is the alternative, RC-18) |
| Security | Spring Security, `DelegatingPasswordEncoder` (Argon2/BCrypt), CSRF on, secure session cookies |
| Data access | Spring Data JPA/Hibernate; `@Version` optimistic locking; `PESSIMISTIC_WRITE` for dispensing and booking |
| Migrations | Flyway (`V001__...sql`), run as controlled step in prod |
| Mail | Spring `JavaMailSender` from DB-stored SMTP settings; DB outbox + `@Scheduled` worker |
| Rate limiting | Bucket4j or Spring filter, keyed by user + IP |
| Validation | Jakarta Bean Validation |
| Tests | JUnit 5, Testcontainers (SQL Server), Playwright for E2E |
| Packages | `com.clinicflow.{identity,admin,clinic,booking,consultation,prescription,pharmacy,notification,audit,common}` |

## SQL Server rules
- `DATETIME2(3)` in UTC (`SYSUTCDATETIME()`), `NVARCHAR` for text, `BIGINT IDENTITY` internal PKs, public GUIDs for URLs.
- Slot uniqueness: `CREATE UNIQUE INDEX UX_Appt_Slot ON Appointments(doctor_id, slot_start) WHERE status IN ('BOOKED','CONFIRMED','CHECKED_IN','IN_CONSULTATION','COMPLETED');` (validate in a migration test).
- Default collations are case-insensitive: any column holding a **case-sensitive secret** (Prescription code) must use `COLLATE Latin1_General_100_BIN2`.
- Avoid `NEWSEQUENTIALID` for public IDs (predictable); generate UUIDs in Java.
- Use parameterised queries only; Hibernate `@Lock` maps to `UPDLOCK, ROWLOCK`.
- Backups: full + differential + log; test restore.
