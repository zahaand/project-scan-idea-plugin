# Public API Contract: :prompt Module

**Module**: `dev.zahaand.projectscan.prompt`
**Version**: Sprint 4 (2026-06-15)

This document defines the stable, externally visible interface of the `:prompt` Gradle submodule.
Only the types and methods listed here are considered public API. Internal helpers may change
freely.

---

## Entry Point

### PromptGenerator

```kotlin
package dev.zahaand.projectscan.prompt

class PromptGenerator {
    fun generate(
        scanResult: ScanResult,
        baselineRules: List<BaselineRule>,
    ): ConstitutionPrompt
}
```

**Contract**:
- Stateless — safe to reuse across multiple calls.
- Returns a non-null `ConstitutionPrompt` for any valid combination of inputs (including all
  `SectionResult.Empty` sections and an empty `baselineRules` list).
- Never throws a checked or unchecked exception for well-formed inputs.
- No side effects: no file I/O, no IntelliJ Platform API, no clipboard access.

**Inputs**:

| Parameter | Type | Source |
|---|---|---|
| `scanResult` | `ScanResult` (`:model`) | Produced by `:scan` at runtime; passed by the caller |
| `baselineRules` | `List<BaselineRule>` (`:baseline`) | Loaded by caller via `BaselineRuleProvider` |

---

## Output Type

### ConstitutionPrompt

```kotlin
package dev.zahaand.projectscan.prompt

class ConstitutionPrompt(val blocks: List<PromptBlock>) {
    fun render(): String
}
```

**Contract**:
- `blocks` always contains exactly **six** `PromptBlock` entries in this canonical order:
  1. `Core Principles`
  2. `Tech Stack`
  3. `Code Style & Static Analysis`
  4. `Testing`
  5. `Project Structure`
  6. `Governance`
- `render()` returns a non-empty, deterministic Markdown string.
- `render()` output contains no runtime-variable content (no timestamps, UUIDs, random values).

---

## Supporting Types (public, read-only)

### PromptBlock

```kotlin
package dev.zahaand.projectscan.prompt

data class PromptBlock(
    val heading: String,
    val content: String,
)
```

### OriginGroup

```kotlin
package dev.zahaand.projectscan.prompt

data class OriginGroup(
    val label: String,
    val mandatoryRules: List<String>,
    val advisoryRules: List<String>,
    val emptyNotation: String?,
)
```

Used internally by `PromptGenerator` to compose the Core Principles block. Exposed as a public
type to allow test assertions on the intermediate structure without parsing Markdown.

For the `"baseline quality requirement"` group, baseline rules are placed into `advisoryRules`
and `mandatoryRules` is always empty; the mandatory/advisory split applies only to the
`"project standard"` group.

---

## Rendered Markdown Structure

`ConstitutionPrompt.render()` produces Markdown with this heading hierarchy:

```markdown
## Core Principles
### project standard
#### Mandatory (build-breaking)
- <rule>
#### Advisory
- <rule>
### baseline quality requirement
- <rule>

## Tech Stack
...

## Code Style & Static Analysis
...

## Testing
...

## Project Structure
...

## Governance
...
```

**Heading level rules** (contractual — verified by unit tests):
- `##` (level 2) — six block names
- `###` (level 3) — two origin groups within Core Principles only
- `####` (level 4) — `Mandatory (build-breaking)` and `Advisory` sub-sections within
  `### project standard` only; omitted when the sub-section would contain zero rules
- `-` (bullet) — individual rules within their enclosing section
- `### baseline quality requirement` is ALWAYS a flat `-` bullet list — it NEVER uses `####`
  sub-sections

**Separator conventions** (contractual — required for deterministic string assertions):
- Exactly one blank line between each `##` block
- Exactly one blank line between each `###` group within Core Principles
- No separator between `####` sub-section heading and its bullet items

---

## Filtering and Classification Contracts

### Language-Level Filtering

Baseline rules are filtered before inclusion using the leading decimal integer from
`StackInfo.languageLevel`:

| `languageLevel` value | Extracted integer | Effect |
|---|---|---|
| `"8"` | 8 | Excludes rules with `minJavaLevel > 8` |
| `"11"` | 11 | Excludes rules with `minJavaLevel > 11` |
| `"17.0.1"` | 17 | Excludes rules with `minJavaLevel > 17` |
| `"21_PREVIEW"` | 21 | Excludes rules with `minJavaLevel > 21` |
| `null` | — | No filtering; full baseline emitted |
| `"unknown"` | — | No filtering; no leading digit |
| Stack section `Empty`/`Error` | — | No filtering; full baseline emitted |

### Mandatory vs Advisory Classification

A rule from `LinterInfo.activeRules` is **mandatory** if:
- `severity == RuleSeverity.ERROR`, OR
- `breaksBuild == true`

All other rules (including `breaksBuild == null`) are **advisory**.

### Obligation-to-Rendered-Text Mapping

Each `BaselineRule` bullet in the `### baseline quality requirement` group MUST include an
explicit leading obligation marker derived from `BaselineRule.obligation`:

| `Obligation` value | Rendered marker |
|---|---|
| `MUST` | `MUST` |
| `SHOULD` | `SHOULD` |

The marker MUST be deterministically present in every baseline rule bullet. Exact surrounding
format (e.g., whether the marker is bold, bracketed, or plain) is an implementation choice.

### Unavailable-Data Markers

Sections derived from unavailable scan data MUST use these exact fixed phrases:

| `SectionResult` variant | Required marker |
|---|---|
| `SectionResult.Empty` | `not detected` |
| `SectionResult.Error` (cause is non-null) | `not available (cause: <cause>)` |
| `SectionResult.Error` (cause is null) | `not available` |

No other phrasing is permitted. `(cause: null)` MUST NEVER appear.

### Empty-Group Notations

When either origin group in Core Principles has no rules, the group heading is still emitted
and a notation line replaces the bullet list:

| Condition | Required rendering |
|---|---|
| `LinterInfo.activeRules` is empty | `### project standard` present; notation line states no active linter rules were detected; NO `####` headings |
| `baselineRules` list is empty | `### baseline quality requirement` present; notation line states no baseline rules are available |

Both groups follow the same structural pattern: group NOT omitted, notation line MUST be present.

---

## Governance Block Requirements

The Governance block content does not vary per project and is not derived from scan data.
Regardless of exact phrasing (an implementation choice), the block MUST contain all three of
the following elements — verifiable by their presence, not by exact text match:

1. **Semantic-versioning policy** — when MAJOR, MINOR, and PATCH version bumps apply to the
   Constitution file
2. **Changelog convention** — how changes to the Constitution are recorded
3. **Amendment and compliance procedure** — how the Constitution is updated and by whom

---

## Module Dependency Contract (FR-010 / SC-006)

The `:prompt` module declares only these compile-time dependencies:
- `project(":model")` — provides `ScanResult` and all data types
- `project(":baseline")` — provides `BaselineRule` and `Obligation`

**Prohibited**:
- Any direct or transitive dependency on `com.intellij.*`
- Any dependency on `:scan` or `:ui`

---

## Out of Scope

The following are explicitly NOT part of this API (see spec Out of Scope):

- Writing prompts to disk, clipboard, or any external system
- Detecting or reading an existing `.specify/memory/constitution.md`
- Semantic-version bumping or Sync Impact Report generation
- Any UI interaction (dialogs, notifications)
- Ranking or sorting rules within an origin group
- Filtering by language type (e.g., Kotlin vs Java — only `minJavaLevel` filtering is in scope)
- Detecting semantic conflicts between project and baseline rules
