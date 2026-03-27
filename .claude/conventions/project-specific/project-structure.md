# Project Structure

## Repository Layout

```
open-crm/
├── backend/                        — Java Spring Boot backend
│   ├── src/main/java/com/openelements/crm/
│   │   ├── CrmApplication.java     — Application entry point
│   │   ├── company/                — Company domain (controller, service, repository, DTOs, entity)
│   │   ├── contact/                — Contact domain (controller, service, repository, DTOs, entity, enums)
│   │   ├── comment/                — Comment domain (controller, service, repository, DTOs, entity)
│   │   └── health/                 — Health check endpoint
│   ├── src/main/resources/
│   │   ├── application.yml         — Application configuration
│   │   └── db/migration/           — Flyway SQL migrations (V1–V3)
│   ├── src/test/                   — Integration tests (one per controller)
│   ├── pom.xml                     — Maven build configuration
│   ├── Dockerfile                  — Multi-stage Docker build
│   └── .sdkmanrc                   — Java version pinning
├── frontend/                       — Next.js TypeScript frontend
│   ├── src/app/                    — Next.js App Router pages
│   │   ├── page.tsx                — Home page
│   │   ├── layout.tsx              — Root layout
│   │   ├── health/                 — Health status page
│   │   ├── companies/              — Company pages (list, detail, new, edit)
│   │   └── contacts/               — Contact pages (list, detail, new, edit)
│   ├── src/components/             — React components
│   │   ├── ui/                     — shadcn/ui primitives (button, card, table, etc.)
│   │   ├── sidebar.tsx             — Navigation sidebar
│   │   ├── company-list.tsx        — Company list with filters
│   │   ├── company-detail.tsx      — Company detail view
│   │   ├── company-form.tsx        — Company create/edit form
│   │   ├── contact-list.tsx       — Contact list with filters
│   │   ├── contact-detail.tsx     — Contact detail view
│   │   ├── contact-form.tsx       — Contact create/edit form
│   │   └── __tests__/              — Component tests
│   ├── src/lib/                    — Shared utilities
│   │   ├── api.ts                  — Backend API client functions
│   │   ├── types.ts                — TypeScript type definitions (DTOs, Page)
│   │   ├── constants.ts            — UI strings / i18n constants
│   │   └── utils.ts                — General utilities
│   ├── package.json                — Dependencies and scripts
│   ├── Dockerfile                  — Multi-stage Docker build
│   └── .nvmrc                      — Node.js version pinning
├── specs/                          — Feature specifications (spec-driven development)
│   ├── base-project-setup/         — Initial project setup spec
│   ├── company-frontend/           — Company frontend feature spec
│   ├── company-comments/           — Company comments feature spec
│   ├── 007-contact-frontend/       — Contact frontend feature spec
│   ├── core-data-model/            — Core data model spec
│   ├── dto-refactoring/            — DTO refactoring spec
│   └── frontend-i18n/              — Internationalization spec
├── .claude/                        — Claude Code configuration and conventions
├── .github/workflows/build.yml     — CI/CD pipeline
├── docker-compose.yml              — Full-stack orchestration (db, backend, frontend)
├── .env.example                    — Environment variable template
├── .editorconfig                   — Editor formatting rules
└── README.md                       — Project documentation
```

## Key Entry Points

- **Backend main:** `backend/src/main/java/com/openelements/crm/CrmApplication.java`
- **Frontend main:** `frontend/src/app/layout.tsx` (root layout) and `frontend/src/app/page.tsx` (home)
- **API client:** `frontend/src/lib/api.ts`
- **Database schema:** `backend/src/main/resources/db/migration/`

## Naming Convention

Each backend domain follows a consistent package structure: `Controller`, `Service`, `Repository`, `Entity`, `Dto`, `CreateDto`, `UpdateDto`.