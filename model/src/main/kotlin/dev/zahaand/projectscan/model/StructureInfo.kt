package dev.zahaand.projectscan.model

enum class PackageOrganisation { BY_LAYER, BY_FEATURE }

data class Module(
    val name: String,
    val declaredDependencies: List<Dependency> = emptyList(),
    val moduleDependencies: List<String> = emptyList(),
)

data class StructureInfo(
    val modules: List<Module> = emptyList(),
    val packageOrganisation: PackageOrganisation? = null,
    val rootPackages: List<String> = emptyList(),
)
