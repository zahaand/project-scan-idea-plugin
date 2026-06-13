package dev.zahaand.projectscan.scan.collector

import dev.zahaand.projectscan.model.BuildSystem
import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.scan.fake.FakeBuildSystemPort
import dev.zahaand.projectscan.scan.fake.FakeDependencyPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StackCollectorTest {

    @Test
    fun `single-module project with 5 dependencies returns all in Ok`() {
        val deps = listOf(
            Dependency("org.junit.jupiter", "junit-jupiter", "5.11.4"),
            Dependency("org.mockito", "mockito-core", "5.12.0"),
            Dependency("org.assertj", "assertj-core", "3.26.3"),
            Dependency("org.springframework.boot", "spring-boot-starter", "3.4.0"),
            Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.18.1"),
        )
        val result = collector(
            buildSystem = BuildSystem.GRADLE,
            modules = mapOf("app" to deps),
        ).collect()
        val ok = assertOk(result)
        assertEquals(5, ok.dependencies.size)
        assertEquals(BuildSystem.GRADLE, ok.buildSystem)
    }

    @Test
    fun `multi-module same coordinate selects max version including non-semver`() {
        val result = collector(
            buildSystem = BuildSystem.MAVEN,
            modules = mapOf(
                "module-a" to listOf(Dependency("org.springframework", "spring-core", "1.0.0.RELEASE")),
                "module-b" to listOf(Dependency("org.springframework", "spring-core", "1.0.1")),
            ),
        ).collect()
        val ok = assertOk(result)
        val dep = ok.dependencies.single { it.artifactId == "spring-core" }
        assertEquals("1.0.1", dep.resolvedVersion)
    }

    @Test
    fun `zero-dependency project with build system returns Ok with empty dependencies`() {
        val result = collector(buildSystem = BuildSystem.GRADLE, modules = emptyMap()).collect()
        val ok = assertOk(result)
        assertTrue(ok.dependencies.isEmpty())
    }

    @Test
    fun `no build system and no modules returns Empty`() {
        val result = collector(buildSystem = null, modules = emptyMap()).collect()
        assertTrue(result is SectionResult.Empty, "Expected Empty but got $result")
    }

    @Test
    fun `max language level aggregation across modules with differing levels`() {
        val result = collector(
            buildSystem = BuildSystem.GRADLE,
            modules = emptyMap(),
            moduleLevels = mapOf("core" to "11", "api" to "17", "web" to "21"),
        ).collect()
        val ok = assertOk(result)
        assertEquals("21", ok.languageLevel)
    }

    @Test
    fun `no language levels produces null languageLevel`() {
        val result = collector(buildSystem = BuildSystem.GRADLE, modules = emptyMap()).collect()
        val ok = assertOk(result)
        assertNull(ok.languageLevel)
    }

    @Test
    fun `dependency with null version is preserved in output`() {
        val result = collector(
            buildSystem = BuildSystem.GRADLE,
            modules = mapOf("app" to listOf(Dependency("com.example", "some-lib", null))),
        ).collect()
        val ok = assertOk(result)
        val dep = ok.dependencies.single()
        assertEquals("com.example", dep.groupId)
        assertEquals("some-lib", dep.artifactId)
        assertNull(dep.resolvedVersion)
    }

    @Test
    fun `null version and non-null version for same coordinate keeps non-null version`() {
        val result = collector(
            buildSystem = BuildSystem.GRADLE,
            modules = mapOf(
                "module-a" to listOf(Dependency("com.example", "lib", null)),
                "module-b" to listOf(Dependency("com.example", "lib", "2.0.0")),
            ),
        ).collect()
        val ok = assertOk(result)
        val dep = ok.dependencies.single()
        assertEquals("2.0.0", dep.resolvedVersion)
    }

    @Test
    fun `same coordinate in multiple modules deduplicated to single entry`() {
        val result = collector(
            buildSystem = BuildSystem.GRADLE,
            modules = mapOf(
                "module-a" to listOf(Dependency("org.slf4j", "slf4j-api", "2.0.0")),
                "module-b" to listOf(Dependency("org.slf4j", "slf4j-api", "2.0.0")),
            ),
        ).collect()
        val ok = assertOk(result)
        assertEquals(1, ok.dependencies.size)
    }

    @Test
    fun `identifies Maven build system`() {
        val result = collector(buildSystem = BuildSystem.MAVEN, modules = emptyMap()).collect()
        val ok = assertOk(result)
        assertEquals(BuildSystem.MAVEN, ok.buildSystem)
    }

    @Test
    fun `identifies Gradle build system`() {
        val result = collector(buildSystem = BuildSystem.GRADLE, modules = emptyMap()).collect()
        val ok = assertOk(result)
        assertEquals(BuildSystem.GRADLE, ok.buildSystem)
    }

    // --- helpers ---

    private fun collector(
        buildSystem: BuildSystem?,
        modules: Map<String, List<Dependency>>,
        moduleLevels: Map<String, String> = emptyMap(),
    ) = StackCollector(
        FakeBuildSystemPort(buildSystem, moduleLevels),
        FakeDependencyPort(modules),
    )

    private fun assertOk(result: SectionResult<*>): dev.zahaand.projectscan.model.StackInfo {
        assertTrue(result is SectionResult.Ok<*>, "Expected Ok but got $result")
        @Suppress("UNCHECKED_CAST")
        return (result as SectionResult.Ok<dev.zahaand.projectscan.model.StackInfo>).data
    }
}
