-- ClinicFlow initial schema (Microsoft SQL Server 2022 Express)
-- Database: ClinicFlow_Dev
-- Conventions: DATETIME2(3) UTC, NVARCHAR text, BIGINT IDENTITY PKs, UNIQUEIDENTIFIER public IDs,
--   case-sensitive binary collation on prescription_code, filtered unique index on appointment slot.

SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

-- ============================== Clinics ==============================
CREATE TABLE dbo.Clinics (
  clinic_id        BIGINT IDENTITY(1,1) CONSTRAINT PK_Clinics PRIMARY KEY,
  public_uuid      UNIQUEIDENTIFIER NOT NULL CONSTRAINT UQ_Clinics_uuid UNIQUE DEFAULT NEWID(),
  name             NVARCHAR(200) NOT NULL,
  address_line1    NVARCHAR(200) NOT NULL,
  address_line2    NVARCHAR(200) NULL,
  city             NVARCHAR(100) NOT NULL,
  area             NVARCHAR(100) NULL,
  district         NVARCHAR(100) NULL,
  state            NVARCHAR(100) NOT NULL,
  postal_code      NVARCHAR(20)  NOT NULL,
  latitude         DECIMAL(9,6)  NULL,
  longitude        DECIMAL(9,6)  NULL,
  phone            NVARCHAR(30)  NULL,
  email            NVARCHAR(254) NULL,
  opening_hours    NVARCHAR(500) NULL,
  fees             NVARCHAR(200) NULL,
  logo_url         NVARCHAR(500) NULL,
  status           VARCHAR(12)   NOT NULL CONSTRAINT DF_Clinics_status DEFAULT 'PENDING'
                   CONSTRAINT CK_Clinics_status CHECK (status IN ('PENDING','ACTIVE','SUSPENDED')),
  created_at       DATETIME2(3)  NOT NULL CONSTRAINT DF_Clinics_created DEFAULT SYSUTCDATETIME(),
  updated_at       DATETIME2(3)  NOT NULL CONSTRAINT DF_Clinics_updated DEFAULT SYSUTCDATETIME()
);
CREATE INDEX IX_Clinics_city ON dbo.Clinics(city);
GO

-- ============================== Users ==============================
CREATE TABLE dbo.Users (
  user_id          BIGINT IDENTITY(1,1) CONSTRAINT PK_Users PRIMARY KEY,
  public_uuid      UNIQUEIDENTIFIER NOT NULL CONSTRAINT UQ_Users_uuid UNIQUE DEFAULT NEWID(),
  email            NVARCHAR(254) NOT NULL CONSTRAINT UQ_Users_email UNIQUE,
  password_hash    NVARCHAR(255) NOT NULL,
  role             VARCHAR(20)   NOT NULL
                   CONSTRAINT CK_Users_role CHECK (role IN ('SUPER_ADMIN','CLINIC_ADMIN','DOCTOR','PATIENT','PHARMACY_ADMIN','PHARMACIST')),
  status           VARCHAR(12)   NOT NULL CONSTRAINT DF_Users_status DEFAULT 'PENDING'
                   CONSTRAINT CK_Users_status CHECK (status IN ('PENDING','ACTIVE','SUSPENDED','DISABLED')),
  email_verified_at DATETIME2(3) NULL,
  clinic_id        BIGINT NULL CONSTRAINT FK_Users_clinic REFERENCES dbo.Clinics(clinic_id),
  created_at       DATETIME2(3) NOT NULL CONSTRAINT DF_Users_created DEFAULT SYSUTCDATETIME(),
  updated_at       DATETIME2(3) NOT NULL CONSTRAINT DF_Users_updated DEFAULT SYSUTCDATETIME(),
  row_ver          ROWVERSION
);
CREATE INDEX IX_Users_clinic ON dbo.Users(clinic_id);
CREATE INDEX IX_Users_role   ON dbo.Users(role);
GO

