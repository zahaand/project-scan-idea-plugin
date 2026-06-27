# Feature Specification: Output Readability for Large Projects

**Feature Branch**: `006-output-readability`
**Created**: 2026-06-27
**Status**: Updated 2026-06-27 (amendments from checklist CHK001–CHK034)
**Input**: User description: "Sprint 6 — output readability. Improve the readability of the plugin's output on large projects (monorepos with 80+ modules and ~250 dependencies)."

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Tech Stack section is readable on large projects (Priority: P1)

A developer scans a monorepo with 250+ declared dependencies. Currently the Tech Stack section in both the Constitution prompt and the Tool Window is a flat list of 250+ `groupId:artifactId:version` lines with no organisation. After this change the same section groups entries by `groupId`, and shows a shared version once at the group header when all artifacts in the group share it.

**Why this priority**: Tech Stack is the first section the developer reads and the one most likely to be pasted into the Constitution. Readability here has the highest leverage.

**Independent Test**: Can be fully tested by building a `ScanResult` with >10 distinct `groupId` groups (some with shared versions, some with mixed), running `PromptGenerator` and `ScanResultRenderer`, and asserting that multi-artifact groups with a shared non-null version render a single group header with `artifactId`-only lines, while single-artifact groups and mixed/null-version groups render per-artifact format without a header.

**Acceptance Scenarios**:

1. **Given** a `StackInfo` with 5 `org.springframework` artifacts all at version `6.1.4`, **When** either consumer renders the Tech Stack section, **Then** the output contains one group header showing `org.springframework:* @ 6.1.4` and each artifact appears as its `artifactId` only (without repeating `6.1.4`).
2. **Given** a `StackInfo` where two artifacts in the same `groupId` have different versions, **When** either consumer renders the Tech Stack section, **Then** both artifact lines appear with their individual versions (per-artifact format, no group header).
3. **Given** an empty `StackInfo.dependencies`, **When** either consumer renders the Tech Stack section, **Then** build-system / JDK / language-level metadata is still shown unchanged and no dependency section appears.
4. **Given** a `groupId` group containing exactly one artifact (regardless of whether `resolvedVersion` is set), **When** either consumer renders the Tech Stack section, **Then** the artifact is rendered in per-artifact format without a group header.

---

### User Story 2 — Project Structure section surfaces version discrepancies instead of repeating dependency lists (Priority: P1)

A developer scans a monorepo with 80 modules. Currently the Project Structure section dumps every module's full declared-dependency list — 80 × N lines. After this change the section shows the module graph (inter-module links), the package organisation pattern, and root packages, plus a focused "Version discrepancies" sub-block listing only artifacts that appear at different versions across modules.

**Why this priority**: Equal to P1 because this section currently produces the most output volume and the version-discrepancy signal is genuinely actionable for the developer.

**Independent Test**: Can be fully tested by building a `ScanResult` with modules that share some dependencies at the same version and disagree on one, running both consumers, and asserting: no per-module dependency lists appear; the discrepancy artifact and its per-module versions appear in the "Version discrepancies" block; the version that is consistent across all modules does NOT appear in the discrepancy block.

**Acceptance Scenarios**:

