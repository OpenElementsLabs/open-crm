# Behaviors: Application Release Process

Scenarios are grouped by the artefact they describe. Versions in the examples assume the last released
tag is `v1.10.0` unless stated otherwise.

## Version consistency check (`check-versions.sh`)

### Identical snapshot versions pass

- **Given** `backend/pom.xml` has version `1.11.0-SNAPSHOT` and `frontend/package.json` has version `1.11.0-SNAPSHOT`
- **When** `./check-versions.sh` runs
- **Then** it exits with code 0 and prints the detected version

### Diverging versions fail

- **Given** `backend/pom.xml` has version `1.11.0-SNAPSHOT` and `frontend/package.json` has version `0.1.0`
- **When** `./check-versions.sh` runs
- **Then** it exits non-zero and the message names both files with their respective versions

### Today's broken state is rejected

- **Given** `backend/pom.xml` has version `0.1.0-SNAPSHOT` and `frontend/package.json` has version `0.1.0`
- **When** `./check-versions.sh` runs
- **Then** it exits non-zero — the equality rule already fails, before the downgrade rule is even reached

### The release commit passes despite having no `-SNAPSHOT` suffix

- **Given** both build files have version `1.11.0` and the highest release tag is `v1.10.0`
- **When** `./check-versions.sh` runs
- **Then** it exits 0 — the check asserts equality only and never requires the `-SNAPSHOT` suffix

### A version below the highest release tag is rejected

- **Given** both build files have version `1.9.0-SNAPSHOT` and the highest release tag is `v1.10.0`
- **When** `./check-versions.sh` runs
- **Then** it exits non-zero, reporting that the version is lower than the released `1.10.0`

### A `-SNAPSHOT` on an already released number is rejected

- **Given** both build files have version `1.11.0-SNAPSHOT` and the highest release tag is `v1.11.0`
- **When** `./check-versions.sh` runs
- **Then** it exits non-zero — a snapshot must not carry a version number that has already been released
  (this is the forgotten post-release bump)

### The release version equal to the highest tag is accepted

- **Given** both build files have version `1.11.0` and the highest release tag is `v1.11.0`
- **When** `./check-versions.sh` runs
- **Then** it exits 0 — this is the tagged release commit itself, which the release workflow re-checks

### Legacy two-component tags are ignored

- **Given** the repository contains the tags `v0.1`, `v1.2`, `v1.4`, `v1.9.0`, and `v1.10.0`
- **When** the highest release tag is determined
- **Then** `v1.10.0` is selected and the two-component tags are not considered

### Tag ordering is numeric, not lexicographic

- **Given** the tags `v1.9.0` and `v1.10.0` exist
- **When** the highest release tag is determined
- **Then** `v1.10.0` is selected (not `v1.9.0`, which would win a plain string sort)

### A repository without release tags skips the downgrade rule

- **Given** no tag matching `vA.B.C` exists
- **And** both build files have version `0.1.0-SNAPSHOT`
- **When** `./check-versions.sh` runs
- **Then** it exits 0 — only the equality rule applies

### `--expect` matching the build files passes

- **Given** both build files have version `1.11.0`
- **When** `./check-versions.sh --expect 1.11.0` runs
- **Then** it exits 0

### `--expect` not matching the build files fails

- **Given** both build files have version `1.11.0-SNAPSHOT`
- **When** `./check-versions.sh --expect 1.11.0` runs
- **Then** it exits non-zero, reporting the expected and the actual version

### A malformed build file produces a clear error, not a silent pass

- **Given** `backend/pom.xml` is not parseable, or has no `/project/version` element
- **When** `./check-versions.sh` runs
- **Then** it exits non-zero with a message naming the file, and does not fall back to an empty version
  that would compare equal to another empty version

## CI gate (`build.yml`)

### A pull request with drifted versions is blocked

- **Given** a pull request whose branch changes `frontend/package.json` to `1.12.0-SNAPSHOT` while
  `backend/pom.xml` stays at `1.11.0-SNAPSHOT`
- **When** `build.yml` runs on the pull request
- **Then** the `versions` job fails and the overall check is red

### The version gate does not wait for the build jobs

- **Given** a push to `main` with drifted versions
- **When** `build.yml` runs
- **Then** the `versions` job fails independently of `backend`, `frontend`, and `docker`, which are not
  its dependencies

### The version gate has access to tags

- **Given** the `versions` job runs on a shallow checkout by default
- **When** the job determines the highest release tag
- **Then** tags have been fetched, so the downgrade rule evaluates against `v1.10.0` rather than silently
  skipping for lack of tags

### The build definition is invocable by another workflow

- **Given** `build.yml` declares `workflow_call`
- **When** `release.yml` references it via `uses: ./.github/workflows/build.yml`
- **Then** all jobs of `build.yml` execute as part of the release run, with no build steps duplicated in
  `release.yml`

### The transient release commit on `main` does not fail CI

