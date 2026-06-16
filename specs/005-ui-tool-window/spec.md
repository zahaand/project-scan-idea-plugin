# Feature Specification: UI Tool Window

**Feature Branch**: `005-ui-tool-window`
**Created**: 2026-06-16
**Status**: Draft
**Input**: User description: "Implement Sprint 5 — the :ui module: IntelliJ Platform Tool Window assembling all lower modules"

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Run a Full Scan and View Results (Priority: P1)

A developer opens the Project Scan tool window in their IDE, clicks "Scan", waits briefly, and sees six sections populated with real information about their project: Tech Stack, Code Style, Linters, Tests, Project Structure, and the generated Constitution prompt.

**Why this priority**: This is the primary end-to-end flow. Without it, the tool window delivers no value at all.

**Independent Test**: Open the plugin in any IntelliJ-based IDE with a non-empty project, click "Scan", and verify that all six sections appear with non-empty bodies within a few seconds and the IDE remains responsive throughout.

**Acceptance Scenarios**:

1. **Given** the tool window is open and no scan has run, **When** the user clicks "Scan", **Then** the button becomes disabled, a background progress indicator appears, and the six section panels are not yet shown.
2. **Given** a scan is running, **When** the scan completes successfully, **Then** the six collapsible sections appear in the correct order (Tech Stack → Code Style → Linters → Tests → Project Structure → Constitution Prompt) and the "Scan" button is re-enabled.
3. **Given** the scan has completed on a project with detectable tech stack, **When** the user views the Tech Stack section, **Then** the section body contains human-readable text describing the project's technologies.

---

### User Story 2 — Copy a Section's Content to Clipboard (Priority: P2)

After a successful scan, the developer wants to paste one of the collected sections into a document or conversation. They click the "Copy" button on that section and the text is placed on the system clipboard.

**Why this priority**: Clipboard copy is the primary output action of the MVP; the tool window collects and displays information specifically so the user can use it elsewhere.

**Independent Test**: After a scan on a project with at least one `Ok` section, click "Copy" on that section and paste into any text editor — the pasted content must match what is displayed in the section body.

**Acceptance Scenarios**:

1. **Given** a section is in the `Ok` state, **When** the user clicks "Copy" on that section, **Then** the system clipboard contains the exact text shown in that section's body.
2. **Given** a section is in the `Empty` state, **When** the user views the "Copy" button for that section, **Then** the button is disabled and cannot be clicked.
3. **Given** a section is in the `Error` state, **When** the user views the "Copy" button for that section, **Then** the button is disabled and cannot be clicked.
4. **Given** the Constitution Prompt section is shown, **When** the user clicks "Copy" on the prompt section, **Then** the clipboard contains the full rendered Markdown prompt — regardless of whether any collection section was `Empty` or `Error`.

---

### User Story 3 — Distinguish Between "Nothing Found" and "Error" (Priority: P3)

A developer scans a project that has no linter configuration. They can tell at a glance whether the section is empty because the tool found nothing, or because the tool encountered a problem collecting the data.

**Why this priority**: Preserving this distinction is a "Never Fabricate" principle: showing an error as if it were an empty result misleads the developer about the reliability of the data.

**Independent Test**: On a project without linter config, verify the Linters section shows a "not detected" placeholder (not an error placeholder). To test the error path, a controlled test that injects a failing collector verifies the error placeholder is textually distinct from "not detected".

**Acceptance Scenarios**:

1. **Given** a collection section produces no data because nothing was found, **When** the section is rendered, **Then** the body shows a "Not detected" placeholder message and the "Copy" button is disabled.
2. **Given** a collection section fails with an error during collection, **When** the section is rendered, **Then** the body shows a "Not available" placeholder (optionally with a short cause) that is textually distinct from the "Not detected" placeholder, and the "Copy" button is disabled.

---

### User Story 4 — First Launch: Clean Pre-Scan State (Priority: P4)

A developer installs the plugin and opens the tool window for the first time. The panel shows a "Scan" button and a short hint, but no section panels — there is nothing to show yet.

**Why this priority**: Avoids rendering six empty or placeholder sections that could confuse the user about whether a scan has run.

**Independent Test**: Open the tool window with no prior scan state; verify only the "Scan" button and hint text are visible, and no section panels exist in the UI.

**Acceptance Scenarios**:

1. **Given** the tool window has never been used, **When** the user opens it, **Then** only the "Scan" button and hint text are visible; no section panels are rendered.
2. **Given** the tool window is in the pre-scan state, **When** the user clicks "Scan" and the scan completes successfully, **Then** all six section panels appear and remain visible for the duration of the current IDE session. Results are held in memory only; they are not persisted and are not restored after an IDE restart.

---

### Edge Cases

