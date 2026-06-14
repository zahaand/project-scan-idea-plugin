# Tasks: Scan — Project Facts Collectors

**Input**: Design documents from `specs/002-scan-collectors/`
**Prerequisites**: plan.md ✅ | spec.md ✅ | data-model.md ✅ | research.md ✅ | quickstart.md ✅

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story. Tests are included because FR-019 and SC-005 mandate unit test coverage via port fakes.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no inter-task dependencies)
- **[Story]**: Which user story this task belongs to (US1–US5)

## Path Conventions

- Model source: `model/src/main/kotlin/dev/zahaand/projectscan/model/`
- Model tests: `model/src/test/kotlin/dev/zahaand/projectscan/model/`
- Scan source: `scan/src/main/kotlin/dev/zahaand/projectscan/scan/`
- Scan tests: `scan/src/test/kotlin/dev/zahaand/projectscan/scan/`

---

## Phase 1: Setup (Gradle Module Scaffolding)

**Purpose**: Create the `:scan` Gradle submodule so the compiler can resolve all subsequent files.

- [X] T001 Register `:scan` in `settings.gradle.kts` via `include(":scan")`
- [X] T002 Create `scan/build.gradle.kts` with dependencies on `:model`, IntelliJ Platform SDK (External System API, JavaPlugin, MavenIntegration), and JUnit 5 for tests (per plan.md Technical Context)
- [X] T003 Create the source directory tree: `scan/src/main/kotlin/dev/zahaand/projectscan/scan/{port,collector,adapter}/` and `scan/src/test/kotlin/dev/zahaand/projectscan/scan/{fake,collector}/`

**Checkpoint**: `./gradlew :scan:compileKotlin` resolves without "module not found" errors.

---

## Phase 2: Foundational (Model Changes + Port Interfaces)

**Purpose**: Land all `:model` changes and define all port interfaces before any collector work begins. No collector or adapter can be written until these contracts exist.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Model Changes

- [X] T004 [P] Create `model/src/main/kotlin/dev/zahaand/projectscan/model/ScanResult.kt` — `sealed class SectionResult<out T>` with `Ok`, `Empty`, `Error` variants, and `data class ScanResult` with five `SectionResult` fields (stack, codeStyle, linters, tests, structure) per data-model.md §1.1
- [X] T005 [P] Update `model/src/main/kotlin/dev/zahaand/projectscan/model/StructureInfo.kt` — remove `packageOrganisation: PackageOrganisation?` field and `PackageOrganisation` enum; add `rootPackages: List<String>` and `packageSegments: List<String>` fields (both required — T038/T039 rely on `rootPackages` for raw root package data; T038 maps `PackageTreeData.secondLevelSegments` → `packageSegments`); update `model/src/test/kotlin/dev/zahaand/projectscan/model/StructureInfoTest.kt` to remove assertions on removed field and add `rootPackages` and `packageSegments` construction
- [X] T006 [P] Update `model/src/main/kotlin/dev/zahaand/projectscan/model/LinterInfo.kt` — widen `breaksBuild: Boolean` to `breaksBuild: Boolean?` in `ActiveRule`; add `toolsWithUnresolvableConfig: List<String> = emptyList()` to `LinterInfo`; update `model/src/test/kotlin/dev/zahaand/projectscan/model/LinterInfoTest.kt` to construct `ActiveRule` with null/non-null `breaksBuild` and `LinterInfo` with `toolsWithUnresolvableConfig`
- [X] T007 [P] Update `model/src/main/kotlin/dev/zahaand/projectscan/model/TestInfo.kt` — add `unknownTestDependencies: List<Dependency> = emptyList()` to `TestInfo`; update `model/src/test/kotlin/dev/zahaand/projectscan/model/TestInfoTest.kt` to include `unknownTestDependencies` in construction

### Port Interfaces

