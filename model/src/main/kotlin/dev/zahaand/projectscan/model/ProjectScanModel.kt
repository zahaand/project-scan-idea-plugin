package dev.zahaand.projectscan.model

data class ProjectScanModel(
    val stack: StackInfo,
    val codeStyle: CodeStyleInfo,
    val linters: LinterInfo,
    val tests: TestInfo,
    val structure: StructureInfo,
)
