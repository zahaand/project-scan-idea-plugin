# Plan Coverage Checklist: Sprint 7 — Usability Rework: Tech Stack & Testing Collection Layer

**Purpose**: Post-plan validation — confirm the plan fully covers the spec across all layers, with mandatory gate on Maven direct-vs-transitive distinction
**Created**: 2026-06-28
**Feature**: [spec.md](../spec.md) · [plan.md](../plan.md) · [data-model.md](../data-model.md)

---

## ⛔ Mandatory Gate — Maven Direct-vs-Transitive Distinction (FR-003)

*These items must pass before any collection-layer implementation begins.*

- [x] CHK001 — Does FR-003 define what "directly-declared" means for a dependency inherited from a parent POM's own `<dependencies>` block — is it counted as direct in the child, or only in the parent? [Ambiguity, Spec §FR-003] → **Resolved**: FR-003 now defines parent-inherited deps as direct for each child (effective declared set; origin not distinguished).
- [x] CHK002 — Does FR-003 address how BOM imports (`<type>pom</type>`, `<scope>import</scope>`) are handled — is the BOM artifact itself included or excluded from the direct slice? [Edge Case, Spec §FR-003] → **Resolved**: FR-003 now explicitly excludes BOM-import artifacts.
- [x] CHK003 — Does FR-003 or SC-007 define version resolution precedence when the same coordinate is declared in both the child POM and its parent — which version wins in the resolved model? [Ambiguity, Spec §FR-003, §SC-007] → **Resolved**: FR-003 now states resolved effective version always used; nearest wins (child overrides parent); at most one resolved version per coordinate per module.
- [x] CHK004 — Does FR-003 specify the treatment of `<optional>true</optional>` dependencies — are they included or excluded from the direct slice? [Gap, Spec §FR-003] → **Resolved**: FR-003 now explicitly includes optional dependencies (optional affects transitive propagation, not membership).
- [x] CHK005 — Does FR-007 define what "external" means (by groupId, by coordinate exclusion from `internalModuleNames`, or another criterion), or is the filtering rule specified only in the data model algorithm? [Ambiguity, Spec §FR-007] → **Resolved**: FR-007 now defines "external" as coordinate not matching `internalModuleNames` (by artifactId or groupId:artifactId).

---

## Plan Coverage of Requirements

- [x] CHK006 — Does the plan trace FR-003's direct-only intersection logic to a specific code location beyond "IjModuleStructureAdapter.mavenModules()"? [Coverage, Plan §IjModuleStructureAdapter] → **Resolved**: Added to Task Generation Requirements in plan.md; task must confirm API path against 2025.3.5 classpath.
- [x] CHK007 — Does the plan address the canonical-path approach used for aggregator map keys (FR-004) — is there a requirement or note about filesystem case sensitivity or symlinks? [Coverage, Spec §FR-004] → **Resolved**: Added to Edge Cases as deferred — canonicalPath used; robustness to case-sensitivity/symlinks verified during real-project testing, not blocking.
- [x] CHK008 — Are the four files marked for deletion (DependencyPort.kt, IjDependencyAdapter.kt, FakeDependencyPort.kt, IjDependencyAdapterTest.kt) each covered by a task or plan entry? [Completeness, Plan §Project Structure] → **Resolved**: Added explicit deletion tasks to Task Generation Requirements in plan.md.
- [x] CHK009 — Does the plan cover the ScanServiceFactory / ProjectScanPanel wiring change required when `DependencyPort` is removed from `ScanService`'s constructor? [Gap, Plan §ScanService] → **Resolved**: Added explicit wiring-change task to Task Generation Requirements in plan.md.
- [x] CHK010 — Does the plan specify how `renderStack()` in `ScanResultRenderer` receives `List<Module>` or a pre-built `InvertedTechStack` — is the signature change documented with its call-site impact? [Completeness, Plan §UI] → **Resolved**: Added explicit signature-change task to Task Generation Requirements in plan.md.
- [x] CHK011 — Does the plan address updating any serialization, persistence, or display code that currently reads `StructureInfo.rootPackages` or `StructureInfo.packageSegments` beyond the listed files? [Coverage, Gap] → **Resolved**: Added consumer-audit task to Task Generation Requirements in plan.md.

