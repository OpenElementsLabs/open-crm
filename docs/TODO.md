# TODO

## URL ↔ Filter Synchronization for Contact List

The contact list should fully synchronize URL parameters with the filter UI:

- All filter values (`search`, `companyId`, `language`, `brevo`, `tagIds`) should be readable from URL parameters
- Filter changes by the user should update the URL in real-time
- This enables sharing filtered views via URL

**Context:** Deferred from spec 009 (contact-company cross-navigation). Current state (verified 2026-07-17,
`frontend/src/app/(app)/contacts/contacts-client.tsx`): only `companyId` and `tagIds` are read from the URL on
initial load; the unified `search` field (spec 031, which replaced the former firstName/lastName/email filters),
`language`, and the Brevo filter are **not** read from the URL. A write-back to the URL now exists, but **only** for
`tagIds` (the `TagMultiSelect` `onChange` handler calls `router.replace` with the tag query); `search`, `companyId`,
`language`, and `brevo` changes still only update local state. So filtered views still cannot be shared via URL — full
bidirectional sync for all five filters is still open.

_Note: the two previous test-infrastructure TODO entries ("H2 Tests: Switch to Flyway + validate" and
"Testcontainers Integration Tests") were consolidated into **Spec 103 — Tests on Postgres via Testcontainers**, which
is now `done` (see `docs/specs/103-tests-postgres-testcontainers/`). Those two items are therefore resolved._

## Company Duplicate Merging

Provide a way to detect and merge duplicate companies. This is needed because the Brevo import creates new companies
from the `COMPANY` text field on contacts without matching against existing company names — duplicates are expected and
acceptable during import. A separate merge feature will allow cleaning these up later.

**Context:** Deferred from the Brevo import integration spec. Still open — no merge or duplicate-detection code exists
in the `company` package (backend) or the frontend (verified 2026-07-17).

## Webhook Integration Tests

Add integration tests for webhook firing that use an embedded HTTP server (e.g. MockWebServer or WireMock) to verify
that webhook calls are actually sent with the correct payload, headers, and timing. Unit tests with mocked HTTP client
are part of the initial implementation — these integration tests go beyond that.

**Context:** Identified during the grill session for Spec 075 (Webhook Support), which is now `done`. Note: the actual
webhook-firing logic (`WebhookSender`, `WebhookDataService`) now lives in the shared `spring-services` library
(`com.openelements.spring.base.services.webhook`), not in this repo — `open-crm` only has the REST controller
(`WebhookController`). The MockWebServer/WireMock delivery test therefore most naturally belongs in `spring-services`;
this repo currently has only authorization tests for the webhook endpoint. MockWebServer is already used here for the
enrichment HTTP clients, so the tooling is available if the test is added on the CRM side instead.

## Cmd-K-Shortcut für globale Suche

Add a `Cmd+K` / `Ctrl+K` keyboard shortcut that opens the global search from anywhere in the app — either as
a fast-path navigation to the `/search` view or as an in-place command palette overlay. The decision between
the two UX variants is part of this future spec.

**Context:** Deferred from the Meilisearch global search initiative (see spec 104 and
`docs/adr/0001-meilisearch-client.md`). v1 shipped only the dedicated `/search` view with a sidebar menu entry; no
keyboard shortcut exists yet (no `keydown`/`metaKey`/`Ctrl+K` handler anywhere in `frontend/src`, verified
2026-07-17). The shortcut is a separate later spec.

**Prerequisite met:** Meilisearch global search v1 has shipped (spec 104, status `done`).

## Synchroner Fan-Out für Company- und Tag-Umbenennungen in der globalen Suche

Add fan-out re-indexing so that when a company or tag is renamed (or deleted), all affected contact documents are
immediately re-indexed in Meilisearch via a batch `POST /indexes/crm_contacts/documents`. This eliminates the stale
`companyName` / `tagNames` values embedded in contact search results between a rename and the next backend restart.

