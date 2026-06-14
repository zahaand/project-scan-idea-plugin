package dev.zahaand.projectscan.scan.adapter

import dev.zahaand.projectscan.model.RuleSeverity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CheckstyleConfigParserTest {
    private val parser = CheckstyleConfigParser()

    @Test
    fun `multiple rules with explicit severities map correctly`() {
        val xml =
            """
            <module name="Checker">
              <module name="TreeWalker">
                <module name="MethodLength">
                  <property name="severity" value="error"/>
                </module>
                <module name="MagicNumber">
                  <property name="severity" value="warning"/>
                </module>
                <module name="LineLength">
                  <property name="severity" value="info"/>
                </module>
              </module>
            </module>
            """.trimIndent()

        val rules = parser.parseRulesFromXml(xml)

        assertEquals(3, rules.size)
        assertEquals(RuleSeverity.ERROR, rules.first { it.ruleId == "MethodLength" }.severity)
        assertEquals(RuleSeverity.WARNING, rules.first { it.ruleId == "MagicNumber" }.severity)
        assertEquals(RuleSeverity.INFO, rules.first { it.ruleId == "LineLength" }.severity)
    }

    @Test
    fun `rule with no severity inherits from ancestor module`() {
        val xml =
            """
            <module name="Checker">
              <property name="severity" value="warning"/>
              <module name="TreeWalker">
                <module name="MagicNumber"/>
              </module>
            </module>
            """.trimIndent()

        val rules = parser.parseRulesFromXml(xml)

        assertEquals(1, rules.size)
        assertEquals(RuleSeverity.WARNING, rules.first().severity)
    }

    @Test
    fun `rule with no severity anywhere defaults to INFO`() {
        val xml =
            """
            <module name="Checker">
              <module name="TreeWalker">
                <module name="MagicNumber"/>
              </module>
            </module>
            """.trimIndent()

        val rules = parser.parseRulesFromXml(xml)

        assertEquals(1, rules.size)
        assertEquals(RuleSeverity.INFO, rules.first().severity)
    }

    @Test
    fun `Checker and TreeWalker containers are not emitted as rules`() {
        val xml =
            """
            <module name="Checker">
              <module name="TreeWalker">
                <module name="MethodLength">
                  <property name="severity" value="error"/>
                </module>
              </module>
            </module>
            """.trimIndent()

        val rules = parser.parseRulesFromXml(xml)

        assertTrue(rules.none { it.ruleId == "Checker" })
        assertTrue(rules.none { it.ruleId == "TreeWalker" })
        assertEquals(1, rules.size)
        assertEquals("MethodLength", rules.first().ruleId)
    }

    @Test
    fun `ignore severity maps to INFO`() {
        val xml =
            """
            <module name="Checker">
              <module name="TreeWalker">
                <module name="MagicNumber">
                  <property name="severity" value="ignore"/>
                </module>
              </module>
            </module>
            """.trimIndent()

        val rules = parser.parseRulesFromXml(xml)

        assertEquals(1, rules.size)
        assertEquals(RuleSeverity.INFO, rules.first().severity)
    }

    @Test
    fun `config with import or reference — only local rules returned, import not followed`() {
        val xml =
            """
            <module name="Checker">
              <module name="TreeWalker">
                <module name="MethodLength">
                  <property name="severity" value="error"/>
                </module>
              </module>
              <module name="SuppressionFilter">
                <property name="file" value="suppressions.xml"/>
              </module>
            </module>
            """.trimIndent()

        val rules = parser.parseRulesFromXml(xml)

        // SuppressionFilter is a module and will be recorded with ruleId="SuppressionFilter".
        // The key requirement is that the external file is NOT followed.
        // Only modules within this XML are returned.
        assertTrue(rules.none { it.ruleId == "Checker" })
        assertTrue(rules.none { it.ruleId == "TreeWalker" })
        // MethodLength must be present
        assertTrue(rules.any { it.ruleId == "MethodLength" })
        // No rule from any external file should appear (we only parse local XML)
        assertEquals(rules.size, rules.count { it.ruleId == "MethodLength" || it.ruleId == "SuppressionFilter" })
    }

    @Test
    fun `nested severity inheritance — closer ancestor wins`() {
        val xml =
            """
            <module name="Checker">
              <property name="severity" value="error"/>
              <module name="TreeWalker">
                <property name="severity" value="warning"/>
                <module name="MagicNumber"/>
              </module>
            </module>
            """.trimIndent()

        val rules = parser.parseRulesFromXml(xml)

        assertEquals(1, rules.size)
        // TreeWalker declares warning, which is closer ancestor than Checker's error
        assertEquals(RuleSeverity.WARNING, rules.first().severity)
    }
}