---

## Requirement Clarity — Aggregator Extraction and Grouping (FR-004, FR-009)

- [x] CHK012 — Does the spec define what happens when a module appears in multiple aggregators' `<modules>` lists — is the first match used, the nearest, or is this treated as an error? [Edge Case, Spec §FR-004] → **Resolved**: Added to Edge Cases — last aggregator in iteration order wins; not an error; must not crash or produce non-deterministic output.
- [x] CHK013 — Is the null-aggregator (root/top-level = null) behavior specified consistently across FR-001, FR-009, and the edge cases section — do they all agree on what "ungrouped" rendering looks like? [Consistency, Spec §FR-001, §FR-009, Edge Cases] → **Resolved**: FR-009 now includes explicit consistency note: null = root/top-level, renders ungrouped, appears last.
- [x] CHK014 — Does the spec specify that `aggregator` stores the `artifactId` specifically, given the plan uses `mp.mavenId.artifactId ?: mp.displayName` as fallback — is the fallback rule part of the spec or only the plan? [Clarity, Spec §FR-001] → **Resolved**: FR-001 now specifies artifactId with displayName as fallback when artifactId is unavailable.
- [x] CHK015 — Does FR-009 specify the ordering of aggregator groups within a multi-version entry (alphabetical by aggregator name, reactor topology order, or another rule)? [Ambiguity, Spec §FR-009] → **Resolved**: FR-009 now specifies named aggregators alphabetically, null-aggregator group last.
- [x] CHK016 — Does FR-009 specify the ordering of module names within an aggregator group — the data model states "sorted alphabetically" but is this requirement in the spec? [Clarity, Spec §FR-009] → **Resolved**: FR-009 now specifies module names within a group sorted alphabetically.

---

## Requirement Clarity — InvertedTechStack Rendering (FR-008)

- [x] CHK017 — Is the exact rendering format for a uniform single-version entry (e.g., `spring-core:6.1.0 [40 modules]`) formally specified in the spec, or does the spec only describe the semantic intent while the format is defined only in data-model.md? [Clarity, Spec §FR-008, §SC-001] → **Resolved**: FR-008 now specifies the exact format as `coordinate:version [N modules]` as a firm contract.
- [x] CHK018 — Is "count" vs. "all modules" in FR-008 a firm format requirement or an implementation choice — and does the spec make this deterministic enough to enforce SC-005 byte-identical parity? [Ambiguity, Spec §FR-008, §SC-005] → **Resolved**: FR-008 now fixes `[N modules]` as the required format; byte-identical parity is structurally guaranteed via single shared formatter.
- [x] CHK019 — Does the rendering spec (FR-008/FR-009) define character-level formatting (whitespace, separators, indentation) sufficient to enforce byte-identical output between prompt and UI consumers? [Measurability, Spec §SC-005] → **Resolved**: FR-008 now notes that SC-005 is guaranteed structurally — `renderInvertedTechStack` is the sole formatter; noted in data-model.md as well.

---

## Requirement Consistency