- [X] T008 [P] Create `scan/src/main/kotlin/dev/zahaand/projectscan/scan/port/BuildSystemPort.kt` — `interface BuildSystemPort { fun getBuildSystem(): BuildSystem?; fun getModuleLanguageLevels(): Map<String, String>; fun getJdkVersion(): String? }` per data-model.md §2.1
- [X] T009 [P] Create `scan/src/main/kotlin/dev/zahaand/projectscan/scan/port/DependencyPort.kt` — `interface DependencyPort { fun getModuleDependencies(): Map<String, List<Dependency>> }` per data-model.md §2.2
- [X] T010 [P] Create `scan/src/main/kotlin/dev/zahaand/projectscan/scan/port/StyleSourcePort.kt` — `interface StyleSourcePort { fun findStyleSources(): List<StyleSource> }` per data-model.md §2.3
- [X] T011 [P] Create `scan/src/main/kotlin/dev/zahaand/projectscan/scan/port/LinterPort.kt` — `data class LinterToolDescriptor` (toolName, configFilePath, breaksBuild) and `interface LinterPort { fun getAppliedLinterTools(): List<LinterToolDescriptor> }` per data-model.md §2.4
- [X] T012 [P] Create `scan/src/main/kotlin/dev/zahaand/projectscan/scan/port/LinterConfigParser.kt` — `data class ParsedRule` (ruleId, severity) and `interface LinterConfigParser { fun parseRules(absoluteConfigPath: String): List<ParsedRule> }` per data-model.md §2.5
- [X] T013 [P] Create `scan/src/main/kotlin/dev/zahaand/projectscan/scan/port/TestInfoPort.kt` — `interface TestInfoPort { fun getTestSourceRoots(): List<String>; fun getTestScopedDependencies(): List<Dependency>; fun getCoverageThreshold(): Double?; fun getTestClassNames(): List<String> }` per data-model.md §2.6
- [X] T014 [P] Create `scan/src/main/kotlin/dev/zahaand/projectscan/scan/port/ModuleStructurePort.kt` — `data class PackageTreeData` (rootPackages, secondLevelSegments), `data class ModuleDescriptor` (name, externalDependencies, moduleDependencies, sourceRootPaths, hasSourceRoots), and `interface ModuleStructurePort { fun getModules(): List<ModuleDescriptor>; fun getPackageTree(): PackageTreeData }` per data-model.md §2.7

### Service Skeleton

- [X] T015 Create `scan/src/main/kotlin/dev/zahaand/projectscan/scan/ScanService.kt` — constructor accepting exactly seven parameters: `BuildSystemPort`, `DependencyPort`, `StyleSourcePort`, `LinterPort`, `TestInfoPort`, `ModuleStructurePort` (six port interfaces) plus `linterConfigParsers: Map<String, LinterConfigParser>`; the map is **received as a constructor parameter, not constructed inside ScanService** — the caller supplies it at the call site (see T041); `fun scan(): ScanResult` with five individual `try { collector.collect() } catch (e: Exception) { SectionResult.Error(e.message) }` blocks, one per section (FR-021 error isolation); collector bodies are `TODO()` stubs until Phase 3+

**Checkpoint**: `./gradlew :model:test :scan:compileKotlin` passes — all model tests green; scan module compiles with port interfaces and service skeleton.

---

## Phase 3: User Story 1 — Technology Stack Discovery (Priority: P1) 🎯 MVP

**Goal**: Collect build system, JDK/language level, and deduplicated declared dependencies across all modules. Max-version aggregation with ComparableVersion semantics.

**Independent Test**: `StackCollectorTest` with `FakeBuildSystemPort` and `FakeDependencyPort` — no IntelliJ fixtures required.

### Fakes

- [X] T016 [P] [US1] Create `scan/src/test/kotlin/dev/zahaand/projectscan/scan/fake/FakeBuildSystemPort.kt` — constructor takes `buildSystem: BuildSystem?`, `moduleLevels: Map<String, String>`, and `jdkVersion: String? = null`; implements `BuildSystemPort`
- [X] T017 [P] [US1] Create `scan/src/test/kotlin/dev/zahaand/projectscan/scan/fake/FakeDependencyPort.kt` — constructor takes `moduleMap: Map<String, List<Dependency>>` and `error: Exception? = null` (used by the partial-failure sub-scenario in StackCollectorTest); implements `DependencyPort`

