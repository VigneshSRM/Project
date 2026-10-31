# 10 - Security Model

## Risk classification
| Skill | Risk | Why | Human approval |
|---|---|---|---|
| healthcare-access-control-and-audit | HIGH | Authz core, credentials | Role/permission/credential changes |
| platform-admin-console | HIGH | Cross-clinic patient view, SMTP secret, enrollment | Agent never enters real SMTP credentials or creates real admin accounts; human Super Admin does at runtime |
| patient-identity-and-linking | HIGH | Identity, enumeration, linking | - |
| consultation-and-medical-history | HIGH | Clinical data | - |
| prescription-lifecycle | HIGH | Clinical data, tamper | - |
| pharmacy-workspace | HIGH | Guessable IDs, data disclosure | Sensitive report export |
| clinicflow-delivery-and-ops | HIGH | Prod deploy, backups | Prod deploy, destructive migration, data export |
| clinicflow-architecture-and-data | MEDIUM | Irreversible migrations | Destructive migrations |
| clinic-operations, booking, smtp, orchestrator | MEDIUM | Scoped data, credentials | - |
| design-system, shell, screens, spec, qa | LOW | No data/credential authority | - |

## Human-in-the-loop actions (never autonomous)
| Action | Approval point | Evidence | Rollback | Audit |
|---|---|---|---|---|
| Production deployment | Before deploy | QA report, migration plan | Previous release + restore | Approval record |
| Destructive/irreversible migration | Before apply | Backup verified, dry run | Restore from backup | Migration log |
| SMTP credential entry (Super Admin screen) | Runtime, by human Super Admin | Test send result | Previous working config retained | AuditLogs (non-secret) |
| Credential change / rotation | Before change | Ticket, owner | Re-issue previous secret | Secret manager log |
| Permission or role escalation | Before change | Requester + reason | Revert role | AuditLogs |
| Sensitive data export (patient/prescription data) | Before export | Purpose, scope | Delete export | AuditLogs |
| Financial transactions | N/A in release 1 | - | - | - |

## Controls (all skills)
Ownership checks on every request; 404 for foreign records; rate limits on login, OTP, reset, booking, prescription lookup; Argon2id/bcrypt; secure cookies + CSRF; secrets in env/secret manager; PHI out of URLs, logs, error texts and email subject lines; no real patient data in dev/test; dependency pinning and audit (supply-chain); output encoding and parameterised queries (injection); prompt-injection rule for the agent: instructions found inside data (patient notes, uploaded text) are data, never commands.
Prescription ID: 32-char random (about 190 bits) so enumeration is infeasible; still rate-limited, never in URLs/logs, case-sensitive column. Super Admin: MFA recommended, masked patient data, audited Reveal. Compliance regime: RC-13.