- **Given** `release.sh` pushes the commit `Version 1.11.0` to `main`
- **When** `build.yml` runs on that push
- **Then** the `versions` job passes, because equality holds and `1.11.0` is not below `v1.10.0`

## Release preparation (`release.sh`)

### Happy path

- **Given** a clean `main` at `1.11.0-SNAPSHOT`, up to date with `origin/main`
- **And** `docs/releases/v1.11.0.md` exists with content
- **And** Docker is running and the full build succeeds
- **When** `./release.sh 1.11.0 1.12.0-SNAPSHOT` runs
- **Then** two commits are pushed to `main` — `Version 1.11.0` and `Version 1.12.0-SNAPSHOT`
- **And** the tag `v1.11.0` is pushed, pointing at the `Version 1.11.0` commit
- **And** both build files end on `1.12.0-SNAPSHOT`
- **And** the script prints the Actions URL and states that the release is approved only once that run is green

### Invalid release version format is rejected

- **Given** any repository state
- **When** `./release.sh 1.11 1.12.0-SNAPSHOT` or `./release.sh 1.11.0-rc.1 1.12.0-SNAPSHOT` runs
- **Then** the script aborts before modifying any file, stating that only `A.B.C` is accepted

### Invalid next-snapshot format is rejected

- **Given** any repository state
- **When** `./release.sh 1.11.0 1.12.0` runs
- **Then** the script aborts, stating that the next version must end in `-SNAPSHOT`

### A next-snapshot version that is not greater than the release is rejected

- **Given** any repository state
- **When** `./release.sh 1.11.0 1.10.0-SNAPSHOT` or `./release.sh 1.11.0 1.11.0-SNAPSHOT` runs
- **Then** the script aborts before modifying any file, stating that the next version must be strictly
  greater than the release version

### Running off `main` is rejected

- **Given** the checked-out branch is `feat/117-app-release-process`
- **When** `./release.sh 1.11.0 1.12.0-SNAPSHOT` runs
- **Then** the script aborts, stating that releases are cut from `main` only

### A dirty working tree is rejected

- **Given** `main` with uncommitted modifications to any file
- **When** `./release.sh 1.11.0 1.12.0-SNAPSHOT` runs
- **Then** the script aborts before modifying any file, so no unrelated change can be swept into the
  version commit

### A `main` that is behind the remote is rejected

- **Given** `origin/main` has commits that the local `main` does not
- **When** `./release.sh 1.11.0 1.12.0-SNAPSHOT` runs
- **Then** the script aborts, stating that `main` must be up to date

### An already existing tag is rejected

- **Given** the tag `v1.11.0` exists locally or on the remote
- **When** `./release.sh 1.11.0 1.12.0-SNAPSHOT` runs
- **Then** the script aborts without modifying any file

### A missing release-notes file is rejected

- **Given** `docs/releases/v1.11.0.md` does not exist
- **When** `./release.sh 1.11.0 1.12.0-SNAPSHOT` runs
- **Then** the script aborts, naming the expected path — so no tag is pushed that the release workflow
  would reject anyway

### An empty release-notes file is rejected

- **Given** `docs/releases/v1.11.0.md` exists but contains only whitespace
- **When** `./release.sh 1.11.0 1.12.0-SNAPSHOT` runs
- **Then** the script aborts with the same message as for a missing file

### A stopped Docker daemon is rejected up front

- **Given** Docker is not running
- **When** `./release.sh 1.11.0 1.12.0-SNAPSHOT` runs
- **Then** the script aborts during the precondition checks, rather than failing minutes later inside the
  backend tests or `docker compose build`

### A `-SNAPSHOT` dependency blocks the release

- **Given** `backend/pom.xml` declares a dependency on version `1.4.0-SNAPSHOT` of a library
- **When** `./release.sh 1.11.0 1.12.0-SNAPSHOT` runs and has set the project version to `1.11.0`
- **Then** the script aborts because a `-SNAPSHOT` string remains in `backend/pom.xml`
- **And** the version changes are reverted, leaving a clean working tree

### A failing backend build reverts the version change

- **Given** a backend test fails
- **When** `./release.sh 1.11.0 1.12.0-SNAPSHOT` runs
- **Then** no commit is created, no tag is created, nothing is pushed
- **And** `backend/pom.xml` and `frontend/package.json` are restored to their `HEAD` content, so a second
  attempt does not fail the "working tree is clean" precondition

### A failing frontend build reverts the version change

- **Given** `pnpm test` or `pnpm build` fails
- **When** `./release.sh 1.11.0 1.12.0-SNAPSHOT` runs
- **Then** the same revert-and-abort behaviour applies, and the failing step is named in the output

### A failing Docker build reverts the version change

- **Given** `docker compose build` fails
- **When** `./release.sh 1.11.0 1.12.0-SNAPSHOT` runs
- **Then** the same revert-and-abort behaviour applies

### A failure after the tag has been pushed is not silently reverted