- [x] CHK020 — Does FR-007 ("all direct external dependencies regardless of Maven scope") unambiguously refer to the same slice as FR-003 ("directly-declared dependencies per module") — or could "all Maven scopes" in FR-007 expand the set beyond what FR-003's declared set produces? [Consistency, Spec §FR-003, §FR-007] → **Resolved**: FR-007 now explicitly states "same slice as FR-003" and clarifies "regardless of scope" means no scope-based filtering, not set expansion.
- [x] CHK021 — Are FR-013 (single shared source) and FR-014 (byte-identical) consistent with the data model's `renderInvertedTechStack` returning `"not detected"` when entries and preamble are all null — is this edge case's string value a spec-level contract? [Consistency, Spec §FR-013, §FR-014] → **Resolved**: Added to Edge Cases — `renderInvertedTechStack` returns `"not detected"` for empty/null input as a spec-level contract.
- [x] CHK022 — Does SC-004 ("no standalone Discrepancies block") align unambiguously with FR-009 (discrepancies inline in TechEntry) — are there any spec references to `detectVersionDiscrepancies()` or similar that could be misread as still required? [Consistency, Spec §SC-004, §FR-009] → **No action needed**: SC-004 and FR-009 are consistent; no stale references to old discrepancy functions exist in the spec.

---

## Edge Case Coverage in Requirements

- [x] CHK023 — Does the spec define behavior when a module's `declaredDependencies` contains a coordinate whose resolved version is blank or null after BOM/parent resolution — SC-007 covers the "omits version" happy path but not the null-version failure case? [Edge Case, Spec §SC-007] → **Resolved**: Added to Edge Cases — dependency remains in output with version omitted; consistent with never-fabricate.
- [x] CHK024 — Does the spec address the case where the same coordinate appears in a module's `declaredDependencies` with two different version strings (e.g., a dependency management override overriding a local declaration)? [Edge Case, Gap] → **Resolved**: Covered by FR-003 version-precedence rule (nearest wins; at most one resolved version per coordinate per module).
- [x] CHK025 — Is the behavior defined when `buildInvertedTechStack` receives an empty `modules` list — does `renderInvertedTechStack` produce a meaningful output, "not detected", or empty string? [Edge Case, Gap] → **Resolved**: Added to Edge Cases — returns `"not detected"` when modules list is empty and all preamble metadata is null.
- [x] CHK026 — Does the Gradle denylist (FR-006) specify an exact matching strategy (full coordinate, groupId prefix, or regex) in the spec, or is the matching rule defined only in the data model? [Clarity, Spec §FR-006] → **Resolved**: FR-006 now specifies matching strategy: exact groupId:artifactId for most entries, groupId match for org.ow2.asm.
- [x] CHK027 — Is there a requirement (or explicit non-requirement) for who owns and updates the Gradle denylist when new synthetic artifacts are discovered — or is this deliberately left out of scope? [Gap, Spec §FR-006] → **Resolved**: FR-N6 added — dynamic discovery / automated maintenance NOT required; static list maintained with plugin releases.

---

## Non-Functional Requirements

- [x] CHK028 — Is SC-001's "≤40 lines" criterion deterministically calculable from spec rules (e.g., lines-per-entry formula), or is it validated only empirically by running the plugin against the reference monorepo? [Measurability, Spec §SC-001] → **Resolved**: FR-N7 added — SC-001 is validated empirically, not derivable from a formula.
- [x] CHK029 — Are performance or latency requirements specified for the `buildInvertedTechStack` computation at 130-module × 250-dependency scale, or is the constraint entirely implicit via SC-001? [Gap] → **Resolved**: FR-N7 added — performance/latency NOT specified this sprint.

---

## Dependencies & Assumptions

- [x] CHK030 — Is the assumption that `MavenProject.mavenModel.dependencies` provides the declared-direct dependency set validated against IntelliJ API documentation, or is it listed as assumed with no fallback if the API returns the full resolved set? [Assumption, Spec §Assumptions] → **Resolved**: Assumptions section now documents the prioritized fallback chain; spec does not assume a single hardcoded method succeeds. Plan §Principle II and data-model.md updated accordingly.
- [x] CHK031 — Does the spec or plan identify whether any code outside the listed files consumes `StackInfo.dependencies`, `StructureInfo.rootPackages`, or `TestInfo.frameworks` — or is the consumer list assumed complete? [Assumption, Gap] → **Resolved**: Added consumer-audit task to Task Generation Requirements in plan.md — list assumed complete but audit required before marking removal tasks done.
