# Feature Specification: Scan — Project Facts Collectors

**Feature Branch**: `002-scan-collectors`
**Created**: 2026-06-12
**Status**: Draft
**Input**: User description: "Sprint 2 — `scan`. Collectors that populate the data model from Sprint 1 by reading the IntelliJ project model."

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Technology Stack Discovery (Priority: P1)

A developer opens a JVM/Java project in the IDE. The plugin reads the project's explicitly declared dependencies (not transitive ones), identifies the active build system, and determines the effective JDK/language level. The result is a deduplicated, project-wide dependency list — the stack fingerprint that downstream Constitution generation will use. If nothing is found, the section is empty, never fabricated.

**Why this priority**: Stack facts are the most foundational. Without them no other section is meaningful in context; they are also the most directly verifiable against known build files.

**Independent Test**: A project with a known set of declared dependencies can be scanned and the result compared against the build file by inspection. Empty projects report an empty dependencies list.

**Acceptance Scenarios**:

1. **Given** a single-module Maven project with 5 declared dependencies, **When** the stack collector runs, **Then** exactly 5 dependency records (groupId + artifactId + resolved version) appear in the result with no transitive entries.
2. **Given** a multi-module project where two modules declare the same dependency at different versions, **When** the stack collector aggregates, **Then** a single record appears for that coordinate carrying the **maximum version** (highest semver); per-module version divergence is preserved separately in each module's own dependency list.
3. **Given** a project with no declared dependencies, **When** the stack collector runs, **Then** the dependencies list is explicitly empty.
4. **Given** modules with different JDK/language levels, **When** the stack collector runs, **Then** the effective language level equals the maximum across all modules.
5. **Given** a Gradle project and a Maven project (two separate projects), **When** the stack collector runs on each, **Then** each result identifies the correct build system.

---

### User Story 2 — Code Style Sources Discovery (Priority: P2)

A developer's project may have code style configured through one or more sources — a Checkstyle config, a Spotless formatter config, a PMD ruleset, an `.editorconfig`, or IDE-specific style settings. The plugin discovers all such sources present in the project and records their type and project-relative path. Config file contents are not analyzed here. If none are found, the section is empty.

**Why this priority**: Style sources are lightweight to collect and require no build-system-specific logic; they provide high value for Constitution generation at low collection cost.

**Independent Test**: A project with a known `.editorconfig` and a `checkstyle.xml` can be scanned; both sources appear with their correct types and paths. A project with no style files reports an empty sources list.

**Acceptance Scenarios**:

1. **Given** a project containing `.editorconfig`, `checkstyle.xml`, and `.idea/codeStyles/`, **When** the code style collector runs, **Then** all three sources appear in the result with correct types and project-relative paths.
2. **Given** a project with no style configuration files, **When** the code style collector runs, **Then** the sources list is empty.
3. **Given** a project where only Spotless is configured, **When** the code style collector runs, **Then** Spotless appears as a style source (formatter) and does NOT appear in the linters section.

---

### User Story 3 — Active Linter Rules Discovery (Priority: P2)

When a project applies Checkstyle or PMD as part of its build, the plugin reads the tool's own configuration file to discover which rules are actually enabled and at what severity (error / warning / info). Whether violations fail the build ("hardness") is also recorded at the tool level and attached to each rule. If the hardness cannot be determined from the available project model, it is marked "not detected" — never guessed. If no linters are applied, the section is empty.

**Why this priority**: Linter rule facts directly reflect the team's agreed quality standards; they are high value for Constitution generation, though more complex to collect due to differences between build systems.

**Independent Test**: A project with a Checkstyle config listing known rules can be scanned; the resulting rule list matches the config file entries with correct severities and the build-failure flag at tool level.

**Acceptance Scenarios**:

1. **Given** a project with Checkstyle applied and a config listing 10 rules, **When** the linter collector runs, **Then** exactly those 10 rules appear with their individual severities and the build-failure flag at tool level.
2. **Given** a project where the build-failure flag for a linter tool cannot be read from the project model, **When** the linter collector runs, **Then** the hardness field is "not detected" — not guessed and not defaulted to any value.
3. **Given** a project with no static analysis tools applied, **When** the linter collector runs, **Then** the linters section is empty.
4. **Given** a project that applies only Spotless (a formatter), **When** the linter collector runs, **Then** no linter rule entries appear for Spotless.

---

### User Story 4 — Test Infrastructure Discovery (Priority: P3)