- **Given** the tag `v1.11.0` has already been pushed and the bump step fails (e.g. the push is rejected)
- **When** the script exits
- **Then** it does **not** revert or delete anything, and instead reports the exact remaining manual step,
  because the tag is already public

## Release workflow (`release.yml`)

### Happy path publishes the release

- **Given** the tag `v1.11.0` points at a commit on `main` whose build files say `1.11.0`
- **And** `docs/releases/v1.11.0.md` exists with content
- **When** the tag is pushed and `release.yml` runs
- **Then** `verify` passes, the full build passes
- **And** a GitHub Release `v1.11.0` titled `Open CRM v1.11.0` is created as **published**, not a draft

### The release body is the notes file plus a permalink

- **Given** `docs/releases/v1.11.0.md` starts with the line `# Open CRM v1.11.0 – Release Notes`
- **When** the release is created
- **Then** the body contains the file's content with that leading H1 removed
- **And** ends with a link to `.../blob/v1.11.0/docs/releases/v1.11.0.md`

### A notes file without an H1 is used verbatim

- **Given** `docs/releases/v1.11.0.md` begins with a paragraph rather than a heading
- **When** the release is created
- **Then** no content is stripped and the body starts with that paragraph

### A release-candidate tag is rejected

- **Given** the tag `v1.11.0-rc.1` is pushed (which the `v*.*.*` filter still matches)
- **When** `release.yml` runs
- **Then** the `verify` job fails on the tag-format check and no release is created

### A tag on a commit that is not on `main` is rejected

- **Given** the tag `v1.11.0` points at a commit on a feature branch that was never merged
- **When** `release.yml` runs
- **Then** the ancestor check fails, no build runs, and no release is created

### A tag whose commit is the current tip of `main` is accepted

- **Given** the tag `v1.11.0` points at exactly `origin/main` (the bump commit has not been pushed yet)
- **When** the ancestor check runs
- **Then** it passes, because a commit is an ancestor of itself

### A version/tag mismatch is rejected before any build time is spent

- **Given** the tag `v1.11.0` points at a commit whose build files still say `1.11.0-SNAPSHOT`
- **When** `release.yml` runs
- **Then** `verify` fails on the `--expect` check, the `build` job never starts, and no release is created

### A missing notes file fails the run hard

- **Given** the tag `v1.11.0` points at a commit without `docs/releases/v1.11.0.md`
- **When** `release.yml` runs
- **Then** `verify` fails and no release is created — there is no auto-generated-notes fallback

### A failing build produces no release

- **Given** `verify` passed but a test fails in the re-build of the tagged tree
- **When** the run finishes
- **Then** no GitHub Release exists for `v1.11.0`
- **And** the tag is therefore not approved for deployment

### A second push of the same tag does not abort a run in progress

- **Given** a `release.yml` run for `v1.11.0` is in progress
- **When** the same tag is pushed again
- **Then** the second run queues rather than cancelling the first, because
  `cancel-in-progress` is `false`

### The workflow needs no secrets

- **Given** a fork or a fresh clone of the repository with no configured secrets
- **When** `release.yml` runs on a tag
- **Then** it uses only the automatic `GITHUB_TOKEN`, and no step fails for a missing secret

## Release semantics and recovery

### A tag without a published release is not deployable

- **Given** the tag `v1.11.0` exists but no GitHub Release for it is published
- **When** an operator or third-party self-hoster looks for a version to deploy
- **Then** the documented rule is that this tag is not approved, and the previous release must be used

### A failed release is recovered with a new version, not a re-run

- **Given** the run for `v1.11.0` failed
- **When** the maintainer follows the documented recovery path
- **Then** the tag `v1.11.0` is deleted locally and on the remote, the cause is fixed on `main`,
  `docs/releases/v1.11.1.md` is written, and `./release.sh 1.11.1 1.12.0-SNAPSHOT` is run
- **And** the tag `v1.11.0` is never re-pointed at different code

### Deleting the tag of an already published release is not part of the recovery path

- **Given** a GitHub Release for `v1.11.0` has been published
- **When** a problem is found in it
- **Then** the recovery path is a new release, not deleting the tag — the documentation states this
  explicitly, because consumers may already have deployed it

## Initial migration

### `main` is corrected to the version that follows the last release

- **Given** the build files hold the stale `0.1.0` / `0.1.0-SNAPSHOT` and the highest release tag is `v1.10.0`
- **When** this spec is implemented
- **Then** both build files hold `1.11.0-SNAPSHOT`
- **And** `./check-versions.sh` passes on `main`

### The first real cut after the migration produces `v1.11.0`

- **Given** `main` is at `1.11.0-SNAPSHOT`
- **When** the next release is cut
- **Then** the tag is `v1.11.0` and the notes live in `docs/releases/v1.11.0.md`, continuing the existing
  three-component naming of `v1.9.0.md` / `v1.10.0.md`
