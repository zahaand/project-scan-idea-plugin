# Contract: OutputFormatters API

**Module**: `:model` — `dev.zahaand.projectscan.model`  
**File**: `OutputFormatters.kt`  
**Consumers**: `:prompt` (`PromptGenerator`), root project (`ScanResultRenderer`)

## Function Signatures

```kotlin
fun groupDependencies(deps: List<Dependency>): List<DependencyGroup>

fun detectVersionDiscrepancies(modules: List<Module>): List<VersionDiscrepancy>

fun deduplicateFrameworks(frameworks: List<TestFramework>): List<TestFramework>

fun normalizeSourceRoots(roots: List<String>): List<SourceRootTemplate>
```

## Rendering Contracts (applied identically in both consumers)

| Section | Rendering rule |
|---------|---------------|
| Tech Stack — uniform group | `groupId:* @ version` (one line per group) |
| Tech Stack — mixed group | One line per artifact: `groupId:artifactId:version` (or no version if null) |
| Module graph | `moduleName → [dep1, dep2]` per module; omit modules with empty `moduleDependencies` |
| Version discrepancies | `groupId:artifactId → {moduleName: version, moduleName: version}` per discrepancy |
| Testing — framework | `Framework: name version` (deduplicated) |
| Testing — source root | `src/test/java — N modules` (N from `SourceRootTemplate.count`) |

## Invariants

- `groupDependencies` preserves the order of first-occurrence `groupId` from the input list.
- `detectVersionDiscrepancies` result is sorted (`groupId` asc, then `artifactId` asc).
- `normalizeSourceRoots` result is sorted by `relativePath` asc.
- All functions return empty collections (not null) for empty inputs.
- No function throws for any valid model input.
