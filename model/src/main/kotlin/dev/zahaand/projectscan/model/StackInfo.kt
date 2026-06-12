package dev.zahaand.projectscan.model

data class Dependency(
    val groupId: String,
    val artifactId: String,
    val resolvedVersion: String?,
)

enum class BuildSystem { MAVEN, GRADLE }

data class StackInfo(
    val dependencies: List<Dependency> = emptyList(),
    val jdkVersion: String? = null,
    val languageLevel: String? = null,
    val buildSystem: BuildSystem? = null,
)
