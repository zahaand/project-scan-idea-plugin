# Data Model: Output Readability for Large Projects

**Feature**: `006-output-readability` | **Date**: 2026-06-27 | **Updated**: 2026-06-27 (CHK032 — :shared module; monorepo refinements)

## Existing Model (unchanged)

```kotlin
// model/src/main/kotlin/dev/zahaand/projectscan/model/StackInfo.kt
data class Dependency(val groupId: String, val artifactId: String, val resolvedVersion: String?)
data class StackInfo(
    val dependencies: List<Dependency> = emptyList(),
    val jdkVersion: String? = null,
    val languageLevel: String? = null,
    val buildSystem: BuildSystem? = null,
)

// model/src/main/kotlin/dev/zahaand/projectscan/model/StructureInfo.kt
data class Module(
    val name: String,
    val declaredDependencies: List<Dependency> = emptyList(),
    val moduleDependencies: List<String> = emptyList(),
)
data class StructureInfo(
    val modules: List<Module> = emptyList(),
    val rootPackages: List<String> = emptyList(),
    val packageSegments: List<String> = emptyList(),
)

// model/src/main/kotlin/dev/zahaand/projectscan/model/TestInfo.kt
data class TestFramework(val name: String, val version: String?)
data class TestInfo(
    val frameworks: List<TestFramework> = emptyList(),
    val unknownTestDependencies: List<Dependency> = emptyList(),
    val sourceRoots: List<String> = emptyList(),
    val namingSuffixes: List<String> = emptyList(),
    val coverageThreshold: Double? = null,
)
```

## New Types: OutputFormatters.kt

Located at: `shared/src/main/kotlin/dev/zahaand/projectscan/shared/OutputFormatters.kt`

```kotlin
package dev.zahaand.projectscan.shared

/** A groupId bucket produced by groupDependencies(). */
data class DependencyGroup(
    val groupId: String,
    /** All artifacts in this group, preserving original order. */
    val artifacts: List<Dependency>,
    /**
     * Non-null when every artifact in the group has the same non-null resolvedVersion.
     * Null when versions are mixed or any artifact has resolvedVersion == null.
     * Single-artifact groups always have sharedVersion = null (no group header is emitted for a group of one).
     */
    val sharedVersion: String?,
)

/**
 * An artifact that appears at different versions across two or more modules.
 * versions maps moduleName → resolvedVersion (only non-null versions are included).
 */
data class VersionDiscrepancy(
    val groupId: String,
    val artifactId: String,
    val versions: Map<String, String>,   // moduleName → version
)

/** A recognised test-layout suffix extracted from a source-root path. */
data class SourceRootTemplate(
    val relativePath: String,
)
```

## Transformation Functions

All functions are **pure** (no side effects, no IntelliJ API calls).

### `filterInternalDependencies(deps: List<Dependency>, internalModuleNames: Set<String>): List<Dependency>`

- Returns `deps` with any `Dependency` removed whose `artifactId` is contained in `internalModuleNames`.
- Matching is exact, case-sensitive.
- If `internalModuleNames` is empty, returns `deps` unchanged.
- Called before `groupDependencies` when rendering Tech Stack. The caller derives `internalModuleNames` from `StructureInfo.modules.map { it.name }.toSet()`; if `StructureInfo` is Empty/Error, passes `emptySet()`.

### `groupDependencies(deps: List<Dependency>): List<DependencyGroup>`

- Groups `deps` by `groupId`, ordered lexicographically by `groupId` (per FR-001).
- Within each group, sets `sharedVersion` to the common `resolvedVersion` if ALL artifacts have the same non-null version; otherwise `null`.
- Artifacts with `resolvedVersion == null` within a group cause `sharedVersion` to be `null` for that group.

### `detectVersionDiscrepancies(modules: List<Module>): List<VersionDiscrepancy>`

- Collects `(groupId, artifactId) → Map<moduleName, resolvedVersion>` across all modules.
- Only retains entries where `resolvedVersion != null` in the module.
- Returns a `VersionDiscrepancy` for each coordinate that maps to **two or more distinct version strings** across different modules.
- Intra-module duplicate: when the same `(groupId, artifactId)` coordinate is declared more than once within a single module, the last-declared `resolvedVersion` for that coordinate within the module is used; earlier occurrences are discarded.
- Result is sorted by `groupId`, then `artifactId` for deterministic output.

### `renderVersionDiscrepancyLine(d: VersionDiscrepancy): String`

- Renders a single discrepancy entry in the dominant-version-plus-exceptions format.
- Computes the dominant version: the version appearing in the most `VersionDiscrepancy.versions` values. On a tie, the lexicographically smallest version string wins.
- Exceptions: all module-version pairs where the version is NOT the dominant version, sorted lexicographically by module name.
- Format: `groupId:artifactId → mostly <dominantVersion>, except {moduleName: version, ...}`
- Both consumers call this function (SC-006 preserved).

### `deduplicateFrameworks(frameworks: List<TestFramework>): List<TestFramework>`

- Returns frameworks with distinct `(name, version)` pairs, preserving order of first occurrence.
- A `null` version is distinct from any non-null version; `(name, null)` and `(name, "5.0")` are treated as two different entries.

### `normalizeSourceRoots(roots: List<String>): List<SourceRootTemplate>`

- Returns empty list if `roots` is empty.
- **Build-output filter (FR-009)**: excludes any path that contains a segment exactly equal to `target` or `build` (split on `/`, filter empty). This removes Maven and Gradle build-output paths before template extraction.
- **Tail-template extraction (FR-009)**: for each remaining path, checks if it ends with any recognized test-layout suffix (see FR-009 for the full list). If yes, records that suffix. If no recognized suffix matches and the path is absolute, applies LCP-then-module-strip fallback; relative unrecognized paths are used as-is.
- Collapses duplicates; returns unique recognized suffixes (and fallback tails) sorted lexicographically.
- No count is stored or emitted.

## State Transitions

No state transitions — all entities are immutable value objects produced on demand from `ScanResult`.

## Validation Rules

- `DependencyGroup.artifacts` is never empty (guaranteed by construction).
- `VersionDiscrepancy.versions` always has ≥ 2 entries (guaranteed by construction).
- `SourceRootTemplate.relativePath` is never empty (guaranteed by construction).
