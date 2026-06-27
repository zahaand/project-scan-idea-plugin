# Feature Specification: Output Readability for Large Projects

**Feature Branch**: `006-output-readability`
**Created**: 2026-06-27
**Status**: Draft
**Input**: User description: "Sprint 6 — output readability. Improve the readability of the plugin's output on large projects (monorepos with 80+ modules and ~250 dependencies)."

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Tech Stack section is readable on large projects (Priority: P1)

A developer scans a monorepo with 250+ declared dependencies. Currently the Tech Stack section in both the Constitution prompt and the Tool Window is a flat list of 250+ `groupId:artifactId:version` lines with no organisation. After this change the same section groups entries by `groupId`, and shows a shared version once at the group header when all artifacts in the group share it.

**Why this priority**: Tech Stack is the first section the developer reads and the one most likely to be pasted into the Constitution. Readability here has the highest leverage.

**Independent Test**: Can be fully tested by building a `ScanResult` with >10 distinct `groupId` groups (some with shared versions, some with mixed), running `PromptGenerator` and `ScanResultRenderer`, and asserting the rendered output groups entries and deduplicates versions.

**Acceptance Scenarios**:

1. **Given** a `StackInfo` with 5 `org.springframework` artifacts all at version `6.1.4`, **When** either consumer renders the Tech Stack section, **Then** the output contains one group header showing `org.springframework:* @ 6.1.4` and does NOT repeat `6.1.4` on each artifact line.
2. **Given** a `StackInfo` where two artifacts in the same `groupId` have different versions, **When** either consumer renders the Tech Stack section, **Then** both artifact lines appear with their individual versions.
3. **Given** an empty `StackInfo.dependencies`, **When** either consumer renders the Tech Stack section, **Then** build-system / JDK / language-level metadata is still shown unchanged and no dependency section appears.

---

### User Story 2 — Project Structure section surfaces version discrepancies instead of repeating dependency lists (Priority: P1)

A developer scans a monorepo with 80 modules. Currently the Project Structure section dumps every module's full declared-dependency list — 80 × N lines. After this change the section shows the module graph (inter-module links), the package organisation pattern, and root packages, plus a focused "Version discrepancies" sub-block listing only artifacts that appear at different versions across modules.

**Why this priority**: Equal to P1 because this section currently produces the most output volume and the version-discrepancy signal is genuinely actionable for the developer.

**Independent Test**: Can be fully tested by building a `ScanResult` with modules that share some dependencies at the same version and disagree on one, running both consumers, and asserting: no per-module dependency lists appear; the discrepancy artifact and its per-module versions appear in the "Version discrepancies" block; the version that is consistent across all modules does NOT appear in the discrepancy block.

**Acceptance Scenarios**:

1. **Given** modules `api` and `core` both declare `com.fasterxml.jackson.core:jackson-databind:2.17.0`, **When** either consumer renders Project Structure, **Then** `jackson-databind` does NOT appear in the Version discrepancies block.
2. **Given** module `api` declares `org.mapstruct:mapstruct:1.5.5` and module `core` declares `org.mapstruct:mapstruct:1.6.0`, **When** either consumer renders Project Structure, **Then** the Version discrepancies block contains `org.mapstruct:mapstruct → {api: 1.5.5, core: 1.6.0}`.
3. **Given** all modules agree on every dependency version, **When** either consumer renders Project Structure, **Then** the Version discrepancies block is either omitted or shows an explicit "none" notice.
4. **Given** a `StructureInfo` with modules that have `moduleDependencies`, **When** either consumer renders Project Structure, **Then** the inter-module dependency graph (which module depends on which) is shown.

---

### User Story 3 — Testing section shows deduplicated frameworks and normalised source-root counts (Priority: P2)

A developer scans a monorepo where every one of the 80 modules declares `junit-jupiter` — so the current Testing section lists `Framework: JUnit Jupiter 5.10.2` 80 times, followed by 80 absolute source-root paths. After this change the section shows one deduplicated line per distinct `(framework name, version)` pair, and source roots are collapsed to relative-path templates with module counts (e.g. `src/test/java — 78 modules`).

**Why this priority**: The current duplication makes the section nearly unusable. P2 because Tech Stack / Structure affect the Constitution prompt more directly, but Testing is still broken on large projects.

**Independent Test**: Can be fully tested by building a `TestInfo` with 80 identical `TestFramework` entries and 80 absolute source-root paths that share the same relative suffix, running both consumers, and asserting: exactly one framework line appears; source roots collapse to one template line with count 80; absolute path prefixes are not shown.

**Acceptance Scenarios**:

1. **Given** `TestInfo.frameworks` contains 80 entries for `JUnit Jupiter 5.10.2`, **When** either consumer renders the Testing section, **Then** exactly one `Framework: JUnit Jupiter 5.10.2` line appears.
2. **Given** `TestInfo.sourceRoots` contains `/home/ci/workspace/myapp/api/src/test/java` and `/home/ci/workspace/myapp/core/src/test/java`, **When** either consumer renders the Testing section, **Then** the output shows `src/test/java — 2 modules` (or equivalent) and does NOT show absolute paths.
3. **Given** `TestInfo.sourceRoots` contains paths with two distinct relative suffixes (`src/test/java` and `src/test/kotlin`), **When** either consumer renders the Testing section, **Then** two template lines appear, each with its own module count.
4. **Given** `TestInfo.frameworks` contains two distinct frameworks (e.g. JUnit + Mockito at different versions), **When** either consumer renders the Testing section, **Then** both appear exactly once.

