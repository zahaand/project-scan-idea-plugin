# Data Model Requirements Quality Checklist: Model — Structured Data Contract

**Purpose**: Author self-check — validate that the model type requirements and Gradle submodule setup are complete, clear, and ready for task generation. NOT a test plan — tests whether requirements are well-written.
**Created**: 2026-06-12
**Feature**: [spec.md](../spec.md) · [plan.md](../plan.md) · [data-model.md](../data-model.md)
**Audience**: Author, before `/speckit-tasks`
**Mandatory gates**: CHK017 (priority encoding), CHK018 (additive-stability contract)

> Legend: `[Gap]` = requirement missing · `[Ambiguity]` = requirement unclear · `[Conflict]` = requirements contradict · `[Assumption]` = unvalidated assumption

---

## Requirement Completeness

- [ ] CHK001 — Are all 15 model types enumerated with their complete field lists in the spec and/or data-model.md? [Completeness, Spec §FR-001–FR-007]
- [ ] CHK002 — Is the root aggregate construction contract (all five sections always required, none nullable) explicitly stated? [Completeness, Spec §FR-001]
- [ ] CHK003 — Are default values (empty-state constructors) documented for every section type (`StackInfo`, `CodeStyleInfo`, `LinterInfo`, `TestInfo`, `StructureInfo`)? [Completeness, Spec §FR-002]
- [ ] CHK004 — Are all enum constants documented for `BuildSystem`, `StyleSourceType`, `RuleSeverity`, and `PackageOrganisation`? [Completeness, data-model.md]
- [ ] CHK005 — Is `TestFramework.version` nullability documented with the same rationale as `Dependency.resolvedVersion`? [Completeness, Spec §Assumptions]
- [ ] CHK006 — Is the `Module` type fully specified with both `declaredDependencies` and `moduleDependencies` as separate fields, and are both fields' types and semantics stated? [Completeness, Spec §FR-007, Clarification Q1]

---

## Requirement Clarity

- [ ] CHK007 — Is "project-relative path" for `StyleSource.path` defined clearly enough that a producer knows what format to produce (e.g., `config/checkstyle/checkstyle.xml` not an absolute path)? [Clarity, Spec §Assumptions]
- [ ] CHK008 — Is the deduplication rule for `StackInfo.dependencies` ("flat union, deduplicated by groupId + artifactId") unambiguous — specifically, what happens when the same coordinate appears at different versions across modules? [Clarity, Spec §Clarification Q2, Assumptions]
- [ ] CHK009 — Is the `namingPattern` format in `TestInfo` specified clearly enough (glob? regex? both?) for a producer to know what to populate? [Ambiguity, Spec §Assumptions]
- [ ] CHK010 — Is `coverageThreshold` range (0.0–100.0) and unit (percentage) stated where a producer would find it? [Clarity, Spec §Assumptions]
- [ ] CHK011 — Is the distinction between `jdkVersion` and `languageLevel` on `StackInfo` explained — are these ever the same value, and if so is that valid? [Ambiguity, Spec §FR-003]
- [ ] CHK012 — Is `ActiveRule.tool` format standardised (e.g., canonical casing "Checkstyle" vs "checkstyle") or explicitly left to producers? [Ambiguity, Spec §FR-005]

---

## Requirement Consistency

- [ ] CHK013 — Does `Module.declaredDependencies` explicitly reuse the same `Dependency` type as `StackInfo.dependencies`, and is this reuse documented in both the spec and data-model.md? [Consistency, Spec §FR-003, FR-007]
- [ ] CHK014 — Are the nullability contracts for `Dependency.resolvedVersion` and `TestFramework.version` stated consistently (same rationale, same conditions)? [Consistency, Spec §Assumptions]
- [ ] CHK015 — Does the deduplication contract in `StackInfo` (project-wide union) align without contradiction to `Module.declaredDependencies` (per-module only)? [Consistency, Spec §Clarification Q2]
- [ ] CHK016 — Are empty-state representations consistent across all five sections (empty lists + null nullable fields, no sealed wrapper)? [Consistency, Spec §FR-002]

---

## Acceptance Criteria Quality — Mandatory Gates

> **Items below are MANDATORY GATES. Resolve before proceeding to `/speckit-tasks`.**

- [ ] CHK017 ⚠️ GATE — Is the `StyleSourceType.priority` semantics requirement (lower integer = higher precedence) stated precisely enough that a single wrong implementation would be caught by SC-003? Specifically: does the spec state that Checkstyle/Spotless/PMD share the same priority value (1), and is this tie between them intentional and documented? [Clarity, Spec §FR-004, SC-003]
- [ ] CHK018 ⚠️ GATE — Are SC-006 and SC-007 (additive-stability contract) worded precisely enough to distinguish an allowed change (new field, new enum constant) from a breaking change (rename, semantic redefinition)? Would a reviewer be able to determine pass/fail from the current wording without additional guidance? [Clarity, Spec §SC-006, SC-007]

---

## Scenario Coverage

