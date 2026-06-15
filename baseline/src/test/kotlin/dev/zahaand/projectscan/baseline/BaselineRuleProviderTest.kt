package dev.zahaand.projectscan.baseline

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.StringReader

class BaselineRuleProviderTest {
    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun minimalValidJson(rules: String = minimalRule()): String = """{"schemaVersion":1,"rules":[$rules]}"""

    @Suppress("LongParameterList")
    private fun minimalRule(
        id: String = "correctness.test-rule",
        level: String = "CORRECTNESS",
        category: String = "NULL_SAFETY",
        obligation: String = "MUST",
        statement: String = "Non-blank statement.",
        rationale: String = "Non-blank rationale.",
        minJavaLevel: Int = 8,
        languages: String = """["JAVA"]""",
    ): String =
        """{
          "id":"$id",
          "level":"$level",
          "category":"$category",
          "obligation":"$obligation",
          "statement":"$statement",
          "rationale":"$rationale",
          "minJavaLevel":$minJavaLevel,
          "languages":$languages
        }"""

    private fun load(json: String): List<BaselineRule> = BaselineRuleProvider.loadFromReader(StringReader(json))

    // -------------------------------------------------------------------------
    // US1 — S1: happy-path from the real bundled resource
    // -------------------------------------------------------------------------

    @Test
    fun `US1-S1 rules returns a non-empty list with no exception`() {
        val rules = BaselineRuleProvider.rules
        assertNotNull(rules)
        assertTrue(rules.isNotEmpty(), "Bundled rules must not be empty")
    }

    // -------------------------------------------------------------------------
    // US1 — S2–S10: negative paths via loadFromReader seam
    // -------------------------------------------------------------------------

    @Test
    fun `US1-S2 blank statement throws BaselineLoadException`() {
        val json = minimalValidJson(minimalRule(statement = "   "))
        val ex = assertThrows<BaselineLoadException> { load(json) }
        assertTrue(ex.message!!.contains("statement"), "Message should identify 'statement': ${ex.message}")
    }

    @Test
    fun `US1-S2b blank rationale throws BaselineLoadException`() {
        val json = minimalValidJson(minimalRule(rationale = "   "))
        val ex = assertThrows<BaselineLoadException> { load(json) }
        assertTrue(ex.message!!.contains("rationale"), "Message should identify 'rationale': ${ex.message}")
    }

    @Test
    fun `US1-S3 blank id throws BaselineLoadException mentioning blank id`() {
        val json = minimalValidJson(minimalRule(id = "  "))
        val ex = assertThrows<BaselineLoadException> { load(json) }
        assertTrue(
            ex.message!!.lowercase().contains("blank id"),
            "Message must say 'blank id' (not 'duplicate'): ${ex.message}",
        )
    }

    @Test
    fun `US1-S4 duplicate id throws BaselineLoadException`() {
        val rule = minimalRule(id = "correctness.dup")
        val json = minimalValidJson("$rule,$rule")
        val ex = assertThrows<BaselineLoadException> { load(json) }
        assertTrue(
            ex.message!!.lowercase().contains("duplicate"),
            "Message must mention 'duplicate': ${ex.message}",
        )
        assertTrue(
            ex.message!!.contains("correctness.dup"),
            "Message must identify the duplicate id: ${ex.message}",
        )
    }

    @Test
    fun `US1-S5 malformed JSON throws BaselineLoadException with non-null cause`() {
        val ex = assertThrows<BaselineLoadException> { load("{not valid json") }
        assertNotNull(ex.cause, "Cause must be non-null for structural parse failures")
    }

    @Test
    fun `US1-S6 invalid minJavaLevel throws BaselineLoadException`() {
        val json = minimalValidJson(minimalRule(minJavaLevel = 7))
        val ex = assertThrows<BaselineLoadException> { load(json) }
        assertTrue(
            ex.message!!.contains("minJavaLevel") || ex.message!!.contains("7"),
            "Message must reference the invalid level: ${ex.message}",
        )
    }

    @Test
    fun `US1-S7 empty rules array throws BaselineLoadException`() {
        val json = """{"schemaVersion":1,"rules":[]}"""
        assertThrows<BaselineLoadException> { load(json) }
    }

    @Test
    fun `US1-S8 category-level mismatch throws BaselineLoadException`() {
        // EXCEPTION_HANDLING belongs to BEST_PRACTICE, not CORRECTNESS
        val json =
            minimalValidJson(
                minimalRule(level = "CORRECTNESS", category = "EXCEPTION_HANDLING"),
            )
        val ex = assertThrows<BaselineLoadException> { load(json) }
        assertTrue(
            ex.message!!.contains("EXCEPTION_HANDLING"),
            "Message must identify the offending category: ${ex.message}",
        )
    }

    @Test
    fun `US1-S9 empty languages list throws BaselineLoadException`() {
        val json = minimalValidJson(minimalRule(languages = "[]"))
        val ex = assertThrows<BaselineLoadException> { load(json) }
        assertTrue(
            ex.message!!.lowercase().contains("language"),
            "Message must mention 'language': ${ex.message}",
        )
    }

    @Test
    fun `US1-S10 unsupported schemaVersion throws BaselineLoadException stating actual value`() {
        val json = """{"schemaVersion":2,"rules":[${minimalRule()}]}"""
        val ex = assertThrows<BaselineLoadException> { load(json) }
        assertTrue(
            ex.message!!.contains("2"),
            "Message must state the actual schemaVersion found: ${ex.message}",
        )
    }

    // -------------------------------------------------------------------------
    // US2 — consume bundled rules
    // -------------------------------------------------------------------------

    @Test
    fun `US2-S1 total count is at least 13 and matches bundled file exactly`() {
        val rules = BaselineRuleProvider.rules
        assertTrue(rules.size >= 13, "Expected at least 13 rules, got ${rules.size}")
        // Reload from reader to get the independent count; both must agree.
        val reloaded =
            BaselineRuleProvider.javaClass
                .getResourceAsStream("/dev/zahaand/projectscan/baseline/rules.json")!!
                .bufferedReader()
                .use { BaselineRuleProvider.loadFromReader(it) }
        assertTrue(
            rules.size == reloaded.size,
            "Provider must return exactly what rules.json contains — got ${rules.size} vs ${reloaded.size}",
        )
    }

    @Test
    fun `US2-S2 rules include minJavaLevel 8 and at least one level greater than 8`() {
        val rules = BaselineRuleProvider.rules
        assertTrue(
            rules.any { it.minJavaLevel == 8 },
            "Expected at least one rule with minJavaLevel == 8",
        )
        assertTrue(
            rules.any { it.minJavaLevel > 8 },
            "Expected at least one rule with minJavaLevel > 8",
        )
    }

    @Test
    fun `US2-S3 two successive accesses to rules return the same instance`() {
        val first = BaselineRuleProvider.rules
        val second = BaselineRuleProvider.rules
        assertSame(first, second, "rules must be cached — same instance on every access (FR-001)")
        assertFalse(first.isEmpty())
    }
}
