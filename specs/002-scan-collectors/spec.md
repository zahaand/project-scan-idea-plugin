# Feature Specification: Scan — Project Facts Collectors

**Feature Branch**: `002-scan-collectors`
**Created**: 2026-06-12
**Status**: Draft
**Input**: User description: "Sprint 2 — `scan`. Collectors that populate the data model from Sprint 1 by reading the IntelliJ project model."

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Technology Stack Discovery (Priority: P1)

A developer opens a JVM/Java project in the IDE. The plugin reads the project's explicitly declared dependencies (not transitive ones), identifies the active build system, and determines the effective JDK version and Java language level independently as distinct facts. The result is a deduplicated, project-wide dependency list — the stack fingerprint that downstream Constitution generation will use. If nothing is found, the section is empty, never fabricated.

**Why this priority**: Stack facts are the most foundational. Without them no other section is meaningful in context; they are also the most directly verifiable against known build files.

**Independent Test**: A project with a known set of declared dependencies can be scanned and the result compared against the build file by inspection. Empty projects report an empty dependencies list.

**Acceptance Scenarios**:

1. **Given** a single-module Maven project with 5 declared dependencies, **When** the stack collector runs, **Then** exactly 5 dependency records (groupId + artifactId + resolved version) appear in the result with no transitive entries.
2. **Given** a multi-module project where two modules declare the same dependency at different versions, **When** the stack collector aggregates, **Then** a single record appears for that coordinate carrying the **maximum version** (highest ComparableVersion-ordered value); per-module version divergence is preserved separately in each module's own dependency list.
3. **Given** a project with no declared dependencies, **When** the stack collector runs, **Then** the dependencies list is explicitly empty.
4. **Given** modules with different JDK/language levels, **When** the stack collector runs, **Then** the effective language level equals the maximum across all modules; a module with no explicit language level inherits the project-level default before aggregation.
5. **Given** a Gradle project and a Maven project (two separate projects), **When** the stack collector runs on each, **Then** each result identifies the correct build system.
6. **Given** a dependency whose resolved version cannot be determined from the IntelliJ model, **When** the stack collector runs, **Then** the dependency is included in the result with a `null` version field — it is not excluded and does not trigger an error state.

---

### User Story 2 — Code Style Sources Discovery (Priority: P2)

A developer's project may have code style configured through one or more sources — a Checkstyle config, a Spotless external config file, a PMD ruleset, an `.editorconfig`, or IDE-specific style settings. The plugin discovers all such sources present in the project and records their type and project-relative path. Spotless configured only inline in the build file (no external standalone config) is not modeled — detecting it would require build-file parsing, which is prohibited. If none are found, the section is empty.

**Why this priority**: Style sources are lightweight to collect and require no build-system-specific logic; they provide high value for Constitution generation at low collection cost.

**Independent Test**: A project with a known `.editorconfig` and a `checkstyle.xml` can be scanned; both sources appear with their correct types and paths. A project with no style files reports an empty sources list.

**Acceptance Scenarios**:

1. **Given** a project containing `.editorconfig`, `checkstyle.xml`, and `.idea/codeStyles/`, **When** the code style collector runs, **Then** all three sources appear in the result with correct types and project-relative paths (relative to the project root directory).
2. **Given** a project with no style configuration files, **When** the code style collector runs, **Then** the sources list is empty.
3. **Given** a project where only Spotless is configured inline in the Gradle build file (no external Spotless config file), **When** the code style collector runs, **Then** Spotless does NOT appear as a style source — inline build-file Spotless is not modeled.
4. **Given** a project with multiple `.editorconfig` files (root-level and nested in subdirectories), **When** the code style collector runs, **Then** all `.editorconfig` files are collected with their respective project-relative paths.

---

### User Story 3 — Active Linter Rules Discovery (Priority: P2)

When a project applies Checkstyle or PMD as part of its build, the plugin reads the tool's own configuration file to discover which rules are actually enabled and at what severity (error / warning / info). Whether violations fail the build ("hardness") is also recorded at the tool level and denormalized onto each rule. If the hardness cannot be determined from the available project model, it is marked "not detected" — never guessed. If no linters are applied, the section is empty. Rule extraction is limited to Checkstyle and PMD; SpotBugs, ErrorProne, and other tools are out of scope for this sprint.

**Why this priority**: Linter rule facts directly reflect the team's agreed quality standards; they are high value for Constitution generation, though more complex to collect due to differences between build systems.