-- ============================== Patients ==============================
CREATE TABLE dbo.Patients (
  patient_id        BIGINT IDENTITY(1,1) CONSTRAINT PK_Patients PRIMARY KEY,
  public_patient_id NVARCHAR(20)  NOT NULL CONSTRAINT UQ_Patients_public UNIQUE,
  user_id           BIGINT NULL CONSTRAINT FK_Patients_user REFERENCES dbo.Users(user_id),
  first_name        NVARCHAR(100) NOT NULL,
  last_name         NVARCHAR(100) NOT NULL,
  dob               DATE NOT NULL,
  gender            VARCHAR(10) NULL CONSTRAINT CK_Patients_gender CHECK (gender IN ('MALE','FEMALE','OTHER','UNKNOWN')),
  mobile            NVARCHAR(30)  NULL,
  email             NVARCHAR(254) NULL,
  address_line1     NVARCHAR(200) NULL,
  address_line2     NVARCHAR(200) NULL,
  city              NVARCHAR(100) NULL,
  district          NVARCHAR(100) NULL,
  state             NVARCHAR(100) NULL,
  postal_code       NVARCHAR(20)  NULL,
  blood_group       NVARCHAR(10)  NULL,
  allergies         NVARCHAR(1000) NULL,
  conditions        NVARCHAR(1000) NULL,
  emergency_name    NVARCHAR(200) NULL,
  emergency_phone   NVARCHAR(30)  NULL,
  source_flag       VARCHAR(20)   NOT NULL CONSTRAINT DF_Patients_source DEFAULT 'CLINIC_ENTERED'
                    CONSTRAINT CK_Patients_source CHECK (source_flag IN ('PATIENT_ENTERED','CLINIC_ENTERED')),
  created_at        DATETIME2(3) NOT NULL CONSTRAINT DF_Patients_created DEFAULT SYSUTCDATETIME(),
  updated_at        DATETIME2(3) NOT NULL CONSTRAINT DF_Patients_updated DEFAULT SYSUTCDATETIME(),
  row_ver           ROWVERSION
);
CREATE INDEX IX_Patients_user ON dbo.Patients(user_id);
GO

-- ============================== Doctors ==============================
CREATE TABLE dbo.Doctors (
  doctor_id     BIGINT IDENTITY(1,1) CONSTRAINT PK_Doctors PRIMARY KEY,
  public_uuid   UNIQUEIDENTIFIER NOT NULL CONSTRAINT UQ_Doctors_uuid UNIQUE DEFAULT NEWID(),
  name          NVARCHAR(200) NOT NULL,
  specialization NVARCHAR(100) NULL,
  status        VARCHAR(12) NOT NULL CONSTRAINT DF_Doctors_status DEFAULT 'ACTIVE'
                CONSTRAINT CK_Doctors_status CHECK (status IN ('ACTIVE','INACTIVE')),
  created_at    DATETIME2(3) NOT NULL CONSTRAINT DF_Doctors_created DEFAULT SYSUTCDATETIME(),
  updated_at    DATETIME2(3) NOT NULL CONSTRAINT DF_Doctors_updated DEFAULT SYSUTCDATETIME()
);
GO

-- ============================== ClinicDoctors ==============================
CREATE TABLE dbo.ClinicDoctors (
  clinic_doctor_id BIGINT IDENTITY(1,1) CONSTRAINT PK_ClinicDoctors PRIMARY KEY,
  clinic_id        BIGINT NOT NULL CONSTRAINT FK_CD_clinic REFERENCES dbo.Clinics(clinic_id),
  doctor_id        BIGINT NOT NULL CONSTRAINT FK_CD_doctor REFERENCES dbo.Doctors(doctor_id),
  fee              DECIMAL(10,2) NULL,
  status           VARCHAR(12) NOT NULL CONSTRAINT DF_CD_status DEFAULT 'ACTIVE'
                   CONSTRAINT CK_CD_status CHECK (status IN ('ACTIVE','INACTIVE')),
  created_at       DATETIME2(3) NOT NULL CONSTRAINT DF_CD_created DEFAULT SYSUTCDATETIME(),
  CONSTRAINT UQ_CD_pair UNIQUE (clinic_id, doctor_id)
);
CREATE INDEX IX_CD_doctor ON dbo.ClinicDoctors(doctor_id);
GO

