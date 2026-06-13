package dev.zahaand.projectscan.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LinterInfoTest {
    @Test
    fun `empty-state LinterInfo has empty activeRules`() {
        val info = LinterInfo()
        assertTrue(info.activeRules.isEmpty())
    }

    @Test
    fun `ActiveRule with ERROR severity and breaksBuild true round-trips all fields`() {
        val rule =
            ActiveRule(
                ruleId = "LineLength",
                tool = "Checkstyle",
                severity = RuleSeverity.ERROR,
                breaksBuild = true,
            )
        assertEquals("LineLength", rule.ruleId)
        assertEquals("Checkstyle", rule.tool)
        assertEquals(RuleSeverity.ERROR, rule.severity)
        assertEquals(true, rule.breaksBuild)
    }

    @Test
    fun `ActiveRule with WARNING severity and breaksBuild false round-trips correctly`() {
        val rule =
            ActiveRule(
                ruleId = "UnusedImports",
                tool = "PMD",
                severity = RuleSeverity.WARNING,
                breaksBuild = false,
            )
        assertEquals("UnusedImports", rule.ruleId)
        assertEquals("PMD", rule.tool)
        assertEquals(RuleSeverity.WARNING, rule.severity)
        assertEquals(false, rule.breaksBuild)
    }

    @Test
    fun `ActiveRule with null breaksBuild represents Gradle not-detected case`() {
        val rule =
            ActiveRule(
                ruleId = "checkstyleMain",
                tool = "Checkstyle",
                severity = RuleSeverity.WARNING,
                breaksBuild = null,
            )
        assertNull(rule.breaksBuild)
    }
}
