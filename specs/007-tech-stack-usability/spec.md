# Feature Specification: Sprint 7 — Usability Rework: Tech Stack & Testing Collection Layer

**Feature Branch**: `007-tech-stack-usability`  
**Created**: 2026-06-28  
**Status**: Implemented  
**Input**: Sprint 7 feature description — usability rework of the collection layer for Tech Stack and Testing output

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Concise Inverted Tech Stack on a Large Monorepo (Priority: P1)

A developer scans a Maven monorepo with 130+ modules and ~250 resolved dependencies. Instead of a ~150-line list of every module's resolved classpath (the Sprint 6 output), the plugin now shows a compact inverted view: each external technology appears once, with its version(s) and — only when versions differ across modules — the carrier modules grouped by reactor topology. A uniform dependency is shown as a single line with a count.

**Why this priority**: The previous output was technically correct but not usable at scale. The inverted representation is the primary deliverable of this sprint and the foundation for all other improvements.

**Independent Test**: Scan the real Maven monorepo; verify that the Tech Stack section contains at most one entry per groupId:artifactId coordinate and that each entry fits on one to three lines for the majority of dependencies.

**Acceptance Scenarios**:

1. **Given** a Maven monorepo where `spring-core:6.1.0` is declared directly in all 40 leaf modules, **When** the plugin runs, **Then** the Tech Stack entry for `spring-core` shows `6.1.0` with a count or "all modules" label and does NOT list individual module names.
2. **Given** a Maven monorepo where `testcontainers` is declared at `1.19.8` in group A's modules and `1.20.1` in group B's modules, **When** the plugin runs, **Then** the Tech Stack entry for `testcontainers` shows both versions, each followed by its carrier modules grouped by aggregator, on separate lines.
3. **Given** a dependency (`asm`, `objenesis`, `listenablefuture`) that is pulled in only transitively and never declared directly in any module's POM, **When** the plugin runs, **Then** that artifact does NOT appear in the Tech Stack section.

---

### User Story 2 — Repurposed Testing Section: Coverage, Source Roots, Naming (Priority: P2)

A developer reviews the Testing section and sees test infrastructure configuration: the coverage threshold reported by JaCoCo (or "not detected" if not configured), test source roots (already compacted in Sprint 6), and the test naming pattern (already implemented). Test framework identities — JUnit, Mockito, Testcontainers, AssertJ — are visible in the Tech Stack section as direct dependencies. The Testing section no longer contains a framework list or any family grouping.

**Why this priority**: Repurposing Testing to carry only non-Tech-Stack test configuration (coverage, layout, naming) removes the need for any framework allow-list or denylist. It also aligns with the "never fabricate" principle: the section fills in on projects that do configure coverage, and shows "not detected" otherwise — exactly the behavior already established in Sprint 6 for absent configurations.

**Independent Test**: Scan the reference Maven monorepo (JaCoCo absent); verify the Testing section shows coverage "not detected", the compacted source root layout from Sprint 6, and the naming pattern — and contains NO framework list or "Frameworks:" header.

**Acceptance Scenarios**:

1. **Given** a project with no JaCoCo plugin configured, **When** the plugin runs, **Then** the Testing section shows coverage threshold as "not detected" and does NOT show a framework list anywhere in that section.
2. **Given** a project with JaCoCo configured at a minimum coverage of 80%, **When** the plugin runs, **Then** the Testing section shows the coverage threshold as "80%" (or equivalent), sourced from the build configuration.
3. **Given** any project, **When** the plugin runs, **Then** the Testing section contains no "Frameworks:" header and no test-framework family entries; those coordinates are visible only in the Tech Stack section as direct dependencies.

---

### User Story 3 — Removal of Low-Value Project Structure and Package Outputs (Priority: P3)

A developer runs the plugin and does not see a "Project Structure" block or package/root-package segment values in the output. Both were identified as low-value on real projects (developers read module topology in the IDE; package patterns were garbage on multi-module Maven builds).

**Why this priority**: These removals reduce output noise and eliminate confusing/incorrect values, but they are secondary to the positive value added by stories 1 and 2.

**Independent Test**: Scan any Maven project; verify that neither "Project Structure" nor any package/root-package field appears anywhere in the plugin's generated prompt or UI tool window output.

**Acceptance Scenarios**:

1. **Given** any Maven or Gradle project, **When** the plugin runs, **Then** no "Project Structure" section or block appears in the generated output.
2. **Given** any project, **When** the plugin runs, **Then** no `rootPackages` or `secondLevelSegments` field or value appears in the output.
3. **Given** a project that would previously have shown package tree data, **When** the plugin runs, **Then** the output is shorter by exactly the removed Project Structure block, with no other sections affected.

---

### Edge Cases

- What happens when a Maven module declares zero direct external dependencies? — It contributes nothing to Tech Stack; it still appears in carrier-module groupings for other dependencies it is a carrier of (if any).
- What happens when all modules use the same version of every dependency? — Tech Stack shows one line per technology with a count; no module names are listed anywhere.
- What happens when a module has no Maven model (Gradle project)? — A thin denylist of clearly-synthetic artifacts is applied; direct-vs-transitive distinction is not available; all non-denylisted resolved dependencies are included.
- What happens when an aggregator module itself declares a direct dependency? — It appears as a carrier module in the Tech Stack entry, listed first before its submodules per reactor topology.
- What happens when a module's aggregator cannot be determined? — The `aggregator` field is null; the module appears ungrouped (top-level) in any version-discrepancy listing.
- What happens when no coverage plugin (e.g., JaCoCo) is configured? — The coverage threshold field shows "not detected", consistent with Sprint 6 behavior for absent configurations. No framework list is ever shown in Testing regardless of coverage presence.
- What happens when a directly-declared dependency's resolved version is null or blank after parent/BOM resolution? — The dependency REMAINS in the output with its version field omitted (not "unknown", not dropped). Consistent with the never-fabricate principle; the coordinate is still surfaced.
- What happens when `buildInvertedTechStack` receives an empty modules list and all preamble metadata (JDK, language level, build system) is null? — `renderInvertedTechStack` returns `"not detected"`, consistent with the never-fabricate principle applied to absent data.
- What happens when entries are empty but at least one preamble value (Build System, JDK Version, Language Level) is non-null? — `renderInvertedTechStack` renders the non-null preamble lines only and omits the `"not detected"` sentinel. `"not detected"` is returned only when both entries and all preamble values are null. This prevents a silent sentinel on Gradle projects that have JDK/build-system data but zero non-denylisted dependencies.
- What happens when a module appears in multiple aggregators' `<modules>` lists (pathological reactor configuration)? — The implementation resolves this deterministically using a fixed rule (last aggregator encountered in iteration order wins). This is a pathological configuration; the result is implementation-defined but must not crash or produce non-deterministic output.
- **Deferred**: Robustness of the aggregator `canonicalPath` map to filesystem case-sensitivity and symlinks is to be verified during real-project testing. Not blocking for this sprint.

## Clarifications

### Session 2026-06-28

- Q: Do test-framework dependencies (Testcontainers, JUnit, Mockito, AssertJ) appear in the Tech Stack section, the Testing section, or both? → A: **Superseded by Q2** — see below.
- Q: How does the plugin determine which test-scope dependencies belong in the Testing section? → A: The premise of a Testing framework list is **eliminated**. Test frameworks appear in **Tech Stack only** as direct dependencies, identifiable by their coordinates. The Testing section is repurposed to carry only what Tech Stack does not: (1) test coverage threshold (JaCoCo config, or "not detected"), (2) test source roots (already compacted in Sprint 6), (3) test naming pattern (already implemented). FR-010 is rewritten; FR-011 and FR-012 are removed.
- Q: How are Tech Stack entries ordered in the output? → A: Alphabetical by `groupId:artifactId` coordinate — deterministic, reproducible across scans.

## Requirements *(mandatory)*

### Functional Requirements

**Model layer**:

- **FR-001**: The module descriptor MUST include an `aggregator` field (`String?`) containing the `artifactId` of the Maven aggregator module that directly lists this module (`displayName` is used as fallback only when `artifactId` is unavailable), or null for root/top-level modules.
- **FR-002**: The `PackageTreeData` structure (fields: `rootPackages`, `secondLevelSegments`) MUST be removed from the data model, and all consumers and tests MUST be updated accordingly.

**Scan / collection layer**:

