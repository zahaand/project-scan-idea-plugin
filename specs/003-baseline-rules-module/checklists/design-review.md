# Design Review Checklist: Baseline — Static Curated Code-Quality Rules Module

**Purpose**: PR review gate — validates that spec + plan + data-model artifacts are complete,
clear, and consistent before implementation begins. Tests the quality of requirements writing,
not the implementation.
**Created**: 2026-06-14
**Feature**: [spec.md](../spec.md) | [plan.md](../plan.md) | [data-model.md](../data-model.md)
**Depth**: ~30 items | **Audience**: Peer reviewer (pre-implementation PR gate)
**Focus**: Invariant validation completeness · Module boundary requirements · Test coverage traceability

---

## Requirement Completeness

- [ ] CHK001 - Is every FR in §Requirements traceable to at least one acceptance scenario in §User Scenarios — or explicitly excluded by an Out of Scope item? [Completeness]
- [ ] CHK002 - Does the spec define "non-empty" for `statement` and `rationale` as non-blank (whitespace-only strings fail), not merely non-null? [Clarity, Spec §FR-008]
- [ ] CHK003 - Is the rationale for the allowed `minJavaLevel` set `{8, 11, 17, 21}` documented — or at minimum noted as "LTS Java versions" — so implementers understand why 9, 10, 14, etc. are rejected? [Clarity, Spec §Assumptions]
- [ ] CHK004 - Is the `id` field's non-emptiness constraint explicitly stated in FR-008? FR-008 lists uniqueness and non-empty `statement`/`rationale`, but non-empty `id` is only implied by "stable unique identifier." [Completeness, Gap, Spec §FR-008]
- [ ] CHK005 - Is there an acceptance scenario in US1 specifically for an empty `languages` list (e.g., `"languages": []`)? FR-008 mandates "non-empty languages list", and §Edge Cases names it, but US1's 7 scenarios do not include this case. [Coverage, Gap, Spec §US1/FR-008]

---

## Requirement Clarity & Measurability

- [ ] CHK006 - Is "unique `id` across all rules" defined as case-sensitive in the spec? §Edge Cases implies case-sensitive dedup but FR-008 does not state it explicitly. [Clarity, Spec §FR-008 vs §Edge Cases]
- [ ] CHK007 - Does FR-005 specify what "identifying the specific violation" means in the exception message — enough for an implementer to know which field, which rule, and which constraint to include? [Clarity, Spec §FR-005]
- [ ] CHK008 - Does US2 scenario 3 ("both calls return identical rule sets") define "identical" as structural equality or same object reference? The distinction matters for the test assertion. [Clarity, Spec §US2]
- [ ] CHK009 - Is FR-006 ("complete and unfiltered — every rule stored in rules.json is returned") measurable via a count assertion — does the spec cite this verification mechanism (e.g., SC-002)? [Measurability, Spec §FR-006/SC-002]
- [ ] CHK010 - Can SC-003 ("all 4 CORRECTNESS and 5 BEST_PRACTICE categories covered") be verified unambiguously via the `category` field alone, without relying on `id` string patterns? The spec's §Clarifications answers yes — is this constraint visible enough in FR-008 and SC-003 themselves? [Clarity, Spec §SC-003/Clarifications]

---

## Invariant Validation Requirements (FR-008)

- [ ] CHK011 - Are all FR-008 invariants (unique id, non-empty statement, non-empty rationale, minJavaLevel in {8,11,17,21}, non-empty languages, category/level consistency) each covered by at least one distinct acceptance scenario in US1? Map each invariant to scenario 2–7 and identify any invariant with no dedicated scenario. [Coverage, Spec §FR-008/US1]
- [ ] CHK012 - Does the spec define the validation order (structural parse → schemaVersion check → per-rule invariants) or is the order an implementation detail? If order matters for error message clarity, it should be a requirement. [Clarity, Gap]
- [ ] CHK013 - Is `schemaVersion` absence (missing key) distinguished from "unrecognized value" in the spec? Both trigger `BaselineLoadException` but for different reasons — does the spec require separate, distinguishable messages? [Clarity, Spec §Edge Cases]
- [ ] CHK014 - Is the category/level consistency map (NULL_SAFETY/RESOURCE_MANAGEMENT/CONCURRENCY/DANGEROUS_CONSTRUCTS → CORRECTNESS; EXCEPTION_HANDLING/STRING_PERFORMANCE/DECOMPOSITION/IMMUTABILITY/INTERFACE_PROGRAMMING → BEST_PRACTICE) fully enumerated in one place in the spec, or must implementers assemble it from §Key Entities and FR-008 together? [Completeness, Spec §Key Entities/FR-008]
- [ ] CHK015 - Is there a requirement that `BaselineLoadException.cause` is non-null when wrapping a library exception (e.g., `SerializationException`)? FR-005 says "wrapping the original cause where applicable" — is "where applicable" defined precisely enough? [Clarity, Spec §FR-005]

