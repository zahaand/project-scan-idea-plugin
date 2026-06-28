package dev.zahaand.projectscan.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StructureInfoTest {
    @Test
    fun `empty-state StructureInfo has empty modules list`() {
        val info = StructureInfo()
        assertTrue(info.modules.isEmpty())
    }

    @Test
    fun `single-module project with empty deps lists`() {
        val module = Module(name = "app")
        assertTrue(module.declaredDependencies.isEmpty())
        assertTrue(module.moduleDependencies.isEmpty())
        assertNull(module.aggregator)
        val info = StructureInfo(modules = listOf(module))
        assertEquals(1, info.modules.size)
        assertEquals("app", info.modules[0].name)
    }

    @Test
    fun `module with aggregator set`() {
        val module = Module(name = "child", aggregator = "parent-aggregator")
        assertEquals("parent-aggregator", module.aggregator)
    }

    @Test
    fun `multi-module with inter-module links and external dependencies`() {
        val dep = Dependency("com.google.guava", "guava", "32.1.2-jre")
        val coreModule =
            Module(
                name = "core",
                declaredDependencies = listOf(dep),
                moduleDependencies = emptyList(),
            )
        val appModule =
            Module(
                name = "app",
                declaredDependencies = emptyList(),
                moduleDependencies = listOf("core"),
            )
        val info = StructureInfo(modules = listOf(coreModule, appModule))
        assertEquals(2, info.modules.size)
        val app = info.modules.find { it.name == "app" }!!
        assertEquals(listOf("core"), app.moduleDependencies)
        val core = info.modules.find { it.name == "core" }!!
        assertEquals(listOf(dep), core.declaredDependencies)
    }
}
