# Tokens and Components

Palette character: calm corporate blue/navy with teal accent, neutral greys, restrained use of colour. Verify every text/background pair reaches WCAG AA (4.5:1) with a contrast checker before release.

## Status badge map (colour + icon + text, always)
| Entity | Status | Tone | Icon |
|---|---|---|---|
| Appointment | Booked | info | calendar |
| | Confirmed | primary | check-circle |
| | Checked In | accent/teal | log-in |
| | In Consultation | warning | stethoscope |
| | Completed | success | check |
| | Cancelled | neutral (strike not required) | x-circle |
| | No-Show | danger | user-x |
| Prescription | Valid | success | shield-check |
| | Expired | warning | clock |
| | Cancelled | danger | ban |
| Account | Pending Verification / Active / Suspended / Deactivated | warning / success / danger / neutral | - |
| Email | Queued / Sent / Failed | info / success / danger | - |

## Components and rules
- **Button:** primary (filled), secondary (outline), tertiary (text), danger. 36px height (32 compact). Loading state disables and shows spinner. One primary per header.
- **Input/Select/DatePicker:** label above, required `*`, helper text, inline error under field (text + icon), 40px height.
- **DataTable:** sticky header, sortable columns with aria-sort, 40px rows (32 compact), zebra optional, row hover, row click opens detail, pagination 20/page, toolbar (search, filters, column toggle optional), empty state, mobile -> cards.
- **Card:** surface, 1px border, radius 6, header + body; KPI card = number, label, delta/link.
- **Tabs:** underline style; URL-synced.
- **Dialog:** confirm dialogs name the record ("Cancel appointment APT-2026-00045?"), destructive button on the right in danger tone, focus trapped, Esc closes.
- **Toast:** top-right, 4s success, sticky errors, `role="status"`.
- **Stepper:** numbered, current/complete/upcoming, keyboard accessible.
- **Skeleton / EmptyState / ErrorState:** each with explanation and next action.
- **Watermark overlay:** see `erp-screen-blueprints/references/screen-specs.md`.

## Accessibility baseline
WCAG 2.1 AA: contrast, visible focus, labels for inputs, error association (`aria-describedby`), 44px minimum touch target on mobile, no colour-only meaning, reduced-motion respected, page language set.

## Density
Clinic and pharmacy default **compact**; patient default **comfortable**. Toggle is optional.
