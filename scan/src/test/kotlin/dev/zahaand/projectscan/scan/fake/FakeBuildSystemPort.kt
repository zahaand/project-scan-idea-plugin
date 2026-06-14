package dev.zahaand.projectscan.scan.fake

import dev.zahaand.projectscan.model.BuildSystem
import dev.zahaand.projectscan.scan.port.BuildSystemPort

class FakeBuildSystemPort(
    private val buildSystem: BuildSystem?,
    private val moduleLevels: Map<String, String> = emptyMap(),
    private val jdkVersion: String? = null,
) : BuildSystemPort {
    override fun getBuildSystem(): BuildSystem? = buildSystem

    override fun getModuleLanguageLevels(): Map<String, String> = moduleLevels

    override fun getJdkVersion(): String? = jdkVersion
}
