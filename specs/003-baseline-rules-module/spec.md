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
2. **Given** a test-only replacement `rules.json` where one rule has a blank `statement` or blank `rationale` (empty string or whitespace-only), **When** the baseline provider is invoked, **Then** it throws `BaselineLoadException` identifying the invariant violation — it does NOT return a partial or empty list silently.
3. **Given** a test-only replacement `rules.json` where two rules share the same `id`, **When** the baseline provider is invoked, **Then** it throws `BaselineLoadException` identifying the duplicate id.
4. **Given** a test-only replacement `rules.json` that is malformed JSON, **When** the baseline provider is invoked, **Then** it throws `BaselineLoadException` from the structural parsing layer.
5. **Given** a test-only replacement `rules.json` where a rule's `minJavaLevel` is set to an invalid value (e.g. `7` or `99`), **When** the baseline provider is invoked, **Then** it throws `BaselineLoadException` identifying the disallowed value.
6. **Given** a test-only replacement `rules.json` where the `rules` array is empty, **When** the baseline provider is invoked, **Then** it throws `BaselineLoadException` — an empty bundled set is invalid.
7. **Given** a test-only replacement `rules.json` where a rule carries `level = CORRECTNESS` but `category = EXCEPTION_HANDLING` (a BEST_PRACTICE-level category), **When** the baseline provider is invoked, **Then** it throws `BaselineLoadException` identifying the category/level mismatch.
8. **Given** a test-only replacement `rules.json` where a rule has an empty `languages` list (`"languages": []`), **When** the baseline provider is invoked, **Then** it throws `BaselineLoadException` identifying the empty-languages violation.
9. **Given** a test-only replacement `rules.json` where a rule has a blank `id` (empty string or whitespace-only), **When** the baseline provider is invoked, **Then** it throws `BaselineLoadException` identifying the blank-id violation.

---

### User Story 2 — Consume the Full Unfiltered Rule Set (Priority: P1)

A downstream component (e.g. the future `:prompt` module) calls the baseline provider and receives the entire rule set, whole and unranked, ready for further processing. The provider applies no filtering by language level, project language, or any other project-specific fact. The consumer receives all rules and decides what to do with them.

**Why this priority**: The contract "return the full set, no filtering" is the core module boundary. Violating it would silently mis-deliver rules to `:prompt` (Sprint 4).

**Independent Test**: A unit test loads the real bundled rules and asserts the count equals the total expected number of rules, confirming no filtering occurred.

**Acceptance Scenarios**:

1. **Given** the bundled rule set contains 13 or more rules covering all required categories, **When** the baseline provider is invoked, **Then** it returns all rules — the count is ≥ 13 and matches the count in `rules.json` exactly.
2. **Given** the bundled rules include rules with `minJavaLevel` values of both 8 and 11 (or higher), **When** the baseline provider is invoked, **Then** rules with all `minJavaLevel` values are returned — none filtered out.
3. **Given** the baseline provider is invoked multiple times in the same process, **When** both calls return, **Then** both calls return the **same list instance** (referential equality — not merely a structurally equal copy). This proves that the resource is parsed exactly once and the result is cached. (Traces to FR-001.)

---

### User Story 3 — Rule Metadata Completeness and Correctness (Priority: P2)

Each returned `BaselineRule` carries all 8 mandatory fields with correct values: a stable `id`, a `level` (`CORRECTNESS` or `BEST_PRACTICE`), a `category` (one of 9 `BaselineCategory` values), an `obligation` (`MUST` or `SHOULD`), a non-empty `statement`, a non-empty `rationale`, a valid `minJavaLevel`, and a non-empty `languages` list. Downstream consumers rely on these fields to render rules in the generated Constitution.

**Why this priority**: Metadata correctness is what makes the rules useful in the Constitution. Incomplete or incorrect metadata breaks the Constitution rendering contract.

**Independent Test**: A unit test iterates over every rule returned by the real provider and asserts each field satisfies its constraint (non-null, non-blank, enum values recognized, allowed integer range). All US3 acceptance scenarios operate on the **real bundled `rules.json`** — not test-only JSON substitutions (contrast with US1 negative scenarios, which use test-only strings injected via `loadFromReader`).

**Acceptance Scenarios**:

