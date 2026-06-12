# Research: Model — Structured Data Contract

**Feature**: 001-model-data-contract  
**Date**: 2026-06-12  
**Status**: Complete — no external research required

## Summary

All significant decisions for this sprint are resolved by the project constitution and the clarified spec. No external unknowns remain. This document records each decision and its rationale for traceability.

---

## Decision 1: Gradle submodule isolation for `model`

**Decision**: Add a `model/` submodule to the existing single-module Gradle project. The submodule uses only `org.jetbrains.kotlin.jvm` plugin — no `org.jetbrains.intellij.platform` application. The root plugin module will depend on `:model` via a project dependency.

**Rationale**: The constitution mandates no IntelliJ Platform dependency on the `model` component (SC-004 in spec). Separating into a submodule makes this enforceable at compile time — the platform SDK simply does not appear on the `model` classpath.

**Alternatives considered**:
- Single-module with source-set separation: rejected — does not enforce classpath isolation; platform SDK would still be resolvable.
- Independent Gradle project (composite build): rejected — overkill for a same-repo component; project dependency is sufficient.

---

## Decision 2: Enum with `priority: Int` for `StyleSourceType`

**Decision**: `StyleSourceType` is a Kotlin enum where each constant carries a `val priority: Int` constructor parameter. Lower integer = higher precedence. Checkstyle/Spotless/PMD → 1, EditorConfig → 2, IdeCodeStyle → 3.

**Rationale**: The spec requires consumers to be able to sort sources without re-implementing the priority rule. Embedding priority as an enum property is the simplest approach that keeps the rule in one place (the enum definition) and requires no separate lookup table.

**Alternatives considered**:
- Separate `priorityOf(type)` companion function: rejected — consumers could bypass it or forget to call it.
- Sealed class hierarchy with priority in each subclass: rejected — overengineered for five fixed values.

---

## Decision 3: Default values on all section constructors

**Decision**: All section data classes (`StackInfo`, `CodeStyleInfo`, `LinterInfo`, `TestInfo`, `StructureInfo`) have default values that represent the empty/not-detected state. `ProjectScanModel` itself does NOT have defaults — all five sections must be explicitly provided.

**Rationale**: Default values let producers construct a section incrementally or omit missing data without boilerplate. Requiring all five sections on the root aggregate forces producers to be explicit about completeness and prevents partial construction from silently escaping as a "full" model.

**Alternatives considered**:
- All fields nullable at root level: rejected — spec explicitly forbids nullable sections at the root; sections must always be present.
- No defaults anywhere: rejected — makes test construction verbose and goes against spec guidance (empty state expressed via empty collections, not null sections).

---

## Decision 4: `Dependency` reused in both `StackInfo` and `Module`

**Decision**: `Module.declaredDependencies` uses the same `Dependency` type as `StackInfo.dependencies`. No separate type is introduced.

**Rationale**: Both sites carry the same semantic: a Maven coordinate with an optional resolved version. Reusing the type keeps the model lean and avoids mapping between equivalent structures.

**Alternatives considered**:
- Separate `ModuleDependency` type: rejected — structurally identical to `Dependency`; adds noise without benefit.

---

## Decision 5: `TestFramework.version` is nullable

**Decision**: `TestFramework.version: String?` — nullable. Test framework entries may be derived from a dependency declaration where version is BOM-managed (same pattern as `Dependency.resolvedVersion`).

**Rationale**: Consistent with the existing nullable version contract on `Dependency`. Producers in Sprint 2 may not always be able to resolve a version for every framework.

---

## Decision 6: No `Comparable` or sorting utilities on model types

**Decision**: Model types do not implement `Comparable` or include utility functions (e.g., `sortedBySources()`). Priority is exposed as a raw `Int` property on `StyleSourceType`; sorting is left to consumers.

**Rationale**: Sprint 1 scope is strictly the data contract. Utilities belong to the consumer layer and can be added there without touching the model. Keeping the model as plain data classes also avoids logic that would need testing beyond structural correctness.

---

## Decision 7: Package layout within the `model` submodule

**Decision**: All model types live in a single package: `dev.zahaand.projectscan.model`. No sub-packages per section.

**Rationale**: Fourteen types across five sections is not large enough to warrant sub-packaging. A flat package keeps imports simple for consumers and avoids churn if types are later reorganised.

**Alternatives considered**:
- Sub-packages per section (e.g., `model.stack`, `model.codestyle`): rejected — excessive for MVP; can be added later if the type count grows significantly.
