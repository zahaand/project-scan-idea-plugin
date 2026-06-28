package dev.zahaand.projectscan.shared

import dev.zahaand.projectscan.model.BuildSystem
import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.Module
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OutputFormattersTest {
    // === buildInvertedTechStack ===

    @Test
    fun `buildInvertedTechStack - empty modules produces empty stack`() {
        val stack = buildInvertedTechStack(emptyList(), emptySet())
        assertTrue(stack.entries.isEmpty())
    }

    @Test
    fun `buildInvertedTechStack - single module single dep produces one entry`() {
        val modules =
            listOf(
                Module("api", listOf(Dependency("org.spring", "core", "6.1.4"))),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        assertEquals(1, stack.entries.size)
        assertEquals("org.spring:core", stack.entries[0].coordinate)
        assertEquals("6.1.4", stack.entries[0].versions[0].version)
        assertTrue(stack.entries[0].versions[0].isUniform)
        assertEquals(1, stack.entries[0].versions[0].uniformModuleCount)
    }

    @Test
    fun `buildInvertedTechStack - dep with null resolvedVersion is excluded`() {
        val modules =
            listOf(
                Module("api", listOf(Dependency("org.spring", "core", null))),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        assertTrue(stack.entries.isEmpty())
    }

    @Test
    fun `buildInvertedTechStack - artifactId matching internal module name is excluded`() {
        val modules =
            listOf(
                Module(
                    "api",
                    listOf(
                        Dependency("com.example", "shared", "1.0"),
                        Dependency("org.spring", "core", "6.1.4"),
                    ),
                ),
            )
        val stack = buildInvertedTechStack(modules, setOf("shared"))
        assertEquals(1, stack.entries.size)
        assertEquals("org.spring:core", stack.entries[0].coordinate)
    }

    @Test
    fun `buildInvertedTechStack - coordinate matching internal module name is excluded`() {
        val modules =
            listOf(
                Module(
                    "api",
                    listOf(
                        Dependency("com.example", "shared", "1.0"),
                        Dependency("org.spring", "core", "6.1.4"),
                    ),
                ),
            )
        val stack = buildInvertedTechStack(modules, setOf("com.example:shared"))
        assertEquals(1, stack.entries.size)
        assertEquals("org.spring:core", stack.entries[0].coordinate)
    }

    @Test
    fun `buildInvertedTechStack - same dep in two modules at same version is uniform with count 2`() {
        val dep = Dependency("org.spring", "core", "6.1.4")
        val modules =
            listOf(
                Module("api", listOf(dep)),
                Module("core", listOf(dep)),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        assertEquals(1, stack.entries.size)
        val ve = stack.entries[0].versions[0]
        assertTrue(ve.isUniform)
        assertEquals(2, ve.uniformModuleCount)
    }

    @Test
    fun `buildInvertedTechStack - same dep in two modules at different versions is multi-version`() {
        val modules =
            listOf(
                Module("api", listOf(Dependency("org.spring", "core", "6.1.4"))),
                Module("svc", listOf(Dependency("org.spring", "core", "6.0.0"))),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        assertEquals(1, stack.entries.size)
        val entry = stack.entries[0]
        assertEquals(2, entry.versions.size)
        assertTrue(entry.versions.none { it.isUniform })
    }

    @Test
    fun `buildInvertedTechStack - entries sorted alphabetically by coordinate`() {
        val modules =
            listOf(
                Module(
                    "api",
                    listOf(
                        Dependency("org.zzz", "z", "1.0"),
                        Dependency("com.aaa", "a", "1.0"),
                        Dependency("org.mmm", "m", "1.0"),
                    ),
                ),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        assertEquals(listOf("com.aaa:a", "org.mmm:m", "org.zzz:z"), stack.entries.map { it.coordinate })
    }

    @Test
    fun `buildInvertedTechStack - multi-version named aggregator groups aggregated correctly`() {
        val modules =
            listOf(
                Module("api", listOf(Dependency("org.spring", "core", "6.1.4")), aggregator = "parent"),
                Module("svc", listOf(Dependency("org.spring", "core", "6.0.0")), aggregator = "parent"),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        val entry = stack.entries[0]
        for (ve in entry.versions) {
            assertTrue(ve.groups.isNotEmpty())
            val group = ve.groups[0]
            assertEquals("parent", group.aggregator)
        }
    }

    @Test
    fun `buildInvertedTechStack - null aggregator module goes to null-aggregator group`() {
        val modules =
            listOf(
                Module("api", listOf(Dependency("org.spring", "core", "6.1.4"))),
                Module("svc", listOf(Dependency("org.spring", "core", "6.0.0"))),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        val entry = stack.entries[0]
        for (ve in entry.versions) {
            assertTrue(ve.groups.any { it.aggregator == null })
        }
    }

    // T039 (b): named aggregators alphabetical, null-aggregator group last, modules alphabetical within group
    @Test
    fun `buildInvertedTechStack - multi-version named aggregators are sorted alphabetically`() {
        val modules =
            listOf(
                Module("api", listOf(Dependency("org.spring", "core", "6.1.4")), aggregator = "zeta-agg"),
                Module("svc", listOf(Dependency("org.spring", "core", "6.1.4")), aggregator = "alpha-agg"),
                Module("web", listOf(Dependency("org.spring", "core", "6.0.0")), aggregator = "beta-agg"),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        val entry = stack.entries[0]
        val v614 = entry.versions.first { it.version == "6.1.4" }
        val aggNames = v614.groups.mapNotNull { it.aggregator }
        assertEquals(listOf("alpha-agg", "zeta-agg"), aggNames)
    }

    @Test
    fun `buildInvertedTechStack - null-aggregator group appears last in multi-version entry`() {
        val modules =
            listOf(
                Module("api", listOf(Dependency("org.spring", "core", "6.1.4")), aggregator = "alpha-agg"),
                Module("root", listOf(Dependency("org.spring", "core", "6.1.4")), aggregator = null),
                Module("svc", listOf(Dependency("org.spring", "core", "6.0.0")), aggregator = null),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        val entry = stack.entries[0]
        val v614 = entry.versions.first { it.version == "6.1.4" }
        assertTrue(v614.groups.last().aggregator == null, "null-aggregator group must be last")
    }

    @Test
    fun `buildInvertedTechStack - module names within an aggregator group are sorted alphabetically`() {
        val dep = Dependency("org.spring", "core", "6.1.4")
        val modules =
            listOf(
                Module("z-module", listOf(dep), aggregator = "parent"),
                Module("a-module", listOf(dep), aggregator = "parent"),
                Module("m-module", listOf(dep), aggregator = "parent"),
                Module("other", listOf(Dependency("org.spring", "core", "6.0.0")), aggregator = "parent"),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        val entry = stack.entries[0]
        val v614 = entry.versions.first { it.version == "6.1.4" }
        val group = v614.groups.first { it.aggregator == "parent" }
        assertEquals(listOf("a-module", "m-module", "z-module"), group.moduleNames)
    }

    // T039 (e): SC-007 — dependency with version resolved from parent shows that version, not blank
    @Test
    fun `buildInvertedTechStack - SC-007 dep with resolved-from-parent version appears with that version`() {
        // resolvedVersion is non-null (resolved via parent/BOM); no explicit version on the declaring module
        val modules =
            listOf(
                Module("api", listOf(Dependency("org.springframework", "spring-core", "5.3.39"))),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        assertEquals(1, stack.entries.size)
        val entry = stack.entries[0]
        assertEquals("org.springframework:spring-core", entry.coordinate)
        assertEquals("5.3.39", entry.versions[0].version)
        assertTrue(entry.versions[0].isUniform)
    }

    // === renderInvertedTechStack ===

    @Test
    fun `renderInvertedTechStack - empty stack and all null preamble returns not detected`() {
        val result = renderInvertedTechStack(InvertedTechStack(emptyList()), null, null, null)
        assertEquals("not detected", result)
    }

    @Test
    fun `renderInvertedTechStack - empty stack with preamble returns preamble only`() {
        val result = renderInvertedTechStack(InvertedTechStack(emptyList()), BuildSystem.MAVEN, null, null)
        assertTrue(result.contains("Build System: MAVEN"))
        assertTrue(!result.contains("not detected"))
    }

    @Test
    fun `renderInvertedTechStack - uniform single-version entry renders coordinate-version-count format`() {
        val stack =
            buildInvertedTechStack(
                listOf(Module("api", listOf(Dependency("org.spring", "core", "6.1.4")))),
                emptySet(),
            )
        val result = renderInvertedTechStack(stack, null, null, null)
        assertTrue(result.contains("- org.spring:core:6.1.4 [1 modules]"), "Actual: $result")
    }

    @Test
    fun `renderInvertedTechStack - uniform two-module entry shows count 2`() {
        val dep = Dependency("org.spring", "core", "6.1.4")
        val stack =
            buildInvertedTechStack(
                listOf(Module("api", listOf(dep)), Module("svc", listOf(dep))),
                emptySet(),
            )
        val result = renderInvertedTechStack(stack, null, null, null)
        assertTrue(result.contains("- org.spring:core:6.1.4 [2 modules]"), "Actual: $result")
    }

    @Test
    fun `renderInvertedTechStack - multi-version entry renders coordinate header then indented version lines`() {
        val stack =
            buildInvertedTechStack(
                listOf(
                    Module("api", listOf(Dependency("org.spring", "core", "6.1.4"))),
                    Module("svc", listOf(Dependency("org.spring", "core", "6.0.0"))),
                ),
                emptySet(),
            )
        val result = renderInvertedTechStack(stack, null, null, null)
        val lines = result.lines()
        val headerIdx = lines.indexOfFirst { it == "- org.spring:core" }
        assertTrue(headerIdx >= 0, "Header line expected. Actual:\n$result")
        val indented = lines.drop(headerIdx + 1).filter { it.startsWith("  - ") }
        assertTrue(indented.isNotEmpty(), "Expected indented version lines. Actual:\n$result")
    }

    @Test
    fun `renderInvertedTechStack - named aggregator group rendered with aggregator prefix`() {
        val stack =
            buildInvertedTechStack(
                listOf(
                    Module("api", listOf(Dependency("org.spring", "core", "6.1.4")), aggregator = "parent"),
                    Module("svc", listOf(Dependency("org.spring", "core", "6.0.0")), aggregator = "parent"),
                ),
                emptySet(),
            )
        val result = renderInvertedTechStack(stack, null, null, null)
        assertTrue(result.contains("parent: "), "Expected 'parent: ' in output. Actual:\n$result")
    }

    @Test
    fun `renderInvertedTechStack - null aggregator group renders version then module list without prefix`() {
        val stack =
            buildInvertedTechStack(
                listOf(
                    Module("api", listOf(Dependency("org.spring", "core", "6.1.4"))),
                    Module("svc", listOf(Dependency("org.spring", "core", "6.0.0"))),
                ),
                emptySet(),
            )
        val result = renderInvertedTechStack(stack, null, null, null)
        val versionLines = result.lines().filter { it.trimStart().startsWith("- 6.") }
        assertTrue(versionLines.isNotEmpty(), "Expected version lines. Actual:\n$result")
    }

    @Test
    fun `renderInvertedTechStack - all preamble fields rendered when non-null`() {
        val stack = InvertedTechStack(emptyList())
        val result = renderInvertedTechStack(stack, BuildSystem.GRADLE, "21", "21")
        assertTrue(result.contains("Build System: GRADLE"), "Actual: $result")
        assertTrue(result.contains("JDK Version: 21"), "Actual: $result")
        assertTrue(result.contains("Language Level: 21"), "Actual: $result")
    }

    @Test
    fun `renderInvertedTechStack - preamble appears before dependency entries`() {
        val stack =
            buildInvertedTechStack(
                listOf(Module("api", listOf(Dependency("org.spring", "core", "6.1.4")))),
                emptySet(),
            )
        val result = renderInvertedTechStack(stack, BuildSystem.MAVEN, "17", null)
        val lines = result.lines()
        val buildSystemIdx = lines.indexOfFirst { it.contains("Build System") }
        val depIdx = lines.indexOfFirst { it.contains("org.spring:core") }
        assertTrue(buildSystemIdx >= 0 && depIdx >= 0, "Both preamble and dep expected. Actual:\n$result")
        assertTrue(buildSystemIdx < depIdx, "Preamble must appear before deps. Actual:\n$result")
    }

    // === normalizeSourceRoots ===

    @Test
    fun `normalizeSourceRoots - build-output target paths filtered out`() {
        val roots =
            listOf(
                "/project/api/src/test/java",
                "/project/api/target/generated-test-sources/test-annotations",
            )
        val result = normalizeSourceRoots(roots)
        assertEquals(1, result.size)
        assertEquals("src/test/java", result[0].relativePath)
    }

    @Test
    fun `normalizeSourceRoots - build-output gradle build paths filtered out`() {
        val roots =
            listOf(
                "/project/api/src/test/java",
                "/project/api/build/generated-test-sources",
            )
        val result = normalizeSourceRoots(roots)
        assertEquals(1, result.size)
        assertEquals("src/test/java", result[0].relativePath)
    }

    @Test
    fun `normalizeSourceRoots - absolute paths ending with recognized suffix produce that suffix`() {
        val roots =
            listOf(
                "/home/ci/workspace/myapp/api/src/test/java",
                "/home/ci/workspace/myapp/core/src/test/java",
            )
        val result = normalizeSourceRoots(roots)
        assertEquals(1, result.size)
        assertEquals("src/test/java", result[0].relativePath)
    }

    @Test
    fun `normalizeSourceRoots - two distinct recognized suffixes produce two templates`() {
        val roots =
            listOf(
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
    fun `normalizeSourceRoots - all paths filtered out by build-output yields empty result`() {
        val roots =
            listOf(
                "/project/api/target/generated-test-sources/annotations",
                "/project/core/build/generated",
            )
        val result = normalizeSourceRoots(roots)
        assertEquals(0, result.size)
    }
}
