# TODO

## URL ↔ Filter Synchronization for Contact List

The contact list should fully synchronize URL parameters with the filter UI:

- All filter values (`search`, `companyId`, `language`, `brevo`, `tagIds`) should be readable from URL parameters
- Filter changes by the user should update the URL in real-time
- This enables sharing filtered views via URL

**Context:** Deferred from spec 009 (contact-company cross-navigation). Current state (verified 2026-07-03,
`frontend/src/app/(app)/contacts/contacts-client.tsx`): only `companyId` and `tagIds` are read from the URL on
initial load; the unified `search` field (spec 031, which replaced the former firstName/lastName/email filters),
`language`, and the Brevo filter are **not** read from the URL, and **no** filter change is written back to the URL.
So filtered views still cannot be shared via URL — full bidirectional sync is still open.

_Note: the two previous test-infrastructure TODO entries ("H2 Tests: Switch to Flyway + validate" and
"Testcontainers Integration Tests") have been consolidated into **Spec 103 — Tests on Postgres via Testcontainers**.
See `specs/103-tests-postgres-testcontainers/`._

## Company Duplicate Merging

Provide a way to detect and merge duplicate companies. This is needed because the Brevo import creates new companies
from the `COMPANY` text field on contacts without matching against existing company names — duplicates are expected and
acceptable during import. A separate merge feature will allow cleaning these up later.

**Context:** Deferred from the Brevo import integration spec.

## Webhook Integration Tests

Add integration tests for webhook firing that use an embedded HTTP server (e.g. MockWebServer or WireMock) to verify
that webhook calls are actually sent with the correct payload, headers, and timing. Unit tests with mocked HTTP client
are part of the initial implementation — these integration tests go beyond that.

**Context:** Identified during the grill session for Spec 075 (Webhook Support). Prerequisite: Spec 075 must be
implemented first.

## Cmd-K-Shortcut für globale Suche

Add a `Cmd+K` / `Ctrl+K` keyboard shortcut that opens the global search from anywhere in the app — either as
a fast-path navigation to the `/search` view or as an in-place command palette overlay. The decision between
the two UX variants is part of this future spec.

**Context:** Deferred from the Meilisearch global search initiative (see `meilisearch.md`). v1 ships only the
dedicated `/search` view with sidebar menu entry; the keyboard shortcut is a separate later spec.

**Prerequisite:** Meilisearch global search v1 must ship first (third of the three currently planned spec
initiatives — see `meilisearch.md`).

## Synchroner Fan-Out für Company- und Tag-Umbenennungen in der globalen Suche

Add listeners on `OnObjectUpdate<CompanyDto>` and `OnObjectUpdate<TagDto>` (plus the corresponding delete events)
that immediately re-index all affected contact documents in Meilisearch via a batch
`POST /indexes/crm_contacts/documents`. This eliminates the stale `companyName` / `tagNames` values in contact
search results between renames and the next backend restart.

v1 of the global search deliberately ships without this — renames are rare, deploys happen regularly, and the
auto-bootstrap on startup repairs the staleness. Implement this hardening only once operational data shows the
staleness actually bothers users (e.g. complaints, support tickets, missed search hits after a rename).

**Context:** Deferred from the Meilisearch global search initiative (see `meilisearch.md` § 6.3). v1 uses
defer-to-reindex; this entry tracks the synchronous fan-out as the later hardening so it does not get lost.

**Prerequisite:** Meilisearch global search v1 must ship first (third of the three currently planned spec
initiatives — see `meilisearch.md`).

## Auslagerung der globalen Suche in die Open-Elements-Libs

Move the reusable parts of the global search stack out of `open-crm` and into the shared libraries, so other
Open-Elements applications can adopt the same pattern with minimal code:

- **Backend** (`spring-services`): `MeilisearchClient`, the `SearchIndexEventListener` pattern based on
  `GenericDataEvent`, `SearchSettingsConfigurer`, and a generic indexer framework where each application only
  contributes a mapping function and the index settings per entity type.
- **Frontend** (`@open-elements/ui` / `@open-elements/nextjs-app-layer`): the `/search` page shell, the
  grouped-results component, the highlight renderer, and the sidebar menu entry as prop-driven, reusable parts.

