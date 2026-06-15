---

description: "Task list for :prompt module — Constitution Prompt Generator"
---

# Tasks: Prompt — Constitution Prompt Generator Module

**Input**: Design documents from `/specs/004-prompt-module-generator/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/prompt-api.md, research.md, quickstart.md

**Tests**: Included — FR-011 mandates JUnit 5 unit tests; plan.md names four test files aligned to the four User Stories.

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US4)

---

## Phase 1: Setup (Gradle Submodule Registration)

**Purpose**: Register the `:prompt` submodule and create the Gradle build configuration so the module compiles against `:model` and `:baseline`.

- [x] T000 Verify `:model` and `:baseline` have no transitive `com.intellij.*` dependency: run `./gradlew :model:dependencies :baseline:dependencies --configuration compileClasspath` and confirm no `com.intellij.*` JAR appears — if found, this is a **BLOCKER** and implementation MUST NOT proceed until the upstream module removes the dependency
- [x] T001 Register `:prompt` by adding `include(":prompt")` to `settings.gradle.kts`
- [x] T002 [P] Create `prompt/build.gradle.kts` mirroring `:baseline` — plugins: kotlin jvm + detekt + ktlint; deps: kotlin stdlib, `project(":model")`, `project(":baseline")`, `libs.junit.jupiter` (test), `junit-platform-launcher` (testRuntime); `tasks.test { useJUnitPlatform() }`; detekt config from `rootProject.files("config/detekt.yml")`
- [x] T003 Create source directory tree: `prompt/src/main/kotlin/dev/zahaand/projectscan/prompt/` and `prompt/src/test/kotlin/dev/zahaand/projectscan/prompt/`

**Checkpoint**: `./gradlew :prompt:compileKotlin` succeeds (empty source tree is fine at this stage)

---

## Phase 2: Foundational (Shared Value Types)

**Purpose**: Establish the three output value types (`PromptBlock`, `OriginGroup`, `ConstitutionPrompt`) that every user story phase depends on. Must be complete before US1 implementation begins.

**⚠️ CRITICAL**: No user story implementation can begin until this phase is complete.

- [x] T004 [P] Create `PromptBlock.kt` as a `data class` with `heading: String` and `content: String` in `prompt/src/main/kotlin/dev/zahaand/projectscan/prompt/PromptBlock.kt`
- [x] T005 [P] Create `OriginGroup.kt` as a `data class` with `label: String`, `mandatoryRules: List<String>`, `advisoryRules: List<String>`, `emptyNotation: String?` in `prompt/src/main/kotlin/dev/zahaand/projectscan/prompt/OriginGroup.kt`
- [x] T006 Create `ConstitutionPrompt.kt` with `class ConstitutionPrompt(val blocks: List<PromptBlock>)` and a `render(): String` stub that returns an empty string in `prompt/src/main/kotlin/dev/zahaand/projectscan/prompt/ConstitutionPrompt.kt`

**Checkpoint**: `./gradlew :prompt:compileKotlin` still succeeds with the three types present

---

## Phase 3: User Story 1 — Generate a Complete Prompt from a Fully-Scanned Project (Priority: P1) 🎯 MVP

**Goal**: `PromptGenerator.generate()` returns a `ConstitutionPrompt` whose rendered text: opens with an explicit instructional address to `/speckit-constitution`, contains all six `##` headings, tags every linter rule as `"project standard"` with `MUST`/`SHOULD` obligation markers on each baseline rule bullet, and emits all `StackInfo` fields verbatim in the Tech Stack block.

**Independent Test**: Run `./gradlew :prompt:test --tests "*.PromptGeneratorFullModelTest"` — all five acceptance scenarios must pass.

### Tests for User Story 1 ⚠️ Write FIRST — verify they FAIL before implementation

