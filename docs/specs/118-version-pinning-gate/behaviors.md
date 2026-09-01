# Behaviors: Version Pinning Gate

Scenarios for `check-pinning.sh` (Rules D, N, A), the `maven-enforcer-plugin` rules (Rule M), the CI and
release wiring, and the one-off fixes.

Exit-code contract used throughout: `0` all rules pass · `1` at least one rule failed · `2` usage error ·
`3` a scanned file could not be parsed.

---

## Rule D — Container images

### Compliant reference passes

- **Given** `docker-compose.yml` references `ghcr.io/openelementslabs/db-backup-service:0.1.1@sha256:1c331a1f…`
- **When** `./check-pinning.sh` runs
- **Then** that reference is reported as compliant and does not contribute a violation

### Missing digest fails

- **Given** `backend/Dockerfile` contains `FROM eclipse-temurin:21-jre`
- **When** `./check-pinning.sh` runs
- **Then** it exits `1` and the message names the file, the line number, and "missing digest"

### Missing tag fails even with a digest

- **Given** a Dockerfile contains `FROM node@sha256:abc…` with no tag
- **When** `./check-pinning.sh` runs
- **Then** it exits `1` with a "digest without tag" message
- **And** the rationale is enforced: the digest alone is immutable but unreadable, so the tag is required as
  human-facing documentation

### Untagged reference fails

- **Given** `docker-compose.override.yml` contains `image: ghcr.io/navikt/mock-oauth2-server` with neither tag
  nor digest
- **When** `./check-pinning.sh` runs
- **Then** it exits `1` and reports both the missing tag and the missing digest

### Development-only files are covered

- **Given** the only unpinned reference in the repository is in `docker-compose.override.yml`
- **When** `./check-pinning.sh` runs
- **Then** it exits `1` — the gate does not exempt development-only files

### Multi-stage reference is not an image

- **Given** `frontend/Dockerfile` contains `FROM node:24-alpine@sha256:… AS base` followed by
  `FROM base AS deps`, `FROM base AS build` and `FROM base AS runner`
- **When** `./check-pinning.sh` runs
- **Then** only the first `FROM` is evaluated as an image reference
- **And** the three stage references produce no violation
- **And** the script exits `0`

### Stage name shadowing an image name

- **Given** a Dockerfile defines `FROM alpine:3.21@sha256:… AS node` and later contains `FROM node`
- **When** `./check-pinning.sh` runs
- **Then** the second `FROM` is treated as a stage reference, because `node` was introduced by an earlier
  `AS`, and produces no violation

### Stage referenced before it is defined

- **Given** a Dockerfile contains `FROM builder` on line 1 and `FROM alpine:3.21@sha256:… AS builder` on
  line 5
- **When** `./check-pinning.sh` runs
- **Then** line 1 is reported as a violation, because only stage names introduced *earlier* in the file are
  recognised as stages

### `scratch` is skipped

- **Given** a Dockerfile contains `FROM scratch`
- **When** `./check-pinning.sh` runs
- **Then** no violation is reported for that line

### `ARG`-substituted base image fails with a distinct message

- **Given** a Dockerfile contains `FROM ${BASE_IMAGE}`
- **When** `./check-pinning.sh` runs
- **Then** it exits `1` with an "unresolvable operand" message
- **And** the message explains that the reference is hidden from the gate, rather than claiming it is unpinned

### A newly added Dockerfile is covered automatically

- **Given** a contributor adds `worker/Dockerfile` with `FROM python:3.12-slim`
- **And** the file is not listed anywhere in `check-pinning.sh`
- **When** `./check-pinning.sh` runs
- **Then** it exits `1` — coverage comes from globbing, not from an explicit file list

### Excluded paths are not scanned

- **Given** `.claude/skills/` contains example Dockerfiles with unpinned `FROM` lines
- **And** `node_modules/` contains vendored Dockerfiles
- **When** `./check-pinning.sh` runs against an otherwise compliant tree
- **Then** it exits `0`
- **And** the printed scan list does not include any path under `.git/`, `node_modules/`, `target/`, `.next/`
  or `.claude/`

### The scanned file list is printed

- **Given** any repository state
- **When** `./check-pinning.sh` runs
- **Then** it prints every file it scanned
- **And** a file that is *not* covered is therefore visible in the CI log rather than being indistinguishable
  from a pass

---

## Rule N — Node and npm

### Committed lockfile plus frozen installs passes

