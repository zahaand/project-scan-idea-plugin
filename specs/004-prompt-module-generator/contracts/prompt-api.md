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
  `### project standard`; omitted when the sub-section would contain zero rules
- `-` (bullet) — individual rules within their enclosing section

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
