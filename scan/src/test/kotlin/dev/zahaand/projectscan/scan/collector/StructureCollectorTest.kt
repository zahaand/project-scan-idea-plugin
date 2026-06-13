package dev.zahaand.projectscan.scan.collector

import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StructureInfo
import dev.zahaand.projectscan.scan.fake.FakeModuleStructurePort
import dev.zahaand.projectscan.scan.port.ModuleDescriptor
import dev.zahaand.projectscan.scan.port.ModuleStructurePort
import dev.zahaand.projectscan.scan.port.PackageTreeData
import org.junit.jupiter.api.Assertions.assertEquals
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
        val deps = listOf(
            Dependency("org.springframework", "spring-core", "6.1.0"),
            Dependency("com.google.guava", "guava", "32.0.0-jre"),
            Dependency("org.apache.commons", "commons-lang3", "3.13.0"),
        )
        val result = collector(modules = listOf(moduleDescriptor(":lib", externalDeps = deps))).collect()

        val ok = assertOk(result)
        assertEquals(deps, ok.modules.single().declaredDependencies)
    }

    @Test
    fun `root packages land in rootPackages and second-level in packageSegments in dotted notation`() {
        val tree = PackageTreeData(
            rootPackages = listOf("com", "org"),
            secondLevelSegments = listOf("com.example", "com.example.web", "org.utils"),
        )
        val result = collector(modules = listOf(moduleDescriptor(":app")), packageTree = tree).collect()

        val ok = assertOk(result)
        assertEquals(listOf("com", "org"), ok.rootPackages)
        assertEquals(listOf("com.example", "com.example.web", "org.utils"), ok.packageSegments)
        assertTrue(ok.rootPackages.none { it.contains("/") }, "rootPackages must use dotted notation, not path notation")
        assertTrue(ok.packageSegments.none { it.contains("/") }, "packageSegments must use dotted notation, not path notation")
    }

    @Test
    fun `root packages are not duplicated in packageSegments`() {
        val tree = PackageTreeData(
            rootPackages = listOf("com"),
            secondLevelSegments = listOf("com.example"),
        )
        val result = collector(modules = listOf(moduleDescriptor(":app")), packageTree = tree).collect()

        val ok = assertOk(result)
        assertTrue("com" !in ok.packageSegments, "root package 'com' must not appear in packageSegments")
        assertTrue("com.example" in ok.packageSegments)
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
    fun `source-less module is included with empty package contribution`() {
        val sourceless = ModuleDescriptor(
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
    fun `partial failure - modules readable but package tree throws - section Ok with modules and empty package data`() {
        val throwingPort = object : ModuleStructurePort {
            override fun getModules(): List<ModuleDescriptor> =
                listOf(moduleDescriptor(":core"))
            override fun getPackageTree(): PackageTreeData =
                throw RuntimeException("package tree unresolvable")
        }
        val result = StructureCollector(throwingPort).collect()

        val ok = assertOk(result)
        assertEquals(1, ok.modules.size)
        assertEquals(":core", ok.modules.single().name)
        assertTrue(ok.rootPackages.isEmpty())
        assertTrue(ok.packageSegments.isEmpty())
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

    private fun collector(
        modules: List<ModuleDescriptor>,
        packageTree: PackageTreeData = PackageTreeData(emptyList(), emptyList()),
    ) = StructureCollector(FakeModuleStructurePort(modules, packageTree))

    private fun assertOk(result: SectionResult<*>): StructureInfo {
        assertTrue(result is SectionResult.Ok<*>, "Expected Ok but got $result")
        @Suppress("UNCHECKED_CAST")
        return (result as SectionResult.Ok<StructureInfo>).data
    }
}
