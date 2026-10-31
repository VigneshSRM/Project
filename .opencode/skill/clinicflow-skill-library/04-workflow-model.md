# 04 - Workflow Model

## Workflows (source)
- **A:** Register -> Verify email -> Login -> Find clinics -> Select clinic -> Doctor -> Slot -> Confirm -> Appears in clinic dashboard. (identity, booking)
- **B:** Visit -> Doctor opens appointment -> Consultation -> Create prescription -> ID generated -> Saved -> ID emailed -> Appears in patient account. (booking, consultation, prescription, smtp)
- **C:** Pharmacist logs in -> Enters ID -> Validate -> Valid: details / Expired: EXPIRED watermark + block / Cancelled: CANCELLED watermark + block. (pharmacy)
- **D:** Patient -> My Medical History -> Visits -> Visit -> Prescription -> Medicines. (consultation-history)
- **F (new):** Super Admin enrolls clinic -> invite email -> Clinic Admin sets password -> adds doctors (+ doctor logins) -> clinic live.
- **G (new):** Pharmacist verifies (32-char ID) -> Valid -> dispense some items (Partially Dispensed) -> later fill (same or other pharmacy) -> Fully Dispensed -> further dispensing blocked.
- **E (inferred):** Account linking. Patient requests link -> proof -> clinic records attached. (identity)

## Appointment state machine
```
Booked -> Confirmed -> Checked In -> In Consultation -> Completed
Booked/Confirmed -> Cancelled      Confirmed -> No-Show
```
Actors: patient (cancel own), Clinic Admin (confirm, check in, no-show, cancel), Doctor (start consultation, complete). Terminal: Completed, Cancelled, No-Show.

## Prescription states
Validity: Valid -> Cancelled (Doctor or Clinic Admin, reason). Valid -> Expired (derived by date). Fulfilment (derived): Not Dispensed -> Partially Dispensed -> Fully Dispensed. Expired, Cancelled and Fully Dispensed all block dispensing.

## UI navigation flow (UX)
```
Public: /login <-> /register -> /verify-email -> role dashboard
Patient: Dashboard -> {Find Clinics -> Clinic -> Book -> Appointment detail}
         Dashboard -> Prescriptions -> Detail ; Dashboard -> History -> Visit -> Prescription
Clinic:  Dashboard -> Appointments -> Detail (check-in) ; Patient Masters ; Doctors (+login) ; Prescriptions (view/resend/cancel)
Doctor:  Dashboard -> My Appointments -> Detail -> Consultation -> New Prescription -> Prescription detail
Pharmacy: Dashboard -> Verify -> Result (watermark if invalid) -> Dispense dialog -> back to Verify (autofocus)
Super Admin: Dashboard -> Clinic Enrollment -> New/Detail ; Patient Masters (read-only) ; Reports ; SMTP Settings
```
Every arrow has a breadcrumb path back; creation flows end on the created record's detail page with a success toast.
