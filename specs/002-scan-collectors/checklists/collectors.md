# Data Completeness Checklist: Scan — Project Facts Collectors

**Purpose**: Pre-implementation gate — validates that per-collector data requirements are
complete, unambiguous, and consistent before coding begins. Tests the quality of the
requirements themselves, not the implementation.
**Created**: 2026-06-13
**Depth**: Thorough (pre-implementation gate)
**Feature**: [spec.md](../spec.md)

---

## Stack Collector — Requirement Completeness

- [x] CHK001 — Does the spec define behavior when a dependency's resolved version cannot be
  determined (null or unresolvable)? Should it be included with `null` version, excluded, or
  trigger an error state? [Completeness, FR-001, Gap]

- [x] CHK002 — Are `jdkVersion` and `languageLevel` defined as distinct concepts with separate
  collection requirements, or does "JDK / language level" in FR-004 refer to a single value?
  [Clarity, FR-004, Ambiguity]

- [x] CHK003 — Is the fallback defined when all modules lack an explicit language level (all
  inherit the project default)? Is the project-level default always available? [Completeness,
  FR-005, Gap]

- [x] CHK004 — Does the spec define how non-semver version strings (e.g., `1.0.0.RELEASE`,
  `1.0-SNAPSHOT`, `2.0.0.Final`) are ordered when applying the max-version aggregation rule?
  [Clarity, FR-003, Ambiguity]

- [x] CHK005 — The Assumptions section states "scope is recorded as a fact, not used as a
  filter," but the `Dependency` model has no scope field — is scope collection a missing
  requirement or is the assumption inconsistent with the model contract? [Consistency, FR-001,
  Conflict]

---

## CodeStyle Collector — Requirement Completeness

- [x] CHK006 — Is the reference point for "project-relative file path" (FR-006) explicitly
  defined? (Relative to project root? VCS root? Module root?) [Clarity, FR-006, Ambiguity]

- [x] CHK007 — Does the spec define the file name patterns or discovery criteria for each
  StyleSourceType — specifically which filenames constitute a Checkstyle config, a PMD config,
  and a Spotless config? [Completeness, FR-006, Gap]

- [x] CHK008 — Spotless is a build plugin without a dedicated standalone config file; its
  configuration lives in the build file. Does the spec define how a Spotless StyleSource is
  detected and what path value is recorded for it, given that FR-020 prohibits reading build
  files? [Completeness, FR-006, FR-020, Conflict]

- [x] CHK009 — Is behavior defined for projects with multiple `.editorconfig` files (root-level
  and nested per-directory)? Are all of them collected or only the root? [Completeness, FR-006,
  Gap]

- [x] CHK010 — Does the spec precisely define which file(s) under `.idea/` constitute "IDE style
  settings"? (e.g., `.idea/codeStyles/Project.xml` only, or any file under `.idea/codeStyles/`?)
  [Clarity, FR-006, Ambiguity]

---

## Linter Collector — Requirement Completeness

- [x] CHK011 — Does the spec explicitly enumerate the complete set of linter tools for which rule
  extraction is supported (Checkstyle and PMD only, or are SpotBugs, ErrorProne, or others
  included)? [Completeness, FR-008, Ambiguity]

- [x] CHK012 — Is the default severity defined for a linter rule that declares no explicit
  severity in its config file? FR-009 says "as declared in the tool's config file" but is silent
  on the absence case. [Completeness, FR-009, Gap]

- [x] CHK013 — Does the spec define how "applied" linter state is determined independently for
  Maven and Gradle? (The spec states the outcome but not the criteria by which applied-state is
  judged per build system.) [Completeness, FR-008, Gap]

- [x] CHK014 — Is behavior defined when a Checkstyle config references or imports an external
  config file (e.g., via `<module name="SuppressionFilter">` or a Google Checks reference)?
  Are rules from imported files included in the collected set? [Completeness, FR-008, Gap]

- [x] CHK015 — Is behavior defined when multiple configurations of the same linter tool coexist
  (e.g., separate Checkstyle configurations for main and test source sets)? Is the build-failure
  flag from each configuration captured or only one? [Completeness, FR-010, Gap]

- [x] CHK016 — The spec states the build-failure flag "lives at tool level" and is "denormalized
  onto each ActiveRule." Are the denormalization rules specified clearly enough to produce
  consistent results across Checkstyle and PMD? [Clarity, FR-010, Ambiguity]

---

## Test Collector — Requirement Completeness

- [x] CHK017 — Does the spec enumerate the full closed list of recognised test framework
  groupIds, or only provide examples ("JUnit, Mockito, AssertJ, Testcontainers, Awaitility")?
  An incomplete list in the spec leaves the acceptance boundary undefined. [Completeness,
  FR-012, Gap]

- [x] CHK018 — Is "test scope" explicitly defined for both Maven (which Maven scopes qualify:
  `test`, `provided`, `system`?) and Gradle (which configurations: `testImplementation`,
  `testCompileOnly`, `testRuntimeOnly`?)? [Completeness, FR-012, Ambiguity]

