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
│  Frontend  │────▶│  Spring Boot │────▶│ PostgreSQL │
└────────────┘     │   Backend    │     └────────────┘
                   └──────┬───────┘
                          │
                ┌─────────┼─────────┐
                ▼                   ▼
         ┌────────────┐     ┌────────────┐
         │  Authentik │     │   Brevo    │
         │   (SSO)    │     │   (Sync)   │
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

## Authentication

Open CRM uses OpenID Connect (OIDC) for authentication. Locally, a [mock-oauth2-server](https://github.com/navikt/mock-oauth2-server) runs automatically via Docker Compose. For production, you connect a real [Authentik](https://goauthentik.io/) instance.

### Local Development (default)

No configuration needed — `docker compose up` starts the mock-oauth2-server automatically. The default `.env` values point to the local mock server. The mock server shows an interactive login form where you can enter any username and claims.

### Configure Authentik for Production

#### 1. Create an OAuth2/OpenID Provider in Authentik

1. In Authentik, go to **Applications → Providers → Create**
2. Select **OAuth2/OpenID Provider**
3. Configure:
   - **Name:** `Open CRM`
   - **Authorization flow:** Select your default authorization flow
   - **Client type:** Confidential
   - **Client ID:** Note this value (e.g., `open-crm`)
   - **Client Secret:** Note this value
   - **Redirect URIs:** `https://crm.example.com/api/auth/callback/oidc`
   - **Logout URI:** `https://crm.example.com` (required for logout redirect back to the app)
   - **Scopes:** `openid`, `profile`, `email`, `offline_access`
4. Save the provider

> **Important:** The `offline_access` scope is required for refresh tokens. Without it, users will be logged out when the access token expires (typically after a few minutes).

#### 2. Create an Application in Authentik

1. Go to **Applications → Applications → Create**
2. Configure:
   - **Name:** `Open CRM`
   - **Slug:** `open-crm`
   - **Provider:** Select the provider created above
3. Save the application

#### 3. Set Environment Variables

Set these variables in your deployment environment (e.g., Coolify):

```env
# Frontend OIDC configuration
OIDC_ISSUER_URI=https://auth.example.com/application/o/open-crm/
OIDC_CLIENT_ID=open-crm
OIDC_CLIENT_SECRET=your-client-secret-from-authentik
AUTH_SECRET=generate-a-random-secret-here
AUTH_URL=https://crm.example.com

# Backend JWT validation
OIDC_JWK_SET_URI=https://auth.example.com/application/o/open-crm/jwks/
```

- `OIDC_ISSUER_URI` — The Authentik OpenID issuer URL for your application
- `OIDC_CLIENT_ID` / `OIDC_CLIENT_SECRET` — From the Authentik provider configuration
- `AUTH_SECRET` — A random string for encrypting session cookies (generate with `openssl rand -base64 32`)
- `AUTH_URL` — The public URL of your frontend
- `OIDC_JWK_SET_URI` — The JWKS endpoint for JWT signature validation (usually `{issuer}/jwks/`)

#### 4. Verify

After deploying with the new variables:
1. Open the frontend URL — you should be redirected to the Authentik login page
2. Log in with an Authentik user
3. You should be redirected back to the CRM with your name displayed in the sidebar

## License

See [LICENSE](LICENSE) for details.