Current state (verified 2026-07-17): `SearchIndexEventListener` already subscribes to `OnObjectUpdate`/`OnObjectDelete`
for companies and tags, but on a company/tag change it only re-indexes that entity's **own** document
(`indexService.upsertCompany` / `upsertTag`). It does **not** query the affected contacts and re-index them, so the
denormalized `companyName`/`tagNames` fields on contact documents stay stale until the startup bootstrap re-indexes
contacts. The fan-out (find all contacts with `companyId=X` / `tagId=X`, batch re-index) is what remains.

v1 deliberately ships without this — renames are rare, deploys happen regularly, and the auto-bootstrap on startup
repairs the staleness. Implement this hardening only once operational data shows the staleness actually bothers users
(e.g. complaints, support tickets, missed search hits after a rename).

**Context:** Deferred from the Meilisearch global search initiative (see spec 104 and
`docs/adr/0001-meilisearch-client.md`). v1 uses defer-to-reindex; this entry tracks the synchronous fan-out as the
later hardening so it does not get lost.

**Prerequisite met:** Meilisearch global search v1 has shipped (spec 104, status `done`).

## Auslagerung der globalen Suche in die Open-Elements-Libs (nur noch Frontend)

Move the reusable parts of the global search stack out of `open-crm` and into the shared libraries, so other
Open-Elements applications can adopt the same pattern with minimal code.

- **Backend — done.** The generic backend stack (`MeilisearchClient`, `SearchIndexBootstrapStep`, `IndexSettings`,
  `Highlighter`, `SearchReadinessState`, `MeilisearchProperties`, `ScopedKeySpec`, …) now lives in the shared
  `spring-services` library under `com.openelements.spring.base.services.search` and is consumed via the
  `spring-services` dependency (`backend/pom.xml`). `open-crm` keeps only CRM-specific glue (`SearchController`,
  `CrmSearchService`, `SearchIndexService`, `SearchIndexEventListener`, the `*BootstrapStep` classes, DTOs). This part
  of the extraction is complete (it landed via a later `spring-services` release, not via spec 105 — which was only an
  in-repo package split).
- **Frontend — still open** (`@open-elements/ui` / `@open-elements/nextjs-app-layer`): the `/search` page shell, the
  grouped-results component, the highlight renderer, and the sidebar menu entry are still local to `open-crm`
  (`frontend/src/app/(app)/search/search-client.tsx`, sidebar entry in `frontend/src/app/(app)/layout.tsx`). Only the
  primitive `Input` comes from `@open-elements/ui`. These search-specific parts should become prop-driven, reusable
  components in the shared frontend libraries.

**Context:** Deferred from the Meilisearch global search initiative (see spec 104 and
`docs/adr/0001-meilisearch-client.md`). Extraction happens once the implementation has settled and the abstraction
boundary is clear. Only the frontend extraction remains.

## GDPR-Abdeckung für Updates-View (Mitarbeiter-Aktivitätstransparenz)

Die „Updates"-View (Activity Feed) zeigt jedem eingeloggten Benutzer, welcher Kollege wann welche Firma/
Person/Kommentar erstellt, geändert oder gelöscht hat. Das ist eine personenbezogene Aktivitätsverfolgung von
Mitarbeitenden durch andere Mitarbeitende und benötigt eine saubere rechtliche Grundlage — z. B. eine
Betriebsvereinbarung oder eine entsprechende Klausel im AV-Vertrag, die diese Transparenz abdeckt.

**Context:** Offene Frage aus der Grill-Session zur Updates-View-Spec. Die Updates-View selbst ist umgesetzt
(Specs 096/097, Status `done`; Code in `frontend/.../updates/` und `backend/.../updates/`) — offen bleibt **nur** die
rechtliche/organisatorische Grundlage (Betriebsvereinbarung bzw. AV-Vertrags-Klausel). Das ist kein Code-Thema, sondern
ein separater organisatorischer Schritt und bleibt daher als stehender rechtlicher Hinweis bestehen.

