#!/usr/bin/env bash
#
# Behaviour tests for check-versions.sh. Each case builds a throwaway git repository with specific
# build-file versions and tags, copies check-versions.sh into it (the script resolves its repo root
# from its own location), runs it, and asserts on exit code and message. No network, no JDK, no Maven.
#
# Run: ./test/check-versions.test.sh   (exit 0 = all passed)
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECK_VERSIONS="$SCRIPT_DIR/../check-versions.sh"

PASS=0
FAIL=0

# make_repo <backend-version> <frontend-version> [tag ...]
# Prints the path of a fresh temp repo. A single commit carries every tag.
make_repo() {
  local backend="$1" frontend="$2"
  shift 2
  local dir
  dir="$(mktemp -d)"
  mkdir -p "$dir/backend" "$dir/frontend"
  cat >"$dir/backend/pom.xml" <<POM
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.open-elements</groupId>
    <artifactId>java-parent</artifactId>
    <version>1.2.1</version>
  </parent>
  <artifactId>open-crm-backend</artifactId>
  <version>${backend}</version>
</project>
POM
  cat >"$dir/frontend/package.json" <<PKG
{ "name": "open-crm-frontend", "private": true, "version": "${frontend}" }
PKG
  cp "$CHECK_VERSIONS" "$dir/check-versions.sh"
  git -C "$dir" init -q
  git -C "$dir" -c user.email=t@t -c user.name=t commit -q --allow-empty -m init
  local tag
  for tag in "$@"; do
    git -C "$dir" tag "$tag"
  done
  echo "$dir"
}

# assert_exit <description> <expected-code> <repo-dir> [extra check-versions args...]
assert_exit() {
  local desc="$1" expected="$2" dir="$3"
  shift 3
  local out code
  out="$(cd "$dir" && ./check-versions.sh "$@" 2>&1)"
  code=$?
  if [[ "$code" -eq "$expected" ]]; then
    PASS=$((PASS + 1))
  else
    FAIL=$((FAIL + 1))
    echo "FAIL: $desc — expected exit $expected, got $code"
    echo "      output: $out"
  fi
  rm -rf "$dir"
}

# assert_message <description> <expected-code> <substring> <repo-dir> [args...]
assert_message() {
  local desc="$1" expected="$2" needle="$3" dir="$4"
  shift 4
  local out code
  out="$(cd "$dir" && ./check-versions.sh "$@" 2>&1)"
  code=$?
  if [[ "$code" -eq "$expected" && "$out" == *"$needle"* ]]; then
    PASS=$((PASS + 1))
  else
    FAIL=$((FAIL + 1))
    echo "FAIL: $desc — expected exit $expected with '$needle'; got $code"
    echo "      output: $out"
  fi
  rm -rf "$dir"
}

# --- Version consistency ------------------------------------------------------

assert_exit "identical snapshot versions pass" 0 \
  "$(make_repo 1.11.0-SNAPSHOT 1.11.0-SNAPSHOT v1.10.0)"

assert_message "diverging versions fail and name both files" 1 "frontend/package.json=0.1.0" \
  "$(make_repo 1.11.0-SNAPSHOT 0.1.0 v1.10.0)"

assert_exit "today's broken 0.1.0 state is rejected by equality" 1 \
  "$(make_repo 0.1.0-SNAPSHOT 0.1.0 v1.10.0)"

assert_exit "release commit passes despite no -SNAPSHOT" 0 \
  "$(make_repo 1.11.0 1.11.0 v1.10.0)"

assert_message "a version below the highest tag is rejected" 1 "lower than the released 1.10.0" \
  "$(make_repo 1.9.0-SNAPSHOT 1.9.0-SNAPSHOT v1.10.0)"

assert_message "a -SNAPSHOT on an already-released number is rejected" 1 "already-released version 1.11.0" \
  "$(make_repo 1.11.0-SNAPSHOT 1.11.0-SNAPSHOT v1.11.0)"

assert_exit "release version equal to the highest tag is accepted" 0 \
  "$(make_repo 1.11.0 1.11.0 v1.11.0)"

# --- Tag selection ------------------------------------------------------------

# Legacy two-component tags are ignored: 1.9.0-SNAPSHOT is below v1.10.0, not above v1.4.
assert_message "legacy two-component tags are ignored when picking the highest" 1 "released 1.10.0" \
  "$(make_repo 1.9.0-SNAPSHOT 1.9.0-SNAPSHOT v0.1 v1.2 v1.4 v1.9.0 v1.10.0)"

# Numeric, not lexicographic: with v1.9.0 and v1.10.0, 1.9.5-SNAPSHOT is below the highest (v1.10.0).
# A lexicographic sort would pick v1.9.0 and wrongly accept 1.9.5-SNAPSHOT.
assert_message "tag ordering is numeric not lexicographic" 1 "released 1.10.0" \
  "$(make_repo 1.9.5-SNAPSHOT 1.9.5-SNAPSHOT v1.9.0 v1.10.0)"

assert_exit "a repository without release tags skips the downgrade rule" 0 \
  "$(make_repo 0.1.0-SNAPSHOT 0.1.0-SNAPSHOT)"

# --- --expect -----------------------------------------------------------------

assert_exit "--expect matching the build files passes" 0 \
  "$(make_repo 1.11.0 1.11.0 v1.10.0)" --expect 1.11.0

assert_message "--expect not matching the build files fails" 1 "expected 1.11.0 but build files are 1.11.0-SNAPSHOT" \
  "$(make_repo 1.11.0-SNAPSHOT 1.11.0-SNAPSHOT v1.10.0)" --expect 1.11.0

# --- Malformed input ----------------------------------------------------------

malformed_pom_repo() {
  local dir
  dir="$(make_repo 1.11.0-SNAPSHOT 1.11.0-SNAPSHOT v1.10.0)"
  # Remove the <version> element entirely.
  cat >"$dir/backend/pom.xml" <<'POM'
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <artifactId>open-crm-backend</artifactId>
</project>
POM
  echo "$dir"
}

assert_message "a pom without /project/version fails clearly (not a silent empty pass)" 3 "no /project/version" \
  "$(malformed_pom_repo)"

# --- Summary ------------------------------------------------------------------

echo "----------------------------------------"
echo "check-versions.test.sh: $PASS passed, $FAIL failed"
[[ "$FAIL" -eq 0 ]]
