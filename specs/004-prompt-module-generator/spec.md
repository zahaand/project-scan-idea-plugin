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

1. **Given** a `ScanResult` with all sections as `SectionResult.Ok` (populated stack, linters, code style, tests, structure) and a non-empty `BaselineRule` list, **When** the generator is called, **Then** it returns a non-empty `ConstitutionPrompt` whose rendered text contains six block headings matching: "Core Principles", "Tech Stack", "Code Style", "Testing", "Project Structure", and "Governance".
2. **Given** the scanned linter section contains five active rules (mix of ERROR and WARNING severity), **When** the generator produces the prompt, **Then** each of those five linter rules appears in the Core Principles block tagged with the label `"project standard"` — no other label.
3. **Given** the baseline rule list contains 13 rules and the project language level allows all of them, **When** the generator produces the prompt, **Then** each of those 13 baseline rules appears in the Core Principles block tagged with the label `"baseline quality requirement"` — no other label.
4. **Given** the prompt is generated from a fully-populated model, **When** the rendered text is inspected, **Then** the "project standard" principle group appears before the "baseline quality requirement" principle group in the Core Principles block.
5. **Given** the prompt is generated, **When** the rendered text is inspected, **Then** the Tech Stack block contains information derived from `StackInfo` (e.g., build system, language level, key dependencies) — no fabricated data.

---

### User Story 2 — Priority Hierarchy Is Clearly Expressed (Priority: P1)

A developer reads the generated prompt before passing it to `/speckit-constitution`. The prompt makes it unambiguous that project linter rules are the highest-priority standards (especially those that break the build), that baseline quality requirements apply universally but yield to project rules on conflict, and that unwritten practices are lowest priority. The developer can use this prompt as-is without needing to manually clarify priority intent to the Constitution command.

**Why this priority**: The Constitution's Core Principles section is meaningless if the priority ordering is unclear. An incorrectly prioritized prompt produces a misleading Constitution.

**Independent Test**: A unit test that generates a prompt with both project linter rules and baseline rules, then asserts that (a) the "project standard" group appears before the "baseline quality requirement" group, and (b) the rendered text contains explicit priority/conflict-resolution language in the Core Principles block.

**Acceptance Scenarios**:

1. **Given** the project has both linter rules and applicable baseline rules, **When** the prompt is generated, **Then** the rendered Core Principles block contains explicit wording stating that project standard rules take precedence over baseline quality requirements in case of conflict.
2. **Given** the linter section contains rules with `severity = ERROR` and rules with `severity = WARNING`, **When** the prompt is generated, **Then** the "project standard" principle group's wording distinguishes build-breaking rules (those with `severity = ERROR` or `breaksBuild = true`) from advisory rules, conveying that build-breaking rules are mandatory.
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
5. **Given** `StackInfo.languageLevel` is a non-numeric string (e.g., `"unknown"` or `"17.0.1"`), **When** the generator is called, **Then** all baseline rules appear in the prompt — a non-parseable level is treated as absent.
6. **Given** the stack section is `SectionResult.Empty` (no stack data detected), **When** the generator is called, **Then** all baseline rules appear in the prompt — the full set is emitted without filtering.

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

