# Implementation Plan: Output Readability for Large Projects

**Branch**: `006-output-readability` | **Date**: 2026-06-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/006-output-readability/spec.md`

## Summary

Improve readability of the Tech Stack, Project Structure, and Testing sections produced by `PromptGenerator` (`:prompt`) and `ScanResultRenderer` (root `:ui`) for large monorepos. Shared data-transformation utilities are extracted to a new `:shared` module (depends only on `:model`); both consumers call the same transformations and produce byte-identical section text.

## Technical Context

**Language/Version**: Kotlin 2.2 / JDK 21 (JetBrains Runtime)  
**Primary Dependencies**: IntelliJ Platform Gradle Plugin 2.x; JUnit 5 (testing)  
**Storage**: N/A — in-memory `ScanResult` data only  
**Testing**: JUnit 5 (`org.junit.jupiter`); no IntelliJ platform test framework needed (pure logic)  
**Target Platform**: IntelliJ IDEA 2025.3.5 plugin  
**Project Type**: IntelliJ IDEA plugin (Kotlin multi-module Gradle project)  
**Performance Goals**: Rendering is synchronous and sub-millisecond; no throughput target  
**Constraints**: All changes limited to new `:shared` module, `:prompt`, and root project (`:ui`). `:scan` and `:model` are untouched per FR-010.  
**Scale/Scope**: Support monorepos with 80+ modules and 250+ dependencies without performance concerns

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Decoupling of Collection and Generation | ✅ PASS | Shared utility in new `:shared` module (depends only on `:model`, zero outgoing deps to scan/prompt/ui); `:scan` untouched. |
| II. Read Through IntelliJ Project Model Only | ✅ PASS | No build-file parsing; working only from existing `ScanResult` model objects. |
| III. Never Fabricate | ✅ PASS | FR-011 preserves `Empty`/`Error` handling; no data is invented. |
| IV. Curated Baseline Rule Set | ✅ PASS | No changes to baseline or rule-emission paths. |
| Constitution §Testing | ✅ PASS | `:prompt` pure logic → full JUnit 5 coverage. `:ui` rendering → one smoke test per changed section (per spec agreement SC-005). |
| Constitution §Project Structure (dep direction) | ✅ PASS | `:shared` depends only on `:model`; `:prompt` and root project each depend on `:shared` + `:model`. No new cross-consumer dependency introduced. |
| FR-010 compliance | ✅ PASS | FR-010 amended 2026-06-27: `:shared` is a new additive module; `:model` and `:scan` files are not modified. |

## Project Structure

### Documentation (this feature)

```text
specs/006-output-readability/
├── plan.md              ← this file
├── research.md          ← Phase 0 output
├── data-model.md        ← Phase 1 output
├── contracts/           ← Phase 1 output
└── tasks.md             ← Phase 2 output (/speckit-tasks)
```

### Source Code Layout

```text
shared/                           ← NEW Gradle submodule (:shared)
├── build.gradle.kts
└── src/
    ├── main/kotlin/dev/zahaand/projectscan/shared/
    │   └── OutputFormatters.kt   ← NEW: pure data-transformation utilities + result types
    └── test/kotlin/dev/zahaand/projectscan/shared/
        └── OutputFormattersTest.kt ← NEW: unit tests for all four utility functions

model/
└── src/main/kotlin/dev/zahaand/projectscan/model/
    ├── StackInfo.kt              (unchanged)
    ├── StructureInfo.kt          (unchanged)
    └── TestInfo.kt               (unchanged)

prompt/
└── src/
    ├── main/kotlin/dev/zahaand/projectscan/prompt/
    │   └── PromptGenerator.kt    (modified: buildTechStackBlock, buildProjectStructureBlock, buildTestingBlock)
    └── test/kotlin/dev/zahaand/projectscan/prompt/
        ├── PromptGeneratorFullModelTest.kt        (updated: scenario 5 assertions for new format)
        ├── PromptGeneratorEmptyModelTest.kt       (unchanged)
        ├── PromptGeneratorLanguageLevelFilterTest.kt (unchanged)
        ├── PromptGeneratorPriorityHierarchyTest.kt   (unchanged)
        └── PromptGeneratorOutputReadabilityTest.kt   ← NEW: FR-001 through FR-014 scenarios

src/
└── main/kotlin/dev/zahaand/projectscan/ui/
    └── ScanResultRenderer.kt    (modified: renderStack, renderStructure, renderTests)

# Smoke tests for ScanResultRenderer under:
# src/test/kotlin/dev/zahaand/projectscan/ui/ScanResultRendererSmokeTest.kt
```

**Structure Decision**: Single root project (`:ui`) + four existing Gradle submodules + one new `:shared` submodule. Changes span `:shared` (new module + new file + unit tests), `:prompt` (modified + new test), root project (modified + new smoke test). `:model` and `:scan` files are unchanged.

## Complexity Tracking

> No constitution violations requiring justification.

---

# Phase 0: Research

See [research.md](research.md).

# Phase 1: Design

See [data-model.md](data-model.md) and [contracts/](contracts/).