**Independent Test**: A project with a Checkstyle config listing known rules can be scanned; the resulting rule list matches the config file entries with correct severities and the build-failure flag at tool level.

**Acceptance Scenarios**:

1. **Given** a Maven project with Checkstyle applied and a config listing 10 rules, **When** the linter collector runs, **Then** exactly those 10 rules appear with their individual severities and the build-failure flag at tool level denormalized onto each rule.
2. **Given** a project where the build-failure flag for a linter tool cannot be read from the project model, **When** the linter collector runs, **Then** the `breaksBuild` field is `null` ("not detected") — not guessed and not defaulted to any value.
3. **Given** a project with no static analysis tools applied, **When** the linter collector runs, **Then** the linters section is empty.
4. **Given** a project that applies only Spotless (a formatter), **When** the linter collector runs, **Then** no linter rule entries appear for Spotless.
5. **Given** a Gradle project with the Checkstyle plugin applied (evidenced by `checkstyleMain` and `checkstyleTest` task names in the External System model) and a valid Checkstyle config file, **When** the linter collector runs, **Then** the tool is recorded as applied with its rules parsed from the config file and `breaksBuild = null` (Gradle build-failure flag is unavailable via standard TAPI).

---

### User Story 4 — Test Infrastructure Discovery (Priority: P3)

The plugin identifies which test frameworks and companion libraries appear in the project's declared test-scoped dependencies, locates the test source directories, and records the JaCoCo coverage threshold if JaCoCo is applied. When JaCoCo is applied but the threshold value cannot be read, `null` is recorded — never a fabricated value. When JaCoCo is applied for reporting only (no threshold/check rule configured), `null` is also recorded. Detailed per-counter threshold structures are out of scope.

**Why this priority**: Test facts enrich the Constitution but have no dependencies on other sections and carry lower collection complexity than linter discovery.

**Independent Test**: A project with JUnit 5 and Mockito declared as test dependencies, and a known JaCoCo threshold in a Maven pom, can be scanned; the result lists both frameworks and the exact threshold value.

**Acceptance Scenarios**:

1. **Given** a project with JUnit 5, Mockito, and AssertJ declared as test dependencies, **When** the test collector runs, **Then** all three frameworks appear in the result.
2. **Given** a project where JaCoCo is applied with a minimum coverage threshold of 80%, **When** the test collector runs, **Then** the threshold is recorded as `0.8` (a ratio in the range 0.0–1.0, not a percentage integer).
3. **Given** a project where JaCoCo is applied for reporting only (no threshold/check rule configured), **When** the test collector runs, **Then** the `coverageThreshold` field is `null`.
4. **Given** a project with no testing dependencies, **When** the test collector runs, **Then** the frameworks list is empty and the coverage threshold is absent.
5. **Given** a project with a test-scoped dependency that is not on the known framework list, **When** the test collector runs, **Then** it is recorded as "unknown test dependency" and does not appear in the named frameworks list.

---

### User Story 5 — Module Structure and Package Layout Discovery (Priority: P3)

For multi-module projects, the plugin records each module's build-system identifier as its name, its declared external dependencies (coordinate + version), and inter-module dependency links by module name (deduplicated). Package structure is collected as raw data — root packages and their second-level segments in dotted Java package notation — without classifying the organization pattern (by-layer vs. by-feature). That classification is left to the downstream consumer. For single-module projects, a single module entry is produced.

**Why this priority**: Structure facts are the richest and most multi-faceted; they are valuable but depend on correct dependency collection (P1/P2) being in place first.

**Independent Test**: A multi-module project with known module names and inter-module links can be scanned; the result contains one entry per module with correct external dependencies and inter-module links.

**Acceptance Scenarios**:

1. **Given** a three-module project where module A depends on module B which depends on module C, **When** the structure collector runs, **Then** the module dependency graph shows A→B and B→C links by module name.
2. **Given** a module with 3 declared external dependencies, **When** the structure collector runs, **Then** `declaredDependencies` for that module contains exactly those 3 entries with correct coordinates and versions.
3. **Given** a project's source roots, **When** the package structure collector runs, **Then** the result contains root packages and their second-level segments in dotted Java notation (e.g. `com.example.web`, `com.example.domain`) — no deeper — without any organization pattern classification, stored in the `packageSegments` field.
4. **Given** a single-module project, **When** the structure collector runs, **Then** the result contains exactly one module entry with its dependencies populated.

