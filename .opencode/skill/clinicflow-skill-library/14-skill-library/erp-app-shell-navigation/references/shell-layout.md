# Shell Layout (all three roles)

```
+--------------------------------------------------------------------------------+
| TOP BAR 56px  [≡] Logo  ClinicFlow        [Org/Clinic name]   [Bell*] [User ▾] |
+-----------+--------------------------------------------------------------------+
| SIDEBAR   | BREADCRUMB BAR 40px   🏠 › Appointments › APT-2026-00045            |
| 256px     +--------------------------------------------------------------------+
| (64px     | PAGE HEADER   Title  [Status badge]            [Secondary] [PRIMARY]|
| collapsed)|              subtitle / key facts                                   |
|           +--------------------------------------------------------------------+
| ▣ Dash    | CONTENT  (cards / filter bar + table / form sections / tabs)        |
| ▣ Module  |                                                                    |
| ▣ Module  |                                                                    |
|-----------+--------------------------------------------------------------------+
| Collapse  | FOOTER (optional): version · help                                  |
+-----------+--------------------------------------------------------------------+
```
*Notification bell is not in the source document; omit until confirmed.

## Rules
- Top bar: sidebar toggle, product name, role/organisation label (clinic or pharmacy name; patient shows patient name), user menu (Profile, Logout). Fixed.
- Sidebar: role-keyed list from `navigation-map.md`; icon + label; active item = filled background + 3px accent bar + `aria-current="page"`; collapsible to icons with tooltips; state remembered per user.
- Breadcrumb bar: directly under the top bar, always visible, sticky with the page header.
- Page header: H1 title (20px/600), optional status badge, subtitle, actions right-aligned (one primary).
- Content: 24px padding, max readable width for forms (720px), tables full width.
- Landmarks: `<header>`, `<nav aria-label="Main">`, `<nav aria-label="Breadcrumb">`, `<main>`.

## Responsive
| Width | Sidebar | Breadcrumb | Tables |
|---|---|---|---|
| ≥1280 | expanded | full trail | table |
| 768–1279 | collapsed (icons) | full trail, middle truncated | table, secondary columns hidden |
| <768 | off-canvas drawer via ≡ | `‹ Back` + current title | card list |

## Navigation behaviour
- Route change keeps scroll at top and moves focus to the H1.
- Returning to a list from a detail page restores filters, sort and page (store in URL query string, no PHI).
- Unsaved form + navigation -> confirm dialog "Leave without saving?".
- Session expiry -> `/login?returnTo=<same-origin path>`, restored after login.
- Wrong role -> redirect to own dashboard with a toast; unknown route -> 404 page inside the shell with a link to the role dashboard.
- Long operations show inline progress; no full-page spinners after first load.
