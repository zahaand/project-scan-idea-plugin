package dev.zahaand.projectscan.scan.adapter

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.project.Project
import dev.zahaand.projectscan.scan.port.LinterPort
import dev.zahaand.projectscan.scan.port.LinterToolDescriptor
import org.jdom.Element
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

class IjLinterAdapter(private val project: Project) : LinterPort {
    override fun getAppliedLinterTools(): List<LinterToolDescriptor> {
        val mavenManager = MavenProjectsManager.getInstance(project)
        return if (mavenManager.isMavenizedProject) {
            mavenDescriptors(mavenManager)
        } else {
            gradleDescriptors()
        }
    }

    private fun mavenDescriptors(mavenManager: MavenProjectsManager): List<LinterToolDescriptor> {
        val basePath = project.basePath ?: return emptyList()
        val seen = linkedSetOf<LinterToolDescriptor>()
        for (mavenProject in mavenManager.projects) {
            // Checkstyle — one descriptor per distinct execution config path (FR-008 / CHK015)
            mavenProject.findPlugin("org.apache.maven.plugins", "maven-checkstyle-plugin")?.let { plugin ->
                seen +=
                    descriptorsFromPlugin(
                        basePath, "checkstyle", "configLocation",
                        plugin.configurationElement,
                        plugin.executions.map { it.configurationElement },
                    )
            }
            // PMD — one descriptor per distinct execution config path (FR-008 / CHK015)
            mavenProject.findPlugin("org.apache.maven.plugins", "maven-pmd-plugin")?.let { plugin ->
                seen +=
                    descriptorsFromPlugin(
                        basePath, "pmd", "ruleset",
                        plugin.configurationElement,
                        plugin.executions.map { it.configurationElement },
                    )
            }
        }
        return seen.toList()
    }

    // Emits one LinterToolDescriptor per distinct config path across plugin-level and per-execution
    // configs. If a plugin has no executions the plugin-level config produces a single descriptor.
    private fun descriptorsFromPlugin(
        basePath: String,
        toolName: String,
        configKey: String,
        pluginConfig: Element?,
        executionConfigs: List<Element?>,
    ): List<LinterToolDescriptor> {
        val pluginBreaksBuild =
            pluginConfig?.getChildText("failOnViolation")
                ?.let { value -> value.equals("true", ignoreCase = true) }
        val pluginConfigValue = pluginConfig?.getChildText(configKey)
        if (executionConfigs.isEmpty()) {
            return listOf(
                LinterToolDescriptor(
                    toolName,
                    pluginConfigValue?.let { resolveLocalPath(basePath, it) },
                    pluginBreaksBuild,
                ),
            )
        }
        val visitedPaths = linkedSetOf<String?>()
        return executionConfigs.mapNotNull { execConfig ->
            val breaksBuild =
                execConfig?.getChildText("failOnViolation")
                    ?.let { value -> value.equals("true", ignoreCase = true) } ?: pluginBreaksBuild
            val configPath =
                (execConfig?.getChildText(configKey) ?: pluginConfigValue)
                    ?.let { resolveLocalPath(basePath, it) }
            if (visitedPaths.add(configPath)) LinterToolDescriptor(toolName, configPath, breaksBuild) else null
        }
    }

    private fun gradleDescriptors(): List<LinterToolDescriptor> {
        val taskNames = collectGradleTaskNames()
        val basePath = project.basePath ?: return emptyList()
        val descriptors = mutableListOf<LinterToolDescriptor>()

        if (taskNames.any { it == "checkstyleMain" || it == "checkstyleTest" }) {
            descriptors +=
                LinterToolDescriptor(
                    "checkstyle",
                    resolveFirstExisting(basePath, "config/checkstyle/checkstyle.xml"),
                    null,
                )
        }

        if (taskNames.any { it == "pmdMain" || it == "pmdTest" }) {
            // Only match config/pmd/ paths to avoid false positives from generic ruleset.xml names
            descriptors +=
                LinterToolDescriptor(
                    "pmd",
                    resolveFirstExisting(basePath, "config/pmd/ruleset.xml", "config/pmd/pmd-ruleset.xml"),
                    null,
                )
        }

        return descriptors
    }

    private fun collectGradleTaskNames(): Set<String> {
        val names = mutableSetOf<String>()
        val dataManager = ProjectDataManager.getInstance()
        for (projectData in dataManager.getExternalProjectsData(project, GradleConstants.SYSTEM_ID)) {
            val projectNode = projectData.externalProjectStructure ?: continue
            collectTaskNamesRecursively(projectNode, names)
        }
        return names
    }

    private fun collectTaskNamesRecursively(
        node: DataNode<*>,
        names: MutableSet<String>,
    ) {
        for (child in node.children) {
            @Suppress("UNCHECKED_CAST")
            if (child.key == ProjectKeys.TASK) {
                names += (child.data as TaskData).name
            }
            collectTaskNamesRecursively(child, names)
        }
    }

    private fun resolveLocalPath(
        basePath: String,
        value: String,
    ): String? {
        if (value.startsWith("classpath") || value.contains("://")) return null
        val file = if (value.startsWith("/")) File(value) else File(basePath, value)
        return if (file.exists()) file.absolutePath else null
    }

    private fun resolveFirstExisting(
        basePath: String,
        vararg relativePaths: String,
    ): String? =
        relativePaths.firstNotNullOfOrNull { rel ->
            val file = File(basePath, rel)
            if (file.exists()) file.absolutePath else null
        }
}
