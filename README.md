# Open CRM

An open-source CRM system designed for startups to manage customers, contacts, and companies with ease.

## Overview

Open CRM provides a straightforward way to organize and maintain business relationships.
It focuses on the core needs of small teams: keeping track of companies, contacts, and customer records without the complexity of enterprise CRM solutions.

## Key Features

- **Company Management** — Create and manage company records with relevant business details
- **Contact Management** — Track individual contacts and their associations with companies
- **Customer Management** — Manage customer relationships and history
- **Single Sign-On (SSO)** — Integration with [Authentik](https://goauthentik.io/) for authentication and user management
- **Brevo Sync** — Automatic synchronization of customers and companies with [Brevo](https://www.brevo.com/) for marketing and communication workflows

## Tech Stack

- **Backend:** Spring Boot (Java)
- **Frontend:** TBD
- **Database:** PostgreSQL
- **Authentication:** Authentik (SSO via OpenID Connect)
- **External Integrations:** Brevo API

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

## License

See [LICENSE](LICENSE) for details.
