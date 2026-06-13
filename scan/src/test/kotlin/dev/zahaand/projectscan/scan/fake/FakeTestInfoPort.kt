package dev.zahaand.projectscan.scan.fake

import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.scan.port.TestInfoPort

class FakeTestInfoPort(
    private val testSourceRoots: List<String> = emptyList(),
    private val testScopedDependencies: List<Dependency> = emptyList(),
    private val coverageThreshold: Double? = null,
    private val testClassNames: List<String> = emptyList(),
) : TestInfoPort {
    override fun getTestSourceRoots(): List<String> = testSourceRoots
    override fun getTestScopedDependencies(): List<Dependency> = testScopedDependencies
    override fun getCoverageThreshold(): Double? = coverageThreshold
    override fun getTestClassNames(): List<String> = testClassNames
}
