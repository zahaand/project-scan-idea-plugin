package dev.zahaand.projectscan.scan.adapter

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.zahaand.projectscan.model.StyleSourceType
import java.io.File

class IjStyleSourceAdapterTest : BasePlatformTestCase() {
    private val createdRoots = mutableListOf<File>()

    override fun tearDown() {
        createdRoots.forEach { it.deleteRecursively() }
        createdRoots.clear()
        LocalFileSystem.getInstance().refreshAndFindFileByPath(project.basePath!!)?.refresh(false, true)
        super.tearDown()
    }

    private fun createProjectFile(
        relativePath: String,
        content: String = "",
    ) {
        val file = File("${project.basePath}/$relativePath")
        file.parentFile?.mkdirs()
        file.writeText(content)
        val topLevel = File("${project.basePath}/${relativePath.substringBefore('/')}")
        if (topLevel !in createdRoots) createdRoots += topLevel
        LocalFileSystem.getInstance().refreshAndFindFileByPath(project.basePath!!)?.refresh(false, true)
    }

    fun testFindsEditorConfigAtRoot() {
        createProjectFile(".editorconfig", "root = true")

        val sources = IjStyleSourceAdapter(project).findStyleSources()

        assertTrue(sources.any { it.type == StyleSourceType.EDITOR_CONFIG && it.path == ".editorconfig" })
    }

    fun testFindsIdeCodeStylesDir() {
        createProjectFile(".idea/codeStyles/Project.xml", "<component/>")
        createProjectFile(".idea/codeStyles/codeStyleConfig.xml", "<component/>")

        val sources = IjStyleSourceAdapter(project).findStyleSources()

        val ideEntries = sources.filter { it.type == StyleSourceType.IDE_CODE_STYLE }
        assertEquals(2, ideEntries.size)
        assertTrue(ideEntries.any { it.path == ".idea/codeStyles/Project.xml" })
        assertTrue(ideEntries.any { it.path == ".idea/codeStyles/codeStyleConfig.xml" })
    }

    fun testFindsEditorConfigAndIdeCodeStylesTogether() {
        createProjectFile(".editorconfig", "root = true")
        createProjectFile(".idea/codeStyles/Project.xml", "<component/>")

        val sources = IjStyleSourceAdapter(project).findStyleSources()
        val types = sources.map { it.type }.toSet()

        assertTrue(StyleSourceType.EDITOR_CONFIG in types)
        assertTrue(StyleSourceType.IDE_CODE_STYLE in types)
    }

    fun testEmptyProjectReturnsNoSources() {
        val sources = IjStyleSourceAdapter(project).findStyleSources()

        assertTrue(sources.isEmpty())
    }

    fun testFindsCheckstyleConfigAtWellKnownPath() {
        createProjectFile("config/checkstyle/checkstyle.xml", "<module name=\"Checker\"/>")

        val sources = IjStyleSourceAdapter(project).findStyleSources()

        assertTrue(
            sources.any { it.type == StyleSourceType.CHECKSTYLE && it.path == "config/checkstyle/checkstyle.xml" },
        )
    }
}