### Collector + Tests

- [X] T018 [US1] Implement `scan/src/main/kotlin/dev/zahaand/projectscan/scan/collector/StackCollector.kt` — aggregate per-module deps: dedup by groupId+artifactId, max version via ComparableVersion (lexicographic fallback); resolve per-module language levels (null → project default from BuildSystemPort); take max across modules; return `SectionResult.Ok(StackInfo(...))` when any fact present, `SectionResult.Empty` when no build system and no modules
- [X] T019 [US1] Write `scan/src/test/kotlin/dev/zahaand/projectscan/scan/collector/StackCollectorTest.kt` — covers: 5-dep single-module project; multi-module same-dep max-version selection; zero-dependency project (Empty); max language level aggregation; null version preserved; Gradle vs Maven build system identification

### Adapters

- [X] T020 [P] [US1] Implement `scan/src/main/kotlin/dev/zahaand/projectscan/scan/adapter/IjBuildSystemAdapter.kt` — `getBuildSystem()` via `ExternalSystemUtil.getDefaultExternalSystemId()` or `MavenProjectsManager.isMavenizedProject()`; `getModuleLanguageLevels()` via `LanguageLevelModuleExtension.getInstance(module).languageLevel` per module (null = inherits project default from `LanguageLevelProjectExtension`) per research.md R-002/R-003; `getJdkVersion()` via project-level SDK (`ProjectRootManager.getInstance(project).projectSdk?.versionString`), falling back to max across modules, then `null` if undetectable
- [X] T021 [P] [US1] Implement `scan/src/main/kotlin/dev/zahaand/projectscan/scan/adapter/IjDependencyAdapter.kt` — for Maven: `MavenProjectsManager.getInstance(project).projects` → `MavenProject.getDependencies()`; for Gradle: `ExternalProjectDataCache` + `ExternalSystemApiUtil.findAll(moduleNode, LibraryDependencyData.KEY)`; returns only declared (non-transitive) entries per research.md R-001

**Checkpoint**: `./gradlew :scan:test --tests "*.StackCollectorTest"` passes. User Story 1 is fully functional via fakes.

---

## Phase 4: User Story 2 — Code Style Sources Discovery (Priority: P2)

**Goal**: Discover all style config files (Checkstyle, Spotless, PMD, .editorconfig, IDE settings) and return project-relative paths per the Style Source Recognition Table.

**Independent Test**: `CodeStyleCollectorTest` with `FakeStyleSourcePort` — no IntelliJ fixtures required.

### Fake

- [X] T022 [P] [US2] Create `scan/src/test/kotlin/dev/zahaand/projectscan/scan/fake/FakeStyleSourcePort.kt` — constructor takes `sources: List<StyleSource>`; implements `StyleSourcePort`

### Collector + Tests

- [X] T023 [US2] Implement `scan/src/main/kotlin/dev/zahaand/projectscan/scan/collector/CodeStyleCollector.kt` — delegates to `StyleSourcePort.findStyleSources()`; returns `SectionResult.Ok` when any source found, `SectionResult.Empty` when none
- [X] T024 [US2] Write `scan/src/test/kotlin/dev/zahaand/projectscan/scan/collector/CodeStyleCollectorTest.kt` — covers: project with three source types (all appear); no style files (Empty); multiple .editorconfig files (all collected); Spotless without external file (not present); assert that a StyleSource with an XML-named path is returned as-is without parsing its contents (enforces FR-007 — the collector MUST NOT parse style config file contents for style facts); per-collector partial-failure sub-scenario: style source port partially succeeds (returns subset of sources or partial data) → section is Ok with whatever was returned, not Error

### Adapter

