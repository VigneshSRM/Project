-- ============================== ClinicMedicines ==============================
CREATE TABLE dbo.ClinicMedicines (
  clinic_medicine_id BIGINT IDENTITY(1,1) CONSTRAINT PK_ClinicMedicines PRIMARY KEY,
  clinic_id          BIGINT NOT NULL,
  medicine           NVARCHAR(200) NOT NULL,
  dosage             NVARCHAR(100) NULL,
  default_frequency  NVARCHAR(100) NULL,
  default_duration   NVARCHAR(100) NULL,
  default_quantity   INT NULL,
  default_unit       NVARCHAR(50) NULL,
  default_instructions NVARCHAR(500) NULL,
  created_at         DATETIME2(3) NOT NULL,
  CONSTRAINT FK_CM_clinic FOREIGN KEY (clinic_id) REFERENCES dbo.Clinics(clinic_id),
  CONSTRAINT UQ_CM_clinic_medicine_dosage UNIQUE (clinic_id, medicine, dosage)
);

CREATE INDEX IX_CM_clinic_medicine ON dbo.ClinicMedicines(clinic_id, medicine);
