# Screen Specs

Patterns: **D**ashboard · **L**ist · **De**tail · **F**orm · **W**izard · **V**erification. All states required: loading (skeleton), empty, error (retry), no-permission.

Admin screens: see `platform-admin-console/references/admin-screens.md`.

## Patient
| Screen | Route | Pattern | Content | Primary action |
|---|---|---|---|---|
| Dashboard | /patient/dashboard | D | KPIs: Upcoming, Completed, Cancelled, Prescriptions (Total Appointments as subtitle on the Appointments card). Lists: Upcoming Appointments (ID, clinic, doctor, date, time, status), Recent Prescriptions (ID, clinic, doctor, date, expiry, status, View). Recent Clinic Visits. Quick actions: Find Clinics, Book, Prescriptions, History, Profile | Book Appointment |
| My Profile | /patient/profile | F (sections) | Personal, Address, Optional Medical, account status; edit email/mobile triggers re-verify; link-records panel | Save Changes |
| Find Nearby Clinics | /patient/clinics | L (cards) | Search (name/city/area/postal), "Use my location" button, cards with name, address, distance, contact, specializations, doctors, fees, next available | Book Appointment (per card) |
| Clinic details | /patient/clinics/:id | De | Address, contact, opening hours, doctors + specialization, fees, availability | Book Appointment |
| Book Appointment | /patient/clinics/:id/book | W | 1 Doctor 2 Date & slot 3 Reason 4 Confirm summary | Confirm Booking |
| Appointments | /patient/appointments | L | Tabs Upcoming / Past / Cancelled; columns ID, clinic, doctor, date, time, status | Book Appointment |
| Appointment detail | .../:id | De | Facts, status timeline, Cancel / Reschedule (rules) | Reschedule |
| My Prescriptions | /patient/prescriptions | L | Short ID (first4…last4), clinic, doctor, issue, expiry, validity badge + fulfilment badge (Not/Partially/Fully Dispensed), View | - |
| Prescription detail | /patient/prescriptions/:uuid | De | Status banner, **full 32-char ID in 4 groups + Copy**, medicines table (name, dosage, frequency, duration, prescribed / dispensed / remaining), instructions, fill history (pharmacy, date, items), Print/Download | Download |
| My Medical History | /patient/history | Timeline | Newest first, date headers; card per visit: clinic, doctor, consultation, prescription ID + status; filters | - |

## Clinic
| Screen | Route | Pattern | Content | Primary action |
|---|---|---|---|---|
| Dashboard | /clinic/dashboard | D | Today's appointments, status counts, check-in queue, prescriptions issued (widgets to confirm) | New Appointment (INFERRED) |
| Patient Masters | /clinic/patients | L | Search name/mobile/ID; columns ID, name, age/gender, mobile, last visit | Add Patient |
| Patient detail | .../:id | De (tabs) | Profile · Appointments · Prescriptions · Linking status | Edit |
| Appointments | /clinic/appointments | L | Filters date, doctor, status; inline status actions (Confirm, Check In, No-Show) | - |
| Appointment detail | .../:id | De | Patient, doctor, reason, status timeline; front-desk actions | Check In |
| Doctors | /clinic/doctors | L | Name, specialization, status, fee | Add Doctor |
| Doctor schedule | .../:id/schedule | F (weekly grid) | Working hours, leave dates, conflict list | Save Schedule |
| Prescriptions | /clinic/prescriptions | L | Short ID, patient, doctor, issue, expiry, validity + fulfilment status | - |
| Prescription detail | .../:uuid | De | Header, items with dispensed/remaining, fill history, email log, Resend, Cancel (confirm + reason) | Print |

## Doctor (own login)
| Screen | Route | Pattern | Content | Primary action |
|---|---|---|---|---|
| Dashboard | /doctor/dashboard | D | Today's queue (checked-in first), counts, recent prescriptions | Open Next Patient |
| My Appointments | /doctor/appointments | L | Date, patient, reason, status | - |
| Appointment detail | .../:id | De | Patient summary, history link, reason | Start Consultation |
| Consultation | .../:id/consultation | F | Diagnosis, notes, follow-up; Finalise | Finalise & Create Prescription |
| New prescription | /doctor/prescriptions/new | F (repeatable rows) | Header, medicine rows (name, dosage, frequency, duration, quantity+unit, instructions), validity | Issue Prescription |
| Prescription detail | .../:uuid | De | As clinic detail | Print |

## Pharmacy
| Screen | Route | Pattern | Content | Primary action |
|---|---|---|---|---|
| Dashboard | /pharmacy/dashboard | D | Today's verifications, outcome counts, recent lookups | Verify Prescription |
| Verification / POS | /pharmacy/verify | V | One large 32-character ID field (autofocus, paste friendly, accepts spaces/hyphens, case-sensitive), Verify | Verify |
| Verification result | /pharmacy/verify/result/:verificationId | V | Status banner; card with patient, doctor, clinic, medicines with Prescribed / Dispensed so far / Remaining; fill history; **watermark EXPIRED / CANCELLED / FULLY DISPENSED over card**; Dispense disabled with reason when not allowed | Dispense |
| Dispense dialog | (modal on result) | Dialog | Per item: remaining, "Dispense now" (1..remaining), summary, confirm | Confirm Dispense |
| Users | /pharmacy/users | L + F | Name, role, status, last login | Add User |
| Reports | /pharmacy/reports | L + chart | Verifications by day/outcome/user, date range, export (approval for sensitive export) | Run Report |
Partial fills are confirmed; continuation at another pharmacy and the verification window are defaults (RC-20).

## Watermark spec
Overlay layer on the detail card: text rotated -20°, 96px bold, 12% opacity, `pointer-events:none`; paired with a status banner and `role="status"` text; printed output keeps the watermark.
