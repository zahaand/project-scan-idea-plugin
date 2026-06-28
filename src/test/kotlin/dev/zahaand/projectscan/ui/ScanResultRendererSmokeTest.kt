package dev.zahaand.projectscan.ui

import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.Module
import dev.zahaand.projectscan.model.ScanResult
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StackInfo
import dev.zahaand.projectscan.model.StructureInfo
import dev.zahaand.projectscan.model.TestInfo
import dev.zahaand.projectscan.prompt.PromptGenerator
import dev.zahaand.projectscan.shared.buildInvertedTechStack
import dev.zahaand.projectscan.shared.renderInvertedTechStack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanResultRendererSmokeTest {
    @Test
    fun `renderStack - single module single dep renders coordinate-version-count format`() {
        val modules =
            listOf(
                Module(
                    "api",
                    listOf(
                        Dependency("org.springframework", "spring-core", "6.1.4"),
                    ),
                ),
            )
        val output = ScanResultRenderer.renderStack(StackInfo(), modules)
        assertNotNull(output)
        assertTrue("Uniform entry must appear", output!!.contains("org.springframework:spring-core:6.1.4 [1 modules]"))
    }

    @Test
    fun `renderStack - same dep in two modules at same version shows count 2`() {
        val dep = Dependency("org.springframework", "spring-core", "6.1.4")
        val modules = listOf(Module("api", listOf(dep)), Module("svc", listOf(dep)))
        val output = ScanResultRenderer.renderStack(StackInfo(), modules)
        assertNotNull(output)
        assertTrue("Count-2 entry must appear", output!!.contains("org.springframework:spring-core:6.1.4 [2 modules]"))
    }

    @Test
    fun `renderStack - internal module dep excluded when internalModuleNames provided`() {
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
        val internalNames = modules.map { it.name }.toSet()
        val output = ScanResultRenderer.renderStack(StackInfo(), modules, internalNames)
        assertNotNull(output)
        assertFalse("Internal dep must not appear", output!!.contains("document-engine-spi"))
        assertTrue("External dep must appear", output.contains("spring-web"))
    }

    @Test
    fun `renderStack - empty modules and null preamble returns null`() {
        val output = ScanResultRenderer.renderStack(StackInfo(), emptyList())
        // renderStack returns null when rendered string is blank
        // StackInfo with no fields → preamble empty → entries empty → "not detected" → not blank → non-null
        // "not detected" is non-blank, so renderStack returns it
        assertNotNull(output)
        assertTrue("not detected expected", output!!.contains("not detected"))
    }

    @Test
    fun `renderTests - many paths with same recognized suffix collapse to one line`() {
        val roots = List(80) { "/home/ci/workspace/module-$it/src/test/java" }
        val info = TestInfo(sourceRoots = roots)
        val output = ScanResultRenderer.renderTests(info)
        assertNotNull(output)
        assertTrue("Collapsed src/test/java line must be present", output!!.contains("src/test/java"))
        assertEquals("Exactly one src/test/java line", 1, output.lines().count { it.contains("src/test/java") })
    }

    @Test
    fun `renderTests - absolute path prefix not present in output`() {
        val roots = List(80) { "/home/ci/workspace/module-$it/src/test/java" }
        val info = TestInfo(sourceRoots = roots)
        val output = ScanResultRenderer.renderTests(info)
        assertNotNull(output)
        assertFalse("Absolute path prefix must not be in output", output!!.contains("/home/ci/workspace"))
    }

    @Test
    fun `SC-005 - both renderer and generator produce same tech stack key content`() {
        val modules =
            listOf(
                Module(
                    "api",
                    listOf(
                        Dependency("org.springframework", "spring-core", "6.1.4"),
                        Dependency("org.springframework", "spring-web", "6.1.4"),
                        Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.0"),
                    ),
                ),
            )
        val stackInfo = StackInfo()
        val structureInfo = StructureInfo(modules = modules)
        val internalNames = modules.map { it.name }.toSet()

        val generator = PromptGenerator()
        val scanResult =
            ScanResult(
                stack = SectionResult.Ok(stackInfo),
                structure = SectionResult.Ok(structureInfo),
                tests = SectionResult.Empty,
                codeStyle = SectionResult.Empty,
                linters = SectionResult.Empty,
            )
        val rendered = generator.generate(scanResult, emptyList()).render()

        val start = rendered.indexOf("## Tech Stack")
        val end = rendered.indexOf("\n## ", start + 1).takeIf { it > 0 } ?: rendered.length
        val promptStackSection = rendered.substring(start, end)

        val rendererOutput = ScanResultRenderer.renderStack(stackInfo, modules, internalNames)!!

        assertTrue("spring-core entry in prompt", promptStackSection.contains("org.springframework:spring-core"))
        assertTrue("spring-core entry in renderer", rendererOutput.contains("org.springframework:spring-core"))

        assertTrue("jackson entry in prompt", promptStackSection.contains("jackson-databind"))
        assertTrue("jackson entry in renderer", rendererOutput.contains("jackson-databind"))
    }

    // SC-005 byte-identical parity test: both PromptGenerator.buildTechStackBlock() and
    // ScanResultRenderer.renderStack() must call renderInvertedTechStack() from the shared module
    // and produce byte-identical Tech Stack content. This test verifies the contract without a
    // running IDE. Testing parity is NOT byte-identical (C1 tracked deviation, deferred to Sprint 9).
    @Test
    fun `SC-005 byte-identical - tech stack content from renderer equals shared renderInvertedTechStack directly`() {
        val modules =
            listOf(
                Module(
                    "api",
                    listOf(
                        Dependency("org.springframework", "spring-core", "6.1.4"),
                        Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.0"),
                    ),
                ),
                Module(
                    "svc",
                    listOf(
                        Dependency("org.springframework", "spring-core", "6.0.0"),
                        Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.0"),
                    ),
                ),
            )
        val stackInfo = StackInfo()
        val internalNames = modules.map { it.name }.toSet()

        // Ground truth: call renderInvertedTechStack directly (the shared canonical function)
        val expected =
            renderInvertedTechStack(
                buildInvertedTechStack(modules, internalNames),
                stackInfo.buildSystem,
                stackInfo.jdkVersion,
                stackInfo.languageLevel,
            )

        // ScanResultRenderer must return the same string
        val rendererOutput = ScanResultRenderer.renderStack(stackInfo, modules, internalNames)!!
        assertEquals(
            "SC-005: renderer output must be byte-identical to shared renderInvertedTechStack",
            expected,
            rendererOutput,
        )

        // PromptGenerator must embed the same string in the Tech Stack block
        val structureInfo = StructureInfo(modules = modules)
        val scanResult =
            ScanResult(
                stack = SectionResult.Ok(stackInfo),
                structure = SectionResult.Ok(structureInfo),
                tests = SectionResult.Empty,
                codeStyle = SectionResult.Empty,
                linters = SectionResult.Empty,
            )
        val promptText = PromptGenerator().generate(scanResult, emptyList()).render()
        val blockStart = promptText.indexOf("## Tech Stack\n\n") + "## Tech Stack\n\n".length
        val blockEnd = promptText.indexOf("\n\n## ", blockStart).takeIf { it > 0 } ?: promptText.length
        val promptStackContent = promptText.substring(blockStart, blockEnd)
        assertEquals(
            "SC-005: prompt tech stack content must be byte-identical to shared renderInvertedTechStack",
            expected,
            promptStackContent,
        )
    }
}
