package dev.zahaand.projectscan.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CodeStyleInfoTest {

    @Test
    fun `empty-state CodeStyleInfo has empty list`() {
        val info = CodeStyleInfo()
        assertTrue(info.sources.isEmpty())
    }

    @Test
    fun `all five StyleSourceType values carry correct priority integers`() {
        assertEquals(1, StyleSourceType.CHECKSTYLE.priority)
        assertEquals(1, StyleSourceType.SPOTLESS.priority)
        assertEquals(1, StyleSourceType.PMD.priority)
        assertEquals(2, StyleSourceType.EDITOR_CONFIG.priority)
        assertEquals(3, StyleSourceType.IDE_CODE_STYLE.priority)
    }

    @Test
    fun `minByOrNull on mixed list returns a linter-type source`() {
        val sources = listOf(
            StyleSource(StyleSourceType.EDITOR_CONFIG, ".editorconfig"),
            StyleSource(StyleSourceType.CHECKSTYLE, "config/checkstyle/checkstyle.xml"),
            StyleSource(StyleSourceType.IDE_CODE_STYLE, ".idea/codeStyle.xml")
        )
        val highest = sources.minByOrNull { it.type.priority }
        assertNotNull(highest)
        assertEquals(StyleSourceType.CHECKSTYLE, highest!!.type)
    }

    @Test
    fun `Checkstyle Spotless and PMD share rank 1`() {
        assertEquals(StyleSourceType.CHECKSTYLE.priority, StyleSourceType.SPOTLESS.priority)
        assertEquals(StyleSourceType.SPOTLESS.priority, StyleSourceType.PMD.priority)
        assertEquals(1, StyleSourceType.CHECKSTYLE.priority)
    }

    @Test
    fun `StyleSource path field round-trips`() {
        val source = StyleSource(StyleSourceType.PMD, "config/pmd/ruleset.xml")
        assertEquals("config/pmd/ruleset.xml", source.path)
        assertEquals(StyleSourceType.PMD, source.type)
    }

    @Test
    fun `multiple StyleSource entries with same type but different paths coexist`() {
        val root = StyleSource(StyleSourceType.EDITOR_CONFIG, ".editorconfig")
        val sub = StyleSource(StyleSourceType.EDITOR_CONFIG, "module/.editorconfig")
        val info = CodeStyleInfo(sources = listOf(root, sub))
        assertEquals(2, info.sources.size)
        assertEquals(".editorconfig", info.sources[0].path)
        assertEquals("module/.editorconfig", info.sources[1].path)
        assertTrue(info.sources.all { it.type == StyleSourceType.EDITOR_CONFIG })
    }
}