- What happens when the baseline rule list is empty? → The generator MUST still produce a prompt; the "baseline quality requirement" group is present but notes that no baseline rules are available.
- What happens when `LinterInfo.activeRules` is an empty list (tool configured but no rules active)? → The "project standard" group notes that no active linter rules were found; the group is not omitted.
- What happens when `languageLevel` parses to a value not in `{8, 11, 17, 21}` (e.g., `"9"`, `"16"`)? → Filtering still applies using the numeric comparison — rules whose `minJavaLevel` exceeds the parsed level are excluded. Only unparseable strings trigger the "no filtering" fallback.
- What happens when the same conceptual rule appears in both linter rules and baseline rules? → The generator does not detect semantic duplicates. Both rules appear with their respective origin tags; conflict resolution is left to the `/speckit-constitution` command per the priority hierarchy expressed in the prompt.
- What happens when a `SectionResult.Error` carries a diagnostic cause string? → The generator MAY include the cause string in the corresponding block's "not available" notation to aid diagnostics; it MUST NOT suppress it silently.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The module MUST expose a public entry point — `PromptGenerator` or equivalent — that accepts a `ScanResult` (from `:scan` / `:model`) and a `List<BaselineRule>` (from `:baseline`), and returns a `ConstitutionPrompt` (a structured value type with a method to render to `String`). The generator MUST NOT call any IntelliJ Platform API, perform file I/O, or access clipboard.
- **FR-002**: The generated `ConstitutionPrompt` MUST be a create-oriented prompt addressed to `/speckit-constitution`. It MUST instruct the command to assemble a new project Constitution from the supplied data. It MUST NOT reference, detect, or read any existing Constitution file. Mode detection, semantic-version bumping, and Sync Impact Report are explicitly excluded from this module's scope.
- **FR-003**: The generated prompt MUST present data in exactly six named blocks in this order: (1) Core Principles, (2) Tech Stack, (3) Code Style & Static Analysis, (4) Testing, (5) Project Structure, (6) Governance. All six blocks MUST appear in every generated prompt, regardless of which scan sections contain data.
- **FR-004**: The Core Principles block MUST contain exactly two origin groups, in this order: (1) a "project standard" group derived from `LinterInfo.activeRules`, then (2) a "baseline quality requirement" group derived from the (filtered) baseline rule list. Each group MUST be labeled using exactly the English origin tag: `"project standard"` for linter rules; `"baseline quality requirement"` for baseline rules. Every individual rule emitted in either group MUST be associated with its group's origin tag.
- **FR-005**: The prompt wording MUST express the priority hierarchy: project standard rules (especially those with `severity = ERROR` or `breaksBuild = true`) take precedence over baseline quality requirements; baseline quality requirements take precedence over unwritten team practice. In case of conflict between a project standard rule and a baseline rule, the project standard rule wins. This hierarchy MUST be conveyed through the ordering of the two principle groups and through explicit conflict-resolution language in the Core Principles block.
- **FR-006**: The "project standard" group MUST include all rules from `LinterInfo.activeRules`, regardless of severity. The wording of the group MUST distinguish mandatory (build-breaking) rules from advisory ones: rules where `severity = ERROR` or `breaksBuild = true` are mandatory; all others are advisory.
- **FR-007**: Baseline rules MUST be filtered by the project's Java language level before inclusion. The language level is parsed from `StackInfo.languageLevel` (a nullable `String`). A baseline rule whose `minJavaLevel` exceeds the parsed integer level MUST be excluded. When `StackInfo` is unavailable (section is `Empty` or `Error`), or `languageLevel` is `null`, or `languageLevel` cannot be parsed as an integer, NO filtering is applied — all baseline rules are included.
- **FR-008**: Sections of the generated prompt derived from unavailable scan data (section is `Empty` or `Error`) MUST be marked with explicit "not detected" or "not available" language. The generator MUST NOT fabricate or infer stack versions, framework names, rule sets, or any other project-specific data that was not present in the scanned model.
- **FR-009**: The Governance block MUST be included in every generated prompt with standard /speckit-constitution governance guidance, since no scan section maps to Governance. It does not vary per project and is not derived from any scan section.
- **FR-010**: The module MUST depend only on `:model` (for `ScanResult`, `StackInfo`, `LinterInfo`, etc.) and `:baseline` (for `BaselineRule`). `:scan` is the module that produces `ScanResult` at runtime, but the `:prompt` module depends on the types in `:model`, not on `:scan` itself. Nothing MUST depend on `:prompt`.
- **FR-011**: All pure-logic behaviors of the generator (full model, empty model, language-level filtering at each boundary) MUST be covered by unit tests using JUnit 5. Tests MUST run without IntelliJ Platform fixtures — pure JVM, no `LightPlatformTestCase`, no `BasePlatformTestCase`.

### Key Entities *(include if feature involves data)*

