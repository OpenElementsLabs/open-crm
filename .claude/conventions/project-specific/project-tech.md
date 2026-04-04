# Project Tech Stack

## Languages

- Java 21 (backend)
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

- PostgreSQL 17 (primary database, via Docker or standalone; uses `bytea` columns for binary image storage)
- Authentik (SSO via OpenID Connect) — production identity provider
- mock-oauth2-server (local development OIDC provider, via Docker Compose)
- Brevo API (contact/company import) — implemented

## Key Libraries

### Backend

- Spring Data JPA (ORM / repository layer)
- Flyway (database migrations, `db/migration/V*.sql`)
- SpringDoc OpenAPI 2.8.6 (Swagger UI + OpenAPI spec generation)
- Jakarta Bean Validation (request validation)
- Apache Commons CSV 1.12.0 (CSV export generation)
- Spring Multipart (file upload with 2 MB max, configured in `application.yml`)
- PostgreSQL JDBC driver (runtime)
- Spring Security OAuth2 Resource Server (JWT token validation for OIDC authentication)
- CycloneDX Maven Plugin 2.9.1 (SBOM generation)

### Backend Testing

- Spring Boot Test (integration tests with `@WebMvcTest`)
- Spring Security Test (mock JWT tokens for authenticated endpoint testing)
- H2 Database (in-memory test database)

### Frontend

- Radix UI + Base UI (accessible component primitives via shadcn/ui)
- Lucide React (icon library)
- class-variance-authority + clsx + tailwind-merge (styling utilities)
- react-day-picker + date-fns (date picker for birthday/due date fields)
- next-auth 5 / Auth.js v5 (OIDC authentication, JWT session management, token refresh)

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
- Node.js 22 Alpine (frontend Docker base image, pinned via `.nvmrc` to v22.19.0)
- Alpine 3.21 (backup container base image, with postgresql17-client and aws-cli)
- Hetzner Object Storage (S3-compatible backup target)
- GitHub Actions (CI/CD: build, test, Docker image verification)
- Coolify (deployment platform, uses Traefik reverse proxy for FQDN-based routing and TLS)