package dev.zahaand.projectscan.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StackInfoTest {

    @Test
    fun `empty-state StackInfo has empty list and null fields`() {
        val info = StackInfo()
        assertTrue(info.dependencies.isEmpty())
        assertNull(info.jdkVersion)
        assertNull(info.languageLevel)
        assertNull(info.buildSystem)
    }

    @Test
    fun `populated StackInfo round-trips all fields`() {
        val dep1 = Dependency("org.junit.jupiter", "junit-jupiter", "5.10.0")
        val dep2 = Dependency("org.mockito", "mockito-core", "5.4.0")
        val info = StackInfo(
            dependencies = listOf(dep1, dep2),
            jdkVersion = "21",
            languageLevel = "21",
            buildSystem = BuildSystem.GRADLE
        )
        assertEquals(listOf(dep1, dep2), info.dependencies)
        assertEquals("21", info.jdkVersion)
        assertEquals("21", info.languageLevel)
        assertEquals(BuildSystem.GRADLE, info.buildSystem)
    }

    @Test
    fun `Dependency resolvedVersion accepts null for BOM-managed case`() {
        val dep = Dependency("org.springframework.boot", "spring-boot-starter", null)
        assertNull(dep.resolvedVersion)
        assertEquals("org.springframework.boot", dep.groupId)
        assertEquals("spring-boot-starter", dep.artifactId)
    }
}