---

### Edge Cases

- What happens when a module has no declared dependencies? The module still appears in the module list; it contributes nothing to the Version discrepancies block.
- What happens when `resolvedVersion` is `null` on a dependency? The artifact is excluded from version-discrepancy detection (version unknown) and rendered without a version in Tech Stack grouping.
- What happens when all source roots in `TestInfo` are already relative (no common absolute prefix)? They are used as-is for grouping — no normalization error occurs.
- What happens when `TestInfo.sourceRoots` is empty? The source-roots line is omitted from the output (unchanged from current behaviour).
- What happens when only one module in the entire project uses a dependency? That artifact cannot have a cross-module discrepancy and is not shown in the Version discrepancies block regardless of version.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Both `:prompt` (PromptGenerator) and `:ui` (ScanResultRenderer) MUST group `StackInfo.dependencies` by `groupId` when rendering the Tech Stack section.
- **FR-002**: When all artifacts within a `groupId` group share the same `resolvedVersion`, both consumers MUST render that version once at the group level and omit per-artifact version repetition.
- **FR-003**: When artifacts within a `groupId` group have differing `resolvedVersion` values, both consumers MUST render each artifact with its individual version.
- **FR-004**: Both consumers MUST NOT include per-module external dependency lists in the Project Structure section.
- **FR-005**: Both consumers MUST render the inter-module dependency graph (`Module.moduleDependencies`) in the Project Structure section.
- **FR-006**: Both consumers MUST compute and render a Version discrepancies sub-block in the Project Structure section, listing only artifacts whose `resolvedVersion` differs across the modules that declare them, in the form `groupId:artifactId → {moduleName: version, ...}`.
- **FR-007**: Artifacts that appear at the same version in every module that declares them MUST NOT appear in the Version discrepancies sub-block.
- **FR-008**: Both consumers MUST deduplicate `TestInfo.frameworks` by `(name, version)` so each distinct framework appears exactly once in the Testing section.
- **FR-009**: Both consumers MUST normalise `TestInfo.sourceRoots` to relative path templates by stripping the longest common absolute prefix, then collapse identical relative paths to a single line with a count of contributing modules.
- **FR-010**: The `:scan` and `:model` modules MUST NOT be modified in this sprint.
- **FR-011**: Empty/Error `SectionResult` handling for all three sections MUST remain unchanged ("not detected" / "not available").
- **FR-012**: All formatting changes to `PromptGenerator` MUST be covered by unit tests.

### Key Entities

- **Dependency** (`groupId`, `artifactId`, `resolvedVersion`): the atomic unit of grouping in Tech Stack and version-discrepancy detection.
- **Module** (`name`, `declaredDependencies`, `moduleDependencies`): the source of per-module dependency data used to detect discrepancies and to render the module graph.
- **TestFramework** (`name`, `version`): deduplicated by `(name, version)` pair in the Testing section.
- **Source root template**: a relative path suffix derived from normalising absolute source-root paths; aggregated with a module count.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On a 80-module project with 250 dependencies across 40 `groupId` groups, the Tech Stack section output is reduced to at most 60 lines (group headers + per-artifact lines only where versions diverge), down from 250+.
- **SC-002**: On a 80-module project, the Project Structure section no longer lists per-module external dependency lines; only module names, inter-module links, root packages, and version discrepancies appear.
- **SC-003**: On a project where all 80 modules declare the same 3 test frameworks, the Testing section shows exactly 3 framework lines regardless of module count.
- **SC-004**: On a project with 80 modules each contributing one `src/test/java` source root, the Testing section shows exactly one source-root line with `— 80 modules`.
- **SC-005**: All new `PromptGenerator` formatting logic is verified by automated tests; no regression in existing `PromptGenerator` tests.
- **SC-006**: Both consumers (`:prompt` and `:ui`) produce consistent section content for the same `ScanResult` — the same grouping and deduplication logic is applied in both.

## Assumptions

- `StructureInfo` does not carry an explicit "package organisation pattern" field at this time; the existing `packageSegments` field covers that signal and will be rendered if non-empty.
- Source roots in `TestInfo.sourceRoots` are stored as absolute paths on the scanning machine; the consumer strips the longest common prefix to derive relative templates.
- A "module count" per source-root template is inferred by counting how many raw entries in `TestInfo.sourceRoots` normalize to that template — no per-module attribution is stored in the model.
- Dependencies with `resolvedVersion == null` are treated as "version unknown" and excluded from version-discrepancy detection; they are still rendered in Tech Stack without a version.
- The `:ui` module's `ScanResultRenderer` is not covered by unit tests at the same level as `:prompt`; the spec requires tests for `:prompt` changes and leaves `:ui` testing to manual verification or a future sprint.
- Both consumers will share the same folding algorithm but via independent implementations (not a shared utility module), to avoid coupling `:prompt` and `:ui` — unless the implementation naturally leads to a shared internal helper within one module.
