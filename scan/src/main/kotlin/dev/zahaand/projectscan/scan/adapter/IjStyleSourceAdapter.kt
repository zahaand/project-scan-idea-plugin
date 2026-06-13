package dev.zahaand.projectscan.scan.adapter

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import dev.zahaand.projectscan.model.StyleSource
import dev.zahaand.projectscan.model.StyleSourceType
import dev.zahaand.projectscan.scan.port.StyleSourcePort

class IjStyleSourceAdapter(private val project: Project) : StyleSourcePort {

    override fun findStyleSources(): List<StyleSource> {
        val basePath = project.basePath ?: return emptyList()
        val base = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return emptyList()
        return buildList {
            // Checkstyle: well-known exact path + name-pattern at depth ≤ 2
            addAll(filesInDir(base, "config/checkstyle", StyleSourceType.CHECKSTYLE))
            addAll(byNamePattern(base, "checkstyle", StyleSourceType.CHECKSTYLE, maxDepth = 2) {
                it.startsWith("config/checkstyle/")
            })

            // PMD: well-known exact path + name-pattern at depth ≤ 2
            addAll(filesInDir(base, "config/pmd", StyleSourceType.PMD))
            addAll(byNamePattern(base, "pmd", StyleSourceType.PMD, maxDepth = 2) {
                it.startsWith("config/pmd/")
            })

            // .editorconfig: root via relative path + recursive subdirectory walk
            addAll(findEditorconfigs(base))

            // IDE code style: all direct children of .idea/codeStyles/
            addAll(filesInDir(base, ".idea/codeStyles", StyleSourceType.IDE_CODE_STYLE))

            // Spotless: standalone non-build config file required; name-pattern at depth ≤ 2
            addAll(byNamePattern(base, "spotless", StyleSourceType.SPOTLESS, maxDepth = 2))
        }
    }

    /** Lists all non-directory files in a known directory, mapping them to the given type. */
    private fun filesInDir(base: VirtualFile, relDir: String, type: StyleSourceType): List<StyleSource> {
        val dir = base.findFileByRelativePath(relDir) ?: return emptyList()
        return dir.children.filter { !it.isDirectory }.map { StyleSource(type, toRelative(base, it)) }
    }

    /**
     * Walks up to [maxDepth] levels below [base], collecting files whose names start with [prefix].
     * Files whose relative path satisfies [exclude] are skipped (used to avoid double-counting
     * exact-path results).
     */
    private fun byNamePattern(
        base: VirtualFile,
        prefix: String,
        type: StyleSourceType,
        maxDepth: Int,
        exclude: (String) -> Boolean = { false },
    ): List<StyleSource> {
        val results = mutableListOf<StyleSource>()
        walkDepth(base, maxDepth) { file ->
            if (!file.isDirectory && file.name.startsWith(prefix)) {
                val rel = toRelative(base, file)
                if (!exclude(rel)) results += StyleSource(type, rel)
            }
        }
        return results
    }

    /**
     * Finds .editorconfig at the project root via an exact relative-path lookup, then recursively
     * walks subdirectories to collect nested .editorconfig files.
     */
    private fun findEditorconfigs(base: VirtualFile): List<StyleSource> {
        val results = mutableListOf<StyleSource>()
        base.findFileByRelativePath(".editorconfig")
            ?.let { results += StyleSource(StyleSourceType.EDITOR_CONFIG, ".editorconfig") }
        for (child in base.children) {
            if (child.isDirectory) walkRecursive(base, child) { file ->
                if (file.name == ".editorconfig") {
                    results += StyleSource(StyleSourceType.EDITOR_CONFIG, toRelative(base, file))
                }
            }
        }
        return results
    }

    private fun walkDepth(dir: VirtualFile, maxDepth: Int, depth: Int = 1, action: (VirtualFile) -> Unit) {
        if (depth > maxDepth) return
        for (child in dir.children) {
            action(child)
            if (child.isDirectory) walkDepth(child, maxDepth, depth + 1, action)
        }
    }

    private fun walkRecursive(base: VirtualFile, dir: VirtualFile, action: (VirtualFile) -> Unit) {
        for (child in dir.children) {
            action(child)
            if (child.isDirectory) walkRecursive(base, child, action)
        }
    }

    private fun toRelative(base: VirtualFile, file: VirtualFile): String =
        file.path.removePrefix(base.path).trimStart('/')
}
