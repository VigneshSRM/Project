# 03 - Domain Model

| Domain | Purpose | Actors | Key entities | Rules | Depends on | Security | Transactional |
|---|---|---|---|---|---|---|---|
| Identity & Access | Accounts, auth, roles, linking | Patient, staff, pharmacist | Users, Patients, AuthTokens, PatientLinkRequests | No auto-link; verified login | - | HIGH | Medium |
| Clinic Operations | Clinic profile, doctors, schedules, patient masters, dashboard | Clinic staff | Clinics, Doctors, ClinicDoctors, DoctorSchedules, ClinicPatients | Scoped to clinic; no hard delete | Identity | MEDIUM | Low |
| Booking | Discovery + appointments | Patient, clinic | Appointments | No double booking; state machine | Clinic Ops, Identity | MEDIUM | HIGH (slot uniqueness) |
| Clinical Records | Consultations + history | Doctor, patient | Consultations, Addenda | Immutable after finalise | Booking | HIGH | HIGH (audit) |
| Prescription | Issue, expire, cancel, view | Doctor, patient | Prescriptions, Items | Immutable; derived expiry | Clinical | HIGH | HIGH |
| Pharmacy | Verify, partial dispensing, users, reports | Pharmacist, pharmacy admin | Pharmacies, PharmacyUsers, PharmacyVerifications, PrescriptionDispenses, PrescriptionDispenseItems | Block expired/cancelled; log all | Prescription | HIGH | Medium |
| Platform Administration | Enrollment, platform view, reports, SMTP | Super Admin | ClinicEnrollments, SmtpSettings | No clinical data; audited | Clinic Ops, Identity | HIGH | Medium |
| Notifications (cross-cutting) | Email | System | EmailLogs | Best effort, retry | - | MEDIUM | Low |
| Audit (cross-cutting) | Evidence | System | AuditLogs | Append-only | - | HIGH | Medium |

Relationships follow the source "Database Relationships" list plus ClinicPatients (clinic ↔ patient) and Pharmacies (pharmacy ↔ users, verifications).
