package dev.zahaand.projectscan.scan.adapter

import dev.zahaand.projectscan.model.RuleSeverity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PmdConfigParserTest {

    private val parser = PmdConfigParser()

    @Test
    fun `priorities 1 through 5 map to correct severities`() {
        val xml = """
            <ruleset name="Custom Rules">
              <rule ref="category/java/a.xml/Rule1"><priority>1</priority></rule>
              <rule ref="category/java/a.xml/Rule2"><priority>2</priority></rule>
              <rule ref="category/java/a.xml/Rule3"><priority>3</priority></rule>
              <rule ref="category/java/a.xml/Rule4"><priority>4</priority></rule>
              <rule ref="category/java/a.xml/Rule5"><priority>5</priority></rule>
            </ruleset>
        """.trimIndent()

        val rules = parser.parseRulesFromXml(xml)

        assertEquals(5, rules.size)
        assertEquals(RuleSeverity.ERROR, rules.first { it.ruleId == "category/java/a.xml/Rule1" }.severity)
        assertEquals(RuleSeverity.ERROR, rules.first { it.ruleId == "category/java/a.xml/Rule2" }.severity)
        assertEquals(RuleSeverity.WARNING, rules.first { it.ruleId == "category/java/a.xml/Rule3" }.severity)
        assertEquals(RuleSeverity.INFO, rules.first { it.ruleId == "category/java/a.xml/Rule4" }.severity)
        assertEquals(RuleSeverity.INFO, rules.first { it.ruleId == "category/java/a.xml/Rule5" }.severity)
    }

    @Test
    fun `absent priority defaults to INFO`() {
        val xml = """
            <ruleset name="Custom Rules">
              <rule ref="category/java/codestyle.xml/LongVariable"/>
            </ruleset>
        """.trimIndent()

        val rules = parser.parseRulesFromXml(xml)

        assertEquals(1, rules.size)
        assertEquals(RuleSeverity.INFO, rules.first().severity)
    }

    @Test
    fun `ruleId taken from ref attribute`() {
        val xml = """
            <ruleset name="Custom Rules">
              <rule ref="category/java/bestpractices.xml/AbstractClassWithoutAbstractMethod">
                <priority>2</priority>
              </rule>
            </ruleset>
        """.trimIndent()

        val rules = parser.parseRulesFromXml(xml)

        assertEquals(1, rules.size)
        assertEquals("category/java/bestpractices.xml/AbstractClassWithoutAbstractMethod", rules.first().ruleId)
    }

    @Test
    fun `ruleset referencing external ruleset by ref — only local rules returned, reference not followed`() {
        val xml = """
            <ruleset name="Custom Rules">
              <rule ref="category/java/bestpractices.xml"/>
              <rule ref="category/java/codestyle.xml/LongVariable">
                <priority>3</priority>
              </rule>
            </ruleset>
        """.trimIndent()

        val rules = parser.parseRulesFromXml(xml)

        // Both entries have a ref attribute and are direct children of the root.
        // The whole-category ref is recorded as a rule entry (with its ref value) but NOT followed.
        // Only rules defined in this XML are returned; no rules from external category files appear.
        assertTrue(rules.any { it.ruleId == "category/java/codestyle.xml/LongVariable" })
        assertTrue(rules.any { it.ruleId == "category/java/bestpractices.xml" })
        // Ensure no deep rules from the external category were added
        assertTrue(rules.none { it.ruleId.contains("AbstractClass") })
    }

    @Test
    fun `rule without ref attribute is skipped`() {
        val xml = """
            <ruleset name="Custom Rules">
              <rule name="SomeRule">
                <priority>1</priority>
              </rule>
              <rule ref="category/java/a.xml/ValidRule">
                <priority>2</priority>
              </rule>
            </ruleset>
        """.trimIndent()

        val rules = parser.parseRulesFromXml(xml)

        assertEquals(1, rules.size)
        assertEquals("category/java/a.xml/ValidRule", rules.first().ruleId)
    }

    @Test
    fun `multiple rules with mixed priorities`() {
        val xml = """
            <ruleset name="Mixed">
              <rule ref="a/Rule1"><priority>1</priority></rule>
              <rule ref="b/Rule2"><priority>3</priority></rule>
              <rule ref="c/Rule3"/>
            </ruleset>
        """.trimIndent()

        val rules = parser.parseRulesFromXml(xml)

        assertEquals(3, rules.size)
        assertEquals(RuleSeverity.ERROR, rules.first { it.ruleId == "a/Rule1" }.severity)
        assertEquals(RuleSeverity.WARNING, rules.first { it.ruleId == "b/Rule2" }.severity)
        assertEquals(RuleSeverity.INFO, rules.first { it.ruleId == "c/Rule3" }.severity)
    }
}
