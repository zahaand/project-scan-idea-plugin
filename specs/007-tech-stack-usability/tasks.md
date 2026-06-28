# Tasks: Sprint 7 — Usability Rework: Tech Stack & Testing Collection Layer

**Input**: Design documents from `/specs/007-tech-stack-usability/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- All task descriptions include exact file paths

---

## Phase 1: Setup

**Purpose**: Pre-change audit to capture all consumers of removed symbols before any code is modified

- [X] T001 Run `grep -rn` audit across all subprojects for every symbol being removed: `rootPackages`, `packageSegments`, `StackInfo.dependencies`, `TestInfo.frameworks`, `TestInfo.unknownTestDependencies`, `TestFramework`, `getPackageTree`, `getTestScopedDependencies`, `DependencyPort`, `PackageTreeData`; record all call sites before making any changes

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Model and port interface changes that all story phases depend on. Project will not compile until all downstream consumers (addressed in story phases) are updated.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 [P] Update `model/src/main/kotlin/dev/zahaand/projectscan/model/StructureInfo.kt`: add `val aggregator: String? = null` to `Module`; remove `rootPackages` and `packageSegments` fields from `StructureInfo`
- [X] T003 [P] Update `model/src/main/kotlin/dev/zahaand/projectscan/model/TestInfo.kt`: delete `TestFramework` data class; remove `frameworks: List<TestFramework>` and `unknownTestDependencies: List<Dependency>` fields from `TestInfo`
- [X] T004 [P] Update `model/src/main/kotlin/dev/zahaand/projectscan/model/StackInfo.kt`: remove `dependencies: List<Dependency>` field
- [X] T005 [P] Update `scan/src/main/kotlin/dev/zahaand/projectscan/scan/port/ModuleStructurePort.kt`: add `val aggregator: String? = null` to `ModuleDescriptor`; delete `PackageTreeData` data class; remove `getPackageTree()` from `ModuleStructurePort` interface
- [X] T006 [P] Update `scan/src/main/kotlin/dev/zahaand/projectscan/scan/port/TestInfoPort.kt`: remove `getTestScopedDependencies(): List<Dependency>` from the interface
- [X] T007 [P] Delete `scan/src/main/kotlin/dev/zahaand/projectscan/scan/port/DependencyPort.kt`

**Checkpoint**: Model and port interfaces updated — story phases can now proceed (each story phase fixes its own compilation errors)

---

## Phase 3: User Story 1 — Concise Inverted Tech Stack (Priority: P1) 🎯 MVP

**Goal**: Replace the flat ~150-line per-module dependency list with a compact inverted view: one entry per `groupId:artifactId`, showing version(s) and carrier modules grouped by reactor aggregator only when versions differ across modules.

**Independent Test**: Scan the real 130-module Maven monorepo; verify the Tech Stack section has ≤ 40 lines, each coordinate appears at most once, and no transitive-only artifact (asm, objenesis, listenablefuture, failureaccess, j2objc-annotations, checker-qual, aopalliance, paranamer) appears in the output.

### Implementation for User Story 1

- [X] T008 [US1] Delete `scan/src/main/kotlin/dev/zahaand/projectscan/scan/adapter/IjDependencyAdapter.kt`
- [X] T009 [US1] Delete `scan/src/test/kotlin/dev/zahaand/projectscan/scan/adapter/IjDependencyAdapterTest.kt`
- [X] T010 [US1] Delete `scan/src/test/kotlin/dev/zahaand/projectscan/scan/fake/FakeDependencyPort.kt`
- [X] T011 [US1] Update `scan/src/main/kotlin/dev/zahaand/projectscan/scan/adapter/IjModuleStructureAdapter.kt`: (a) implement FR-003 direct-only Maven deps via intersection of `mp.mavenModel.dependencies` coordinate set with resolved `mp.dependencies`; verify which fallback path succeeds against IntelliJ 2025.3.5 classpath and document the confirmed path in an inline code comment — task is not complete until this comment is present; (b) implement FR-004 aggregator reverse map — build `aggregatorByDir: MutableMap<String, String>` from `mp.mavenModel.modules` before iterating modules, populate `ModuleDescriptor.aggregator`; (c) add FR-006 Gradle denylist as companion object constants (`GRADLE_DENYLIST_EXACT` set + `GRADLE_DENYLIST_ASM_GROUP` const) and apply in `gradleModules()`; (d) remove `getPackageTree()` method and its `collectPackageTree()` / `isValidJavaIdentifier()` helpers if unused elsewhere
- [X] T012 [P] [US1] Update `scan/src/test/kotlin/dev/zahaand/projectscan/scan/fake/FakeModuleStructurePort.kt`: remove `getPackageTree()` implementation; add `aggregator` field to module descriptors in fake fixture data
- [X] T013 [US1] Update `scan/src/main/kotlin/dev/zahaand/projectscan/scan/collector/StackCollector.kt`: remove `dependencyPort: DependencyPort` constructor parameter; remove `aggregate()` and `pickMaxVersion()` helpers; retain build-system metadata collection only
- [X] T014 [US1] Update `scan/src/main/kotlin/dev/zahaand/projectscan/scan/ScanService.kt` and all wiring call sites (`ScanServiceFactory.kt` and `ProjectScanPanel.kt` in root/ui subproject): remove `DependencyPort` from `StackCollector` construction; run `grep -rn DependencyPort` to confirm no remaining references before marking complete
- [X] T015 [P] [US1] Add new data classes to `shared/src/main/kotlin/dev/zahaand/projectscan/shared/OutputFormatters.kt`: `CarrierModule(name: String, aggregator: String?)`, `AggregatorGroup(aggregator: String?, moduleNames: List<String>)`, `VersionEntry(version: String, isUniform: Boolean, uniformModuleCount: Int, groups: List<AggregatorGroup>)`, `TechEntry(coordinate: String, versions: List<VersionEntry>)`, `InvertedTechStack(entries: List<TechEntry>)`
- [X] T016 [US1] Add `fun buildInvertedTechStack(modules: List<Module>, internalModuleNames: Set<String>): InvertedTechStack` to `shared/src/main/kotlin/dev/zahaand/projectscan/shared/OutputFormatters.kt`: group direct external deps by coordinate; collect versions and carrier sets; for single version produce `VersionEntry(isUniform=true, uniformModuleCount=N, groups=emptyList())`; for multiple versions produce per-version `AggregatorGroup` lists sorted (named aggregators alphabetically, null-aggregator group last; module names within group alphabetically); sort all coordinates alphabetically (depends on T015)
- [X] T017 [US1] Add `fun renderInvertedTechStack(stack: InvertedTechStack, buildSystem: BuildSystem?, jdkVersion: String?, languageLevel: String?): String` to `shared/src/main/kotlin/dev/zahaand/projectscan/shared/OutputFormatters.kt`: render preamble (Build System, JDK Version, Language Level); uniform entries as `- coordinate:version [N modules]`; multi-version entries as coordinate header followed by indented `- version  aggregator: module1, module2` lines; return `"not detected"` when `stack.entries.isEmpty()` and all preamble values are null (depends on T015)
- [X] T018 [US1] Remove old symbols from `shared/src/main/kotlin/dev/zahaand/projectscan/shared/OutputFormatters.kt`: delete `DependencyGroup`, `VersionDiscrepancy`, `groupDependencies()`, `deduplicateFrameworks()`, `detectVersionDiscrepancies()`, `renderVersionDiscrepancyLine()`, `filterInternalDependencies()`; retain `SourceRootTemplate` and `normalizeSourceRoots()` (do this only after T016 and T017 are complete)
- [X] T019 [US1] Update `prompt/src/main/kotlin/dev/zahaand/projectscan/prompt/PromptGenerator.kt` `buildTechStackBlock()`: replace `groupDependencies(filterInternalDependencies(...))` call chain with `buildInvertedTechStack(structure.modules, internalNames)` + `renderInvertedTechStack(stack, stackInfo.buildSystem, stackInfo.jdkVersion, stackInfo.languageLevel)` (depends on T016, T017, T018)
- [X] T020 [US1] Update `src/main/kotlin/dev/zahaand/projectscan/ui/ScanResultRenderer.kt` `renderStack()`: change signature to accept `List<Module>` and `Set<String>` (internal module names) instead of relying on `StackInfo.dependencies`; call `buildInvertedTechStack` + `renderInvertedTechStack` from shared; run `grep -rn "renderStack"` to find and update all call sites before marking complete (depends on T016, T017, T018)
- [X] T021 [P] [US1] Update `scan/src/test/kotlin/dev/zahaand/projectscan/scan/adapter/IjModuleStructureAdapterTest.kt`: add integration tests covering direct-only dep extraction (FR-003) and aggregator field population (FR-004); remove `getPackageTree()` test cases
- [X] T022 [P] [US1] Update `scan/src/test/kotlin/dev/zahaand/projectscan/scan/collector/StackCollectorTest.kt`: remove dependency-related test cases; update constructor invocation to reflect removed `DependencyPort` parameter

**Checkpoint**: US1 complete — run against 130-module monorepo; confirm ≤ 40 Tech Stack lines, no transitives, byte-identical output between prompt and UI (SC-001, SC-002, SC-005)

---

## Phase 4: User Story 2 — Repurposed Testing Section (Priority: P2)

**Goal**: Remove the framework list from the Testing section; Testing section carries only coverage threshold (or "not detected"), test source roots, and naming pattern.

**Independent Test**: Scan the reference Maven monorepo (JaCoCo absent); verify the Testing section shows coverage "not detected", source root layout, naming pattern, and contains NO "Frameworks:" header or framework name entries.

### Implementation for User Story 2

- [ ] T023 [US2] Update `scan/src/main/kotlin/dev/zahaand/projectscan/scan/adapter/IjTestInfoAdapter.kt`: remove `getTestScopedDependencies()` implementation
- [ ] T024 [P] [US2] Update `scan/src/test/kotlin/dev/zahaand/projectscan/scan/fake/FakeTestInfoPort.kt`: remove `getTestScopedDependencies()` implementation
- [ ] T025 [US2] Update `scan/src/main/kotlin/dev/zahaand/projectscan/scan/collector/TestCollector.kt`: remove the framework detection loop and `matchFramework()` helper; retain `getTestSourceRoots()`, `getTestClassNames()`, and `getCoverageThreshold()` collection paths
- [ ] T026 [P] [US2] Update `scan/src/test/kotlin/dev/zahaand/projectscan/scan/adapter/IjTestInfoAdapterTest.kt`: remove test cases for `getTestScopedDependencies()`
- [ ] T027 [P] [US2] Update `scan/src/test/kotlin/dev/zahaand/projectscan/scan/collector/TestCollectorTest.kt`: remove framework detection test cases
- [ ] T028 [US2] Update `prompt/src/main/kotlin/dev/zahaand/projectscan/prompt/PromptGenerator.kt` `buildTestingBlock()`: remove `deduplicateFrameworks(info.frameworks).forEach { ... }` and all framework rendering lines; retain coverage, source roots, and naming pattern rendering
- [ ] T029 [US2] Update `src/main/kotlin/dev/zahaand/projectscan/ui/ScanResultRenderer.kt` `renderTests()`: remove all framework rendering lines; retain coverage, source roots, and naming pattern rendering

**Checkpoint**: US2 complete — verify no "Frameworks:" header on any project; coverage shows "not detected" on reference monorepo (SC-003)

---

## Phase 5: User Story 3 — Remove Project Structure and Package Outputs (Priority: P3)

**Goal**: Eliminate the Project Structure block and all package/root-package values from both the generated prompt and the UI tool window output.

**Independent Test**: Scan any Maven project; verify neither "Project Structure" nor any `rootPackages`/`secondLevelSegments` value appears in the plugin's generated prompt or UI tool window output.

### Implementation for User Story 3

- [ ] T030 [US3] Update `scan/src/main/kotlin/dev/zahaand/projectscan/scan/collector/StructureCollector.kt`: remove `getPackageTree()` call; propagate `aggregator` field from `ModuleDescriptor` to `Module` (i.e., `Module(name = ..., aggregator = descriptor.aggregator, ...)`); fix `StructureInfo` construction to omit removed `rootPackages`/`packageSegments` fields
- [ ] T031 [P] [US3] Update `scan/src/test/kotlin/dev/zahaand/projectscan/scan/collector/StructureCollectorTest.kt`: remove `rootPackages`/`packageSegments` assertions; add test cases verifying `aggregator` field is propagated from `ModuleDescriptor` to `Module`
- [ ] T032 [US3] Update `prompt/src/main/kotlin/dev/zahaand/projectscan/prompt/PromptGenerator.kt`: delete `buildProjectStructureBlock()` method; remove the `PromptBlock("Project Structure", ...)` (or equivalent) entry from `generate()`
- [ ] T033 [US3] Update `src/main/kotlin/dev/zahaand/projectscan/ui/ScanResultRenderer.kt`: delete `renderStructure()` method; remove the `section(titleKey = "section.Structure.title", ...)` call (or equivalent) from `render()`

**Checkpoint**: US3 complete — verify no "Project Structure" section in any output; output is shorter by exactly that block (SC-006)

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final compilation check, parity verification, and success-criteria sign-off

- [ ] T034 [P] Run `./gradlew build` from repository root and fix any remaining compilation errors not resolved in story phases
- [ ] T035 Verify SC-005 and SC-004: (a) scan any project and confirm Tech Stack and Testing content is byte-identical between the generated LLM prompt and the UI tool window for the same scan — if not identical, trace which consumer is re-implementing formatting instead of calling `renderInvertedTechStack`; (b) confirm Tech Stack version discrepancies (for multi-version deps) appear inline within each entry and no standalone "Discrepancies" block exists in the output (SC-004)
- [ ] T036 [P] Verify SC-002: confirm no transitive-only artifact (`asm`, `objenesis`, `listenablefuture`, `failureaccess`, `j2objc-annotations`, `checker-qual`, `aopalliance`, `paranamer`) appears in Tech Stack or Testing output for any Maven project
- [ ] T037 Verify SC-001: scan the 130-module Maven monorepo and confirm Tech Stack output is ≤ 40 lines (preamble lines excluded from count — dependency entry lines only)
- [ ] T038 Record the Sprint 9 constitution-amendment obligation for the `shared` component: constitution §Project Structure must add `shared` to the component table with its dependency rules (`prompt` and `ui` MAY depend on `shared`; `scan` MUST NOT depend on `shared`). This is a tracking-only marker; the amendment lands in the Sprint 9 constitution package. No constitution edit in Sprint 7 (FR-N3).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 audit — BLOCKS all story phases
- **US1 (Phase 3)**: Depends on Phase 2 completion
- **US2 (Phase 4)**: Depends on Phase 2 completion — can run in parallel with Phase 3
- **US3 (Phase 5)**: Depends on Phase 2 completion — can run in parallel with Phases 3 and 4
- **Polish (Phase 6)**: Depends on all story phases completing

### User Story Dependencies

- **US1 (P1)**: Independent after foundational. Primary sprint deliverable.
- **US2 (P2)**: Independent after foundational. `TestInfoPort` and `TestInfo` model changes are in Phase 2.
- **US3 (P3)**: Independent after foundational. `StructureInfo` model changes and `ModuleDescriptor.aggregator` are in Phase 2; `StructureCollector` (T030) propagates the aggregator forward.

### Within Each User Story

- Deletion tasks (T008–T010) before adapter changes that reference deleted files/types
- Adapter and collector changes before shared layer (shared consumes model types fixed in Phase 2)
- T016 + T017 (`buildInvertedTechStack` + `renderInvertedTechStack`) before T018 (symbol removal) and before T019 + T020 (prompt/UI consumers)
- T015 (new data classes) before T016 and T017
- T014 (`ScanService` wiring) any time after T007 (port deleted) and T013 (collector updated)

### Parallel Opportunities

- All Phase 2 tasks (T002–T007): different files, fully parallelizable
- Within Phase 3: T012, T015, T021, T022 marked [P]; T019 and T020 parallelizable after T016–T018
- Within Phase 4: T024, T026, T027 marked [P]
- Within Phase 5: T031 marked [P]
- Within Phase 6: T034, T036 marked [P]
- Phases 3, 4, and 5 can overlap after Phase 2 when working as a team

---

## Parallel Example: User Story 1

```bash
# After Phase 2 and T008–T011 complete, these can run simultaneously:
Task T012: Update FakeModuleStructurePort.kt — add aggregator field to descriptors
Task T015: Add new data classes to OutputFormatters.kt
Task T021: Update IjModuleStructureAdapterTest.kt
Task T022: Update StackCollectorTest.kt

