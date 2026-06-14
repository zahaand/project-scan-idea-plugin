# Feature Specification: Baseline — Static Curated Code-Quality Rules Module

**Feature Branch**: `003-baseline-rules-module`  
**Created**: 2026-06-14  
**Status**: Draft  
**Input**: User description: "Sprint 3 — baseline: a static, curated set of baseline code-quality rules that always applies, implemented as a decoupled module depending ONLY on :model."

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Load and Validate the Bundled Rule Set (Priority: P1)

A plugin developer (or release pipeline) instantiates the baseline rule provider. The provider reads the bundled `rules.json` resource via the module's classloader, parses it, validates structural and invariant constraints, and returns the full set of baseline rules. If the resource is malformed or violates any invariant, the operation fails loudly with a diagnosable exception — never silently returning an empty set.

**Why this priority**: The entire module's value depends on successfully loading a correct, validated rule set. Nothing else can work if this fails. It is the root capability.

**Independent Test**: A unit test that calls the provider and asserts the returned list is non-empty, all rules parse correctly, and all invariants hold. Can be executed without any IDE or project context.

**Acceptance Scenarios**:

1. **Given** the module's bundled `rules.json` resource is well-formed and all invariants are satisfied, **When** the baseline provider is invoked, **Then** it returns a non-empty list of `BaselineRule` objects with no exception thrown.
2. **Given** a test-only replacement `rules.json` where one rule has an empty `statement`, **When** the baseline provider is invoked, **Then** it throws a diagnosable exception identifying the invariant violation — it does NOT return a partial or empty list silently.
3. **Given** a test-only replacement `rules.json` where two rules share the same `id`, **When** the baseline provider is invoked, **Then** it throws a diagnosable exception identifying the duplicate id.
4. **Given** a test-only replacement `rules.json` that is malformed JSON, **When** the baseline provider is invoked, **Then** it throws a diagnosable exception from the structural parsing layer.
5. **Given** a test-only replacement `rules.json` where a rule's `minJavaLevel` is set to an invalid value (e.g. `7` or `99`), **When** the baseline provider is invoked, **Then** it throws a diagnosable exception identifying the disallowed value.
6. **Given** a test-only replacement `rules.json` where the `rules` array is empty, **When** the baseline provider is invoked, **Then** it throws a diagnosable exception — an empty bundled set is invalid.

---

### User Story 2 — Consume the Full Unfiltered Rule Set (Priority: P1)

A downstream component (e.g. the future `:prompt` module) calls the baseline provider and receives the entire rule set, whole and unranked, ready for further processing. The provider applies no filtering by language level, project language, or any other project-specific fact. The consumer receives all rules and decides what to do with them.

**Why this priority**: The contract "return the full set, no filtering" is the core module boundary. Violating it would silently mis-deliver rules to `:prompt` (Sprint 4).

**Independent Test**: A unit test loads the real bundled rules and asserts the count equals the total expected number of rules, confirming no filtering occurred.

**Acceptance Scenarios**:

1. **Given** the bundled rule set contains 13 or more rules covering all required categories, **When** the baseline provider is invoked, **Then** it returns all rules — the count is ≥ 13 and matches the count in `rules.json` exactly.
2. **Given** the bundled rules include rules with `minJavaLevel` values of both 8 and 11 (or higher), **When** the baseline provider is invoked, **Then** rules with all `minJavaLevel` values are returned — none filtered out.
3. **Given** the baseline provider is invoked multiple times in the same process, **When** both calls return, **Then** both calls return identical rule sets (idempotent, no stateful filtering applied between calls).

---

### User Story 3 — Rule Metadata Completeness and Correctness (Priority: P2)

Each returned `BaselineRule` carries all 7 mandatory fields with correct values: a stable `id`, a `level` (`CORRECTNESS` or `BEST_PRACTICE`), an `obligation` (`MUST` or `SHOULD`), a non-empty `statement`, a non-empty `rationale`, a valid `minJavaLevel`, and a non-empty `languages` list. Downstream consumers rely on these fields to render rules in the generated Constitution.

**Why this priority**: Metadata correctness is what makes the rules useful in the Constitution. Incomplete or incorrect metadata breaks the Constitution rendering contract.

**Independent Test**: A unit test iterates over every rule returned by the real provider and asserts each field satisfies its constraint (non-null, non-empty, enum values recognized, allowed integer range).

**Acceptance Scenarios**:

