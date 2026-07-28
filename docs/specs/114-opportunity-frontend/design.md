# Design: Opportunity (Deal) — Frontend

## GitHub Issue

*To be created — issue draft exists; number will be added here once the issue is on GitHub.*

## Summary

Management UI for the Opportunity entity introduced in spec `113-opportunity-backend`: a paginated,
filterable list view, a detail view with tags and comments, and create/edit forms — following the existing
Company/Contact frontend patterns (Next.js App Router, `@open-elements/ui` components, DE/EN i18n). In
addition, opportunities are surfaced in the two cross-cutting views that spec 113 feeds: the global search
results page and the updates feed.

## Goals

- `/opportunities` list, `/opportunities/new`, `/opportunities/[id]`, `/opportunities/[id]/edit` routes.
- Sidebar navigation entry with i18n.
- Non-blocking warning in the form when a selected contact does not belong to the selected company.
- Opportunity section in the `/search` results view; opportunity events rendered in the updates feed.
- Opportunity count column in the tag list (backend value from spec 113).

## Non-goals

- CSV export and print view for opportunities (deferred, see `docs/TODO.md`).
- Kanban board visualization or Kanban-app integration (own issue later).
- Forecast charts/aggregation over estimated values.

## Prerequisite

Spec 113 (backend) must be implemented and merged first.

## Technical approach

Follows the established per-entity frontend structure under `frontend/src/app/(app)/opportunities/`:

- `page.tsx` (server component, `force-dynamic`) → `opportunities-client.tsx` (list)
- `new/page.tsx`, `[id]/page.tsx`, `[id]/edit/page.tsx`
- Shared components in `frontend/src/components/`: `opportunity-form.tsx`, `opportunity-detail.tsx`,
  `opportunity-comments.tsx` (mirroring the contact equivalents)
- API functions in `frontend/src/lib/api.ts` (`getOpportunities`, `getOpportunity`, `createOpportunity`,
  `updateOpportunity`, `deleteOpportunity`, comment functions, `getUserOptions`), types in
  `frontend/src/lib/types.ts` (`OpportunityDto`, `OpportunityCreateDto`, `OpportunityUpdateDto`,
  `OpportunityStatus = "OPEN" | "WON" | "LOST"`, `UserOptionDto`)

### Design quality & branding

The UI uses the existing `@open-elements/ui` component library, which already encodes the Open Elements
brand (colors, typography, spacing) — no new brand assets are needed. New UI (status badges, warning hint)
uses the established semantic Tailwind/shadcn tokens so light/dark themes keep working. Status badge
colors: `OPEN` neutral, `WON` green (success token), `LOST` red (destructive token) — consistent with
existing badge usage.

### Value lists (frontend-only)

Spec 113 deliberately stores `stage` and `product` as free strings. The value lists live **only here**:

- Stage combobox options: `Lead`, `Erstkontakt`, `Qualifiziert`, `Angebot`, `Gewonnen`, `Verloren`
  (fixed suggestion list; stored as the displayed string, not translated — these are pipeline terms that
  will later come from the Kanban app). The field may be left empty.
- Product combobox options: `Support & Care`, `Digital Trust`. May be left empty.
- Status select: the three fixed enum values, displayed translated (DE: offen/gewonnen/verloren,
  EN: open/won/lost), stored as `OPEN`/`WON`/`LOST`.

Rationale: keeping the lists in a small `opportunity-options.ts` constant module makes the later switch to
Kanban-provided values a one-file change.

## Key screens

### List view (`/opportunities`)

- Table columns: Title (link to detail), Company (link), Main contact (link), Stage, Status (badge),
  Value (formatted `Intl.NumberFormat` de-DE/en-US, EUR, e.g. "25.000,00 €"), Owner (avatar + name).
- Filter row (single row, following spec 035 layout): text search (title), status select (All/open/won/lost),
  stage combobox, company combobox, tag multi-select.
- Pagination with page-size selector (10/20/50/100/200, spec 059) and record count display (spec 028).
- Row actions: edit and comment buttons (spec 034 pattern) with tooltips (spec 068).
- "New opportunity" primary button.

### Detail view (`/opportunities/[id]`)

- Header: title, status badge, tags as chips.
- Detail fields (spec 040/043 `DetailField` pattern): stage, product, estimated value (formatted),
  company (navigation link), main contact (navigation link), additional contacts (list of links),
  owner (avatar + name), created/updated timestamps.
- Comments section identical to company/contact detail (add via modal, delete with confirmation for
  admins, live count update).
- Edit button; delete button visible but disabled without ADMIN role (spec 085 pattern), delete uses the
  confirmation dialog.

### Create/edit form

- Fields: title (required), stage (combobox, optional), status (select, default "offen" on create),
  product (combobox, optional), estimated value (numeric input with "€" suffix, optional, two decimals,
  non-negative), company (searchable combobox, required), main contact (searchable combobox, required),
  additional contacts (multi-select combobox), owner (combobox over `GET /api/users/options`,
  **pre-selected with the current user** on create), tags (existing `TagMultiSelect`).
- **Company/contact mismatch warning:** whenever a selected main or additional contact has a
  `companyId` different from the selected company (or none), a non-blocking warning hint is shown below
  the respective field ("Kontakt gehört nicht zur ausgewählten Firma"). Submission stays possible —
  the backend accepts it by design. Computed client-side from the already-loaded contact data.
- **Main-contact duplication guard:** the additional-contacts selector excludes the currently selected
  main contact (client-side prevention of the backend's 400 rule).
- Client-side validation messages for required fields and invalid value input; server 400s surface as
  form errors following the existing form pattern.

### Sidebar navigation

New entry "Opportunities" (EN) / "Opportunitäten" (DE) with a fitting lucide icon (e.g. `Handshake`),
placed directly below Contacts. Uses the existing prop-driven Sidebar from `@open-elements/ui`.

### Global search view

`/search` gets an "Opportunities" section rendering the new `opportunities` hits from
`GET /api/search` (title as primary line, company name as secondary), consistent with the existing
company/contact/tag/comment sections; section hidden when empty.

### Updates feed view

The updates page renders the six new `UpdateType` values (`OPPORTUNITY_*`,
`OPPORTUNITY_COMMENT_*`): i18n labels ("hat die Opportunität … angelegt/geändert/gelöscht", comment
variants), entity name links to `/opportunities/[id]` (no link for deletes), no leading entity image
(opportunities have none) — author avatar and trash-icon-on-delete behavior as in spec 097.

### Tag list

The tag list table gets an "Opportunities" count column (linking to the opportunity list filtered by the
tag), next to the existing company/contact counts.

## i18n

All new labels, placeholders, warnings, and update-feed texts are added in both DE and EN via the existing
`LanguageProvider` translation mechanism. Stage and product values themselves are **not** translated (see
above).

## GDPR (DSGVO) considerations

No new personal data is collected by the frontend; it displays what spec 113 exposes. The owner selector
uses the reduced `UserOptionDto` (no email). GDPR obligations (anonymization before go-live, updates-feed
works-council coverage) are tracked in spec 113 and `docs/TODO.md`.

## Testing

Following the existing frontend conventions: type-safe API layer, `pnpm build` must pass (Next.js
build-time checks), and manual verification of the behaviors below. (The project currently has no
automated frontend test suite; behaviors serve as the manual test script and as the basis for future
tests.)

## Open questions

- None — decisions resolved in the spec-113 grill session apply here (combobox value lists, warning
  instead of validation, owner default).
