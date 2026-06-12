# Tasks: Model — Structured Data Contract for Project-Scan

**Input**: Design documents from `specs/001-model-data-contract/`
**Prerequisites**: plan.md ✅ · spec.md ✅ · research.md ✅ · data-model.md ✅ · contracts/ ✅

**Tests**: Included — unit tests are part of the sprint acceptance criteria (FR-009, SC-001–SC-007), not optional.

**Implementation note on priority vs. ordering**: US1 (root aggregate) is P1 in the spec because the type shape must be decided first — and it has been (see data-model.md). In *implementation* order, US1 is last because `ProjectScanModel` depends on all five section types existing. US2 and US3 are both P1 and fully independent of each other.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared-state dependencies)
- **[Story]**: Maps to user story — [US1]…[US6]

---

## Phase 1: Setup — Gradle Submodule Wiring

**Purpose**: Create the isolated `:model` Gradle submodule. Nothing else can compile until this phase is complete.

**⚠️ CRITICAL**: All subsequent phases depend on this setup completing successfully.

- [x] T001 Create `model/build.gradle.kts` with only `org.jetbrains.kotlin.jvm` plugin, `testImplementation(libs.junit.jupiter)` dependency, `tasks.test { useJUnitPlatform() }`, and NO `intellijPlatform { }` block (SC-004 isolation requirement)
- [x] T002 Update `settings.gradle.kts` — add `include(":model")` after the existing `rootProject.name` assignment
- [x] T003 Update root `build.gradle.kts` — add `implementation(project(":model"))` to the `dependencies { }` block

**Checkpoint**: Run `./gradlew :model:dependencies` — must resolve without any IntelliJ Platform artifact appearing.

---

## Phase 2: User Story 2 — Stack Section (Priority: P1)

**Goal**: Define `Dependency`, `BuildSystem`, and `StackInfo` in `model/src/main/kotlin/dev/zahaand/projectscan/model/StackInfo.kt`. This phase also defines the shared `Dependency` type reused by User Story 6 (Structure).

**Independent Test**: `./gradlew :model:test --tests "*StackInfoTest"` — populated + empty scenarios pass, `Dependency` fields round-trip correctly.

- [ ] T004 [P] [US2] Create `model/src/main/kotlin/dev/zahaand/projectscan/model/StackInfo.kt` — define `Dependency(groupId, artifactId, resolvedVersion: String?)`, `BuildSystem` enum (MAVEN, GRADLE), and `StackInfo(dependencies, jdkVersion?, languageLevel?, buildSystem?)` with empty-state defaults per data-model.md
- [ ] T005 [P] [US2] Create `model/src/test/kotlin/dev/zahaand/projectscan/model/StackInfoTest.kt` — test: (1) empty-state `StackInfo()` has empty list + null fields; (2) populated `StackInfo` with two `Dependency` entries round-trips all fields; (3) `Dependency.resolvedVersion` accepts null (BOM-managed case)

**Checkpoint**: `./gradlew :model:test --tests "*StackInfoTest"` passes.

---

## Phase 3: User Story 3 — Code Style Section (Priority: P1)

**Goal**: Define `StyleSourceType` (with embedded `priority: Int`), `StyleSource`, and `CodeStyleInfo`. The priority encoding is a mandatory gate (CHK017).

**Independent Test**: `./gradlew :model:test --tests "*CodeStyleInfoTest"` — all five source types assert their priority ranks; linter types rank lower than EditorConfig, which ranks lower than IdeCodeStyle.

> ⚠️ **Phase 2 and Phase 3 can run in parallel** — `StackInfo.kt` and `CodeStyleInfo.kt` are independent files.

