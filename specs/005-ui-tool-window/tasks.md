---

description: "Task list for UI Tool Window implementation"
---

# Tasks: UI Tool Window

**Input**: Design documents from `/specs/005-ui-tool-window/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, quickstart.md ✅

**Tests**: No unit test tasks generated — per spec Assumptions, no mandatory unit coverage for `:ui`; manual IDE run is the primary validation method.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies between them)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Exact file paths are included in each description

---

## Phase 1: Setup & Demo Cleanup

**Purpose**: Remove the generated scaffold and put foundational infrastructure in place. Every subsequent phase depends on these tasks being done.

- [X] T001 Delete demo scaffold files: `src/main/kotlin/dev/zahaand/MyToolWindowFactory.kt`, `src/main/kotlin/dev/zahaand/MyMessageBundle.kt`, `src/main/resources/messages/MyMessageBundle.properties` (FR-017, FR-018)
- [X] T002 [P] Create tool window SVG icon `src/main/resources/icons/projectScanToolWindow.svg` — minimal 16×16 SVG with a visually distinct mark from `pluginIcon.svg`; use `viewBox="0 0 16 16"` (FR-002)
- [X] T003 Update root `build.gradle.kts` — add `implementation(project(":scan"))`, `implementation(project(":baseline"))`, `implementation(project(":prompt"))` to the root module's `dependencies {}` block
- [X] T004 Update `src/main/resources/META-INF/plugin.xml` — change `factoryClass` to `dev.zahaand.projectscan.ui.ProjectScanToolWindowFactory`; update `<resource-bundle>` to `messages.ProjectScanBundle`; add `icon="/icons/projectScanToolWindow.svg"` and `anchor="RIGHT"` attributes to `<toolWindow>`; remove all `My*` class and bundle references (FR-001, FR-017). Verify `.run/*.run.xml` run-configuration files contain no `My*` references (per SC-005 scope).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Message bundle, UI state model types, and the section renderer. Every user story phase depends on these.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T005 Create `src/main/resources/messages/ProjectScanBundle.properties` with all 13 key-value pairs from research.md §7 (FR-016):
  ```
  toolwindow.ProjectScan.title=Project Scan
  toolwindow.ProjectScan.scan.button=Scan
  toolwindow.ProjectScan.hint=Click "Scan" to analyze the project.
  section.TechStack.title=Tech Stack
  section.CodeStyle.title=Code Style
  section.Linters.title=Linters
  section.Tests.title=Tests
  section.Structure.title=Project Structure
  section.Constitution.title=Constitution Prompt
  section.state.empty=Not detected
  section.state.error=Not available
  section.state.error.with.cause=Not available (cause: {0})
  section.copy.button=Copy
  scan.error.notification.title=Project Scan Failed
  ```
- [X] T006 Create `src/main/kotlin/dev/zahaand/projectscan/ui/ProjectScanBundle.kt` — `object` extending `DynamicBundle("messages/ProjectScanBundle")` with a `message(key: String, vararg params: Any): String` companion function (FR-016)
- [X] T007 [P] Create `src/main/kotlin/dev/zahaand/projectscan/ui/ScanPanelState.kt` — `sealed class ScanPanelState` with `object PreScan` and `data class PostScan(val data: PostScanData)`; `data class PostScanData(val scanResult: ScanResult, val constitutionPrompt: ConstitutionPrompt)` per data-model.md. *Superseded — ScanPanelState.kt was deleted after creation (see Block 2 corrections); UI state is managed inline via `@Volatile` fields in `ProjectScanPanel.kt`.*
- [X] T008 [P] Create `src/main/kotlin/dev/zahaand/projectscan/ui/UiSection.kt` — `data class UiSection(val title: String, val body: String, val copyEnabled: Boolean, val collapsedByDefault: Boolean)` per data-model.md
- [X] T009 Create `src/main/kotlin/dev/zahaand/projectscan/ui/ScanResultRenderer.kt` — `object` with `render(scanResult: ScanResult, constitutionPrompt: ConstitutionPrompt): List<UiSection>` returning exactly 6 items in declared order; implement all rendering rules from data-model.md: Tech Stack/Code Style/Linters/Tests/Project Structure each produce bullet-list text for `Ok` and bundle-keyed placeholders for `Empty`/`Error` (with optional cause); `copyEnabled = result is SectionResult.Ok`; `collapsedByDefault = false` for collection sections, `true` for Constitution section; Constitution section always returns `constitutionPrompt.render()` with `copyEnabled = true`

**Checkpoint**: Foundation ready — user story implementation can now begin.

---

## Phase 3: User Story 1 — Run a Full Scan and View Results (Priority: P1) 🎯 MVP

**Goal**: A developer opens the tool window, clicks "Scan", waits briefly, and sees six collapsible sections populated with real project data.

**Independent Test**: Open the sandboxed IDE on any non-empty JVM project, open View → Tool Windows → Project Scan, click "Scan", and verify that all six sections appear within a few seconds with non-empty bodies and the IDE remains responsive throughout.

- [X] T010 [US1] Create `src/main/kotlin/dev/zahaand/projectscan/ui/SectionPanel.kt` — `JBPanel(BorderLayout())`: north bar contains a `JButton` expand/collapse toggle (label `▶` collapsed / `▼` expanded) plus section title and a `JButton` copyButton (label from `section.copy.button`); center is a `JBTextArea` (non-editable, `lineWrap=true`, `wrapStyleWord=true`) wrapped in `JBScrollPane`; body panel `isVisible = !collapsedByDefault` on construction; `copyButton.isEnabled = copyEnabled` on construction; toggle `ActionListener` flips body `isVisible` and updates arrow label; `copyButton.ActionListener` is a no-op placeholder (wired in T013)
- [X] T011 [US1] Create `src/main/kotlin/dev/zahaand/projectscan/ui/ProjectScanPanel.kt` — `JBPanel(BorderLayout())`: north: top bar with `scanButton` (label from `toolwindow.ProjectScan.scan.button`) and `hintLabel` (text from `toolwindow.ProjectScan.hint`), always visible; center: `JScrollPane` wrapping `sectionContainer: JPanel(VerticalLayout or BoxLayout Y_AXIS)`, initially empty; implement `showResults(scanResult, constitutionPrompt)`: calls `ScanResultRenderer.render()`, clears `sectionContainer`, adds 6 `SectionPanel` rows, `revalidate()`/`repaint()`; implement `revertToPreScan()`: clears `sectionContainer`, `revalidate()`/`repaint()`; implement `setScanButtonEnabled(enabled: Boolean)`; `scanButton.ActionListener`: calls `setScanButtonEnabled(false)`, then creates and `queue()`s a `Task.Backgroundable(project, title, false)` with `@Volatile private var scanResult: ScanResult? = null` and `@Volatile private var constitutionPrompt: ConstitutionPrompt? = null`; `run()` assigns both; `onSuccess()` calls `showResults(scanResult!!, constitutionPrompt!!)`; `onThrowable(error)` calls `revertToPreScan()` and `Notifications.Bus.notify(Notification("ProjectScan", bundle.message("scan.error.notification.title"), error.message ?: "Unknown error", NotificationType.ERROR), project)`; `onFinished()` calls `setScanButtonEnabled(true)` (FR-005, FR-006, FR-006a, FR-019, FR-020, FR-021, FR-022). During a re-scan (user clicks Scan after results are already displayed), existing sections remain visible and are NOT cleared on button click; they are replaced wholesale only when `onSuccess()` calls `showResults()`. The collapse state always resets to the default (collection sections expanded, Constitution Prompt collapsed) because `SectionPanel` rows are rebuilt from scratch on each `showResults()` call.
- [X] T012 [US1] Create `src/main/kotlin/dev/zahaand/projectscan/ui/ProjectScanToolWindowFactory.kt` — implement `ToolWindowFactory`; `createToolWindowContent(project, toolWindow)`: instantiate `IjBuildSystemAdapter(project)`, `IjDependencyAdapter(project)`, `IjStyleSourceAdapter(project)`, `IjLinterAdapter(project)`, `IjTestInfoAdapter(project)`, `IjModuleStructureAdapter(project)`; `linterConfigParsers = mapOf("checkstyle" to CheckstyleConfigParser(), "pmd" to PmdConfigParser())`; `ScanService(buildSystemPort, dependencyPort, styleSourcePort, linterPort, linterConfigParsers, testInfoPort, moduleStructurePort)`; `PromptGenerator()`; `BaselineRuleProvider.rules`; construct `ProjectScanPanel(project, scanService, promptGenerator, baselineRules)`; add panel as content to `toolWindow.contentManager` using `contentManager.factory.createContent(panel, null, false)`

**Checkpoint**: At this point, the full scan-and-view flow (US1) should be functional via `./gradlew runIde`.

---

## Phase 4: User Story 2 — Copy Section Content to Clipboard (Priority: P2)

**Goal**: After a successful scan, clicking "Copy" on an `Ok` section places the exact displayed text on the system clipboard. The button is disabled for `Empty` and `Error` sections.

**Independent Test**: After a scan on a project with at least one `Ok` section, click "Copy" on that section and paste into any text editor — pasted content must match the displayed section body exactly.

- [X] T013 [US2] Wire clipboard copy action in `src/main/kotlin/dev/zahaand/projectscan/ui/SectionPanel.kt` — replace the no-op `copyButton.ActionListener` with `CopyPasteManager.getInstance().setContents(StringSelection(bodyTextArea.text))`; confirm `copyButton.isEnabled` is driven by `UiSection.copyEnabled` passed at construction (already set in T010); no additional change needed for Constitution section — `ScanResultRenderer` already sets `copyEnabled = true` for it (FR-014, FR-015, SC-002)

**Checkpoint**: At this point, clipboard copy works on `Ok` sections and the button is correctly disabled on `Empty`/`Error` sections.

---

## Phase 5: User Story 3 — Distinguish "Nothing Found" vs "Error" (Priority: P3)

**Goal**: A developer can tell at a glance whether a section is empty because nothing was found ("Not detected") or because the collector failed ("Not available"). These two placeholders are textually distinct.

**Independent Test**: On a project without linter config, verify the Linters section shows "Not detected". Verify that the Error placeholder "Not available" is textually distinct from "Not detected" (SC-004).

- [X] T014 [US3] Review `src/main/kotlin/dev/zahaand/projectscan/ui/ScanResultRenderer.kt` — confirm `SectionResult.Empty` renders `ProjectScanBundle.message("section.state.empty")` → `"Not detected"` with `copyEnabled = false`; confirm `SectionResult.Error` without cause renders `ProjectScanBundle.message("section.state.error")` → `"Not available"` with `copyEnabled = false`; confirm `SectionResult.Error` with non-null cause renders `ProjectScanBundle.message("section.state.error.with.cause", cause)` → `"Not available (cause: …)"` with `copyEnabled = false`; verify the two placeholder strings are textually distinct (FR-011, FR-012, SC-004)

**Checkpoint**: Empty and Error section states are visually and textually distinguishable.

---

## Phase 6: User Story 4 — First Launch: Clean Pre-Scan State (Priority: P4)

**Goal**: When the plugin is installed and the tool window opened for the first time, the panel shows only the "Scan" button and hint text — no section panels, nothing to confuse the user about whether a scan has run.

**Independent Test**: Open the tool window with no prior scan; verify only the "Scan" button and hint text are visible and no `SectionPanel` rows exist in the UI.

- [X] T015 [US4] Review `src/main/kotlin/dev/zahaand/projectscan/ui/ProjectScanPanel.kt` — confirm that at construction `sectionContainer` is empty (no `SectionPanel` rows added); confirm `scanButton` is enabled and `hintLabel` is visible; confirm `revertToPreScan()` clears all `SectionPanel` rows from `sectionContainer` and `revalidate()`/`repaint()` is called, restoring the same appearance as initial construction (FR-007)

**Checkpoint**: Pre-scan state is correct on first open and after a failed scan.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Code style enforcement and end-to-end verification before the sprint is considered done.

- [X] T016 [P] Run `./gradlew detekt ktlintCheck` from the repo root and fix all reported violations in `src/main/kotlin/dev/zahaand/projectscan/ui/`
- [ ] T017 [PENDING MANUAL] Run `./gradlew runIde`; in the sandboxed IDE open any JVM (Maven or Gradle) project; execute the full quickstart.md verification checklist: (1) open View → Tool Windows → Project Scan, (2) confirm pre-scan state, (3) click Scan and confirm button disables, (4) confirm 6 sections in correct order after scan, (5) confirm first 5 expanded / Constitution collapsed, (6) click Copy on an Ok section and paste into text editor; confirm SC-001–SC-006 are all satisfied

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — **BLOCKS all user story phases**
- **US1 (Phase 3)**: Depends on Phase 2 completion
- **US2 (Phase 4)**: Depends on Phase 3 (T010 must exist before T013 can wire the copy action)
- **US3 (Phase 5)**: Depends on Phase 2 (T009 must exist before T014 reviews it)
- **US4 (Phase 6)**: Depends on Phase 3 (T011 must exist before T015 reviews it)
- **Polish (Phase 7)**: Depends on all user story phases being complete

### User Story Dependencies

- **US1 (P1)**: Requires Phase 2 — no dependency on other user stories
- **US2 (P2)**: Requires T010 (SectionPanel) from US1
- **US3 (P3)**: Requires T009 (ScanResultRenderer) from Phase 2
- **US4 (P4)**: Requires T011 (ProjectScanPanel) from US1

### Within Each Phase

- T007 and T008 within Phase 2 are independent and can run in parallel
- T010, T011, T012 within Phase 3 must run in this order (SectionPanel → ProjectScanPanel → Factory)
- T002 and T003 within Phase 1 are independent and can run in parallel with T001

### Parallel Opportunities

- **Phase 1**: T002 and T003 can run in parallel alongside T001
- **Phase 2**: T007 and T008 can run in parallel; T005 and T006 can run in parallel
- **Polish**: T016 can run while T017 is being prepared

---

## Parallel Example: Phase 2

```
# These four tasks have no file conflicts and can run in parallel:
T005  Create ProjectScanBundle.properties
T006  Create ProjectScanBundle.kt
T007  Create ScanPanelState.kt
T008  Create UiSection.kt

# Then T009 (ScanResultRenderer) after all four complete — depends on T007 + T008
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup & Demo Cleanup
2. Complete Phase 2: Foundational — CRITICAL, blocks everything
3. Complete Phase 3: US1 — full scan and view flow
4. **STOP and VALIDATE**: Run `./gradlew runIde`, click Scan, confirm 6 sections appear
5. Demo if ready

### Incremental Delivery

1. Phase 1 + Phase 2 → Foundation ready
2. Phase 3 (US1) → End-to-end scan works → Demo (MVP!)
3. Phase 4 (US2) → Clipboard copy working
4. Phase 5 (US3) → Empty/Error distinction confirmed
5. Phase 6 (US4) → Pre-scan state confirmed
6. Phase 7 → Code style clean, quickstart checklist green

---

## Notes

- **No tests generated** — spec explicitly states no mandatory unit coverage for `:ui`; manual IDE run via `./gradlew runIde` is the primary validation
- **[P]** tasks operate on different files and have no shared state — safe to run concurrently
- **Story labels** map each task to the user story it satisfies for traceability
- US3 (T014) and US4 (T015) are review-and-confirm tasks; if a discrepancy is found during review, fix it in-place in the relevant file before marking complete
- All strings MUST come from `ProjectScanBundle` — no hardcoded user-visible strings anywhere in `:ui`
- The `ui → prompt` dependency is an accepted deviation (D1) — do not add any comments or TODO markers about it in the source code; the disposition is recorded in `plan.md` and `spec.md`
