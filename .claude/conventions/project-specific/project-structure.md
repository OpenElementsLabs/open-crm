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
│   │   ├── brevo/                  — Brevo integration (sync service, controller, DTOs, records)
│   │   ├── health/                 — Health check endpoint
│   │   ├── settings/               — Settings storage (Brevo API key)
│   │   └── user/                   — User model (UserInfo record, UserService with JWT extraction)
│   ├── src/main/resources/
│   │   ├── application.yml         — Application configuration
│   │   └── db/migration/           — Flyway SQL migrations (V1–V11)
│   ├── src/test/                   — Tests (repository, service, DTO conversion tests)
│   ├── pom.xml                     — Maven build configuration
│   └── Dockerfile                  — Multi-stage Docker build
├── frontend/                       — Next.js TypeScript frontend
│   ├── src/auth.ts                 — Auth.js v5 OIDC configuration (provider, JWT, session callbacks)
│   ├── src/middleware.ts            — Route protection (redirects unauthenticated users)
│   ├── src/app/                    — Next.js App Router pages
│   │   ├── page.tsx                — Home page (redirects to companies)
│   │   ├── layout.tsx              — Root layout (fonts, sidebar, language provider)
│   │   ├── globals.css             — Global styles, theme, print rules
│   │   ├── health/                 — Health status page
│   │   ├── brevo-sync/             — Brevo import page
│   │   ├── companies/              — Company pages (list, detail, new, edit, print)
│   │   ├── contacts/               — Contact pages (list, detail, new, edit, print)
│   │   └── api/                    — API routes
│   │       ├── auth/[...nextauth]/ — Auth.js route handlers
│   │       └── [...path]/          — API proxy with JWT token forwarding
│   ├── src/components/             — React components
│   │   ├── ui/                     — shadcn/ui primitives (button, card, table, dialog, etc.)
│   │   ├── sidebar.tsx             — Navigation sidebar with branding and authenticated user section
│   │   ├── session-provider.tsx    — Auth.js SessionProvider wrapper
│   │   ├── detail-field.tsx        — Shared detail field with action icons (copy, link, mail, tel)
│   │   ├── company-list.tsx        — Company list with filters, print, CSV export
│   │   ├── company-detail.tsx      — Company detail view with merged address block
│   │   ├── company-form.tsx        — Company create/edit form
│   │   ├── contact-list.tsx        — Contact list with unified search and filters
│   │   ├── contact-detail.tsx      — Contact detail view
│   │   ├── contact-form.tsx        — Contact create/edit form
│   │   ├── csv-export-dialog.tsx   — CSV column selection dialog
│   │   ├── add-comment-dialog.tsx  — Comment creation modal
│   │   └── __tests__/              — Component tests
│   ├── src/lib/                    — Shared utilities
│   │   ├── api.ts                  — Backend API client functions
│   │   ├── types.ts                — TypeScript type definitions (DTOs, Page)
│   │   ├── constants.ts            — Shared constants
│   │   ├── utils.ts                — General utilities (cn helper)
│   │   └── i18n/                   — Internationalization
│   │       ├── de.ts               — German translations
│   │       ├── en.ts               — English translations
│   │       ├── index.ts            — Type exports
│   │       └── language-context.tsx — Language provider and hooks
│   ├── public/                     — Static assets (OE logo)
│   ├── package.json                — Dependencies and scripts
│   ├── Dockerfile                  — Multi-stage Docker build
│   └── .nvmrc                      — Node.js version pinning (v22.19.0)
├── specs/                          — Feature specifications (46 completed, see INDEX.md)
│   ├── INDEX.md                    — Central spec index with IDs, names, and status
│   └── <spec-name>/               — Individual spec folders (design.md, behaviors.md)
├── .claude/                        — Claude Code configuration and conventions
├── .github/workflows/build.yml     — CI/CD pipeline (backend, frontend, Docker jobs)
├── docker-compose.yml              — Service definitions without port bindings (for Coolify)
├── docker-compose.override.yml     — Port bindings for local development (auto-merged)
├── .env.example                    — Environment variable template
├── DOCKER-COMPOSE-COOLIFY.md       — Deployment documentation for Coolify
├── TODO.md                         — Deferred work items
├── .editorconfig                   — Editor formatting rules
└── README.md                       — Project documentation
```

## Key Entry Points

- **Backend main:** `backend/src/main/java/com/openelements/crm/CrmApplication.java`
- **Frontend main:** `frontend/src/app/layout.tsx` (root layout) and `frontend/src/app/page.tsx` (home)
- **API client:** `frontend/src/lib/api.ts`
- **Database schema:** `backend/src/main/resources/db/migration/`

## Naming Conventions

Each backend domain follows a consistent package structure: `Controller`, `Service`, `Repository`, `Entity`, `Dto`, `CreateDto`, `UpdateDto`. Export functionality adds `ExportColumn` enums. Shared types like `ImageData` and `UserInfo` live in the root `crm` package or dedicated sub-packages.
