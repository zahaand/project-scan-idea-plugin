package dev.zahaand.projectscan.prompt

import dev.zahaand.projectscan.baseline.BaselineCategory
import dev.zahaand.projectscan.baseline.BaselineLanguage
import dev.zahaand.projectscan.baseline.BaselineLevel
import dev.zahaand.projectscan.baseline.BaselineRule
import dev.zahaand.projectscan.baseline.Obligation
import dev.zahaand.projectscan.model.LinterInfo
import dev.zahaand.projectscan.model.ScanResult
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StackInfo
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromptGeneratorLanguageLevelFilterTest {
    private fun rule(
        id: String,
        minJavaLevel: Int,
    ) = BaselineRule(
        id,
        BaselineLevel.CORRECTNESS,
        BaselineCategory.NULL_SAFETY,
        Obligation.MUST,
        "Statement for $id.",
        "Rationale.",
        minJavaLevel,
        listOf(BaselineLanguage.JAVA),
    )

    private val ruleLevel8 = rule("rule-8", 8)
    private val ruleLevel11 = rule("rule-11", 11)
    private val ruleLevel17 = rule("rule-17", 17)
    private val ruleLevel21 = rule("rule-21", 21)

    private val allRules = listOf(ruleLevel8, ruleLevel11, ruleLevel17, ruleLevel21)

    private fun scanWithLevel(languageLevel: String?) =
        ScanResult(
            stack =
                SectionResult.Ok(
                    StackInfo(
                        buildSystem = null,
                        jdkVersion = null,
                        languageLevel = languageLevel,
                    ),
                ),
            codeStyle = SectionResult.Empty,
            linters = SectionResult.Ok(LinterInfo(activeRules = emptyList())),
            tests = SectionResult.Empty,
            structure = SectionResult.Empty,
        )

    private val emptyStackScan =
        ScanResult(
            stack = SectionResult.Empty,
            codeStyle = SectionResult.Empty,
            linters = SectionResult.Ok(LinterInfo(activeRules = emptyList())),
            tests = SectionResult.Empty,
            structure = SectionResult.Empty,
        )

    private val generator = PromptGenerator()

    private fun baselineSection(rendered: String): String {
        val start = rendered.indexOf("### baseline quality requirement")
        assertTrue(start >= 0, "### baseline quality requirement must be present")
        val nextBlock = rendered.indexOf("\n## ", start)
        return if (nextBlock >= 0) rendered.substring(start, nextBlock) else rendered.substring(start)
    }

    @Test
    fun `scenario 1 - level 11 filters out rules with minJavaLevel greater than 11`() {
        val section = baselineSection(generator.generate(scanWithLevel("11"), allRules).render())

        assertTrue(section.contains("Statement for rule-8"), "rule-8 (minJavaLevel=8) must pass level-11 filter")
        assertTrue(section.contains("Statement for rule-11"), "rule-11 (minJavaLevel=11) must pass level-11 filter")
        assertFalse(
            section.contains("Statement for rule-17"),
            "rule-17 (minJavaLevel=17) must be filtered out at level 11",
        )
        assertFalse(
            section.contains("Statement for rule-21"),
            "rule-21 (minJavaLevel=21) must be filtered out at level 11",
        )
    }

    @Test
    fun `scenario 2 - level 21 passes all rules`() {
        val section = baselineSection(generator.generate(scanWithLevel("21"), allRules).render())

        allRules.forEach { rule ->
            assertTrue(section.contains("Statement for ${rule.id}"), "${rule.id} must pass level-21 filter")
        }
    }

    @Test
    fun `scenario 3 - level 8 passes only minJavaLevel=8 rule`() {
        val section = baselineSection(generator.generate(scanWithLevel("8"), allRules).render())

        assertTrue(section.contains("Statement for rule-8"), "rule-8 (minJavaLevel=8) must pass level-8 filter")
        assertFalse(
            section.contains("Statement for rule-11"),
            "rule-11 (minJavaLevel=11) must be filtered out at level 8",
        )
        assertFalse(
            section.contains("Statement for rule-17"),
            "rule-17 (minJavaLevel=17) must be filtered out at level 8",
        )
        assertFalse(
            section.contains("Statement for rule-21"),
            "rule-21 (minJavaLevel=21) must be filtered out at level 8",
        )
    }

    @Test
    fun `scenario 4 - null language level emits full rule set without filtering`() {
        val section = baselineSection(generator.generate(scanWithLevel(null), allRules).render())

        allRules.forEach { rule ->
            assertTrue(
                section.contains("Statement for ${rule.id}"),
                "${rule.id} must appear when languageLevel is null",
            )
        }
    }

    @Test
    fun `scenario 5 - unknown language level string emits full rule set without filtering`() {
        val section = baselineSection(generator.generate(scanWithLevel("unknown"), allRules).render())

        allRules.forEach { rule ->
            assertTrue(
                section.contains("Statement for ${rule.id}"),
                "${rule.id} must appear when languageLevel is 'unknown'",
            )
        }
    }

    @Test
    fun `scenario 6 - empty stack section emits full rule set without filtering`() {
        val section = baselineSection(generator.generate(emptyStackScan, allRules).render())

        allRules.forEach { rule ->
            assertTrue(
                section.contains("Statement for ${rule.id}"),
                "${rule.id} must appear when stack section is Empty",
            )
        }
    }

    @Test
    fun `scenario 7 - version strings with suffix extract leading integer correctly`() {
        val sectionDot = baselineSection(generator.generate(scanWithLevel("17.0.1"), allRules).render())
        assertTrue(sectionDot.contains("Statement for rule-8"), "rule-8 must pass level-17 filter from '17.0.1'")
        assertTrue(sectionDot.contains("Statement for rule-11"), "rule-11 must pass level-17 filter from '17.0.1'")
        assertTrue(sectionDot.contains("Statement for rule-17"), "rule-17 must pass level-17 filter from '17.0.1'")
        assertFalse(
            sectionDot.contains("Statement for rule-21"),
            "rule-21 must be filtered out at level 17 from '17.0.1'",
        )

        val sectionPreview = baselineSection(generator.generate(scanWithLevel("21_PREVIEW"), allRules).render())
        allRules.forEach { rule ->
            assertTrue(
                sectionPreview.contains("Statement for ${rule.id}"),
                "${rule.id} must pass level-21 filter from '21_PREVIEW'",
            )
        }
    }

    @Test
    fun `scenario 8 - leading whitespace is trimmed before digit extraction`() {
        val section = baselineSection(generator.generate(scanWithLevel(" 11"), allRules).render())

        assertTrue(section.contains("Statement for rule-8"), "rule-8 must pass level-11 filter extracted from ' 11'")
        assertTrue(section.contains("Statement for rule-11"), "rule-11 must pass level-11 filter extracted from ' 11'")
        assertFalse(section.contains("Statement for rule-17"), "rule-17 must be filtered out from ' 11'")
        assertFalse(section.contains("Statement for rule-21"), "rule-21 must be filtered out from ' 11'")
    }

    @Test
    fun `scenario 9 - digits followed by non-digit extracts only leading digits`() {
        val section = baselineSection(generator.generate(scanWithLevel("11a"), allRules).render())

        assertTrue(section.contains("Statement for rule-8"), "rule-8 must pass level-11 filter extracted from '11a'")
        assertTrue(section.contains("Statement for rule-11"), "rule-11 must pass level-11 filter extracted from '11a'")
        assertFalse(section.contains("Statement for rule-17"), "rule-17 must be filtered out from '11a'")
        assertFalse(section.contains("Statement for rule-21"), "rule-21 must be filtered out from '11a'")
    }

    @Test
    fun `SC-004 case e - empty string language level emits full rule set without filtering`() {
        val section = baselineSection(generator.generate(scanWithLevel(""), allRules).render())

        allRules.forEach { rule ->
            assertTrue(
                section.contains("Statement for ${rule.id}"),
                "${rule.id} must appear when languageLevel is empty string",
            )
        }
    }
}