- What happens when the IDE project contains no source files? — All five collection sections return `Empty`; the prompt section is still rendered and its "Copy" button is enabled (per FR-014: the prompt Copy button is always enabled after any successful scan, regardless of the states of the collection sections).
- What happens if the user clicks "Scan" while a scan is already running? — The "Scan" button is disabled during the scan, so re-entry is prevented.
- What happens if one section errors but others succeed? — Each section is rendered independently with its own state; the erroring section shows the error placeholder, and other sections with `Ok` state have their "Copy" buttons enabled.
- What happens when the tool window is closed and reopened mid-scan? — The background task continues uninterrupted. `ScanService.scan()` is a blocking call that returns a complete `ScanResult` when finished — there is no partial-sections state. Upon reopening: if the scan is still running, the panel is in the scanning state (button disabled); once the scan completes, the full result is rendered. Results are held in session memory and are not restored after an IDE restart.
- What happens when the background task throws an unexpected top-level exception? — The panel reverts to the pre-scan state, the "Scan" button is re-enabled, and an IDE error notification balloon is shown describing the failure.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The plugin MUST register a Tool Window in the IDE with default anchor `RIGHT`, accessible from the standard IDE view menu.
- **FR-002**: The Tool Window MUST display a dedicated icon distinct from the Marketplace plugin icon (`pluginIcon.svg`). A dedicated SVG icon asset for the tool window MUST be created in this sprint under the `:ui` module resources and referenced from `plugin.xml`. The exact visual is decided at implementation time; the requirement is that the asset exists and is distinct from `pluginIcon.svg`.
- **FR-003**: The Tool Window panel MUST be vertically scrollable via a single scroll pane. The "Scan" button and hint text (see FR-004, FR-007) are pinned outside the scroll pane, at the top of the panel, and remain visible regardless of scroll position. Only the section area (the six collapsible panels) is inside the scroll pane. *Validated by SC-001, SC-003; verified by design.*
- **FR-004**: The panel MUST show a single "Scan" button at the top with a fixed label that does not change based on prior scan history.
- **FR-005**: Clicking "Scan" MUST disable the button immediately and run the collection, baseline loading, and prompt generation in the background — within a single `Task.Backgroundable` run — without blocking the IDE. The complete result (scan sections and generated prompt) is returned to the EDT together when the run finishes.
- **FR-006**: The "Scan" button MUST be re-enabled once the background task completes (success or failure), including when a top-level exception aborts the scan. "The background task" refers to the single `Task.Backgroundable` run described in FR-005.
- **FR-006a**: If the background task throws an unexpected top-level exception, the panel MUST revert to the pre-scan state (identical to FR-007: section panels removed, only the "Scan" button and hint text visible) and the IDE MUST display an error notification balloon describing the failure. A "top-level exception" is a thrown `java.lang.Exception` subtype that escapes the background run; `java.lang.Error` subtypes (e.g., `OutOfMemoryError`, `StackOverflowError`) are NOT caught — they are left to the platform.
- **FR-007**: Before the first scan completes successfully, the panel MUST NOT render the six section panels. Only the "Scan" button and hint text are visible.
- **FR-008**: After a scan **successfully** completes, the panel MUST render six collapsible sections in this order, with these exact displayed titles: "Tech Stack", "Code Style", "Linters", "Tests", "Project Structure", "Constitution Prompt". The five collection sections MUST be expanded by default; the Constitution Prompt section MUST be collapsed by default. If the background task ends with a top-level exception, sections are NOT rendered — see FR-006a.
- **FR-009**: Each of the five collection sections MUST display a human-readable text body rendered from the corresponding `SectionResult`. For `Ok` sections, the body is rendered as plain text presenting the collected fields as-is, following the "facts, not interpretation" principle: the renderer shows what was found without classifying or interpreting the data. Per-section formatting detail is specified in the implementation plan.
- **FR-010**: For a collection section in the `Ok` state, the body MUST show rendered section data and the "Copy" button MUST be enabled.
- **FR-011**: For a collection section in the `Empty` state, the body MUST show the placeholder text "Not detected" and the "Copy" button MUST be disabled.
- **FR-012**: For a collection section in the `Error` state, the body MUST show the placeholder text "Not available" (optionally appended with a short cause) and the "Copy" button MUST be disabled. The "Not available" text is textually distinct from the "Not detected" text of FR-011; textual distinction alone satisfies SC-004 — color or icon differentiation is not required.
- **FR-013**: The Constitution Prompt section MUST display the full output of `ConstitutionPrompt.render()` verbatim, without modification.
- **FR-014**: The Constitution Prompt section's "Copy" button MUST always be enabled after any successful scan, regardless of the states of the five collection sections. The prompt is always generated from `PromptGenerator.generate()` even when all collection sections are `Empty` or `Error`.
- **FR-015**: Clicking "Copy" on any enabled section MUST place the section's displayed text on the system clipboard.
- **FR-016**: All user-displayed strings (button label, section headers, state placeholders, hint text, notification messages) MUST be sourced from a project-owned message bundle; no displayed strings may be hardcoded. Technical identifiers in `plugin.xml` (tool window registration id, notification group id) are not user-displayed strings and are not externalized to the bundle.
- **FR-017**: The plugin MUST remove all demo-generated files and replace them with project-owned equivalents under `dev.zahaand.projectscan.ui`. Files to remove: `MyToolWindowFactory.kt`, `MyMessageBundle.kt`, `messages/MyMessageBundle.properties`. Replacements: `ProjectScanToolWindowFactory.kt` and `ProjectScanBundle.kt` (backed by `messages/ProjectScanBundle.properties`) under `dev.zahaand.projectscan.ui`. The `plugin.xml` entries `<resource-bundle>messages.MyMessageBundle</resource-bundle>` and `<toolWindow factoryClass="dev.zahaand.MyToolWindowFactory" .../>` MUST also be updated to reference the new class and bundle. *Validated by SC-005.*
- **FR-018**: After demo cleanup, no Kotlin source files (`.kt`) MUST remain under the root `dev.zahaand` package without the `.projectscan` sub-package. Resource files and `plugin.xml` declarations are covered by FR-017. This constraint applies to the root module only; lower modules (`:model`, `:scan`, `:baseline`, `:prompt`) already use `dev.zahaand.projectscan.*` and are out of scope. *Validated by SC-005.*
- **FR-019**: When the user clicks "Scan" after a prior scan has already displayed results, the existing section panels MUST remain visible until the new scan completes successfully, then be replaced wholesale with the new results. Sections are NOT cleared immediately on button click (no blank flash). The "Scan" button is disabled for the duration of the new scan and re-enabled when it completes. *Validated by SC-001, SC-003.*
- **FR-020**: On every scan completion (including re-scans), the expand/collapse state of all six sections MUST reset to the default declared in FR-008: collection sections expanded, Constitution Prompt collapsed. Collapse state is not carried over between scans.
- **FR-021**: If `BaselineRuleProvider.rules` throws a `BaselineLoadException` during the background run, it MUST be treated as a top-level exception per FR-006a: the panel reverts to the pre-scan state and an error notification balloon is shown. In the assembled plugin the `rules.json` resource is always present, making this a defensive safety branch rather than an expected failure mode.
- **FR-022**: All UI mutations triggered by the background task's lifecycle callbacks (section rendering, button state changes, panel state transitions) MUST occur on the EDT. `Task.Backgroundable`'s `onSuccess()`, `onThrowable()`, and `onFinished()` callbacks provide EDT execution and MUST be used for all panel updates. *Validated by SC-003; verified by design.*