- [X] T025 [US2] Implement `scan/src/main/kotlin/dev/zahaand/projectscan/scan/adapter/IjStyleSourceAdapter.kt` — `ProjectUtil.guessProjectDir()` as base; discovery uses two distinct mechanisms: (1) well-known exact paths (`config/checkstyle/`, `config/pmd/`) checked as exact-path lookups regardless of nesting depth; (2) name-pattern matching (`checkstyle*`, `pmd*`) via VirtualFile descent up to max depth 2; `findFileByRelativePath(".editorconfig")` + recursive subdirectory walk for nested .editorconfig; `.idea/codeStyles/` enumeration for all child files; Spotless: presence of a task like `spotlessApply` in External System model MAY indicate Spotless is applied, but a `StyleSource(SPOTLESS, path)` is emitted ONLY when a standalone non-build config file also exists (task name alone is insufficient, consistent with FR-020 and CHK008)

**Checkpoint**: `./gradlew :scan:test --tests "*.CodeStyleCollectorTest"` passes.

---

## Phase 5: User Story 3 — Active Linter Rules Discovery (Priority: P2)

**Goal**: Detect applied Checkstyle/PMD tools (per build system), parse their config XML files for rules and severities, denormalize `breaksBuild` onto each `ActiveRule`.

**Independent Test**: `LinterCollectorTest` with `FakeLinterPort` and `FakeLinterConfigParser` — no IntelliJ fixtures required.

### Fakes

- [X] T026 [P] [US3] Create `scan/src/test/kotlin/dev/zahaand/projectscan/scan/fake/FakeLinterPort.kt` — constructor takes `tools: List<LinterToolDescriptor>`; implements `LinterPort`
- [X] T027 [P] [US3] Create `scan/src/test/kotlin/dev/zahaand/projectscan/scan/fake/FakeLinterConfigParser.kt` — constructor takes `rulesByPath: Map<String, List<ParsedRule>>`; implements `LinterConfigParser`; throws for paths not in map

### Config Parsers

- [X] T028 [P] [US3] Implement `scan/src/main/kotlin/dev/zahaand/projectscan/scan/adapter/CheckstyleConfigParser.kt` — DOM parse via `DocumentBuilderFactory`; walk `<module>` elements recursively; skip `Checker` and `TreeWalker` containers; `ruleId` = `name` attribute; severity from `<property name="severity">`, inherit from nearest ancestor, default `INFO`; mapping: `"error"→ERROR`, `"warning"→WARNING`, `"info"/"ignore"→INFO` per research.md R-006
- [X] T029 [P] [US3] Implement `scan/src/main/kotlin/dev/zahaand/projectscan/scan/adapter/PmdConfigParser.kt` — DOM parse; enumerate `<rule>` elements; `ruleId` = `ref` attribute; `<priority>` → severity: 1–2=`ERROR`, 3=`WARNING`, 4–5=`INFO`; absent priority → `INFO` (per FR-009 and research.md R-007)

### Collector + Tests

- [X] T030 [US3] Implement `scan/src/main/kotlin/dev/zahaand/projectscan/scan/collector/LinterCollector.kt` — constructor takes `linterPort: LinterPort` and `linterConfigParsers: Map<String, LinterConfigParser>` (key = toolName e.g. "checkstyle", "pmd"); for each `LinterToolDescriptor`: there are **two paths that both funnel to "applied but unresolvable"** — (1) `configFilePath == null` (config not found in project tree); (2) `parseRules()` throws (config found but unparseable/malformed XML); in both cases: add toolName to `LinterInfo.toolsWithUnresolvableConfig`, contribute zero `ActiveRule` entries for that tool, and keep the section `Ok` (not `Error`); if no parser is registered for the tool's toolName, treat it as unresolvable in the same way; **wrap the `parseRules(absoluteConfigPath)` call in `try-catch(Exception)`** to ensure path (2) is handled; denormalize `breaksBuild` onto each `ActiveRule`; multiple configs for same tool collected independently (all rules merged); `SectionResult.Ok` if any tool applied, `SectionResult.Empty` if none
- [X] T031 [US3] Write `scan/src/test/kotlin/dev/zahaand/projectscan/scan/collector/LinterCollectorTest.kt` — covers: 10-rule Checkstyle config (all rules with severities and breaksBuild); undetectable breaksBuild (null); no tools (Empty); Spotless tool descriptor in list produces zero ActiveRule entries (explicitly assert `activeRules` is empty for a Spotless-only input — enforces FR-011); Gradle Checkstyle applied (breaksBuild=null); **assert both "applied but unresolvable" paths independently and verify they produce identical outcomes**: (a) `configFilePath == null` (config not found) → toolName in `toolsWithUnresolvableConfig`, zero `ActiveRule` entries, section `Ok`; (b) `parseRules()` throws (malformed config) → toolName in `toolsWithUnresolvableConfig`, zero `ActiveRule` entries, section `Ok`; no parser registered for tool (toolName in `toolsWithUnresolvableConfig`, zero ActiveRule entries); multiple configs merged; per-collector partial-failure sub-scenario: linter port reads successfully but config parser throws → that tool's name in `toolsWithUnresolvableConfig`, remaining tools unaffected, section is Ok

