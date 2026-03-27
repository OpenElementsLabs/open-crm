# Project Features

Open CRM is an open-source CRM system designed for startups and small teams to manage business relationships. It focuses on core CRM needs — companies, contacts, and communication history — without the complexity of enterprise solutions. The project is in early development.

## Core Features

- **Company Management** — Create, view, edit, and soft-delete companies with address and contact details. Supports filtering by name/city/country, sorting, pagination, and restoring archived companies.
- **Contact Management** — Create, view, edit, and hard-delete contacts with name, email, phone, position, gender, language, and LinkedIn URL. Contacts can be linked to a company via dropdown. Supports filtering by first name/last name/email/company/language, sorting, and pagination. Brevo sync status and double opt-in are displayed as read-only fields in the detail view. Both backend API and frontend UI are implemented.
- **Comments** — Add comments to companies (and later contacts). Author is set automatically to "UNKNOWN" (prepared for SSO). Comments are displayed in the company detail view with pagination ("Load more"), sorted by creation date descending. The frontend sends only the text; the backend sets the author.
- **Health Monitoring** — A health endpoint to verify backend availability, displayed in the frontend.
- **Internationalization (i18n)** — Frontend supports German and English with language detection and switching.
- **API Documentation** — Swagger UI and OpenAPI spec auto-generated from backend annotations.

## Planned Features

- **Single Sign-On (SSO)** — Authentication via Authentik using OpenID Connect.
- **Brevo Sync** — Automatic synchronization of contacts and companies with Brevo for marketing workflows.