---

### Edge Cases

- A dependency appearing only in test scope is included in the flat stack aggregate. The `Dependency` model carries no scope field; scope is used only within the test collector's port DTO to select test-scoped entries. Scope is not a filter for structure or stack purposes.
- A module with no source roots (e.g., pure aggregator or BOM) is still listed in the structure section with its dependency links intact; its `packageSegments` contribution is empty. (See FR-015.)
- If a linter config file is referenced in the build but is missing from the project tree, the tool is recorded as applied with its rules in an error/unresolvable state. The tool's presence is a fact; only its rule details are unresolvable. The linters section remains `Ok`. (See FR-008.)
- If two modules declare an identical inter-module dependency, the dependency link is recorded exactly once in the module's `moduleDependencies` list — duplicates are deduplicated by module name. (CHK025 resolved.)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The scan layer MUST collect each module's explicitly declared (non-transitive) dependencies including groupId, artifactId, and resolved version. If a dependency's resolved version cannot be determined from the IntelliJ project model, it MUST be included with a `null` version field — it MUST NOT be excluded and MUST NOT trigger an error state.

- **FR-002**: The scan layer MUST produce a flat, deduplicated project-wide dependency list aggregated from all modules, keyed by coordinate (groupId + artifactId).

- **FR-003**: When the same dependency coordinate (groupId + artifactId) appears at different versions across modules, the scan layer MUST place the **maximum version** in the flat `StackInfo.dependencies` aggregate. Version ordering MUST follow Maven ComparableVersion semantics; for version strings that cannot be compared by ComparableVersion, lexicographic ordering is used as a deterministic fallback. Per-module version divergence is preserved separately in `StructureInfo.Module.declaredDependencies` and is NOT collapsed.

- **FR-004**: The scan layer MUST identify the project's primary build system (Maven or Gradle), the effective JDK version (`jdkVersion`), and the effective Java language level (`languageLevel`) as **two distinct facts**. These are separate fields in `StackInfo` and MUST NOT be conflated into a single value.

- **FR-005**: When modules have differing language levels, the scan layer MUST record the maximum level as the project-level `languageLevel`. Modules with no explicit language level setting inherit the project-level default before aggregation. If no language level can be determined at all (neither module-level nor project-level), the `languageLevel` field is `null`.

- **FR-006**: The scan layer MUST discover code style configuration sources present in the project and record each source's type and project-relative path (relative to the project root directory returned by `ProjectUtil.guessProjectDir()`, using `/` separators). Sources are recognized per the **Style Source Recognition Table** below. All `.editorconfig` files found anywhere under the project root are collected — not only the root-level one. All files under `.idea/codeStyles/` are collected as `IDE_CODE_STYLE` sources.

**Style Source Recognition Table**:

| File pattern / location | `StyleSourceType` |
|---|---|
| `checkstyle.xml`, `**/checkstyle*.xml`, `config/checkstyle/**` | `CHECKSTYLE` |
| External standalone Spotless config file (only when project model indicates Spotless applied and a non-build config file exists) | `SPOTLESS` |
| `pmd.xml`, `ruleset.xml`, `**/pmd*.xml`, `config/pmd/**` | `PMD` |
| `.editorconfig` — all occurrences found under the project root | `EDITOR_CONFIG` |
| Any file under `.idea/codeStyles/` | `IDE_CODE_STYLE` |

- **FR-007**: The scan layer MUST NOT parse the contents of style config files for style facts; config file contents are only read when extracting linter rules (Checkstyle, PMD).

- **FR-008**: The scan layer supports rule extraction for **Checkstyle and PMD only**. SpotBugs, ErrorProne, and other static analysis tools are out of scope for this sprint. The scan layer MUST collect only linter rules that are actually applied in the build; it MUST NOT enumerate tool default catalogs.

  - **Maven applied-state**: a tool is applied when its plugin entry appears in `<plugins>` (not only `<pluginManagement>`). Specifically: `maven-checkstyle-plugin` (groupId `com.puppycrawl.tools`) for Checkstyle; `maven-pmd-plugin` (groupId `org.apache.maven.plugins`) for PMD.
  - **Gradle applied-state**: inferred from task names in the External System task node list. Presence of `checkstyleMain` or `checkstyleTest` tasks indicates Checkstyle is applied; presence of `pmdMain` or `pmdTest` tasks indicates PMD is applied.

  If a linter tool is applied in the build but its config file cannot be read (missing or unresolvable), the tool MUST be recorded as applied with its rules in an error/unresolvable state — it MUST NOT be silently omitted. The linters section remains `Ok` in this case.

  If a Checkstyle config references an imported or external config (e.g., `SuppressionFilter`, a URL reference, or a classpath resource), rules from imported files are NOT resolved in this sprint — only locally-parsed rules are collected.

  When multiple configurations of the same linter tool coexist (e.g., separate Checkstyle configs for main and test source sets), each configuration is collected independently and all rules are included in the aggregate.

