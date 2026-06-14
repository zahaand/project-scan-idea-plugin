package dev.zahaand.projectscan.scan.adapter

import dev.zahaand.projectscan.model.RuleSeverity
import dev.zahaand.projectscan.scan.port.LinterConfigParser
import dev.zahaand.projectscan.scan.port.ParsedRule
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class CheckstyleConfigParser : LinterConfigParser {
    private val containers = setOf("Checker", "TreeWalker")

    override fun parseRules(absoluteConfigPath: String): List<ParsedRule> {
        val doc =
            DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(File(absoluteConfigPath))
        doc.documentElement.normalize()
        val rules = mutableListOf<ParsedRule>()
        collectRules(doc.documentElement, null, rules)
        return rules
    }

    fun parseRulesFromXml(xml: String): List<ParsedRule> {
        val doc =
            DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(xml.byteInputStream())
        doc.documentElement.normalize()
        val rules = mutableListOf<ParsedRule>()
        collectRules(doc.documentElement, null, rules)
        return rules
    }

    private fun collectRules(
        element: Element,
        inheritedSeverity: RuleSeverity?,
        result: MutableList<ParsedRule>,
    ) {
        val name = element.getAttribute("name")
        val declaredSeverity = extractSeverity(element)
        val effectiveSeverity = declaredSeverity ?: inheritedSeverity

        if (name.isNotEmpty() && name !in containers) {
            result.add(ParsedRule(ruleId = name, severity = effectiveSeverity ?: RuleSeverity.INFO))
        }

        val childModules = element.childNodes
        for (i in 0 until childModules.length) {
            val node = childModules.item(i)
            if (node is Element && node.tagName == "module") {
                collectRules(node, effectiveSeverity, result)
            }
        }
    }

    private fun extractSeverity(element: Element): RuleSeverity? {
        val props: NodeList = element.childNodes
        for (i in 0 until props.length) {
            val node = props.item(i)
            if (node is Element && node.tagName == "property" &&
                node.getAttribute("name") == "severity"
            ) {
                return when (node.getAttribute("value").lowercase()) {
                    "error" -> RuleSeverity.ERROR
                    "warning" -> RuleSeverity.WARNING
                    "info", "ignore" -> RuleSeverity.INFO
                    else -> null
                }
            }
        }
        return null
    }
}
