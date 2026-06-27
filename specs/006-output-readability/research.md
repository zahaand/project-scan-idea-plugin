# Research: Output Readability for Large Projects

**Feature**: `006-output-readability` | **Date**: 2026-06-27

## Resolved Decisions

### 1. Shared utility placement

**Decision**: Create a new `:shared` Gradle submodule containing `OutputFormatters.kt`. `:shared` declares only `:model` as a dependency.

**Rationale**: FR-013 (added 2026-06-27 via checklist amendment) mandates the `:shared` module. FR-010 explicitly prohibits modifying `:model`; the earlier informal preference for placing utilities in `:model` was superseded by the FR-010 amendment. A dedicated `:shared` module is also cleaner architecturally — it keeps `:model` a pure data contract and `:shared` the pure-logic tier.

**Alternatives considered**:
- Add `OutputFormatters.kt` to `:model` — Rejected: FR-010 prohibits modifying `:model`; the FR-013 mandate settles the question.
- Duplicate logic in both `:prompt` and root project — Rejected by spec (SC-006 and NFR-001 forbid drift between consumers).
- Place in `:prompt`, have root project use it — Root project already depends on `:prompt` only for `ConstitutionPrompt`; mixing rendering utilities there blurs layer boundaries.

### 2. Utility function strategy: data transformation vs. text rendering

**Decision**: `OutputFormatters.kt` in `:shared` exposes **data-transformation functions only** — it returns structured types (`DependencyGroup`, `VersionDiscrepancy`, `SourceRootTemplate`). Text rendering stays inside each consumer.

**Rationale**: Keeps `:shared` format-agnostic. Both consumers produce byte-identical text strings (NFR-001, SC-006), but the "how to render" knowledge stays in the consumer layer where it belongs.

**Alternatives considered**:
- Shared text-rendering functions in `:shared` — Places format knowledge in the wrong layer; `:shared` should be concerned with data transformation only.
- Shared text-rendering in a new `:format` module — Over-engineered for the current scope.

### 3. Source-root normalization algorithm

**Decision**: Strip the longest common absolute prefix from absolute-path entries in `TestInfo.sourceRoots`; relative-path entries are used as-is. Then group identical templates with a count.

**Algorithm**:
1. If `sourceRoots` is empty, return empty.
2. Partition entries into absolute (starts with `/`) and relative.
3. Find the longest common directory prefix across the absolute entries only (split by `/`).
4. Strip that prefix from each absolute entry to get a relative template.
5. Relative entries become their own template unchanged.
6. Group all templates and count raw occurrences; sort by template string.

**Edge cases handled** (per FR-009 and spec edge cases):
- All roots already relative: absolute set is empty; no prefix is computed; each root is its own template.
- Single root: count = 1.
- Roots with no shared absolute prefix: each becomes its own template with count = 1.
- Mixed absolute/relative: absolute and relative paths handled independently (no cross-type prefix computation).
- `resolvedVersion == null` on `Dependency`: excluded from version-discrepancy detection; rendered without version in Tech Stack per-artifact format.

### 4. Version-discrepancy detection scope

**Decision**: An artifact appears in the discrepancy block only when it:
- Appears in ≥2 distinct modules, AND
- Has `resolvedVersion != null` in at least two modules with differing values.

A module with a single artifact declaration and no other module declaring it never produces a discrepancy.

**Rationale**: Matches spec edge cases exactly (FR-007, edge case for single-module artifacts).

### 5. FR-010 resolution

**Decision**: FR-010 is satisfied by using a new `:shared` module. FR-010 has been formally amended (2026-06-27) to state that `:shared`'s creation is additive and does not modify `:model` or `:scan`.

**Rationale**: The earlier informal interpretation ("adding a file to `:model` is not a modification") was invalidated by the checklist review (CHK032). The clean resolution is FR-013: all shared logic goes into `:shared`, which depends only on `:model`. Neither `:model` nor `:scan` is touched. No interpretation ambiguity remains.

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
