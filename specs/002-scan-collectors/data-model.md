# Data Model: Scan — Project Facts Collectors

**Phase**: 1 | **Date**: 2026-06-13 | **Plan**: [plan.md](plan.md)

---

## 1. Changes to `:model` Module

### 1.1 `ScanResult.kt` — NEW FILE

```kotlin
package dev.zahaand.projectscan.model

sealed class SectionResult<out T> {
    data class Ok<out T>(val data: T) : SectionResult<T>()
    data object Empty : SectionResult<Nothing>()
    data class Error(val cause: String? = null) : SectionResult<Nothing>()
}

data class ScanResult(
    val stack: SectionResult<StackInfo>,
    val codeStyle: SectionResult<CodeStyleInfo>,
    val linters: SectionResult<LinterInfo>,
    val tests: SectionResult<TestInfo>,
    val structure: SectionResult<StructureInfo>,
)
```

**Rationale**: Lives in `:model` (not `:scan`) so future consumers (`:prompt`, `:ui`) can depend
on it without depending on the `:scan` module, preserving Constitution Principle I.

`Empty` — no data found (section applicable but project has nothing configured).
`Error` — collector ran but failed (e.g., malformed config file); MUST NOT be treated as empty.

---

### 1.2 `StructureInfo.kt` — MODIFIED

**Remove**: `packageOrganisation: PackageOrganisation?` and the `PackageOrganisation` enum.
**Add**: `packageSegments: List<String>` — second-level package paths (e.g., `"com.example.web"`).

```kotlin
package dev.zahaand.projectscan.model

// PackageOrganisation enum REMOVED (was inference; replaced by raw data)

data class Module(
    val name: String,
    val declaredDependencies: List<Dependency> = emptyList(),
    val moduleDependencies: List<String> = emptyList(),
)

data class StructureInfo(
    val modules: List<Module> = emptyList(),
    val rootPackages: List<String> = emptyList(),
    val packageSegments: List<String> = emptyList(), // second-level segments only, e.g. "com.example.web"; root packages map to rootPackages
)
```

**Migration note**: `packageOrganisation` was nullable with no known consumers in Sprint 1 tests
that assert on its value (tests only construct the field; no assertions against it). Removal is
safe for Sprint 2.

---

### 1.3 `LinterInfo.kt` — MODIFIED

**Change**: `breaksBuild: Boolean` → `breaksBuild: Boolean?`
- `true` = violations fail the build
- `false` = violations do not fail the build
- `null` = could not be determined (Gradle standard TAPI limitation)

```kotlin
package dev.zahaand.projectscan.model

enum class RuleSeverity { ERROR, WARNING, INFO }

data class ActiveRule(
    val ruleId: String,
    val tool: String,
    val severity: RuleSeverity,
    val breaksBuild: Boolean?,  // null = "not detected"
)

data class LinterInfo(
    val activeRules: List<ActiveRule> = emptyList(),
    val toolsWithUnresolvableConfig: List<String> = emptyList(),
)
```

---

### 1.4 `TestInfo.kt` — MODIFIED

**Add**: `unknownTestDependencies: List<Dependency>` — test-scoped dependencies that did not
match any entry in the known framework registry (FR-012).

```kotlin
package dev.zahaand.projectscan.model

data class TestFramework(
    val name: String,
    val version: String?,
)

data class TestInfo(
    val frameworks: List<TestFramework> = emptyList(),
    val unknownTestDependencies: List<Dependency> = emptyList(),
    val sourceRoots: List<String> = emptyList(),
    val namingSuffixes: List<String> = emptyList(),
    val coverageThreshold: Double? = null,
)
```

---

## 2. Port Interfaces (`:scan` module)

Ports are narrow read-only interfaces in `scan/src/main/.../scan/port/`. They return only types
from `:model` or plain Kotlin types — never IntelliJ platform types. This keeps collectors
platform-free and fakes trivial to write.

### 2.1 `BuildSystemPort`

```kotlin
interface BuildSystemPort {
    fun getBuildSystem(): BuildSystem?
    fun getJdkVersion(): String?
    /** Returns effective language level per module (module name → level string, e.g. "17"). */
    fun getModuleLanguageLevels(): Map<String, String>
}
```

### 2.2 `DependencyPort`

```kotlin
interface DependencyPort {
    /** Returns explicitly declared (non-transitive) dependencies per module. */
    fun getModuleDependencies(): Map<String, List<Dependency>>
}
```

### 2.3 `StyleSourcePort`

```kotlin
interface StyleSourcePort {
    fun findStyleSources(): List<StyleSource>
}
```

