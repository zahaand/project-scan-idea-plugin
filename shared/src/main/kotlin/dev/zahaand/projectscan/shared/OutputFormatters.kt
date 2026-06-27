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
    val count: Int,
)

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

fun deduplicateFrameworks(frameworks: List<TestFramework>): List<TestFramework> =
    frameworks.distinctBy { it.name to it.version }

fun normalizeSourceRoots(roots: List<String>): List<SourceRootTemplate> {
    if (roots.isEmpty()) return emptyList()

    val absoluteRoots = roots.filter { it.startsWith("/") }
    val relativeRoots = roots.filter { !it.startsWith("/") }

    val templates = mutableMapOf<String, Int>()

    if (absoluteRoots.size == 1) {
        val template = absoluteRoots[0].trimStart('/')
        templates[template] = (templates[template] ?: 0) + 1
    } else if (absoluteRoots.size > 1) {
        val splitPaths = absoluteRoots.map { it.split("/").filter { s -> s.isNotEmpty() } }

        val minLen = splitPaths.minOf { it.size }
        var lcpLength = 0
        for (i in 0 until minLen) {
            if (splitPaths.all { it[i] == splitPaths[0][i] }) lcpLength++ else break
        }

        val partialRelative = splitPaths.map { segs -> segs.drop(lcpLength).joinToString("/") }
        val splitPartials = partialRelative.map { it.split("/").filter { s -> s.isNotEmpty() } }

        val hasEmpty = splitPartials.any { it.isEmpty() }
        var suffixLength = 0
        if (!hasEmpty) {
            val minSufLen = splitPartials.minOf { it.size }
            for (i in 0 until minSufLen) {
                val last = splitPartials[0][splitPartials[0].size - 1 - i]
                if (splitPartials.all { it[it.size - 1 - i] == last }) suffixLength++ else break
            }
        }

        if (suffixLength > 0) {
            val commonSuffix = splitPartials[0].takeLast(suffixLength).joinToString("/")
            for (root in absoluteRoots) {
                templates[commonSuffix] = (templates[commonSuffix] ?: 0) + 1
            }
        } else {
            for (partial in partialRelative) {
                templates[partial] = (templates[partial] ?: 0) + 1
            }
        }
    }

    for (root in relativeRoots) {
        templates[root] = (templates[root] ?: 0) + 1
    }

    return templates.entries
        .sortedBy { it.key }
        .map { (path, count) -> SourceRootTemplate(path, count) }
}
