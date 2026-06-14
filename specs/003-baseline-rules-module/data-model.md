# Data Model: Baseline — Static Curated Code-Quality Rules Module

**Phase**: 1 | **Date**: 2026-06-14 | **Plan**: [plan.md](plan.md)

---

## 1. Types Defined in `:baseline`

All types below are in `package dev.zahaand.projectscan.baseline`.
None of these types go into `:model` (FR-015).

### 1.1 Enumerations

```kotlin
@Serializable
enum class BaselineLevel {
    CORRECTNESS,      // SpotBugs-type violations, Level 1
    BEST_PRACTICE,    // Effective Java / PMD-type practices, Level 2
}

@Serializable
enum class BaselineCategory {
    // CORRECTNESS-level categories — must pair with BaselineLevel.CORRECTNESS
    NULL_SAFETY,
    RESOURCE_MANAGEMENT,
    CONCURRENCY,
    DANGEROUS_CONSTRUCTS,

    // BEST_PRACTICE-level categories — must pair with BaselineLevel.BEST_PRACTICE
    EXCEPTION_HANDLING,
    STRING_PERFORMANCE,
    DECOMPOSITION,
    IMMUTABILITY,
    INTERFACE_PROGRAMMING,
}

@Serializable
enum class Obligation {
    MUST,     // mandatory — violation is a defect
    SHOULD,   // strong recommendation — deviation requires justification
}

@Serializable
enum class BaselineLanguage {
    JAVA,     // only MVP value; list-per-rule structure is forward-compatible
}
```

### 1.2 `BaselineRule` Data Class

```kotlin
@Serializable
data class BaselineRule(
    val id: String,
    val level: BaselineLevel,
    val category: BaselineCategory,
    val obligation: Obligation,
    val statement: String,
    val rationale: String,
    val minJavaLevel: Int,
    val languages: List<BaselineLanguage>,
)
```

**Field constraints** (enforced by invariant validation inside `BaselineRuleProvider`):

| Field | Constraint |
|-------|-----------|
| `id` | Non-blank (empty string or whitespace-only fails); unique across all rules (case-sensitive: `"a.b"` ≠ `"A.B"`); naming convention is a curation guideline only, not validated beyond non-blank + uniqueness |
| `level` | One of `CORRECTNESS`, `BEST_PRACTICE` |
| `category` | One of 9 `BaselineCategory` values; MUST be consistent with `level` per FR-008 mapping (see table below) |
| `obligation` | One of `MUST`, `SHOULD` |
| `statement` | Non-blank (empty string or whitespace-only fails) |
| `rationale` | Non-blank (empty string or whitespace-only fails) |
| `minJavaLevel` | One of `{8, 11, 17, 21}` |
| `languages` | Non-empty list; all entries recognized `BaselineLanguage` values |

**Category/Level consistency mapping** (enforced as an invariant at load time):

| Category | Required `level` |
|----------|-----------------|
| `NULL_SAFETY` | `CORRECTNESS` |
| `RESOURCE_MANAGEMENT` | `CORRECTNESS` |
| `CONCURRENCY` | `CORRECTNESS` |
| `DANGEROUS_CONSTRUCTS` | `CORRECTNESS` |
| `EXCEPTION_HANDLING` | `BEST_PRACTICE` |
| `STRING_PERFORMANCE` | `BEST_PRACTICE` |
| `DECOMPOSITION` | `BEST_PRACTICE` |
| `IMMUTABILITY` | `BEST_PRACTICE` |
| `INTERFACE_PROGRAMMING` | `BEST_PRACTICE` |

A rule where `category == EXCEPTION_HANDLING` but `level == CORRECTNESS` is an invariant
violation that throws `BaselineLoadException`.

### 1.3 `BaselineLoadException`

```kotlin
class BaselineLoadException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
```

Unchecked exception; the single failure signal for all load and validation errors. Consumers
(`:prompt`, Sprint 4) catch this specifically to distinguish baseline failures from other errors.
Every `BaselineLoadException` message MUST identify the specific violation (which rule, which
field, which constraint) to enable diagnosis from logs without a debugger.

---

## 2. Internal Types (not part of the public `:baseline` API)

### 2.1 `RuleSet` — JSON Wrapper

```kotlin
@Serializable
internal data class RuleSet(
    val schemaVersion: Int,
    val rules: List<BaselineRule>,
)
```

