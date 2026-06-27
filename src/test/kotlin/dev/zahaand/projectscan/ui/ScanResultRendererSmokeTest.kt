package dev.zahaand.projectscan.ui

import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.Module
import dev.zahaand.projectscan.model.ScanResult
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StackInfo
import dev.zahaand.projectscan.model.StructureInfo
import dev.zahaand.projectscan.model.TestFramework
import dev.zahaand.projectscan.model.TestInfo
import dev.zahaand.projectscan.prompt.PromptGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanResultRendererSmokeTest {

    @Test
    fun `US1 positive - uniform group renders group header`() {
        val info = StackInfo(
            dependencies = listOf(
                Dependency("org.springframework", "spring-core", "6.1.4"),
                Dependency("org.springframework", "spring-web", "6.1.4"),
                Dependency("org.springframework", "spring-context", "6.1.4"),
                Dependency("org.springframework", "spring-beans", "6.1.4"),
                Dependency("org.springframework", "spring-aop", "6.1.4"),
            ),
        )
        val output = ScanResultRenderer.renderStack(info)
        assertNotNull(output)
        assertTrue("Group header must be present", output!!.contains("org.springframework:* @ 6.1.4"))
    }

    @Test
    fun `US1 negative - version not repeated on individual artifact lines under header`() {
        val info = StackInfo(
            dependencies = listOf(
                Dependency("org.springframework", "spring-core", "6.1.4"),
                Dependency("org.springframework", "spring-web", "6.1.4"),
            ),
        )
        val output = ScanResultRenderer.renderStack(info)
        assertNotNull(output)
        assertFalse(
            "Version must not appear on individual artifact lines under header",
            output!!.contains("spring-core:6.1.4"),
        )
    }

    @Test
    fun `US2 positive - version discrepancy entry present when modules disagree`() {
        val info = StructureInfo(
            modules = listOf(
                Module("api", listOf(Dependency("org.mapstruct", "mapstruct", "1.5.5"))),
                Module("core", listOf(Dependency("org.mapstruct", "mapstruct", "1.6.0"))),
            ),
        )
        val output = ScanResultRenderer.renderStructure(info)
        assertNotNull(output)
        assertTrue("Discrepancy entry must be present", output!!.contains("org.mapstruct:mapstruct"))
    }

    @Test
    fun `US2 negative - same-version artifact not in discrepancy block`() {
        val info = StructureInfo(
            modules = listOf(
                Module("api", listOf(Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.0"))),
                Module("core", listOf(Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.0"))),
            ),
        )
        val output = ScanResultRenderer.renderStructure(info)
        assertNotNull(output)
        assertFalse(
            "Same-version artifact must not appear in discrepancy block",
            output!!.contains("jackson-databind") && output.contains("→"),
        )
    }

    @Test
    fun `US2 negative - no Dependency lines in output`() {
        val info = StructureInfo(
            modules = listOf(
                Module("api", listOf(Dependency("org.mapstruct", "mapstruct", "1.5.5"))),
                Module("core", listOf(Dependency("org.mapstruct", "mapstruct", "1.6.0"))),
            ),
        )
        val output = ScanResultRenderer.renderStructure(info)
        assertNotNull(output)
        assertFalse("No Dependency: lines in output", output!!.contains("Dependency:"))
        assertFalse("No '  Dependency:' lines in output", output.contains("  Dependency:"))
    }

    @Test
    fun `US3 positive - many paths with same recognized suffix collapse to one line`() {
        val roots = List(80) { "/home/ci/workspace/module-$it/src/test/java" }
        val info = TestInfo(sourceRoots = roots)
        val output = ScanResultRenderer.renderTests(info)
        assertNotNull(output)
        assertTrue("Collapsed src/test/java line must be present", output!!.contains("src/test/java"))
        assertEquals("Exactly one src/test/java line", 1, output.lines().count { it.contains("src/test/java") })
        assertFalse("No module count suffix", output.contains("modules"))
    }

    @Test
    fun `US3 negative - absolute path prefix not present in output`() {
        val roots = List(80) { "/home/ci/workspace/module-$it/src/test/java" }
        val info = TestInfo(sourceRoots = roots)
        val output = ScanResultRenderer.renderTests(info)
        assertNotNull(output)
        assertFalse("Absolute path prefix must not be in output", output!!.contains("/home/ci/workspace"))
    }

    @Test
    fun `T022 SC-006 - both consumers produce same key content for non-empty ScanResult`() {
        val stackInfo = StackInfo(
            dependencies = listOf(
                Dependency("org.springframework", "spring-core", "6.1.4"),
                Dependency("org.springframework", "spring-web", "6.1.4"),
                Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.0"),
            ),
        )
        val structureInfo = StructureInfo(
            modules = listOf(
                Module("api", listOf(
                    Dependency("org.mapstruct", "mapstruct", "1.5.5"),
                    Dependency("org.junit.jupiter", "junit-jupiter", "5.11.0"),
                )),
                Module("core", listOf(
                    Dependency("org.mapstruct", "mapstruct", "1.6.0"),
                    Dependency("org.junit.jupiter", "junit-jupiter", "5.11.0"),
                )),
            ),
        )
        val testInfo = TestInfo(
            frameworks = listOf(TestFramework("JUnit Jupiter", "5.11.0")),
            sourceRoots = listOf(
                "/home/ci/workspace/api/src/test/java",
                "/home/ci/workspace/core/src/test/java",
            ),
        )

        val generator = PromptGenerator()
        val scanResult = ScanResult(
            stack = SectionResult.Ok(stackInfo),
            structure = SectionResult.Ok(structureInfo),
            tests = SectionResult.Ok(testInfo),
            codeStyle = SectionResult.Empty,
            linters = SectionResult.Empty,
        )
        val rendered = generator.generate(scanResult, emptyList()).render()

        fun extractSection(heading: String): String {
            val start = rendered.indexOf("## $heading")
            val end = rendered.indexOf("\n## ", start + 1).takeIf { it > 0 } ?: rendered.length
            return rendered.substring(start, end)
        }

        val promptStackSection = extractSection("Tech Stack")
        val rendererStackOutput = ScanResultRenderer.renderStack(stackInfo)!!

        assertTrue("Group header in prompt", promptStackSection.contains("org.springframework:* @ 6.1.4"))
        assertTrue("Group header in renderer", rendererStackOutput.contains("org.springframework:* @ 6.1.4"))

        assertTrue("jackson line in prompt", promptStackSection.contains("jackson-databind"))
        assertTrue("jackson line in renderer", rendererStackOutput.contains("jackson-databind"))

        assertFalse("spring-core version not repeated in prompt", promptStackSection.contains("spring-core:6.1.4"))
        assertFalse("spring-core version not repeated in renderer", rendererStackOutput.contains("spring-core:6.1.4"))

        val promptStructSection = extractSection("Project Structure")
        val rendererStructOutput = ScanResultRenderer.renderStructure(structureInfo)!!

        assertTrue("Discrepancy in prompt", promptStructSection.contains("org.mapstruct:mapstruct"))
        assertTrue("Discrepancy in renderer", rendererStructOutput.contains("org.mapstruct:mapstruct"))

        assertFalse("junit not in discrepancy (prompt)", promptStructSection.contains("junit-jupiter →"))
        assertFalse("junit not in discrepancy (renderer)", rendererStructOutput.contains("junit-jupiter →"))

        val promptTestSection = extractSection("Testing")
        val rendererTestOutput = ScanResultRenderer.renderTests(testInfo)!!

        assertTrue("Framework line in prompt", promptTestSection.contains("JUnit Jupiter 5.11.0"))
        assertTrue("Framework line in renderer", rendererTestOutput.contains("JUnit Jupiter 5.11.0"))

        assertTrue("Source root template in prompt", promptTestSection.contains("src/test/java"))
        assertTrue("Source root template in renderer", rendererTestOutput.contains("src/test/java"))

        assertFalse("No module count in prompt test section", promptTestSection.contains("modules"))
        assertFalse("No module count in renderer test output", rendererTestOutput.contains("modules"))

        assertEquals("Framework count same",
            promptTestSection.lines().count { it.contains("Framework:") },
            rendererTestOutput.lines().count { it.contains("Framework:") })
    }

    @Test
    fun `Change 1 - internal module deps excluded from renderStack when module names provided`() {
        val info = StackInfo(
            dependencies = listOf(
                Dependency("com.example", "document-engine-spi", "1.0"),
                Dependency("org.spring", "spring-web", "6.0"),
            ),
        )
        val output = ScanResultRenderer.renderStack(info, internalModuleNames = setOf("document-engine-spi"))
        assertNotNull(output)
        assertFalse("Internal dep must not appear", output!!.contains("document-engine-spi"))
        assertTrue("External dep must appear", output.contains("spring-web"))
    }

    @Test
    fun `Change 4 - inter-module graph not rendered in renderStructure`() {
        val info = StructureInfo(
            modules = listOf(
                Module("api", moduleDependencies = listOf("core")),
                Module("core", moduleDependencies = emptyList()),
            ),
        )
        val output = ScanResultRenderer.renderStructure(info)
        assertNotNull(output)
        assertFalse("Inter-module graph must not be rendered", output!!.contains("api → ["))
        assertTrue("Module names still appear", output.contains("api"))
        assertTrue("Module names still appear", output.contains("core"))
    }

    @Test
    fun `Change 5 - dominant-version format in renderStructure discrepancy block`() {
        val info = StructureInfo(
            modules = listOf(
                Module("api", listOf(Dependency("org.mapstruct", "mapstruct", "1.5.5"))),
                Module("core", listOf(Dependency("org.mapstruct", "mapstruct", "1.6.0"))),
            ),
        )
        val output = ScanResultRenderer.renderStructure(info)
        assertNotNull(output)
        assertTrue("Dominant-version keyword present", output!!.contains("mostly"))
        assertTrue("Exceptions keyword present", output.contains("except"))
        assertTrue("Artifact coordinate present", output.contains("org.mapstruct:mapstruct"))
    }
}
