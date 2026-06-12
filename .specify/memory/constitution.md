<!--
## Sync Impact Report
- Version change: (none) → 1.0.0 (initial ratification)
- Prior principles: none — this is the first constitution for this project
- Added sections: Core Principles (I–IV), Tech Stack, Code Style & Static Analysis, Testing,
  Project Structure, Governance
- Removed sections: none
- Templates requiring updates:
  - .specify/templates/plan-template.md — Constitution Check gates reference
    "[Gates determined based on constitution file]"; remains valid as a dynamic reference ✅
  - .specify/templates/spec-template.md — no constitution-specific constraints to encode ✅
  - .specify/templates/tasks-template.md — no constitution-specific task categories to encode ✅
  - .specify/templates/commands/ — directory does not exist; no action required ✅
  - README.md — generic scaffold README; no principle references to update ✅
- Deferred TODOs: none — all fields resolved at initial ratification
-->

# project-scan Constitution

## Core Principles

### I. Decoupling of Collection and Generation

The data-collection layer (`scan`) is self-contained and MUST NOT depend on prompt generation.
A structured, section-based data model (`model`) is the contract between all producers and consumers.
The prompt generator (`prompt`) and per-section copy actions (`ui`) are independent consumers of
this model. The `model` component MUST allow new consumers to be added without any modification
to the collection layer.

**Rationale**: Coupling data collection to a specific output format locks the plugin into a single
consumer. Keeping the model independent makes the collected data reusable by future consumers
(alternative prompt strategies, CLI export, etc.) without invasive changes to the collection layer.

### II. Read Through the IntelliJ Project Model Only

Project information — dependencies with resolved versions, JDK/language level, build system, modules —
MUST be read exclusively via the IntelliJ project model and External System API. Textual parsing of
build files (e.g., reading `build.gradle` or `pom.xml` as raw text) is prohibited.

**Rationale**: The IntelliJ project model provides resolved, IDE-validated data. Textual parsing is
fragile, duplicates IDE logic, and produces results that diverge from what IntelliJ itself understands
about the project.

### III. Never Fabricate

When data is absent or undetectable, the corresponding output section MUST be explicitly marked empty
or "not detected". The curated baseline rule set still applies in all cases, regardless of what is
found in the scanned project. Scan history MUST NOT be stored; the generated constitution file is the
single source of truth for subsequent update comparisons.

**Rationale**: Users trust the generated output to accurately reflect their project. Silent omissions
or invented values corrupt that trust and lead to wrong downstream decisions. A single source of truth
for comparisons keeps the update workflow simple and auditable.

### IV. Curated Baseline Rule Set

The plugin ships a static, curated rule set stored as an in-plugin resource. This baseline is always
applied, even when the scanned project has no linter configuration. The baseline MUST NOT define
style, formatting, or naming rules — that plane belongs exclusively to the project's own linters.

Rule priority hierarchy (highest first):
1. Project linter rules, especially those with `severity=error` / `failOnViolation=true`
2. Baseline set — levels 1 and 2
3. Unwritten team practice (not enforced)

Each emitted rule MUST be tagged with its origin: either `"project standard"` or
`"baseline quality requirement"`. The baseline changes only through plugin releases.

**Rationale**: A consistent quality floor across all scanned projects makes constitution output
immediately useful even for projects with no linter configuration. Restricting the baseline to
non-style rules avoids generating false conflicts with a project's own formatting choices.

## Tech Stack

- **Language**: Kotlin
- **Build**: Gradle with Kotlin DSL; IntelliJ Platform Gradle Plugin 2.x
  (`org.jetbrains.intellij.platform`)
- **Target IDE**: IntelliJ IDEA 2025.3.5; JDK 21 (JetBrains Runtime)
- **Required bundled plugin**: `bundledPlugin("com.intellij.java")`
- **MVP scope**: JVM/Java projects; Maven and Gradle build systems

Tool versions (detekt, ktlint), `since-build`/`until-build` values, and publication parameters
are decided at implementation/publication time and are not pinned in this constitution.

## Code Style & Static Analysis

- **detekt** MUST be configured for static analysis and quality checks.
- **ktlint** MUST be configured for Kotlin code style enforcement and formatting.
- Formatting ownership belongs to ktlint; detekt rules MUST NOT duplicate formatting concerns
  to avoid conflicts between the two tools.
- Both tools MUST be integrated into CI. Any violation MUST fail the build.
- Specific tool versions and Gradle integration mechanism are deferred to implementation time.

## Testing

- **Pure logic** (`model`, `baseline`, `prompt` components): JUnit 5
- **Platform-dependent code** (`scan` component and platform adapters): IntelliJ Platform Test
  Framework
- All key logic in `model`, `scan`, `baseline`, and `prompt` MUST be covered by tests.
- The `ui` layer and thin platform adapters are NOT subject to mandatory coverage requirements.
- No numeric coverage threshold is defined. The requirement is qualitative: key logic paths are
  exercised by the test suite.

## Project Structure

**Root package**: `dev.zahaand.projectscan`

**Organization**: by component

| Component  | Responsibility |
|------------|----------------|
| `model`    | Section-based data model — the shared contract between producers and consumers |
| `scan`     | Collectors reading the IntelliJ project model; internally split by section: `stack`, `codestyle`, `linters`, `tests`, `structure` |
| `baseline` | Curated static rule set stored as an in-plugin resource |
| `prompt`   | Prompt generator; supports create and update modes |
| `ui`       | Tool Window and per-section copy actions |

**Dependency direction** (enforced):
- `model` has NO outgoing dependencies on any other component.
- `scan`, `prompt`, and `ui` each depend on `model`.
- `scan`, `prompt`, and `ui` MUST NOT depend on each other.

## Governance

- **Plugin versioning**: Semantic versioning (MAJOR.MINOR.PATCH).
- **CHANGELOG**: Keep a Changelog format (`CHANGELOG.md`).
- **Baseline rule set**: Changes only through plugin releases; no runtime modification.
- **Constitution versioning** is independent of the plugin version. Bump rules:
  - MAJOR — removing or redefining an existing principle
  - MINOR — adding a principle or a new section
  - PATCH — wording clarifications, typo fixes, non-semantic refinements
- **Compliance**: All specs, plans, and implementations MUST conform to this constitution.
  Deviations are tracked explicitly as technical debt.
  Conformance is verified via `/speckit-analyze`.
- **Amendment procedure**: Proposed amendments are documented, the constitution version is bumped
  per the rules above, and the change is recorded in `CHANGELOG.md`.

**Version**: 1.0.0 | **Ratified**: 2026-06-12 | **Last Amended**: 2026-06-12