### Adapter

- [X] T032 [US3] Implement `scan/src/main/kotlin/dev/zahaand/projectscan/scan/adapter/IjLinterAdapter.kt` — Maven: `MavenProject.findPlugin("org.apache.maven.plugins", "maven-checkstyle-plugin")` + `findPlugin("org.apache.maven.plugins", "maven-pmd-plugin")`; `breaksBuild` from `getConfigurationElement()?.getChildText("failOnViolation")` (not `failsOnError` per CHK013); Gradle: scan task names for `checkstyleMain`/`checkstyleTest`/`pmdMain`/`pmdTest` in ExternalProjectDataCache; Gradle `breaksBuild = null` always per research.md R-005; Wiring of `linterConfigParsers` into `LinterCollector` happens in T041; this adapter is unaffected.

**Checkpoint**: `./gradlew :scan:test --tests "*.LinterCollectorTest"` passes.

---

## Phase 6: User Story 4 — Test Infrastructure Discovery (Priority: P3)

**Goal**: Match test-scoped dependencies against the Known Test Framework Registry; record unknown test deps; collect test source roots, naming suffixes, and JaCoCo threshold.

**Independent Test**: `TestCollectorTest` with `FakeTestInfoPort` — no IntelliJ fixtures required.

### Fake

- [X] T033 [P] [US4] Create `scan/src/test/kotlin/dev/zahaand/projectscan/scan/fake/FakeTestInfoPort.kt` — constructor takes `testSourceRoots: List<String>`, `testScopedDependencies: List<Dependency>`, `coverageThreshold: Double? = null`, `testClassNames: List<String> = emptyList()`; implements `TestInfoPort` (all four methods: `getTestSourceRoots()`, `getTestScopedDependencies()`, `getCoverageThreshold()`, `getTestClassNames()`)

### Collector + Tests

- [X] T034 [US4] Implement `scan/src/main/kotlin/dev/zahaand/projectscan/scan/collector/TestCollector.kt` — contains `KNOWN_TEST_FRAMEWORKS` registry (13 entries per spec FR-012 table); for each test-scoped dep: prefix-match groupId (exact artifactId match for `junit:junit`); matched → `TestFramework(name, resolvedVersion)`; unmatched → `unknownTestDependencies`; collect test source root paths from `TestInfoPort.getTestSourceRoots()`; derive raw `namingSuffixes` from `TestInfoPort.getTestClassNames()` using the closed set of recognised suffix tokens: `Test`, `Tests`, `IT`, `ITCase`, `Spec`; for each class name, if it ends with one of these tokens record that token verbatim; if it does not end with any known token ignore it (no heuristic camelCase inference); distinct recorded tokens form `namingSuffixes` (raw facts, classification left to the LLM); JaCoCo threshold via `TestInfoPort.getCoverageThreshold()` (null when reporting-only or unreadable per FR-014); `SectionResult.Ok` if any test root or dep found, `SectionResult.Empty` if neither
- [X] T035 [US4] Write `scan/src/test/kotlin/dev/zahaand/projectscan/scan/collector/TestCollectorTest.kt` — covers: JUnit 5 + Mockito + AssertJ detected; JaCoCo threshold 0.8; JaCoCo reporting-only (null); no test deps (Empty); unknown test dep recorded; resolved version used; Maven scope=test vs Gradle testImplementation; multiple coexisting suffixes (Test, IT, Spec) all captured in `namingSuffixes` without normalization (enforces FR-013); suffix extraction scenarios: `FooBarIT` → `["IT"]`, `OrderServiceSpec` → `["Spec"]`, a non-test class name → contributes nothing to `namingSuffixes`, multiple coexisting suffixes all captured; per-collector partial-failure sub-scenario: test source roots readable but test-scoped dependency read fails → section is Ok with empty frameworks and `unknownTestDependencies`, test roots still populated