## HEIC- und WebP-Support für Company-Logos

Extend the company-logo upload pipeline (`CompanyController.uploadLogo` / `CompanyService.updateLogo` via
`ImageData.of(file)`) to also accept HEIC and WebP uploads, transcoded to JPEG. Spec 102 deliberately shipped
HEIC/WebP only for contact photos because the logo pipeline uses a different code path (`ImageData.of` helper, storing
the raw bytes) while the contact path in `ContactService.uploadPhoto` does content-type dispatch, HEIC-availability
gating (415 if `CrmHeicSupportCheck.isHeicAvailable()` is false), and transcodes to JPEG. Bundling the change would
have inflated spec 102's scope.

Result is an inconsistent v1 UX: a user uploading their company logo from an iPhone (HEIC) sees an
"invalid format" error, even though uploading their own contact photo from the same iPhone works. This
TODO closes that gap. As part of the work, extract the transcode logic — currently a private static method
`transcodeToJpeg` inside `ContactService` (there is no separate `ContactPhotoTranscoder` class) — into a shared helper
so both pipelines share one source of truth and the same HEIC gating, instead of diverging.

**Context:** Deferred from spec 102 (HEIC & WebP image format support). The decision to defer was a
scope-vs-consistency trade-off; logo uploads are far less frequent than contact-photo uploads, so the
inconsistency is bearable until this spec lands.

**Prerequisite met:** Spec 102 (HEIC & WebP image format support) is merged (status `done`).

## HEIC- und WebP-Edge-Case-Testfixtures bereitstellen + verbleibende Tests aktivieren

Happy-path fixtures for all four formats are in place under `backend/src/test/resources/images/`
(`test.jpeg`, `test.png`, `test.webp`, `test.heic`). Several edge-case scenarios still ship `@Disabled` because they
need specific variants — all of them in `ContactPhotoHeicWebpIntegrationTest` and gated on `FIXTURE_TODO`:

- **Rotated HEIC** (EXIF orientation 6 — 90° CW) — exercises the upright-rotation path.
- **PNG with alpha** — exercises spec 101's flatten-on-white path (the existing `test.png` is opaque RGB).
- **Lossless WebP with alpha** — same flatten-on-white path on the WebP code path.
- **Animated WebP** — exercises "silent first frame only" behavior.
- **Malformed HEIC** — exercises the 400-rejection path.
- **Probe sample `heic-probe/sample.heic`** (< 10 KB target) — bundled into the production JAR for the
  `HeicSupportCheck` startup probe. The existing 1 MB `test.heic` is too large to ship inside the production
  artifact. This sample does not yet exist anywhere under `backend/src`.

Currently **9** `@Disabled` tests remain in `ContactPhotoHeicWebpIntegrationTest`. (The oversize > 2 MB size-cap
scenarios are already active — they generate bytes in-memory rather than needing a fixture file.) When the fixtures
land, remove the corresponding `@Disabled` annotations.

Generation tools: `heif-enc` (libheif) for rotated/tiny HEIC, `cwebp -alpha_q` / animated `webpmux` for WebP
variants, ImageMagick `convert -alpha set` for alpha PNG.

**Context:** Deferred from spec 102 (HEIC & WebP image format support). The four happy-path fixtures cover the
v1 decode contract; this entry tracks the remaining edge-case fixtures so the disabled scenarios become active
once produced.

**Prerequisite met:** Spec 102 (HEIC & WebP image format support) is merged (status `done`).

## Strikte Audience-Prüfung für JWT-Validierung (inkl. MCP-Endpoint)

