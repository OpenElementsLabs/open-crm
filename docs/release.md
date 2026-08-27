# Releasing Open CRM

How to cut a release of **this application**. Open CRM publishes nothing to any registry — a release
is a git tag plus a published GitHub Release. Production is deployed from a tag (manually, via
Coolify) and third-party self-hosters clone the repository at a tag and build the images themselves,
so the tagged tree must carry the correct version, actually build on a neutral machine, and ship with
release notes an outside operator can act on.

> **The one rule that matters:** a **tag without a published GitHub Release is not approved for
> deployment.** Deploy a tag only once its Release is green and published; otherwise deploy the
> previous release.

## Version model

- The application has **one version**, `A.B.C`, held identically in `backend/pom.xml`
  (`/project/version`) and `frontend/package.json` (`version`).
- Between releases, `main` carries **`A.B.C-SNAPSHOT`** — the Coolify dev environment continuously
  deploys `main`, so a dev build must never be mistakable for a release.
- A release is that number without the suffix; the tag is `v` + the number (e.g. `v1.11.0`).
- Immediately after the tag is pushed, `main` is bumped to the next `-SNAPSHOT`, so `main` never sits
  on a release version for more than one commit.
- `frontend/package.json` is `private: true`, so `-SNAPSHOT` carries no npm meaning — it exists only
  so both files can be compared literally by `check-versions.sh`.

`check-versions.sh` enforces this on every PR and push (the `versions` job in `build.yml`): the two
files must be equal, and the version must not be below the highest existing `vA.B.C` tag.

## Cutting a release

### 1. Write the release notes (by hand, reviewed)

The notes file **is** the public release body that operators read to decide whether they can upgrade,
so it is written and reviewed by a human — `release.sh` will not generate it and refuses to proceed
without it.

```bash
# Draft with the release-doc skill, then read and edit it.
claude -p "/release-doc <last-version> <this-version>"
# Result: docs/releases/vA.B.C.md   (three-component name, e.g. docs/releases/v1.11.0.md)
git add docs/releases/vA.B.C.md
git commit -m "docs: release notes for vA.B.C"
git push origin main
```

### 2. Run the release script

```bash
./release.sh <release-version> <next-snapshot-version>
# e.g.
./release.sh 1.11.0 1.12.0-SNAPSHOT
```

`release.sh` refuses to produce an invalid tag. Before changing anything it checks: the version
formats; that the next snapshot is strictly greater than the release; that you are on a clean, in-sync
`main`; that the tag does not already exist; that `docs/releases/v<release>.md` exists and is
non-empty; and that Docker is running. Then it:

1. sets the release version in both build files and re-checks them with `check-versions.sh`;
2. asserts **no `-SNAPSHOT` remains** anywhere in `backend/pom.xml` (a `-SNAPSHOT` dependency would
   make the release unreproducible — release that dependency first);
3. runs the **full local build** — `./mvnw clean verify`, `pnpm install --frozen-lockfile && pnpm test
   && pnpm build`, and `docker compose build`;
4. commits and pushes `Version <release>` to `main`;
5. **tags and pushes `v<release>`** — this triggers the release workflow;
6. bumps both files to `<next-snapshot>`, commits and pushes.

If any step **before** the release commit fails, the script reverts the version changes and leaves a
clean tree, so a second attempt does not trip the "working tree is clean" precondition. If a step
**after** the tag was pushed fails, it does **not** revert — the tag is already public — and instead
prints the remaining manual step.

### 3. Let CI publish the Release

Pushing the tag triggers `.github/workflows/release.yml`:

1. **verify** — the tag is `vA.B.C` (release candidates rejected); the tagged commit is an ancestor of
   `main`; both build files equal the tag (`check-versions.sh --expect`); the notes file exists and is
   non-empty. Cheap checks run before any build time is spent.
2. **build** — the complete PR build (`build.yml`, invoked via `workflow_call`) re-runs against the
   tagged tree from a clean checkout.
3. **release** — publishes a GitHub Release titled `Open CRM vA.B.C`, whose body is
   `docs/releases/vA.B.C.md` (leading `# …` heading stripped) followed by a permalink to the file at
   the tag. It uses only the automatic `GITHUB_TOKEN`; no secrets are required.

The Release is published immediately, not as a draft — **green CI is the approval gate.**

### 4. Deploy

Point Coolify production at the new tag once its GitHub Release is published.

## Recovery from a failed release run

A failed run is **not** repaired by re-running it, because the tag may already have been fetched, read,
or deployed the moment it appeared. Re-pointing a tag at different code would silently change what that
version means for every consumer.

1. Delete the tag locally and on the remote:
   ```bash
   git tag -d v1.11.0
   git push --delete origin v1.11.0
   ```
2. Fix the cause on `main`.
3. Write `docs/releases/v1.11.1.md` and cut a **new** version:
   ```bash
   ./release.sh 1.11.1 1.12.0-SNAPSHOT
   ```

Burning a patch number is cheap; a mutable release tag is not.

**Once a Release has been published**, the recovery path is likewise a new release — never delete the
tag of a published Release, because consumers may already have deployed it.