---

## Module Boundary Requirements

- [ ] CHK016 - Is SC-005 ("zero compile-time or runtime dependencies on :scan") verifiable by a concrete mechanism — is "inspecting build.gradle.kts" the full verification story, or should a Gradle dependency task output also be considered? [Measurability, Spec §SC-005]
- [ ] CHK017 - Does the spec define which types constitute the public API of `:baseline` — i.e., is it explicit that `BaselineRuleProvider`, `BaselineRule`, the four enums, and `BaselineLoadException` are public, while `RuleSet` and internal helpers are not? [Completeness, Gap]
- [ ] CHK018 - Is FR-015 ("types in :baseline, not :model") accompanied by a rationale in the spec? Without it, the contrast with `ScanResult` (which lives in `:model`) may confuse reviewers of future sprints. [Clarity, Spec §FR-015]
- [ ] CHK019 - Does the spec state which module is responsible for the category/level consistency mapping at runtime — is `:prompt` (Sprint 4) expected to re-validate, or does it rely entirely on `:baseline` having pre-validated it? [Clarity, Gap]

---

## JSON Resource Contract

- [ ] CHK020 - Is the exact classloader resource path (`dev/zahaand/projectscan/baseline/rules.json`) stated as a contractual constant in the spec (not only in §Assumptions)? [Completeness, Spec §Assumptions/FR-002]
- [ ] CHK021 - Does the spec define behavior when `rules.json` is syntactically valid JSON but missing the `rules` key entirely (e.g., `{"schemaVersion":1}`) — is this a structural parse error or a separate invariant violation? [Edge Case, Gap]
- [ ] CHK022 - Does the spec define whether extra unknown root-level JSON fields (keys other than `schemaVersion` and `rules`) are silently ignored or cause a load error? The §Edge Cases section covers unknown rule fields but not unknown wrapper fields. [Edge Case, Gap, Spec §Edge Cases]
- [ ] CHK023 - Is the `id` naming convention (lowercase-dotted, `<level-prefix>.<slug>`) stated as a curation requirement in the spec, or is it a guideline? If it is only a guideline and not enforced by invariants, the spec should say so explicitly to prevent incorrect validator implementations. [Clarity, Spec §Key Entities]

---

## Test Requirements Coverage

- [ ] CHK024 - Does each of the 7 acceptance scenarios in US1 correspond to a distinct, independently executable unit test without any IntelliJ IDE fixture — is the "independent test" note in US1 consistent with SC-006? [Coverage, Spec §US1/SC-006]
- [ ] CHK025 - Is there an acceptance scenario or SC entry that validates caching behavior specifically — i.e., that the second call returns the same list object (or structurally equal), not just "no exception"? [Completeness, Spec §US2/FR-001]
- [ ] CHK026 - Does US4 / SC-003 explicitly state that coverage is measured by the `category` field of returned rules (not by the `id` naming convention), making the test assertion unambiguous? [Clarity, Spec §SC-003/Clarifications]
- [ ] CHK027 - Are US3 acceptance scenarios 1–7 each formulated as assertions over the full real bundled `rules.json`, not over synthetic test data — is this intent clear in the spec? [Clarity, Spec §US3]
- [ ] CHK028 - Is SC-007 ("at least one rule has minJavaLevel > 8, asserted by a dedicated unit test") tied to a specific acceptance scenario, or is it only a success criterion without a backing scenario? [Traceability, Spec §SC-007]

---

## Plan / Data-Model vs. Spec Cross-Coverage

- [ ] CHK029 - Does `data-model.md` enumerate all FR-008 invariants in the field-constraints table, including `id` non-emptiness (even if it is a gap in the spec itself, per CHK004)? [Completeness, data-model §1.2]
- [ ] CHK030 - Does `plan.md` list `settings.gradle.kts` as a required change for both `include(":baseline")` and the `kotlin.plugin.serialization` plugin registration? [Completeness, plan §Project Structure]
- [ ] CHK031 - Is the `internal loadFromReader` design decision in `data-model.md` traceable to SC-006 (pure JVM tests, no IntelliJ fixtures)? If the spec does not mandate this testability mechanism, `data-model.md` should flag it as an assumption. [Traceability, data-model §4 vs Spec §SC-006]

---

## Notes

- Check items off as completed: `[x]`
- CHK004, CHK005, CHK007, CHK012, CHK013, CHK015, CHK017, CHK019, CHK021, CHK022, CHK023 are
  flagged as **[Gap]** or **[Clarity]** — these may require spec amendments before implementation.
- Items marked `[Gap]` indicate missing requirements; `[Ambiguity]` or `[Clarity]` indicate
  requirements present but insufficiently precise.
- CHK004 and CHK005 are the highest-risk gaps: a missing `id` non-emptiness invariant and a
  missing US1 acceptance scenario for empty `languages` both directly affect FR-008 correctness.
