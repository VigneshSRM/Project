# Java / Thymeleaf Mapping (default; RC-18)

- `layout/shell.html` fragment: top bar, sidebar (`th:each` over `navItems(role)`), breadcrumb bar (`th:each` over `page.breadcrumbs`), page header (`page.title`, `page.primaryAction`), `<main layout:fragment="content">`.
- `PageMeta` (title, breadcrumbs[], primaryAction, activeNav) is added to the model by each controller via a helper; a `HandlerInterceptor` fails fast (dev/test) if a view is rendered without `PageMeta`.
- `NavigationConfig` bean: role -> items (label, icon, route prefix) exactly as `navigation-map.md`; filtered by `GrantedAuthority`.
- Active item: `request.requestURI.startsWith(item.prefix)`.
- Breadcrumb names resolved in services (clinic name, doctor name); fallback to public id.
- Return URL after login: validated same-origin.
- Route/breadcrumb test: MockMvc iterates the route table and asserts title, crumbs and active item.
- If React is chosen instead: same metadata as route `handle`, same tests via Playwright.