The plugin identifies which test frameworks and companion libraries appear in the project's declared dependencies, locates the test source directories, and records the JaCoCo coverage threshold if JaCoCo is applied. When JaCoCo is applied but the threshold value cannot be read, `null` is recorded — never a fabricated value. Detailed per-counter threshold structures are out of scope.

**Why this priority**: Test facts enrich the Constitution but have no dependencies on other sections and carry lower collection complexity than linter discovery.

**Independent Test**: A project with JUnit 5 and Mockito declared as test dependencies, and a known JaCoCo threshold in a Maven pom, can be scanned; the result lists both frameworks and the exact threshold value.

**Acceptance Scenarios**:

1. **Given** a project with JUnit 5, Mockito, and AssertJ declared as test dependencies, **When** the test collector runs, **Then** all three frameworks appear in the result.
2. **Given** a project where JaCoCo is applied with a minimum coverage threshold of 80%, **When** the test collector runs, **Then** the threshold is recorded as `0.8` (or equivalent representation).
3. **Given** a project where JaCoCo is applied but its threshold cannot be read via the standard project model, **When** the test collector runs, **Then** the threshold field is `null`, not absent and not a defaulted value.
4. **Given** a project with no testing dependencies, **When** the test collector runs, **Then** the frameworks list is empty and the coverage threshold is absent.

---

### User Story 5 — Module Structure and Package Layout Discovery (Priority: P3)

For multi-module projects, the plugin records each module's name, its declared external dependencies (coordinate + version), and inter-module dependency links by module name. Package structure is collected as raw data — root packages and some depth of the package tree — without classifying the organization pattern (by-layer vs. by-feature). That classification is left to the downstream consumer. For single-module projects, a single module entry is produced.

**Why this priority**: Structure facts are the richest and most multi-faceted; they are valuable but depend on correct dependency collection (P1/P2) being in place first.

**Independent Test**: A multi-module project with known module names and inter-module links can be scanned; the result contains one entry per module with correct external dependencies and inter-module links.

**Acceptance Scenarios**:

1. **Given** a three-module project where module A depends on module B which depends on module C, **When** the structure collector runs, **Then** the module dependency graph shows A→B and B→C links by module name.
2. **Given** a module with 3 declared external dependencies, **When** the structure collector runs, **Then** `declaredDependencies` for that module contains exactly those 3 entries with correct coordinates and versions.
3. **Given** a project's source roots, **When** the package structure collector runs, **Then** the result contains root packages and their second-level segments (e.g. `com.example.web`, `com.example.domain`) — no deeper — without any organization pattern classification.
4. **Given** a single-module project, **When** the structure collector runs, **Then** the result contains exactly one module entry with its dependencies populated.

---

### Edge Cases

