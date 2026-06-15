package dev.zahaand.projectscan.baseline

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BaselineRuleCoverageTest {
    private val rules = BaselineRuleProvider.rules
    private val byCategory = rules.groupBy { it.category }

    @Test
    fun `total rule count is at least 13`() {
        assertTrue(rules.size >= 13, "Expected at least 13 rules, got ${rules.size}")
    }

    @Test
    fun `NULL_SAFETY has at least 2 rules`() {
        val count = byCategory[BaselineCategory.NULL_SAFETY]?.size ?: 0
        assertTrue(count >= 2, "NULL_SAFETY has $count rules; expected at least 2")
    }

    @Test
    fun `RESOURCE_MANAGEMENT has at least 2 rules`() {
        val count = byCategory[BaselineCategory.RESOURCE_MANAGEMENT]?.size ?: 0
        assertTrue(count >= 2, "RESOURCE_MANAGEMENT has $count rules; expected at least 2")
    }

    @Test
    fun `CONCURRENCY has at least 2 rules`() {
        val count = byCategory[BaselineCategory.CONCURRENCY]?.size ?: 0
        assertTrue(count >= 2, "CONCURRENCY has $count rules; expected at least 2")
    }

    @Test
    fun `DANGEROUS_CONSTRUCTS has at least 2 rules`() {
        val count = byCategory[BaselineCategory.DANGEROUS_CONSTRUCTS]?.size ?: 0
        assertTrue(count >= 2, "DANGEROUS_CONSTRUCTS has $count rules; expected at least 2")
    }

    @Test
    fun `EXCEPTION_HANDLING has at least 1 rule`() {
        val count = byCategory[BaselineCategory.EXCEPTION_HANDLING]?.size ?: 0
        assertTrue(count >= 1, "EXCEPTION_HANDLING has $count rules; expected at least 1")
    }

    @Test
    fun `STRING_PERFORMANCE has at least 1 rule`() {
        val count = byCategory[BaselineCategory.STRING_PERFORMANCE]?.size ?: 0
        assertTrue(count >= 1, "STRING_PERFORMANCE has $count rules; expected at least 1")
    }

    @Test
    fun `DECOMPOSITION has at least 1 rule`() {
        val count = byCategory[BaselineCategory.DECOMPOSITION]?.size ?: 0
        assertTrue(count >= 1, "DECOMPOSITION has $count rules; expected at least 1")
    }

    @Test
    fun `IMMUTABILITY has at least 1 rule`() {
        val count = byCategory[BaselineCategory.IMMUTABILITY]?.size ?: 0
        assertTrue(count >= 1, "IMMUTABILITY has $count rules; expected at least 1")
    }

    @Test
    fun `INTERFACE_PROGRAMMING has at least 1 rule`() {
        val count = byCategory[BaselineCategory.INTERFACE_PROGRAMMING]?.size ?: 0
        assertTrue(count >= 1, "INTERFACE_PROGRAMMING has $count rules; expected at least 1")
    }
}