### 2.4 `LinterPort` + `LinterToolDescriptor`

```kotlin
data class LinterToolDescriptor(
    val toolName: String,         // canonical: "checkstyle" | "pmd"
    val configFilePath: String?,  // project-relative; null if path not found
    val breaksBuild: Boolean?,    // null = not detected
)

interface LinterPort {
    fun getAppliedLinterTools(): List<LinterToolDescriptor>
}
```

### 2.5 `LinterConfigParser` + `ParsedRule`

```kotlin
data class ParsedRule(
    val ruleId: String,
    val severity: RuleSeverity,
)

interface LinterConfigParser {
    /** Parses the config file at the given absolute path and returns its rules. */
    fun parseRules(absoluteConfigPath: String): List<ParsedRule>
}
```

Two production implementations: `CheckstyleConfigParser` and `PmdConfigParser`, both in
`adapter/`. `ScanService` or `LinterCollector` selects the right parser based on `toolName`.

### 2.6 `TestInfoPort`

```kotlin
interface TestInfoPort {
    fun getTestSourceRoots(): List<String>           // project-relative paths
    fun getTestScopedDependencies(): List<Dependency>
    fun getCoverageThreshold(): Double?              // null if reporting-only or unreadable
    fun getTestClassNames(): List<String>            // simple class names without extension
}
```

### 2.7 `ModuleStructurePort` + `ModuleDescriptor` + `PackageTreeData`

```kotlin
data class PackageTreeData(
    val rootPackages: List<String>,        // e.g. ["com.example"]
    val secondLevelSegments: List<String>, // e.g. ["com.example.web", "com.example.domain"]
)

data class ModuleDescriptor(
    val name: String,
    val externalDependencies: List<Dependency>,
    val moduleDependencies: List<String>,  // by module name
    val sourceRootPaths: List<String>,     // absolute paths to main source roots
    val hasSourceRoots: Boolean,
)

interface ModuleStructurePort {
    fun getModules(): List<ModuleDescriptor>
    fun getPackageTree(): PackageTreeData
}
```

---

## 3. `ScanResult` Entity Relationships

```
ScanResult
├── stack:     SectionResult<StackInfo>
│                └── Ok(StackInfo(dependencies, jdkVersion, languageLevel, buildSystem))
├── codeStyle: SectionResult<CodeStyleInfo>
│                └── Ok(CodeStyleInfo(sources: List<StyleSource>))
├── linters:   SectionResult<LinterInfo>
│                └── Ok(LinterInfo(activeRules: List<ActiveRule>))
│                    ActiveRule has: ruleId, tool, severity, breaksBuild: Boolean?
├── tests:     SectionResult<TestInfo>
│                └── Ok(TestInfo(frameworks, unknownTestDependencies, sourceRoots,
│                                namingSuffixes: List<String>, coverageThreshold))
└── structure: SectionResult<StructureInfo>
                 └── Ok(StructureInfo(modules, rootPackages, packageSegments))
                     Module has: name, declaredDependencies, moduleDependencies
```

Each `SectionResult` is one of: `Ok(data)` | `Empty` | `Error(cause)`.

---

## 4. Collector Logic Summary

| Collector | Port(s) used | Key logic |
|---|---|---|
| `StackCollector` | `DependencyPort`, `BuildSystemPort` | Aggregate per-module deps → dedup by coord; take max version; take max language level |
| `CodeStyleCollector` | `StyleSourcePort` | Return discovered sources as-is; empty if none |
| `LinterCollector` | `LinterPort`, `LinterConfigParser` | Per tool: config null or unparseable → tool added to `toolsWithUnresolvableConfig`; section stays `Ok`; else parse → `ActiveRule` list with `breaksBuild` denormalized |
| `TestCollector` | `TestInfoPort` | Split test-scope deps into known frameworks vs unknowns; read JaCoCo threshold |
| `StructureCollector` | `ModuleStructurePort` | Map `ModuleDescriptor` → `Module`; get `PackageTreeData` for `rootPackages`+`packageSegments` |

---

## 5. Model Change Impact on Existing Tests

Sprint 1 model tests in `:model/test/` need minor updates:

| File | Change required |
|---|---|
| `StructureInfoTest.kt` | Remove any assertions on `packageOrganisation`; add assertions on `packageSegments` |
| `LinterInfoTest.kt` | Update `ActiveRule` construction to `breaksBuild = true/false/null` |
| `TestInfoTest.kt` | Add construction of `unknownTestDependencies` field |
| All other test files | No changes needed |
