-- ============================== POS pricing, sale totals and finance linkage ==============================

-- Per-pharmacy medicine price list. The POS pre-fills these prices and remembers
-- the price entered on a sale so repeat sales are fully automatic.
CREATE TABLE dbo.PharmacyMedicinePrices (
  price_id      BIGINT IDENTITY(1,1) CONSTRAINT PK_PharmacyMedicinePrices PRIMARY KEY,
  pharmacy_id   BIGINT NOT NULL CONSTRAINT FK_PMP_pharmacy REFERENCES dbo.Pharmacies(pharmacy_id),
  medicine      NVARCHAR(200) NOT NULL,
  unit_price    DECIMAL(12,2) NOT NULL CONSTRAINT DF_PMP_price DEFAULT 0
                CONSTRAINT CK_PMP_price CHECK (unit_price >= 0),
  updated_at    DATETIME2(3) NOT NULL CONSTRAINT DF_PMP_updated DEFAULT SYSUTCDATETIME(),
  CONSTRAINT UQ_PMP_pharmacy_medicine UNIQUE (pharmacy_id, medicine)
);
CREATE INDEX IX_PMP_pharmacy ON dbo.PharmacyMedicinePrices(pharmacy_id);
GO

-- A dispense is the POS sale/receipt. Monetary columns are nullable for legacy
-- dispenses created before the POS module existed.
ALTER TABLE dbo.PrescriptionDispenses ADD
  subtotal        DECIMAL(12,2) NOT NULL CONSTRAINT DF_PD_subtotal DEFAULT 0,
  discount        DECIMAL(12,2) NOT NULL CONSTRAINT DF_PD_discount DEFAULT 0,
  tax_rate        DECIMAL(5,2)  NOT NULL CONSTRAINT DF_PD_tax_rate DEFAULT 0,
  tax_amount      DECIMAL(12,2) NOT NULL CONSTRAINT DF_PD_tax_amount DEFAULT 0,
  total           DECIMAL(12,2) NOT NULL CONSTRAINT DF_PD_total DEFAULT 0,
  payment_method  NVARCHAR(20)  NULL,
  amount_tendered DECIMAL(12,2) NULL,
  change_due      DECIMAL(12,2) NULL,
  CONSTRAINT CK_PD_money CHECK (subtotal >= 0 AND discount >= 0 AND tax_rate >= 0
                                AND tax_amount >= 0 AND total >= 0);
GO

ALTER TABLE dbo.PrescriptionDispenseItems ADD
  unit_price DECIMAL(12,2) NOT NULL CONSTRAINT DF_PDI_price DEFAULT 0,
  line_total DECIMAL(12,2) NOT NULL CONSTRAINT DF_PDI_total DEFAULT 0,
  CONSTRAINT CK_PDI_money CHECK (unit_price >= 0 AND line_total >= 0);
GO

ALTER TABLE dbo.PharmacyFinanceEntries ADD
  dispense_id BIGINT NULL CONSTRAINT FK_PFE_dispense REFERENCES dbo.PrescriptionDispenses(dispense_id);
GO

CREATE INDEX IX_PFE_dispense ON dbo.PharmacyFinanceEntries(dispense_id) WHERE dispense_id IS NOT NULL;
GO

PRINT 'IMS POS pricing and sale totals created.';
GO
