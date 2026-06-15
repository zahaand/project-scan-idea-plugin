---

description: "Task list for Baseline — Static Curated Code-Quality Rules Module"
---

# Tasks: Baseline — Static Curated Code-Quality Rules Module

**Input**: Design documents from `/specs/003-baseline-rules-module/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, quickstart.md ✅

**Tests**: Included — test files are explicitly specified in plan.md and quickstart.md (BaselineRuleProviderTest.kt, BaselineRuleMetadataTest.kt, BaselineRuleCoverageTest.kt).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Include exact file paths in all descriptions

## Path Conventions

Gradle submodule layout: `baseline/src/main/kotlin/dev/zahaand/projectscan/baseline/` for sources,
`baseline/src/main/resources/dev/zahaand/projectscan/baseline/` for resources,
`baseline/src/test/kotlin/dev/zahaand/projectscan/baseline/` for tests.

---

## Phase 1: Setup (Gradle Submodule Wiring)

**Purpose**: Register `:baseline` as a Gradle submodule and configure its build script

- [X] T001 Update `settings.gradle.kts`: add `include(":baseline")` to the includes block and add `id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"` inside the `pluginManagement.plugins` block (same version as the Kotlin JVM plugin)
- [X] T002 [P] Create `baseline/build.gradle.kts`: apply plugins `kotlin("jvm")`, `id("org.jetbrains.kotlin.plugin.serialization")`, `id("io.gitlab.arturbosch.detekt")`, `id("org.jlleitschuh.gradle.ktlint")`; add dependencies `implementation(kotlin("stdlib"))`, `implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")`, `testImplementation(libs.junit.jupiter)`, `testRuntimeOnly("org.junit.platform:junit-platform-launcher")`; configure `tasks.test { useJUnitPlatform() }` and `detekt { config.setFrom(files("config/detekt.yml")); buildUponDefaultConfig = true }`
- [X] T003 [P] Create the source directory tree: `baseline/src/main/kotlin/dev/zahaand/projectscan/baseline/`, `baseline/src/main/resources/dev/zahaand/projectscan/baseline/`, and `baseline/src/test/kotlin/dev/zahaand/projectscan/baseline/`

---

## Phase 2: Foundational (Data Types — Blocking Prerequisites)

**Purpose**: Define all public types that `BaselineRuleProvider`, test classes, and future consumers (`：prompt`) depend on

**⚠️ CRITICAL**: No user story implementation can begin until these types exist

- [X] T004 [P] Create `baseline/src/main/kotlin/dev/zahaand/projectscan/baseline/BaselineRule.kt`: define `@Serializable enum class BaselineLevel { CORRECTNESS, BEST_PRACTICE }`, `@Serializable enum class BaselineCategory { NULL_SAFETY, RESOURCE_MANAGEMENT, CONCURRENCY, DANGEROUS_CONSTRUCTS, EXCEPTION_HANDLING, STRING_PERFORMANCE, DECOMPOSITION, IMMUTABILITY, INTERFACE_PROGRAMMING }`, `@Serializable enum class Obligation { MUST, SHOULD }`, `@Serializable enum class BaselineLanguage { JAVA }`, and `@Serializable data class BaselineRule(val id: String, val level: BaselineLevel, val category: BaselineCategory, val obligation: Obligation, val statement: String, val rationale: String, val minJavaLevel: Int, val languages: List<BaselineLanguage>)`
- [X] T005 [P] Create `baseline/src/main/kotlin/dev/zahaand/projectscan/baseline/BaselineLoadException.kt`: define `class BaselineLoadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)` — the single unchecked failure signal for all load and validation errors in `:baseline`

**Checkpoint**: Data types compile — Phase 3 implementation can now begin

---

## Phase 3: User Stories 1 & 2 — Load, Validate, and Consume (Priority: P1) 🎯 MVP

