package dev.zahaand.projectscan.shared

import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.Module
import dev.zahaand.projectscan.model.TestFramework
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class OutputFormattersTest {

    // === filterInternalDependencies ===

    @Test
    fun `filterInternalDependencies - empty module set keeps all deps`() {
        val deps = listOf(
            Dependency("org.spring", "core", "6.1.4"),
            Dependency("com.example", "my-module", "1.0"),
        )
        assertEquals(2, filterInternalDependencies(deps, emptySet()).size)
    }

    @Test
    fun `filterInternalDependencies - artifactId matching module name is excluded`() {
        val deps = listOf(
            Dependency("org.spring", "core", "6.1.4"),
            Dependency("com.example", "document-engine-spi", "1.0"),
        )
        val result = filterInternalDependencies(deps, setOf("document-engine-spi"))
        assertEquals(1, result.size)
        assertEquals("core", result[0].artifactId)
    }

    @Test
    fun `filterInternalDependencies - non-matching artifactId is kept`() {
        val deps = listOf(Dependency("org.spring", "core", "6.1.4"))
        val result = filterInternalDependencies(deps, setOf("other-module"))
        assertEquals(1, result.size)
    }

    @Test
    fun `filterInternalDependencies - multiple internal modules excluded`() {
        val deps = listOf(
            Dependency("com.example", "api", "1.0"),
            Dependency("com.example", "core", "1.0"),
            Dependency("org.spring", "spring-web", "6.0"),
        )
        val result = filterInternalDependencies(deps, setOf("api", "core"))
        assertEquals(1, result.size)
        assertEquals("spring-web", result[0].artifactId)
    }

    // === groupDependencies ===

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

    // === detectVersionDiscrepancies ===

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

    // === renderVersionDiscrepancyLine ===

    @Test
    fun `renderVersionDiscrepancyLine - majority version is dominant`() {
        val d = VersionDiscrepancy(
            groupId = "org.mapstruct",
            artifactId = "mapstruct",
            versions = mapOf("api" to "1.6.0", "core" to "1.6.0", "svc" to "1.5.5"),
        )
        val line = renderVersionDiscrepancyLine(d)
        assertTrue(line.startsWith("org.mapstruct:mapstruct → mostly 1.6.0, except {"))
        assertTrue(line.contains("svc: 1.5.5"))
        assertFalse(line.contains("api: "))
        assertFalse(line.contains("core: "))
    }

    @Test
    fun `renderVersionDiscrepancyLine - tie broken by lexicographically smaller version`() {
        val d = VersionDiscrepancy(
            groupId = "org.ex",
            artifactId = "lib",
            versions = mapOf("api" to "1.5.5", "core" to "1.6.0"),
        )
        val line = renderVersionDiscrepancyLine(d)
        // 1.5.5 < 1.6.0 lexicographically, so 1.5.5 is dominant
        assertTrue(line.contains("mostly 1.5.5"), "Smaller version is dominant on tie: $line")
        assertTrue(line.contains("core: 1.6.0"), "Exception is core: $line")
    }

    @Test
    fun `renderVersionDiscrepancyLine - exceptions sorted by module name`() {
        // "1.0" used by 2 modules (zzz, bbb), "2.0" used by 2 modules (aaa, mmm) — tie
        // dominant = "1.0" (lexicographically smaller); exceptions = {aaa: 2.0, mmm: 2.0}
        val d = VersionDiscrepancy(
            groupId = "org.ex",
            artifactId = "lib",
            versions = mapOf("zzz" to "1.0", "aaa" to "2.0", "mmm" to "2.0", "bbb" to "1.0"),
        )
        val line = renderVersionDiscrepancyLine(d)
        val exceptIdx = line.indexOf("except {")
        val exceptPart = line.substring(exceptIdx)
        val aaaIdx = exceptPart.indexOf("aaa")
        val mmmIdx = exceptPart.indexOf("mmm")
        assertTrue(aaaIdx >= 0 && mmmIdx >= 0, "Both exception modules present: $line")
        assertTrue(aaaIdx < mmmIdx, "aaa must appear before mmm: $line")
    }

    // === deduplicateFrameworks ===

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
    fun `deduplicateFrameworks - null version is distinct from non-null version with same name`() {
        val frameworks = listOf(
            TestFramework("JUnit Jupiter", null),
            TestFramework("JUnit Jupiter", "5.10.2"),
        )
        val result = deduplicateFrameworks(frameworks)
        assertEquals(2, result.size)
        assertEquals(null, result[0].version)
        assertEquals("5.10.2", result[1].version)
    }

    // === normalizeSourceRoots ===

    @Test
    fun `normalizeSourceRoots - build-output target paths filtered out`() {
        val roots = listOf(
            "/project/api/src/test/java",
            "/project/api/target/generated-test-sources/test-annotations",
        )
        val result = normalizeSourceRoots(roots)
        assertEquals(1, result.size)
        assertEquals("src/test/java", result[0].relativePath)
    }

    @Test
    fun `normalizeSourceRoots - build-output gradle build paths filtered out`() {
        val roots = listOf(
            "/project/api/src/test/java",
            "/project/api/build/generated-test-sources",
        )
        val result = normalizeSourceRoots(roots)
        assertEquals(1, result.size)
        assertEquals("src/test/java", result[0].relativePath)
    }

    @Test
    fun `normalizeSourceRoots - absolute paths ending with recognized suffix produce that suffix`() {
        val roots = listOf(
            "/home/ci/workspace/myapp/api/src/test/java",
            "/home/ci/workspace/myapp/core/src/test/java",
        )
        val result = normalizeSourceRoots(roots)
        assertEquals(1, result.size)
        assertEquals("src/test/java", result[0].relativePath)
    }

    @Test
    fun `normalizeSourceRoots - two distinct recognized suffixes produce two templates`() {
        val roots = listOf(
            "/home/ci/workspace/myapp/api/src/test/java",
            "/home/ci/workspace/myapp/core/src/test/kotlin",
        )
        val result = normalizeSourceRoots(roots)
        assertEquals(2, result.size)
        val paths = result.map { it.relativePath }.toSet()
        assertTrue("src/test/java" in paths)
        assertTrue("src/test/kotlin" in paths)
    }

    @Test
    fun `normalizeSourceRoots - many paths with same recognized suffix produce one template`() {
        val roots = List(80) { "/home/ci/workspace/module-$it/src/test/java" }
        val result = normalizeSourceRoots(roots)
        assertEquals(1, result.size)
        assertEquals("src/test/java", result[0].relativePath)
    }

    @Test
    fun `normalizeSourceRoots - all-relative inputs used as-is when recognized`() {
        val roots = listOf("src/test/java", "src/test/kotlin")
        val result = normalizeSourceRoots(roots)
        assertEquals(2, result.size)
        assertEquals("src/test/java", result.first { it.relativePath == "src/test/java" }.relativePath)
        assertEquals("src/test/kotlin", result.first { it.relativePath == "src/test/kotlin" }.relativePath)
    }

    @Test
    fun `normalizeSourceRoots - mixed absolute and relative - recognized suffixes extracted`() {
        val roots = listOf(
            "/home/ci/workspace/myapp/api/src/test/java",
            "/home/ci/workspace/myapp/core/src/test/java",
            "src/test/kotlin",
        )
        val result = normalizeSourceRoots(roots)
        assertEquals(2, result.size)
        val paths = result.map { it.relativePath }.toSet()
        assertTrue("src/test/java" in paths) { "Expected src/test/java in $paths" }
        assertTrue("src/test/kotlin" in paths) { "Expected src/test/kotlin in $paths" }
    }

    @Test
    fun `normalizeSourceRoots - empty input returns empty result`() {
        val result = normalizeSourceRoots(emptyList())
        assertEquals(0, result.size)
    }

    @Test
    fun `normalizeSourceRoots - output sorted by relativePath`() {
        val roots = listOf("src/test/kotlin", "src/test/java", "src/integrationTest/java")
        val result = normalizeSourceRoots(roots)
        assertEquals(3, result.size)
        assertEquals("src/integrationTest/java", result[0].relativePath)
        assertEquals("src/test/java", result[1].relativePath)
        assertEquals("src/test/kotlin", result[2].relativePath)
    }

    @Test
    fun `normalizeSourceRoots - single recognized source root`() {
        val roots = listOf("src/test/java")
        val result = normalizeSourceRoots(roots)
        assertEquals(1, result.size)
        assertEquals("src/test/java", result[0].relativePath)
    }

    @Test
    fun `normalizeSourceRoots - all paths filtered out by build-output yields empty result`() {
        val roots = listOf(
            "/project/api/target/generated-test-sources/annotations",
            "/project/core/build/generated",
        )
        val result = normalizeSourceRoots(roots)
        assertEquals(0, result.size)
    }
}