1. **Given** the bundled rules are loaded, **When** each rule is inspected, **Then** every rule has a non-empty `id`, a non-empty `statement`, and a non-empty `rationale`.
2. **Given** the bundled rules are loaded, **When** each rule's `level` is checked, **Then** it is exactly one of `CORRECTNESS` or `BEST_PRACTICE` — no other value is present.
3. **Given** the bundled rules are loaded, **When** each rule's `category` is checked, **Then** it is exactly one of the 9 `BaselineCategory` values — no other value is present, and the category is consistent with the rule's `level` (CORRECTNESS-level categories pair with `CORRECTNESS`; BEST_PRACTICE-level categories pair with `BEST_PRACTICE`).
4. **Given** the bundled rules are loaded, **When** each rule's `obligation` is checked, **Then** it is exactly one of `MUST` or `SHOULD` — no other value is present.
5. **Given** the bundled rules are loaded, **When** each rule's `minJavaLevel` is checked, **Then** it belongs to the allowed set `{8, 11, 17, 21}`.
6. **Given** the bundled rules are loaded, **When** each rule's `languages` list is checked, **Then** it is non-empty and every entry is `JAVA`.
7. **Given** the bundled rules are loaded, **When** the complete set is inspected, **Then** at least one rule has `minJavaLevel > 8`. (Traces to SC-007.)

---

### User Story 4 — Set Composition Coverage (Priority: P2)

The bundled rule set covers all required correctness and best-practice categories with at least the minimum number of rules per category. This ensures the module delivers adequate baseline coverage for the Constitution across all supported Java projects.

**Why this priority**: A rule set that omits a required category silently degrades Constitution quality. The coverage constraint is the measurable quality bar for the curated set.

**Independent Test**: A unit test groups the real bundled rules by the `category` field and asserts minimum counts per `BaselineCategory` value: ≥2 rules for each of the 4 CORRECTNESS categories and ≥1 rule for each of the 5 BEST_PRACTICE categories (≥13 total).

**Acceptance Scenarios**:

1. **Given** the bundled rules are loaded, **When** rules where `category == NULL_SAFETY` are counted, **Then** there are at least 2 (covering null checks, empty collections, Optional usage).
2. **Given** the bundled rules are loaded, **When** rules where `category == RESOURCE_MANAGEMENT` are counted, **Then** there are at least 2 (covering try-with-resources, no reliance on finalize).
3. **Given** the bundled rules are loaded, **When** rules where `category == CONCURRENCY` are counted, **Then** there are at least 2 (covering safe publication, synchronization pitfalls, atomicity).
4. **Given** the bundled rules are loaded, **When** rules where `category == DANGEROUS_CONSTRUCTS` are counted, **Then** there are at least 2 (covering reference equality, equals/hashCode pairing, ignoring return values).
5. **Given** the bundled rules are loaded, **When** rules are grouped by `category`, **Then** `EXCEPTION_HANDLING` has ≥1, `STRING_PERFORMANCE` has ≥1, `DECOMPOSITION` has ≥1, `IMMUTABILITY` has ≥1, and `INTERFACE_PROGRAMMING` has ≥1.
6. **Given** the bundled rules are counted, **When** all levels are combined, **Then** the total count is ≥ 13.

---

### Edge Cases

