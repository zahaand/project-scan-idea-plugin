package dev.zahaand.projectscan.scan.collector

import dev.zahaand.projectscan.model.LinterInfo
import dev.zahaand.projectscan.model.RuleSeverity
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.scan.fake.FakeLinterConfigParser
import dev.zahaand.projectscan.scan.fake.FakeLinterPort
import dev.zahaand.projectscan.scan.port.LinterConfigParser
import dev.zahaand.projectscan.scan.port.LinterToolDescriptor
import dev.zahaand.projectscan.scan.port.ParsedRule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LinterCollectorTest {

    @Test
    fun `10-rule Checkstyle config produces all rules with correct severities and breaksBuild denormalized`() {
        val configPath = "/project/checkstyle.xml"
        val severities = listOf(
            RuleSeverity.WARNING, RuleSeverity.INFO, RuleSeverity.ERROR,
            RuleSeverity.WARNING, RuleSeverity.INFO, RuleSeverity.ERROR,
            RuleSeverity.WARNING, RuleSeverity.INFO, RuleSeverity.ERROR,
            RuleSeverity.WARNING,
        )
        val rules = (1..10).map { i -> ParsedRule("Rule$i", severities[i - 1]) }
        val result = collector(
            tools = listOf(LinterToolDescriptor("checkstyle", configPath, true)),
            parsers = mapOf("checkstyle" to FakeLinterConfigParser(mapOf(configPath to rules))),
        ).collect()
        val ok = assertOk(result)
        assertEquals(10, ok.activeRules.size)
        ok.activeRules.forEachIndexed { index, rule ->
            assertEquals("Rule${index + 1}", rule.ruleId)
            assertEquals("checkstyle", rule.tool)
            assertEquals(true, rule.breaksBuild)
            assertEquals(severities[index], rule.severity)
        }
    }

    @Test
    fun `undetectable breaksBuild is null on all rules`() {
        val configPath = "/project/checkstyle.xml"
        val rules = listOf(ParsedRule("LineLength", RuleSeverity.WARNING))
        val result = collector(
            tools = listOf(LinterToolDescriptor("checkstyle", configPath, null)),
            parsers = mapOf("checkstyle" to FakeLinterConfigParser(mapOf(configPath to rules))),
        ).collect()
        val ok = assertOk(result)
        assertTrue(ok.activeRules.all { it.breaksBuild == null })
    }

    @Test
    fun `no tools applied returns Empty`() {
        val result = collector(tools = emptyList(), parsers = emptyMap()).collect()
        assertTrue(result is SectionResult.Empty, "Expected Empty but got $result")
    }

    @Test
    fun `Spotless-only input produces zero activeRules and section is Ok`() {
        val result = collector(
            tools = listOf(LinterToolDescriptor("spotless", "/project/spotless.xml", null)),
            parsers = emptyMap(),
        ).collect()
        val ok = assertOk(result)
        assertTrue(ok.activeRules.isEmpty())
        assertTrue(ok.toolsWithUnresolvableConfig.contains("spotless"))
    }

    @Test
    fun `Gradle Checkstyle applied breaksBuild is null on all rules`() {
        val configPath = "/project/config/checkstyle/checkstyle.xml"
        val rules = listOf(ParsedRule("LineLength", RuleSeverity.WARNING))
        val result = collector(
            tools = listOf(LinterToolDescriptor("checkstyle", configPath, null)),
            parsers = mapOf("checkstyle" to FakeLinterConfigParser(mapOf(configPath to rules))),
        ).collect()
        val ok = assertOk(result)
        assertTrue(ok.activeRules.all { it.breaksBuild == null })
    }

    @Test
    fun `configFilePath null marks tool as unresolvable with zero activeRules and section Ok`() {
        val result = collector(
            tools = listOf(LinterToolDescriptor("checkstyle", null, true)),
            parsers = mapOf("checkstyle" to FakeLinterConfigParser()),
        ).collect()
        val ok = assertOk(result)
        assertTrue(ok.activeRules.isEmpty())
        assertTrue(ok.toolsWithUnresolvableConfig.contains("checkstyle"))
    }

    @Test
    fun `parseRules throwing marks tool as unresolvable with zero activeRules and section Ok`() {
        val configPath = "/project/checkstyle.xml"
        val result = collector(
            tools = listOf(LinterToolDescriptor("checkstyle", configPath, true)),
            parsers = mapOf("checkstyle" to FakeLinterConfigParser(emptyMap())),
        ).collect()
        val ok = assertOk(result)
        assertTrue(ok.activeRules.isEmpty())
        assertTrue(ok.toolsWithUnresolvableConfig.contains("checkstyle"))
    }

    @Test
    fun `no parser registered for tool marks it as unresolvable`() {
        val result = collector(
            tools = listOf(LinterToolDescriptor("pmd", "/project/pmd.xml", true)),
            parsers = emptyMap(),
        ).collect()
        val ok = assertOk(result)
        assertTrue(ok.activeRules.isEmpty())
        assertTrue(ok.toolsWithUnresolvableConfig.contains("pmd"))
    }

    @Test
    fun `multiple configs for same tool rules merged each carrying its own breaksBuild`() {
        val mainPath = "/project/checkstyle-main.xml"
        val testPath = "/project/checkstyle-test.xml"
        val mainRules = listOf(ParsedRule("LineLength", RuleSeverity.WARNING))
        val testRules = listOf(ParsedRule("MagicNumber", RuleSeverity.ERROR))
        val result = collector(
            tools = listOf(
                LinterToolDescriptor("checkstyle", mainPath, true),
                LinterToolDescriptor("checkstyle", testPath, false),
            ),
            parsers = mapOf(
                "checkstyle" to FakeLinterConfigParser(
                    mapOf(mainPath to mainRules, testPath to testRules)
                )
            ),
        ).collect()
        val ok = assertOk(result)
        assertEquals(2, ok.activeRules.size)
        val lineLengthRule = ok.activeRules.single { it.ruleId == "LineLength" }
        assertEquals(true, lineLengthRule.breaksBuild)
        val magicNumberRule = ok.activeRules.single { it.ruleId == "MagicNumber" }
        assertEquals(false, magicNumberRule.breaksBuild)
    }

    @Test
    fun `partial failure one tool resolves while another is unresolvable section is Ok`() {
        val checkstylePath = "/project/checkstyle.xml"
        val rules = listOf(ParsedRule("LineLength", RuleSeverity.WARNING))
        val result = collector(
            tools = listOf(
                LinterToolDescriptor("checkstyle", checkstylePath, true),
                LinterToolDescriptor("pmd", null, null),
            ),
            parsers = mapOf("checkstyle" to FakeLinterConfigParser(mapOf(checkstylePath to rules))),
        ).collect()
        val ok = assertOk(result)
        assertEquals(1, ok.activeRules.size)
        assertEquals("LineLength", ok.activeRules.single().ruleId)
        assertEquals(listOf("pmd"), ok.toolsWithUnresolvableConfig)
    }

    @Test
    fun `resolved tool produces null breaksBuild when descriptor has null`() {
        val configPath = "/project/pmd.xml"
        val rules = listOf(ParsedRule("AbstractClassWithoutAbstractMethod", RuleSeverity.ERROR))
        val result = collector(
            tools = listOf(LinterToolDescriptor("pmd", configPath, null)),
            parsers = mapOf("pmd" to FakeLinterConfigParser(mapOf(configPath to rules))),
        ).collect()
        val ok = assertOk(result)
        assertNull(ok.activeRules.single().breaksBuild)
    }

    // --- helpers ---

    private fun collector(
        tools: List<LinterToolDescriptor>,
        parsers: Map<String, LinterConfigParser>,
    ) = LinterCollector(FakeLinterPort(tools), parsers)

    private fun assertOk(result: SectionResult<*>): LinterInfo {
        assertTrue(result is SectionResult.Ok<*>, "Expected Ok but got $result")
        @Suppress("UNCHECKED_CAST")
        return (result as SectionResult.Ok<LinterInfo>).data
    }
}
