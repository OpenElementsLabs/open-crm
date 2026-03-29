# Open CRM

An open-source CRM system designed for startups to manage customers, contacts, and companies with ease.

**Status:** Early development

## Overview

Open CRM provides a straightforward way to organize and maintain business relationships.
It focuses on the core needs of small teams: keeping track of companies, contacts, and customer records without the complexity of enterprise CRM solutions.

## Key Features

- **Company Management** — Create and manage company records with relevant business details
- **Contact Management** — Track individual contacts and their associations with companies
- **Image Upload** — Upload and manage company logos (SVG, PNG, JPEG) and contact photos (JPEG) with thumbnails in list views
- **Customer Management** — Manage customer relationships and history
- **Single Sign-On (SSO)** — Integration with [Authentik](https://goauthentik.io/) for authentication and user management
- **Brevo Sync** — Automatic synchronization of customers and companies with [Brevo](https://www.brevo.com/) for marketing and communication workflows

## Tech Stack

- **Backend:** Spring Boot 3.4 (Java 21)
- **Frontend:** Next.js 15 (React 19, TypeScript, Tailwind CSS, shadcn/ui)
- **Database:** PostgreSQL 17
- **Authentication:** Authentik (SSO via OpenID Connect) — planned
- **External Integrations:** Brevo API — planned

## Architecture

```
┌────────────┐     ┌──────────────┐     ┌────────────┐
│  Frontend   │────▶│  Spring Boot  │────▶│ PostgreSQL │
└────────────┘     │   Backend     │     └────────────┘
                   └──────┬───────┘
                          │
                ┌─────────┼─────────┐
                ▼                   ▼
         ┌────────────┐     ┌────────────┐
         │  Authentik  │     │   Brevo    │
         │   (SSO)     │     │   (Sync)   │
         └────────────┘     └────────────┘
```

## Prerequisites

- **Java 21** — recommended via [SDKMAN!](https://sdkman.io/) (see `backend/.sdkmanrc`)
- **Node.js 22** — recommended via [nvm](https://github.com/nvm-sh/nvm) (see `frontend/.nvmrc`)
- **pnpm** — package manager for the frontend
- **Docker & Docker Compose** — for running the full stack
- **PostgreSQL 17** — for standalone backend development (or use Docker)

## Getting Started

### Setup

1. Clone the repository
2. Copy the environment file:
   ```bash
   cp .env.example .env
   ```
3. Adjust values in `.env` if needed (defaults work for local development)

### Run with Docker Compose (Local Development)

Start all services (PostgreSQL, backend, frontend):

```bash
docker compose up --build
```

- Frontend: [http://localhost:4001](http://localhost:4001)
- Backend API: [http://localhost:9081/api/health](http://localhost:9081/api/health)
- Swagger UI: [http://localhost:9081/swagger-ui.html](http://localhost:9081/swagger-ui.html)

Port bindings are defined in `docker-compose.override.yml`, which Docker Compose merges automatically. The main `docker-compose.yml` contains no `ports` entries — see [Docker Compose & Coolify](#docker-compose--coolify) for details.

Stop services:

```bash
docker compose down
```

Stop services and remove database data:

```bash
docker compose down -v
```

### Run Standalone (without Docker)

**Backend:**

```bash
cd backend
sdk env install          # install Java 21 via SDKMAN!
./mvnw spring-boot:run   # starts on port 8080
```

Requires a running PostgreSQL instance. Configure connection via environment variables or `application.yml`.

**Frontend:**

```bash
cd frontend
nvm install              # install Node.js 22 via nvm
pnpm install
BACKEND_URL=http://localhost:8080 pnpm dev   # starts on port 3000
```

### Build

**Backend:**

```bash
cd backend && ./mvnw clean verify
```

**Frontend:**

```bash
cd frontend && pnpm install && pnpm test && pnpm build
```

## Docker Compose & Coolify

The project uses a split Docker Compose setup to support both local development and [Coolify](https://coolify.io/) deployment:

| File | Purpose |
|------|---------|
| `docker-compose.yml` | Service definitions without port bindings (used by Coolify) |
| `docker-compose.override.yml` | Host port bindings for local development (merged automatically) |

This split is necessary because Coolify uses Traefik as a reverse proxy to route traffic via FQDNs. Host port bindings are not needed in Coolify and cause port allocation conflicts during deployment.

For more details, see [DOCKER-COMPOSE-COOLIFY.md](DOCKER-COMPOSE-COOLIFY.md).

### Deploy to Coolify

1. In Coolify, create a new application and connect the GitHub repository
2. Select **Docker Compose** as the build method
3. Set the branch to `main`
4. Configure the FQDNs for the services in Coolify:
   - **frontend** — e.g., `crm.example.com`
   - **backend** — e.g., `crm-backend.example.com`
5. Add the required environment variables in Coolify:
   - `DB_NAME` — PostgreSQL database name (e.g., `opencrm`)
   - `DB_USER` — PostgreSQL user (e.g., `opencrm`)
   - `DB_PASSWORD` — PostgreSQL password
6. Deploy — Coolify builds the Docker images and starts the services
7. Traefik automatically handles TLS certificates and routes traffic to the containers

After deployment:
- Frontend: `https://crm.example.com`
- Backend / Swagger UI: `https://crm-backend.example.com/swagger-ui.html`

## License

See [LICENSE](LICENSE) for details.
