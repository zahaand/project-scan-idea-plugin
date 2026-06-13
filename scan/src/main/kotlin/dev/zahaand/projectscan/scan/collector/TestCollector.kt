package dev.zahaand.projectscan.scan.collector

import dev.zahaand.projectscan.model.Dependency
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.TestFramework
import dev.zahaand.projectscan.model.TestInfo
import dev.zahaand.projectscan.scan.port.TestInfoPort

class TestCollector(private val port: TestInfoPort) {

    fun collect(): SectionResult<TestInfo> {
        val testRoots = try { port.getTestSourceRoots() } catch (e: Exception) { emptyList() }

        val frameworks = mutableListOf<TestFramework>()
        val unknownTestDependencies = mutableListOf<Dependency>()
        try {
            for (dep in port.getTestScopedDependencies()) {
                val name = matchFramework(dep)
                if (name != null) frameworks += TestFramework(name, dep.resolvedVersion)
                else unknownTestDependencies += dep
            }
        } catch (e: Exception) {
            // partial failure: dependency read failed; continue with what was collected
        }

        if (testRoots.isEmpty() && frameworks.isEmpty() && unknownTestDependencies.isEmpty()) {
            return SectionResult.Empty
        }

        val classNames = try { port.getTestClassNames() } catch (e: Exception) { emptyList() }
        val namingSuffixes = classNames
            .mapNotNull { className -> SUFFIX_TOKENS.firstOrNull { token -> className.endsWith(token) } }
            .distinct()

        val coverageThreshold = try { port.getCoverageThreshold() } catch (e: Exception) { null }

        return SectionResult.Ok(
            TestInfo(
                frameworks = frameworks,
                unknownTestDependencies = unknownTestDependencies,
                sourceRoots = testRoots,
                namingSuffixes = namingSuffixes,
                coverageThreshold = coverageThreshold,
            )
        )
    }

    private fun matchFramework(dep: Dependency): String? =
        KNOWN_TEST_FRAMEWORKS.firstOrNull { matcher ->
            dep.groupId.startsWith(matcher.groupIdPrefix) &&
                (matcher.artifactIdExact == null || dep.artifactId == matcher.artifactIdExact)
        }?.canonicalName

    private data class FrameworkMatcher(
        val groupIdPrefix: String,
        val artifactIdExact: String? = null,
        val canonicalName: String,
    )

    companion object {
        private val SUFFIX_TOKENS = listOf("ITCase", "Tests", "Test", "IT", "Spec")

        private val KNOWN_TEST_FRAMEWORKS = listOf(
            FrameworkMatcher("org.junit.jupiter", canonicalName = "JUnit 5"),
            FrameworkMatcher("org.junit.vintage", canonicalName = "JUnit 4 (Vintage)"),
            FrameworkMatcher("org.junit.platform", canonicalName = "JUnit Platform"),
            FrameworkMatcher("junit", artifactIdExact = "junit", canonicalName = "JUnit 4"),
            FrameworkMatcher("org.mockito", canonicalName = "Mockito"),
            FrameworkMatcher("org.assertj", canonicalName = "AssertJ"),
            FrameworkMatcher("org.hamcrest", canonicalName = "Hamcrest"),
            FrameworkMatcher("org.testcontainers", canonicalName = "Testcontainers"),
            FrameworkMatcher("io.cucumber", canonicalName = "Cucumber"),
            FrameworkMatcher("org.awaitility", canonicalName = "Awaitility"),
            FrameworkMatcher("io.rest-assured", canonicalName = "REST Assured"),
            FrameworkMatcher("org.spockframework", canonicalName = "Spock"),
            FrameworkMatcher("org.testng", canonicalName = "TestNG"),
        )
    }
}
