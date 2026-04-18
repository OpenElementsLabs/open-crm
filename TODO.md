# TODO

## URL ↔ Filter Synchronization for Contact List

The contact list should fully synchronize URL parameters with the filter UI:

- All filter values (firstName, lastName, email, companyId, language, sort) should be readable from URL parameters
- Filter changes by the user should update the URL in real-time
- This enables sharing filtered views via URL

**Context:** Deferred from spec 009 (contact-company cross-navigation). Currently, only `companyId` is read from the URL
on initial load, and the filter dropdown does not reflect the URL-driven filter value.

## H2 Tests: Switch to Flyway + validate

Switch H2-based tests from `ddl-auto: create-drop` (Flyway disabled) to Flyway-managed schema creation with
`ddl-auto: validate`. This would catch migration/entity mismatches without needing Testcontainers. Requires adding
`flyway-database-h2` as a test dependency. All 5 existing migrations are H2-compatible.

**Context:** Identified during the grill session for Spec 018. Prerequisite: Spec 018 (component tests) should be
completed first.

## Testcontainers Integration Tests

Add Testcontainers-based integration tests that run against a real PostgreSQL database via a separate Spring profile.
This catches PostgreSQL-specific issues that H2 cannot reproduce.

**Context:** Identified during the grill sessions for Spec 017 and 018. Prerequisite: H2 Flyway + validate switch should
be done first.

## Company Duplicate Merging

Provide a way to detect and merge duplicate companies. This is needed because the Brevo import creates new companies
from the `COMPANY` text field on contacts without matching against existing company names — duplicates are expected and
acceptable during import. A separate merge feature will allow cleaning these up later.

**Context:** Deferred from the Brevo import integration spec.

## Admin-View mit Bearer Token für Swagger

Eine Admin-Seite im Frontend, die den aktuellen Bearer Token (Access Token) des eingeloggten Users anzeigt. So kann man
den Token einfach kopieren und in Swagger UIs "Authorize"-Dialog einfügen, ohne die Browser-DevTools bemühen zu müssen.

**Context:** Identifiziert nach Implementierung von Specs 048/049. Swagger UI nutzt einen manuellen
Bearer-Token-Paste-Workflow.

## Comment Author: Umstellung auf User-FK

Das `author`-Feld (String) in Kommentaren soll durch einen Fremdschlüssel auf die neue User-Entity ersetzt werden. Damit
werden Kommentare einem konkreten User zugeordnet, Namensänderungen automatisch reflektiert und GDPR-Löschanträge sauber
umsetzbar.

**Context:** Identifiziert während der Grill-Session für Spec 065 (User Entity). Voraussetzung: Spec 065 muss zuerst
implementiert sein.

## Webhook Integration Tests

Add integration tests for webhook firing that use an embedded HTTP server (e.g. MockWebServer or WireMock) to verify
that webhook calls are actually sent with the correct payload, headers, and timing. Unit tests with mocked HTTP client
are part of the initial implementation — these integration tests go beyond that.

**Context:** Identified during the grill session for Spec 075 (Webhook Support). Prerequisite: Spec 075 must be
implemented first.

## Frontend: Extract Reusable Components into @open-elements/ui Package

Extract generic UI components, shared domain types, and brand styling from the CRM frontend into a reusable
`@open-elements/ui` package — mirroring what `spring-services` does for the backend.

### Approach

- **Distribution model:** Raw `.tsx` source files (no compile step in the package). The consuming app's Tailwind config
  scans the package source to discover utility classes.
- **Package boundary:** pnpm workspace with `"@open-elements/ui": "workspace:*"`. Later extraction to a separate repo
  and npm publishing.
- **Entry point:** Barrel export — `import { Combobox, TagMultiSelect, TagDto } from "@open-elements/ui"`.

### Minimal Scope (first iteration)

- **Combobox** component (generic multi-select primitive based on @base-ui/react)
- **TagMultiSelect** component (refactored: receives `getTags: () => Promise<TagDto[]>` callback instead of importing
  from `api.ts`)
- **TagDto** type definition (owned by the package, consumed by the app)
- **`cn()` utility** (clsx + tailwind-merge)
- **Brand CSS** — Open Elements brand colors (oe-green, oe-red, oe-blue, etc.), fonts (Montserrat, Lato), Tailwind
  theme tokens. Package owns the brand; app imports and can override.
- **Translation strings** — Package ships DE/EN translations for its components. App merges them into its own
  LanguageProvider (no separate provider in the package).

### Dependencies

All dependencies as `peerDependencies`: React, @base-ui/react, tailwind-merge, clsx, class-variance-authority.

### Tooling

Package has its own ESLint, Prettier, Vitest + Testing Library config. Tests live with the components in the package.

### Open

- **npm publishing strategy** — deferred until the package is extracted to its own repo. Decision needed: publish raw
  `.tsx` source (conflicts with npm convention) or compiled JS + CSS (conflicts with Tailwind class scanning).

**Context:** Identified during grill session for frontend component extraction. Mirrors the backend's `spring-services`
extraction pattern.