# After T015–T017 complete, these can run simultaneously:
Task T019: Update PromptGenerator.buildTechStackBlock()
Task T020: Update ScanResultRenderer.renderStack()
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Consumer audit (T001)
2. Complete Phase 2: Model and port changes (T002–T007)
3. Complete Phase 3: US1 — Inverted Tech Stack (T008–T022)
4. **STOP and VALIDATE**: Run against 130-module monorepo; confirm ≤ 40 Tech Stack lines, no transitives, byte-identical parity
5. Build is green; demo or merge if ready

### Incremental Delivery

1. Phase 1 + Phase 2 → model and ports stable; project compiles against updated APIs
2. Phase 3 (US1) → inverted Tech Stack working → primary sprint deliverable done
3. Phase 4 (US2) → Testing section cleaned → "Frameworks" header eliminated
4. Phase 5 (US3) → Project Structure removed → output noise reduced
5. Phase 6 → final verification and success criteria sign-off

---

## Notes

- **[P]** tasks = different files, no dependencies on incomplete tasks within the same phase
- **[Story]** label maps each task to a specific user story for traceability
- **T011** (FR-003 fallback): must verify which IntelliJ 2025.3.5 API path succeeds (`mavenModel.dependencies` intersection, or fallback via `getDependencyTree()` root nodes, or transitives subtraction) and add a code comment documenting the confirmed path — task is not complete until this comment is present
- **T014** (wiring): check `ScanServiceFactory.kt` and `ProjectScanPanel.kt` in root/ui subproject; run `grep -rn "StackCollector"` and `grep -rn "DependencyPort"` to find all construction sites before marking complete
- **T018** (symbol removal): remove old `OutputFormatters.kt` symbols only after T016 + T017 are in place to avoid removing functions before their replacements exist
- **T020** (`renderStack` signature): run `grep -rn "renderStack"` to find every call site; update each caller to pass `List<Module>` and `Set<String>` (internal names) — mark complete only after all call sites are updated
- **Consumer audit**: Cross-reference T001 audit results against each removal task before marking it complete to confirm no call site was missed