Used only during JSON deserialization inside `BaselineRuleProvider`. Never exposed outside
`:baseline`. Enables `schemaVersion` validation before invariant checking.

---

## 3. `rules.json` Format and Coverage Requirements

### 3.1 Schema

```json
{
  "schemaVersion": 1,
  "rules": [
    {
      "id": "<lowercase-dotted string>",
      "level": "CORRECTNESS | BEST_PRACTICE",
      "category": "<one of 9 BaselineCategory names>",
      "obligation": "MUST | SHOULD",
      "statement": "<non-blank prose>",
      "rationale": "<non-blank prose>",
      "minJavaLevel": 8,
      "languages": ["JAVA"]
    }
  ]
}
```

**ID convention**: lowercase, dot-separated, `<level-prefix>.<slug>`.
Examples: `"correctness.null-check-before-deref"`, `"best-practice.try-with-resources"`.
The `id` is a stable identifier for deduplication and future override support; it does NOT
determine `level` or `category` — both fields are explicit and stored.

**Classpath location**: `src/main/resources/dev/zahaand/projectscan/baseline/rules.json`
(loaded at runtime as `/dev/zahaand/projectscan/baseline/rules.json` via classloader).

### 3.2 Coverage Requirements

| Category | Level | Minimum Rules |
|----------|-------|--------------|
| `NULL_SAFETY` | `CORRECTNESS` | ≥2 |
| `RESOURCE_MANAGEMENT` | `CORRECTNESS` | ≥2 |
| `CONCURRENCY` | `CORRECTNESS` | ≥2 |
| `DANGEROUS_CONSTRUCTS` | `CORRECTNESS` | ≥2 |
| `EXCEPTION_HANDLING` | `BEST_PRACTICE` | ≥1 |
| `STRING_PERFORMANCE` | `BEST_PRACTICE` | ≥1 |
| `DECOMPOSITION` | `BEST_PRACTICE` | ≥1 |
| `IMMUTABILITY` | `BEST_PRACTICE` | ≥1 |
| `INTERFACE_PROGRAMMING` | `BEST_PRACTICE` | ≥1 |
| **Total** | | **≥13** |

At least one rule MUST have `minJavaLevel > 8` (required by FR-011; verified by SC-007).

### 3.3 Illustrative Sample (first 3 of ≥13 rules; full set authored during implementation)

```json
{
  "schemaVersion": 1,
  "rules": [
    {
      "id": "correctness.null-check-before-deref",
      "level": "CORRECTNESS",
      "category": "NULL_SAFETY",
      "obligation": "MUST",
      "statement": "Never dereference a reference that may be null without a prior null guard or Optional unwrap.",
      "rationale": "Unconditional dereference of a potentially null reference is the most common source of NullPointerExceptions in Java codebases. An explicit null guard makes the intent visible and eliminates an entire class of runtime failures.",
      "minJavaLevel": 8,
      "languages": ["JAVA"]
    },
    {
      "id": "correctness.optional-no-get-without-check",
      "level": "CORRECTNESS",
      "category": "NULL_SAFETY",
      "obligation": "MUST",
      "statement": "Never call Optional.get() without first checking isPresent() or using orElse()/orElseThrow().",
      "rationale": "Optional.get() on an empty Optional throws NoSuchElementException, which is semantically identical to a NullPointerException and defeats the purpose of Optional as a null-safe container.",
      "minJavaLevel": 8,
      "languages": ["JAVA"]
    },
    {
      "id": "correctness.try-with-resources",
      "level": "CORRECTNESS",
      "category": "RESOURCE_MANAGEMENT",
      "obligation": "MUST",
      "statement": "Always close AutoCloseable resources in a try-with-resources block rather than manually in a finally clause.",
      "rationale": "Manual finally-based close is error-prone: exceptions during close can suppress the original exception, and omissions in multi-resource scenarios are common. Try-with-resources guarantees correct close ordering and exception suppression semantics.",
      "minJavaLevel": 8,
      "languages": ["JAVA"]
    }
  ]
}
```

---

## 4. `BaselineRuleProvider` — Design Sketch

