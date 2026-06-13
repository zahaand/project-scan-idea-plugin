package dev.zahaand.projectscan.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestInfoTest {
    @Test
    fun `empty-state TestInfo has empty lists and null nullable fields`() {
        val info = TestInfo()
        assertTrue(info.frameworks.isEmpty())
        assertTrue(info.unknownTestDependencies.isEmpty())
        assertTrue(info.sourceRoots.isEmpty())
        assertTrue(info.namingSuffixes.isEmpty())
        assertNull(info.coverageThreshold)
    }

    @Test
    fun `TestFramework version accepts null for BOM-managed case`() {
        val framework = TestFramework("JUnit 5", null)
        assertNull(framework.version)
        assertEquals("JUnit 5", framework.name)
    }

    @Test
    fun `populated TestInfo round-trips all fields`() {
        val junit5 = TestFramework("JUnit 5", "5.10.0")
        val mockito = TestFramework("Mockito", "5.4.0")
        val unknownDep = Dependency("com.example", "custom-test-util", "1.0.0")
        val info =
            TestInfo(
                frameworks = listOf(junit5, mockito),
                unknownTestDependencies = listOf(unknownDep),
                sourceRoots = listOf("src/test/kotlin"),
                namingSuffixes = listOf("Test", "IT"),
                coverageThreshold = 80.0,
            )
        assertEquals(listOf(junit5, mockito), info.frameworks)
        assertEquals(listOf(unknownDep), info.unknownTestDependencies)
        assertEquals(listOf("src/test/kotlin"), info.sourceRoots)
        assertEquals(listOf("Test", "IT"), info.namingSuffixes)
        assertEquals(80.0, info.coverageThreshold)
    }

    @Test
    fun `coverageThreshold null represents JaCoCo absent case`() {
        val info = TestInfo(coverageThreshold = null)
        assertNull(info.coverageThreshold)
    }

    @Test
    fun `namingSuffixes captures multiple coexisting raw suffixes`() {
        val info = TestInfo(namingSuffixes = listOf("Test", "Spec", "IT"))
        assertEquals(3, info.namingSuffixes.size)
        assertTrue(info.namingSuffixes.containsAll(listOf("Test", "Spec", "IT")))
    }
}
