package dev.zahaand.projectscan.scan.collector

import dev.zahaand.projectscan.model.Module
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StructureInfo
import dev.zahaand.projectscan.scan.port.ModuleStructurePort

class StructureCollector(private val port: ModuleStructurePort) {

    fun collect(): SectionResult<StructureInfo> {
        val descriptors = port.getModules()
        if (descriptors.isEmpty()) return SectionResult.Empty

        val modules = descriptors.map { descriptor ->
            Module(
                name = descriptor.name,
                declaredDependencies = descriptor.externalDependencies,
                moduleDependencies = descriptor.moduleDependencies.distinct(),
            )
        }

        val (rootPackages, packageSegments) = try {
            val tree = port.getPackageTree()
            Pair(tree.rootPackages, tree.secondLevelSegments)
        } catch (e: Exception) {
            Pair(emptyList<String>(), emptyList<String>())
        }

        return SectionResult.Ok(
            StructureInfo(
                modules = modules,
                rootPackages = rootPackages,
                packageSegments = packageSegments,
            )
        )
    }
}
