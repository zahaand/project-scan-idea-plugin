# Implementation Plan: Prompt — Constitution Prompt Generator Module

**Branch**: `004-prompt-module-generator` | **Date**: 2026-06-15 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/004-prompt-module-generator/spec.md`

## Summary

Build the `:prompt` Gradle submodule — a pure-JVM Kotlin library that transforms a `ScanResult`
(from `:model`) and a `List<BaselineRule>` (from `:baseline`) into a `ConstitutionPrompt`, which
renders to a Markdown string addressed to `/speckit-constitution`. The module has no IntelliJ
Platform dependencies and is fully covered by JUnit 5 unit tests.

## Technical Context

**Language/Version**: Kotlin 2.2.20, JVM 21
**Primary Dependencies**: `:model` (`ScanResult`, `StackInfo`, `LinterInfo`, …); `:baseline` (`BaselineRule`)
**Storage**: N/A — pure in-memory transformation
**Testing**: JUnit 5 (`junit.jupiter`), pure JVM, no IntelliJ Platform fixtures
**Target Platform**: JVM library (no IntelliJ Platform SDK in compile or test classpath)
**Project Type**: Gradle submodule within a multi-project build
**Performance Expectation** *(informational — not a verifiable requirement; no SC)*: Synchronous, expected <10 ms per call (no I/O, prompt is small)
**Constraints**: No IntelliJ Platform API; no file I/O; no clipboard access
**Scale/Scope**: ~4 production source files; ~4 test files aligned to the four User Stories

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Evidence |
|------|--------|----------|
| I. Decoupling — `:prompt` MUST NOT depend on `:scan` or `:ui` | ✅ PASS | FR-010 restricts compile deps to `:model` + `:baseline` only |
| II. Read through IntelliJ Project Model only | ✅ N/A | `:prompt` reads an already-populated `ScanResult`; no IDE API called |
| III. Never Fabricate — empty sections marked "not detected" | ✅ PASS | FR-008 mandates explicit "not detected" / "not available" markers |
| IV. Curated Baseline Rule Set always applied | ✅ PASS | Baseline included unconditionally; language-level filtering is the only exclusion gate |
| Tech Stack — Kotlin, Gradle; no platform SDK in test classpath | ✅ PASS | `:prompt` build mirrors `:baseline` (stdlib + JUnit 5) |
| Testing — JUnit 5 for pure logic | ✅ PASS | FR-011 and SC-006 mandate pure-JVM tests |
| Dependency direction enforced | ✅ PASS | `prompt` → `model` + `baseline`; nothing depends on `prompt` |

No constitution violations. No Complexity Tracking entries required.

## Project Structure

### Documentation (this feature)

```text
specs/004-prompt-module-generator/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── prompt-api.md    # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks — NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
prompt/
├── build.gradle.kts
└── src/
    ├── main/kotlin/dev/zahaand/projectscan/prompt/
    │   ├── PromptGenerator.kt            # Public entry point — stateless
    │   ├── ConstitutionPrompt.kt         # Output value type with render(): String
    │   ├── PromptBlock.kt                # Named section (one of six blocks)
    │   └── OriginGroup.kt                # Rule group within Core Principles; label holds the origin tag
    └── test/kotlin/dev/zahaand/projectscan/prompt/
        ├── PromptGeneratorFullModelTest.kt           # US1 — complete prompt from full scan
        ├── PromptGeneratorPriorityHierarchyTest.kt   # US2 — ordering and sub-headings
        ├── PromptGeneratorLanguageLevelFilterTest.kt # US3 — language-level filtering
        └── PromptGeneratorEmptyModelTest.kt          # US4 — empty project / baseline-only

settings.gradle.kts  # add include(":prompt")
```

**Structure Decision**: Single Gradle submodule under `prompt/`, mirroring the `:baseline` build
pattern (stdlib + JUnit 5, no serialization, no IntelliJ Platform). Four production files, four
test files — one per User Story.
