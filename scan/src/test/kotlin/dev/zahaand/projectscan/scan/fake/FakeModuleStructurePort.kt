package dev.zahaand.projectscan.scan.fake

import dev.zahaand.projectscan.scan.port.ModuleDescriptor
import dev.zahaand.projectscan.scan.port.ModuleStructurePort

class FakeModuleStructurePort(
    private val modules: List<ModuleDescriptor>,
) : ModuleStructurePort {
    override fun getModules(): List<ModuleDescriptor> = modules
}