- [ ] T006 [P] [US3] Create `model/src/main/kotlin/dev/zahaand/projectscan/model/CodeStyleInfo.kt` — define `StyleSourceType` enum with `val priority: Int` constructor parameter (CHECKSTYLE=1, SPOTLESS=1, PMD=1, EDITOR_CONFIG=2, IDE_CODE_STYLE=3), `StyleSource(type, path: String)`, and `CodeStyleInfo(sources)` with empty-state default per data-model.md
- [ ] T007 [P] [US3] Create `model/src/test/kotlin/dev/zahaand/projectscan/model/CodeStyleInfoTest.kt` — test: (1) empty-state `CodeStyleInfo()` has empty list; (2) all five `StyleSourceType` values carry the correct `priority` integer; (3) `minByOrNull { it.type.priority }` on a mixed list returns a linter-type source; (4) Checkstyle/Spotless/PMD share rank 1 (tie — verify equality, not ordering among them); (5) `StyleSource` path field round-trips

**Checkpoint**: `./gradlew :model:test --tests "*CodeStyleInfoTest"` passes; SC-003 satisfied.

---

## Phase 4: User Story 4 — Linters Section (Priority: P2)

**Goal**: Define `RuleSeverity`, `ActiveRule`, and `LinterInfo`.

**Independent Test**: `./gradlew :model:test --tests "*LinterInfoTest"` — empty list and two-rule populated cases pass.

> ⚠️ **Phase 4 and Phase 5 can run in parallel** — `LinterInfo.kt` and `TestInfo.kt` are independent files.

- [ ] T008 [P] [US4] Create `model/src/main/kotlin/dev/zahaand/projectscan/model/LinterInfo.kt` — define `RuleSeverity` enum (ERROR, WARNING, INFO), `ActiveRule(ruleId: String, tool: String, severity: RuleSeverity, breaksBuild: Boolean)`, and `LinterInfo(activeRules)` with empty-state default per data-model.md
- [ ] T009 [P] [US4] Create `model/src/test/kotlin/dev/zahaand/projectscan/model/LinterInfoTest.kt` — test: (1) empty-state `LinterInfo()` has empty `activeRules`; (2) `ActiveRule` with `severity=ERROR` and `breaksBuild=true` round-trips all four fields; (3) `ActiveRule` with `severity=WARNING` and `breaksBuild=false` round-trips correctly

**Checkpoint**: `./gradlew :model:test --tests "*LinterInfoTest"` passes.

---

## Phase 5: User Story 5 — Tests Section (Priority: P2)

**Goal**: Define `TestFramework` and `TestInfo`.

**Independent Test**: `./gradlew :model:test --tests "*TestInfoTest"` — null coverage and 80.0 coverage scenarios pass.

- [ ] T010 [P] [US5] Create `model/src/main/kotlin/dev/zahaand/projectscan/model/TestInfo.kt` — define `TestFramework(name: String, version: String?)`, and `TestInfo(frameworks, sourceRoots, namingPattern?, coverageThreshold?)` with empty-state defaults per data-model.md
- [ ] T011 [P] [US5] Create `model/src/test/kotlin/dev/zahaand/projectscan/model/TestInfoTest.kt` — test: (1) empty-state `TestInfo()` has empty lists and null nullable fields; (2) `TestFramework.version` accepts null (BOM-managed); (3) populated `TestInfo` with JUnit 5 + Mockito, source root, naming pattern, and `coverageThreshold=80.0` round-trips all fields; (4) `coverageThreshold=null` represents "JaCoCo absent" case

**Checkpoint**: `./gradlew :model:test --tests "*TestInfoTest"` passes.

---

## Phase 6: User Story 6 — Structure Section (Priority: P2)

**Goal**: Define `Module`, `PackageOrganisation`, and `StructureInfo`. **Depends on Phase 2** because `Module.declaredDependencies` reuses the `Dependency` type from `StackInfo.kt`.

**Independent Test**: `./gradlew :model:test --tests "*StructureInfoTest"` — single-module and multi-module scenarios pass.

