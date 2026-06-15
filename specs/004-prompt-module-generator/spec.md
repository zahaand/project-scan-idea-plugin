# Feature Specification: Prompt — Constitution Prompt Generator Module

**Feature Branch**: `004-prompt-module-generator`  
**Created**: 2026-06-15  
**Status**: Draft  
**Input**: User description: "Build the `prompt` module (Sprint 4 of the project-scan IntelliJ plugin): a generator that turns the collected project model into a ready-to-use prompt for the Spec Kit `/speckit-constitution` command."

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Generate a Complete Prompt from a Fully-Scanned Project (Priority: P1)

A developer has a well-configured Java project in IntelliJ. The plugin has scanned it and populated all five data sections: a detected tech stack (Java 17, Gradle, several dependencies), active linter rules from Checkstyle and PMD, code style sources, test frameworks (JUnit 5), and a multi-module project structure. The developer invokes the prompt generator. The generator returns a ready-to-use text prompt addressed to `/speckit-constitution` that covers all six blocks — Core Principles, Tech Stack, Code Style & Static Analysis, Testing, Project Structure, and Governance — using only the scanned data, with every rule correctly tagged by its origin.

**Why this priority**: This is the end-to-end success case. A correct, complete prompt is the primary deliverable of the module; all other stories test special cases of this core flow.

**Independent Test**: A unit test that provides a fully-populated `ScanResult` and a non-empty list of `BaselineRule` objects, calls the generator, and asserts that the returned prompt text is non-empty, contains all six block headings, and contains at least one rule from each origin group.

**Acceptance Scenarios**:

1. **Given** a `ScanResult` with all sections as `SectionResult.Ok` (populated stack, linters, code style, tests, structure) and a non-empty `BaselineRule` list, **When** the generator is called, **Then** it returns a non-empty `ConstitutionPrompt` whose rendered text contains six `##` Markdown headings: `## Core Principles`, `## Tech Stack`, `## Code Style & Static Analysis`, `## Testing`, `## Project Structure`, and `## Governance`.
2. **Given** the scanned linter section contains five active rules (mix of ERROR and WARNING severity), **When** the generator produces the prompt, **Then** each of those five linter rules appears in the Core Principles block tagged with the label `"project standard"` — no other label.
3. **Given** the baseline rule list contains 13 rules and the project language level allows all of them, **When** the generator produces the prompt, **Then** each of those 13 baseline rules appears in the Core Principles block tagged with the label `"baseline quality requirement"` — no other label.
4. **Given** the prompt is generated from a fully-populated model, **When** the rendered text is inspected, **Then** the "project standard" principle group appears before the "baseline quality requirement" principle group in the Core Principles block.
5. **Given** the prompt is generated, **When** the rendered text is inspected, **Then** the Tech Stack block contains all data from `StackInfo`: build system, JDK version, language level, and all declared dependencies — no fabricated data and no dependency omitted.

---

### User Story 2 — Priority Hierarchy Is Clearly Expressed (Priority: P1)

A developer reads the generated prompt before passing it to `/speckit-constitution`. The prompt makes it unambiguous that project linter rules are the highest-priority standards (especially those that break the build), that baseline quality requirements apply universally but yield to project rules on conflict, and that unwritten practices are lowest priority. The developer can use this prompt as-is without needing to manually clarify priority intent to the Constitution command.

**Why this priority**: The Constitution's Core Principles section is meaningless if the priority ordering is unclear. An incorrectly prioritized prompt produces a misleading Constitution.

**Independent Test**: A unit test that generates a prompt with both project linter rules and baseline rules, then asserts that (a) the "project standard" group appears before the "baseline quality requirement" group, and (b) the rendered text contains explicit priority/conflict-resolution language in the Core Principles block.

**Acceptance Scenarios**:

1. **Given** the project has both linter rules and applicable baseline rules, **When** the prompt is generated, **Then** the rendered Core Principles block contains explicit wording stating that project standard rules take precedence over baseline quality requirements in case of conflict.
2. **Given** the linter section contains rules with `severity = ERROR` and rules with `severity = WARNING`, **When** the prompt is generated, **Then** the "project standard" principle group renders them under two `####` sub-headings: `#### Mandatory (build-breaking)` for rules where `severity = ERROR` or `breaksBuild = true`, and `#### Advisory` for all others (including rules where `breaksBuild = null`).
3. **Given** the prompt is generated with any combination of project and baseline rules, **When** the rendered text is inspected, **Then** no rule appears without a clearly associated origin tag ("project standard" or "baseline quality requirement").

