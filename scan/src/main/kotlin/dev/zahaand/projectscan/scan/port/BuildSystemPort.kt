package dev.zahaand.projectscan.scan.port

import dev.zahaand.projectscan.model.BuildSystem

interface BuildSystemPort {
    fun getBuildSystem(): BuildSystem?

    fun getModuleLanguageLevels(): Map<String, String>

    fun getJdkVersion(): String?
}
