<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Project-scan-idea-plugin Changelog

## [Unreleased]

### Pending Constitution Amendment — Sprint 9 (PENDING)

The following constitution changes are owed as a single amendment package in Sprint 9.
The constitution version is NOT bumped in Sprint 7 (FR-N3); the full amendment lands together.

- **(a) Add `:shared` to §Project Structure component table**: Sprint 7 introduces `shared` as the
  central rendering component consumed by both `:prompt` and `:ui`. The constitution's component
  table and dependency direction rules must be updated to reflect this: `:prompt` and `:ui` MAY
  depend on `:shared`; `:scan` MUST NOT depend on `:shared`.

- **(e) Legitimise `:ui` as composition root**: `:ui` currently depends on `:scan`, `:baseline`,
  and `:prompt` in its role as the plugin's composition root (wiring `ScanService →
  BaselineRuleProvider → PromptGenerator → render`). The constitution's blanket "scan, prompt, and
  ui MUST NOT depend on each other" must be narrowed to prohibit only the `scan ↔ prompt` coupling;
  the `ui → scan/baseline/prompt` wiring must be explicitly permitted. Tracked as a deliberate
  deviation in `specs/007-tech-stack-usability/plan.md §Tracked Deviations`.

### Breaking Changes (Sprint 2 — `002-scan-collectors`)

The following `:model` changes are **deliberate breaking changes** documented per SC-006 and
constitution §Governance. No migration shim is provided; callers must update on upgrade.

- **`StructureInfo`**: field `packageOrganisation: PackageOrganisation?` and enum class
  `PackageOrganisation` removed. Replaced by two raw-data fields: `rootPackages: List<String>`
  (first-level package names) and `packageSegments: List<String>` (dotted second-level paths,
  e.g. `com.example.web`). Downstream consumers that classified package layout must now derive
  their own classification from these raw values (FR-017).

- **`ActiveRule.breaksBuild`**: widened from `Boolean` to `Boolean?`. A `null` value means the
  build-failure flag could not be determined from the project model (e.g. all Gradle linter tools
  due to Tooling API limitations). Callers that assumed a non-null value must handle `null` as
  "not detected" (FR-009 / FR-010).

- **`TestInfo`**: field `namingPattern: String?` removed. Replaced by
  `namingSuffixes: List<String>` — the raw set of observed known suffix tokens (`Test`, `Tests`,
  `IT`, `ITCase`, `Spec`) found in test source file names. Convention classification is left to
  the downstream consumer (FR-013).

### Added (Sprint 2 — `002-scan-collectors`)

- `ScanResult` type in `:model`: discriminated aggregate with five `SectionResult<T>` fields
  (`stack`, `codeStyle`, `linters`, `tests`, `structure`), each `Ok(data)`, `Empty`, or
  `Error(cause)`.
- `TestInfo.unknownTestDependencies: List<Dependency>` — test-scoped dependencies not matched
  by the Known Test Framework Registry, recorded instead of silently dropped (FR-012).
- `LinterInfo.toolsWithUnresolvableConfig: List<String>` — linter tools applied in the build
  but whose config file could not be resolved or parsed (additive, default `emptyList()`).
- `StackInfo.jdkVersion: String?` — raw JDK vendor/version string (e.g. `"temurin-21"`), kept
  as the vendor-determined format without normalisation (FR-004).
- `:scan` Gradle submodule with five section collectors (Stack, CodeStyle, Linters, Tests,
  Structure), read-only port interfaces, IntelliJ adapter implementations, and in-memory fakes
  for IDE-free unit testing (FR-019 / SC-005).
