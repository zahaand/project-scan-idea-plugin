package dev.zahaand.projectscan.scan.collector

import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StructureInfo
import dev.zahaand.projectscan.scan.fake.FakeModuleStructurePort
import dev.zahaand.projectscan.scan.port.ModuleDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StructureCollectorTest {
    @Test
    fun `three-module dependency graph A points to B which points to C`() {
        val moduleA = moduleDescriptor(":a", moduleDeps = listOf(":b"))
        val moduleB = moduleDescriptor(":b", moduleDeps = listOf(":c"))
        val moduleC = moduleDescriptor(":c")
        val result = collector(modules = listOf(moduleA, moduleB, moduleC)).collect()

        val ok = assertOk(result)
        val byName = ok.modules.associateBy { it.name }
        assertEquals(3, ok.modules.size)
        assertEquals(listOf(":b"), byName[":a"]?.moduleDependencies)
        assertEquals(listOf(":c"), byName[":b"]?.moduleDependencies)
        assertTrue(byName[":c"]?.moduleDependencies.isNullOrEmpty())
    }

    @Test
    fun `module with three external declared dependencies`() {
        val deps =
            listOf(
                Dependency("org.springframework", "spring-core", "6.1.0"),
                Dependency("com.google.guava", "guava", "32.0.0-jre"),
                Dependency("org.apache.commons", "commons-lang3", "3.13.0"),
            )
        val result = collector(modules = listOf(moduleDescriptor(":lib", externalDeps = deps))).collect()

        val ok = assertOk(result)
        assertEquals(deps, ok.modules.single().declaredDependencies)
    }

    @Test
    fun `single-module project`() {
        val deps = listOf(Dependency("junit", "junit", "4.13.2"))
        val result = collector(modules = listOf(moduleDescriptor(":app", externalDeps = deps))).collect()

        val ok = assertOk(result)
        assertEquals(1, ok.modules.size)
        assertEquals(":app", ok.modules.single().name)
        assertEquals(deps, ok.modules.single().declaredDependencies)
    }

    @Test
    fun `source-less module is included`() {
        val sourceless =
            ModuleDescriptor(
                name = ":bom",
                externalDependencies = emptyList(),
                moduleDependencies = emptyList(),
                sourceRootPaths = emptyList(),
                hasSourceRoots = false,
            )
        val result = collector(modules = listOf(sourceless)).collect()

        val ok = assertOk(result)
        assertEquals(1, ok.modules.size)
        assertEquals(":bom", ok.modules.single().name)
    }

    @Test
    fun `duplicate inter-module dependency is deduplicated and recorded once`() {
        val module = moduleDescriptor(":app", moduleDeps = listOf(":core", ":core", ":util"))
        val result = collector(modules = listOf(module)).collect()

        val ok = assertOk(result)
        assertEquals(listOf(":core", ":util"), ok.modules.single().moduleDependencies)
    }

    @Test
    fun `empty module list returns Empty`() {
        val result = collector(modules = emptyList()).collect()
        assertTrue(result is SectionResult.Empty, "Expected Empty but got $result")
    }

    @Test
    fun `aggregator field is propagated from descriptor to module`() {
        val descriptor =
            ModuleDescriptor(
                name = "child-module",
                externalDependencies = emptyList(),
                moduleDependencies = emptyList(),
                sourceRootPaths = listOf("src/main/kotlin"),
                hasSourceRoots = true,
                aggregator = "parent-aggregator",
            )
        val result = collector(modules = listOf(descriptor)).collect()

        val ok = assertOk(result)
        assertEquals("parent-aggregator", ok.modules.single().aggregator)
    }

    @Test
    fun `null aggregator in descriptor produces null aggregator in module`() {
        val descriptor = moduleDescriptor(":root")
        val result = collector(modules = listOf(descriptor)).collect()

        val ok = assertOk(result)
        assertNull(ok.modules.single().aggregator)
    }

    // --- helpers ---

    private fun moduleDescriptor(
        name: String,
        externalDeps: List<Dependency> = emptyList(),
        moduleDeps: List<String> = emptyList(),
        hasSourceRoots: Boolean = true,
    ) = ModuleDescriptor(
        name = name,
        externalDependencies = externalDeps,
        moduleDependencies = moduleDeps,
        sourceRootPaths = if (hasSourceRoots) listOf("src/main/kotlin") else emptyList(),
        hasSourceRoots = hasSourceRoots,
    )

    private fun collector(modules: List<ModuleDescriptor>) = StructureCollector(FakeModuleStructurePort(modules))

    private fun assertOk(result: SectionResult<*>): StructureInfo {
        assertTrue(result is SectionResult.Ok<*>, "Expected Ok but got $result")
        @Suppress("UNCHECKED_CAST")
        return (result as SectionResult.Ok<StructureInfo>).data
    }
}