- [x] T007 [US1] Write `PromptGeneratorFullModelTest.kt` in `prompt/src/test/kotlin/dev/zahaand/projectscan/prompt/` covering all five US1 acceptance scenarios: (1) six `##` headings present and rendered text opens with an instructional address containing `/speckit-constitution` (FR-002); (2) all five linter rules appear under `### project standard`; (3) all 13 baseline rules appear under `### baseline quality requirement` each prefixed with their `MUST` or `SHOULD` obligation marker; (4) `### project standard` precedes `### baseline quality requirement` by character offset; (5) Tech Stack block contains build system, JDK version, language level, and all declared dependencies verbatim

### Implementation for User Story 1

- [x] T008 [US1] Create `PromptGenerator.kt` with `class PromptGenerator` and the public `fun generate(scanResult: ScanResult, baselineRules: List<BaselineRule>): ConstitutionPrompt` signature in `prompt/src/main/kotlin/dev/zahaand/projectscan/prompt/PromptGenerator.kt` — stub returning `ConstitutionPrompt(emptyList())`
- [x] T009 [US1] Implement `buildCorePrinciplesBlock()` private method in `PromptGenerator.kt`: assemble two `OriginGroup` values — `"project standard"` from `LinterInfo.activeRules` (flat bullet list, no mandatory/advisory split yet), `"baseline quality requirement"` from `baselineRules` where each bullet is prefixed with the rule's `obligation` marker (`MUST` for `Obligation.MUST`, `SHOULD` for `Obligation.SHOULD`); `### project standard` group always precedes `### baseline quality requirement`; include `emptyNotation` when a group has no rules; separate the two `###` groups with exactly one blank line
- [x] T010 [US1] Implement `buildTechStackBlock()` private method in `PromptGenerator.kt`: when stack section is `Ok`, emit build system, JDK version, language level, and all `Dependency` entries as bullet items; when `Empty`, emit exactly `"not detected"`; when `Error` with non-null cause emit `"not available (cause: <cause>)"`, when `Error` with null cause emit `"not available"`
- [x] T011 [US1] Implement `buildCodeStyleBlock()` private method in `PromptGenerator.kt`: when `Ok`, emit each `StyleSource` (type + path) as a bullet; when `Empty`, emit `"not detected"`; when `Error`, emit `"not available"` (plus cause per FR-008)
- [x] T012 [US1] Implement `buildTestingBlock()` private method in `PromptGenerator.kt`: when `Ok`, emit each `TestFramework` (name + version), source roots, naming suffixes, and coverage threshold as bullets; when `Empty`, emit `"not detected"`; when `Error`, emit `"not available"` (plus cause per FR-008)
- [x] T013 [US1] Implement `buildProjectStructureBlock()` private method in `PromptGenerator.kt`: when `Ok`, emit each `Module` (name, declared dependencies, module dependencies) and root packages as bullets; when `Empty`, emit `"not detected"`; when `Error`, emit `"not available"` (plus cause per FR-008)
- [x] T014 [US1] Implement `buildGovernanceBlock()` private method in `PromptGenerator.kt` with fixed content that MUST contain all three required elements (exact phrasing is an implementation choice): (1) a constitution semantic-versioning policy stating when MAJOR, MINOR, and PATCH version bumps apply; (2) a changelog convention; (3) an amendment and compliance procedure
- [x] T015 [US1] Wire all six block builders into `generate()`: call each builder in canonical order (Core Principles → Tech Stack → Code Style & Static Analysis → Testing → Project Structure → Governance), wrap each in a `PromptBlock`, return `ConstitutionPrompt(blocks)`; implement `ConstitutionPrompt.render()` to: open with an explicit instructional preamble addressed to `/speckit-constitution`, then emit each block as `## {heading}\n\n{content}` separated by exactly one blank line; within Core Principles, `###` groups are separated by exactly one blank line

**Checkpoint**: `./gradlew :prompt:test --tests "*.PromptGeneratorFullModelTest"` — all five scenarios green

---

## Phase 4: User Story 2 — Priority Hierarchy Is Clearly Expressed (Priority: P1)

**Goal**: The rendered Core Principles block contains explicit conflict-resolution language, and the `"project standard"` group renders `ERROR`/`breaksBuild=true` rules under `#### Mandatory (build-breaking)` and all other rules (including `breaksBuild=null`) under `#### Advisory`. Empty sub-sections are omitted; when both are empty only the `emptyNotation` line is rendered.

