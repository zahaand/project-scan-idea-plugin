package dev.zahaand.projectscan.prompt

import dev.zahaand.projectscan.baseline.BaselineRule
import dev.zahaand.projectscan.model.ActiveRule
import dev.zahaand.projectscan.model.CodeStyleInfo
import dev.zahaand.projectscan.model.LinterInfo
import dev.zahaand.projectscan.model.RuleSeverity
import dev.zahaand.projectscan.model.ScanResult
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StackInfo
import dev.zahaand.projectscan.model.StructureInfo
import dev.zahaand.projectscan.model.TestInfo
import dev.zahaand.projectscan.shared.detectVersionDiscrepancies
import dev.zahaand.projectscan.shared.deduplicateFrameworks
import dev.zahaand.projectscan.shared.groupDependencies
import dev.zahaand.projectscan.shared.normalizeSourceRoots

class PromptGenerator {
    private fun extractLanguageLevel(languageLevel: String?): Int? {
        if (languageLevel == null || languageLevel.isBlank()) return null
        val digits = languageLevel.trimStart().takeWhile { it.isDigit() }
        return if (digits.isEmpty()) null else digits.toInt()
    }

    fun generate(
        scanResult: ScanResult,
        baselineRules: List<BaselineRule>,
    ): ConstitutionPrompt {
        val filteredBaseline =
            when (val stack = scanResult.stack) {
                is SectionResult.Ok -> {
                    val level = extractLanguageLevel(stack.data.languageLevel)
                    if (level != null) baselineRules.filter { it.minJavaLevel <= level } else baselineRules
                }
                else -> baselineRules
            }
        return ConstitutionPrompt(
            listOf(
                PromptBlock("Core Principles", buildCorePrinciplesBlock(scanResult.linters, filteredBaseline)),
                PromptBlock("Tech Stack", buildTechStackBlock(scanResult.stack)),
                PromptBlock("Code Style & Static Analysis", buildCodeStyleBlock(scanResult.codeStyle)),
                PromptBlock("Testing", buildTestingBlock(scanResult.tests)),
                PromptBlock("Project Structure", buildProjectStructureBlock(scanResult.structure)),
                PromptBlock("Governance", buildGovernanceBlock()),
            ),
        )
    }

    private fun isMandatory(rule: ActiveRule): Boolean = rule.severity == RuleSeverity.ERROR || rule.breaksBuild == true

    private fun renderProjectStandardGroup(group: OriginGroup): String =
        buildString {
            if (group.emptyNotation != null) {
                append(group.emptyNotation)
            } else {
                if (group.mandatoryRules.isNotEmpty()) {
                    append("#### Mandatory (build-breaking)\n")
                    append(group.mandatoryRules.joinToString("\n"))
                    append("\n\n")
                }
                if (group.advisoryRules.isNotEmpty()) {
                    append("#### Advisory\n")
                    append(group.advisoryRules.joinToString("\n"))
                }
            }
        }

