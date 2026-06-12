# Feature Specification: Model — Structured Data Contract for Project-Scan

**Feature Branch**: `001-model-data-contract`  
**Created**: 2026-06-12  
**Status**: Draft  
**Input**: User description: "Sprint 1 — `model`: the structured data contract for project-scan."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Define Root Aggregate with Five Sections (Priority: P1)

A developer consuming the model (e.g., writing a prompt generator or a UI copy button) needs a single, stable root type that organises all project information into predictable sections. They import `ProjectScanModel` and navigate its five typed sections without needing to know how the data was collected.

**Why this priority**: This is the foundational contract. Every other story depends on the root type existing with the correct shape.

**Independent Test**: Construct a `ProjectScanModel` from scratch in a unit test with all sections set to their empty states; verify the object compiles and each section is accessible.

**Acceptance Scenarios**:

1. **Given** a `ProjectScanModel` instance with all-empty sections, **When** a consumer accesses each section, **Then** every section is non-null and represents a valid empty / not-detected state (empty lists, null nullable fields).
2. **Given** a fully-populated `ProjectScanModel`, **When** a consumer reads any field, **Then** the value matches what was passed at construction time (data class copy-semantics).

---

### User Story 2 - Represent the Stack Section (Priority: P1)

A scan producer populates the Stack section with explicitly declared dependencies (coordinate + resolved version), the JDK/language level, and the build system. A prompt consumer reads these fields to generate a technology context preamble.

**Why this priority**: Stack is the most universally useful section; every downstream consumer is expected to use it.

**Independent Test**: Construct a `StackInfo` with two dependencies, a JDK level, and a build system; verify each field round-trips through the data class.

**Acceptance Scenarios**:

1. **Given** a project with no declared dependencies, **When** the Stack section is built, **Then** `dependencies` is an empty list, `jdkVersion` and `languageLevel` are null, and `buildSystem` is null.
2. **Given** a project declaring `org.springframework.boot:spring-boot-starter:3.2.0` and Java 21 on Gradle, **When** the Stack section is built, **Then** the dependency list contains one entry with the correct coordinate and version, `jdkVersion` is `"21"`, and `buildSystem` is `GRADLE`.

---

### User Story 3 - Represent the Code Style Section with Ranked Priority (Priority: P1)

A style consumer (prompt generator or baseline rule engine) needs to determine which style source takes precedence when multiple sources exist. The priority rank must be carried by the model itself — the consumer must not re-implement the rule.

**Why this priority**: Encoding priority in the model is one of the explicit acceptance criteria and affects how every consumer reads the section.

**Independent Test**: Create `StyleSource` instances for each source type; assert their `priority` ranks satisfy: Checkstyle/Spotless/PMD rank < EditorConfig rank < IdeCodeStyle rank (lower rank = higher priority).

**Acceptance Scenarios**:

1. **Given** style sources of types Checkstyle, EditorConfig, and IdeCodeStyle, **When** a consumer sorts them by `StyleSourceType.priority`, **Then** Checkstyle comes first, EditorConfig second, IdeCodeStyle last.
2. **Given** a project with no style files detected, **When** the Code Style section is built, **Then** `sources` is an empty list.

---

### User Story 4 - Represent Linter Rules Section (Priority: P2)

A prompt consumer inspects which linter rules are active, their severity, and whether a violation breaks the build. The section is empty when no linters are wired into the build.

**Why this priority**: Important for code-style context, but the model is still useful without it.

**Independent Test**: Build a `LinterInfo` with two active rules (one error/hard, one warning/soft); assert each rule's fields are preserved.

**Acceptance Scenarios**:

1. **Given** a project with no linter build integration, **When** the Linter section is built, **Then** `activeRules` is an empty list.
2. **Given** a Checkstyle rule `LineLength` with severity `ERROR` and `breaksBuild = true`, **When** the rule is added, **Then** `tool`, `ruleId`, `severity`, and `breaksBuild` all round-trip correctly.

---

### User Story 5 - Represent the Tests Section (Priority: P2)

A prompt consumer reads which test frameworks are present, where tests live, what naming conventions are used, and — if available — the JaCoCo coverage threshold.

**Why this priority**: Test context is valuable for prompt generation but the plugin still works without it.