**Context:** Deferred from the Meilisearch global search initiative (see `meilisearch.md`). v1 keeps everything
local to `open-crm` to keep the initial scope manageable. Extraction happens once the implementation has settled
and the abstraction boundary is clear.

**Prerequisite:** Meilisearch global search v1 must ship first (third of the three currently planned spec
initiatives — see `meilisearch.md`).

## GDPR-Abdeckung für Updates-View (Mitarbeiter-Aktivitätstransparenz)

Die geplante „Updates"-View (Activity Feed) zeigt jedem eingeloggten Benutzer, welcher Kollege wann welche Firma/
Person/Kommentar erstellt, geändert oder gelöscht hat. Das ist eine personenbezogene Aktivitätsverfolgung von
Mitarbeitenden durch andere Mitarbeitende und benötigt eine saubere rechtliche Grundlage — z. B. eine
Betriebsvereinbarung oder eine entsprechende Klausel im AV-Vertrag, die diese Transparenz abdeckt.

**Context:** Offene Frage aus der Grill-Session zur Updates-View-Spec. Die Updates-View selbst ist inzwischen
umgesetzt (Specs 096/097, Status `done`) — offen bleibt **nur** die rechtliche/organisatorische Grundlage
(Betriebsvereinbarung bzw. AV-Vertrags-Klausel). Das ist kein Code-Thema, sondern ein separater organisatorischer
Schritt und bleibt daher als stehender rechtlicher Hinweis bestehen.

## HEIC- und WebP-Support für Company-Logos

Extend the company-logo upload pipeline (`CompanyController.uploadLogo` / `CompanyService.updateLogo` via
`ImageData.of(file)`) to also accept HEIC and WebP uploads, transcoded to JPEG. Spec 102 deliberately ships
HEIC/WebP only for contact photos because the logo pipeline uses a different code path (`ImageData.of` helper
instead of manual content-type handling in the service) and bundling the change would have inflated spec 102's
scope.

Result is an inconsistent v1 UX: a user uploading their company logo from an iPhone (HEIC) sees an
"invalid format" error, even though uploading their own contact photo from the same iPhone works. This
TODO closes that gap. As part of the work, consider extracting the transcode logic from
`ContactPhotoTranscoder` into a shared helper so both pipelines share one source of truth instead of
diverging.

**Context:** Deferred from spec 102 (HEIC & WebP image format support). The decision to defer was a
scope-vs-consistency trade-off; logo uploads are far less frequent than contact-photo uploads, so the
inconsistency is bearable until this spec lands.

**Prerequisite:** Spec 102 (HEIC & WebP image format support) must be merged.

## HEIC-Support-Status im Admin-Bereich anzeigen

Add a visual indicator in the admin section showing whether HEIC decoding is currently available
(i.e. whether `libheif` / `libheif-plugin-libde265` are present in the running container). Surfaces the
result of the `HeicSupportCheck` bean so operators detect at a glance after a deploy whether the native
dependency shipped correctly — without having to read startup logs.