- [ ] T012 [US6] Create `model/src/main/kotlin/dev/zahaand/projectscan/model/StructureInfo.kt` — define `PackageOrganisation` enum (BY_LAYER, BY_FEATURE), `Module(name: String, declaredDependencies: List<Dependency>, moduleDependencies: List<String>)` with empty-list defaults, and `StructureInfo(modules, packageOrganisation?, rootPackages)` with empty-state defaults per data-model.md; `Dependency` is already in the same package (no import needed)
- [ ] T013 [US6] Create `model/src/test/kotlin/dev/zahaand/projectscan/model/StructureInfoTest.kt` — test: (1) empty-state `StructureInfo()` has empty lists and null packageOrganisation; (2) single-module project: one `Module` with empty deps lists; (3) multi-module: `app` module has non-empty `moduleDependencies=["core"]` and external `declaredDependencies`; (4) `rootPackages` is project-wide list; (5) `PackageOrganisation.BY_FEATURE` round-trips

**Checkpoint**: `./gradlew :model:test --tests "*StructureInfoTest"` passes.

---

## Phase 7: User Story 1 — Root Aggregate (Priority: P1)

**Goal**: Define `ProjectScanModel` and verify the full model contract — all five sections present, empty-state construction, and data class copy semantics. **Depends on Phases 2–6** (all section types must exist).

**Independent Test**: `./gradlew :model:test --tests "*ProjectScanModelTest"` — all-empty construction and fully-populated round-trip pass.

- [ ] T014 [US1] Create `model/src/main/kotlin/dev/zahaand/projectscan/model/ProjectScanModel.kt` — define `ProjectScanModel(stack: StackInfo, codeStyle: CodeStyleInfo, linters: LinterInfo, tests: TestInfo, structure: StructureInfo)` with NO default values (all five sections must be explicitly provided per data-model.md invariant)
- [ ] T015 [US1] Create `model/src/test/kotlin/dev/zahaand/projectscan/model/ProjectScanModelTest.kt` — test: (1) all-empty `ProjectScanModel(StackInfo(), CodeStyleInfo(), LinterInfo(), TestInfo(), StructureInfo())` constructs without error and all sections are non-null; (2) fully-populated model round-trips all fields via data class equality; (3) `.copy(stack = StackInfo())` produces a new instance with the replaced section and all other sections unchanged

**Checkpoint**: `./gradlew :model:test` — full suite (all six test classes) passes.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Constitution-required tooling and final validation.

- [ ] T016 [P] Apply detekt static analysis plugin to cover `:model` sources — add `id("io.gitlab.arturbosch.detekt")` to `model/build.gradle.kts` (or root `build.gradle.kts` with subproject configuration) and verify `./gradlew :model:detekt` passes with no violations; constitution requires this to fail the build on violation
- [ ] T017 [P] Apply ktlint code style plugin to cover `:model` sources — add ktlint plugin to `model/build.gradle.kts` (or root) and verify `./gradlew :model:ktlintCheck` passes; constitution requires ktlint to own formatting, not detekt
- [ ] T018 Verify classpath isolation and FR-010 (SC-004): (1) run `./gradlew :model:dependencies --configuration compileClasspath` and confirm no `com.jetbrains.intellij` or `org.jetbrains.intellij` artifact appears; (2) grep `model/src/main` for any import of IntelliJ Platform APIs (`com.intellij`, `org.jetbrains.annotations` excluded), data-collection logic, prompt-generation logic, or UI components — confirm zero matches (FR-010)
- [ ] T019 Verify test suite timing (SC-005): run `./gradlew :model:test` with `--info` flag and confirm total test execution completes in < 5 seconds on a local developer workstation

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup)
    ├── Phase 2 (US2 – Stack)       ←── defines shared Dependency type
    ├── Phase 3 (US3 – CodeStyle)   [parallel with Phase 2]
    ├── Phase 4 (US4 – Linters)     [parallel with Phase 5; depends only on Phase 1]
    ├── Phase 5 (US5 – Tests)       [parallel with Phase 4; depends only on Phase 1]
    └── Phase 6 (US6 – Structure)   [depends on Phase 2 for Dependency type]
            └── Phase 7 (US1 – Root Aggregate)  [depends on ALL of Phases 2–6]
                    └── Phase 8 (Polish)
