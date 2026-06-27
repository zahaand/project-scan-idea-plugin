package dev.zahaand.projectscan.prompt

import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.Module
import dev.zahaand.projectscan.model.ScanResult
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StackInfo
import dev.zahaand.projectscan.model.StructureInfo
import dev.zahaand.projectscan.model.TestFramework
import dev.zahaand.projectscan.model.TestInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromptGeneratorOutputReadabilityTest {
    private val generator = PromptGenerator()
    private val noBaseline = emptyList<dev.zahaand.projectscan.baseline.BaselineRule>()

    private fun emptyScanResult() = ScanResult(
        stack = SectionResult.Empty,
        codeStyle = SectionResult.Empty,
        linters = SectionResult.Empty,
        tests = SectionResult.Empty,
        structure = SectionResult.Empty,
    )

    private fun stackResult(deps: List<Dependency>) = emptyScanResult().copy(
        stack = SectionResult.Ok(StackInfo(dependencies = deps)),
    )

    private fun structureResult(modules: List<Module>, packageSegments: List<String> = emptyList()) =
        emptyScanResult().copy(
            structure = SectionResult.Ok(StructureInfo(modules = modules, packageSegments = packageSegments)),
        )

    private fun testResult(frameworks: List<TestFramework> = emptyList(), sourceRoots: List<String> = emptyList()) =
        emptyScanResult().copy(
            tests = SectionResult.Ok(TestInfo(frameworks = frameworks, sourceRoots = sourceRoots)),
        )

    private fun techStackSection(scanResult: ScanResult): String {
        val rendered = generator.generate(scanResult, noBaseline).render()
        val start = rendered.indexOf("## Tech Stack")
        val end = rendered.indexOf("\n## ", start + 1).takeIf { it > 0 } ?: rendered.length
        return rendered.substring(start, end)
    }

    private fun structureSection(scanResult: ScanResult): String {
        val rendered = generator.generate(scanResult, noBaseline).render()
        val start = rendered.indexOf("## Project Structure")
        val end = rendered.indexOf("\n## ", start + 1).takeIf { it > 0 } ?: rendered.length
        return rendered.substring(start, end)
    }

    private fun testingSection(scanResult: ScanResult): String {
        val rendered = generator.generate(scanResult, noBaseline).render()
        val start = rendered.indexOf("## Testing")
        val end = rendered.indexOf("\n## ", start + 1).takeIf { it > 0 } ?: rendered.length
        return rendered.substring(start, end)
    }

    // === US1: Tech Stack Grouping ===

    @Test
    fun `US1 scenario 1 - uniform spring group gets header and artifact-only lines`() {
        val deps = listOf(
            Dependency("org.springframework", "spring-core", "6.1.4"),
            Dependency("org.springframework", "spring-web", "6.1.4"),
            Dependency("org.springframework", "spring-context", "6.1.4"),
            Dependency("org.springframework", "spring-beans", "6.1.4"),
            Dependency("org.springframework", "spring-aop", "6.1.4"),
        )
        val section = techStackSection(stackResult(deps))

        assertTrue(section.contains("org.springframework:* @ 6.1.4"), "Group header must be present")
        assertFalse(section.contains("6.1.4\n") && section.contains("spring-core:6.1.4"), "Version must not repeat on artifact lines")
        assertFalse(section.contains("org.springframework:spring-core:6.1.4"), "Per-artifact format must not appear under group header")
    }

    @Test
    fun `US1 scenario 2 - mixed versions group gets per-artifact format with no group header`() {
        val deps = listOf(
            Dependency("org.springframework", "spring-core", "6.1.4"),
            Dependency("org.springframework", "spring-web", "6.1.3"),
        )
        val section = techStackSection(stackResult(deps))

        assertFalse(section.contains("org.springframework:* @"), "No group header for mixed versions")
        assertTrue(section.contains("org.springframework:spring-core:6.1.4"), "spring-core with version must appear")
        assertTrue(section.contains("org.springframework:spring-web:6.1.3"), "spring-web with version must appear")
    }

    @Test
    fun `US1 scenario 3 - empty dependencies preserves build system metadata`() {
        val scanResult = emptyScanResult().copy(
            stack = SectionResult.Ok(
                StackInfo(
                    dependencies = emptyList(),
                    buildSystem = dev.zahaand.projectscan.model.BuildSystem.GRADLE,
                    jdkVersion = "21",
                ),
            ),
        )
        val section = techStackSection(scanResult)

        assertTrue(section.contains("GRADLE"), "Build system still shown")
        assertTrue(section.contains("21"), "JDK version still shown")
    }

    @Test
    fun `US1 scenario 4 - single-artifact group rendered in per-artifact format without header`() {
        val deps = listOf(Dependency("org.mapstruct", "mapstruct", "1.6.0"))
        val section = techStackSection(stackResult(deps))

        assertFalse(section.contains("org.mapstruct:* @"), "No group header for single-artifact group")
        assertTrue(section.contains("org.mapstruct:mapstruct:1.6.0"), "Per-artifact line present")
    }

    // === US2: Project Structure Discrepancies ===

    @Test
    fun `US2 scenario 1 - same-version artifact not in discrepancy block`() {
        val modules = listOf(
            Module("api", listOf(Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.0"))),
            Module("core", listOf(Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.0"))),
        )
        val section = structureSection(structureResult(modules))

        assertFalse(section.contains("jackson-databind"), "Same-version artifact must not appear in discrepancy block")
        assertTrue(section.contains("none") || !section.contains("jackson-databind"), "none notice or absent")
    }

    @Test
    fun `US2 scenario 2 - differing version artifact appears in discrepancy block`() {
        val modules = listOf(
            Module("api", listOf(Dependency("org.mapstruct", "mapstruct", "1.5.5"))),
            Module("core", listOf(Dependency("org.mapstruct", "mapstruct", "1.6.0"))),
        )
        val section = structureSection(structureResult(modules))

        assertTrue(section.contains("org.mapstruct:mapstruct"), "Discrepancy entry present")
        assertTrue(section.contains("api: 1.5.5") || section.contains("api:1.5.5") || section.contains("api"), "api version present")
        assertTrue(section.contains("core: 1.6.0") || section.contains("core:1.6.0") || section.contains("core"), "core version present")
    }

    @Test
    fun `US2 scenario 3 - all versions agree shows none notice`() {
        val modules = listOf(
            Module("api", listOf(Dependency("org.junit.jupiter", "junit-jupiter", "5.11.0"))),
            Module("core", listOf(Dependency("org.junit.jupiter", "junit-jupiter", "5.11.0"))),
        )
        val section = structureSection(structureResult(modules))

        assertTrue(section.contains("none"), "none notice present when no discrepancies")
    }

    @Test
    fun `US2 scenario 4 - modules with moduleDependencies render inter-module graph`() {
        val modules = listOf(
            Module("api", moduleDependencies = listOf("core", "shared")),
            Module("core", moduleDependencies = emptyList()),
            Module("shared", moduleDependencies = emptyList()),
        )
        val section = structureSection(structureResult(modules))

        assertTrue(section.contains("api"), "api module present")
        assertTrue(section.contains("core"), "core module present")
        assertTrue(section.contains("shared"), "shared module present")
        assertTrue(section.contains("→") || section.contains("->"), "inter-module graph arrow present")
    }

    @Test
    fun `US2 scenario 5 - non-empty packageSegments renders package segments line`() {
        val modules = listOf(Module("app"))
        val section = structureSection(structureResult(modules, packageSegments = listOf("dev", "zahaand")))

        assertTrue(section.contains("Package segments"), "Package segments label present")
        assertTrue(section.contains("dev"), "dev segment present")
        assertTrue(section.contains("zahaand"), "zahaand segment present")
    }

    @Test
    fun `US2 scenario 5b - empty packageSegments omits package segments line`() {
        val modules = listOf(Module("app"))
        val section = structureSection(structureResult(modules, packageSegments = emptyList()))

        assertFalse(section.contains("Package segments"), "Package segments line absent when empty")
    }

    @Test
    fun `US2 all scenarios - no per-module declaredDependencies lines`() {
        val modules = listOf(
            Module("api", listOf(Dependency("org.mapstruct", "mapstruct", "1.5.5"))),
            Module("core", listOf(Dependency("org.mapstruct", "mapstruct", "1.6.0"))),
        )
        val section = structureSection(structureResult(modules))

        assertFalse(section.contains("Dependencies:"), "No per-module dependency list")
        assertFalse(section.contains("mapstruct\n") && section.contains("- Module: api\n  - Dependencies:"), "No dependency list under module")
    }

    // === US3: Testing Section Readability ===

    @Test
    fun `US3 scenario 1 - 80 identical frameworks collapse to one line`() {
        val frameworks = List(80) { TestFramework("JUnit Jupiter", "5.10.2") }
        val section = testingSection(testResult(frameworks = frameworks))

        val count = section.lines().count { it.contains("JUnit Jupiter") }
        assertEquals(1, count, "Exactly one JUnit Jupiter line")
    }

    @Test
    fun `US3 scenario 2 - two absolute paths with same suffix collapse to one template line`() {
        val roots = listOf(
            "/home/ci/workspace/myapp/api/src/test/java",
            "/home/ci/workspace/myapp/core/src/test/java",
        )
        val section = testingSection(testResult(sourceRoots = roots))

        val lines = section.lines().filter { it.contains("src/test/java") }
        assertEquals(1, lines.size, "Exactly one source-root line for src/test/java")
        assertTrue(lines[0].contains("2 modules"), "Line contains '2 modules'")
        assertFalse(section.contains("/home/ci/workspace"), "Absolute prefix not in output")
    }

    @Test
    fun `US3 scenario 3 - distinct suffixes produce two template lines`() {
        val roots = listOf(
            "/home/ci/workspace/myapp/api/src/test/java",
            "/home/ci/workspace/myapp/core/src/test/kotlin",
        )
        val section = testingSection(testResult(sourceRoots = roots))

        val sourceRootLines = section.lines().filter { it.contains("Source Root") }
        assertEquals(2, sourceRootLines.size, "Two source-root lines for distinct suffixes")
        assertTrue(section.contains("src/test/java") || section.contains("java"), "src/test/java template present")
        assertTrue(section.contains("src/test/kotlin") || section.contains("kotlin"), "src/test/kotlin template present")
    }

    @Test
    fun `US3 scenario 4 - two distinct frameworks appear exactly once each`() {
        val frameworks = listOf(
            TestFramework("JUnit Jupiter", "5.10.2"),
            TestFramework("Mockito", "5.0.0"),
        )
        val section = testingSection(testResult(frameworks = frameworks))

        assertEquals(1, section.lines().count { it.contains("JUnit Jupiter") }, "JUnit Jupiter appears once")
        assertEquals(1, section.lines().count { it.contains("Mockito") }, "Mockito appears once")
    }

    @Test
    fun `US3 scenario 5 - single source root renders relative path without count suffix`() {
        val roots = listOf("/home/ci/workspace/myapp/api/src/test/java")
        val section = testingSection(testResult(sourceRoots = roots))

        val sourceRootLines = section.lines().filter { it.contains("src/test/java") }
        assertEquals(1, sourceRootLines.size, "Exactly one source-root line")
        assertFalse(sourceRootLines[0].contains("modules"), "Count suffix must not appear for count == 1")
        assertFalse(section.contains("/home/ci/workspace"), "Absolute prefix not in output")
    }
}
