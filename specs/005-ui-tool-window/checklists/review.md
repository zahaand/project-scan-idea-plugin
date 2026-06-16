# PR-Review Gate Checklist: UI Tool Window

**Purpose**: Validate requirement quality across all four risk clusters before implementation starts
**Created**: 2026-06-16
**Feature**: [spec.md](../spec.md) | [plan.md](../plan.md)
**Depth**: PR-review gate (thorough)
**Scope**: 005 spec + cross-sprint consistency with lower-module contracts

---

## UI State & Interaction Requirements (FR-001–FR-015)

- [ ] CHK001 — Is the tool window icon requirement (FR-002) specific enough? "Distinct from the Marketplace plugin icon" names only an exclusion — is the actual icon identifier, path, or visual description specified anywhere? [Clarity, Spec §FR-002, Gap]
- [ ] CHK002 — Is the scope of the scroll pane (FR-003) defined precisely? The spec says "the panel MUST be vertically scrollable via a single scroll pane" but does not state whether the Scan button sits inside or outside the scroll container. [Clarity, Spec §FR-003]
- [ ] CHK003 — Is "background progress indicator" (FR-005) specified with enough detail for implementation? The spec mandates one exists but does not define its type (indeterminate progress bar, spinner, status text) or location. [Clarity, Spec §FR-005]
- [ ] CHK004 — Are the six section titles specified verbatim, or just named descriptively in FR-008? If titles come from the message bundle (FR-016), is the mapping between FR-008 names and bundle keys defined in the spec or left to the plan? [Completeness, Spec §FR-008, §FR-016]
- [ ] CHK005 — Is the rendering format for the five collection section bodies (FR-009) specified? "Human-readable text body rendered from the corresponding SectionResult" describes intent but not format — are bullet lists, plain text, key-value pairs, or other layouts required? [Clarity, Spec §FR-009, Gap]
- [ ] CHK006 — Does FR-008's definition of "after a scan completes" account for the exception path? FR-006a states the panel reverts to pre-scan state on a top-level exception — is "completes" explicitly scoped to successful completion only? [Consistency, Spec §FR-008 vs §FR-006a]
- [ ] CHK007 — Is FR-014 ("Copy button MUST always be enabled after a scan") consistent with the edge case where all five collection sections are Empty? The spec's edge-case note says "the prompt section is still rendered and its Copy button is enabled" but only for the no-source-files case — is this general enough? [Consistency, Spec §FR-014 vs §Edge Cases]
- [ ] CHK008 — Is the re-scan scenario (user clicks Scan after a prior scan has already displayed results) addressed in the requirements? None of the FRs or user stories describe whether prior results are cleared immediately on re-scan or only replaced on completion. [Coverage, Gap]
- [ ] CHK009 — Are requirements defined for the section expand/collapse state after a re-scan in the same session? FR-008 specifies initial collapse states but not whether they reset on a second scan. [Coverage, Gap]
- [ ] CHK010 — Is "no UI freeze" in SC-003 quantified? "IDE remains fully responsive" is described qualitatively; no EDT-block threshold (e.g., <16ms, <100ms) is specified, making the criterion difficult to measure objectively. [Measurability, Spec §SC-003]
- [ ] CHK011 — Can SC-004 ("visually and textually distinguishable without any tooltip") be objectively verified? The spec does not define measurable visual criteria (color, icon, text prefix) that distinguish the Empty and Error states. [Measurability, Spec §SC-004]
- [ ] CHK012 — Is FR-016's scope ("all UI strings") defined precisely? Does it cover tool window IDs and notification group IDs registered in plugin.xml, or only displayed text? [Clarity, Spec §FR-016]
- [ ] CHK013 — Are accessibility requirements (keyboard navigation, screen reader labels) defined for any interactive element in the tool window? [Coverage, Gap]

---

## Background Task Safety Requirements (FR-005, FR-006, FR-006a)

- [ ] CHK014 — Is it explicitly required that BOTH the scan AND the prompt generation run off the EDT? FR-005 says "run the collection and prompt generation in the background" which implies both, but FR-006's re-enable condition ("once the background task completes") is singular — are these the same task? [Completeness, Spec §FR-005, §FR-006]
- [ ] CHK015 — Is "top-level exception" (FR-006, FR-006a) formally defined? The spec distinguishes it from a per-section `SectionResult.Error` but does not specify whether it includes `java.lang.Error` subtypes (e.g., `OutOfMemoryError`, `StackOverflowError`) or only `Exception` subtypes. [Clarity, Spec §FR-006a]
- [ ] CHK016 — Is "revert to the pre-scan state" (FR-006a) defined precisely? Does it mean the six section panels are removed from the UI, leaving only the Scan button and hint text — identical to the first-launch state per FR-007? [Clarity, Spec §FR-006a vs §FR-007]
- [ ] CHK017 — Are requirements defined for the failure case where `BaselineRuleProvider` throws a `BaselineLoadException` before the scan begins? Is this considered a top-level exception per FR-006a, or is it out of scope? [Coverage, Gap]
- [ ] CHK018 — Is a thread-safety requirement stated for UI panel updates triggered by `Task.Backgroundable` lifecycle callbacks? The spec requires background execution but does not mandate that all UI mutations occur on the EDT specifically. [Completeness, Gap]
- [ ] CHK019 — Are requirements defined for what happens if the IDE is closed or the plugin is unloaded while a scan is in progress (mid-scan lifecycle)? The edge case covers "tool window closed and reopened mid-scan" but not full IDE shutdown during a scan. [Coverage, Spec §Edge Cases, Gap]

---

## Demo Scaffold Removal Requirements (FR-017, FR-018)

