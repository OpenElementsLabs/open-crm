# Implementation Steps: Base Project Setup

## Step 1: Root Configuration Files

- [ ] Create `.editorconfig` with 4-space Java/XML, 2-space TS/JS/YAML/JSON, UTF-8, LF endings
- [ ] Update `.gitignore` to cover `.env`, `.idea/`, `target/`, `node_modules/`, `.next/`, `*.log`, `.DS_Store`, `.claude/settings.local.json`
- [ ] Create `LICENSE` with Apache License 2.0 full text
- [ ] Create `CODE_OF_CONDUCT.md` with Contributor Covenant 2.0

**Acceptance criteria:**
- [ ] All four files exist at project root
- [ ] `.gitignore` covers all required patterns

**Related behaviors:** None directly — foundational setup

---

## Step 2: Backend Project Skeleton

- [ ] Create `backend/.sdkmanrc` with `java=21`
- [ ] Create `backend/pom.xml` with Spring Boot 3.4.x parent, Java 21, all dependencies (web, data-jpa, postgresql, flyway, springdoc, test), pinned plugin versions, CycloneDX plugin
- [ ] Generate Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`)
- [ ] Create `backend/src/main/java/com/openelements/crm/CrmApplication.java` — Spring Boot main class
- [ ] Create `backend/src/main/resources/application.yml` — datasource via env vars, Flyway config, server port 8080
- [ ] Create `backend/src/main/resources/db/migration/.gitkeep`
- [ ] Create `backend/.dockerignore`

**Acceptance criteria:**
- [ ] `cd backend && ./mvnw clean verify` compiles successfully (tests may be empty at this point)
- [ ] Spring Boot application context loads (verified by default test)

**Related behaviors:** Backend builds and tests pass (partial — build only)

---

## Step 3: Health Endpoint

- [ ] Create `backend/src/main/java/com/openelements/crm/health/HealthResponse.java` — record with `String status`
- [ ] Create `backend/src/main/java/com/openelements/crm/health/HealthController.java` — `@RestController` with `GET /api/health` returning `HealthResponse("UP")`

**Acceptance criteria:**
- [ ] `cd backend && ./mvnw clean verify` succeeds
- [ ] Starting the backend locally and calling `GET /api/health` returns `{"status":"UP"}` with 200

**Related behaviors:** Returns UP status when backend is running

---

## Step 4: Backend Tests

- [ ] Create `backend/src/test/java/com/openelements/crm/health/HealthControllerTest.java` — MockMvc integration test verifying:
  - GET /api/health returns 200
  - Response body contains `{"status":"UP"}`
  - Content type is `application/json`

**Acceptance criteria:**
- [ ] `cd backend && ./mvnw clean verify` succeeds with all tests passing
- [ ] SBOM is generated in `target/`

**Related behaviors:** Returns UP status when backend is running, Swagger UI is accessible, Backend builds and tests pass

---

## Step 5: Backend Dockerfile

- [ ] Create `backend/Dockerfile` — multi-stage build: Maven build stage with `eclipse-temurin:21-alpine`, runtime stage with minimal JRE, non-root user, expose 8080

**Acceptance criteria:**
- [ ] `docker build ./backend` succeeds
- [ ] Container starts and serves `/api/health`

**Related behaviors:** Docker images build successfully (backend part)

---

## Step 6: Frontend Project Skeleton

- [ ] Create `frontend/.nvmrc` with `v22.19.0`
- [ ] Initialize Next.js 15 project with TypeScript, Tailwind CSS, App Router, pnpm
- [ ] Configure `tsconfig.json` with `strict: true`
- [ ] Configure `next.config.ts` with `output: 'standalone'` and API rewrites using `BACKEND_URL`
- [ ] Configure Tailwind with Open Elements brand colors (dark `#020144`, green `#5CBA9E`, red `#E63277`, blue `#5DB9F5`, gray `#b0aea5`, light gray `#e8e6dc`) and Montserrat/Lato typography
- [ ] Set up ESLint and Prettier
- [ ] Create `frontend/public/.gitkeep`
- [ ] Create `frontend/.dockerignore`
- [ ] Install and configure shadcn/ui

**Acceptance criteria:**
- [ ] `cd frontend && pnpm install && pnpm build` succeeds
- [ ] `pnpm dev` starts the dev server on port 3000

**Related behaviors:** Frontend builds successfully (partial — skeleton only)

---

## Step 7: Frontend Health Status Page

- [ ] Create `frontend/src/lib/constants.ts` with i18n-ready string constants (`HEALTH_STATUS_UP`, `HEALTH_STATUS_DOWN`, page title, etc.)
- [ ] Create `frontend/src/components/health-status.tsx` — client component rendering green/red dot with status text, using shadcn/ui Card
- [ ] Create `frontend/src/app/layout.tsx` — root layout with Open Elements metadata, Montserrat + Lato fonts via Google Fonts
- [ ] Create `frontend/src/app/globals.css` — Tailwind base styles with Open Elements brand CSS custom properties
- [ ] Create `frontend/src/app/page.tsx` — server component with `dynamic = 'force-dynamic'`, fetches `/api/health`, passes result to HealthStatus component
- [ ] Apply Open Elements brand guidelines: dark header color `#020144`, green `#5CBA9E` for healthy status, red `#E63277` for unhealthy, Montserrat for headings, Lato for body text
- [ ] Professional, polished UI with shadcn/ui Card, responsive layout, generous spacing

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] With backend running: page shows green indicator and "Backend is running" text
- [ ] With backend stopped: page shows red indicator and "Backend is unavailable" text
- [ ] UI is responsive and uses Open Elements brand colors/typography

