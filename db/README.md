# ClinicFlow database

- Engine: Microsoft SQL Server 2022 Express (`localhost\SQLEXPRESS`)
- Database: `ClinicFlow_Dev`
- Auth: SQL login `SA` (password in `.opencode/skill/Passwords.txt`)
- Migrations:
  - `db/migrations/V001__init_schema.sql` — 24 tables + `vw_PrescriptionFulfilment`
  - `db/migrations/V002__identity_email_outbox.sql` — email outbox columns and auth-token indexes
  - `db/migrations/V003__patient_link_request_resolution.sql` — link resolution metadata, clinic-status indexes

## Apply migrations with sqlcmd

`sqlcmd` uses Shared Memory locally, so it works even while TCP/IP is disabled.
Run the scripts in version order with `QUOTED_IDENTIFIER ON` (the scripts set it themselves):

```powershell
$sqlcmd = "C:\Program Files\Microsoft SQL Server\Client SDK\ODBC\170\Tools\Binn\sqlcmd.exe"
& $sqlcmd -S ".\SQLEXPRESS" -U SA -P "Wonder@12345" -C -b -Q "CREATE DATABASE ClinicFlow_Dev;"
& $sqlcmd -S ".\SQLEXPRESS" -U SA -P "Wonder@12345" -C -b -d ClinicFlow_Dev -f 65001 -i "db\migrations\V001__init_schema.sql"
& $sqlcmd -S ".\SQLEXPRESS" -U SA -P "Wonder@12345" -C -b -d ClinicFlow_Dev -f 65001 -i "db\migrations\V002__identity_email_outbox.sql"
& $sqlcmd -S ".\SQLEXPRESS" -U SA -P "Wonder@12345" -C -b -d ClinicFlow_Dev -f 65001 -i "db\migrations\V003__patient_link_request_resolution.sql"
```

## Flyway

The Spring Boot application runs Flyway with:

```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 1
```

Because `V001` was applied before the application existed, the first application run
baselines the existing schema at version `1` and applies `V002` and `V003`. Do not run
`V002` or `V003` manually against `ClinicFlow_Dev` before the first Flyway run, or Flyway
will try to add columns that already exist.

## JDBC / TCP

The Microsoft JDBC driver needs TCP/IP. SQL Server Express currently listens on
Shared Memory only, so the default profile cannot connect until TCP/IP is enabled:

1. SQL Server 2022 Configuration Manager → SQL Server Network Configuration →
   Protocols for SQLEXPRESS → TCP/IP → Enabled.
2. IP Addresses → IPAll → TCP Port `1433` (clear TCP Dynamic Ports).
3. Restart `SQL Server (SQLEXPRESS)`.
4. Start the app with `scripts\run-sqlserver.ps1`, or set `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`.

Until then, use the in-memory H2 profile (`scripts\run-h2.ps1`); it recreates the
schema on each start and is for UI/flow work only.

## Notes

- `prescription_code` uses `Latin1_General_100_BIN2` (case-sensitive).
- Appointment slot uniqueness is enforced by filtered unique index `UX_Appt_Slot`.
- `quantity_dispensed <= quantity` is enforced by a CHECK constraint.
- UTC timestamps use `DATETIME2(3)`; text uses `NVARCHAR`; primary keys are `BIGINT IDENTITY`.
- Auth tokens are stored as SHA-256 hashes in `AuthTokens.token_hash` (`BINARY(32)`),
  with a unique index (`UX_AuthTokens_token_hash`).
- `EmailLogs` stores the rendered outbox message (`subject`, `body`) and the next
  delivery attempt time (`next_attempt_at`), indexed by `(status, next_attempt_at)`.
- `PatientLinkRequests` stores the clinic patient record confirmed by staff, the resolving
  user and the resolution note, indexed by `(clinic_id, status, created_at DESC)` and
  `(patient_id, status)`. On approval the account's clinic links, appointments, prescriptions
  and request history are moved to that record; the old account row is retained without a
  user so audit entries remain valid.
- The schema and all three migrations were verified against SQL Server 2022 Express in a
  scratch database (`ClinicFlow_V003_Check` for `V003`) before being left for the
  application's Flyway baseline run.