- [ ] CHK019 — Do the user story acceptance scenarios cover construction of all five section types (at minimum one populated + one empty scenario per section)? [Coverage, Spec §User Stories 1–6]
- [ ] CHK020 — Is there a documented test scenario for the multi-module deduplication case (same Maven coordinate declared in two modules at different versions)? [Coverage, Gap]
- [ ] CHK021 — Is the style-source priority ordering test scenario complete — does it exercise all five `StyleSourceType` values, including the three linter types that share priority rank 1? [Coverage, Spec §SC-003, User Story 3]
- [ ] CHK022 — Are test scenarios defined for `Module` instances that have `moduleDependencies` but empty `declaredDependencies`, and vice versa? [Coverage, Spec §Edge Cases]

---

## Edge Case Coverage

- [ ] CHK023 — Is the BOM-managed null version edge case covered for `Dependency.resolvedVersion` (null) and carried through consistently to `TestFramework.version`? [Edge Case, Spec §Edge Cases, Assumptions]
- [ ] CHK024 — Is the "multiple `StyleSource` entries of the same type at different paths" edge case explicitly addressed (e.g., two `.editorconfig` files)? [Edge Case, Spec §Edge Cases]
- [ ] CHK025 — Is the case of a `Module` with no external dependencies but non-empty `moduleDependencies` covered in the edge cases? [Edge Case, Spec §Edge Cases]
- [ ] CHK026 — Is the "non-Maven/Gradle build system" case (null `buildSystem`) covered — and is it clear whether `dependencies` may still be populated in that case? [Edge Case, Spec §Edge Cases]

---

## Gradle Submodule Requirements

- [ ] CHK027 — Is the constraint that `:model` must NOT apply `org.jetbrains.intellij.platform` stated as a verifiable, testable requirement (not just an assumption)? [Completeness, Spec §SC-004, plan.md §Technical Context]
- [ ] CHK028 — Is the `:model` → root plugin module project dependency (via `implementation(project(":model"))`) documented in the plan with enough specificity for task generation? [Completeness, plan.md §Project Structure]
- [ ] CHK029 — Is the `settings.gradle.kts` change (`include(":model")`) explicitly listed as a required deliverable? [Completeness, quickstart.md]
- [ ] CHK030 — Are the detekt/ktlint deferred items tracked with enough precision to become tasks (i.e., is there a concrete acceptance criterion for "applied to `:model`")? [Gap, plan.md §Constitution Check]

---

## Non-Functional Requirements

- [ ] CHK031 — Is the "< 5 seconds" test suite timing requirement (SC-005) tied to a qualifying environment (e.g., developer workstation, no remote I/O), or is it ambiguous across CI vs. local? [Clarity, Spec §SC-005]
- [ ] CHK032 — Is "no IntelliJ Platform class on compile or runtime classpath" (SC-004) measurable by a specific mechanism (e.g., Gradle dependency insight, classpath dump)? [Measurability, Spec §SC-004]
- [ ] CHK033 — Is the immutability-by-convention assumption documented with a clear scope boundary (Sprint 1 only; no defensive copying required now)? [Clarity, Spec §Assumptions]

---

## Dependencies & Assumptions

- [ ] CHK034 — Is the assumption that "deduplication keeps the highest resolved version" for version-conflicting coordinates explicitly documented and is it clear this is a scan-layer responsibility, not model-layer? [Assumption, Spec §Assumptions, Clarification Q2]
- [ ] CHK035 — Is the inline style configuration out-of-scope decision (Q3 clarification) documented explicitly enough that a Sprint 2 implementer would not attempt to model it? [Assumption, Spec §Assumptions, Clarification Q3]
- [ ] CHK036 — Is the assumption that `StyleSource.path` is always project-relative validated — or could a future scan producer encounter absolute paths, breaking consumers? [Assumption, Spec §Assumptions]

---

## Ambiguities & Conflicts

- [x] CHK037 — Is the tie between Checkstyle, Spotless, and PMD at priority rank 1 intentional, and is the consequence documented (consumers cannot determine precedence between these three via priority alone)? [Ambiguity, Spec §FR-004, data-model.md] → **Resolved**: equal rank is intentional; documented in data-model.md, spec Edge Cases, and consumer contract (item 4).
- [ ] CHK038 — Is `ActiveRule.ruleId` type (String) and format (tool-specific, no cross-tool normalisation) unambiguously documented so producers don't invent a canonical scheme? [Ambiguity, Spec §FR-005]
- [x] CHK039 — Does the spec or data-model state whether `StructureInfo.rootPackages` is derived from all modules aggregated or from the root module only? [Ambiguity, Gap, Spec §FR-007] → **Resolved**: project-wide union across all modules; per-module breakdown is post-MVP. Documented in FR-007, Assumptions, data-model.md, and producer contract (item 8a).

---

## Notes

- Mark items completed with `[x]` as you work through them.
- CHK017 and CHK018 are **mandatory gates** — do not proceed to `/speckit-tasks` until both are resolved.
- Items marked `[Gap]` indicate requirements that are missing and may need spec updates before task generation.
- Items marked `[Ambiguity]` indicate requirements that exist but need clarification to be implementable.
- CHK039 is a newly surfaced gap — `rootPackages` aggregation scope was not addressed in clarification; consider a quick spec update or accept the ambiguity and document it.
