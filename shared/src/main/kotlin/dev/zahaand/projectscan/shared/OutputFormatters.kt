package dev.zahaand.projectscan.shared

import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.Module
import dev.zahaand.projectscan.model.TestFramework

data class DependencyGroup(
    val groupId: String,
    val artifacts: List<Dependency>,
    val sharedVersion: String?,
)

data class VersionDiscrepancy(
    val groupId: String,
    val artifactId: String,
    val versions: Map<String, String>,
)

data class SourceRootTemplate(
    val relativePath: String,
)

private val RECOGNIZED_TEST_SUFFIXES = listOf(
    "src/test/java",
    "src/test/kotlin",
    "src/test/groovy",
    "src/test/scala",
    "src/integration-test/java",
    "src/integration-test/kotlin",
    "src/integration-test/groovy",
    "src/integrationTest/java",
    "src/integrationTest/kotlin",
    "src/integrationTest/groovy",
    "src/testFixtures/java",
    "src/testFixtures/kotlin",
    "src/testFixtures/groovy",
)

private val BUILD_OUTPUT_SEGMENTS = setOf("target", "build")

fun filterInternalDependencies(deps: List<Dependency>, internalModuleNames: Set<String>): List<Dependency> =
    if (internalModuleNames.isEmpty()) deps else deps.filter { it.artifactId !in internalModuleNames }

fun groupDependencies(deps: List<Dependency>): List<DependencyGroup> =
    deps
        .groupBy { it.groupId }
        .entries
        .sortedBy { it.key }
        .map { (groupId, artifacts) ->
            val sharedVersion =
                if (artifacts.size > 1 && artifacts.all { it.resolvedVersion != null }) {
                    val versions = artifacts.map { it.resolvedVersion }.toSet()
                    if (versions.size == 1) versions.first() else null
                } else {
                    null
                }
            DependencyGroup(groupId, artifacts, sharedVersion)
        }

fun detectVersionDiscrepancies(modules: List<Module>): List<VersionDiscrepancy> {
    val coordinateVersionsByModule = mutableMapOf<Pair<String, String>, MutableMap<String, String>>()

    for (module in modules) {
        val seenInModule = mutableMapOf<Pair<String, String>, String>()
        for (dep in module.declaredDependencies) {
            val version = dep.resolvedVersion ?: continue
            val coord = dep.groupId to dep.artifactId
            seenInModule[coord] = version
        }
        for ((coord, version) in seenInModule) {
            coordinateVersionsByModule.getOrPut(coord) { mutableMapOf() }[module.name] = version
        }
    }

    return coordinateVersionsByModule.entries
        .filter { (_, moduleVersions) -> moduleVersions.values.toSet().size >= 2 }
        .sortedWith(compareBy({ it.key.first }, { it.key.second }))
        .map { (coord, moduleVersions) ->
            VersionDiscrepancy(
                groupId = coord.first,
                artifactId = coord.second,
                versions = moduleVersions.entries.sortedBy { it.key }.associate { it.key to it.value },
            )
        }
}

fun renderVersionDiscrepancyLine(d: VersionDiscrepancy): String {
    val versionCounts = d.versions.values.groupingBy { it }.eachCount()
    val dominant = versionCounts.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .first().key
    val exceptions = d.versions.entries
        .filter { it.value != dominant }
        .sortedBy { it.key }
        .joinToString(", ") { "${it.key}: ${it.value}" }
    return "${d.groupId}:${d.artifactId} → mostly $dominant, except {$exceptions}"
}

fun deduplicateFrameworks(frameworks: List<TestFramework>): List<TestFramework> =
    frameworks.distinctBy { it.name to it.version }

fun normalizeSourceRoots(roots: List<String>): List<SourceRootTemplate> {
    if (roots.isEmpty()) return emptyList()

    val filtered = roots.filter { root ->
        root.split("/").filter { it.isNotEmpty() }.none { it in BUILD_OUTPUT_SEGMENTS }
    }

    if (filtered.isEmpty()) return emptyList()

    val resultPaths = mutableSetOf<String>()
    val unrecognizedAbsolute = mutableListOf<String>()

    for (root in filtered) {
        val cleanRoot = root.trimEnd('/')
        val suffix = RECOGNIZED_TEST_SUFFIXES.firstOrNull { cleanRoot.endsWith(it) }
        when {
            suffix != null -> resultPaths.add(suffix)
            cleanRoot.startsWith("/") -> unrecognizedAbsolute.add(cleanRoot)
            else -> resultPaths.add(cleanRoot)
        }
    }

    if (unrecognizedAbsolute.isNotEmpty()) {
        val splitPaths = unrecognizedAbsolute.map { it.split("/").filter { s -> s.isNotEmpty() } }
        val minLen = splitPaths.minOf { it.size }
        var lcpLength = 0
        for (i in 0 until minLen) {
            if (splitPaths.all { it[i] == splitPaths[0][i] }) lcpLength++ else break
        }
        for (segs in splitPaths) {
            val afterLcp = segs.drop(lcpLength)
            // first segment after LCP is typically the module name; drop it for the tail
            val tail = if (afterLcp.size > 1) afterLcp.drop(1) else afterLcp
            val path = tail.joinToString("/").ifEmpty { afterLcp.joinToString("/") }
            if (path.isNotEmpty()) resultPaths.add(path)
        }
    }

    return resultPaths.sorted().map { SourceRootTemplate(it) }
}
