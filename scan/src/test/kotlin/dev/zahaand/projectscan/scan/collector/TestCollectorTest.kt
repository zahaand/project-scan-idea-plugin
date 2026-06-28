package dev.zahaand.projectscan.scan.collector

import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.TestInfo
import dev.zahaand.projectscan.scan.fake.FakeTestInfoPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestCollectorTest {
    @Test
    fun `JaCoCo threshold 0_8 surfaced`() {
        val result =
            collector(
                testRoots = listOf("src/test/kotlin"),
                coverageThreshold = 0.8,
            ).collect()

        val ok = assertOk(result)
        assertEquals(0.8, ok.coverageThreshold)
    }

    @Test
    fun `JaCoCo reporting-only returns null coverageThreshold`() {
        val result =
            collector(
                testRoots = listOf("src/test/kotlin"),
                coverageThreshold = null,
            ).collect()

        val ok = assertOk(result)
        assertNull(ok.coverageThreshold)
    }

    @Test
    fun `no test roots returns Empty`() {
        val result = collector(testRoots = emptyList()).collect()
        assertTrue(result is SectionResult.Empty, "Expected Empty but got $result")
    }

    @Test
    fun `FooBarTest produces suffix Test`() {
        val result =
            collector(
                testRoots = listOf("src/test/kotlin"),
                testClassNames = listOf("FooBarTest"),
            ).collect()

        assertEquals(listOf("Test"), assertOk(result).namingSuffixes)
    }

    @Test
    fun `FooBarIT produces suffix IT`() {
        val result =
            collector(
                testRoots = listOf("src/test/kotlin"),
                testClassNames = listOf("FooBarIT"),
            ).collect()

        assertEquals(listOf("IT"), assertOk(result).namingSuffixes)
    }

    @Test
    fun `OrderServiceSpec produces suffix Spec`() {
        val result =
            collector(
                testRoots = listOf("src/test/kotlin"),
                testClassNames = listOf("OrderServiceSpec"),
            ).collect()

        assertEquals(listOf("Spec"), assertOk(result).namingSuffixes)
    }

    @Test
    fun `multiple coexisting suffixes all captured`() {
        val result =
            collector(
                testRoots = listOf("src/test/kotlin"),
                testClassNames = listOf("FooBarTest", "FooBarIT", "OrderServiceSpec", "FooBarITCase", "FooBarTests"),
            ).collect()

        val suffixes = assertOk(result).namingSuffixes.toSet()
        assertTrue(suffixes.containsAll(setOf("Test", "IT", "Spec", "ITCase", "Tests")))
    }

    @Test
    fun `non-test class name contributes nothing to namingSuffixes`() {
        val result =
            collector(
                testRoots = listOf("src/test/kotlin"),
                testClassNames = listOf("OrderServiceImpl"),
            ).collect()

        assertTrue(assertOk(result).namingSuffixes.isEmpty())
    }

    // --- helpers ---

    private fun collector(
        testRoots: List<String> = emptyList(),
        coverageThreshold: Double? = null,
        testClassNames: List<String> = emptyList(),
    ) = TestCollector(
        FakeTestInfoPort(
            testSourceRoots = testRoots,
            coverageThreshold = coverageThreshold,
            testClassNames = testClassNames,
        ),
    )

    private fun assertOk(result: SectionResult<*>): TestInfo {
        assertTrue(result is SectionResult.Ok<*>, "Expected Ok but got $result")
        @Suppress("UNCHECKED_CAST")
        return (result as SectionResult.Ok<TestInfo>).data
    }
}