- **FR-003**: For Maven projects, the scan adapter MUST return only directly-declared dependencies per module (identity = declared-direct set, version = resolved effective value). The following rules govern membership in the declared-direct set:
  - **Parent-inherited dependencies**: a dependency declared in a parent POM's own `<dependencies>` block (NOT `<dependencyManagement>`) is inherited by every child module and MUST be counted as direct for each child (it is present in the child's effective declared set). Distinguishing the declaration origin — parent vs. local — is explicitly out of scope (tech debt); only presence in the effective declared set matters.
  - **BOM imports**: an artifact imported as a BOM (`<type>pom</type>` with `<scope>import</scope>`) is a version-management mechanism, not a dependency. BOM-import artifacts MUST be excluded from the direct slice.
  - **Optional dependencies**: a dependency with `<optional>true</optional>` IS included in the direct slice. Optional affects transitive propagation, not membership in the declared set.
  - **Version precedence**: if the same coordinate is declared in both a child POM and its parent with different versions, the resolved effective version is always used (nearest wins — child overrides parent). There is at most one resolved version per coordinate per module.
- **FR-004**: For Maven projects, the scan adapter MUST extract each module's aggregator name from the Maven reactor topology and populate the `aggregator` field in the module descriptor.
- **FR-005**: Package-tree collection (`getPackageTree()`) MUST be removed from the scan adapter and its port/interface.
- **FR-006**: For Gradle projects, the scan adapter MUST apply a thin denylist of clearly-synthetic artifacts and exclude matching artifacts from the dependency output; no precise direct-slice is required. Matching strategy: exact `groupId:artifactId` coordinate match for known artifacts (objenesis, paranamer, listenablefuture, failureaccess, j2objc-annotations, checker-qual, aopalliance); groupId match for `org.ow2.asm` (covers all asm coordinates). The denylist is a static, manually-maintained list; dynamic discovery or automated maintenance is NOT required (see FR-N6).

**Shared / representation layer**:

- **FR-007**: The shared module MUST build an inverted Tech Stack representation covering **all** direct external dependencies regardless of Maven scope: a mapping from technology coordinate (`groupId:artifactId`) to an ordered list of (version, carrier-modules) pairs, sorted alphabetically by `groupId:artifactId`. "All direct" refers to the same slice produced by FR-003 — "regardless of scope" means no scope-based filtering is applied (compile, runtime, test, and provided are all included); it does NOT expand the set beyond FR-003's declared-direct dependencies. System-scoped dependencies are excluded from the direct slice (they reference local filesystem JARs not resolved through the Maven repository). A dependency is **external** when its coordinate is NOT an internal project module: neither its `artifactId` nor its `groupId:artifactId` matches any entry in `internalModuleNames`. Test-framework coordinates (JUnit, Mockito, Testcontainers, AssertJ, etc.) appear in Tech Stack only; they are NOT duplicated in the Testing section.
- **FR-008**: When a technology has a single version across all modules, the representation MUST render that entry as a single line with the exact format `coordinate:version [N modules]` (where N is the carrier-module count), WITHOUT storing or rendering individual module names. This format is a firm contract: SC-005 byte-identical parity is structurally guaranteed because `renderInvertedTechStack` in the shared module is the sole rendering function — neither the prompt generator nor the UI tool window re-implements formatting.
- **FR-009**: When a technology has multiple versions across modules, the representation MUST store carrier modules per version, grouped by Maven reactor topology, with the following ordering rules:
  - Aggregator groups within a version entry are ordered: named aggregators alphabetically, with the null-aggregator (ungrouped/top-level) group LAST.
  - Module names within an aggregator group are sorted alphabetically.
  - Each aggregator group is rendered on a separate line.
  - Null-aggregator behavior is consistent across the spec: `aggregator = null` means a root/top-level module; it renders ungrouped and appears last in any multi-version entry.
- **FR-010**: The shared module MUST build a Testing representation that carries: (a) test coverage threshold sourced from build configuration (e.g., JaCoCo minimum line/branch coverage setting), or the literal string "not detected" if no coverage plugin is configured; (b) test source root paths, already compacted per Sprint 6 behavior; (c) test naming pattern, already implemented. No framework list, family grouping, or framework denylist is produced in this representation.
- **FR-011** — Removed 2026-06-28 (superseded by the repurposing of the Testing section; see §Clarifications).
- **FR-012** — Removed 2026-06-28 (same).
- **FR-013**: The inversion and grouping logic MUST reside exclusively in the shared module so that both the prompt consumer and the UI consumer derive output from a single shared source.

**Prompt / UI layer**:

- **FR-014**: Both the prompt generator and the UI tool window MUST consume the shared inverted Tech Stack and Testing representations, producing byte-identical Tech Stack and Testing content (SC-005 parity retained).
- **FR-015**: The Project Structure block MUST be removed from the prompt output and the UI tool window output.
- **FR-016**: Package and root-package values MUST be removed from the prompt output and the UI tool window output.