Suggested UX: a small status row in the existing admin/status page (similar to the DB / Brevo health
panels), with a green check if `heicAvailable == true` and a red warning with tooltip ("HEIC uploads will
be rejected with 415 — check Dockerfile") if false. Could later be expanded into a generic
"optional-features" status panel for similar runtime-detected capabilities (WebP, PDF rendering, ...).

**Context:** Deferred from spec 102 (HEIC & WebP image format support). v1 ships with logs-only detection
— the visual admin indicator is the operational hardening so silent deploy regressions (forgotten
`libheif` install, base-image update stripping the package) are caught immediately rather than only when
the first user hits a 415.

**Prerequisite:** Spec 102 (HEIC & WebP image format support) must be merged.

## HEIC- und WebP-Edge-Case-Testfixtures bereitstellen + verbleibende Tests aktivieren

Happy-path fixtures for all four formats are already in place under `backend/src/test/resources/images/`
(`test.jpeg`, `test.png`, `test.webp`, `test.heic`) — they unblock the basic decode/transcode tests in spec 102.
Several edge-case scenarios still ship `@Disabled` because they need specific variants:

- **Rotated HEIC** (EXIF orientation 6 — 90° CW) — exercises the upright-rotation path.
- **PNG with alpha** — exercises spec 101's flatten-on-white path (the existing `test.png` is opaque RGB).
- **Lossless WebP with alpha** — same flatten-on-white path on the WebP code path.
- **Animated WebP** — exercises "silent first frame only" behavior.
- **Oversize fixtures > 2 MB per format** — exercises the size-cap rejection. The four existing fixtures are
  all under 2 MB.
- **Probe sample `heic-probe/sample.heic`** (< 10 KB target) — bundled into the production JAR for the
  `HeicSupportCheck` startup probe. The existing 1 MB `test.heic` is too large to ship inside the production
  artifact.

When fixtures land, remove the corresponding `@Disabled` annotations.

Generation tools: `heif-enc` (libheif) for rotated/tiny HEIC, `cwebp -alpha_q` / animated `webpmux` for WebP
variants, ImageMagick `convert -alpha set` for alpha PNG, `dd if=/dev/urandom bs=1M count=3 > oversize.*` for
size-cap fixtures.

**Context:** Deferred from spec 102 (HEIC & WebP image format support). The four happy-path fixtures cover the
v1 decode contract; this entry tracks the remaining edge-case fixtures so the disabled scenarios become active
once produced.

**Prerequisite:** Spec 102 (HEIC & WebP image format support) must be merged.

## Frontend mit PWA erweitern

Es soll einfach möglich sein, das Frontend als PWA zu installieren.

## Strikte Audience-Prüfung für JWT-Validierung (inkl. MCP-Endpoint)

The Spring Security Resource Server (`spring.security.oauth2.resourceserver.jwt`) currently validates only the JWT
signature via JWKS — it does **not** check the `aud` claim. As a result, any Authentik-issued token from the same
tenant (e.g. one issued for the `open-crm` web frontend client) is accepted on all `/api/*` endpoints, and the same
applies to the planned MCP endpoint.

For tighter isolation, configure an audience validator that requires `aud` to contain the expected client ID:

- `/api/*` (web/api-key paths) → require `aud=open-crm` (or whatever the existing frontend client is named).
- `/mcp/*` (new MCP endpoint) → require `aud=open-crm-mcp` so a leaked frontend token cannot access the MCP server
  and vice versa.

The MCP-Connector spec deliberately defers this and treats both clients as accepted; harden once the MCP endpoint
is in production and the operational impact of stricter audience checks has been evaluated.

**Context:** Surfaced during the grill session for the MCP-Connector spec. Treated as a follow-up so the initial
MCP rollout is not blocked.

**Prerequisite:** MCP-Connector spec must be merged.

## Read-access audit for sensitive records (MCP and other consumers)

Today only mutations are audited (`audit_log` with `INSERT`/`UPDATE`/`DELETE`); reading a record — in the
frontend or via the MCP server — is not recorded. For data-protection purposes it may become desirable to audit
**who read which personal-data record**, at least for machine consumers (MCP, API-key clients) that pull data in
bulk. This should **not** be bolted onto the current mutation `audit_log` (an `INSERT` action for a read is
semantically wrong, and read access would drown the mutation trail). A dedicated read-/access-log is the right
model, most likely hung off **API keys and the controller endpoints** (a cross-cutting access log over the REST
layer) so it covers every external read consumer uniformly — not just MCP.

**Context:** Surfaced during review of spec 108 (MCP server). Phase 1 deliberately drops per-read DB auditing to
stay consistent with the unaudited frontend reads; access is recorded only as structured INFO logs
(`tool=… actor=…`).

**Prerequisite:** Best designed together with the planned scoped API keys (per-key identity makes the access log
meaningful).

## CSV export and print view for opportunities

Extend the opportunity list with the CSV export (column-selection dialog, like companies/contacts, spec 038) and
the print-optimized table view (spec 032/037). Both are established per-entity integrations that were deliberately
cut from the initial opportunity specs to keep their scope manageable.

**Context:** Deferred during the grill session for spec 113 (opportunity backend) / 114 (opportunity frontend).

**Prerequisite:** Specs 113 and 114 must be merged.

## Kanban integration for opportunity stage and status

Connect the opportunity `stage` and `status` fields to the external Open Elements Kanban app, which will become
the leading source for both values. The groundwork is already in place: `stage` is a free-text string (no enum,
any value accepted by the backend) and `status` is manually maintained until Kanban sets it. The integration
itself — sync direction, matching, API, and whether webhook events on stage/status changes are needed — is
undesigned and will be its own GitHub issue and spec.

**Context:** Deferred during the grill session for spec 113 (opportunity backend). Explicitly no preparation
beyond the string-typed stage was wanted in 113 (no external Kanban ID field, no guaranteed webhook events).

**Prerequisite:** Specs 113 and 114 must be merged.

## Anonymization instead of hard delete for contacts and companies

Replace (or complement) the current hard-delete of contacts and companies with an anonymization flow so that
GDPR Art. 17 erasure requests can be fulfilled even when the record is referenced by other entities. This became
pressing with spec 113: deleting a company or a contact is now **blocked** (409) while it is referenced as the
company or main contact of an opportunity — the interim workaround for an erasure request is manual (delete or
re-assign the opportunity first). Anonymization resolves this properly by scrubbing personal data while keeping
referential integrity.

**This must land before the system goes into production operation** (decision from the spec-113 grill session).

**Context:** Decided during the grill session for spec 113 (opportunity backend) as the successor to the
delete-blocking interim rule.

**Prerequisite:** Spec 113 must be merged.

## Software-side GDPR support for contact enrichment (Art. 14 information)

Contact enrichment (spec 110) pulls personal data from external sources (Dropcontact/Cognism). Step 1 only shows a
manual reminder notice after applying. The software could do more to support the Art. 14 information obligation:
trigger an information email to the data subject, set a reminder/deadline, and/or flag enriched contacts as
"pending information". This would turn the operational GDPR duty into a tracked, semi-automated workflow.

**Context:** Deferred from the grill session for spec 110 (contact enrichment). Step 1 deliberately ships only a
post-apply notice to keep scope manageable.

**Prerequisite:** Spec 110 (contact enrichment) must be merged.

## Provenance tracking for enriched contact fields (Art. 14(2)(f) source disclosure)

Persist, per enriched field, which external service it came from and when (e.g. "position via Cognism on
2026-07-02"). This is needed to answer a data subject's request about the **source** of their data
(Art. 14(2)(f) / Art. 15). The existing mutation audit log (spec 090) records that a field changed but not the
enrichment source. A dedicated provenance record (or an enrichment-source annotation on the audit entry) is the
right model — step 1 stores no source at all.

**Context:** Deferred from the grill session for spec 110 (contact enrichment). Step 1 consciously stores no
provenance.

**Prerequisite:** Spec 110 (contact enrichment) must be merged.

## Adopt the `spring-services-mcp` module and delete CRM's local MCP classes

`spring-services` 1.3.1 ships a `spring-services-mcp` module containing twelve classes in
`com.openelements.spring.base.mcp` — `McpConfiguration`, `McpPage`, `McpPaging`, `McpProperties`,
`McpSecurityConfig`, `McpServerConfig`, `McpToolLogic`, `McpToolProvider`, `McpTools`,
`McpToolSupport`, `McpActorLabel`, `McpUnavailableException` — every one of which also exists under
`backend/src/main/java/com/openelements/spring/base/mcp/`. Spec 108 wrote CRM's MCP implementation
into the library's package precisely as an extraction target, so the intended end state is to consume
the module and delete the local copies.

This is blocked on two things: `McpImageLogic` (spec 109) exists only in CRM and is **not** in the
library jar, so it must either stay behind or be contributed upstream first; and any behavioural drift
between the two copies has to be reconciled before switching. Note that MCP is enabled in production,
so this is not a low-stakes swap.

Until it happens there is **no automated protection** — no enforcer rule, no guard test — against
someone adding `spring-services-mcp` to `backend/pom.xml` and silently shadowing the local classes with
identically-named ones from the jar. Adding a banned-dependency enforcer rule is a cheaper interim
option if the adoption slips.

**Context:** Deferred from spec 115 (dependency updates); the reason that spec takes the à-la-carte
Path B (`spring-services-bom` + `-core` + `-search` + `-dbbackup`) instead of `spring-services-all`.

**Prerequisite:** Spec 115 must land first.

## Brand fonts are declared but never loaded

`@open-elements/ui`'s `brand.css` declares `--font-heading: "Montserrat", sans-serif` and
`--font-body: "Lato", sans-serif`, but contains **no `@font-face` rule** and nothing in the frontend
loads either family. The entire UI therefore renders in the platform's default `sans-serif`, and the
brand typography is effectively decorative CSS.

Either the fonts should be shipped properly (self-hosted in `@open-elements/ui`, not fetched from
Google Fonts, so the standalone build stays self-contained), or the declarations should be corrected
to name what is actually rendered. If they are introduced, the `next/og` preview image from spec 116
has to receive the same font data in the same change — otherwise the generated image diverges from
the application it depicts.

**Context:** Found while designing spec 116 (page metadata), which deliberately uses `ImageResponse`'s
default font for exactly this reason.

## Show the running application version at runtime

Make the deployed version observable from a running instance: expose it from the backend (e.g. a version
field on an existing admin/status endpoint, fed from the Maven build via `build-info` / `@project.version@`)
and display it on the `/admin/status` page next to the existing health and capability rows. The frontend
should surface its own `package.json` version too, so a mismatch between the two containers is visible.

Why it matters: the dev environment in Coolify continuously deploys `main` (an `A.B.C-SNAPSHOT` version)
while production is deployed from a concrete `vA.B.C` tag. Without a runtime indicator, an operator looking
at an instance cannot tell which of the two they are on, and a third-party self-hoster cannot tell which
release they are running when filing a bug report.

**Context:** Deferred during the grill session for the app release process spec (117). That spec establishes
the version as correct build-file metadata only; making it observable is explicitly out of scope.

**Prerequisite:** Spec 117 (app release process) must land first, so the build files actually carry a
truthful version.

## Hotfix releases from a branch off a released tag

Allow cutting a patch release (e.g. `v1.11.1`) from a hotfix branch based on the `v1.11.0` tag, instead of
only from `main`. Today `release.sh` refuses to run anywhere but `main`, and the release workflow enforces
the same via an ancestor check — so the only way to ship an urgent production fix is to cut from `main`,
which also ships whatever half-finished work has landed there since the last release.

Open design questions: how the hotfix commit gets back onto `main` (cherry-pick vs. merge), how the
`-SNAPSHOT` bump behaves on a hotfix branch (it must not push `main` forward), whether the release-notes
file for a hotfix lives on the branch or on `main`, and how the ancestor check is relaxed without opening
the door to tagging arbitrary feature branches.

**Context:** Deferred during the grill session for the app release process spec (117) — "erstmal geht nur
main". Accepted risk: an urgent fix currently drags unreleased `main` content into production with it.

**Prerequisite:** Spec 117 (app release process) must land first.

## Publish versioned container images for self-hosters

Publish pullable backend and frontend container images (e.g. `ghcr.io/openelementslabs/open-crm-backend:1.11.0`)
as part of a release, and offer a `docker-compose.yml` variant that references those tags instead of building
from source. Today a third-party operator has to clone the repository at the tag and let `docker compose`
build both images locally, which requires a full JDK/Maven and Node/pnpm toolchain build on their machine
and takes minutes rather than seconds.

This is the one piece the app release process deliberately leaves out ("no deployment, nothing published to
a registry"). Adding it later means deciding on image naming, whether images are also published for `main`
(dev), how digests are pinned in the shipped compose file, and how the reproducible-builds convention applies
to the published images.

**Context:** Deferred during the grill session for the app release process spec (117) — "clone the tag and
build locally is fine for now".

**Prerequisite:** Spec 117 (app release process) must land first.

## Generic `app-release-process.md` for infrastructure-docs

Write the org-wide convention document for **application** releases as a sibling to the existing
`java-release-process.md` and `npm-release-process.md` in
`~/git/open-elements/infrastructure-docs/docs/releases/`. It should describe the *pattern* — one app version
kept in sync across all build files, `A.B.C-SNAPSHOT` between releases, per-PR version-consistency gate,
locally-verified cut, tag-triggered full rebuild, auto-published GitHub Release whose body is the committed
release-notes file, "tag without release = not approved" — and point at `open-crm` as the reference
implementation, the way `java-parent` is the reference for libraries.

Deliberately **not** a copy-paste script: unlike the library pipelines, `release.sh` is app-specific
(open-crm has `backend/` + `frontend/`; another app may have one module or three services). The doc carries
the pattern, each app writes its own script.

**Context:** The generic doc was split off from the app release process spec (117), which is scoped to the
concrete implementation in this repository.

**Prerequisite:** Spec 117 (app release process) must be implemented, so the doc describes something that
actually exists and has been used at least once.

## Brand fonts in the app and the Open Graph preview image

`@open-elements/ui`'s `brand.css` declares `--font-heading: "Montserrat"` and `--font-body: "Lato"` but
ships **no `@font-face` rule**, so no web font is loaded — the UI renders in the platform `sans-serif`, and
the `next/og` preview image (spec 116) deliberately uses satori's bundled default font to match. If brand
fonts are introduced later, they must land in **both** places together: an `@font-face` in `brand.css` for the
app, and a font passed to `ImageResponse` in `src/app/(app)/{contacts,companies}/[id]/opengraph-image.tsx`
so the preview image does not diverge from the app it depicts.

**Context:** Deferred from spec 116 (per-page metadata). See `specs/116-page-metadata/design.md` → _Typography_.

Frage 9: Setzt die Spec Speicher-Limits (mem_limit bzw. deploy.resources.limits.memory) plus ein JAVA_TOOL_OPTIONS=-XX:
MaxRAMPercentage=75 — oder bleibt es unbegrenzt wie bei
den Schwester-Projekten, mit dem Risiko, dass ein Bootstrap-Lauf die Nachbar-Apps auf demselben Host verdrängt?                                                               
 

## Enforcer pinning rules in `java-parent` instead of per-project

The `maven-enforcer-plugin` rules that forbid dynamic versions and SNAPSHOT dependencies in the resolved
dependency graph (`banDynamicVersions` / `requireReleaseDeps`) are added locally to `backend/pom.xml` by the
version-pinning spec. They belong org-wide in `java-parent`, next to the existing
`enforce-build-environment` execution — every Open Elements Java project needs the same guarantee, and today
each one would have to repeat the configuration.

Moving them upstream also removes the risk that the local rule is silently dropped during a future pom
cleanup, since nothing outside this repository would notice.

**Context:** Deferred during the grill session for the version-pinning spec. The rule was placed locally
because `java-parent` had no release carrying it, and blocking the spec on an unreleased parent was not
wanted.

**Prerequisite:** A `java-parent` release that contains the rules, plus the separate dependency-update spec
that bumps `backend/pom.xml` onto it.

## Exact base-image tags and a `.nvmrc` ↔ Dockerfile equality check

The version-pinning spec requires Docker base images to carry `tag + digest` but leaves the *granularity of
the tag* free — `node:24-alpine@sha256:…` satisfies it just as `node:24.9.0-alpine@sha256:…` does. As a
result the Node version is declared in three places (`frontend/.nvmrc`, `frontend/package.json` → `engines`,
`frontend/Dockerfile`) with nothing enforcing that they agree, so `.nvmrc` and the image can drift apart
silently.

Requiring the *exact* patch tag alongside the digest would make the drift mechanically detectable: the gate
could compare `.nvmrc` against the tag in `frontend/Dockerfile` and fail when they diverge. The cost is a
second value to update on every base-image bump.

**Context:** Raised during the grill session for the version-pinning spec and explicitly deferred — "muss
später entschieden werden". The spec ships with tag granularity free.

**Prerequisite:** The version-pinning spec must land first (it introduces the gate this check would extend).

## Deterministic Next.js `buildId`

`frontend/next.config.ts` sets no `generateBuildId`, so Next.js generates a random id on every build. That
id ends up in the `/_next/static/<buildId>/…` paths and in `__NEXT_DATA__.buildId`, which makes the frontend
build output differ between two builds of the same commit — one of the two concrete blockers for
byte-identical rebuilds.

The fix is not a one-liner, because the id has to satisfy **two** constraints at once:

1. **Deterministic per commit** — otherwise it does not solve the problem.
2. **Different between commits** — the client compares `__NEXT_DATA__.buildId` to detect that a new version
   was deployed and to force a hard reload. A constant id means clients never notice a deploy and keep
   referencing chunks that no longer exist on the server.

The obvious candidate, the Git commit SHA, is **not available inside the Docker build**: the build context
is `./frontend`, while `.git` lives in the repository root and is therefore never copied into the image —
and it is exactly the Docker build that produces the shipped artifact. Deriving the id from the application
version violates constraint 2, because every commit on `main` between two releases carries the same
`A.B.C-SNAPSHOT`.

So the realistic option is passing the commit SHA in as a build `ARG`, which means CI (`build.yml`),
`release.sh` and plain `docker compose build` must all set it, plus a defined fallback for when it is unset.
That fallback is the actual design question: it must not silently reintroduce either a random or a constant
id.

**Context:** Deferred during the grill session for the version-pinning spec, which is scoped to pinning and
deliberately contains no byte-identity work.

## Test suite for the pinning gate

The pinning gate introduced by the version-pinning spec ships without tests, unlike `check-versions.sh`
which has `test/check-versions.test.sh`. The gate is the more complex of the two: it parses three
Dockerfiles, two Compose files and two workflow files, each with its own syntax.

A false-negative parser is worse than no gate — it claims protection that does not exist, and nobody notices
when it misses something. The concrete trap is multi-stage builds: `frontend/Dockerfile` contains
`FROM base AS deps`, an internal stage reference and not an image, which a naive regex reports as an
unpinned image. Fixtures should cover at minimum: pinned, unpinned, internal stage reference, digest without
tag, and an image reference with no tag at all.

**Context:** Raised during the grill session for the version-pinning spec and consciously accepted as a
risk — "ein TODO dass ein check hierfür in Zukunft sinnvoll ist".

**Prerequisite:** The version-pinning spec must land first.

## Automated digest and action-SHA updates (Dependabot/Renovate)

After the version-pinning spec, base images are pinned by digest and GitHub Actions by commit SHA. Nothing
updates either. Both therefore go stale silently: a pinned digest keeps pulling a base image whose CVEs were
fixed upstream months ago, and a pinned action SHA never picks up its own security fixes. Pinning converts a
reproducibility problem into a maintenance obligation, and right now that obligation has no owner and no
automation.

Dependabot supports both (`docker` and `github-actions` ecosystems) and rewrites `tag@sha256:…` pairs
correctly. The open questions are update cadence, whether Compose files are covered alongside Dockerfiles,
and how the resulting PRs are reviewed given that a digest bump is unreviewable by reading the diff.

**Context:** Raised during the grill session for the version-pinning spec, where the deliberate decision was
that the gate checks *pinning* (syntactic, offline) and never *freshness* — "das wird ja später etwas wie
Dependabot leisten müssen".

**Prerequisite:** The version-pinning spec must land first.

## Byte-identical builds (the actual reproducible-builds goal)

The version-pinning spec establishes that nothing floats, which is the precondition for reproducibility but
not reproducibility itself. Building the same tag twice still produces different artifacts. Closing that gap
needs three separate pieces:

- **`project.build.outputTimestamp`** — currently set nowhere, so JAR entries carry the build time and the
  backend JAR can never be byte-identical. The fix already exists upstream in `java-parent` but is not
  released yet; it arrives via the separate dependency-update spec, not through any change in this
  repository.
- **Deterministic Next.js `buildId`** — see the dedicated TODO above.
- **OS packages in the container images** — `backend/Dockerfile` runs `apt-get update && apt-get install`
  and `db-backup/Dockerfile` runs `apk add`, both unversioned. The base-image digest does **not** cover
  this: `apt-get update` fetches the package index at build time, so what gets installed depends on the
  build date. Pinning exact package versions is not the answer — Debian and Alpine drop superseded versions
  from their production mirrors, so an exact pin makes the build *fail* within weeks instead of making it
  reproducible. The real solution is a snapshot mirror (`snapshot.debian.org` and an equivalent for Alpine),
  which is a substantial change to how the images are built.

Only once all three land does a `verify-reproducible-build` job (build twice, compare SHA-256, fail on
difference) become meaningful; adding it earlier would just be permanently red.

**Context:** Split off from the version-pinning spec during its grill session. The explicit decision there
was that a differing SHA-256 between a local build and CI is currently **expected, not a bug** — "am Ende
muss es byte-gleich sein, aber für diese Spec wollen wir erst einmal Version-Pinning betreiben".

**Prerequisite:** The version-pinning spec, plus the dependency-update spec that bumps `java-parent`.
