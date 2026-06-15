# Research: Prompt — Constitution Prompt Generator Module

**Feature**: `004-prompt-module-generator` | **Date**: 2026-06-15

No NEEDS CLARIFICATION items were identified in the Technical Context. All types are verified
from the current codebase. Findings are recorded below for traceability.

---

## Finding 1 — ScanResult sealed class structure

**Decision**: Use Kotlin `when` expression pattern-matching on `SectionResult<T>` variants.

**Rationale**: `SectionResult` is a sealed class (`Ok<T>`, `Empty`, `Error`) defined in
`model/src/main/kotlin/dev/zahaand/projectscan/model/ScanResult.kt`. The compiler enforces
exhaustive `when` coverage at compile time, eliminating missed-case bugs.

**Key detail**: `SectionResult.Error` carries an optional `cause: String?`. Per FR-008 and the
edge-case spec, the generator MAY include `cause` in the "not available" notation and MUST NOT
suppress it silently.

---

## Finding 2 — Language-level extraction

**Decision**: Extract the leading decimal integer from `StackInfo.languageLevel: String?` using
`trimStart().takeWhile { it.isDigit() }.toIntOrNull()`.

**Rationale**: No NEEDS CLARIFICATION — the spec fully defines the algorithm in FR-007 and US3:
- `"11"` → 11, `"17.0.1"` → 17, `"21_PREVIEW"` → 21, `"9"` → 9
- `null`, empty, or no leading digit → no filtering (full baseline emitted)

**Alternatives considered**: `Regex("^\\s*(\\d+)")` — equivalent; `takeWhile` is more idiomatic
in Kotlin and avoids a compiled regex allocation per call.

---

## Finding 3 — breaksBuild null treatment

**Decision**: A rule with `breaksBuild = null` is treated as **advisory** (placed under
`#### Advisory`). The generator MUST NOT throw or infer mandatory from a null flag.

**Rationale**: FR-006 explicitly states: "A rule with `breaksBuild = null` (undetermined
build-breaking status — the Gradle path may not expose this flag) MUST be treated as NOT
mandatory." This avoids incorrect mandatory classification when the scanner cannot determine
build-breaking status.

---

## Finding 4 — No IntelliJ Platform dependency in :model or :baseline

**Decision**: `:prompt` can safely declare compile-time dependencies on `:model` and `:baseline`
without pulling in the IntelliJ Platform SDK.

**Rationale**: Verified from current build files:
- `model/build.gradle.kts` — depends on `kotlin("stdlib")` + JUnit 5 only
- `baseline/build.gradle.kts` — depends on `kotlin("stdlib")` + kotlinx-serialization + JUnit 5

This satisfies SC-006 inherently: no platform SDK can appear in `:prompt`'s test classpath via
transitive dependency.

---

## Finding 5 — ProjectScanModel vs ScanResult

**Decision**: The generator accepts `ScanResult`, not `ProjectScanModel`.

**Rationale**: `ScanResult` (section-aware with Ok/Empty/Error variants) is the input contract
per FR-001. `ProjectScanModel` is a separate, flat data holder without section status — it does
not carry the empty/error state needed for FR-008.

---

## Finding 6 — Governance block content

**Decision**: The Governance block is a fixed, standard text not derived from scan data.

**Rationale**: FR-009 confirms this. No scan section maps to Governance. The exact wording is an
implementation choice; it should instruct `/speckit-constitution` on standard governance
conventions (versioning, changelog format, compliance notes).

---

## Finding 7 — Rendering Markdown structure

**Decision**: `ConstitutionPrompt.render()` produces:
- `##` — six block headings (Core Principles, Tech Stack, Code Style & Static Analysis, Testing,
  Project Structure, Governance)
- `###` — two origin groups within Core Principles (`project standard`,
  `baseline quality requirement`)
- `####` — mandatory/advisory sub-sections within `### project standard`
  (`Mandatory (build-breaking)`, `Advisory`) — omitted when empty
- `-` — bullet items for individual rules

**Rationale**: FR-012 defines this as the contractual output surface. It is verified by all tests
that assert on rendered text.

---

## Summary: No open unknowns

All NEEDS CLARIFICATION items were resolved through codebase inspection. Phase 1 design can
proceed immediately.
