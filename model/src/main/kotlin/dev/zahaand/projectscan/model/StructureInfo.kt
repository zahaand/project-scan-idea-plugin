package dev.zahaand.projectscan.model

data class Module(
    val name: String,
    val declaredDependencies: List<Dependency> = emptyList(),
    val moduleDependencies: List<String> = emptyList(),
    val aggregator: String? = null,
)

data class StructureInfo(
    val modules: List<Module> = emptyList(),
)