- [x] CHK019 — Does FR-013 define the exact format and content of "test source directory
  structure and file naming conventions"? "Naming conventions" is not quantified — are specific
  patterns (e.g., `**/*Test.java`, `**/*Spec.java`) in scope, and how are they detected?
  [Clarity, FR-013, Ambiguity]

- [x] CHK020 — Is the numeric unit for the coverage threshold value explicitly specified
  (0.0–1.0 ratio or 0–100 percentage)? The acceptance scenario implies ratio (`0.8` for 80%)
  but the spec does not state this explicitly. [Clarity, FR-014, Ambiguity]

- [x] CHK021 — Is behavior defined when JaCoCo is applied for report generation only (no
  threshold/check rule configured)? Should `coverageThreshold` be `null` or absent in that
  case? [Completeness, FR-014, Gap]

- [x] CHK022 — Is the value recorded as `TestFramework.version` explicitly defined (dependency's
  resolved version, declared version, or something else)? [Completeness, FR-012, Gap]

---

## Structure Collector — Requirement Completeness

- [x] CHK023 — Is "module name" explicitly defined — does it refer to the build system module
  identifier (e.g., Gradle `:core`, Maven `core`) or the IntelliJ module display name, which
  may differ? [Clarity, FR-015, Ambiguity]

- [x] CHK024 — Is the string format for package segments explicitly defined? The examples use
  dotted Java package notation (`com.example.web`) but path notation (`com/example/web`) is
  not ruled out. [Clarity, FR-016, Ambiguity]

- [x] CHK025 — The edge cases section lists "What if two modules declare an identical
  inter-module dependency?" as an open question without a resolved answer. Is the expected
  behavior (record once vs. twice) defined? [Completeness, Edge Cases, Gap]

- [x] CHK026 — Does the spec name the new raw-material field replacing `packageOrganization`?
  FR-017 mandates the replacement but only describes the content; the field name is specified
  only in plan artifacts outside the spec. [Completeness, FR-017, Gap]

- [x] CHK027 — Is behavior defined for a fully source-less project (every module has no source
  roots)? Are `rootPackages` and `packageSegments` both empty, or is the structure section
  marked Empty? [Completeness, FR-015, FR-016, Gap]

---

## Error & Empty State Boundaries

- [x] CHK028 — Is the boundary between `Ok(populated data)` and `Empty` defined per section?
  For example, if the stack collector detects a build system but finds zero dependencies, is
  that `Ok(StackInfo(buildSystem=MAVEN, dependencies=[]))` or `Empty`? [Clarity, FR-018,
  Ambiguity]

- [x] CHK029 — The Key Entities description of `ScanResult` mentions only two states
  ("populated or explicitly marked empty"), but FR-018 and FR-021 define three
  (populated / empty / error). Is this an inconsistency in the spec? [Consistency, FR-018,
  Conflict]

- [x] CHK030 — Does the spec provide concrete per-section examples distinguishing "no data
  found" (Empty) from "data source unresolvable" (Error) for each of the five sections?
  [Clarity, FR-018, Ambiguity]

- [x] CHK031 — Is behavior defined when a partial error occurs within a single collector —
  e.g., the stack collector reads the build system successfully but then fails reading
  dependencies? Is the section `Error` or `Ok(partial data)`? [Completeness, FR-021, Gap]

---

## Cross-Collector Consistency

- [x] CHK032 — SC-006 states "no breaking changes to meaning of existing model fields," yet
  the spec mandates removing `packageOrganisation` and widening `breaksBuild` to nullable —
  are these intentional deviations from SC-006 explicitly acknowledged with a justification?
  [Consistency, SC-006, Conflict]

- [x] CHK033 — Are acceptance scenarios provided for Gradle projects (not just Maven) for the
  linter collector, given that the Gradle adapter's applied-state detection and hardness
  reporting differ significantly from Maven's? [Coverage, User Story 3, Gap]

---

## Measurability & Testability

- [x] CHK034 — Is SC-002 ("100% of declared dependency coordinates appear") testable via unit
  tests using port fakes, given that the "declared vs. transitive" distinction is enforced by
  the IntelliJ model? Does the spec provide a test-observable definition of "declared"?
  [Measurability, SC-002, Ambiguity]

- [x] CHK035 — Are the linter collector requirements (FR-008–FR-011) specific enough to write
  independent unit tests for Checkstyle and PMD separately, or are tool-specific parsing
  behaviors underspecified in the spec? [Completeness, FR-008, Gap]

---

## Notes

- Items marked `[Gap]` identify requirements missing from the spec — not gaps in implementation.
- Items marked `[Conflict]` identify internal inconsistencies within the spec.
- Items marked `[Ambiguity]` identify requirements that are present but not specific enough to
  yield a single unambiguous implementation.
- Address all `[Conflict]` items before starting implementation; `[Gap]` and `[Ambiguity]`
  items may be resolved via `/speckit-clarify` or inline in the plan.
