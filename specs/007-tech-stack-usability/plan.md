# Implementation Plan: Sprint 7 — Usability Rework: Tech Stack & Testing Collection Layer

**Branch**: `007-tech-stack-usability` | **Date**: 2026-06-28 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/007-tech-stack-usability/spec.md`

## Summary

Rework the collection and representation layers for Tech Stack and Testing to produce a compact inverted-view output (one entry per Maven coordinate) for large monorepos, remove the framework list from Testing, eliminate Project Structure and package outputs, and centralise all inversion/grouping logic in the `shared` module for single-source-of-truth consumption by both the prompt generator and the UI tool window. The key model changes are: `Module` gains `aggregator: String?`; `StructureInfo` drops package fields; `TestInfo` drops framework fields; `StackInfo` drops the flat dependency list; `DependencyPort` + `IjDependencyAdapter` are deleted; `shared` gains `InvertedTechStack` building and rendering.

## Technical Context

**Language/Version**: Kotlin (JVM 21, JetBrains Runtime)
**Primary Dependencies**: IntelliJ Platform API (`MavenProjectsManager`, `ExternalSystemApiUtil`, `ProjectDataManager`, `ModuleManager`, `ModuleRootManager`); `maven-artifact` (version comparison, already on classpath); IntelliJ Platform Gradle Plugin 2.x (`org.jetbrains.intellij.platform`)
**Storage**: N/A
**Testing**: JUnit 4 (4.13.2) and JUnit Jupiter 5 (5.11.4) for unit tests; IntelliJ Platform Test Framework for adapter integration tests; Fake-port pattern used throughout (`scan/src/test/.../fake/`)
**Target Platform**: IntelliJ IDEA 2025.3.5; JDK 21
**Project Type**: IntelliJ IDEA plugin; 6-subproject Gradle build (`model`, `scan`, `shared`, `baseline`, `prompt`, `ui`+root)
**Performance Goals**: Tech Stack output for 130-module monorepo ≤ 40 lines (SC-001)
**Constraints**: Must use IntelliJ project model exclusively — no POM file parsing (Constitution II)
**Scale/Scope**: ~50 Kotlin source files; reference test target is a 130-module Maven monorepo (~11 aggregator groups, ~250 resolved deps)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I — Decoupling of Collection and Generation ✓
Inverted Tech Stack building logic moves to `shared`, which is the single contract layer between `scan` producers and `prompt`/`ui` consumers. `scan` gains no dependency on `prompt` or `ui`. Dependency direction remains: `scan` → `model`; `shared` → `model`; `prompt` → `model` + `shared`; `ui` → `model` + `shared` + `prompt`. The `model` layer is not modified for generation purposes.

### Principle II — Read Through IntelliJ Project Model Only ✓ (confirmed via R1/R2)
FR-003 (direct-only Maven deps): resolved via the prioritized fallback chain documented in spec §Assumptions: (1) `MavenProject.mavenModel.dependencies` coordinate set intersected with `MavenProject.dependencies` (resolved versions) — primary path; (2) root-level nodes of the resolved dependency tree — first fallback; (3) resolved set minus computed transitives — last resort. All paths access IntelliJ's Maven plugin; no POM text parsing. Exact path confirmed against 2025.3.5 classpath at implement time.
FR-004 (aggregator): derived from `MavenProject.mavenModel.modules` (each project's `<modules>` list) by building a reverse directory→aggregatorName map at collection time.
FR-006 (Gradle denylist): applied to results from `ExternalSystemApiUtil.findAll(..., ProjectKeys.LIBRARY_DEPENDENCY)`.

### Principle III — Never Fabricate ✓
Uniform module count is computed from actual collected module data. Coverage "not detected" if JaCoCo absent (preserved from Sprint 6). `aggregator` null when module not listed in any reactor `<modules>` block — renders ungrouped (edge case in spec).

### Principle IV — Curated Baseline Rule Set ✓
`baseline` component untouched (FR-N3).

**Post-Phase 1 re-check**: No new violations introduced. The inverted Tech Stack is derived exclusively from model data; no heuristics or fabrication.

## Project Structure

### Documentation (this feature)

```text
specs/007-tech-stack-usability/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
model/src/main/kotlin/dev/zahaand/projectscan/model/
├── StructureInfo.kt         # Module gains aggregator: String?; StructureInfo drops rootPackages + packageSegments
├── TestInfo.kt              # Drop frameworks + unknownTestDependencies; delete TestFramework class
└── StackInfo.kt             # Drop dependencies: List<Dependency>

scan/src/main/kotlin/dev/zahaand/projectscan/scan/
├── port/
│   ├── ModuleStructurePort.kt   # ModuleDescriptor gains aggregator: String?; PackageTreeData deleted; getPackageTree() removed
│   ├── TestInfoPort.kt          # getTestScopedDependencies() removed
│   └── DependencyPort.kt        # FILE DELETED
├── adapter/
│   ├── IjModuleStructureAdapter.kt  # mavenModules(): direct-only deps (FR-003) + aggregator (FR-004); getPackageTree() removed (FR-005)
│   ├── IjTestInfoAdapter.kt         # getTestScopedDependencies() removed
│   └── IjDependencyAdapter.kt       # FILE DELETED
└── collector/
    ├── StackCollector.kt        # Remove DependencyPort constructor param; build-system metadata only
    ├── StructureCollector.kt    # Propagate aggregator descriptor→Module; remove getPackageTree() call; fix StructureInfo construction
    └── TestCollector.kt         # Remove framework detection; only sourceRoots + namingSuffixes + coverageThreshold
