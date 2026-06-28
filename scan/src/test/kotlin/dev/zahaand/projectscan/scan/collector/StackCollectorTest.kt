package dev.zahaand.projectscan.scan.collector

import dev.zahaand.projectscan.model.BuildSystem
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.scan.fake.FakeBuildSystemPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StackCollectorTest {
    @Test
    fun `build system only produces Ok`() {
        val result = collector(buildSystem = BuildSystem.GRADLE).collect()
        val ok = assertOk(result)
        assertEquals(BuildSystem.GRADLE, ok.buildSystem)
    }

    @Test
    fun `no build system and no language levels returns Empty`() {
        val result = collector(buildSystem = null).collect()
        assertTrue(result is SectionResult.Empty, "Expected Empty but got $result")
    }

    @Test
    fun `max language level aggregation across modules with differing levels`() {
        val result =
            collector(
                buildSystem = BuildSystem.GRADLE,
                moduleLevels = mapOf("core" to "11", "api" to "17", "web" to "21"),
            ).collect()
        val ok = assertOk(result)
        assertEquals("21", ok.languageLevel)
    }

    @Test
    fun `no language levels produces null languageLevel`() {
        val result = collector(buildSystem = BuildSystem.GRADLE).collect()
        val ok = assertOk(result)
        assertNull(ok.languageLevel)
    }

    @Test
    fun `identifies Maven build system`() {
        val result = collector(buildSystem = BuildSystem.MAVEN).collect()
        val ok = assertOk(result)
        assertEquals(BuildSystem.MAVEN, ok.buildSystem)
    }

    @Test
    fun `identifies Gradle build system`() {
        val result = collector(buildSystem = BuildSystem.GRADLE).collect()
        val ok = assertOk(result)
        assertEquals(BuildSystem.GRADLE, ok.buildSystem)
    }

    @Test
    fun `jdkVersion from project SDK is propagated`() {
        val result =
            collector(
                buildSystem = BuildSystem.GRADLE,
                jdkVersion = "temurin-21",
            ).collect()
        val ok = assertOk(result)
        assertEquals("temurin-21", ok.jdkVersion)
    }

    @Test
    fun `jdkVersion is null when no SDK is configured`() {
        val result = collector(buildSystem = BuildSystem.GRADLE).collect()
        val ok = assertOk(result)
        assertNull(ok.jdkVersion)
    }

    // --- helpers ---

    private fun collector(
        buildSystem: BuildSystem?,
        moduleLevels: Map<String, String> = emptyMap(),
        jdkVersion: String? = null,
    ) = StackCollector(FakeBuildSystemPort(buildSystem, moduleLevels, jdkVersion))

    private fun assertOk(result: SectionResult<*>): dev.zahaand.projectscan.model.StackInfo {
        assertTrue(result is SectionResult.Ok<*>, "Expected Ok but got $result")
        @Suppress("UNCHECKED_CAST")
        return (result as SectionResult.Ok<dev.zahaand.projectscan.model.StackInfo>).data
    }
}