- **FR-009**: For each applied linter rule, the scan layer MUST record its severity (`ERROR` / `WARNING` / `INFO`) as declared in the tool's config file. When a rule declares no explicit severity in its config file, `INFO` is used as the fallback severity. The `breaksBuild: Boolean?` flag from the tool descriptor MUST be denormalized onto each `ActiveRule` record.

- **FR-010**: The scan layer MUST record the build-failure flag for each applied linter tool. When this fact cannot be determined from the available project model (e.g., all Gradle linters due to TAPI limitations), it MUST be recorded as `null` ("not detected") — never guessed or defaulted to `true` or `false`.

- **FR-011**: Spotless MUST be recorded only as a code style source (and only when a standalone external config file exists); it MUST NOT produce linter rule entries.

- **FR-012**: The scan layer MUST detect test frameworks from test-scoped declared dependencies using the **closed Known Test Framework Registry** below. Test-scoped dependencies that do not match a known framework MUST be recorded as "unknown test dependency" — not silently dropped and not misclassified as a named framework. The version recorded for a detected framework is the dependency's resolved version as returned by the port.

  **Test scope definition**:
  - Maven: `<scope>test</scope>` only.
  - Gradle: configurations `testImplementation`, `testCompileOnly`, or `testRuntimeOnly`.

**Known Test Framework Registry** (closed list for this sprint; prefix match on groupId unless artifactId constraint noted):

| groupId | artifactId constraint | Canonical name |
|---|---|---|
| `org.junit.jupiter` | — | JUnit 5 |
| `org.junit.vintage` | — | JUnit 4 (Vintage) |
| `org.junit.platform` | — | JUnit Platform |
| `junit` | `junit` (exact) | JUnit 4 |
| `org.mockito` | — | Mockito |
| `org.assertj` | — | AssertJ |
| `org.hamcrest` | — | Hamcrest |
| `org.testcontainers` | — | Testcontainers |
| `io.cucumber` | — | Cucumber |
| `org.awaitility` | — | Awaitility |
| `io.rest-assured` | — | REST Assured |
| `org.spockframework` | — | Spock |
| `org.testng` | — | TestNG |

- **FR-013**: The scan layer MUST record test source directory paths (project-relative) and the file-naming suffixes found in test source files, recognised against the closed suffix set: `Test`, `Tests`, `IT`, `ITCase`, `Spec`. Each recognised suffix is recorded verbatim — "raw observed" means the token value is unmodified (e.g., `IT` recorded as `"IT"`, not normalised into a regex or pattern), NOT an unbounded enumeration of arbitrary endings. Class names that do not end with a known token contribute nothing. The plugin does not classify the team's naming convention — it records the set of observed known suffixes and leaves convention classification to the downstream consumer (LLM). This aligns with the closed-list approach used for test framework detection (FR-012).

- **FR-014**: When JaCoCo is applied, the scan layer MUST record the coverage threshold as a single representative `Double` value in the range 0.0–1.0 (ratio, not percentage). When JaCoCo is applied for reporting only (no threshold/check rule configured), `coverageThreshold` MUST be `null`. When the threshold value is present but cannot be read from the project model, it MUST also be `null`.

- **FR-015**: The scan layer MUST record each module's build-system identifier as its name (Gradle: `:module-name`; Maven: `module-name`), its declared external dependencies (coordinate + version), and its inter-module dependency links by module name. If two modules declare an identical inter-module dependency, the link is recorded once — duplicates are deduplicated by module name. Modules with no source roots MUST still appear in the structure section with their dependency links intact; their `packageSegments` contribution is empty.

- **FR-016**: The scan layer MUST collect raw package structure data: root packages and their second-level segments (fixed depth, no deeper), in dotted Java package notation (e.g., `com.example.web`). Path notation (`com/example/web`) is not used. It MUST NOT classify the organization pattern (by-layer vs. by-feature). Classification is the responsibility of downstream consumers.