---

### User Story 3 — Baseline Rules Filtered by Java Language Level (Priority: P2)

A developer's project targets Java 11. The baseline rule set contains both Java-8-compatible rules and Java-17-only rules. The generator automatically filters out rules that require a Java version higher than the project's declared language level before including them in the prompt. The developer receives a prompt containing only the baseline rules that are actually applicable to their Java version.

**Why this priority**: Emitting rules the project cannot use wastes attention and may produce a Constitution with incompatible standards. Language-level filtering is the primary applicability gate.

**Independent Test**: A unit test that sets `languageLevel = "11"` and provides a mixed baseline list (some `minJavaLevel = 8`, some `minJavaLevel = 17`), then asserts that rules with `minJavaLevel > 11` are absent from the generated prompt.

**Acceptance Scenarios**:

1. **Given** `StackInfo.languageLevel = "11"` and a baseline list containing a rule with `minJavaLevel = 8` and a rule with `minJavaLevel = 17`, **When** the generator is called, **Then** the Java-8 rule appears in the prompt and the Java-17 rule does not.
2. **Given** `StackInfo.languageLevel = "21"`, **When** the generator is called with the full baseline set, **Then** all baseline rules appear in the prompt (no filtering occurs for the highest supported level).
3. **Given** `StackInfo.languageLevel = "8"`, **When** the generator is called, **Then** only baseline rules with `minJavaLevel = 8` appear; rules with `minJavaLevel > 8` are absent.
4. **Given** `StackInfo.languageLevel` is `null` (absent), **When** the generator is called, **Then** all baseline rules appear in the prompt — the full set is emitted without filtering.
5. **Given** `StackInfo.languageLevel` is a string with no leading digit (e.g., `"unknown"`), **When** the generator is called, **Then** all baseline rules appear in the prompt — no leading integer can be extracted, so no filtering is applied.
6. **Given** the stack section is `SectionResult.Empty` (no stack data detected), **When** the generator is called, **Then** all baseline rules appear in the prompt — the full set is emitted without filtering.
7. **Given** `StackInfo.languageLevel` is a suffixed string with a leading digit (e.g., `"17.0.1"` or `"21_PREVIEW"`), **When** the generator is called, **Then** the generator extracts the leading integer (`17` or `21` respectively) and filters baseline rules whose `minJavaLevel` exceeds that value — these strings are NOT treated as absent.
8. **Given** `StackInfo.languageLevel = " 11"` (leading whitespace), **When** the generator is called, **Then** the generator trims leading whitespace and extracts `11`, applying filtering for `minJavaLevel > 11`.
9. **Given** `StackInfo.languageLevel = "11a"` (digits followed by a non-digit character), **When** the generator is called, **Then** the generator extracts `11` (stops at the non-digit) and applies filtering for `minJavaLevel > 11`.

---

### User Story 4 — Minimal/Empty Project Yields an Honest Baseline-Only Prompt (Priority: P2)

A developer opens a brand-new or not-yet-imported project in IntelliJ. The scan collects no data: all five sections return `SectionResult.Empty`. The developer invokes the prompt generator anyway. The generator returns a prompt that honestly marks each scan-dependent block as "not detected" while still including all applicable baseline quality requirements in the Core Principles block. The developer gets a usable starting point for their Constitution — anchored entirely on the universal baseline.

**Why this priority**: The baseline always applies, even when no project data exists. An empty-project case that crashes or returns garbage would make the module unreliable.

**Independent Test**: A unit test that provides a `ScanResult` with all five sections as `SectionResult.Empty` and a non-empty baseline list, calls the generator, and asserts that (a) the prompt is non-empty, (b) the Core Principles block contains baseline rules, (c) the "project standard" group contains a "no rules detected" notation, and (d) none of the other five blocks contains fabricated content.

**Acceptance Scenarios**:

