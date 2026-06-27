# Contract: OutputFormatters API

**Module**: `:shared` — `dev.zahaand.projectscan.shared`
**File**: `OutputFormatters.kt`
**Consumers**: `:prompt` (`PromptGenerator`), root project (`ScanResultRenderer`)
**Updated**: 2026-06-27 (CHK032 — moved from :model to :shared; CHK001/CHK008/CHK023/CHK026 format fixes)

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
| Tech Stack — uniform group (>1 artifact, all same non-null version) | `groupId:* @ version` header; then `artifactId` per artifact (no version) |
| Tech Stack — per-artifact (single artifact, mixed versions, or any null version) | `groupId:artifactId:version` or `groupId:artifactId` if version null |
| Module graph | `moduleName → [dep1, dep2]` per module; omit modules with no `moduleDependencies` |
| Version discrepancies — entry | `groupId:artifactId → {moduleName: version, ...}` (modules sorted lexicographically) |
| Version discrepancies — no discrepancies | explicit `none` notice (block not omitted) |
| Package segments | `Package segments: seg1, seg2, ...` (comma-separated); omit if empty |
| Testing — framework | `Framework: name version` (deduplicated by (name, version)) |
| Testing — source root | `src/test/java — N modules` (N = raw `sourceRoots` entries normalising to that template) |

## Invariants

- `groupDependencies` outputs groups in lexicographic order by `groupId` (FR-001).
- `detectVersionDiscrepancies` result is sorted lexicographically by `groupId`, then `artifactId`; module names within each entry sorted lexicographically (FR-006).
- `normalizeSourceRoots` result is sorted by `relativePath` asc; count = raw entry occurrences (FR-009).
- All functions return empty collections (not null) for empty inputs.
- No function throws for any valid model input.
- Same non-empty `ScanResult` → byte-identical section body from both consumers (NFR-001, SC-006). Empty/error wrapper strings are excluded from this invariant per FR-011.
