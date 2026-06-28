package dev.zahaand.projectscan.scan.adapter

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import dev.zahaand.projectscan.scan.port.TestInfoPort
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.jps.model.java.JavaSourceRootType

class IjTestInfoAdapter(private val project: Project) : TestInfoPort {
    override fun getTestSourceRoots(): List<String> {
        val basePath = project.basePath ?: return emptyList()
        val roots = mutableListOf<String>()
        for (module in ModuleManager.getInstance(project).modules) {
            for (root in ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.TEST_SOURCE)) {
                roots += root.path.removePrefix(basePath).trimStart('/')
            }
        }
        return roots.distinct()
    }

    override fun getTestClassNames(): List<String> {
        val names = mutableListOf<String>()
        for (module in ModuleManager.getInstance(project).modules) {
            for (root in ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.TEST_SOURCE)) {
                collectClassNames(root, names)
            }
        }
        return names
    }

    private fun collectClassNames(
        dir: VirtualFile,
        names: MutableList<String>,
    ) {
        for (child in dir.children) {
            if (child.isDirectory) {
                collectClassNames(child, names)
            } else if (child.extension == "java" || child.extension == "kt") {
                names += child.nameWithoutExtension
            }
        }
    }

    override fun getCoverageThreshold(): Double? {
        val mavenManager = MavenProjectsManager.getInstance(project)
        if (!mavenManager.isMavenizedProject) return null
        return mavenManager.projects.firstNotNullOfOrNull { extractJacocoThreshold(it) }
    }

    private fun extractJacocoThreshold(mavenProject: MavenProject): Double? {
        val plugin = mavenProject.findPlugin("org.jacoco", "jacoco-maven-plugin") ?: return null
        return plugin.executions
            .filter { "check" in it.goals }
            .firstNotNullOfOrNull { execution ->
                execution.configurationElement
                    ?.getChild("rules")
                    ?.getChild("rule")
                    ?.getChild("limits")
                    ?.getChild("limit")
                    ?.getChildText("minimum")
                    ?.toDoubleOrNull()
            }
    }
}
