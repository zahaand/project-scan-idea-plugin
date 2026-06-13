package dev.zahaand.projectscan.scan.port

import dev.zahaand.projectscan.model.Dependency

interface TestInfoPort {
    fun getTestSourceRoots(): List<String>
    fun getTestScopedDependencies(): List<Dependency>
}
