package dev.zahaand.projectscan.scan.adapter

import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.scan.port.ModuleDescriptor
import dev.zahaand.projectscan.scan.port.ModuleStructurePort
import org.jetbrains.idea.maven.model.MavenArtifact
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

class IjModuleStructureAdapter(private val project: Project) : ModuleStructurePort {
    companion object {
        private val GRADLE_DENYLIST_EXACT =
            setOf(
                "org.objenesis:objenesis",
                "com.thoughtworks.paranamer:paranamer",
                "com.google.guava:listenablefuture",
                "com.google.guava:failureaccess",
                "com.google.j2objc:j2objc-annotations",
                "org.checkerframework:checker-qual",
                "aopalliance:aopalliance",
            )
        private const val GRADLE_DENYLIST_ASM_GROUP = "org.ow2.asm"
    }

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

        // Build aggregator reverse map: canonicalPath(childDir) → aggregatorArtifactId.
        // Uses getModulesPathsAndNames() which maps child module paths to their declared names;
        // keys may be relative or absolute — File() handles both correctly.
        val aggregatorByDir = mutableMapOf<String, String>()
        for (mp in mavenProjects) {
            val aggregatorName = mp.mavenId.artifactId ?: mp.displayName
            for (childPath in mp.modulesPathsAndNames.keys) {
                aggregatorByDir[File(mp.directory, childPath).canonicalPath] = aggregatorName
            }
        }

        return mavenProjects.map { mp ->
            val name = mp.mavenId.artifactId ?: mp.displayName

            // FR-003 direct-only dep extraction.
            // CONFIRMED PATH (IntelliJ 2025.3.5): primary path mp.mavenModel.dependencies is NOT
            // available — MavenProject has no getMavenModel() in this API version.
            // Working path: root-level nodes of mp.dependencyTree (parent==null nodes = direct deps).
            // BOM-import artifacts (type=pom, scope=import) excluded per spec.
            val externalDeps =
                mp.dependencyTree
                    .map { it.artifact }
                    .filter { !(it.type == "pom" && it.scope == "import") }
                    .filter { "${it.groupId}:${it.artifactId}" !in moduleCoordinates }
                    .map { it.toDependency() }

            val allDeps = mp.dependencies
            val moduleDeps =
                allDeps
                    .filter { "${it.groupId}:${it.artifactId}" in moduleCoordinates }
                    .map { it.artifactId }
                    .distinct()

            val aggregator = aggregatorByDir[File(mp.directory).canonicalPath]

            val ijModule = ModuleManager.getInstance(project).findModuleByName(name)
            val sourceRoots =
                ijModule?.let {
                    ModuleRootManager.getInstance(it).getSourceRoots(JavaSourceRootType.SOURCE).map { root ->
                        root.path
                    }
                } ?: emptyList()

            ModuleDescriptor(
                name = name,
                externalDependencies = externalDeps,
                moduleDependencies = moduleDeps,
                sourceRootPaths = sourceRoots,
                hasSourceRoots = sourceRoots.isNotEmpty(),
                aggregator = aggregator,
            )
        }
    }

    private fun isDenylisted(dep: Dependency): Boolean =
        dep.groupId == GRADLE_DENYLIST_ASM_GROUP ||
            "${dep.groupId}:${dep.artifactId}" in GRADLE_DENYLIST_EXACT

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
                        .filter { !isDenylisted(it) }

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
