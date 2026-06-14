package dev.zahaand.projectscan.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProjectScanModelTest {
    @Test
    fun `all-empty ProjectScanModel constructs without error and all sections are non-null`() {
        val model =
            ProjectScanModel(
                stack = StackInfo(),
                codeStyle = CodeStyleInfo(),
                linters = LinterInfo(),
                tests = TestInfo(),
                structure = StructureInfo(),
            )
        assertNotNull(model.stack)
        assertNotNull(model.codeStyle)
        assertNotNull(model.linters)
        assertNotNull(model.tests)
        assertNotNull(model.structure)
    }

    @Test
    fun `fully-populated model round-trips all fields via data class equality`() {
        val stack =
            StackInfo(
                dependencies = listOf(Dependency("org.junit.jupiter", "junit-jupiter", "5.11.4")),
                jdkVersion = "21",
                languageLevel = "21",
                buildSystem = BuildSystem.GRADLE,
            )
        val codeStyle =
            CodeStyleInfo(
                sources = listOf(StyleSource(StyleSourceType.EDITOR_CONFIG, ".editorconfig")),
            )
        val linters =
            LinterInfo(
                activeRules = listOf(ActiveRule("MaxLineLength", "detekt", RuleSeverity.WARNING, false)),
            )
        val tests =
            TestInfo(
                frameworks = listOf(TestFramework("JUnit", "5.11.4")),
                sourceRoots = listOf("src/test/kotlin"),
                namingSuffixes = listOf("Test"),
                coverageThreshold = 80.0,
            )
        val structure =
            StructureInfo(
                modules = listOf(Module("app")),
                rootPackages = listOf("dev.zahaand.projectscan"),
                packageSegments = listOf("dev.zahaand.projectscan.model"),
            )
        val model = ProjectScanModel(stack, codeStyle, linters, tests, structure)
        assertEquals(stack, model.stack)
        assertEquals(codeStyle, model.codeStyle)
        assertEquals(linters, model.linters)
        assertEquals(tests, model.tests)
        assertEquals(structure, model.structure)
    }

    @Test
    fun `copy with replaced stack produces new instance with all other sections unchanged`() {
        val original =
            ProjectScanModel(
                stack = StackInfo(jdkVersion = "17"),
                codeStyle = CodeStyleInfo(sources = listOf(StyleSource(StyleSourceType.CHECKSTYLE, "checkstyle.xml"))),
                linters = LinterInfo(),
                tests = TestInfo(coverageThreshold = 75.0),
                structure = StructureInfo(rootPackages = listOf("dev.zahaand")),
            )
        val updated = original.copy(stack = StackInfo())
        assertEquals(StackInfo(), updated.stack)
        assertEquals(original.codeStyle, updated.codeStyle)
        assertEquals(original.linters, updated.linters)
        assertEquals(original.tests, updated.tests)
        assertEquals(original.structure, updated.structure)
        assertNotSame(original, updated)
    }
}
