package dev.zahaand.projectscan.scan.adapter

import dev.zahaand.projectscan.model.RuleSeverity
import dev.zahaand.projectscan.scan.port.LinterConfigParser
import dev.zahaand.projectscan.scan.port.ParsedRule
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PmdConfigParser : LinterConfigParser {

    override fun parseRules(absoluteConfigPath: String): List<ParsedRule> {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File(absoluteConfigPath))
        doc.documentElement.normalize()
        return extractRules(doc.documentElement)
    }

    fun parseRulesFromXml(xml: String): List<ParsedRule> {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(xml.byteInputStream())
        doc.documentElement.normalize()
        return extractRules(doc.documentElement)
    }

    private fun extractRules(root: Element): List<ParsedRule> {
        val rules = mutableListOf<ParsedRule>()
        val ruleNodes = root.getElementsByTagName("rule")
        for (i in 0 until ruleNodes.length) {
            val node = ruleNodes.item(i)
            if (node !is Element) continue
            val ref = node.getAttribute("ref")
            if (ref.isBlank()) continue
            // Skip rules that are nested inside another rule element (rulesets-within-rules)
            if (node.parentNode != root) continue
            val severity = extractSeverity(node)
            rules.add(ParsedRule(ruleId = ref, severity = severity))
        }
        return rules
    }

    private fun extractSeverity(ruleElement: Element): RuleSeverity {
        val priorityNodes = ruleElement.getElementsByTagName("priority")
        for (i in 0 until priorityNodes.length) {
            val node = priorityNodes.item(i)
            if (node is Element && node.parentNode == ruleElement) {
                return when (node.textContent.trim()) {
                    "1", "2" -> RuleSeverity.ERROR
                    "3" -> RuleSeverity.WARNING
                    "4", "5" -> RuleSeverity.INFO
                    else -> RuleSeverity.INFO
                }
            }
        }
        return RuleSeverity.INFO
    }
}
