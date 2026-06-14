package dev.zahaand.projectscan.scan.adapter

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class IjDependencyAdapterTest : BasePlatformTestCase() {
    fun testGetModuleDependenciesEmptyWhenNoBuildSystem() {
        val result = IjDependencyAdapter(project).getModuleDependencies()
        assertTrue(result.isEmpty())
    }
}
