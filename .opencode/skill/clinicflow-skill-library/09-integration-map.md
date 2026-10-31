# 09 - Integration Map

| System | Purpose | Protocol / auth | Direction | Sensitivity | Failure / retry | Idempotency | Audit | Status |
|---|---|---|---|---|---|---|---|---|
| SMTP server | Verify email, reset, invites, prescription ID | SMTP over TLS; settings entered by Super Admin, stored AES-GCM encrypted (Spring `JavaMailSender`) | Write | MEDIUM (minimal content) | Queue, 3 retries backoff, mark failed, Resend | Key per (event, recipient) | EmailLogs | Provider RC-01/10 |
| Browser Geolocation API | "Near me" | Browser permission, not stored | Read | MEDIUM | Fallback to text search | n/a | none | U (optional) |
| Maps | Optional | - | - | - | - | - | - | Out of scope |
| Online payments | Optional | - | - | - | - | - | - | Out of scope |
| SMS | Optional | - | - | - | - | - | - | Out of scope |

**MCP candidates: 0.** No external system justifies an MCP server; none invented. If a mail provider with an MCP is later chosen, mark `REQUIRES-CONFIRMATION`.
**A2A candidates: 0.** A single agent with these skills suffices; the trust boundaries here are runtime roles, not agents.