-- ============================== DoctorSchedules ==============================
CREATE TABLE dbo.DoctorSchedules (
  schedule_id   BIGINT IDENTITY(1,1) CONSTRAINT PK_DoctorSchedules PRIMARY KEY,
  doctor_id     BIGINT NOT NULL CONSTRAINT FK_DS_doctor REFERENCES dbo.Doctors(doctor_id),
  clinic_id     BIGINT NOT NULL CONSTRAINT FK_DS_clinic  REFERENCES dbo.Clinics(clinic_id),
  weekday       TINYINT NOT NULL CONSTRAINT CK_DS_weekday CHECK (weekday BETWEEN 0 AND 6),
  start_time    TIME NOT NULL,
  end_time      TIME NOT NULL,
  slot_minutes  INT NOT NULL CONSTRAINT DF_DS_slot DEFAULT 15 CONSTRAINT CK_DS_slot CHECK (slot_minutes > 0),
  effective_from DATE NULL,
  effective_to   DATE NULL,
  created_at    DATETIME2(3) NOT NULL CONSTRAINT DF_DS_created DEFAULT SYSUTCDATETIME()
);
CREATE INDEX IX_DS_doctor_clinic ON dbo.DoctorSchedules(doctor_id, clinic_id);
GO

-- ============================== DoctorUserLinks ==============================
CREATE TABLE dbo.DoctorUserLinks (
  link_id    BIGINT IDENTITY(1,1) CONSTRAINT PK_DoctorUserLinks PRIMARY KEY,
  user_id    BIGINT NOT NULL CONSTRAINT UQ_DUL_user UNIQUE CONSTRAINT FK_DUL_user REFERENCES dbo.Users(user_id),
  doctor_id  BIGINT NOT NULL CONSTRAINT FK_DUL_doctor REFERENCES dbo.Doctors(doctor_id),
  created_at DATETIME2(3) NOT NULL CONSTRAINT DF_DUL_created DEFAULT SYSUTCDATETIME()
);
GO

-- ============================== Appointments ==============================
CREATE TABLE dbo.Appointments (
  appointment_id       BIGINT IDENTITY(1,1) CONSTRAINT PK_Appointments PRIMARY KEY,
  public_appointment_id NVARCHAR(20) NOT NULL CONSTRAINT UQ_Appt_public UNIQUE,
  patient_id           BIGINT NOT NULL CONSTRAINT FK_Appt_patient REFERENCES dbo.Patients(patient_id),
  clinic_id            BIGINT NOT NULL CONSTRAINT FK_Appt_clinic  REFERENCES dbo.Clinics(clinic_id),
  doctor_id            BIGINT NOT NULL CONSTRAINT FK_Appt_doctor  REFERENCES dbo.Doctors(doctor_id),
  slot_start           DATETIME2(3) NOT NULL,
  slot_end             DATETIME2(3) NOT NULL,
  reason               NVARCHAR(500) NULL,
  status               VARCHAR(20) NOT NULL CONSTRAINT DF_Appt_status DEFAULT 'BOOKED'
                       CONSTRAINT CK_Appt_status CHECK (status IN ('BOOKED','CONFIRMED','CHECKED_IN','IN_CONSULTATION','COMPLETED','CANCELLED','NO_SHOW')),
  booked_at            DATETIME2(3) NOT NULL CONSTRAINT DF_Appt_booked DEFAULT SYSUTCDATETIME(),
  cancelled_at         DATETIME2(3) NULL,
  cancel_reason        NVARCHAR(300) NULL,
  created_at           DATETIME2(3) NOT NULL CONSTRAINT DF_Appt_created DEFAULT SYSUTCDATETIME(),
  updated_at           DATETIME2(3) NOT NULL CONSTRAINT DF_Appt_updated DEFAULT SYSUTCDATETIME(),
  row_ver              ROWVERSION
);
CREATE INDEX IX_Appt_patient  ON dbo.Appointments(patient_id);
CREATE INDEX IX_Appt_clinic   ON dbo.Appointments(clinic_id, slot_start);
CREATE INDEX IX_Appt_doctor   ON dbo.Appointments(doctor_id, slot_start);
-- filtered unique index: no double booking on active statuses
CREATE UNIQUE INDEX UX_Appt_Slot ON dbo.Appointments(doctor_id, slot_start)
  WHERE status IN ('BOOKED','CONFIRMED','CHECKED_IN','IN_CONSULTATION','COMPLETED');
