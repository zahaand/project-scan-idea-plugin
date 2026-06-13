package dev.zahaand.projectscan.scan.port

import dev.zahaand.projectscan.model.Dependency

data class PackageTreeData(
    val rootPackages: List<String>,
    val secondLevelSegments: List<String>,
)

data class ModuleDescriptor(
    val name: String,
    val externalDependencies: List<Dependency>,
    val moduleDependencies: List<String>,
    val sourceRootPaths: List<String>,
    val hasSourceRoots: Boolean,
)

interface ModuleStructurePort {
    fun getModules(): List<ModuleDescriptor>
    fun getPackageTree(): PackageTreeData
}
