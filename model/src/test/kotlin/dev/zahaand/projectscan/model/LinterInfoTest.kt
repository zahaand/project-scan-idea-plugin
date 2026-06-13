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

    @Test
    fun `LinterInfo with toolsWithUnresolvableConfig records applied tool names`() {
        val info = LinterInfo(
            activeRules = emptyList(),
            toolsWithUnresolvableConfig = listOf("checkstyle", "pmd"),
        )
        assertEquals(listOf("checkstyle", "pmd"), info.toolsWithUnresolvableConfig)
        assertTrue(info.activeRules.isEmpty())
    }

    @Test
    fun `LinterInfo default has empty toolsWithUnresolvableConfig`() {
        val info = LinterInfo(activeRules = emptyList())
        assertTrue(info.toolsWithUnresolvableConfig.isEmpty())
    }

    @Test
    fun `LinterInfo can carry both activeRules and toolsWithUnresolvableConfig`() {
        val rule = ActiveRule("LineLength", "checkstyle", RuleSeverity.ERROR, true)
        val info = LinterInfo(
            activeRules = listOf(rule),
            toolsWithUnresolvableConfig = listOf("pmd"),
        )
        assertEquals(1, info.activeRules.size)
        assertEquals(listOf("pmd"), info.toolsWithUnresolvableConfig)
    }
}