- **PromptGenerator**: The public entry point of the module. Accepts a `ScanResult` and a `List<BaselineRule>`; returns a `ConstitutionPrompt`. Stateless — contains no cached state; safe to call repeatedly with different inputs.
- **ConstitutionPrompt**: The structured output of the generator. Represents the complete prompt as an ordered list of `PromptBlock` values. Provides a method to render itself to a `String`. Immutable after construction.
- **PromptBlock**: A named section of the generated prompt (Core Principles, Tech Stack, etc.). Contains the block heading and its rendered content. Blocks appear in the canonical order defined by FR-003.
- **OriginGroup**: Within the Core Principles block, a named grouping of rules sharing the same origin tag. Carries: origin label (`"project standard"` or `"baseline quality requirement"`), list of formatted rules, and any notations for the empty-rules case.
- **ScanResult** (from `:model`): The root aggregate of all five scanned sections. Each section is `SectionResult.Ok<T>`, `SectionResult.Empty`, or `SectionResult.Error`. The generator reads this value; it never writes to it.
- **BaselineRule** (from `:baseline`): A single baseline quality rule. The generator reads `id`, `obligation`, `statement`, `rationale`, `minJavaLevel`; it uses `minJavaLevel` for filtering and `obligation` for rendered wording (`MUST` / `SHOULD`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For every valid combination of `ScanResult` and `List<BaselineRule>` (including all-empty sections and empty baseline list), the generator completes without throwing an exception and returns a non-empty `ConstitutionPrompt`.
- **SC-002**: 100% of emitted rules in the generated prompt carry an origin tag — verified by a unit test that parses the rendered output and confirms no rule-like line lacks an origin association.
- **SC-003**: The "project standard" principle group always precedes the "baseline quality requirement" group in the rendered Core Principles block — verified by asserting the character-offset order of the two groups in the rendered string.
- **SC-004**: When `languageLevel = "11"`, baseline rules with `minJavaLevel > 11` are absent from the generated prompt; when `languageLevel` is null or unparseable, all baseline rules are present — verified by dedicated unit tests for each boundary case.
- **SC-005**: When all five scan sections are `SectionResult.Empty`, the generated prompt contains baseline-sourced Core Principles content and explicit "not detected" markers in all scan-dependent blocks — verified by unit test inspection of the rendered string.
- **SC-006**: The `:prompt` module's build configuration lists only `:model` and `:baseline` as compile-time dependencies. No IntelliJ Platform SDK classes appear in the module's test classpath.
- **SC-007**: All generator unit tests are pure JVM and produce deterministic output — the same input always yields the same rendered string (no timestamps, no random IDs in the output).

## Assumptions

- `StackInfo.languageLevel` is either `null` or a string representation of a Java major version number (e.g., `"8"`, `"11"`, `"17"`, `"21"`). Non-numeric strings (e.g., `"17.0.1"`, `"unknown"`) are treated as absent for filtering purposes.
- All rules from `LinterInfo.activeRules` are included in the "project standard" principle group regardless of severity. Severity and `breaksBuild` influence the wording within the group but do not gate a rule's inclusion.
- The Governance block content is a fixed, standard text (not derived from scan data), since no scan section maps to Governance concepts. Its wording is an implementation choice.
- The `:scan` module's `ScanResult` type (defined in `:model`) is the input contract. The generator does not call any scanner or IntelliJ API — it only reads the already-populated `ScanResult`.
- The `ConstitutionPrompt.render()` output is deterministic for a given input — no runtime-variable content (timestamps, UUIDs) appears in the rendered string.
- The `:prompt` module is the sole module that performs language-level filtering and origin-tagging of rules; `:baseline` ships rules with no applied filtering, as per the `:baseline` Sprint 3 contract (FR-012 of `:baseline`).

## Out of Scope

- **Writing the prompt to disk, clipboard, or any external system** — belongs to the UI module (Sprint 5).
- **Detecting or reading an existing `.specify/memory/constitution.md`** — the generator is mode-agnostic; mode detection belongs to the `/speckit-constitution` command.
- **Semantic-version bumping or Sync Impact Report** — belongs to the `/speckit-constitution` command protocol.
- **Any UI interaction** (dialogs, progress indicators, IntelliJ notifications) — belongs to Sprint 5.
- **Ranking or sorting rules within an origin group** — the generator preserves the order in which rules are provided; ranking is not in scope.
- **Filtering by language** (e.g., excluding `JAVA`-only rules for a Kotlin project) — only Java language-level filtering is in scope; language-type filtering is a future concern.
- **Detecting semantic conflicts between project linter rules and baseline rules** — the generator expresses the priority hierarchy in the prompt text; actual conflict resolution is left to `/speckit-constitution`.
- **Calling `BaselineRuleProvider` directly** — the generator accepts an already-loaded `List<BaselineRule>` as input; loading baseline rules is the caller's responsibility.
