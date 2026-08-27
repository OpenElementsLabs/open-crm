#!/usr/bin/env bash
#
# check-versions.sh — the shared definition of "the application versions are correct".
#
# Used by CI (build.yml) and by release.sh, so the rules live in exactly one place. It reads the
# backend version from /project/version of backend/pom.xml (parsed as XML, NOT via mvnw, so it needs
# no JDK, no Maven and no network) and the frontend version from frontend/package.json, then asserts:
#
#   1. Equality      — both files carry the exact same version string (suffix included).
#   2. No downgrade  — the (A,B,C) triple is not below the highest existing vA.B.C tag; and a
#                      -SNAPSHOT must not sit on a version number that has already been released.
#   3. --expect <v>  — (optional) both files equal exactly <v>. Used by the release workflow.
#
# Exit codes: 0 = ok, 1 = a rule failed, 2 = usage error, 3 = a build file could not be parsed.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
POM="$ROOT/backend/pom.xml"
PKG="$ROOT/frontend/package.json"

usage() {
  echo "usage: check-versions.sh [--expect <version>]" >&2
  exit 2
}

EXPECT=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --expect)
      [[ $# -ge 2 ]] || usage
      EXPECT="$2"
      shift 2
      ;;
    -h | --help) usage ;;
    *)
      echo "check-versions: unknown argument '$1'" >&2
      usage
      ;;
  esac
done

# Reads /project/version (the direct child of the root, i.e. NOT <parent><version>) from a pom,
# namespace-agnostically. Exits 3 with a message naming the file on any parse problem or if the
# element is absent/empty — never falls back to an empty string that would compare equal to another.
read_pom_version() {
  python3 - "$1" <<'PY'
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1]
try:
    root = ET.parse(path).getroot()
except Exception as exc:  # noqa: BLE001 - any parse failure is a hard error
    sys.stderr.write(f"check-versions: cannot parse {path}: {exc}\n")
    sys.exit(3)

for child in root:
    if child.tag.split("}")[-1] == "version":
        text = (child.text or "").strip()
        if not text:
            sys.stderr.write(f"check-versions: empty <version> in {path}\n")
            sys.exit(3)
        print(text)
        sys.exit(0)

sys.stderr.write(f"check-versions: no /project/version element in {path}\n")
sys.exit(3)
PY
}

read_pkg_version() {
  python3 - "$1" <<'PY'
import json
import sys

path = sys.argv[1]
try:
    with open(path, encoding="utf-8") as handle:
        data = json.load(handle)
except Exception as exc:  # noqa: BLE001 - any parse failure is a hard error
    sys.stderr.write(f"check-versions: cannot parse {path}: {exc}\n")
    sys.exit(3)

version = data.get("version")
if not isinstance(version, str) or not version.strip():
    sys.stderr.write(f"check-versions: no usable \"version\" in {path}\n")
    sys.exit(3)
print(version.strip())
PY
}

BACKEND="$(read_pom_version "$POM")"
FRONTEND="$(read_pkg_version "$PKG")"

# Rule 1 — equality.
if [[ "$BACKEND" != "$FRONTEND" ]]; then
  echo "check-versions: version mismatch — backend/pom.xml=$BACKEND, frontend/package.json=$FRONTEND" >&2
  exit 1
fi
VERSION="$BACKEND"

# Rule 3 — optional exact expectation (used by the release workflow to compare against the tag).
if [[ -n "$EXPECT" && "$VERSION" != "$EXPECT" ]]; then
  echo "check-versions: expected $EXPECT but build files are $VERSION" >&2
  exit 1
fi

# Validate the shape before the numeric comparison.
BASE="${VERSION%-SNAPSHOT}"
if [[ ! "$BASE" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "check-versions: version '$VERSION' is not A.B.C or A.B.C-SNAPSHOT" >&2
  exit 1
fi
HAS_SNAPSHOT=0
[[ "$VERSION" == *-SNAPSHOT ]] && HAS_SNAPSHOT=1

# Rule 2 — no downgrade below the highest released tag. Legacy two-component tags (v1.4) are ignored;
# ordering is numeric, not lexicographic; if no vA.B.C tag exists the rule is skipped. All of that is
# done in one python pass to keep the numeric comparison correct and portable across bash/sort variants.
mapfile -t TAGS < <(git -C "$ROOT" tag --list 2>/dev/null || true)
python3 - "$BASE" "$HAS_SNAPSHOT" "${TAGS[@]}" <<'PY'
import re
import sys

base = sys.argv[1]
has_snapshot = sys.argv[2] == "1"
tags = sys.argv[3:]

released = []
for tag in tags:
    match = re.fullmatch(r"v(\d+)\.(\d+)\.(\d+)", tag)
    if match:
        released.append(tuple(int(part) for part in match.groups()))

if released:
    highest = max(released)
    current = tuple(int(part) for part in base.split("."))
    highest_str = ".".join(str(part) for part in highest)
    if current < highest:
        sys.stderr.write(
            f"check-versions: version {base} is lower than the released {highest_str}\n"
        )
        sys.exit(1)
    if current == highest and has_snapshot:
        sys.stderr.write(
            f"check-versions: -SNAPSHOT sits on the already-released version {highest_str} "
            "(forgotten post-release bump?)\n"
        )
        sys.exit(1)
PY

echo "check-versions: OK — $VERSION"
