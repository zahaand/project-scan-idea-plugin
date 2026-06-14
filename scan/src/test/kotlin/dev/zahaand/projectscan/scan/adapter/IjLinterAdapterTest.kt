package dev.zahaand.projectscan.scan.adapter

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class IjLinterAdapterTest : BasePlatformTestCase() {
    fun testGradleLinterReturnsSmokeEmptyInLightFixture() {
        val tools = IjLinterAdapter(project).getAppliedLinterTools()
        assertTrue(tools.isEmpty())
    }
}
