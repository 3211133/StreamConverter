#!/usr/bin/env bash
set -euo pipefail

# Creates GitHub issues for known inconsistencies in this repository.
# Prerequisites:
# - GitHub CLI installed: https://cli.github.com/
# - Authenticated: gh auth login
# Usage:
#   ./scripts/create-issues.sh

req() { command -v "$1" >/dev/null 2>&1 || { echo "Error: $1 not found"; exit 1; }; }
req gh

echo "Creating issues in repo: $(gh repo view --json nameWithOwner -q .nameWithOwner)" || true

# Ensure required labels exist (idempotent)
ensure_label() {
  local name="$1"; local color="$2"; shift 2
  local desc="$*"
  if ! gh label list --limit 200 | grep -E "^${name}[[:space:]]" >/dev/null 2>&1; then
    gh label create "$name" --color "$color" --description "$desc" >/dev/null
  fi
}

ensure_label build        FFDD57 "Build configuration and tasks"
ensure_label dependencies 0E8A16 "Dependency updates and alignment"
ensure_label architecture 0366D6 "Architecture and module boundaries"
ensure_label refactor     FBCA04 "Refactoring and naming"
ensure_label documentation 0075CA "Docs and README"
ensure_label cleanup      C5DEF5 "Cleanup and repository hygiene"
ensure_label tech-debt    D93F0B "Technical debt tracking"

create() {
  local title="$1"; shift
  local label="$1"; shift
  local body="$*"
  echo "- $title [$label]"
  gh issue create \
    --title "$title" \
    --label "$label,tech-debt" \
    --body "$body"
}

create "Root build.gradle: duplicate application block" build \
"Root build.gradle.kts defines application { ... } twice. Consolidate into a single block and keep mainClass configuration in one place to avoid confusion.\n\nAcceptance:\n- Single application block remains\n- mainClass set once, documented"

create "Root JavaExec tasks reference examples incorrectly" build \
"Tasks runQuickStart/runDemo/etc reference classes in :streamconverter-examples but use root classpath. Move these tasks into :streamconverter-examples or set classpath to that module. Also fix runDemo main class (demo vs examples).\n\nAcceptance:\n- Tasks live in examples or have correct classpath\n- Main class names verified"

create "Root build: implementation deps at root are ineffective" build \
"Root project declares implementation(...) although it has no source sets. Move version alignment to dependency management (BOM/constraints) under subprojects/allprojects.\n\nAcceptance:\n- Common versions managed via BOM or constraints\n- No stray implementation deps at root"

create "Netty/Spring comments vs versions out of sync" dependencies \
"Comments claim compatibility with Netty 4.1.x while dependencies pin 4.2.4.Final. Align comments and versions; ensure reactor-netty matches chosen Netty and Spring versions.\n\nAcceptance:\n- Comments match actual versions\n- Verified build/runtime compatibility"

create "Boot BOM vs direct version pins (spring-web etc.)" dependencies \
"Direct versions for Spring artifacts (e.g., spring-web) may conflict with Spring Boot's dependency management. Prefer BOM-managed versions unless a CVE override is required and documented.\n\nAcceptance:\n- Direct pins removed or justified\n- Build uses coherent dependency graph"

create "web module mixes starter-web and webflux" architecture \
"streamconverter-web depends on both spring-boot-starter-web (Servlet/Tomcat) and webflux (Reactive/Netty). Choose one stack or clearly separate profiles.\n\nAcceptance:\n- Single web stack by default\n- If both retained, docs explain routing/runtime impact"

create "Package naming uses CamelCase segment (com.streamConverter)" refactor \
"Java packages under src use com.streamConverter with CamelCase. Standard convention is lowercase (com.streamconverter). Plan a package rename with refactors and migration guide.\n\nAcceptance:\n- Agreed naming policy\n- Tracked refactor plan (possibly incremental)"

create "Inconsistent directory name: impl/charaCode" refactor \
"Directory path contains 'charaCode'; unclear spelling/intent. Rename to 'charcode' or a precise domain name aligned with functionality.\n\nAcceptance:\n- Directory/package renamed\n- Imports updated; build passes"

create "README: example paths/links incorrect" documentation \
"README links reference examples under 'examples/...' but actual paths are 'streamconverter-examples/src/...'. Update links and samples.\n\nAcceptance:\n- All example links resolve\n- Add a quick table of example entry classes"

create "Javadoc output locations are split" build \
"tasks.javadoc outputs to docs/javadoc while javadocAll writes to build/docs/javadoc. Unify to a single location and update docs/CI accordingly.\n\nAcceptance:\n- One canonical output path\n- CI (Pages) points to the same"

create "Library module contains Boot-specific resources" architecture \
"streamconverter-core includes application.yml and logback-spring.xml. Library modules should avoid Boot-specific resources; move to web module or tests.\n\nAcceptance:\n- Core free of Boot-only config\n- Web/test modules carry runtime configs"

create "Stray compiled artifacts committed (.class, logs)" cleanup \
"Repository root contains compiled classes (e.g., debug_test.class, test_typed_paths.class) and logs directories. Remove and update .gitignore to prevent reintroduction.\n\nAcceptance:\n- Files removed\n- .gitignore updated and validated"

echo "Done. Review newly created issues above."
