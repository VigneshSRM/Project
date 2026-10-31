-- Add a lifetime/chronic medication flag to prescription items.
ALTER TABLE dbo.PrescriptionItems
ADD lifetime BIT NOT NULL CONSTRAINT DF_PrescriptionItems_lifetime DEFAULT 0;