- What happens when a dependency appears only in test scope — is it included in the flat stack aggregate? (Assumption: all declared scopes are included; scope is a fact to preserve, not a filter.)
- How does the system handle a module with no source roots — is it still listed in the structure section?
- What if a linter config file is referenced in the build but the file is missing from the project tree — is the linter entry omitted or flagged as unresolvable?
- What if two modules declare an identical inter-module dependency — is it recorded once or twice in the module's dependency list?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The scan layer MUST collect each module's explicitly declared (non-transitive) dependencies including groupId, artifactId, and resolved version.
- **FR-002**: The scan layer MUST produce a flat, deduplicated project-wide dependency list aggregated from all modules, keyed by coordinate (groupId + artifactId).
- **FR-003**: When the same dependency coordinate (groupId + artifactId) appears at different versions across modules, the scan layer MUST place the **maximum version** (highest semver) in the flat `StackInfo.dependencies` aggregate. Per-module version divergence is preserved separately in `StructureInfo.Module.declaredDependencies` and is NOT collapsed.
- **FR-004**: The scan layer MUST identify the project's primary build system (Maven or Gradle) and the effective project-level JDK/language level.
- **FR-005**: When modules have differing JDK/language levels, the scan layer MUST record the maximum level as the project-level value.
- **FR-006**: The scan layer MUST discover all code style configuration sources present in the project (Checkstyle configs, Spotless configs, PMD configs, `.editorconfig`, IDE style settings) and record each source's type and project-relative file path.
- **FR-007**: The scan layer MUST NOT parse the contents of style config files for style facts; config contents are only read when extracting linter rules (Checkstyle, PMD).
- **FR-008**: The scan layer MUST collect only linter rules that are actually applied in the build; it MUST NOT enumerate tool default catalogs.
- **FR-009**: For each applied linter rule, the scan layer MUST record its severity (error / warning / info) as declared in the tool's config file.
- **FR-010**: The scan layer MUST record the build-failure flag for each applied linter tool; when this fact cannot be determined from the available project model, it MUST be recorded as "not detected".
- **FR-011**: Spotless MUST be recorded only as a code style source; it MUST NOT produce linter rule entries.
- **FR-012**: The scan layer MUST detect test frameworks and companion libraries from declared dependencies (JUnit, Mockito, AssertJ, Testcontainers, and similar).
- **FR-013**: The scan layer MUST record the test source directory structure and file naming conventions.
- **FR-014**: When JaCoCo is applied, the scan layer MUST record the coverage threshold as a single representative number; when the value is unavailable, it MUST record `null`.
- **FR-015**: The scan layer MUST record each module's declared external dependencies (coordinate + version) and inter-module dependency links by module name.
- **FR-016**: The scan layer MUST collect raw package structure data consisting of root packages and their second-level segments (fixed depth — no deeper); it MUST NOT classify the organization pattern (by-layer vs. by-feature). Classification is the responsibility of downstream consumers (LLM at Constitution time).
- **FR-017**: The `packageOrganization` inference field from the Sprint 1 model MUST be replaced with a raw-material field containing the collected package tree data.
- **FR-018**: When a section has no data to report, it MUST be explicitly marked empty — never silently omitted and never populated with fabricated data.
- **FR-019**: The scan layer MUST be independently testable via unit tests using port fakes, without requiring a running IDE instance.
- **FR-020**: The scan layer MUST NOT text-parse build files (pom.xml, build.gradle, build.gradle.kts, settings files, version catalogs) to extract project facts.

### Key Entities

- **DependencyCoordinate**: Identifies a library by groupId + artifactId + resolved version; the unit of deduplication in the flat stack aggregate.
- **StyleSource**: A discovered code style configuration; holds the source type (Checkstyle / Spotless / PMD / EditorConfig / IDE) and its project-relative file path.
- **ActiveRule**: A linter rule confirmed as enabled in the build; holds the rule identifier, severity, and the denormalized build-failure flag inherited from its tool.
- **Module**: A build sub-project; holds its name, its declared external dependencies, and its links to other modules it depends on.
- **PackageTree**: Raw package namespace data collected from the project's source roots; contains root packages and their second-level segments (fixed depth, no deeper), without any organization pattern classification.
- **ScanResult**: The root aggregate produced by the scan layer; contains one section entry (Stack, CodeStyle, Linters, Tests, Structure), each either populated or explicitly marked empty.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For any single-module or multi-module Maven or Gradle project, the scan completes and produces a result with all five sections either populated or explicitly marked empty — no crashes and no silent omissions — in 100% of test cases.
- **SC-002**: Dependency collection accuracy: for projects with known build files used as test fixtures, 100% of declared dependency coordinates appear in the scan result; 0% of transitive-only dependencies appear.
- **SC-003**: Linter rule completeness: for projects with Checkstyle or PMD configured in test fixtures, all rules listed in the tool's config file appear in the scan result with correct severities.
- **SC-004**: Honesty under absence: for each section, when no relevant configuration exists in the project, the section is marked empty in 100% of test cases — never populated with fabricated values.
- **SC-005**: All primary collector code paths are exercised by unit tests that run without a running IDE instance.
- **SC-006**: No breaking changes are introduced to the meaning of existing model fields that downstream consumers depend on.

## Assumptions

- The scan layer is invoked by the plugin's internal orchestration layer, not directly by end users.
- Maven and Gradle are the only supported build systems for this sprint; other systems (Ant, Bazel, etc.) are out of scope.
- Only JVM/Java projects are in scope for this sprint.
- All declared dependency scopes (compile, test, runtime, etc.) are included in collection; scope is recorded as a fact, not used as a filter.
- Per-module breakdown of non-dependency sections (codestyle, linters, tests) is out of scope; those sections aggregate project-wide.
- Transitive dependency resolution is not performed in this sprint.
- The IntelliJ External System project model is the authoritative data source; direct parsing of build files is prohibited.
- Precise Gradle build-failure flag detection and detailed JaCoCo threshold structure are tracked as tech debt and are not required for this sprint.
- Inter-module dependency links are recorded by module name, not by module object reference.
