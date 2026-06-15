package dev.zahaand.projectscan.prompt

import dev.zahaand.projectscan.baseline.BaselineRule
import dev.zahaand.projectscan.model.CodeStyleInfo
import dev.zahaand.projectscan.model.LinterInfo
import dev.zahaand.projectscan.model.ScanResult
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StackInfo
import dev.zahaand.projectscan.model.StructureInfo
import dev.zahaand.projectscan.model.TestInfo

class PromptGenerator {

    fun generate(scanResult: ScanResult, baselineRules: List<BaselineRule>): ConstitutionPrompt =
        ConstitutionPrompt(
            listOf(
                PromptBlock("Core Principles", buildCorePrinciplesBlock(scanResult.linters, baselineRules)),
                PromptBlock("Tech Stack", buildTechStackBlock(scanResult.stack)),
                PromptBlock("Code Style & Static Analysis", buildCodeStyleBlock(scanResult.codeStyle)),
                PromptBlock("Testing", buildTestingBlock(scanResult.tests)),
                PromptBlock("Project Structure", buildProjectStructureBlock(scanResult.structure)),
                PromptBlock("Governance", buildGovernanceBlock()),
            ),
        )

    private fun buildCorePrinciplesBlock(
        linters: SectionResult<LinterInfo>,
        baselineRules: List<BaselineRule>,
    ): String {
        val linterBullets = when (linters) {
            is SectionResult.Ok -> linters.data.activeRules.map { rule ->
                "- ${rule.ruleId} [${rule.tool}] (${rule.severity.name})"
            }
            else -> emptyList()
        }
        val projectStandard = OriginGroup(
            label = "project standard",
            mandatoryRules = emptyList(),
            advisoryRules = linterBullets,
            emptyNotation = if (linterBullets.isEmpty()) "No active linter rules were detected." else null,
        )

        val baselineBullets = baselineRules.map { rule ->
            "- ${rule.obligation.name} ${rule.statement}"
        }
        val baseline = OriginGroup(
            label = "baseline quality requirement",
            mandatoryRules = emptyList(),
            advisoryRules = baselineBullets,
            emptyNotation = if (baselineBullets.isEmpty()) "No baseline rules are available." else null,
        )

        return buildString {
            append("### ${projectStandard.label}\n\n")
            append(projectStandard.emptyNotation ?: projectStandard.advisoryRules.joinToString("\n"))
            append("\n\n")
            append("### ${baseline.label}\n\n")
            append(baseline.emptyNotation ?: baseline.advisoryRules.joinToString("\n"))
        }
    }

    private fun buildTechStackBlock(stack: SectionResult<StackInfo>): String =
        when (stack) {
            is SectionResult.Ok -> {
                val info = stack.data
                val lines = mutableListOf<String>()
                info.buildSystem?.let { lines.add("- Build System: ${it.name}") }
                info.jdkVersion?.let { lines.add("- JDK Version: $it") }
                info.languageLevel?.let { lines.add("- Language Level: $it") }
                info.dependencies.forEach { dep ->
                    val version = dep.resolvedVersion?.let { ":$it" } ?: ""
                    lines.add("- ${dep.groupId}:${dep.artifactId}$version")
                }
                if (lines.isEmpty()) "not detected" else lines.joinToString("\n")
            }
            is SectionResult.Empty -> "not detected"
            is SectionResult.Error -> formatError(stack.cause)
        }

    private fun buildCodeStyleBlock(codeStyle: SectionResult<CodeStyleInfo>): String =
        when (codeStyle) {
            is SectionResult.Ok -> {
                val sources = codeStyle.data.sources
                if (sources.isEmpty()) "not detected"
                else sources.joinToString("\n") { "- ${it.type.name}: ${it.path}" }
            }
            is SectionResult.Empty -> "not detected"
            is SectionResult.Error -> formatError(codeStyle.cause)
        }

    private fun buildTestingBlock(tests: SectionResult<TestInfo>): String =
        when (tests) {
            is SectionResult.Ok -> {
                val info = tests.data
                val lines = mutableListOf<String>()
                info.frameworks.forEach { fw ->
                    val version = fw.version?.let { " $it" } ?: ""
                    lines.add("- Framework: ${fw.name}$version")
                }
                if (info.sourceRoots.isNotEmpty()) {
                    lines.add("- Source Roots: ${info.sourceRoots.joinToString(", ")}")
                }
                if (info.namingSuffixes.isNotEmpty()) {
                    lines.add("- Naming Suffixes: ${info.namingSuffixes.joinToString(", ")}")
                }
                info.coverageThreshold?.let { lines.add("- Coverage Threshold: $it") }
                if (lines.isEmpty()) "not detected" else lines.joinToString("\n")
            }
            is SectionResult.Empty -> "not detected"
            is SectionResult.Error -> formatError(tests.cause)
        }

    private fun buildProjectStructureBlock(structure: SectionResult<StructureInfo>): String =
        when (structure) {
            is SectionResult.Ok -> {
                val info = structure.data
                val lines = mutableListOf<String>()
                info.modules.forEach { module ->
                    lines.add("- Module: ${module.name}")
                    if (module.declaredDependencies.isNotEmpty()) {
                        val deps = module.declaredDependencies.joinToString(", ") {
                            "${it.groupId}:${it.artifactId}"
                        }
                        lines.add("  - Dependencies: $deps")
                    }
                    if (module.moduleDependencies.isNotEmpty()) {
                        lines.add("  - Module Dependencies: ${module.moduleDependencies.joinToString(", ")}")
                    }
                }
                if (info.rootPackages.isNotEmpty()) {
                    lines.add("- Root Packages: ${info.rootPackages.joinToString(", ")}")
                }
                if (lines.isEmpty()) "not detected" else lines.joinToString("\n")
            }
            is SectionResult.Empty -> "not detected"
            is SectionResult.Error -> formatError(structure.cause)
        }

    private fun buildGovernanceBlock(): String =
        """
        ### Semantic Versioning Policy

        Increment the Constitution version number recorded in its header according to these rules:
        - **MAJOR**: Removal or redefinition of core principles; changes to enforcement that would render a currently compliant codebase non-compliant.
        - **MINOR**: New rules, new sections, or additional guidance that does not conflict with existing principles.
        - **PATCH**: Wording corrections, clarifications, or reformatting that does not alter intent or enforcement.

        ### Changelog Convention

        Record every Constitution amendment in `CONSTITUTION_CHANGELOG.md` with the date, the new version, and a one-line description of each change. No change to the Constitution may be applied without a corresponding changelog entry.

        ### Amendment and Compliance Procedure

        To amend the Constitution: (1) propose the change in a team discussion, (2) regenerate or update this Constitution using `/speckit-constitution`, (3) tag the commit with the new version, and (4) review compliance within the next sprint. All team members are responsible for reading and adhering to the current Constitution version. Violations discovered in code review must be corrected before merge.
        """.trimIndent()

    private fun formatError(cause: String?): String =
        if (cause != null) "not available (cause: $cause)" else "not available"
}
