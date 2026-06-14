# Quickstart: Using `BaselineRuleProvider`

**Audience**: Developers integrating the baseline rule set into `:prompt` (Sprint 4+) or writing
baseline unit tests.

---

## Dependency

`:baseline` depends only on `kotlinx-serialization-json` and `:model` (architectural anchor;
not consumed in Sprint 3). Consumers depend on `:baseline` directly — they do NOT need `:scan`.

```kotlin
// In the consumer's build.gradle.kts (e.g., :prompt in Sprint 4):
implementation(project(":model"))
implementation(project(":baseline"))
```

---

## Getting the Rule Set

`BaselineRuleProvider` is a Kotlin `object` (singleton). Access `.rules` to get the complete,
validated, immutable list. First call loads and caches; all subsequent calls return the same list.

```kotlin
import dev.zahaand.projectscan.baseline.BaselineRuleProvider
import dev.zahaand.projectscan.baseline.BaselineLoadException

// Happy path:
val rules = BaselineRuleProvider.rules   // List<BaselineRule>, ≥13 entries

// With error handling at the call site (recommended in :prompt):
val rules = try {
    BaselineRuleProvider.rules
} catch (e: BaselineLoadException) {
    logger.error("Baseline rule set unavailable: ${e.message}", e)
    emptyList()
}
```

No instantiation, no configuration, no project context required.

---

## Accessing Rule Fields

```kotlin
val rules = BaselineRuleProvider.rules

// All filtering belongs to :prompt — :baseline returns the full, unranked set.

// Example: group by level
val byLevel = rules.groupBy { it.level }
// byLevel[BaselineLevel.CORRECTNESS] → rules with level CORRECTNESS
// byLevel[BaselineLevel.BEST_PRACTICE] → rules with level BEST_PRACTICE

// Example: group by category
val byCategory = rules.groupBy { it.category }
// byCategory[BaselineCategory.NULL_SAFETY] → ≥2 rules

// Example: check obligation
val mandatory = rules.filter { it.obligation == Obligation.MUST }

// Example: rules applicable to a given Java level (consumer filters, not baseline)
val applicableRules = rules.filter { it.minJavaLevel <= projectJavaLevel }
```

`:baseline` returns the FULL set — unfiltered, unranked. Filtering by language level or any
project-specific fact belongs exclusively to `:prompt` (FR-012).

---

## Unit Testing Error Paths

`BaselineRuleProvider.loadFromReader` is `internal`, so tests inside the `:baseline` module call
it directly with a `StringReader` to inject controlled JSON — no classloader manipulation needed.

```kotlin
import dev.zahaand.projectscan.baseline.BaselineRuleProvider
import dev.zahaand.projectscan.baseline.BaselineLoadException
import org.junit.jupiter.api.assertThrows
import java.io.StringReader

class BaselineRuleProviderTest {

    @Test
    fun `real bundled rules load successfully`() {
        val rules = BaselineRuleProvider.rules
        assertTrue(rules.isNotEmpty())
    }

    @Test
    fun `empty rules array throws BaselineLoadException`() {
        val json = """{"schemaVersion":1,"rules":[]}"""
        assertThrows<BaselineLoadException> {
            BaselineRuleProvider.loadFromReader(StringReader(json))
        }
    }

    @Test
    fun `duplicate id throws BaselineLoadException`() {
        val json = """
            {
              "schemaVersion": 1,
              "rules": [
                {"id":"x","level":"CORRECTNESS","category":"NULL_SAFETY","obligation":"MUST",
                 "statement":"s","rationale":"r","minJavaLevel":8,"languages":["JAVA"]},
                {"id":"x","level":"CORRECTNESS","category":"NULL_SAFETY","obligation":"MUST",
                 "statement":"s2","rationale":"r2","minJavaLevel":8,"languages":["JAVA"]}
              ]
            }
        """.trimIndent()
        assertThrows<BaselineLoadException> {
            BaselineRuleProvider.loadFromReader(StringReader(json))
        }
    }

    @Test
    fun `malformed JSON throws BaselineLoadException`() {
        assertThrows<BaselineLoadException> {
            BaselineRuleProvider.loadFromReader(StringReader("{not json"))
        }
    }

    @Test
    fun `unsupported schemaVersion throws BaselineLoadException`() {
        val json = """{"schemaVersion":99,"rules":[]}"""
        assertThrows<BaselineLoadException> {
            BaselineRuleProvider.loadFromReader(StringReader(json))
        }
    }

    @Test
    fun `category level mismatch throws BaselineLoadException`() {
        val json = """
            {
              "schemaVersion": 1,
              "rules": [
                {"id":"x","level":"CORRECTNESS","category":"EXCEPTION_HANDLING","obligation":"MUST",
                 "statement":"s","rationale":"r","minJavaLevel":8,"languages":["JAVA"]}
              ]
            }
        """.trimIndent()
        assertThrows<BaselineLoadException> {
            BaselineRuleProvider.loadFromReader(StringReader(json))
        }
    }
}
```

No `LightPlatformTestCase`, no IntelliJ fixtures — pure JUnit 5, runs in any JVM. This mirrors
the `:model` module's test approach (SC-006).

---

## Unit Testing Coverage (US4)

```kotlin
class BaselineRuleCoverageTest {

    private val rules = BaselineRuleProvider.rules
    private val byCategory = rules.groupBy { it.category }

    @Test fun `NULL_SAFETY has at least 2 rules`() {
        assertTrue((byCategory[BaselineCategory.NULL_SAFETY]?.size ?: 0) >= 2)
    }

    @Test fun `RESOURCE_MANAGEMENT has at least 2 rules`() {
        assertTrue((byCategory[BaselineCategory.RESOURCE_MANAGEMENT]?.size ?: 0) >= 2)
    }

    @Test fun `CONCURRENCY has at least 2 rules`() {
        assertTrue((byCategory[BaselineCategory.CONCURRENCY]?.size ?: 0) >= 2)
    }

    @Test fun `DANGEROUS_CONSTRUCTS has at least 2 rules`() {
        assertTrue((byCategory[BaselineCategory.DANGEROUS_CONSTRUCTS]?.size ?: 0) >= 2)
    }

    @Test fun `EXCEPTION_HANDLING has at least 1 rule`() {
        assertTrue((byCategory[BaselineCategory.EXCEPTION_HANDLING]?.size ?: 0) >= 1)
    }

    @Test fun `STRING_PERFORMANCE has at least 1 rule`() {
        assertTrue((byCategory[BaselineCategory.STRING_PERFORMANCE]?.size ?: 0) >= 1)
    }

    @Test fun `DECOMPOSITION has at least 1 rule`() {
        assertTrue((byCategory[BaselineCategory.DECOMPOSITION]?.size ?: 0) >= 1)
    }

    @Test fun `IMMUTABILITY has at least 1 rule`() {
        assertTrue((byCategory[BaselineCategory.IMMUTABILITY]?.size ?: 0) >= 1)
    }

    @Test fun `INTERFACE_PROGRAMMING has at least 1 rule`() {
        assertTrue((byCategory[BaselineCategory.INTERFACE_PROGRAMMING]?.size ?: 0) >= 1)
    }

    @Test fun `total rule count is at least 13`() {
        assertTrue(rules.size >= 13)
    }

    @Test fun `at least one rule has minJavaLevel greater than 8`() {
        assertTrue(rules.any { it.minJavaLevel > 8 })
    }
}
```
