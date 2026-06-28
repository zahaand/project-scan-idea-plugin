package dev.zahaand.projectscan.scan.port

interface TestInfoPort {
    fun getTestSourceRoots(): List<String>

    fun getCoverageThreshold(): Double?

    fun getTestClassNames(): List<String>
}
