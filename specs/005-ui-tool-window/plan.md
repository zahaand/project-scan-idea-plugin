# Implementation Plan: UI Tool Window

**Branch**: `005-ui-tool-window` | **Date**: 2026-06-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/005-ui-tool-window/spec.md`

## Summary

Implement the `:ui` module — an IntelliJ Platform Tool Window that assembles the four lower modules (`:scan`, `:baseline`, `:prompt`, `:model`) into a complete user-facing plugin. The panel runs a background scan on button click and renders six collapsible sections displaying the collected data and the generated constitution prompt. Demo scaffold files (`MyToolWindowFactory`, `MyMessageBundle`, demo `.properties`) are deleted and replaced with project-owned equivalents under `dev.zahaand.projectscan.ui`.

## Technical Context

**Language/Version**: Kotlin 2.2.20, JDK 21 (JetBrains Runtime)
**Primary Dependencies**: IntelliJ Platform SDK (IDEA 2025.3.5), `Task.Backgroundable`, `DynamicBundle`, submodules `:scan`, `:baseline`, `:prompt`, `:model`
**Storage**: N/A — session-only in-memory state; no persistent storage
**Testing**: Manual verification via IDE run (no mandatory unit coverage for `:ui` per spec Assumptions)
**Target Platform**: IntelliJ IDEA 2025.3.5+ desktop IDE plugin
**Project Type**: IntelliJ Platform Plugin
**Performance Goals**: No artificial delays; background scan via `Task.Backgroundable`; full IDE responsiveness throughout
**Constraints**: No EDT blocking; no Kotlin coroutines; all strings from message bundle; no hardcoded strings
**Scale/Scope**: Single tool window; single scan-button trigger; six sections; session-only state

## Constitution Check

| Gate | Requirement | Status |
|------|-------------|--------|
| Principle I — Decoupling | `:ui` depends on `:model`, `:scan`, `:baseline`, `:prompt`; `:scan` never depends on `:prompt` or `:ui` | ✅ |
| Principle II — IntelliJ Project Model Only | `:ui` reads nothing from project files; all reading delegated to `:scan` adapters | ✅ |
| Principle III — Never Fabricate | `SectionResult.Empty` → "Not detected"; `SectionResult.Error` → "Not available"; placeholders are textually distinct | ✅ |
| Principle IV — Curated Baseline | `BaselineRuleProvider.rules` passed unmodified to `PromptGenerator`; `:ui` does not alter baseline | ✅ |
| Tech Stack | Kotlin 2.2.20, JDK 21, IntelliJ Platform Gradle Plugin 2.x, IDEA 2025.3.5 | ✅ |
| Code Style | detekt + ktlint already configured in root `build.gradle.kts`; no additional setup needed | ✅ |
| Testing | No mandatory coverage for `:ui` per spec Assumptions; manual IDE run is primary validation | ✅ |
| Project Structure | `dev.zahaand.projectscan.ui` package; FR-018: no code under `dev.zahaand` without `.projectscan` after cleanup | ✅ |
| Dependency Direction | Root module → `:model`, `:scan`, `:baseline`, `:prompt`; constitution conflict noted in Complexity Tracking | ⚠️ |

## Project Structure

### Documentation (this feature)

```text
specs/005-ui-tool-window/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

This feature lives in the root module (`src/`), which is the `:ui` plugin entry point.