The Spring Security Resource Server (`spring.security.oauth2.resourceserver.jwt`) currently validates only the JWT
signature via JWKS (plus `exp`/`nbf`) — it does **not** check the `aud` claim (verified 2026-07-17: `application.yml`
configures only `jwk-set-uri`, and the library `SecurityConfig` wires no `OAuth2TokenValidator`/audience). As a result,
any Authentik-issued token from the same tenant (e.g. one issued for the `open-crm` web frontend client) is accepted on
all `/api/*` endpoints.

For tighter isolation, configure an audience validator that requires `aud` to contain the expected client ID:

- `/api/*` (web/api-key paths) → require `aud=open-crm` (or whatever the existing frontend client is named).
- MCP endpoint → require `aud=open-crm-mcp` so a leaked frontend token cannot access the MCP server and vice versa.

Note: in the shipped Phase 1 the MCP surface (`/mcp/**`) authenticates via `X-API-Key`
(`ApiKeyAuthenticationFilter`), not JWT, so an `aud=open-crm-mcp` check is not applicable there yet — it becomes
relevant if/when the MCP endpoint accepts JWTs. Harden the `/api/*` audience check once the operational impact of
stricter audience checks has been evaluated.

**Context:** Surfaced during the grill session for the MCP-Connector spec (108, now `done`). Treated as a follow-up so
the initial MCP rollout was not blocked.

## Read-access audit for sensitive records (MCP and other consumers)

Today only mutations are audited (`audit_log` with `INSERT`/`UPDATE`/`DELETE`); reading a record — in the
frontend or via the MCP server — is not recorded (verified 2026-07-17: MCP reads emit only a structured INFO log line
`tool=… actor=…`; no read/access audit table exists). For data-protection purposes it may become desirable to audit
**who read which personal-data record**, at least for machine consumers (MCP, API-key clients) that pull data in
bulk. This should **not** be bolted onto the current mutation `audit_log` (an `INSERT` action for a read is
semantically wrong, and read access would drown the mutation trail). A dedicated read-/access-log is the right
model, most likely hung off **API keys and the controller endpoints** (a cross-cutting access log over the REST
layer) so it covers every external read consumer uniformly — not just MCP.

**Context:** Surfaced during review of spec 108 (MCP server, now `done`). Phase 1 deliberately drops per-read DB
auditing to stay consistent with the unaudited frontend reads; access is recorded only as structured INFO logs
(`tool=… actor=…`).

**Prerequisite:** Best designed together with the planned scoped API keys (per-key identity makes the access log
meaningful).

## Software-side GDPR support for contact enrichment (Art. 14 information)

Contact enrichment (spec 110) pulls personal data from external sources (Dropcontact/Cognism). Step 1 only shows a
manual reminder notice after applying (a hard-coded `GDPR_NOTICE` string in `ContactEnrichmentApplier`, returned as
`gdprNotice` on the apply result). The software could do more to support the Art. 14 information obligation:
trigger an information email to the data subject, set a reminder/deadline, and/or flag enriched contacts as
"pending information". This would turn the operational GDPR duty into a tracked, semi-automated workflow.

**Context:** Deferred from the grill session for spec 110 (contact enrichment, now `done`). Step 1 deliberately ships
only a post-apply notice to keep scope manageable.

## Provenance tracking for enriched contact fields (Art. 14(2)(f) source disclosure)

Persist, per enriched field, which external service it came from and when (e.g. "position via Cognism on
2026-07-02"). This is needed to answer a data subject's request about the **source** of their data
(Art. 14(2)(f) / Art. 15). The existing mutation audit log (spec 090) records that a field changed but not the
enrichment source. A dedicated provenance record (or an enrichment-source annotation on the audit entry) is the
right model — step 1 stores no source at all (verified 2026-07-17: `EnrichmentPayloadDto` / `EnrichmentChangeDto`
carry no source or timestamp, and `ContactEnrichmentApplier.apply` writes fields directly without recording origin).

**Context:** Deferred from the grill session for spec 110 (contact enrichment, now `done`). Step 1 consciously stores
no provenance.
