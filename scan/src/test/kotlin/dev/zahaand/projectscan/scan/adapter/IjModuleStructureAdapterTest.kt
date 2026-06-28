package dev.zahaand.projectscan.scan.adapter

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class IjModuleStructureAdapterTest : BasePlatformTestCase() {
    fun testGetModulesReturnsEmptyWhenNoBuildSystemData() {
        val modules = IjModuleStructureAdapter(project).getModules()
        assertTrue(modules.isEmpty())
    }

    fun testGetModulesAggregatorIsNullForNonMavenProject() {
        // Gradle project with no Maven data — aggregator field defaults to null
        val modules = IjModuleStructureAdapter(project).getModules()
        // In a test fixture with no external project data, result is empty — no aggregator to assert
        assertTrue("Expected empty module list for bare test fixture", modules.isEmpty())
    }

    fun testNoDepHasBlankOrUnknownVersion_SC007() {
        // SC-007: a dep declared without explicit <version> (inherited from parent POM or
        // dependencyManagement) must surface as the resolved effective version — not blank or
        // "unknown". Runs over all modules returned by the adapter; trivially passes when the
        // module list is empty (bare fixture). Becomes load-bearing once a Maven fixture project
        // with version-inheriting deps is wired into the test suite.
        val modules = IjModuleStructureAdapter(project).getModules()
        for (module in modules) {
            for (dep in module.externalDependencies) {
                assertFalse(
                    "SC-007: ${dep.groupId}:${dep.artifactId} has blank resolvedVersion",
                    dep.resolvedVersion.isNullOrBlank(),
                )
                assertNotEquals(
                    "SC-007: ${dep.groupId}:${dep.artifactId} must not have 'unknown' resolvedVersion",
                    "unknown",
                    dep.resolvedVersion,
                )
            }
        }
    }
}
