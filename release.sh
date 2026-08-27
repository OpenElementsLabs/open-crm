#!/usr/bin/env bash
#
# release.sh — prepare the git state for an Open CRM application release.
#
#   ./release.sh <release-version> <next-snapshot-version>
#   ./release.sh 1.11.0 1.12.0-SNAPSHOT
#
# It sets the release version in both build files, proves the tagged tree builds locally, then
# commits + tags + pushes, and finally bumps main to the next -SNAPSHOT. It never deploys and never
# publishes anything to a registry, so it needs no secrets — any maintainer can run it.
#
# Pushing the tag triggers .github/workflows/release.yml, which re-builds the tagged tree from a clean
# checkout and, only on success, publishes the GitHub Release. A tag without a published Release is
# NOT approved for deployment. See docs/release.md.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

ACTIONS_URL="https://github.com/OpenElementsLabs/open-crm/actions"

RELEASE="${1:-}"
NEXT="${2:-}"

if [[ -z "$RELEASE" || -z "$NEXT" ]]; then
  echo "usage: ./release.sh <release-version> <next-snapshot-version>" >&2
  echo "example: ./release.sh 1.11.0 1.12.0-SNAPSHOT" >&2
  exit 2
fi

TAG="v$RELEASE"
NOTES="docs/releases/$TAG.md"

die() {
  echo "release.sh: $1" >&2
  exit 1
}

# Returns 0 iff $1 > $2 as numeric A.B.C triples.
version_gt() {
  python3 - "$1" "$2" <<'PY'
import sys
a = tuple(int(x) for x in sys.argv[1].split("."))
b = tuple(int(x) for x in sys.argv[2].split("."))
sys.exit(0 if a > b else 1)
PY
}

# The revert safety net. Armed for the whole run; only reverts the two build files while we are still
# *before* the release commit. Once the commit exists the state is public and must not be rewound —
# the trap then prints the manual recovery step instead of silently undoing anything.
PRECOMMIT=1
cleanup() {
  local code=$?
  [[ "$code" -eq 0 ]] && return
  if [[ "$PRECOMMIT" -eq 1 ]]; then
    # Only announce/revert if a version change was actually applied (i.e. we failed in steps 1-3,
    # not in the preconditions where nothing was modified yet).
    if ! git diff --quiet -- backend/pom.xml frontend/package.json; then
      echo "release.sh: failed before the release commit — reverting version changes." >&2
      git checkout -- backend/pom.xml frontend/package.json 2>/dev/null || true
    fi
  else
    echo "release.sh: FAILED after the release commit/tag was created." >&2
    echo "  The tag $TAG may already be public — do NOT re-run this script for the same version." >&2
    echo "  If the follow-up bump did not push, finish it manually:  git push origin main" >&2
    echo "  Recovery (delete the tag and cut a new patch version) is in docs/release.md." >&2
  fi
}
trap cleanup EXIT

# --- Preconditions (checked before anything is modified) ----------------------

[[ "$RELEASE" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ||
  die "release version '$RELEASE' must be A.B.C (no suffix, no release candidates)"
[[ "$NEXT" =~ ^[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT$ ]] ||
  die "next version '$NEXT' must be A.B.C-SNAPSHOT"
version_gt "${NEXT%-SNAPSHOT}" "$RELEASE" ||
  die "next version '$NEXT' must be strictly greater than the release version '$RELEASE'"

[[ "$(git symbolic-ref --short HEAD)" == "main" ]] ||
  die "releases are cut from 'main' only (current branch: $(git symbolic-ref --short HEAD))"

if ! git diff --quiet || ! git diff --cached --quiet; then
  die "working tree is not clean — commit or stash first so nothing unrelated is swept into the version commit"
fi

git fetch --quiet origin main --tags
if [[ "$(git rev-parse @)" != "$(git rev-parse origin/main)" ]]; then
  die "local 'main' is not in sync with 'origin/main' — pull/push first"
fi

if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null 2>&1; then
  die "tag $TAG already exists locally — re-cutting a released version must be deliberate"
fi
if git ls-remote --exit-code --tags origin "$TAG" >/dev/null 2>&1; then
  die "tag $TAG already exists on origin"
fi

if [[ ! -f "$NOTES" ]] || ! grep -q '[^[:space:]]' "$NOTES"; then
  die "release notes '$NOTES' are missing or empty — write them (e.g. via /release-doc) before releasing"
fi

if ! docker info >/dev/null 2>&1; then
  die "the Docker daemon is not running — the backend tests and 'docker compose build' both need it"
fi

echo "release.sh: preconditions passed. Cutting $TAG, then bumping main to $NEXT."

# --- Step 1: set the release version in both build files ----------------------

echo "==> [1/6] setting version to $RELEASE"
(cd backend && ./mvnw -q versions:set -DnewVersion="$RELEASE" -DgenerateBackupPoms=false)
(cd frontend && pnpm version "$RELEASE" --no-git-tag-version --no-git-checks >/dev/null)
./check-versions.sh

# --- Step 2: no -SNAPSHOT may survive in the pom (would be an unreproducible release) --------------

echo "==> [2/6] asserting no -SNAPSHOT remains in backend/pom.xml"
if grep -q -- '-SNAPSHOT' backend/pom.xml; then
  echo "release.sh: a -SNAPSHOT string remains in backend/pom.xml after setting the release version." >&2
  echo "  That is a -SNAPSHOT dependency; a release must not depend on one. Release the dependency first." >&2
  exit 1
fi

# --- Step 3: full local build (the gate) --------------------------------------

echo "==> [3/6] full local build (backend verify, frontend test+build, docker compose build)"
(cd backend && ./mvnw clean verify)
(cd frontend && pnpm install --frozen-lockfile && pnpm test && pnpm build)
docker compose build

# --- Step 4: commit and push the release version ------------------------------

echo "==> [4/6] committing and pushing 'Version $RELEASE'"
git add backend/pom.xml frontend/package.json
git commit -q -m "Version $RELEASE"
PRECOMMIT=0 # from here the state is public; the trap must not revert
git push origin main

# --- Step 5: tag and push (this triggers release.yml) -------------------------

echo "==> [5/6] tagging and pushing $TAG (triggers the release workflow)"
git tag "$TAG"
git push origin "$TAG"

# --- Step 6: bump main to the next -SNAPSHOT ----------------------------------

echo "==> [6/6] bumping main to $NEXT"
(cd backend && ./mvnw -q versions:set -DnewVersion="$NEXT" -DgenerateBackupPoms=false)
(cd frontend && pnpm version "$NEXT" --no-git-tag-version --no-git-checks >/dev/null)
git add backend/pom.xml frontend/package.json
git commit -q -m "Version $NEXT"
git push origin main

cat <<EOF

release.sh: $TAG pushed and main bumped to $NEXT.
  Watch the release run:  $ACTIONS_URL
  The release is approved ONLY once that run is green and the GitHub Release is published.
  A tag without a published Release is not approved for deployment.
EOF