**Goal**: Implement the provider that loads and caches the bundled rule set; author `rules.json` with full coverage; verify the provider contract through all 12 acceptance scenarios (US1 scenarios 1–9 and US2 scenarios 1–3)

**Independent Test**: `./gradlew :baseline:test --tests "*.BaselineRuleProviderTest"` — all scenarios pass with no IntelliJ Platform fixtures

### Tests for User Stories 1 & 2

> **NOTE: Write these tests FIRST — they will fail until T007 (provider) and T008 (rules.json) are complete**

- [ ] T006 [US1] [US2] Write `baseline/src/test/kotlin/dev/zahaand/projectscan/baseline/BaselineRuleProviderTest.kt` covering all 12 acceptance scenarios: US1 scenario 1 — `BaselineRuleProvider.rules` returns a non-empty list with no exception; US1 scenarios 2–9 via `loadFromReader(StringReader(json))` — throws `BaselineLoadException` for: blank `statement`, blank `id`, duplicate `id`, malformed JSON, invalid `minJavaLevel` (e.g. `7`), empty `rules` array, `category`/`level` mismatch (EXCEPTION_HANDLING + CORRECTNESS), empty `languages` list; US2 scenario 1 — total count ≥13 and matches the count in `rules.json` exactly; US2 scenario 2 — rules with `minJavaLevel` of both 8 and 11 (or higher) are present in the returned list; US2 scenario 3 — two successive accesses to `BaselineRuleProvider.rules` return the same list instance (`assertSame`)

### Implementation for User Stories 1 & 2

- [ ] T007 [P] [US1] Author `baseline/src/main/resources/dev/zahaand/projectscan/baseline/rules.json` with `schemaVersion: 1` wrapper; ≥13 rules total; ≥2 rules each for `NULL_SAFETY`, `RESOURCE_MANAGEMENT`, `CONCURRENCY`, `DANGEROUS_CONSTRUCTS`; ≥1 rule each for `EXCEPTION_HANDLING`, `STRING_PERFORMANCE`, `DECOMPOSITION`, `IMMUTABILITY`, `INTERFACE_PROGRAMMING`; all `id` values unique and non-blank; all `statement` and `rationale` values non-blank; all `minJavaLevel` values in `{8, 11, 17, 21}`; at least one rule with `minJavaLevel > 8`; every `category` consistent with its `level` per the FR-008 mapping
- [ ] T008 [US1] Implement `baseline/src/main/kotlin/dev/zahaand/projectscan/baseline/BaselineRuleProvider.kt` as a Kotlin `object`: define `internal @Serializable data class RuleSet(val schemaVersion: Int, val rules: List<BaselineRule>)`; `private val jsonParser = Json { ignoreUnknownKeys = true }`; `CATEGORY_LEVEL_MAP` (all 9 entries per FR-008); `ALLOWED_JAVA_LEVELS = setOf(8, 11, 17, 21)`; `RESOURCE_PATH = "/dev/zahaand/projectscan/baseline/rules.json"`; `SUPPORTED_SCHEMA_VERSION = 1`; `internal fun loadFromReader(reader: Reader)` with ordered validation: (1) structural parse via `jsonParser.decodeFromString<RuleSet>` — wrap `SerializationException` in `BaselineLoadException` with non-null cause, (2) schema version check (`!= 1` → throw), (3) non-empty rules array check, (4) per-rule invariants — check `id.isBlank()` before uniqueness, then uniqueness, then `statement.isBlank()`, `rationale.isBlank()`, `minJavaLevel !in ALLOWED_JAVA_LEVELS`, `languages.isEmpty()`, and category/level consistency; `private fun loadRules()` via `BaselineRuleProvider::class.java.getResourceAsStream(RESOURCE_PATH)` (null → throw); `val rules: List<BaselineRule> by lazy { loadRules() }`

**Checkpoint**: `./gradlew :baseline:test --tests "*.BaselineRuleProviderTest"` passes — US1 and US2 fully functional; `:prompt` (Sprint 4) can already integrate

