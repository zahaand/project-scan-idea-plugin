package dev.zahaand.projectscan.scan.adapter

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LanguageLevelModuleExtension
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.pom.java.LanguageLevel
import dev.zahaand.projectscan.model.BuildSystem
import dev.zahaand.projectscan.scan.port.BuildSystemPort
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.plugins.gradle.settings.GradleSettings

class IjBuildSystemAdapter(private val project: Project) : BuildSystemPort {
    override fun getBuildSystem(): BuildSystem? =
        when {
            MavenProjectsManager.getInstance(project).isMavenizedProject -> BuildSystem.MAVEN
            GradleSettings.getInstance(project).linkedProjectsSettings.isNotEmpty() -> BuildSystem.GRADLE
            else -> null
        }

    override fun getModuleLanguageLevels(): Map<String, String> {
        val projectDefault = LanguageLevelProjectExtension.getInstance(project).languageLevel
        return ModuleManager.getInstance(project).modules.associate { module ->
            val moduleExt =
                ModuleRootManager.getInstance(module)
                    .getModuleExtension(LanguageLevelModuleExtension::class.java)
            val effective = moduleExt?.languageLevel ?: projectDefault
            module.name to effective.featureVersionString()
        }
    }

    override fun getJdkVersion(): String? {
        ProjectRootManager.getInstance(project).projectSdk?.name?.let { return it }
        return ModuleManager.getInstance(project).modules
            .mapNotNull { ModuleRootManager.getInstance(it).sdk?.name }
            .maxOrNull()
    }

    private fun LanguageLevel.featureVersionString(): String =
        when {
            name.startsWith("JDK_1_") -> name.removePrefix("JDK_1_")
            name.startsWith("JDK_") -> name.removePrefix("JDK_")
            else -> name
        }
}