**Out of scope (explicit non-requirements)**:

- **FR-N1**: A precise Gradle direct slice via a custom Gradle Tooling model builder is NOT required in this sprint.
- **FR-N2**: Human-readable dependency names (e.g., "log4j 2.23.1") are NOT required; Maven coordinates are used throughout.
- **FR-N3**: The Constitution text is NOT modified in this sprint.
- **FR-N4**: Code Style and Linters sections are NOT changed.
- **FR-N5**: A "Frameworks:" header or test-framework family list is NOT produced in the Testing section; framework identity is communicated via Tech Stack coordinates alone.
- **FR-N6**: Dynamic discovery or automated maintenance of the Gradle denylist is NOT required; it is a static list maintained with plugin releases.
- **FR-N7**: Performance or latency requirements for `buildInvertedTechStack` at 130-module × 250-dependency scale are NOT specified this sprint. SC-001 (≤40 lines) is validated empirically against the reference monorepo, not derived from a formula.

### Key Entities

- **ModuleDescriptor**: Represents a single project module; gains the `aggregator: String?` field; loses `packageTree: PackageTreeData`; retains `externalDependencies` (now populated with direct-only slice for Maven).
- **DirectDependency**: A dependency declared directly in a module's build file, with identity from the declared set and version resolved from the effective model (parent/BOM).
- **InvertedTechStack**: The cross-module view: technology coordinate → list of `(version, List<CarrierModule>)` entries; CarrierModule carries module name and its aggregator for grouping.
- **TestingRepresentation**: Testing section data — coverage threshold string (or "not detected"), test source root paths, and naming pattern. No framework list or family grouping; test frameworks are represented in InvertedTechStack.
- **AggregatorGroup**: Logical grouping used in rendering: aggregator module name → ordered list of submodule names present in a given version's carrier set.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Tech Stack output for a 130-module Maven monorepo is reduced from ~150 lines to ≤ 40 dependency entry lines. Preamble lines (Build System, JDK Version, Language Level) are NOT counted toward the 40-line limit.
- **SC-002**: No transitive-only artifact (asm, objenesis, listenablefuture, checker-qual, aopalliance, paranamer, failureaccess, j2objc-annotations, or similar) appears in the Tech Stack or Testing output for any Maven project.
- **SC-003**: Testing section contains exactly: test coverage threshold (or "not detected"), test source roots, and naming pattern. No framework list or "Frameworks:" header appears in the Testing section for any project.
- **SC-004**: Version discrepancies for Tech Stack are embedded inline within each entry; no standalone "Discrepancies" block exists in the output.
- **SC-005**: Tech Stack and Testing content is byte-identical between the generated LLM prompt and the UI tool window for the same project scan (byte-identical parity from the prior sprint is retained).
- **SC-006**: Project Structure block and package/root-package values are absent from all output for every supported project type.
- **SC-007**: For a Maven project, the resolved version of a dependency that omits `<version>` in its POM (inheriting from parent or dependencyManagement) is correctly shown as the resolved version, not blank or "unknown".

## Assumptions

- FR-003's direct slice is obtained via a prioritized fallback chain: (1) the declared-direct set from the Maven project model (primary path: `mavenModel.dependencies` coordinate set intersected with the resolved dependency list); (2) root-level nodes of the resolved dependency tree, which correspond to direct dependencies by tree position (first fallback); (3) resolved set minus computed transitives (last resort). The implementation confirms which path succeeds against the IntelliJ 2025.3.5 classpath at implement time; the spec does not assume a single hardcoded method succeeds.
- The `aggregator` field stores a flat module name string (not a path or nested structure); consumer logic reconstructs the grouping from this flat field at render time.
- Removal of `PackageTreeData` is a deliberate breaking model change accepted by the team; restoration from git history is trivial if a better implementation is designed later.
- Constitution wording changes resulting from the architectural decision to classify dependencies inside the collection layer are deferred to a Sprint 9 Constitution-amendment package and are NOT part of this sprint's scope.
- Source-root compaction introduced in Sprint 6 remains unchanged and is not revisited.
- Code Style and Linters sections remain unchanged; their "not detected" behavior on the reference test project is preserved.
- The Gradle denylist is a static, maintained list of known synthetic/shading artifacts; it does not require dynamic discovery.
- The reference test project for validation is the real Maven monorepo (130+ modules, ~11 aggregator groups, ~250 resolved dependencies) used in Sprint 6 manual testing.
