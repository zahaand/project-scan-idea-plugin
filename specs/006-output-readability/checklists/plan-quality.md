# Plan Quality Checklist: Output Readability for Large Projects

**Purpose**: Pre-implementation gate — validate completeness, clarity, and consistency of all requirements before /speckit-tasks. All items must be resolved or explicitly accepted as-is.
**Created**: 2026-06-27
**Feature**: [spec.md](../spec.md) | [plan.md](../plan.md)
**Depth**: Comprehensive (pre-implementation gate)
**Focus**: Full FR coverage + inter-consumer consistency (SC-006) + gap analysis

---

## Requirement Completeness

- [x] CHK001 — Does the spec define rendering behavior for a `groupId` with exactly one artifact — should it use the group-header format (`groupId:* @ version`) or the per-artifact format? [Completeness, Gap, Spec §FR-001, FR-002]
  > **Resolved 2026-06-27**: FR-002 amended — single-artifact groups use per-artifact format (not group header). US1 acceptance scenario 4 added.
  > **Refined 2026-06-27**: US1 Independent Test updated to explicitly state that multi-artifact uniform groups render a group header with `artifactId`-only lines, while single-artifact and mixed/null groups render per-artifact format without a header.

- [x] CHK002 — Is the Tech Stack rendering behavior specified when ALL dependencies have `resolvedVersion == null` (no version on any artifact in any group)? [Completeness, Edge Case, Gap]
  > **Resolved 2026-06-27**: Edge case added — all-null groups render per-artifact format without versions; no group headers appear.

- [x] CHK003 — Does FR-005 explicitly state that `moduleName → [dep1, dep2]` is the canonical required format, or is this format only implied by the Clarifications section and not formally required? [Completeness, Spec §FR-005, §Clarifications]
  > **Resolved 2026-06-27**: FR-005 now states "The canonical format is one line per module: `moduleName → [dep1, dep2]`." US2 acceptance scenario 4 updated to match.

- [x] CHK004 — **MANDATORY RESOLUTION**: Are requirements for `StructureInfo.packageSegments` rendering present in any FR? User Story 2 says "shows the package organisation pattern" and Assumptions confirms `packageSegments` covers that signal — yet FR-004 through FR-006 do not mention it. Is this a deliberate exclusion or a missed requirement? [Completeness, Gap, Spec §US2, §Assumptions, §FR-004–006]
  > **Resolved 2026-06-27**: FR-014 added — both consumers MUST render `packageSegments` when non-empty using format `Package segments: seg1, seg2, ...`. US2 acceptance scenario 5 added. SC-002 updated to include package segments.

- [ ] CHK005 — Is there a defined requirement for what renders in Project Structure when `StructureInfo.modules` is empty? [Completeness, Edge Case, Gap, Spec §FR-004]
  > **Implementation assumption**: Falls under FR-011 (empty/error handling unchanged). When `StructureInfo` is `SectionResult.Empty` or `SectionResult.Ok` with an empty modules list, existing "not detected" / empty-section handling applies. No FR change required.

- [ ] CHK006 — Is FR-012 specific enough about what "covered by unit tests" means — happy path per FR only, or all acceptance scenarios from User Stories 1–3? [Completeness, Clarity, Spec §FR-012]
  > **Implementation assumption**: FR-012 remains broad. Test coverage must include all acceptance scenarios from US1–US3 (they are the normative FRs). No FR change; the new test class `PromptGeneratorOutputReadabilityTest` will enumerate them.

---

## Requirement Clarity

- [x] CHK007 — Is "omit per-artifact version repetition" in FR-002 unambiguous about what DOES appear per artifact within a uniform-version group — just the `artifactId`, or nothing below the group header line? [Clarity, Spec §FR-002]
  > **Resolved 2026-06-27**: FR-002 now states "each artifact below the header as `artifactId` only (without repeating the version)." US1 acceptance scenario 1 updated accordingly.

- [x] CHK008 — Is the ordering of entries in the Version discrepancies block specified — alphabetical by `groupId:artifactId`, declaration order across modules, or unspecified? The same question applies to the ordering of modules within `{moduleName: version, ...}`. [Clarity, Gap, Spec §FR-006]
  > **Resolved 2026-06-27**: FR-006 now mandates lexicographic sort of entries by groupId then artifactId; module names within each entry sorted lexicographically. NFR-001 (Determinism) added.

