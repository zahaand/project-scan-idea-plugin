# Data Model: UI Tool Window

**Feature**: `005-ui-tool-window`
**Phase**: 1 — Design
**Date**: 2026-06-16

## Domain Entities (consumed by UI)

These types are defined in lower modules; the `:ui` module consumes them read-only.

### ScanResult (`:model`)

```
ScanResult
├── stack:     SectionResult<StackInfo>
├── codeStyle: SectionResult<CodeStyleInfo>
├── linters:   SectionResult<LinterInfo>
├── tests:     SectionResult<TestInfo>
└── structure: SectionResult<StructureInfo>
```

### SectionResult<T> (`:model`) — discriminated union

| Variant | Fields | Copy button | Body text |
|---------|--------|-------------|-----------|
| `Ok<T>` | `data: T` | Enabled | Rendered from `data` |
| `Empty` | — | Disabled | `section.state.empty` |
| `Error` | `cause: String?` | Disabled | `section.state.error[.with.cause]` |

### ConstitutionPrompt (`:prompt`)

```
ConstitutionPrompt
└── render(): String   // full Markdown prompt — displayed verbatim in Constitution Prompt section
```

---

## UI State Model (`:ui`)

These types exist only within the `:ui` module and are not shared with lower modules.

### ScanPanelState (sealed class)

Controls what the main panel renders.

| State | Contents |
|-------|----------|
| `PreScan` | Scan button + hint text; no section panels |
| `PostScan(data: PostScanData)` | Scan button (re-enabled) + six SectionPanel rows |

### PostScanData (data class)

Carries the scan results after a successful scan. Built on the EDT in `onSuccess()`.

```
PostScanData
├── scanResult:         ScanResult
└── constitutionPrompt: ConstitutionPrompt
```

### UiSection (data class)

A flattened view of one section ready for rendering. Built by `ScanResultRenderer`.

| Field | Type | Description |
|-------|------|-------------|
| `title` | String | Section header label (from message bundle) |
| `body` | String | Rendered display text |
| `copyEnabled` | Boolean | Whether the Copy button is interactive |
| `collapsedByDefault` | Boolean | Initial collapse state |

**Mapping** (produced by `ScanResultRenderer.render(scanResult, constitutionPrompt, bundle)`):

| # | Title | Source | copyEnabled | collapsedByDefault |
|---|-------|--------|-------------|-------------------|
| 1 | Tech Stack | `scanResult.stack` | `is Ok` | false |
| 2 | Code Style | `scanResult.codeStyle` | `is Ok` | false |
| 3 | Linters | `scanResult.linters` | `is Ok` | false |
| 4 | Tests | `scanResult.tests` | `is Ok` | false |
| 5 | Project Structure | `scanResult.structure` | `is Ok` | false |
| 6 | Constitution Prompt | `constitutionPrompt.render()` | always true | true |

---

## ScanResultRenderer — rendering rules

Object in `dev.zahaand.projectscan.ui`. All methods are pure functions.

### Tech Stack (`SectionResult<StackInfo>`)

```
Ok  → bullet list: buildSystem, jdkVersion, languageLevel, then each dependency (groupId:artifactId:version?)
      if all fields null/empty → section.state.empty
Empty → section.state.empty
Error → section.state.error (+ cause if present)
```

### Code Style (`SectionResult<CodeStyleInfo>`)

```
Ok  → bullet list of StyleSource entries: "TYPE: path"
      if sources empty → section.state.empty
Empty → section.state.empty
Error → section.state.error (+ cause if present)
```

### Linters (`SectionResult<LinterInfo>`)

```
Ok  → bullet list of ActiveRule entries: "ruleId [tool] (SEVERITY)"
      if activeRules empty → section.state.empty
Empty → section.state.empty
Error → section.state.error (+ cause if present)
```

### Tests (`SectionResult<TestInfo>`)

```
Ok  → bullet list: frameworks (name + version?), sourceRoots, namingSuffixes, coverageThreshold
      if all empty → section.state.empty
Empty → section.state.empty
Error → section.state.error (+ cause if present)
```

### Project Structure (`SectionResult<StructureInfo>`)

```
Ok  → bullet list: modules (name, declaredDependencies, moduleDependencies), rootPackages
      if all empty → section.state.empty
Empty → section.state.empty
Error → section.state.error (+ cause if present)
```

### Constitution Prompt (`ConstitutionPrompt`)

```
Always → constitutionPrompt.render()   // verbatim, no processing
```

---

## Component Relationships

```
ProjectScanToolWindowFactory
  └── creates → ProjectScanPanel(project, scanService, promptGenerator, baselineRules)

ProjectScanPanel
  ├── state: ScanPanelState  (PreScan initially)
  ├── scanButton: JButton
  ├── hintLabel: JBLabel
  └── (post-scan) → [SectionPanel × 6]

SectionPanel
  ├── titleButton: JButton  (acts as expand/collapse toggle)
  ├── bodyPanel: JBPanel    (hidden when collapsed)
  ├── bodyLabel: JBTextArea (non-editable)
  └── copyButton: JButton

ScanResultRenderer
  └── render(scanResult, constitutionPrompt, bundle) → List<UiSection>  (always 6 items, ordered)
```

---

## Background Task Data Flow

```
[EDT] User clicks Scan
  → scanButton.isEnabled = false
  → Task.Backgroundable.queue(project)

[BGT] Task.run()
  → scanResult = ScanService.scan()
  → constitutionPrompt = PromptGenerator.generate(scanResult, baselineRules)

[EDT] Task.onSuccess()
  → panel.showResults(scanResult, constitutionPrompt)
  → builds List<UiSection> via ScanResultRenderer
  → replaces panel content with SectionPanel × 6

[EDT] Task.onThrowable()
  → panel.revertToPreScan()
  → Notifications.Bus.notify(error balloon)

[EDT] Task.onFinished()  ← always runs last, on both paths
  → scanButton.isEnabled = true
```