GO

-- ============================== Consultations ==============================
CREATE TABLE dbo.Consultations (
  consultation_id BIGINT IDENTITY(1,1) CONSTRAINT PK_Consultations PRIMARY KEY,
  appointment_id  BIGINT NOT NULL CONSTRAINT UQ_Cons_appt UNIQUE CONSTRAINT FK_Cons_appt REFERENCES dbo.Appointments(appointment_id),
  diagnosis       NVARCHAR(2000) NULL,
  notes           NVARCHAR(MAX)  NULL,
  follow_up       DATETIME2(3)   NULL,
  status          VARCHAR(10) NOT NULL CONSTRAINT DF_Cons_status DEFAULT 'DRAFT'
                  CONSTRAINT CK_Cons_status CHECK (status IN ('DRAFT','FINAL')),
  finalised_at    DATETIME2(3) NULL,
  created_at      DATETIME2(3) NOT NULL CONSTRAINT DF_Cons_created DEFAULT SYSUTCDATETIME(),
  updated_at      DATETIME2(3) NOT NULL CONSTRAINT DF_Cons_updated DEFAULT SYSUTCDATETIME()
);
GO

CREATE TABLE dbo.ConsultationAddenda (
  addendum_id     BIGINT IDENTITY(1,1) CONSTRAINT PK_ConsultationAddenda PRIMARY KEY,
  consultation_id BIGINT NOT NULL CONSTRAINT FK_CA_cons REFERENCES dbo.Consultations(consultation_id),
  author_user_id  BIGINT NOT NULL CONSTRAINT FK_CA_user  REFERENCES dbo.Users(user_id),
  reason          NVARCHAR(300) NOT NULL,
  text            NVARCHAR(MAX) NOT NULL,
  created_at      DATETIME2(3) NOT NULL CONSTRAINT DF_CA_created DEFAULT SYSUTCDATETIME()
);
CREATE INDEX IX_CA_cons ON dbo.ConsultationAddenda(consultation_id);
GO

-- ============================== Prescriptions ==============================
CREATE TABLE dbo.Prescriptions (
  prescription_id   BIGINT IDENTITY(1,1) CONSTRAINT PK_Prescriptions PRIMARY KEY,
  prescription_code CHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL
                    CONSTRAINT UQ_Rx_code UNIQUE
                    CONSTRAINT CK_Rx_code CHECK (LEN(prescription_code)=32 AND prescription_code NOT LIKE '%[^A-Za-z0-9]%'),
  public_uuid       UNIQUEIDENTIFIER NOT NULL CONSTRAINT UQ_Rx_uuid UNIQUE DEFAULT NEWID(),
  consultation_id   BIGINT NOT NULL CONSTRAINT FK_Rx_cons REFERENCES dbo.Consultations(consultation_id),
  patient_id        BIGINT NOT NULL CONSTRAINT FK_Rx_patient REFERENCES dbo.Patients(patient_id),
  clinic_id         BIGINT NOT NULL CONSTRAINT FK_Rx_clinic  REFERENCES dbo.Clinics(clinic_id),
  doctor_id         BIGINT NOT NULL CONSTRAINT FK_Rx_doctor  REFERENCES dbo.Doctors(doctor_id),
  issue_date        DATETIME2(3) NOT NULL CONSTRAINT DF_Rx_issue DEFAULT SYSUTCDATETIME(),
  expiry_date       DATETIME2(3) NOT NULL,
  status            VARCHAR(10) NOT NULL CONSTRAINT DF_Rx_status DEFAULT 'VALID'
                    CONSTRAINT CK_Rx_status CHECK (status IN ('VALID','CANCELLED')),
  cancel_reason     NVARCHAR(300) NULL,
  cancelled_at      DATETIME2(3) NULL,
  cancelled_by      BIGINT NULL CONSTRAINT FK_Rx_cancelledby REFERENCES dbo.Users(user_id),
  created_at        DATETIME2(3) NOT NULL CONSTRAINT DF_Rx_created DEFAULT SYSUTCDATETIME(),
  updated_at        DATETIME2(3) NOT NULL CONSTRAINT DF_Rx_updated DEFAULT SYSUTCDATETIME()
);
CREATE INDEX IX_Rx_patient ON dbo.Prescriptions(patient_id);
CREATE INDEX IX_Rx_clinic  ON dbo.Prescriptions(clinic_id);
GO

