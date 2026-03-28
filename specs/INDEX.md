# Spec Index

| ID  | Name                       | Description                                                                                         | GitHub Issue | Status |
|-----|----------------------------|-----------------------------------------------------------------------------------------------------|--------------|--------|
| 001 | Base project setup         | Spring Boot backend with health endpoint and Next.js frontend displaying health status              | —            | done   |
| 002 | Core data model            | Company, Contact, and Comment entities with CRUD REST APIs, Flyway migrations, and pagination       | —            | done   |
| 003 | DTO refactoring            | Rename DTOs from *Response/*Request to *Dto/*CreateDto/*UpdateDto and improve OpenAPI annotations   | —            | done   |
| 004 | Company frontend           | Company management UI with list, detail, create, edit, soft-delete, restore, and sidebar navigation | —            | done   |
| 005 | Company comments           | Display and create comments in the company detail view, connected to existing backend API           | —            | done   |
| 006 | Frontend i18n              | Bilingual support (DE/EN) with language toggle in sidebar and automatic detection                   | —            | done   |
| 007 | Contact frontend           | Contact management UI with list, detail, create, edit, delete, and company association              | —            | done   |
| 008 | Global UI styling fixes    | Add missing shadcn/ui semantic CSS variables for dialog/select backgrounds and table borders        | —            | done   |
| 009 | Comment Modal Dialog       | Refactor how comments are added                                                                     | —            | done   |
| 010 | Contact-company navigation | Bidirectional navigation links between contact detail and company detail views                      | —            | done   |
| 011 | Count columns              | Contact and comment counts in company/contact list tables and detail views                          | —            | done   |
| 012 | Contact birthday           | Optional birthday date field for contacts with standard date picker                                 | —            | done   |
| 013 | Image upload               | Company logo and contact photo upload with PostgreSQL storage and list/detail display               | —            | done   |
| 014 | Comment count live update  | Increment comment count in heading immediately after successful comment creation                    | —            | done   |
| 015 | Optional contact language  | Make language field on contacts nullable to allow unknown language                                  | —            | done   |
| 016 | Brevo import               | Import companies and contacts from Brevo with field mapping and company-contact association         | —            | done   |
| 017 | Fix image column type      | Fix @Lob/BYTEA type mismatch causing backend startup failure after Spec 013 implementation          | —            | done   |
| 018 | Component tests            | Repository, Service, and DTO conversion tests for Company, Contact, and Comment modules             | —            | done   |
| 019 | Logo display fix           | Show company logos fully (object-contain, no rounded corners) instead of cropping                   | —            | done   |
| 020 | Simplify company filters   | Remove unused city/country filters and sorting dropdown from company list                         | — | open   |
| 021 | Fix Brevo company ID       | Brevo company IDs are hex strings, not longs — all companies collapse into one entity            | — | open   |