```text
src/
└── main/
    ├── kotlin/
    │   └── dev/zahaand/projectscan/ui/
    │       ├── ProjectScanToolWindowFactory.kt   # ToolWindowFactory entry point; wires all adapters + ScanService
    │       ├── ProjectScanPanel.kt               # Main JBPanel; manages PreScan / PostScan state transitions
    │       ├── SectionPanel.kt                   # Collapsible section: header toggle + body + copy button
    │       ├── ScanResultRenderer.kt             # Pure functions: SectionResult<T> → display string per section
    │       ├── ProjectScanBundle.kt              # DynamicBundle wrapper for messages/ProjectScanBundle.properties
    │       └── UiSection.kt                      # Data class: flattened section ready for rendering
    └── resources/
        ├── META-INF/
        │   └── plugin.xml                        # Updated: factoryClass, resource-bundle, icon, anchor
        ├── icons/
        │   └── projectScanToolWindow.svg         # 16×16 tool window icon (distinct from pluginIcon.svg)
        └── messages/
            └── ProjectScanBundle.properties      # All UI strings (replaces MyMessageBundle.properties)

# Files to DELETE (demo scaffold — FR-017):
src/main/kotlin/dev/zahaand/MyToolWindowFactory.kt
src/main/kotlin/dev/zahaand/MyMessageBundle.kt
src/main/resources/messages/MyMessageBundle.properties
```

*Note: `ScanPanelState.kt` was removed during implementation; UI state (PreScan / PostScan) is managed inline via `@Volatile` fields in `ProjectScanPanel.kt` rather than a dedicated sealed class.*

**Structure Decision**: Root module only. No new Gradle subproject is created for `:ui`. The root module is already the plugin module — code is added to `src/main/kotlin/dev/zahaand/projectscan/ui/`. The `build.gradle.kts` is updated to add `:scan`, `:baseline`, `:prompt` dependencies.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| `ui → prompt` dependency (constitution says `scan`, `prompt`, `ui` MUST NOT depend on each other) | FR-013 requires `ConstitutionPrompt.render()` from `:prompt`; no other module exposes this type | Moving `ConstitutionPrompt` to `:model` would pollute the data model with rendering logic; the constitution rule targets lateral coupling (e.g., `:scan` → `:prompt`), not the composition root assembling its parts |

## Design Decisions

### 1. Collapsible Sections

Use a custom expandable panel built from `JBPanel` + `JButton` toggle (shows/hides the body `JPanel` via `isVisible`). `CollapsiblePanel` is internal API and not reliably available. See [research.md § 1](research.md).

### 2. ScanService Wiring Contract

Wire all six IJ adapters and two config parsers directly in `ProjectScanToolWindowFactory.createToolWindowContent()`. No DI framework, factory method, or companion object exists in `:scan` — the `ScanService` constructor is the only wiring point. No IntelliJ Service registration is required.

**Exact constructor parameter order** (verified against `scan/ScanService.kt`):

```kotlin
ScanService(
    buildSystemPort    = IjBuildSystemAdapter(project),
    dependencyPort     = IjDependencyAdapter(project),
    styleSourcePort    = IjStyleSourceAdapter(project),
    linterPort         = IjLinterAdapter(project),
    linterConfigParsers = mapOf(
        "checkstyle" to CheckstyleConfigParser(),
        "pmd"        to PmdConfigParser(),
    ),
    testInfoPort       = IjTestInfoAdapter(project),
    moduleStructurePort = IjModuleStructureAdapter(project),
)
```

`PromptGenerator` takes no constructor arguments. `BaselineRuleProvider.rules` is a lazy-loaded singleton list — pass it unfiltered (see spec Assumptions). See [research.md § 2](research.md).

### 3. Section Body Rendering

`ScanResultRenderer` in `:ui` renders each `SectionResult<T>` to a display string independently. PromptGenerator's rendering functions are private and their output targets the generated prompt, not the UI display. See [research.md § 3](research.md).

### 4. Background Task

`Task.Backgroundable` with `@Volatile` result fields; `onSuccess()` updates the panel on EDT; `onThrowable()` reverts to pre-scan and shows error notification balloon; `onFinished()` always re-enables the scan button. See [research.md § 4](research.md).

### 5. Message Bundle

`ProjectScanBundle` backed by `messages/ProjectScanBundle.properties`. Key schema in [research.md § 7](research.md).

## References

- [research.md](research.md) — Phase 0 findings and design decisions
- [data-model.md](data-model.md) — Entity definitions and UI state model
- [quickstart.md](quickstart.md) — How to run and verify the plugin locally