- [x] CHK009 — Does FR-009 define "longest common absolute prefix" with enough precision to produce a deterministic result when some entries in `TestInfo.sourceRoots` are already relative (no leading `/`)? [Clarity, Edge Case, Spec §FR-009, §Assumptions]
  > **Resolved 2026-06-27**: FR-009 now states "longest common absolute prefix across all absolute-path entries (relative-path entries are used as-is)." Edge case and algorithm in research.md updated.

- [x] CHK010 — Is "module count" in FR-009 unambiguously defined — count of distinct module names contributing that relative path, or count of raw entries in `TestInfo.sourceRoots` that resolve to that template? [Clarity, Spec §FR-009, §Assumptions]
  > **Resolved 2026-06-27**: FR-009 now states "a count equal to the number of raw `TestInfo.sourceRoots` entries that normalise to that template."

- [ ] CHK011 — Does FR-011 define which empty-state strings apply per section, or does it only prohibit behavioral change? Is "not detected" the canonical string or is it left to each consumer? [Clarity, Spec §FR-011]
  > **Implementation assumption**: FR-011 means "do not change the existing code paths for empty/error states." The actual strings (`ProjectScanBundle.message("section.state.empty")` in `:ui`; `"not detected"` in `:prompt`) differ by design and are both preserved. No FR change required.

---

## Requirement Consistency (Inter-Consumer, SC-006)

- [x] CHK012 — Are the format strings in User Story 1 acceptance scenarios (e.g., `org.springframework:* @ 6.1.4`) byte-for-byte consistent with the canonical format stated in the Clarifications section and in `contracts/output-formatters-api.md`? [Consistency, SC-006, Spec §US1-Scenario-1, §Clarifications, plan/contracts]
  > **Resolved 2026-06-27**: All three sources (US1, Clarifications, contracts) now state the same format. Contracts updated to note "artifactId only" under header.

- [ ] CHK013 — Does FR-004 ("MUST NOT include per-module external dependency lists") cover exactly the same scope as User Story 2 acceptance scenario 1 — do "dependency list" and "external dependency lines" mean the same thing? [Consistency, Spec §FR-004, §US2-Scenario-1]
  > **Implementation assumption**: "per-module external dependency lists" and "per-module external dependency lines" are equivalent. The implementation must not render any line of the form `Dependency: groupId:artifactId` under a Module block.

- [x] CHK014 — Are FR-008 and FR-009 each written precisely enough that they individually produce the outcomes of all four User Story 3 acceptance scenarios (1–4)? [Consistency, Spec §FR-008, FR-009, §US3]
  > **Resolved 2026-06-27**: FR-008 (deduplication) and FR-009 (normalisation with raw-entry count) now precisely cover US3 scenarios 1–4. Reviewed and confirmed consistent.

- [x] CHK015 — Is the test coverage scope difference between FR-012 (PromptGenerator only) and SC-005 (adds smoke tests for ScanResultRenderer) explicit and intentional — are there no FRs that inadvertently apply only to one consumer? [Consistency, Spec §FR-012, §SC-005]
  > **Resolved 2026-06-27**: FR-012 covers `:prompt` unit tests; SC-005 adds `:ui` smoke tests. The asymmetry is intentional and documented in spec Assumptions (":ui requires one smoke test per changed section"). No hidden consumer-specific FRs found.

- [x] CHK016 — Do FR-001 through FR-009 all explicitly say "both consumers" (or equivalent), leaving no FR that binds only `:prompt` or only `:ui` by wording alone? [Consistency, SC-006, Spec §FR-001–009]
  > **Resolved 2026-06-27**: All FRs FR-001 through FR-009 and FR-013–FR-014 explicitly say "both consumers" or "both `:prompt` and `:ui`." Confirmed.

---

## Acceptance Criteria Measurability

- [x] CHK017 — Is SC-001 ("at most 60 lines" for 250 deps across 40 groupId groups) mathematically consistent with FR-002 and FR-003? In a worst-case distribution where all 40 groups have mixed versions, 250 artifact lines remain — does the "60 line" cap hold? [Measurability, Conflict, Spec §SC-001, FR-002–003]
  > **Resolved 2026-06-27**: SC-001 reformulated — removes the hard 60-line cap; expresses the goal as "approximately 40–60 lines when most groups are uniform" and explicitly states worst-case behaviour (all mixed → no line reduction, but still grouped by groupId).

