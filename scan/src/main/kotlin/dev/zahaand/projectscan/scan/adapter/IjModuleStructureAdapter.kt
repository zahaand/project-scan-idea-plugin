package dev.zahaand.projectscan.scan.adapter

import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.scan.port.ModuleDescriptor
import dev.zahaand.projectscan.scan.port.ModuleStructurePort
import dev.zahaand.projectscan.scan.port.PackageTreeData
import org.jetbrains.idea.maven.model.MavenArtifact
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.plugins.gradle.util.GradleConstants

class IjModuleStructureAdapter(private val project: Project) : ModuleStructurePort {
    override fun getModules(): List<ModuleDescriptor> {
        val mavenManager = MavenProjectsManager.getInstance(project)
        if (mavenManager.isMavenizedProject) {
            return mavenModules(mavenManager)
        }
        return gradleModules()
    }

    private fun mavenModules(mavenManager: MavenProjectsManager): List<ModuleDescriptor> {
        val mavenProjects = mavenManager.projects
        val moduleCoordinates =
            mavenProjects
                .map { "${it.mavenId.groupId}:${it.mavenId.artifactId}" }
                .toSet()

        return mavenProjects.map { mp ->
            val name = mp.mavenId.artifactId ?: mp.displayName
            val allDeps = mp.dependencies
            val externalDeps =
                allDeps
                    .filter { "${it.groupId}:${it.artifactId}" !in moduleCoordinates }
                    .map { it.toDependency() }
            val moduleDeps =
                allDeps
                    .filter { "${it.groupId}:${it.artifactId}" in moduleCoordinates }
                    .map { it.artifactId }
                    .distinct()
            val ijModule = ModuleManager.getInstance(project).findModuleByName(name)
            val sourceRoots =
                ijModule?.let {
                    ModuleRootManager.getInstance(it).getSourceRoots(JavaSourceRootType.SOURCE).map {
                            root ->
                        root.path
                    }
                } ?: emptyList()
            ModuleDescriptor(
                name = name,
                externalDependencies = externalDeps,
                moduleDependencies = moduleDeps,
                sourceRootPaths = sourceRoots,
                hasSourceRoots = sourceRoots.isNotEmpty(),
            )
        }
    }

    private fun gradleModules(): List<ModuleDescriptor> {
        val result = mutableListOf<ModuleDescriptor>()
        val dataManager = ProjectDataManager.getInstance()
        for (projectData in dataManager.getExternalProjectsData(project, GradleConstants.SYSTEM_ID)) {
            val projectNode = projectData.externalProjectStructure ?: continue
            for (moduleNode in ExternalSystemApiUtil.findAll(projectNode, ProjectKeys.MODULE)) {
                val externalName = moduleNode.data.externalName

                val externalDeps =
                    ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.LIBRARY_DEPENDENCY)
                        .mapNotNull { toDependency(it.data.target) }

                val moduleDeps =
                    ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.MODULE_DEPENDENCY)
                        .map { it.data.target.externalName }
                        .distinct()

                val sourceRoots =
                    ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.CONTENT_ROOT)
                        .flatMap { contentRoot ->
                            contentRoot.data.getPaths(ExternalSystemSourceType.SOURCE).map { it.path }
                        }

                result +=
                    ModuleDescriptor(
                        name = externalName,
                        externalDependencies = externalDeps,
                        moduleDependencies = moduleDeps,
                        sourceRootPaths = sourceRoots,
                        hasSourceRoots = sourceRoots.isNotEmpty(),
                    )
            }
        }
        return result
    }

    override fun getPackageTree(): PackageTreeData {
        val rootPackages = mutableSetOf<String>()
        val secondLevelSegments = mutableSetOf<String>()

        for (module in ModuleManager.getInstance(project).modules) {
            for (root in ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.SOURCE)) {
                collectPackageTree(root, rootPackages, secondLevelSegments)
            }
        }

        return PackageTreeData(
            rootPackages = rootPackages.sorted(),
            secondLevelSegments = secondLevelSegments.sorted(),
        )
    }

    private fun collectPackageTree(
        root: VirtualFile,
        rootPackages: MutableSet<String>,
        secondLevelSegments: MutableSet<String>,
    ) {
        for (child in root.children) {
            if (!child.isDirectory || !isValidJavaIdentifier(child.name)) continue
            rootPackages += child.name
            for (grandchild in child.children) {
                if (!grandchild.isDirectory || !isValidJavaIdentifier(grandchild.name)) continue
                secondLevelSegments += "${child.name}.${grandchild.name}"
            }
        }
    }

    private fun isValidJavaIdentifier(name: String): Boolean {
        if (name.isEmpty() || name.startsWith(".") || !name[0].isJavaIdentifierStart()) return false
        return name.drop(1).all { it.isJavaIdentifierPart() }
    }

    private fun toDependency(lib: LibraryData): Dependency? {
        val name = lib.externalName.removePrefix("Gradle: ")
        val parts = name.split(":")
        if (parts.size < 2) return null
        val groupId = parts[0].takeIf(String::isNotBlank)
        val artifactId = parts[1].takeIf(String::isNotBlank)
        return if (groupId != null && artifactId != null) {
            Dependency(groupId, artifactId, parts.getOrNull(2)?.takeIf(String::isNotBlank))
        } else {
            null
        }
    }

    private fun MavenArtifact.toDependency(): Dependency =
        Dependency(groupId, artifactId, version.takeIf(String::isNotBlank))
}
