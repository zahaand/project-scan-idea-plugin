package dev.zahaand.projectscan.model

data class TestFramework(
    val name: String,
    val version: String?,
)

data class TestInfo(
    val frameworks: List<TestFramework> = emptyList(),
    val unknownTestDependencies: List<Dependency> = emptyList(),
    val sourceRoots: List<String> = emptyList(),
    val namingSuffixes: List<String> = emptyList(),
    val coverageThreshold: Double? = null,
)
