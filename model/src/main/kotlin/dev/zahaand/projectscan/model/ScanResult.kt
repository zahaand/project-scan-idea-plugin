package dev.zahaand.projectscan.model

sealed class SectionResult<out T> {
    data class Ok<out T>(val data: T) : SectionResult<T>()

    data object Empty : SectionResult<Nothing>()

    data class Error(val cause: String? = null) : SectionResult<Nothing>()
}

data class ScanResult(
    val stack: SectionResult<StackInfo>,
    val codeStyle: SectionResult<CodeStyleInfo>,
    val linters: SectionResult<LinterInfo>,
    val tests: SectionResult<TestInfo>,
    val structure: SectionResult<StructureInfo>,
)