CREATE TABLE dbo.PrescriptionItems (
  prescription_item_id BIGINT IDENTITY(1,1) CONSTRAINT PK_PrescriptionItems PRIMARY KEY,
  prescription_id      BIGINT NOT NULL CONSTRAINT FK_PI_rx REFERENCES dbo.Prescriptions(prescription_id),
  medicine             NVARCHAR(200) NOT NULL,
  dosage               NVARCHAR(100) NULL,
  frequency            NVARCHAR(100) NULL,
  duration             NVARCHAR(100) NULL,
  quantity             INT NOT NULL CONSTRAINT CK_PI_qty CHECK (quantity > 0),
  quantity_unit        NVARCHAR(30)  NULL,
  quantity_dispensed   INT NOT NULL CONSTRAINT DF_PI_disp DEFAULT 0,
  instructions         NVARCHAR(500) NULL,
  row_ver              ROWVERSION,
  CONSTRAINT CK_PI_disp CHECK (quantity_dispensed BETWEEN 0 AND quantity)
);
CREATE INDEX IX_PI_rx ON dbo.PrescriptionItems(prescription_id);
GO

-- ============================== Pharmacy (from pharmacy-schema.sql) ==============================
CREATE TABLE dbo.Pharmacies (
  pharmacy_id     BIGINT IDENTITY(1,1) CONSTRAINT PK_Pharmacies PRIMARY KEY,
  public_uuid     UNIQUEIDENTIFIER NOT NULL CONSTRAINT UQ_Pharmacies_uuid UNIQUE DEFAULT NEWID(),
  name            NVARCHAR(200) NOT NULL,
  licence_number  NVARCHAR(50)  NOT NULL CONSTRAINT UQ_Pharmacies_licence UNIQUE,
  address_line1   NVARCHAR(200) NOT NULL,
  address_line2   NVARCHAR(200) NULL,
  city            NVARCHAR(100) NOT NULL,
  district        NVARCHAR(100) NULL,
  state           NVARCHAR(100) NOT NULL,
  postal_code     NVARCHAR(20)  NOT NULL,
  phone           NVARCHAR(30)  NULL,
  email           NVARCHAR(254) NULL,
  status          VARCHAR(12)   NOT NULL CONSTRAINT DF_Pharmacies_status DEFAULT 'PENDING'
                  CONSTRAINT CK_Pharmacies_status CHECK (status IN ('PENDING','ACTIVE','SUSPENDED')),
  created_at      DATETIME2(3)  NOT NULL CONSTRAINT DF_Pharmacies_created DEFAULT SYSUTCDATETIME()
);
GO

CREATE TABLE dbo.PharmacyUsers (
  pharmacy_user_id BIGINT IDENTITY(1,1) CONSTRAINT PK_PharmacyUsers PRIMARY KEY,
  user_id          BIGINT NOT NULL CONSTRAINT UQ_PharmacyUsers_user UNIQUE
                   CONSTRAINT FK_PharmacyUsers_user REFERENCES dbo.Users(user_id),
  pharmacy_id      BIGINT NOT NULL CONSTRAINT FK_PharmacyUsers_pharmacy REFERENCES dbo.Pharmacies(pharmacy_id),
  role             VARCHAR(16) NOT NULL CONSTRAINT CK_PharmacyUsers_role CHECK (role IN ('PHARMACY_ADMIN','PHARMACIST')),
  status           VARCHAR(12) NOT NULL CONSTRAINT DF_PharmacyUsers_status DEFAULT 'ACTIVE'
                   CONSTRAINT CK_PharmacyUsers_status CHECK (status IN ('ACTIVE','DISABLED')),
  created_at       DATETIME2(3) NOT NULL CONSTRAINT DF_PharmacyUsers_created DEFAULT SYSUTCDATETIME()
);
CREATE INDEX IX_PharmacyUsers_pharmacy ON dbo.PharmacyUsers(pharmacy_id);
GO

