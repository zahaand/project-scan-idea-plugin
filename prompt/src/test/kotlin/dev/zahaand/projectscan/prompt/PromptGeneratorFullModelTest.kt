package dev.zahaand.projectscan.prompt

import dev.zahaand.projectscan.baseline.BaselineCategory
import dev.zahaand.projectscan.baseline.BaselineLanguage
import dev.zahaand.projectscan.baseline.BaselineLevel
import dev.zahaand.projectscan.baseline.BaselineRule
import dev.zahaand.projectscan.baseline.Obligation
import dev.zahaand.projectscan.model.ActiveRule
import dev.zahaand.projectscan.model.BuildSystem
import dev.zahaand.projectscan.model.CodeStyleInfo
import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.LinterInfo
import dev.zahaand.projectscan.model.Module
import dev.zahaand.projectscan.model.RuleSeverity
import dev.zahaand.projectscan.model.ScanResult
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StackInfo
import dev.zahaand.projectscan.model.StructureInfo
import dev.zahaand.projectscan.model.StyleSource
import dev.zahaand.projectscan.model.StyleSourceType
import dev.zahaand.projectscan.model.TestInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromptGeneratorFullModelTest {
    private val fiveLinterRules =
        listOf(
            ActiveRule("checkstyle:MagicNumber", "checkstyle", RuleSeverity.WARNING, null),
            ActiveRule("checkstyle:EmptyBlock", "checkstyle", RuleSeverity.ERROR, true),
            ActiveRule("pmd:ExcessiveMethodLength", "pmd", RuleSeverity.WARNING, false),
            ActiveRule("pmd:NullAssignment", "pmd", RuleSeverity.ERROR, null),
            ActiveRule("detekt:TooManyFunctions", "detekt", RuleSeverity.WARNING, null),
        )

    private val thirteenBaselineRules =
        listOf(
            BaselineRule(
                "rule-01",
                BaselineLevel.CORRECTNESS,
                BaselineCategory.NULL_SAFETY,
                Obligation.MUST,
                "Statement 01 null-check guard.",
                "Rationale 01.",
                8,
                listOf(BaselineLanguage.JAVA),
            ),
            BaselineRule(
                "rule-02",
                BaselineLevel.CORRECTNESS,
                BaselineCategory.NULL_SAFETY,
                Obligation.MUST,
                "Statement 02 optional-get-check.",
                "Rationale 02.",
                8,
                listOf(BaselineLanguage.JAVA),
            ),
            BaselineRule(
                "rule-03",
                BaselineLevel.CORRECTNESS,
                BaselineCategory.RESOURCE_MANAGEMENT,
                Obligation.MUST,
                "Statement 03 try-with-resources.",
                "Rationale 03.",
                8,
                listOf(BaselineLanguage.JAVA),
            ),
            BaselineRule(
                "rule-04",
                BaselineLevel.CORRECTNESS,
                BaselineCategory.RESOURCE_MANAGEMENT,
                Obligation.MUST,
                "Statement 04 no-stream-after-close.",
                "Rationale 04.",
                8,
                listOf(BaselineLanguage.JAVA),
            ),
            BaselineRule(
                "rule-05",
                BaselineLevel.CORRECTNESS,
                BaselineCategory.CONCURRENCY,
                Obligation.MUST,
                "Statement 05 synchronized-on-non-final.",
                "Rationale 05.",
                8,
                listOf(BaselineLanguage.JAVA),
            ),
            BaselineRule(
                "rule-06",
                BaselineLevel.CORRECTNESS,
                BaselineCategory.CONCURRENCY,
                Obligation.MUST,
                "Statement 06 double-checked-locking-volatile.",
                "Rationale 06.",
                8,
                listOf(BaselineLanguage.JAVA),
            ),
            BaselineRule(
                "rule-07",
                BaselineLevel.CORRECTNESS,
                BaselineCategory.DANGEROUS_CONSTRUCTS,
                Obligation.MUST,
                "Statement 07 no-reflection-access.",
                "Rationale 07.",
                8,
                listOf(BaselineLanguage.JAVA),
            ),
            BaselineRule(
                "rule-08",
                BaselineLevel.CORRECTNESS,
                BaselineCategory.DANGEROUS_CONSTRUCTS,
                Obligation.MUST,
                "Statement 08 no-system-exit.",
                "Rationale 08.",
                8,
                listOf(BaselineLanguage.JAVA),
            ),
            BaselineRule(
                "rule-09",
                BaselineLevel.CORRECTNESS,
                BaselineCategory.DANGEROUS_CONSTRUCTS,
                Obligation.MUST,
                "Statement 09 no-finalizer.",
                "Rationale 09.",
                8,
                listOf(BaselineLanguage.JAVA),
            ),
            BaselineRule(
                "rule-10",
                BaselineLevel.CORRECTNESS,
                BaselineCategory.CONCURRENCY,
                Obligation.MUST,
                "Statement 10 parallel-streams-no-shared-state.",
                "Rationale 10.",
                8,
                listOf(BaselineLanguage.JAVA),
            ),
            BaselineRule(
                "rule-11",
                BaselineLevel.BEST_PRACTICE,
                BaselineCategory.EXCEPTION_HANDLING,
                Obligation.SHOULD,
                "Statement 11 checked-exceptions-for-api.",
                "Rationale 11.",
                8,
                listOf(BaselineLanguage.JAVA),
            ),
            BaselineRule(
                "rule-12",
                BaselineLevel.BEST_PRACTICE,
                BaselineCategory.STRING_PERFORMANCE,
                Obligation.MUST,
                "Statement 12 string-builder-in-loop.",
                "Rationale 12.",
                8,
                listOf(BaselineLanguage.JAVA),
            ),
            BaselineRule(
                "rule-13",
                BaselineLevel.BEST_PRACTICE,
                BaselineCategory.DECOMPOSITION,
                Obligation.SHOULD,
                "Statement 13 single-responsibility.",
                "Rationale 13.",
                8,
                listOf(BaselineLanguage.JAVA),
            ),
        )

    private val fullScanResult =
        ScanResult(
            stack =
                SectionResult.Ok(
                    StackInfo(
                        buildSystem = BuildSystem.GRADLE,
                        jdkVersion = "21",
                        languageLevel = "17",
                    ),
                ),
            codeStyle =
                SectionResult.Ok(
                    CodeStyleInfo(
                        sources = listOf(StyleSource(StyleSourceType.CHECKSTYLE, ".checkstyle.xml")),
                    ),
                ),
            linters =
                SectionResult.Ok(
                    LinterInfo(activeRules = fiveLinterRules),
                ),
            tests =
                SectionResult.Ok(
                    TestInfo(
                        sourceRoots = listOf("src/test/kotlin"),
                        namingSuffixes = listOf("Test"),
                        coverageThreshold = 0.8,
                    ),
                ),
            structure =
                SectionResult.Ok(
                    StructureInfo(
                        modules =
                            listOf(
                                Module(
                                    "app",
                                    listOf(
                                        Dependency("org.junit.jupiter", "junit-jupiter", "5.11.0"),
                                        Dependency("org.springframework.boot", "spring-boot-starter", "3.3.0"),
                                        Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.0"),
                                    ),
                                    emptyList(),
                                ),
                            ),
                    ),
                ),
        )

    private val generator = PromptGenerator()

    @Test
    fun `scenario 1 - rendered text opens with speckit-constitution address and contains all six headings`() {
        val rendered = generator.generate(fullScanResult, thirteenBaselineRules).render()

        val specKitIndex = rendered.indexOf("/speckit-constitution")
        assertTrue(specKitIndex >= 0, "Rendered text must contain reference to /speckit-constitution")
        assertTrue(
            specKitIndex < 200,
            "Reference to /speckit-constitution must appear near the beginning (within 200 chars)",
        )

        assertTrue(rendered.contains("## Core Principles"), "Must contain ## Core Principles")
        assertTrue(rendered.contains("## Tech Stack"), "Must contain ## Tech Stack")
        assertTrue(rendered.contains("## Code Style & Static Analysis"), "Must contain ## Code Style & Static Analysis")
        assertTrue(rendered.contains("## Testing"), "Must contain ## Testing")
        assertTrue(rendered.contains("## Governance"), "Must contain ## Governance")
    }

    @Test
    fun `scenario 2 - all five linter rules appear under project standard`() {
        val rendered = generator.generate(fullScanResult, thirteenBaselineRules).render()

        val projectStandardStart = rendered.indexOf("### project standard")
        val baselineStart = rendered.indexOf("### baseline quality requirement")
        assertTrue(projectStandardStart >= 0, "### project standard must be present")
        assertTrue(
            baselineStart > projectStandardStart,
            "### baseline quality requirement must follow ### project standard",
        )

        val projectStandardSection = rendered.substring(projectStandardStart, baselineStart)
        fiveLinterRules.forEach { rule ->
            assertTrue(
                projectStandardSection.contains(rule.ruleId),
                "Rule ${rule.ruleId} must appear under ### project standard",
            )
        }
    }

    @Test
    fun `scenario 3 - all 13 baseline rules appear under baseline quality requirement with obligation markers`() {
        val rendered = generator.generate(fullScanResult, thirteenBaselineRules).render()

        val baselineStart = rendered.indexOf("### baseline quality requirement")
        assertTrue(baselineStart >= 0, "### baseline quality requirement must be present")

        val nextBlockStart = rendered.indexOf("\n## ", baselineStart)
        val baselineSection =
            if (nextBlockStart >= 0) {
                rendered.substring(baselineStart, nextBlockStart)
            } else {
                rendered.substring(baselineStart)
            }

        thirteenBaselineRules.forEach { rule ->
            val marker = rule.obligation.name
            val lines = baselineSection.lines()
            val ruleLine = lines.firstOrNull { it.contains(rule.statement) }
            assertNotNull(ruleLine, "Baseline rule '${rule.id}' statement must appear in baseline section")
            val markerIdx = ruleLine!!.indexOf(marker)
            val stmtIdx = ruleLine.indexOf(rule.statement)
            assertTrue(markerIdx >= 0, "Rule '${rule.id}' bullet must contain obligation marker $marker")
            assertTrue(markerIdx < stmtIdx, "Marker $marker must precede statement for rule '${rule.id}'")
        }

        assertEquals(13, thirteenBaselineRules.size, "Fixture must contain exactly 13 baseline rules")
    }

    @Test
    fun `scenario 4 - project standard precedes baseline quality requirement by character offset`() {
        val rendered = generator.generate(fullScanResult, thirteenBaselineRules).render()

        val projectStandardOffset = rendered.indexOf("### project standard")
        val baselineOffset = rendered.indexOf("### baseline quality requirement")

        assertTrue(projectStandardOffset >= 0, "### project standard must be present")
        assertTrue(baselineOffset >= 0, "### baseline quality requirement must be present")
        assertTrue(
            projectStandardOffset < baselineOffset,
            "### project standard must precede ### baseline quality requirement" +
                " (positions $projectStandardOffset vs $baselineOffset)",
        )
    }

    @Test
    fun `scenario 5 - tech stack block contains all stack info and all dependencies in per-artifact format`() {
        val rendered = generator.generate(fullScanResult, thirteenBaselineRules).render()

        val techStackStart = rendered.indexOf("## Tech Stack")
        assertTrue(techStackStart >= 0, "## Tech Stack must be present")

        val nextBlockStart = rendered.indexOf("\n## ", techStackStart + 1)
        val techStackSection =
            if (nextBlockStart >= 0) {
                rendered.substring(techStackStart, nextBlockStart)
            } else {
                rendered.substring(techStackStart)
            }

        assertTrue(techStackSection.contains("GRADLE"), "Tech Stack must contain build system GRADLE")
        assertTrue(techStackSection.contains("21"), "Tech Stack must contain JDK version 21")
        assertTrue(techStackSection.contains("Language Level"), "Tech Stack must contain language level label")
        assertTrue(techStackSection.contains("17"), "Tech Stack must contain language level value 17")
        assertTrue(techStackSection.contains("junit-jupiter"), "Tech Stack must contain dependency junit-jupiter")
        assertTrue(
            techStackSection.contains("spring-boot-starter"),
            "Tech Stack must contain dependency spring-boot-starter",
        )
        assertTrue(techStackSection.contains("jackson-databind"), "Tech Stack must contain dependency jackson-databind")
        assertFalse(
            techStackSection.contains(":* @"),
            "Single-artifact groups must use per-artifact format (no group headers)",
        )
    }

    @Test
    fun `scenario 6 - governance block contains all three required elements`() {
        val rendered = generator.generate(fullScanResult, thirteenBaselineRules).render()

        val governanceStart = rendered.indexOf("## Governance")
        assertTrue(governanceStart >= 0, "## Governance must be present")
        val governanceSection = rendered.substring(governanceStart)

        // (1) Semantic-versioning policy: MAJOR/MINOR/PATCH language must appear
        assertTrue(
            governanceSection.contains("MAJOR") &&
                governanceSection.contains("MINOR") &&
                governanceSection.contains("PATCH"),
            "Governance must contain a semantic-versioning policy referencing MAJOR, MINOR, and PATCH",
        )

        // (2) Changelog convention: some reference to recording/changelog/changelog file
        assertTrue(
            governanceSection.contains("changelog", ignoreCase = true) ||
                governanceSection.contains("CHANGELOG", ignoreCase = false),
            "Governance must contain a changelog convention",
        )

        // (3) Amendment and compliance procedure: reference to amendment/amend/compliance/procedure
        assertTrue(
            governanceSection.contains("amend", ignoreCase = true) ||
                governanceSection.contains("compliance", ignoreCase = true) ||
                governanceSection.contains("procedure", ignoreCase = true),
            "Governance must contain an amendment and compliance procedure",
        )
    }

    @Test
    fun `scenario 7 - render is deterministic for identical inputs`() {
        val first = generator.generate(fullScanResult, thirteenBaselineRules).render()
        val second = generator.generate(fullScanResult, thirteenBaselineRules).render()
        assertEquals(first, second, "render() must produce identical output for identical inputs (SC-007)")
    }
}
