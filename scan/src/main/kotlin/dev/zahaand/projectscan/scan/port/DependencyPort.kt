package dev.zahaand.projectscan.scan.port

import dev.zahaand.projectscan.model.Dependency

interface DependencyPort {
    fun getModuleDependencies(): Map<String, List<Dependency>>
}
