package dev.zahaand.projectscan.prompt

import dev.zahaand.projectscan.model.BuildSystem
import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.Module
import dev.zahaand.projectscan.model.ScanResult
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StackInfo
import dev.zahaand.projectscan.model.StructureInfo
import dev.zahaand.projectscan.model.TestInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromptGeneratorOutputReadabilityTest {
    private val generator = PromptGenerator()
    private val noBaseline = emptyList<dev.zahaand.projectscan.baseline.BaselineRule>()

    private fun emptyScanResult() =
        ScanResult(
            stack = SectionResult.Empty,
            codeStyle = SectionResult.Empty,
            linters = SectionResult.Empty,
            tests = SectionResult.Empty,
            structure = SectionResult.Empty,
        )

    private fun techStackFromModules(
        modules: List<Module>,
        buildSystem: BuildSystem? = null,
        jdkVersion: String? = null,
        languageLevel: String? = null,
    ) = emptyScanResult().copy(
        stack =
            SectionResult.Ok(
                StackInfo(buildSystem = buildSystem, jdkVersion = jdkVersion, languageLevel = languageLevel),
            ),
        structure = SectionResult.Ok(StructureInfo(modules = modules)),
    )

    private fun testScan(sourceRoots: List<String> = emptyList()) =
        emptyScanResult().copy(
            tests = SectionResult.Ok(TestInfo(sourceRoots = sourceRoots)),
        )

    private fun techStackSection(scanResult: ScanResult): String {
        val rendered = generator.generate(scanResult, noBaseline).render()
        val start = rendered.indexOf("## Tech Stack")
        val end = rendered.indexOf("\n## ", start + 1).takeIf { it > 0 } ?: rendered.length
        return rendered.substring(start, end)
    }

    private fun testingSection(scanResult: ScanResult): String {
        val rendered = generator.generate(scanResult, noBaseline).render()
        val start = rendered.indexOf("## Testing")
        val end = rendered.indexOf("\n## ", start + 1).takeIf { it > 0 } ?: rendered.length
        return rendered.substring(start, end)
    }

    // === Tech Stack (inverted format) ===

    @Test
    fun `inverted stack - single module single dep renders coord-version-count format`() {
        val modules =
            listOf(
                Module("api", listOf(Dependency("org.springframework", "spring-core", "6.1.4"))),
            )
        val section = techStackSection(techStackFromModules(modules))

        assertTrue(section.contains("org.springframework:spring-core:6.1.4 [1 modules]"), "Actual: $section")
    }

    @Test
    fun `inverted stack - same dep in two modules at same version shows count 2`() {
        val dep = Dependency("org.springframework", "spring-core", "6.1.4")
        val modules = listOf(Module("api", listOf(dep)), Module("svc", listOf(dep)))
        val section = techStackSection(techStackFromModules(modules))

        assertTrue(section.contains("org.springframework:spring-core:6.1.4 [2 modules]"), "Actual: $section")
    }

    @Test
    fun `inverted stack - same coord at two different versions renders multi-version entry`() {
        val modules =
            listOf(
                Module("api", listOf(Dependency("org.springframework", "spring-core", "6.1.4"))),
                Module("svc", listOf(Dependency("org.springframework", "spring-core", "6.0.0"))),
            )
        val section = techStackSection(techStackFromModules(modules))

        val lines = section.lines()
        val headerIdx = lines.indexOfFirst { it.contains("org.springframework:spring-core") && !it.contains("[") }
        assertTrue(headerIdx >= 0, "Coordinate header expected. Actual:\n$section")
        val indented = lines.drop(headerIdx + 1).filter { it.trimStart().startsWith("- ") }
        assertTrue(indented.isNotEmpty(), "Indented version lines expected. Actual:\n$section")
    }

    @Test
    fun `inverted stack - empty modules and no preamble yields not detected`() {
        val section = techStackSection(techStackFromModules(emptyList()))

        assertTrue(section.contains("not detected"), "Actual: $section")
    }

    @Test
    fun `inverted stack - preamble metadata renders when non-null`() {
        val section =
            techStackSection(techStackFromModules(emptyList(), buildSystem = BuildSystem.GRADLE, jdkVersion = "21"))

        assertTrue(section.contains("GRADLE"), "Build system expected. Actual: $section")
        assertTrue(section.contains("21"), "JDK version expected. Actual: $section")
        assertFalse(
            section.contains("not detected"),
            "Should not show not-detected when preamble present. Actual: $section",
        )
    }

    // === Internal module filtering ===

    @Test
    fun `internal module names excluded from tech stack`() {
        val modules =
            listOf(
                Module("document-engine-spi", emptyList()),
                Module(
                    "api",
                    listOf(
                        Dependency("com.example", "document-engine-spi", "1.0"),
                        Dependency("org.spring", "spring-web", "6.0"),
                    ),
                ),
            )
        val section = techStackSection(techStackFromModules(modules))

        assertFalse(section.contains("document-engine-spi"), "Internal module dep must not appear in Tech Stack")
        assertTrue(section.contains("spring-web"), "External dep must remain")
    }

    @Test
    fun `no modules means no internal filtering`() {
        val modules =
            listOf(
                Module(
                    "api",
                    listOf(
                        Dependency("com.example", "my-lib", "1.0"),
                        Dependency("org.spring", "spring-web", "6.0"),
                    ),
                ),
            )
        val section = techStackSection(techStackFromModules(modules))

        assertTrue(section.contains("my-lib"), "All external deps shown. Actual: $section")
        assertTrue(section.contains("spring-web"), "Actual: $section")
    }

    // === Testing section — source root normalization ===

    @Test
    fun `US3 - build-output target generated-test-sources excluded from Testing section`() {
        val roots =
            listOf(
                "/project/api/src/test/java",
                "/project/api/target/generated-test-sources/test-annotations",
            )
        val section = testingSection(testScan(sourceRoots = roots))

        assertFalse(section.contains("generated-test-sources"), "Build-output path must not appear")
        assertTrue(section.contains("src/test/java"), "Real test root must appear")
    }

    @Test
    fun `US3 - two absolute paths with same recognized suffix collapse to one template line`() {
        val roots =
            listOf(
                "/home/ci/workspace/myapp/api/src/test/java",
                "/home/ci/workspace/myapp/core/src/test/java",
            )
        val section = testingSection(testScan(sourceRoots = roots))

        val lines = section.lines().filter { it.contains("src/test/java") }
        assertEquals(1, lines.size, "Exactly one source-root line for src/test/java")
        assertFalse(section.contains("/home/ci/workspace"), "Absolute prefix not in output")
    }

    @Test
    fun `US3 - distinct suffixes produce two template lines`() {
        val roots =
            listOf(
                "/home/ci/workspace/myapp/api/src/test/java",
                "/home/ci/workspace/myapp/core/src/test/kotlin",
            )
        val section = testingSection(testScan(sourceRoots = roots))

        assertTrue(section.contains("src/test/java"), "src/test/java template present")
        assertTrue(section.contains("src/test/kotlin"), "src/test/kotlin template present")
    }

    @Test
    fun `US3 - single source root renders tail template`() {
        val roots = listOf("/home/ci/workspace/myapp/api/src/test/java")
        val section = testingSection(testScan(sourceRoots = roots))

        val sourceRootLines = section.lines().filter { it.contains("src/test/java") }
        assertEquals(1, sourceRootLines.size, "Exactly one source-root line")
        assertFalse(section.contains("/home/ci/workspace"), "Absolute prefix not in output")
    }

    @Test
    fun `US3 - many paths with same recognized suffix collapse to one line`() {
        val roots = List(80) { "/home/ci/workspace/module-$it/src/test/java" }
        val section = testingSection(testScan(sourceRoots = roots))

        val count = section.lines().count { it.contains("src/test/java") }
        assertEquals(1, count, "Exactly one src/test/java line")
        assertFalse(section.contains("/home/ci/workspace"), "Absolute prefix not in output")
    }
}