    private fun buildCorePrinciplesBlock(
        linters: SectionResult<LinterInfo>,
        baselineRules: List<BaselineRule>,
    ): String {
        val activeRules: List<ActiveRule>
        val projectStandardEmptyNotation: String?

        when (linters) {
            is SectionResult.Ok -> {
                activeRules = linters.data.activeRules
                projectStandardEmptyNotation = if (activeRules.isEmpty()) "not detected" else null
            }
            is SectionResult.Empty -> {
                activeRules = emptyList()
                projectStandardEmptyNotation = "not detected"
            }
            is SectionResult.Error -> {
                activeRules = emptyList()
                projectStandardEmptyNotation = formatError(linters.cause)
            }
        }

        val mandatoryBullets =
            activeRules.filter { isMandatory(it) }.map { "- ${it.ruleId} [${it.tool}] (${it.severity.name})" }
        val advisoryBullets =
            activeRules.filterNot { isMandatory(it) }.map { "- ${it.ruleId} [${it.tool}] (${it.severity.name})" }
        val projectStandard =
            OriginGroup(
                label = "project standard",
                mandatoryRules = mandatoryBullets,
                advisoryRules = advisoryBullets,
                emptyNotation = projectStandardEmptyNotation,
            )

        val baselineBullets = baselineRules.map { "- ${it.obligation.name} ${it.statement}" }
        val baseline =
            OriginGroup(
                label = "baseline quality requirement",
                mandatoryRules = emptyList(),
                advisoryRules = baselineBullets,
                emptyNotation = if (baselineBullets.isEmpty()) "No baseline rules are available." else null,
            )

        val preamble =
            "When rules conflict: project standard rules take precedence over baseline quality requirements; " +
                "baseline quality requirements take precedence over unwritten team practice."

        return buildString {
            append(preamble)
            append("\n\n")
            append("### ${projectStandard.label}\n\n")
            append(renderProjectStandardGroup(projectStandard))
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
                groupDependencies(info.dependencies).forEach { group ->
                    if (group.sharedVersion != null) {
                        lines.add("- ${group.groupId}:* @ ${group.sharedVersion}")
                        group.artifacts.forEach { dep -> lines.add("  - ${dep.artifactId}") }
                    } else {
                        group.artifacts.forEach { dep ->
                            val version = dep.resolvedVersion?.let { ":$it" } ?: ""
                            lines.add("- ${dep.groupId}:${dep.artifactId}$version")
                        }
                    }
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
                if (sources.isEmpty()) {
                    "not detected"
                } else {
                    sources.joinToString("\n") { "- ${it.type.name}: ${it.path}" }
                }
            }
            is SectionResult.Empty -> "not detected"
            is SectionResult.Error -> formatError(codeStyle.cause)
        }

    private fun buildTestingBlock(tests: SectionResult<TestInfo>): String =
        when (tests) {
            is SectionResult.Ok -> {
                val info = tests.data
                val lines = mutableListOf<String>()
                deduplicateFrameworks(info.frameworks).forEach { fw ->
                    val version = fw.version?.let { " $it" } ?: ""
                    lines.add("- Framework: ${fw.name}$version")
                }
                normalizeSourceRoots(info.sourceRoots).forEach { t ->
                    val suffix = if (t.count > 1) " — ${t.count} modules" else ""
                    lines.add("- Source Roots: ${t.relativePath}$suffix")
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

    private fun buildProjectStructureBlock(structure: SectionResult<StructureInfo>): String {
        return when (structure) {
            is SectionResult.Ok -> {
                val info = structure.data
                val lines = mutableListOf<String>()
                info.modules.forEach { module ->
                    lines.add("- Module: ${module.name}")
                    if (module.moduleDependencies.isNotEmpty()) {
                        lines.add("  - ${module.name} → [${module.moduleDependencies.joinToString(", ")}]")
                    }
                }
                if (info.packageSegments.isNotEmpty()) {
                    lines.add("- Package segments: ${info.packageSegments.joinToString(", ")}")
                }
                if (info.rootPackages.isNotEmpty()) {
                    lines.add("- Root Packages: ${info.rootPackages.joinToString(", ")}")
                }
                if (lines.isEmpty()) return "not detected"
                lines.add("- Version discrepancies:")
                val discrepancies = detectVersionDiscrepancies(info.modules)
                if (discrepancies.isEmpty()) {
                    lines.add("  - none")
                } else {
                    discrepancies.forEach { d ->
                        val versions = d.versions.entries.sortedBy { it.key }.joinToString(", ") { "${it.key}: ${it.value}" }
                        lines.add("  - ${d.groupId}:${d.artifactId} → {$versions}")
                    }
                }
                lines.joinToString("\n")
            }
            is SectionResult.Empty -> "not detected"
            is SectionResult.Error -> formatError(structure.cause)
        }
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