1. **Given** the bundled rules are loaded, **When** each rule is inspected, **Then** every rule has a non-empty `id`, a non-empty `statement`, and a non-empty `rationale`.
2. **Given** the bundled rules are loaded, **When** each rule's `level` is checked, **Then** it is exactly one of `CORRECTNESS` or `BEST_PRACTICE` — no other value is present.
3. **Given** the bundled rules are loaded, **When** each rule's `obligation` is checked, **Then** it is exactly one of `MUST` or `SHOULD` — no other value is present.
4. **Given** the bundled rules are loaded, **When** each rule's `minJavaLevel` is checked, **Then** it belongs to the allowed set `{8, 11, 17, 21}`.
5. **Given** the bundled rules are loaded, **When** each rule's `languages` list is checked, **Then** it is non-empty and every entry is `JAVA`.
6. **Given** the bundled rules are loaded, **When** the complete set is inspected, **Then** at least one rule has `minJavaLevel > 8`.

---

### User Story 4 — Set Composition Coverage (Priority: P2)

The bundled rule set covers all required correctness and best-practice categories with at least the minimum number of rules per category. This ensures the module delivers adequate baseline coverage for the Constitution across all supported Java projects.

**Why this priority**: A rule set that omits a required category silently degrades Constitution quality. The coverage constraint is the measurable quality bar for the curated set.

**Independent Test**: A unit test groups the real bundled rules by category-keyword patterns in their IDs and asserts minimum counts per category: ≥2 rules for each of the 4 CORRECTNESS categories and ≥1 rule for each of the 5 BEST_PRACTICE categories (≥13 total).

**Acceptance Scenarios**:

1. **Given** the bundled rules are loaded, **When** CORRECTNESS rules are filtered by null-safety category, **Then** at least 2 rules cover null safety (null checks, empty collections, Optional usage).
2. **Given** the bundled rules are loaded, **When** CORRECTNESS rules are filtered by resource-management category, **Then** at least 2 rules cover resource management (try-with-resources, no reliance on finalize).
3. **Given** the bundled rules are loaded, **When** CORRECTNESS rules are filtered by concurrency category, **Then** at least 2 rules cover concurrency (safe publication, synchronization pitfalls, atomicity).
4. **Given** the bundled rules are loaded, **When** CORRECTNESS rules are filtered by dangerous-constructs category, **Then** at least 2 rules cover dangerous constructs (reference equality, equals/hashCode pairing, ignoring return values).
5. **Given** the bundled rules are loaded, **When** BEST_PRACTICE rules are inspected, **Then** at least 1 rule covers exception handling, 1 covers string performance, 1 covers decomposition, 1 covers immutability, and 1 covers programming to interfaces.
6. **Given** the bundled rules are counted, **When** all levels are combined, **Then** the total count is ≥ 13.

---

### Edge Cases

- What happens when the `rules.json` resource is entirely absent from the jar? → The loader MUST throw a diagnosable exception; it MUST NOT return an empty list.
- What happens when `schemaVersion` in `rules.json` is absent or has an unrecognized value? → The loader MUST either fail loudly or (if version is unrecognized) throw an exception identifying the unsupported version — silent fallback is prohibited.
- What happens when a rule's `languages` field is present but empty (`[]`)? → Invariant validation MUST reject this; `languages` must be non-empty per the data model.
- What happens when a rule contains extra/unknown JSON fields not in the model? → The parser silently ignores them (lenient deserialization); no exception is thrown for unrecognized fields.
- What happens when two rules have the same `id` but differ only in case? → Duplicate detection is case-sensitive; `"correctness.null-deref"` and `"CORRECTNESS.NULL-DEREF"` are considered distinct — but the rule ID convention (lowercase dotted) should prevent this in practice.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The module MUST expose a public API (loader/provider) that returns `List<BaselineRule>` loaded from the bundled JSON resource.
- **FR-002**: The module MUST load `rules.json` via the module's classloader (`getResourceAsStream`), NOT via filesystem access.
- **FR-003**: The module MUST parse `rules.json` using `kotlinx.serialization` (the only permitted external dependency besides `:model`).
- **FR-004**: The provider MUST perform structural validation (via the parser) and invariant validation (via explicit post-parse checks) before returning any results.
- **FR-005**: On any structural or invariant violation, the provider MUST throw a diagnosable exception with a message identifying the violation — it MUST NOT silently return an empty or partial list.
- **FR-006**: The returned list MUST be complete and unfiltered — every rule stored in `rules.json` is returned, regardless of `minJavaLevel`, `languages`, or any other field.
- **FR-007**: The returned list MUST be unranked — the module stores no priority ordering and applies no sorting by priority.
- **FR-008**: The module MUST validate the following invariants after parsing: unique `id` across all rules; non-empty `statement` and `rationale`; `minJavaLevel` within `{8, 11, 17, 21}`; non-empty `languages` list.
- **FR-009**: The `rules.json` resource MUST use wrapper format `{ "schemaVersion": 1, "rules": [ ... ] }` to support future schema evolution.
- **FR-010**: The bundled rule set MUST satisfy category coverage: ≥2 CORRECTNESS rules in each of the 4 required correctness categories; ≥1 BEST_PRACTICE rule in each of the 5 required best-practice categories; ≥13 rules total.
- **FR-011**: At least one bundled rule MUST have `minJavaLevel > 8` so the language-level field is demonstrable and testable.
- **FR-012**: The module MUST NOT contain any filtering, ranking, or priority logic — these belong exclusively in `:prompt` (Sprint 4).
- **FR-013**: The module MUST NOT depend on `:scan` and MUST NOT reference or import any type from `:scan`.
- **FR-014**: The module MUST NOT contain style, formatting, or naming rules — only correctness and architectural best-practice rules.
- **FR-015**: The `BaselineRule` data type, `BaselineLevel` enum, `Obligation` enum, and `BaselineLanguage` enum MUST be defined inside `:baseline`, not in `:model`.

