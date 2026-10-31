# Pharmacy Tables Plan (Microsoft SQL Server)

Status: PLAN (draft, not yet executed against a live SQL Server). DDL in `pharmacy-schema.sql`. Tag **INF** = inferred, confirm.

## Entity relationships
```
Users 1─0..1 PharmacyUsers *─1 Pharmacies
Pharmacies 1─* PharmacyVerifications *─0..1 Prescriptions
Prescriptions 1─* PrescriptionItems
Prescriptions 1─* PrescriptionDispenses *─1 Pharmacies
PrescriptionDispenses 1─* PrescriptionDispenseItems *─1 PrescriptionItems
PharmacyVerifications 1─* PrescriptionDispenses   (a fill must follow a verification)
```

## Tables
| Table | Purpose | Key columns |
|---|---|---|
| Pharmacies | Pharmacy master (INF: licence no.) | pharmacy_id, public_id (GUID), name, licence_number (unique), address fields, phone, email, status (PENDING/ACTIVE/SUSPENDED) |
| PharmacyUsers | Links a login to one pharmacy with a role | pharmacy_user_id, user_id (unique), pharmacy_id, role (PHARMACY_ADMIN/PHARMACIST), status |
| PharmacyVerifications | Every lookup attempt (source: PharmacyVerifications) | verification_id (GUID, used in result URL), pharmacy_id, pharmacy_user_id, prescription_id (null if not found), attempt_hash (SHA-256, only when not found), outcome, verified_at, ip_address |
| PrescriptionDispenses | One row per fill event (partial fills = several rows) | dispense_id, public_id, prescription_id, pharmacy_id, pharmacy_user_id, verification_id, dispensed_at, status (COMPLETED/VOIDED), void fields |
| PrescriptionDispenseItems | Quantity of each medicine in a fill | dispense_item_id, dispense_id, prescription_item_id, quantity_dispensed (>0) |
| (alter) PrescriptionItems | Prescribed vs dispensed counters | quantity, quantity_unit, quantity_dispensed (0..quantity), row_ver |
| (alter) Prescriptions | Unguessable code + URL id | prescription_code CHAR(32) case-sensitive unique, public_uuid |
| vw_PrescriptionFulfilment | Not / Partially / Fully Dispensed derived | per prescription |

## Rules enforced in the database
- `quantity_dispensed <= quantity` (CHECK) - over-dispensing impossible.
- `prescription_code` is exactly 32 alphanumerics, unique, **binary collation** (case-sensitive).
- One pharmacy login belongs to exactly one pharmacy (unique user_id).
- Dispense and verification rows are append-only (void, never delete).

## Dispense transaction (Java / JPA)
1. Load prescription items with `PESSIMISTIC_WRITE` (SQL Server `UPDLOCK, ROWLOCK`).
2. Re-check status Valid, not expired, not cancelled; verification recent (default 30 min).
3. For each line: `qty <= quantity - quantity_dispensed` else reject with refreshed remaining.
4. Insert `PrescriptionDispenses` + items; update `quantity_dispensed`; write audit row; commit.
5. Void: pharmacy admin + reason; decrement counters in the same transaction.

## Reports (queries over these tables)
Verifications by day/outcome/user; fills partial vs full; units dispensed; expired/cancelled attempts; per-pharmacy comparison (Super Admin).

## Open items
RC-19 pharmacy enrollment path · RC-20 continue partial fills at another pharmacy, verification window, void policy · RC-05 visible fields.