### Adapter

- [X] T036 [US4] Implement `scan/src/main/kotlin/dev/zahaand/projectscan/scan/adapter/IjTestInfoAdapter.kt` — `getTestSourceRoots()` via `ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.TEST_SOURCE)` → project-relative paths **relative to `ProjectUtil.guessProjectDir()`** (the same anchor used by `IjStyleSourceAdapter`, ensuring consistent path roots across sections); `getTestScopedDependencies()` filters test-scope entries from ExternalProjectDataCache / MavenProjectsManager (Maven: `scope=test`; Gradle: testImplementation/testCompileOnly/testRuntimeOnly); `getCoverageThreshold()` via Maven `jacoco-maven-plugin` execution `check` goal `<minimum>` element per research.md R-010 (Gradle: return `null`); `getTestClassNames()` — for each test source root, enumerate VirtualFile children recursively, **filter to `.java` and `.kt` files only** (exclude non-source resources such as `SomeTest.xml`, `SomeTest.json` that would otherwise inflate `namingSuffixes`), and collect simple file names (without extension) so the collector can extract naming suffixes without filesystem access

**Checkpoint**: `./gradlew :scan:test --tests "*.TestCollectorTest"` passes.

---

## Phase 7: User Story 5 — Module Structure and Package Layout (Priority: P3)

**Goal**: Map each module to its build-system identifier, external deps, and inter-module links; collect two-level package tree in dotted Java notation.

**Independent Test**: `StructureCollectorTest` with `FakeModuleStructurePort` — no IntelliJ fixtures required.

### Fake

- [X] T037 [P] [US5] Create `scan/src/test/kotlin/dev/zahaand/projectscan/scan/fake/FakeModuleStructurePort.kt` — constructor takes `modules: List<ModuleDescriptor>` and `packageTree: PackageTreeData`; implements `ModuleStructurePort`

### Collector + Tests

- [X] T038 [US5] Implement `scan/src/main/kotlin/dev/zahaand/projectscan/scan/collector/StructureCollector.kt` — map each `ModuleDescriptor` → `Module(name, declaredDependencies, moduleDependencies.distinct())`; deduplicate `moduleDependencies` by name; source-less modules included with empty package contribution; `packageSegments` from `PackageTreeData.secondLevelSegments`; `SectionResult.Ok` if module list non-empty, `SectionResult.Empty` if empty
- [X] T039 [US5] Write `scan/src/test/kotlin/dev/zahaand/projectscan/scan/collector/StructureCollectorTest.kt` — covers: three-module dependency graph A→B→C; 3-dep module; package segments in dotted notation; single-module project; source-less module included; duplicate inter-module dep deduplicated; empty module list (Empty); verify root packages land in `rootPackages` not `packageSegments`; per-collector partial-failure sub-scenario: module list readable but package tree read fails → section is Ok with modules populated and empty packageSegments/rootPackages

### Adapter

- [X] T040 [US5] Implement `scan/src/main/kotlin/dev/zahaand/projectscan/scan/adapter/IjModuleStructureAdapter.kt` — `getModules()`: iterate `ModuleManager.getInstance(project).modules`; per module get external deps via ExternalProjectDataCache and inter-module deps via `ModuleDependencyData`; module name = build-system identifier (Gradle `:name`, Maven `name`); `getPackageTree()`: for each module's main source roots, walk VirtualFile children (skip `.`-prefixed and non-Java-identifier dirs) → rootPackages; recurse one more level → secondLevelSegments (full dotted path); aggregate + dedup across modules per research.md R-008

**Checkpoint**: `./gradlew :scan:test --tests "*.StructureCollectorTest"` passes.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Wire `ScanService`, verify error isolation across collectors, and confirm the quickstart example compiles.