- [ ] CHK018 — Can SC-002 ("Project Structure no longer lists per-module external dependency lines") be objectively verified with a fabricated `ScanResult` in a unit test, or does it require a real 80-module project to be meaningful? [Measurability, Spec §SC-002]
  > **Acceptable as-is**: SC-002 is verifiable with a fabricated `ScanResult` containing multiple modules with `declaredDependencies`. The smoke test for `ScanResultRenderer` and the `PromptGeneratorOutputReadabilityTest` will cover this assertion.

- [ ] CHK019 — Can SC-003 and SC-004 be verified using a fabricated `TestInfo` (80 identical `TestFramework` entries, 80 identical source-root paths) in a unit test without platform startup? [Measurability, Spec §SC-003, SC-004]
  > **Acceptable as-is**: Yes — `TestInfo` is a plain data class; fabricating 80 entries is straightforward. Unit tests in `:prompt` and smoke tests in root project will cover SC-003 and SC-004.

- [x] CHK020 — Is SC-006 ("consistent section content for the same ScanResult") measurable — does the spec require byte-identical output strings from both consumers, or only semantic equivalence? [Measurability, Ambiguity, Spec §SC-006]
  > **Resolved 2026-06-27**: SC-006 amended to "byte-identical section content." NFR-001 (Determinism) added to formally require this.
  > **Refined 2026-06-27**: SC-006 and NFR-001 scoped to section body content on non-empty `ScanResult` data (strings returned by render functions). Empty/error wrapper strings differ between consumers by design (FR-011) and are explicitly excluded. Tests for SC-006 must use non-empty `ScanResult` fixtures.

- [ ] CHK021 — Is "no regression in existing PromptGenerator tests" (SC-005) objectively verifiable before implementation starts — are all existing tests currently green and their coverage scope documented? [Measurability, Spec §SC-005]
  > **Implementation assumption**: Existing tests (`PromptGeneratorFullModelTest`, `PromptGeneratorEmptyModelTest`, etc.) are the regression baseline. They must all pass after FR changes. Implementation must run the test suite before marking tasks complete.

---

## Scenario Coverage

- [x] CHK022 — Is the scenario where a `groupId` group contains a mix of null and non-null `resolvedVersion` values covered by an FR? FR-002 handles uniform non-null; FR-003 handles differing non-null; neither explicitly addresses a null-mixed group. [Coverage, Edge Case, Gap, Spec §FR-002, FR-003, §Edge Cases]
  > **Resolved 2026-06-27**: FR-002 amended — "any artifact with `resolvedVersion == null` is not eligible for uniform format." FR-003 now explicitly lists this as a trigger. Edge case added to spec.

- [x] CHK023 — Does FR-006 explicitly reflect User Story 2 acceptance scenario 3 — when ALL modules agree on every version, is the Version discrepancies block omitted or shown with a "none" notice? [Coverage, Clarity, Spec §US2-Scenario-3, §FR-006]
  > **Resolved 2026-06-27**: FR-006 now requires "When no discrepancies exist, the sub-block MUST render an explicit `none` notice rather than being omitted." US2 acceptance scenario 3 updated to remove "either omitted or."

- [x] CHK024 — Is there a defined requirement for what happens when the same module declares the same `(groupId, artifactId)` artifact twice with different versions (intra-module duplicate)? [Coverage, Edge Case, Gap]
  > **Resolved 2026-06-27**: Edge case added — last declared version for that coordinate within the module wins; earlier occurrences are discarded.

- [x] CHK025 — Is the mixed absolute/relative source roots edge case covered — what renders when `TestInfo.sourceRoots` contains both `/abs/path/src/test/java` and a bare `src/test/kotlin`? [Coverage, Edge Case, Gap, Spec §Edge Cases]
  > **Resolved 2026-06-27**: FR-009 updated and edge case added — LCP computed only for absolute entries; relative entries used as-is; no normalisation error.

- [x] CHK026 — Are Tech Stack dependency group ordering requirements specified — rendered in input order, alphabetical by `groupId`, or intentionally unspecified? [Coverage, Non-Functional, Gap]
  > **Resolved 2026-06-27**: FR-001 amended — groups output in lexicographic order by `groupId`.

---

## Non-Functional Requirements

- [x] CHK027 — Are output determinism requirements explicitly stated for the new formatting logic — must the same `ScanResult` always produce identical rendered strings in both consumers across multiple calls? [Non-Functional, Gap, Spec §SC-006 implied]
  > **Resolved 2026-06-27**: NFR-001 (Determinism) added — byte-identical output on repeated calls; lexicographic sort orders in FR-001 and FR-006 are the mechanism.

