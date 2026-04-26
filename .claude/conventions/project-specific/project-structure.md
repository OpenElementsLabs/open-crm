# Project Structure

## Repository Layout

```
open-crm/
├── backend/                        — Java Spring Boot backend
│   ├── src/main/java/com/openelements/crm/
│   │   ├── CrmApplication.java     — Application entry point
│   │   ├── SecurityConfig.java     — Spring Security filter chain (JWT Resource Server)
│   │   ├── OpenApiConfig.java      — Swagger UI OIDC security scheme
│   │   ├── ImageData.java          — Shared record holding image bytes + content type
│   │   ├── company/                — Company domain (controller, service, repository, DTOs, entity, export enum)
│   │   ├── contact/                — Contact domain (controller, service, repository, DTOs, entity, enums, export enum)
│   │   ├── comment/                — Comment domain (controller, service, repository, DTOs, entity)
│   │   ├── task/                   — Task domain (controller, service, repository, DTOs, entity, status enum)
│   │   ├── tag/                    — Tag domain (entity, repository, service, controller, DTOs)
│   │   ├── user/                   — User domain (entity, repository, service, controller, DTO)
│   │   ├── apikey/                 — API key domain (entity, repository, service, controller, DTOs, auth filter)
│   │   ├── auditlog/               — Audit log REST surface (controller, project-local repository over AuditLogEntity from spring-services)
│   │   ├── webhook/                — Webhook domain (entity, repository, service, controller, DTOs, event listener, sender, event types, config)
│   │   ├── brevo/                  — Brevo integration (sync service, API client, controller, DTOs, records)
│   │   ├── health/                 — Health check endpoint
│   │   └── settings/               — Settings storage (key-value pairs, e.g. Brevo API key)
│   ├── src/main/resources/
│   │   ├── application.yml         — Application configuration
│   │   └── db/migration/           — Flyway SQL migrations (V1–V22)
│   ├── src/test/                   — Tests (repository, service, DTO conversion tests)
│   ├── pom.xml                     — Maven build configuration
│   └── Dockerfile                  — Multi-stage Docker build
├── scripts/                        — Operations scripts and backup infrastructure
│   ├── db-backup.sh                — pg_dump + gzip + S3 upload with retention cleanup
│   ├── db-restore.sh               — S3 download + database restore
│   └── Dockerfile                  — Alpine container with postgresql-client and aws-cli
├── frontend/                       — Next.js TypeScript frontend (pnpm workspace root)
│   ├── pnpm-workspace.yaml         — Workspace config (packages/*)
│   ├── packages/
│   │   └── ui/                     — @open-elements/ui package
│   │       ├── src/
│   │       │   ├── components/     — Extracted UI components (Button, Input, Textarea, InputGroup, Combobox, TagMultiSelect)
│   │       │   ├── lib/utils.ts    — cn() class merging utility
│   │       │   ├── types/index.ts  — TagDto and TagMultiSelect types
│   │       │   ├── i18n/           — DE/EN translations for package components
│   │       │   ├── styles/brand.css — Open Elements brand colors, fonts, semantic tokens
│   │       │   └── index.ts        — Barrel export (public API)
│   │       ├── package.json        — Peer dependencies, dev tooling
│   │       ├── tsconfig.json       — TypeScript config with @ui/* path alias
│   │       └── vitest.config.ts    — Test config (jsdom)
│   ├── src/auth.ts                 — Auth.js v5 OIDC configuration (provider, JWT, session callbacks, custom login page)
│   ├── src/middleware.ts            — Route protection (excludes /login, /api/auth, static assets)
│   ├── src/app/                    — Next.js App Router pages
│   │   ├── layout.tsx              — Root layout (fonts, SessionProvider, LanguageProvider — no sidebar)
│   │   ├── globals.css             — Global styles, theme, print rules
│   │   ├── login/                  — Custom branded login page (no sidebar, standalone layout)
│   │   │   └── page.tsx            — OE-branded sign-in with error display and i18n
│   │   ├── (app)/                  — Route group for authenticated pages (with sidebar)
│   │   │   ├── layout.tsx          — App layout (Sidebar, TooltipProvider, main content area)
│   │   │   ├── page.tsx            — Home page (redirects to companies)
│   │   │   ├── admin/              — Admin pages (redirect to /admin/status)
│   │   │   │   ├── status/         — Server Status page (health check)
│   │   │   │   ├── token/          — Bearer Token page (show/copy/validity)
│   │   │   │   ├── brevo/          — Brevo Integration page (settings + import)
│   │   │   │   ├── users/          — Read-only paginated list of registered users (IT-ADMIN)
│   │   │   │   └── audit-logs/     — Read-only paginated, filterable audit log table (IT-ADMIN)
│   │   │   ├── companies/          — Company pages (list, detail, new, edit, print)
│   │   │   ├── contacts/           — Contact pages (list, detail, new, edit, print)
│   │   │   ├── tasks/              — Task pages (list, detail, new, edit)
│   │   │   ├── tags/               — Tag pages (list, new, edit)
│   │   │   ├── webhooks/           — Webhook management page (list with inline actions)
│   │   │   └── api-keys/           — API key management page (list with create/delete dialogs)
│   │   └── api/                    — API routes
│   │       ├── auth/[...nextauth]/ — Auth.js route handlers
│   │       ├── logout/             — Logout handler (chunked cookie deletion, OIDC end-session)
│   │       └── [...path]/          — API proxy with JWT token forwarding
│   ├── src/components/             — React components
│   │   ├── ui/                     — shadcn/ui primitives (card, table, dialog, tooltip, etc. — Button, Input, Textarea, InputGroup, Combobox moved to @open-elements/ui)
│   │   ├── sidebar.tsx             — Navigation sidebar with branding and authenticated user section
│   │   ├── session-provider.tsx    — Auth.js SessionProvider wrapper
│   │   ├── company-list.tsx        — Company list with filters, print, CSV export
│   │   ├── company-detail.tsx      — Company detail view with merged address block
│   │   ├── company-form.tsx        — Company create/edit form
│   │   ├── company-comments.tsx    — Company comments section
│   │   ├── contact-list.tsx        — Contact list with unified search and filters
│   │   ├── contact-detail.tsx      — Contact detail view
│   │   ├── contact-form.tsx        — Contact create/edit form
│   │   ├── contact-comments.tsx    — Contact comments section
│   │   ├── task-list.tsx           — Task list with filters and pagination
│   │   ├── task-detail.tsx         — Task detail view
│   │   ├── task-form.tsx           — Task create/edit form
│   │   ├── tag-list.tsx            — Tag list with search, pagination, delete
│   │   ├── webhook-list.tsx        — Webhook list with create dialog, toggle, ping, delete
│   │   ├── api-key-list.tsx        — API key list with create/key-display/delete dialogs
│   │   ├── bearer-token-card.tsx   — Bearer token display with show/hide, copy, validity countdown
│   │   ├── tag-form.tsx            — Tag create/edit form with color picker
│   │   ├── tag-chips.tsx           — Colored tag chips for detail views
│   │   ├── (tag-multi-select.tsx)   — Moved to @open-elements/ui package
│   │   ├── detail-field.tsx        — Shared detail field with action icons (copy, link, mail, tel)
│   │   ├── csv-export-dialog.tsx   — CSV column selection dialog
│   │   ├── add-comment-dialog.tsx  — Comment creation modal
│   │   ├── delete-confirm-dialog.tsx — Hard-delete confirmation dialog
│   │   ├── brevo-sync.tsx          — Brevo import UI
│   │   ├── health-status.tsx       — Backend health indicator
│   │   ├── language-switch.tsx     — Language toggle (DE/EN)
│   │   └── __tests__/              — Component tests
│   ├── src/lib/                    — Shared utilities
│   │   ├── api.ts                  — Backend API client functions
│   │   ├── types.ts                — TypeScript type definitions (DTOs, Page)
│   │   ├── (utils.ts)              — cn() moved to @open-elements/ui package
│   │   └── i18n/                   — Internationalization
│   │       ├── de.ts               — German translations
│   │       ├── en.ts               — English translations
│   │       ├── index.ts            — Type exports
│   │       └── language-context.tsx — Language provider and hooks
│   ├── public/                     — Static assets (OE logo)
│   ├── package.json                — Dependencies and scripts
│   ├── Dockerfile                  — Multi-stage Docker build
│   └── .nvmrc                      — Node.js version pinning (v22.19.0)
├── specs/                          — Feature specifications (design docs, behavioral scenarios)
│   ├── INDEX.md                    — Central spec index with IDs, names, and status
│   └── <spec-name>/               — Individual spec folders (design.md, behaviors.md, steps.md)
├── .claude/                        — Claude Code configuration and conventions
├── .github/workflows/build.yml     — CI/CD pipeline (backend, frontend, Docker jobs)
├── docker-compose.yml              — Service definitions without port bindings (for Coolify)
├── docker-compose.override.yml     — Port bindings for local development (auto-merged)
├── mock-oauth2-config.json         — Mock OIDC server configuration for local dev
├── .env.example                    — Environment variable template
├── DOCKER-COMPOSE-COOLIFY.md       — Deployment documentation for Coolify
├── BREVO.md                        — Brevo integration documentation
├── TODO.md                         — Deferred work items
├── .editorconfig                   — Editor formatting rules
└── README.md                       — Project documentation
```

## Key Entry Points

- **Backend main:** `backend/src/main/java/com/openelements/crm/CrmApplication.java`
- **Frontend main:** `frontend/src/app/layout.tsx` (root layout), `frontend/src/app/(app)/layout.tsx` (app layout with sidebar), and `frontend/src/app/(app)/page.tsx` (home)
- **API client:** `frontend/src/lib/api.ts`
- **Database schema:** `backend/src/main/resources/db/migration/`

## Naming Conventions

Each backend domain follows a consistent package structure: `Controller`, `Service`, `Repository`, `Entity`, `Dto`, `CreateDto`, `UpdateDto`. Export functionality adds `ExportColumn` enums. Shared types like `ImageData` live in the root `crm` package.