- **FR-017**: The `packageOrganization` inference field from the Sprint 1 model MUST be replaced with `packageSegments: List<String>` — a raw-material field containing the collected second-level package paths in dotted Java package notation.

- **FR-018**: When a section has no data to report, it MUST be explicitly marked `Empty` — never silently omitted and never populated with fabricated data. A section that could not be collected due to an unrecoverable error MUST be marked `Error`; it MUST NOT be conflated with `Empty`. The presence of any single fact (e.g., build system identifier, one style source, one applied tool, one module) qualifies the section as `Ok`, not `Empty`.

**State Boundary Reference** (per section):

| Section | `Empty` — no data found | `Error` — data source unresolvable |
|---|---|---|
| Stack | No build system detected and module list is empty | Unrecoverable exception reading build system or module data |
| CodeStyle | No style source files found anywhere in the project tree | Unrecoverable exception during VirtualFile traversal |
| Linters | No linter tools applied anywhere in the build | Unrecoverable exception reading build model entirely; note — a tool applied but with an unresolvable config file keeps the section as `Ok` (not `Error`) |
| Tests | No test-scoped dependencies and no test source roots found | Unrecoverable exception reading dependency or source root data |
| Structure | Module list is entirely empty | Unrecoverable exception reading module structure data |

- **FR-019**: The scan layer MUST be independently testable via unit tests using port fakes, without requiring a running IDE instance.

- **FR-020**: The scan layer MUST NOT text-parse build files (pom.xml, build.gradle, build.gradle.kts, settings files, version catalogs) to extract project facts. Linter config XML files (e.g., `checkstyle.xml`, `pmd.xml`) are not build files and MAY be parsed. The presence of a Spotless task (e.g. `spotlessApply`) in the External System model MAY signal that Spotless is applied, but a `StyleSource` is emitted ONLY when a standalone non-build config file also exists; task name alone is insufficient and reading the build file is not performed. Spotless configured only inline in the Gradle build file generates no `StyleSource` record.

- **FR-021**: If one collector encounters an unrecoverable error during execution (e.g., malformed config file, unexpected model failure), the scan layer MUST continue running the remaining collectors. The failing section is marked `Error`; all other sections proceed independently. When a **partial failure** occurs within a single collector (e.g., build system read successfully but dependency list read fails), the section is recorded as `Ok` with successfully-collected sub-fields populated and failed sub-fields set to `null` or empty — it is NOT forced to `Error`.

### Key Entities

- **DependencyCoordinate**: Identifies a library by groupId + artifactId + resolved version (version may be `null` if unresolvable); the unit of deduplication in the flat stack aggregate.
- **StyleSource**: A discovered code style configuration; holds the source type (`CHECKSTYLE` / `SPOTLESS` / `PMD` / `EDITOR_CONFIG` / `IDE_CODE_STYLE`) and its project-relative file path.
- **ActiveRule**: A linter rule confirmed as enabled in the build; holds the rule identifier, severity (`ERROR` / `WARNING` / `INFO`), tool name, and the denormalized `breaksBuild: Boolean?` flag inherited from its tool (`null` = not detected).
- **Module**: A build sub-project; holds its build-system identifier as name, its declared external dependencies, and its links to other modules it depends on (by module name, deduplicated).
- **PackageTree**: Raw package namespace data collected from the project's source roots; contains root packages and their second-level segments (`packageSegments`) in dotted Java notation (fixed depth, no deeper), without any organization pattern classification.
- **ScanResult**: The root aggregate produced by the scan layer; contains one section entry per domain (Stack, CodeStyle, Linters, Tests, Structure). Each section is one of **three** states:
  - `Ok(data: T)` — section was collected and contains at least one fact.
  - `Empty` — section was scanned successfully but the project has no relevant data for this domain.
  - `Error(cause: String?)` — collection failed for this section; MUST NOT be treated as empty.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For any single-module or multi-module Maven or Gradle project, the scan completes and produces a result with all five sections either `Ok`, `Empty`, or `Error` — no crashes, no silent omissions, and no cascading failures from one collector to another — in 100% of test cases.

