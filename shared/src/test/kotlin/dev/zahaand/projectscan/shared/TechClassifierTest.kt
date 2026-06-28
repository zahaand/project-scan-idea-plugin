package dev.zahaand.projectscan.shared

import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.Module
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private fun makeConfig(
    denylist: Set<String> = emptySet(),
    artifactIdFamilies: Map<String, Map<String, String>> = emptyMap(),
    allowlist: List<AllowlistEntry> = emptyList(),
): TechConfig = TechConfig(denylist = denylist, artifactIdFamilies = artifactIdFamilies, allowlist = allowlist)

class TechClassifierTest {
    // === classify — denylist ===

    @Test
    fun `classify - denylisted coordinate is dropped entirely`() {
        val modules =
            listOf(Module("api", listOf(Dependency("org.jetbrains", "annotations", "24.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val config = makeConfig(denylist = setOf("org.jetbrains:annotations"))
        val result = classify(stack, config)
        assertTrue(result.technologies.isEmpty())
        assertTrue(result.others.isEmpty())
    }

    @Test
    fun `classify - non-denylisted coordinate is not dropped`() {
        val modules = listOf(Module("api", listOf(Dependency("org.spring", "core", "6.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val config = makeConfig(denylist = setOf("org.jetbrains:annotations"))
        val result = classify(stack, config)
        assertEquals(1, result.others.size)
    }

    // === classify — Spring artifactId sub-family routing ===

    @Test
    fun `classify - org-springframework spring-core routes to Spring Core family`() {
        val modules =
            listOf(Module("api", listOf(Dependency("org.springframework", "spring-core", "6.1.4"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val springMap =
            mapOf(
                "spring-core" to "Spring Core",
                "spring-beans" to "Spring Core",
                "spring-context" to "Spring Core",
            )
        val config = makeConfig(artifactIdFamilies = mapOf("org.springframework" to springMap))
        val result = classify(stack, config)
        assertEquals(1, result.technologies.size)
        assertEquals("Spring Core", result.technologies[0].name)
        assertEquals(listOf("6.1.4"), result.technologies[0].versions)
        assertTrue(result.others.isEmpty())
    }

    @Test
    fun `classify - org-springframework unknown artifactId goes to Other dependencies`() {
        val modules =
            listOf(Module("api", listOf(Dependency("org.springframework", "spring-unknown", "6.1.4"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val springMap = mapOf("spring-core" to "Spring Core")
        val config = makeConfig(artifactIdFamilies = mapOf("org.springframework" to springMap))
        val result = classify(stack, config)
        assertTrue(result.technologies.isEmpty())
        assertEquals(1, result.others.size)
        assertEquals("org.springframework:spring-unknown", result.others[0].coordinate)
    }

    @Test
    fun `classify - org-springframework-boot goes through allowlist not artifactId sub-family routing`() {
        val modules =
            listOf(Module("api", listOf(Dependency("org.springframework.boot", "spring-boot-starter", "3.2.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val springMap = mapOf("spring-core" to "Spring Core")
        val config =
            makeConfig(
                artifactIdFamilies = mapOf("org.springframework" to springMap),
                allowlist = listOf(AllowlistEntry("org.springframework.boot", "Spring Boot")),
            )
        val result = classify(stack, config)
        assertEquals(1, result.technologies.size)
        assertEquals("Spring Boot", result.technologies[0].name)
    }

    // === classify — Micrometer artifactId sub-family routing ===

    @Test
    fun `classify - micrometer-core routes to Micrometer Core sub-family`() {
        val modules =
            listOf(Module("api", listOf(Dependency("io.micrometer", "micrometer-core", "1.12.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val micrometerMap =
            mapOf(
                "micrometer-core" to "Micrometer Core",
                "micrometer-commons" to "Micrometer Core",
                "micrometer-registry-prometheus" to "Micrometer Registry",
                "micrometer-tracing" to "Micrometer Tracing",
            )
        val config = makeConfig(artifactIdFamilies = mapOf("io.micrometer" to micrometerMap))
        val result = classify(stack, config)
        assertEquals(1, result.technologies.size)
        assertEquals("Micrometer Core", result.technologies[0].name)
        assertEquals(listOf("1.12.0"), result.technologies[0].versions)
        assertTrue(result.others.isEmpty())
    }

    @Test
    fun `classify - micrometer-registry-prometheus routes to Micrometer Registry sub-family`() {
        val modules =
            listOf(Module("api", listOf(Dependency("io.micrometer", "micrometer-registry-prometheus", "1.12.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val micrometerMap =
            mapOf(
                "micrometer-core" to "Micrometer Core",
                "micrometer-registry-prometheus" to "Micrometer Registry",
            )
        val config = makeConfig(artifactIdFamilies = mapOf("io.micrometer" to micrometerMap))
        val result = classify(stack, config)
        assertEquals(1, result.technologies.size)
        assertEquals("Micrometer Registry", result.technologies[0].name)
    }

    @Test
    fun `classify - micrometer-tracing routes to Micrometer Tracing sub-family`() {
        val modules =
            listOf(Module("api", listOf(Dependency("io.micrometer", "micrometer-tracing", "1.2.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val micrometerMap =
            mapOf(
                "micrometer-tracing" to "Micrometer Tracing",
                "micrometer-tracing-bridge-brave" to "Micrometer Tracing",
            )
        val config = makeConfig(artifactIdFamilies = mapOf("io.micrometer" to micrometerMap))
        val result = classify(stack, config)
        assertEquals(1, result.technologies.size)
        assertEquals("Micrometer Tracing", result.technologies[0].name)
    }

    @Test
    fun `classify - unknown io-micrometer artifact goes to Other dependencies`() {
        val modules =
            listOf(Module("api", listOf(Dependency("io.micrometer", "micrometer-unknown", "1.12.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val micrometerMap = mapOf("micrometer-core" to "Micrometer Core")
        val config = makeConfig(artifactIdFamilies = mapOf("io.micrometer" to micrometerMap))
        val result = classify(stack, config)
        assertTrue(result.technologies.isEmpty())
        assertEquals(1, result.others.size)
        assertEquals("io.micrometer:micrometer-unknown", result.others[0].coordinate)
    }

    // === classify — allowlist prefix matching ===

    @Test
    fun `classify - exact groupId prefix match recognized`() {
        val modules =
            listOf(Module("api", listOf(Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val config = makeConfig(allowlist = listOf(AllowlistEntry("com.fasterxml.jackson", "Jackson")))
        val result = classify(stack, config)
        assertEquals(1, result.technologies.size)
        assertEquals("Jackson", result.technologies[0].name)
    }

    @Test
    fun `classify - unrecognized groupId goes to Other dependencies`() {
        val modules = listOf(Module("api", listOf(Dependency("com.example.unknown", "lib", "1.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val config = makeConfig(allowlist = listOf(AllowlistEntry("com.fasterxml.jackson", "Jackson")))
        val result = classify(stack, config)
        assertTrue(result.technologies.isEmpty())
        assertEquals(1, result.others.size)
        assertEquals("com.example.unknown:lib", result.others[0].coordinate)
    }

    @Test
    fun `classify - longest prefix wins when multiple prefixes match`() {
        val modules =
            listOf(Module("api", listOf(Dependency("org.springframework.boot", "spring-boot-starter", "3.2.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val allowlist =
            listOf(
                AllowlistEntry("org.springframework", "Spring Framework"),
                AllowlistEntry("org.springframework.boot", "Spring Boot"),
            )
        val config = makeConfig(allowlist = allowlist)
        val result = classify(stack, config)
        assertEquals(1, result.technologies.size)
        assertEquals("Spring Boot", result.technologies[0].name)
    }

    @Test
    fun `classify - groupId equal to prefix matches (not just startsWith)`() {
        val modules =
            listOf(Module("api", listOf(Dependency("com.fasterxml.jackson", "jackson-databind", "2.17.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val config = makeConfig(allowlist = listOf(AllowlistEntry("com.fasterxml.jackson", "Jackson")))
        val result = classify(stack, config)
        assertEquals(1, result.technologies.size)
        assertEquals("Jackson", result.technologies[0].name)
    }

    @Test
    fun `classify - false prefix match rejected (com-fasterxml-jacksonX does not match com-fasterxml-jackson)`() {
        val modules = listOf(Module("api", listOf(Dependency("com.fasterxml.jacksonX", "lib", "1.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val config = makeConfig(allowlist = listOf(AllowlistEntry("com.fasterxml.jackson", "Jackson")))
        val result = classify(stack, config)
        assertTrue(result.technologies.isEmpty())
        assertEquals(1, result.others.size)
    }

    // === classify — multi-version families ===

    @Test
    fun `classify - same family from two coords merges versions`() {
        val modules =
            listOf(
                Module(
                    "api",
                    listOf(
                        Dependency("org.springframework", "spring-core", "6.1.4"),
                        Dependency("org.springframework", "spring-beans", "6.1.4"),
                    ),
                ),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        val springMap = mapOf("spring-core" to "Spring Core", "spring-beans" to "Spring Core")
        val config = makeConfig(artifactIdFamilies = mapOf("org.springframework" to springMap))
        val result = classify(stack, config)
        assertEquals(1, result.technologies.size)
        assertEquals("Spring Core", result.technologies[0].name)
        assertEquals(listOf("6.1.4"), result.technologies[0].versions)
    }

    @Test
    fun `classify - same family from two coords with different versions lists both versions`() {
        val modules =
            listOf(
                Module("api", listOf(Dependency("org.springframework", "spring-core", "6.1.4"))),
                Module("svc", listOf(Dependency("org.springframework", "spring-beans", "6.0.0"))),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        val springMap = mapOf("spring-core" to "Spring Core", "spring-beans" to "Spring Core")
        val config = makeConfig(artifactIdFamilies = mapOf("org.springframework" to springMap))
        val result = classify(stack, config)
        assertEquals(1, result.technologies.size)
        val versions = result.technologies[0].versions
        assertTrue("6.0.0" in versions && "6.1.4" in versions)
    }

    // === classify — output sorting ===

    @Test
    fun `classify - technologies sorted alphabetically by name`() {
        val modules =
            listOf(
                Module(
                    "api",
                    listOf(
                        Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.0"),
                        Dependency("org.springframework", "spring-core", "6.1.4"),
                    ),
                ),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        val springMap = mapOf("spring-core" to "Spring Core")
        val config =
            makeConfig(
                artifactIdFamilies = mapOf("org.springframework" to springMap),
                allowlist = listOf(AllowlistEntry("com.fasterxml.jackson", "Jackson")),
            )
        val result = classify(stack, config)
        assertEquals(listOf("Jackson", "Spring Core"), result.technologies.map { it.name })
    }

    @Test
    fun `classify - others sorted alphabetically by coordinate`() {
        val modules =
            listOf(
                Module(
                    "api",
                    listOf(
                        Dependency("org.zzz", "z", "1.0"),
                        Dependency("com.aaa", "a", "1.0"),
                    ),
                ),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        val result = classify(stack, makeConfig())
        assertEquals(listOf("com.aaa:a", "org.zzz:z"), result.others.map { it.coordinate })
    }

    // === classify — production config verification ===

    @Test
    fun `classify - org-apache-commons falls to Other in production config`() {
        val modules =
            listOf(Module("api", listOf(Dependency("org.apache.commons", "commons-lang3", "3.14.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val result = classify(stack)
        assertTrue(
            result.technologies.none { it.name == "Apache Commons" },
            "Apache Commons must not appear in Technologies",
        )
        assertEquals(1, result.others.size)
    }

    @Test
    fun `classify - com-google-guava falls to Other in production config`() {
        val modules =
            listOf(Module("api", listOf(Dependency("com.google.guava", "guava", "33.0.0-jre"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val result = classify(stack)
        assertTrue(result.technologies.none { it.name == "Guava" }, "Guava must not appear in Technologies")
        assertEquals(1, result.others.size)
    }

    @Test
    fun `classify - org-json falls to Other in production config`() {
        val modules =
            listOf(Module("api", listOf(Dependency("org.json", "json", "20240303"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val result = classify(stack)
        assertTrue(result.technologies.none { it.name == "JSON" }, "org.json must not appear in Technologies")
        assertEquals(1, result.others.size)
    }

    @Test
    fun `classify - ru-mkskom-jcp folds to КриптоПро JCP in production config`() {
        val modules =
            listOf(Module("api", listOf(Dependency("ru.mkskom.jcp", "jcp", "2.0.40455"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val result = classify(stack)
        assertEquals(1, result.technologies.size)
        assertEquals("КриптоПро JCP", result.technologies[0].name)
        assertEquals(listOf("2.0.40455"), result.technologies[0].versions)
    }

    // === renderInvertedTechStack — classification rendering ===

    @Test
    fun `renderInvertedTechStack - classified Technologies block appears`() {
        val modules =
            listOf(Module("api", listOf(Dependency("org.springframework", "spring-core", "6.1.4"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val result = renderInvertedTechStack(stack, null, null, null)
        assertTrue(result.contains("Technologies:"), "Actual: $result")
        assertTrue(result.contains("Spring Core 6.1.4"), "Actual: $result")
    }

    @Test
    fun `renderInvertedTechStack - single version rendered inline`() {
        val modules =
            listOf(Module("api", listOf(Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val result = renderInvertedTechStack(stack, null, null, null)
        assertTrue(result.contains("Jackson 2.17.0"), "Actual: $result")
        assertFalse(result.contains("  2.17.0"), "Version must not be indented for single-version. Actual: $result")
    }

    @Test
    fun `renderInvertedTechStack - multi-version rendered with indented lines`() {
        val modules =
            listOf(
                Module("api", listOf(Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.0"))),
                Module("svc", listOf(Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.16.0"))),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        val result = renderInvertedTechStack(stack, null, null, null)
        assertTrue(result.contains("Jackson\n  2.16.0\n  2.17.0"), "Actual: $result")
    }

    @Test
    fun `renderInvertedTechStack - Other dependencies block appears for unrecognized coord`() {
        val modules = listOf(Module("api", listOf(Dependency("com.example.unknown", "lib", "1.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val result = renderInvertedTechStack(stack, null, null, null)
        assertTrue(result.contains("Other dependencies:"), "Actual: $result")
        assertTrue(result.contains("com.example.unknown:lib 1.0"), "Actual: $result")
    }

    @Test
    fun `renderInvertedTechStack - denylisted coord absent from output`() {
        val modules = listOf(Module("api", listOf(Dependency("org.jetbrains", "annotations", "24.0"))))
        val stack = buildInvertedTechStack(modules, emptySet())
        val result = renderInvertedTechStack(stack, null, null, null)
        assertFalse(result.contains("annotations"), "Denylisted coord must not appear. Actual: $result")
    }

    @Test
    fun `renderInvertedTechStack - Technologies and Other dependencies blocks separated by blank line`() {
        val modules =
            listOf(
                Module(
                    "api",
                    listOf(
                        Dependency("org.springframework", "spring-core", "6.1.4"),
                        Dependency("com.example.unknown", "lib", "1.0"),
                    ),
                ),
            )
        val stack = buildInvertedTechStack(modules, emptySet())
        val result = renderInvertedTechStack(stack, null, null, null)
        val lines = result.lines()
        val techIdx = lines.indexOfFirst { it == "Technologies:" }
        val otherIdx = lines.indexOfFirst { it == "Other dependencies:" }
        assertTrue(techIdx >= 0 && otherIdx >= 0, "Both blocks expected. Actual:\n$result")
        assertTrue(lines[otherIdx - 1].isBlank(), "Blank line expected before Other block. Actual:\n$result")
    }
}