**Independent Test**: Run `./gradlew :prompt:test --tests "*.PromptGeneratorPriorityHierarchyTest"` — all three acceptance scenarios must pass.

### Tests for User Story 2 ⚠️ Write FIRST — verify they FAIL before implementation

- [x] T016 [US2] Write `PromptGeneratorPriorityHierarchyTest.kt` in `prompt/src/test/kotlin/dev/zahaand/projectscan/prompt/` covering all three US2 acceptance scenarios: (1) rendered Core Principles block contains explicit conflict-resolution wording stating project standard rules win over baseline; (2) `#### Mandatory (build-breaking)` sub-section present for `ERROR`/`breaksBuild=true` rules, `#### Advisory` for all others including `breaksBuild=null` — no exception thrown for null; (3) every `-` bullet within `## Core Principles` appears under a `###` heading whose text is exactly `"project standard"` or `"baseline quality requirement"` (SC-002)

### Implementation for User Story 2

- [x] T017 [US2] Refactor `buildCorePrinciplesBlock()` in `PromptGenerator.kt` to split the `"project standard"` group into mandatory/advisory sub-sections using `isMandatory(rule): Boolean = rule.severity == RuleSeverity.ERROR || rule.breaksBuild == true`; populate `OriginGroup.mandatoryRules` and `OriginGroup.advisoryRules`; render `#### Mandatory (build-breaking)` only when non-empty; render `#### Advisory` only when non-empty; when both are empty render only the `emptyNotation` line with no `####` headings at all
- [x] T018 [US2] Add explicit conflict-resolution preamble text to the Core Principles block content in `PromptGenerator.kt`: state that project standard rules take precedence over baseline quality requirements in case of conflict, and that baseline quality requirements take precedence over unwritten team practice

**Checkpoint**: `./gradlew :prompt:test --tests "*.PromptGeneratorPriorityHierarchyTest"` — all three scenarios green; US1 tests still pass

---

## Phase 5: User Story 3 — Baseline Rules Filtered by Java Language Level (Priority: P2)

**Goal**: Baseline rules whose `minJavaLevel` exceeds the leading decimal integer extracted from `StackInfo.languageLevel` are excluded. No filtering is applied when the level is null, `""` (empty string), absent, has no leading digit, or the stack section is Empty/Error. Any extracted integer (including non-LTS values like 9, 16) is used as-is for comparison.

**Independent Test**: Run `./gradlew :prompt:test --tests "*.PromptGeneratorLanguageLevelFilterTest"` — all nine acceptance scenarios plus SC-004 boundary cases must pass.

### Tests for User Story 3 ⚠️ Write FIRST — verify they FAIL before implementation

- [x] T019 [US3] Write `PromptGeneratorLanguageLevelFilterTest.kt` in `prompt/src/test/kotlin/dev/zahaand/projectscan/prompt/` covering all nine US3 acceptance scenarios: (1) `"11"` → filters minJavaLevel > 11; (2) `"21"` → all pass; (3) `"8"` → only minJavaLevel=8 pass; (4) `null` → full set; (5) `"unknown"` → full set; (6) stack section `Empty` → full set; (7) `"17.0.1"` extracts 17 and `"21_PREVIEW"` extracts 21; (8) `" 11"` (leading whitespace) → extracts 11, filters minJavaLevel > 11; (9) `"11a"` (digits then non-digit) → extracts 11, filters minJavaLevel > 11 — plus SC-004 case (e): `""` (empty string) → full set emitted

### Implementation for User Story 3

- [x] T020 [US3] Implement `extractLanguageLevel(languageLevel: String?): Int?` private function in `PromptGenerator.kt`: returns `null` when input is null or blank; returns `null` when `trimStart().takeWhile { it.isDigit() }` yields an empty string (e.g., `"unknown"`, `""`); otherwise returns the extracted digits as `Int` — no regex, pure `takeWhile` (research.md Finding 2)
- [x] T021 [US3] Integrate filtering into `generate()` in `PromptGenerator.kt`: extract the language level from `StackInfo.languageLevel` (skip extraction when stack section is `Empty`/`Error`); filter `baselineRules` retaining only `rule.minJavaLevel <= extractedLevel` (keep all when `extractedLevel` is null); pass the filtered list to `buildCorePrinciplesBlock()`

