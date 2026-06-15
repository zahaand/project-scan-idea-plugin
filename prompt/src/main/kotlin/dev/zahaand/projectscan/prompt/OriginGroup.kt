package dev.zahaand.projectscan.prompt

data class OriginGroup(
    val label: String,
    val mandatoryRules: List<String>,
    val advisoryRules: List<String>,
    val emptyNotation: String?,
)
