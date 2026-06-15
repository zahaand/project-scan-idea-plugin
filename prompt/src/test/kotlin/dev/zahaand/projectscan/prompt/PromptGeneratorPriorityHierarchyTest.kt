package dev.zahaand.projectscan.prompt

import dev.zahaand.projectscan.baseline.BaselineCategory
import dev.zahaand.projectscan.baseline.BaselineLanguage
import dev.zahaand.projectscan.baseline.BaselineLevel
import dev.zahaand.projectscan.baseline.BaselineRule
import dev.zahaand.projectscan.baseline.Obligation
import dev.zahaand.projectscan.model.ActiveRule
import dev.zahaand.projectscan.model.LinterInfo
import dev.zahaand.projectscan.model.RuleSeverity
import dev.zahaand.projectscan.model.ScanResult
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StackInfo
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromptGeneratorPriorityHierarchyTest {

    private val errorSeverityRule = ActiveRule("rule-ERROR", "checkstyle", RuleSeverity.ERROR, null)
    private val breaksBuildTrueRule = ActiveRule("rule-BREAKS", "pmd", RuleSeverity.WARNING, true)
    private val breaksBuildNullRule = ActiveRule("rule-NULL", "detekt", RuleSeverity.WARNING, null)
    private val advisoryOnlyRule = ActiveRule("rule-ADVISORY", "ktlint", RuleSeverity.WARNING, false)

    private val linterRules = listOf(errorSeverityRule, breaksBuildTrueRule, breaksBuildNullRule, advisoryOnlyRule)

    private val baselineRules = listOf(
        BaselineRule(
            "b-01", BaselineLevel.CORRECTNESS, BaselineCategory.NULL_SAFETY, Obligation.MUST,
            "Baseline statement 01.", "Rationale.", 8, listOf(BaselineLanguage.JAVA),
        ),
    )

    private val scanResult = ScanResult(
        stack = SectionResult.Ok(StackInfo(buildSystem = null, jdkVersion = null, languageLevel = null, dependencies = emptyList())),
        codeStyle = SectionResult.Empty,
        linters = SectionResult.Ok(LinterInfo(activeRules = linterRules)),
        tests = SectionResult.Empty,
        structure = SectionResult.Empty,
    )

    private val generator = PromptGenerator()

    private fun corePrinciplesSection(rendered: String): String {
        val start = rendered.indexOf("## Core Principles")
        assertTrue(start >= 0, "## Core Principles block must be present")
        val nextBlock = rendered.indexOf("\n## ", start + 1)
        return if (nextBlock >= 0) rendered.substring(start, nextBlock) else rendered.substring(start)
    }

    @Test
    fun `scenario 1 - core principles contains explicit conflict-resolution wording`() {
        val section = corePrinciplesSection(generator.generate(scanResult, baselineRules).render())

        assertTrue(
            section.contains("take precedence", ignoreCase = true) ||
                section.contains("supersede", ignoreCase = true) ||
                section.contains("override", ignoreCase = true) ||
                section.contains("precedence", ignoreCase = true),
            "Core Principles must contain explicit conflict-resolution wording (e.g. 'take precedence')",
        )
        assertTrue(
            section.contains("project standard"),
            "Conflict-resolution text must reference 'project standard'",
        )
        assertTrue(
            section.contains("baseline quality requirement"),
            "Conflict-resolution text must reference 'baseline quality requirement'",
        )
    }

    @Test
    fun `scenario 2 - mandatory and advisory sub-sections split correctly with null breaksBuild handled`() {
        val section = corePrinciplesSection(generator.generate(scanResult, baselineRules).render())

        assertTrue(
            section.contains("#### Mandatory (build-breaking)"),
            "Core Principles must contain '#### Mandatory (build-breaking)' for ERROR/breaksBuild=true rules",
        )
        assertTrue(
            section.contains("#### Advisory"),
            "Core Principles must contain '#### Advisory' for other rules including breaksBuild=null",
        )

        val mandatoryStart = section.indexOf("#### Mandatory (build-breaking)")
        val advisoryStart = section.indexOf("#### Advisory")

        val mandatorySection = section.substring(mandatoryStart, advisoryStart)
        assertTrue(mandatorySection.contains("rule-ERROR"), "rule-ERROR (severity=ERROR) must be under #### Mandatory (build-breaking)")
        assertTrue(mandatorySection.contains("rule-BREAKS"), "rule-BREAKS (breaksBuild=true) must be under #### Mandatory (build-breaking)")

        val advisorySection = section.substring(advisoryStart)
        assertTrue(advisorySection.contains("rule-NULL"), "rule-NULL (breaksBuild=null) must be under #### Advisory — no exception for null")
        assertTrue(advisorySection.contains("rule-ADVISORY"), "rule-ADVISORY (breaksBuild=false) must be under #### Advisory")

        assertTrue(
            section.contains("#### Mandatory (build-breaking)\n-"),
            "No blank line between '#### Mandatory (build-breaking)' heading and its first bullet (prompt-api.md separator contract)",
        )
        assertTrue(
            section.contains("#### Advisory\n-"),
            "No blank line between '#### Advisory' heading and its first bullet (prompt-api.md separator contract)",
        )
    }

    @Test
    fun `scenario 3 - every bullet in core principles is under a project standard or baseline quality requirement heading`() {
        val section = corePrinciplesSection(generator.generate(scanResult, baselineRules).render())

        val validHeadings = setOf("project standard", "baseline quality requirement")
        var currentH3: String? = null

        for (line in section.lines()) {
            if (line.startsWith("### ")) {
                val heading = line.removePrefix("### ").trim()
                assertTrue(
                    heading in validHeadings,
                    "### heading must be exactly 'project standard' or 'baseline quality requirement', found: '$heading'",
                )
                currentH3 = heading
            }
            if (line.trimStart().startsWith("- ")) {
                assertTrue(
                    currentH3 != null,
                    "Bullet '$line' must appear under a ### project standard or ### baseline quality requirement heading",
                )
            }
        }
    }
}