1. **Given** modules `api` and `core` both declare `com.fasterxml.jackson.core:jackson-databind:2.17.0`, **When** either consumer renders Project Structure, **Then** `jackson-databind` does NOT appear in the Version discrepancies block.
2. **Given** module `api` declares `org.mapstruct:mapstruct:1.5.5` and module `core` declares `org.mapstruct:mapstruct:1.6.0`, **When** either consumer renders Project Structure, **Then** the Version discrepancies block contains `org.mapstruct:mapstruct → {api: 1.5.5, core: 1.6.0}`.
3. **Given** all modules agree on every dependency version, **When** either consumer renders Project Structure, **Then** the Version discrepancies block shows an explicit `none` notice.
4. **Given** a `StructureInfo` with modules that have `moduleDependencies`, **When** either consumer renders Project Structure, **Then** the inter-module dependency graph is shown using the format `moduleName → [dep1, dep2]` per line.
5. **Given** a `StructureInfo` with non-empty `packageSegments` (e.g., `["dev", "zahaand"]`), **When** either consumer renders Project Structure, **Then** the output includes a `Package segments: dev, zahaand` line; **Given** `packageSegments` is empty, **Then** no such line appears.

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
- What happens when `resolvedVersion` is `null` on a dependency? The artifact is excluded from version-discrepancy detection (version unknown) and rendered without a version in per-artifact format in Tech Stack grouping.
- What happens when all source roots in `TestInfo` are already relative (no common absolute prefix)? They are used as-is for grouping — no normalization error occurs.
- What happens when `TestInfo.sourceRoots` is empty? The source-roots line is omitted from the output (unchanged from current behaviour).
- What happens when only one module in the entire project uses a dependency? That artifact cannot have a cross-module discrepancy and is not shown in the Version discrepancies block regardless of version.
- What happens when a `groupId` group contains a mix of null and non-null `resolvedVersion` values? The group is treated as non-uniform: the per-artifact format (FR-003) applies to all artifacts in the group; null-version artifacts are rendered without a version.
- What happens when all artifacts in all groups have `resolvedVersion == null`? All groups are rendered in per-artifact format without versions (no group headers); the Tech Stack section still lists all `groupId:artifactId` entries, grouped by `groupId`.
- What happens when the same `(groupId, artifactId)` coordinate is declared more than once within a single module (intra-module duplicate)? The last declared version for that coordinate within that module is used for version-discrepancy detection; earlier occurrences are discarded.
- What happens when `TestInfo.sourceRoots` contains a mix of absolute paths and relative paths? The longest common prefix is computed only across the absolute-path entries. Relative-path entries are used as-is for template grouping. No normalisation error occurs.
- What happens when exactly one source root normalises to a given template (count == 1)? The `— N modules` count suffix is omitted; only the relative path is rendered (e.g., `src/test/java` with no suffix).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Both `:prompt` (PromptGenerator) and `:ui` (ScanResultRenderer) MUST group `StackInfo.dependencies` by `groupId` when rendering the Tech Stack section. Groups MUST be output in lexicographic order by `groupId`.
- **FR-002**: When a `groupId` group contains **more than one artifact** AND **all** its artifacts share the same non-null `resolvedVersion`, both consumers MUST render: (a) one group header line `groupId:* @ version` (e.g., `org.springframework:* @ 6.1.4`), and (b) each artifact below the header as `artifactId` only (without repeating the version). A group with exactly one artifact, or any artifact with `resolvedVersion == null`, is not eligible for this format and falls under FR-003.
- **FR-003**: When a `groupId` group is not eligible for the uniform-version format (FR-002) — i.e., it has exactly one artifact, any artifact has `resolvedVersion == null`, or artifacts have differing non-null versions — both consumers MUST render each artifact individually as `groupId:artifactId:version` (or `groupId:artifactId` when `resolvedVersion` is null). No group header is rendered.
- **FR-004**: Both consumers MUST NOT include per-module external dependency lists in the Project Structure section.
- **FR-005**: Both consumers MUST render the inter-module dependency graph (`Module.moduleDependencies`) in the Project Structure section. The canonical format is one line per module: `moduleName → [dep1, dep2]`. Modules with no inter-module dependencies are omitted from this list.
- **FR-006**: Both consumers MUST compute and render a Version discrepancies sub-block in the Project Structure section. The block MUST: list only artifacts whose non-null `resolvedVersion` differs across two or more modules that declare them, using format `groupId:artifactId → {moduleName: version, ...}`; sort entries lexicographically by `groupId`, then `artifactId`; sort module names within each entry lexicographically. When no discrepancies exist, the sub-block MUST render an explicit `none` notice rather than being omitted.
- **FR-007**: Artifacts that appear at the same version in every module that declares them MUST NOT appear in the Version discrepancies sub-block.
- **FR-008**: Both consumers MUST deduplicate `TestInfo.frameworks` by `(name, version)` so each distinct framework appears exactly once in the Testing section. A `null` version is distinct from any non-null version; `(name, null)` and `(name, "5.0")` are two separate entries and are not merged.
- **FR-009**: Both consumers MUST normalise `TestInfo.sourceRoots` to relative path templates by stripping the longest common absolute prefix across all absolute-path entries (relative-path entries are used as-is). Identical relative templates MUST be collapsed to a single line with a count equal to the number of raw `TestInfo.sourceRoots` entries that normalise to that template. When a template has exactly one matching entry (count == 1), the `— N modules` count suffix is omitted; only the relative path is rendered.
- **FR-010**: The `:scan` and `:model` modules MUST NOT be modified in this sprint. *[Amendment 2026-06-27: The new `:shared` module introduced in this sprint is an additive standalone module; its creation does not modify `:scan` or `:model` and does not conflict with this requirement.]*
- **FR-011**: Empty/Error `SectionResult` handling for all three sections MUST remain unchanged ("not detected" / "not available").
- **FR-012**: All formatting changes to `PromptGenerator` MUST be covered by unit tests.
- **FR-013**: All shared rendering logic — Tech Stack grouping, test framework deduplication, source-root normalisation, and version-discrepancy detection — MUST be implemented in a new `:shared` Gradle module that depends only on `:model`. Both `:prompt` and the root project (`:ui`) MUST depend on `:shared` and use its utilities; neither consumer may re-implement this logic independently.
- **FR-014**: Both consumers MUST render `StructureInfo.packageSegments` in the Project Structure section when it is non-empty, using the format `Package segments: seg1, seg2, ...` (comma-separated). When `packageSegments` is empty, this line is omitted.

### Non-Functional Requirements

