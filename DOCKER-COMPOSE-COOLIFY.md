# Docker Compose & Coolify Deployment

## Problem

When deploying with [Coolify](https://coolify.io/), the application failed to start because host port bindings in `docker-compose.yml` caused conflicts:

```
Bind for 0.0.0.0:9081 failed: port is already allocated
```

Coolify uses Traefik as a reverse proxy to route traffic to containers via FQDNs (e.g., `crm-backend.playground.open-elements.cloud`). It does not need host port bindings — Traefik connects to the containers on the internal Docker network. The `ports` entries in `docker-compose.yml` were only needed for local development but caused port allocation conflicts in the Coolify environment.

## Solution

Port bindings are split across two files:

### `docker-compose.yml` (used by Coolify)

Contains the service definitions **without** `ports` entries. Coolify references only this file (`-f docker-compose.yml`) and relies on Traefik labels for external routing.

### `docker-compose.override.yml` (used locally)

Contains the `ports` entries for local development:

| Service  | Host Port | Container Port |
|----------|-----------|----------------|
| db       | 5432      | 5432           |
| backend  | 9081      | 8080           |
| frontend | 4001      | 3000           |

Docker Compose automatically merges `docker-compose.override.yml` when running `docker compose up` locally. Coolify does not pick up the override file because it explicitly references only `docker-compose.yml`.

## How It Works

### Local Development

```bash
docker compose up
```

Ports are available at `localhost:9081` (backend/Swagger UI), `localhost:4001` (frontend), `localhost:5432` (database).

### Coolify Deployment

Coolify deploys using only `docker-compose.yml`. Traefik routes external traffic based on the FQDNs configured in Coolify:

- Frontend: `https://crm.playground.open-elements.cloud` → frontend container port 3000
- Backend: `https://crm-backend.playground.open-elements.cloud` → backend container port 8080

Swagger UI is accessible at `https://crm-backend.playground.open-elements.cloud/swagger-ui.html`.