- **Given** `frontend/pnpm-lock.yaml` is tracked by git
- **And** all three install sites (`frontend/Dockerfile`, `.github/workflows/build.yml`, `release.sh`) use
  `pnpm install --frozen-lockfile`
- **When** `./check-pinning.sh` runs
- **Then** Rule N passes

### Version ranges in `package.json` are not flagged

- **Given** `frontend/package.json` declares `"next": "^15.3.3"` and `"react": "^19.1.0"`
- **When** `./check-pinning.sh` runs
- **Then** no violation is reported for those ranges
- **And** the reason holds: under `--frozen-lockfile` the ranges are never consulted, and the lockfile pins
  every transitive dependency plus its integrity hash

### Untracked lockfile fails

- **Given** `frontend/pnpm-lock.yaml` is absent, or present but not tracked by git
- **When** `./check-pinning.sh` runs
- **Then** it exits `1` with a message naming the lockfile

### Install without the frozen flag fails

- **Given** any Dockerfile, workflow file or `*.sh` script invokes `pnpm install` without `--frozen-lockfile`
- **When** `./check-pinning.sh` runs
- **Then** it exits `1` and the message names the file and line

### `.nvmrc` with only a major version fails

- **Given** `frontend/.nvmrc` contains `24`
- **When** `./check-pinning.sh` runs
- **Then** it exits `1` with a message requiring the exact `X.Y.Z` form

### `.nvmrc` with an exact version passes

- **Given** `frontend/.nvmrc` contains `24.9.0` followed by a trailing newline
- **When** `./check-pinning.sh` runs
- **Then** Rule N passes for that file
- **And** surrounding whitespace and the trailing newline are tolerated

---

## Rule A — GitHub Actions

### SHA-pinned action passes

- **Given** `build.yml` contains `uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683 # v4.2.2`
- **When** `./check-pinning.sh` runs
- **Then** the reference is compliant
- **And** the trailing version comment is permitted

### Mutable major tag fails

- **Given** `build.yml` contains `uses: actions/checkout@v6`
- **When** `./check-pinning.sh` runs
- **Then** it exits `1` and the message names the file, line, and the action

### Short SHA fails

- **Given** a workflow contains `uses: actions/checkout@11bd719`
- **When** `./check-pinning.sh` runs
- **Then** it exits `1` — only a full 40-character lowercase hex SHA is accepted

### Local workflow call is skipped

- **Given** `release.yml` contains `uses: ./.github/workflows/build.yml`
- **When** `./check-pinning.sh` runs
- **Then** no violation is reported — a `./`-prefixed value is a path in this repository, not a pinnable
  reference

### All workflow files are covered

- **Given** a new workflow `.github/workflows/nightly.yml` is added with `uses: actions/checkout@v6`
- **When** `./check-pinning.sh` runs
- **Then** it exits `1`

---

## Rule M — Maven resolved dependency graph

### Clean graph passes

- **Given** `backend/pom.xml` as it stands, with all dependencies at explicit release versions
- **When** `./mvnw verify` runs in `backend/`
- **Then** the enforcer execution passes

### The application's own SNAPSHOT version is allowed

- **Given** `backend/pom.xml` carries `<version>1.12.0-SNAPSHOT</version>`
- **When** `./mvnw verify` runs
- **Then** `requireReleaseDeps` does not fail — the project's own version is excluded, since spec 117 requires
  `main` to carry `A.B.C-SNAPSHOT` between releases

### A direct SNAPSHOT dependency fails

- **Given** a dependency is changed to `1.4.0-SNAPSHOT`
- **When** `./mvnw verify` runs
- **Then** the build fails in the enforcer execution and the message names the offending coordinate

### A transitive SNAPSHOT dependency fails

- **Given** a released dependency is added whose own pom depends on a SNAPSHOT artifact
- **When** `./mvnw verify` runs
- **Then** the build fails — this is the case a pom-parsing shell script cannot detect and the reason Rule M
  lives in the build rather than in `check-pinning.sh`

### A version range fails

- **Given** a dependency version is changed to `[1.0,2.0)`
- **When** `./mvnw verify` runs
- **Then** `banDynamicVersions` fails the build

### `LATEST` and `RELEASE` fail

- **Given** a dependency version is set to `LATEST` or `RELEASE`
- **When** `./mvnw verify` runs
- **Then** the build fails

---

## Deliberate non-violations

### OS package installs are not flagged

- **Given** `backend/Dockerfile` runs `apt-get update && apt-get install -y --no-install-recommends libheif1
  libheif-plugin-libde265` with no version pins