**Checkpoint**: `./gradlew :prompt:test --tests "*.PromptGeneratorLanguageLevelFilterTest"` — all scenarios green; US1 and US2 tests still pass

---

## Phase 6: User Story 4 — Minimal/Empty Project Yields an Honest Baseline-Only Prompt (Priority: P2)

**Goal**: When all five `ScanResult` sections are `SectionResult.Empty`, the generator returns a valid prompt: six headings, all baseline rules in Core Principles with obligation markers, `"project standard"` group shows `emptyNotation`, and each scan-dependent block contains exactly `"not detected"`. For `SectionResult.Error`: non-null cause renders as `"not available (cause: X)"`, null cause renders as plain `"not available"` — never `"(cause: null)"`.

**Independent Test**: Run `./gradlew :prompt:test --tests "*.PromptGeneratorEmptyModelTest"` — all five acceptance scenarios must pass.

### Tests for User Story 4 ⚠️ Write FIRST — verify they FAIL before implementation

- [x] T022 [US4] Write `PromptGeneratorEmptyModelTest.kt` in `prompt/src/test/kotlin/dev/zahaand/projectscan/prompt/` covering all five US4 acceptance scenarios: (1) all-Empty scan + non-empty baseline → non-empty prompt with six headings; (2) `"project standard"` group present with emptyNotation, not omitted; (3) all baseline rules with obligation markers in Core Principles (no filtering); (4) Tech Stack, Code Style, Testing, Project Structure each contain exactly `"not detected"` (not "not available", not inferred data); (5) mixed Ok/Error scan: Ok sections contribute data, Error sections show `"not available"` + cause when non-null or plain `"not available"` when cause is null — never `"(cause: null)"` — no exception thrown

### Implementation for User Story 4

- [x] T023 [US4] Audit all six block builders in `PromptGenerator.kt` and verify every `SectionResult.Empty` branch emits exactly `"not detected"` and every `SectionResult.Error` branch emits `"not available (cause: ${error.cause})"` when `error.cause != null` or plain `"not available"` when `error.cause == null` — add or fix missing cases; the centralised `formatError()` helper from Phase 3 is the single enforcement point
- [x] T024 [US4] ~~Separate cause-null guard task — subsumed by T023~~. Subsumed by T023; automatically satisfied when T022 scenario 5 (mixed Ok/Error) passes — no independent work.

**Checkpoint**: `./gradlew :prompt:test` — all tests across the four test classes pass

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Static analysis, style compliance, and final dependency verification.

- [ ] T025 [P] Run `./gradlew :prompt:test` and resolve any remaining failures; SC-007 determinism is already verified by scenario 7 in `PromptGeneratorFullModelTest` (added during post-US1 corrections): calls `generate()` twice with identical inputs and asserts the two `render()` outputs are equal (`assertEquals`) — no additional @Test needed; confirm this scenario is green
- [ ] T026 [P] Run `./gradlew :prompt:detekt :prompt:ktlintCheck` and fix all violations in `prompt/src/main/kotlin/` and `prompt/src/test/kotlin/`
- [ ] T027 Verify SC-006 compliance: run `./gradlew :prompt:dependencies --configuration compileClasspath` and confirm no `com.intellij.*` JARs appear; confirm `:scan` and `:ui` are absent from all configurations

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T000 is a BLOCKER — if it fails, nothing proceeds. T001–T003 can start after T000 clears.
- **Foundational (Phase 2)**: Depends on Phase 1 completion — **BLOCKS all user stories**
- **US1 (Phase 3)**: Depends on Phase 2 — no dependency on US2, US3, US4
- **US2 (Phase 4)**: Depends on Phase 3 completion (refines Core Principles already built in T009/T017)
- **US3 (Phase 5)**: Depends on Phase 3 completion (adds filtering to `generate()`); can run in parallel with US2 since T020/T021 and T017/T018 touch different areas of `PromptGenerator.kt`
- **US4 (Phase 6)**: Depends on Phase 3 completion; best after US2 and US3 so the full generator behavior is in place before the empty/error tests run
- **Polish (Phase 7)**: Depends on all Phase 3–6 tasks completing