- **SC-002**: Dependency collection accuracy: for projects with known build files used as test fixtures, 100% of declared dependency coordinates appear in the scan result; 0% of transitive-only dependencies appear. "Declared" is operationally defined as: what `DependencyPort.getModuleDependencies()` returns. In production this reflects the IntelliJ External System model's explicitly-listed library dependencies; in unit tests `FakeDependencyPort` supplies the list directly. SC-002 is verified by unit tests that do not require a running IDE instance.

- **SC-003**: Linter rule completeness: for projects with Checkstyle or PMD configured in test fixtures, all rules listed in the tool's config file appear in the scan result with correct severities.

- **SC-004**: Honesty under absence: for each section, when no relevant configuration exists in the project, the section is marked `Empty` in 100% of test cases — never populated with fabricated values.

- **SC-005**: All primary collector code paths are exercised by unit tests that run without a running IDE instance.

- **SC-006**: Additive model changes (new fields with defaults, new enum values) are permitted without restriction. Deliberate breaking changes to the model (field removal, type widening, semantic redefinition) are permitted when explicitly documented in this sprint spec with migration guidance. Silently altering the meaning of fields that downstream consumers depend on, without documentation, is prohibited.

## Clarifications

### Session 2026-06-13

- Q: If one collector encounters an error, do the remaining collectors still run? → A: Yes — continue remaining collectors; the failing section is marked with a distinct error/unresolvable state (not empty, not fabricated). Collectors are independent; one failure must not cascade to others.
- Q: If a linter tool is applied in the build but its config file is missing from the project tree, is the tool omitted or flagged? → A: Record the tool as applied with its rules in an error/unresolvable state. The tool's presence in the build is a fact; only the rule details are unresolvable.
- Q: Is a module with no source roots still listed in the structure section? → A: Yes — include it; its package tree is empty. A source-less module (e.g., aggregator or BOM) is a real build participant; its dependency links are valid facts.
- Q: How are test frameworks detected — closed explicit list, test-scope heuristic, or hybrid? → A: Closed explicit list for well-known frameworks (JUnit, Mockito, AssertJ, Testcontainers, etc.); test-scoped dependencies not matching a known framework are recorded as "unknown test dependency" rather than silently dropped or misclassified.

### Session 2026-06-13 — Checklist Resolution (CHK001–CHK035)