1. **Given** a `ScanResult` where all five sections are `SectionResult.Empty`, **When** the generator is called with a non-empty baseline list, **Then** the returned prompt is non-empty and its rendered text contains six block headings.
2. **Given** all scan sections are `SectionResult.Empty`, **When** the generated prompt is inspected, **Then** the "project standard" principle group is present but explicitly notes that no project linter rules were detected — it does NOT omit the group entirely.
3. **Given** all scan sections are `SectionResult.Empty`, **When** the generated prompt is inspected, **Then** all baseline rules appear in the Core Principles block (language-level filtering is inactive: no level to compare against).
4. **Given** all scan sections are `SectionResult.Empty`, **When** the generated prompt is inspected, **Then** the Tech Stack, Code Style, Testing, and Project Structure blocks each contain an explicit "not detected" or "not available" marker — none contain guessed or inferred data.
5. **Given** a `ScanResult` where some sections are `SectionResult.Ok` and others are `SectionResult.Error`, **When** the generator is called, **Then** Ok sections contribute their data, Error sections are marked as "not available" with an optional error note, and no exception is thrown.

---

### Edge Cases

- What happens when the baseline rule list is empty? → The generator MUST still produce a prompt; the `### baseline quality requirement` group MUST be present (not omitted) and MUST include a notation line stating that no baseline rules are available.
- What happens when `LinterInfo.activeRules` is an empty list (tool configured but no rules active)? → The `### project standard` group MUST be present (not omitted) and MUST include a notation line stating that no active linter rules were detected.
- What happens when the extracted leading integer is not in `{8, 11, 17, 21}` (e.g., `"9"` → 9, `"16"` → 16)? → Filtering still applies — any baseline rule whose `minJavaLevel` exceeds the extracted integer is excluded. Only strings with no leading digit (and `null`) trigger the no-filtering fallback.
- What happens when the same conceptual rule appears in both linter rules and baseline rules? → The generator does not detect semantic duplicates. Both rules appear with their respective origin tags; conflict resolution is left to the `/speckit-constitution` command per the priority hierarchy expressed in the prompt.
- What happens when a `SectionResult.Error` carries a diagnostic cause string? → The generator MUST include the non-null cause in the "not available" notation (per FR-008). When `cause` is `null`, the notation is plain `"not available"` with no cause suffix — never `"(cause: null)"`. MUST NOT suppress a non-null cause silently.
- What happens when a section is `SectionResult.Ok` but the data collection is empty (e.g., `CodeStyleInfo(sources = emptyList())`)? → Treated identically to `SectionResult.Empty` — rendered as `"not detected"` (per FR-008). No data was found to contribute; fabricating content is prohibited.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The module MUST expose a public entry point — `PromptGenerator` or equivalent — that accepts a `ScanResult` (from `:scan` / `:model`) and a `List<BaselineRule>` (from `:baseline`), and returns a `ConstitutionPrompt` (a structured value type with a method to render to `String`). The generator MUST NOT call any IntelliJ Platform API — this prohibition covers transitive presence: no `com.intellij.*` class may appear on the `:prompt` compile or test classpath, including via a `:model` or `:baseline` type; SC-006's classpath inspection is the enforcement mechanism. The generator MUST NOT perform file I/O, or access clipboard.
- **FR-002**: The generated `ConstitutionPrompt` MUST be a create-oriented prompt addressed to `/speckit-constitution`. The rendered prompt MUST open with an explicit instructional address to `/speckit-constitution` — exact preamble wording is an implementation choice, but the address MUST be present as the first content in the rendered output and MUST appear within the first 200 characters of the rendered string, before any `##` heading. It MUST instruct the command to assemble a new project Constitution from the supplied data. It MUST NOT reference, detect, or read any existing Constitution file. Mode detection, semantic-version bumping, and Sync Impact Report are explicitly excluded from this module's scope.
- **FR-003**: The generated prompt MUST present data in exactly six named blocks in this order: (1) Core Principles, (2) Tech Stack, (3) Code Style & Static Analysis, (4) Testing, (5) Project Structure, (6) Governance. All six blocks MUST appear in every generated prompt, regardless of which scan sections contain data.
- **FR-004**: The Core Principles block MUST contain exactly two origin groups, in this order: (1) a "project standard" group derived from `LinterInfo.activeRules`, then (2) a "baseline quality requirement" group derived from the (filtered) baseline rule list. Each group MUST be labeled using exactly the English origin tag: `"project standard"` for linter rules; `"baseline quality requirement"` for baseline rules. Every individual rule emitted in either group MUST be associated with its group's origin tag in the rendered Markdown output; this is a guarantee about the observable rendered string, not about the intermediate `OriginGroup` structure.
- **FR-005**: The prompt wording MUST express the priority hierarchy: project standard rules (especially those with `severity = ERROR` or `breaksBuild = true`; a rule whose build-breaking flag is null is treated as advisory, never mandatory) take precedence over baseline quality requirements; baseline quality requirements take precedence over unwritten team practice. In case of conflict between a project standard rule and a baseline rule, the project standard rule wins. This hierarchy MUST be conveyed through the ordering of the two principle groups and through explicit conflict-resolution language in the Core Principles block.
- **FR-006**: The "project standard" group MUST include all rules from `LinterInfo.activeRules`, regardless of severity. The group MUST render mandatory and advisory rules in two separate `####` (level-4) sub-sections within the `### project standard` group: `#### Mandatory (build-breaking)` for rules where `severity = ERROR` OR `breaksBuild = true`; `#### Advisory` for all others. A rule with `breaksBuild = null` (undetermined build-breaking status — the Gradle path may not expose this flag) MUST be treated as NOT mandatory and placed under `#### Advisory`; the generator MUST NOT throw or infer mandatory on a null flag. A sub-section that would contain zero rules MUST be omitted entirely — only non-empty sub-sections are rendered. When `LinterInfo.activeRules` is empty, the `### project standard` group renders only its `emptyNotation` line; NO `#### Mandatory (build-breaking)` and NO `#### Advisory` headings are emitted.
- **FR-007**: Baseline rules MUST be filtered by the project's Java language level before inclusion. The language level is determined by extracting the leading decimal integer from `StackInfo.languageLevel` (a nullable `String`): `"11"` → 11, `"17.0.1"` → 17, `"21_PREVIEW"` → 21, `"9"` → 9, `" 11"` → 11 (leading space stripped by `trimStart`). A baseline rule whose `minJavaLevel` exceeds the extracted integer MUST be excluded. NO filtering is applied — the full baseline set is emitted — when: `StackInfo` is unavailable (section is `Empty` or `Error`), `languageLevel` is `null`, `languageLevel` is `""` (empty string), or the string's first non-space character is not a digit (e.g., `"unknown"`).
- **FR-008**: Sections of the generated prompt derived from unavailable scan data MUST use the following fixed markers: `"not detected"` when the section is `SectionResult.Empty`; `"not available"` when the section is `SectionResult.Error`. When a `SectionResult.Error` carries a non-null `cause`, the generator MUST include that cause in the "not available" notation (e.g., `"not available (cause: <cause>)"`); when `cause` is `null`, the notation is plain `"not available"` with no cause suffix. A section that is `SectionResult.Ok` but contains no data entries (e.g., an empty collection such as `CodeStyleInfo(sources = emptyList())`) is treated the same as `SectionResult.Empty` for display purposes — rendered as `"not detected"`. The `Ok`-empty vs `Empty` distinction carries no value for the Constitution, since either way the section has no data to contribute. The generator MUST NOT fabricate or infer stack versions, framework names, rule sets, or any other project-specific data that was not present in the scanned model.
- **FR-009**: The Governance block MUST be included in every generated prompt with standard `/speckit-constitution` governance guidance, since no scan section maps to Governance. It does not vary per project and is not derived from any scan section. Regardless of exact phrasing (an implementation choice), the Governance block MUST contain all three of the following elements: (1) a constitution semantic-versioning policy describing when MAJOR, MINOR, and PATCH version bumps apply; (2) a changelog convention; (3) an amendment and compliance procedure. These elements are verifiable by their presence, not by exact text match.
- **FR-010**: The module MUST depend only on `:model` (for `ScanResult`, `StackInfo`, `LinterInfo`, etc.) and `:baseline` (for `BaselineRule`). `:scan` is the module that produces `ScanResult` at runtime, but the `:prompt` module depends on the types in `:model`, not on `:scan` itself. Nothing MUST depend on `:prompt`.
- **FR-011**: All pure-logic behaviors of the generator (full model, empty model, language-level filtering at each boundary) MUST be covered by unit tests using JUnit 5. Tests MUST run without any IntelliJ Platform test base class or fixture — `LightPlatformTestCase` and `BasePlatformTestCase` are named as examples, not an exhaustive list; tests MUST be pure JVM JUnit 5 with no IntelliJ Platform test infrastructure of any kind.
- **FR-012**: The rendered output of `ConstitutionPrompt.render()` MUST be Markdown with the following heading hierarchy: `##` (level-2) for the six block names (e.g., `## Core Principles`); `###` (level-3) for the two origin group labels within Core Principles (`### project standard`, `### baseline quality requirement`); `####` (level-4) for the mandatory/advisory sub-sections within the `### project standard` group (`#### Mandatory (build-breaking)`, `#### Advisory`) — emitted only when non-empty per FR-006. Individual rules MUST be rendered as `-` bullet list items within their enclosing section. Separator conventions: exactly one blank line separates each `##` block from the next, and exactly one blank line separates each `###` group from the next within Core Principles. The `### baseline quality requirement` group is ALWAYS rendered as a flat `-` bullet list with NO `####` sub-sections; only the `### project standard` group uses `####` Mandatory/Advisory sub-sections. This Markdown structure is the contractual output surface verified by all unit tests that assert on rendered text.