- What happens when the `rules.json` resource is entirely absent from the jar? → The loader MUST throw `BaselineLoadException`; it MUST NOT return an empty list.
- What happens when `schemaVersion` in `rules.json` is absent or has an unrecognized value? → The loader MUST throw `BaselineLoadException` identifying the unsupported version — silent fallback is prohibited.
- What happens when a rule's `languages` field is present but empty (`[]`)? → Invariant validation MUST reject this; `languages` must be non-empty per the data model.
- What happens when a rule contains extra/unknown JSON fields not in the model? → The parser silently ignores them (lenient deserialization); no exception is thrown for unrecognized fields.
- What happens when two rules have the same `id` but differ only in case? → Duplicate detection is case-sensitive; `"correctness.null-deref"` and `"CORRECTNESS.NULL-DEREF"` are considered distinct — but the rule ID convention (lowercase dotted) should prevent this in practice.
- What happens when `rules.json` contains extra root-level fields in the wrapper (e.g., `"author"`, `"generated"`)? → They are silently ignored (`ignoreUnknownKeys` applies at the wrapper level, consistent with lenient handling of unknown rule-level fields).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The module MUST expose a public API (loader/provider) that returns `List<BaselineRule>` loaded from the bundled JSON resource. The provider MUST parse and validate the resource exactly once (lazy init) and return the same cached immutable list instance (referential equality) on every subsequent call — callers MAY verify caching by asserting that two successive calls return the same object reference.
- **FR-002**: The module MUST load `rules.json` via the module's classloader (`getResourceAsStream`), NOT via filesystem access. The resource MUST be addressable at the classpath path `dev/zahaand/projectscan/baseline/rules.json`; this path is a contractual constant of the module.
- **FR-003**: The module MUST parse `rules.json` using `kotlinx.serialization` (the only permitted external dependency besides `:model`).
- **FR-004**: The provider MUST perform structural validation (via the parser) and invariant validation (via explicit post-parse checks) before returning any results.
- **FR-005**: On any structural or invariant violation, the provider MUST throw `BaselineLoadException` (a custom exception defined in `:baseline`). The exception message MUST identify (a) the kind of violation, (b) the specific rule by its `id` (or by its index in the `rules` array when `id` is blank — whitespace-only; a missing `id` key is a Phase 1 structural failure and is not addressed by the Phase 3 index fallback), and (c) the violated constraint. The exception's `cause` MUST be non-null when wrapping a library failure (e.g., `SerializationException`, `IOException`); for violations detected by the module's own post-parse invariant checks, `cause` MUST be null and all diagnostics live in the message. The provider MUST NOT silently return an empty or partial list.
- **FR-006**: The returned list MUST be complete and unfiltered — every rule stored in `rules.json` is returned, regardless of `minJavaLevel`, `languages`, or any other field. Completeness is verified by asserting that the count of returned rules equals the count of entries in the `rules` array of `rules.json` (see US2 scenario 1, SC-002).
- **FR-007**: The returned list MUST be unranked — the module stores no priority ordering and applies no sorting by priority.
- **FR-008**: The module MUST validate the resource in the following ordered phases, stopping on the first failure and throwing `BaselineLoadException`:

  **Phase 1 — Structural parse**: JSON MUST be well-formed and MUST deserialize successfully into the `RuleSet` wrapper. `schemaVersion` and `rules` are required fields; their absence is a structural parse failure (caught by the library, wrapped in `BaselineLoadException` with non-null `cause`). Unknown root-level wrapper fields (keys other than `schemaVersion` and `rules`) are silently ignored, consistent with lenient handling at the rule level.

  **Phase 2 — Schema version**: `schemaVersion` MUST equal `1`. Any other integer value causes `BaselineLoadException`; the message states the value found (e.g., `"unsupported schemaVersion: 2"`). Absence is a Phase 1 structural failure.

  **Phase 3 — Per-rule invariants**: The `rules` array MUST be non-empty (checked before per-rule iteration; an empty array throws before any per-rule check). Each rule is then validated; loading fails on the first violation encountered; order among the per-rule checks is not fixed, **except** that the blank-`id` check MUST precede the uniqueness (seenIds) check — a blank `id` MUST be reported as a blank-id violation, not misreported as a duplicate:
  - `id` is non-blank (empty string or whitespace-only fails) and unique across all rules. Uniqueness is **case-sensitive**: `"correctness.null-deref"` and `"CORRECTNESS.NULL-DEREF"` are considered distinct.
  - `statement` is non-blank (empty string or whitespace-only fails).
  - `rationale` is non-blank (empty string or whitespace-only fails).
  - `minJavaLevel` belongs to `{8, 11, 17, 21}` (LTS Java versions for MVP; intermediate releases such as 9, 10, or 14 are excluded; this set may expand in future sprints).
  - `languages` list is non-empty.
  - `category` is consistent with `level` per the following mapping — **authoritative single source of truth** for the category/level constraint:
    - `NULL_SAFETY`, `RESOURCE_MANAGEMENT`, `CONCURRENCY`, `DANGEROUS_CONSTRUCTS` → MUST pair with `level = CORRECTNESS`
    - `EXCEPTION_HANDLING`, `STRING_PERFORMANCE`, `DECOMPOSITION`, `IMMUTABILITY`, `INTERFACE_PROGRAMMING` → MUST pair with `level = BEST_PRACTICE`

  The `id` naming convention (lowercase-dotted, `<level-prefix>.<slug>`) is a **curation guideline** for human readability only — it is NOT validated by any invariant. Validators MUST NOT check `id` format beyond non-blank and uniqueness.
