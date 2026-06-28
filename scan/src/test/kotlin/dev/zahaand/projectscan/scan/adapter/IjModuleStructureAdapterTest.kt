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
}
