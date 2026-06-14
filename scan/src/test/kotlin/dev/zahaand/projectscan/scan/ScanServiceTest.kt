package dev.zahaand.projectscan.scan

import dev.zahaand.projectscan.model.BuildSystem
import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.RuleSeverity
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StyleSource
import dev.zahaand.projectscan.model.StyleSourceType
import dev.zahaand.projectscan.scan.fake.FakeBuildSystemPort
import dev.zahaand.projectscan.scan.fake.FakeDependencyPort
import dev.zahaand.projectscan.scan.fake.FakeLinterConfigParser
import dev.zahaand.projectscan.scan.fake.FakeLinterPort
import dev.zahaand.projectscan.scan.fake.FakeModuleStructurePort
import dev.zahaand.projectscan.scan.fake.FakeStyleSourcePort
import dev.zahaand.projectscan.scan.fake.FakeTestInfoPort
import dev.zahaand.projectscan.scan.port.LinterPort
import dev.zahaand.projectscan.scan.port.LinterToolDescriptor
import dev.zahaand.projectscan.scan.port.ModuleDescriptor
import dev.zahaand.projectscan.scan.port.PackageTreeData
import dev.zahaand.projectscan.scan.port.ParsedRule
import dev.zahaand.projectscan.scan.port.StyleSourcePort
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class ScanServiceTest {
    private val configPath = "/config/checkstyle.xml"

    private fun checkstyleParsers() =
        mapOf(
            "checkstyle" to
                FakeLinterConfigParser(
                    mapOf(configPath to listOf(ParsedRule("WhitespaceAround", RuleSeverity.WARNING))),
                ),
        )

    private fun allSucceedService() =
        ScanService(
            buildSystemPort = FakeBuildSystemPort(BuildSystem.MAVEN, mapOf("app" to "17")),
            dependencyPort = FakeDependencyPort(mapOf("app" to listOf(Dependency("com.example", "lib", "1.0")))),
            styleSourcePort = FakeStyleSourcePort(listOf(StyleSource(StyleSourceType.EDITOR_CONFIG, ".editorconfig"))),
            linterPort = FakeLinterPort(listOf(LinterToolDescriptor("checkstyle", configPath, true))),
            linterConfigParsers = checkstyleParsers(),
            testInfoPort =
                FakeTestInfoPort(
                    testSourceRoots = listOf("src/test/java"),
                    testScopedDependencies = listOf(Dependency("org.junit.jupiter", "junit-jupiter", "5.10.0")),
                ),
            moduleStructurePort =
                FakeModuleStructurePort(
                    modules = listOf(ModuleDescriptor("app", emptyList(), emptyList(), listOf("src/main/java"), true)),
                    packageTree = PackageTreeData(listOf("com.example"), listOf("com.example.service")),
                ),
        )

    @Test
    fun `all five collectors succeed — all five sections are Ok (SC-001)`() {
        val result = allSucceedService().scan()

        assertInstanceOf(SectionResult.Ok::class.java, result.stack)
        assertInstanceOf(SectionResult.Ok::class.java, result.codeStyle)
        assertInstanceOf(SectionResult.Ok::class.java, result.linters)
        assertInstanceOf(SectionResult.Ok::class.java, result.tests)
        assertInstanceOf(SectionResult.Ok::class.java, result.structure)
    }

    @Test
    fun `one collector throws — that section is Error, all others remain Ok (FR-021)`() {
        val throwingStylePort =
            object : StyleSourcePort {
                override fun findStyleSources(): Nothing = throw RuntimeException("style source failure")
            }
        val service =
            ScanService(
                buildSystemPort = FakeBuildSystemPort(BuildSystem.MAVEN, mapOf("app" to "17")),
                dependencyPort = FakeDependencyPort(mapOf("app" to listOf(Dependency("com.example", "lib", "1.0")))),
                styleSourcePort = throwingStylePort,
                linterPort = FakeLinterPort(listOf(LinterToolDescriptor("checkstyle", configPath, true))),
                linterConfigParsers = checkstyleParsers(),
                testInfoPort =
                    FakeTestInfoPort(
                        testSourceRoots = listOf("src/test/java"),
                        testScopedDependencies = listOf(Dependency("org.junit.jupiter", "junit-jupiter", "5.10.0")),
                    ),
                moduleStructurePort =
                    FakeModuleStructurePort(
                        modules =
                            listOf(
                                ModuleDescriptor("app", emptyList(), emptyList(), listOf("src/main/java"), true),
                            ),
                        packageTree = PackageTreeData(listOf("com.example"), listOf("com.example.service")),
                    ),
            )

        val result = service.scan()

        assertInstanceOf(SectionResult.Ok::class.java, result.stack, "stack should be Ok")
        assertInstanceOf(SectionResult.Error::class.java, result.codeStyle, "codeStyle should be Error")
        assertInstanceOf(SectionResult.Ok::class.java, result.linters, "linters should be Ok")
        assertInstanceOf(SectionResult.Ok::class.java, result.tests, "tests should be Ok")
        assertInstanceOf(SectionResult.Ok::class.java, result.structure, "structure should be Ok")
    }

    @Test
    fun `one collector throws — error message is preserved in section`() {
        val throwingLinterPort =
            object : LinterPort {
                override fun getAppliedLinterTools(): Nothing = throw RuntimeException("linter boom")
            }
        val service =
            ScanService(
                buildSystemPort = FakeBuildSystemPort(BuildSystem.MAVEN),
                dependencyPort = FakeDependencyPort(),
                styleSourcePort = FakeStyleSourcePort(),
                linterPort = throwingLinterPort,
                linterConfigParsers = emptyMap(),
                testInfoPort = FakeTestInfoPort(),
                moduleStructurePort = FakeModuleStructurePort(emptyList(), PackageTreeData(emptyList(), emptyList())),
            )

        val result = service.scan()

        val error = result.linters as SectionResult.Error
        assert(error.cause == "linter boom") { "Expected cause 'linter boom' but got '${error.cause}'" }
    }

    @Test
    fun `empty project — all five sections are Empty`() {
        val service =
            ScanService(
                buildSystemPort = FakeBuildSystemPort(null),
                dependencyPort = FakeDependencyPort(),
                styleSourcePort = FakeStyleSourcePort(),
                linterPort = FakeLinterPort(),
                linterConfigParsers = emptyMap(),
                testInfoPort = FakeTestInfoPort(),
                moduleStructurePort = FakeModuleStructurePort(emptyList(), PackageTreeData(emptyList(), emptyList())),
            )

        val result = service.scan()

        assertInstanceOf(SectionResult.Empty::class.java, result.stack)
        assertInstanceOf(SectionResult.Empty::class.java, result.codeStyle)
        assertInstanceOf(SectionResult.Empty::class.java, result.linters)
        assertInstanceOf(SectionResult.Empty::class.java, result.tests)
        assertInstanceOf(SectionResult.Empty::class.java, result.structure)
    }

    @Test
    fun `partial failure in stack collector — build system reads, dep read fails — section is Ok not Error (CHK031)`() {
        val service =
            ScanService(
                buildSystemPort = FakeBuildSystemPort(BuildSystem.MAVEN, emptyMap()),
                dependencyPort = FakeDependencyPort(error = RuntimeException("dep read failed")),
                styleSourcePort =
                    FakeStyleSourcePort(
                        listOf(StyleSource(StyleSourceType.EDITOR_CONFIG, ".editorconfig")),
                    ),
                linterPort = FakeLinterPort(),
                linterConfigParsers = emptyMap(),
                testInfoPort = FakeTestInfoPort(testSourceRoots = listOf("src/test/java")),
                moduleStructurePort =
                    FakeModuleStructurePort(
                        modules = listOf(ModuleDescriptor("app", emptyList(), emptyList(), emptyList(), false)),
                        packageTree = PackageTreeData(emptyList(), emptyList()),
                    ),
            )

        val result = service.scan()

        val stack = result.stack
        assertInstanceOf(SectionResult.Ok::class.java, stack, "stack section must be Ok even when dep read fails")
        val stackData = (stack as SectionResult.Ok).data
        assert(stackData.dependencies.isEmpty()) { "Expected empty dependencies due to dep read failure" }
        assert(stackData.buildSystem == BuildSystem.MAVEN) { "Build system should still be populated" }
    }
}
