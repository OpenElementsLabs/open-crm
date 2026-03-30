# Spec Index

| ID  | Name                       | Description                                                                                           | GitHub Issue | Status |
|-----|----------------------------|-------------------------------------------------------------------------------------------------------|--------------|--------|
| 001 | Base project setup         | Spring Boot backend with health endpoint and Next.js frontend displaying health status                | —            | done   |
| 002 | Core data model            | Company, Contact, and Comment entities with CRUD REST APIs, Flyway migrations, and pagination         | —            | done   |
| 003 | DTO refactoring            | Rename DTOs from *Response/*Request to *Dto/*CreateDto/*UpdateDto and improve OpenAPI annotations     | —            | done   |
| 004 | Company frontend           | Company management UI with list, detail, create, edit, soft-delete, restore, and sidebar navigation   | —            | done   |
| 005 | Company comments           | Display and create comments in the company detail view, connected to existing backend API             | —            | done   |
| 006 | Frontend i18n              | Bilingual support (DE/EN) with language toggle in sidebar and automatic detection                     | —            | done   |
| 007 | Contact frontend           | Contact management UI with list, detail, create, edit, delete, and company association                | —            | done   |
| 008 | Global UI styling fixes    | Add missing shadcn/ui semantic CSS variables for dialog/select backgrounds and table borders          | —            | done   |
| 009 | Comment Modal Dialog       | Refactor how comments are added                                                                       | —            | done   |
| 010 | Contact-company navigation | Bidirectional navigation links between contact detail and company detail views                        | —            | done   |
| 011 | Count columns              | Contact and comment counts in company/contact list tables and detail views                            | —            | done   |
| 012 | Contact birthday           | Optional birthday date field for contacts with standard date picker                                   | —            | done   |
| 013 | Image upload               | Company logo and contact photo upload with PostgreSQL storage and list/detail display                 | —            | done   |
| 014 | Comment count live update  | Increment comment count in heading immediately after successful comment creation                      | —            | done   |
| 015 | Optional contact language  | Make language field on contacts nullable to allow unknown language                                    | —            | done   |
| 016 | Brevo import               | Import companies and contacts from Brevo with field mapping and company-contact association           | —            | done   |
| 017 | Fix image column type      | Fix @Lob/BYTEA type mismatch causing backend startup failure after Spec 013 implementation            | —            | done   |
| 018 | Component tests            | Repository, Service, and DTO conversion tests for Company, Contact, and Comment modules               | —            | done   |
| 019 | Logo display fix           | Show company logos fully (object-contain, no rounded corners) instead of cropping                     | —            | done   |
| 020 | Simplify company filters   | Remove unused city/country filters and sorting dropdown from company list                             | —            | done   |
| 021 | Fix Brevo company ID       | Brevo company IDs are hex strings, not longs — all companies collapse into one entity                 | —            | done   |
| 022 | Remove double opt-in       | Remove unused doubleOptIn boolean field from contacts across entire stack                             | —            | done   |
| 023 | Contact brevo cleanup      | Remove syncedToBrevo flag, change brevoId from Long to String, expose computed brevo boolean          | —            | done   |
| 024 | Brevo origin filter        | Three-way filter (All/From Brevo/Not from Brevo) on company and contact list tables                   | —            | done   |
| 025 | Brevo detail badge         | Show "Brevo" tag below name in company/contact detail views, remove synced checkbox                   | —            | done   |
| 026 | Brevo fields readonly      | Prevent editing of firstName, lastName, email, language on Brevo contacts (400 + disabled UI)         | —            | done   |
| 027 | Brevo reimport protect     | Only overwrite Brevo-managed fields on contact re-import, preserve user-editable fields               | —            | done   |
| 028 | Record count display       | Show total filtered record count in company and contact list pagination ("42 Firmen · Seite 1 von 3") | —            | done   |
| 029 | Contact table columns      | Merge firstName/lastName into single Name column, add Email column to contact list table              | —            | done   |
| 030 | Remove contact sorting     | Remove sorting dropdown from contact list, fixed lastName ascending default                           | —            | done   |
| 031 | Contact unified search     | Replace three text filters with single search field across name, email, and company name              | —            | done   |
| 032 | Print table view           | Print button opens new tab with all filtered records in print-optimized table, auto-triggers print    | —            | done   |
| 033 | OpenAPI parameter docs     | Add @Parameter annotations to all REST controller method parameters for Swagger UI descriptions       | —            | done   |
| 034 | Table action buttons       | Add edit and comment action buttons to company and contact list table rows                            | —            | done   |
| 035 | Company filter layout      | Move archive toggle into the same row as name filter and Brevo dropdown                               | —            | done   |
| 036 | Detail view cleanup        | Remove redundant name fields, improve language display, merge address into block                      | —            | done   |
| 037 | Print view A4 fit          | Scale print tables to fit DIN A4 portrait, remove comment column, wrap text, repeat headers           | —            | done   |
| 038 | CSV export                 | Backend CSV generation with dynamic column selection via frontend checkbox dialog                     | —            | done   |
| 039 | No company filter          | Add "No company" option to contact list company filter for unassigned contacts                        | —            | done   |
| 040 | Detail field actions       | Copy-to-clipboard, open URL, mailto:, tel: action icons on detail view fields                         | —            | done   |
| 041 | Company phone number       | Add optional phone number field to Company entity, detail view, form, and CSV export                  | —            | done   |
| 042 | Print Safari fix           | Fix table cell text wrapping in Safari print by applying classes at component level                   | —            | done   |
| 043 | Always-visible actions     | Show detail field action icons always (light color), darken on hover, dark on touch devices           | —            | done   |
| 044 | Page serialization fix     | Enable VIA_DTO page serialization to fix PageImpl warning, update frontend Page type                  | —            | done   |
| 045 | User model prep            | Hardcoded dummy user for comments and sidebar, prepare for future Authentik SSO                       | —            | done   |
| 046 | OE branding                | Sidebar header with app logo placeholder, "Open CRM" name, and "Developed by" OE branding             | —            | done   |
| 047 | OIDC infrastructure        | mock-oauth2-server for local dev, OIDC env vars for Authentik in production                           | —            | done   |
| 048 | Frontend OIDC auth         | Auth.js v5 login, session management, token forwarding, sidebar user display, logout                  | —            | done   |
| 049 | Backend OIDC auth          | Spring Security Resource Server, JWT validation, UserService from token, Swagger authorize            | —            | done   |
| 050 | Tags backend               | Tag entity with CRUD API, many-to-many to companies/contacts, join tables, cascade delete             | —            | done   |
| 051 | Tag frontend CRUD          | Tag list, create/edit pages, tag chips on detail views, tag assignment in entity forms                | —            | done   |
| 052 | Tag count & filter         | Company/contact counts in tag list with navigation, tag multi-select filter in list views             | —            | done   |
| 053 | Tag filter fixes           | Compact TagMultiSelect layout, pass tag filter to print view and CSV export                           | —            | done   |
| 054 | Admin view                 | Merge Brevo Import and Server Health into single Admin page, reposition nav item                      | —            | done   |
| 055 | Brevo unlink               | Remove brevoId/brevoCompanyId when entries no longer exist in Brevo, with unlinked counts in result   | —            | open   |

