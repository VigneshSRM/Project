-- Rename the inventory "unit" column to "form" (dosage form).
EXEC sp_rename 'dbo.ClinicMedicines.default_unit', 'default_form', 'COLUMN';
