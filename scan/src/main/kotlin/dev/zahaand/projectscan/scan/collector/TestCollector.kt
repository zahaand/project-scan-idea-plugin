package dev.zahaand.projectscan.scan.collector

import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.TestInfo
import dev.zahaand.projectscan.scan.port.TestInfoPort

class TestCollector(private val port: TestInfoPort) {
    fun collect(): SectionResult<TestInfo> {
        val testRoots =
            try {
                port.getTestSourceRoots()
            } catch (_: Exception) {
                emptyList()
            }

        if (testRoots.isEmpty()) return SectionResult.Empty

        val classNames =
            try {
                port.getTestClassNames()
            } catch (_: Exception) {
                emptyList()
            }
        val namingSuffixes =
            classNames
                .mapNotNull { className -> SUFFIX_TOKENS.firstOrNull { token -> className.endsWith(token) } }
                .distinct()

        val coverageThreshold =
            try {
                port.getCoverageThreshold()
            } catch (_: Exception) {
                null
            }

        return SectionResult.Ok(
            TestInfo(
                sourceRoots = testRoots,
                namingSuffixes = namingSuffixes,
                coverageThreshold = coverageThreshold,
            ),
        )
    }

    companion object {
        private val SUFFIX_TOKENS = listOf("ITCase", "Tests", "Test", "IT", "Spec")
    }
}
