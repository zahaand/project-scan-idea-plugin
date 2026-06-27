# Research: Output Readability for Large Projects

**Feature**: `006-output-readability` | **Date**: 2026-06-27

## Resolved Decisions

### 1. Shared utility placement

**Decision**: Add a new file `OutputFormatters.kt` to `:model` submodule.

**Rationale**: The spec clarification (2026-06-27) explicitly names `:model` as the preferred location. `:model` is already the shared contract between all consumers; pure data-transformation functions (no rendering) naturally live there. No outgoing dependencies are introduced, so the module stays cohesive.

**Alternatives considered**:
- New `:shared` submodule — Clean, but adds build complexity (new `build.gradle.kts`, `settings.gradle.kts` entry) for a small set of functions. The spec says `:model` is preferred.
- Duplicate logic in both `:prompt` and root project — Rejected by spec (SC-006 forbids drift between consumers).
- Place in `:prompt`, have root project use it — Root project already imports `ConstitutionPrompt` from `:prompt`, so this works technically, but puts rendering-adjacent logic in the wrong layer.

### 2. Utility function strategy: data transformation vs. text rendering

**Decision**: `OutputFormatters.kt` exposes **data-transformation functions only** — it returns structured types (`DependencyGroup`, `VersionDiscrepancy`, `SourceRootTemplate`). Text rendering stays inside each consumer.

**Rationale**: Keeps `:model` format-agnostic (the constitution's intent). Both consumers produce identical text strings, fulfilling SC-006, but the "how to render" knowledge stays in the consumer layer.

**Alternatives considered**:
- Shared text-rendering functions in `:model` — Violates the separation of concerns: model shouldn't know about output format.
- Shared text-rendering in a new `:shared` module — Possible but over-engineered for the current scope.

### 3. Source-root normalization algorithm

**Decision**: Strip the longest common absolute prefix from `TestInfo.sourceRoots`, then group identical relative paths with a count.

**Algorithm**:
1. If `sourceRoots` is empty, return empty.
2. Find the longest common path prefix across all roots (split by `/` to avoid partial directory matches).
3. Subtract the prefix from each root to get a relative template.
4. Group by relative template and count.

**Edge cases handled**:
- All roots already relative (no common absolute prefix): common prefix is empty string; each root is used as-is.
- Single root: count = 1.
- Roots with no shared prefix: each becomes its own template with count = 1.
- `resolvedVersion == null` on `Dependency`: excluded from version-discrepancy detection; rendered without version in Tech Stack.

### 4. Version-discrepancy detection scope

**Decision**: An artifact appears in the discrepancy block only when it:
- Appears in ≥2 distinct modules, AND
- Has `resolvedVersion != null` in at least two modules with differing values.

A module with a single artifact declaration and no other module declaring it never produces a discrepancy.

**Rationale**: Matches spec edge cases exactly (FR-007, edge case for single-module artifacts).

### 5. FR-010 conflict resolution

**Decision**: Interpret FR-010 ("`:scan` and `:model` MUST NOT be modified") as applying to existing model data class files. Adding a new utility file to `:model` is a controlled extension, not a modification, and is explicitly sanctioned by the spec's Clarifications.

**Rationale**: The Clarifications section was written after FR-010 and directly addresses the placement question. The spec's Assumptions section restates this as fact ("will be extracted to `:model`"). Treating FR-010 as an absolute block on any file addition would contradict the spec's own authoritative resolution.

### 6. Test strategy for ScanResultRenderer

**Decision**: Add smoke tests to the root project's test source set (`src/test/kotlin/dev/zahaand/projectscan/ui/`). One test class per changed section. Each test asserts:
- A key positive signal is present (e.g., group header `groupId:* @ version` appears).
- A key negative signal is absent (e.g., the version is NOT repeated per artifact when uniform).

**Rationale**: SC-005 and the spec agreement say "one smoke test per changed section"; full unit-test parity with `:prompt` is out of scope (per spec agreement).

**Risk**: The root project is the IntelliJ platform plugin host. Pure unit tests (no platform startup) must be careful not to call IntelliJ platform APIs. `ScanResultRenderer` itself uses `ProjectScanBundle.message(...)` for titles and empty-state strings. Tests must either:
- Mock/stub `ProjectScanBundle` — complex, requires platform context.
- Only test the private render functions indirectly by inspecting the `UiSection.body` value — simpler.
- Or restructure so the private render functions are testable without platform bundle calls.

**Resolution**: The private `renderStack`, `renderTests`, `renderStructure` functions only take model types and return `String?` — they do NOT call `ProjectScanBundle`. The `section()` wrapper calls the bundle for titles/empty-state. So smoke tests can call `ScanResultRenderer` with a fabricated `ScanResult` and `ConstitutionPrompt`, then inspect `UiSection.body` on the relevant section entry (found by index). This avoids needing a platform environment.
