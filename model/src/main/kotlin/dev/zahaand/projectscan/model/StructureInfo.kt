package dev.zahaand.projectscan.model

data class Module(
    val name: String,
    val declaredDependencies: List<Dependency> = emptyList(),
    val moduleDependencies: List<String> = emptyList(),
)

data class StructureInfo(
    val modules: List<Module> = emptyList(),
    val rootPackages: List<String> = emptyList(),
    val packageSegments: List<String> = emptyList(),
)