### Structural Requirements Note

FR-003, FR-016, FR-017, FR-018, FR-019, FR-020, FR-021, FR-022 are structural requirements without a dedicated user story. Their validation:

| Requirement | Validated by |
|-------------|-------------|
| FR-003 (scroll pane layout) | SC-001, SC-003; verified by design |
| FR-016 (message bundle) | Code review: no hardcoded strings in merged diff |
| FR-017 (demo cleanup) | SC-005 |
| FR-018 (package constraint) | SC-005 |
| FR-019 (re-scan flow) | SC-001, SC-003 |
| FR-020 (state reset on re-scan) | Manual verification via IDE run |
| FR-021 (BaselineLoadException) | Defensive safety branch; not a testable path in the assembled plugin |
| FR-022 (EDT mutations) | SC-003; verified by design via Task.Backgroundable callbacks |

### Key Entities

- **ScanResult**: The aggregate output of the five collectors; contains one `SectionResult` per collection section.
- **SectionResult**: A discriminated union of `Ok` (with data), `Empty`, and `Error` (with optional cause); produced per section by the `:scan` module.
- **ConstitutionPrompt**: The generated prompt object produced by `:prompt`; its `render()` method returns the full Markdown string to display and copy.
- **ProjectScanBundle**: The project-owned message bundle holding all English UI strings.
- **ProjectScanToolWindowFactory**: The IntelliJ Platform entry point that constructs and returns the tool window panel.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer can open the tool window, click "Scan", and see all six sections populated within the time it takes the IDE's existing project model to be queried — no artificial delays.
- **SC-002**: Clicking "Copy" on an enabled section always places the exact displayed text on the clipboard, verified by paste into any text field.
- **SC-003**: The IDE remains fully responsive (no UI freeze, no modal dialog) throughout a scan run. Responsiveness is guaranteed by design (off-EDT execution via `Task.Backgroundable`; no modal dialogs) and verified by the IDE not freezing during a scan.
- **SC-004**: The `Empty` and `Error` section states are textually distinguishable by their placeholder text alone: `Empty` shows "Not detected"; `Error` shows "Not available" (optionally with a short cause). Textual distinction alone satisfies this criterion; color or icon differentiation is not required.
- **SC-005**: After demo-file cleanup, no reference to `MyToolWindowFactory`, `MyMessageBundle`, or the demo `.properties` file remains anywhere in the plugin's source or configuration. "Configuration" for this criterion includes: `plugin.xml`, Gradle build files, `.run` run-configuration files, and Kotlin source files.
- **SC-006**: All six sections appear in the following order on every scan: Tech Stack → Code Style → Linters → Tests → Project Structure → Constitution Prompt.