CREATE TABLE dbo.PharmacyVerifications (
  verification_id  UNIQUEIDENTIFIER NOT NULL CONSTRAINT PK_PharmacyVerifications PRIMARY KEY DEFAULT NEWID(),
  pharmacy_id      BIGINT NOT NULL CONSTRAINT FK_PV_pharmacy REFERENCES dbo.Pharmacies(pharmacy_id),
  pharmacy_user_id BIGINT NOT NULL CONSTRAINT FK_PV_user REFERENCES dbo.PharmacyUsers(pharmacy_user_id),
  prescription_id  BIGINT NULL CONSTRAINT FK_PV_prescription REFERENCES dbo.Prescriptions(prescription_id),
  attempt_hash     BINARY(32) NULL,
  outcome          VARCHAR(20) NOT NULL CONSTRAINT CK_PV_outcome
                   CHECK (outcome IN ('VALID','EXPIRED','CANCELLED','FULLY_DISPENSED','NOT_FOUND','RATE_LIMITED')),
  verified_at      DATETIME2(3) NOT NULL CONSTRAINT DF_PV_at DEFAULT SYSUTCDATETIME(),
  ip_address       VARCHAR(45) NULL
);
CREATE INDEX IX_PV_pharmacy_time ON dbo.PharmacyVerifications(pharmacy_id, verified_at DESC);
CREATE INDEX IX_PV_prescription  ON dbo.PharmacyVerifications(prescription_id) WHERE prescription_id IS NOT NULL;
GO

CREATE TABLE dbo.PrescriptionDispenses (
  dispense_id      BIGINT IDENTITY(1,1) CONSTRAINT PK_PrescriptionDispenses PRIMARY KEY,
  public_uuid      UNIQUEIDENTIFIER NOT NULL CONSTRAINT UQ_PD_uuid UNIQUE DEFAULT NEWID(),
  prescription_id  BIGINT NOT NULL CONSTRAINT FK_PD_prescription REFERENCES dbo.Prescriptions(prescription_id),
  pharmacy_id      BIGINT NOT NULL CONSTRAINT FK_PD_pharmacy REFERENCES dbo.Pharmacies(pharmacy_id),
  pharmacy_user_id BIGINT NOT NULL CONSTRAINT FK_PD_user REFERENCES dbo.PharmacyUsers(pharmacy_user_id),
  verification_id  UNIQUEIDENTIFIER NOT NULL CONSTRAINT FK_PD_verification REFERENCES dbo.PharmacyVerifications(verification_id),
  dispensed_at     DATETIME2(3) NOT NULL CONSTRAINT DF_PD_at DEFAULT SYSUTCDATETIME(),
  status           VARCHAR(10) NOT NULL CONSTRAINT DF_PD_status DEFAULT 'COMPLETED'
                   CONSTRAINT CK_PD_status CHECK (status IN ('COMPLETED','VOIDED')),
  notes            NVARCHAR(500) NULL,
  voided_at        DATETIME2(3) NULL,
  voided_by        BIGINT NULL CONSTRAINT FK_PD_voidedby REFERENCES dbo.PharmacyUsers(pharmacy_user_id),
  void_reason      NVARCHAR(300) NULL,
  CONSTRAINT CK_PD_void CHECK ((status='VOIDED' AND voided_at IS NOT NULL AND void_reason IS NOT NULL) OR status='COMPLETED')
);
CREATE INDEX IX_PD_prescription ON dbo.PrescriptionDispenses(prescription_id);
CREATE INDEX IX_PD_pharmacy_time ON dbo.PrescriptionDispenses(pharmacy_id, dispensed_at DESC);
GO

CREATE TABLE dbo.PrescriptionDispenseItems (
  dispense_item_id     BIGINT IDENTITY(1,1) CONSTRAINT PK_PDI PRIMARY KEY,
  dispense_id          BIGINT NOT NULL CONSTRAINT FK_PDI_dispense REFERENCES dbo.PrescriptionDispenses(dispense_id),
  prescription_item_id BIGINT NOT NULL CONSTRAINT FK_PDI_item REFERENCES dbo.PrescriptionItems(prescription_item_id),
  quantity_dispensed   INT NOT NULL CONSTRAINT CK_PDI_qty CHECK (quantity_dispensed > 0),
  CONSTRAINT UQ_PDI UNIQUE (dispense_id, prescription_item_id)
);
GO

