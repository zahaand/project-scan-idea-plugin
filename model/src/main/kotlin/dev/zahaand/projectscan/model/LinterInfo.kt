package dev.zahaand.projectscan.model

enum class RuleSeverity { ERROR, WARNING, INFO }

data class ActiveRule(
    val ruleId: String,
    val tool: String,
    val severity: RuleSeverity,
    val breaksBuild: Boolean?,
)

data class LinterInfo(
    val activeRules: List<ActiveRule> = emptyList(),
    val toolsWithUnresolvableConfig: List<String> = emptyList(),
)
