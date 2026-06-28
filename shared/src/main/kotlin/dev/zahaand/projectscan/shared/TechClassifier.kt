package dev.zahaand.projectscan.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// --- Public output types ---

data class TechFamilyEntry(val name: String, val versions: List<String>)

data class RawDepEntry(val coordinate: String, val versions: List<String>)

data class ClassifiedTechStack(
    val technologies: List<TechFamilyEntry>,
    val others: List<RawDepEntry>,
)

// --- Internal resource types (JSON DTO) ---

@Serializable
private data class AllowlistEntryDto(
    @SerialName("groupIdPrefix") val groupIdPrefix: String,
    @SerialName("name") val name: String,
)

@Serializable
private data class TechConfigDto(
    @SerialName("schemaVersion") val schemaVersion: Int,
    @SerialName("denylist") val denylist: List<String>,
    @SerialName("springCoreArtifactFamilies") val springCoreArtifactFamilies: Map<String, String>,
    @SerialName("allowlist") val allowlist: List<AllowlistEntryDto>,
)

// --- Runtime config ---

internal data class AllowlistEntry(val groupIdPrefix: String, val name: String)

internal class TechConfig(
    val denylist: Set<String>,
    val springCoreArtifactFamilies: Map<String, String>,
    // Preserved in JSON order; longest-prefix resolution applied at classify time.
    val allowlist: List<AllowlistEntry>,
)

// --- Loader ---

private const val TECH_RESOURCE = "/dev/zahaand/projectscan/shared/technologies.json"
private val jsonParser = Json { ignoreUnknownKeys = true }

internal object TechClassifierLoader {
    val config: TechConfig by lazy { load() }

    private fun load(): TechConfig {
        val stream =
            TechClassifierLoader::class.java.getResourceAsStream(TECH_RESOURCE)
                ?: error("technologies.json not found at classpath path: $TECH_RESOURCE")
        val dto = jsonParser.decodeFromString<TechConfigDto>(stream.bufferedReader().readText())
        return TechConfig(
            denylist = dto.denylist.toHashSet(),
            springCoreArtifactFamilies = dto.springCoreArtifactFamilies,
            allowlist = dto.allowlist.map { AllowlistEntry(it.groupIdPrefix, it.name) },
        )
    }
}

// --- Classification ---

private const val SPRING_BASE_GROUP = "org.springframework"

// Null return = unrecognized (→ Other dependencies).
// DENIED sentinel = entry is in the denylist (→ drop entirely).
private val DENIED = object {}

private fun findFamilyOrDenied(
    groupId: String,
    artifactId: String,
    config: TechConfig,
): Any? =
    when {
        "$groupId:$artifactId" in config.denylist -> DENIED
        groupId == SPRING_BASE_GROUP ->
            // Null means this org.springframework artifact is not in any sub-family → Other.
            config.springCoreArtifactFamilies[artifactId]
        else ->
            config.allowlist
                .filter { e -> groupId == e.groupIdPrefix || groupId.startsWith("${e.groupIdPrefix}.") }
                .maxByOrNull { e -> e.groupIdPrefix.length }
                ?.name
    }

internal fun classify(
    stack: InvertedTechStack,
    config: TechConfig = TechClassifierLoader.config,
): ClassifiedTechStack {
    val familyVersions = mutableMapOf<String, MutableSet<String>>()
    val othersMap = mutableMapOf<String, MutableSet<String>>()

    for (entry in stack.entries) {
        val ci = entry.coordinate.indexOf(':')
        val groupId = if (ci >= 0) entry.coordinate.substring(0, ci) else entry.coordinate
        val artifactId = if (ci >= 0) entry.coordinate.substring(ci + 1) else ""

        when (val result = findFamilyOrDenied(groupId, artifactId, config)) {
            DENIED -> continue
            is String -> {
                familyVersions
                    .getOrPut(result) { mutableSetOf() }
                    .addAll(entry.versions.map { it.version })
            }
            else -> {
                othersMap
                    .getOrPut(entry.coordinate) { mutableSetOf() }
                    .addAll(entry.versions.map { it.version })
            }
        }
    }

    return ClassifiedTechStack(
        technologies =
            familyVersions.entries
                .sortedBy { it.key }
                .map { (name, versions) -> TechFamilyEntry(name, versions.sorted()) },
        others =
            othersMap.entries
                .sortedBy { it.key }
                .map { (coord, versions) -> RawDepEntry(coord, versions.sorted()) },
    )
}
