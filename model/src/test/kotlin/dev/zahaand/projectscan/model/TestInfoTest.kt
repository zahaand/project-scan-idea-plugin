package dev.zahaand.projectscan.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestInfoTest {
    @Test
    fun `empty-state TestInfo has empty lists and null nullable fields`() {
        val info = TestInfo()
        assertTrue(info.sourceRoots.isEmpty())
        assertTrue(info.namingSuffixes.isEmpty())
        assertNull(info.coverageThreshold)
    }

    @Test
    fun `populated TestInfo round-trips all fields`() {
        val info =
            TestInfo(
                sourceRoots = listOf("src/test/kotlin"),
                namingSuffixes = listOf("Test", "IT"),
                coverageThreshold = 80.0,
            )
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