---

## Phase 4: User Story 3 — Rule Metadata Completeness (Priority: P2)

**Goal**: Assert that every field of every bundled rule satisfies its invariant; all assertions operate on the real bundled `rules.json` — no test-injected JSON substitutions (contrast with US1 negative scenarios)

**Independent Test**: `./gradlew :baseline:test --tests "*.BaselineRuleMetadataTest"` — passes without IntelliJ Platform fixtures

### Tests for User Story 3

- [ ] T009 [US3] Write `baseline/src/test/kotlin/dev/zahaand/projectscan/baseline/BaselineRuleMetadataTest.kt`: load `BaselineRuleProvider.rules`; iterate every rule and assert: `id.isNotBlank()`, `statement.isNotBlank()`, `rationale.isNotBlank()`, `level` is one of `{CORRECTNESS, BEST_PRACTICE}`, `category` is one of the 9 `BaselineCategory` values, `obligation` is one of `{MUST, SHOULD}`, `minJavaLevel` is in `{8, 11, 17, 21}`, `languages` is non-empty, category is consistent with level per the FR-008 mapping (CORRECTNESS-level categories paired with CORRECTNESS; BEST_PRACTICE-level categories paired with BEST_PRACTICE); plus one cross-cutting assertion: `rules.any { it.minJavaLevel > 8 }` is true (SC-007)

**Checkpoint**: All 7 per-field invariants confirmed correct on the real bundled rule set

---

## Phase 5: User Story 4 — Set Composition Coverage (Priority: P2)

**Goal**: Assert the bundled set covers all 9 required categories with at least the minimum rule counts; grouping uses the `category` field — NOT id string patterns

**Independent Test**: `./gradlew :baseline:test --tests "*.BaselineRuleCoverageTest"` — passes without IntelliJ Platform fixtures

### Tests for User Story 4

- [ ] T010 [US4] Write `baseline/src/test/kotlin/dev/zahaand/projectscan/baseline/BaselineRuleCoverageTest.kt`: load `BaselineRuleProvider.rules` and `groupBy { it.category }`; assert ≥2 for each of `NULL_SAFETY`, `RESOURCE_MANAGEMENT`, `CONCURRENCY`, `DANGEROUS_CONSTRUCTS`; assert ≥1 for each of `EXCEPTION_HANDLING`, `STRING_PERFORMANCE`, `DECOMPOSITION`, `IMMUTABILITY`, `INTERFACE_PROGRAMMING`; assert `rules.size >= 13`

**Checkpoint**: All 9 category coverage requirements verified; US3 and US4 complete the quality bar for the curated rule set

---

## Phase 6: Polish & Verification

**Purpose**: Confirm all 7 success criteria (SC-001–SC-007) are met; static analysis gates pass before merge

- [ ] T011 [P] Run `./gradlew :baseline:build` and confirm: compilation succeeds with zero errors, detekt reports no violations against `config/detekt.yml`, ktlint reports no formatting violations
- [ ] T012 [P] Run `./gradlew :baseline:dependencies` and verify that `:scan` and all IntelliJ Platform artifacts (`com.jetbrains.intellij.*`, `org.jetbrains.intellij.*`, `com.intellij.*`) are absent from the dependency tree output (SC-005)
- [ ] T013 Run `./gradlew :baseline:test` and verify: all tests in `BaselineRuleProviderTest`, `BaselineRuleMetadataTest`, and `BaselineRuleCoverageTest` pass; test report shows only JUnit 5 tests with no `LightPlatformTestCase` or `BasePlatformTestCase` in the output (SC-006); exit code is 0

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (module directory and build script must exist)
- **US1 & US2 (Phase 3)**: Depends on Phase 2 (`BaselineRule`, `BaselineLoadException` must be defined before `BaselineRuleProvider` or test code can compile)
- **US3 (Phase 4)**: Depends on Phase 3 (`BaselineRuleProvider.rules` must load the real bundled rules)
- **US4 (Phase 5)**: Depends on Phase 3 — same prerequisite as US3; US4 and US3 can run in parallel (different test files)
- **Polish (Phase 6)**: Depends on Phases 4 and 5 complete