- **And** `db-backup/Dockerfile` runs `apk add --no-cache postgresql17-client aws-cli bash tzdata`
- **When** `./check-pinning.sh` runs against an otherwise compliant tree
- **Then** it exits `0`
- **And** these lines produce neither a violation nor a warning, because pinning them would make the build
  fail rather than reproducible

### The gate makes no claim about byte-identity

- **Given** `./check-pinning.sh` exits `0`
- **When** the same tag is built twice
- **Then** the two artifacts may still differ, and this is expected
- **And** no output of the gate states or implies that the build is reproducible

---

## Gate behaviour and wiring

### Runs offline

- **Given** no network connectivity
- **When** `./check-pinning.sh` runs
- **Then** it completes normally — no registry, no JDK, no Maven, no Node, no `pnpm` is required
- **And** it never asks a registry whether a newer digest exists for a pinned tag

### Unparseable file is distinguishable from a violation

- **Given** `docker-compose.yml` contains malformed YAML
- **When** `./check-pinning.sh` runs
- **Then** it exits `3` with a message naming the file
- **And** it does not exit `0`, and does not silently treat the file as compliant

### Unknown argument

- **Given** the script is invoked as `./check-pinning.sh --nope`
- **When** it runs
- **Then** it exits `2` and prints usage

### All violations are reported in one run

- **Given** three different files each contain one violation
- **When** `./check-pinning.sh` runs
- **Then** all three are reported before it exits `1` — it does not stop at the first

### CI blocks a pull request

- **Given** a pull request removes a digest from `frontend/Dockerfile`
- **When** `build.yml` runs
- **Then** the `pinning` job fails
- **And** it fails within seconds, independently of the `backend`, `frontend` and `docker` jobs, because it
  declares no `needs`

### Release build inherits the gate

- **Given** a `v*.*.*` tag is pushed
- **When** `release.yml` runs
- **Then** the `pinning` job runs as part of the `build.yml` call
- **And** a violation prevents the GitHub Release from being published

### `release.sh` aborts before building

- **Given** a violation exists in the working tree
- **When** `./release.sh <version>` runs
- **Then** it aborts in the precondition block with a non-zero exit
- **And** no version is written to any build file and no full build is started

---

## One-off fixes

### `engines` no longer claims Node 20 support

- **Given** `frontend/package.json` declares `"engines": { "node": ">=24" }`
- **When** a developer on Node 22 runs `pnpm install`
- **Then** pnpm reports an unsupported engine
- **And** the declaration now matches reality: `pnpm build` runs `generate-pwa-assets.mjs`, which imports a
  `.ts` file directly and needs Node ≥ 23.6

### `pnpm build` still succeeds on the pinned Node version

- **Given** `.nvmrc` holds the exact patch version matching the pinned `node:24-alpine` digest
- **When** `pnpm install --frozen-lockfile && pnpm test && pnpm build` runs
- **Then** all three succeed
- **And** `public/offline.html` and `public/sw.js` are generated as before

### Compose stack starts with every digest pinned

- **Given** all image references are pinned by tag + digest
- **When** `docker compose build` and `docker compose up` run
- **Then** all services start and reach healthy
- **And** `mock-oauth2` serves its JWKS so the backend accepts tokens as before

### Digests resolve on both architectures

- **Given** the pinned digests are manifest-list digests
- **When** the images are pulled on macOS/arm64 and on Linux/amd64
- **Then** both succeed, each receiving its own platform's image
- **And** a single-platform digest would fail on the other architecture — the failure mode this scenario
  exists to catch

### Line endings are enforced by git

- **Given** `.gitattributes` declares `* text=auto eol=lf`
- **When** the repository is checked out on Windows and a tracked text file is committed unchanged
- **Then** the committed content carries LF endings
- **And** the files marked `binary` are byte-identical to their committed form

---

## Regression protection

### Removing a pin is caught

- **Given** any single pinned reference is reverted to a floating form
- **When** `./check-pinning.sh` runs
- **Then** it exits `1`

### Adding a new service is caught

- **Given** a new service with an unpinned `image:` is added to `docker-compose.yml`
- **When** `./check-pinning.sh` runs
- **Then** it exits `1`

### Known gap in the gate's own coverage

- **Given** `check-pinning.sh` ships without a test suite
- **When** its parser fails to recognise a reference form not covered by the scenarios above
- **Then** that reference passes silently and the gate reports success
- **And** this is a consciously accepted risk, tracked in `docs/TODO.md` — a false negative here is
  indistinguishable from compliance