### User Story Dependencies

- **US1 (P1)**: Implements the complete happy-path generator — prerequisite for US2, US3, US4
- **US2 (P1)**: Refines Core Principles rendering (mandatory/advisory sub-sections + conflict text); depends on US1; independent of US3/US4
- **US3 (P2)**: Adds filtering logic to `generate()`; depends on US1; can be done in parallel with US2
- **US4 (P2)**: Hardens empty/error paths; depends on US1; test coverage is most meaningful after US2/US3 behaviors are in place

### Within Each Phase

1. Write the test first — run `./gradlew :prompt:test` and verify it FAILS before writing production code
2. Implement the feature to make tests pass
3. Verify the full test suite after each story phase before moving to the next

### Parallel Opportunities

- T001 (settings.gradle.kts) and T002 (build.gradle.kts) — different files, can run simultaneously
- T004 (PromptBlock.kt) and T005 (OriginGroup.kt) — different files, can run simultaneously
- T010–T014 (block builders in PromptGenerator.kt) — same file; implement sequentially
- T025 (test run) and T026 (linting) — different tools, can run simultaneously

---

## Parallel Example: User Story 1

```bash
# Phase 2 — run together (different files):
# T004: Create PromptBlock.kt
# T005: Create OriginGroup.kt

# Phase 3 — sequential within PromptGenerator.kt:
# T008 → T009 → T010 → T011 → T012 → T013 → T014 → T015
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. T000: Verify platform-cleanliness gate (BLOCKER if fails)
2. Complete Phase 1: Setup (T001–T003)
3. Complete Phase 2: Foundational (T004–T006)
4. Complete Phase 3: User Story 1 (T007–T015)
5. **STOP and VALIDATE**: `./gradlew :prompt:test --tests "*.PromptGeneratorFullModelTest"` — all five scenarios green
6. Proceed to US2 (same priority P1)

### Incremental Delivery

1. T000 + Phase 1 + Phase 2 → Module compiles
2. Phase 3 (US1) → End-to-end generator works for the happy path
3. Phase 4 (US2) → Priority hierarchy, mandatory/advisory split, SC-002 origin-tag coverage
4. Phase 5 (US3) → Language-level filtering active across all 9 scenarios + SC-004 case (e)
5. Phase 6 (US4) → Empty/error resilience; `"not detected"` / `"not available"` markers; cause=null handled
6. Phase 7 → detekt, ktlint, SC-006 dependency check

---

## Notes

- **[P]** tasks = different files, no blocking dependencies
- **[Story]** label maps each task to a specific user story for traceability
- All test tasks MUST be written BEFORE their corresponding implementation tasks and MUST FAIL initially
- `ConstitutionPrompt.render()` output MUST be deterministic — no timestamps, UUIDs, or random values; same input always yields the same string (SC-007)
- No `com.intellij.*` imports anywhere under `prompt/` — any such import is a build violation (FR-001, SC-006)
- No `File`, `Path`, `System.out`, clipboard, or I/O calls in `prompt/src/main/` (FR-001)
- Governance block (T014) MUST contain all three elements: (1) semver policy, (2) changelog convention, (3) amendment procedure — exact wording is an implementation choice (FR-009)
- Baseline rule bullets MUST carry `MUST`/`SHOULD` obligation marker — never omit (Assumption/Obligation mapping)
- `breaksBuild = null` → advisory, NEVER mandatory — no exception may be thrown (FR-006, research.md Finding 3)
- `cause = null` → plain `"not available"` — the string `"(cause: null)"` MUST NEVER appear (FR-008, Edge Cases C3)
- `"not detected"` is the exact phrase for `SectionResult.Empty`; `"not available"` for `SectionResult.Error` — no other phrasing (FR-008, B1)
