package dev.zahaand.projectscan.shared

import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.Module
import dev.zahaand.projectscan.model.TestFramework
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OutputFormattersTest {

    @Test
    fun `groupDependencies - multi-artifact uniform group sets sharedVersion`() {
        val deps = listOf(
            Dependency("org.spring", "core", "6.1.4"),
            Dependency("org.spring", "web", "6.1.4"),
            Dependency("org.spring", "context", "6.1.4"),
        )
        val groups = groupDependencies(deps)
        assertEquals(1, groups.size)
        assertEquals("6.1.4", groups[0].sharedVersion)
        assertEquals(3, groups[0].artifacts.size)
    }

    @Test
    fun `groupDependencies - single-artifact group sharedVersion is null`() {
        val deps = listOf(Dependency("org.spring", "core", "6.1.4"))
        val groups = groupDependencies(deps)
        assertEquals(1, groups.size)
        assertNull(groups[0].sharedVersion)
    }

    @Test
    fun `groupDependencies - mixed non-null versions sharedVersion is null`() {
        val deps = listOf(
            Dependency("org.spring", "core", "6.1.4"),
            Dependency("org.spring", "web", "6.1.3"),
        )
        val groups = groupDependencies(deps)
        assertNull(groups[0].sharedVersion)
    }

    @Test
    fun `groupDependencies - any null version in group yields sharedVersion null`() {
        val deps = listOf(
            Dependency("org.spring", "core", "6.1.4"),
            Dependency("org.spring", "web", null),
        )
        val groups = groupDependencies(deps)
        assertNull(groups[0].sharedVersion)
    }

    @Test
    fun `groupDependencies - output sorted lexicographically by groupId`() {
        val deps = listOf(
            Dependency("org.zzz", "a", "1.0"),
            Dependency("com.aaa", "b", "2.0"),
            Dependency("org.mmm", "c", "3.0"),
        )
        val groups = groupDependencies(deps)
        assertEquals(listOf("com.aaa", "org.mmm", "org.zzz"), groups.map { it.groupId })
    }

    @Test
    fun `detectVersionDiscrepancies - two modules with differing non-null versions returns discrepancy`() {
        val modules = listOf(
            Module("api", listOf(Dependency("org.mapstruct", "mapstruct", "1.5.5"))),
            Module("core", listOf(Dependency("org.mapstruct", "mapstruct", "1.6.0"))),
        )
        val discrepancies = detectVersionDiscrepancies(modules)
        assertEquals(1, discrepancies.size)
        assertEquals("org.mapstruct", discrepancies[0].groupId)
        assertEquals("mapstruct", discrepancies[0].artifactId)
        assertEquals(mapOf("api" to "1.5.5", "core" to "1.6.0"), discrepancies[0].versions)
    }

    @Test
    fun `detectVersionDiscrepancies - same version in both modules produces no entry`() {
        val modules = listOf(
            Module("api", listOf(Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.0"))),
            Module("core", listOf(Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.0"))),
        )
        val discrepancies = detectVersionDiscrepancies(modules)
        assertEquals(0, discrepancies.size)
    }

    @Test
    fun `detectVersionDiscrepancies - artifact in only one module produces no entry`() {
        val modules = listOf(
            Module("api", listOf(Dependency("org.example", "lib", "1.0"))),
            Module("core", emptyList()),
        )
        val discrepancies = detectVersionDiscrepancies(modules)
        assertEquals(0, discrepancies.size)
    }

    @Test
    fun `detectVersionDiscrepancies - null resolvedVersion excluded from detection`() {
        val modules = listOf(
            Module("api", listOf(Dependency("org.example", "lib", null))),
            Module("core", listOf(Dependency("org.example", "lib", "2.0"))),
        )
        val discrepancies = detectVersionDiscrepancies(modules)
        assertEquals(0, discrepancies.size)
    }

    @Test
    fun `detectVersionDiscrepancies - intra-module duplicate uses last declared version`() {
        val modules = listOf(
            Module(
                "api",
                listOf(
                    Dependency("org.example", "lib", "1.0"),
                    Dependency("org.example", "lib", "1.1"),
                ),
            ),
            Module("core", listOf(Dependency("org.example", "lib", "2.0"))),
        )
        val discrepancies = detectVersionDiscrepancies(modules)
        assertEquals(1, discrepancies.size)
        assertEquals("1.1", discrepancies[0].versions["api"])
    }

    @Test
    fun `detectVersionDiscrepancies - output sorted by groupId then artifactId`() {
        val modules = listOf(
            Module(
                "api",
                listOf(
                    Dependency("org.zzz", "b", "1.0"),
                    Dependency("com.aaa", "a", "1.0"),
                ),
            ),
            Module(
                "core",
                listOf(
                    Dependency("org.zzz", "b", "2.0"),
                    Dependency("com.aaa", "a", "2.0"),
                ),
            ),
        )
        val discrepancies = detectVersionDiscrepancies(modules)
        assertEquals(2, discrepancies.size)
        assertEquals("com.aaa", discrepancies[0].groupId)
        assertEquals("org.zzz", discrepancies[1].groupId)
    }

    @Test
    fun `detectVersionDiscrepancies - module names in versions map sorted lexicographically`() {
        val modules = listOf(
            Module("zzz", listOf(Dependency("org.example", "lib", "1.0"))),
            Module("aaa", listOf(Dependency("org.example", "lib", "2.0"))),
        )
        val discrepancies = detectVersionDiscrepancies(modules)
        assertEquals(1, discrepancies.size)
        assertEquals(listOf("aaa", "zzz"), discrepancies[0].versions.keys.toList())
    }

    @Test
    fun `deduplicateFrameworks - 80 identical entries collapse to exactly 1`() {
        val frameworks = List(80) { TestFramework("JUnit Jupiter", "5.10.2") }
        val result = deduplicateFrameworks(frameworks)
        assertEquals(1, result.size)
        assertEquals("JUnit Jupiter", result[0].name)
        assertEquals("5.10.2", result[0].version)
    }

    @Test
    fun `deduplicateFrameworks - 2 distinct frameworks returns 2 results`() {
        val frameworks = listOf(
            TestFramework("JUnit Jupiter", "5.10.2"),
            TestFramework("Mockito", "5.0.0"),
        )
        val result = deduplicateFrameworks(frameworks)
        assertEquals(2, result.size)
    }

    @Test
    fun `deduplicateFrameworks - first-occurrence order preserved`() {
        val frameworks = listOf(
            TestFramework("Mockito", "5.0.0"),
            TestFramework("JUnit Jupiter", "5.10.2"),
            TestFramework("Mockito", "5.0.0"),
        )
        val result = deduplicateFrameworks(frameworks)
        assertEquals(2, result.size)
        assertEquals("Mockito", result[0].name)
        assertEquals("JUnit Jupiter", result[1].name)
    }

    @Test
    fun `normalizeSourceRoots - absolute paths with shared prefix produce relative template with count`() {
        val roots = listOf(
            "/home/ci/workspace/myapp/api/src/test/java",
            "/home/ci/workspace/myapp/core/src/test/java",
        )
        val result = normalizeSourceRoots(roots)
        assertEquals(1, result.size)
        assertEquals("src/test/java", result[0].relativePath)
        assertEquals(2, result[0].count)
    }

    @Test
    fun `normalizeSourceRoots - all-relative inputs used as-is`() {
        val roots = listOf("src/test/java", "src/test/kotlin")
        val result = normalizeSourceRoots(roots)
        assertEquals(2, result.size)
        assertEquals("src/test/java", result.first { it.relativePath == "src/test/java" }.relativePath)
        assertEquals("src/test/kotlin", result.first { it.relativePath == "src/test/kotlin" }.relativePath)
    }

    @Test
    fun `normalizeSourceRoots - mixed absolute and relative - LCP only over absolute entries`() {
        val roots = listOf(
            "/home/ci/workspace/myapp/api/src/test/java",
            "/home/ci/workspace/myapp/core/src/test/java",
            "src/test/kotlin",
        )
        val result = normalizeSourceRoots(roots)
        assertEquals(2, result.size)
        val paths = result.map { it.relativePath }.toSet()
        assert("src/test/java" in paths) { "Expected src/test/java in $paths" }
        assert("src/test/kotlin" in paths) { "Expected src/test/kotlin in $paths" }
    }

    @Test
    fun `normalizeSourceRoots - empty input returns empty result`() {
        val result = normalizeSourceRoots(emptyList())
        assertEquals(0, result.size)
    }

    @Test
    fun `normalizeSourceRoots - output sorted by relativePath`() {
        val roots = listOf("src/test/kotlin", "src/test/java", "src/main/resources")
        val result = normalizeSourceRoots(roots)
        assertEquals(3, result.size)
        assertEquals("src/main/resources", result[0].relativePath)
        assertEquals("src/test/java", result[1].relativePath)
        assertEquals("src/test/kotlin", result[2].relativePath)
    }

    @Test
    fun `normalizeSourceRoots - count reflects raw entry occurrences`() {
        val roots = List(80) { "/home/ci/workspace/module-$it/src/test/java" }
        val result = normalizeSourceRoots(roots)
        assertEquals(1, result.size)
        assertEquals(80, result[0].count)
    }
}
