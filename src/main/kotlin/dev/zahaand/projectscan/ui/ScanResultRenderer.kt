package dev.zahaand.projectscan.ui

import dev.zahaand.projectscan.model.CodeStyleInfo
import dev.zahaand.projectscan.model.LinterInfo
import dev.zahaand.projectscan.model.Module
import dev.zahaand.projectscan.model.ScanResult
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StackInfo
import dev.zahaand.projectscan.model.TestInfo
import dev.zahaand.projectscan.prompt.ConstitutionPrompt
import dev.zahaand.projectscan.shared.buildInvertedTechStack
import dev.zahaand.projectscan.shared.normalizeSourceRoots
import dev.zahaand.projectscan.shared.renderInvertedTechStack

object ScanResultRenderer {
    fun render(
        scanResult: ScanResult,
        constitutionPrompt: ConstitutionPrompt,
    ): List<UiSection> {
        val modules = (scanResult.structure as? SectionResult.Ok)?.data?.modules ?: emptyList()
        val internalModuleNames = modules.map { it.name }.toSet()
        return listOf(
            section(
                titleKey = "section.TechStack.title",
                result = scanResult.stack,
                render = { stackInfo -> renderStack(stackInfo, modules, internalModuleNames) },
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
            UiSection(
                title = ProjectScanBundle.message("section.Constitution.title"),
                body = constitutionPrompt.render(),
                copyEnabled = true,
                collapsedByDefault = true,
            ),
        )
    }

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

    internal fun renderStack(
        info: StackInfo,
        modules: List<Module>,
        internalModuleNames: Set<String> = emptySet(),
    ): String? {
        val invertedStack = buildInvertedTechStack(modules, internalModuleNames)
        val rendered = renderInvertedTechStack(invertedStack, info.buildSystem, info.jdkVersion, info.languageLevel)
        return rendered.ifBlank { null }
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
        normalizeSourceRoots(info.sourceRoots).forEach { t ->
            lines += "Source root: ${t.relativePath}"
        }
        info.namingSuffixes.forEach { lines += "Naming suffix: $it" }
        info.coverageThreshold?.let { lines += "Coverage threshold: $it" }
        return lines.joinToString("\n").ifBlank { null }
    }
}
