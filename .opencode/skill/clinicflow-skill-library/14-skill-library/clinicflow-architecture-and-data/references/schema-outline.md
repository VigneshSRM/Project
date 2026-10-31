# Schema Outline (DB-agnostic)

Tag: **SRC** = from source document, **INF** = inferred (confirm).

| Table | Tag | Key columns / constraints |
|---|---|---|
| Users | SRC | id, email (unique), password_hash, role, status, email_verified_at |
| Patients | SRC | id, public_patient_id (unique), user_id (nullable until registered), name, dob, gender, mobile, address fields, blood_group, allergies, conditions, emergency contact, source_flag |
| Clinics | SRC | id, name, address, city, area, postal_code, latitude, longitude, phone, opening_hours, fees (optional), logo (optional) |
| Doctors | SRC | id, name, specialization, status |
| ClinicDoctors | SRC | clinic_id, doctor_id, fee, status (unique pair) |
| DoctorSchedules | SRC | doctor_id, clinic_id, weekday, start, end, slot_minutes (`REQUIRES-CONFIRMATION`), exceptions |
| Appointments | SRC | id, public_id (unique), patient_id, clinic_id, doctor_id, slot_start, reason, status, booked_at; **filtered unique index (doctor_id, slot_start) WHERE status IN (active statuses)** |
| Consultations | SRC | id, appointment_id (unique), diagnosis, notes, follow_up, status (draft/final), finalised_at |
| ConsultationAddenda | INF | consultation_id, author, reason, text, created_at |
| Prescriptions | SRC | id, prescription_code CHAR(32) case-sensitive unique (random, unguessable), public_uuid (URLs), consultation_id, patient_id, clinic_id, doctor_id, issue_date, expiry_date, status (valid/cancelled; expired derived), cancel_reason |
| PrescriptionItems | SRC | prescription_id, medicine, dosage, frequency, duration, quantity, quantity_unit, quantity_dispensed (0..quantity), instructions, row_ver |
| PharmacyVerifications | SRC | id, pharmacy_id, user_id, prescription_public_id, outcome, at, ip |
| EmailLogs | SRC | id, type, recipient, status, attempts, last_error, idempotency_key (unique) |
| Pharmacies, PharmacyUsers, PharmacyVerifications, PrescriptionDispenses, PrescriptionDispenseItems | PLANNED | See `pharmacy-workspace/references/pharmacy-schema.md` + `.sql` |
| SmtpSettings | INF | host, port, security, username, password_encrypted, from, active, last_test_at/result (single active row) |
| Roles/Users | SRC/INF | role in SUPER_ADMIN, CLINIC_ADMIN, DOCTOR, PATIENT, PHARMACY_ADMIN, PHARMACIST; DoctorUsers link (user_id, doctor_id) |
| ClinicEnrollments | INF | clinic_id, enrolled_by, invite token hash, status |
| ClinicPatients | INF | clinic_id, patient_id (unique pair) |
| PatientLinkRequests | INF | id, patient_id, clinic_patient_record_id, method, status, attempts |
| AuthTokens | INF | user_id, purpose (verify/reset), token_hash, expires_at, used_at |
| AuditLogs | INF | id, actor, role, action, resource_type, resource_id, patient_id, ip, outcome, at (append-only) |