- [x] CHK028 — Are performance requirements defined for the shared utility functions operating on large inputs (80 modules, 250 dependencies)? [Non-Functional, Gap, plan.md §Technical Context]
  > **Resolved 2026-06-27**: NFR-002 added as informal engineering target ("sub-millisecond expected; not a normative gating requirement").

---

## Dependencies & Assumptions

- [ ] CHK029 — Is the assumption "source roots are stored as absolute paths on the scanning machine" validated against the actual scan collector — could the scanning layer produce relative or platform-normalised paths in practice? [Assumption, Spec §Assumptions]
  > **Implementation assumption**: The spec assumption is taken at face value. FR-009 and the edge case for mixed absolute/relative handle both cases gracefully; no validation against scan collector is required before implementation.

- [x] CHK030 — Is the assumption "module count is inferred by counting raw `TestInfo.sourceRoots` entries that normalize to the same template" unambiguous when one logical module contributes multiple absolute paths for the same relative suffix? [Assumption, Spec §Assumptions]
  > **Resolved 2026-06-27**: FR-009 defines count as "number of raw `TestInfo.sourceRoots` entries that normalise to that template" — unambiguous regardless of how many paths one module contributes.

- [x] CHK031 — Does the root project's `build.gradle.kts` currently declare `:model` as a compile dependency — is the new `OutputFormatters.kt` accessible to `ScanResultRenderer` without a build configuration change? [Dependency, Gap]
  > **Resolved 2026-06-27**: `OutputFormatters.kt` moves to new `:shared` module (FR-013). Root project already depends on `:model`; it must also add `:shared` as a compile dependency. This is a required build task in the implementation.

---

## Ambiguities & Conflicts

- [x] CHK032 — Is the conflict between FR-010 ("`:model` MUST NOT be modified") and the Clarifications section ("extract to `:model` preferred") resolved by a formal FR-010 update or amendment note in the spec, rather than left as an interpretation in `plan.md` only? [Conflict, Spec §FR-010, §Clarifications]
  > **Resolved 2026-06-27**: FR-010 formally amended with note "`:shared` is additive, does not modify `:model`." FR-013 added. Clarifications section updated to supersede the old ":model preferred" answer. plan.md, research.md, data-model.md, and contracts/ all updated to reference `:shared`.

- [x] CHK033 — **MANDATORY RESOLUTION**: Is the narrative in User Story 2 ("shows the package organisation pattern") intentionally not backed by an FR, or is it a missed requirement? [Conflict/Gap, Spec §US2, §Assumptions, §FR-004–006]
  > **Resolved 2026-06-27**: FR-014 added — both consumers render `packageSegments` when non-empty. See CHK004.

- [ ] CHK034 — Does FR-011 ("empty/error handling MUST remain unchanged") conflict with ScanResultRenderer's current bundle-key-based empty-state strings if those keys produce different text than "not detected"? [Ambiguity, Spec §FR-011]
  > **Implementation assumption**: No conflict — the private render functions (`renderStack`, `renderTests`, `renderStructure`) return `String?`; the `section()` wrapper applies bundle keys for empty/error. FR-011 targets the wrapper, not the render functions. Implementation must not change the wrapper's empty/error code paths.
  > **Refined 2026-06-27**: SC-006 and NFR-001 now explicitly exclude empty/error wrapper strings from their byte-identical requirement, confirming this assumption is fully consistent with those criteria.

---

## Implementation Assumptions (unresolved items accepted as-is)

The following items were reviewed and accepted without spec changes. Each is an implementation-time decision, not a requirements gap:

| CHK | Decision |
|-----|----------|
| CHK005 | Empty `StructureInfo.modules` → existing FR-011 / empty-section handling covers it |
| CHK006 | FR-012 "covered by unit tests" means all acceptance scenarios from US1–US3 |
| CHK011 | FR-011 preserves existing bundle-key and "not detected" strings per consumer |
| CHK013 | "per-module dependency list" = "per-module external dependency lines" — same concept |
| CHK018 | SC-002 is verifiable with a fabricated `ScanResult` in unit/smoke tests |
| CHK019 | SC-003/SC-004 are verifiable without platform startup using plain `TestInfo` fixtures |
| CHK021 | Regression baseline = all existing `:prompt` tests passing before FR changes |
| CHK029 | Mixed absolute/relative source roots handled by FR-009; no scan-collector audit needed |
| CHK034 | FR-011 targets the `section()` wrapper; render functions are unaffected |
