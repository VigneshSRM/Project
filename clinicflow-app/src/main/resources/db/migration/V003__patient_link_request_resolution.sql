-- ClinicFlow migration V003: patient link request resolution.
--
-- The inferred PatientLinkRequests table records that an account asked to be linked
-- to a clinic. Clinic staff approval needs three extra pieces of evidence:
--   * which clinic patient record was confirmed,
--   * which clinic user resolved the request,
--   * the resolution note shown to the patient and kept in the audit trail.
--
-- The account-side record remains Patients.patient_id. clinic_patient_record_id is
-- nullable so the column can also record a merge into an existing clinic-entered
-- patient row; on approval the account links and history move to that row.

SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

ALTER TABLE dbo.PatientLinkRequests ADD clinic_patient_record_id BIGINT NULL;
ALTER TABLE dbo.PatientLinkRequests ADD resolved_by BIGINT NULL;
ALTER TABLE dbo.PatientLinkRequests ADD resolution_note NVARCHAR(500) NULL;
GO

ALTER TABLE dbo.PatientLinkRequests
  ADD CONSTRAINT FK_PLR_clinic_patient
  FOREIGN KEY (clinic_patient_record_id) REFERENCES dbo.Patients(patient_id);
GO

ALTER TABLE dbo.PatientLinkRequests
  ADD CONSTRAINT FK_PLR_resolved_by
  FOREIGN KEY (resolved_by) REFERENCES dbo.Users(user_id);
GO

CREATE INDEX IX_PLR_clinic_status
  ON dbo.PatientLinkRequests(clinic_id, status, created_at DESC);
GO

CREATE INDEX IX_PLR_patient_status
  ON dbo.PatientLinkRequests(patient_id, status);
GO
