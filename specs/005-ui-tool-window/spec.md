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
2. **Given** a scan is running, **When** the scan completes, **Then** the six collapsible sections appear in the correct order (Tech Stack → Code Style → Linters → Tests → Project Structure → Constitution Prompt) and the "Scan" button is re-enabled.
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

**Independent Test**: On a project without linter config, verify the Linters section shows a "not detected" placeholder (not an error placeholder). To test the error path, a controlled test that injects a failing collector verifies the error placeholder is distinct.

**Acceptance Scenarios**:

1. **Given** a collection section produces no data because nothing was found, **When** the section is rendered, **Then** the body shows a "not detected" placeholder message and the "Copy" button is disabled.
2. **Given** a collection section fails with an error during collection, **When** the section is rendered, **Then** the body shows a "not available" placeholder (optionally with a short cause) that is visually and textually distinct from the "not detected" placeholder, and the "Copy" button is disabled.

---

### User Story 4 — First Launch: Clean Pre-Scan State (Priority: P4)

A developer installs the plugin and opens the tool window for the first time. The panel shows a "Scan" button and a short hint, but no section panels — there is nothing to show yet.

**Why this priority**: Avoids rendering six empty or placeholder sections that could confuse the user about whether a scan has run.

**Independent Test**: Open the tool window with no prior scan state; verify only the "Scan" button (and optional hint text) are visible, and no section panels exist in the UI.

**Acceptance Scenarios**:

1. **Given** the tool window has never been used, **When** the user opens it, **Then** only the "Scan" button and optional hint text are visible; no section panels are rendered.
2. **Given** the tool window is in the pre-scan state, **When** the user clicks "Scan" and the scan completes, **Then** all six section panels appear and remain visible for the duration of the current IDE session (results are held in memory and are not restored after an IDE restart).

---

### Edge Cases

- What happens when the IDE project contains no source files? — All five collection sections return `Empty`; the prompt section is still rendered and its "Copy" button is enabled.
- What happens if the user clicks "Scan" while a scan is already running? — The "Scan" button is disabled during the scan, so re-entry is prevented.
- What happens if one section errors but others succeed? — Each section is rendered independently with its own state; the erroring section shows the error placeholder, and other sections with `Ok` state have their "Copy" buttons enabled.
- What happens when the tool window is closed and reopened mid-scan? — The background task continues; upon reopening, the state reflects whatever the scan has produced so far or the completed result.
- What happens when `ScanService` throws an unexpected top-level exception (not a per-section `SectionResult.Error`)? — The panel reverts to the pre-scan state, the "Scan" button is re-enabled, and an IDE error notification balloon is shown describing the failure.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The plugin MUST register a Tool Window in the IDE with default anchor `RIGHT`, accessible from the standard IDE view menu.
- **FR-002**: The Tool Window MUST display a dedicated icon distinct from the Marketplace plugin icon.
- **FR-003**: The Tool Window panel MUST be vertically scrollable via a single scroll pane.
- **FR-004**: The panel MUST show a single "Scan" button at the top with a fixed label that does not change based on prior scan history.
- **FR-005**: Clicking "Scan" MUST disable the button immediately and run the collection and prompt generation in the background without blocking the IDE.
- **FR-006**: The "Scan" button MUST be re-enabled once the background task completes (success or failure), including when a top-level exception aborts the scan.
- **FR-006a**: If `ScanService` throws an unexpected top-level exception, the panel MUST revert to the pre-scan state and the IDE MUST display an error notification balloon describing the failure.
- **FR-007**: Before the first scan completes, the panel MUST NOT render the six section panels.
- **FR-008**: After a scan completes, the panel MUST render six collapsible sections in order: Tech Stack, Code Style, Linters, Tests, Project Structure, Constitution Prompt. The five collection sections MUST be expanded by default; the Constitution Prompt section MUST be collapsed by default.
- **FR-009**: Each of the five collection sections MUST display a human-readable text body rendered from the corresponding `SectionResult`.
- **FR-010**: For a collection section in the `Ok` state, the body MUST show rendered section data and the "Copy" button MUST be enabled.
- **FR-011**: For a collection section in the `Empty` state, the body MUST show a "not detected" placeholder and the "Copy" button MUST be disabled.
- **FR-012**: For a collection section in the `Error` state, the body MUST show a "not available" placeholder (optionally with a short cause) distinct from the `Empty` placeholder, and the "Copy" button MUST be disabled.
- **FR-013**: The Constitution Prompt section MUST display the full output of `ConstitutionPrompt.render()` verbatim, without modification.
- **FR-014**: The Constitution Prompt section's "Copy" button MUST always be enabled after a scan.
- **FR-015**: Clicking "Copy" on any enabled section MUST place the section's displayed text on the system clipboard.
- **FR-016**: All UI strings (button label, section headers, state placeholders) MUST be sourced from a project-owned message bundle; no strings may be hardcoded.
- **FR-017**: The plugin MUST remove all demo-generated files (`MyToolWindowFactory`, `MyMessageBundle`, demo `.properties`) and replace them with project-owned equivalents under `dev.zahaand.projectscan.ui`.
- **FR-018**: After cleanup, no code MUST remain under the root `dev.zahaand` package without the `.projectscan` sub-package.

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
- **SC-003**: The IDE remains fully responsive (no UI freeze, no modal dialog) throughout a scan run.
- **SC-004**: The `Empty` and `Error` section states are visually and textually distinguishable without any tooltip or additional explanation needed.
- **SC-005**: After demo-file cleanup, no reference to `MyToolWindowFactory`, `MyMessageBundle`, or the demo `.properties` file remains anywhere in the plugin's source or configuration.
- **SC-006**: All six sections appear in the correct declared order on every scan.

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
- Collapsible sections use the standard IntelliJ Platform collapsible panel mechanism; no custom expand/collapse widgets are built.
