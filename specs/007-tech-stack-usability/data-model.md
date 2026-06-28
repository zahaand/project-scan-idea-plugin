# Data Model: Sprint 7 — Tech Stack & Testing Usability Rework

**Date**: 2026-06-28
**Feature**: `007-tech-stack-usability`

---

## Model Layer (`model/`)

### Module + StructureInfo

**File**: `model/src/main/kotlin/.../model/StructureInfo.kt`

```kotlin
// Before
data class Module(
    val name: String,
    val declaredDependencies: List<Dependency> = emptyList(),
    val moduleDependencies: List<String> = emptyList(),
)

data class StructureInfo(
    val modules: List<Module> = emptyList(),
    val rootPackages: List<String> = emptyList(),    // REMOVED (FR-002)
    val packageSegments: List<String> = emptyList(), // REMOVED (FR-002)
)

// After
data class Module(
    val name: String,
    val declaredDependencies: List<Dependency> = emptyList(),
    val moduleDependencies: List<String> = emptyList(),
    val aggregator: String? = null,  // ADDED (FR-001): artifactId of containing aggregator, null if root
)

data class StructureInfo(
    val modules: List<Module> = emptyList(),
)
```

**Consumer impact**:
- `StructureInfoTest`: remove `rootPackages`/`packageSegments` assertions; add `aggregator` propagation tests.
- `StructureCollector`: remove `getPackageTree()` call; propagate `aggregator` from descriptor.
- `ScanResultRenderer.renderStructure()`: deleted (no Project Structure section, FR-015).
- `PromptGenerator.buildProjectStructureBlock()`: deleted (FR-015).

---

### TestInfo

**File**: `model/src/main/kotlin/.../model/TestInfo.kt`

```kotlin
// Before
data class TestFramework(val name: String, val version: String?)

data class TestInfo(
    val frameworks: List<TestFramework> = emptyList(),              // REMOVED (FR-010)
    val unknownTestDependencies: List<Dependency> = emptyList(),   // REMOVED (FR-010)
    val sourceRoots: List<String> = emptyList(),
    val namingSuffixes: List<String> = emptyList(),
    val coverageThreshold: Double? = null,
)

// After
// TestFramework class deleted entirely

data class TestInfo(
    val sourceRoots: List<String> = emptyList(),
    val namingSuffixes: List<String> = emptyList(),
    val coverageThreshold: Double? = null,
)
```

**Consumer impact**:
- `TestInfoTest`: remove framework/unknownDep assertions.
- `TestCollector`: remove entire framework detection loop and `matchFramework()` helper; keep sourceRoots, namingSuffixes, coverageThreshold collection.
- `ScanResultRenderer.renderTests()`: remove framework lines.
- `PromptGenerator.buildTestingBlock()`: remove framework lines.

---

### StackInfo

**File**: `model/src/main/kotlin/.../model/StackInfo.kt`

```kotlin
// Before
data class StackInfo(
    val dependencies: List<Dependency> = emptyList(),  // REMOVED: inverted view built from StructureInfo.modules
    val jdkVersion: String? = null,
    val languageLevel: String? = null,
    val buildSystem: BuildSystem? = null,
)

// After
data class StackInfo(
    val jdkVersion: String? = null,
    val languageLevel: String? = null,
    val buildSystem: BuildSystem? = null,
)
```

**Consumer impact**:
- `StackInfoTest`: remove dependency-related tests.
- `StackCollector`: remove `DependencyPort` constructor parameter; remove `aggregate()` and `pickMaxVersion()` helpers.
- `ScanResultRenderer.renderStack()`: no longer reads `info.dependencies`; calls `buildInvertedTechStack(modules, internalNames)` from shared instead.
- `PromptGenerator.buildTechStackBlock()`: same.

---

## Scan Layer (`scan/`)

### ModuleDescriptor + ModuleStructurePort

**File**: `scan/src/main/kotlin/.../scan/port/ModuleStructurePort.kt`

```kotlin
// Before
data class PackageTreeData(                     // DELETED (FR-002)
    val rootPackages: List<String>,
    val secondLevelSegments: List<String>,
)

data class ModuleDescriptor(
    val name: String,
    val externalDependencies: List<Dependency>,  // now direct-only for Maven (FR-003)
    val moduleDependencies: List<String>,
    val sourceRootPaths: List<String>,
    val hasSourceRoots: Boolean,
    // aggregator: String? ADDED (FR-001/FR-004)
)

interface ModuleStructurePort {
    fun getModules(): List<ModuleDescriptor>
    fun getPackageTree(): PackageTreeData   // REMOVED (FR-005)
}

// After
// PackageTreeData class deleted

data class ModuleDescriptor(
    val name: String,
    val externalDependencies: List<Dependency>,
    val moduleDependencies: List<String>,
    val sourceRootPaths: List<String>,
    val hasSourceRoots: Boolean,
    val aggregator: String? = null,   // ADDED
)

interface ModuleStructurePort {
    fun getModules(): List<ModuleDescriptor>
}
```

