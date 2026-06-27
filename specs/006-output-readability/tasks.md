# Tasks: Output Readability for Large Projects

**Input**: Design documents from `/specs/006-output-readability/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: US1, US2, or US3 — maps to user stories in spec.md

---

## Phase 1: Setup — :shared Gradle Module

**Purpose**: Register and scaffold the new `:shared` submodule so `:prompt` and the root project can depend on it.

- [ ] T001 Register `:shared` submodule: add `include(":shared")` to `settings.gradle.kts`
- [ ] T002 [P] Create `shared/build.gradle.kts` — mirror `prompt/build.gradle.kts` but with only `implementation(project(":model"))`, `testImplementation(libs.junit.jupiter)`, `testRuntimeOnly("org.junit.platform:junit-platform-launcher")`, detekt and ktlint blocks
- [ ] T003 [P] Add `implementation(project(":shared"))` to `prompt/build.gradle.kts`
- [ ] T004 [P] Add `implementation(project(":shared"))` to root `build.gradle.kts`

**Checkpoint**: `./gradlew :shared:compileKotlin` resolves without error.

---

## Phase 2: Foundational — OutputFormatters in :shared

**Purpose**: Core shared utility required by all three user stories. Must be complete and tested before any consumer is modified.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T005 Create `shared/src/main/kotlin/dev/zahaand/projectscan/shared/OutputFormatters.kt` containing:
  - Data classes:
    - `DependencyGroup(groupId: String, artifacts: List<Dependency>, sharedVersion: String?)` — `sharedVersion` is non-null only when the group has >1 artifact AND all have the same non-null `resolvedVersion`
    - `VersionDiscrepancy(groupId: String, artifactId: String, versions: Map<String, String>)` — `versions` maps `moduleName → resolvedVersion`; always has ≥2 entries
    - `SourceRootTemplate(relativePath: String, count: Int)` — `count` is the number of raw `sourceRoots` entries normalising to that template
  - Functions:
    - `fun groupDependencies(deps: List<Dependency>): List<DependencyGroup>` — groups by `groupId` in lexicographic order; single-artifact groups always yield `sharedVersion = null`; any `resolvedVersion == null` within a group also yields `sharedVersion = null`
    - `fun detectVersionDiscrepancies(modules: List<Module>): List<VersionDiscrepancy>` — for each `(groupId, artifactId)` coordinate, collects per-module non-null `resolvedVersion`; intra-module duplicates: use the last-declared version; returns only coordinates with ≥2 distinct version values across different modules; result sorted lexicographically by `groupId` then `artifactId`; module names in each result's `versions` map sorted lexicographically
    - `fun deduplicateFrameworks(frameworks: List<TestFramework>): List<TestFramework>` — returns distinct `(name, version)` pairs in first-occurrence order
    - `fun normalizeSourceRoots(roots: List<String>): List<SourceRootTemplate>` — partitions into absolute (starts with `/`) and relative; computes longest common directory prefix across absolute entries only (split on `/`); strips prefix from absolute entries; relative entries used as-is; groups identical relative templates, counting raw occurrences; returns sorted by `relativePath`

- [ ] T006 Create `shared/src/test/kotlin/dev/zahaand/projectscan/shared/OutputFormattersTest.kt` — JUnit 5 test class covering:
  - `groupDependencies`: multi-artifact uniform group → `sharedVersion` set; single-artifact group → `sharedVersion` null; mixed non-null versions → `sharedVersion` null; group with any null version → `sharedVersion` null; output sorted lexicographically by `groupId`
  - `detectVersionDiscrepancies`: two modules with differing non-null versions → discrepancy entry; same version in both → no entry; artifact in only one module → no entry; null `resolvedVersion` excluded from detection; intra-module duplicate → last declared version used; output sorted by `groupId` then `artifactId`; module names in result sorted
  - `deduplicateFrameworks`: 80 identical entries → exactly 1 result; 2 distinct frameworks → 2 results; first-occurrence order preserved
  - `normalizeSourceRoots`: absolute paths with shared prefix → relative template + count; all-relative inputs → used as-is; mixed absolute/relative → LCP computed only over absolute entries; empty input → empty result; output sorted by `relativePath`; count reflects raw entry occurrences

**Checkpoint**: `./gradlew :shared:test` — all OutputFormatters tests pass.

---

## Phase 3: User Story 1 — Tech Stack Grouping (Priority: P1) 🎯 MVP

**Goal**: Both consumers render Tech Stack grouped by `groupId`: uniform multi-artifact groups get a version header with `artifactId`-only lines; single-artifact and mixed/null groups get per-artifact lines without a header. Groups sorted lexicographically.

**Independent Test**: Fabricate a `StackInfo` with three groups — one uniform (5 artifacts, `6.1.4`), one single-artifact, one with mixed versions. Run both consumers. Assert: uniform group has header `groupId:* @ 6.1.4` and no version on artifact lines; single-artifact group has no header; mixed group has no header; groups in lexicographic order.

- [ ] T007 [US1] Update `PromptGenerator.buildTechStackBlock()` in `prompt/src/main/kotlin/dev/zahaand/projectscan/prompt/PromptGenerator.kt`:
  - Replace the `forEach` over `info.dependencies` with a call to `groupDependencies(info.dependencies)` from `:shared`
  - For each group: if `sharedVersion != null` → emit `"- ${group.groupId}:* @ ${group.sharedVersion}"` then each artifact as `"  - ${dep.artifactId}"`; else → emit each artifact as `"- ${dep.groupId}:${dep.artifactId}${if (dep.resolvedVersion != null) ":${dep.resolvedVersion}" else ""}"`

- [ ] T008 [P] [US1] Update `ScanResultRenderer.renderStack()` in `src/main/kotlin/dev/zahaand/projectscan/ui/ScanResultRenderer.kt`:
  - Same `groupDependencies()` call and identical rendering format as T007 (byte-identical section body per NFR-001/SC-006)

- [ ] T009 [P] [US1] Create `prompt/src/test/kotlin/dev/zahaand/projectscan/prompt/PromptGeneratorOutputReadabilityTest.kt` with US1 scenarios:
  - Scenario 1: 5 `org.springframework` artifacts at `6.1.4` → header `org.springframework:* @ 6.1.4` present; `6.1.4` NOT repeated on individual artifact lines
  - Scenario 2: 2 artifacts in same `groupId` with different versions → both artifact lines with individual versions; no group header
  - Scenario 3: empty `StackInfo.dependencies` → no dependency lines; build system / JDK / language-level still shown
  - Scenario 4: `groupId` group with exactly one artifact → per-artifact format; no header

- [ ] T010 [P] [US1] Update `PromptGeneratorFullModelTest` scenario 5 in `prompt/src/test/kotlin/dev/zahaand/projectscan/prompt/PromptGeneratorFullModelTest.kt`:
  - The fixture has 3 different `groupId` values, each with one artifact → all render in per-artifact format
  - Update assertions: `junit-jupiter`, `spring-boot-starter`, `jackson-databind` still present; verify no group headers appear for single-artifact groups

- [ ] T011 [P] [US1] Create `src/test/kotlin/dev/zahaand/projectscan/ui/ScanResultRendererSmokeTest.kt` — US1 smoke:
  - Change `renderStack`, `renderStructure`, `renderTests` in `ScanResultRenderer.kt` from `private` to `internal` to allow direct test-module access without IntelliJ platform startup
  - Call `ScanResultRenderer.renderStack()` with a fabricated `StackInfo` containing one uniform group (5 artifacts, same non-null version)
  - Assert positive: group header `groupId:* @ version` present in output
  - Assert negative: version NOT repeated on any artifact line below the header

**Checkpoint**: `./gradlew :prompt:test` — all PromptGenerator tests pass, including new US1 scenarios.

---

## Phase 4: User Story 2 — Project Structure Discrepancies (Priority: P1)

**Goal**: Both consumers render Project Structure without per-module dependency lists; render module names, inter-module graph, package segments, root packages, and version discrepancies block (with `none` notice when empty).

**Independent Test**: Fabricate a `StructureInfo` where `api` declares `mapstruct:1.5.5` and `core` declares `mapstruct:1.6.0`, and both declare `jackson-databind:2.17.0`. Assert: no per-module dependency lines; `mapstruct` discrepancy entry present as `org.mapstruct:mapstruct → {api: 1.5.5, core: 1.6.0}`; `jackson-databind` NOT in discrepancy block; module graph rendered; `none` notice when all versions agree.

- [ ] T012 [US2] Update `PromptGenerator.buildProjectStructureBlock()` in `prompt/src/main/kotlin/dev/zahaand/projectscan/prompt/PromptGenerator.kt`:
  - Remove per-module `declaredDependencies` rendering entirely
  - Render module names: `"- Module: ${module.name}"` for each module
  - Render inter-module graph: for modules with non-empty `moduleDependencies`, emit `"  - ${module.name} → [${module.moduleDependencies.joinToString(", ")}]"`
  - Render package segments (FR-014): if `info.packageSegments.isNotEmpty()`, emit `"- Package segments: ${info.packageSegments.joinToString(", ")}"`
  - Render root packages (unchanged logic)
  - Render version discrepancies using `detectVersionDiscrepancies(info.modules)`: emit header `"- Version discrepancies:"`; for each `VersionDiscrepancy` emit `"  - ${d.groupId}:${d.artifactId} → {${d.versions.entries.sortedBy { it.key }.joinToString(", ") { "${it.key}: ${it.value}" }}}"` ; if result is empty emit `"  - none"`

- [ ] T013 [P] [US2] Update `ScanResultRenderer.renderStructure()` in `src/main/kotlin/dev/zahaand/projectscan/ui/ScanResultRenderer.kt`:
  - Same structural changes as T012 with identical text format (byte-identical section body per NFR-001/SC-006)

- [ ] T014 [P] [US2] Add US2 scenarios to `prompt/src/test/kotlin/dev/zahaand/projectscan/prompt/PromptGeneratorOutputReadabilityTest.kt`:
  - Scenario US2-1: `api` and `core` both have `jackson-databind:2.17.0` → `jackson-databind` NOT in version discrepancies block
  - Scenario US2-2: `api` has `mapstruct:1.5.5`, `core` has `mapstruct:1.6.0` → discrepancy block contains `org.mapstruct:mapstruct → {api: 1.5.5, core: 1.6.0}`
  - Scenario US2-3: all modules agree on all versions → discrepancy block contains `none` notice
  - Scenario US2-4: modules with non-empty `moduleDependencies` → inter-module graph line `moduleName → [dep1, dep2]` present
  - Scenario US2-5: non-empty `packageSegments` → `Package segments:` line present; empty `packageSegments` → line absent
  - All scenarios: assert no per-module `declaredDependencies` lines appear

- [ ] T015 [P] [US2] Add US2 smoke tests to `src/test/kotlin/dev/zahaand/projectscan/ui/ScanResultRendererSmokeTest.kt`:
  - Positive: version discrepancy entry present when two modules disagree on a version
  - Negative: same-version artifact NOT in discrepancy block; no `Dependency:` or `  Dependency:` lines in output

**Checkpoint**: `./gradlew :prompt:test` — all tests including US2 scenarios pass.

---

## Phase 5: User Story 3 — Testing Section Readability (Priority: P2)

**Goal**: Both consumers render Testing section with deduplicated frameworks and normalised source-root templates with counts. No absolute paths; one line per distinct framework; one template line per distinct relative suffix.

**Independent Test**: Fabricate a `TestInfo` with 80 identical `JUnit Jupiter 5.10.2` entries and 80 absolute paths sharing prefix `/home/ci/workspace/myapp/` with suffix `src/test/java`. Assert: exactly one `Framework: JUnit Jupiter 5.10.2` line; exactly one source-root line containing `src/test/java` and `80 modules`; no absolute path prefix shown.

- [ ] T016 [US3] Update `PromptGenerator.buildTestingBlock()` in `prompt/src/main/kotlin/dev/zahaand/projectscan/prompt/PromptGenerator.kt`:
  - Replace `info.frameworks.forEach` with `deduplicateFrameworks(info.frameworks).forEach`
  - Replace the `info.sourceRoots.isNotEmpty()` / `joinToString` block with `normalizeSourceRoots(info.sourceRoots)`: for each `SourceRootTemplate` emit `"- Source Roots: ${t.relativePath}${if (t.count > 1) " — ${t.count} modules" else ""}"` (omit count suffix when count == 1)

- [ ] T017 [P] [US3] Update `ScanResultRenderer.renderTests()` in `src/main/kotlin/dev/zahaand/projectscan/ui/ScanResultRenderer.kt`:
  - Same `deduplicateFrameworks()` and `normalizeSourceRoots()` calls with identical rendering format as T016 (byte-identical section body per NFR-001/SC-006)

- [ ] T018 [P] [US3] Add US3 scenarios to `prompt/src/test/kotlin/dev/zahaand/projectscan/prompt/PromptGeneratorOutputReadabilityTest.kt`:
  - Scenario US3-1: 80 identical `JUnit Jupiter 5.10.2` entries → exactly one `Framework: JUnit Jupiter 5.10.2` line
  - Scenario US3-2: two absolute paths sharing prefix, both ending in `src/test/java` → one line containing `src/test/java` and `2 modules`; absolute prefix NOT in output
  - Scenario US3-3: two paths with distinct suffixes `src/test/java` and `src/test/kotlin` → two template lines, each with its own count
  - Scenario US3-4: two distinct frameworks (e.g. JUnit Jupiter + Mockito at different versions) → both appear exactly once

- [ ] T019 [P] [US3] Add US3 smoke tests to `src/test/kotlin/dev/zahaand/projectscan/ui/ScanResultRendererSmokeTest.kt`:
  - Positive: collapsed source-root line with `— 80 modules` suffix present
  - Negative: absolute path prefix NOT present in output

**Checkpoint**: `./gradlew :prompt:test` — all US3 scenarios pass.

---

## Final Phase: Validation & Regression

**Purpose**: Confirm SC-005 (no regressions), SC-006 (byte-identical section bodies on non-empty data), and NFR-001 (determinism).

- [ ] T020 Run `./gradlew :shared:test :prompt:test` and verify: all four existing PromptGenerator test classes (`FullModel`, `EmptyModel`, `LanguageLevelFilter`, `PriorityHierarchy`) still pass with zero failures
- [ ] T021 [P] Run `./gradlew test` (root project) — `ScanResultRendererSmokeTest` passes for all three sections
- [ ] T022 [P] SC-006 verification: write a single shared `ScanResult` fixture (non-empty: ≥2 groupId groups, ≥2 modules with version discrepancy, ≥2 source roots) and assert that `PromptGenerator` section text equals `ScanResultRenderer.renderStack/renderStructure/renderTests` output for each section

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Requires Phase 1 complete — blocks all user stories
- **Phase 3 (US1)**: Requires Phase 2 complete
- **Phase 4 (US2)**: Requires Phase 3 complete (both consumers: same files, must be sequential)
- **Phase 5 (US3)**: Requires Phase 4 complete
- **Final**: Requires all US phases complete

### Parallel Opportunities within Each Story

| Story | Anchor task | Parallel after anchor |
|-------|------------|----------------------|
| US1 | T007 | T008, T009, T010, T011 |
| US2 | T012 | T013, T014, T015 |
| US3 | T016 | T017, T018, T019 |
| Final | T020 | T021, T022 |

### Critical Constraint

US1 → US2 → US3 are sequential because all three modify the same two files (`PromptGenerator.kt` and `ScanResultRenderer.kt`). Each story modifies different methods within those files, so sequential edits are safe and clean.

---

## Implementation Strategy

### MVP (US1 + US2 — highest readability value)

1. Phase 1 → Phase 2 → Phase 3 (US1) → Phase 4 (US2)
2. Validate: `./gradlew :shared:test :prompt:test`
3. Tech Stack grouping + Project Structure discrepancies deliver the largest output volume reduction

### Full Sprint

1. Complete all 5 phases sequentially
2. Run `./gradlew :shared:test :prompt:test` after each US phase
3. Final phase confirms no regressions and SC-006 compliance

---

## Notes

- `[P]` tasks touch different files — safe to work on simultaneously after their anchor task
- `renderStack`, `renderStructure`, `renderTests` in `ScanResultRenderer` must become `internal` (T011) to enable direct smoke testing without IntelliJ platform startup
- SC-006/NFR-001 byte-identical constraint applies to section body strings on non-empty `ScanResult` only; empty/error wrapper strings differ between consumers by design (FR-011)
- Source-root count suffix `— N modules` is omitted when `N == 1`
- `data-model.md` notes "preserving insertion order" for `groupDependencies` — superseded by FR-001 lexicographic ordering; implement as sorted, not insertion-order