└── ScanService.kt               # Remove DependencyPort constructor param + StackCollector wiring

shared/src/main/kotlin/dev/zahaand/projectscan/shared/
└── OutputFormatters.kt          # ADD: InvertedTechStack/TechEntry/VersionEntry/AggregatorGroup/CarrierModule
                                 # ADD: buildInvertedTechStack(modules, internalNames): InvertedTechStack
                                 # ADD: renderInvertedTechStack(stack: InvertedTechStack, buildSystem: BuildSystem?, jdkVersion: String?, languageLevel: String?): String
                                 # REMOVE: DependencyGroup, groupDependencies, deduplicateFrameworks,
                                 #         detectVersionDiscrepancies, renderVersionDiscrepancyLine,
                                 #         filterInternalDependencies, VersionDiscrepancy
                                 # RETAIN: SourceRootTemplate, normalizeSourceRoots

prompt/src/main/kotlin/dev/zahaand/projectscan/prompt/
└── PromptGenerator.kt           # buildTechStackBlock: call buildInvertedTechStack + renderInvertedTechStack from shared
                                 # buildTestingBlock: remove framework rendering
                                 # DELETE buildProjectStructureBlock
                                 # generate(): remove "Project Structure" PromptBlock entry

src/main/kotlin/dev/zahaand/projectscan/ui/
└── ScanResultRenderer.kt        # renderStack: call buildInvertedTechStack + renderInvertedTechStack from shared
                                 # renderTests: remove framework rendering
                                 # DELETE renderStructure
                                 # render(): remove Structure section entry

scan/src/test/kotlin/dev/zahaand/projectscan/scan/
├── fake/FakeDependencyPort.kt          # FILE DELETED
├── fake/FakeModuleStructurePort.kt     # Remove getPackageTree(); add aggregator field to descriptors
├── fake/FakeTestInfoPort.kt            # Remove getTestScopedDependencies()
├── adapter/IjDependencyAdapterTest.kt  # FILE DELETED
├── adapter/IjModuleStructureAdapterTest.kt  # Update: direct-only deps, aggregator field
├── adapter/IjTestInfoAdapterTest.kt    # Remove getTestScopedDependencies test
├── collector/StackCollectorTest.kt     # Remove dependency-related test cases
├── collector/StructureCollectorTest.kt # Update: aggregator propagation, no packageTree
└── collector/TestCollectorTest.kt      # Remove framework detection tests
```

**Structure Decision**: Single Gradle multi-project build. No new submodules needed. `shared` already exists and is the correct home for inversion logic (FR-013, Constitution I).

## Tracked Deviations

| Deviation | Description |
|-----------|-------------|
| `:ui` → `:prompt` dependency | `:ui` depends on `:scan`, `:baseline`, `:prompt`, and `:model`, which the constitution's §Project Structure currently forbids for prompt/ui ("scan, prompt, and ui MUST NOT depend on each other"). `:ui` is the composition root that wires `ScanService → BaselineRuleProvider → PromptGenerator → render`; this coupling is deliberate and necessary. Origin: Sprint 5 (introduced when the composition root was established). Sprint 7 neither introduces nor worsens this dependency. Resolution target: Sprint 9 constitution-amendment package — item (e) legitimise `:ui` as composition root (`ui` MAY depend on `scan`, `baseline`, and `prompt`; narrow the mutual-exclusion ban to `scan ↔ prompt` only). To be recorded in `TECH_DEBT.md` when that file is created in Sprint 9. |

## Complexity Tracking

> No constitution violations requiring justification in this sprint.

## Task Generation Requirements (for /speckit-tasks)

When generating `tasks.md`, ensure the task list explicitly covers:

- **Deleted files**: An explicit task for each of the four files being deleted — `DependencyPort.kt`, `IjDependencyAdapter.kt`, `FakeDependencyPort.kt`, `IjDependencyAdapterTest.kt`.
- **Wiring change**: The `ScanService` / `ScanServiceFactory` / `ProjectScanPanel` constructor change when `DependencyPort` is removed from `StackCollector`'s constructor.
- **Signature change**: `renderStack()` in `ScanResultRenderer` — new signature accepting `List<Module>` or pre-built `InvertedTechStack`; all call sites updated.
- **Consumer audit**: Verify the complete consumer list for removed fields (`StructureInfo.rootPackages`, `StructureInfo.packageSegments`, `StackInfo.dependencies`, `TestInfo.frameworks`, `TestInfo.unknownTestDependencies`) before marking removal tasks complete; the list in the project structure section is assumed but not exhaustively verified.
- **FR-003 fallback chain**: The task implementing `IjModuleStructureAdapter.mavenModules()` direct-only slice MUST verify which API path succeeds against the 2025.3.5 classpath and document the result in a code comment; it is not complete until the path is confirmed.