---

### TestInfoPort

**File**: `scan/src/main/kotlin/.../scan/port/TestInfoPort.kt`

```kotlin
// Before
interface TestInfoPort {
    fun getTestSourceRoots(): List<String>
    fun getTestScopedDependencies(): List<Dependency>  // REMOVED
    fun getCoverageThreshold(): Double?
    fun getTestClassNames(): List<String>
}

// After
interface TestInfoPort {
    fun getTestSourceRoots(): List<String>
    fun getCoverageThreshold(): Double?
    fun getTestClassNames(): List<String>
}
```

---

### Deleted Files

| File | Reason |
|---|---|
| `scan/port/DependencyPort.kt` | Interface no longer needed: per-module direct deps flow through ModuleStructurePort |
| `scan/adapter/IjDependencyAdapter.kt` | DependencyPort implementation removed |
| `scan/src/test/.../fake/FakeDependencyPort.kt` | Test fake for deleted port |
| `scan/src/test/.../adapter/IjDependencyAdapterTest.kt` | Tests for deleted adapter |

---

### IjModuleStructureAdapter Key Changes

**File**: `scan/src/main/kotlin/.../scan/adapter/IjModuleStructureAdapter.kt`

1. **`mavenModules()`** — direct-only deps (FR-003):
```kotlin
val declaredCoordinates = mp.mavenModel.dependencies
    .map { "${it.groupId}:${it.artifactId}" }
    .toSet()

val externalDeps = mp.dependencies
    .filter { "${it.groupId}:${it.artifactId}" in declaredCoordinates }
    .filter { "${it.groupId}:${it.artifactId}" !in moduleCoordinates }
    .map { it.toDependency() }
```

2. **Aggregator map** (FR-004) — built before iterating modules:
```kotlin
val aggregatorByDir = mutableMapOf<String, String>()
for (mp in mavenProjects) {
    val name = mp.mavenId.artifactId ?: mp.displayName
    for (relPath in mp.mavenModel.modules) {
        aggregatorByDir[File(mp.directory, relPath).canonicalPath] = name
    }
}
// Usage per module:
val aggregator = aggregatorByDir[File(mp.directory).canonicalPath]
```

3. **`getPackageTree()`** — method deleted (FR-005). The entire `collectPackageTree()` helper and `isValidJavaIdentifier()` helper may be removed if unused elsewhere.

4. **`gradleModules()`** — Gradle denylist (FR-006):
```kotlin
private val GRADLE_DENYLIST_EXACT = setOf(
    "org.objenesis:objenesis",
    "com.thoughtworks.paranamer:paranamer",
    "com.google.guava:listenablefuture",
    "com.google.guava:failureaccess",
    "com.google.j2objc:j2objc-annotations",
    "org.checkerframework:checker-qual",
    "aopalliance:aopalliance",
)
private const val GRADLE_DENYLIST_ASM_GROUP = "org.ow2.asm"

private fun isDenylisted(dep: Dependency): Boolean =
    dep.groupId == GRADLE_DENYLIST_ASM_GROUP ||
        "${dep.groupId}:${dep.artifactId}" in GRADLE_DENYLIST_EXACT
```

---

### ScanService

**File**: `scan/src/main/kotlin/.../scan/ScanService.kt`

- Remove `dependencyPort: DependencyPort` constructor parameter.
- Remove `StackCollector(buildSystemPort, dependencyPort)` — change to `StackCollector(buildSystemPort)`.
- Wiring in the root module's `ProjectScanPanel` or `ScanServiceFactory` must be updated accordingly.

---

## Shared Layer (`shared/`)

### New Types (OutputFormatters.kt)

```kotlin
// Intermediate: a module that carries a dependency at a specific version
data class CarrierModule(
    val name: String,
    val aggregator: String?,  // null = top-level/root module
)

// A group of carrier modules under one aggregator for a given version
data class AggregatorGroup(
    val aggregator: String?,          // null = ungrouped (top-level modules)
    val moduleNames: List<String>,    // sorted alphabetically
)

// One version entry within a TechEntry
data class VersionEntry(
    val version: String,
    val isUniform: Boolean,           // true when all modules share this single version
    val uniformModuleCount: Int,      // carrier count; only meaningful when isUniform = true
    val groups: List<AggregatorGroup>,// non-empty only when isUniform = false
)

// One coordinate in the inverted Tech Stack
data class TechEntry(
    val coordinate: String,           // "groupId:artifactId"
    val versions: List<VersionEntry>, // single entry if uniform; multiple if version discrepancy
)

// Full inverted Tech Stack
data class InvertedTechStack(
    val entries: List<TechEntry>,     // sorted alphabetically by coordinate
)
```

