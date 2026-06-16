# Research: UI Tool Window

**Feature**: `005-ui-tool-window`
**Phase**: 0 — Research
**Date**: 2026-06-16

## 1. IntelliJ Platform Collapsible Section Mechanism

**Decision**: Use a custom expandable panel built from standard Swing/IntelliJ components — a `JPanel` with a `JButton` toggle (labelled with `▶`/`▼` or via `AllIcons`) that shows/hides a child `JPanel` body.

**Rationale**: The IntelliJ Platform's `CollapsiblePanel` class lives in internal or platform-UI modules that are not consistently available as public API across the target version range. The spec says "standard IntelliJ Platform collapsible panel mechanism; no custom expand/collapse widgets are built" — this is satisfied by using the platform's standard Swing building blocks (`JBPanel`, `JButton`, `JBLabel`) with a simple `isVisible` toggle on the body panel, styled via `JBUI`. This is the most common approach in open-source IntelliJ plugins.

**Alternatives considered**:
- `com.intellij.ui.CollapsiblePanel` — internal API, not publicly exported.
- `DetailsComponent` / `TabbedPaneWrapper` — wrong semantic; designed for settings dialogs.

---

## 2. ScanService Wiring (No DI Framework)

**Decision**: Wire `ScanService` and all adapters directly inside `ProjectScanToolWindowFactory.createToolWindowContent()`, using the `Project` instance provided by the platform at factory call time.

**Rationale**: The spec requires no DI framework. IntelliJ Platform provides `Project` to `createToolWindowContent()`. All six IJ adapters take `Project` as their only constructor argument. The linter config parsers are stateless and take no constructor arguments. The wiring is four lines of code and is easily readable.

**Wiring**:
```kotlin
val buildSystemPort = IjBuildSystemAdapter(project)
val dependencyPort  = IjDependencyAdapter(project)
val styleSourcePort = IjStyleSourceAdapter(project)
val linterPort      = IjLinterAdapter(project)
val testInfoPort    = IjTestInfoAdapter(project)
val modulePort      = IjModuleStructureAdapter(project)
val parsers         = mapOf("checkstyle" to CheckstyleConfigParser(), "pmd" to PmdConfigParser())
val scanService     = ScanService(buildSystemPort, dependencyPort, styleSourcePort,
                                  linterPort, parsers, testInfoPort, modulePort)
val promptGenerator = PromptGenerator()
val baselineRules   = BaselineRuleProvider.rules
```

**Alternatives considered**:
- IntelliJ Service (`@Service`) — adds XML registration overhead and abstraction not needed for a single-use, single-instance factory.

---

## 3. Section Body Rendering

**Decision**: Write a dedicated `ScanResultRenderer` object in `:ui` with one function per section type; do not reuse `PromptGenerator`'s private methods.

**Rationale**: The spec requires the five collection section bodies to display "human-readable text rendered from the corresponding SectionResult". `PromptGenerator`'s rendering functions are `private` and their output is formatted for the generated prompt (e.g., the Linters section is blended with baseline rules into "Core Principles"). The UI needs distinct per-section text, especially for Linters which is not a standalone block in the prompt. A thin renderer in `:ui` is cleaner than making PromptGenerator's internals accessible.

**Alternatives considered**:
- Expose `buildTechStackBlock()` et al. as `internal` — couples `:prompt` API surface to a UI implementation detail; fragile.
- Reuse PromptBlock from ConstitutionPrompt — not possible for Linters since it has no standalone block.

---

## 4. EDT-Safe Result Passing in Task.Backgroundable

**Decision**: Capture `ScanResult` and `ConstitutionPrompt` into `@Volatile` fields declared inside the `Task.Backgroundable` anonymous class; read them in `onSuccess()`.

**Rationale**: `Task.Backgroundable.run()` executes on a background thread. `onSuccess()` runs on the EDT. The simplest thread-safe handoff is a `@Volatile var` or a nullable `AtomicReference` holding the result. `onFinished()` always runs on EDT (after both success and failure paths) and is the correct place to re-enable the "Scan" button.

**Pattern**:
```kotlin
object : Task.Backgroundable(project, title, false) {
    @Volatile private var scanResult: ScanResult? = null
    @Volatile private var constitutionPrompt: ConstitutionPrompt? = null

    override fun run(indicator: ProgressIndicator) {
        scanResult = scanService.scan()
        constitutionPrompt = promptGenerator.generate(scanResult!!, baselineRules)
    }

    override fun onSuccess() {
        panel.showResults(scanResult!!, constitutionPrompt!!)
    }

    override fun onThrowable(error: Throwable) {
        panel.revertToPreScan()
        Notifications.Bus.notify(
            Notification("ProjectScan", "Scan Failed",
                         error.message ?: "Unknown error", NotificationType.ERROR),
            project
        )
    }

    override fun onFinished() {
        panel.setScanButtonEnabled(true)
    }
}
```

**Alternatives considered**:
- `CompletableFuture` — unnecessary overhead; `Task.Backgroundable` already has lifecycle hooks.
- Kotlin coroutines — explicitly excluded by spec Assumptions.

---

## 5. Error Notification Balloon (FR-006a)

**Decision**: Use `Notifications.Bus.notify()` with notification group `"ProjectScan"` and `NotificationType.ERROR`.

**Rationale**: This is the standard IntelliJ SDK balloon notification API. The group ID `"ProjectScan"` is self-consistent and requires no additional plugin.xml registration for basic notifications (since IntelliJ 2023.x, unregistered notification groups produce a balloon silently).

---

## 6. Constitution Dependency Conflict — ui → prompt

**Observation**: The project constitution states "`scan`, `prompt`, and `ui` MUST NOT depend on each other." However, the feature spec explicitly requires `ConstitutionPrompt.render()` (defined in `:prompt`) to be called from `:ui` for FR-013. There is no way to satisfy FR-013 without `:ui` depending on `:prompt`.

**Resolution**: This sprint accepts the dependency `ui → prompt` as an intentional deviation from the stated constitution rule. The `:ui` module is the composition root that assembles all lower modules; the constitution rule was written to prevent lateral coupling (e.g., `:scan` depending on `:prompt`), not to prohibit the assembly root from depending on the modules it assembles. The deviation is tracked in `plan.md` (Complexity Tracking). A future constitution amendment should clarify that `:ui` is exempt from this lateral isolation rule.

---

## 7. Message Bundle Key Design

All strings are kept in `ProjectScanBundle.properties`. Key schema:

| Key | Value |
|-----|-------|
| `toolwindow.ProjectScan.title` | `Project Scan` |
| `toolwindow.ProjectScan.scan.button` | `Scan` |
| `toolwindow.ProjectScan.hint` | `Click "Scan" to analyze the project.` |
| `section.TechStack.title` | `Tech Stack` |
| `section.CodeStyle.title` | `Code Style` |
| `section.Linters.title` | `Linters` |
| `section.Tests.title` | `Tests` |
| `section.Structure.title` | `Project Structure` |
| `section.Constitution.title` | `Constitution Prompt` |
| `section.state.empty` | `Not detected` |
| `section.state.error` | `Not available` |
| `section.state.error.with.cause` | `Not available (cause: {0})` |
| `section.copy.button` | `Copy` |
| `scan.error.notification.title` | `Project Scan Failed` |
