# Breadcrumb Specification

Interpretation: the user asked for "breadcrumbs" (written "bar grimps"); this spec implements standard hierarchical breadcrumbs.

## Format
`🏠 Home › Module › [Sub-list] › Record`
- Level 0 is a home icon (accessible name "Home") linking to the role dashboard. On the dashboard itself the trail is just `Dashboard` (no link, no icon).
- Each crumb except the last is a link. The last crumb is plain text with `aria-current="page"` and equals the page title.
- Separator is `›` (decorative, `aria-hidden`).
- Use friendly names where known (clinic name, doctor name); fall back to the public ID (`APT-2026-00045`, an opaque UUID, never the Prescription ID; prescriptions show the last 4 characters of the ID). Never put PHI in the URL.
- Max label length 24 chars with ellipsis + full text in tooltip.
- If trail length > 4 at 768–1279 px: collapse middle items into `…` menu. Under 768 px: show `‹ Parent` and current title only.
- Breadcrumbs show hierarchy, not browsing history.
- Wizard steps are not crumbs; the stepper shows progress.

## Definition (page declares, shell renders)
```
breadcrumbs: [
  { label: "Appointments", to: "/patient/appointments" },
  { label: "{appointment.publicId}" }   // last = current page
]
```
CI rule: a route without breadcrumb metadata fails the build.

## Examples
| Route | Breadcrumb |
|---|---|
| /patient/dashboard | Dashboard |
| /patient/clinics | 🏠 › Find Nearby Clinics |
| /patient/clinics/:id/book | 🏠 › Find Nearby Clinics › {Clinic name} › Book Appointment |
| /clinic/patients/:id/edit | 🏠 › Patient Masters › {Patient name} › Edit |
| /doctor/appointments/:id/consultation | 🏠 › My Appointments › {APT ID} › Consultation |
| /doctor/prescriptions/:uuid | 🏠 › Prescriptions › Rx …9aQz (last 4 of the ID) |
| /admin/clinics/:id | 🏠 › Clinic Enrollment › {Clinic name} |
| /pharmacy/verify/result/:verificationId | 🏠 › POS / Prescription Verification › Result |
