package dev.zahaand.projectscan.scan.fake

import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.scan.port.DependencyPort

class FakeDependencyPort(
    private val moduleMap: Map<String, List<Dependency>> = emptyMap(),
) : DependencyPort {
    override fun getModuleDependencies(): Map<String, List<Dependency>> = moduleMap
}
