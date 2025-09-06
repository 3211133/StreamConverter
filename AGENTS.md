# Repository Guidelines

## Project Structure & Module Organization
- `streamconverter-core/`: Core library (commands, controllers, utilities). Tests under `src/test/java`.
- `streamconverter-web/`: Spring Boot API wrapper. Entry: `com.streamConverter.StreamConverterWebApplication`.
- `streamconverter-tools/`: Benchmarks, analysis, and utilities.
- `streamconverter-examples/`: Runnable demos and usage examples.
- `docs/`: Architecture, testing, and reports. Root Gradle config in `build.gradle.kts`.

## Build, Test, and Development Commands
- Build all: `./gradlew build` (runs Spotless, PMD, SpotBugs, tests).
- Test all: `./gradlew test` or `./gradlew testAll` (multi‑module).
- Run web API: `./gradlew :streamconverter-web:bootRun`.
- Demos: `./gradlew runQuickStart`, `runContextDemo`, `runMDC`.
- Javadoc (all modules): `./gradlew javadocAll` → `build/docs/javadoc/index.html`.
- Benchmarks: `./gradlew benchmarkAll` (heavy; uses larger heap).

## Coding Style & Naming Conventions
- Java 17; Google Java Format via Spotless. Apply with `./gradlew spotlessApply`.
- Packages: `com.streamConverter.*`. Classes: PascalCase; methods/fields: camelCase; constants: UPPER_SNAKE_CASE.
- Keep `core` free of web-only dependencies; prefer composition over inheritance for commands.

## Testing Guidelines
- Framework: JUnit 5 (+ Spring Boot Test in `web`). Name tests `*Test` under `src/test/java`.
- Default skips network/benchmark tests. Enable networks: `./gradlew test -DskipNetworkTests=false`.
- Coverage: JaCoCo after `test` → `build/reports/jacoco/test/html/index.html`.
- Mutation testing (optional): `./gradlew pitest`.

## Commit & Pull Request Guidelines
- Conventional Commits. Examples:
  - `feat(core): add CSV streaming validator`
  - `fix(web): handle 400 on invalid path`
- PRs: clear description, linked issues, before/after notes (logs or sample requests), tests for new behavior, updated docs if APIs change. CI must pass.

## Security & Configuration Tips
- Do not commit secrets. Use `application.yml`/`application-prod.properties` with env overrides.
- Dependencies are pinned for CVE fixes (Netty, Spring, Logback). Avoid downgrades.
- Large-data runs are memory intensive; prefer provided benchmark tasks.

## Architecture Overview
- Pipeline + command pattern in `core`; `web` exposes HTTP endpoints over the same engine. See `docs/ARCHITECTURE.md` for diagrams and deeper rationale.

