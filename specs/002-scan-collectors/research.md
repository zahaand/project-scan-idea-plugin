# Research: Scan — Project Facts Collectors

**Phase**: 0 | **Date**: 2026-06-13 | **Plan**: [plan.md](plan.md)

No blocking NEEDS CLARIFICATION items from Technical Context — all resolved via spec,
constitution, and codebase inspection. This document records API decisions and design choices
that inform data model and implementation.

---

## R-001: IntelliJ External System API for Module Dependencies

**Decision**: Use `ExternalProjectDataCache` + `ExternalSystemApiUtil.findAll()` to walk the
`DataNode` tree per module.

**Key APIs**:
- `ExternalProjectDataCache.getInstance(project)` — entry point; returns the cached external
  project data tree for a given system ID and project path.
- `ExternalSystemApiUtil.findAll(projectNode, ModuleData.KEY)` — enumerates module nodes.
- `ExternalSystemApiUtil.findAll(moduleNode, LibraryDependencyData.KEY)` — enumerates a module's
  library (external) dependencies.
- `ExternalSystemApiUtil.findAll(moduleNode, ModuleDependencyData.KEY)` — enumerates a module's
  inter-module dependencies.
- `LibraryDependencyData.target.groupId / artifactId / version` — resolved coordinate.

**Maven fallback**: `MavenProjectsManager.getInstance(project).projects` gives `MavenProject`
objects with `getMavenProject().getDependencies(): List<MavenArtifact>`. Use
`MavenArtifact.groupId`, `artifactId`, `version`. This provides resolved (effective POM) values
and is preferable for Maven projects where the External System cache may be stale.

**Alternatives considered**:
- Parsing `pom.xml` / `build.gradle` as text — rejected; violates Constitution Principle II and
  produces unreliable, unresolved versions.
- `ModuleManager` + `OrderEntry` — provides library order entries but not resolved version
  strings reliably; External System cache is the authoritative resolved source.

---

## R-002: JDK and Language Level

**Decision**: Use `LanguageLevelProjectExtension` for the project-level language level; iterate
`LanguageLevelModuleExtension` per module; take the maximum.

**Key APIs**:
- `LanguageLevelProjectExtension.getInstance(project).languageLevel: LanguageLevel` — project-
  level setting (may reflect the IDE default if not explicitly set).
- `LanguageLevelModuleExtension.getInstance(module).languageLevel: LanguageLevel?` — per-module
  override; null means inherits from project.
- `LanguageLevel.toJavaVersion().toFeatureString()` — produces a canonical string like "17".
- JDK version: `ModuleRootManager.getInstance(module).sdk?.name` or
  `ProjectJdkTable.getInstance().getSdksOfType(JavaSdk.getInstance())`.

**Aggregation rule**: iterate all modules; for modules where `languageLevel` is null, fall back
to the project-level value; take the maximum across all effective per-module values.

---

## R-003: Build System Detection

**Decision**: Check which External System manager has data for the project.

**Key APIs**:
- `ExternalSystemUtil.getDefaultExternalSystemId(project): ProjectSystemId?` — returns
  `MavenId` or `GradleConstants.SYSTEM_ID`.
- Alternatively: `MavenProjectsManager.getInstance(project).isMavenizedProject` for Maven;
  `GradleSettings.getInstance(project).linkedProjectsSettings.isNotEmpty()` for Gradle.

---

## R-004: Style Source File Discovery

**Decision**: Enumerate known config file names relative to the project base directory using
`ProjectUtil.guessProjectDir(project)` and recursive VirtualFile descent (max depth 2).

**Files to detect**:

| File / Path | StyleSourceType |
|---|---|
| `checkstyle.xml`, `config/checkstyle/checkstyle.xml`, `**/checkstyle*.xml` | CHECKSTYLE |
| `spotless*.xml` or Spotless tasks in build (detected via `IjLinterAdapter`) | SPOTLESS |
| `pmd.xml`, `ruleset.xml`, `config/pmd/**` | PMD |
| `.editorconfig` (project root and parents up to VCS root) | EDITOR_CONFIG |
| `.idea/codeStyles/Project.xml`, `.idea/codeStyles/codeStyleConfig.xml` | IDE_CODE_STYLE |

