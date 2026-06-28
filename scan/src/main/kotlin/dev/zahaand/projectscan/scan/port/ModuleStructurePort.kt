package dev.zahaand.projectscan.scan.port

import dev.zahaand.projectscan.model.Dependency

data class ModuleDescriptor(
    val name: String,
    val externalDependencies: List<Dependency>,
    val moduleDependencies: List<String>,
    val sourceRootPaths: List<String>,
    val hasSourceRoots: Boolean,
    val aggregator: String? = null,
)

interface ModuleStructurePort {
    fun getModules(): List<ModuleDescriptor>
}