- **FR-009**: The `rules.json` resource MUST use wrapper format `{ "schemaVersion": 1, "rules": [ ... ] }` to support future schema evolution.
- **FR-010**: The bundled rule set MUST satisfy category coverage as determined by the explicit `category` field of each returned `BaselineRule` (NOT by `id` string patterns — `id` is a stable identifier, not a classification key): ≥2 rules with `category == NULL_SAFETY`; ≥2 with `RESOURCE_MANAGEMENT`; ≥2 with `CONCURRENCY`; ≥2 with `DANGEROUS_CONSTRUCTS`; ≥1 with `EXCEPTION_HANDLING`; ≥1 with `STRING_PERFORMANCE`; ≥1 with `DECOMPOSITION`; ≥1 with `IMMUTABILITY`; ≥1 with `INTERFACE_PROGRAMMING`; ≥13 rules total.
- **FR-011**: At least one bundled rule MUST have `minJavaLevel > 8` so the language-level field is demonstrable and testable.
- **FR-012**: The module MUST NOT contain any filtering, ranking, or priority logic — these belong exclusively in `:prompt` (Sprint 4).
- **FR-013**: The module MUST NOT depend on `:scan` and MUST NOT reference or import any type from `:scan`.
- **FR-014**: The module MUST NOT contain style, formatting, or naming rules — only correctness and architectural best-practice rules.
- **FR-015**: The `BaselineRule` data type, `BaselineLevel` enum, `BaselineCategory` enum, `Obligation` enum, and `BaselineLanguage` enum MUST be defined inside `:baseline`, not in `:model`. The **public API of `:baseline`** consists exactly of: `BaselineRuleProvider`, `BaselineRule`, `BaselineLevel`, `BaselineCategory`, `Obligation`, `BaselineLanguage`, and `BaselineLoadException`. All other types — including the JSON wrapper DTO (`RuleSet`), parsing helpers, and validators — MUST be `internal` in Kotlin and MUST NOT be part of the public API. `BaselineRule` intentionally has no `origin` field; origin-tagging is a `:prompt` rendering concern (see Out of Scope).

  **Rationale**: A `BaselineRule` is a curated code-quality entity, not a section of the scan model. The `:model` module is the contract of the five scan sections (`StackInfo`, `LinterInfo`, etc.); placing baseline types there would blur `:model`'s domain and couple the scan contract to the baseline concept.

- **FR-016**: The category/level consistency mapping defined in FR-008 is the sole responsibility of `:baseline`; it is validated at load time by `BaselineRuleProvider`. Consumers (e.g., `:prompt`, Sprint 4) MUST rely on this pre-validation and MUST NOT re-validate the mapping independently.

### Key Entities *(include if feature involves data)*

