package dev.zahaand.projectscan.scan.adapter

import com.intellij.openapi.project.Project
import dev.zahaand.projectscan.scan.ScanService

/**
 * Compilation-only verification that all Ij*Adapter classes accept a Project parameter and their
 * constructor signatures satisfy the port interfaces required by ScanService.
 *
 * This function is never called at runtime; the compiler validates the wiring.
 */
@Suppress("unused")
fun verifyProductionWiringCompiles(project: Project) {
    ScanService(
        buildSystemPort = IjBuildSystemAdapter(project),
        styleSourcePort = IjStyleSourceAdapter(project),
        linterPort = IjLinterAdapter(project),
        linterConfigParsers =
            mapOf(
                "checkstyle" to CheckstyleConfigParser(),
                "pmd" to PmdConfigParser(),
            ),
        testInfoPort = IjTestInfoAdapter(project),
        moduleStructurePort = IjModuleStructureAdapter(project),
    )
}
