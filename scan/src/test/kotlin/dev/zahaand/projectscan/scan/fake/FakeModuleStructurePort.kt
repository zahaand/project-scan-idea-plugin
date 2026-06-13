package dev.zahaand.projectscan.scan.fake

import dev.zahaand.projectscan.scan.port.ModuleDescriptor
import dev.zahaand.projectscan.scan.port.ModuleStructurePort
import dev.zahaand.projectscan.scan.port.PackageTreeData

class FakeModuleStructurePort(
    private val modules: List<ModuleDescriptor>,
    private val packageTree: PackageTreeData,
) : ModuleStructurePort {
    override fun getModules(): List<ModuleDescriptor> = modules
    override fun getPackageTree(): PackageTreeData = packageTree
}