### User Story Dependencies

- **US1 & US2 (P1)**: Start after Phase 2 — no dependency on US3 or US4
- **US3 (P2)**: Start after Phase 3 complete — can run in parallel with US4
- **US4 (P2)**: Start after Phase 3 complete — can run in parallel with US3

### Within Phase 3

- T006 (write tests first — they fail until T007+T008 are done)
- T007 (author rules.json) and T008 (implement provider) can be written in parallel with each other
- T006 tests pass only after both T007 and T008 are complete

### Parallel Opportunities

- Phase 1: T002 and T003 can run in parallel (after T001 is done)
- Phase 2: T004 and T005 can run in parallel
- Phase 3: T007 (rules.json) and T008 (provider) can be written in parallel
- Phase 4 and Phase 5 can run in parallel (different test files, no shared state)
- Phase 6: T011 and T012 can run in parallel

---

## Parallel Example: Phase 3 (US1 & US2)

```bash
# Step 1: Write failing tests first (TDD)
T006: Write BaselineRuleProviderTest.kt — compile fails until provider exists

# Step 2: Author rules.json and implement provider in parallel (different files)
T007: Author rules.json with ≥13 curated rules covering all 9 categories
T008: Implement BaselineRuleProvider.kt with full parse + validate pipeline

# Step 3: Verify (after T006 + T007 + T008 are all done)
./gradlew :baseline:test --tests "*.BaselineRuleProviderTest"
```

## Parallel Example: Phases 4 & 5 (US3 & US4)

```bash
# Both phases can run concurrently after Phase 3 completes:
T009: Write BaselineRuleMetadataTest.kt (per-field invariants on real rules)
T010: Write BaselineRuleCoverageTest.kt (category counts on real rules)
```

---

## Implementation Strategy

### MVP First (US1 + US2 Only)

1. Complete Phase 1: Setup (Gradle wiring — `include(":baseline")` + build file)
2. Complete Phase 2: Foundational (data types — `BaselineRule`, enums, `BaselineLoadException`)
3. Complete Phase 3: US1 & US2 (rules.json + provider + provider tests)
4. **STOP and VALIDATE**: `./gradlew :baseline:test --tests "*.BaselineRuleProviderTest"` passes
5. `:prompt` (Sprint 4) can already depend on `:baseline` and call `BaselineRuleProvider.rules`

### Incremental Delivery

1. Setup + Foundational → module compiles, types are available
2. Phase 3 (US1+US2) → provider contract verified; ≥13 rules loading; MVP deliverable
3. Phase 4 (US3) → per-rule metadata invariants verified on real bundled data
4. Phase 5 (US4) → category coverage verified on real bundled data
5. Phase 6 → build + dependency + test gates pass → ready to merge to `main`

---

## Notes

- [P] tasks operate on different files with no shared state — safe for concurrent execution
- [Story] label maps each task to its user story for traceability
- T007 (rules.json) is the only task requiring prose judgment — actual `statement` and `rationale` wording is a curation decision; all other tasks are mechanical
- `loadFromReader` is `internal` — all US1 negative-path tests use `StringReader` injection; no classloader manipulation needed (R-002)
- US2 scenario 3 (caching) MUST use `assertSame`, not `assertEquals` — the assertion is referential equality, not structural equality
- Category/level consistency is enforced by the provider at load time (T008) and re-asserted by `BaselineRuleMetadataTest` (T009) — intentional belt-and-suspenders for a critical invariant (FR-016)
- The blank-`id` check in `validateRules` MUST run before the uniqueness check (seenIds.add) — a blank id must be rejected on its own merits, not caught as a duplicate