```kotlin
object BaselineRuleProvider {

    val rules: List<BaselineRule> by lazy { loadRules() }

    internal fun loadFromReader(reader: Reader): List<BaselineRule> {
        val text = reader.readText()
        val ruleSet = try {
            jsonParser.decodeFromString<RuleSet>(text)
        } catch (e: SerializationException) {
            throw BaselineLoadException("Failed to parse rules.json: ${e.message}", e)
        }
        if (ruleSet.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw BaselineLoadException("Unsupported schemaVersion: ${ruleSet.schemaVersion}")
        }
        if (ruleSet.rules.isEmpty()) {
            throw BaselineLoadException("rules array is empty — bundled rule set must not be empty")
        }
        validateRules(ruleSet.rules)
        return ruleSet.rules.toList()
    }

    private fun loadRules(): List<BaselineRule> {
        val stream = BaselineRuleProvider::class.java.getResourceAsStream(RESOURCE_PATH)
            ?: throw BaselineLoadException("rules.json not found at classpath path: $RESOURCE_PATH")
        return loadFromReader(stream.bufferedReader())
    }

    private fun validateRules(rules: List<BaselineRule>) {
        val seenIds = mutableSetOf<String>()
        for (rule in rules) {
            if (!seenIds.add(rule.id)) {
                throw BaselineLoadException("Duplicate rule id: '${rule.id}'")
            }
            if (rule.statement.isBlank()) {
                throw BaselineLoadException("Rule '${rule.id}' has a blank statement")
            }
            if (rule.rationale.isBlank()) {
                throw BaselineLoadException("Rule '${rule.id}' has a blank rationale")
            }
            if (rule.minJavaLevel !in ALLOWED_JAVA_LEVELS) {
                throw BaselineLoadException(
                    "Rule '${rule.id}' has invalid minJavaLevel ${rule.minJavaLevel}; " +
                        "allowed: $ALLOWED_JAVA_LEVELS"
                )
            }
            if (rule.languages.isEmpty()) {
                throw BaselineLoadException("Rule '${rule.id}' has an empty languages list")
            }
            validateCategoryLevel(rule)
        }
    }

    private fun validateCategoryLevel(rule: BaselineRule) {
        val expected = CATEGORY_LEVEL_MAP[rule.category]
            ?: throw BaselineLoadException("Unknown category '${rule.category}' in rule '${rule.id}'")
        if (rule.level != expected) {
            throw BaselineLoadException(
                "Rule '${rule.id}': category ${rule.category} requires level $expected " +
                    "but found ${rule.level}"
            )
        }
    }

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private val CATEGORY_LEVEL_MAP = mapOf(
        BaselineCategory.NULL_SAFETY          to BaselineLevel.CORRECTNESS,
        BaselineCategory.RESOURCE_MANAGEMENT  to BaselineLevel.CORRECTNESS,
        BaselineCategory.CONCURRENCY          to BaselineLevel.CORRECTNESS,
        BaselineCategory.DANGEROUS_CONSTRUCTS to BaselineLevel.CORRECTNESS,
        BaselineCategory.EXCEPTION_HANDLING   to BaselineLevel.BEST_PRACTICE,
        BaselineCategory.STRING_PERFORMANCE   to BaselineLevel.BEST_PRACTICE,
        BaselineCategory.DECOMPOSITION        to BaselineLevel.BEST_PRACTICE,
        BaselineCategory.IMMUTABILITY         to BaselineLevel.BEST_PRACTICE,
        BaselineCategory.INTERFACE_PROGRAMMING to BaselineLevel.BEST_PRACTICE,
    )

    private val ALLOWED_JAVA_LEVELS = setOf(8, 11, 17, 21)
    private const val RESOURCE_PATH = "/dev/zahaand/projectscan/baseline/rules.json"
    private const val SUPPORTED_SCHEMA_VERSION = 1
}
```

**`loadFromReader` is `internal`** so tests in the same Gradle module can call it with a
`StringReader` to exercise all error paths without touching the classloader resource.
This is the testability seam that makes all US1 negative acceptance scenarios (scenarios 2–9)
runnable as pure-JVM unit tests with no IntelliJ Platform fixtures, as mandated by SC-006.
Without this seam, negative-path tests would require classloader manipulation or real
filesystem resources. (Assumption: the spec does not explicitly mandate this seam by name;
it is the implementation of SC-006's pure-JVM constraint.)

**`by lazy` exception behavior**: If `loadRules()` throws, Kotlin's `SynchronizedLazyImpl` does
NOT cache the exception — the next access to `rules` re-attempts loading. This means a corrupt
deployment surfaces `BaselineLoadException` on every call, making the failure impossible to hide.