**Independent Test**: Build a `TestInfo` with JUnit 5 + Mockito, a test source root, a naming pattern, and a coverage threshold of 80 %; verify all fields.

**Acceptance Scenarios**:

1. **Given** a project with no test dependencies or configuration, **When** the Tests section is built, **Then** `frameworks` is an empty list, `sourceRoots` is an empty list, `namingPattern` is null, and `coverageThreshold` is null.
2. **Given** JaCoCo configured at 80 % line coverage, **When** the Tests section is built, **Then** `coverageThreshold` is non-null with value 80.0.

---

### User Story 6 - Represent the Structure Section (Priority: P2)

A prompt consumer reads the module list, their inter-dependencies, package organisation pattern, and root packages to generate an architecture summary.

**Why this priority**: Useful context but the plugin functions without it.

**Independent Test**: Build a `StructureInfo` with two modules, a by-layer organisation pattern, and two root packages; assert each field.

**Acceptance Scenarios**:

1. **Given** a single-module project with no detectable package pattern, **When** the Structure section is built, **Then** `modules` has one entry, `packageOrganisation` is null, and `rootPackages` is an empty list.
2. **Given** a multi-module project with a by-feature layout where `app` depends on `core`, **When** the Structure section is built, **Then** `packageOrganisation` is `BY_FEATURE`, `rootPackages` contains the detected packages, and the `app` module's `moduleDependencies` contains `"core"` while its `declaredDependencies` contains only external Maven/Gradle coordinates.

---

### Edge Cases

- What happens when a dependency has a resolved version of `null` (e.g., BOM-managed without explicit version declaration)? → `resolvedVersion` is nullable.
- What happens when the same style source type appears more than once (e.g., two `.editorconfig` files)? → Multiple `StyleSource` entries with the same type but different paths are allowed.
- What happens when a module declares no external dependencies? → `declaredDependencies` on the module is an empty list; `moduleDependencies` may still be non-empty.
- What happens when a module has no inter-module links? → `moduleDependencies` on the module is an empty list.
- How does the model handle a project that is neither Maven nor Gradle? → `buildSystem` is null.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The model MUST expose a root aggregate type `ProjectScanModel` that holds exactly five typed sections: Stack, CodeStyle, Linters, Tests, and Structure.
- **FR-002**: Every section MUST be representable in an empty / not-detected state using empty collections and nullable fields — no sealed Present/Empty wrapper.
- **FR-003**: The `StackInfo` section MUST carry a `dependencies: List<Dependency>` representing the flat union of all modules' declared dependencies, deduplicated by groupId + artifactId (no transitive dependencies). It also carries a nullable `jdkVersion`, a nullable `languageLevel`, and a nullable `buildSystem` enum (`MAVEN`, `GRADLE`).
- **FR-004**: The `CodeStyleInfo` section MUST carry a list of `StyleSource`, where each source has a `StyleSourceType` and a `path`. `StyleSourceType` MUST encode a `priority` rank such that Checkstyle/Spotless/PMD < EditorConfig < IdeCodeStyle.
- **FR-005**: The `LinterInfo` section MUST carry a list of `ActiveRule`. Each rule MUST have a `ruleId`, a `tool` (string), a `severity` enum (`ERROR`, `WARNING`, `INFO`), and a `breaksBuild` boolean.
- **FR-006**: The `TestInfo` section MUST carry a list of `TestFramework` (name + version), a list of `sourceRoots` (strings), a nullable `namingPattern`, and a nullable `coverageThreshold` (Double).
- **FR-007**: The `StructureInfo` section MUST carry a list of `Module`, a nullable `packageOrganisation` enum (`BY_LAYER`, `BY_FEATURE`), and a list of `rootPackages` (strings). Each `Module` MUST carry its `name`, a `declaredDependencies: List<Dependency>` for external Maven/Gradle coordinates specific to that module, and a `moduleDependencies: List<String>` for inter-module references (sibling module names). Both lists may be empty.
- **FR-008**: All model types MUST be plain Kotlin data classes with no IntelliJ Platform dependencies.
- **FR-009**: The model MUST be covered by unit tests that verify construction, field access, empty-state representability, and style-source priority ordering.
- **FR-010**: The model definition MUST NOT include any data collection, prompt generation, or UI logic.

### Key Entities

