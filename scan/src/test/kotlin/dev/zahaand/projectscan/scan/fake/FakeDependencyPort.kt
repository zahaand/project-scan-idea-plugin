package dev.zahaand.projectscan.scan.fake

import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.scan.port.DependencyPort

class FakeDependencyPort(
    private val moduleMap: Map<String, List<Dependency>> = emptyMap(),
    private val error: Throwable? = null,
) : DependencyPort {
    override fun getModuleDependencies(): Map<String, List<Dependency>> {
        if (error != null) throw error
        return moduleMap
    }
}