- **CHK001** (null version): A dependency with an unresolvable version is included with `null` version — not excluded, not an error. Applied to FR-001.
- **CHK002** (jdkVersion vs languageLevel): These are two distinct `StackInfo` fields. `jdkVersion` = JDK vendor/version string; `languageLevel` = Java source compatibility level. Applied to FR-004.
- **CHK003** (language-level fallback): A module with no explicit language level inherits the project-level default before aggregation. If neither is set, `languageLevel` is `null`. Applied to FR-005.
- **CHK004** (version comparison): Max-version aggregation uses Maven ComparableVersion semantics; lexicographic ordering is the deterministic fallback for strings that cannot be compared by ComparableVersion. Applied to FR-003.
- **CHK005** (scope in Dependency): `Dependency` model carries no scope field. Scope is modeled only in the test collector's port DTO to select test-scoped entries. Assumptions updated to remove the incorrect "scope is recorded as a fact" statement.
- **CHK006** (path reference point): Project-relative means relative to the project root directory (`ProjectUtil.guessProjectDir()`), using `/` separators. Applied to FR-006.
- **CHK007** (style source recognition table): Recognition table added to spec immediately after FR-006.
- **CHK008** (Spotless detection): A `StyleSource` of type `SPOTLESS` is produced only when a standalone external Spotless config file exists. Inline Gradle Spotless configuration is not modeled; FR-020 updated accordingly.
- **CHK009** (multiple .editorconfig): All `.editorconfig` files under the project root are collected — not only the root-level one. Applied to FR-006.
- **CHK010** (.idea/codeStyles scope): All files under `.idea/codeStyles/` are collected as `IDE_CODE_STYLE` sources — not only `Project.xml`. Applied to FR-006.
- **CHK011** (supported linters): Checkstyle and PMD only. SpotBugs, ErrorProne, and others are out of scope for this sprint. Applied to FR-008.
- **CHK012** (default severity): When a rule declares no explicit severity, `INFO` is used as the fallback. Applied to FR-009.
- **CHK013** (applied-state criteria per build system): Maven = plugin present in `<plugins>` (not just `<pluginManagement>`). Gradle = task names `checkstyleMain`/`checkstyleTest` for Checkstyle, `pmdMain`/`pmdTest` for PMD. Applied to FR-008.
- **CHK014** (import resolution): Rules from imported or externally-referenced config files are not resolved in MVP — only locally-parsed rules are collected. Applied to FR-008.
- **CHK015** (multiple configurations): Multiple configurations of the same linter tool are each collected independently; all rules are included in the aggregate. Applied to FR-008.
- **CHK016** (denormalization): `breaksBuild` from the tool descriptor is explicitly denormalized onto each `ActiveRule`. Applied to FR-009.
- **CHK017** (test framework closed list): Full closed registry of 13 entries added after FR-012.
- **CHK018** (test scope definition): Maven: `scope=test` only. Gradle: `testImplementation`, `testCompileOnly`, `testRuntimeOnly`. Applied to FR-012.
- **CHK019** (naming conventions): Raw observed file-naming suffixes collected as-is; no normalization. Applied to FR-013.
- **CHK020** (threshold unit): Coverage threshold is a `Double` in 0.0–1.0 range (ratio). Applied to FR-014.
- **CHK021** (JaCoCo reporting-only): JaCoCo applied for reporting only (no check rule) → `coverageThreshold = null`. Applied to FR-014.
- **CHK022** (framework version): The version recorded is the dependency's resolved version as returned by the port. Applied to FR-012.
- **CHK023** (module name): Module name is the build-system identifier (Gradle: `:module-name`; Maven: `module-name`), not the IntelliJ display name. Applied to FR-015.
- **CHK024** (package notation): Dotted Java package notation only (e.g., `com.example.web`). Path notation (`com/example/web`) is not used. Applied to FR-016.
- **CHK025** (duplicate inter-module dep): Deduplicate — identical inter-module dependency links are recorded once. Applied to FR-015 and Edge Cases.
- **CHK026** (field name): The raw-material field replacing `packageOrganization` is named `packageSegments`. Applied to FR-017.
- **CHK027** (fully source-less project): Structure section is `Empty` only when the module list is entirely empty. A project where all modules have no source roots still has a non-empty module list → `Ok`. Applied to FR-018 state boundary table.
- **CHK028** (Ok boundary): Any single fact makes the section `Ok` — build system identifier alone, one style source, one applied tool, one framework, or one module each qualify. Applied to FR-018.
- **CHK029** (ScanResult states): `ScanResult` has three states per section: `Ok(data)` / `Empty` / `Error(cause)`. Fixed in Key Entities; `Empty` ≠ `Error` by definition.
- **CHK030** (state boundary table): Per-section {Empty, Error} boundary table added to FR-018.
- **CHK031** (partial failure): A partial failure within a single collector → `Ok` with successfully-collected sub-fields populated, failed sub-fields null/empty. Not forced to `Error`. Applied to FR-021.
- **CHK032** (SC-006): Reworded to permit deliberate documented breaking changes while prohibiting silent semantic breakage. Applied to SC-006.
- **CHK033** (Gradle linter scenario): Acceptance scenario 5 added to User Story 3 covering Gradle applied-state detection and `breaksBuild = null`.
- **CHK034** (SC-002 testability): "Declared" operationally defined via port contract (`DependencyPort.getModuleDependencies()`); SC-002 verified by unit tests using `FakeDependencyPort`. Applied to SC-002.
- **CHK035** (linter unit test specificity): Satisfied by CHK011 (Checkstyle/PMD enumerated), CHK012 (INFO fallback), and CHK014 (no import resolution) — these constraints yield distinct independently-testable code paths for each tool.

## Assumptions

- The scan layer is invoked by the plugin's internal orchestration layer, not directly by end users.
- Maven and Gradle are the only supported build systems for this sprint; other systems (Ant, Bazel, etc.) are out of scope.
- Only JVM/Java projects are in scope for this sprint.
- The `Dependency` model carries no scope field. Scope is used only within the test collector's port DTO to select test-scoped entries. The flat stack aggregate and module structure sections include all declared dependencies regardless of scope.
- Per-module breakdown of non-dependency sections (codestyle, linters, tests) is out of scope; those sections aggregate project-wide.
- Transitive dependency resolution is not performed in this sprint.
- The IntelliJ External System project model is the authoritative data source; direct parsing of build files is prohibited.
- Precise Gradle build-failure flag detection and detailed JaCoCo threshold structure are tracked as tech debt and are not required for this sprint.
- Inter-module dependency links are recorded by module name, not by module object reference.