- **ProjectScanModel**: Root aggregate; owns all five sections.
- **StackInfo**: Declared dependencies, JDK/language level, build system.
- **Dependency**: Maven coordinate (groupId, artifactId) + resolved version.
- **BuildSystem**: Enum — `MAVEN`, `GRADLE`.
- **CodeStyleInfo**: Ordered list of style sources.
- **StyleSource**: A detected style configuration file identified by type and path.
- **StyleSourceType**: Enum with embedded priority rank — `CHECKSTYLE`, `SPOTLESS`, `PMD`, `EDITOR_CONFIG`, `IDE_CODE_STYLE`.
- **LinterInfo**: List of active linter rules.
- **ActiveRule**: A single enabled linter rule with identity, tool, severity, and build-breaking flag.
- **RuleSeverity**: Enum — `ERROR`, `WARNING`, `INFO`.
- **TestInfo**: Test frameworks, source locations, naming pattern, coverage threshold.
- **TestFramework**: Framework name + version string.
- **StructureInfo**: Modules, package organisation, root packages.
- **Module**: Module name + `declaredDependencies` (external `List<Dependency>`) + `moduleDependencies` (inter-module `List<String>` of sibling module names).
- **PackageOrganisation**: Enum — `BY_LAYER`, `BY_FEATURE`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All five sections of `ProjectScanModel` can be independently constructed in unit tests within the same test run with zero failures.
- **SC-002**: An empty `ProjectScanModel` (all sections empty/null) can be constructed and all fields accessed without any runtime error.
- **SC-003**: Style source priority order is verified by at least one dedicated test that exercises all five source types.
- **SC-004**: No IntelliJ Platform class appears in the model module's compile or runtime classpath.
- **SC-005**: The entire model test suite completes in under 5 seconds on a developer workstation.
- **SC-006**: The five-section structure of `ProjectScanModel` and the meaning of all existing fields remain stable across Sprints 2–5; no consumer written against Sprint 1's model requires changes due to field renames, removals, or semantic redefinitions.
- **SC-007**: Additive refinements to the model (new fields, new enum values, new source types) driven by Sprint 2 scan findings are acceptable without violating this contract, provided they do not alter the interpretation of any field that already exists.

## Assumptions

- The model module is a standard Kotlin/JVM module inside the existing Gradle build; the IntelliJ plugin SDK is a dependency only of the plugin shell module, not of the model module.
- `resolvedVersion` on `Dependency` may be null for BOM-managed or otherwise unresolved versions — this is an intentional design choice, not a defect.
- Style source `path` is a project-relative string (e.g., `config/checkstyle/checkstyle.xml`); absolute path resolution is the responsibility of the scan layer (Sprint 2). Inline style configuration embedded in build scripts (no separate file) is not representable in `StyleSource` in Sprint 1 — it is out of scope until the scan layer confirms it is encountered.
- `namingPattern` in `TestInfo` is a simple glob or regex string (e.g., `"**/*Test.kt"`); pattern syntax validation is out of scope for the model.
- `StackInfo.dependencies` is the project-wide flat union (deduplicated by groupId + artifactId) of all modules' declared external dependencies. Per-module breakdowns are available via `StructureInfo.Module.declaredDependencies`. Deduplication keeps the highest resolved version when two modules declare the same coordinate at different versions; version conflict resolution is the scan layer's responsibility.
- `coverageThreshold` is a percentage value in the range 0.0–100.0; enforcement of this range is the responsibility of the scan layer.
- The model is immutable by convention (Kotlin data classes); defensive copying of list fields is not required in Sprint 1 but may be added later if mutation is observed.

## Clarifications

### Session 2026-06-12

- Q: Should `Module.declaredDependencies` include inter-module references (sibling module names) alongside external Maven/Gradle coordinates, or are they separate? → A: Separate — `declaredDependencies: List<Dependency>` for external coords only; add `moduleDependencies: List<String>` for inter-module links.
- Q: For multi-module projects, should `StackInfo.dependencies` be root/parent-level only, a flat union across all modules, or omitted? → A: Flat union (deduplicated by groupId + artifactId) across all modules — the full project dependency fingerprint.
- Q: Should `StyleSource` represent inline build-script style configs (no file path), or file-based only? → A: File-based only; inline style config is out of scope for Sprint 1.