```

### User Story Dependencies

- **US2 (P1)**: Depends on Setup — no story dependencies
- **US3 (P1)**: Depends on Setup — no story dependencies; **runs in parallel with US2**
- **US4 (P2)**: Depends on Setup — no story dependencies; **runs in parallel with US5**
- **US5 (P2)**: Depends on Setup — no story dependencies; **runs in parallel with US4**
- **US6 (P2)**: Depends on US2 (needs `Dependency` type from same package)
- **US1 (P1)**: Depends on US2, US3, US4, US5, US6 — must be last

### Within Each Phase

- Implementation task (T_N04, T_N06, etc.) before its paired test task (T_N05, T_N07, etc.) — or write test first if practising TDD; both approaches are valid since these are data classes.
- Both tasks in a phase are marked [P] and can run in parallel within a phase (separate files).
- T016 and T017 (Polish) can run in parallel.

---

## Parallel Example: P1 Stories (Phase 2 + Phase 3)

```bash
# These can be worked on simultaneously:
Task T004: "Create StackInfo.kt with Dependency, BuildSystem, StackInfo"
Task T006: "Create CodeStyleInfo.kt with StyleSourceType, StyleSource, CodeStyleInfo"

# Then in parallel:
Task T005: "Create StackInfoTest.kt"
Task T007: "Create CodeStyleInfoTest.kt"
```

## Parallel Example: P2 Stories (Phases 4 + 5)

```bash
# These can be worked on simultaneously:
Task T008: "Create LinterInfo.kt"
Task T010: "Create TestInfo.kt"

# Then in parallel:
Task T009: "Create LinterInfoTest.kt"
Task T011: "Create TestInfoTest.kt"
```

---

## Implementation Strategy

### MVP First

> ⚠️ `ProjectScanModel` (Phase 7) cannot compile until all five section types exist. Phases 4, 5, and 6 are compile-required, not skippable. The correct minimum path to a compiling root aggregate is: **1 → 2+3 → 4+5 → 6 → 7**.

1. Complete **Phase 1** (Setup) — mandatory gate
2. Complete **Phases 2 + 3** in parallel (US2 Stack + US3 CodeStyle — both P1)
3. Complete **Phases 4 + 5** in parallel (US4 Linters + US5 Tests — both P2)
4. Complete **Phase 6** (US6 Structure — depends on Phase 2 for `Dependency`)
5. Complete **Phase 7** (US1 root aggregate — depends on all five section types)
6. **STOP and validate**: `./gradlew :model:test` — full suite green
7. Proceed to Phase 8 (Polish)

### Full Sprint Delivery

1. Phases 1 → 2+3 (parallel) → 4+5 (parallel) → 6 → 7 → 8
2. Each phase checkpoint passes before moving forward
3. `./gradlew :model:test` green after every phase

---

## Notes

- [P] tasks = operate on different files with no shared in-progress dependencies — safe to run concurrently
- US1 is listed last in implementation order even though it is Priority P1 in the spec. The spec priority reflects the importance of the contract shape (decided in the spec/plan), not when the Kotlin file is written.
- US6 has an implicit dependency on US2: `Module.declaredDependencies` reuses `Dependency` which lives in `StackInfo.kt`. Because all types share the same package, no Kotlin import is required — but the file must exist.
- `Dependency` living in `StackInfo.kt` is intentional (per research.md Decision 4). Do not create a separate `Dependency.kt` file.
- T016/T017 (detekt/ktlint): if the root build already has these plugins configured, apply subproject filtering rather than duplicating plugin application. Check existing `build.gradle.kts` first.
