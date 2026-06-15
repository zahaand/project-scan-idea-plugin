package dev.zahaand.projectscan.baseline

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.Reader

@Serializable
internal data class RuleSet(
    val schemaVersion: Int,
    val rules: List<BaselineRule>,
)

object BaselineRuleProvider {
    private const val RESOURCE_PATH = "/dev/zahaand/projectscan/baseline/rules.json"
    private const val SUPPORTED_SCHEMA_VERSION = 1
    private val ALLOWED_JAVA_LEVELS = setOf(8, 11, 17, 21)

    private val jsonParser = Json { ignoreUnknownKeys = true }

    val rules: List<BaselineRule> by lazy { loadRules() }

    @Suppress("ThrowsCount")
    internal fun loadFromReader(reader: Reader): List<BaselineRule> {
        val text =
            try {
                reader.readText()
            } catch (e: IOException) {
                throw BaselineLoadException("Failed to read rules.json: ${e.message}", e)
            }
        val ruleSet =
            try {
                jsonParser.decodeFromString<RuleSet>(text)
            } catch (e: SerializationException) {
                throw BaselineLoadException("Failed to parse rules.json: ${e.message}", e)
            }
        if (ruleSet.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw BaselineLoadException(
                "Unsupported schemaVersion: ${ruleSet.schemaVersion}; expected $SUPPORTED_SCHEMA_VERSION",
            )
        }
        if (ruleSet.rules.isEmpty()) {
            throw BaselineLoadException("rules array is empty — bundled rule set must not be empty")
        }
        validateRules(ruleSet.rules)
        return ruleSet.rules.toList()
    }

    private fun loadRules(): List<BaselineRule> {
        val stream =
            BaselineRuleProvider::class.java.getResourceAsStream(RESOURCE_PATH)
                ?: throw BaselineLoadException("rules.json not found at classpath path: $RESOURCE_PATH")
        return loadFromReader(stream.bufferedReader())
    }

    @Suppress("ThrowsCount")
    private fun validateRules(rules: List<BaselineRule>) {
        val seenIds = mutableSetOf<String>()
        for ((index, rule) in rules.withIndex()) {
            if (rule.id.isBlank()) {
                throw BaselineLoadException("Rule at index $index has a blank id")
            }
            if (!seenIds.add(rule.id)) {
                throw BaselineLoadException("Duplicate rule id: '${rule.id}'")
            }
            if (rule.statement.isBlank()) {
                throw BaselineLoadException("Rule '${rule.id}' has a blank statement")
            }
            if (rule.rationale.isBlank()) {
                throw BaselineLoadException("Rule '${rule.id}' has a blank rationale")
            }
            if (rule.minJavaLevel !in ALLOWED_JAVA_LEVELS) {
                throw BaselineLoadException(
                    "Rule '${rule.id}' has invalid minJavaLevel ${rule.minJavaLevel}; " +
                        "allowed: $ALLOWED_JAVA_LEVELS",
                )
            }
            if (rule.languages.isEmpty()) {
                throw BaselineLoadException("Rule '${rule.id}' has an empty languages list")
            }
            val expectedLevel = rule.category.level
            if (rule.level != expectedLevel) {
                throw BaselineLoadException(
                    "Rule '${rule.id}': category ${rule.category} requires level $expectedLevel " +
                        "but found ${rule.level}",
                )
            }
        }
    }
}
