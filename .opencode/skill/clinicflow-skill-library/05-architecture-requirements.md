# 05 - Architecture Requirements

**Stack (confirmed): Java + Microsoft SQL Server.** Framework defaults (Spring Boot, Flyway, Thymeleaf) are INFERRED - see `clinicflow-architecture-and-data/references/java-mssql-conventions.md`. Hosting, scale and time zone remain RC-01.

- **Style:** modular monolith, one deployable, one relational DB (source: "single relational database" is acceptable).
- **Layers:** SPA/server-rendered UI -> API with auth middleware + validation -> domain services -> repositories.
- **Modules:** identity, platform-admin, clinic-ops, booking, consultation, prescription, pharmacy, notifications, audit.
- **Data integrity:** SQL Server filtered unique index for slot uniqueness, CHECK constraints for dispensing quantities, case-sensitive collation for the Prescription code,  FK integrity, unique public IDs; transactions around booking and prescription issue.
- **Async:** email queue + worker with retry.
- **UI architecture:** one shell component; route metadata (title, breadcrumbs, role, nav item); design tokens CSS; shared components; role-keyed nav config; server-driven permission list.
- **API:** resource-oriented, versioned, consistent error envelope, pagination, no PHI in URLs/query strings.
- **Security:** see `10-security-model.md`.
- **NFR placeholders (RC-01):** page load target, concurrency, uptime, backup RPO/RTO. Suggested starting hypothesis (unconfirmed): interactive pages under 2 s on a typical connection at college scale.
- **Observability:** structured logs without PHI, request IDs, health endpoint, error monitoring.
- **Environments:** dev, staging, prod; synthetic data outside prod.
