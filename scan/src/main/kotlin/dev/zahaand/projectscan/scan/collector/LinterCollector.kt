package dev.zahaand.projectscan.scan.collector

import dev.zahaand.projectscan.model.ActiveRule
import dev.zahaand.projectscan.model.LinterInfo
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.scan.port.LinterConfigParser
import dev.zahaand.projectscan.scan.port.LinterPort

class LinterCollector(
    private val linterPort: LinterPort,
    private val linterConfigParsers: Map<String, LinterConfigParser>,
) {
    fun collect(): SectionResult<LinterInfo> {
        val tools = linterPort.getAppliedLinterTools()
        if (tools.isEmpty()) return SectionResult.Empty

        val activeRules = mutableListOf<ActiveRule>()
        val toolsWithUnresolvableConfig = mutableListOf<String>()

        for (tool in tools) {
            val parser = linterConfigParsers[tool.toolName]
            val configPath = tool.configFilePath

            if (parser == null || configPath == null) {
                toolsWithUnresolvableConfig += tool.toolName
                continue
            }

            val parsedRules = try {
                parser.parseRules(configPath)
            } catch (e: Exception) {
                toolsWithUnresolvableConfig += tool.toolName
                continue
            }

            parsedRules.mapTo(activeRules) { parsed ->
                ActiveRule(
                    ruleId = parsed.ruleId,
                    tool = tool.toolName,
                    severity = parsed.severity,
                    breaksBuild = tool.breaksBuild,
                )
            }
        }

        return SectionResult.Ok(
            LinterInfo(
                activeRules = activeRules,
                toolsWithUnresolvableConfig = toolsWithUnresolvableConfig,
            )
        )
    }
}
