-- ClinicFlow migration V004: staff invitations, verified email change and merge marker.
--
-- Staff accounts are created in PENDING state by a Super Admin (clinic/pharmacy) or
-- by a clinic administrator (doctor). AuthTokens carries the single-use invitation
-- token, so no invite credential is stored on Users or duplicated in other tables.
--
-- Email changes are verified before they become the login address. The requested
-- address is kept in Users.pending_email until the token is consumed.
--
-- Merged patient rows are retained for audit history. merged_into_patient_id lets
-- search and duplicate checks hide the dormant row without deleting evidence.

SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

ALTER TABLE dbo.Users ADD pending_email NVARCHAR(254) NULL;
GO

ALTER TABLE dbo.Patients ADD merged_into_patient_id BIGINT NULL;
GO

ALTER TABLE dbo.Patients
  ADD CONSTRAINT FK_Patients_merged_into
  FOREIGN KEY (merged_into_patient_id) REFERENCES dbo.Patients(patient_id);
GO

CREATE INDEX IX_Patients_merged_into
  ON dbo.Patients(merged_into_patient_id)
  WHERE merged_into_patient_id IS NOT NULL;
GO

-- Add the invitation and verified-email-change purposes to the existing check.
ALTER TABLE dbo.AuthTokens DROP CONSTRAINT CK_AT_purpose;
GO

ALTER TABLE dbo.AuthTokens
  ADD CONSTRAINT CK_AT_purpose
  CHECK (purpose IN ('VERIFY_EMAIL','RESET_PASSWORD','STAFF_INVITE','EMAIL_CHANGE'));
GO
