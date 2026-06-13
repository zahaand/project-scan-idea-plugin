package dev.zahaand.projectscan.scan.adapter

import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.scan.port.DependencyPort
import org.jetbrains.idea.maven.model.MavenArtifact
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.plugins.gradle.util.GradleConstants

class IjDependencyAdapter(private val project: Project) : DependencyPort {

    override fun getModuleDependencies(): Map<String, List<Dependency>> {
        val mavenManager = MavenProjectsManager.getInstance(project)
        if (mavenManager.isMavenizedProject) {
            return mavenManager.projects.associate { mavenProject ->
                val name = mavenProject.mavenId.artifactId ?: mavenProject.displayName
                name to mavenProject.dependencies.map { it.toDependency() }
            }
        }
        return gradleDependencies()
    }

    private fun gradleDependencies(): Map<String, List<Dependency>> {
        val result = mutableMapOf<String, List<Dependency>>()
        val dataManager = ProjectDataManager.getInstance()
        for (projectData in dataManager.getExternalProjectsData(project, GradleConstants.SYSTEM_ID)) {
            val projectNode = projectData.externalProjectStructure ?: continue
            for (moduleNode in ExternalSystemApiUtil.findAll(projectNode, ProjectKeys.MODULE)) {
                val moduleName = moduleNode.data.externalName
                val deps = ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.LIBRARY_DEPENDENCY)
                    .mapNotNull { node -> toDependency(node.data.target) }
                result[moduleName] = deps
            }
        }
        return result
    }

    private fun toDependency(lib: LibraryData): Dependency? {
        // External name format: "groupId:artifactId:version" or "Gradle: groupId:artifactId:version"
        val name = lib.externalName.removePrefix("Gradle: ")
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