### Key Entities *(include if feature involves data)*

- **PromptGenerator**: The public entry point of the module. Accepts a `ScanResult` and a `List<BaselineRule>`; returns a `ConstitutionPrompt`. Stateless — contains no cached state; safe to call repeatedly with different inputs.
- **ConstitutionPrompt**: The structured output of the generator. Represents the complete prompt as an ordered list of `PromptBlock` values. Provides a method to render itself to a Markdown `String`: `##` headings for blocks, `###` headings for origin groups within Core Principles, `-` bullet items for individual rules. Immutable after construction.
- **PromptBlock**: A named section of the generated prompt (Core Principles, Tech Stack, etc.). Contains the block heading and its rendered content. Blocks appear in the canonical order defined by FR-003.
- **OriginGroup**: Within the Core Principles block, a named grouping of rules sharing the same origin tag. Carries: origin label (`"project standard"` or `"baseline quality requirement"`), list of formatted rules, and any notations for the empty-rules case.
- **ScanResult** (from `:model`): The root aggregate of all five scanned sections. Each section is `SectionResult.Ok<T>`, `SectionResult.Empty`, or `SectionResult.Error`. The generator reads this value; it never writes to it.
- **BaselineRule** (from `:baseline`): A single baseline quality rule. The generator reads `id`, `obligation`, `statement`, `rationale`, `minJavaLevel`; it uses `minJavaLevel` for filtering and `obligation` for rendered wording (`MUST` / `SHOULD`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For every valid combination of `ScanResult` and `List<BaselineRule>` (including all-empty sections and empty baseline list), the generator completes without throwing an exception and returns a non-empty `ConstitutionPrompt`. Both parameters are non-null Kotlin types; the caller guarantees non-null arguments and null-argument handling is out of scope (enforced by the type system).
- **SC-002**: 100% of emitted rules in the generated prompt carry an origin tag — verified by a unit test that parses the rendered Markdown and confirms every `-` bullet item within the `## Core Principles` section appears under a `###` heading whose text is exactly `"project standard"` or `"baseline quality requirement"`. No `-` bullet may appear within `## Core Principles` outside such a heading.
- **SC-003**: The `### project standard` heading always precedes the `### baseline quality requirement` heading in the rendered Core Principles block — verified by asserting the character-offset order of the two `###` headings in the rendered Markdown string.
- **SC-004**: Language-level filtering boundary cases are verified by dedicated unit tests: (a) `languageLevel = "11"` → rules with `minJavaLevel > 11` are absent; (b) `languageLevel = "17.0.1"` → leading integer 17 extracted → rules with `minJavaLevel > 17` are absent; (c) `languageLevel = null` → full baseline set emitted; (d) `languageLevel = "unknown"` → full baseline set emitted (no leading digit); (e) `languageLevel = ""` → full baseline set emitted (empty string, no leading digit); (f) `languageLevel = " 11"` (leading whitespace) → leading whitespace stripped via `trimStart`, extracts 11 → rules with `minJavaLevel > 11` are absent; (g) `languageLevel = "11a"` (digits then non-digit) → extracts 11 (stops at non-digit) → rules with `minJavaLevel > 11` are absent.
- **SC-005**: When all five scan sections are `SectionResult.Empty`, the generated prompt contains baseline-sourced Core Principles content and explicit "not detected" markers in all scan-dependent blocks — verified by unit test inspection of the rendered string.
- **SC-006**: The `:prompt` module's build configuration lists only `:model` and `:baseline` as compile-time dependencies. No IntelliJ Platform SDK classes appear in the module's test classpath.
- **SC-007**: All generator unit tests are pure JVM and produce deterministic output — the same input always yields the same rendered string (no timestamps, no random IDs in the output).

## Assumptions

- `StackInfo.languageLevel` is `null`, a plain version string (e.g., `"8"`, `"11"`), or a suffixed/preview string (e.g., `"17.0.1"`, `"21_PREVIEW"`). The generator extracts the leading decimal integer for filtering. Only strings whose first non-space character is not a digit (e.g., `"unknown"`) and `null` are treated as absent — no filtering applied.
- All rules from `LinterInfo.activeRules` are included in the "project standard" principle group regardless of severity. Severity and `breaksBuild` influence the wording within the group but do not gate a rule's inclusion.
- The Governance block content is a fixed, standard text (not derived from scan data), since no scan section maps to Governance concepts. Its wording is an implementation choice.
- The `:scan` module's `ScanResult` type (defined in `:model`) is the input contract. The generator does not call any scanner or IntelliJ API — it only reads the already-populated `ScanResult`.
- The `ConstitutionPrompt.render()` output is deterministic for a given input — no runtime-variable content (timestamps, UUIDs) appears in the rendered string.
- The `:prompt` module is the sole module that performs language-level filtering and origin-tagging of rules; `:baseline` ships rules with no applied filtering, as per the `:baseline` Sprint 3 contract (FR-012 of `:baseline`).
- `PromptGenerator.generate()` parameters (`ScanResult`, `List<BaselineRule>`) are non-null Kotlin types; the caller guarantees non-null arguments. Null-argument handling is explicitly out of scope and enforced at compile time by the Kotlin type system. SC-001's no-exception guarantee applies only to valid non-null inputs.
- Each `BaselineRule.obligation` value (`MUST`, `SHOULD`) MUST be rendered as an explicit leading obligation marker in the corresponding bullet — `"MUST"` for `Obligation.MUST` and `"SHOULD"` for `Obligation.SHOULD` — so the strength of each requirement is visible in the generated prompt. Exact bullet format is an implementation choice, but the obligation marker MUST be deterministically present.
- `ScanResult`, `SectionResult`, and all `:model` types the generator reads (`StackInfo`, `LinterInfo`, `ActiveRule`, `RuleSeverity`, `CodeStyleInfo`, `TestInfo`, `StructureInfo`, `Dependency`, `BuildSystem`, `StyleSource`, `TestFramework`, `Module`) are pure Kotlin data classes with no IntelliJ Platform dependency — confirmed by `:model`'s `build.gradle.kts` (stdlib + JUnit 5 test only). This is a prerequisite for SC-006. If any consumed `:model` type is found to carry a platform dependency, that is a blocker to be surfaced at plan time, not silently worked around.

## Clarifications

### Session 2026-06-15

- Q: What output format should `ConstitutionPrompt.render()` produce? → A: Markdown — `##` headings for blocks, `###` headings for origin groups within Core Principles, `-` bullet items for rules (FR-012).
- Q: How should the "project standard" group distinguish mandatory from advisory rules? → A: Two `####` sub-headings — `#### Mandatory (build-breaking)` and `#### Advisory`; empty sub-sections are omitted (FR-006, FR-012).
- Q: Which dependencies from `StackInfo.dependencies` should appear in the Tech Stack block? → A: All declared dependencies — every entry emitted verbatim, no filtering (US1-scenario5).

## Out of Scope

- **Writing the prompt to disk, clipboard, or any external system** — belongs to the UI module (Sprint 5).
- **Detecting or reading an existing `.specify/memory/constitution.md`** — the generator is mode-agnostic; mode detection belongs to the `/speckit-constitution` command.
- **Semantic-version bumping or Sync Impact Report** — belongs to the `/speckit-constitution` command protocol.
- **Any UI interaction** (dialogs, progress indicators, IntelliJ notifications) — belongs to Sprint 5.
- **Ranking or sorting rules within an origin group** — the generator preserves the order in which rules are provided; ranking is not in scope.
- **Filtering by language** (e.g., excluding `JAVA`-only rules for a Kotlin project) — only Java language-level filtering is in scope; language-type filtering is a future concern.
- **Detecting semantic conflicts between project linter rules and baseline rules** — the generator expresses the priority hierarchy in the prompt text; actual conflict resolution is left to `/speckit-constitution`.
- **Calling `BaselineRuleProvider` directly** — the generator accepts an already-loaded `List<BaselineRule>` as input; loading baseline rules is the caller's responsibility.