- **BaselineRule**: A single code-quality rule. Has 8 fields: `id` (String, stable unique non-blank identifier — used for deduplication and future override support, NOT for category classification; the lowercase-dotted naming convention is a curation guideline, not an enforced invariant), `level` (BaselineLevel), `category` (BaselineCategory), `obligation` (Obligation), `statement` (String, non-blank), `rationale` (String, non-blank), `minJavaLevel` (Int, from `{8, 11, 17, 21}`), `languages` (List\<BaselineLanguage\>, non-empty). The `level` and `category` fields are both explicit and stored; `level` is NOT derived from `category` at runtime — the redundancy is intentional so downstream consumers can use `level` directly without knowing the category→level mapping.
- **BaselineLevel**: Enum with two values — `CORRECTNESS` (SpotBugs-type, Level 1) and `BEST_PRACTICE` (Effective Java / PMD-type, Level 2).
- **BaselineCategory**: Enum with 9 values representing the semantic category of a rule. Four CORRECTNESS-level values: `NULL_SAFETY`, `RESOURCE_MANAGEMENT`, `CONCURRENCY`, `DANGEROUS_CONSTRUCTS`. Five BEST_PRACTICE-level values: `EXCEPTION_HANDLING`, `STRING_PERFORMANCE`, `DECOMPOSITION`, `IMMUTABILITY`, `INTERFACE_PROGRAMMING`. Each value belongs to exactly one `BaselineLevel`; the loader enforces this consistency as an invariant.
- **Obligation**: Enum with two values — `MUST` (mandatory requirement) and `SHOULD` (strong recommendation). Curated per rule; NOT derived from `level` or `category`.
- **BaselineLanguage**: Enum with a single MVP value — `JAVA`. Exists as a list-per-rule to support future multi-language expansion without data model changes.
- **RuleSet (JSON resource)**: Wrapper object in `rules.json` — `{ "schemaVersion": 1, "rules": [ <BaselineRule>... ] }`. `schemaVersion` is the evolution hook for format changes.
- **BaselineRuleProvider** (or equivalent loader): The public entry point of the module. Parses and validates the resource once on first access (lazy init) and caches the immutable result; returns the same `List<BaselineRule>` **instance** (referential equality) on every subsequent call. Throws `BaselineLoadException` on any failure.
- **BaselineLoadException**: Custom unchecked exception defined in `:baseline`. Carries a human-readable violation message and wraps the original cause. Used as the single failure signal across all load and validation errors; enables `:prompt` (Sprint 4) to catch baseline failures specifically.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The baseline provider successfully loads and validates the bundled rule set in any unit test environment without requiring an IDE, build system, or project context.
- **SC-002**: 100% of the ≥13 bundled rules pass all invariant checks (non-blank unique id, non-blank statement, non-blank rationale, valid minJavaLevel, non-empty languages, category consistent with level per the FR-008 mapping).
- **SC-003**: All 4 CORRECTNESS categories and all 5 BEST_PRACTICE categories are covered with at least the specified minimum rule counts, verified by a single unit test that groups rules by the `category` **field** (NOT by `id` string patterns) over the real bundled `rules.json`.
- **SC-004**: Any invalid `rules.json` (structural or invariant violation) is detected at load time and produces a thrown `BaselineLoadException` with a human-readable message identifying the specific problem — zero silent failures.
- **SC-005**: The module has zero compile-time or runtime dependencies on `:scan` or on any IntelliJ Platform class — verifiable by inspecting the module's `build.gradle.kts` dependency block and by confirming that `:scan` and IntelliJ Platform artifacts are absent from the output of `./gradlew :baseline:dependencies`.
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

## Clarifications

### Session 2026-06-14

- Q: Should `BaselineRuleProvider` parse `rules.json` once and cache the result, or re-parse on every call? → A: Parse-once, cache internally (lazy init); same list returned on every call.
- Q: What exception type should the provider throw on any load or validation failure? → A: A single custom exception (`BaselineLoadException`) wrapping the cause and carrying the violation message.
- Correction: Added explicit `category: BaselineCategory` as the 3rd field of `BaselineRule` (8-field model). `BaselineCategory` enum has 9 values (4 CORRECTNESS-level, 5 BEST_PRACTICE-level). Category coverage (FR-010, SC-003, US4) is now verified via the `category` field, not id-string patterns. Category/level consistency is enforced as an invariant (FR-008). The `id` field is a stable identifier only — not the basis for category classification.

## Out of Scope

- **Filtering rules** by project language level or project language — this belongs to `:prompt` (Sprint 4).
- **Priority hierarchy** realization (project linter rule > baseline > silent team practice) — belongs to `:prompt` (Sprint 4); the baseline set has no ranking.
- **Multi-language rule sets** / non-Java language rules — `BaselineLanguage` enum has only `JAVA` in MVP; the structure is forward-compatible but not exercised.
- **Editing the rule set via plugin settings** or any in-IDE UI for rule management — post-MVP tech debt.
- **Runtime or remote update** of the rule set — rules ship with the plugin binary.
- **Consuming any project facts** from `:scan` — `:baseline` has zero knowledge of the scanned project.
- **Origin-tagging of rendered rules** (`"baseline quality requirement"` vs `"project standard"`) — this is a `:prompt` (Sprint 4) responsibility applied at render time. `BaselineRule` intentionally carries no `origin` field; the field is unnecessary in the data model because `:prompt` already knows the source of every rule it processes. This keeps the `:baseline` schema stable while remaining compliant with Constitution Principle IV, which requires origin-tagging in the rendered output — not in the data model. *(Cross-reference: FR-015.)*
