package dev.zahaand.projectscan.scan.fake

import dev.zahaand.projectscan.scan.port.LinterConfigParser
import dev.zahaand.projectscan.scan.port.ParsedRule

class FakeLinterConfigParser(
    private val rulesByPath: Map<String, List<ParsedRule>> = emptyMap(),
) : LinterConfigParser {
    override fun parseRules(absoluteConfigPath: String): List<ParsedRule> =
        rulesByPath[absoluteConfigPath]
            ?: throw IllegalArgumentException("No rules configured for path: $absoluteConfigPath")
}
