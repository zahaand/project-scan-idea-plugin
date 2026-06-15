package dev.zahaand.projectscan.baseline

import kotlinx.serialization.Serializable

@Serializable
enum class BaselineLevel {
    CORRECTNESS,
    BEST_PRACTICE,
}

@Serializable
enum class BaselineCategory(val level: BaselineLevel) {
    NULL_SAFETY(BaselineLevel.CORRECTNESS),
    RESOURCE_MANAGEMENT(BaselineLevel.CORRECTNESS),
    CONCURRENCY(BaselineLevel.CORRECTNESS),
    DANGEROUS_CONSTRUCTS(BaselineLevel.CORRECTNESS),
    EXCEPTION_HANDLING(BaselineLevel.BEST_PRACTICE),
    STRING_PERFORMANCE(BaselineLevel.BEST_PRACTICE),
    DECOMPOSITION(BaselineLevel.BEST_PRACTICE),
    IMMUTABILITY(BaselineLevel.BEST_PRACTICE),
    INTERFACE_PROGRAMMING(BaselineLevel.BEST_PRACTICE),
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