### Key Entities *(include if feature involves data)*

- **BaselineRule**: A single code-quality rule. Has 7 fields: `id` (String, stable unique identifier), `level` (BaselineLevel), `obligation` (Obligation), `statement` (String, non-empty), `rationale` (String, non-empty), `minJavaLevel` (Int, from `{8, 11, 17, 21}`), `languages` (List\<BaselineLanguage\>, non-empty).
- **BaselineLevel**: Enum with two values — `CORRECTNESS` (SpotBugs-type, Level 1) and `BEST_PRACTICE` (Effective Java / PMD-type, Level 2).
- **Obligation**: Enum with two values — `MUST` (mandatory requirement) and `SHOULD` (strong recommendation). Curated per rule; NOT derived from `level`.
- **BaselineLanguage**: Enum with a single MVP value — `JAVA`. Exists as a list-per-rule to support future multi-language expansion without data model changes.
- **RuleSet (JSON resource)**: Wrapper object in `rules.json` — `{ "schemaVersion": 1, "rules": [ <BaselineRule>... ] }`. `schemaVersion` is the evolution hook for format changes.
- **BaselineRuleProvider** (or equivalent loader): The public entry point of the module. Loads the resource, validates it, and returns `List<BaselineRule>`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The baseline provider successfully loads and validates the bundled rule set in any unit test environment without requiring an IDE, build system, or project context.
- **SC-002**: 100% of the ≥13 bundled rules pass all invariant checks (unique id, non-empty statement/rationale, valid minJavaLevel, non-empty languages).
- **SC-003**: All 4 CORRECTNESS categories and all 5 BEST_PRACTICE categories are covered with at least the specified minimum rule counts, verified by a single unit test over the real bundled `rules.json`.
- **SC-004**: Any invalid `rules.json` (structural or invariant violation) is detected at load time and produces a thrown exception with a human-readable message identifying the specific problem — zero silent failures.
- **SC-005**: The module has zero compile-time or runtime dependencies on `:scan` or on any IntelliJ Platform class — verifiable by inspecting the module's `build.gradle.kts` dependency block.
- **SC-006**: The module's unit tests run without IntelliJ Platform fixtures (no `LightPlatformTestCase`, no `BasePlatformTestCase`) — tests are pure JVM, mirroring the `:model` module's test approach.
- **SC-007**: At least one bundled rule has `minJavaLevel > 8`, and this is asserted by a dedicated unit test assertion.

## Assumptions

- The module's classloader can always locate `rules.json` at the classpath path `dev/zahaand/projectscan/baseline/rules.json`; this is guaranteed by the Gradle resource configuration.
- `kotlinx-serialization-json` is acceptable as an external dependency for `:baseline` given that it is already used or planned elsewhere in the plugin (consistent with the technology choices made for `:model`).
- The allowed set of `minJavaLevel` values is `{8, 11, 17, 21}` — these are the LTS Java versions relevant for the MVP. This set may expand in future sprints.
- The `:prompt` module (Sprint 4) is the sole consumer of `:baseline`'s public API. No other module in Sprint 3 calls `BaselineRuleProvider`.
- Rule wording (the actual `statement` and `rationale` text) is a curation concern to be decided during implementation; the spec mandates category coverage and field constraints but does not dictate exact rule prose.
- `BaselineLanguage.JAVA` is the only value in the enum for MVP; the enum and list-per-rule structure are intentionally forward-compatible but not exercised beyond Java in this sprint.
- "Silent team practice" rules are never represented in any data structure — they are a conceptual layer only.

## Out of Scope

- **Filtering rules** by project language level or project language — this belongs to `:prompt` (Sprint 4).
- **Priority hierarchy** realization (project linter rule > baseline > silent team practice) — belongs to `:prompt` (Sprint 4); the baseline set has no ranking.
- **Multi-language rule sets** / non-Java language rules — `BaselineLanguage` enum has only `JAVA` in MVP; the structure is forward-compatible but not exercised.
- **Editing the rule set via plugin settings** or any in-IDE UI for rule management — post-MVP tech debt.
- **Runtime or remote update** of the rule set — rules ship with the plugin binary.
- **Consuming any project facts** from `:scan` — `:baseline` has zero knowledge of the scanned project.