### New Functions (OutputFormatters.kt)

#### `buildInvertedTechStack`

```kotlin
fun buildInvertedTechStack(
    modules: List<Module>,
    internalModuleNames: Set<String>,
): InvertedTechStack
```

**Algorithm**:
1. For each `module` in `modules`, iterate `module.declaredDependencies` where `dep.artifactId !in internalModuleNames` and `dep.groupId:dep.artifactId !in internalModuleNames`.
2. Group carriers by `groupId:artifactId` → `Map<String, List<Pair<CarrierModule, String>>>` (carrier with version).
3. Per coordinate:
   - Collect all unique versions and their carrier sets.
   - If single version across all carriers → `VersionEntry(isUniform=true, uniformModuleCount=carriers.size, groups=emptyList())`.
   - If multiple versions → for each version, group carriers by `aggregator` → `List<AggregatorGroup>` (sorted: null aggregator last; within aggregator, modules sorted alphabetically).
4. Sort coordinates alphabetically.
5. Return `InvertedTechStack`.

#### `renderInvertedTechStack`

```kotlin
fun renderInvertedTechStack(
    stack: InvertedTechStack,
    buildSystem: BuildSystem?,
    jdkVersion: String?,
    languageLevel: String?,
): String
```

**Rendering format**:

Preamble lines (before dependency entries):
```
- Build System: MAVEN
- JDK Version: 21
- Language Level: 21
```

Uniform single-version entry:
```
- org.springframework:spring-core:6.1.0 [40 modules]
```

Multi-version entry:
```
- org.testcontainers:testcontainers
  - 1.19.8  groupA: module1, module2, module3
  - 1.20.1  groupB: module4, module5
```

Null aggregator (ungrouped modules):
```
- com.example:lib
  - 2.0  module-standalone, module-other
```

Returns `"not detected"` if `stack.entries.isEmpty()` and all preamble values are null.

### Removed from OutputFormatters.kt

| Symbol | Reason |
|---|---|
| `DependencyGroup` data class | Replaced by `InvertedTechStack` |
| `groupDependencies()` | Replaced by `buildInvertedTechStack()` |
| `deduplicateFrameworks()` | `TestInfo.frameworks` field deleted |
| `detectVersionDiscrepancies()` | Version discrepancies now inline in multi-version TechEntry |
| `renderVersionDiscrepancyLine()` | Same |
| `filterInternalDependencies()` | Filtering embedded in `buildInvertedTechStack()` |
| `VersionDiscrepancy` data class | No longer used |

**Retained**:
- `SourceRootTemplate` + `normalizeSourceRoots()` — used in Testing section rendering

---

## Prompt + UI Layer Changes

### Prompt (PromptGenerator.kt)

- `buildTechStackBlock(stack, structure)`: replace `groupDependencies(filterInternalDependencies(...))` with `buildInvertedTechStack(modules, internalNames)` + `renderInvertedTechStack(...)`. Pass `stack.data` metadata separately.
- `buildTestingBlock(tests)`: remove `deduplicateFrameworks(info.frameworks).forEach { ... }` lines.
- `buildProjectStructureBlock()`: deleted entirely.
- `generate()`: remove `PromptBlock("Project Structure", ...)` entry.

### UI (ScanResultRenderer.kt)

- `renderStack(info, internalModuleNames)`: signature changes — needs `List<Module>` or pre-built `InvertedTechStack` rather than `StackInfo.dependencies`. Refactor callers accordingly.
- `renderTests(info)`: remove framework lines.
- `renderStructure()`: deleted entirely.
- `render()`: remove the `section(titleKey = "section.Structure.title", ...)` entry.

---

## State Transition: ScanResult flow

```
Before Sprint 7:
  IjModuleStructureAdapter  →  ModuleStructurePort  →  StructureCollector  →  StructureInfo(modules, rootPackages, packageSegments)
  IjDependencyAdapter       →  DependencyPort       →  StackCollector      →  StackInfo(dependencies, jdkVersion, ...)
  IjTestInfoAdapter         →  TestInfoPort         →  TestCollector       →  TestInfo(frameworks, sourceRoots, ...)
  [prompt/ui] groupDependencies(StackInfo.dependencies) → flat grouped output

After Sprint 7:
  IjModuleStructureAdapter  →  ModuleStructurePort  →  StructureCollector  →  StructureInfo(modules[+aggregator])
  [DependencyPort DELETED]                           →  StackCollector      →  StackInfo(jdkVersion, languageLevel, buildSystem)
  IjTestInfoAdapter         →  TestInfoPort         →  TestCollector       →  TestInfo(sourceRoots, namingSuffixes, coverageThreshold)
  [prompt/ui] buildInvertedTechStack(StructureInfo.modules) → InvertedTechStack → renderInvertedTechStack()
```