CREATE VIEW dbo.vw_PrescriptionFulfilment AS
SELECT i.prescription_id,
       CASE WHEN SUM(i.quantity_dispensed) = 0 THEN 'NOT_DISPENSED'
            WHEN SUM(CASE WHEN i.quantity_dispensed < i.quantity THEN 1 ELSE 0 END) = 0 THEN 'FULLY_DISPENSED'
            ELSE 'PARTIALLY_DISPENSED' END AS fulfilment_status
FROM dbo.PrescriptionItems i
GROUP BY i.prescription_id;
GO

-- ============================== EmailLogs ==============================
CREATE TABLE dbo.EmailLogs (
  email_log_id    BIGINT IDENTITY(1,1) CONSTRAINT PK_EmailLogs PRIMARY KEY,
  type            VARCHAR(40) NOT NULL,
  recipient       NVARCHAR(254) NOT NULL,
  status          VARCHAR(12) NOT NULL CONSTRAINT DF_EL_status DEFAULT 'QUEUED'
                  CONSTRAINT CK_EL_status CHECK (status IN ('QUEUED','SENT','FAILED')),
  attempts        INT NOT NULL CONSTRAINT DF_EL_attempts DEFAULT 0,
  last_error      NVARCHAR(1000) NULL,
  idempotency_key NVARCHAR(100) NOT NULL CONSTRAINT UQ_EL_idem UNIQUE,
  created_at      DATETIME2(3) NOT NULL CONSTRAINT DF_EL_created DEFAULT SYSUTCDATETIME(),
  sent_at         DATETIME2(3) NULL
);
CREATE INDEX IX_EL_status ON dbo.EmailLogs(status, created_at);
GO

-- ============================== SmtpSettings ==============================
CREATE TABLE dbo.SmtpSettings (
  settings_id       BIGINT IDENTITY(1,1) CONSTRAINT PK_SmtpSettings PRIMARY KEY,
  host              NVARCHAR(200) NOT NULL,
  port              INT NOT NULL,
  security          VARCHAR(10) NOT NULL CONSTRAINT CK_SS_security CHECK (security IN ('NONE','STARTTLS','SSL')),
  username          NVARCHAR(200) NULL,
  password_encrypted VARBINARY(512) NULL,
  from_name         NVARCHAR(200) NULL,
  from_address      NVARCHAR(254) NULL,
  active            BIT NOT NULL CONSTRAINT DF_SS_active DEFAULT 0,
  last_test_at      DATETIME2(3) NULL,
  last_test_result  NVARCHAR(200) NULL,
  updated_at        DATETIME2(3) NOT NULL CONSTRAINT DF_SS_updated DEFAULT SYSUTCDATETIME()
);
GO

-- ============================== ClinicEnrollments ==============================
CREATE TABLE dbo.ClinicEnrollments (
  enrollment_id     BIGINT IDENTITY(1,1) CONSTRAINT PK_ClinicEnrollments PRIMARY KEY,
  clinic_id         BIGINT NOT NULL CONSTRAINT FK_CE_clinic REFERENCES dbo.Clinics(clinic_id),
  enrolled_by       BIGINT NOT NULL CONSTRAINT FK_CE_user REFERENCES dbo.Users(user_id),
  invite_token_hash BINARY(32) NULL,
  status            VARCHAR(12) NOT NULL CONSTRAINT DF_CE_status DEFAULT 'PENDING'
                    CONSTRAINT CK_CE_status CHECK (status IN ('PENDING','ACTIVE','SUSPENDED')),
  enrolled_at       DATETIME2(3) NOT NULL CONSTRAINT DF_CE_enrolled DEFAULT SYSUTCDATETIME(),
  activated_at      DATETIME2(3) NULL
);
GO

