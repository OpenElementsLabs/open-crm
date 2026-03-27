# Project Tech Stack

## Languages

- Java 21 (via SDKMAN!, see `backend/.sdkmanrc`)
- TypeScript 5.8 (frontend)

## Frameworks

- Spring Boot 3.4.7 (backend)
- Next.js 15.3 (frontend, with App Router)
- React 19.1 (frontend)
- Tailwind CSS 4.1 (frontend styling)

## Build Tools & Package Managers

- Maven with Maven Wrapper (`./mvnw`) — backend
- pnpm — frontend package manager

## Databases & External Services

- PostgreSQL 17 (primary database, via Docker or standalone)
- Authentik (SSO via OpenID Connect) — planned
- Brevo API (contact/company sync) — planned

## Key Libraries

### Backend

- Spring Data JPA (ORM / repository layer)
- Flyway (database migrations, `db/migration/V*.sql`)
- SpringDoc OpenAPI 2.8.6 (Swagger UI + OpenAPI spec generation)
- Jakarta Bean Validation (request validation)
- PostgreSQL JDBC driver (runtime)
- CycloneDX Maven Plugin 2.9.1 (SBOM generation)

### Backend Testing

- Spring Boot Test (integration tests with `@WebMvcTest`)
- H2 Database (in-memory test database)

### Frontend

- Radix UI (accessible component primitives via shadcn/ui)
- Lucide React (icon library)
- class-variance-authority + clsx + tailwind-merge (styling utilities)

### Frontend Testing

- Vitest 4.1 (test runner)
- Testing Library (React + jest-dom)
- jsdom (DOM environment for tests)

### Frontend Tooling

- ESLint 9 + eslint-config-next (linting)
- Prettier 3.5 + prettier-plugin-tailwindcss (formatting)
- PostCSS 8.5 (CSS processing)

## Infrastructure

- Docker & Docker Compose (multi-service orchestration)
- Eclipse Temurin 21 (backend Docker base image)
- Node.js 22 Alpine (frontend Docker base image)
- GitHub Actions (CI/CD: build, test, Docker image verification)