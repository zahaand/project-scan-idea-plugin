# Data Model: ProjectScanModel

**Feature**: 001-model-data-contract  
**Package**: `dev.zahaand.projectscan.model`  
**Module**: `:model` (Gradle submodule)  
**Date**: 2026-06-12

All types are plain Kotlin data classes or enums. No IntelliJ Platform dependency. Default values on section types express the empty/not-detected state.

---

## Root Aggregate

```kotlin
data class ProjectScanModel(
    val stack: StackInfo,
    val codeStyle: CodeStyleInfo,
    val linters: LinterInfo,
    val tests: TestInfo,
    val structure: StructureInfo
)
```

**Invariant**: All five sections are always present (never null). Producers must supply all sections; use the section's companion `empty()` factory or the default constructor if a section was not detected.

---

## Section 1 — Stack

```kotlin
data class StackInfo(
    val dependencies: List<Dependency> = emptyList(),
    val jdkVersion: String? = null,
    val languageLevel: String? = null,
    val buildSystem: BuildSystem? = null
)
```

`dependencies` — flat union of all modules' declared external dependencies, deduplicated by `groupId + artifactId`. No transitive dependencies. When the same coordinate appears in multiple modules with different versions, the entry with the highest resolved version is kept (deduplication responsibility belongs to the scan layer).

```kotlin
data class Dependency(
    val groupId: String,
    val artifactId: String,
    val resolvedVersion: String?  // null for BOM-managed or unresolved
)
```

```kotlin
enum class BuildSystem { MAVEN, GRADLE }
```

**Empty state**: `StackInfo()` — empty dependency list, all nullable fields null.

---

## Section 2 — Code Style

```kotlin
data class CodeStyleInfo(
    val sources: List<StyleSource> = emptyList()
)
```

```kotlin
data class StyleSource(
    val type: StyleSourceType,
    val path: String  // project-relative path, e.g. "config/checkstyle/checkstyle.xml"
)
```

```kotlin
enum class StyleSourceType(val priority: Int) {
    CHECKSTYLE(1),
    SPOTLESS(1),
    PMD(1),
    EDITOR_CONFIG(2),
    IDE_CODE_STYLE(3);
}
```

**Priority rule**: lower `priority` integer = higher precedence. Consumers that need the highest-priority source call `sources.minByOrNull { it.type.priority }`. Multiple entries with the same type (e.g., two `.editorconfig` files at different paths) are allowed.

**Equal-rank linter sources**: `CHECKSTYLE`, `SPOTLESS`, and `PMD` intentionally share rank 1. The model does NOT impose any ordering among these three. When two or more same-rank linter sources are present, the consumer (e.g., the Sprint 4 prompt generator) is responsible for conflict resolution — the model never invents a hierarchy the project itself did not define.

**Scope**: `StyleSource` models file-based configuration only. Inline style configuration embedded in build scripts is out of scope for Sprint 1.

**Empty state**: `CodeStyleInfo()` — empty source list.

---

## Section 3 — Linters

```kotlin
data class LinterInfo(
    val activeRules: List<ActiveRule> = emptyList()
)
```

```kotlin
data class ActiveRule(
    val ruleId: String,   // tool-specific rule identifier, e.g. "LineLength"
    val tool: String,     // tool name, e.g. "Checkstyle", "PMD"
    val severity: RuleSeverity,
    val breaksBuild: Boolean  // true = failOnViolation / false = ignoreFailures
)
```

```kotlin
enum class RuleSeverity { ERROR, WARNING, INFO }
```

**Empty state**: `LinterInfo()` — empty rule list. Represents a project with no linter wired into the build.

---

## Section 4 — Tests

```kotlin
data class TestInfo(
    val frameworks: List<TestFramework> = emptyList(),
    val sourceRoots: List<String> = emptyList(),  // project-relative paths
    val namingPattern: String? = null,             // glob or regex, e.g. "**/*Test.kt"
    val coverageThreshold: Double? = null          // percentage 0.0–100.0; null = JaCoCo absent
)
```

```kotlin
data class TestFramework(
    val name: String,       // e.g. "JUnit 5", "Mockito", "AssertJ"
    val version: String?    // null if BOM-managed or unresolved
)
```

**Empty state**: `TestInfo()` — empty framework list, empty source roots, null pattern, null threshold.

---

## Section 5 — Structure

```kotlin
data class StructureInfo(
    val modules: List<Module> = emptyList(),
    val packageOrganisation: PackageOrganisation? = null,
    val rootPackages: List<String> = emptyList()  // project-wide aggregation across all modules
)
```

```kotlin
data class Module(
    val name: String,
    val declaredDependencies: List<Dependency> = emptyList(),  // external Maven/Gradle coords
    val moduleDependencies: List<String> = emptyList()         // sibling module names
)
```

`declaredDependencies` — external (Maven/Gradle) dependencies specific to this module. Uses the same `Dependency` type as `StackInfo`.  
`moduleDependencies` — names of sibling modules this module depends on (inter-module links).

```kotlin
enum class PackageOrganisation { BY_LAYER, BY_FEATURE }
```

`rootPackages` — the union of root packages detected across ALL modules in the project. This is a project-wide aggregate, consistent with the `StackInfo.dependencies` aggregation design (Q2 clarification). Per-module root package breakdown is explicitly out of MVP scope (post-MVP tech debt).

**Empty state**: `StructureInfo()` — empty module list, null organisation, empty root packages.

---

## Type inventory

| Type | Kind | Section |
|------|------|---------|
| `ProjectScanModel` | data class | root |
| `StackInfo` | data class | Stack |
| `Dependency` | data class | Stack, Structure |
| `BuildSystem` | enum | Stack |
| `CodeStyleInfo` | data class | Code Style |
| `StyleSource` | data class | Code Style |
| `StyleSourceType` | enum (with priority) | Code Style |
| `LinterInfo` | data class | Linters |
| `ActiveRule` | data class | Linters |
| `RuleSeverity` | enum | Linters |
| `TestInfo` | data class | Tests |
| `TestFramework` | data class | Tests |
| `StructureInfo` | data class | Structure |
| `Module` | data class | Structure |
| `PackageOrganisation` | enum | Structure |

**Total**: 15 types (14 from spec + `PackageOrganisation` which was in spec — count matches).

---

## Additive-stability contract

The types above constitute the stable core of the model contract. Starting Sprint 2:
- New fields may be added to any data class (additive, non-breaking).
- New enum constants may be added to any enum (additive, non-breaking).
- Existing field names, types, and semantics MUST NOT change.
- No type may be removed without a major constitution amendment.