-- ============================== ClinicPatients ==============================
CREATE TABLE dbo.ClinicPatients (
  clinic_patient_id BIGINT IDENTITY(1,1) CONSTRAINT PK_ClinicPatients PRIMARY KEY,
  clinic_id         BIGINT NOT NULL CONSTRAINT FK_CP_clinic REFERENCES dbo.Clinics(clinic_id),
  patient_id        BIGINT NOT NULL CONSTRAINT FK_CP_patient REFERENCES dbo.Patients(patient_id),
  created_at        DATETIME2(3) NOT NULL CONSTRAINT DF_CP_created DEFAULT SYSUTCDATETIME(),
  CONSTRAINT UQ_CP_pair UNIQUE (clinic_id, patient_id)
);
GO

-- ============================== PatientLinkRequests ==============================
CREATE TABLE dbo.PatientLinkRequests (
  link_request_id   BIGINT IDENTITY(1,1) CONSTRAINT PK_PatientLinkRequests PRIMARY KEY,
  patient_id        BIGINT NOT NULL CONSTRAINT FK_PLR_patient REFERENCES dbo.Patients(patient_id),
  clinic_id         BIGINT NOT NULL CONSTRAINT FK_PLR_clinic  REFERENCES dbo.Clinics(clinic_id),
  method            VARCHAR(20) NOT NULL CONSTRAINT CK_PLR_method CHECK (method IN ('OTP','CLINIC_APPROVAL','CODE')),
  status            VARCHAR(12) NOT NULL CONSTRAINT DF_PLR_status DEFAULT 'PENDING'
                    CONSTRAINT CK_PLR_status CHECK (status IN ('PENDING','APPROVED','REJECTED','EXPIRED')),
  attempts          INT NOT NULL CONSTRAINT DF_PLR_attempts DEFAULT 0,
  created_at        DATETIME2(3) NOT NULL CONSTRAINT DF_PLR_created DEFAULT SYSUTCDATETIME(),
  resolved_at       DATETIME2(3) NULL
);
GO

-- ============================== AuthTokens ==============================
CREATE TABLE dbo.AuthTokens (
  token_id    BIGINT IDENTITY(1,1) CONSTRAINT PK_AuthTokens PRIMARY KEY,
  user_id     BIGINT NOT NULL CONSTRAINT FK_AT_user REFERENCES dbo.Users(user_id),
  purpose     VARCHAR(20) NOT NULL CONSTRAINT CK_AT_purpose CHECK (purpose IN ('VERIFY_EMAIL','RESET_PASSWORD')),
  token_hash  BINARY(32) NOT NULL,
  expires_at  DATETIME2(3) NOT NULL,
  used_at     DATETIME2(3) NULL,
  created_at  DATETIME2(3) NOT NULL CONSTRAINT DF_AT_created DEFAULT SYSUTCDATETIME()
);
CREATE INDEX IX_AT_user ON dbo.AuthTokens(user_id, purpose);
GO

-- ============================== AuditLogs ==============================
CREATE TABLE dbo.AuditLogs (
  audit_id       BIGINT IDENTITY(1,1) CONSTRAINT PK_AuditLogs PRIMARY KEY,
  actor_user_id  BIGINT NULL CONSTRAINT FK_AL_user REFERENCES dbo.Users(user_id),
  role           VARCHAR(20) NULL,
  action         VARCHAR(80) NOT NULL,
  resource_type  VARCHAR(60) NOT NULL,
  resource_id    NVARCHAR(100) NULL,
  patient_id     BIGINT NULL CONSTRAINT FK_AL_patient REFERENCES dbo.Patients(patient_id),
  ip_address     VARCHAR(45) NULL,
  outcome        VARCHAR(12) NOT NULL CONSTRAINT CK_AL_outcome CHECK (outcome IN ('SUCCESS','FAILURE','DENIED')),
  created_at     DATETIME2(3) NOT NULL CONSTRAINT DF_AL_created DEFAULT SYSUTCDATETIME()
);
CREATE INDEX IX_AL_patient ON dbo.AuditLogs(patient_id, created_at DESC);
CREATE INDEX IX_AL_actor   ON dbo.AuditLogs(actor_user_id, created_at DESC);
GO

PRINT 'ClinicFlow schema created.';
GO