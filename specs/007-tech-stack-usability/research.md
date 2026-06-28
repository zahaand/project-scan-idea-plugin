# Research: Sprint 7 — Tech Stack & Testing Usability Rework

**Date**: 2026-06-28

---

## R1: Maven Direct-Dependency API

### Finding

The IntelliJ Maven plugin (`org.jetbrains.idea.maven`) exposes `MavenProject` with two relevant collections:

- **`mavenProject.dependencies: List<MavenArtifact>`** — all resolved dependencies (declared + transitive + inherited from parent), with versions resolved from the effective model including parent/BOM management.
- **`mavenProject.mavenModel.dependencies: List<MavenDependency>`** — only coordinates declared in **this module's own `pom.xml` `<dependencies>` block**. Versions may be property references (e.g., `${spring.version}`) or absent (managed by parent). No transitive deps appear here.

The current `IjModuleStructureAdapter.mavenModules()` uses `mp.dependencies` (all resolved), which includes transitives. This is the root cause of the ~150-line output on 130-module monorepos.

### Decision: Intersection Approach

```
directDeclaredDeps = mavenProject.dependencies
    .filter { "${it.groupId}:${it.artifactId}" in declaredCoordinates }
    .filter { "${it.groupId}:${it.artifactId}" !in internalModuleCoordinates }
```

where `declaredCoordinates` is the set of `"groupId:artifactId"` from `mavenProject.mavenModel.dependencies`.

This gives:
- **Identity**: from `mavenModel.dependencies` (declared in POM)
- **Version**: from `dependencies` (resolved from effective model, including parent/BOM)

Satisfies FR-003 and remains 100% within the IntelliJ project model (Constitution II).

### Rationale

- `MavenProject.directDependencies` does not exist in the 2025.3.5 API surface.
- Parsing raw `pom.xml` text is prohibited (Constitution II).
- `MavenDependency.version` from `mavenModel` is unreliable (may be `${property}` or blank for BOM-managed deps); the resolved version from `dependencies` is required (FR-003: "version = resolved value from parent/dependencyManagement").
- `mavenModel.dependencies` does NOT include `<dependencyManagement>` entries — only the `<dependencies>` block — so BOM imports don't pollute the declared set.

### Alternatives Considered

| Alternative | Rejected Because |
|---|---|
| Use `directDependencies` property | Does not exist in this API version |
| Parse `pom.xml` directly | Prohibited by Constitution II |
| Use `MavenDependency.version` for resolved value | Unresolved (property refs, blank for BOM deps) |
| Trust `mavenProject.dependencies` scope filtering | `scope` in resolved list is the effective scope, not POm-declared; transitives also appear with valid scopes |

---

## R2: Maven Aggregator Topology API

### Finding

`MavenProject` has no `aggregatorProject` property. The "aggregator" is the POM that lists a module in its `<modules>` section — which may differ from the `<parent>` POM in flat multi-module layouts. Key API facts:

- **`mavenProject.mavenModel.modules: List<String>`** — relative paths of child modules declared in this project's `<modules>` block. Empty for leaf modules.
- **`mavenProject.parentId: MavenId?`** — the `<parent>` POM's maven ID; NOT the aggregator (they can differ in flat layouts).
- **`mavenProject.directory: String`** — absolute path of the directory containing this module's `pom.xml`.

### Decision: Reverse Map via Module Paths

Build a reverse mapping `childDirectory → aggregatorArtifactId` inside `IjModuleStructureAdapter.mavenModules()`:

```kotlin
// Step 1: build reverse map before processing modules
val aggregatorByDir = mutableMapOf<String, String>()
for (mp in mavenProjects) {
    val aggregatorName = mp.mavenId.artifactId ?: mp.displayName
    for (relPath in mp.mavenModel.modules) {
        val childDir = File(mp.directory, relPath).canonicalPath
        aggregatorByDir[childDir] = aggregatorName
    }
}

// Step 2: for each module being processed
val aggregator = aggregatorByDir[File(mp.directory).canonicalPath]  // null = root/ungrouped
```

`aggregator` is the `artifactId` of the module that listed this one in its `<modules>` block, or null for root/top-level modules not listed by any other module.

This is Constitution II-compliant: all data read through `MavenProjectsManager` and `MavenProject` model, not filesystem or raw XML.

### Edge Cases

| Case | Outcome |
|---|---|
| Module not in any `<modules>` list (root of reactor) | `aggregator = null`; appears ungrouped in multi-version entries |
| Module in multiple `<modules>` lists (unusual but possible) | Last writer wins in the map — acceptable for display purposes |
| Symlinks or `..` in relative paths | `canonicalPath` resolves both correctly |
| Aggregator module itself declares a direct dependency | It has an entry with `aggregator = null`; appears first in Tech Stack groupings per spec |

### Alternatives Considered

| Alternative | Rejected Because |
|---|---|
| `MavenProjectsTree.findParentProject(child)` | Returns `<parent>` POM, not aggregator — different in flat layouts |
| `mavenProject.parentId` | Same issue — parent ≠ aggregator |
| `MavenProjectsManager.rootProjects` | Identifies reactor roots, not parent-child aggregator relationships within the tree |

---

## R3: Gradle Denylist Completeness

### Finding

The spec provides an initial list of synthetic/shading artifacts: `asm, objenesis, paranamer, listenablefuture, failureaccess, j2objc-annotations, checker-qual, aopalliance`. These correspond to well-known transitive-only artifacts from:

- Google Guava shading: `com.google.guava:listenablefuture`, `com.google.guava:failureaccess`, `com.google.j2objc:j2objc-annotations`, `org.checkerframework:checker-qual`
- Spring/Mockito bytecode: `org.ow2.asm:asm*`
- Mockito/Hibernate: `org.objenesis:objenesis`
- AOP Alliance: `aopalliance:aopalliance`
- Paranamer: `com.thoughtworks.paranamer:paranamer`

### Decision: Exact groupId:artifactId Denylist with ASM Prefix

```kotlin
private val GRADLE_DENYLIST_EXACT: Set<String> = setOf(
    "org.objenesis:objenesis",
    "com.thoughtworks.paranamer:paranamer",
    "com.google.guava:listenablefuture",
    "com.google.guava:failureaccess",
    "com.google.j2objc:j2objc-annotations",
    "org.checkerframework:checker-qual",
    "aopalliance:aopalliance",
)

private const val GRADLE_DENYLIST_ASM_GROUP = "org.ow2.asm"

fun isDenylisted(groupId: String, artifactId: String): Boolean =
    groupId == GRADLE_DENYLIST_ASM_GROUP ||
        "$groupId:$artifactId" in GRADLE_DENYLIST_EXACT
```

Stored as `companion object` constants in `IjModuleStructureAdapter`, applied in `gradleModules()` only (FR-006 is Gradle-specific; Maven FR-003 handles exclusion by direct-only filtering).

### Rationale

- The spec explicitly states this is a "static, maintained list" — no dynamic discovery.
- `org.ow2.asm` group prefix covers all ASM submodules (`asm`, `asm-commons`, `asm-tree`, etc.) that appear as Gradle deps.
- Remaining entries are exact `groupId:artifactId` matches — conservative (only known-synthetic artifacts).
- List can be extended in future sprints without architecture change (just add a constant).

### Alternatives Considered

| Alternative | Rejected Because |
|---|---|
| Dynamic detection (no denylist) | FR-N1: precise Gradle direct-slice is out of scope for this sprint |
| GroupId-only matching beyond ASM | Too broad — could exclude legitimate artifacts from the same group |
| artifactId-only matching | Would incorrectly exclude e.g. a custom `objenesis` artifact from another group |
