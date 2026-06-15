package dev.zahaand.projectscan.baseline

import kotlinx.serialization.Serializable

@Serializable
enum class BaselineLevel {
    CORRECTNESS,
    BEST_PRACTICE,
}

@Serializable
enum class BaselineCategory {
    NULL_SAFETY,
    RESOURCE_MANAGEMENT,
    CONCURRENCY,
    DANGEROUS_CONSTRUCTS,
    EXCEPTION_HANDLING,
    STRING_PERFORMANCE,
    DECOMPOSITION,
    IMMUTABILITY,
    INTERFACE_PROGRAMMING,
}

@Serializable
enum class Obligation {
    MUST,
    SHOULD,
}

@Serializable
enum class BaselineLanguage {
    JAVA,
}

@Serializable
data class BaselineRule(
    val id: String,
    val level: BaselineLevel,
    val category: BaselineCategory,
    val obligation: Obligation,
    val statement: String,
    val rationale: String,
    val minJavaLevel: Int,
    val languages: List<BaselineLanguage>,
)
