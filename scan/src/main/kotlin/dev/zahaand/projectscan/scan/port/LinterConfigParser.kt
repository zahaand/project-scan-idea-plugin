package dev.zahaand.projectscan.scan.port

import dev.zahaand.projectscan.model.RuleSeverity

data class ParsedRule(
    val ruleId: String,
    val severity: RuleSeverity,
)

interface LinterConfigParser {
    fun parseRules(absoluteConfigPath: String): List<ParsedRule>
}