- [X] T041 Complete `scan/src/main/kotlin/dev/zahaand/projectscan/scan/ScanService.kt` — replace `TODO()` stubs with actual collector instantiation and calls; each collector wrapped in `try/catch(Exception)` returning `SectionResult.Error(e.message)` on failure; all other collectors always execute regardless of sibling failure (FR-021); ScanService passes the `linterConfigParsers` map it received via its constructor **directly to `LinterCollector`** — it does **not** construct the map internally; the production caller that instantiates ScanService (plugin wiring/factory code) supplies `mapOf("checkstyle" to CheckstyleConfigParser(), "pmd" to PmdConfigParser())` as the constructor argument
- [X] T042 [P] Write `scan/src/test/kotlin/dev/zahaand/projectscan/scan/ScanServiceTest.kt` — covers: all five collectors succeed (all Ok); one collector throws (that section Error, others Ok); empty project (all sections Empty); partial failure in stack collector (Ok with partial data, not Error)
- [X] T043 [P] Verify the production wiring from `quickstart.md` compiles: instantiate all `Ij*Adapter` classes with a stub `Project` parameter in a compilation-only test or example to confirm constructor signatures match port interfaces
- [X] T043a Apply detekt and ktlint to all modules — add `id("io.gitlab.arturbosch.detekt")` and `id("org.jlleitschuh.gradle.ktlint")` plugins via a `subprojects { }` block in root `build.gradle.kts` (versions are already registered in `settings.gradle.kts` pluginManagement as detekt 1.23.8 and ktlint 12.2.0); configure a `detekt { }` block with baseline configuration; run `./gradlew detekt ktlintCheck` locally and **fix all reported violations** in existing `:model` and `:scan` source files — do **not** silence violations via a baseline file; **CI enforcement (fail-on-violation) is deferred to Sprint 6** as tech debt

**Checkpoint**: `./gradlew :model:test :scan:test` passes — all 8+ collector test classes green, model tests green.

---

## Phase 9: Adapter Integration Tests (constitution §Testing compliance)

**Purpose**: Satisfy constitution requirement — "Platform-dependent code (`scan` component and platform adapters): IntelliJ Platform Test Framework". T043 (Phase 8) is compilation-only; this phase adds one functional happy-path integration test per adapter that benefits from precise, declarative fixture data. Full Gradle External System import coverage is deferred as tech debt (see plan.md Governance).

**Note**: Use `LightProjectDescriptor` or `HeavyIdeaTestFixtureFactory` per adapter needs. Maven adapter tests take priority (precise declarative data via pom.xml fixture).