## Clarifications

### Session 2026-06-16

- Q: Are scan results persisted across IDE restarts, or held in memory for the current session only? → A: Session-only — results are kept in memory and lost when the IDE is restarted.
- Q: What happens when ScanService throws an unexpected top-level exception (not a per-section SectionResult.Error)? → A: Show an IDE error notification balloon; revert the panel to the pre-scan state; re-enable the "Scan" button.
- Q: What is the default expand/collapse state of sections when first rendered after a scan? → A: The five collection sections are expanded by default; the Constitution Prompt section is collapsed by default (the prompt body is long).

## Assumptions

- The four lower modules (`:model`, `:scan`, `:baseline`, `:prompt`) are complete, stable, and their public APIs will not change during this sprint.
- The plugin is create-only: it never reads an existing Constitution file and never distinguishes a "first run" from a "rescan" at the data level.
- Scan results are held in memory for the current IDE session only; no persistent state storage is required. Opening a new IDE window or restarting the IDE shows the pre-scan state again.
- File-export of section content is out of scope; clipboard copy is the only output action.
- No Kotlin coroutines are introduced; `Task.Backgroundable` is sufficient for the single button-triggered background task.
- The UI is English-only; no localization infrastructure is built.
- Testing of the thin UI/assembly layer is proportionate: no mandatory unit-test coverage is required; manual verification via IDE run is the primary validation method.
- The `:ui` module is the only module that may depend on the IntelliJ Platform SDK; lower modules remain platform-free.
- Collapsible sections are built from a `JBPanel` + a `JButton` toggle that flips body visibility; the platform's internal `CollapsiblePanel` API is not used (it is internal and not reliably available).
- The `:scan` module isolates collection errors per section (Sprint 2 design): `ScanService.scan()` always returns a `ScanResult` under normal operation. A top-level exception (FR-006a) is an anomaly outside per-section isolation — a service bug or infrastructure failure — not an expected code path.
- `:ui` passes the **full, unfiltered** `BaselineRuleProvider.rules` list to `PromptGenerator.generate()`. Baseline filtering by Java language level is performed inside `:prompt`; `:ui` MUST NOT pre-filter the baseline list, or the prompt output would differ from the intended constitution content.
- Accessibility (keyboard navigation, screen reader labels for the tool window and section panels) is out of MVP scope. Standard Swing/IntelliJ Platform components provide baseline keyboard navigation. Tracked as post-MVP technical debt.

## Accepted Deviations

### Deviation D1 — `:ui` depends on `:prompt` (Sprint 5)

**Status**: Accepted for Sprint 5. Tracked as technical debt. Constitution amendment deferred to Sprint 6.

**Description**: The `:ui` module is the composition root of the plugin and legitimately depends on `:model`, `:scan`, `:baseline`, and `:prompt`. It calls `ScanService.scan()`, `BaselineRuleProvider.rules`, `PromptGenerator.generate()`, and `ConstitutionPrompt.render()`. This is composition, not the lateral coupling the constitution's Dependency Direction prohibits between sibling consumers (`scan ↔ prompt`).

**Known divergence from the current constitution**: The constitution's §Dependency Direction lists `ui` among the components that "MUST NOT depend on each other", and Principle I calls `ui` an "independent consumer of the model". These statements do not reflect the assembly-root role of `:ui`.

**Disposition**: Per the constitution's own Governance ("Deviations are tracked explicitly as technical debt"), this deviation is accepted for Sprint 5. The constitution text fix is deferred to the Sprint 6 constitution-amendment package alongside other queued amendments (D1/I1, D2, I3). The Sprint 6 amendment will:
- Amend §Dependency Direction: `:ui` MAY depend on `:scan`, `:baseline`, and `:prompt`; the mutual-independence prohibition is narrowed to `scan ↔ prompt`.
- Amend Principle I: clarify `:ui`'s role as the composition consumer, not a peer consumer alongside `:prompt`.

**Note**: The constitution MUST NOT be edited in this sprint. This disposition is recorded here only.
