package dev.zahaand.projectscan.scan.collector

import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.TestInfo
import dev.zahaand.projectscan.scan.fake.FakeTestInfoPort
import dev.zahaand.projectscan.scan.port.TestInfoPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestCollectorTest {

    @Test
    fun `JUnit 5, Mockito, and AssertJ detected with resolved versions`() {
        val deps = listOf(
            Dependency("org.junit.jupiter", "junit-jupiter-api", "5.10.1"),
            Dependency("org.mockito", "mockito-core", "5.5.0"),
            Dependency("org.assertj", "assertj-core", "3.24.2"),
        )
        val result = collector(
            testRoots = listOf("src/test/kotlin"),
            testDeps = deps,
        ).collect()

        val ok = assertOk(result)
        assertEquals(3, ok.frameworks.size)
        val frameworks = ok.frameworks.associateBy { it.name }
        assertEquals("5.10.1", frameworks["JUnit 5"]?.version)
        assertEquals("5.5.0", frameworks["Mockito"]?.version)
        assertEquals("3.24.2", frameworks["AssertJ"]?.version)
        assertTrue(ok.unknownTestDependencies.isEmpty())
    }

    @Test
    fun `JaCoCo threshold 0_8 surfaced`() {
        val result = collector(
            testRoots = listOf("src/test/kotlin"),
            coverageThreshold = 0.8,
        ).collect()

        val ok = assertOk(result)
        assertEquals(0.8, ok.coverageThreshold)
    }

    @Test
    fun `JaCoCo reporting-only returns null coverageThreshold`() {
        val result = collector(
            testRoots = listOf("src/test/kotlin"),
            coverageThreshold = null,
        ).collect()

        val ok = assertOk(result)
        assertNull(ok.coverageThreshold)
    }

    @Test
    fun `no test deps and no test roots returns Empty`() {
        val result = collector(testRoots = emptyList(), testDeps = emptyList()).collect()
        assertTrue(result is SectionResult.Empty, "Expected Empty but got $result")
    }

    @Test
    fun `unknown test-scoped dependency recorded in unknownTestDependencies`() {
        val unknown = Dependency("com.example", "some-test-lib", "1.0.0")
        val result = collector(
            testRoots = listOf("src/test/kotlin"),
            testDeps = listOf(unknown),
        ).collect()

        val ok = assertOk(result)
        assertEquals(listOf(unknown), ok.unknownTestDependencies)
        assertTrue(ok.frameworks.isEmpty())
    }

    @Test
    fun `resolved version is used for TestFramework version`() {
        val dep = Dependency("org.junit.jupiter", "junit-jupiter", "5.9.3")
        val result = collector(
            testRoots = listOf("src/test/kotlin"),
            testDeps = listOf(dep),
        ).collect()

        val ok = assertOk(result)
        assertEquals("5.9.3", ok.frameworks.single().version)
    }

    @Test
    fun `null version is preserved for TestFramework`() {
        val dep = Dependency("org.mockito", "mockito-core", null)
        val result = collector(
            testRoots = listOf("src/test/kotlin"),
            testDeps = listOf(dep),
        ).collect()

        val ok = assertOk(result)
        assertNull(ok.frameworks.single().version)
    }

    @Test
    fun `FooBarTest produces suffix Test`() {
        val result = collector(
            testRoots = listOf("src/test/kotlin"),
            testClassNames = listOf("FooBarTest"),
        ).collect()

        assertEquals(listOf("Test"), assertOk(result).namingSuffixes)
    }

    @Test
    fun `FooBarIT produces suffix IT`() {
        val result = collector(
            testRoots = listOf("src/test/kotlin"),
            testClassNames = listOf("FooBarIT"),
        ).collect()

        assertEquals(listOf("IT"), assertOk(result).namingSuffixes)
    }

    @Test
    fun `OrderServiceSpec produces suffix Spec`() {
        val result = collector(
            testRoots = listOf("src/test/kotlin"),
            testClassNames = listOf("OrderServiceSpec"),
        ).collect()

        assertEquals(listOf("Spec"), assertOk(result).namingSuffixes)
    }

    @Test
    fun `multiple coexisting suffixes all captured`() {
        val result = collector(
            testRoots = listOf("src/test/kotlin"),
            testClassNames = listOf("FooBarTest", "FooBarIT", "OrderServiceSpec", "FooBarITCase", "FooBarTests"),
        ).collect()

        val suffixes = assertOk(result).namingSuffixes.toSet()
        assertTrue(suffixes.containsAll(setOf("Test", "IT", "Spec", "ITCase", "Tests")))
    }

    @Test
    fun `non-test class name contributes nothing to namingSuffixes`() {
        val result = collector(
            testRoots = listOf("src/test/kotlin"),
            testClassNames = listOf("OrderServiceImpl"),
        ).collect()

        assertTrue(assertOk(result).namingSuffixes.isEmpty())
    }

    @Test
    fun `partial failure - roots readable but deps throw - section Ok with roots and empty frameworks`() {
        val throwingPort = object : TestInfoPort {
            override fun getTestSourceRoots(): List<String> = listOf("src/test/kotlin")
            override fun getTestScopedDependencies(): List<Dependency> =
                throw RuntimeException("dependency read failed")
            override fun getCoverageThreshold(): Double? = null
            override fun getTestClassNames(): List<String> = emptyList()
        }

        val result = TestCollector(throwingPort).collect()

        val ok = assertOk(result)
        assertEquals(listOf("src/test/kotlin"), ok.sourceRoots)
        assertTrue(ok.frameworks.isEmpty())
        assertTrue(ok.unknownTestDependencies.isEmpty())
    }

    @Test
    fun `JUnit 4 matched only on exact artifactId junit`() {
        val junit4 = Dependency("junit", "junit", "4.13.2")
        val notJunit4 = Dependency("junit", "junit-dep", "4.11")
        val result = collector(
            testRoots = listOf("src/test/kotlin"),
            testDeps = listOf(junit4, notJunit4),
        ).collect()

        val ok = assertOk(result)
        assertEquals(1, ok.frameworks.size)
        assertEquals("JUnit 4", ok.frameworks.single().name)
        assertEquals(1, ok.unknownTestDependencies.size)
        assertEquals("junit-dep", ok.unknownTestDependencies.single().artifactId)
    }

    // --- helpers ---

    private fun collector(
        testRoots: List<String> = emptyList(),
        testDeps: List<Dependency> = emptyList(),
        coverageThreshold: Double? = null,
        testClassNames: List<String> = emptyList(),
    ) = TestCollector(
        FakeTestInfoPort(
            testSourceRoots = testRoots,
            testScopedDependencies = testDeps,
            coverageThreshold = coverageThreshold,
            testClassNames = testClassNames,
        )
    )

    private fun assertOk(result: SectionResult<*>): TestInfo {
        assertTrue(result is SectionResult.Ok<*>, "Expected Ok but got $result")
        @Suppress("UNCHECKED_CAST")
        return (result as SectionResult.Ok<TestInfo>).data
    }
}
