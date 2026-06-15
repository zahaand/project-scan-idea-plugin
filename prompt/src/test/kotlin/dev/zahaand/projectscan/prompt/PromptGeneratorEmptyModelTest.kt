package dev.zahaand.projectscan.prompt

import dev.zahaand.projectscan.baseline.BaselineCategory
import dev.zahaand.projectscan.baseline.BaselineLanguage
import dev.zahaand.projectscan.baseline.BaselineLevel
import dev.zahaand.projectscan.baseline.BaselineRule
import dev.zahaand.projectscan.baseline.Obligation
import dev.zahaand.projectscan.model.CodeStyleInfo
import dev.zahaand.projectscan.model.LinterInfo
import dev.zahaand.projectscan.model.Module
import dev.zahaand.projectscan.model.ScanResult
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StructureInfo
import dev.zahaand.projectscan.model.StyleSource
import dev.zahaand.projectscan.model.StyleSourceType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromptGeneratorEmptyModelTest {

    private val twoBaselineRules = listOf(
        BaselineRule(
            "rule-01", BaselineLevel.CORRECTNESS, BaselineCategory.NULL_SAFETY, Obligation.MUST,
            "Statement 01 null-check guard.", "Rationale 01.", 8, listOf(BaselineLanguage.JAVA),
        ),
        BaselineRule(
            "rule-02", BaselineLevel.BEST_PRACTICE, BaselineCategory.DECOMPOSITION, Obligation.SHOULD,
            "Statement 02 single-responsibility.", "Rationale 02.", 8, listOf(BaselineLanguage.JAVA),
        ),
    )

    private val allEmptyScanResult = ScanResult(
        stack = SectionResult.Empty,
        codeStyle = SectionResult.Empty,
        linters = SectionResult.Empty,
        tests = SectionResult.Empty,
        structure = SectionResult.Empty,
    )

    private val generator = PromptGenerator()

    @Test
    fun `scenario 1 - all-empty scan with non-empty baseline yields non-empty prompt with all six headings`() {
        val rendered = generator.generate(allEmptyScanResult, twoBaselineRules).render()

        assertFalse(rendered.isBlank(), "Rendered prompt must not be blank")
        assertTrue(rendered.contains("## Core Principles"), "Must contain ## Core Principles")
        assertTrue(rendered.contains("## Tech Stack"), "Must contain ## Tech Stack")
        assertTrue(rendered.contains("## Code Style & Static Analysis"), "Must contain ## Code Style & Static Analysis")
        assertTrue(rendered.contains("## Testing"), "Must contain ## Testing")
        assertTrue(rendered.contains("## Project Structure"), "Must contain ## Project Structure")
        assertTrue(rendered.contains("## Governance"), "Must contain ## Governance")
    }

    @Test
    fun `scenario 2 - project standard group present with not-detected emptyNotation when linters is Empty`() {
        val rendered = generator.generate(allEmptyScanResult, twoBaselineRules).render()

        val projectStandardStart = rendered.indexOf("### project standard")
        assertTrue(projectStandardStart >= 0, "### project standard must be present even when linters is SectionResult.Empty")

        val baselineStart = rendered.indexOf("### baseline quality requirement")
        assertTrue(baselineStart > projectStandardStart, "### baseline quality requirement must follow ### project standard")

        val projectStandardSection = rendered.substring(projectStandardStart, baselineStart)

        // SectionResult.Empty linters → emptyNotation must contain "not detected" per FR-008
        assertTrue(
            projectStandardSection.contains("not detected"),
            "project standard emptyNotation must contain 'not detected' when linters is SectionResult.Empty (FR-008)",
        )
        // No #### sub-headings when emptyNotation is shown
        assertFalse(projectStandardSection.contains("#### Mandatory"), "#### Mandatory must not appear when linters is Empty")
        assertFalse(projectStandardSection.contains("#### Advisory"), "#### Advisory must not appear when linters is Empty")
    }

    @Test
    fun `scenario 3 - all baseline rules present with obligation markers and no language-level filtering when stack is Empty`() {
        val rendered = generator.generate(allEmptyScanResult, twoBaselineRules).render()

        val baselineStart = rendered.indexOf("### baseline quality requirement")
        assertTrue(baselineStart >= 0, "### baseline quality requirement must be present")

        val nextBlockStart = rendered.indexOf("\n## ", baselineStart)
        val baselineSection = if (nextBlockStart >= 0) rendered.substring(baselineStart, nextBlockStart)
        else rendered.substring(baselineStart)

        twoBaselineRules.forEach { rule ->
            assertTrue(baselineSection.contains(rule.statement), "Baseline rule '${rule.id}' statement must appear in Core Principles")
            assertTrue(
                baselineSection.contains(rule.obligation.name),
                "Baseline rule '${rule.id}' must carry obligation marker ${rule.obligation.name}",
            )
        }
    }

    @Test
    fun `scenario 4 - Tech Stack Code Style Testing Project Structure each contain not detected when all sections Empty`() {
        val rendered = generator.generate(allEmptyScanResult, twoBaselineRules).render()

        fun extractBlockContent(heading: String): String {
            val start = rendered.indexOf("## $heading")
            assertTrue(start >= 0, "## $heading must be present")
            val nextBlock = rendered.indexOf("\n## ", start + 1)
            return if (nextBlock >= 0) rendered.substring(start, nextBlock) else rendered.substring(start)
        }

        listOf("Tech Stack", "Code Style & Static Analysis", "Testing", "Project Structure").forEach { heading ->
            val section = extractBlockContent(heading)
            assertTrue(section.contains("not detected"), "## $heading must contain 'not detected' when SectionResult.Empty (FR-008)")
            assertFalse(section.contains("not available"), "## $heading must NOT contain 'not available' when SectionResult.Empty — wrong marker")
        }
    }

    @Test
    fun `scenario 5 - mixed Ok and Error scan renders error sections as not available and never emits cause-null`() {
        val mixedScanResult = ScanResult(
            stack = SectionResult.Error("build system detection failed"),
            codeStyle = SectionResult.Ok(
                CodeStyleInfo(sources = listOf(StyleSource(StyleSourceType.CHECKSTYLE, ".checkstyle.xml"))),
            ),
            linters = SectionResult.Error("linter tool crashed"),
            tests = SectionResult.Error(null),
            structure = SectionResult.Ok(
                StructureInfo(
                    modules = listOf(Module("app", emptyList(), emptyList())),
                    rootPackages = listOf("dev.zahaand.app"),
                ),
            ),
        )

        val rendered = generator.generate(mixedScanResult, twoBaselineRules).render()

        // stack Error with non-null cause → Tech Stack contains "not available (cause: X)"
        val techStackStart = rendered.indexOf("## Tech Stack")
        assertTrue(techStackStart >= 0, "## Tech Stack must be present")
        val nextAfterStack = rendered.indexOf("\n## ", techStackStart + 1)
        val techStackSection = rendered.substring(techStackStart, nextAfterStack)
        assertTrue(
            techStackSection.contains("not available (cause: build system detection failed)"),
            "Tech Stack with Error(non-null cause) must render 'not available (cause: build system detection failed)'",
        )

        // tests Error with null cause → Testing contains plain "not available", never "(cause: null)"
        val testingStart = rendered.indexOf("## Testing")
        assertTrue(testingStart >= 0, "## Testing must be present")
        val nextAfterTesting = rendered.indexOf("\n## ", testingStart + 1)
        val testingSection = rendered.substring(testingStart, nextAfterTesting)
        assertTrue(testingSection.contains("not available"), "Testing with Error(null cause) must render 'not available'")
        assertFalse(testingSection.contains("(cause:"), "Testing with Error(null cause) must NOT include any cause fragment")

        // linters Error → project standard section shows "not available" indication (FR-008, SC-005)
        val projectStandardStart = rendered.indexOf("### project standard")
        assertTrue(projectStandardStart >= 0, "### project standard must be present")
        val baselineIdx = rendered.indexOf("### baseline quality requirement")
        val projectStandardSection = rendered.substring(projectStandardStart, baselineIdx)
        assertTrue(
            projectStandardSection.contains("not available"),
            "project standard must show 'not available' when linters is SectionResult.Error (FR-008)",
        )

        // "(cause: null)" must NEVER appear anywhere
        assertFalse(rendered.contains("(cause: null)"), "'(cause: null)' must never appear in rendered output")
    }

    @Test
    fun `scenario 6 C1 - linters Ok with empty activeRules renders project standard with not-detected emptyNotation and no mandatory or advisory headings`() {
        val scanWithEmptyLinterRules = ScanResult(
            stack = SectionResult.Empty,
            codeStyle = SectionResult.Empty,
            linters = SectionResult.Ok(LinterInfo(activeRules = emptyList())),
            tests = SectionResult.Empty,
            structure = SectionResult.Empty,
        )

        val rendered = generator.generate(scanWithEmptyLinterRules, twoBaselineRules).render()

        val projectStandardStart = rendered.indexOf("### project standard")
        assertTrue(projectStandardStart >= 0, "(a) ### project standard must be present")

        val baselineStart = rendered.indexOf("### baseline quality requirement")
        assertTrue(baselineStart > projectStandardStart, "### baseline quality requirement must follow ### project standard")
        val projectStandardSection = rendered.substring(projectStandardStart, baselineStart)

        // (b) emptyNotation must contain "not detected" per FR-008 — never omit the notation
        assertTrue(
            projectStandardSection.contains("not detected"),
            "(b) project standard emptyNotation must contain 'not detected' when linters is Ok(emptyList()) (FR-008)",
        )
        // (c) No #### sub-headings when emptyNotation is shown
        assertFalse(projectStandardSection.contains("#### Mandatory"), "(c) #### Mandatory must not appear when activeRules is empty")
        assertFalse(projectStandardSection.contains("#### Advisory"), "(c) #### Advisory must not appear when activeRules is empty")
    }
}
