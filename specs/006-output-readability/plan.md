# Implementation Plan: Output Readability for Large Projects

**Branch**: `006-output-readability` | **Date**: 2026-06-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/006-output-readability/spec.md`

## Summary

Improve readability of the Tech Stack, Project Structure, and Testing sections produced by `PromptGenerator` (`:prompt`) and `ScanResultRenderer` (root `:ui`) for large monorepos. Shared data-transformation utilities are extracted to `:model`; both consumers call the same transformations and render the result in identical text format.

## Technical Context

**Language/Version**: Kotlin 2.2 / JDK 21 (JetBrains Runtime)  
**Primary Dependencies**: IntelliJ Platform Gradle Plugin 2.x; JUnit 5 (testing)  
**Storage**: N/A — in-memory `ScanResult` data only  
**Testing**: JUnit 5 (`org.junit.jupiter`); no IntelliJ platform test framework needed (pure logic)  
**Target Platform**: IntelliJ IDEA 2025.3.5 plugin  
**Project Type**: IntelliJ IDEA plugin (Kotlin multi-module Gradle project)  
**Performance Goals**: Rendering is synchronous and sub-millisecond; no throughput target  
**Constraints**: All changes limited to `:model` (new files only), `:prompt`, and root project (`:ui`). `:scan` is untouched per FR-010.  
**Scale/Scope**: Support monorepos with 80+ modules and 250+ dependencies without performance concerns

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Decoupling of Collection and Generation | ✅ PASS | Shared utility goes into `:model` (zero outgoing deps); `:scan` untouched. |
| II. Read Through IntelliJ Project Model Only | ✅ PASS | No build-file parsing; working only from existing `ScanResult` model objects. |
| III. Never Fabricate | ✅ PASS | FR-011 preserves `Empty`/`Error` handling; no data is invented. |
| IV. Curated Baseline Rule Set | ✅ PASS | No changes to baseline or rule-emission paths. |
| Constitution §Testing | ✅ PASS | `:prompt` pure logic → full JUnit 5 coverage. `:ui` rendering → one smoke test per changed section (per spec agreement SC-005). |
| Constitution §Project Structure (dep direction) | ⚠️ NOTE | Root project `:ui` already imports `ConstitutionPrompt` from `:prompt` (pre-existing). Shared utility placed in `:model` (no outgoing deps) avoids adding a new `:ui` → `:prompt` for rendering logic. |
| FR-010 interpretation | ✅ RESOLVED | FR-010 says "`:scan` and `:model` MUST NOT be modified." The spec's Clarifications section (same date) says "Extract to `:model` (preferred)." Resolution: FR-010's intent is to protect the *data model classes* (no field changes to `StackInfo`, `Module`, etc.). Adding new utility files to `:model` is permitted per the explicit clarification. Existing model files remain unchanged. |

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
model/
└── src/main/kotlin/dev/zahaand/projectscan/model/
    ├── StackInfo.kt              (unchanged)
    ├── StructureInfo.kt          (unchanged)
    ├── TestInfo.kt               (unchanged)
    └── OutputFormatters.kt       ← NEW: pure data-transformation utilities

prompt/
└── src/
    ├── main/kotlin/dev/zahaand/projectscan/prompt/
    │   └── PromptGenerator.kt    (modified: buildTechStackBlock, buildProjectStructureBlock, buildTestingBlock)
    └── test/kotlin/dev/zahaand/projectscan/prompt/
        ├── PromptGeneratorFullModelTest.kt        (updated: scenario 5 assertions for new format)
        ├── PromptGeneratorEmptyModelTest.kt       (unchanged)
        ├── PromptGeneratorLanguageLevelFilterTest.kt (unchanged)
        ├── PromptGeneratorPriorityHierarchyTest.kt   (unchanged)
        └── PromptGeneratorOutputReadabilityTest.kt   ← NEW: FR-001 through FR-009 scenarios

src/
└── main/kotlin/dev/zahaand/projectscan/ui/
    └── ScanResultRenderer.kt    (modified: renderStack, renderStructure, renderTests)

# No test directory exists yet for the root project's ui code;
# smoke tests for ScanResultRenderer will be added alongside ScanResultRenderer.kt
# under src/test/kotlin/dev/zahaand/projectscan/ui/ once confirmed buildable.
```

**Structure Decision**: Single root project (`:ui`) + four Gradle submodules. Changes span `:model` (new file), `:prompt` (modified + new test), root project (modified + new smoke test). No new submodule required.

## Complexity Tracking

> No constitution violations requiring justification.

---

# Phase 0: Research

See [research.md](research.md).

# Phase 1: Design

See [data-model.md](data-model.md) and [contracts/](contracts/).