- [ ] T044 [P] Write `scan/src/test/kotlin/dev/zahaand/projectscan/scan/adapter/IjBuildSystemAdapterTest.kt` — IntelliJ Platform fixture: Maven-ized project → `getBuildSystem()` returns `MAVEN`; `getModuleLanguageLevels()` returns the configured source compatibility level
- [ ] T045 [P] Write `scan/src/test/kotlin/dev/zahaand/projectscan/scan/adapter/IjDependencyAdapterTest.kt` — IntelliJ Platform fixture: Maven project with 2 declared dependencies → `getModuleDependencies()` returns exactly those 2 entries; no transitive entries appear
- [ ] T046 [P] Write `scan/src/test/kotlin/dev/zahaand/projectscan/scan/adapter/IjStyleSourceAdapterTest.kt` — IntelliJ Platform fixture: project with `.editorconfig` and `.idea/codeStyles/` → `findStyleSources()` returns both; project with neither → empty list
- [ ] T047 [P] Write `scan/src/test/kotlin/dev/zahaand/projectscan/scan/adapter/IjLinterAdapterTest.kt` — IntelliJ Platform fixture (Maven): project with `maven-checkstyle-plugin` in `<plugins>` → tool recorded as applied with `breaksBuild` from `failOnViolation` (not `failsOnError` — CHK013: `failsOnError` tests Checkstyle's internal error handling, not build failure on rule violations); plugin in `<pluginManagement>` only → not applied; Gradle adapter: smoke-level compilation check only (Gradle TAPI import not feasible in light fixture)
- [ ] T048 [P] Write `scan/src/test/kotlin/dev/zahaand/projectscan/scan/adapter/IjTestInfoAdapterTest.kt` — IntelliJ Platform fixture: module with test source root → `getTestSourceRoots()` returns project-relative path; `getCoverageThreshold()` returns correct value from Maven jacoco plugin configuration; `getTestClassNames()` returns file names from test source root
- [ ] T049 [P] Write `scan/src/test/kotlin/dev/zahaand/projectscan/scan/adapter/IjModuleStructureAdapterTest.kt` — IntelliJ Platform fixture: two-module project → `getModules()` returns both; `getPackageTree()` returns correct root packages and second-level segments in dotted notation

**Checkpoint**: `./gradlew :scan:test --tests "*.Ij*AdapterTest"` passes.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — **BLOCKS all user story phases**
- **Phases 3–7 (User Stories)**: All depend on Phase 2; stories are otherwise independent of each other and can proceed in parallel
- **Phase 8 (Polish)**: Depends on all story phases completing

### User Story Independence

| Story | Depends on | Independent of |
|---|---|---|
| US1 (Stack) | Phase 2 only | US2, US3, US4, US5 |
| US2 (CodeStyle) | Phase 2 only | US1, US3, US4, US5 |
| US3 (Linters) | Phase 2 only | US1, US2, US4, US5 |
| US4 (Tests) | Phase 2 only | US1, US2, US3, US5 |
| US5 (Structure) | Phase 2 only | US1, US2, US3, US4 |

### Within Each User Story

1. Fakes (parallel) → Collector → Collector Tests → Adapter

---

## Parallel Execution Examples

### Phase 2 (all can run together after Phase 1)
```
T004 ScanResult.kt
T005 StructureInfo.kt changes
T006 LinterInfo.kt changes
T007 TestInfo.kt changes
T008 BuildSystemPort.kt
T009 DependencyPort.kt
T010 StyleSourcePort.kt
T011 LinterPort.kt
T012 LinterConfigParser.kt
T013 TestInfoPort.kt
T014 ModuleStructurePort.kt
```
Then sequentially: T015 ScanService.kt skeleton

### Phase 5 (Linters — two parsers in parallel)
```
T028 CheckstyleConfigParser.kt
T029 PmdConfigParser.kt
```

### Phases 3–7 (once Phase 2 complete, all stories in parallel)
```
Phase 3: T016 → T017 → T018 → T019 → T020 ∥ T021
Phase 4: T022 → T023 → T024 → T025
Phase 5: T026 ∥ T027 → T028 ∥ T029 → T030 → T031 → T032
Phase 6: T033 → T034 → T035 → T036
Phase 7: T037 → T038 → T039 → T040
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T003)
2. Complete Phase 2: Foundational (T004–T015)
3. Complete Phase 3: User Story 1 (T016–T021)
4. **STOP and VALIDATE**: `./gradlew :model:test :scan:test --tests "*.StackCollectorTest"` passes
5. Stack section fully functional; ship or demo if needed

### Incremental Delivery

1. Phase 1 + Phase 2 → Foundation ready
2. Phase 3 → Stack collector done → test independently
3. Phase 4 → CodeStyle collector done → test independently
4. Phase 5 → Linter collector done → test independently
5. Phase 6 → Test collector done → test independently
6. Phase 7 → Structure collector done → test independently
7. Phase 8 → Full scan wired and integration-tested

---

## Notes

- [P] tasks touch different files — no risk of conflict
- Tests are required by FR-019 and SC-005 — they are not optional for this feature
- Each collector's test file references only its own fake ports — tests are fully isolated from IntelliJ
- `breaksBuild` on `ActiveRule` is always `null` for Gradle projects (research.md R-005)
- Import resolution in Checkstyle/PMD configs is explicitly excluded (FR-008, CHK014)
- `packageSegments` uses dotted Java notation — `com.example.web`, never `com/example/web`
- Commit after each task checkpoint to keep the branch bisectable
