# Research: Baseline — Static Curated Code-Quality Rules Module

**Phase**: 0 | **Date**: 2026-06-14 | **Plan**: [plan.md](plan.md)

No blocking NEEDS CLARIFICATION items from Technical Context — all resolved via spec,
constitution, and codebase inspection. This document records setup decisions and design choices
that inform data model and implementation.

---

## R-001: kotlinx.serialization Setup for a Pure Kotlin Submodule

**Decision**: Add `org.jetbrains.kotlin.plugin.serialization` to `pluginManagement.plugins` in
`settings.gradle.kts` (same version as the Kotlin JVM plugin: `2.2.20`) and apply it in
`baseline/build.gradle.kts`; add `kotlinx-serialization-json:1.7.3` as an implementation
dependency.

**Setup**:

```kotlin
// settings.gradle.kts — add to pluginManagement.plugins block:
id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"

// baseline/build.gradle.kts:
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

detekt {
    config.setFrom(files("config/detekt.yml"))
    buildUponDefaultConfig = true
}
```

**Version alignment**: `kotlinx-serialization-json 1.7.x` is the stable release line for
Kotlin 2.x. Version `1.7.3` is compatible with Kotlin `2.2.20`. No snapshot or EAP dependencies.

The `kotlinx-serialization-json` artifact is a plain Maven dependency (not bundled with the
IntelliJ Platform), so it requires no special handling in this pure-JVM module.

**Alternatives considered**:
- Jackson / Gson — rejected; spec FR-003 mandates `kotlinx.serialization` exclusively.
- Kotlinx-serialization via the IntelliJ Platform bundle — rejected; `:baseline` has no
  IntelliJ Platform dependency, so the platform's bundled copy is not on this module's classpath.

---

## R-002: Classloader Resource Loading for Bundled JSON

**Decision**: Load via `BaselineRuleProvider::class.java.getResourceAsStream("/dev/zahaand/projectscan/baseline/rules.json")` (absolute classloader path, leading `/`).

**Key details**:
- Gradle places files under `src/main/resources/` at the root of the compiled output classpath.
  A file at `src/main/resources/dev/zahaand/projectscan/baseline/rules.json` is addressable as
  `/dev/zahaand/projectscan/baseline/rules.json` with the leading `/` (absolute form) via
  `Class.getResourceAsStream`.
- The absolute form (`/`-prefixed) is independent of the calling class's package and is
  unambiguous — preferred over the class-relative form.
- A `null` return means the resource was not found on the classpath — throw `BaselineLoadException`.

**Test resource injection**: Negative-path tests (malformed JSON, empty array, duplicate IDs)
must substitute controlled JSON without touching the classloader resource. Strategy:

`BaselineRuleProvider` exposes an `internal` `loadFromReader(reader: Reader): List<BaselineRule>`
function that contains the full parse-and-validate pipeline. The public `by lazy` property calls
`loadFromReader` with the real classloader-sourced reader. Tests call `loadFromReader(StringReader(badJson))` directly.

This avoids test-classloader manipulation and keeps tests dependency-free.

**Alternatives considered**:
- Classloader override in tests — complex, fragile; requires `URLClassLoader` setup.
- Filesystem resource override — creates platform path dependency; rejected.

---

## R-003: Lazy Initialization Pattern

**Decision**: `object BaselineRuleProvider` with `val rules: List<BaselineRule> by lazy { loadRules() }`.

**Rationale**: Kotlin `lazy` uses `SynchronizedLazyImpl` by default (mode `SYNCHRONIZED`),
which provides thread-safe double-checked locking at no manual cost. The first call to `rules`
triggers `loadRules()`; all subsequent calls return the cached immutable list.

Using `object` (singleton) means no accidental second provider instance. The JVM guarantees the
object is initialized at most once (class-loading semantics).

**Exception behavior**: If `loadRules()` throws, `SynchronizedLazyImpl` does NOT cache the
exception — the next call to `rules` will re-attempt loading. This is the correct behavior:
a deployment with a corrupt resource gets `BaselineLoadException` on every access, making the
failure impossible to miss.

**Alternatives considered**:
- Plain `class` + `companion object` factory — more verbose with no behavioral difference for
  this use case (single provider, no dependency injection required in Sprint 3).
- `@JvmStatic lateinit var` — not thread-safe without explicit synchronization; rejected.

---

## R-004: Lenient Deserialization with kotlinx.serialization

**Decision**: Use `Json { ignoreUnknownKeys = true }` to silently discard extra JSON fields.
Enum values are matched by name (case-sensitive) — the default kotlinx.serialization behavior.

**Configuration**:
```kotlin
private val jsonParser = Json { ignoreUnknownKeys = true }
```

**Enum behavior**: kotlinx.serialization matches JSON string values to Kotlin enum entry names
exactly (e.g., `"CORRECTNESS"` → `BaselineLevel.CORRECTNESS`). `rules.json` MUST use the exact
Kotlin enum name. `@SerialName` annotations on enum entries are not needed since JSON names and
Kotlin names are identical.

**Structural errors**: `Json.decodeFromString` throws `SerializationException` on malformed
JSON or unrecognized enum values (since enum matching is strict by default). This is caught by
`loadFromReader` and wrapped in `BaselineLoadException`.

**Alternatives considered**:
- `Json { isLenient = true }` — relaxes string quoting rules; not needed for well-formed JSON
  written by the team. `ignoreUnknownKeys` is sufficient.

---

## R-005: schemaVersion Validation Strategy

**Decision**: Parse the outer wrapper `{ "schemaVersion": 1, "rules": [ ... ] }` into an
internal `@Serializable data class RuleSet(val schemaVersion: Int, val rules: List<BaselineRule>)`.
Validate `schemaVersion == 1` immediately after parsing, before accessing rules.

**Failure modes handled**:
- `schemaVersion` absent in JSON → `SerializationException` (non-nullable field) → wrapped in
  `BaselineLoadException`.
- `schemaVersion` present but unrecognized value → explicit check throws `BaselineLoadException`.
- `rules` array absent → `SerializationException` → wrapped in `BaselineLoadException`.

**Future-proofing**: When `schemaVersion` changes to `2`, the loader can branch on version and
apply a migration transform before invariant validation.

`RuleSet` is `internal` — not part of the public `:baseline` API.