- [ ] CHK020 — Does FR-017 enumerate all demo-generated files? The spec lists `MyToolWindowFactory`, `MyMessageBundle`, and the demo `.properties` file. The current `plugin.xml` also references `messages.MyMessageBundle` in `<resource-bundle>` and `dev.zahaand.MyToolWindowFactory` in `<toolWindow factoryClass>` — are these `plugin.xml` entries covered by FR-017 or only implied? [Completeness, Spec §FR-017]
- [ ] CHK021 — Does FR-018's package constraint ("no code MUST remain under the root `dev.zahaand` package without the `.projectscan` sub-package") cover resource files and `plugin.xml` declarations, or only `.kt` source files? [Clarity, Spec §FR-018]
- [ ] CHK022 — Is there a requirement specifying the project-owned bundle name that replaces `MyMessageBundle`? FR-017 says "replace with project-owned equivalents" but does not name the replacement bundle, leaving the bundle identifier undefined in the spec. [Clarity, Spec §FR-017, Gap]
- [ ] CHK023 — Does SC-005 ("no reference to `MyToolWindowFactory`, `MyMessageBundle`, or the demo `.properties` file remains anywhere in the plugin's source or configuration") define what "configuration" includes? Plugin.xml, Gradle build files, `.run` configs, and Kotlin source are all plausible candidates. [Clarity, Spec §SC-005]
- [ ] CHK024 — Is FR-018's constraint scoped to the root `:ui` module only, or does it apply to all submodules (`:scan`, `:model`, `:baseline`, `:prompt`)? All existing submodule code already uses `dev.zahaand.projectscan.*`, so this is likely UI-scoped, but the requirement text does not say so explicitly. [Clarity, Spec §FR-018]

---

## Constitution Dependency Conflict (ui → prompt)

- [ ] CHK025 — Is the `ui → prompt` dependency conflict resolved at the spec level or only at the plan level? FR-013 requires `ConstitutionPrompt.render()` from `:prompt`, but the spec does not acknowledge the conflict with the constitution's dependency rule. The plan's Complexity Tracking carries the disposition — should the spec carry it too? [Completeness, Spec §FR-013 vs plan.md §Complexity Tracking]
- [ ] CHK026 — Is there a requirement (or explicit exemption) in the spec or constitution for how `ConstitutionPrompt` reaches the `:ui` module given the prohibition on cross-consumer dependencies? [Clarity, Spec §FR-013 vs constitution §Dependency Direction, Gap]
- [ ] CHK027 — Is the plan's interpretation — "the constitution rule targets lateral coupling, not the composition root" — validated against the constitution or only inferred? Is the author's intent documented somewhere that confirms `:ui` is the intended composition root and exempt from lateral isolation? [Consistency, research.md §6 vs constitution §I]
- [ ] CHK028 — Is there a requirement to amend the constitution to reflect the accepted `ui → prompt` dependency before implementation is merged? The plan notes it as technical debt but does not specify a timeline or gate. [Gap, plan.md §Complexity Tracking]

---

## Cross-Sprint Consistency (005 vs Lower Modules)

- [ ] CHK029 — Are the `SectionResult.Empty` and `SectionResult.Error` semantics as defined in `:model` aligned with how FR-011 and FR-012 describe the "not detected" vs "not available" distinction? The model defines `Empty` and `Error(cause: String?)` — do the spec's placeholder messages exactly match what `:scan` collectors produce for each variant? [Consistency, Spec §FR-011, §FR-012 vs model/ScanResult.kt]
- [ ] CHK030 — Does the `:scan` module's per-section `try-catch` design (producing `SectionResult.Error` per section) align with the `:ui` spec's assumption that a "top-level exception" (FR-006a) is a distinct failure mode thrown by `ScanService.scan()` itself? Is the boundary between these two failure paths verified in the `:scan` spec or tests? [Consistency, Spec §FR-006a vs scan/ScanService.kt]
- [ ] CHK031 — Is it specified that `BaselineRuleProvider.rules` (the full unfiltered list) must be passed to `PromptGenerator.generate()`? The spec says FR-013 requires `ConstitutionPrompt.render()` verbatim, but the `PromptGenerator` filters baseline rules by `minJavaLevel` based on the scanned stack — if `:ui` passed a pre-filtered list, the output would differ. Is the "unfiltered" assumption made explicit? [Consistency, Spec §FR-013 vs prompt/PromptGenerator.kt]
- [ ] CHK032 — Is the order of arguments to `ScanService` (six ports + parsers map) specified or validated anywhere? The `:ui` module must wire adapters in the exact order expected by `ScanService`'s constructor — is there a contract, factory, or companion object in `:scan` that enforces correct wiring? [Consistency, Spec §FR-005 vs scan/ScanService.kt, Gap]

---

## Acceptance Criteria Quality

- [ ] CHK033 — Do the acceptance scenarios in US1–US4 cover every FR-0XX requirement? A mapping of FR → scenario is absent from the spec — are there FRs (e.g., FR-003 scroll pane, FR-016 string bundle, FR-017 cleanup) that have no corresponding scenario? [Completeness, Spec §FR-001–018 vs §US1–US4]
- [ ] CHK034 — Are US4 scenario 2 ("all six section panels appear and remain visible for the duration of the current IDE session") and the edge case "tool window closed and reopened mid-scan" internally consistent? If the window is reopened mid-scan and the scan later completes, do six panels appear? [Consistency, Spec §US4.2 vs §Edge Cases]
- [ ] CHK035 — Is SC-006 ("all six sections appear in the correct declared order on every scan") testable without a reference fixture defining "correct order"? FR-008 defines the order, but SC-006 references it only by name — should SC-006 enumerate the order explicitly? [Measurability, Spec §SC-006 vs §FR-008]
