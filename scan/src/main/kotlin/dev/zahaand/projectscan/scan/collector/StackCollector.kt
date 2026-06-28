package dev.zahaand.projectscan.scan.collector

import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StackInfo
import dev.zahaand.projectscan.scan.port.BuildSystemPort

class StackCollector(
    private val buildSystemPort: BuildSystemPort,
) {
    fun collect(): SectionResult<StackInfo> {
        val buildSystem = buildSystemPort.getBuildSystem()
        val jdkVersion = buildSystemPort.getJdkVersion()
        val languageLevel = maxLanguageLevel(buildSystemPort.getModuleLanguageLevels().values)

        if (buildSystem == null && jdkVersion == null && languageLevel == null) return SectionResult.Empty

        return SectionResult.Ok(
            StackInfo(
                jdkVersion = jdkVersion,
                languageLevel = languageLevel,
                buildSystem = buildSystem,
            ),
        )
    }

    private fun maxLanguageLevel(levels: Collection<String>): String? =
        levels.maxWithOrNull { a, b ->
            val ia = a.toIntOrNull()
            val ib = b.toIntOrNull()
            if (ia != null && ib != null) ia.compareTo(ib) else a.compareTo(b)
        }
}
