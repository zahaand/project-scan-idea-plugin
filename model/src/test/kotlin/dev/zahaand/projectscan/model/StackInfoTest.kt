package dev.zahaand.projectscan.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StackInfoTest {
    @Test
    fun `empty-state StackInfo has null fields`() {
        val info = StackInfo()
        assertNull(info.jdkVersion)
        assertNull(info.languageLevel)
        assertNull(info.buildSystem)
    }

    @Test
    fun `populated StackInfo round-trips all fields`() {
        val info =
            StackInfo(
                jdkVersion = "21",
                languageLevel = "21",
                buildSystem = BuildSystem.GRADLE,
            )
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
