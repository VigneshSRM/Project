# Local run scripts

`run-h2.ps1` starts the application against an in-memory H2 database in SQL Server
compatibility mode. Use it when SQL Server Express TCP/IP is not enabled yet.

`run-sqlserver.ps1` starts the application against the real `ClinicFlow_Dev`
database on `localhost\SQLEXPRESS`.

Both scripts assume the Maven distribution downloaded into
`D:\JAVA_APP\PROJECT\tools\apache-maven-3.9.11` and JDK 27 at
`C:\Program Files\Java\jdk-27`.

## Enable TCP/IP on SQL Server Express

The JDBC driver only speaks TCP. The current SQLEXPRESS instance has TCP/IP
disabled and SQL Server Browser stopped, so it is reachable from `sqlcmd` over
shared memory but not from Java.

1. Open **SQL Server 2022 Configuration Manager**.
2. **SQL Server Network Configuration > Protocols for SQLEXPRESS**.
3. Open **TCP/IP** and set **Enabled = Yes**.
4. Open **TCP/IP Properties > IP Addresses > IPAll**.
5. Clear **TCP Dynamic Ports** and set **TCP Port = 1433**.
6. Restart the **SQL Server (SQLEXPRESS)** service.
7. Verify with `Test-NetConnection localhost -Port 1433`.

Then run `run-sqlserver.ps1`. The default `DB_URL` uses the named instance, so
SQL Server Browser can also stay stopped when port 1433 is fixed; override it if
needed:

```powershell
$env:DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=ClinicFlow_Dev;encrypt=true;trustServerCertificate=true"
```
