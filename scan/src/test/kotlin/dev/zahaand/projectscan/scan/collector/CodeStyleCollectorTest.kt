package dev.zahaand.projectscan.scan.collector

import dev.zahaand.projectscan.model.CodeStyleInfo
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StyleSource
import dev.zahaand.projectscan.model.StyleSourceType
import dev.zahaand.projectscan.scan.fake.FakeStyleSourcePort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CodeStyleCollectorTest {

    @Test
    fun `project with three source types returns all in Ok`() {
        val sources = listOf(
            StyleSource(StyleSourceType.CHECKSTYLE, "checkstyle.xml"),
            StyleSource(StyleSourceType.EDITOR_CONFIG, ".editorconfig"),
            StyleSource(StyleSourceType.IDE_CODE_STYLE, ".idea/codeStyles/Project.xml"),
        )
        val result = collector(sources).collect()
        val ok = assertOk(result)
        assertEquals(3, ok.sources.size)
        assertTrue(ok.sources.any { it.type == StyleSourceType.CHECKSTYLE })
        assertTrue(ok.sources.any { it.type == StyleSourceType.EDITOR_CONFIG })
        assertTrue(ok.sources.any { it.type == StyleSourceType.IDE_CODE_STYLE })
    }

    @Test
    fun `no style files returns Empty`() {
        val result = collector(emptyList()).collect()
        assertTrue(result is SectionResult.Empty, "Expected Empty but got $result")
    }

    @Test
    fun `multiple editorconfig files all collected as separate StyleSource entries`() {
        val sources = listOf(
            StyleSource(StyleSourceType.EDITOR_CONFIG, ".editorconfig"),
            StyleSource(StyleSourceType.EDITOR_CONFIG, "module-a/.editorconfig"),
            StyleSource(StyleSourceType.EDITOR_CONFIG, "module-b/.editorconfig"),
        )
        val result = collector(sources).collect()
        val ok = assertOk(result)
        val editorconfigs = ok.sources.filter { it.type == StyleSourceType.EDITOR_CONFIG }
        assertEquals(3, editorconfigs.size)
        assertTrue(editorconfigs.any { it.path == ".editorconfig" })
        assertTrue(editorconfigs.any { it.path == "module-a/.editorconfig" })
        assertTrue(editorconfigs.any { it.path == "module-b/.editorconfig" })
    }

    @Test
    fun `Spotless applied without standalone config file emits no Spotless StyleSource`() {
        // Adapter found a Spotless task but no standalone config file — returns only Checkstyle
        val sources = listOf(StyleSource(StyleSourceType.CHECKSTYLE, "checkstyle.xml"))
        val result = collector(sources).collect()
        val ok = assertOk(result)
        assertTrue(ok.sources.none { it.type == StyleSourceType.SPOTLESS })
    }

    @Test
    fun `FR-007 StyleSource with XML path is returned as-is without parsed content`() {
        // Collector must expose only path+type; it must not parse the XML for style facts
        val xmlSource = StyleSource(StyleSourceType.CHECKSTYLE, "config/checkstyle/checkstyle.xml")
        val result = collector(listOf(xmlSource)).collect()
        val ok = assertOk(result)
        val returned = ok.sources.single()
        assertEquals(xmlSource, returned)
        assertEquals("config/checkstyle/checkstyle.xml", returned.path)
        assertEquals(StyleSourceType.CHECKSTYLE, returned.type)
    }

    @Test
    fun `partial port result returns Ok with collected sources not Error`() {
        // Port surfaces 2 of 3 expected sources (one path unresolvable) — section is Ok, not Error
        val partialSources = listOf(
            StyleSource(StyleSourceType.CHECKSTYLE, "checkstyle.xml"),
            StyleSource(StyleSourceType.EDITOR_CONFIG, ".editorconfig"),
        )
        val result = collector(partialSources).collect()
        val ok = assertOk(result)
        assertEquals(2, ok.sources.size)
    }

    // --- helpers ---

    private fun collector(sources: List<StyleSource>) = CodeStyleCollector(FakeStyleSourcePort(sources))

    private fun assertOk(result: SectionResult<*>): CodeStyleInfo {
        assertTrue(result is SectionResult.Ok<*>, "Expected Ok but got $result")
        @Suppress("UNCHECKED_CAST")
        return (result as SectionResult.Ok<CodeStyleInfo>).data
    }
}