**Key APIs**:
- `ProjectUtil.guessProjectDir(project): VirtualFile?` — project base directory.
- `VirtualFile.findFileByRelativePath(path)` — targeted lookup for known paths.
- `VirtualFileManager.getInstance().findFileByUrl(url)` — for `.editorconfig` parent-dir walk.
- `.idea/codeStyles/` — check `ProjectUtil.guessProjectDir()/.idea/codeStyles/`.

**Path representation**: all `StyleSource.path` values are project-relative (relative to
`ProjectUtil.guessProjectDir()`), using `/` separators.

---

## R-005: Linter Applied-State Detection

**Decision**: Maven — use `MavenProjectsManager` plugin list. Gradle — infer from task names in
External System data (standard TAPI does not expose applied plugin extensions).

**Maven**:
- `MavenProject.findPlugin("org.apache.maven.plugins", "maven-checkstyle-plugin")` → non-null =
  applied. (Note: `com.puppycrawl.tools` is the Checkstyle *library* groupId, not the Maven
  plugin — using it in `findPlugin` silently returns null on standard projects.)
- `MavenProject.findPlugin("org.apache.maven.plugins", "maven-pmd-plugin")` → non-null = applied.
- `MavenPlugin.getConfigurationElement()?.getChildText("failOnViolation")` → hardness (Checkstyle).
  (`failsOnError` tests Checkstyle's own execution errors, not build failure on rule violations.)
- `MavenPlugin.getConfigurationElement()?.getChildText("failOnViolation")` → hardness (PMD).
- If the element is absent or the plugin is in `<pluginManagement>` but not `<plugins>`,
  applied-state = false.

**Gradle**:
- Standard Tooling API (`GradleBuild` model) does not expose plugin extension state.
- Applied-state heuristic: look for tasks named `checkstyleMain`, `checkstyleTest` (Checkstyle
  plugin) or `pmdMain`, `pmdTest` (PMD plugin) in `ExternalProjectDataCache` task nodes.
  Presence of these tasks reliably indicates the plugin is applied.
- `breaksBuild` for Gradle: always `null` ("not detected") — consistent with FR-010 and
  clarification from spec session.

**Rationale for Gradle heuristic**: The Checkstyle and PMD Gradle plugins create predictably
named tasks; this is stable across Gradle versions and does not require a custom Tooling API
model builder (deferred to tech debt).

---

## R-006: Checkstyle XML Rule Parsing

**Decision**: Use `javax.xml.parsers.DocumentBuilderFactory` (standard Java) for DOM parsing.
No third-party XML library needed; available in all IntelliJ plugin runtimes.

**Format** (standard Checkstyle config):
```xml
<module name="Checker">
  <property name="severity" value="warning"/>
  <module name="TreeWalker">
    <module name="MethodLength">
      <property name="severity" value="error"/>
    </module>
    <module name="MagicNumber"/>  <!-- inherits parent severity -->
  </module>
</module>
```

**Parsing rules**:
1. Walk all `<module>` elements recursively.
2. For each module that is NOT `Checker` or `TreeWalker` (i.e., actual rule modules), record its
   `name` attribute as `ruleId`.
3. Severity: the `<property name="severity" value="..."/>` child. If absent, inherit from the
   nearest ancestor `<module>` that declares severity; if none, default to `INFO` (see FR-009).
4. Severity mapping: `"error"` → `ERROR`, `"warning"` → `WARNING`, `"info"` / `"ignore"` → `INFO`.

---

## R-007: PMD Ruleset XML Parsing

**Decision**: DOM parsing, same library as Checkstyle.

**Format** (standard PMD ruleset):
```xml
<ruleset name="Custom Rules">
  <rule ref="category/java/bestpractices.xml/AbstractClassWithoutAbstractMethod">
    <priority>2</priority>
  </rule>
  <rule ref="category/java/codestyle.xml/LongVariable"/>
</ruleset>
```

**Parsing rules**:
1. Enumerate all `<rule>` elements.
2. `ruleId` = the `ref` attribute value (full category path, e.g.,
   `category/java/bestpractices.xml/AbstractClassWithoutAbstractMethod`).
3. Priority → severity: 1–2 = `ERROR`, 3 = `WARNING`, 4–5 = `INFO`. Default if absent: `INFO` (see FR-009).

---

## R-008: Package Structure Enumeration

**Decision**: Enumerate source root directories; walk two levels of subdirectory names to derive
root packages and second-level segments.

**Key APIs**:
- `ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.SOURCE)` — main
  source roots.
- `VirtualFile.children` — iterate immediate subdirectories of each source root → root packages.
- Recurse one more level → second-level segments (full dotted path: `rootPkg.childPkg`).

**Rules**:
- Skip directories whose names start with `.` or are not valid Java identifiers.
- Aggregate across all modules; deduplicate.
- Result stored in `PackageTreeData(rootPackages, secondLevelSegments)`.

**Alternatives considered**:
- `JavaPsiFacade.findPackage()` + `getSubPackages()` — more semantic but requires read action
  and PSI initialisation overhead; VirtualFile traversal is lighter and sufficient for
  structural discovery.

---

## R-009: Test Framework Detection — Explicit Registry

**Decision**: Closed set of known framework `groupId` prefixes (or `groupId:artifactId` for
disambiguating artefacts). Anything test-scoped but NOT matched → "unknown test dependency".

**Known framework registry** (initial set; extensible in future sprints):

| groupId (prefix match) | Canonical name |
|---|---|
| `junit` | JUnit 4 |
| `org.junit.jupiter` | JUnit 5 |
| `org.junit.vintage` | JUnit 4 (Vintage) |
| `org.junit.platform` | JUnit Platform |
| `org.mockito` | Mockito |
| `org.assertj` | AssertJ |
| `org.testcontainers` | Testcontainers |
| `io.cucumber` | Cucumber |
| `org.awaitility` | Awaitility |
| `io.rest-assured` | REST Assured |
| `org.spockframework` | Spock |
| `org.hamcrest` | Hamcrest |
| `org.testng` | TestNG |

**Matching rule**: `dependency.groupId.startsWith(knownGroupId)` for prefix entries. Exact
`groupId:artifactId` match reserved for cases where groupId alone is ambiguous.

**Test scope detection**: A dependency is considered "test-scoped" if its scope/configuration
contains `test` (case-insensitive) — e.g., Maven `<scope>test</scope>`, Gradle
`testImplementation`, `testCompileOnly`, `testRuntimeOnly`.

---

## R-010: JaCoCo Threshold

**Decision**: Maven — read from `jacoco-maven-plugin` configuration via `MavenProject` API.
Gradle — `null` (not available via standard TAPI).

**Maven path**:
- `MavenProject.findPlugin("org.jacoco", "jacoco-maven-plugin")` → check `<executions>`
  containing `check` goal → `<configuration><rules><rule><limits><limit><minimum>` value.
- Parse the `<minimum>` text as `Double` (e.g., `"0.80"` → `0.8`).
- If multiple `check` executions exist, take the first; document as known simplification.

**Gradle**: Return `null` per FR-014. The detailed threshold structure is tracked as tech debt.

---

## R-011: ScanService Error Isolation

**Decision**: Each collector is called inside an individual `try/catch(Exception)` block in
`ScanService`. A caught exception produces `SectionResult.Error(cause = e.message)` for that
section; all other collectors still execute (FR-021).

**Log**: Each caught error is logged at WARN level via IntelliJ's `Logger` — sufficient for
diagnostics without exposing stack traces to end users.
