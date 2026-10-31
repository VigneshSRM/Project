-- ============================== PharmacyFinanceEntries ==============================
CREATE TABLE dbo.PharmacyFinanceEntries (
  entry_id     BIGINT IDENTITY(1,1) CONSTRAINT PK_PharmacyFinanceEntries PRIMARY KEY,
  pharmacy_id  BIGINT NOT NULL,
  entry_type   NVARCHAR(10) NOT NULL,
  amount       DECIMAL(18,2) NOT NULL,
  description  NVARCHAR(500) NULL,
  entry_date   DATE NOT NULL,
  created_by   BIGINT NULL,
  created_at   DATETIME2(3) NOT NULL,
  CONSTRAINT FK_PFE_pharmacy FOREIGN KEY (pharmacy_id) REFERENCES dbo.Pharmacies(pharmacy_id),
  CONSTRAINT CK_PFE_type CHECK (entry_type IN ('INCOME', 'EXPENSE'))
);

CREATE INDEX IX_PFE_pharmacy_date ON dbo.PharmacyFinanceEntries(pharmacy_id, entry_date);