**Related behaviors:** Shows green indicator when backend is healthy, Shows red indicator when backend is unavailable, Health status is checked once on page load, Frontend proxies API requests to backend, Frontend handles missing BACKEND_URL gracefully

---

## Step 8: Frontend Dockerfile

- [ ] Create `frontend/Dockerfile` — multi-stage build: pnpm install + next build stage with `node:22-alpine`, runtime stage with minimal Node image, non-root user, expose 3000

**Acceptance criteria:**
- [ ] `docker build ./frontend` succeeds
- [ ] Container starts and serves the health status page

**Related behaviors:** Docker images build successfully (frontend part)

---

## Step 9: Docker Compose & Environment

- [ ] Create `docker-compose.yml` with three services: `db` (postgres:17-alpine), `backend`, `frontend` — all config via env vars with defaults
- [ ] Create `.env.example` with DB_NAME, DB_USER, DB_PASSWORD, DB_PORT, BACKEND_PORT, FRONTEND_PORT placeholders

**Acceptance criteria:**
- [ ] Copy `.env.example` to `.env`, run `docker compose up --build` — all three services start
- [ ] Frontend accessible at `http://localhost:4001` showing green health indicator
- [ ] Backend accessible at `http://localhost:9081/api/health` returning 200
- [ ] `docker compose down` stops cleanly
- [ ] `docker compose down -v` removes volumes

**Related behaviors:** All services start successfully, Backend connects to PostgreSQL on startup, Services stop cleanly, Volume cleanup removes database data

---

## Step 10: Frontend Test Setup & Tests

- [ ] Set up Vitest (or Jest) with React Testing Library in the frontend
- [ ] Create `frontend/src/components/__tests__/health-status.test.tsx` — tests for the HealthStatus component:
  - Renders green indicator and "Backend is running" text when healthy=true
  - Renders red indicator and "Backend is unavailable" text when healthy=false

**Acceptance criteria:**
- [ ] `pnpm test` runs and all tests pass
- [ ] `pnpm build` still succeeds

**Related behaviors:** Shows green indicator when backend is healthy, Shows red indicator when backend is unavailable, Health status is checked once on page load

---

## Step 11: CI/CD Workflow

- [ ] Create `.github/workflows/build.yml` with:
  - Backend job: setup Java 21, `./mvnw clean verify`
  - Frontend job: setup Node 22, pnpm, `pnpm install --frozen-lockfile`, `pnpm test`, `pnpm build`
  - Docker job (needs backend + frontend): `docker compose build`
  - Triggers: push to `main`, PRs to `main`

**Acceptance criteria:**
- [ ] Workflow file is valid YAML
- [ ] Local verification: backend build passes, frontend build passes, docker compose build passes

**Related behaviors:** Backend builds and tests pass, Frontend builds successfully, Docker images build successfully

---

## Step 12: Update README

- [ ] Update `README.md` with:
  - Prerequisites (Java 21, Node.js 22, pnpm, Docker & Docker Compose, PostgreSQL)
  - Recommended tools (SDKMAN!, nvm)
  - How to run backend standalone (`./mvnw spring-boot:run`)
  - How to run frontend standalone (`pnpm dev` with `BACKEND_URL`)
  - How to run with Docker Compose (`docker compose up --build`)
  - Docker Compose commands (up --build, down, down -v)
  - Copy `.env.example` to `.env` instruction

**Acceptance criteria:**
- [ ] README contains all required sections
- [ ] Instructions are accurate and follow conventions

**Related behaviors:** Backend runs standalone without Docker, Frontend runs standalone without Docker

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Returns UP status when backend is running | Backend | Step 4 |
| Swagger UI is accessible | Backend | Step 4 |
| Shows green indicator when backend is healthy | Frontend | Steps 7, 10 |
| Shows red indicator when backend is unavailable | Frontend | Steps 7, 10 |
| Health status is checked once on page load | Frontend | Steps 7, 10 |
| Frontend proxies API requests to backend | Both | Steps 6 (config), 7 (usage), 9 (integration) |
| Frontend handles missing BACKEND_URL gracefully | Frontend | Steps 7, 10 |
| All services start successfully | Infrastructure | Step 9 |
| Backend connects to PostgreSQL on startup | Infrastructure | Step 9 |
| Services stop cleanly | Infrastructure | Step 9 |
| Volume cleanup removes database data | Infrastructure | Step 9 |
| Backend runs standalone without Docker | Backend | Step 12 (documented) |
| Frontend runs standalone without Docker | Frontend | Step 12 (documented) |
| Backend builds and tests pass | Backend | Steps 4, 11 |
| Frontend builds successfully | Frontend | Steps 6, 11 |
| Docker images build successfully | Infrastructure | Steps 5, 8, 11 |
