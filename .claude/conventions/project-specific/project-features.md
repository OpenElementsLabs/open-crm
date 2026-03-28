# Project Features

Open CRM is an open-source CRM system designed for startups and small teams to manage business relationships. It focuses on core CRM needs — companies, contacts, and communication history — without the complexity of enterprise solutions. The project is in early development.

## Core Features

- **Company Management** — Create, view, edit, and soft-delete companies with address and contact details. Supports filtering by name/city/country, sorting, pagination, and restoring archived companies. The company list shows contact count and comment count columns. The detail view shows contact count in the "show employees" link and comment count in the comments heading.
- **Contact Management** — Create, view, edit, and hard-delete contacts with name, email, phone, position, gender, language, LinkedIn URL, and optional birthday. Contacts can be linked to a company via dropdown. Supports filtering by first name/last name/email/company/language, sorting, and pagination. Birthday is displayed in locale-specific format (DE: DD.MM.YYYY, EN: MM/DD/YYYY) in the detail view. Brevo sync status and double opt-in are displayed as read-only fields. The contact list shows a comment count column. The detail view shows comment count in the comments heading. Bidirectional navigation links between contact and company detail views.
- **Comments** — Add comments to companies and contacts via a modal dialog. Author is set automatically to "UNKNOWN" (prepared for SSO). Comments are displayed in both company and contact detail views with pagination ("Load more"), sorted by creation date descending. The frontend sends only the text; the backend sets the author.
- **Image Upload** — Upload, display, and remove company logos (SVG, PNG, JPEG) and contact photos (JPEG only). Thumbnails appear in list tables (32x32, rounded); larger images display in detail views. Create/edit forms include file upload with client-side validation. Images are stored as `bytea` in PostgreSQL with a 2 MB size limit. DTOs expose `hasLogo`/`hasPhoto` boolean flags so the frontend can decide whether to load the image or show a placeholder icon.
- **Health Monitoring** — A health endpoint to verify backend availability, displayed in the frontend.
- **Internationalization (i18n)** — Frontend supports German and English with language detection and switching.
- **API Documentation** — Swagger UI and OpenAPI spec auto-generated from backend annotations.

## Planned Features

- **Single Sign-On (SSO)** — Authentication via Authentik using OpenID Connect.
- **Brevo Sync** — Automatic synchronization of contacts and companies with Brevo for marketing workflows.