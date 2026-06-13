package dev.zahaand.projectscan.scan.adapter

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import dev.zahaand.projectscan.scan.port.LinterPort
import dev.zahaand.projectscan.scan.port.LinterToolDescriptor
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
            // Checkstyle
            mavenProject.findPlugin("com.puppycrawl.tools", "maven-checkstyle-plugin")?.let { plugin ->
                val configElement = plugin.configurationElement
                val breaksBuild = configElement?.getChildText("failsOnError")
                    ?.let { it.equals("true", ignoreCase = true) }
                val configLocation = configElement?.getChildText("configLocation")
                val configFilePath = configLocation?.let { resolveLocalPath(basePath, it) }
                seen += LinterToolDescriptor("checkstyle", configFilePath, breaksBuild)
            }
            // PMD
            mavenProject.findPlugin("org.apache.maven.plugins", "maven-pmd-plugin")?.let { plugin ->
                val configElement = plugin.configurationElement
                val breaksBuild = configElement?.getChildText("failOnViolation")
                    ?.let { it.equals("true", ignoreCase = true) }
                val ruleset = configElement?.getChildText("ruleset")
                val configFilePath = ruleset?.let { resolveLocalPath(basePath, it) }
                seen += LinterToolDescriptor("pmd", configFilePath, breaksBuild)
            }
        }
        return seen.toList()
    }

    private fun gradleDescriptors(): List<LinterToolDescriptor> {
        val taskNames = collectGradleTaskNames()
        val basePath = project.basePath ?: return emptyList()
        val descriptors = mutableListOf<LinterToolDescriptor>()

        if (taskNames.any { it == "checkstyleMain" || it == "checkstyleTest" }) {
            val configFilePath = resolveFirstExisting(
                basePath,
                "config/checkstyle/checkstyle.xml",
            )
            descriptors += LinterToolDescriptor("checkstyle", configFilePath, null)
        }

        if (taskNames.any { it == "pmdMain" || it == "pmdTest" }) {
            val configFilePath = resolveFirstExisting(
                basePath,
                "config/pmd/ruleset.xml",
                "config/pmd/pmd-ruleset.xml",
                "pmd.xml",
                "ruleset.xml",
            )
            descriptors += LinterToolDescriptor("pmd", configFilePath, null)
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

    private fun collectTaskNamesRecursively(node: DataNode<*>, names: MutableSet<String>) {
        for (child in node.children) {
            @Suppress("UNCHECKED_CAST")
            if (child.key == ProjectKeys.TASK) {
                names += (child.data as TaskData).name
            }
            collectTaskNamesRecursively(child, names)
        }
    }

    private fun resolveLocalPath(basePath: String, value: String): String? {
        if (value.startsWith("classpath") || value.contains("://")) return null
        val file = if (value.startsWith("/")) File(value) else File(basePath, value)
        return if (file.exists()) file.absolutePath else null
    }

    private fun resolveFirstExisting(basePath: String, vararg relativePaths: String): String? =
        relativePaths.firstNotNullOfOrNull { rel ->
            val file = File(basePath, rel)
            if (file.exists()) file.absolutePath else null
        }
}
