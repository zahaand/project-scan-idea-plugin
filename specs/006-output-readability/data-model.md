# Data Model: Output Readability for Large Projects

**Feature**: `006-output-readability` | **Date**: 2026-06-27 | **Updated**: 2026-06-27 (CHK032 — :shared module)

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

/** A normalised source-root path template with its occurrence count. */
data class SourceRootTemplate(
    val relativePath: String,
    val count: Int,
)
```

## Transformation Functions

All functions are **pure** (no side effects, no IntelliJ API calls).

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

### `deduplicateFrameworks(frameworks: List<TestFramework>): List<TestFramework>`

- Returns frameworks with distinct `(name, version)` pairs, preserving order of first occurrence.
- A `null` version is distinct from any non-null version; `(name, null)` and `(name, "5.0")` are treated as two different entries.

### `normalizeSourceRoots(roots: List<String>): List<SourceRootTemplate>`

- Returns empty list if `roots` is empty.
- When there are no absolute-path entries, the LCP computation is skipped and all inputs are treated as relative templates directly.
- Computes the longest common absolute path prefix (split by `/`; prefix must end at a directory boundary).
- Strips the prefix (and leading `/`) from each root to obtain a relative template.
- Groups identical relative templates and counts occurrences.
- Returns sorted list by relative template string for deterministic output.

## State Transitions

No state transitions — all entities are immutable value objects produced on demand from `ScanResult`.

## Validation Rules

- `DependencyGroup.artifacts` is never empty (guaranteed by construction).
- `VersionDiscrepancy.versions` always has ≥ 2 entries (guaranteed by construction).
- `SourceRootTemplate.count` is always ≥ 1.
