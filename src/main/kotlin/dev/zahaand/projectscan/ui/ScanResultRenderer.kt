package dev.zahaand.projectscan.ui

import dev.zahaand.projectscan.model.CodeStyleInfo
import dev.zahaand.projectscan.model.LinterInfo
import dev.zahaand.projectscan.model.ScanResult
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StackInfo
import dev.zahaand.projectscan.model.StructureInfo
import dev.zahaand.projectscan.model.TestInfo
import dev.zahaand.projectscan.prompt.ConstitutionPrompt
import dev.zahaand.projectscan.shared.detectVersionDiscrepancies
import dev.zahaand.projectscan.shared.deduplicateFrameworks
import dev.zahaand.projectscan.shared.groupDependencies
import dev.zahaand.projectscan.shared.normalizeSourceRoots

object ScanResultRenderer {
    fun render(
        scanResult: ScanResult,
        constitutionPrompt: ConstitutionPrompt,
    ): List<UiSection> =
        listOf(
            section(
                titleKey = "section.TechStack.title",
                result = scanResult.stack,
                render = ::renderStack,
            ),
            section(
                titleKey = "section.CodeStyle.title",
                result = scanResult.codeStyle,
                render = ::renderCodeStyle,
            ),
            section(
                titleKey = "section.Linters.title",
                result = scanResult.linters,
                render = ::renderLinters,
            ),
            section(
                titleKey = "section.Tests.title",
                result = scanResult.tests,
                render = ::renderTests,
            ),
            section(
                titleKey = "section.Structure.title",
                result = scanResult.structure,
                render = ::renderStructure,
            ),
            UiSection(
                title = ProjectScanBundle.message("section.Constitution.title"),
                body = constitutionPrompt.render(),
                copyEnabled = true,
                collapsedByDefault = true,
            ),
        )

    private fun <T> section(
        titleKey: String,
        result: SectionResult<T>,
        render: (T) -> String?,
    ): UiSection {
        val title = ProjectScanBundle.message(titleKey)
        return when (result) {
            is SectionResult.Ok -> {
                val body =
                    render(result.data)
                        ?: ProjectScanBundle.message("section.state.empty")
                UiSection(title = title, body = body, copyEnabled = true, collapsedByDefault = false)
            }
            is SectionResult.Empty ->
                UiSection(
                    title = title,
                    body = ProjectScanBundle.message("section.state.empty"),
                    copyEnabled = false,
                    collapsedByDefault = false,
                )
            is SectionResult.Error -> {
                val cause = result.cause
                val body =
                    if (cause != null) {
                        ProjectScanBundle.message("section.state.error.with.cause", cause)
                    } else {
                        ProjectScanBundle.message("section.state.error")
                    }
                UiSection(title = title, body = body, copyEnabled = false, collapsedByDefault = false)
            }
        }
    }

    internal fun renderStack(info: StackInfo): String? {
        val lines = mutableListOf<String>()
        info.buildSystem?.let { lines += "Build system: $it" }
        info.jdkVersion?.let { lines += "JDK version: $it" }
        info.languageLevel?.let { lines += "Language level: $it" }
        groupDependencies(info.dependencies).forEach { group ->
            if (group.sharedVersion != null) {
                lines += "${group.groupId}:* @ ${group.sharedVersion}"
                group.artifacts.forEach { dep -> lines += "  ${dep.artifactId}" }
            } else {
                group.artifacts.forEach { dep ->
                    val ver = dep.resolvedVersion?.let { ":$it" } ?: ""
                    lines += "${dep.groupId}:${dep.artifactId}$ver"
                }
            }
        }
        return lines.joinToString("\n").ifBlank { null }
    }

    private fun renderCodeStyle(info: CodeStyleInfo): String? {
        if (info.sources.isEmpty()) return null
        return info.sources.joinToString("\n") { "${it.type}: ${it.path}" }
    }

    private fun renderLinters(info: LinterInfo): String? {
        if (info.activeRules.isEmpty()) return null
        return info.activeRules.joinToString("\n") { "${it.ruleId} [${it.tool}] (${it.severity})" }
    }

    internal fun renderTests(info: TestInfo): String? {
        val lines = mutableListOf<String>()
        deduplicateFrameworks(info.frameworks).forEach { fw ->
            val ver = fw.version?.let { " $it" } ?: ""
            lines += "Framework: ${fw.name}$ver"
        }
        normalizeSourceRoots(info.sourceRoots).forEach { t ->
            val suffix = if (t.count > 1) " — ${t.count} modules" else ""
            lines += "Source root: ${t.relativePath}$suffix"
        }
        info.namingSuffixes.forEach { lines += "Naming suffix: $it" }
        info.coverageThreshold?.let { lines += "Coverage threshold: $it" }
        return lines.joinToString("\n").ifBlank { null }
    }

    internal fun renderStructure(info: StructureInfo): String? {
        val lines = mutableListOf<String>()
        info.modules.forEach { mod ->
            lines += "Module: ${mod.name}"
            if (mod.moduleDependencies.isNotEmpty()) {
                lines += "  ${mod.name} → [${mod.moduleDependencies.joinToString(", ")}]"
            }
        }
        if (info.packageSegments.isNotEmpty()) {
            lines += "Package segments: ${info.packageSegments.joinToString(", ")}"
        }
        info.rootPackages.forEach { lines += "Root package: $it" }
        if (lines.isEmpty()) return null
        lines += "Version discrepancies:"
        val discrepancies = detectVersionDiscrepancies(info.modules)
        if (discrepancies.isEmpty()) {
            lines += "  none"
        } else {
            discrepancies.forEach { d ->
                val versions = d.versions.entries.sortedBy { it.key }.joinToString(", ") { "${it.key}: ${it.value}" }
                lines += "  ${d.groupId}:${d.artifactId} → {$versions}"
            }
        }
        return lines.joinToString("\n")
    }
}
