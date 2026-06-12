# Component Contract: `model`

**Component**: `model`  
**Gradle coordinate**: `:model`  
**Root package**: `dev.zahaand.projectscan.model`  
**Date**: 2026-06-12

This document defines the boundary contract for the `model` component. It is the authoritative reference for any component that produces or consumes `ProjectScanModel`.

---

## What `model` exposes

The `model` component exposes exactly one public API surface: the types defined in `data-model.md`. There are no services, no factories beyond data class constructors, and no utilities.

**Public types** (all in `dev.zahaand.projectscan.model`):

```
ProjectScanModel
StackInfo           Dependency          BuildSystem
CodeStyleInfo       StyleSource         StyleSourceType
LinterInfo          ActiveRule          RuleSeverity
TestInfo            TestFramework
StructureInfo       Module              PackageOrganisation
```

---

## Producer contract (scan layer obligations)

A producer (`scan` component) MUST:

1. Always supply all five sections when constructing `ProjectScanModel`. No section may be null.
2. Use the section's default constructor (empty state) when a section cannot be detected — never omit a section.
3. Populate `StackInfo.dependencies` as a flat union of all modules' external declared dependencies, deduplicated by `groupId + artifactId`. Transitive dependencies MUST NOT appear.
4. When the same coordinate appears in multiple modules at different versions, include the entry with the highest resolved version and discard duplicates.
5. Populate `Module.declaredDependencies` with only external (Maven/Gradle) coordinates specific to that module — not the project-wide union.
6. Populate `Module.moduleDependencies` with sibling module names (strings), not with external coordinates.
7. Set `StyleSource.path` to a project-relative path string. Inline build-script style config MUST NOT be represented in Sprint 1 (no path = not a `StyleSource`).
8. Set `StyleSource` entries only for file-based configuration files that actually exist in the project.
8a. Populate `StructureInfo.rootPackages` as the union of root packages across ALL modules (project-wide). Per-module root package data is not in scope for MVP; do not attempt to populate it.
9. Set `LinterInfo.activeRules` to the empty list when no linter is wired into the build — never fabricate rules.
10. Set `TestInfo.coverageThreshold` to non-null only when JaCoCo is configured with an explicit threshold value.

---

## Consumer contract (prompt, ui, and future consumers)

A consumer MUST:

1. Treat any section's empty state as valid and meaningful — "not detected" is not an error.
2. Sort `CodeStyleInfo.sources` by `StyleSourceType.priority` (ascending) to determine precedence. Do NOT re-implement priority logic independently.
3. Never mutate model instances. Data classes are treated as immutable; produce a modified copy via `.copy()` if transformation is needed.
4. When multiple `StyleSource` entries share the same priority rank (e.g., Checkstyle + Spotless both at rank 1), the model provides no ordering between them. The consumer is responsible for conflict resolution among same-rank sources. The model guarantees only the linter-configs-over-EditorConfig-over-IdeCodeStyle ordering.
4. Not depend on list ordering within sections unless the ordering is explicitly documented here. Currently, no ordering guarantee is given for any list field.

---

## Dependency rules (from constitution)

- `model` has **no outgoing dependencies** on any other component (`scan`, `baseline`, `prompt`, `ui`).
- `model` has **no IntelliJ Platform dependency** on its compile or runtime classpath.
- `scan`, `prompt`, and `ui` each depend on `model`. They MUST NOT depend on each other.

---

## Stability guarantees

| Guarantee | Scope |
|-----------|-------|
| Field names and semantics are frozen | All 15 types listed above |
| New fields may be added | Any data class (additive) |
| New enum constants may be added | Any enum (additive) |
| Types will not be removed | Requires major constitution amendment |
| Package will not change | `dev.zahaand.projectscan.model` is stable |
