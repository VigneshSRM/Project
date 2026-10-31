-- ClinicFlow: pharmacy + dispensing tables (Microsoft SQL Server). DRAFT - validate with Flyway on a dev instance.
-- Assumes dbo.Users(user_id BIGINT PK), dbo.Prescriptions(prescription_id BIGINT PK), dbo.PrescriptionItems(prescription_item_id BIGINT PK) exist.

-- 1) Fragments that belong to the Prescriptions / PrescriptionItems create scripts -------------------------
-- Prescriptions:
--   prescription_code CHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,   -- 32-char random, CASE-SENSITIVE
--   public_uuid       UNIQUEIDENTIFIER NOT NULL,                           -- generated in Java (UUID.randomUUID()), used in URLs
--   CONSTRAINT UQ_Prescriptions_code UNIQUE (prescription_code),
--   CONSTRAINT UQ_Prescriptions_uuid UNIQUE (public_uuid),
--   CONSTRAINT CK_Prescriptions_code CHECK (LEN(prescription_code)=32 AND prescription_code NOT LIKE '%[^A-Za-z0-9]%')
-- PrescriptionItems:
--   quantity           INT NOT NULL CONSTRAINT CK_PrescriptionItems_qty CHECK (quantity > 0),
--   quantity_unit      NVARCHAR(30) NULL,
--   quantity_dispensed INT NOT NULL CONSTRAINT DF_PrescriptionItems_disp DEFAULT 0,
--   row_ver            ROWVERSION,
--   CONSTRAINT CK_PrescriptionItems_disp CHECK (quantity_dispensed BETWEEN 0 AND quantity)

-- 2) Pharmacy tables ------------------------------------------------------------------------------------
CREATE TABLE dbo.Pharmacies (
  pharmacy_id     BIGINT IDENTITY(1,1) CONSTRAINT PK_Pharmacies PRIMARY KEY,
  public_uuid     UNIQUEIDENTIFIER NOT NULL CONSTRAINT UQ_Pharmacies_uuid UNIQUE,
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

CREATE TABLE dbo.PharmacyVerifications (
  verification_id  UNIQUEIDENTIFIER NOT NULL CONSTRAINT PK_PharmacyVerifications PRIMARY KEY,
  pharmacy_id      BIGINT NOT NULL CONSTRAINT FK_PV_pharmacy REFERENCES dbo.Pharmacies(pharmacy_id),
  pharmacy_user_id BIGINT NOT NULL CONSTRAINT FK_PV_user REFERENCES dbo.PharmacyUsers(pharmacy_user_id),
  prescription_id  BIGINT NULL CONSTRAINT FK_PV_prescription REFERENCES dbo.Prescriptions(prescription_id),
  attempt_hash     BINARY(32) NULL,                     -- SHA-256 of submitted text, only when not found
  outcome          VARCHAR(16) NOT NULL CONSTRAINT CK_PV_outcome
                   CHECK (outcome IN ('VALID','EXPIRED','CANCELLED','FULLY_DISPENSED','NOT_FOUND','RATE_LIMITED')),
  verified_at      DATETIME2(3) NOT NULL CONSTRAINT DF_PV_at DEFAULT SYSUTCDATETIME(),
  ip_address       VARCHAR(45) NULL
);
CREATE INDEX IX_PV_pharmacy_time ON dbo.PharmacyVerifications(pharmacy_id, verified_at DESC);
CREATE INDEX IX_PV_prescription  ON dbo.PharmacyVerifications(prescription_id) WHERE prescription_id IS NOT NULL;

CREATE TABLE dbo.PrescriptionDispenses (
  dispense_id      BIGINT IDENTITY(1,1) CONSTRAINT PK_PrescriptionDispenses PRIMARY KEY,
  public_uuid      UNIQUEIDENTIFIER NOT NULL CONSTRAINT UQ_PD_uuid UNIQUE,
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
