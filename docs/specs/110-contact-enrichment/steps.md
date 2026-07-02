# Implementation Steps: Contact Enrichment via Gravatar, Dropcontact & Cognism

Feature package: `com.openelements.crm.enrich` (backend). No generic provider SPI — concrete
client/service/controller per service; only DTOs and the fill-empty apply helper are shared.

## Step 1: Shared enrichment DTOs & value types

- [ ] `EnrichmentPayloadDto` — echoed enrichable bundle (email, position, phoneNumber, socialLinks
      map, companyName, photoBase64, photoContentType)
- [ ] `EnrichmentChangeDto` (field, currentValue, proposedValue)
- [ ] `CompanyResolutionDto` (kind MATCHED|NEW|NONE, companyId, companyName)
- [ ] `EnrichmentCandidateDto` (candidateId, label, changes, companyResolution, nothingToEnrich, payload)
- [ ] `EnrichmentResultDto` (status MATCH|NO_MATCH, candidates)
- [ ] `EnrichmentApplyDto` (payload, createCompany)
- [ ] `EnrichmentApplyResultDto` (contact, gdprNotice)
- [ ] `EnrichmentSettingsDto` (configured) / `EnrichmentSettingsUpdateDto` (apiKey)
- [ ] `RawCandidate` — internal (candidateId, label, payload) input to the applier

**Acceptance:** compiles; records only.

## Step 2: Contact-domain fill-empty helper

- [ ] `ContactEnrichmentApplier` (@Component): `buildResult(contactId, rawCandidates)` computes
      server-side changes (fill-empty only), per-network social-link diff, company resolution,
      `nothingToEnrich`; `apply(contactId, payload, createCompany)` re-checks fill-empty, sets photo
      (Gravatar), resolves/creates company, saves, publishes `OnObjectUpdate<ContactDto>`, returns DTO.

**Acceptance:** unit tests for fill-empty, per-network links, company MATCHED/NEW/NONE, photo-only-if-none,
re-check at apply, event published.

## Step 3: Concrete clients

- [ ] `GravatarClient` (keyless): md5(email) → avatar (`d=404`) + public profile JSON (best-effort).
- [ ] `DropcontactClient` (API key): batch submit + poll; `validateApiKey`.
- [ ] `CognismClient` (API key, ships inactive): search; `validateApiKey`. Mock-only.

**Acceptance:** client tests against MockWebServer parse representative JSON; generic errors on 5xx/timeout.

## Step 4: Concrete enrichment services

- [ ] `GravatarEnrichmentService` (email-only; skip without email)
- [ ] `DropcontactEnrichmentService` (email else name+company; 503 if unconfigured)
- [ ] `CognismEnrichmentService` (same)

**Acceptance:** service tests for matching input selection and unconfigured → 503.

## Step 5: Controllers

- [ ] `GravatarEnrichmentController` — search + apply (`@PreAuthorize` app-or-it-admin)
- [ ] `DropcontactEnrichmentController` — settings (`@RequiresItAdmin`) + search + apply
- [ ] `CognismEnrichmentController` — settings + search + apply

**Acceptance:** controller tests: 403 for non-admin, 403 for non-IT-admin on settings, keys never
returned, DELETE→configured:false, apply publishes event & returns GDPR notice.

## Step 6: Frontend — enrichment dialog & menu

- [ ] `EnrichButton`/`EnrichDialog` (shared): loading / selection / preview / info / error / success.
- [ ] Admin-gated "Anreichern" dropdown in `contact-detail.tsx`; Gravatar always, Dropcontact/Cognism
      when configured.
- [ ] API client functions + settings gating hooks.
- [ ] Admin settings panels for Dropcontact & Cognism (mirror Brevo panel).
- [ ] DE/EN i18n strings for all labels/states/GDPR notice.

**Acceptance:** component tests for gating, dialog states, per-network preview, create-company checkbox.

## Step 7: Reviews & PR

- [ ] `/spec-review`, `/quality-review` clean; build + tests green; PR opened closing #46.
