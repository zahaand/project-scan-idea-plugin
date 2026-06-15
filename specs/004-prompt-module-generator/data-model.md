# Data Model: Prompt — Constitution Prompt Generator Module

**Feature**: `004-prompt-module-generator` | **Date**: 2026-06-15

---

## Entities

### PromptGenerator

**Package**: `dev.zahaand.projectscan.prompt`
**File**: `PromptGenerator.kt`

Public entry point of the `:prompt` module. Stateless — safe to call repeatedly with different
inputs.

| Field/Method | Type | Notes |
|---|---|---|
| `generate(scanResult, baselineRules)` | `ConstitutionPrompt` | Primary operation; no side effects |

**Inputs**:
- `scanResult: ScanResult` — from `:model`; carries five sections (stack, codeStyle, linters,
  tests, structure), each as `SectionResult.Ok<T>`, `SectionResult.Empty`, or
  `SectionResult.Error`
- `baselineRules: List<BaselineRule>` — from `:baseline`; already-loaded list (loading is the
  caller's responsibility)

**Invariants**:
- Never calls IntelliJ Platform API
- Never performs file I/O or clipboard access
- Same inputs always produce the same output (deterministic)

---

### ConstitutionPrompt

**Package**: `dev.zahaand.projectscan.prompt`
**File**: `ConstitutionPrompt.kt`

The structured output of the generator. Immutable after construction.

| Field/Method | Type | Notes |
|---|---|---|
| `blocks` | `List<PromptBlock>` | Six blocks in canonical order (FR-003) |
| `render()` | `String` | Produces the full Markdown string |

**Invariants**:
- `blocks` always contains exactly six entries in this order: Core Principles, Tech Stack,
  Code Style & Static Analysis, Testing, Project Structure, Governance
- `render()` output is deterministic — no timestamps, UUIDs, or random content

---

### PromptBlock

**Package**: `dev.zahaand.projectscan.prompt`
**File**: `PromptBlock.kt`

A named section of the generated prompt. Rendered as a `##` Markdown heading followed by content.

| Field | Type | Notes |
|---|---|---|
| `heading` | `String` | Exact heading text (e.g., `"Core Principles"`) |
| `content` | `String` | Rendered Markdown content for this block |

**Block headings** (canonical order):
1. `Core Principles`
2. `Tech Stack`
3. `Code Style & Static Analysis`
4. `Testing`
5. `Project Structure`
6. `Governance`

---

### OriginGroup

**Package**: `dev.zahaand.projectscan.prompt`
**File**: `OriginGroup.kt`

A named grouping of rules sharing the same origin tag, used within the Core Principles block.
Rendered as a `###` Markdown heading.

The `label` field holds the rule's **origin tag** — the canonical string that identifies which source
a group of rules came from. The two valid origin tags are `"project standard"` (for linter rules)
and `"baseline quality requirement"` (for baseline rules). These match the values required by
FR-004, SC-002, and Constitution Principle IV.

| Field | Type | Notes |
|---|---|---|
| `label` | `String` | The origin tag: exactly `"project standard"` or `"baseline quality requirement"` |
| `mandatoryRules` | `List<String>` | Formatted rules for `#### Mandatory (build-breaking)` sub-section; empty list → sub-section omitted |
| `advisoryRules` | `List<String>` | Formatted rules for `#### Advisory` sub-section; empty list → sub-section omitted |
| `emptyNotation` | `String?` | Present only when `mandatoryRules` and `advisoryRules` are both empty; describes that no rules were found |

**Applies to**: `"project standard"` group only uses `mandatoryRules`/`advisoryRules` split.
The `"baseline quality requirement"` group renders all rules as a flat `-` bullet list (no
mandatory/advisory split — obligation level is expressed in the rule wording via `MUST`/`SHOULD`).
For the `"baseline quality requirement"` group, baseline rules are placed into `advisoryRules`
and `mandatoryRules` is always empty; the mandatory/advisory split applies only to the
`"project standard"` group.

---

## Consumed Types from :model

These types are defined in `:model` and consumed read-only by the generator.

| Type | Key Fields Used |
|---|---|
| `ScanResult` | `stack`, `codeStyle`, `linters`, `tests`, `structure` |
| `SectionResult<T>` | sealed variants: `Ok(data)`, `Empty`, `Error(cause?)` |
| `StackInfo` | `languageLevel: String?`, `jdkVersion: String?`, `buildSystem: BuildSystem?`, `dependencies: List<Dependency>` |
| `LinterInfo` | `activeRules: List<ActiveRule>`, `toolsWithUnresolvableConfig: List<String>` |
| `ActiveRule` | `ruleId: String`, `tool: String`, `severity: RuleSeverity`, `breaksBuild: Boolean?` |
| `RuleSeverity` | enum: `ERROR`, `WARNING`, `INFO` |
| `CodeStyleInfo` | `sources: List<StyleSource>` |
| `StyleSource` | `type: StyleSourceType`, `path: String` |
| `TestInfo` | `frameworks: List<TestFramework>`, `sourceRoots: List<String>`, `namingSuffixes: List<String>`, `coverageThreshold: Double?` |
| `TestFramework` | `name: String`, `version: String?` |
| `StructureInfo` | `modules: List<Module>`, `rootPackages: List<String>` |
| `Module` | `name: String`, `declaredDependencies: List<Dependency>`, `moduleDependencies: List<String>` |
| `Dependency` | `groupId: String`, `artifactId: String`, `resolvedVersion: String?` |
| `BuildSystem` | enum: `MAVEN`, `GRADLE` |

---

## Consumed Types from :baseline

| Type | Key Fields Used |
|---|---|
| `BaselineRule` | `id: String`, `obligation: Obligation`, `statement: String`, `rationale: String`, `minJavaLevel: Int` |
| `Obligation` | enum: `MUST`, `SHOULD` |

---

## Language-Level Filtering Algorithm

Used by the generator to determine which baseline rules to include.

```
fun extractLanguageLevel(languageLevel: String?): Int?
    if languageLevel == null → return null
    val digits = languageLevel.trimStart().takeWhile { it.isDigit() }
    if digits.isEmpty() → return null
    return digits.toInt()

fun isBaselineRuleApplicable(rule: BaselineRule, extractedLevel: Int?): Boolean
    if extractedLevel == null → return true   (no filtering)
    return rule.minJavaLevel <= extractedLevel
```

**Inputs leading to no filtering** (full baseline emitted):
- `languageLevel = null`
- `languageLevel = ""` (empty string)
- `languageLevel = "unknown"` (no leading digit)
- Stack section is `SectionResult.Empty` or `SectionResult.Error`

---

## Mandatory vs Advisory Classification (project standard group)

```
fun isMandatory(rule: ActiveRule): Boolean =
    rule.severity == RuleSeverity.ERROR || rule.breaksBuild == true
```

- `breaksBuild = null` → advisory (not mandatory); no exception thrown
- `breaksBuild = false` → advisory
- `severity = ERROR` → mandatory regardless of `breaksBuild`

---

## State Transitions

The generator is stateless — there are no state transitions. A `ScanResult` is processed in a
single pass to produce a `ConstitutionPrompt`.

---

## Validation Rules

| Rule | Enforcement |
|---|---|
| Six blocks always present | Generator always constructs all six blocks |
| Core Principles always has two origin groups | Both groups rendered even when rules list is empty |
| No fabricated data | Only data from `ScanResult` fields is used; sections with `Empty`/`Error` get "not detected" markers |
| Deterministic output | No runtime-variable content (timestamps, UUIDs) in render output |
