# Super Admin Screens (`/admin/...`)

| Screen | Route | Breadcrumb | Pattern | Content | Primary |
|---|---|---|---|---|---|
| Dashboard | /admin/dashboard | Dashboard | D | KPIs (clinics A/P/S, patients, appointments today/month, prescriptions, verifications+fills, email failures), recent enrollments, SMTP status | Enroll Clinic |
| Clinic Enrollment | /admin/clinics | 🏠 › Clinic Enrollment | L | Name, city, status, enrolled on, admin email; filter status | Enroll Clinic |
| New clinic | /admin/clinics/new | 🏠 › Clinic Enrollment › New Clinic | F | Clinic details + first Clinic Admin | Enroll |
| Clinic detail | /admin/clinics/:id | 🏠 › Clinic Enrollment › {Clinic} | De (tabs) | Profile, Admin & invite status, Counts, Audit | Suspend/Reactivate |
| Patient Masters | /admin/patients | 🏠 › Patient Masters | L | Read-only; masked mobile/email; filter clinic/city | - |
| Patient detail | /admin/patients/:id | 🏠 › Patient Masters › {Patient} | De | Master profile, linked clinics, Reveal (audited) | - |
| Reports | /admin/reports | 🏠 › Reports | L (report cards) | List of 5 reports | - |
| Report view | /admin/reports/:key | 🏠 › Reports › {Report} | L + chart | Date range, tables/charts | Run |
| SMTP Settings | /admin/smtp | 🏠 › SMTP Settings | F | Host, port, security, username, password (write-only), from; status; Send Test | Save Settings |
