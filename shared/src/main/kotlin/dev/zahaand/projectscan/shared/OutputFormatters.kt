package dev.zahaand.projectscan.shared

import dev.zahaand.projectscan.model.BuildSystem
import dev.zahaand.projectscan.model.Module

// --- Inverted Tech Stack data model (T015) ---

data class CarrierModule(
    val name: String,
    val aggregator: String?,
)

data class AggregatorGroup(
    val aggregator: String?,
    val moduleNames: List<String>,
)

data class VersionEntry(
    val version: String,
    val isUniform: Boolean,
    val uniformModuleCount: Int,
    val groups: List<AggregatorGroup>,
)

data class TechEntry(
    val coordinate: String,
    val versions: List<VersionEntry>,
)

data class InvertedTechStack(
    val entries: List<TechEntry>,
)

// --- Source root normalisation (retained) ---

data class SourceRootTemplate(
    val relativePath: String,
)

private val RECOGNIZED_TEST_SUFFIXES =
    listOf(
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

// --- buildInvertedTechStack (T016) ---

private fun buildVersionEntries(versionGroups: Map<String, List<Pair<CarrierModule, String>>>): List<VersionEntry> =
    versionGroups.entries.sortedBy { it.key }.map { (version, pairs) ->
        val groups =
            pairs.groupBy { it.first.aggregator }.entries
                .sortedWith(compareBy(nullsLast()) { it.key })
                .map { (agg, ps) -> AggregatorGroup(aggregator = agg, moduleNames = ps.map { it.first.name }.sorted()) }
        VersionEntry(version = version, isUniform = false, uniformModuleCount = pairs.size, groups = groups)
    }

fun buildInvertedTechStack(
    modules: List<Module>,
    internalModuleNames: Set<String>,
): InvertedTechStack {
    val carriersByCoordinate = mutableMapOf<String, MutableList<Pair<CarrierModule, String>>>()

    for (module in modules) {
        val carrier = CarrierModule(name = module.name, aggregator = module.aggregator)
        module.declaredDependencies
            .filter { dep ->
                dep.artifactId !in internalModuleNames &&
                    "${dep.groupId}:${dep.artifactId}" !in internalModuleNames
            }
            .mapNotNull { dep -> dep.resolvedVersion?.let { dep to it } }
            .forEach { (dep, version) ->
                val coord = "${dep.groupId}:${dep.artifactId}"
                carriersByCoordinate.getOrPut(coord) { mutableListOf() } += carrier to version
            }
    }

    val entries =
        carriersByCoordinate.entries.sortedBy { it.key }.map { (coordinate, carriers) ->
            val versionGroups = carriers.groupBy { it.second }
            if (versionGroups.size == 1) {
                val (version, pairs) = versionGroups.entries.single()
                TechEntry(coordinate, listOf(VersionEntry(version, true, pairs.size, emptyList())))
            } else {
                TechEntry(coordinate, buildVersionEntries(versionGroups))
            }
        }

    return InvertedTechStack(entries = entries)
}

// --- renderInvertedTechStack (T017) ---

private fun renderGroup(
    version: String,
    group: AggregatorGroup,
): String {
    val prefix = "  - $version"
    val moduleList = group.moduleNames.joinToString(", ")
    return if (group.aggregator != null) "$prefix  ${group.aggregator}: $moduleList" else "$prefix  $moduleList"
}

private fun renderEntry(entry: TechEntry): List<String> {
    val ve = entry.versions.singleOrNull()
    if (ve != null && ve.isUniform) {
        return listOf("- ${entry.coordinate}:${ve.version} [${ve.uniformModuleCount} modules]")
    }
    val lines = mutableListOf("- ${entry.coordinate}")
    entry.versions.forEach { v -> v.groups.forEach { g -> lines += renderGroup(v.version, g) } }
    return lines
}

fun renderInvertedTechStack(
    stack: InvertedTechStack,
    buildSystem: BuildSystem?,
    jdkVersion: String?,
    languageLevel: String?,
): String {
    val preambleLines = mutableListOf<String>()
    buildSystem?.let { preambleLines += "- Build System: ${it.name}" }
    jdkVersion?.let { preambleLines += "- JDK Version: $it" }
    languageLevel?.let { preambleLines += "- Language Level: $it" }

    if (stack.entries.isEmpty() && preambleLines.isEmpty()) return "not detected"

    val lines = preambleLines + stack.entries.flatMap { renderEntry(it) }
    return if (lines.isEmpty()) "not detected" else lines.joinToString("\n")
}

// --- normalizeSourceRoots (retained) ---

fun normalizeSourceRoots(roots: List<String>): List<SourceRootTemplate> {
    val filtered =
        roots.filter { root ->
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
