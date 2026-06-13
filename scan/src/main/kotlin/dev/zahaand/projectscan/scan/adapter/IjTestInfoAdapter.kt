package dev.zahaand.projectscan.scan.adapter

import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.scan.port.TestInfoPort
import org.jetbrains.idea.maven.model.MavenArtifact
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.plugins.gradle.util.GradleConstants

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

    private fun collectClassNames(dir: VirtualFile, names: MutableList<String>) {
        for (child in dir.children) {
            if (child.isDirectory) collectClassNames(child, names)
            else if (child.extension == "java" || child.extension == "kt") {
                names += child.nameWithoutExtension
            }
        }
    }

    override fun getTestScopedDependencies(): List<Dependency> {
        val mavenManager = MavenProjectsManager.getInstance(project)
        if (mavenManager.isMavenizedProject) {
            return mavenManager.projects
                .flatMap { it.dependencies.filter { dep -> dep.scope == "test" } }
                .map { it.toDependency() }
                .distinct()
        }
        return gradleTestDependencies()
    }

    private fun gradleTestDependencies(): List<Dependency> {
        val result = mutableListOf<Dependency>()
        val dataManager = ProjectDataManager.getInstance()
        for (projectData in dataManager.getExternalProjectsData(project, GradleConstants.SYSTEM_ID)) {
            val projectNode = projectData.externalProjectStructure ?: continue
            for (moduleNode in ExternalSystemApiUtil.findAll(projectNode, ProjectKeys.MODULE)) {
                for (depNode in ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.LIBRARY_DEPENDENCY)) {
                    if (depNode.data.scope == DependencyScope.TEST) {
                        toDependency(depNode.data.target.externalName)?.let { result += it }
                    }
                }
            }
        }
        return result.distinct()
    }

    override fun getCoverageThreshold(): Double? {
        val mavenManager = MavenProjectsManager.getInstance(project)
        if (!mavenManager.isMavenizedProject) return null
        for (mavenProject in mavenManager.projects) {
            val threshold = extractJacocoThreshold(mavenProject)
            if (threshold != null) return threshold
        }
        return null
    }

    private fun extractJacocoThreshold(mavenProject: MavenProject): Double? {
        val plugin = mavenProject.findPlugin("org.jacoco", "jacoco-maven-plugin") ?: return null
        for (execution in plugin.executions) {
            if ("check" !in execution.goals) continue
            val minimum = execution.configurationElement
                ?.getChild("rules")
                ?.getChild("rule")
                ?.getChild("limits")
                ?.getChild("limit")
                ?.getChildText("minimum")
                ?: continue
            return minimum.toDoubleOrNull()
        }
        return null
    }

    private fun toDependency(externalName: String): Dependency? {
        val name = externalName.removePrefix("Gradle: ")
        val parts = name.split(":")
        if (parts.size < 2) return null
        val groupId = parts[0].takeIf(String::isNotBlank) ?: return null
        val artifactId = parts[1].takeIf(String::isNotBlank) ?: return null
        val version = parts.getOrNull(2)?.takeIf(String::isNotBlank)
        return Dependency(groupId, artifactId, version)
    }

    private fun MavenArtifact.toDependency(): Dependency =
        Dependency(groupId, artifactId, version.takeIf(String::isNotBlank))
}
