# Navigation Map (sidebar + routes + breadcrumbs)

Icon names are suggestions from a generic outline icon set; swap for the chosen stack.

## Public (no shell)
`/login` · `/register` · `/verify-email` · `/forgot-password` · `/reset-password` (centered card layout, product logo, links between them)

## Super Admin (`/admin/...`) - confirmed scope
| Sidebar item (icon) | Route | Sub-routes |
|---|---|---|
| Dashboard (layout-dashboard) | /admin/dashboard | - |
| Clinic Enrollment (building-2) | /admin/clinics | /new · /:clinicId |
| Patient Masters (users) | /admin/patients | /:patientId (read-only) |
| Reports (bar-chart-3) | /admin/reports | /:reportKey |
| SMTP Settings (mail-cog) | /admin/smtp | - |

## Doctor (`/doctor/...`) - own login; module list INFERRED, confirm
| Sidebar item | Route | Sub-routes |
|---|---|---|
| Dashboard (layout-dashboard) | /doctor/dashboard | - |
| My Appointments (calendar-check) | /doctor/appointments | /:appointmentId · /:appointmentId/consultation |
| Prescriptions (file-text) | /doctor/prescriptions | /new?consultationId= · /:prescriptionUuid |

## Patient (`/patient/...`)
| Sidebar item (icon) | Route | Sub-routes (breadcrumb only) |
|---|---|---|
| Dashboard (layout-dashboard) | /patient/dashboard | - |
| My Profile (user) | /patient/profile | /patient/profile/link-records |
| Find Nearby Clinics (map-pin) | /patient/clinics | /:clinicId · /:clinicId/book |
| Appointments (calendar) | /patient/appointments | /:appointmentId · /:appointmentId/reschedule |
| My Prescriptions (file-text) | /patient/prescriptions | /:prescriptionUuid |
| My Medical History (history) | /patient/history | /:visitId |

## Clinic (`/clinic/...`)
| Sidebar item | Route | Sub-routes |
|---|---|---|
| Dashboard (layout-dashboard) | /clinic/dashboard | - |
| Patient Masters (users) | /clinic/patients | /new · /:patientId · /:patientId/edit |
| Appointments (calendar-check) | /clinic/appointments | /:appointmentId (check-in, confirm, no-show) |
| Doctors (stethoscope) | /clinic/doctors | /new · /:doctorId · /:doctorId/schedule |
| Prescriptions (file-text) | /clinic/prescriptions | /:prescriptionUuid (view, resend email, cancel) |
Clinic profile (address, coordinates, hours) lives in the user menu > "Clinic Settings" (INFERRED; not a source module).
Consultation has no sidebar item in the source; the Doctor opens it from Appointment detail. Clinic Admin does not create consultations or prescriptions.

## Pharmacy (`/pharmacy/...`)
| Sidebar item | Route | Sub-routes |
|---|---|---|
| Dashboard (layout-dashboard) | /pharmacy/dashboard | - |
| POS / Prescription Verification (scan-search) | /pharmacy/verify | /result/:verificationId (never the Prescription ID) |
| Users (users) | /pharmacy/users | /new · /:userId |
| Reports (bar-chart-3) | /pharmacy/reports | /:reportKey |

## Sidebar rules
- Items appear only if the role may use them (also enforced on the server).
- Active by route prefix; child routes keep the parent active.
- Order and labels are exactly as the source "Final Application Navigation".
- Section labels are optional; with ≤6 items, use no grouping.
