package dev.zahaand.projectscan.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import dev.zahaand.projectscan.baseline.BaselineRuleProvider
import dev.zahaand.projectscan.prompt.PromptGenerator
import dev.zahaand.projectscan.scan.ScanService
import dev.zahaand.projectscan.scan.adapter.CheckstyleConfigParser
import dev.zahaand.projectscan.scan.adapter.IjBuildSystemAdapter
import dev.zahaand.projectscan.scan.adapter.IjLinterAdapter
import dev.zahaand.projectscan.scan.adapter.IjModuleStructureAdapter
import dev.zahaand.projectscan.scan.adapter.IjStyleSourceAdapter
import dev.zahaand.projectscan.scan.adapter.IjTestInfoAdapter
import dev.zahaand.projectscan.scan.adapter.PmdConfigParser

class ProjectScanToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        val scanService =
            ScanService(
                buildSystemPort = IjBuildSystemAdapter(project),
                styleSourcePort = IjStyleSourceAdapter(project),
                linterPort = IjLinterAdapter(project),
                linterConfigParsers =
                    mapOf(
                        "checkstyle" to CheckstyleConfigParser(),
                        "pmd" to PmdConfigParser(),
                    ),
                testInfoPort = IjTestInfoAdapter(project),
                moduleStructurePort = IjModuleStructureAdapter(project),
            )
        val promptGenerator = PromptGenerator()
        val baselineRules = BaselineRuleProvider.rules
        val panel = ProjectScanPanel(project, scanService, promptGenerator, baselineRules)
        toolWindow.contentManager.let {
            it.addContent(it.factory.createContent(panel, null, false))
        }
    }
}
