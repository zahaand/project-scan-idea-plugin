package dev.zahaand.projectscan.model

enum class StyleSourceType(val priority: Int) {
    CHECKSTYLE(1),
    SPOTLESS(1),
    PMD(1),
    EDITOR_CONFIG(2),
    IDE_CODE_STYLE(3),
}

data class StyleSource(
    val type: StyleSourceType,
    val path: String,
)

data class CodeStyleInfo(
    val sources: List<StyleSource> = emptyList(),
)
