package dev.zahaand.projectscan.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StructureInfoTest {
    @Test
    fun `empty-state StructureInfo has empty lists`() {
        val info = StructureInfo()
        assertTrue(info.modules.isEmpty())
        assertTrue(info.rootPackages.isEmpty())
        assertTrue(info.packageSegments.isEmpty())
    }

    @Test
    fun `single-module project with empty deps lists`() {
        val module = Module(name = "app")
        assertTrue(module.declaredDependencies.isEmpty())
        assertTrue(module.moduleDependencies.isEmpty())
        val info = StructureInfo(modules = listOf(module))
        assertEquals(1, info.modules.size)
        assertEquals("app", info.modules[0].name)
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
        val info =
            StructureInfo(
                modules = listOf(coreModule, appModule),
                rootPackages = listOf("dev.zahaand.projectscan"),
            )
        assertEquals(2, info.modules.size)
        val app = info.modules.find { it.name == "app" }!!
        assertEquals(listOf("core"), app.moduleDependencies)
        val core = info.modules.find { it.name == "core" }!!
        assertEquals(listOf(dep), core.declaredDependencies)
    }

    @Test
    fun `rootPackages is project-wide list`() {
        val info = StructureInfo(rootPackages = listOf("dev.zahaand.projectscan", "dev.zahaand.util"))
        assertEquals(2, info.rootPackages.size)
        assertTrue(info.rootPackages.contains("dev.zahaand.projectscan"))
    }

    @Test
    fun `packageSegments holds second-level dotted package paths`() {
        val info =
            StructureInfo(
                packageSegments = listOf("com.example.web", "com.example.domain"),
            )
        assertEquals(2, info.packageSegments.size)
        assertTrue(info.packageSegments.contains("com.example.web"))
    }
}