- **NFR-001 (Determinism)**: For a given `ScanResult` with non-empty section data, both consumers MUST produce byte-identical section body content on repeated calls — specifically, the string returned by each consumer's render function for that section. The lexicographic sort orders mandated in FR-001 and FR-006 are the primary mechanism ensuring this. The empty/error wrapper strings differ between consumers by design (`:prompt` uses `"not detected"` / `"not available"`; `:ui` uses bundle-key strings) and are excluded from this requirement per FR-011.
- **NFR-002 (Performance)**: Rendering 80 modules and 250 dependencies is expected to complete in sub-millisecond time. This is an informal engineering target and not a normative gating requirement for this sprint.

### Key Entities

- **Dependency** (`groupId`, `artifactId`, `resolvedVersion`): the atomic unit of grouping in Tech Stack and version-discrepancy detection.
- **Module** (`name`, `declaredDependencies`, `moduleDependencies`): the source of per-module dependency data used to detect discrepancies and to render the module graph.
- **TestFramework** (`name`, `version`): deduplicated by `(name, version)` pair in the Testing section.
- **Source root template**: a relative path suffix derived from normalising absolute source-root paths; aggregated with a raw-entry count.
- **:shared module**: a new Gradle module housing all shared rendering utilities; its sole declared dependency is `:model`; both `:prompt` and the root project (`:ui`) depend on it.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On an 80-module project with 250 dependencies across 40 `groupId` groups where most groups share a single version, the Tech Stack section is expected to reduce from 250+ lines to approximately 40–60 lines (one header per uniform group, one line per artifact in mixed groups). In the worst case where all groups have mixed versions, line count is not reduced, but all dependencies are still rendered grouped by `groupId` rather than flat.
- **SC-002**: On an 80-module project, the Project Structure section no longer lists per-module external dependency lines; only module names, inter-module links, root packages, package segments, and version discrepancies appear.
- **SC-003**: On a project where all 80 modules declare the same 3 test frameworks, the Testing section shows exactly 3 framework lines regardless of module count.
- **SC-004**: On a project with 80 modules each contributing one `src/test/java` source root, the Testing section shows exactly one source-root line with `— 80 modules`.
- **SC-005**: All new `PromptGenerator` formatting logic is verified by automated tests; no regression in existing `PromptGenerator` tests. Each changed section in `ScanResultRenderer` is covered by at least one smoke test asserting the key positive and negative rendering signals.
- **SC-006**: Both consumers (`:prompt` and `:ui`) produce byte-identical section body content for the same `ScanResult` with non-empty data — the same grouping, deduplication, ordering, and text format applied in both render functions. Verifiable by comparing section body strings from both consumers for an identical, non-empty `ScanResult` input. The empty/error wrapper strings (FR-011) intentionally differ between consumers and are excluded from this criterion.

## Clarifications

### Session 2026-06-27

- Q: Should grouping/deduplication logic be shared between `:prompt` and `:ui` or duplicated independently? → A: Extract to a new `:shared` Gradle module that depends only on `:model`. Both `:prompt` and `:ui` depend on `:shared`. This is formalised in FR-013. (Original answer "extract to :model preferred" superseded by FR-010 Amendment 2026-06-27.)
- Q: What is the canonical format for a Tech Stack group header when all artifacts share a version? → A: `groupId:* @ version` (e.g., `org.springframework:* @ 6.1.4`); artifact lines under the header show `artifactId` only (no version repeated).
- Q: What is the canonical format for the inter-module dependency graph in Project Structure? → A: `moduleName → [dep1, dep2]` per line; modules with no inter-module dependencies are omitted.
- Q: What level of automated test coverage is required for `:ui` (ScanResultRenderer) changes? → A: One smoke test per changed section — assert key positive and negative signals in rendered output; full unit-test parity with `:prompt` is out of scope.
- Q: Should the version discrepancy block use the same text format in both `:prompt` and `:ui`, or a richer HTML format in `:ui`? → A: Same text format `groupId:artifactId → {module: version, ...}` in both consumers.

## Assumptions

- `StructureInfo.packageSegments` contains the common package name segments for the project (e.g., `["dev", "zahaand"]`); it is rendered comma-separated if non-empty, per FR-014.
- Source roots in `TestInfo.sourceRoots` may be absolute paths on the scanning machine; the consumer strips the longest common prefix across absolute entries to derive relative templates. Relative entries are used as-is.
- A "module count" per source-root template is the number of raw `TestInfo.sourceRoots` entries that normalise to that template, per FR-009.
- Dependencies with `resolvedVersion == null` are treated as "version unknown" and excluded from version-discrepancy detection; they are still rendered in Tech Stack without a version.
- The `:ui` module's `ScanResultRenderer` requires one smoke test per changed section (Tech Stack, Project Structure, Testing) asserting key positive and negative signals. Full unit-test parity with `:prompt` is deferred to a future sprint.
- All shared rendering utilities live in the new `:shared` module. Both `:prompt` and the root project depend on `:shared`. This avoids SC-006 drift between consumers. (Previously stated as ":model preferred" — superseded by FR-013 and FR-010 Amendment 2026-06-27.)
