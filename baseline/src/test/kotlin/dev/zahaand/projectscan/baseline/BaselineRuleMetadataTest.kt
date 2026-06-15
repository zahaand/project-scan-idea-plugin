package dev.zahaand.projectscan.baseline

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BaselineRuleMetadataTest {
    private val rules = BaselineRuleProvider.rules
    private val allowedJavaLevels = setOf(8, 11, 17, 21)

    @Test
    fun `every rule has a non-blank id`() {
        for (rule in rules) {
            assertTrue(rule.id.isNotBlank(), "Rule has a blank id")
        }
    }

    @Test
    fun `every rule has a non-blank statement`() {
        for (rule in rules) {
            assertTrue(rule.statement.isNotBlank(), "Rule '${rule.id}' has a blank statement")
        }
    }

    @Test
    fun `every rule has a non-blank rationale`() {
        for (rule in rules) {
            assertTrue(rule.rationale.isNotBlank(), "Rule '${rule.id}' has a blank rationale")
        }
    }

    @Test
    fun `every rule has a valid level`() {
        val validLevels = BaselineLevel.entries.toSet()
        for (rule in rules) {
            assertTrue(
                rule.level in validLevels,
                "Rule '${rule.id}' has invalid level: ${rule.level}; expected one of $validLevels",
            )
        }
    }

    @Test
    fun `every rule has a valid category`() {
        val validCategories = BaselineCategory.entries.toSet()
        for (rule in rules) {
            assertTrue(
                rule.category in validCategories,
                "Rule '${rule.id}' has invalid category: ${rule.category}; expected one of $validCategories",
            )
        }
    }

    @Test
    fun `every rule has a valid obligation`() {
        val validObligations = Obligation.entries.toSet()
        for (rule in rules) {
            assertTrue(
                rule.obligation in validObligations,
                "Rule '${rule.id}' has invalid obligation: ${rule.obligation}; expected one of $validObligations",
            )
        }
    }

    @Test
    fun `every rule has a valid minJavaLevel`() {
        for (rule in rules) {
            assertTrue(
                rule.minJavaLevel in allowedJavaLevels,
                "Rule '${rule.id}' has invalid minJavaLevel: ${rule.minJavaLevel}; allowed: $allowedJavaLevels",
            )
        }
    }

    @Test
    fun `every rule has a non-empty languages list containing only JAVA`() {
        for (rule in rules) {
            assertTrue(rule.languages.isNotEmpty(), "Rule '${rule.id}' has an empty languages list")
            for (lang in rule.languages) {
                assertTrue(
                    lang == BaselineLanguage.JAVA,
                    "Rule '${rule.id}' has unexpected language: $lang; expected only JAVA",
                )
            }
        }
    }

    @Test
    fun `every rule category is consistent with its level`() {
        for (rule in rules) {
            val expectedLevel = rule.category.level
            assertTrue(
                rule.level == expectedLevel,
                "Rule '${rule.id}': category ${rule.category} requires level $expectedLevel but found ${rule.level}",
            )
        }
    }

    @Test
    fun `SC-007 at least one rule has minJavaLevel greater than 8`() {
        assertTrue(
            rules.any { it.minJavaLevel > 8 },
            "Expected at least one rule with minJavaLevel > 8 (SC-007)",
        )
    }
}
