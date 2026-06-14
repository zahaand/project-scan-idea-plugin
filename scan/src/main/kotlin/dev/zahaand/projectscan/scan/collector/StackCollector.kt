package dev.zahaand.projectscan.scan.collector

import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StackInfo
import dev.zahaand.projectscan.scan.port.BuildSystemPort
import dev.zahaand.projectscan.scan.port.DependencyPort
import org.apache.maven.artifact.versioning.ComparableVersion

class StackCollector(
    private val buildSystemPort: BuildSystemPort,
    private val dependencyPort: DependencyPort,
) {
    fun collect(): SectionResult<StackInfo> {
        val buildSystem = buildSystemPort.getBuildSystem()
        val moduleMap =
            try {
                dependencyPort.getModuleDependencies()
            } catch (_: Exception) {
                emptyMap()
            }

        if (buildSystem == null && moduleMap.isEmpty()) return SectionResult.Empty

        return SectionResult.Ok(
            StackInfo(
                dependencies = aggregate(moduleMap),
                jdkVersion = buildSystemPort.getJdkVersion(),
                languageLevel = maxLanguageLevel(buildSystemPort.getModuleLanguageLevels().values),
                buildSystem = buildSystem,
            ),
        )
    }

    private fun aggregate(moduleMap: Map<String, List<Dependency>>): List<Dependency> {
        val byCoordinate = mutableMapOf<String, Dependency>()
        for (dep in moduleMap.values.flatten()) {
            val key = "${dep.groupId}:${dep.artifactId}"
            val existing = byCoordinate[key]
            byCoordinate[key] = if (existing == null) dep else pickMaxVersion(existing, dep)
        }
        return byCoordinate.values.toList()
    }

    private fun pickMaxVersion(
        a: Dependency,
        b: Dependency,
    ): Dependency {
        val vA = a.resolvedVersion
        val vB = b.resolvedVersion
        return when {
            vA == null -> b
            vB == null -> a
            else -> if (compareVersions(vA, vB) >= 0) a else b
        }
    }

    private fun compareVersions(
        a: String,
        b: String,
    ): Int =
        try {
            ComparableVersion(a).compareTo(ComparableVersion(b))
        } catch (_: Exception) {
            a.compareTo(b)
        }

    private fun maxLanguageLevel(levels: Collection<String>): String? =
        levels.maxWithOrNull { a, b ->
            val ia = a.toIntOrNull()
            val ib = b.toIntOrNull()
            if (ia != null && ib != null) ia.compareTo(ib) else a.compareTo(b)
        }
}
